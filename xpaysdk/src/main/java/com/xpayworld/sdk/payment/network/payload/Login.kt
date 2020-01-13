package com.xpayworld.sdk.payment.network.payload

import com.google.gson.annotations.SerializedName
import com.xpayworld.payment.network.APIConstant
import com.xpayworld.payment.network.PosWS

import com.xpayworld.payment.util.SharedPref
import io.reactivex.Observable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST


class Login {

    @SerializedName("AppVersion")
    var appVersion = "1"

    @SerializedName("Pin")
    var pin = ""

    @SerializedName("Password")
    var password = ""

    @SerializedName("Username")
    var userName = ""

    @SerializedName("POSWSRequest")
    private var posWsRequest: PosWS.REQUEST? = null

    init {
        val sharedPref = SharedPref.INSTANCE
        val posReq = PosWS.REQUEST()
        posReq.activationKey = sharedPref.readMessage(PosWS.PREF_ACTIVATION)
        posWsRequest  = posReq
    }

    class REQUEST(data: Login) {
        @SerializedName("request")
        var data = data
    }
    class RESULT {
        @SerializedName("LoginByMobilePinResult")
        var data = PosWS.RESPONSE()
    }

    interface API {
        @Headers(
            APIConstant.Charset,
            APIConstant.Content)
        @POST(APIConstant.Login)
        fun login(@Body login: REQUEST) : Observable<Response<RESULT>>
    }
}