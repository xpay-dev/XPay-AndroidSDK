package com.xpayworld.sdk.payment.network

import android.util.Log
import com.xpayworld.payment.util.SharedPref
import com.xpayworld.sdk.payment.ActionType
import com.xpayworld.sdk.payment.PaymentServiceListener
import com.xpayworld.sdk.payment.XPayLink
import com.xpayworld.sdk.payment.data.Transaction
import com.xpayworld.sdk.payment.network.payload.Activation
import com.xpayworld.sdk.payment.network.payload.Login
import com.xpayworld.sdk.payment.network.payload.PurchaseTransaction
import com.xpayworld.sdk.payment.network.payload.TransactionResponse
import com.xpayworld.sdk.payment.utils.ProgressDialog
import com.xpayworld.sdk.payment.utils.XPayResponse

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.Observable
import retrofit2.Response
import java.util.*

class API {

    private var mListener: PaymentServiceListener? = null
    private lateinit var subscription: Disposable

    private val mClient = RetrofitClient().getRetrofit()

    init {
        INSTANCE = this
        ProgressDialog.INSTANCE.attach(XPayLink.CONTEXT)
        ProgressDialog.INSTANCE.message("Loading...")
    }

    companion object {
        var INSTANCE = API()
    }


    fun attach(listener: PaymentServiceListener) {
        mListener = listener
    }

    fun callActivation(activationPhrase: String, callback: (() -> Unit)? = null) {
        var pos = PosWS.REQUEST()
        pos.activationKey = activationPhrase
        var data = Activation()
        data.imei = ""
        data.manufacturer = ""
        data.ip = "0.0.0"
        data.posWsRequest = pos
        val api = mClient.create(Activation.API::class.java)
        subscription = api.activation(Activation.REQUEST(data))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { ProgressDialog.INSTANCE.show() }
            .doOnDispose { ProgressDialog.INSTANCE.dismiss() }
            .subscribe(
                { result ->
                    subscription.dispose()
                    val result = result.body()?.data
                    if (result?.errNumber != 0.0) {
                        mListener?.onError(
                            XPayResponse.ACTIVATION_FAILED.value,
                            XPayResponse.ACTIVATION_FAILED.name
                        )
                        return@subscribe
                    }
                    SharedPref.INSTANCE.writeMessage(PosWS.PREF_ACTIVATION, pos.activationKey!!)

                    callback?.invoke()
                },
                { error ->
                    mListener?.onError(
                        XPayResponse.NETWORK_FAILED.value,
                        XPayResponse.NETWORK_FAILED.name
                    )
                    Log.e("Activation", error.message)
                    println(error.message)
                    subscription.dispose()
                }
            )
    }

    fun callLogin(pinCode: String, callback: (() -> Unit)? = null) {

        var data = Login()
        data.pin = pinCode

        val api = mClient.create(Login.API::class.java)
        subscription = api.login(Login.REQUEST(data))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { ProgressDialog.INSTANCE.show() }
            .doOnDispose { ProgressDialog.INSTANCE.dismiss() }
            .subscribe(
                { result ->
                    subscription.dispose()
                    val result = result.body()?.data
                    if (result?.errNumber != 0.0) {
                        mListener?.onError(
                            XPayResponse.ENTER_PIN_FAILED.value,
                            XPayResponse.ENTER_PIN_FAILED.name
                        )
                        return@subscribe
                    }
                    SharedPref.INSTANCE.writeMessage(PosWS.PREF_PIN, data.pin)
                    SharedPref.INSTANCE.writeMessage(PosWS.PREF_RTOKEN, result.rToken!!)

                    callback?.invoke()
                },
                { error ->
                    mListener?.onError(
                        XPayResponse.NETWORK_FAILED.value,
                        XPayResponse.NETWORK_FAILED.name
                    )
                    Log.e("PIN", error.message)
                    println(error.message)
                    subscription.dispose()
                }
            )
    }

    fun callTransaction(
        txn: Transaction,
        callback: ((response: Any, purchase: Transaction) -> Unit)
    ) {
        val data = PurchaseTransaction()
        data.processOffline = txn.isOffline
        data.attach(txn)
        var resultTxn: Observable<Response<PurchaseTransaction.RESULT>>? = null
        val api = mClient.create(PurchaseTransaction.API::class.java)
        resultTxn = if (txn.card?.serviceCode != "") {
            data.action = PurchaseTransaction.Action.SWIPE.ordinal
            api.SWIPE(PurchaseTransaction.REQUEST(data))
        } else {
            data.action = PurchaseTransaction.Action.EMV.ordinal
            api.EMV(PurchaseTransaction.REQUEST(data))
        }
        subscription = resultTxn
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result ->
                    val response = result.body()!!.data!!
                    callback.invoke(response, txn)
                },
                { error ->

                    callback.invoke(error, txn)
                    Log.e("Trans error", error.localizedMessage)
                }
            )

    }
}