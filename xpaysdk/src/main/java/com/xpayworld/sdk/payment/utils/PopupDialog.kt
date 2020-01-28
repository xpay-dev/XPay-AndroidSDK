package com.xpayworld.sdk.payment.utils

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.widget.EditText
import android.widget.LinearLayout
import com.xpayworld.sdk.payment.XPayLink

class PopupDialog  {

    var alertDialog: AlertDialog? = null
    private var mInput : EditText? = null

    var buttonPositive: String? = null
    var buttonNegative: String? = null
    var hasEditText: Boolean? = null
    var text: String? = ""
    var title: String? = null

    fun show(callback: ((buttonId: Int) -> Unit)? = null){

        val builder = AlertDialog.Builder(XPayLink.CONTEXT)
        builder.setTitle(title)

        builder.setPositiveButton(buttonPositive) { _: DialogInterface, _: Int ->
            text = mInput?.text.toString()
            callback?.invoke(1)
            alertDialog?.dismiss()
        }

        builder.setNegativeButton(buttonNegative){_: DialogInterface,_: Int ->
            callback?.invoke(2)
            alertDialog?.dismiss()
        }

        alertDialog = builder.create()

        if(hasEditText!!){
            mInput = EditText(XPayLink.CONTEXT)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            mInput?.layoutParams = lp
            alertDialog?.setView(mInput)
        }

        alertDialog?.show()
    }



}

interface DialogCallBack{
    fun onClickButtons(int: Int)
}