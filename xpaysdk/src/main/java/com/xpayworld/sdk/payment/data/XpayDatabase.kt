package com.xpayworld.sdk.payment.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.xpayworld.payment.network.Constant

@Database(entities = [Purchase::class ],version = 1,exportSchema = false)
abstract class XpayDatabase : RoomDatabase(){

    abstract fun transactionDao(): PurchaseDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        private var INSTANCE: XpayDatabase? = null

        fun getDatabase(context: Context): XpayDatabase? {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    XpayDatabase::class.java,
                    Constant.DATABASE_NAME
                ).fallbackToDestructiveMigration()
                    .allowMainThreadQueries().build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
