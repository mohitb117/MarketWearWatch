package com.marketwear.indices.data

/**
 * Re-export the shared model from the :shared module.
 */
typealias IndexQuote = com.marketwear.shared.model.IndexQuote

/**
 * Complication Display Format.
 */
enum class ComplicationFormat {
    POINTS,
    PERCENT_CHANGE,
    POINTS_AND_PCT,
    COMPACT
}
