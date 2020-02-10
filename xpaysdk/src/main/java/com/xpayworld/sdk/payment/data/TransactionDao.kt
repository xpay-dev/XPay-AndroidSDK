package com.xpayworld.sdk.payment.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query


@Dao
interface TransactionDao {

    @Query("SELECT * FROM `transaction`")
    fun getTransaction(): List<Transaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTransaction(vararg transaction: Transaction)

    @Query("DELETE FROM `transaction` WHERE orderId =:orderId")
    fun deleteTransaction(orderId: String)

    @Query("DELETE FROM `transaction` WHERE isSync = 'true'")
    fun deleteSyncTransaction()

    @Query("UPDATE `transaction` SET errorMessage =  :errorMessage, isSync = :isSync  WHERE orderId = :orderId")
    fun updateSync(errorMessage: String, isSync: Boolean, orderId: String)

    @Query("UPDATE `transaction` SET signature = :sign  WHERE orderId = :orderId")
    fun updateSignature(sign: String, orderId: String)

    @Query("SELECT * FROM `transaction` WHERE orderId =:orderId")
    fun searchTransaction(orderId: String):  List<Transaction>

    @Query("DELETE FROM `transaction`")
    fun deleteAllTransaction()
}