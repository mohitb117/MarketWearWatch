package com.marketwear.indices.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IndexQuoteDao {

    @Query("SELECT * FROM index_quotes ORDER BY CASE WHEN symbol = 'SPX' THEN 0 ELSE 1 END")
    fun getAllQuotesFlow(): Flow<List<IndexQuoteEntity>>

    @Query("SELECT * FROM index_quotes WHERE symbol = :symbol LIMIT 1")
    fun getQuoteFlow(symbol: String): Flow<IndexQuoteEntity?>

    @Query("SELECT * FROM index_quotes WHERE symbol = :symbol LIMIT 1")
    suspend fun getQuote(symbol: String): IndexQuoteEntity?

    @Query("SELECT * FROM index_quotes")
    suspend fun getAllQuotes(): List<IndexQuoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(quotes: List<IndexQuoteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(quote: IndexQuoteEntity)
}
