package com.xpayworld.sdk.payment.utils

import com.xpayworld.sdk.payment.network.payload.EnumCompanion


enum class XPayResponse(val value: Int) {
    ACTIVATION_FAILED(-13),
    ENTER_PIN_FAILED(-14),
    CARD_EXPIRED(-15),
    BATCH_NETWORK_FAILED(-16),
    TXN_NETWORK_FAILED(-17);

    companion object : EnumCompanion<Int, XPayResponse>(
        XPayResponse.values().associateBy(
            XPayResponse::value))

}