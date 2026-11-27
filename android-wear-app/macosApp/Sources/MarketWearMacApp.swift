import AppKit
import Combine
import SwiftUI
import WidgetKit

enum IslandSurfaceMode {
    case collapsedNotch
    case expandedIsland
    case menuBarPopover
}

enum IslandActionCode: String {
    case toggleMode = "TOGGLE_MODE"
    case presetAll37 = "PRESET_ALL_37"
    case presetTop10 = "PRESET_TOP_10"
    case resetWeights = "RESET_WEIGHTS"
    case refreshLive = "REFRESH"
    case scenarioDefault = "SCENARIO_DEFAULT"
    case scenarioBull = "SCENARIO_BULL"
    case scenarioBear = "SCENARIO_BEAR"
    case quitApp = "QUIT_APP"
}

enum BridgeConnectionStatus: String {
    case liveLoopback = "KMP SYNC"
    case standaloneLive = "YAHOO SYNC"
    case localCache = "LOCAL SIM"
}

enum IslandPinState {
    case autoHover
    case pinnedExpanded
}

@MainActor
final class MarketWearMacViewModel: ObservableObject {
    @Published private(set) var dashboard: KmpDashboardState = .loadLatestOrFallback()
    @Published private(set) var connectionStatus: BridgeConnectionStatus = .localCache
    @Published var scrubSelection: ScrubSelection = .live
    @Published var islandMode: IslandSurfaceMode = .collapsedNotch
    @Published var pinState: IslandPinState = .autoHover
    @Published var notchMetrics: NotchMetrics = .detect()
    @Published private(set) var isSyncing: Bool = false

    private var timerCancellable: AnyCancellable?
    private let baseURL = URL(string: "http://127.0.0.1:49172")!

    init() {
        Task { await pollLatestState() }
        timerCancellable = Timer.publish(every: 0.75, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                Task { await self?.pollLatestState() }
            }
    }

    func refreshNotchMetrics() {
        let updated = NotchMetrics.detect()
        if updated != notchMetrics {
            notchMetrics = updated
        }
    }

    func sendIntent(_ action: IslandActionCode) {
        if action == .quitApp {
            NSApplication.shared.terminate(nil)
            return
        }
        Task {
            if action == .refreshLive {
                isSyncing = true
            }
            let url = baseURL.appending(path: "intent").appending(queryItems: [URLQueryItem(name: "code", value: action.rawValue)])
            var req = URLRequest(url: url)
            req.timeoutInterval = 6.0
            if let (data, _) = try? await URLSession.shared.data(for: req),
               let decoded = try? JSONDecoder().decode(KmpDashboardState.self, from: data) {
                dashboard = decoded
                connectionStatus = .liveLoopback
                saveToDisk(decoded)
            } else {
                await handleOfflineIntent(action)
            }
            if action == .refreshLive {
                isSyncing = false
            }
        }
    }

    private func handleOfflineIntent(_ action: IslandActionCode) async {
        if action == .refreshLive {
            if let liveState = await fetchDirectLiveState() {
                dashboard = liveState
                connectionStatus = .standaloneLive
                saveToDisk(liveState)
                return
            }
        }
        let simulated = simulateLocalAction(action, current: dashboard)
        dashboard = simulated
        connectionStatus = .localCache
        saveToDisk(simulated)
    }

    private func saveToDisk(_ state: KmpDashboardState) {
        if let encoded = try? JSONEncoder().encode(state) {
            try? encoded.write(to: URL(fileURLWithPath: "/tmp/marketwear_island_state.json"))
            WidgetCenter.shared.reloadAllTimelines()
        }
    }

