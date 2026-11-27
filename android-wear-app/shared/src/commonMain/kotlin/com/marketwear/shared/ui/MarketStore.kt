package com.marketwear.shared.ui

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.model.MarketRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Shared MVIKotlin [Store] contract implementing unidirectional
 * Intent -> CoroutineExecutor -> Message -> Reducer -> State flow for MarketWear surfaces.
 */
interface MarketStore : Store<MarketStore.Intent, MarketStore.State, MarketStore.Label> {

    sealed interface Intent {
        data class User(val intent: MarketIntent) : Intent
        data class ExternalQuotesUpdated(val quotes: List<IndexQuote>) : Intent
        data class ExternalDisabledHoldingsUpdated(val disabledHoldingIds: Set<String>) : Intent
        data class ExternalCustomWeightsUpdated(val customWeights: Map<String, Double>) : Intent
    }

    data class State(
        val quotes: List<IndexQuote> = emptyList(),
        val weightMode: PortfolioWeightMode = PortfolioWeightMode.NORMALIZED_100,
        val disabledHoldingIds: Set<String> = emptySet(),
        val customWeights: Map<String, Double> = emptyMap(),
        val refreshState: RefreshState = RefreshState.IDLE,
        val dashboard: MarketDashboardState = MarketUiStateBuilder.buildDashboardState(
            quotes = emptyList(),
            weightMode = PortfolioWeightMode.NORMALIZED_100,
            disabledHoldingIds = emptySet(),
            customWeights = emptyMap()
        ),
        val errorMessage: String? = null
    )

    sealed interface Label {
        data class ErrorOccurred(val message: String) : Label
    }
}

/**
 * MVIKotlin [StoreFactory] wrapper creating platform-agnostic [MarketStore] instances.
 */
