package com.marketwear.indices

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.marketwear.indices.data.IndexQuote
import com.marketwear.indices.data.MarketRepository
import com.marketwear.shared.model.PortfolioRegistry
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Wear OS Watch Face Complication Provider for the 100-baseline Daily Portfolio Score.
 * Reads strictly from local Room DB cache for zero battery penalty.
 */
class PortfolioScoreComplicationService : SuspendingComplicationDataSourceService(), KoinComponent {

    private val repository: MarketRepository by inject()

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val quote = repository.getQuote(PortfolioRegistry.PORTFOLIO_SYMBOL)
        return createComplicationData(quote, request.complicationType)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val preview = PortfolioRegistry.createDefaultFallbackPortfolioQuote(System.currentTimeMillis())
        return createComplicationData(preview, type)
    }

    private fun createComplicationData(quote: IndexQuote, type: ComplicationType): ComplicationData? {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_TARGET_INDEX, PortfolioRegistry.PORTFOLIO_SYMBOL)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            1004,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val scoreText = quote.formattedScore
        val deltaText = quote.formattedChange
        val contentDesc = PlainComplicationText.Builder("Portfolio Score $scoreText ($deltaText)").build()

        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(scoreText).build(),
                contentDescription = contentDesc
            )
                .setTitle(PlainComplicationText.Builder("PF $deltaText").build())
                .setTapAction(pendingIntent)
                .build()

            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                text = PlainComplicationText.Builder("Portfolio $scoreText ($deltaText)").build(),
                contentDescription = contentDesc
            )
                .setTitle(PlainComplicationText.Builder("37 Holdings Score").build())
                .setTapAction(pendingIntent)
                .build()

            ComplicationType.RANGED_VALUE -> {
                val clamped = quote.price.toFloat().coerceIn(95.0f, 105.0f)
                RangedValueComplicationData.Builder(
                    value = clamped,
                    min = 95.0f,
                    max = 105.0f,
                    contentDescription = contentDesc
                )
                    .setText(PlainComplicationText.Builder(scoreText).build())
                    .setTitle(PlainComplicationText.Builder(deltaText).build())
                    .setTapAction(pendingIntent)
                    .build()
            }

            else -> null
        }
    }
}
