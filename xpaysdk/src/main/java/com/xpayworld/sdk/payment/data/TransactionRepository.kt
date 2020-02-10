package com.xpayworld.sdk.payment.data

class TransactionRepository private constructor(
    private val transDao: TransactionDao
) {

    fun getTransaction() = transDao.getTransaction()

    fun createTransaction(trans: Transaction) {
        transDao.insertTransaction(trans)
    }

    fun deleteAllTransaction() {
        transDao.deleteAllTransaction()
    }

    fun deleteSyncTransaction() {
        transDao.deleteSyncTransaction()
    }

    fun updateTransaction(errorMessage: String, isSync: Boolean, orderId: String) {
        transDao.updateSync(errorMessage = errorMessage, isSync = isSync, orderId = orderId)
    }

    fun updateSignatureTransaction(sign: String, orderId: String) {
        transDao.updateSignature(sign, orderId)
    }

    fun deleteTransaction(orderId: String) {
        transDao.deleteTransaction(orderId = orderId)
    }

    fun searchTransaction(orderId: String): List<Transaction> {
        return transDao.searchTransaction(orderId)
    }

    companion object {

        @Volatile
        private var instance: TransactionRepository? = null

        fun getInstance(transDao: TransactionDao) = instance ?: synchronized(this) {
            instance ?: TransactionRepository(transDao).also { instance = it }
        }
    }
}