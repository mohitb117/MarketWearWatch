import React, { useEffect, useId, useRef, useState } from 'react';
import mermaid from 'mermaid';
import { BatteryCharging, Cpu, Network, GitBranch } from 'lucide-react';

interface DiagramSpec {
  id: string;
  title: string;
  subtitle: string;
  badge: string;
  code: string;
}

const DIAGRAMS: DiagramSpec[] = [
  {
    id: 'system-overview',
    title: 'End-to-End Battery-Optimized System Architecture',
    subtitle: 'Market-hours sync gating, concurrent Ktor fetching, non-urgent Data Layer coalescing, and offline Room complications.',
    badge: 'Battery & Sync',
    code: `flowchart TD
    subgraph Cloud ["External Services"]
        YF["Yahoo Finance v8 Chart API"]
    end

    subgraph PhoneDevice ["Android Mobile Companion (:mobile · mobileModule)"]
        PhoneUI["MobileDashboardScreen (Uses :shared-ui MarketDashboardBody)"]
        SharedClient["KMP SharedMarketClient (Mutex Coalescing, Semaphore(6), 5m/6h Cache, 2h Portfolio Throttle)"]
        PhoneRepo["MobileMarketRepository (Koin Singleton)"]
        WorkMgr["MobileSyncWorker (30m + Off-Hours 6h Gate)"]
        GlanceWidget["Jetpack Glance AppWidget (KoinComponent)"]
        WearSyncMgr["WearDataSyncManager (Non-Urgent DataClient)"]

        WorkMgr -->|isUsMarketWindowActive?| SharedClient
        PhoneUI -->|Manual forceRefresh| SharedClient
        SharedClient -->|Bounded Fetch| YF
        SharedClient -->|Quotes + 100-Baseline Score| PhoneRepo
        PhoneRepo --> PhoneUI
        PhoneRepo -->|Update State| GlanceWidget
        PhoneRepo -->|Coalesced Payload| WearSyncMgr
    end

    subgraph WearableLayer ["Google Play Services Wearable Data Layer"]
        DataClientMsg["DataClient: /market/quotes (Non-Urgent PutDataMapRequest)"]
        MsgClientReq["MessageClient: /market/request_refresh"]
    end

    subgraph WatchDevice ["Wear OS Watch (:app · wearModule)"]
        ListenerSvc["MarketWearableListenerService (WakeLock)"]
        RoomDB[("Room Database (IndexQuoteEntity)")]
        WatchRepo["MarketRepository (Koin + SharedMarketClient)"]
        WatchWorker["MarketSyncWorker (45m / 6h Freshness Guard)"]
        WatchVM["MarketViewModel (koinViewModel)"]
        WatchUI["Wear Compose (ScalingLazyColumn + Rotary)"]
        ComplicationSvc["Complication Services (Offline Room, 3600s Period)"]

        WatchUI -->|Crown / Tap Refresh| WatchRepo
        WatchWorker -->|Skip if Fresh| RoomDB
        WatchWorker -->|Refresh if Stale| WatchRepo
        WatchRepo -->|Delegate Fetch| MsgClientReq
        DataClientMsg -->|Receive OnDataChanged| ListenerSvc
        ListenerSvc -->|Upsert Payload| RoomDB
        RoomDB -->|Flow Observation| WatchRepo
        WatchRepo --> WatchVM
        WatchVM --> WatchUI
        RoomDB -->|Zero-Network Dial Read| ComplicationSvc
    end

    WearSyncMgr -->|Push /market/quotes| DataClientMsg
    MsgClientReq -->|Receive Request| WearSyncMgr`,
  },
  {
    id: 'kmp-koin',
    title: 'KMP (:shared + :shared-ui) Multi-Surface Sharing & Koin DI',
    subtitle: 'Single source of truth for domain models, enums, network coalescing, design tokens, and Compose Multiplatform UI across Mobile, Desktop, iOS, Web & Wear OS.',
    badge: 'KMP + :shared-ui',
    code: `flowchart LR
    subgraph SharedCore [":shared KMP Core (commonMain / androidMain / iosMain / desktopMain / jsMain)"]
        DomainModels["IndexQuote, MarketRegistry & PortfolioRegistry (37 Holdings)"]
        UiShapes["PortfolioUiState, MarketUiStateBuilder & Enums"]
        SharedClient["SharedMarketClient (Mutex Coalescing, Semaphore(6), 2h Portfolio Throttle)"]
        SyncProtocol["WearConstants & isUsMarketWindowActive()"]
        SharedDI["sharedModule & initKoin() (Koin 4.0.0)"]
    end

    subgraph SharedUI [":shared-ui Compose Multiplatform (Android, Desktop, iOS, Web)"]
        Tokens["MarketWearDesignTokens (Palette, Dimens, TextSizes, Shapes, ItemKeys)"]
        Theme["MarketWearTheme (Dark Mode Default)"]
        Components["MarketDashboardBody, PortfolioScoreCard (Vico 3.3.1), IndexMetricCard, WatchStatusChip"]
        Entries["MarketWearIosViewController & MarketWearWebEntry"]
    end

    subgraph MobileModule [":mobile (Android Phone & Fold)"]
        MobileDI["mobileModule (Koin)"]
        MobileUI["MobileDashboardScreen + Glance + AppFunctions"]
        DataClientPhone["WearDataSyncManager (DataClient)"]
    end

    subgraph DesktopModule [":desktop (KMP JVM Desktop)"]
        DesktopUI["DesktopDashboardScreen + Side-by-Side Watch Preview"]
    end

    subgraph AppModule [":app (Wear OS Watch)"]
        WearDI["wearModule (Koin)"]
        WearUI["MarketWearScreen + PortfolioScoreWatchCard (Vico 3.3.1)"]
        RoomLocal["Room Persistence + Offline Complications"]
    end

    subgraph IosWebTargets ["iOS (Swift) & Web (Compose JS)"]
        IosTarget["iOS App (ComposeUIViewController + MarketWearShared)"]
        WebTarget["Web Target (ComposeViewport)"]
    end

    SharedUI --> SharedCore
    MobileModule --> SharedUI
    MobileModule --> SharedDI
    DesktopModule --> SharedUI
    DesktopModule --> SharedCore
    AppModule --> SharedCore
    AppModule --> WearDI
    WearDI --> SharedDI
    IosTarget --> Entries
    WebTarget --> Entries
    DataClientPhone --> SyncProtocol`,
  },
  {
    id: 'sync-sequence',
    title: 'Watch-to-Phone Sync, Freshness Guard & Fallback Protocol',
    subtitle: 'Delegates network calls to paired phone when nearby, coalesces background payloads, and suppresses retry storms.',
    badge: 'Sync Protocol',
    code: `sequenceDiagram
    autonumber
    actor User as Watch User
    participant WatchUI as Watch UI (ScalingLazyColumn)
    participant WatchRepo as Watch Repository
    participant NodeClient as Play Services NodeClient
    participant PhoneListener as Phone WearableListenerService
    participant KtorClient as KMP SharedMarketClient
    participant YahooAPI as Yahoo Finance API
    participant DataClient as Play Services DataClient
    participant WatchService as Watch WearableListenerService
    participant RoomDB as Watch Room DB

    Note over WatchRepo,RoomDB: Background Worker checks Room freshness (45m active / 6h off-hours)
    User->>WatchUI: Tap Refresh or Worker Triggers (Stale Cache)
    WatchUI->>WatchRepo: refreshWithPhoneFallback()
    WatchRepo->>NodeClient: getConnectedNodes()

    alt Phone Connected (Nearby / BT / Wi-Fi)
        NodeClient-->>WatchRepo: Node Available (isNearby = true)
        WatchRepo->>PhoneListener: MessageClient: /market/request_refresh
        activate PhoneListener
        PhoneListener->>KtorClient: fetchAllQuotes()
        activate KtorClient
        KtorClient->>YahooAPI: Concurrent async/awaitAll (13 tickers, 10s timeout)
        YahooAPI-->>KtorClient: JSON Quote Responses (~800ms)
        KtorClient-->>PhoneListener: List of IndexQuote
        deactivate KtorClient
        PhoneListener->>DataClient: PutDataMapRequest: /market/quotes (Non-Urgent on BG Sync)
        deactivate PhoneListener
        activate DataClient
        DataClient-->>WatchService: onDataChanged(dataEvents)
        deactivate DataClient
        activate WatchService
        Note over WatchService: Acquires WakeLock via runBlocking(Dispatchers.IO)
        WatchService->>RoomDB: upsertQuotes(quotes)
        WatchService-->>WatchUI: Flow emits updated state
        deactivate WatchService
        WatchUI-->>User: Screen shows fresh prices
    else Phone Disconnected / Standalone Watch
        NodeClient-->>WatchRepo: No Connected Phone Nodes
        Note over WatchRepo: Graceful Fallback via Koin-injected SharedMarketClient
        WatchRepo->>YahooAPI: Direct Concurrent Ktor Fetch
        YahooAPI-->>WatchRepo: JSON Responses (or Result.success() on transient error)
        WatchRepo->>RoomDB: upsertQuotes(quotes)
        WatchRepo-->>WatchUI: Flow emits updated state
        WatchUI-->>User: Screen shows fresh prices
    end`,
  },
];

