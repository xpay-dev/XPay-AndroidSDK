package com.xpayworld.payment.network

import com.google.gson.annotations.SerializedName
import com.xpayworld.payment.util.SharedPref

class PosWS {
    companion object {
        const val PREF_ACTIVATION = "kActivationSecret"
        const val PREF_RTOKEN = "kRtokenSession"
        const val PREF_PIN = "kPinEnteredSecrete"
    }

    class REQUEST {
        @SerializedName("ActivationKey")
        var activationKey: String? = null

        @SerializedName("GPSLat")
        var gpsLat: String? = "0.0"

        @SerializedName("GPSLong")
        var gpsLong: String? = "0.0"

        @SerializedName("RToken")
        var rToken: String? = ""

        @SerializedName("SystemMode")
        var systemMode: String? = "Live"


        init {
            val sharedPref = SharedPref.INSTANCE
            activationKey = sharedPref.readMessage(PREF_ACTIVATION)
            rToken = sharedPref.readMessage(PREF_RTOKEN)
        }
    }

    class RESPONSE {

        @SerializedName("AccountId")
        val accountId: String? = null

        @SerializedName("ErrNumber")
        val errNumber: Double? = null

        @SerializedName("Message")
        val message: String? = null

        @SerializedName("MobileAppId")
        val mobileAppId: String? = null

        @SerializedName("RToken")
        val rToken: String? = null

        @SerializedName("SequenceNumber")
        val sequenceNumber: String? = null

        @SerializedName("Status")
        val status: String? = null

        @SerializedName("UpdatePending")
        val updatePending: Boolean = false

    }
}

