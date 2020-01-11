package com.xpayworld.sdk.payment

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.bbpos.bbdevice.BBDeviceController
import com.bbpos.bbdevice.BBDeviceController.CurrencyCharacter
import com.bbpos.bbdevice.CAPK
import java.text.SimpleDateFormat
import java.util.*

enum class Connection {
    SERIAL, BLUETOOTH
}

sealed class ActionType {
    // for transaction
    data class SALE(var saleData: SaleData) : ActionType()
    object PRINTER : ActionType()
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

class SaleData {
    var amount: Int? = null
    var currency: String? = null
    var currencyCode: Int? = null
    var orderId: String? = null
    var connection: Connection? = null
    var cardMode: CardMode? = null
}


interface PaymentServiceListener {
    fun onBluetoothScanResult(devices: MutableList<BluetoothDevice>?)
}

class PaymentService(private val mContext: Context, listener: PaymentServiceListener) {

    private val CARD_MODE: BBDeviceController.CheckCardMode? = null
    private val DEVICE_NAMES = arrayOf("WP")

    private var mBBDeviceController: BBDeviceController? = null
    private var mDeviceListener: BBPOSDeviceListener? = null
    private var mSelectedDevice: BluetoothDevice? = null

    private var mSale: SaleData? = null
    private var mActionType: ActionType? = null

    init {
        mDeviceListener = BBPOSDeviceListener(listener)
        mBBDeviceController = BBDeviceController.getInstance(mContext, mDeviceListener)
        BBDeviceController.setDebugLogEnabled(true)
    }

    /**
     * Payment Service Configuration
     *
     * @param type             Integer   eg. 1,0023.40 = 1002340
     * @param currency       String   eg. PHP = Philippine Peso
     * @param currencyCode     Integer  eg. 608 = PHP
     *                           reference https://www.iban.com/currency-codes
     * @param connection         Connection Object
     *                          `BLUETOOTH` and `SERIAL`
     * @param cardMode           BBDeviceController.CheckCardMode
     */
    fun startDevice(type: ActionType) {
        mActionType = type
        when (type) {
            is ActionType.SALE -> {

                mSale = type.saleData

                when (mSale?.connection) {
                    Connection.SERIAL -> {

                    }
                    Connection.BLUETOOTH -> {
                        mBBDeviceController?.startBTScan(DEVICE_NAMES, 120)
                    }
                }
            }
            is ActionType.PRINTER -> {

            }
        }
    }

    fun startPrinter(connection: Connection) {

    }

    fun setBTConnection(device: BluetoothDevice) {
        mSelectedDevice = device
        mBBDeviceController?.connectBT(device)
    }


    @SuppressLint("SimpleDateFormat")
    private fun startEMV() {
        val data: Hashtable<String, Any> = Hashtable() //define empty hashmap
        data["emvOption"] = BBDeviceController.EmvOption.START
        data["orderID"] = "${mSale?.orderId}"
        data["randomNumber"] = "012345"
        data["checkCardMode"] = valueOf(value = mSale?.cardMode!!.ordinal)
        // Terminal Time
        val terminalTime = SimpleDateFormat("yyMMddHHmmss").format(Calendar.getInstance().time)
        data["terminalTime"] = terminalTime
        mBBDeviceController?.startEmv(data)
    }

    private fun setAmount() {
        val input: Hashtable<String, Any> = Hashtable() //define empty hashmap

        // Currency Sign
        val currencyCharacter = arrayOf(
            CurrencyCharacter.P,
            CurrencyCharacter.H,
            CurrencyCharacter.P
        )

        // Configure Amount
        input.put("amount", "${mSale?.amount}")
        input.put("transactionType", BBDeviceController.TransactionType.GOODS)
        input.put("currencyCode", "${mSale?.currencyCode}")

        if (mSale?.connection == Connection.BLUETOOTH) {
            input.put("currencyCharacters", currencyCharacter)
        }

        mBBDeviceController?.setAmount(input)
    }

    companion object {
        fun valueOf(value: Int): BBDeviceController.CheckCardMode? =
            BBDeviceController.CheckCardMode.values().find { it.value == value }
    }

    private inner class BBPOSDeviceListener(private val listener: PaymentServiceListener) :
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

        }

        override fun onRequestDisplayText(p0: BBDeviceController.DisplayText?) {

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
                is ActionType.PRINTER -> {

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

        override fun onRequestOnlineProcess(p0: String?) {

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

        override fun onReturnTransactionResult(p0: BBDeviceController.TransactionResult?) {

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
            listener.onBluetoothScanResult(devices)
        }

        override fun onEnterStandbyMode() {

        }

        override fun onPrintDataEnd() {

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

        }

        override fun onReturnBarcode(p0: String?) {

        }

        override fun onReturnUpdateGprsSettingsResult(
            p0: Boolean,
            p1: Hashtable<String, BBDeviceController.TerminalSettingStatus>?
        ) {

        }

        override fun onRequestPrintData(p0: Int, p1: Boolean) {

        }

        override fun onSerialConnected() {

        }

        override fun onReturnBatchData(p0: String?) {

        }

        override fun onReturnEncryptDataResult(p0: Boolean, p1: Hashtable<String, String>?) {

        }

        override fun onBarcodeReaderConnected() {

        }

        override fun onRequestDisplayAsterisk(p0: Int) {

        }

        override fun onReturnDeviceInfo(p0: Hashtable<String, String>?) {

        }

        override fun onReturnCancelCheckCardResult(p0: Boolean) {

        }

        override fun onBatteryLow(p0: BBDeviceController.BatteryStatus?) {

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

        }

        override fun onReturnCheckCardResult(
            p0: BBDeviceController.CheckCardResult?,
            p1: Hashtable<String, String>?
        ) {

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

        override fun onError(p0: BBDeviceController.Error?, p1: String?) {

        }

        override fun onReturnAmountConfirmResult(p0: Boolean) {

        }

        override fun onReturnInjectSessionKeyResult(p0: Boolean, p1: Hashtable<String, String>?) {

        }

        override fun onReturnPowerOffIccResult(p0: Boolean) {

        }

    }
}


