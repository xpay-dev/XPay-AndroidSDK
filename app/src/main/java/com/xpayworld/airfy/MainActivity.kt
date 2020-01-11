package com.xpayworld.airfy

import android.bluetooth.BluetoothDevice
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.xpayworld.payment.network.PosWS
import com.xpayworld.payment.util.SharedPrefStorage
import com.xpayworld.sdk.payment.*
import com.xpayworld.sdk.payment.network.RetrofitClient
import com.xpayworld.sdk.payment.network.activation.Activation
import com.xpayworld.sdk.payment.network.activation.Login
import io.reactivex.disposables.Disposable

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() ,PaymentServiceListener {
    private lateinit var subscription: Disposable
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


       // service?.startDevice(ActionType.SALE(saleData))


        val api = RetrofitClient().getRetrofit().create(Login.API::class.java)

        var pos = PosWS.REQUEST()
        pos.activationKey = "1VEFF5YUBV39ARIZ"


        var data = Activation()
        data.imei=  ""
        data.manufacturer = ""
        data.ip = "0.0.0"
        data.posWsRequest = pos

//        subscription = api.activation(Activation.REQUEST(data))
//            .subscribe {
//
//                val sharedPref = applicationContext.let { SharedPrefStorage(it) }
//
//                sharedPref.writeMessage(PosWS.PREF_ACTIVATION,pos.activationKey!!)
//
//            }

        var lg = Login()
        lg.pin = "1234"

        subscription  = api.login(Login.REQUEST(lg))
            .subscribe {

            }

        btn_connect.setOnClickListener {
            service?.setBTConnection(device = devices1!![0])
        }
    }

    override fun onBluetoothScanResult(devices: MutableList<BluetoothDevice>?) {

        devices1 = devices

    }
}
