package com.edoctor.dlvn_sdk.widget

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.edoctor.dlvn_sdk.AppointmentSchedulesQuery
import com.edoctor.dlvn_sdk.Constants
import com.edoctor.dlvn_sdk.EdoctorDlvnSdk
import com.edoctor.dlvn_sdk.R
import com.edoctor.dlvn_sdk.SubscribeToScheduleSubscription
import com.edoctor.dlvn_sdk.helper.WidgetUtils
import com.edoctor.dlvn_sdk.sendbirdCall.CallManager
import com.edoctor.dlvn_sdk.store.AppStore
import com.edoctor.dlvn_sdk.type.AppointmentScheduleState
import com.edoctor.dlvn_sdk.type.ProfileRelation

class AppointmentListAdapter() :
    RecyclerView.Adapter<AppointmentListAdapter.ViewHolder>() {
        private var dataSet: MutableList<SubscribeToScheduleSubscription.AppointmentSchedule> = mutableListOf()

    init {
        Log.d("zzz", "init list widget")
        AppStore.widgetList = this
    }

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appointmentType: TextView
        val appointmentState: TextView
        val appointmentTime: TextView
        val appointmentProfile: TextView
        val doctorAvatar: ImageView
        val doctorDegree: TextView
        val doctorFullName: TextView
        val appointmentStateBg: LinearLayout
        val negativeButton: AppCompatButton
        val positiveButton: AppCompatButton
        val consultingShortcut: LinearLayout
        val shortcutIcon: ImageView
        val shortcutText: LinearLayout

        init {
            // Define click listener for the ViewHolder's View
            negativeButton = view.findViewById(R.id.btn_negative_wg)
            positiveButton = view.findViewById(R.id.btn_positive_wg)
            doctorAvatar = view.findViewById(R.id.img_doctor_avatar_wg)
            appointmentType = view.findViewById(R.id.tv_appointment_type_wg)
            appointmentTime = view.findViewById(R.id.tv_appointment_time_wg)
            appointmentState = view.findViewById(R.id.tv_appointment_state_wg)
            appointmentProfile = view.findViewById(R.id.tv_appointment_profile_wg)
            appointmentStateBg = view.findViewById(R.id.appointment_state_bg_wg)
            doctorDegree = view.findViewById(R.id.tv_appointment_doctor_degree_wg)
            doctorFullName = view.findViewById(R.id.tv_appointment_doctor_name_wg)
            consultingShortcut = view.findViewById(R.id.consulting_shortcut_wg)
            shortcutIcon = view.findViewById(R.id.shortcut_icon_wg)
            shortcutText = view.findViewById(R.id.shortcut_text_wg)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.home_widget_layout, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    @SuppressLint("SetTextI18n", "ResourceAsColor")
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        Glide
        .with(viewHolder.itemView.context)
        .load(if (EdoctorDlvnSdk.environment == Constants.Env.SANDBOX) {
                Constants.edrAttachmentUrlDev
            } else {
                Constants.edrAttachmentUrlProd
            } + dataSet[position].doctor?.avatar
        )
        .circleCrop()
        .into(viewHolder.doctorAvatar)
        viewHolder.doctorDegree.text = dataSet[position].doctor?.degree?.shortName + " "
        viewHolder.doctorFullName.text = dataSet[position].doctor?.fullName
        viewHolder.appointmentType.text = "Lịch ${if (dataSet[position].`package`?.rawValue?.lowercase() == "chat") "nhắn tin" else "video"}:"
        viewHolder.appointmentState.text = getAppointmentState(dataSet[position].state)
        viewHolder.appointmentState.setTextColor(Color.parseColor(getAppointmentStateTextColor(dataSet[position].state)))
        viewHolder.appointmentStateBg.background.setTint(Color.parseColor(getAppointmentStateBg(dataSet[position].state)))
        dataSet[position].scheduledAt?.let {
            if (dataSet[position].state == AppointmentScheduleState.RINGING) {
                viewHolder.appointmentTime.setTextColor(R.color.red_primary)
            }
            viewHolder.appointmentTime.text = if (WidgetUtils.isToday(it as String)) "Hôm nay, lúc ${WidgetUtils.getTime(it).toString()}" else WidgetUtils.getAnotherDayTime(it)
        }
        viewHolder.appointmentProfile.text = "Tư vấn cho: ${dataSet[position].profile?.fullName?.split(" ")?.lastOrNull()} (${getProfileRelation(dataSet[position].profile?.relation)})"
        viewHolder.positiveButton.text = getPositiveButtonLabel(dataSet[position].state)

        viewHolder.positiveButton.setOnClickListener {
            if (dataSet[position].state == AppointmentScheduleState.PENDING) {
                CallManager.getInstance()?.confirmAppointmentSchedule(dataSet[position].eClinic?.eClinicId!!, dataSet[position].appointmentScheduleId!!) {
                    if (!it) { showConfirmFailedDialog() }
                }
            } else {
                dataSet[position].thirdParty?.sendbird?.channelUrl?.let { channelUrl ->
                    openConsultingRoom(channelUrl)
                }
            }
        }
        viewHolder.negativeButton.setOnClickListener {
            showCancelConfirmDialog(dataSet[position].eClinic?.eClinicId!!, dataSet[position].appointmentScheduleId!!)
        }
        if (position == itemCount - 1) {
            viewHolder.consultingShortcut.visibility = View.VISIBLE
            viewHolder.shortcutIcon.setOnClickListener { openConsultingRoom(null) }
            viewHolder.shortcutText.setOnClickListener { openConsultingRoom(null) }
        } else {
            viewHolder.consultingShortcut.visibility = View.GONE
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

    private fun getAppointmentState(state: AppointmentScheduleState?): String {
        return when (state) {
            AppointmentScheduleState.PENDING -> "Đã đặt lịch"
            AppointmentScheduleState.JOINING -> "Chờ tư vấn"
            AppointmentScheduleState.JOINED -> "Đang tư vấn"
            AppointmentScheduleState.ENDCALL -> "Đang tư vấn"
            AppointmentScheduleState.EXPIRED_RINGING -> "Bị nhỡ"
            AppointmentScheduleState.RINGING -> "Bác sĩ đang gọi"
            else -> ""
        }
    }

    private fun getAppointmentStateBg(state: AppointmentScheduleState?): String {
        return when (state) {
            AppointmentScheduleState.PENDING -> "#C2E5FF"
            AppointmentScheduleState.JOINING -> "#81C675"
            AppointmentScheduleState.JOINED -> "#069000"
            AppointmentScheduleState.ENDCALL -> "#069000"
            AppointmentScheduleState.EXPIRED_RINGING -> "#F15B57"
            AppointmentScheduleState.RINGING -> "#81C67559"
            else -> "#727272"
        }
    }

    private fun getAppointmentStateTextColor(state: AppointmentScheduleState?): String {
        return when (state) {
            AppointmentScheduleState.PENDING -> "#0D99FF"
            AppointmentScheduleState.RINGING -> "#069000"
            else -> "#FFFFFF"
        }
    }

    private fun getPositiveButtonLabel(state: AppointmentScheduleState?): String {
        return when (state) {
            AppointmentScheduleState.PENDING -> "Tư vấn ngay"
            AppointmentScheduleState.EXPIRED_RINGING -> "Xác nhận chờ gọi lại"
            AppointmentScheduleState.RINGING -> "Xác nhận chờ gọi lại"
            else -> "Nhắn tin"
        }
    }

    private fun getProfileRelation(profile: ProfileRelation?): String {
        return when (profile) {
            ProfileRelation.self -> "tôi"
            ProfileRelation.wife -> "vợ"
            ProfileRelation.husband -> "chồng"
            ProfileRelation.father -> "bố"
            ProfileRelation.mother -> "mẹ"
            ProfileRelation.daughter -> "con gái"
            else -> "con trai"
        }
    }

    private fun openConsultingRoom(channelUrl: String?) {
        try {
            if (EdoctorDlvnSdk.context is AppCompatActivity) {
                val webView = AppStore.webViewInstance
                val context = EdoctorDlvnSdk.context as AppCompatActivity
                AppStore.sdkInstance?.openWebView(context.supportFragmentManager, webView?.defaultDomain + if (channelUrl != null) "/phong-tu-van?channel=$channelUrl" else "/phong-tu-van")
            }
        } catch (_: Exception) {

        }
    }

    private fun showCancelConfirmDialog(eClinicId: String, appointmentScheduleId: String) {
        val dialog = Dialog(EdoctorDlvnSdk.context as AppCompatActivity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.cancel_schedule_confirm_dialog)
        dialog.window?.setBackgroundDrawableResource(R.drawable.corner_background)

        val closeButton = dialog.findViewById(R.id.btn_close_confirm_dialog) as LinearLayout
        val dismissButton = dialog.findViewById(R.id.btn_dismiss_cancel_dialog_wg) as AppCompatButton
        val confirmButton = dialog.findViewById(R.id.btn_confirm_cancel_appointment_wg) as TextView

        closeButton.setOnClickListener { dialog.dismiss() }
        dismissButton.setOnClickListener { dialog.dismiss() }
        confirmButton.setOnClickListener {
            CallManager.getInstance()?.cancelAppointmentSchedule(eClinicId, appointmentScheduleId) {
                if (it is String && it == "CANCELED") {
                    dialog.dismiss()
                    showCancelSuccessDialog()
                }
            }
        }

        dialog.show()
    }

    private fun showCancelSuccessDialog() {
        val dialog = Dialog(EdoctorDlvnSdk.context as AppCompatActivity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.cancel_schedule_success_dialog)
        dialog.window?.setBackgroundDrawableResource(R.drawable.corner_background)

        val closeButton = dialog.findViewById(R.id.btn_close_cancel_success_dialog) as LinearLayout
        val dismissButton = dialog.findViewById(R.id.btn_dismiss_cancel_success_dialog_wg) as AppCompatButton
        closeButton.setOnClickListener { dialog.dismiss() }
        dismissButton.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    @SuppressLint("SetTextI18n")
    private fun showConfirmFailedDialog() {
        val dialog = Dialog(EdoctorDlvnSdk.context as AppCompatActivity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.cancel_schedule_success_dialog)
        dialog.window?.setBackgroundDrawableResource(R.drawable.corner_background)

        val icon = dialog.findViewById(R.id.icon_cancel_success_dialog_wg) as ImageView
        val message = dialog.findViewById(R.id.tv_message_cancel_success_dialog_wg) as TextView
        val detail = dialog.findViewById(R.id.tv_detail_cancel_success_dialog_wg) as TextView
        val closeButton = dialog.findViewById(R.id.btn_close_cancel_success_dialog) as LinearLayout
        val dismissButton = dialog.findViewById(R.id.btn_dismiss_cancel_success_dialog_wg) as AppCompatButton

        icon.setImageResource(R.drawable.ic_action_failed)
        message.text = "Chưa thể bắt đầu phiên tư vấn"
        dismissButton.text = "Tôi đã hiểu"
        detail.visibility = View.VISIBLE
        closeButton.setOnClickListener { dialog.dismiss() }
        dismissButton.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(updatedItem: SubscribeToScheduleSubscription.AppointmentSchedule) {
        val existIndex = dataSet.indexOfFirst { it.appointmentScheduleId == updatedItem.appointmentScheduleId }
        if (existIndex != -1) {
            if (updatedItem.state == AppointmentScheduleState.CANCELED) {
                dataSet.removeAt(existIndex)
                notifyItemRemoved(existIndex)
            } else {
                dataSet[existIndex] = updatedItem
            }
        } else {
            dataSet.add(0, updatedItem)
        }
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateDataList(newData: List<AppointmentSchedulesQuery.AppointmentSchedule>) {
        for (index in newData.indices) {
            val check = newData[index].state == AppointmentScheduleState.EXPIRED || newData[index].state == AppointmentScheduleState.FINISHED || newData[index].state == AppointmentScheduleState.CANCELED || newData[index].state == AppointmentScheduleState.UNKNOWN__

            if (!check) {
                val degree =
                    SubscribeToScheduleSubscription.Degree(newData[index].doctor?.degree?.shortName)
                val doctor = SubscribeToScheduleSubscription.Doctor(
                    newData[index].doctor?.fullName,
                    degree,
                    newData[index].doctor?.doctorId,
                    newData[index].doctor?.avatar
                )
                val eclinic =
                    SubscribeToScheduleSubscription.EClinic(newData[index].eClinic?.eClinicId)
                val profile = SubscribeToScheduleSubscription.Profile(
                    newData[index].profile?.profileCode,
                    newData[index].profile?.profileId,
                    newData[index].profile?.fullName,
                    newData[index].profile?.relation,
                    newData[index].profile?.phone
                )
                val sendbird = SubscribeToScheduleSubscription.Sendbird(newData[index].thirdParty?.sendbird?.channelUrl)
                val thirdParty = SubscribeToScheduleSubscription.ThirdParty(sendbird)
                val data = SubscribeToScheduleSubscription.AppointmentSchedule(
                    newData[index].appointmentScheduleId,
                    doctor,
                    eclinic,
                    profile,
                    thirdParty,
                    newData[index].`package`,
                    newData[index].reason,
                    newData[index].scheduledAt,
                    newData[index].scheduleToken,
                    newData[index].createdAt,
                    newData[index].updatedAt as String?,
                    newData[index].state,
                    newData[index].joinAt,
                    newData[index].supportNumber,
                )
                dataSet.add(data)
            }
        }
//        dataSet = newData as MutableList<SubscribeToScheduleSubscription.AppointmentSchedule>
        notifyDataSetChanged()
    }
}
