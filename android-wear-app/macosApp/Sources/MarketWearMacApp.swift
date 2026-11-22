import AppKit
import Combine
import SwiftUI

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
}

enum BridgeConnectionStatus: String {
    case liveLoopback = "KMP SYNC"
    case localCache = "LOCAL SEED"
}

@MainActor
final class MarketWearMacViewModel: ObservableObject {
    @Published private(set) var dashboard: KmpDashboardState = .loadLatestOrFallback()
    @Published private(set) var connectionStatus: BridgeConnectionStatus = .localCache
    @Published var scrubSelection: ScrubSelection = .live

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

    func sendIntent(_ action: IslandActionCode) {
        Task {
            let url = baseURL.appending(path: "intent").appending(queryItems: [URLQueryItem(name: "code", value: action.rawValue)])
            var req = URLRequest(url: url)
            req.timeoutInterval = 1.0
            if let (data, _) = try? await URLSession.shared.data(for: req),
               let decoded = try? JSONDecoder().decode(KmpDashboardState.self, from: data) {
                dashboard = decoded
                connectionStatus = .liveLoopback
            } else {
                dashboard = KmpDashboardState.loadLatestOrFallback()
                connectionStatus = .localCache
            }
        }
    }

    private func pollLatestState() async {
        var req = URLRequest(url: baseURL.appending(path: "state"))
        req.timeoutInterval = 0.6
        if let (data, _) = try? await URLSession.shared.data(for: req),
           let decoded = try? JSONDecoder().decode(KmpDashboardState.self, from: data) {
            dashboard = decoded
            connectionStatus = .liveLoopback
        } else {
            let cached = KmpDashboardState.loadLatestOrFallback()
            if cached.latestUpdatedMillis != dashboard.latestUpdatedMillis {
                dashboard = cached
            }
            connectionStatus = .localCache
        }
    }
}

struct DynamicIslandDeskView: View {
    @ObservedObject var vm: MarketWearMacViewModel
    let mode: IslandSurfaceMode

    var body: some View {
        let p = vm.dashboard.portfolioState
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
            HStack(spacing: 10) {
                Circle()
                    .fill(isPositive ? ObsidianTheme.positive : ObsidianTheme.negative)
                    .frame(width: 7, height: 7)
                Text("PORT")
                    .font(ObsidianTheme.microLabel)
                    .foregroundStyle(ObsidianTheme.primary)
                Text(String(format: "%.2f", activePrice))
                    .font(ObsidianTheme.tabularSmall)
                    .monospacedDigit()
                    .foregroundStyle(ObsidianTheme.onSurface)
                BipolarLogGaugeView(spec: p.liveUpdateSpec, height: 5, scaleVisibility: .hidden)
                    .frame(width: 64)
                DeltaPill(text: String(format: "%+.2f%%", activePct), trend: SignalTrend(delta: activePct))
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 7)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(ObsidianTheme.background, in: Capsule())
            .overlay(Capsule().stroke(ObsidianTheme.outlineVariant, lineWidth: 1))

        case .expandedIsland, .menuBarPopover:
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
                    Button(action: { vm.sendIntent(.toggleMode) }) {
                        Text(p.mobileModePillLabel)
                            .font(ObsidianTheme.microLabel)
                            .foregroundStyle(ObsidianTheme.onPrimaryContainer)
                            .padding(.horizontal, 9)
                            .padding(.vertical, 4)
                            .background(ObsidianTheme.primaryContainer, in: Capsule())
                    }
                    .buttonStyle(.plain)
                }

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
                    intentChip("Sync", .refreshLive, emphasis: .primary)
                }
            }
            .padding(14)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(ObsidianTheme.background, in: RoundedRectangle(cornerRadius: 22))
            .overlay(RoundedRectangle(cornerRadius: 22).stroke(ObsidianTheme.outlineVariant, lineWidth: 1))
        }
    }

    private enum ChipEmphasis {
        case standard
        case primary
    }

    private func intentChip(_ label: String, _ action: IslandActionCode, emphasis: ChipEmphasis = .standard) -> some View {
        Button(action: { vm.sendIntent(action) }) {
            Text(label)
                .font(ObsidianTheme.microLabel)
                .foregroundStyle(emphasis == .primary ? ObsidianTheme.onPrimaryContainer : ObsidianTheme.onSurfaceVariant)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(emphasis == .primary ? ObsidianTheme.primaryContainer : ObsidianTheme.surfaceContainer, in: Capsule())
        }
        .buttonStyle(.plain)
    }
}