    private func fetchDirectLiveState() async -> KmpDashboardState? {
        guard let url = URL(string: "https://query1.finance.yahoo.com/v8/finance/chart/SPY?interval=15m&range=1d") else { return nil }
        var req = URLRequest(url: url)
        req.setValue("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36", forHTTPHeaderField: "User-Agent")
        req.timeoutInterval = 4.0
        guard let (data, resp) = try? await URLSession.shared.data(for: req),
              (resp as? HTTPURLResponse)?.statusCode == 200,
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let chart = json["chart"] as? [String: Any],
              let result = (chart["result"] as? [[String: Any]])?.first,
              let meta = result["meta"] as? [String: Any],
              let regularPrice = meta["regularMarketPrice"] as? Double,
              let previousClose = (meta["previousClose"] as? Double) ?? (meta["chartPreviousClose"] as? Double),
              previousClose > 0 else {
            return nil
        }

        let indicators = result["indicators"] as? [String: Any]
        let quoteList = indicators?["quote"] as? [[String: Any]]
        let rawCloses = (quoteList?.first?["close"] as? [Any])?.compactMap { $0 as? Double } ?? [regularPrice]
        let cleanCloses = rawCloses.isEmpty ? [regularPrice] : rawCloses

        let pctChange = ((regularPrice - previousClose) / previousClose) * 100.0
        let score = 100.0 + pctChange
        let sparkline = cleanCloses.map { 100.0 + ((($0 - previousClose) / previousClose) * 100.0) }
        let now = Int64(Date().timeIntervalSince1970 * 1000)

        let isPositive = pctChange >= 0
        let accentArgb: Int32 = isPositive ? Int32(bitPattern: 0xFF10B981) : Int32(bitPattern: 0xFFEF4444)
        let prog = min(max(Int(500 + (pctChange * 50)), 0), 1000)
        let segLen = abs(prog - 500)

        let pQuote = KmpIndexQuote(
            symbol: "PORTFOLIO",
            name: "Portfolio Score",
            price: round(score * 100) / 100,
            change: round(pctChange * 100) / 100,
            percentChange: round(pctChange * 100) / 100,
            dayHigh: sparkline.max() ?? score,
            dayLow: sparkline.min() ?? score,
            lastUpdated: now,
            category: "PORTFOLIO",
            sparkline: sparkline
        )

        let rawQuote = KmpIndexQuote(
            symbol: "PORTFOLIO",
            name: "Portfolio Score",
            price: round((100.0 + (pctChange * 0.5941)) * 100) / 100,
            change: round((pctChange * 0.5941) * 100) / 100,
            percentChange: round((pctChange * 0.5941) * 100) / 100,
            dayHigh: 100.0 + (((sparkline.max() ?? score) - 100.0) * 0.5941),
            dayLow: 100.0 + (((sparkline.min() ?? score) - 100.0) * 0.5941),
            lastUpdated: now,
            category: "PORTFOLIO",
            sparkline: sparkline.map { 100.0 + (($0 - 100.0) * 0.5941) }
        )

        let card = KmpPortfolioCardState(
            weightMode: dashboard.portfolioState.weightMode,
            activeQuote: dashboard.portfolioState.weightMode == "RAW" ? rawQuote : pQuote,
            secondaryQuote: dashboard.portfolioState.weightMode == "RAW" ? pQuote : rawQuote,
            trendDirection: isPositive ? "POSITIVE" : "NEGATIVE",
            watchTitleLabel: dashboard.portfolioState.weightMode == "RAW" ? "Portfolio (Raw)" : "Portfolio (100%)",
            mobileModePillLabel: dashboard.portfolioState.weightMode == "RAW" ? "Raw (59.41%)" : "Normalized (100%)",
            mobileSecondarySummary: String(format: "Raw (59.41%%): %.2f (%+.2f%%)", rawQuote.price, rawQuote.percentChange),
            mobileRangeSummary: String(format: "Range: %.2f – %.2f", sparkline.min() ?? score, sparkline.max() ?? score),
            primarySparkline: sparkline,
            secondarySparkline: rawQuote.sparkline,
            enabledHoldingsCount: dashboard.portfolioState.enabledHoldingsCount,
            totalHoldingsCount: 37,
            liveUpdateSpec: KmpLiveUpdateSpec(
                progress: prog,
                maxProgress: 1000,
                baselinePoint: 500,
                trendDirection: isPositive ? "POSITIVE" : "NEGATIVE",
                accentArgb: accentArgb,
                segments: [
                    KmpBipolarSegment(length: min(prog, 500), argbColor: Int32(bitPattern: 0xFF141C27)),
                    KmpBipolarSegment(length: segLen, argbColor: accentArgb),
                    KmpBipolarSegment(length: max(0, 1000 - max(prog, 500) - (prog < 500 ? segLen : 0)), argbColor: Int32(bitPattern: 0xFF141C27))
                ],
                shortCriticalText: String(format: "%+.2f%%", pctChange),
                title: String(format: "Portfolio %.2f (%+.2f%%)", score, pctChange),
                subtitle: "\(dashboard.portfolioState.enabledHoldingsCount)/37 holdings · Yahoo Finance Live"
            )
        )

        let spx = KmpIndexQuote(
            symbol: "SPX",
            name: "S&P 500",
            price: round(regularPrice * 1000) / 100,
            change: round((regularPrice - previousClose) * 1000) / 100,
            percentChange: round(pctChange * 100) / 100,
            dayHigh: round(regularPrice * 1.005 * 1000) / 100,
            dayLow: round(regularPrice * 0.995 * 1000) / 100,
            lastUpdated: now,
            category: "INDEX",
            sparkline: cleanCloses
        )

        return KmpDashboardState(
            portfolioState: card,
            indices: [spx] + dashboard.indices.dropFirst(),
            sectors: dashboard.sectors,
            latestUpdatedMillis: now
        )
    }

