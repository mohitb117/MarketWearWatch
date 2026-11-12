package com.marketwear.indices.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.marketwear.indices.data.IndexQuote

@Entity(tableName = "index_quotes")
data class IndexQuoteEntity(
    @PrimaryKey
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val percentChange: Double,
    val dayHigh: Double,
    val dayLow: Double,
    val lastUpdated: Long
) {
    fun toDomainModel(): IndexQuote = IndexQuote(
        symbol = symbol,
        name = name,
        price = price,
        change = change,
        percentChange = percentChange,
        dayHigh = dayHigh,
        dayLow = dayLow,
        lastUpdated = lastUpdated
    )

    companion object {
        fun fromDomainModel(quote: IndexQuote): IndexQuoteEntity = IndexQuoteEntity(
            symbol = quote.symbol,
            name = quote.name,
            price = quote.price,
            change = quote.change,
            percentChange = quote.percentChange,
            dayHigh = quote.dayHigh,
            dayLow = quote.dayLow,
            lastUpdated = quote.lastUpdated
        )
    }
}
