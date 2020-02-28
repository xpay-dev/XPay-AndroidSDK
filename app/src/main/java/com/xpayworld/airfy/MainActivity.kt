package com.xpayworld.airfy

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.xpayworld.sdk.payment.*
import com.xpayworld.sdk.payment.Currency
import com.xpayworld.sdk.payment.utils.*
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.util.*


class MainActivity : AppCompatActivity(), PaymentServiceListener {
    private lateinit var subscription: Disposable
    var devices1: MutableList<BluetoothDevice>? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        XPayLink.INSTANCE.attach(this, this)

//        val saleData = Sale()
//            saleData.amount = 10000
//            saleData.connection = Connection.BLUETOOTH
//            saleData.currencyCode = 608
//            saleData.currency = "PHP"
//            saleData.cardMode = CardMode.SWIPE_OR_INSERT_OR_TAP
//            saleData.orderId = randomAlphaNumericString(30)
//            saleData.isOffline = true
            //XPayLink.INSTANCE.startDevice(ActionType.SALE(saleData))

        // CONNECT TO BLUETOOTH DEVICE
        btn_connect.setOnClickListener {
            XPayLink.INSTANCE.Connect()
        }

        // UPLOAD TRANSACTIONS
        btn_batch.visibility = View.INVISIBLE
        btn_batch.setOnClickListener {
            //            XPayLink.INSTANCE.startAction(ActionType.BATCH_UPLOAD)
            val print = PrintDetails()
            print.data = genReceipt()
            print.numOfReceipt = 2
            XPayLink.INSTANCE.startAction(ActionType.PRINT(print))
        }

        // PRINT
        btn_print.visibility = View.INVISIBLE
        btn_print.setOnClickListener {
            // XPayLink.INSTANCE.startAction(ActionType.ACTIVATION)
            XPayLink.INSTANCE.PrintBegin()
        }

        // START TRANSACTION
        btn_start.visibility = View.INVISIBLE
        btn_start.setOnClickListener {
            XPayLink.INSTANCE.ResetProperties()
                XPayLink.INSTANCE.setAmountPurchase(99.99)
                XPayLink.INSTANCE.setCurrencyCode(840)
//                XPayLink.INSTANCE.setCurrency("USD")
                XPayLink.INSTANCE.setCurrency(Currency.DOLLAR.value)
                XPayLink.INSTANCE.setCardCaptureMethod(CardMode.INSERT)
                XPayLink.INSTANCE.setOrderID(randomAlphaNumericString(30))
            XPayLink.INSTANCE.Transaction()
        }
    }

//=====================================================================================
//#region XPay Callbacks
//    override fun onBluetoothScanResult(devices: MutableList<BluetoothDevice>?) {
////        devices1 = devices
////        textView.text = "${devices1!![0].name} ${devices1!![0].address}"
//    }
//
//    override fun onTransactionComplete() {
////        textView.text = "Transaction Completed"
//    }
//
//    override fun onBatchUploadResult(totalTxn: Int?, unsyncTxn: Int?) {
////        textView.text = "ON BATCH UPLOAD RESULT, total:  ${totalTxn} , UNSYNC: ${unsyncTxn}"
//    }
//
//    override fun onPrintComplete() {
////        textView.text = "Print Completed"
//    }
//
//    override fun onError(error: Int?, message: String?) {
////        textView.text = "Device ERROR  ${error}  : ${message} "
////        Log.e("ERROR", "${error} : ${message}")
//    }
//#endregion XPay Callbacks

