package com.marketwear.mobile.ui

import android.app.Application
import androidx.glance.appwidget.updateAll
import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.marketwear.mobile.data.MobileMarketRepository
import com.marketwear.mobile.data.PortfolioSelectionStore
import com.marketwear.mobile.notification.PortfolioLiveNotificationManager
import com.marketwear.mobile.sync.WearDataSyncManager
import com.marketwear.mobile.widget.MarketGlanceWidget
import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.ui.LiveNotificationDeliveryState
import com.marketwear.shared.ui.LiveNotificationPreference
import com.marketwear.shared.ui.MarketIntent
import com.marketwear.shared.ui.MarketSessionStatus
import com.marketwear.shared.ui.MarketStateReducer
import com.marketwear.shared.ui.NotificationExpansionMode
import com.marketwear.shared.ui.PortfolioWeightMode
import com.marketwear.shared.ui.RefreshPolicy
import com.marketwear.shared.ui.RefreshState
import com.marketwear.shared.ui.SectorDisplayMode
import com.marketwear.shared.ui.SectorSortOrder
import com.marketwear.shared.ui.SyncOutcome
import com.marketwear.shared.ui.SyncUrgency
import com.marketwear.shared.ui.WatchConnectionState
import com.marketwear.shared.ui.WeightResetAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

interface MobileMarketStore : Store<MobileMarketIntent, MobileMarketUiState, Nothing>

/**
 * MVIKotlin [StoreFactory] wrapper implementing the Mobile Companion unidirectional MVI loop:
 * [MobileMarketIntent] -> [CoroutineExecutor] -> [Message] -> [Reducer] -> [MobileMarketUiState].
 */