let mermaidInitialized = false;

export const SystemDiagrams: React.FC = () => {
  const [selectedId, setSelectedId] = useState<string>(DIAGRAMS[0].id);
  const [svgMarkup, setSvgMarkup] = useState<string>('');
  const [renderError, setRenderError] = useState<string | null>(null);
  const renderCounter = useRef(0);
  const baseId = useId().replace(/:/g, '');

  const activeDiagram = DIAGRAMS.find((d) => d.id === selectedId) ?? DIAGRAMS[0];

  useEffect(() => {
    let cancelled = false;

    if (!mermaidInitialized) {
      mermaid.initialize({
        startOnLoad: false,
        theme: 'dark',
        securityLevel: 'loose',
        flowchart: { useMaxWidth: true, htmlLabels: true, curve: 'basis' },
        sequence: { useMaxWidth: true },
        themeVariables: {
          darkMode: true,
          background: '#0a0a0a',
          primaryColor: '#0c4a6e',
          primaryTextColor: '#f5f5f5',
          primaryBorderColor: '#0284c7',
          lineColor: '#38bdf8',
          secondaryColor: '#1e1b4b',
          tertiaryColor: '#171717',
        },
      });
      mermaidInitialized = true;
    }

    const renderDiagram = async () => {
      try {
        setRenderError(null);
        renderCounter.current += 1;
        const uniqueId = `mermaid-${baseId}-${activeDiagram.id}-${renderCounter.current}`;
        const { svg } = await mermaid.render(uniqueId, activeDiagram.code);
        if (!cancelled) {
          setSvgMarkup(svg);
        }
      } catch (err) {
        if (!cancelled) {
          setRenderError(err instanceof Error ? err.message : 'Failed to render diagram');
        }
      }
    };

    renderDiagram();

    return () => {
      cancelled = true;
    };
  }, [activeDiagram, baseId]);

  return (
    <div className="space-y-6">
      {/* Diagram Selector Header */}
      <div className="bg-neutral-900/60 border border-neutral-800 rounded-2xl p-5 flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 text-xs text-sky-400 font-semibold uppercase tracking-wider">
            <GitBranch className="w-3.5 h-3.5" />
            <span>Interactive System Diagrams</span>
          </div>
          <h2 className="text-lg font-bold text-white mt-0.5">{activeDiagram.title}</h2>
          <p className="text-xs text-neutral-400 mt-1">{activeDiagram.subtitle}</p>
        </div>

        <div className="flex flex-wrap items-center gap-1.5 bg-neutral-950 p-1 rounded-xl border border-neutral-800 text-xs shrink-0">
          {DIAGRAMS.map((diagram) => (
            <button
              key={diagram.id}
              onClick={() => setSelectedId(diagram.id)}
              className={`px-3.5 py-2 rounded-lg font-semibold transition-all cursor-pointer ${
                selectedId === diagram.id
                  ? 'bg-sky-600 text-white shadow-sm'
                  : 'text-neutral-400 hover:text-white'
              }`}
            >
              {diagram.badge}
            </button>
          ))}
        </div>
      </div>

      {/* Rendered Mermaid Diagram */}
      <div className="bg-neutral-950 border border-neutral-800 rounded-2xl p-4 sm:p-6 overflow-x-auto min-h-[380px] flex items-center justify-center">
        {renderError ? (
          <div className="text-xs text-rose-400 font-mono p-4">
            Failed to render diagram: {renderError}
          </div>
        ) : svgMarkup ? (
          <div
            className="w-full flex justify-center [&_svg]:max-w-full [&_svg]:h-auto"
            dangerouslySetInnerHTML={{ __html: svgMarkup }}
          />
        ) : (
          <div className="text-xs text-neutral-500">Rendering system diagram...</div>
        )}
      </div>

      {/* Optimization & Refactor Breakdown Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-neutral-900/50 border border-neutral-800 rounded-xl p-5 space-y-2.5">
          <div className="flex items-center gap-2 text-sm font-bold text-white">
            <Cpu className="w-4 h-4 text-purple-400" />
            <span>KMP :shared + :shared-ui</span>
          </div>
          <p className="text-xs text-neutral-400 leading-relaxed">
            <code className="text-purple-300">:shared</code> holds <code className="text-purple-300">PortfolioRegistry</code> (37 holdings), <code className="text-purple-300">MarketUiStateBuilder</code> + enum states, and <code className="text-purple-300">SharedMarketClient</code>; <code className="text-purple-300">:shared-ui</code> shares Compose Multiplatform cards, Vico <code className="text-purple-300">3.3.1</code> charts, and Dark Mode tokens across Mobile, Desktop, iOS &amp; Web.
          </p>
        </div>

        <div className="bg-neutral-900/50 border border-neutral-800 rounded-xl p-5 space-y-2.5">
          <div className="flex items-center gap-2 text-sm font-bold text-white">
            <Network className="w-4 h-4 text-sky-400" />
            <span>Zero-Duplication Tokens &amp; DI</span>
          </div>
          <p className="text-xs text-neutral-400 leading-relaxed">
            <code className="text-sky-300">MarketWearDesignTokens</code> (<code className="text-sky-300">Palette</code>, <code className="text-sky-300">Dimens</code>, <code className="text-sky-300">TextSizes</code>, <code className="text-sky-300">Shapes</code>, <code className="text-sky-300">ItemKeys</code>) and Koin <code className="text-sky-300">4.0.0</code> (<code className="text-sky-300">sharedModule</code>, <code className="text-sky-300">mobileModule</code>, <code className="text-sky-300">wearModule</code>) eliminate per-platform theme/DI drift.
          </p>
        </div>

        <div className="bg-neutral-900/50 border border-neutral-800 rounded-xl p-5 space-y-2.5">
          <div className="flex items-center gap-2 text-sm font-bold text-white">
            <BatteryCharging className="w-4 h-4 text-emerald-400" />
            <span>Network, Battery &amp; UI Performance</span>
          </div>
          <p className="text-xs text-neutral-400 leading-relaxed">
            <code className="text-emerald-300">Mutex</code> request coalescing, <code className="text-emerald-300">Semaphore(6)</code> concurrency, <code className="text-emerald-300">5m/6h</code> core + <code className="text-emerald-300">2h</code> portfolio caching, plus Vico gesture/diff-animation disabling (<code className="text-emerald-300">scrollEnabled=false</code>) for jank-free Watch &amp; Mobile scrolling.
          </p>
        </div>
      </div>
    </div>
  );
};
