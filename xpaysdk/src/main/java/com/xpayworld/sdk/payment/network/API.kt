package com.xpayworld.sdk.payment.network

import com.xpayworld.payment.network.PosWS
import com.xpayworld.payment.util.SharedPref
import com.xpayworld.sdk.payment.PaymentServiceListener
import com.xpayworld.sdk.payment.XPayLink
import com.xpayworld.sdk.payment.network.payload.Activation
import com.xpayworld.sdk.payment.network.payload.Login
import com.xpayworld.sdk.payment.utils.ProgressDialog
import com.xpayworld.sdk.payment.utils.Response
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class API {

    private var mListener: PaymentServiceListener? = null
    private lateinit var subscription: Disposable

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

    fun callActivation(activationPhrase: String, callback: (() -> Unit)) {
        var pos = PosWS.REQUEST()
        pos.activationKey = activationPhrase
        var data = Activation()
        data.imei = ""
        data.manufacturer = ""
        data.ip = "0.0.0"
        data.posWsRequest = pos
        val api = RetrofitClient().getRetrofit().create(Activation.API::class.java)
        subscription = api.activation(Activation.REQUEST(data))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { ProgressDialog.INSTANCE.show()}
            .doAfterTerminate { ProgressDialog.INSTANCE.dismiss()}
            .subscribe(
                { result ->
                    ProgressDialog.INSTANCE.dismiss()
                    val result = result.body()?.data
                    if (result?.errNumber != 0.0) {
                        mListener?.onError(
                            Response.ACTIVATION_FAILED.value,
                            Response.ACTIVATION_FAILED.name
                        )
                        return@subscribe
                    }
                    SharedPref.INSTANCE.writeMessage(PosWS.PREF_ACTIVATION, pos.activationKey!!)
                    subscription.dispose()
                    callback.invoke()
                },
                { error ->
                    mListener?.onError(
                        Response.ACTIVATION_FAILED.value,
                        Response.ACTIVATION_FAILED.name
                    )
                    println(error.message)
                    subscription.dispose()
                }
            )
    }

    fun callLogin(pinCode: String) {

        var data = Login()
        data.pin = pinCode

        val api = RetrofitClient().getRetrofit().create(Login.API::class.java)
        subscription = api.login(Login.REQUEST(data))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { ProgressDialog.INSTANCE.show() }
            .doAfterTerminate { ProgressDialog.INSTANCE.dismiss() }
            .subscribe(
                { result ->

                    val result = result.body()?.data
                    if (result?.errNumber != 0.0) {
                        mListener?.onError(
                            Response.ENTER_PIN_FAILED.value,
                            Response.ENTER_PIN_FAILED.name
                        )
                        return@subscribe
                    }
                    SharedPref.INSTANCE.writeMessage(PosWS.PREF_PIN, data.pin)
                    subscription.dispose()
                },
                { error ->
                    mListener?.onError(
                        Response.ENTER_PIN_FAILED.value,
                        Response.ENTER_PIN_FAILED.name
                    )
                    println(error.message)
                    subscription.dispose()
                }
            )
    }

    fun callTransaction(){

    }

}