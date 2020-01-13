package com.xpayworld.sdk.payment.network.payload

import com.google.gson.annotations.SerializedName
import com.xpayworld.payment.network.APIConstant
import com.xpayworld.payment.network.PosWS
import io.reactivex.Observable

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

class Activation {

    @SerializedName("IMEI")
    var imei: String? = null

    @SerializedName("IP")
    var ip: String? = null

    @SerializedName("Manufacturer")
    var manufacturer: String? = null

    @SerializedName("Model")
    var model: String? = null

    @SerializedName("OS")
    private var os: String? = null

    @SerializedName("POSWSRequest")
    var posWsRequest: PosWS.REQUEST? = null

    @SerializedName("PhoneNumber")
    var phoneNumber: String? = null

    @SerializedName("Platform")
    var platform = "Android"
    @SerializedName("PosType")
    var postType = "BBPOS"


    class REQUEST(data: Activation) {
        @SerializedName("posInfo")
        var data = data
    }

    class RESULT{
        @SerializedName("ActivateAppResult")
        var data = PosWS.RESPONSE()
    }

    interface API {
        @Headers(
            APIConstant.Charset,
            APIConstant.Content)
        @POST(APIConstant.ActivateApp)
        fun activation(@Body activate :  REQUEST): Observable<Response<RESULT>>
    }

}