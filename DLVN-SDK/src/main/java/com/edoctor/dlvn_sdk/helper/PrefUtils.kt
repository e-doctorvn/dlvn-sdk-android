package com.edoctor.dlvn_sdk.helper

import android.content.Context
import android.content.SharedPreferences

object PrefUtils {
    private const val PREF_NAME = "sendbird_calls"
    private const val PREF_KEY_APP_ID = "edr_app_id"
    private const val PREF_KEY_USER_ID = "edr_user_id"
    private const val PREF_KEY_ACCESS_TOKEN = "edr_access_token"
    private const val PREF_KEY_EDR_TOKEN = "edr_token"
    private const val PREF_KEY_DLVN_TOKEN = "edr_dlvn_token"
    private const val PREF_KEY_SHL_TOKEN = "edr_shl_token"
    private const val PREF_KEY_SHL_USER_ID = "edr_shl_user_id"
    private const val PREF_KEY_CALLEE_ID = "edr_callee_id"
    private const val PREF_KEY_PUSH_TOKEN = "edr_push_token"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setUserId(context: Context, userId: String?) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(PREF_KEY_USER_ID, userId).apply()
    }

    fun getUserId(context: Context): String? {
        return getSharedPreferences(context).getString(PREF_KEY_USER_ID, "")
    }

    fun setShortlinkUserId(context: Context, shortlinkUserId: String?) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(PREF_KEY_SHL_USER_ID, shortlinkUserId).apply()
    }

    fun getShortlinkUserId(context: Context): String? {
        return getSharedPreferences(context).getString(PREF_KEY_SHL_USER_ID, "")
    }

    fun setAccessToken(context: Context, accessToken: String?) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(PREF_KEY_ACCESS_TOKEN, accessToken).apply()
    }

    fun getAccessToken(context: Context): String? {
        return getSharedPreferences(context).getString(PREF_KEY_ACCESS_TOKEN, "")
    }

    fun setDlvnToken(context: Context, dlvnToken: String?) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(PREF_KEY_DLVN_TOKEN, dlvnToken).apply()
    }

    fun getDlvnToken(context: Context): String? {
        return getSharedPreferences(context).getString(PREF_KEY_DLVN_TOKEN, "")
    }

    fun setEdrToken(context: Context, dlvnToken: String?) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(PREF_KEY_EDR_TOKEN, dlvnToken).apply()
    }

    fun getEdrToken(context: Context): String? {
        return getSharedPreferences(context).getString(PREF_KEY_EDR_TOKEN, "")
    }

    fun setShortlinkToken(context: Context, shortlinkToken: String?) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(PREF_KEY_SHL_TOKEN, shortlinkToken).apply()
    }

    fun getShortlinkToken(context: Context): String? {
        return getSharedPreferences(context).getString(PREF_KEY_SHL_TOKEN, "")
    }

    fun setCalleeId(context: Context, calleeId: String?) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(PREF_KEY_CALLEE_ID, calleeId).apply()
    }

    fun getCalleeId(context: Context): String? {
        return getSharedPreferences(context).getString(PREF_KEY_CALLEE_ID, "")
    }

    fun setPushToken(context: Context, pushToken: String?) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(PREF_KEY_PUSH_TOKEN, pushToken).apply()
    }

    fun getPushToken(context: Context): String? {
        return getSharedPreferences(context).getString(PREF_KEY_PUSH_TOKEN, "")
    }

    fun removeSendbirdAuthData(context: Context) {
        val editor = getSharedPreferences(context).edit()
        editor.remove(PREF_KEY_ACCESS_TOKEN).apply()
        editor.remove(PREF_KEY_USER_ID).apply()
        editor.remove(PREF_KEY_PUSH_TOKEN).apply()
    }

    fun removeSdkAuthData(context: Context) {
        val editor = getSharedPreferences(context).edit()
        editor.remove(PREF_KEY_DLVN_TOKEN).apply()
        editor.remove(PREF_KEY_EDR_TOKEN).apply()
    }

    fun removeShortLinkAuthData(context: Context) {
        val editor = getSharedPreferences(context).edit()
        editor.remove(PREF_KEY_SHL_TOKEN).apply()
        editor.remove(PREF_KEY_SHL_USER_ID).apply()
    }
}