package com.xpayworld.sdk.payment.network

import android.util.Log
import com.xpayworld.payment.util.SharedPref
import com.xpayworld.sdk.payment.PaymentServiceListener
import com.xpayworld.sdk.payment.XPayLink
import com.xpayworld.sdk.payment.data.Transaction
import com.xpayworld.sdk.payment.network.payload.Activation
import com.xpayworld.sdk.payment.network.payload.Login
import com.xpayworld.sdk.payment.network.payload.PurchaseTransaction
import com.xpayworld.sdk.payment.utils.ProgressDialog
import com.xpayworld.sdk.payment.utils.XPayError

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.Observable
import retrofit2.Response

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
        data.manufacturer = "Android"
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
//                        mListener?.onError(
//                            XPayError.ACTIVATION_FAILED.value,
//                            XPayError.ACTIVATION_FAILED.name
//                        )
                        mListener?.OnError(
                            XPayError.ACTIVATION_FAILED.value,
                            XPayError.ACTIVATION_FAILED.name
                        )
                        return@subscribe
                    }
                    SharedPref.INSTANCE.writeMessage(PosWS.PREF_ACTIVATION, pos.activationKey!!)
                    SharedPref.INSTANCE.writeMessage(PosWS.PREF_MOBILE_APP_ID, result.mobileAppId!!)
                    SharedPref.INSTANCE.writeMessage(PosWS.PREF_ACCOUNT_ID,result.accountId!!)
                    callback?.invoke()
                },
                { error ->
//                    mListener?.onError(
//                        XPayError.NETWORK_FAILED.value,
//                        XPayError.NETWORK_FAILED.name
//                    )
                    mListener?.OnError(
                        XPayError.NETWORK_FAILED.value,
                        XPayError.NETWORK_FAILED.name
                    )
                    println(error.message)
                    subscription.dispose()
                }
            )
    }

    fun callLogin(pinCode: String, callback: (() -> Unit)? = null)
    {
        Log.w("API","callLogin()")
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
//                        mListener?.onError(
//                            XPayError.ENTER_PIN_FAILED.value,
//                            XPayError.ENTER_PIN_FAILED.name
//                        )
                        mListener?.OnError(
                            XPayError.ENTER_PIN_FAILED.value,
                            XPayError.ENTER_PIN_FAILED.name
                        )
                        return@subscribe
                    }
                    SharedPref.INSTANCE.writeMessage(PosWS.PREF_PIN, data.pin)
                    SharedPref.INSTANCE.writeMessage(PosWS.PREF_RTOKEN, result.rToken!!)

                    callback?.invoke()
                },
                { error ->
//                    mListener?.onError(
//                        XPayError.NETWORK_FAILED.value,
//                        XPayError.NETWORK_FAILED.name
//                    )
                    mListener?.OnError(
                        XPayError.NETWORK_FAILED.value,
                        XPayError.NETWORK_FAILED.name
                    )
                    println(error.message)
                    subscription.dispose()
                }
            )
    }

    fun callTransaction(
        txn: Transaction,
        callback: ((response: Any,
                    purchase: Transaction) -> Unit)
    ) {
        Log.w("API","callTransaction")
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
                    Log.w("API",result.message())
                    val response = result.body()!!.data!!
                    callback.invoke(response, txn)
                },
                { error ->
                    Log.e("ERROR",error.localizedMessage)
                    callback.invoke(error, txn)
                }
            )

    }
}