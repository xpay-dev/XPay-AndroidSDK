package com.xpayworld.payment.network

//GLOBAL HEADERS

class  APIConstant {

    companion object {
        const val Content = "Content-Type: text/json"
        const val Charset = "Accept-Charset: utf-8"
        const val ActivateApp =  "activateapp"
        const val API_HOST1  = "http://mondepay.com/PosService/POSWebserviceJSON.svc/"
        const val API_HOST = "http://mondepay.com/gateway/POSWebserviceJSON.svc/"
        const val API_HOST_= "https://ph.veritaspay.com/paymayav2/webservice/POSWebServiceJSON.svc/"
      // http://13.250.116.190:95/POSWebserviceJSON.svc
        const val EmailReceipt = "EmailReceipt"
        const val ForgotMobilePin = "ForgotMobilePin"
        const val Login = "loginpin"
        const val Registration = "Registration"
        const val CreateTicket = "CreateTicket"


        //TRANSACTION API

        const val TransCreditEMV =  "TransactionPurchaseCreditEMV"
        const val TransCreditSWIPE =  "TransactionPurchaseCreditSwipe"
        const val TransRefund =  "CreditTransactionRefund"
        const val TransVoid = "CreditTransactionVoid"

        const val TransDebit =  "TransactionPurchaseDebit"
        const val BalncInquiry =  "DebitBalanceInquiry"
        const val TransLookUp =  "TransLookUp"
        const val TransSign =  "TransSignatureCapture"
        const val TransVoidReason =  "TransactionVoidReason"
        const val UpdateApp =  "UpdateApp"

    }
}