package com.xpayworld.sdk.payment.utils

import android.graphics.Bitmap
import com.bbpos.bbdevice.BBDeviceController
import java.util.*

val INIT = byteArrayOf(0x1B, 0x40)
val POWER_ON = byteArrayOf(0x1B, 0x3D, 0x01)
val POWER_OFF = byteArrayOf(0x1B, 0x3D, 0x02)
val NEW_LINE = byteArrayOf(0x0A)
val ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
val ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
val ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)
val EMPHASIZE_ON = byteArrayOf(0x1B, 0x45, 0x01)
val EMPHASIZE_OFF = byteArrayOf(0x1B, 0x45, 0x00)
val FONT_5X8 = byteArrayOf(0x1B, 0x4D, 0x00)
val FONT_5X12 = byteArrayOf(0x1B, 0x4D, 0x01)
val FONT_8X12 = byteArrayOf(0x1B, 0x4D, 0x02)
val FONT_10X18 = byteArrayOf(0x1B, 0x4D, 0x03)
val FONT_SIZE_0 = byteArrayOf(0x1D, 0x21, 0x00)
val FONT_SIZE_1 = byteArrayOf(0x1D, 0x21, 0x11)
val CHAR_SPACING_0 = byteArrayOf(0x1B, 0x20, 0x00)
val CHAR_SPACING_1 = byteArrayOf(0x1B, 0x20, 0x01)
val KANJI_FONT_24X24 =
    byteArrayOf(0x1C, 0x28, 0x41, 0x02, 0x00, 0x30, 0x00)
val KANJI_FONT_16X16 =
    byteArrayOf(0x1C, 0x28, 0x41, 0x02, 0x00, 0x30, 0x01)

fun getImageCommand( bitmap: Bitmap): ByteArray{
    return BBDeviceController.getImageCommand(bitmap, 150)
}

fun getBarcodeCommand( barcodeData: Hashtable<String, String>) : ByteArray{
    return BBDeviceController.getBarcodeCommand(barcodeData)
}