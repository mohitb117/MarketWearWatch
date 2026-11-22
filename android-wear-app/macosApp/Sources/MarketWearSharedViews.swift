import SwiftUI

/// Obsidian Terminal Design System tokens matching MarketWatchAIStudio (`docs/DESIGN_SYSTEM.md`).
enum ObsidianTheme {
    static let background = Color(hex: 0x070B11)
    static let surface = Color(hex: 0x0E1520)
    static let surfaceContainer = Color(hex: 0x151F2E)
    static let surfaceContainerHigh = Color(hex: 0x1D2A3D)
    static let outlineVariant = Color(hex: 0x28384F).opacity(0.65)
    static let primary = Color(hex: 0x38BDF8)
    static let primaryContainer = Color(hex: 0x10324A)
    static let onPrimaryContainer = Color(hex: 0xBAE6FD)
    static let onSurface = Color(hex: 0xF1F5F9)
    static let onSurfaceVariant = Color(hex: 0x94A3B8)
    static let positive = Color(hex: 0x34D399)
    static let positiveContainer = Color(hex: 0x0B2E24)
    static let negative = Color(hex: 0xFB7185)
    static let negativeContainer = Color(hex: 0x38131E)
    static let amber = Color(hex: 0xFBBF24)
    static let amberContainer = Color(hex: 0x332609)
    static let secondaryCurve = Color(hex: 0x38BDF8).opacity(0.72)
    static let baselineRule = Color(hex: 0x64748B)
    static let darkTrack = Color(hex: 0x141C27)

    static let tabularHero = Font.system(size: 32, weight: .bold, design: .monospaced)
    static let tabularMetric = Font.system(size: 16, weight: .bold, design: .monospaced)
    static let tabularSmall = Font.system(size: 11, weight: .semibold, design: .monospaced)
    static let microLabel = Font.system(size: 10, weight: .medium, design: .monospaced)
}

extension Color {
    init(hex: UInt32, alpha: Double = 1.0) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255.0,
            green: Double((hex >> 8) & 0xFF) / 255.0,
            blue: Double(hex & 0xFF) / 255.0,
            opacity: alpha
        )
    }

    init(argb: Int32) {
        let u = UInt32(bitPattern: argb)
        self.init(
            red: Double((u >> 16) & 0xFF) / 255.0,
            green: Double((u >> 8) & 0xFF) / 255.0,
            blue: Double(u & 0xFF) / 255.0,
            opacity: Double((u >> 24) & 0xFF) / 255.0
        )
    }
}

/// Direct Codable projection of JSON produced by `:shared`'s `MarketUiStateBuilder.encodeDashboardJson`.
struct KmpDashboardState: Codable {
    let portfolioState: KmpPortfolioCardState
    let indices: [KmpIndexQuote]
    let sectors: [KmpIndexQuote]
    let latestUpdatedMillis: Int64

    static func loadLatestOrFallback(path: String = "/tmp/marketwear_island_state.json") -> KmpDashboardState {
        if let data = try? Data(contentsOf: URL(fileURLWithPath: path)),
           let decoded = try? JSONDecoder().decode(KmpDashboardState.self, from: data) {
            return decoded
        }
        return .fallback
    }

