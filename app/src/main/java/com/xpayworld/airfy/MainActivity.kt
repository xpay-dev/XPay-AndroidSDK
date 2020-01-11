package com.xpayworld.airfy

import android.bluetooth.BluetoothDevice
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.xpayworld.sdk.payment.*

class MainActivity : AppCompatActivity() ,PaymentServiceListener {

    var devices1: MutableList<BluetoothDevice>? = null
    var service : PaymentService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        service = PaymentService(this, this)


        val saleData = SaleData()
        saleData.amount = 10000
        saleData.connection = Connection.BLUETOOTH
        saleData.currencyCode = 608
        saleData.currency = "PHP"
        saleData.cardMode = CardMode.SWIPE_OR_INSERT_OR_TAP
        saleData.orderId = "0123456789ABCDEF0123456789ABCD"


        service?.startDevice(ActionType.SALE(saleData))
    }

    override fun onBluetoothScanResult(devices: MutableList<BluetoothDevice>?) {

    }
}
