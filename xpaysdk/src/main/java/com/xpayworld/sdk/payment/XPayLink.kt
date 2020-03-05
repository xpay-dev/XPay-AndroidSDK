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
import kotlin.collections.ArrayList


// TODO: process for translation
const val RECEIPT_TABLET_ID = "Tablet : "
const val RECEIPT_TICKET_NO = "Ticket Number : "
const val RECEIPT_FLIGHT_DATE = "Flight Date : "
const val RECEIPT_FLIGHT_NO = "Flight Number : "
const val RECEIPT_FLIGHT_ROUTE = "Flight Route : "
const val RECEIPT_CREW_NAME = "Crew : "
const val RECEIPT_ORDER_NO = "Order No : "
const val RECEIPT_TOTAL = "TOTAL : "
const val RECEIPT_CHARGED = "CHARGED : "
const val RECEIPT_ESC_STR_NEW_LINE_DELIMITER = "\n"

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

enum class Currency(val value: String) {
    NONE("SPACE"),
    DIRHAM("DIRHAM"),
    DOLLAR("DOLLAR"),
    EURO("EURO"),
    NEW_SHEKEL("NEW_SHEKEL"),
    POUND("POUND"),
    RIYAL("RIYAL"),
    RIYAL_2("RIYAL_2"),
    RUPEE("RUPEE"),
    WON("WON"),
    YEN("YEN"),
    YUAN("YUAN")
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
    var amount: Double = 0.0
    var currency: String = ""
    var currencyCode: Int = 0
    var orderId: String = ""
    var connection: Connection? = null
    var cardMode: CardMode? = null
    var isOffline: Boolean = false
    var timeOut: Int? = 60
}

/**
 * for order items that don't have a description
 * example:
 * 1 Tiger Beer
 */
class ReceiptOrderItemSingle {
    var item:String = ""
    var price:Double=0.0
}

/**
 * for order items with description of items included
 * example:
 * 1 HOT MEAL COMBO
 * - Coca Cola Regular
 * - Oriental Treasure Rice
 */
class ReceiptOrderItemCombo {
    var item:String = ""
    var price:Double = 0.0
    var description:String = "" // can be '\n' delimited
}

class ReceiptDetails {
    var header : String = ""
    var description : String = ""
    var tabletId : String = ""
    var ticketNo : Int = 0
    var flightDate : String = "" // format: YYYY-MM-dd
    var flightNo : String = ""
    var flightRoute : String = ""
    var crewName : String = ""
    var orderNo : String = ""
    var singleItems:ArrayList<ReceiptOrderItemSingle> = ArrayList<ReceiptOrderItemSingle>()
    var comboItems:ArrayList<ReceiptOrderItemCombo>
            = ArrayList<ReceiptOrderItemCombo>()
    var total: Double = 0.0
}

class PrintOptions {
    var fontSize: Int = 1
    var characterSpacing: Int = 0
    var lineSpacing: Int = 0
}

class PrintDetails
{
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


//#region Callbacks
    // AirFi requests
    fun InitialiseComplete()
    fun OnError(error: Int?, message: String?)
    fun OnBatchUploadResult(totalTxn: Int?, unsyncTxn: Int?)
//    fun OnReadyChange()
//    fun OnPrintReceipt()
//    fun GetStatusComplete()
    fun TransactionComplete()
//    fun OnTerminalConnectedChanged(devices: MutableList<BluetoothDevice>?)
    fun OnTerminalConnectedChanged(device: BluetoothDevice?)
    fun PrintComplete()
//#endregion Callbacks
}

@Suppress("INCOMPATIBLE_ENUM_COMPARISON")
class XPayLink
{
    private val CARD_MODE: BBDeviceController.CheckCardMode? = null
    private val DEVICE_NAMES = arrayOf("WP")

    private var mBBDeviceController: BBDeviceController? = null
    private var mDeviceListener: BBPOSDeviceListener? = null
    private var mSelectedDevice: BluetoothDevice? = null

    private var mSale: Sale? = null
    private var mReceiptDetails: ReceiptDetails? = null
    private var mPrintOptions: PrintOptions? = null
    private var mPrintDetails: PrintDetails? = null
    private var mActionType: ActionType? = null
    private var mListener: PaymentServiceListener? = null
    private var mCard = Card()

    private var mTotalTransactions: Int? = 0

    private var mFirmwareVersion: String? = ""
    private var mBatterLevel: String? = ""
    private var mBatterPercentage: String? = ""

    private var mBaosPrintData:ByteArrayOutputStream? = null

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