    static let fallback = KmpDashboardState(
        portfolioState: KmpPortfolioCardState(
            weightMode: "NORMALIZED_100",
            activeQuote: KmpIndexQuote(
                symbol: "PORTFOLIO", name: "Portfolio Score", price: 100.84,
                change: 0.84, percentChange: 0.84, dayHigh: 100.84, dayLow: 99.94,
                lastUpdated: 0, category: "PORTFOLIO",
                sparkline: [100.00, 100.12, 99.94, 100.08, 100.24, 100.19, 100.38, 100.52, 100.46, 100.68, 100.61, 100.84]
            ),
            secondaryQuote: KmpIndexQuote(
                symbol: "PORTFOLIO", name: "Portfolio Score", price: 100.50,
                change: 0.50, percentChange: 0.50, dayHigh: 100.50, dayLow: 99.96,
                lastUpdated: 0, category: "PORTFOLIO",
                sparkline: [100.00, 100.07, 99.96, 100.05, 100.14, 100.11, 100.23, 100.31, 100.27, 100.40, 100.36, 100.50]
            ),
            trendDirection: "POSITIVE",
            watchTitleLabel: "Portfolio (100%)",
            mobileModePillLabel: "Normalized (100%)",
            mobileSecondarySummary: "Raw (59.41%): 100.50 (+0.50%)",
            mobileRangeSummary: "Range: 99.94 – 100.84",
            primarySparkline: [100.00, 100.12, 99.94, 100.08, 100.24, 100.19, 100.38, 100.52, 100.46, 100.68, 100.61, 100.84],
            secondarySparkline: [100.00, 100.07, 99.96, 100.05, 100.14, 100.11, 100.23, 100.31, 100.27, 100.40, 100.36, 100.50],
            enabledHoldingsCount: 37,
            totalHoldingsCount: 37,
            liveUpdateSpec: KmpLiveUpdateSpec(
                progress: 632, maxProgress: 1000, baselinePoint: 500, trendDirection: "POSITIVE",
                accentArgb: Int32(bitPattern: 0xFF10B981),
                segments: [
                    KmpBipolarSegment(length: 500, argbColor: Int32(bitPattern: 0xFF141C27)),
                    KmpBipolarSegment(length: 132, argbColor: Int32(bitPattern: 0xFF10B981)),
                    KmpBipolarSegment(length: 368, argbColor: Int32(bitPattern: 0xFF141C27))
                ],
                shortCriticalText: "+0.84%",
                title: "Portfolio 100.84 (+0.84%)",
                subtitle: "37/37 holdings · Center 100.0 · Log scale"
            )
        ),
        indices: [
            KmpIndexQuote(symbol: "SPX", name: "S&P 500", price: 5985.42, change: 22.84, percentChange: 0.38, dayHigh: 6005.10, dayLow: 5960.30, lastUpdated: 0, category: "INDEX", sparkline: []),
            KmpIndexQuote(symbol: "NDX", name: "Nasdaq 100", price: 21110.15, change: -45.20, percentChange: -0.21, dayHigh: 21250.00, dayLow: 21050.80, lastUpdated: 0, category: "INDEX", sparkline: [])
        ],
        sectors: [],
        latestUpdatedMillis: 0
    )
}

struct KmpPortfolioCardState: Codable {
    let weightMode: String
    let activeQuote: KmpIndexQuote
    let secondaryQuote: KmpIndexQuote
    let trendDirection: String
    let watchTitleLabel: String
    let mobileModePillLabel: String
    let mobileSecondarySummary: String
    let mobileRangeSummary: String
    let primarySparkline: [Double]
    let secondarySparkline: [Double]
    let enabledHoldingsCount: Int
    let totalHoldingsCount: Int
    let liveUpdateSpec: KmpLiveUpdateSpec
}

struct KmpLiveUpdateSpec: Codable {
    let progress: Int
    let maxProgress: Int
    let baselinePoint: Int
    let trendDirection: String
    let accentArgb: Int32
    let segments: [KmpBipolarSegment]
    let shortCriticalText: String
    let title: String
    let subtitle: String
}

struct KmpBipolarSegment: Codable {
    let length: Int
    let argbColor: Int32
}

struct KmpIndexQuote: Codable, Identifiable {
    let symbol: String
    let name: String
    let price: Double
    let change: Double
    let percentChange: Double
    let dayHigh: Double
    let dayLow: Double
    let lastUpdated: Int64
    let category: String
    let sparkline: [Double]

    var id: String { symbol }
    var formattedPrice: String { String(format: "%.2f", price) }
    var formattedDelta: String { String(format: "%+.2f (%+.2f%%)", change, percentChange) }
    var formattedPercent: String { String(format: "%+.2f%%", percentChange) }
    var signalColor: Color { percentChange >= 0 ? ObsidianTheme.positive : ObsidianTheme.negative }
    var containerColor: Color { percentChange >= 0 ? ObsidianTheme.positiveContainer : ObsidianTheme.negativeContainer }
}

enum ScrubSelection: Equatable {
    case live
    case point(Int)
}

enum GaugeScaleVisibility {
    case hidden
    case showTicks
}

enum SignalTrend {
    case positive
    case negative

    init(delta: Double) {
        self = delta >= 0 ? .positive : .negative
    }

    var accentColor: Color {
        switch self {
        case .positive: return ObsidianTheme.positive
        case .negative: return ObsidianTheme.negative
        }
    }

