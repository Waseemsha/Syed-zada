package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [InvoiceEntity::class], version = 1, exportSchema = false)
@TypeConverters(InvoiceTypeConverters::class)
abstract class InvoiceDatabase : RoomDatabase() {
    abstract fun invoiceDao(): InvoiceDao

    companion object {
        @Volatile
        private var INSTANCE: InvoiceDatabase? = null

        fun getDatabase(context: Context): InvoiceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InvoiceDatabase::class.java,
                    "seller_aspire_invoice_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
