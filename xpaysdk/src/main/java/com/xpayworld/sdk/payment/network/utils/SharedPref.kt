package com.xpayworld.payment.util

import android.content.Context
import com.xpayworld.sdk.payment.XPayLink


class SharedPref  : SharedPrefDelegate{
    private val PREF_DEV = "kXpayWorldSharedPref"

    init {
        INSTANCE = this
    }

    companion object {
        var mContext = XPayLink.CONTEXT
        var INSTANCE = SharedPref()
    }

    override fun removeKey(key: String) {
         mContext.getSharedPreferences(PREF_DEV,Context.MODE_PRIVATE).edit().remove(key).apply()
    }

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

interface SharedPrefDelegate {
    fun writeMessage(key :String ,value : String)
    fun readMessage (key : String) : String
    fun isEmpty(key :String) : Boolean
    fun removeKey (key : String)
}