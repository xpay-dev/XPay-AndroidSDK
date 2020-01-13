package com.xpayworld.sdk.payment.network.payload.transaction

import com.xpayworld.payment.data.Card

class  PurchaseInfo{

    var amount = 0.0
    var tmpAmount = 0.0
    var currencyCode  = "608"
    var currency = "PHP"
    var orderId = ""
//    var paymentType : PaymentType? = null
    var isOffline  = false
    var isFallback = false
    var customerEmail =  ""
    var deviceModelVersion = ""
    var deviceOsVersion = ""
    var posAppVersion = ""
    var timestamp : Long = 0L
    var cardCaptureMethod = 5
    var device = 0
    var card : Card? = null

}