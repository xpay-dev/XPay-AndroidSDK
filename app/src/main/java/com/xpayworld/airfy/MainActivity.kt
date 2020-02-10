package com.xpayworld.airfy

import android.bluetooth.BluetoothDevice
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.xpayworld.sdk.payment.network.PosWS
import com.xpayworld.sdk.payment.*
import com.xpayworld.sdk.payment.network.RetrofitClient
import com.xpayworld.sdk.payment.network.payload.Activation
import io.reactivex.disposables.Disposable

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), PaymentServiceListener {
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
        saleData.orderId = randomAlphaNumericString(30)
        saleData.isOffline = true
        //XPayLink.INSTANCE.startDevice(ActionType.SALE(saleData))

        btn_batch.setOnClickListener {
            XPayLink.INSTANCE.startAction(ActionType.BATCH_UPLOAD)
        }

        btn_connect.setOnClickListener {

            // XPayLink.INSTANCE.startAction(ActionType.ACTIVATION)
            if (devices1?.count() != 0) {
                XPayLink.INSTANCE.setBTConnection(device = devices1!![0])
            }
        }

        btn_start.setOnClickListener {
            // XPayLink.INSTANCE.startAction(ActionType.ACTIVATION)
            XPayLink.INSTANCE.startAction(ActionType.SALE(saleData))
        }
    }

    override fun onBluetoothScanResult(devices: MutableList<BluetoothDevice>?) {
        devices1 = devices
        textView.text = "${devices1!![0].name} ${devices1!![0].address}"
    }

    override fun onTransactionComplete() {
        textView.text = "Transaction Completed"
    }

    override fun onBatchUploadResult(totalTxn: Int?, unsyncTxn: Int?) {
        textView.text = "ON BATCH UPLOAD RESULT, total:  ${totalTxn} , UNSYNC: ${unsyncTxn}"
    }

    override fun onPrintComplete() {
        textView.text = "Print Completed"
    }

    override fun onError(error: Int?, message: String?) {
        textView.text = "Device ERROR  ${error}  : ${message} "
        Log.e("ERROR", "${error} : ${message}")
    }

    fun randomAlphaNumericString(desiredStrLength: Int): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..desiredStrLength)
            .map { kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }
}
