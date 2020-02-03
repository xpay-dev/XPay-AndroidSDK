package com.xpayworld.sdk.payment.utils

import com.xpayworld.sdk.payment.network.payload.EnumCompanion


enum class XPayError(val value: Int) {
    ACTIVATION_FAILED(-13),
    ENTER_PIN_FAILED(-14),
    CARD_EXPIRED(-15),
    NETWORK_FAILED(-16),
    TXN_NETWORK_FAILED(-17),
    TXN_CANCELLED(-18),
    PRINT_CANCELLED(-19);

    companion object : EnumCompanion<Int, XPayError>(
        XPayError.values().associateBy(
            XPayError::value))

}