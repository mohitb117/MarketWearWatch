package com.marketwear.indices

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.marketwear.indices.data.MarketRepository
import java.util.Locale

/**
 * Dual Market Complication: Combines both S&P 500 and Nasdaq 100 into a single slot.
 */
class MarketDualComplicationService : SuspendingComplicationDataSourceService() {

    private val repository by lazy { MarketRepository.getInstance(applicationContext) }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val spx = repository.getQuote("SPX")
        val ndx = repository.getQuote("NDX")

        val tapIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            1003,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val spxSign = if (spx.percentChange >= 0) "+" else ""
        val ndxSign = if (ndx.percentChange >= 0) "+" else ""

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                val text = String.format(Locale.US, "S%s%.1f%%", spxSign, spx.percentChange)
                val title = String.format(Locale.US, "N%s%.1f%%", ndxSign, ndx.percentChange)

                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(text).build(),
                    contentDescription = PlainComplicationText.Builder("S&P $text, Nasdaq $title").build()
                )
                    .setTitle(PlainComplicationText.Builder(title).build())
                    .setTapAction(pendingIntent)
                    .build()
            }

            ComplicationType.LONG_TEXT -> {
                val fullSummary = String.format(
                    Locale.US,
                    "SPX %s%.2f%% · NDX %s%.2f%%",
                    spxSign, spx.percentChange,
                    ndxSign, ndx.percentChange
                )
                LongTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(fullSummary).build(),
                    contentDescription = PlainComplicationText.Builder(fullSummary).build()
                )
                    .setTitle(PlainComplicationText.Builder("Market Watch").build())
                    .setTapAction(pendingIntent)
                    .build()
            }

            else -> null
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val tapIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            1003,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("S+0.7%").build(),
            contentDescription = PlainComplicationText.Builder("Preview Dual").build()
        )
            .setTitle(PlainComplicationText.Builder("N+1.1%").build())
            .setTapAction(pendingIntent)
            .build()
    }
}
