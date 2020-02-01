package com.xpayworld.sdk.payment.network.payload

import com.google.gson.annotations.SerializedName
import com.xpayworld.payment.util.SharedPref
import com.xpayworld.sdk.payment.network.PosWS
import com.xpayworld.sdk.payment.network.TransactionResult
import com.xpayworld.sdk.payment.data.Transaction
import com.xpayworld.sdk.payment.network.Constant
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST


class PurchaseTransaction {

    @SerializedName("AccountTypeId")
    var accountType: Int? = AccountType.NONE.ordinal

    @SerializedName("Action")
    var action: Int? = Action.NONE.ordinal

    @SerializedName("DeviceId")
    var deviceId: Int? = 7

    @SerializedName("Device")
    var device: Int? = 7

    @SerializedName("LanguageUser")
    val languageUser = "EN-CA"

    @SerializedName("ProcessOffline")
    var processOffline = false

    @SerializedName("Tips")
    var tips: Double? = 0.0

    @SerializedName("CustomerEmail")
    var customerEmail: String = ""

    @SerializedName("POSWSRequest")
    var posWsRequest: PosWS.REQUEST? = null

    @SerializedName("DeviceModelVersion")
    var deviceModelVersion: String? = ""

    @SerializedName("DeviceOsVersion")
    var deviceOsVersion: String? = ""

    @SerializedName("PosAppVersion")
    var posAppVersion: String? = ""

    @SerializedName("CardDetails")
    var cardInfo: CardDetails? = null

    companion object{
        var INSTANCE : PurchaseTransaction = PurchaseTransaction()
    }

    fun attach(data : Transaction) {
        val card = CardDetails()
        card.amount = data.amount
        card.currency = data.currency
        card.epb = ""
        card.emvICCData = data.card?.emvICCData ?: ""
        card.epbKsn = data.card?.epbksn ?: ""
        card.expMonth = data.card?.expiryMonth ?: ""
        card.expYear = data.card?.expiryYear ?: ""
        card.isFallback = data.isFallback
        card.ksn = data.card?.ksn ?: ""
        card.merchantOrderId = data.orderId
        card.track2 = data.card?.encTrack2 ?: ""
        card.refNumberApp = posWsRequest!!.activationKey +""+ System.currentTimeMillis()
        cardInfo = card
    }

    init {
        INSTANCE = this

        val sharedPref = SharedPref.INSTANCE
        val posReq = PosWS.REQUEST()
        posReq.activationKey = sharedPref.readMessage(PosWS.PREF_ACTIVATION)
        posWsRequest  = posReq
    }

    inner class CardDetails {

        @SerializedName("Amount")
        var amount = 0.0

        @SerializedName("CardNumber")
        var cardNumber = ""

        @SerializedName("Currency")
        var currency = ""

        @SerializedName("EmvICCData")
        var emvICCData = ""

        @SerializedName("Epb")
        var epb: String? = null

        @SerializedName("EpbKsn")
        var epbKsn: String? = null

        @SerializedName("ExpMonth")
        var expMonth = ""

        @SerializedName("ExpYear")
        var expYear = ""

        @SerializedName("IsFallback")
        var isFallback = false

        @SerializedName("Ksn")
        var ksn = ""

        @SerializedName("MerchantOrderId")
        var merchantOrderId = ""

        @SerializedName("NameOnCard")
        var nameOnCard = ""

        @SerializedName("RefNumberApp")
        var refNumberApp = ""

        @SerializedName("Track1")
        var track1 = ""
        @SerializedName("Track2")
        var track2 = ""

        @SerializedName("Track3")
        var track3 = ""

    }

    class REQUEST(data: PurchaseTransaction) {
        @SerializedName("request")
        var request  = data
    }

    interface API {
        @Headers(
            Constant.API.CHARSET,
            Constant.API.CONTENT)
        @POST(Constant.API.TRANS_CREDIT_EMV)
        fun EMV(@Body data: REQUEST) : Observable<Response<TransactionResult>>

        @Headers(
            Constant.API.CHARSET,
            Constant.API.CONTENT)
        @POST(Constant.API.TRANS_CREDIT_SWIPE)
        fun SWIPE(@Body  data: REQUEST) : Observable<Response<TransactionResult>>

    }

    enum class AccountType(val type: Int) {
        NONE(0), CHEQUING(1), SAVINGS(2), CURRENT(3);
        companion object : EnumCompanion<Int, AccountType>(values().associateBy(AccountType::type))
    }

    enum class Action(val type: Int) {
        NONE(0), SWIPE(1), EMV(2);
        companion object : EnumCompanion<Int, Action>(values().associateBy(Action::type))
    }

}

open class EnumCompanion<T, V>(private val valueMap: Map<T, V>) {
    fun fromInt(type: T) = valueMap[type]
}