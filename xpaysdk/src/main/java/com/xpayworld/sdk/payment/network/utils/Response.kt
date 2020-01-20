package com.xpayworld.sdk.payment.network.utils

import com.xpayworld.sdk.payment.network.transaction.EnumCompanion


enum class Response(val value: Int) {
    ACTIVATION_FAILED(-13),
    ENTER_PIN_FAILED(-14);

    companion object : EnumCompanion<Int, Response>(Response.values().associateBy(Response::value))

}