package com.xpayworld.sdk.payment.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query


@Dao
interface PurchaseDao {

    @Query("SELECT * FROM `purchase`")
    fun getPurchases(): List<Purchase>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPurchase(vararg purchase: Purchase)

    @Query("DELETE FROM `Purchase` WHERE orderId =:orderId")
    fun deletePurchase(orderId: String)

    @Query("UPDATE `purchase` SET isOffline = :status ,isSync = :isSync  WHERE orderId = :orderId")
    fun updateSync(status: Boolean, isSync: Boolean, orderId: String)

    @Query("UPDATE `purchase` SET signature = :sign  WHERE orderId = :orderId")
    fun updateSignature(sign: String, orderId: String)

}