enum IslandPinState {
    case autoHover
    case pinnedExpanded
}

@MainActor
final class NotchIslandPanelController {
    private let panel: NSPanel
    private let vm: MarketWearMacViewModel
    private var surfaceMode: IslandSurfaceMode = .collapsedNotch
    private var pinState: IslandPinState = .autoHover

    init(vm: MarketWearMacViewModel) {
        self.vm = vm
        let frame = Self.targetFrame(for: .collapsedNotch)
        let panel = NSPanel(
            contentRect: frame,
            styleMask: [.borderless, .nonactivatingPanel],
            backing: .buffered,
            defer: false
        )
        panel.level = .statusBar
        panel.backgroundColor = .clear
        panel.isOpaque = false
        panel.hasShadow = true
        panel.collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary, .stationary]
        panel.isMovableByWindowBackground = false
        self.panel = panel
        renderContent()
        panel.orderFrontRegardless()
    }

    private func renderContent() {
        let rootView = DynamicIslandDeskView(vm: vm, mode: surfaceMode)
            .onHover { [weak self] inside in
                guard let self, self.pinState == .autoHover else { return }
                self.setSurfaceMode(inside ? .expandedIsland : .collapsedNotch)
            }
            .onTapGesture { [weak self] in
                guard let self else { return }
                switch self.pinState {
                case .autoHover:
                    self.pinState = .pinnedExpanded
                    self.setSurfaceMode(.expandedIsland)
                case .pinnedExpanded:
                    self.pinState = .autoHover
                    self.setSurfaceMode(.collapsedNotch)
                }
            }
        panel.contentView = NSHostingView(rootView: rootView)
    }

    private func setSurfaceMode(_ next: IslandSurfaceMode) {
        guard surfaceMode != next else { return }
        surfaceMode = next
        renderContent()
        NSAnimationContext.runAnimationGroup { ctx in
            ctx.duration = 0.22
            panel.animator().setFrame(Self.targetFrame(for: next), display: true)
        }
    }

    private static func targetFrame(for mode: IslandSurfaceMode) -> NSRect {
        let screenFrame = NSScreen.main?.frame ?? NSRect(x: 0, y: 0, width: 1440, height: 900)
        let size: NSSize = (mode == .collapsedNotch) ? NSSize(width: 310, height: 34) : NSSize(width: 440, height: 325)
        let x = screenFrame.midX - size.width / 2
        let y = screenFrame.maxY - size.height - 4
        return NSRect(x: x, y: y, width: size.width, height: size.height)
    }
}

@main
struct MarketWearMacApp: App {
    @StateObject private var vm: MarketWearMacViewModel
    private let notchController: NotchIslandPanelController

    init() {
        let sharedVm = MarketWearMacViewModel()
        _vm = StateObject(wrappedValue: sharedVm)
        notchController = NotchIslandPanelController(vm: sharedVm)
    }

    var body: some Scene {
        MenuBarExtra {
            DynamicIslandDeskView(vm: vm, mode: .menuBarPopover)
                .frame(width: 430, height: 320)
        } label: {
            let p = vm.dashboard.portfolioState
            Text("PORT \(String(format: "%.2f", p.activeQuote.price)) \(p.liveUpdateSpec.shortCriticalText)")
                .monospacedDigit()
        }
        .menuBarExtraStyle(.window)
    }
}
