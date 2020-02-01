package com.xpayworld.sdk.payment.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Transaction(
    var amount: Double = 0.0,
    var currencyCode: String = "",
    var currency: String = "",
    var orderId: String = "",
    var isOffline: Boolean = false,
    var isFallback: Boolean = false,
    var isSync: Boolean = false,
    var deviceModelVersion: String = "",
    var deviceOsVersion: String = "",
    var posAppVersion: String = "",
    var signature: String = "",
    @ColumnInfo(name = "trans_date")
    var timestamp : Long = 0L,
    var cardCaptureMethod: Int = 0,
    var device: Int = 0,
    var errorMessage: String = "",
    @Embedded
    var card : Card? = null
)
{
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}
