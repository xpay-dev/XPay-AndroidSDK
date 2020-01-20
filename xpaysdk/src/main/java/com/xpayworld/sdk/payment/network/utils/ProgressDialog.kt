package com.xpayworld.sdk.payment.network.utils


import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.widget.ProgressBar
import com.xpayworld.sdk.payment.R


fun setProgressDialog(context: Context): Dialog {
    val builder = AlertDialog.Builder(context)
    builder.setCancelable(false) // if you want user to wait for some process to finish,

    builder.setView(R.layout.layout_loading_dialog)
   return builder.create()
}

class ProgressDialog {
    var mBuilder : android.app.ProgressDialog?= null
    init {
        INSTANCE = this
    }
    companion object{
        var INSTANCE : ProgressDialog = ProgressDialog()
    }

    fun attach(context: Context) {

        val dialog = android.app.ProgressDialog(context)
        dialog.setCancelable(false)
        mBuilder = dialog

    }
    fun show(){
        mBuilder?.show()
    }

    fun dismiss(){
        mBuilder?.dismiss()
    }

    fun message(msg: String){
        mBuilder?.setMessage(msg)
    }
}