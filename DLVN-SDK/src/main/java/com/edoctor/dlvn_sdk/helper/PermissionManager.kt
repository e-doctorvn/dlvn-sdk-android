package com.edoctor.dlvn_sdk.helper

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.edoctor.dlvn_sdk.EdoctorDlvnSdk
import com.edoctor.dlvn_sdk.R

object PermissionManager {
    private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    private const val MICROPHONE_PERMISSION_REQUEST_CODE = 1002
    private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1003
    const val ALL_PERMISSIONS_REQUEST_CODE = 1004

    fun handleRequestPermission(activity: Activity, permission: String, requestPermissionLauncher: ActivityResultLauncher<String>) {
        when {
            ContextCompat.checkSelfPermission(
                activity,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {

            }
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) -> {
                requestPermissionLauncher.launch(permission)
                setPermissionAsked(activity, permission)
            }
            else -> {
                if (getRationalDisplayStatus(activity, permission)) {
                    EdoctorDlvnSdk.showError(getDisplayMessage(activity, permission))
                    activity.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", activity.packageName, null)
                    })
                } else {
                    requestPermissionLauncher.launch(permission)
                }
            }
        }
    }

    fun setPermissionAsked(context: Context, permission: String?) {
        val genPrefs = context.getSharedPreferences("GENERIC_PREFERENCES", Context.MODE_PRIVATE)
        val editor = genPrefs.edit()
        editor.putBoolean(permission, true)
        editor.apply()
    }

    fun getRationalDisplayStatus(context: Context, permission: String?): Boolean {
        val genPrefs = context.getSharedPreferences("GENERIC_PREFERENCES", Context.MODE_PRIVATE)
        return genPrefs.getBoolean(permission, false)
    }

    private fun getDisplayMessage(activity: Activity, permission: String): String {
        when (permission) {
            Manifest.permission.POST_NOTIFICATIONS -> {
                return activity.getString(R.string.request_noti_permission_msg)
            }
        }

        return activity.getString(R.string.request_permission_msg)
    }
}