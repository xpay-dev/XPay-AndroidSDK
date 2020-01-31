package com.xpayworld.sdk.payment.data

import androidx.room.ColumnInfo
import androidx.room.Entity


@Entity
data class Card(

    //  @ColumnInfo(name = "trans_number")
    var emvICCData: String = "",// how often the plant should be watered, in days

    //   @ColumnInfo(name = "merchant_name")
    var encTrack2: String = "",

    //   @ColumnInfo(name = "merchant_name")
    var expiryMonth: String = "",

    //   @ColumnInfo(name = "merchant_name")
    var expiryYear: String = "",

    //    @ColumnInfo(name = "merchant_name")
    var expiryDate: String = "",

    //  @ColumnInfo(name = "merchant_name")
    var ksn: String = "",

    //   @ColumnInfo(name = "merchant_name")
    var epb: String = "",

    //  @ColumnInfo(name = "merchant_name")
    var epbksn: String = "",

    //   @ColumnInfo(name = "merchant_name")
    var serviceCode: String = "",
    var cardNumber: String = "",

    var cardXNumber: String = "",

    var posEntry: Int = 0
)


