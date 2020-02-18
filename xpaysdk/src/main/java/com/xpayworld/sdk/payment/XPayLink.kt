package com.xpayworld.sdk.payment

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.bbpos.bbdevice.BBDeviceController
import com.bbpos.bbdevice.BBDeviceController.CurrencyCharacter
import com.bbpos.bbdevice.CAPK
import com.xpayworld.payment.util.SharedPref
import com.xpayworld.sdk.payment.data.Card
import com.xpayworld.sdk.payment.data.Transaction
import com.xpayworld.sdk.payment.data.TransactionRepository
import com.xpayworld.sdk.payment.data.XPayDatabase
import com.xpayworld.sdk.payment.network.API
import com.xpayworld.sdk.payment.network.PosWS
import com.xpayworld.sdk.payment.network.payload.TransactionResponse
import com.xpayworld.sdk.payment.utils.*
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*


enum class Connection {
    SERIAL, BLUETOOTH
}

sealed class ActionType {
    // for transaction
    data class SALE(var sale: Sale) : ActionType()
    data class PRINT(var print: PrintDetails) : ActionType()
    data class REFUND(var txnNumber: String) : ActionType()
    object ACTIVATION : ActionType()
    object PIN : ActionType()
    object BATCH_UPLOAD : ActionType()
}

enum class CardMode(val value: Int) {
    SWIPE(0),
    INSERT(1),
    TAP(2),
    SWIPE_OR_INSERT(3),
    SWIPE_OR_TAP(4),
    SWIPE_OR_INSERT_OR_TAP(5),
    INSERT_OR_TAP(6),
}

class Sale {
    var amount: Int = 0
    var currency: String = ""
    var currencyCode: Int = 0
    var orderId: String = ""
    var connection: Connection? = null
    var cardMode: CardMode? = null
    var isOffline: Boolean = false
    var timeOut: Int? = 60
}

class PrintDetails{
    var numOfReceipt: Int = 1
    var timeOut: Int = 60
    var connection: Connection? = Connection.BLUETOOTH
    var data: ByteArray? = null
}


interface PaymentServiceListener
{
    // XPay
//    fun onBluetoothScanResult(devices: MutableList<BluetoothDevice>?)
//    fun onTransactionComplete()
//    fun onBatchUploadResult(totalTxn: Int?, unsyncTxn: Int?)
//    fun onPrintComplete()
//    fun onError(error: Int?, message: String?)


    // AirFi requests
    fun InitialiseComplete()
    fun OnError(error: Int?, message: String?)
    fun OnBatchUploadResult(totalTxn: Int?, unsyncTxn: Int?)
//    fun OnReadyChange()
//    fun OnPrintReceipt()
//    fun GetStatusComplete()
    fun TransactionComplete()
    fun OnTerminalConnectedChanged(devices: MutableList<BluetoothDevice>?)
    fun PrintComplete()
}



@Suppress("INCOMPATIBLE_ENUM_COMPARISON")
class XPayLink {

    private val CARD_MODE: BBDeviceController.CheckCardMode? = null
    private val DEVICE_NAMES = arrayOf("WP")

    private var mBBDeviceController: BBDeviceController? = null
    private var mDeviceListener: BBPOSDeviceListener? = null
    private var mSelectedDevice: BluetoothDevice? = null

    private var mSale: Sale? = null
    private var mPrintDetails: PrintDetails? = null
    private var mActionType: ActionType? = null
    private var mListener: PaymentServiceListener? = null
    private var mCard = Card()

    private var mTotalTransactions: Int? = 0

    private var mFirmwareVersion: String? = ""
    private var mBatterLevel: String? = ""
    private var mBatterPercentage: String? = ""

    private val mTransactionRepo: TransactionRepository by lazy {
        TransactionRepository.getInstance(
            XPayDatabase.getDatabase()!!.transactionDao()
        )
    }

    init {
        Log.d("XPayLink","init")
        INSTANCE = this
    }

    companion object {
        fun valueOf(value: Int): BBDeviceController.CheckCardMode? =
            BBDeviceController.CheckCardMode.values().find { it.value == value }

        lateinit var CONTEXT: Context
        var INSTANCE: XPayLink = XPayLink()
    }

