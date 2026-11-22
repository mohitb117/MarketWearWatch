import SwiftUI
import WidgetKit

struct MarketWearWidgetEntry: TimelineEntry {
    let date: Date
    let dashboard: KmpDashboardState
}

struct MarketWearTimelineProvider: TimelineProvider {
    func placeholder(in context: Context) -> MarketWearWidgetEntry {
        MarketWearWidgetEntry(date: .now, dashboard: .fallback)
    }

    func getSnapshot(in context: Context, completion: @escaping (MarketWearWidgetEntry) -> Void) {
        completion(MarketWearWidgetEntry(date: .now, dashboard: .loadLatestOrFallback()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<MarketWearWidgetEntry>) -> Void) {
        let entry = MarketWearWidgetEntry(date: .now, dashboard: .loadLatestOrFallback())
        let nextRefresh = Calendar.current.date(byAdding: .minute, value: 5, to: .now) ?? .now.addingTimeInterval(300)
        completion(Timeline(entries: [entry], policy: .after(nextRefresh)))
    }
}

struct MarketWearDesktopWidgetView: View {
    let entry: MarketWearWidgetEntry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        let p = entry.dashboard.portfolioState
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text("PORTFOLIO")
                    .font(ObsidianTheme.microLabel)
                    .foregroundStyle(ObsidianTheme.primary)
                Spacer()
                DeltaPill(text: p.liveUpdateSpec.shortCriticalText, trend: SignalTrend(delta: p.activeQuote.percentChange))
            }

            Text(String(format: "%.2f", p.activeQuote.price))
                .font(family == .systemSmall ? ObsidianTheme.tabularMetric : ObsidianTheme.tabularHero)
                .monospacedDigit()
                .foregroundStyle(ObsidianTheme.onSurface)

            BipolarLogGaugeView(
                spec: p.liveUpdateSpec,
                height: 6,
                scaleVisibility: family == .systemSmall ? .hidden : .showTicks
            )

            if family != .systemSmall {
                InteractivePortfolioIslandChart(portfolio: p, scrub: .constant(.live))
                    .frame(height: 64)
            }
        }
        .padding(12)
        .containerBackground(ObsidianTheme.background, for: .widget)
    }
}

@main
struct MarketWearDesktopWidget: Widget {
    let kind: String = "MarketWearDesktopWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: MarketWearTimelineProvider()) { entry in
            MarketWearDesktopWidgetView(entry: entry)
        }
        .configurationDisplayName("MarketWear Portfolio Score")
        .description("Obsidian Terminal center-origin portfolio telemetry powered by KMP :shared.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}
