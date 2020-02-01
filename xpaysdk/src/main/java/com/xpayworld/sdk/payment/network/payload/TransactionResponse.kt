package com.xpayworld.sdk.payment.network.payload

import com.google.gson.annotations.SerializedName
import com.xpayworld.sdk.payment.network.PosWS

class TransactionResponse {

        @SerializedName("AcquirerName ")
        var acquirerName: String? = null

        @SerializedName("ApprovalCode")
        var approvalCode: String? = null

        @SerializedName("AuthNumber")
        var authNumber: String? = null

        @SerializedName("BankCardAssoc")
        var bankCardAssoc: String? = null

        @SerializedName("BatchNumber")
        var batchNumber: String? = null

        @SerializedName("CardExpiry")
        var cardExpiry: String? = null

        @SerializedName("CardNumber")
        var cardNumber: String? = null

        @SerializedName("CardType")
        var cardType: String? = null

        @SerializedName("Currency")
        var currency: String? = "PHP"

        @SerializedName("MerchantId")
        var merchantId: String? = null

        @SerializedName("MerchantOrderId")
        var merchantOrderId: String? = null

        @SerializedName("MobileAppTransType")
        var mobileAppTransType: String? = null

        @SerializedName("Mode")
        var mode: String? = null

        @SerializedName("NameOnCard")
        var nameOnCard: String? = null

        @SerializedName("Notes")
        var notes: String? = null

        @SerializedName("PaymentRefNumber")
        var paymentRefNumber: String? = null

        @SerializedName("ResponseIsoCode")
        var responseIsoCode: String? = null

        @SerializedName("SequenceNumber")
        var sequenceNumber: String? = null

        @SerializedName("SubTotal")
        var subTotal: String? = null

        @SerializedName("Tax1Amount")
        var tax1Amount: String? = null

        @SerializedName("Tax1Name")
        var tax1Name: String? = null

        @SerializedName("Tax1Rate")
        var tax1Rate: String? = null

        @SerializedName("Tax2Amount")
        var tax2Amount: String? = null

        @SerializedName("Tax2Name")
        var tax2Name: String? = null

        @SerializedName("Tax2Rate")
        var tax2Rate: String? = null

        @SerializedName("POSWSResponse")
        var result : PosWS.RESPONSE? = null

        @SerializedName("TerminalId")
        var terminalId: String? = null

        @SerializedName("Timestamp")
        var timestamp: String? = "11/4/2019 10:58:03 PM"

        @SerializedName("Tips")
        var tips: String? = null

        @SerializedName("Total")
        var total: String? = ""

        @SerializedName("TransactionEntryType")
        var transactionEntryType: String? = null

        @SerializedName("TransactionNumber")
        var transNumber: String? = ""

        @SerializedName("TransactionReqToken ")
        var transactionReqToken: String? = null

        @SerializedName("TransactionSignature")
        var transactionSignature: String? = null

        @SerializedName("TransactionType")
        var transType: String? = "Sale"

}