//#region Receipt Generator
    fun randomAlphaNumericString(desiredStrLength: Int): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..desiredStrLength)
            .map { kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }


    fun genReceipt(): ByteArray?
    {
        val lineWidth = 384
        val size0NoEmphasizeLineWidth = 384 / 8 //line width / font width
        var singleLine = ""
        for (i in 0 until size0NoEmphasizeLineWidth) {
            singleLine += "-"
        }
        var doubleLine = ""
        for (i in 0 until size0NoEmphasizeLineWidth) {
            doubleLine += "="
        }
        try {
            val baos = ByteArrayOutputStream()
            baos.write(INIT)
            baos.write(POWER_ON)

            baos.write(NEW_LINE)
            baos.write(CHAR_SPACING_0)
            baos.write(FONT_SIZE_0)
            baos.write(EMPHASIZE_ON)
            baos.write(FONT_5X12)
            baos.write("Suite 1602, 16/F, Tower 2".toByteArray())
            baos.write(NEW_LINE)
            baos.write("Nina Tower, No 8 Yeung Uk Road".toByteArray())
            baos.write(NEW_LINE)
            baos.write("Tsuen Wan, N.T., Hong Kong".toByteArray())
            baos.write(NEW_LINE)
            baos.write(FONT_SIZE_1)
            baos.write(FONT_5X12)
            baos.write("OFFICIAL RECEIPT".toByteArray())
            baos.write(NEW_LINE)
            baos.write(FONT_SIZE_0)
            baos.write(EMPHASIZE_OFF)
            baos.write(FONT_10X18)
            baos.write("Form No. 2524".toByteArray())
            baos.write(NEW_LINE)
            baos.write(FONT_8X12)
            baos.write(singleLine.toByteArray())
            baos.write(NEW_LINE)
            baos.write(ALIGN_LEFT)
            baos.write(FONT_10X18)
            baos.write("ROR NO ".toByteArray())
            baos.write(EMPHASIZE_ON)
            baos.write("ROR2014-000556-000029".toByteArray())
            baos.write(NEW_LINE)
            baos.write(EMPHASIZE_OFF)
            baos.write("DATE/TIME ".toByteArray())
            baos.write(EMPHASIZE_ON)
            baos.write("08/20/2014 10:42:46 AM".toByteArray())
            baos.write(NEW_LINE)
            baos.write(EMPHASIZE_OFF)
            baos.write(FONT_8X12)
            baos.write(singleLine.toByteArray())
            baos.write(NEW_LINE)
            baos.write(FONT_10X18)
            baos.write(EMPHASIZE_ON)
            baos.write("CHAN TAI MAN".toByteArray())
            baos.write(NEW_LINE)
            baos.write(NEW_LINE)
            baos.write(EMPHASIZE_OFF)
            baos.write("BIR FORM NO : ".toByteArray())
            baos.write(EMPHASIZE_ON)
            baos.write("0605".toByteArray())
            baos.write(NEW_LINE)
            baos.write(EMPHASIZE_OFF)
            baos.write("TYPE : ".toByteArray())
            baos.write(EMPHASIZE_ON)
            baos.write("AP".toByteArray())
            baos.write(NEW_LINE)
            baos.write(EMPHASIZE_OFF)
            baos.write("PERIOD COVERED : ".toByteArray())
            baos.write(EMPHASIZE_ON)
            baos.write("2014-8-20".toByteArray())
            baos.write(NEW_LINE)
            baos.write(EMPHASIZE_OFF)
            baos.write("ASSESSMENT NO : ".toByteArray())
            baos.write(EMPHASIZE_ON)
            baos.write("885".toByteArray())
            baos.write(NEW_LINE)
            baos.write(EMPHASIZE_OFF)
            baos.write("DUE DATE : ".toByteArray())
            baos.write(EMPHASIZE_ON)
            baos.write("2014-8-20".toByteArray())
            baos.write(NEW_LINE)
            baos.write(NEW_LINE)
            var fontSize = 0
            var fontWidth = 10 * (fontSize + 1) + (fontSize + 1)
            var s1 = "PARTICULARS"
            var s2 = "AMOUNT"
            var s = s1
            var numOfCharacterPerLine = lineWidth / fontWidth
            for (i in 0 until numOfCharacterPerLine - s1.length - s2.length) {
                s += " "
            }
            s += s2
            baos.write(s.toByteArray())
            baos.write(NEW_LINE)
            baos.write(NEW_LINE)
            fontSize = 0
            fontWidth = 10 * (fontSize + 1)
            s1 = "BASIC"
            s2 = "100.00"
            s = s1
            numOfCharacterPerLine = lineWidth / fontWidth
            for (i in 0 until numOfCharacterPerLine - s1.length - s2.length) {
                s += " "
            }
            s += s2
            baos.write(EMPHASIZE_OFF)
            baos.write(s.toByteArray())
            baos.write(NEW_LINE)
            s1 = "    SUBCHANGE"
            s2 = "500.00"
            s = s1
            for (i in 0 until numOfCharacterPerLine - s1.length - s2.length) {
                s += " "
            }
            s += s2
            baos.write(s.toByteArray())
            baos.write(NEW_LINE)
            s1 = "    INTEREST"
            s2 = "0.00"
            s = s1
            for (i in 0 until numOfCharacterPerLine - s1.length - s2.length) {
                s += " "
            }
            s += s2
            baos.write(s.toByteArray())
            baos.write(NEW_LINE)
            s1 = "    COMPROMISE"
            s2 = "0.00"
            s = s1
            for (i in 0 until numOfCharacterPerLine - s1.length - s2.length) {
                s += " "
            }
            s += s2
            baos.write(s.toByteArray())
            baos.write(NEW_LINE)
            s1 = "TOTAL"
            s2 = "500.00"
            s = s1
            for (i in 0 until numOfCharacterPerLine - s1.length - s2.length) {
                s += " "
            }
            s += s2
            baos.write(s.toByteArray())
            baos.write(NEW_LINE)
            baos.write(FONT_8X12)
            baos.write(singleLine.toByteArray())
            baos.write(NEW_LINE)
            s1 = "TOTAL AMOUNT DUE"
            s2 = "600.00"
            s = s1
            for (i in 0 until numOfCharacterPerLine - s1.length - s2.length) {
                s += " "
            }
            s += s2
            baos.write(FONT_10X18)
            baos.write(s.toByteArray())
            baos.write(NEW_LINE)
            baos.write(FONT_8X12)
            baos.write(doubleLine.toByteArray())
            baos.write(NEW_LINE)
            s1 = "TOTAL AMOUNT PAID"
            s2 = "600.00"
            s = s1
            for (i in 0 until numOfCharacterPerLine - s1.length - s2.length) {
                s += " "
            }
            s += s2
            baos.write(FONT_10X18)
            baos.write(s.toByteArray())
            baos.write(NEW_LINE)
            baos.write(EMPHASIZE_ON)
            baos.write("SIX HUNDRED DOLLARS ONLY".toByteArray())
            baos.write(NEW_LINE)
            baos.write(EMPHASIZE_OFF)
            baos.write(FONT_8X12)
            baos.write(singleLine.toByteArray())
            baos.write(NEW_LINE)
            baos.write(FONT_10X18)
            baos.write("MANNER OF PAYMENT".toByteArray())
            baos.write(NEW_LINE)
            baos.write(EMPHASIZE_ON)
            baos.write(" ACCOUNTS RECEIVABLE".toByteArray())
            baos.write(NEW_LINE)
            baos.write(EMPHASIZE_OFF)
            baos.write("TYPE OF PAYMENT".toByteArray())
            baos.write(NEW_LINE)
            baos.write(EMPHASIZE_ON)
            baos.write(" FULL".toByteArray())
            baos.write(NEW_LINE)
            baos.write(EMPHASIZE_OFF)
            baos.write("MODE OF PAYMENT".toByteArray())
            baos.write(NEW_LINE)
            baos.write(EMPHASIZE_ON)
            baos.write("  CASH".toByteArray())
            baos.write(NEW_LINE)
            s1 = "  AMOUNT"
            s2 = "600.00"
            s = s1
            for (i in 0 until numOfCharacterPerLine - s1.length - s2.length) {
                s += " "
            }
            s += s2
            baos.write(EMPHASIZE_OFF)
            baos.write(s.toByteArray())
            baos.write(NEW_LINE)
            baos.write("REMARKS".toByteArray())
            baos.write(NEW_LINE)
            baos.write(EMPHASIZE_ON)
            baos.write("TEST".toByteArray())
            baos.write(NEW_LINE)
            baos.write(EMPHASIZE_OFF)
            baos.write(FONT_8X12)
            baos.write(singleLine.toByteArray())
            baos.write(NEW_LINE)
            baos.write(ALIGN_CENTER)
            baos.write(FONT_SIZE_1)
            baos.write(EMPHASIZE_ON)
            baos.write(FONT_8X12)
            baos.write("CARDHOLDER'S COPY".toByteArray())
            baos.write(NEW_LINE)
            baos.write(ALIGN_LEFT)
            baos.write(FONT_SIZE_0)
            baos.write(EMPHASIZE_OFF)
            baos.write(singleLine.toByteArray())
            baos.write(NEW_LINE)
            baos.write(ALIGN_CENTER)
            baos.write(FONT_5X12)
            baos.write("This is to certify that the amount indicated herein has".toByteArray())
            baos.write(NEW_LINE)
            baos.write("been received by the undersigned".toByteArray())
            baos.write(NEW_LINE)
            baos.write(NEW_LINE)
            baos.write(NEW_LINE)
            baos.write(NEW_LINE)
            baos.write(NEW_LINE)
            baos.write(NEW_LINE)
            baos.write(NEW_LINE)
            baos.write(EMPHASIZE_ON)
            baos.write(FONT_10X18)
            baos.write("CHAN SIU MING".toByteArray())
            baos.write(NEW_LINE)
            val barcode = "B B P O S"
            val barcodeData =
                Hashtable<String, String>()
            barcodeData["barcodeDataString"] = barcode
            barcodeData["barcodeHeight"] = "" + 50
            barcodeData["barcodeType"] = "128"
            val barcodeCommand: ByteArray =  getBarcodeCommand(barcodeData)
            baos.write(barcodeCommand)
            baos.write(EMPHASIZE_ON)
            baos.write(FONT_10X18)
            baos.write(NEW_LINE)
            baos.write(barcode.toByteArray())
            baos.write(NEW_LINE)
            baos.write(NEW_LINE)
            baos.write(POWER_OFF)
            return baos.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
//#endregion Receipt Generator

    //=====================================================================================
    // AirFi Callbacks
    override fun InitialiseComplete()
    {
        textView.text = "Hi AirFi"
    }

//    override fun OnTerminalConnectedChanged(devices: MutableList<BluetoothDevice>?)
    override fun OnTerminalConnectedChanged(device: BluetoothDevice?)
    {
//        devices1 = devices
////        if (devices1?.count() != 0) {
////            XPayLink.INSTANCE.setBTConnection(device = devices1!![0])
////        }
//        textView.text = "${devices1!![0].name} ${devices1!![0].address}"

        textView.text = "${device?.address}"
        btn_start.visibility = View.VISIBLE
        btn_print.visibility = View.VISIBLE
    }

    override fun TransactionComplete()
    {
        textView.text = "Transaction Completed"
        btn_batch.visibility = View.VISIBLE
    }

    override fun OnBatchUploadResult(totalTxn: Int?, unsyncTxn: Int?)
    {
        textView.text = "ON BATCH UPLOAD RESULT, total:  ${totalTxn} , UNSYNC: ${unsyncTxn}"
    }

    override fun OnError(error: Int?, message: String?)
    {
        textView.text = "Device ERROR  ${error}  : ${message} "
        Log.e("ERROR", "${error} : ${message}")
    }

    override fun PrintComplete()
    {
        textView.text = "Print Completed"
    }
}
