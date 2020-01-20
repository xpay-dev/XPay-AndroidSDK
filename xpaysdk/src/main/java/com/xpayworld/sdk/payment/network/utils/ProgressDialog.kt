package com.xpayworld.sdk.payment.network.utils


import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.widget.TextView
import com.xpayworld.sdk.payment.R


fun setProgressDialog(context: Context): Dialog {
    val builder = AlertDialog.Builder(context)
    builder.setCancelable(false) // if you want user to wait for some process to finish,

    builder.setView(R.layout.layout_loading_dialog)
   return builder.create()
}

class ProgressDialog {
    var mBuilder : Dialog?= null
    init {
        INSTANCE = this
    }
    companion object{
        var INSTANCE : ProgressDialog = ProgressDialog()
    }

    fun attach(context: Context) {

        val dialog = Dialog(context)
        val inflate = LayoutInflater.from(context).inflate(R.layout.layout_loading_dialog, null)

        dialog.setContentView(inflate)
        dialog.setCancelable(false)
        mBuilder = dialog

    }
    fun show(){
        mBuilder?.show()
    }

    fun dismiss(){
        mBuilder?.dismiss()
    }

    fun message(msg : String){
       val text = mBuilder?.findViewById<TextView>(R.id.tv_message)
        text?.text = msg
    }
}