class MarketStoreFactory(
    private val storeFactory: StoreFactory,
    private val mainContext: CoroutineContext = Dispatchers.Main.immediate
) {
    private sealed interface Message {
        data class QuotesChanged(val quotes: List<IndexQuote>) : Message
        data class WeightModeChanged(val mode: PortfolioWeightMode) : Message
        data class PortfolioConfigChanged(
            val disabledHoldingIds: Set<String>,
            val customWeights: Map<String, Double>,
            val updatedQuotes: List<IndexQuote>
        ) : Message
        data class RefreshStateChanged(
            val refreshState: RefreshState,
            val errorMessage: String? = null
        ) : Message
    }

    fun create(
        name: String = "MarketStore",
        initialQuotes: List<IndexQuote> = MarketRegistry.getFallbackQuotes(),
        initialDisabledHoldingIds: Set<String> = emptySet(),
        initialCustomWeights: Map<String, Double> = emptyMap(),
        initialWeightMode: PortfolioWeightMode = PortfolioWeightMode.NORMALIZED_100,
        onRecomputeLocalQuotes: (
            existingQuotes: List<IndexQuote>,
            disabledHoldingIds: Set<String>,
            customWeights: Map<String, Double>
        ) -> List<IndexQuote> = { existing, _, _ -> existing },
        onPersistPortfolioConfig: suspend (
            disabledHoldingIds: Set<String>,
            customWeights: Map<String, Double>,
            updatedQuotes: List<IndexQuote>
        ) -> Unit = { _, _, _ -> },
        onFetchQuotes: suspend (
            policy: RefreshPolicy,
            disabledHoldingIds: Set<String>,
            customWeights: Map<String, Double>
        ) -> List<IndexQuote>
    ): MarketStore {
        val initialSanitizedDisabled = MarketStateReducer.sanitizeDisabledHoldings(initialDisabledHoldingIds)
        val initialSanitizedWeights = MarketStateReducer.sanitizeCustomWeights(initialCustomWeights)
        val initialState = MarketStore.State(
            quotes = initialQuotes,
            weightMode = initialWeightMode,
            disabledHoldingIds = initialSanitizedDisabled,
            customWeights = initialSanitizedWeights,
            refreshState = RefreshState.IDLE,
            dashboard = MarketUiStateBuilder.buildDashboardState(
                quotes = initialQuotes,
                weightMode = initialWeightMode,
                disabledHoldingIds = initialSanitizedDisabled,
                customWeights = initialSanitizedWeights
            )
        )

        return object : MarketStore, Store<MarketStore.Intent, MarketStore.State, MarketStore.Label> by storeFactory.create(
            name = name,
            initialState = initialState,
            executorFactory = {
                ExecutorImpl(
                    mainContext = mainContext,
                    onRecomputeLocalQuotes = onRecomputeLocalQuotes,
                    onPersistPortfolioConfig = onPersistPortfolioConfig,
                    onFetchQuotes = onFetchQuotes
                )
            },
            reducer = ReducerImpl
        ) {}
    }

    private class ExecutorImpl(
        mainContext: CoroutineContext,
        private val onRecomputeLocalQuotes: (List<IndexQuote>, Set<String>, Map<String, Double>) -> List<IndexQuote>,
        private val onPersistPortfolioConfig: suspend (Set<String>, Map<String, Double>, List<IndexQuote>) -> Unit,
        private val onFetchQuotes: suspend (RefreshPolicy, Set<String>, Map<String, Double>) -> List<IndexQuote>
    ) : CoroutineExecutor<MarketStore.Intent, Nothing, MarketStore.State, Message, MarketStore.Label>(
        mainContext = mainContext
    ) {
        override fun executeIntent(intent: MarketStore.Intent) {
            val current = state()
            when (intent) {
                is MarketStore.Intent.User -> handleUserIntent(intent.intent)
                is MarketStore.Intent.ExternalQuotesUpdated -> {
                    val recomputed = onRecomputeLocalQuotes(
                        intent.quotes,
                        current.disabledHoldingIds,
                        current.customWeights
                    )
                    dispatch(Message.QuotesChanged(recomputed))
                }
                is MarketStore.Intent.ExternalDisabledHoldingsUpdated -> {
                    val sanitized = MarketStateReducer.sanitizeDisabledHoldings(intent.disabledHoldingIds)
                    val updatedQuotes = onRecomputeLocalQuotes(current.quotes, sanitized, current.customWeights)
                    dispatch(Message.PortfolioConfigChanged(sanitized, current.customWeights, updatedQuotes))
                }
                is MarketStore.Intent.ExternalCustomWeightsUpdated -> {
                    val sanitizedWeights = MarketStateReducer.sanitizeCustomWeights(intent.customWeights)
                    val updatedQuotes = onRecomputeLocalQuotes(
                        current.quotes,
                        current.disabledHoldingIds,
                        sanitizedWeights
                    )
                    dispatch(Message.PortfolioConfigChanged(current.disabledHoldingIds, sanitizedWeights, updatedQuotes))
                }
            }
        }

        private fun handleUserIntent(intent: MarketIntent) {
            val current = state()
            when (intent) {
                MarketIntent.ToggleWeightMode ->
                    dispatch(Message.WeightModeChanged(current.weightMode.next()))

                is MarketIntent.SetWeightMode ->
                    dispatch(Message.WeightModeChanged(intent.mode))

                is MarketIntent.ToggleHolding -> {
                    val nextDisabled = MarketStateReducer.reduceHoldingToggle(
                        current.disabledHoldingIds,
                        intent.holdingId
                    )
                    applyPortfolioConfig(nextDisabled, current.customWeights, current.quotes)
                }

                is MarketIntent.ApplyHoldingPreset -> {
                    val nextDisabled = MarketStateReducer.reduceHoldingPreset(intent.preset)
                    applyPortfolioConfig(nextDisabled, current.customWeights, current.quotes)
                }

                is MarketIntent.SetHoldingWeight -> {
                    val nextWeights = MarketStateReducer.reduceSetHoldingWeight(
                        current.customWeights,
                        intent.holdingId,
                        intent.weightPercent
                    )
                    applyPortfolioConfig(current.disabledHoldingIds, nextWeights, current.quotes)
                }

                is MarketIntent.StepHoldingWeight -> {
                    val nextWeights = MarketStateReducer.reduceStepHoldingWeight(
                        current.customWeights,
                        intent.holdingId,
                        intent.step
                    )
                    applyPortfolioConfig(current.disabledHoldingIds, nextWeights, current.quotes)
                }

                is MarketIntent.ResetWeights -> {
                    when (intent.action) {
                        WeightResetAction.RESTORE_DEFAULT_WEIGHTS ->
                            applyPortfolioConfig(current.disabledHoldingIds, emptyMap(), current.quotes)
                        WeightResetAction.RESET_ALL_TO_DEFAULTS ->
                            applyPortfolioConfig(emptySet(), emptyMap(), current.quotes)
                    }
                }

                is MarketIntent.RequestRefresh -> {
                    dispatch(Message.RefreshStateChanged(RefreshState.REFRESHING, null))
                    scope.launch {
                        try {
                            val latest = state()
                            val fetched = onFetchQuotes(
                                intent.policy,
                                latest.disabledHoldingIds,
                                latest.customWeights
                            )
                            if (fetched.isNotEmpty()) {
                                dispatch(Message.QuotesChanged(fetched))
                            }
                            dispatch(Message.RefreshStateChanged(RefreshState.IDLE, null))
                        } catch (e: Exception) {
                            val message = e.message ?: "Market refresh failed"
                            dispatch(Message.RefreshStateChanged(RefreshState.ERROR, message))
                            publish(MarketStore.Label.ErrorOccurred(message))
                        }
                    }
                }
            }
        }

        private fun applyPortfolioConfig(
            nextDisabled: Set<String>,
            nextWeights: Map<String, Double>,
            existingQuotes: List<IndexQuote>
        ) {
            val updatedQuotes = onRecomputeLocalQuotes(existingQuotes, nextDisabled, nextWeights)
            dispatch(Message.PortfolioConfigChanged(nextDisabled, nextWeights, updatedQuotes))
            scope.launch {
                onPersistPortfolioConfig(nextDisabled, nextWeights, updatedQuotes)
            }
        }
    }

    private object ReducerImpl : Reducer<MarketStore.State, Message> {
        override fun MarketStore.State.reduce(msg: Message): MarketStore.State = when (msg) {
            is Message.QuotesChanged -> copy(
                quotes = msg.quotes,
                dashboard = MarketUiStateBuilder.buildDashboardState(
                    quotes = msg.quotes,
                    weightMode = weightMode,
                    disabledHoldingIds = disabledHoldingIds,
                    customWeights = customWeights
                )
            )
            is Message.WeightModeChanged -> copy(
                weightMode = msg.mode,
                dashboard = MarketUiStateBuilder.buildDashboardState(
                    quotes = quotes,
                    weightMode = msg.mode,
                    disabledHoldingIds = disabledHoldingIds,
                    customWeights = customWeights
                )
            )
            is Message.PortfolioConfigChanged -> copy(
                disabledHoldingIds = msg.disabledHoldingIds,
                customWeights = msg.customWeights,
                quotes = msg.updatedQuotes,
                dashboard = MarketUiStateBuilder.buildDashboardState(
                    quotes = msg.updatedQuotes,
                    weightMode = weightMode,
                    disabledHoldingIds = msg.disabledHoldingIds,
                    customWeights = msg.customWeights
                )
            )
            is Message.RefreshStateChanged -> copy(
                refreshState = msg.refreshState,
                errorMessage = msg.errorMessage
            )
        }
    }
}