    fun attach(mContext: Context, listener: PaymentServiceListener)
    {
        CONTEXT = mContext
        mListener = listener
        mDeviceListener = BBPOSDeviceListener()
        mBBDeviceController = BBDeviceController.getInstance(mContext, mDeviceListener)
        BBDeviceController.setDebugLogEnabled(true)

        mListener?.InitialiseComplete()
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
    fun startAction(type: ActionType)
    {
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
                            return@startAction
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
                            return@startAction
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
        Log.d("XPayLink","processBatchUpload, pin:"+pin)
        API.INSTANCE.callLogin(pin) {
            uploadTransaction()
        }
    }

    private fun startPrinter() {
        mBBDeviceController?.startPrint(mPrintDetails!!.numOfReceipt,mPrintDetails!!.timeOut)
    }

    private fun uploadTransaction()
    {
        Log.d("XPayLink","uploadTransaction")
        ProgressDialog.INSTANCE.attach(CONTEXT)
        ProgressDialog.INSTANCE.message("Transaction Uploading...")
        ProgressDialog.INSTANCE.show()

        val txnArr = mTransactionRepo.getTransaction()
        mTotalTransactions = txnArr.count()
        Log.d("XPayLink","uploadTransaction, mTotalTransactions:"+mTotalTransactions)
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
                                Log.d("XPayLink","uploadTransaction:TransactionResponse")
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
        Log.w("XPayLink","isActivated(), SharedPref.INSTANCE.readMessage(PosWS.PREF_ACTIVATION)):"
            +SharedPref.INSTANCE.readMessage(PosWS.PREF_ACTIVATION))
        return !SharedPref.INSTANCE.isEmpty(PosWS.PREF_ACTIVATION)
    }

    private fun hasEnteredPin(): Boolean {
        Log.w("XPayLink","isActivated(), SharedPref.INSTANCE.readMessage(PosWS.PREF_PIN):"
                +SharedPref.INSTANCE.readMessage(PosWS.PREF_PIN))
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

        // TODO: fix random alphanumeric number causing error in app
//            data["orderID"] = "0123456789ABCDEF0123456789ABCDEF"
            data["orderID"] = mSale?.orderId
            Log.w("XPayLink","private startEMV, data[\"orderID\"]:"+data["orderID"].toString() )

//            data["randomNumber"] = "012345"

            data["checkCardMode"] = valueOf(value = mSale?.cardMode!!.ordinal)
            Log.w("XPayLink","private startEMV, data[\"checkCardMode\"]:"+data["checkCardMode"].toString() )

            data["terminalTime"] = SimpleDateFormat("yyMMddHHmmss").format(Calendar.getInstance().time)
            Log.w("XPayLink","private startEMV, data[\"checkCardMode\"]:"+data["terminalTime"].toString() )

        mBBDeviceController?.startEmv(data)
    }

    fun getCurrencyCharacterByString(char:String?):CurrencyCharacter
    {
        var currChar:CurrencyCharacter=CurrencyCharacter.SPACE

        when(char)
        {
            Currency.DIRHAM.value -> { currChar = CurrencyCharacter.DIRHAM }
            Currency.DOLLAR.value -> { currChar = CurrencyCharacter.DOLLAR }
            Currency.EURO.value -> { currChar = CurrencyCharacter.EURO }
            Currency.NEW_SHEKEL.value -> { currChar = CurrencyCharacter.NEW_SHEKEL }
            Currency.POUND.value -> { currChar = CurrencyCharacter.POUND }
            Currency.RIYAL.value -> { currChar = CurrencyCharacter.RIYAL }
            Currency.RIYAL_2.value -> { currChar = CurrencyCharacter.RIYAL_2 }
            Currency.RUPEE.value -> { currChar = CurrencyCharacter.RUPEE }
            Currency.WON.value -> { currChar = CurrencyCharacter.WON }
            Currency.YEN.value -> { currChar = CurrencyCharacter.YEN }
            Currency.YUAN.value -> { currChar = CurrencyCharacter.YUAN }
            else -> { currChar = CurrencyCharacter.SPACE }
        }


        return currChar
    }

    fun getCurrencyCharacterByLetter(char:String?):CurrencyCharacter
    {
        var currChar:CurrencyCharacter=CurrencyCharacter.SPACE

        when(char?.toUpperCase())
        {
            "A"->{ currChar = CurrencyCharacter.A }
            "B"->{ currChar = CurrencyCharacter.B }
            "C"->{ currChar = CurrencyCharacter.C }
            "D"->{ currChar = CurrencyCharacter.D }
            "E"->{ currChar = CurrencyCharacter.E }
            "F"->{ currChar = CurrencyCharacter.F }
            "G"->{ currChar = CurrencyCharacter.G }
            "H"->{ currChar = CurrencyCharacter.H }
            "I"->{ currChar = CurrencyCharacter.I }
            "J"->{ currChar = CurrencyCharacter.J }
            "K"->{ currChar = CurrencyCharacter.K }
            "L"->{ currChar = CurrencyCharacter.L }
            "M"->{ currChar = CurrencyCharacter.M }
            "N"->{ currChar = CurrencyCharacter.N }
            "O"->{ currChar = CurrencyCharacter.O }
            "P"->{ currChar = CurrencyCharacter.P }
            "Q"->{ currChar = CurrencyCharacter.Q }
            "R"->{ currChar = CurrencyCharacter.R }
            "S"->{ currChar = CurrencyCharacter.S }
            "T"->{ currChar = CurrencyCharacter.T }
            "U"->{ currChar = CurrencyCharacter.U }
            "V"->{ currChar = CurrencyCharacter.V }
            "W"->{ currChar = CurrencyCharacter.W }
            "X"->{ currChar = CurrencyCharacter.X }
            "Y"->{ currChar = CurrencyCharacter.Y }
            "Z"->{ currChar = CurrencyCharacter.Z }
        }


        return currChar
    }

    private fun setAmount()
    {
        Log.w("XPayLink","setAmount")
        Log.w("XPayLink","setAmount, mSale!!.amount:"
                +mSale!!.amount.toString() )

        Log.w("XPayLink","setAmount, mSale?.currency:"
                + mSale?.currency.toString() )

        ProgressDialog.INSTANCE.show()
        ProgressDialog.INSTANCE.message("CONFIRM AMOUNT")

        val input: Hashtable<String, Any> = Hashtable() //define empty hashmap


        // Currency Sign
//        val currencyCharacter = arrayOf(
//            CurrencyCharacter.P,
//            CurrencyCharacter.H,
//            CurrencyCharacter.P
//        )
//        println(currencyCharacter[0])


        // Configure Amount
//        input.put("amount", "${ mSale!!.amount.div(100.0)}")
        input.put("amount", "${ mSale?.amount}")
        input.put("transactionType", BBDeviceController.TransactionType.GOODS)
        input.put("currencyCode", "${mSale?.currencyCode}")

        if (mSale?.connection == Connection.BLUETOOTH)
        {
//            input.put("currencyCharacters", currencyCharacter)
            if(getCurrencyCharacterByString(mSale?.currency)==CurrencyCharacter.SPACE)
            {
                val currencyCode = mSale?.currency?.split("")
                Log.w("XPayLink","setAmount, currencyCode:"
                        +currencyCode.toString() )
                val currencyCharacter = arrayOf(
                    getCurrencyCharacterByLetter(currencyCode?.get(1)),
                    getCurrencyCharacterByLetter(currencyCode?.get(2)),
                    getCurrencyCharacterByLetter(currencyCode?.get(3))
                )
                input.put("currencyCharacters", currencyCharacter)
            }
            else
            {
                val currencyCharacter = arrayOf(
                    getCurrencyCharacterByString(mSale?.currency)
                )
                input.put("currencyCharacters", currencyCharacter )
            }
        }

        mBBDeviceController?.setAmount(input)
    }

    private fun IntToLeadingZeroByte(value:Int):String
    {
        return String.format("0x%1$02X",value)
    }
//#region Public calls
//================================================================================================//
//============================================ AirFi =============================================//
//================================================================================================//
    /**
     * will connect to the first paired device available
     */
    fun Connect()
    {
        IntToLeadingZeroByte(255)
//        Log.w("XPayLink","Connect(), str:"+str)
        mBBDeviceController?.startBTScan(DEVICE_NAMES,60)
    }

    /**
     * disconnects a connected device
     */
    fun Disconnect()
    {
        mBBDeviceController?.disconnectBT()
    }

    /**
     * This is to ensure no residual data from a previous event
     */
    fun ResetProperties()
    {
        mReceiptDetails = ReceiptDetails()
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
        mSale?.orderId = orderID
        Log.w("XPayLink","setOrderID(), mSale!!.orderId:" + mSale!!.orderId)
    }

    /**
     * @param timeOut [Int]
     */
    fun setTimeout(timeOut:Int)
    {
        mSale?.timeOut = timeOut
    }

    // TODO: set to decimal
    fun setAmountPurchase(transactionAmount:Double)
    {
        mSale?.amount = transactionAmount
        Log.w("XPayLink","setAmountPurchase(), mSale?.amount:"
                + mSale?.amount.toString() )
    }

    /**
     * can be obtained from https://www.currency-iso.org/en/home/tables/table-a1.html
     * as instructed from the bbpos API
     * @param currencyCode [Int]
     */
    fun setCurrencyCode(currencyCode:Int)
    {
        mSale?.currencyCode = currencyCode
        Log.w("XPayLink","setAmountPurchase(), mSale?.currencyCode:"
                + mSale?.currencyCode.toString() )
    }

    /**
     * can be obtained from Currency Enum w/c are provided by the terminal or
     * the 3 character code from http://www.allembassies.com/currency_codes_by_3-letter_code.htm
     * @param currencyName [String]
     */
    fun setCurrency(currencyName:String)
    {
        mSale?.currency = currencyName
        Log.w("XPayLink","setAmountPurchase(), mSale?.currency:"
                + mSale?.currency.toString() )
    }

    /**
     * @param cardCaptureMode [com.xpayworld.sdk.payment.CardMode]
     */
    fun setCardCaptureMethod(cardCaptureMode:CardMode)
    {
        mSale?.cardMode = cardCaptureMode
        Log.w("XPayLink","setAmountPurchase(), mSale?.cardMode:"
                + mSale?.cardMode.toString() )
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
        Log.w("XPayLink","UploadTransaction()")
        startAction(ActionType.BATCH_UPLOAD)
    }

// For Printing
    fun setFontSize(fontSize:Int)
    {
        mPrintOptions?.fontSize = fontSize
    }
    fun setCharacterSpacing(charSpace:Int)
    {
        mPrintOptions?.characterSpacing = charSpace
    }
    fun setLineSpacing(lineSpace:Int)
    {
        mPrintOptions?.lineSpacing = lineSpace
    }

    // disabled for now
//    fun setReceipt(printData:String)
//    {
//        val baos = ByteArrayOutputStream()
//
//        baos.write(INIT)
//        baos.write(POWER_ON)
//
//        baos.write( FONT_SIZE_1 )
//
//        baos.write(byteArrayOf(0x1B, 0x20, mPrintOptions?.characterSpacing!!.toByte() ))//char spacing
//        baos.write(byteArrayOf(0x1B, 0x33, mPrintOptions?.lineSpacing!!.toByte() ))//line spacing
//
//        val _arrNewLineDelimitedData = printData.split("\n")
//        _arrNewLineDelimitedData.forEach { line ->
//            baos.write(NEW_LINE)
//            baos.write(line.toByteArray())
//        }
//
//        baos.write(POWER_OFF)
//
//        mBaosPrintData = baos
//    }

//    private fun genReceipt(): ByteArray?
//    {
//        val lineWidth = 384
////        val size0NoEmphasizeLineWidth = 384 / 8 //line width / font width
//        val size0NoEmphasizeLineWidth = 384 / 9 //line width / font width
//        var singleLine = ""
//        for (i in 0 until size0NoEmphasizeLineWidth) {
//            singleLine += "-"
//        }
//        var doubleLine = ""
//        for (i in 0 until size0NoEmphasizeLineWidth) {
//            doubleLine += "="
//        }
//        try {
//            val baos = ByteArrayOutputStream()
//            baos.write(INIT)
//            baos.write(POWER_ON)
//
//            baos.write(FONT_SIZE_1)
//            baos.write(EMPHASIZE_ON)
//            baos.write(FONT_10X18)
//            baos.write(ALIGN_CENTER)
//            baos.write("SCOOT".toByteArray())
//
//            baos.write(NEW_LINE)
//            baos.write(NEW_LINE)
//            baos.write(NEW_LINE)
//
//            baos.write(FONT_SIZE_0)
//            baos.write(FONT_10X18)
//            baos.write(ALIGN_CENTER)
//            baos.write(EMPHASIZE_ON)
//            baos.write("normal sale".toByteArray())
//
//            baos.write(NEW_LINE)
//            baos.write(ALIGN_CENTER)
//            baos.write(EMPHASIZE_ON)
//            baos.write("passenger copy".toByteArray())
//
//            baos.write(NEW_LINE)
//            baos.write(NEW_LINE)
//
////            baos.write(FONT_SIZE_0)
//            baos.write(ALIGN_LEFT)
//            baos.write(EMPHASIZE_OFF)
////            baos.write(FONT_10X18)
//            baos.write(EMPHASIZE_OFF)
//            baos.write("Tablet:     ".toByteArray())
//            baos.write(EMPHASIZE_OFF)
//            baos.write("1819D67264CC".toByteArray())
//
//            baos.write(NEW_LINE)
//            baos.write(FONT_SIZE_0)
//            baos.write(ALIGN_LEFT)
//            baos.write(EMPHASIZE_OFF)
////            baos.write(FONT_10X18)
//            baos.write(EMPHASIZE_OFF)
//            baos.write("Ticket Number:     ".toByteArray())
//            baos.write(EMPHASIZE_OFF)
//            baos.write("2".toByteArray())
//
//            baos.write(NEW_LINE)
//            baos.write(FONT_SIZE_0)
//            baos.write(ALIGN_LEFT)
//            baos.write(EMPHASIZE_OFF)
////            baos.write(FONT_10X18)
//            baos.write(EMPHASIZE_OFF)
//            baos.write("Flight Date:     ".toByteArray())
//            baos.write(EMPHASIZE_OFF)
//            baos.write("2020-02-12".toByteArray())
//
//            baos.write(NEW_LINE)
//            baos.write(FONT_SIZE_0)
//            baos.write(ALIGN_LEFT)
//            baos.write(EMPHASIZE_OFF)
////            baos.write(FONT_10X18)
//            baos.write(EMPHASIZE_OFF)
//            baos.write("Flight Number:     ".toByteArray())
//            baos.write(EMPHASIZE_OFF)
//            baos.write("TR147".toByteArray())
//
//            baos.write(NEW_LINE)
//            baos.write(FONT_SIZE_0)
//            baos.write(ALIGN_LEFT)
//            baos.write(EMPHASIZE_OFF)
////            baos.write(FONT_10X18)
//            baos.write(EMPHASIZE_OFF)
//            baos.write("Flight Route:     ".toByteArray())
//            baos.write(EMPHASIZE_OFF)
//            baos.write("ANR-BRU".toByteArray())
//
//            baos.write(NEW_LINE)
//            baos.write(FONT_SIZE_0)
//            baos.write(ALIGN_LEFT)
//            baos.write(EMPHASIZE_OFF)
////            baos.write(FONT_10X18)
//            baos.write(EMPHASIZE_OFF)
//            baos.write("Crew:     ".toByteArray())
//            baos.write(EMPHASIZE_OFF)
//            baos.write("AIRFI-TEST".toByteArray())
//
//            baos.write(NEW_LINE)
//            baos.write(FONT_SIZE_0)
//            baos.write(ALIGN_LEFT)
//            baos.write(EMPHASIZE_OFF)
////            baos.write(FONT_10X18)
//            baos.write(EMPHASIZE_OFF)
//            baos.write("Order No:     ".toByteArray())
//            baos.write(EMPHASIZE_OFF)
//            baos.write("8dbac0fc-e0ea-4c67-82a6".toByteArray())
//
//            baos.write(NEW_LINE)
//
//            baos.write(NEW_LINE)
//            baos.write(FONT_SIZE_0)
//            baos.write(ALIGN_LEFT)
//            baos.write(EMPHASIZE_OFF)
////            baos.write(FONT_10X18)
//            baos.write(EMPHASIZE_OFF)
//            baos.write("1 Tiger Beer    ".toByteArray())
//            baos.write(EMPHASIZE_OFF)
//            baos.write("SGD 8.00".toByteArray())
//
//            baos.write(NEW_LINE)
//            baos.write(FONT_SIZE_0)
//            baos.write(ALIGN_LEFT)
//            baos.write(EMPHASIZE_OFF)
////            baos.write(FONT_10X18)
//            baos.write(EMPHASIZE_OFF)
//            baos.write("1 HOT MEAL COMBO    ".toByteArray())
//            baos.write(EMPHASIZE_OFF)
//            baos.write("SGD 15.00".toByteArray())
//
//            baos.write(NEW_LINE)
//            baos.write(EMPHASIZE_OFF)
//            baos.write("     -  Coca Cola Regular".toByteArray())
//
//            baos.write(NEW_LINE)
//            baos.write(EMPHASIZE_OFF)
//            baos.write("     -  Oriental Treasure Rice".toByteArray())
//
//            baos.write(NEW_LINE)
//            baos.write(FONT_8X12)
//            baos.write(singleLine.toByteArray())
//
//            baos.write(NEW_LINE)
//
//            baos.write(NEW_LINE)
//            baos.write(FONT_SIZE_0)
//            baos.write(ALIGN_LEFT)
//            baos.write(EMPHASIZE_OFF)
////            baos.write(FONT_10X18)
//            baos.write(EMPHASIZE_OFF)
//            baos.write("TOTAL:     ".toByteArray())
//            baos.write(EMPHASIZE_OFF)
//            baos.write("SGD  23.00".toByteArray())
//
//            baos.write(NEW_LINE)
//            baos.write(FONT_SIZE_0)
//            baos.write(ALIGN_LEFT)
//            baos.write(EMPHASIZE_OFF)
//            baos.write(FONT_10X18)
//            baos.write(EMPHASIZE_OFF)
//            baos.write("CHARGED:     ".toByteArray())
//            baos.write(EMPHASIZE_OFF)
//            baos.write("SGD  23.00".toByteArray())
//
//        //==========================================================================================
//        // don't write beyond this point
//
//            baos.write(NEW_LINE)
//            baos.write(NEW_LINE)
//            baos.write(NEW_LINE)
//            baos.write(NEW_LINE)
//            baos.write(NEW_LINE)
//            baos.write(NEW_LINE)
//            baos.write(NEW_LINE)
//            baos.write("".toByteArray())
//
//            baos.write(POWER_OFF)
//
//            return baos.toByteArray()
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//        return null
//    }
    private fun genReceipt(): ByteArray?
    {
        val lineWidth = 384
    //        val size0NoEmphasizeLineWidth = 384 / 8 //line width / font width
        val size0NoEmphasizeLineWidth = 384 / 9 //line width / font width
        var singleLine = ""
        for (i in 0 until size0NoEmphasizeLineWidth) {
            singleLine += "-"
        }
        var doubleLine = ""
        for (i in 0 until size0NoEmphasizeLineWidth) {
            doubleLine += "="
        }
        try
        {
            val baos = ByteArrayOutputStream()
            baos.write(INIT)
            baos.write(POWER_ON)

        //==========================================================================================
        // header
            if( !mReceiptDetails!!.header.equals("") )
            {
                baos.write(FONT_SIZE_1)
                baos.write(EMPHASIZE_ON)
                baos.write(FONT_10X18)
                baos.write(ALIGN_CENTER)
                //            baos.write("SCOOT".toByteArray())

                val arrHeaders = mReceiptDetails!!.header.split(RECEIPT_ESC_STR_NEW_LINE_DELIMITER)
                arrHeaders.forEach { header ->
                    baos.write(NEW_LINE)
                    baos.write(header.toByteArray())
                }
            }

        //==========================================================================================
        // description
            if( !mReceiptDetails!!.description.equals("") )
            {
                baos.write(NEW_LINE)

                baos.write(FONT_SIZE_0)
                baos.write(FONT_10X18)
                baos.write(ALIGN_CENTER)
                baos.write(EMPHASIZE_ON)
                //            baos.write("normal sale".toByteArray())

                //            baos.write(NEW_LINE)
                //            baos.write(ALIGN_CENTER)
                //            baos.write(EMPHASIZE_ON)
                //            baos.write("passenger copy".toByteArray())
                val arrDesc = mReceiptDetails!!.description.split(RECEIPT_ESC_STR_NEW_LINE_DELIMITER)
                arrDesc.forEach { desc ->
                    baos.write(NEW_LINE)
                    baos.write(desc.toByteArray())
                }
            }

        //==========================================================================================
        // Tablet ID
            baos.write(FONT_SIZE_0)
            baos.write(FONT_10X18)

            baos.write(NEW_LINE)
            baos.write(NEW_LINE)
            baos.write(NEW_LINE)

            baos.write(ALIGN_LEFT)
            baos.write(EMPHASIZE_OFF)
            baos.write(EMPHASIZE_OFF)
//            baos.write("Tablet :    ".toByteArray())
            baos.write(RECEIPT_TABLET_ID.toByteArray())
            baos.write(EMPHASIZE_OFF)
//            baos.write("1819D67264CC".toByteArray())
            baos.write(mReceiptDetails!!.tabletId.toByteArray())

        //==========================================================================================
        // Ticket Number
            baos.write(NEW_LINE)
            baos.write(FONT_SIZE_0)
            baos.write(ALIGN_LEFT)
            baos.write(EMPHASIZE_OFF)
            baos.write(EMPHASIZE_OFF)
//            baos.write("Ticket Number:     ".toByteArray())
            baos.write(RECEIPT_TICKET_NO.toByteArray())
            baos.write(EMPHASIZE_OFF)
//            baos.write("2".toByteArray())
            baos.write(mReceiptDetails!!.ticketNo.toString().toByteArray())

        //==========================================================================================
        // Flight Date
            baos.write(NEW_LINE)
            baos.write(FONT_SIZE_0)
            baos.write(ALIGN_LEFT)
            baos.write(EMPHASIZE_OFF)
            baos.write(EMPHASIZE_OFF)
//            baos.write("Flight Date:     ".toByteArray())
            baos.write(RECEIPT_FLIGHT_DATE.toByteArray())
            baos.write(EMPHASIZE_OFF)
//            baos.write("2020-02-12".toByteArray())
            baos.write(mReceiptDetails!!.flightDate.toByteArray())

        //==========================================================================================
        // Flight Number
            baos.write(NEW_LINE)
            baos.write(FONT_SIZE_0)
            baos.write(ALIGN_LEFT)
            baos.write(EMPHASIZE_OFF)
            baos.write(EMPHASIZE_OFF)
//            baos.write("Flight Number:     ".toByteArray())
            baos.write(RECEIPT_FLIGHT_NO.toByteArray())
            baos.write(EMPHASIZE_OFF)
//            baos.write("TR147".toByteArray())
            baos.write(mReceiptDetails!!.flightNo.toByteArray())

        //==========================================================================================
        // Flight Route
            baos.write(NEW_LINE)
            baos.write(FONT_SIZE_0)
            baos.write(ALIGN_LEFT)
            baos.write(EMPHASIZE_OFF)
            baos.write(EMPHASIZE_OFF)
//            baos.write("Flight Route:     ".toByteArray())
            baos.write(RECEIPT_FLIGHT_ROUTE.toByteArray())
            baos.write(EMPHASIZE_OFF)
//            baos.write("ANR-BRU".toByteArray())
            baos.write(mReceiptDetails!!.flightRoute.toByteArray())

        //==========================================================================================
        // Crew Name
            baos.write(NEW_LINE)
            baos.write(FONT_SIZE_0)
            baos.write(ALIGN_LEFT)
            baos.write(EMPHASIZE_OFF)
            baos.write(EMPHASIZE_OFF)
//            baos.write("Crew:     ".toByteArray())
            baos.write(RECEIPT_CREW_NAME.toByteArray())
            baos.write(EMPHASIZE_OFF)
//            baos.write("AIRFI-TEST".toByteArray())
            baos.write(mReceiptDetails!!.crewName.toByteArray())

        //==========================================================================================
        // Order Number
            baos.write(NEW_LINE)
            baos.write(FONT_SIZE_0)
            baos.write(ALIGN_LEFT)
            baos.write(EMPHASIZE_OFF)
            baos.write(EMPHASIZE_OFF)
//            baos.write("Order No:     ".toByteArray())
            baos.write(RECEIPT_ORDER_NO.toByteArray())
            baos.write(EMPHASIZE_OFF)
//            baos.write("8dbac0fc-e0ea-4c67-82a6".toByteArray())
            baos.write(mReceiptDetails!!.orderNo.toByteArray())

        //==========================================================================================
        // Order Items
            baos.write(NEW_LINE)

//            baos.write(NEW_LINE)
//            baos.write(FONT_SIZE_0)
//            baos.write(ALIGN_LEFT)
//            baos.write(EMPHASIZE_OFF)
//            baos.write("1 Tiger Beer    ".toByteArray())
//            baos.write(EMPHASIZE_OFF)
//            baos.write("SGD 8.00".toByteArray())
//
//            baos.write(NEW_LINE)
//            baos.write(FONT_SIZE_0)
//            baos.write(ALIGN_LEFT)
//            baos.write(EMPHASIZE_OFF)
//            baos.write(EMPHASIZE_OFF)
//            baos.write("1 HOT MEAL COMBO    ".toByteArray())
//            baos.write(EMPHASIZE_OFF)
//            baos.write("SGD 15.00".toByteArray())
//
//            baos.write(NEW_LINE)
//            baos.write(EMPHASIZE_OFF)
//            baos.write("     -  Coca Cola Regular".toByteArray())
//
//            baos.write(NEW_LINE)
//            baos.write(EMPHASIZE_OFF)
//            baos.write("     -  Oriental Treasure Rice".toByteArray())

            // for single orders
            if( mReceiptDetails!!.singleItems.count()>0 )
            {
                mReceiptDetails!!.singleItems.forEach { singleOrder ->

                    // add the item name to receipt
                    baos.write(NEW_LINE)
                    baos.write(FONT_SIZE_0)
                    baos.write(ALIGN_LEFT)
                    baos.write(EMPHASIZE_OFF)
                    baos.write(singleOrder!!.item.toByteArray())

                    // add the item price to receipt
                    val strPrice = "   " +mSale!!.currency + " " + singleOrder.price.toString()
                    baos.write(strPrice.toByteArray())
                }
            }
            // for combo orders
            if( mReceiptDetails!!.comboItems.count()>0 )
            {
                mReceiptDetails!!.comboItems.forEach { comboOrder ->

                    // add the item name to receipt
                    baos.write(NEW_LINE)
                    baos.write(FONT_SIZE_0)
                    baos.write(ALIGN_LEFT)
                    baos.write(EMPHASIZE_OFF)
                    baos.write(comboOrder!!.item.toByteArray())

                    // add the item price to receipt
                    val strPrice = "   " +mSale!!.currency + " " + comboOrder.price.toString()
                    baos.write(strPrice.toByteArray())

                    // add the description to receipt
                    val arrComboDesc = comboOrder.description.split(RECEIPT_ESC_STR_NEW_LINE_DELIMITER)
                    arrComboDesc.forEach { comboDesc ->
                        baos.write(NEW_LINE)
                        baos.write(EMPHASIZE_OFF)
                        baos.write(comboDesc.toByteArray())
                    }
                }
            }

        //==========================================================================================
        // Total
            baos.write(NEW_LINE)
            baos.write(FONT_8X12)
            baos.write(singleLine.toByteArray())

            baos.write(NEW_LINE)
            baos.write(FONT_10X18)

            baos.write(NEW_LINE)
            baos.write(FONT_SIZE_0)
            baos.write(ALIGN_LEFT)
            baos.write(EMPHASIZE_OFF)
            baos.write(EMPHASIZE_OFF)
//            baos.write("TOTAL:     ".toByteArray())
            baos.write(RECEIPT_TOTAL.toByteArray())
            baos.write(EMPHASIZE_OFF)
//            baos.write("SGD  23.00".toByteArray())
            val strTotal = mSale!!.currency + " " + mReceiptDetails!!.total
            baos.write(strTotal.toByteArray())

            baos.write(NEW_LINE)
            baos.write(FONT_SIZE_0)
            baos.write(ALIGN_LEFT)
            baos.write(EMPHASIZE_OFF)
            baos.write(EMPHASIZE_OFF)
//            baos.write("CHARGED:     ".toByteArray())
            baos.write(RECEIPT_CHARGED.toByteArray())
            baos.write(EMPHASIZE_OFF)
//            baos.write("SGD  23.00".toByteArray())
            val strCharged = mSale!!.currency + " " + mSale!!.amount
            baos.write(strCharged.toByteArray())

        //==========================================================================================
        // don't write beyond this point
        // this is to be able to display the texts above the cutter line
            baos.write(NEW_LINE)
            baos.write(NEW_LINE)
            baos.write(NEW_LINE)
            baos.write(NEW_LINE)
            baos.write(NEW_LINE)
            baos.write(NEW_LINE)
            baos.write(NEW_LINE)
            baos.write("".toByteArray())

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
//            print.data = mBaosPrintData!!.toByteArray()
            print.numOfReceipt = 1
            startAction(ActionType.PRINT(print))
    }
    fun PrintText() {} // TODO: what is this for?
    fun PrintEnd() {}

    fun generateOrderID():String
    {
        val charPool: List<Char> = ('A'..'F') + ('0'..'9')
        return (1..30)
            .map { kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    fun getOrderID():String
    {
        Log.w("XPayLink", "getOrderID(), mSale!!.orderId:" + mSale!!.orderId )
        return mSale!!.orderId
    }

    fun setReceiptHeader(header:String)
    {
        mReceiptDetails?.header = header
    }
    fun setReceiptDescription(description:String)
    {
        mReceiptDetails?.description = description
    }
    fun setReceiptTabletId(tabletId:String)
    {
        mReceiptDetails?.tabletId = tabletId
    }
    fun setReceiptTicketNumber(ticketNo: Int)
    {
        mReceiptDetails?.ticketNo = ticketNo
    }
    fun setReceiptFlightDate(flightDate: String)
    {
        mReceiptDetails?.flightDate = flightDate
    }
    fun setReceiptFlightNumber(flightNo: String)
    {
        mReceiptDetails?.flightNo = flightNo
    }
    fun setReceiptFlightRoute(flightRoute: String)
    {
        mReceiptDetails?.flightRoute = flightRoute
    }
    fun setReceiptFlightCrewName(crewName: String)
    {
        mReceiptDetails?.crewName = crewName
    }
    fun setReceiptOrderNumber(orderNo: String)
    {
        mReceiptDetails?.orderNo = orderNo
    }
    /**
     * @param item {String} example: "1 Tiger Beer"
     * @param price {Double} example: 15.00
     */
    fun addReceiptSingleOrder(item:String, price:Double)
    {
        val order = ReceiptOrderItemSingle()
            order.item = item
            order.price = price

        // add the price to the receipt's total count
        Log.w("XPayLink","addReceiptSingleOrder(), price:"+price)
//        mReceiptDetails?.total?.plus(price)
        mReceiptDetails?.total = mReceiptDetails!!.total + price
        Log.w("XPayLink","addReceiptSingleOrder(), mReceiptDetails!!.total:"
                +mReceiptDetails!!.total)

        mReceiptDetails!!.singleItems.add(order)
    }
    /**
     * @param item {String} example: "1 HOT MEAL COMBO"
     * @param description {String} example: "- Coca Cola Regular\n - Oriental Treasure Rice"
     * @param price {Double} example: 15.00
     */
    fun addReceiptComboOrder(item:String, description:String, price:Double)
    {
        val order = ReceiptOrderItemCombo()
            order.item = item
            order.description = description
            order.price = price

        // add the price to the receipt's total count
        Log.w("XPayLink","addReceiptComboOrder(), price:"+price)
//        mReceiptDetails?.total?.plus(price)
        mReceiptDetails?.total = mReceiptDetails!!.total + price
        Log.w("XPayLink","addReceiptComboOrder(), mReceiptDetails!!.total:"
                +mReceiptDetails!!.total)

        mReceiptDetails!!.comboItems.add(order)
    }

//#endregion Public calls
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
            mListener?.PrintComplete()
        }

        override fun onReturnDisableAccountSelectionResult(p0: Boolean) {

        }

        override fun onReturnAmount(p0: Hashtable<String, String>?) {

        }

        override fun onBTConnected(p0: BluetoothDevice?)
        {
            Log.w("XPayLink","onBTConnected, p0:" + p0.toString() )
            mSelectedDevice = p0
            mPrintOptions = PrintOptions()
            mListener?.OnTerminalConnectedChanged(p0)
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
//                trans.amount = mSale!!.amount.div(100.0)
                trans.amount = mSale!!.amount
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
            Log.w("XPayLink","onBTReturnScanResults, devices:"+devices.toString() )
//            mListener?.onBluetoothScanResult(devices)
//            mListener?.onBluetoothScanResult(devices)
//            mListener?.OnTerminalConnectedChanged(devices)
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


