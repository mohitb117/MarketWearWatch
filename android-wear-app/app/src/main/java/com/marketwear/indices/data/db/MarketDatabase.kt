package com.marketwear.indices.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [IndexQuoteEntity::class], version = 2, exportSchema = false)
abstract class MarketDatabase : RoomDatabase() {

    abstract fun indexQuoteDao(): IndexQuoteDao

    companion object {
        @Volatile
        private var INSTANCE: MarketDatabase? = null

        fun getInstance(context: Context): MarketDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MarketDatabase::class.java,
                    "market_wear.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
