package com.xpayworld.airfy

import android.bluetooth.BluetoothDevice
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.xpayworld.sdk.payment.network.PosWS
import com.xpayworld.sdk.payment.*
import com.xpayworld.sdk.payment.data.TransactionRepository
import com.xpayworld.sdk.payment.data.XPayDatabase
import com.xpayworld.sdk.payment.network.RetrofitClient
import com.xpayworld.sdk.payment.network.payload.Activation
import io.reactivex.disposables.Disposable

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() ,PaymentServiceListener {
    private lateinit var subscription: Disposable
    var devices1: MutableList<BluetoothDevice>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        XPayLink.INSTANCE.attach(this, this)

        val saleData = Sale()
        saleData.amount = 10000
        saleData.connection = Connection.BLUETOOTH
        saleData.currencyCode = 608
        saleData.currency = "PHP"
        saleData.cardMode = CardMode.SWIPE_OR_INSERT_OR_TAP
        saleData.orderId = randomAlphaNumericString(8)
        saleData.isOffline = true
//        XPayLink.INSTANCE.startDevice(ActionType.SALE(saleData))


//      service?.startDevice(ActionType.SALE(saleData))
        val api = RetrofitClient().getRetrofit().create(Activation.API::class.java)

        var pos = PosWS.REQUEST()
        pos.activationKey = "1VEFF5YUBV39ARIZ"


        var data = Activation()
        data.imei=  ""
        data.manufacturer = ""
        data.ip = "0.0.0"
        data.posWsRequest = pos

//        subscription = api.activation(Activation.REQUEST(data))
//            .subscribe {
////
////                val sharedPref = applicationContext.let { SharedPrefStorage(it) }
////                sharedPref.writeMessage(PosWS.PREF_ACTIVATION,pos.activationKey!!)
//
//            }

//        var lg = Login()
//        lg.pin = "1234"
//
//        subscription  = api.login(Login.REQUEST(lg))
//            .subscribe {
//
//            }




        btn_fetch.setOnClickListener {
           XPayLink.INSTANCE.startAction(ActionType.BATCH_UPLOAD)
        }

        btn_connect.setOnClickListener {
           // XPayLink.INSTANCE.startAction(ActionType.ACTIVATION)
              XPayLink.INSTANCE.setBTConnection(device = devices1!![0])
        }

        btn_retrieve.setOnClickListener {
            XPayLink.INSTANCE.startAction(ActionType.SALE(saleData))
        }
    }

    override fun onBluetoothScanResult(devices: MutableList<BluetoothDevice>?) {
        devices1 = devices
    }



    override fun onTransactionResult(result: Int?, message: String?) {
        println("Transaction RESULT  ${result}  : ${message} ")
    }

    override fun onError(error: Int?, message: String?) {

        println("Device ERROR  ${error}  : ${message} ")

    }

    fun randomAlphaNumericString(desiredStrLength: Int): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..desiredStrLength)
            .map{ kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }


}
