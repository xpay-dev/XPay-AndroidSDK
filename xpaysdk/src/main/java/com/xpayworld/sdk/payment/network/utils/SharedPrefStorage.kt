package com.xpayworld.payment.util

import android.content.Context


class SharedPrefStorage(context: Context) : SharedPref{
    override fun removeKey(key: String) {
         mContext.getSharedPreferences(PREF_DEV,Context.MODE_PRIVATE).edit().remove(key).apply()
    }

    var mContext = context
    override fun readMessage(key: String): String {
        return mContext.getSharedPreferences(PREF_DEV,Context.MODE_PRIVATE).getString(key,"")!!
    }

    override fun isEmpty(key: String): Boolean {
        return mContext.getSharedPreferences(PREF_DEV, Context.MODE_PRIVATE)
                .getString(key, "") == ""
    }

    override fun writeMessage(key: String, value: String) {
        mContext.getSharedPreferences(PREF_DEV,Context.MODE_PRIVATE)
                .edit().putString(key,value).apply()
    }
}