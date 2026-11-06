package com.marketwear.indices

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.marketwear.indices.data.MarketRepository
import com.marketwear.indices.data.IndexQuote
import java.util.Locale

/**
 * Android Wear OS Complication Provider for Nasdaq 100 (^NDX).
 */
class NasdaqComplicationService : SuspendingComplicationDataSourceService() {

    private val repository by lazy { MarketRepository.getInstance(applicationContext) }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val quote = repository.getQuote("NDX")
        return createComplicationData(quote, request.complicationType)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val previewQuote = IndexQuote(
            symbol = "NDX",
            name = "Nasdaq 100",
            price = 21450.80,
            change = 230.10,
            percentChange = 1.08,
            dayHigh = 21520.00,
            dayLow = 21280.00,
            lastUpdated = System.currentTimeMillis()
        )
        return createComplicationData(previewQuote, type)
    }

    private fun createComplicationData(quote: IndexQuote, type: ComplicationType): ComplicationData? {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(MainActivity.EXTRA_TARGET_INDEX, "NDX")
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            1002,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isPositive = quote.change >= 0
        val trendIconRes = if (isPositive) R.drawable.ic_trend_up else R.drawable.ic_trend_down
        val icon = Icon.createWithResource(this, trendIconRes)
        val monoImage = MonochromaticImage.Builder(icon).build()

        val sign = if (isPositive) "+" else ""
        val percentText = String.format(Locale.US, "%s%.2f%%", sign, quote.percentChange)
        val pointsText = String.format(Locale.US, "%,.0f", quote.price)
        val fullTitle = "NDX"

        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(pointsText).build(),
                    contentDescription = PlainComplicationText.Builder("Nasdaq 100: $pointsText, $percentText").build()
                )
                    .setTitle(PlainComplicationText.Builder(fullTitle).build())
                    .setMonochromaticImage(monoImage)
                    .setTapAction(pendingIntent)
                    .build()
            }

            ComplicationType.RANGED_VALUE -> {
                val min = quote.dayLow.toFloat()
                val max = quote.dayHigh.coerceAtLeast(quote.dayLow + 1.0).toFloat()
                val current = quote.price.toFloat().coerceIn(min, max)

                RangedValueComplicationData.Builder(
                    value = current,
                    min = min,
                    max = max,
                    contentDescription = PlainComplicationText.Builder("Nasdaq 100 Range: $pointsText").build()
                )
                    .setText(PlainComplicationText.Builder(percentText).build())
                    .setTitle(PlainComplicationText.Builder(fullTitle).build())
                    .setMonochromaticImage(monoImage)
                    .setTapAction(pendingIntent)
                    .build()
            }

            ComplicationType.LONG_TEXT -> {
                val summary = String.format(Locale.US, "Nasdaq 100: %,.2f (%s)", quote.price, percentText)
                LongTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(summary).build(),
                    contentDescription = PlainComplicationText.Builder(summary).build()
                )
                    .setTitle(PlainComplicationText.Builder("Tech Index").build())
                    .setMonochromaticImage(monoImage)
                    .setTapAction(pendingIntent)
                    .build()
            }

            ComplicationType.SMALL_IMAGE -> {
                val smallImage = SmallImage.Builder(icon, SmallImageType.ICON).build()
                SmallImageComplicationData.Builder(
                    smallImage = smallImage,
                    contentDescription = PlainComplicationText.Builder("Nasdaq: $percentText").build()
                )
                    .setTapAction(pendingIntent)
                    .build()
            }

            else -> null
        }
    }
}