    private func simulateLocalAction(_ action: IslandActionCode, current: KmpDashboardState) -> KmpDashboardState {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        var p = current.portfolioState
        let indices = current.indices
        let sectors = current.sectors

        switch action {
        case .toggleMode:
            let isNowRaw = p.weightMode == "NORMALIZED_100"
            let newMode = isNowRaw ? "RAW" : "NORMALIZED_100"
            let active = isNowRaw ? p.secondaryQuote : p.activeQuote
            let secondary = isNowRaw ? p.activeQuote : p.secondaryQuote
            p = KmpPortfolioCardState(
                weightMode: newMode,
                activeQuote: active,
                secondaryQuote: secondary,
                trendDirection: active.percentChange >= 0 ? "POSITIVE" : "NEGATIVE",
                watchTitleLabel: isNowRaw ? "Portfolio (Raw)" : "Portfolio (100%)",
                mobileModePillLabel: isNowRaw ? "Raw (59.41%)" : "Normalized (100%)",
                mobileSecondarySummary: isNowRaw ? String(format: "Normalized (100%%): %.2f (%+.2f%%)", secondary.price, secondary.percentChange) : String(format: "Raw (59.41%%): %.2f (%+.2f%%)", secondary.price, secondary.percentChange),
                mobileRangeSummary: p.mobileRangeSummary,
                primarySparkline: isNowRaw ? p.secondarySparkline : p.primarySparkline,
                secondarySparkline: isNowRaw ? p.primarySparkline : p.secondarySparkline,
                enabledHoldingsCount: p.enabledHoldingsCount,
                totalHoldingsCount: p.totalHoldingsCount,
                liveUpdateSpec: p.liveUpdateSpec
            )

        case .scenarioBull:
            let series = [100.0, 100.28, 100.62, 100.95, 101.24, 101.58, 101.72, 101.85]
            let q = KmpIndexQuote(symbol: "PORTFOLIO", name: "Portfolio Score", price: 101.85, change: 1.85, percentChange: 1.85, dayHigh: 101.85, dayLow: 100.0, lastUpdated: now, category: "PORTFOLIO", sparkline: series)
            p = KmpPortfolioCardState(
                weightMode: p.weightMode,
                activeQuote: q,
                secondaryQuote: KmpIndexQuote(symbol: "PORTFOLIO", name: "Portfolio Score", price: 101.10, change: 1.10, percentChange: 1.10, dayHigh: 101.10, dayLow: 100.0, lastUpdated: now, category: "PORTFOLIO", sparkline: series),
                trendDirection: "POSITIVE",
                watchTitleLabel: p.watchTitleLabel,
                mobileModePillLabel: p.mobileModePillLabel,
                mobileSecondarySummary: "Raw (59.41%): 101.10 (+1.10%)",
                mobileRangeSummary: "Range: 100.00 – 101.85",
                primarySparkline: series,
                secondarySparkline: series,
                enabledHoldingsCount: p.enabledHoldingsCount,
                totalHoldingsCount: p.totalHoldingsCount,
                liveUpdateSpec: KmpLiveUpdateSpec(
                    progress: 685, maxProgress: 1000, baselinePoint: 500, trendDirection: "POSITIVE",
                    accentArgb: Int32(bitPattern: 0xFF10B981),
                    segments: [
                        KmpBipolarSegment(length: 500, argbColor: Int32(bitPattern: 0xFF141C27)),
                        KmpBipolarSegment(length: 185, argbColor: Int32(bitPattern: 0xFF10B981)),
                        KmpBipolarSegment(length: 315, argbColor: Int32(bitPattern: 0xFF141C27))
                    ],
                    shortCriticalText: "+1.85%",
                    title: "Portfolio 101.85 (+1.85%)",
                    subtitle: "\(p.enabledHoldingsCount)/37 holdings · Bull Rally"
                )
            )

        case .scenarioBear:
            let series = [100.0, 99.82, 99.54, 99.21, 98.96, 98.78, 98.55, 98.66]
            let q = KmpIndexQuote(symbol: "PORTFOLIO", name: "Portfolio Score", price: 98.66, change: -1.34, percentChange: -1.34, dayHigh: 100.0, dayLow: 98.55, lastUpdated: now, category: "PORTFOLIO", sparkline: series)
            p = KmpPortfolioCardState(
                weightMode: p.weightMode,
                activeQuote: q,
                secondaryQuote: KmpIndexQuote(symbol: "PORTFOLIO", name: "Portfolio Score", price: 99.20, change: -0.80, percentChange: -0.80, dayHigh: 100.0, dayLow: 99.10, lastUpdated: now, category: "PORTFOLIO", sparkline: series),
                trendDirection: "NEGATIVE",
                watchTitleLabel: p.watchTitleLabel,
                mobileModePillLabel: p.mobileModePillLabel,
                mobileSecondarySummary: "Raw (59.41%): 99.20 (-0.80%)",
                mobileRangeSummary: "Range: 98.55 – 100.00",
                primarySparkline: series,
                secondarySparkline: series,
                enabledHoldingsCount: p.enabledHoldingsCount,
                totalHoldingsCount: p.totalHoldingsCount,
                liveUpdateSpec: KmpLiveUpdateSpec(
                    progress: 366, maxProgress: 1000, baselinePoint: 500, trendDirection: "NEGATIVE",
                    accentArgb: Int32(bitPattern: 0xFFEF4444),
                    segments: [
                        KmpBipolarSegment(length: 366, argbColor: Int32(bitPattern: 0xFF141C27)),
                        KmpBipolarSegment(length: 134, argbColor: Int32(bitPattern: 0xFFEF4444)),
                        KmpBipolarSegment(length: 500, argbColor: Int32(bitPattern: 0xFF141C27))
                    ],
                    shortCriticalText: "-1.34%",
                    title: "Portfolio 98.66 (-1.34%)",
                    subtitle: "\(p.enabledHoldingsCount)/37 holdings · Bear Pullback"
                )
            )

        case .scenarioDefault, .resetWeights:
            return KmpDashboardState.fallback

        case .presetTop10:
            p = KmpPortfolioCardState(
                weightMode: p.weightMode,
                activeQuote: p.activeQuote,
                secondaryQuote: p.secondaryQuote,
                trendDirection: p.trendDirection,
                watchTitleLabel: p.watchTitleLabel,
                mobileModePillLabel: p.mobileModePillLabel,
                mobileSecondarySummary: p.mobileSecondarySummary,
                mobileRangeSummary: p.mobileRangeSummary,
                primarySparkline: p.primarySparkline,
                secondarySparkline: p.secondarySparkline,
                enabledHoldingsCount: 10,
                totalHoldingsCount: 37,
                liveUpdateSpec: p.liveUpdateSpec
            )

        case .presetAll37:
            p = KmpPortfolioCardState(
                weightMode: p.weightMode,
                activeQuote: p.activeQuote,
                secondaryQuote: p.secondaryQuote,
                trendDirection: p.trendDirection,
                watchTitleLabel: p.watchTitleLabel,
                mobileModePillLabel: p.mobileModePillLabel,
                mobileSecondarySummary: p.mobileSecondarySummary,
                mobileRangeSummary: p.mobileRangeSummary,
                primarySparkline: p.primarySparkline,
                secondarySparkline: p.secondarySparkline,
                enabledHoldingsCount: 37,
                totalHoldingsCount: 37,
                liveUpdateSpec: p.liveUpdateSpec
            )

        case .refreshLive, .quitApp:
            break
        }

        return KmpDashboardState(
            portfolioState: p,
            indices: indices,
            sectors: sectors,
            latestUpdatedMillis: now
        )
    }