    fun attach(mContext: Context, listener: PaymentServiceListener) {
        CONTEXT = mContext
        mListener = listener
        mDeviceListener = BBPOSDeviceListener()
        mBBDeviceController = BBDeviceController.getInstance(mContext, mDeviceListener)
        BBDeviceController.setDebugLogEnabled(true)

        mListener?.InitialiseComplete()

        mBBDeviceController?.startBTScan(DEVICE_NAMES,60)
    }

    /**
     * Start Device
     *
     * @param type             Integer   eg. 1,0023.40 = 1002340
     * @param currency       String   eg. PHP = Philippine Peso
     * @param currencyCode     Integer  eg. 608 = PHP
     *                           reference https://www.iban.com/currency-codes
     * @param connection         Connection Object
     *                          `BLUETOOTH` and `SERIAL`
     * @param cardMode           BBDeviceController.CheckCardMode
     */
    fun startAction(type: ActionType) {
        mActionType = type
        when (type)
        {
            is ActionType.SALE -> {
                mSale = type.sale

                if (!isActivated()) {
//                    mListener?.onError(
//                        XPayError.ACTIVATION_FAILED.value,
//                        XPayError.ACTIVATION_FAILED.name
//                    )
                    mListener?.OnError(
                        XPayError.ACTIVATION_FAILED.value,
                        XPayError.ACTIVATION_FAILED.name
                    )
                    return@startAction
                }

                if (!hasEnteredPin()) {
//                    mListener?.onError(
//                        XPayError.ENTER_PIN_FAILED.value,
//                        XPayError.ENTER_PIN_FAILED.name
//                    )
                    mListener?.OnError(
                        XPayError.ENTER_PIN_FAILED.value,
                        XPayError.ENTER_PIN_FAILED.name
                    )
                    return@startAction
                }

                when (mSale?.connection)
                {
                    Connection.SERIAL -> {
//                        mBBDeviceController?.startSerial()
                    }
                    Connection.BLUETOOTH -> {
                        ProgressDialog.INSTANCE.attach(CONTEXT)
                        if (mBBDeviceController?.connectionMode == BBDeviceController.ConnectionMode.BLUETOOTH) {
                            startEMV()
                            return
                        }
                        mBBDeviceController?.startBTScan(DEVICE_NAMES, mSale?.timeOut!!)
                    }
                }
            }
            is ActionType.PRINT -> {
                mPrintDetails = type.print

                when (mPrintDetails?.connection)
                {
                    Connection.SERIAL -> {
                        // mBBDeviceController?.startSerial()
                    }
                    Connection.BLUETOOTH -> {
                        if (mBBDeviceController?.connectionMode == BBDeviceController.ConnectionMode.BLUETOOTH) {

                            startPrinter()
                            return
                        }
                        mBBDeviceController?.startBTScan(DEVICE_NAMES, mPrintDetails?.timeOut!!)
                    }
                }
            }

            is ActionType.REFUND -> {
               mTransactionRepo.deleteTransaction(type.txnNumber)
            }

            is ActionType.ACTIVATION -> {
                showActivation()
            }

            is ActionType.PIN -> {
                showEnterPin()
            }

            is ActionType.BATCH_UPLOAD -> {
                processBatchUpload()
            }
        }
    }


    fun setBTConnection(device: BluetoothDevice)
    {
        Log.w("XPayLink","setBTConnection")
        mSelectedDevice = device
        mBBDeviceController?.connectBT(device)
    }


    fun getTransactions(): List<Transaction> {
        return TransactionRepository.getInstance(
            XPayDatabase.getDatabase()!!.transactionDao()
        ).getTransaction()
    }

    fun getFirmwareVersion(): String{
        return mFirmwareVersion!!
    }

    fun getBatteryPercentage(): String{
        return mBatterPercentage!!
    }

    fun getBatterLevel(): String{
        return  mBatterLevel!!
    }

