package com.xpayworld.payment.util

interface SharedPref {

    fun writeMessage(key :String ,value : String)
    fun readMessage (key : String) : String
    fun isEmpty(key :String) : Boolean
    fun removeKey (key : String)
}