    private func pollLatestState() async {
        var req = URLRequest(url: baseURL.appending(path: "state"))
        req.timeoutInterval = 2.0
        if let (data, _) = try? await URLSession.shared.data(for: req),
           let decoded = try? JSONDecoder().decode(KmpDashboardState.self, from: data) {
            dashboard = decoded
            connectionStatus = .liveLoopback
            saveToDisk(decoded)
        } else {
            let cached = KmpDashboardState.loadLatestOrFallback()
            if cached.latestUpdatedMillis != dashboard.latestUpdatedMillis {
                dashboard = cached
            }
        }
    }
}

struct DynamicIslandDeskView: View {
    @ObservedObject var vm: MarketWearMacViewModel
    let mode: IslandSurfaceMode
    var onTogglePin: (() -> Void)? = nil

    private static let collapsedEarWidth: CGFloat = 122
    private static let expandedBodyHeight: CGFloat = 304

    var body: some View {
        let p = vm.dashboard.portfolioState
        let metrics = vm.notchMetrics
        let series = p.primarySparkline
        let activePrice: Double = {
            if case .point(let idx) = vm.scrubSelection, !series.isEmpty {
                return series[min(max(idx, 0), series.count - 1)]
            }
            return p.activeQuote.price
        }()
        let activePct = activePrice - 100.0
        let isPositive = activePct >= 0

        switch mode {
        case .collapsedNotch:
            collapsedView(p: p, metrics: metrics, activePrice: activePrice, activePct: activePct, isPositive: isPositive)
        case .expandedIsland:
            let shell = NotchIslandShape(shoulderRadius: metrics.shoulderRadius, bottomRadius: 24)
            VStack(spacing: 0) {
                expandedNotchHeader(p: p, metrics: metrics, isPositive: isPositive)
                expandedTelemetryBody(p: p, activePrice: activePrice, activePct: activePct, isPositive: isPositive)
                    .padding(.horizontal, metrics.shoulderRadius + 14)
                    .padding(.top, 8)
                    .padding(.bottom, 14)
                    .background(
                        ObsidianTheme.background,
                        in: RoundedRectangle(cornerRadius: 18)
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 18)
                            .stroke(ObsidianTheme.outlineVariant, lineWidth: 1)
                    )
                    .padding(.horizontal, metrics.shoulderRadius + 6)
                    .padding(.bottom, 8)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(ObsidianTheme.notchHardwareBlack, in: shell)
            .overlay(shell.stroke(ObsidianTheme.outlineVariant.opacity(0.45), lineWidth: 1))
            .clipShape(shell)
            .contextMenu {
                Button("Collapse Island") { onTogglePin?() }
                Button("Sync Live Data") { vm.sendIntent(.refreshLive) }
                Divider()
                Button("Quit MarketWear Island") { NSApplication.shared.terminate(nil) }
            }

        case .menuBarPopover:
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Portfolio Daily Score")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundStyle(ObsidianTheme.onSurface)
                        Text("\(p.enabledHoldingsCount)/\(p.totalHoldingsCount) Holdings · \(vm.connectionStatus.rawValue)")
                            .font(ObsidianTheme.microLabel)
                            .foregroundStyle(ObsidianTheme.onSurfaceVariant)
                    }
                    Spacer()
                    modeToggleButton(p: p)
                    quitButton()
                }
                expandedTelemetryBody(p: p, activePrice: activePrice, activePct: activePct, isPositive: isPositive)
            }
            .padding(14)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(ObsidianTheme.background, in: RoundedRectangle(cornerRadius: 22))
            .overlay(RoundedRectangle(cornerRadius: 22).stroke(ObsidianTheme.outlineVariant, lineWidth: 1))
        }
    }

    @ViewBuilder
    private func collapsedView(
        p: KmpPortfolioCardState,
        metrics: NotchMetrics,
        activePrice: Double,
        activePct: Double,
        isPositive: Bool
    ) -> some View {
        let shell = NotchIslandShape(shoulderRadius: metrics.shoulderRadius, bottomRadius: 14)
        Group {
            if metrics.presence == .hardwareNotch {
                // Symmetric left/right ears keep the physical MacBook notch dead-center at screen midX
                HStack(spacing: 0) {
                    HStack(spacing: 6) {
                        Circle()
                            .fill(isPositive ? ObsidianTheme.positive : ObsidianTheme.negative)
                            .frame(width: 6, height: 6)
                        Text("PORT")
                            .font(ObsidianTheme.microLabel)
                            .foregroundStyle(ObsidianTheme.primary)
                        Text(String(format: "%.2f", activePrice))
                            .font(ObsidianTheme.tabularSmall)
                            .monospacedDigit()
                            .foregroundStyle(ObsidianTheme.onSurface)
                    }
                    .frame(width: Self.collapsedEarWidth, alignment: .trailing)
                    .padding(.trailing, 4)

                    Color.clear.frame(width: metrics.notchWidth)

                    HStack(spacing: 6) {
                        BipolarLogGaugeView(spec: p.liveUpdateSpec, height: 5, scaleVisibility: .hidden)
                            .frame(width: 38)
                        DeltaPill(text: String(format: "%+.2f%%", activePct), trend: SignalTrend(delta: activePct))
                    }
                    .frame(width: Self.collapsedEarWidth, alignment: .leading)
                    .padding(.leading, 4)
                }
            } else {
                HStack(spacing: 8) {
                    Circle()
                        .fill(isPositive ? ObsidianTheme.positive : ObsidianTheme.negative)
                        .frame(width: 6, height: 6)
                    Text("PORT")
                        .font(ObsidianTheme.microLabel)
                        .foregroundStyle(ObsidianTheme.primary)
                    Text(String(format: "%.2f", activePrice))
                        .font(ObsidianTheme.tabularSmall)
                        .monospacedDigit()
                        .foregroundStyle(ObsidianTheme.onSurface)
                    BipolarLogGaugeView(spec: p.liveUpdateSpec, height: 5, scaleVisibility: .hidden)
                        .frame(width: 52)
                    DeltaPill(text: String(format: "%+.2f%%", activePct), trend: SignalTrend(delta: activePct))
                }
                .padding(.horizontal, metrics.shoulderRadius + 10)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(ObsidianTheme.notchHardwareBlack, in: shell)
        .overlay(shell.stroke(ObsidianTheme.outlineVariant.opacity(0.4), lineWidth: 0.75))
        .clipShape(shell)
        .contentShape(shell)
        .onTapGesture {
            onTogglePin?()
        }
        .contextMenu {
            Button("Toggle Pin") { onTogglePin?() }
            Button(p.weightMode == "NORMALIZED_100" ? "Switch to Raw Mode" : "Switch to 100% Mode") {
                vm.sendIntent(.toggleMode)
            }
            Button("Sync Live Data") { vm.sendIntent(.refreshLive) }
            Divider()
            Button("Quit MarketWear Island") { NSApplication.shared.terminate(nil) }
        }
    }

    private func expandedNotchHeader(
        p: KmpPortfolioCardState,
        metrics: NotchMetrics,
        isPositive: Bool
    ) -> some View {
        let sideEarWidth: CGFloat = (metrics.presence == .hardwareNotch) ? 154 : 0
        return Group {
            if metrics.presence == .hardwareNotch {
                HStack(spacing: 0) {
                    HStack(spacing: 6) {
                        Circle()
                            .fill(isPositive ? ObsidianTheme.positive : ObsidianTheme.negative)
                            .frame(width: 6, height: 6)
                        Text("MarketWear")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundStyle(ObsidianTheme.onSurface)
                        Text("\(p.enabledHoldingsCount)/\(p.totalHoldingsCount)")
                            .font(ObsidianTheme.microLabel)
                            .foregroundStyle(ObsidianTheme.onSurfaceVariant)
                    }
                    .frame(width: sideEarWidth, alignment: .leading)

                    Color.clear.frame(width: metrics.notchWidth)

                    HStack(spacing: 6) {
                        modeToggleButton(p: p)
                        pinBadgeButton()
                        quitButton()
                    }
                    .frame(width: sideEarWidth, alignment: .trailing)
                }
            } else {
                HStack {
                    HStack(spacing: 6) {
                        Circle()
                            .fill(isPositive ? ObsidianTheme.positive : ObsidianTheme.negative)
                            .frame(width: 6, height: 6)
                        Text("MarketWear · \(p.enabledHoldingsCount)/\(p.totalHoldingsCount) · \(vm.connectionStatus.rawValue)")
                            .font(ObsidianTheme.microLabel)
                            .foregroundStyle(ObsidianTheme.onSurfaceVariant)
                    }
                    Spacer()
                    modeToggleButton(p: p)
                    pinBadgeButton()
                    quitButton()
                }
                .padding(.horizontal, metrics.shoulderRadius + 12)
            }
        }
        .frame(height: metrics.notchHeight)
    }

    private func expandedTelemetryBody(
        p: KmpPortfolioCardState,
        activePrice: Double,
        activePct: Double,
        isPositive: Bool
    ) -> some View {
        VStack(alignment: .leading, spacing: 9) {
            HStack(alignment: .firstTextBaseline) {
                Text(String(format: "%.2f", activePrice))
                    .font(ObsidianTheme.tabularHero)
                    .monospacedDigit()
                    .foregroundStyle(ObsidianTheme.onSurface)
                DeltaPill(text: String(format: "%+.2f (%+.2f%%)", activePct, activePct), trend: SignalTrend(delta: activePct))
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text(p.mobileSecondarySummary)
                    Text(p.mobileRangeSummary)
                }
                .font(ObsidianTheme.microLabel)
                .monospacedDigit()
                .foregroundStyle(ObsidianTheme.onSurfaceVariant)
            }

            BipolarLogGaugeView(spec: p.liveUpdateSpec, height: 7, scaleVisibility: .showTicks)

            InteractivePortfolioIslandChart(portfolio: p, scrub: $vm.scrubSelection)
                .frame(height: 112)

            HStack(spacing: 8) {
                ForEach(vm.dashboard.indices.prefix(2)) { idxQuote in
                    HStack(spacing: 6) {
                        Text(idxQuote.symbol)
                            .font(ObsidianTheme.microLabel)
                            .foregroundStyle(ObsidianTheme.primary)
                        Text(idxQuote.formattedPrice)
                            .font(ObsidianTheme.tabularSmall)
                            .monospacedDigit()
                            .foregroundStyle(ObsidianTheme.onSurface)
                        DeltaPill(text: idxQuote.formattedPercent, trend: SignalTrend(delta: idxQuote.percentChange))
                    }
                    .padding(.horizontal, 8)
                    .padding(.vertical, 5)
                    .background(ObsidianTheme.surface, in: RoundedRectangle(cornerRadius: 10))
                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(ObsidianTheme.outlineVariant, lineWidth: 1))
                }
                Spacer()
            }

            HStack(spacing: 6) {
                intentChip("All 37", .presetAll37)
                intentChip("Top 10", .presetTop10)
                intentChip("Bull", .scenarioBull)
                intentChip("Bear", .scenarioBear)
                intentChip("Reset %", .resetWeights)
                Spacer()
                intentChip(vm.isSyncing ? "Syncing..." : "Sync", .refreshLive, emphasis: .primary)
                intentChip("Quit", .quitApp, emphasis: .destructive)
            }
        }
    }

    private func modeToggleButton(p: KmpPortfolioCardState) -> some View {
        Button(action: { vm.sendIntent(.toggleMode) }) {
            Text(p.weightMode == "NORMALIZED_100" ? "100%" : "Raw")
                .font(ObsidianTheme.microLabel)
                .foregroundStyle(ObsidianTheme.onPrimaryContainer)
                .padding(.horizontal, 7)
                .padding(.vertical, 3)
                .background(ObsidianTheme.primaryContainer, in: Capsule())
        }
        .buttonStyle(.plain)
    }

    private func pinBadgeButton() -> some View {
        Button(action: { onTogglePin?() }) {
            Text(vm.pinState == .pinnedExpanded ? "PIN" : "AUTO")
                .font(ObsidianTheme.microLabel)
                .foregroundStyle(vm.pinState == .pinnedExpanded ? ObsidianTheme.amber : ObsidianTheme.onSurfaceVariant)
                .padding(.horizontal, 6)
                .padding(.vertical, 3)
                .background(
                    vm.pinState == .pinnedExpanded ? ObsidianTheme.amberContainer : ObsidianTheme.surfaceContainer,
                    in: Capsule()
                )
        }
        .buttonStyle(.plain)
    }

    private func quitButton() -> some View {
        Button(action: { NSApplication.shared.terminate(nil) }) {
            Text("✕")
                .font(.system(size: 10, weight: .bold))
                .foregroundStyle(ObsidianTheme.negative)
                .padding(.horizontal, 7)
                .padding(.vertical, 3)
                .background(ObsidianTheme.surfaceContainer, in: Capsule())
        }
        .buttonStyle(.plain)
        .help("Quit MarketWear Island")
    }

    private enum ChipEmphasis {
        case standard
        case primary
        case destructive
    }

    private func intentChip(_ label: String, _ action: IslandActionCode, emphasis: ChipEmphasis = .standard) -> some View {
        Button(action: { vm.sendIntent(action) }) {
            let textColor: Color = {
                switch emphasis {
                case .primary: return ObsidianTheme.onPrimaryContainer
                case .destructive: return ObsidianTheme.negative
                case .standard: return ObsidianTheme.onSurfaceVariant
                }
            }()
            let bgColor: Color = {
                switch emphasis {
                case .primary: return ObsidianTheme.primaryContainer
                case .destructive: return ObsidianTheme.surfaceContainer
                case .standard: return ObsidianTheme.surfaceContainer
                }
            }()
            Text(label)
                .font(ObsidianTheme.microLabel)
                .foregroundStyle(textColor)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(bgColor, in: Capsule())
        }
        .buttonStyle(.plain)
    }

    static func preferredPanelSize(for mode: IslandSurfaceMode, metrics: NotchMetrics) -> NSSize {
        switch mode {
        case .collapsedNotch:
            let w: CGFloat = (metrics.presence == .hardwareNotch)
                ? metrics.notchWidth + (collapsedEarWidth * 2) + (metrics.shoulderRadius * 2)
                : 320
            return NSSize(width: w, height: metrics.notchHeight)
        case .expandedIsland:
            let w: CGFloat = (metrics.presence == .hardwareNotch)
                ? max(520, metrics.notchWidth + 300)
                : 460
            return NSSize(width: w, height: metrics.notchHeight + expandedBodyHeight)
        case .menuBarPopover:
            return NSSize(width: 440, height: 326)
        }
    }
}

