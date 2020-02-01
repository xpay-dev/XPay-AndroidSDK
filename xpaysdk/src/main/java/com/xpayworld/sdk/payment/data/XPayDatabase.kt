package com.xpayworld.sdk.payment.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.xpayworld.sdk.payment.XPayLink
import com.xpayworld.sdk.payment.network.Constant

@Database(entities = [Transaction::class ],version = 2,exportSchema = false)
abstract class XPayDatabase : RoomDatabase(){

    abstract fun transactionDao(): TransactionDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        private var INSTANCE: XPayDatabase? = null

        fun getDatabase(): XPayDatabase? {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    XPayLink.CONTEXT,
                    XPayDatabase::class.java,
                    Constant.DATABASE_NAME
                ).fallbackToDestructiveMigration()
                    .allowMainThreadQueries().build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
