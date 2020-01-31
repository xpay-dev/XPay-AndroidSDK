package com.xpayworld.sdk.payment.network
//GLOBAL HEADERS

class Constant {


    companion object{
        const val DATABASE_NAME = "XPAY-DB.db"
    }

    class API {
        companion object {
            const val CONTENT = "Content-Type: text/json"
            const val CHARSET = "Accept-Charset: utf-8"
            const val ACTIVATE_APP = "activateapp"
            const val API_HOST1 = "http://mondepay.com/PosService/POSWebserviceJSON.svc/"
            const val API_HOST = "http://mondepay.com/gateway/POSWebserviceJSON.svc/"
            const val API_HOST_ =
                "https://ph.veritaspay.com/paymayav2/webservice/POSWebServiceJSON.svc/"
            // http://13.250.116.190:95/POSWebserviceJSON.svc
            const val EmailReceipt = "EmailReceipt"
            const val ForgotMobilePin = "ForgotMobilePin"
            const val PIN_LOGIN = "loginpin"
            const val Registration = "Registration"
            const val CreateTicket = "CreateTicket"

            //TRANSACTION API

            const val TRANS_CREDIT_EMV = "TransactionPurchaseCreditEMV"
            const val TRANS_CREDIT_SWIPE = "TransactionPurchaseCreditSwipe"
            const val TransRefund = "CreditTransactionRefund"
            const val TransVoid = "CreditTransactionVoid"

            const val TransDebit = "TransactionPurchaseDebit"
            const val BalncInquiry = "DebitBalanceInquiry"
            const val TransLookUp = "TransLookUp"
            const val TransSign = "TransSignatureCapture"
            const val TransVoidReason = "TransactionVoidReason"
            const val UpdateApp = "UpdateApp"

        }
    }
}