    var containerColor: Color {
        switch self {
        case .positive: return ObsidianTheme.positiveContainer
        case .negative: return ObsidianTheme.negativeContainer
        }
    }
}

struct DeltaPill: View {
    let text: String
    let trend: SignalTrend

    var body: some View {
        Text(text)
            .font(ObsidianTheme.tabularSmall)
            .monospacedDigit()
            .foregroundStyle(trend.accentColor)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(trend.containerColor, in: RoundedRectangle(cornerRadius: 8))
    }
}

/// Center-origin log-linear bipolar bar driven directly by KMP `PortfolioLiveUpdateSpec.segments`.
struct BipolarLogGaugeView: View {
    let spec: KmpLiveUpdateSpec
    let height: CGFloat
    let scaleVisibility: GaugeScaleVisibility

    var body: some View {
        VStack(spacing: 4) {
            Canvas { ctx, size in
                let maxUnits = max(Double(spec.maxProgress), 1.0)
                let trackRect = CGRect(x: 0, y: (size.height - height) / 2, width: size.width, height: height)
                ctx.fill(Path(roundedRect: trackRect, cornerRadius: height / 2), with: .color(ObsidianTheme.darkTrack))

                var cursorX: Double = 0
                for seg in spec.segments {
                    let segW = (Double(seg.length) / maxUnits) * size.width
                    let rect = CGRect(x: cursorX, y: trackRect.minY, width: segW, height: height)
                    let segColor = Color(argb: seg.argbColor)
                    ctx.fill(Path(rect), with: .color(segColor == ObsidianTheme.darkTrack ? ObsidianTheme.surfaceContainerHigh : segColor))
                    cursorX += segW
                }

                // 50% center line (100.0 baseline)
                let centerX = (Double(spec.baselinePoint) / maxUnits) * size.width
                var centerLine = Path()
                centerLine.move(to: CGPoint(x: centerX, y: 0))
                centerLine.addLine(to: CGPoint(x: centerX, y: size.height))
                ctx.stroke(centerLine, with: .color(ObsidianTheme.onSurface), lineWidth: 1.5)

                // Active progress indicator dot
                let dotX = min(max((Double(spec.progress) / maxUnits) * size.width, 4), size.width - 4)
                let dotR = height * 0.55
                let dotRect = CGRect(x: dotX - dotR, y: size.height / 2 - dotR, width: dotR * 2, height: dotR * 2)
                ctx.fill(Path(ellipseIn: dotRect), with: .color(Color(argb: spec.accentArgb)))
                ctx.stroke(Path(ellipseIn: dotRect), with: .color(ObsidianTheme.onSurface), lineWidth: 1)
            }
            .frame(height: max(height + 4, 10))

            if scaleVisibility == .showTicks {
                HStack {
                    Text("-25%   -5%   -1%")
                    Spacer()
                    Text("100.0")
                        .foregroundStyle(ObsidianTheme.onSurface)
                    Spacer()
                    Text("+1%   +5%   +25%")
                }
                .font(ObsidianTheme.microLabel)
                .monospacedDigit()
                .foregroundStyle(ObsidianTheme.onSurfaceVariant)
            }
        }
    }
}

/// 4-pass Obsidian Terminal Canvas chart matching `MarketWatchAIStudio` (`InteractivePortfolioChart`).
struct InteractivePortfolioIslandChart: View {
    let portfolio: KmpPortfolioCardState
    @Binding var scrub: ScrubSelection