private struct NotchHostContainer: View {
    @ObservedObject var vm: MarketWearMacViewModel
    let onHoverChange: (Bool) -> Void
    let onTogglePin: () -> Void

    var body: some View {
        DynamicIslandDeskView(vm: vm, mode: vm.islandMode, onTogglePin: onTogglePin)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .onHover(perform: onHoverChange)
    }
}

@MainActor
final class NotchIslandPanelController {
    private let panel: NSPanel
    private let vm: MarketWearMacViewModel
    private var hoverExitTask: Task<Void, Never>?
    private var screenObserver: NSObjectProtocol?

    init(vm: MarketWearMacViewModel) {
        self.vm = vm
        let metrics = vm.notchMetrics
        let frame = Self.targetFrame(for: vm.islandMode, metrics: metrics)
        let panel = NSPanel(
            contentRect: frame,
            styleMask: [.borderless, .nonactivatingPanel],
            backing: .buffered,
            defer: false
        )
        // Sit just above the standard status bar window layer so the island aligns with the hardware notch
        panel.level = NSWindow.Level(rawValue: NSWindow.Level.statusBar.rawValue + 1)
        panel.backgroundColor = .clear
        panel.isOpaque = false
        panel.hasShadow = true
        panel.hidesOnDeactivate = false
        panel.collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary, .stationary]
        panel.isMovableByWindowBackground = false
        self.panel = panel