class MobileMarketStoreFactory(
    private val storeFactory: StoreFactory,
    private val application: Application,
    private val repository: MobileMarketRepository,
    private val syncManager: WearDataSyncManager,
    private val selectionStore: PortfolioSelectionStore,
    private val liveNotificationManager: PortfolioLiveNotificationManager,
    private val mainContext: CoroutineContext = Dispatchers.Main.immediate
) {
    private sealed interface Action {
        data class InitialRefresh(val policy: RefreshPolicy) : Action
        data class ExternalPortfolioConfigChanged(
            val disabledHoldingIds: Set<String>,
            val customWeights: Map<String, Double>
        ) : Action
        data class ExternalNotificationStoreChanged(
            val weightMode: PortfolioWeightMode,
            val expansionMode: NotificationExpansionMode
        ) : Action
    }

    private sealed interface Message {
        data class CoreStateUpdated(
            val quotes: List<IndexQuote>,
            val weightMode: PortfolioWeightMode,
            val disabledHoldingIds: Set<String>,
            val customWeights: Map<String, Double>,
            val refreshState: RefreshState,
            val watchConnectionState: WatchConnectionState,
            val lastSyncedTimestamp: Long,
            val errorMessage: String?
        ) : Message

        data class NotificationStateUpdated(
            val preference: LiveNotificationPreference,
            val deliveryState: LiveNotificationDeliveryState,
            val expansionMode: NotificationExpansionMode
        ) : Message

        data class TabSelected(val tab: MobileAppTab) : Message
        data class SectorPreferencesUpdated(
            val displayMode: SectorDisplayMode,
            val sortOrder: SectorSortOrder
        ) : Message
    }

    fun create(): MobileMarketStore {
        val initialPref = selectionStore.liveNotificationPreference.value
        val initialQuotes = repository.quotes.value.ifEmpty { selectionStore.cachedQuotes.value }
        val initialState = MobileViewModel.buildUiState(
            quotes = initialQuotes,
            weightMode = selectionStore.weightMode.value,
            refreshState = RefreshState.IDLE,
            watchConnectionState = WatchConnectionState.DISCONNECTED,
            lastSyncedTimestamp = 0L,
            errorMessage = null,
            disabledHoldingIds = selectionStore.disabledHoldingIds.value,
            customWeights = selectionStore.customWeights.value,
            liveNotificationPreference = initialPref,
            liveNotificationState = liveNotificationManager.resolveDeliveryState(initialPref),
            notificationExpansionMode = selectionStore.notificationExpansionMode.value,
            activeTab = MobileAppTab.PULSE,
            sectorDisplayMode = SectorDisplayMode.HEATMAP,
            sectorSortOrder = SectorSortOrder.TOP_GAINERS
        )

        return object : MobileMarketStore, Store<MobileMarketIntent, MobileMarketUiState, Nothing> by storeFactory.create(
            name = "MobileMarketStore",
            initialState = initialState,
            bootstrapper = BootstrapperImpl(),
            executorFactory = { ExecutorImpl() },
            reducer = ReducerImpl
        ) {}
    }

    private inner class BootstrapperImpl : CoroutineBootstrapper<Action>(mainContext) {
        override fun invoke() {
            dispatch(Action.InitialRefresh(RefreshPolicy.CACHE_FIRST))
            scope.launch {
                combine(
                    selectionStore.disabledHoldingIds,
                    selectionStore.customWeights
                ) { disabledIds, weights ->
                    disabledIds to weights
                }.drop(1).collect { (disabledIds, weights) ->
                    dispatch(Action.ExternalPortfolioConfigChanged(disabledIds, weights))
                }
            }
            scope.launch {
                combine(
                    selectionStore.weightMode,
                    selectionStore.notificationExpansionMode
                ) { weightMode, expansionMode ->
                    weightMode to expansionMode
                }.drop(1).collect { (weightMode, expansionMode) ->
                    dispatch(Action.ExternalNotificationStoreChanged(weightMode, expansionMode))
                }
            }
        }
    }

    private inner class ExecutorImpl :
        CoroutineExecutor<MobileMarketIntent, Action, MobileMarketUiState, Message, Nothing>(
            mainContext = mainContext
        ) {

        override fun executeAction(action: Action) {
            when (action) {
                is Action.InitialRefresh -> handleRefresh(action.policy)
                is Action.ExternalPortfolioConfigChanged -> {
                    val recomputed = repository.recomputePortfolioForSelection(
                        disabledHoldingIds = action.disabledHoldingIds,
                        customWeights = action.customWeights
                    )
                    val current = state()
                    val deliveryState = liveNotificationManager.resolveDeliveryState(current.liveNotificationPreference)
                    dispatchUpdatedCore(
                        quotes = recomputed.ifEmpty { current.quotes },
                        disabledHoldingIds = action.disabledHoldingIds,
                        customWeights = action.customWeights,
                        deliveryState = deliveryState
                    )
                }
                is Action.ExternalNotificationStoreChanged -> {
                    val current = state()
                    val deliveryState = liveNotificationManager.resolveDeliveryState(current.liveNotificationPreference)
                    dispatch(
                        Message.NotificationStateUpdated(
                            preference = current.liveNotificationPreference,
                            deliveryState = deliveryState,
                            expansionMode = action.expansionMode
                        )
                    )
                    if (action.weightMode != current.portfolioState.weightMode) {
                        dispatchUpdatedCore(
                            weightMode = action.weightMode,
                            deliveryState = deliveryState
                        )
                    } else {
                        syncLiveNotification(state(), deliveryState)
                    }
                }
            }
        }

        override fun executeIntent(intent: MobileMarketIntent) {
            when (intent) {
                is MobileMarketIntent.Core -> handleCoreIntent(intent.intent)
                is MobileMarketIntent.SetLiveNotificationPreference -> {
                    selectionStore.setLiveNotificationPreference(intent.preference)
                    val deliveryState = liveNotificationManager.resolveDeliveryState(intent.preference)
                    dispatch(
                        Message.NotificationStateUpdated(
                            preference = intent.preference,
                            deliveryState = deliveryState,
                            expansionMode = state().notificationExpansionMode
                        )
                    )
                    when (intent.preference) {
                        LiveNotificationPreference.ACTIVE -> syncLiveNotification(state(), deliveryState)
                        LiveNotificationPreference.PAUSED -> liveNotificationManager.dismiss()
                    }
                }
                is MobileMarketIntent.SetNotificationExpansionMode -> {
                    selectionStore.setNotificationExpansionMode(intent.mode)
                    val current = state()
                    val deliveryState = liveNotificationManager.resolveDeliveryState(current.liveNotificationPreference)
                    dispatch(
                        Message.NotificationStateUpdated(
                            preference = current.liveNotificationPreference,
                            deliveryState = deliveryState,
                            expansionMode = intent.mode
                        )
                    )
                    syncLiveNotification(state(), deliveryState)
                }
                MobileMarketIntent.RefreshNotificationCapabilities -> {
                    val current = state()
                    val deliveryState = liveNotificationManager.resolveDeliveryState(current.liveNotificationPreference)
                    dispatch(
                        Message.NotificationStateUpdated(
                            preference = current.liveNotificationPreference,
                            deliveryState = deliveryState,
                            expansionMode = selectionStore.notificationExpansionMode.value
                        )
                    )
                    syncLiveNotification(state(), deliveryState)
                }
                is MobileMarketIntent.SelectTab ->
                    dispatch(Message.TabSelected(intent.tab))
                is MobileMarketIntent.SelectSectorDisplayMode ->
                    dispatch(Message.SectorPreferencesUpdated(intent.mode, state().sectorSortOrder))
                is MobileMarketIntent.SelectSectorSortOrder ->
                    dispatch(Message.SectorPreferencesUpdated(state().sectorDisplayMode, intent.order))
            }
        }

        private fun handleCoreIntent(intent: MarketIntent) {
            val current = state()
            val deliveryState = liveNotificationManager.resolveDeliveryState(current.liveNotificationPreference)
            when (intent) {
                MarketIntent.ToggleWeightMode -> {
                    dispatchUpdatedCore(
                        weightMode = current.portfolioState.weightMode.next(),
                        deliveryState = deliveryState
                    )
                }
                is MarketIntent.SetWeightMode -> {
                    dispatchUpdatedCore(
                        weightMode = intent.mode,
                        deliveryState = deliveryState
                    )
                }
                is MarketIntent.ToggleHolding -> {
                    val nextDisabled = selectionStore.toggleHolding(intent.holdingId)
                    applyPortfolioConfigAndSync(nextDisabled, selectionStore.customWeights.value, deliveryState)
                }
                is MarketIntent.ApplyHoldingPreset -> {
                    val nextDisabled = MarketStateReducer.reduceHoldingPreset(intent.preset)
                    selectionStore.setDisabledHoldings(nextDisabled)
                    applyPortfolioConfigAndSync(nextDisabled, selectionStore.customWeights.value, deliveryState)
                }
                is MarketIntent.SetHoldingWeight -> {
                    val nextWeights = MarketStateReducer.reduceSetHoldingWeight(
                        currentWeights = selectionStore.customWeights.value,
                        holdingId = intent.holdingId,
                        weightPercent = intent.weightPercent
                    )
                    selectionStore.setCustomWeights(nextWeights)
                    applyPortfolioConfigAndSync(selectionStore.disabledHoldingIds.value, nextWeights, deliveryState)
                }
                is MarketIntent.StepHoldingWeight -> {
                    val nextWeights = MarketStateReducer.reduceStepHoldingWeight(
                        currentWeights = selectionStore.customWeights.value,
                        holdingId = intent.holdingId,
                        step = intent.step
                    )
                    selectionStore.setCustomWeights(nextWeights)
                    applyPortfolioConfigAndSync(selectionStore.disabledHoldingIds.value, nextWeights, deliveryState)
                }
                is MarketIntent.ResetWeights -> {
                    val nextDisabled = when (intent.action) {
                        WeightResetAction.RESTORE_DEFAULT_WEIGHTS -> selectionStore.disabledHoldingIds.value
                        WeightResetAction.RESET_ALL_TO_DEFAULTS -> {
                            selectionStore.setDisabledHoldings(emptySet())
                            emptySet()
                        }
                    }
                    selectionStore.setCustomWeights(emptyMap())
                    applyPortfolioConfigAndSync(nextDisabled, emptyMap(), deliveryState)
                }
                is MarketIntent.RequestRefresh -> handleRefresh(intent.policy)
            }
        }

        private fun dispatchUpdatedCore(
            quotes: List<IndexQuote> = state().quotes,
            weightMode: PortfolioWeightMode = state().portfolioState.weightMode,
            disabledHoldingIds: Set<String> = state().disabledHoldingIds,
            customWeights: Map<String, Double> = state().customWeights,
            refreshState: RefreshState = state().refreshState,
            watchConnectionState: WatchConnectionState = state().watchConnectionState,
            lastSyncedTimestamp: Long = state().lastSyncedTimestamp,
            errorMessage: String? = state().errorMessage,
            deliveryState: LiveNotificationDeliveryState = state().liveNotificationState
        ) {
            val prevState = state()
            selectionStore.setWeightMode(weightMode)
            if (quotes.isNotEmpty()) {
                selectionStore.saveCachedQuotes(quotes)
            }
            dispatch(
                Message.CoreStateUpdated(
                    quotes = quotes,
                    weightMode = weightMode,
                    disabledHoldingIds = disabledHoldingIds,
                    customWeights = customWeights,
                    refreshState = refreshState,
                    watchConnectionState = watchConnectionState,
                    lastSyncedTimestamp = lastSyncedTimestamp,
                    errorMessage = errorMessage
                )
            )
            // Build preview of next state to push accurate notification spec immediately
            val nextState = MobileViewModel.buildUiState(
                quotes = quotes,
                weightMode = weightMode,
                refreshState = refreshState,
                watchConnectionState = watchConnectionState,
                lastSyncedTimestamp = lastSyncedTimestamp,
                errorMessage = errorMessage,
                disabledHoldingIds = disabledHoldingIds,
                customWeights = customWeights,
                liveNotificationPreference = prevState.liveNotificationPreference,
                liveNotificationState = deliveryState,
                notificationExpansionMode = prevState.notificationExpansionMode,
                activeTab = prevState.activeTab,
                sectorDisplayMode = prevState.sectorDisplayMode,
                sectorSortOrder = prevState.sectorSortOrder
            )
            syncLiveNotification(nextState, deliveryState)
            scope.launch(Dispatchers.IO) {
                try {
                    MarketGlanceWidget().updateAll(application)
                } catch (_: Exception) {
                    // Ignore if no widgets placed
                }
            }
        }

        private fun applyPortfolioConfigAndSync(
            disabledIds: Set<String>,
            customWeights: Map<String, Double>,
            deliveryState: LiveNotificationDeliveryState
        ) {
            val recomputedQuotes = repository.recomputePortfolioForSelection(
                disabledHoldingIds = disabledIds,
                customWeights = customWeights
            )
            dispatchUpdatedCore(
                quotes = recomputedQuotes.ifEmpty { state().quotes },
                disabledHoldingIds = disabledIds,
                customWeights = customWeights,
                deliveryState = deliveryState
            )
            scope.launch(Dispatchers.IO) {
                syncManager.syncDisabledHoldingsToWatch(disabledIds, customWeights)
                val currentQuotes = repository.quotes.value
                if (currentQuotes.isNotEmpty()) {
                    syncManager.syncQuotesToWatch(
                        quotes = currentQuotes,
                        urgency = SyncUrgency.IMMEDIATE,
                        intradayData = repository.getCachedPortfolioTickerData()
                    )
                }
                try {
                    MarketGlanceWidget().updateAll(application)
                } catch (_: Exception) {
                    // Ignore if no widgets placed
                }
            }
        }

        private fun handleRefresh(policy: RefreshPolicy) {
            dispatch(
                Message.CoreStateUpdated(
                    quotes = state().quotes,
                    weightMode = state().portfolioState.weightMode,
                    disabledHoldingIds = state().disabledHoldingIds,
                    customWeights = state().customWeights,
                    refreshState = RefreshState.REFRESHING,
                    watchConnectionState = state().watchConnectionState,
                    lastSyncedTimestamp = state().lastSyncedTimestamp,
                    errorMessage = null
                )
            )

            scope.launch {
                try {
                    val disabledIds = selectionStore.disabledHoldingIds.value
                    val customWeights = selectionStore.customWeights.value
                    val quotes = repository.fetchAllQuotes(
                        policy = policy,
                        disabledHoldingIds = disabledIds,
                        customWeights = customWeights
                    )
                    val deliveryState = liveNotificationManager.resolveDeliveryState(
                        state().liveNotificationPreference
                    )
                    dispatchUpdatedCore(
                        quotes = quotes,
                        disabledHoldingIds = disabledIds,
                        customWeights = customWeights,
                        refreshState = RefreshState.REFRESHING,
                        errorMessage = null,
                        deliveryState = deliveryState
                    )

                    try {
                        MarketGlanceWidget().updateAll(application)
                    } catch (_: Exception) {
                        // Ignore widget update errors if no widgets placed
                    }

                    val syncOutcome = syncManager.syncQuotesToWatch(
                        quotes = quotes,
                        urgency = SyncUrgency.IMMEDIATE,
                        intradayData = repository.getCachedPortfolioTickerData()
                    )
                    val connectionState = syncManager.getWatchConnectionState()
                    val latestState = state()
                    val newTimestamp = when (syncOutcome) {
                        SyncOutcome.SYNCED -> System.currentTimeMillis()
                        SyncOutcome.FAILED -> latestState.lastSyncedTimestamp
                    }
                    dispatchUpdatedCore(
                        quotes = latestState.quotes,
                        refreshState = RefreshState.IDLE,
                        watchConnectionState = connectionState,
                        lastSyncedTimestamp = newTimestamp,
                        errorMessage = when (syncOutcome) {
                            SyncOutcome.SYNCED -> null
                            SyncOutcome.FAILED -> "Watch sync failed (watch may be offline)"
                        },
                        deliveryState = deliveryState
                    )
                } catch (e: Exception) {
                    val latest = state()
                    dispatch(
                        Message.CoreStateUpdated(
                            quotes = latest.quotes,
                            weightMode = latest.portfolioState.weightMode,
                            disabledHoldingIds = latest.disabledHoldingIds,
                            customWeights = latest.customWeights,
                            refreshState = RefreshState.ERROR,
                            watchConnectionState = latest.watchConnectionState,
                            lastSyncedTimestamp = latest.lastSyncedTimestamp,
                            errorMessage = e.message ?: "Failed to fetch market quotes"
                        )
                    )
                }
            }
        }

        private fun syncLiveNotification(
            targetState: MobileMarketUiState,
            deliveryState: LiveNotificationDeliveryState
        ) {
            when (deliveryState) {
                LiveNotificationDeliveryState.PROMOTED_LIVE_UPDATE,
                LiveNotificationDeliveryState.STANDARD_ONGOING_NEEDS_OPT_IN ->
                    liveNotificationManager.showOrUpdate(
                        spec = targetState.portfolioState.liveUpdateSpec,
                        expansionMode = targetState.notificationExpansionMode
                    )
                LiveNotificationDeliveryState.PERMISSION_REQUIRED,
                LiveNotificationDeliveryState.PAUSED_BY_USER -> Unit
            }
        }
    }

    private object ReducerImpl : Reducer<MobileMarketUiState, Message> {
        override fun MobileMarketUiState.reduce(msg: Message): MobileMarketUiState = when (msg) {
            is Message.CoreStateUpdated -> MobileViewModel.buildUiState(
                quotes = msg.quotes,
                weightMode = msg.weightMode,
                refreshState = msg.refreshState,
                watchConnectionState = msg.watchConnectionState,
                lastSyncedTimestamp = msg.lastSyncedTimestamp,
                errorMessage = msg.errorMessage,
                disabledHoldingIds = msg.disabledHoldingIds,
                customWeights = msg.customWeights,
                liveNotificationPreference = liveNotificationPreference,
                liveNotificationState = liveNotificationState,
                notificationExpansionMode = notificationExpansionMode,
                activeTab = activeTab,
                sectorDisplayMode = sectorDisplayMode,
                sectorSortOrder = sectorSortOrder
            )
            is Message.NotificationStateUpdated -> copy(
                liveNotificationPreference = msg.preference,
                liveNotificationState = msg.deliveryState,
                notificationExpansionMode = msg.expansionMode
            )
            is Message.TabSelected -> copy(activeTab = msg.tab)
            is Message.SectorPreferencesUpdated -> copy(
                sectorDisplayMode = msg.displayMode,
                sectorSortOrder = msg.sortOrder
            )
        }
    }
}