    var body: some View {
        let primary = portfolio.primarySparkline
        let secondary = portfolio.secondarySparkline
        let signalColor = portfolio.activeQuote.signalColor

        GeometryReader { geo in
            Canvas { ctx, size in
                let allPoints = primary + secondary + [100.0]
                let minVal = (allPoints.min() ?? 99.0) - 0.08
                let maxVal = (allPoints.max() ?? 101.0) + 0.08
                let span = max(maxVal - minVal, 0.25)

                func yPos(_ v: Double) -> CGFloat {
                    CGFloat(1.0 - (v - minVal) / span) * (size.height - 12) + 6
                }

                // 1. Dashed 100.0 morning baseline rule
                let baseY = yPos(100.0)
                var basePath = Path()
                basePath.move(to: CGPoint(x: 0, y: baseY))
                basePath.addLine(to: CGPoint(x: size.width, y: baseY))
                ctx.stroke(basePath, with: .color(ObsidianTheme.baselineRule), style: StrokeStyle(lineWidth: 1, dash: [4, 4]))

                // 2. Dashed secondary comparison curve (Raw vs Normalized)
                if secondary.count >= 2 {
                    let secPath = smoothPath(values: secondary, size: size, yFor: yPos)
                    ctx.stroke(secPath, with: .color(ObsidianTheme.secondaryCurve), style: StrokeStyle(lineWidth: 1.4, dash: [4, 3]))
                }

                // 3. Smooth primary curve + vertical area gradient fill
                if primary.count >= 2 {
                    let strokePath = smoothPath(values: primary, size: size, yFor: yPos)
                    var fillPath = strokePath
                    fillPath.addLine(to: CGPoint(x: size.width, y: size.height))
                    fillPath.addLine(to: CGPoint(x: 0, y: size.height))
                    fillPath.closeSubpath()

                    ctx.fill(
                        fillPath,
                        with: .linearGradient(
                            Gradient(colors: [signalColor.opacity(0.28), signalColor.opacity(0.0)]),
                            startPoint: .zero,
                            endPoint: CGPoint(x: 0, y: size.height)
                        )
                    )
                    ctx.stroke(strokePath, with: .color(signalColor), style: StrokeStyle(lineWidth: 2.4, lineCap: .round, lineJoin: .round))

                    // 4. Scrub crosshair & focus halo (or live end point dot)
                    let activeIdx: Int = {
                        switch scrub {
                        case .live: return primary.count - 1
                        case .point(let i): return min(max(i, 0), primary.count - 1)
                        }
                    }()
                    let x = CGFloat(activeIdx) / CGFloat(max(primary.count - 1, 1)) * size.width
                    let y = yPos(primary[activeIdx])

                    if case .point = scrub {
                        var cross = Path()
                        cross.move(to: CGPoint(x: x, y: 0))
                        cross.addLine(to: CGPoint(x: x, y: size.height))
                        ctx.stroke(cross, with: .color(ObsidianTheme.primary.opacity(0.7)), style: StrokeStyle(lineWidth: 1, dash: [3, 3]))
                    }

                    let haloRect = CGRect(x: x - 6, y: y - 6, width: 12, height: 12)
                    ctx.fill(Path(ellipseIn: haloRect), with: .color(signalColor.opacity(0.28)))
                    let dotRect = CGRect(x: x - 3.2, y: y - 3.2, width: 6.4, height: 6.4)
                    ctx.fill(Path(ellipseIn: dotRect), with: .color(signalColor))
                }
            }
            .contentShape(Rectangle())
            .onContinuousHover { phase in
                switch phase {
                case .active(let loc):
                    updateScrub(x: loc.x, width: geo.size.width, count: primary.count)
                case .ended:
                    scrub = .live
                }
            }
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { val in updateScrub(x: val.location.x, width: geo.size.width, count: primary.count) }
                    .onEnded { _ in scrub = .live }
            )
        }
        .padding(.vertical, 6)
        .padding(.horizontal, 8)
        .background(ObsidianTheme.surface, in: RoundedRectangle(cornerRadius: 12))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(ObsidianTheme.outlineVariant, lineWidth: 1))
    }

    private func updateScrub(x: CGFloat, width: CGFloat, count: Int) {
        guard count > 1, width > 0 else { return }
        let ratio = min(max(x / width, 0), 1)
        scrub = .point(Int((ratio * CGFloat(count - 1)).rounded()))
    }

    private func smoothPath(values: [Double], size: CGSize, yFor: (Double) -> CGFloat) -> Path {
        var path = Path()
        let pts = values.enumerated().map { idx, val in
            CGPoint(x: CGFloat(idx) / CGFloat(max(values.count - 1, 1)) * size.width, y: yFor(val))
        }
        guard let first = pts.first else { return path }
        path.move(to: first)
        for i in 1..<pts.count {
            let prev = pts[i - 1]
            let curr = pts[i]
            let midX = (prev.x + curr.x) / 2
            path.addCurve(to: curr, control1: CGPoint(x: midX, y: prev.y), control2: CGPoint(x: midX, y: curr.y))
        }
        return path
    }
}