    private fun processBatchUpload()
    {
        Log.d("XPayLink","processBatchUpload")
        if (!isNetworkAvailable()) {
//            mListener?.onError(
//                XPayError.NETWORK_FAILED.value,
//                XPayError.NETWORK_FAILED.name
//            )
            mListener?.OnError(
                XPayError.NETWORK_FAILED.value,
                XPayError.NETWORK_FAILED.name
            )
            return@processBatchUpload
        }

        // Refresh Session
        val pin = SharedPref.INSTANCE.readMessage(PosWS.PREF_PIN)
        API.INSTANCE.callLogin(pin) {
            uploadTransaction()
        }
    }

    private fun startPrinter() {
        mBBDeviceController?.startPrint(mPrintDetails!!.numOfReceipt,mPrintDetails!!.timeOut)
        mBBDeviceController?.sendPrintData(mPrintDetails?.data)
    }

    private fun uploadTransaction()
    {
        Log.d("XPayLink","uploadTransaction")
        ProgressDialog.INSTANCE.attach(CONTEXT)
        ProgressDialog.INSTANCE.message("Transaction Uploading...")
        ProgressDialog.INSTANCE.show()

        val txnArr = mTransactionRepo.getTransaction()
        mTotalTransactions = txnArr.count()
        val dispatch = DispatchGroup()

        try {
            txnArr.forEach { txn ->
                if (!txn.isSync) {

                    dispatch.enter()
                    // to update the sync status of transaction
                    mTransactionRepo.updateTransaction("", true, txn.orderId)
                    API.INSTANCE.callTransaction(txn) { response, purchase ->
                        when (response) {
                            is TransactionResponse -> {
                                val result = response.result
                                if (result?.errNumber != 0.0) {
                                    dispatch.leave()
                                    mTransactionRepo.updateTransaction(
                                        result?.message!!,
                                        false,
                                        purchase.orderId
                                    )
                                    return@callTransaction
                                }
                                mTransactionRepo.deleteTransaction(purchase.orderId)
                            }
                            is Throwable -> {
                                mTransactionRepo.updateTransaction(
                                    response.message!!,
                                    false,
                                    purchase.orderId
                                )
                            }
                        }
                        dispatch.leave()
                    }
                }
            }

            dispatch.notify {
                ProgressDialog.INSTANCE.dismiss()
//                mListener?.onBatchUploadResult(mTotalTransactions, getTransactions().count())
                mListener?.OnBatchUploadResult(mTotalTransactions, getTransactions().count())
            }
        }
        catch (e:io.reactivex.exceptions.CompositeException)
        {
            ProgressDialog.INSTANCE.dismiss()
            mListener?.OnError(
                XPayError.ENTER_PIN_FAILED.value,
                XPayError.ENTER_PIN_FAILED.name
            )
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            CONTEXT.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val activeNetworkInfo = connectivityManager!!.activeNetworkInfo
        return activeNetworkInfo != null
    }

    private fun isActivated(): Boolean {
        return !SharedPref.INSTANCE.isEmpty(PosWS.PREF_ACTIVATION)
    }

    private fun hasEnteredPin(): Boolean {
        return !SharedPref.INSTANCE.isEmpty(PosWS.PREF_PIN)
    }

    private fun showActivation()
    {
        val dialog = PopupDialog()
        dialog.buttonNegative = "Cancel"
        dialog.buttonPositive = "Activate"
        dialog.title = "Enter Activation"
        dialog.hasEditText = true
        dialog.show(callback = { buttonId ->
            if (buttonId == 1) {
                API.INSTANCE.attach(mListener!!)
                API.INSTANCE.callActivation(dialog.text!!, callback = {
                    showEnterPin()
                })
            }
        })
    }

    private fun showEnterPin() {
        val dialog = PopupDialog()
        dialog.buttonNegative = "Cancel"
        dialog.buttonPositive = "Ok"
        dialog.title = "Enter Pin code"
        dialog.hasEditText = true
        dialog.show(callback = { buttonId ->
            if (buttonId == 1) {
                API.INSTANCE.callLogin(dialog.text!!)
            }
        })
    }

    private fun shouldCheckCardExpiry() : Int
    {
        // Check if the card is already expired
        val cardExpiry = mCard.expiryDate
        val cardYear = "20${cardExpiry.substring(0..2)}".toInt()
        val cardMonth = cardExpiry.substring(2..4).toInt()

        val calendar: Calendar = Calendar.getInstance()
        val year: Int = calendar.get(Calendar.YEAR)
        val month: Int = calendar.get(Calendar.MONTH)

        if ((cardYear <= year) && cardMonth < month) {
//            mListener?.onError(
//                XPayError.CARD_EXPIRED.value,
//                XPayError.CARD_EXPIRED.name
//            )
            mListener?.OnError(
                XPayError.CARD_EXPIRED.value,
                XPayError.CARD_EXPIRED.name
            )
            return XPayError.CARD_EXPIRED.value
        }
        return 0
    }

    private fun insertTransaction()
    {
//        var trans = Transaction()
//        trans.amount = mSale!!.amount.div(100.0)
//        trans.currency = mSale!!.currency
//        trans.orderId = mSale!!.orderId
//        trans.isOffline = mSale!!.isOffline
//        trans.card = mCard
//        trans.timestamp = System.currentTimeMillis()

    }

    @SuppressLint("SimpleDateFormat")
    private fun startEMV()
    {
        Log.w("XPayLink","private startEMV")
        mBBDeviceController?.getDeviceInfo()
        ProgressDialog.INSTANCE.message("PROCESS TRANSACTION")
        val data: Hashtable<String, Any> = Hashtable() //define empty hashmap
        data["emvOption"] = BBDeviceController.EmvOption.START
        // data["orderID"] = "${mSale?.orderId}"

        data["orderID"] = "0123456789ABCDEF0123456789ABCD"
        data["randomNumber"] = "012345"
        data["checkCardMode"] = valueOf(value = mSale?.cardMode!!.ordinal)
        // Terminal Time
        val terminalTime = SimpleDateFormat("yyMMddHHmmss").format(Calendar.getInstance().time)
        data["terminalTime"] = terminalTime
        mBBDeviceController?.startEmv(data)
    }

    private fun setAmount()
    {
        Log.w("XPayLink","setAmount")
        ProgressDialog.INSTANCE.show()
        ProgressDialog.INSTANCE.message("CONFIRM AMOUNT")

        val input: Hashtable<String, Any> = Hashtable() //define empty hashmap
        // Currency Sign
        val currencyCharacter = arrayOf(
            CurrencyCharacter.P,
            CurrencyCharacter.H,
            CurrencyCharacter.P
        )

        println(currencyCharacter[0].value)

        // Configure Amount
        input.put("amount", "${ mSale!!.amount.div(100.0)}")
        input.put("transactionType", BBDeviceController.TransactionType.GOODS)
        input.put("currencyCode", "${mSale?.currencyCode}")

        if (mSale?.connection == Connection.BLUETOOTH) {
            input.put("currencyCharacters", currencyCharacter)
        }

        mBBDeviceController?.setAmount(input)
    }

//================================================================================================//
//============================================ AirFi =============================================//
//================================================================================================//
    /**
     * This is to ensure no residual data from a previous event
     */
    fun ResetProperties()
    {
        mSale = Sale()
        mSale!!.connection = Connection.BLUETOOTH
        mSale!!.isOffline = true
    }

    /**
     * @param transactionType [String] "R" for Refund, "P" for Purchase
     */
    fun setTxnType(transactionType:String)
    {
        // TODO:
    }

    fun setMerchantName(merchantName:String)
    {
        // TODO:
    }

    /**
     * @param orderID [String]
     */
    fun setOrderID(orderID:String)
    {
        mSale!!.orderId = orderID
    }

    /**
     * @param timeOut [Int]
     */
    fun setTimeout(timeOut:Int)
    {
        mSale?.timeOut = timeOut
    }

    // TODO: set to decimal
    fun setAmountPurchase(transactionAmount:Int)
    {
        mSale?.amount = transactionAmount
    }

    /**
     * can be obtained from https://www.currency-iso.org/en/home/tables/table-a1.html
     * as instructed from the bbpos API
     * @param currencyCode [Int]
     */
    fun setCurrencyCode(currencyCode:Int)
    {
        mSale?.currencyCode = currencyCode
    }

    /**
     * can be obtained from https://en.wikipedia.org/wiki/ISO_4217
     * @param currencyName [String]
     */
    fun setCurrency(currencyName:String)
    {
        mSale?.currency = currencyName
    }

    /**
     * @param cardCaptureMode [com.xpayworld.sdk.payment.CardMode]
     */
    fun setCardCaptureMethod(cardCaptureMode:CardMode)
    {
        mSale?.cardMode = cardCaptureMode
    }

    // TODO: what is this for?
    fun setBinaryFormat()
    {

    }

    // TODO:
    fun setStaffId(flightDetails:String)
    {

    }

    /**
     * maybe to initiate the call on the device
     */
    fun Transaction()
    {
        startAction(ActionType.SALE(mSale!!))
    }

    fun InitialiseOneTimeActivationCode()
    {
        startAction(ActionType.ACTIVATION)
    }

    fun ShowPinEntry()
    {
        startAction(ActionType.PIN)
    }

// For Uploading
    fun UploadTransaction()
    {
        startAction(ActionType.PIN)
    }

// For Printing
    fun setFontSize(fontSize:Int) {}
    fun setLineSpacing(fontSize:Int) {}
    fun setReceipt(printData:String){}

    private fun genReceipt(): ByteArray?
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
            baos.write (INIT)
            baos.write( POWER_ON)

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
    fun PrintBegin()
    {
        val print = PrintDetails()
        print.data = genReceipt()
        print.numOfReceipt = 2
        startAction(ActionType.PRINT(print))
    }
    fun PrintText() {} // TODO: what is this for?
    fun PrintEnd() {}
//================================================================================================//
//============================================ AirFi =============================================//
//================================================================================================//

    private inner class BBPOSDeviceListener : BBDeviceController.BBDeviceControllerListener
    {
        override fun onReturnUpdateAIDResult(p0: Hashtable<String, BBDeviceController.TerminalSettingStatus>?) {

        }

        override fun onReturnAccountSelectionResult(
            p0: BBDeviceController.AccountSelectionResult?,
            p1: Int
        ) {

        }

        override fun onReturnEmvCardNumber(p0: Boolean, p1: String?) {

        }

        override fun onDeviceDisplayingPrompt() {

        }

        override fun onRequestSelectApplication(p0: ArrayList<String>?) {
            mBBDeviceController?.selectApplication(0)
        }

        override fun onRequestDisplayText(p0: BBDeviceController.DisplayText?) {
            ProgressDialog.INSTANCE.message(p0.toString())
        }

        override fun onReturnPrintResult(p0: BBDeviceController.PrintResult?) {

        }

        override fun onReturnDisableAccountSelectionResult(p0: Boolean) {

        }

        override fun onReturnAmount(p0: Hashtable<String, String>?) {

        }

        override fun onBTConnected(p0: BluetoothDevice?)
        {
            Log.w("XPayLink","onBTConnected")
//            when (mActionType)
//            {
//                is ActionType.SALE -> {
//                    startEMV()
//                }
//                is ActionType.PRINT -> {
//                    startPrinter()
//                }
//            }
        }

        override fun onReturnApduResult(p0: Boolean, p1: Hashtable<String, Any>?) {

        }

        override fun onReturnCAPKDetail(p0: CAPK?) {

        }

        override fun onReturnReversalData(p0: String?) {

        }

        override fun onReturnReadAIDResult(p0: Hashtable<String, Any>?) {

        }

        override fun onRequestOnlineProcess(tlv: String?)
        {
            val decodeData = BBDeviceController.decodeTlv(tlv)
                mCard.emvICCData = decodeData["C2"].toString()
                mCard.expiryDate = decodeData["5F24"].toString()
                mCard.ksn = decodeData["C0"].toString()
                mCard.cardNumber = decodeData["5A"].toString()
                mCard.cardXNumber = decodeData["C4"].toString()

            var trans = com.xpayworld.sdk.payment.data.Transaction()
                trans.amount = mSale!!.amount.div(100.0)
                trans.currency = mSale!!.currency
                trans.orderId = mSale!!.orderId
                trans.isOffline = mSale!!.isOffline
                trans.card = mCard
                trans.timestamp = System.currentTimeMillis()

            if (shouldCheckCardExpiry() != 0) {
                mBBDeviceController?.sendOnlineProcessResult("8A023035")
                return
            }

            if (mSale!!.isOffline) {
                // if transaction is offline
                mTransactionRepo.createTransaction(trans)
            } else {
                // otherwise online
                ProgressDialog.INSTANCE.attach(CONTEXT)
                ProgressDialog.INSTANCE.message("Loading")
                API.INSTANCE.callTransaction(trans) { _, _ ->
                    ProgressDialog.INSTANCE.dismiss()
                }
            }

            mBBDeviceController?.sendOnlineProcessResult("8A023030")
            //8A023030
        }

        override fun onReturnNfcDataExchangeResult(p0: Boolean, p1: Hashtable<String, String>?) {

        }

        override fun onBTScanStopped() {

        }

        override fun onReturnPowerOnIccResult(p0: Boolean, p1: String?, p2: String?, p3: Int) {

        }

        override fun onReturnEmvReport(p0: String?) {

        }

        override fun onReturnPhoneNumber(p0: BBDeviceController.PhoneEntryResult?, p1: String?) {

        }

        override fun onReturnCAPKLocation(p0: String?) {

        }

        override fun onReturnEncryptPinResult(p0: Boolean, p1: Hashtable<String, String>?) {

        }

        override fun onAudioDeviceUnplugged() {

        }

        override fun onSessionError(p0: BBDeviceController.SessionError?, p1: String?) {

        }

        override fun onAudioAutoConfigCompleted(p0: Boolean, p1: String?) {

        }

        override fun onReturnEmvCardDataResult(p0: Boolean, p1: String?) {

        }

        override fun onReturnReadGprsSettingsResult(p0: Boolean, p1: Hashtable<String, Any>?) {

        }

        override fun onRequestClearDisplay() {

        }

        override fun onRequestTerminalTime() {

        }

        override fun onAudioAutoConfigError(p0: BBDeviceController.AudioAutoConfigError?) {

        }

        override fun onReturnTransactionResult(result: BBDeviceController.TransactionResult?)
        {
            Log.w("XPayLink","onReturnTransactionResult, result:"+result.toString() )
            ProgressDialog.INSTANCE.dismiss()
            if (result == BBDeviceController.TransactionResult.APPROVED){
//                mListener?.onTransactionComplete()
                mListener?.TransactionComplete()
                return
            }
//            mListener?.onError(result?.ordinal, result?.name)
            mListener?.OnError(result?.ordinal, result?.name)
        }

        override fun onReturnReadTerminalSettingResult(p0: Hashtable<String, Any>?) {

        }

        override fun onReturnVasResult(
            p0: BBDeviceController.VASResult?,
            p1: Hashtable<String, Any>?
        ) {

        }

        override fun onReturnPinEntryResult(
            p0: BBDeviceController.PinEntryResult?,
            p1: Hashtable<String, String>?
        ) {

        }

        override fun onReturnControlLEDResult(p0: Boolean, p1: String?) {

        }

        override fun onReturnRemoveCAPKResult(p0: Boolean) {

        }

        override fun onBTReturnScanResults(devices: MutableList<BluetoothDevice>?) {
//            mListener?.onBluetoothScanResult(devices)
//            mListener?.onBluetoothScanResult(devices)
            mListener?.OnTerminalConnectedChanged(devices)
            setBTConnection(device = devices!![0])
        }

        override fun onEnterStandbyMode() {

        }

        override fun onPrintDataEnd() {
//            mListener?.onPrintComplete()
            mListener?.PrintComplete()
        }

        override fun onReturnDisableInputAmountResult(p0: Boolean) {

        }

        override fun onSerialDisconnected() {

        }

        override fun onReturnSetPinPadButtonsResult(p0: Boolean) {

        }

        override fun onBTRequestPairing() {

        }

        override fun onDeviceHere(p0: Boolean) {

        }

        override fun onReturnEmvReportList(p0: Hashtable<String, String>?) {

        }

        override fun onReturnEnableInputAmountResult(p0: Boolean) {

        }

        override fun onAudioAutoConfigProgressUpdate(p0: Double) {

        }

        override fun onPowerButtonPressed() {

        }

        override fun onPrintDataCancelled() {
//            mListener?.onError(XPayError.PRINT_CANCELLED.value,
//                XPayError.PRINT_CANCELLED.name)
            mListener?.OnError(XPayError.PRINT_CANCELLED.value,
                XPayError.PRINT_CANCELLED.name)
        }

        override fun onReturnNfcDetectCardResult(
            p0: BBDeviceController.NfcDetectCardResult?,
            p1: Hashtable<String, Any>?
        ) {

        }

        override fun onPowerDown() {

        }

        override fun onReturnUpdateTerminalSettingResult(p0: BBDeviceController.TerminalSettingStatus?) {

        }

        override fun onAudioDevicePlugged() {

        }

        override fun onRequestKeypadResponse() {

        }

        override fun onNoAudioDeviceDetected() {

        }

        override fun onRequestSetAmount() {
            Log.d("XPayLink", "onRequestSetAmount")
            setAmount()
        }

        override fun onRequestDisplayLEDIndicator(p0: BBDeviceController.ContactlessStatus?) {

        }

        override fun onReturnFunctionKey(p0: BBDeviceController.FunctionKey?) {

        }

        override fun onReturnCAPKList(p0: MutableList<CAPK>?) {

        }

        override fun onRequestStartEmv() {

        }

        override fun onUsbDisconnected() {

        }

        override fun onReturnUpdateCAPKResult(p0: Boolean) {

        }

        override fun onRequestFinalConfirm() {
            mBBDeviceController?.sendFinalConfirmResult(true)
        }

        override fun onReturnBarcode(p0: String?) {

        }

        override fun onReturnUpdateGprsSettingsResult(
            p0: Boolean,
            p1: Hashtable<String, BBDeviceController.TerminalSettingStatus>?
        ) {

        }

        override fun onRequestPrintData(index: Int, isPrint: Boolean) {
           mBBDeviceController?.sendPrintData(mPrintDetails?.data)
        }

        override fun onSerialConnected() {
            Log.w("XPayLink","onSerialConnected")
            startEMV()
        }

        override fun onReturnBatchData(tlv: String?)
        {
            Log.w("XpayLink","onReturnBatchData(), tlv:"+tlv.toString() )
            val decodeData = BBDeviceController.decodeTlv(tlv)
            Log.w("XpayLink","onReturnBatchData(), decodeData:"+decodeData.toString() )
//                mCard.emvICCData = decodeData["C2"].toString()
//                mCard.expiryDate = decodeData["5F24"].toString()
//                mCard.ksn = decodeData["C0"].toString()
//                mCard.cardNumber = decodeData["5A"].toString()
//                mCard.cardXNumber = decodeData["C4"].toString()
//
//                var trans = Transaction()
//                trans.amount = mSale!!.amount.div(100.0)
//                trans.currency = mSale!!.currency
//                trans.orderId = mSale!!.orderId
//                trans.isOffline = mSale!!.isOffline
//                trans.card = mCard
//                trans.timestamp = System.currentTimeMillis()
        }

        override fun onReturnEncryptDataResult(p0: Boolean, p1: Hashtable<String, String>?) {

        }

        override fun onBarcodeReaderConnected() {

        }

        override fun onRequestDisplayAsterisk(p0: Int) {

        }

        override fun onReturnDeviceInfo(deviceInfoData: Hashtable<String, String>?) {

            mFirmwareVersion = deviceInfoData?.get("firmwareVersion").toString()
            mBatterLevel = deviceInfoData?.get("batteryLevel").toString()
            mBatterPercentage = deviceInfoData?.get("batteryPercentage").toString()
            val hardwareVersion: String = deviceInfoData?.get("hardwareVersion").toString()

            val serialNumber: String = deviceInfoData?.get("serialNumber").toString()
            val modelName: String = deviceInfoData?.get("modelName").toString()

        }

        override fun onReturnCancelCheckCardResult(isCancel: Boolean) {
            if (isCancel) {
                ProgressDialog.INSTANCE.dismiss()
//                mListener?.onError(
//                    XPayError.TXN_CANCELLED.value,
//                    XPayError.TXN_CANCELLED.name
//                )
                mListener?.OnError(
                    XPayError.TXN_CANCELLED.value,
                    XPayError.TXN_CANCELLED.name
                )
            }
        }

        override fun onBatteryLow(status: BBDeviceController.BatteryStatus?) {
//            mListener?.onError(status?.ordinal, status?.name)
            mListener?.OnError(status?.ordinal, status?.name)
        }

        override fun onBTScanTimeout() {

        }

        override fun onRequestProduceAudioTone(p0: BBDeviceController.ContactlessStatusTone?) {

        }

        override fun onBTDisconnected() {

        }

        override fun onWaitingReprintOrPrintNext() {

        }

        override fun onSessionInitialized() {

        }

        override fun onReturnEnableAccountSelectionResult(p0: Boolean) {

        }

        override fun onWaitingForCard(p0: BBDeviceController.CheckCardMode?) {
            var resp = ""
            when (mSale?.cardMode?.value) {
                BBDeviceController.CheckCardMode.INSERT.value -> {
                    resp = "PLEASE INSERT CARD"
                }
                BBDeviceController.CheckCardMode.SWIPE.value -> {
                    resp = "PLEASE SWIPE CARD"
                }
                BBDeviceController.CheckCardMode.SWIPE_OR_INSERT.value -> {
                    resp = "PLEASE SWIPE/INSERT CARD"
                }
                BBDeviceController.CheckCardMode.SWIPE_OR_TAP.value -> {
                    resp = "PLEASE SWIPE/TAP CARD"
                }
                BBDeviceController.CheckCardMode.INSERT_OR_TAP.value -> {
                    resp = "PLEASE inert/TAP CARD"
                }
                BBDeviceController.CheckCardMode.SWIPE_OR_INSERT_OR_TAP.value -> {
                    resp = "PLEASE SWIPE/INSERT/TAP CARD"
                }
            }
            ProgressDialog.INSTANCE.message(resp)
        }

        override fun onReturnCheckCardResult(
            checkCardResult: BBDeviceController.CheckCardResult?,
            decodeData: Hashtable<String, String>
        ) {

            if (checkCardResult == BBDeviceController.CheckCardResult.MSR) {

                var expiryDate = decodeData["expiryDate"].toString()

                mCard.ksn = decodeData["ksn"].toString()
                mCard.cardNumber = decodeData["pan"].toString()
                mCard.cardXNumber = decodeData["maskedPAN"].toString()
                mCard.expiryDate = expiryDate
                mCard.expiryYear = expiryDate.substring(0..2)
                mCard.expiryMonth = expiryDate.substring(2..4)
                mCard.encTrack2 = decodeData["encTrack2"].toString()
                mCard.serviceCode = decodeData["serviceCode"].toString()
                mCard.posEntry = decodeData["posEntryMode"]!!.toInt()


            } else if (checkCardResult == BBDeviceController.CheckCardResult.INSERTED_CARD) {

            } else if (checkCardResult == BBDeviceController.CheckCardResult.TAP_CARD_DETECTED) {

            }

        }

        override fun onRequestPinEntry(p0: BBDeviceController.PinEntrySource?) {
        }

        override fun onDeviceReset() {

        }

        override fun onReturnReadWiFiSettingsResult(p0: Boolean, p1: Hashtable<String, Any>?) {

        }

        override fun onReturnDisplayPromptResult(p0: BBDeviceController.DisplayPromptResult?) {

        }

        override fun onBarcodeReaderDisconnected() {

        }

        override fun onUsbConnected() {

        }

        override fun onReturnUpdateWiFiSettingsResult(
            p0: Boolean,
            p1: Hashtable<String, BBDeviceController.TerminalSettingStatus>?
        ) {

        }

        override fun onError(error: BBDeviceController.Error?, p1: String?) {
            ProgressDialog.INSTANCE.dismiss()
//            mListener?.onError(error?.ordinal, error?.name)
            mListener?.OnError(error?.ordinal, error?.name)
        }

        override fun onReturnAmountConfirmResult(p0: Boolean) {

        }

        override fun onReturnInjectSessionKeyResult(p0: Boolean, p1: Hashtable<String, String>?) {

        }

        override fun onReturnPowerOffIccResult(p0: Boolean) {

        }

    }
}


