package com.xpayworld.sdk.payment

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.net.ConnectivityManager
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
import com.xpayworld.sdk.payment.utils.DispatchGroup
import com.xpayworld.sdk.payment.utils.PopupDialog
import com.xpayworld.sdk.payment.utils.ProgressDialog
import com.xpayworld.sdk.payment.utils.XPayError
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


interface PaymentServiceListener {

    fun onBluetoothScanResult(devices: MutableList<BluetoothDevice>?)
    fun onTransactionComplete()
    fun onBatchUploadResult(totalTxn: Int?, unsyncTxn: Int?)
    fun onPrintComplete()
    fun onError(error: Int?, message: String?)
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
        when (type) {
            is ActionType.SALE -> {
                mSale = type.sale

                if (!isActivated()) {
                    mListener?.onError(
                        XPayError.ACTIVATION_FAILED.value,
                        XPayError.ACTIVATION_FAILED.name
                    )
                    return@startAction
                }

                if (!hasEnteredPin()) {
                    mListener?.onError(
                        XPayError.ENTER_PIN_FAILED.value,
                        XPayError.ENTER_PIN_FAILED.name
                    )
                    return@startAction
                }

                when (mSale?.connection) {
                    Connection.SERIAL -> {
                        mBBDeviceController?.startSerial()
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

                when (mPrintDetails?.connection) {
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


    fun setBTConnection(device: BluetoothDevice) {
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

    private fun processBatchUpload() {
        if (!isNetworkAvailable()) {
            mListener?.onError(
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

    private fun uploadTransaction() {
        ProgressDialog.INSTANCE.attach(CONTEXT)
        ProgressDialog.INSTANCE.message("Transaction Uploading...")
        ProgressDialog.INSTANCE.show()

        val txnArr = mTransactionRepo.getTransaction()
        mTotalTransactions = txnArr.count()
        val dispatch = DispatchGroup()

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
            mListener?.onBatchUploadResult(mTotalTransactions, getTransactions().count())
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

    private fun showActivation() {
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

    private fun shouldCheckCardExpiry() : Int{
        // Check if the card is already expired
        val cardExpiry = mCard.expiryDate
        val cardYear = "20${cardExpiry.substring(0..2)}".toInt()
        val cardMonth = cardExpiry.substring(2..4).toInt()

        val calendar: Calendar = Calendar.getInstance()
        val year: Int = calendar.get(Calendar.YEAR)
        val month: Int = calendar.get(Calendar.MONTH)

        if ((cardYear <= year) && cardMonth < month) {
            mListener?.onError(
                XPayError.CARD_EXPIRED.value,
                XPayError.CARD_EXPIRED.name
            )
            return XPayError.CARD_EXPIRED.value
        }
        return 0
    }

    private fun insertTransaction() {
        var trans = Transaction()
        trans.amount = mSale!!.amount.div(100.0)
        trans.currency = mSale!!.currency
        trans.orderId = mSale!!.orderId
        trans.isOffline = mSale!!.isOffline
        trans.card = mCard
        trans.timestamp = System.currentTimeMillis()

    }

    @SuppressLint("SimpleDateFormat")
    private fun startEMV() {
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

    private fun setAmount() {
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


    private inner class BBPOSDeviceListener :
        BBDeviceController.BBDeviceControllerListener {

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

        override fun onBTConnected(p0: BluetoothDevice?) {
            when (mActionType) {
                is ActionType.SALE -> {
                    startEMV()
                }
                is ActionType.PRINT -> {
                    startPrinter()
                }
            }
        }

        override fun onReturnApduResult(p0: Boolean, p1: Hashtable<String, Any>?) {

        }

        override fun onReturnCAPKDetail(p0: CAPK?) {

        }

        override fun onReturnReversalData(p0: String?) {

        }

        override fun onReturnReadAIDResult(p0: Hashtable<String, Any>?) {

        }

        override fun onRequestOnlineProcess(tlv: String?) {
            val decodeData = BBDeviceController.decodeTlv(tlv)
            mCard.emvICCData = decodeData["C2"].toString()
            mCard.expiryDate = decodeData["5F24"].toString()
            mCard.ksn = decodeData["C0"].toString()
            mCard.cardNumber = decodeData["5A"].toString()
            mCard.cardXNumber = decodeData["C4"].toString()

            var trans = Transaction()
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

        override fun onReturnTransactionResult(result: BBDeviceController.TransactionResult?) {
            ProgressDialog.INSTANCE.dismiss()
            if (result == BBDeviceController.TransactionResult.APPROVED){
                mListener?.onTransactionComplete()
                return
            }
            mListener?.onError(result?.ordinal, result?.name)

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
            mListener?.onBluetoothScanResult(devices)
        }

        override fun onEnterStandbyMode() {

        }

        override fun onPrintDataEnd() {
            mListener?.onPrintComplete()
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
            mListener?.onError(XPayError.PRINT_CANCELLED.value,
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
            startEMV()
        }

        override fun onReturnBatchData(p0: String?) {

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
                mListener?.onError(
                    XPayError.TXN_CANCELLED.value,
                    XPayError.TXN_CANCELLED.name
                )
            }
        }

        override fun onBatteryLow(status: BBDeviceController.BatteryStatus?) {
            mListener?.onError(status?.ordinal, status?.name)
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
            mListener?.onError(error?.ordinal, error?.name)
        }

        override fun onReturnAmountConfirmResult(p0: Boolean) {

        }

        override fun onReturnInjectSessionKeyResult(p0: Boolean, p1: Hashtable<String, String>?) {

        }

        override fun onReturnPowerOffIccResult(p0: Boolean) {

        }

    }
}


