package com.xpayworld.sdk.payment.network

import com.google.gson.annotations.SerializedName

data class TransactionResult(
    
    @SerializedName("AcquirerName ")
    val acquirerName: String,

    @SerializedName("ApprovalCode")
    var approvalCode: String,

    @SerializedName("AuthNumber")
    var authNumber: String,

    @SerializedName("BankCardAssoc")
    var bankCardAssoc: String,

    @SerializedName("BatchNumber")
    var batchNumber: String,

    @SerializedName("CardExpiry")
    var cardExpiry: String,

    @SerializedName("CardNumber")
    var cardNumber: String,

    @SerializedName("CardType")
    var cardType: String,

    @SerializedName("Currency")
    var currency: String,

    @SerializedName("MerchantId")
    var merchantId: String,

    @SerializedName("MerchantOrderId")
    var merchantOrderId: String,

    @SerializedName("MobileAppTransType")
    var mobileAppTransType: String,

    @SerializedName("Mode")
    var mode: String,

    @SerializedName("NameOnCard")
    var nameOnCard: String,

    @SerializedName("Notes")
    var notes: String,

    @SerializedName("PaymentRefNumber")
    var paymentRefNumber: String,

    @SerializedName("ResponseIsoCode")
    var responseIsoCode: String,

    @SerializedName("SequenceNumber")
    var sequenceNumber: String,

    @SerializedName("SubTotal")
    var subTotal: String,

    @SerializedName("Tax1Amount")
    var tax1Amount: String,

    @SerializedName("Tax1Name")
    var tax1Name: String?,

    @SerializedName("Tax1Rate")
    var tax1Rate: String?,

    @SerializedName("Tax2Amount")
    var tax2Amount: String?,

    @SerializedName("Tax2Name")
    var tax2Name: String?,

    @SerializedName("Tax2Rate")
    var tax2Rate: String?,

//    @SerializedName("POSWSResponse")
//    var result: PosWsResponse? = null

    @SerializedName("TerminalId")
    var terminalId: String?,

    @SerializedName("Timestamp")
    var timestamp: String? = "11/4/2019 10:58:03 PM",

    @SerializedName("Tips")
    var tips: String?,

    @SerializedName("Total")
    var total: String?,

    @SerializedName("TransactionEntryType")
    var transactionEntryType: String?,

    @SerializedName("TransactionNumber")
    var transNumber: String?,

    @SerializedName("TransactionReqToken ")
    var transactionReqToken: String?,

    @SerializedName("TransactionSignature")
    var transactionSignature: String?,

    @SerializedName("TransactionType")
    var transType: String? = "Sale"
)