        let rootView = NotchHostContainer(
            vm: vm,
            onHoverChange: { [weak self] inside in self?.handleHover(inside) },
            onTogglePin: { [weak self] in self?.togglePin() }
        )
        panel.contentView = NSHostingView(rootView: rootView)
        panel.orderFrontRegardless()

        screenObserver = NotificationCenter.default.addObserver(
            forName: NSApplication.didChangeScreenParametersNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor [weak self] in
                self?.reanchorForCurrentScreen()
            }
        }
    }

    private func handleHover(_ inside: Bool) {
        hoverExitTask?.cancel()
        guard vm.pinState == .autoHover else { return }
        if inside {
            transition(to: .expandedIsland)
        } else {
            hoverExitTask = Task { @MainActor [weak self] in
                try? await Task.sleep(for: .milliseconds(180))
                guard !Task.isCancelled, let self, self.vm.pinState == .autoHover else { return }
                self.transition(to: .collapsedNotch)
            }
        }
    }

    func expandFromDock() {
        hoverExitTask?.cancel()
        vm.pinState = .pinnedExpanded
        transition(to: .expandedIsland)
    }

    private func togglePin() {
        hoverExitTask?.cancel()
        switch vm.pinState {
        case .autoHover:
            vm.pinState = .pinnedExpanded
            transition(to: .expandedIsland)
        case .pinnedExpanded:
            vm.pinState = .autoHover
            transition(to: .collapsedNotch)
        }
    }

    private func reanchorForCurrentScreen() {
        vm.refreshNotchMetrics()
        panel.setFrame(Self.targetFrame(for: vm.islandMode, metrics: vm.notchMetrics), display: true)
    }

    private func transition(to next: IslandSurfaceMode) {
        vm.refreshNotchMetrics()
        guard vm.islandMode != next else { return }
        withAnimation(.easeInOut(duration: 0.22)) {
            vm.islandMode = next
        }
        NSAnimationContext.runAnimationGroup { ctx in
            ctx.duration = 0.22
            ctx.timingFunction = CAMediaTimingFunction(name: .easeInEaseOut)
            panel.animator().setFrame(Self.targetFrame(for: next, metrics: vm.notchMetrics), display: true)
        }
    }

    private static func targetFrame(for mode: IslandSurfaceMode, metrics: NotchMetrics) -> NSRect {
        let screenFrame = metrics.targetScreenFrame
        let size = DynamicIslandDeskView.preferredPanelSize(for: mode, metrics: metrics)
        let x = round(screenFrame.midX - size.width / 2)
        // Flush with top display edge (screenFrame.maxY) so it merges into the hardware camera notch
        let y = screenFrame.maxY - size.height
        return NSRect(x: x, y: y, width: size.width, height: size.height)
    }
}

final class AppDelegate: NSObject, NSApplicationDelegate {
    static var sharedController: NotchIslandPanelController?

    func applicationShouldHandleReopen(_ sender: NSApplication, hasVisibleWindows flag: Bool) -> Bool {
        Self.sharedController?.expandFromDock()
        return true
    }
}

@main
struct MarketWearMacApp: App {
    @NSApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @StateObject private var vm: MarketWearMacViewModel
    private let notchController: NotchIslandPanelController

    init() {
        let sharedVm = MarketWearMacViewModel()
        _vm = StateObject(wrappedValue: sharedVm)
        let controller = NotchIslandPanelController(vm: sharedVm)
        notchController = controller
        AppDelegate.sharedController = controller
    }

    var body: some Scene {
        MenuBarExtra {
            DynamicIslandDeskView(vm: vm, mode: .menuBarPopover)
                .frame(width: 440, height: 326)
        } label: {
            let p = vm.dashboard.portfolioState
            Text("PORT \(String(format: "%.2f", p.activeQuote.price)) \(p.liveUpdateSpec.shortCriticalText)")
                .monospacedDigit()
        }
        .menuBarExtraStyle(.window)
    }
}
