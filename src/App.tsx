import React, { useState, useEffect } from 'react';
import {
  INITIAL_MARKET_STATE,
  MarketState,
  calculateMarketStatus,
  IndexQuote,
} from './data/marketData';
import {
  PixelWatch4Simulator,
  ComplicationSlotId,
  ComplicationContent,
} from './components/PixelWatch4Simulator';
import { MarketGlanceWidgetPreview } from './components/MarketGlanceWidgetPreview';
import { MobileCompanionPreview } from './components/MobileCompanionPreview';
import { MarketIndicesStrip } from './components/MarketIndicesStrip';
import { ComplicationConfigurator } from './components/ComplicationConfigurator';
import { AndroidCodeViewer } from './components/AndroidCodeViewer';
import { InstallationGuide } from './components/InstallationGuide';
import { SystemDiagrams } from './components/SystemDiagrams';
import { generateWearOsProjectZip, downloadBlob } from './utils/zipExport';
import {
  Download,
  Watch,
  Shield,
  Smartphone,
  Layers,
  Sparkles,
  RotateCw,
  Cpu,
  CheckCircle2,
  BatteryCharging,
  Network,
} from 'lucide-react';

export default function App() {
  const [activeTab, setActiveTab] = useState<'simulator' | 'architecture' | 'config' | 'source' | 'install'>('simulator');
  const [activeSurface, setActiveSurface] = useState<'watch' | 'widget' | 'mobile'>('watch');

  const [marketState, setMarketState] = useState<MarketState>(() => {
    const { status, statusText } = calculateMarketStatus();
    return {
      ...INITIAL_MARKET_STATE,
      status,
      statusText,
    };
  });

  const [selectedFormat, setSelectedFormat] = useState<'points' | 'percent' | 'change' | 'compact'>('points');
  const [complicationSlots, setComplicationSlots] = useState<Record<ComplicationSlotId, ComplicationContent>>({
    top: 'sp500',
    bottom: 'nasdaq',
    left: 'date',
    right: 'battery',
  });

  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);
  const [isExporting, setIsExporting] = useState<boolean>(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  const handleSlotChange = (slot: ComplicationSlotId, content: ComplicationContent) => {
    setComplicationSlots((prev) => ({ ...prev, [slot]: content }));
    showToast(`Updated ${slot} slot complication`);
  };

  const simulateTick = () => {
    setMarketState((prev) => {
      const updatedQuotes = prev.quotes.map((quote) => {
        // Fluctuation between -0.25% and +0.25%
        const deltaPct = (Math.random() - 0.48) * 0.25;
        const newPrice = Number((quote.price * (1 + deltaPct / 100)).toFixed(2));
        const prevClose = quote.dayLow + (quote.dayHigh - quote.dayLow) * 0.5;
        const change = Number((newPrice - prevClose).toFixed(2));
        const percentChange = Number(((change / prevClose) * 100).toFixed(2));
        const dayHigh = Math.max(quote.dayHigh, newPrice);
        const dayLow = Math.min(quote.dayLow, newPrice);
        const sparkline = quote.sparkline.length > 0 ? [...quote.sparkline.slice(1), newPrice] : [newPrice];

        return new IndexQuote({
          symbol: quote.symbol,
          name: quote.name,
          price: newPrice,
          change,
          percentChange,
          dayHigh,
          dayLow,
          lastUpdated: Date.now(),
          category: quote.category,
          sparkline,
        });
      });

      const spx = updatedQuotes.find((q) => q.symbol === 'SPX')!;
      const ndx = updatedQuotes.find((q) => q.symbol === 'NDX')!;
      const sectorsMap = updatedQuotes
        .filter((q) => q.category === 'SECTOR')
        .reduce((acc, q) => {
          acc[q.symbol] = q;
          return acc;
        }, {} as Record<string, IndexQuote>);

      return {
        ...prev,
        quotes: updatedQuotes,
        spx,
        ndx,
        sectors: sectorsMap,
        lastSyncTime: new Date(),
      };
    });
    showToast('Market quotes updated');
  };

  const handleRefreshQuotes = async () => {
    setIsRefreshing(true);
    // Simulate Ktor parallel HTTP query (~650ms)
    await new Promise((r) => setTimeout(r, 650));
    simulateTick();
    setIsRefreshing(false);
  };

  const handleQuickExport = async () => {
    try {
      setIsExporting(true);
      const blob = await generateWearOsProjectZip();
      downloadBlob(blob, 'MarketWear-PixelWatch-Project.zip');
      showToast('Downloaded Android Studio Project');
    } catch (e) {
      console.error(e);
      showToast('Export failed');
    } finally {
      setIsExporting(false);
    }
  };

  const timeString = marketState.lastSyncTime.toLocaleTimeString([], {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });

  return (
    <div className="min-h-screen bg-black text-neutral-100 flex flex-col font-sans selection:bg-sky-500/30 selection:text-sky-200">
      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed bottom-5 right-5 z-50 bg-neutral-900 border border-neutral-700 text-white px-4 py-2.5 rounded-xl shadow-2xl flex items-center gap-2 text-xs animate-in fade-in slide-in-from-bottom-3 duration-200">
          <CheckCircle2 className="w-4 h-4 text-emerald-400" />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* Global Header */}
      <header className="border-b border-neutral-900 bg-neutral-950/80 backdrop-blur sticky top-0 z-40 px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-sky-500 to-indigo-600 flex items-center justify-center shadow-md">
            <Watch className="w-5 h-5 text-white" />
          </div>
          <div>
            <div className="font-bold text-sm tracking-tight text-white flex items-center gap-1.5">
              <span>MarketWear</span>
              <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-sky-950 text-sky-400 border border-sky-800/40">
                v2.0
              </span>
            </div>
            <div className="text-[11px] text-neutral-400">KMP · Koin DI · Wear OS &amp; Mobile</div>
          </div>
        </div>

        {/* Global Navigation Tabs */}
        <nav className="hidden md:flex items-center gap-6 text-xs text-neutral-400">
          <button
            onClick={() => setActiveTab('simulator')}
            className={`transition-colors whitespace-nowrap cursor-pointer ${
              activeTab === 'simulator' ? 'text-white font-semibold' : 'hover:text-neutral-200'
            }`}
          >
            Interactive Surfaces
          </button>
          <button
            onClick={() => setActiveTab('architecture')}
            className={`transition-colors whitespace-nowrap cursor-pointer ${
              activeTab === 'architecture' ? 'text-white font-semibold' : 'hover:text-neutral-200'
            }`}
          >
            System Diagrams
          </button>
          <button
            onClick={() => setActiveTab('config')}
            className={`transition-colors whitespace-nowrap cursor-pointer ${
              activeTab === 'config' ? 'text-white font-semibold' : 'hover:text-neutral-200'
            }`}
          >
            Complication Formats
          </button>
          <button
            onClick={() => setActiveTab('source')}
            className={`transition-colors whitespace-nowrap cursor-pointer ${
              activeTab === 'source' ? 'text-white font-semibold' : 'hover:text-neutral-200'
            }`}
          >
            Android Source Code
          </button>
          <button
            onClick={() => setActiveTab('install')}
            className={`transition-colors whitespace-nowrap cursor-pointer ${
              activeTab === 'install' ? 'text-sky-400 font-semibold' : 'hover:text-neutral-200'
            }`}
          >
            Sideload &amp; Install
          </button>
        </nav>

        {/* Actions */}
        <div className="flex items-center gap-3 shrink-0">
          <button
            onClick={handleQuickExport}
            disabled={isExporting}
            className="flex items-center gap-2 px-3.5 py-1.5 text-xs font-semibold text-white bg-sky-600 rounded-lg hover:bg-sky-500 transition-colors whitespace-nowrap cursor-pointer disabled:opacity-50 shadow-sm"
          >
            <Download className="w-3.5 h-3.5" />
            <span>{isExporting ? 'Exporting...' : 'Export Project'}</span>
          </button>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
        {/* Editorial Subtitle */}
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 pb-4 border-b border-neutral-900">
          <div>
            <div className="flex flex-wrap items-center gap-2 text-xs text-neutral-400 mb-1">
              <span>Google Pixel Watch 4</span>
              <span>·</span>
              <span>Galaxy Z Fold 6</span>
              <span>·</span>
              <span className="text-purple-400 font-medium">KMP + Koin 4.0.0 DI</span>
              <span>·</span>
              <span className="text-emerald-400 font-medium">Battery-Optimized Sync</span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-white">
              Financial Market Ecosystem for Wear OS, Android &amp; KMP
            </h1>
            <p className="text-sm text-neutral-400 mt-1 max-w-2xl">
              Cross-device financial intelligence powered by a Kotlin Multiplatform shared core, Koin DI, Room cache, and market-hours-aware background sync for minimal watch battery drain.
            </p>
          </div>

          {/* Quick tab switcher */}
          <div className="flex items-center gap-1 p-1 bg-neutral-900 border border-neutral-800 rounded-lg self-start md:self-auto text-xs">
            <button
              onClick={() => setActiveTab('simulator')}
              className={`px-3 py-1.5 font-medium rounded-md transition-colors ${
                activeTab === 'simulator' ? 'bg-neutral-800 text-white' : 'text-neutral-400 hover:text-white'
              }`}
            >
              Surfaces
            </button>
            <button
              onClick={() => setActiveTab('architecture')}
              className={`px-3 py-1.5 font-medium rounded-md transition-colors ${
                activeTab === 'architecture' ? 'bg-neutral-800 text-white' : 'text-neutral-400 hover:text-white'
              }`}
            >
              Diagrams
            </button>
            <button
              onClick={() => setActiveTab('config')}
              className={`px-3 py-1.5 font-medium rounded-md transition-colors ${
                activeTab === 'config' ? 'bg-neutral-800 text-white' : 'text-neutral-400 hover:text-white'
              }`}
            >
              Complications
            </button>
            <button
              onClick={() => setActiveTab('source')}
              className={`px-3 py-1.5 font-medium rounded-md transition-colors ${
                activeTab === 'source' ? 'bg-neutral-800 text-white' : 'text-neutral-400 hover:text-white'
              }`}
            >
              Source
            </button>
            <button
              onClick={() => setActiveTab('install')}
              className={`px-3 py-1.5 font-medium rounded-md transition-colors ${
                activeTab === 'install' ? 'bg-neutral-800 text-white' : 'text-neutral-400 hover:text-white'
              }`}
            >
              Install
            </button>
          </div>
        </div>

        {/* Live Market Indices & Sectors Bar */}
        <MarketIndicesStrip
          spx={marketState.spx}
          ndx={marketState.ndx}
          quotes={marketState.quotes}
          marketStatus={marketState.status}
          statusText={marketState.statusText}
          onSimulateTick={simulateTick}
          onRefreshQuotes={handleRefreshQuotes}
          isRefreshing={isRefreshing}
        />

        {/* TAB CONTENT: Interactive Surfaces */}
        {activeTab === 'simulator' && (
          <div className="space-y-6">
            {/* Surface Selector Bar */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-neutral-900/60 border border-neutral-800 rounded-2xl p-4">
              <div>
                <div className="text-xs text-neutral-400 uppercase tracking-wider font-semibold">
                  Multi-Device Architecture Showcase
                </div>
                <div className="text-sm font-bold text-white">
                  Switch Target Device Surface
                </div>
              </div>

              <div className="flex items-center gap-1.5 bg-neutral-950 p-1 rounded-xl border border-neutral-800 text-xs">
                <button
                  onClick={() => setActiveSurface('watch')}
                  className={`px-3.5 py-2 rounded-lg font-semibold transition-all flex items-center gap-1.5 cursor-pointer ${
                    activeSurface === 'watch'
                      ? 'bg-sky-600 text-white shadow-sm'
                      : 'text-neutral-400 hover:text-white'
                  }`}
                >
                  <Watch className="w-3.5 h-3.5" />
                  <span>Pixel Watch 4</span>
                </button>

                <button
                  onClick={() => setActiveSurface('widget')}
                  className={`px-3.5 py-2 rounded-lg font-semibold transition-all flex items-center gap-1.5 cursor-pointer ${
                    activeSurface === 'widget'
                      ? 'bg-sky-600 text-white shadow-sm'
                      : 'text-neutral-400 hover:text-white'
                  }`}
                >
                  <Layers className="w-3.5 h-3.5" />
                  <span>Glance Widget</span>
                </button>

                <button
                  onClick={() => setActiveSurface('mobile')}
                  className={`px-3.5 py-2 rounded-lg font-semibold transition-all flex items-center gap-1.5 cursor-pointer ${
                    activeSurface === 'mobile'
                      ? 'bg-sky-600 text-white shadow-sm'
                      : 'text-neutral-400 hover:text-white'
                  }`}
                >
                  <Smartphone className="w-3.5 h-3.5" />
                  <span>Phone Companion</span>
                </button>
              </div>
            </div>

            {/* Main Interactive Stage */}
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
              {/* Left Stage: Active Surface Rendering */}
              <div className="lg:col-span-7 bg-neutral-900/40 border border-neutral-800/80 rounded-2xl p-6 flex flex-col items-center justify-center min-h-[560px]">
                {activeSurface === 'watch' && (
                  <PixelWatch4Simulator
                    quotes={marketState.quotes}
                    spx={marketState.spx}
                    ndx={marketState.ndx}
                    marketStatus={marketState.status}
                    selectedFormat={selectedFormat}
                    complicationSlots={complicationSlots}
                    onSlotChange={handleSlotChange}
                    onRefreshQuotes={handleRefreshQuotes}
                    isRefreshing={isRefreshing}
                  />
                )}

                {activeSurface === 'widget' && (
                  <MarketGlanceWidgetPreview
                    quotes={marketState.quotes}
                    lastUpdatedText={timeString}
                    onRefresh={handleRefreshQuotes}
                    isRefreshing={isRefreshing}
                  />
                )}

                {activeSurface === 'mobile' && (
                  <MobileCompanionPreview
                    quotes={marketState.quotes}
                    isWatchConnected={marketState.isWatchConnected}
                    isSyncing={isRefreshing}
                    lastSyncedText={timeString}
                    onRefreshAndSync={handleRefreshQuotes}
                  />
                )}
              </div>

              {/* Right Stage: Contextual Controls & Architecture Deep Dive */}
              <div className="lg:col-span-5 space-y-6">
                {activeSurface === 'watch' && (
                  <>
                    <ComplicationConfigurator
                      spx={marketState.spx}
                      ndx={marketState.ndx}
                      selectedFormat={selectedFormat}
                      onFormatChange={setSelectedFormat}
                      complicationSlots={complicationSlots}
                      onSlotChange={handleSlotChange}
                    />

                    <div className="bg-neutral-900/40 border border-neutral-800/80 rounded-xl p-4 text-xs text-neutral-400 space-y-2">
                      <div className="flex items-center gap-2 font-semibold text-neutral-200">
                        <Watch className="w-4 h-4 text-sky-400" />
                        <span>Wear OS 5 Rotary &amp; ScalingLazyColumn</span>
                      </div>
                      <p className="leading-relaxed">
                        Scroll using the right-side rotating crown or mouse wheel. Uses <code className="text-sky-300">@Immutable</code> domain classes and zero-allocation card hoisting to maintain locked 60 FPS scrolling through major indices and all 11 sectors.
                      </p>
                    </div>
                  </>
                )}

                {activeSurface === 'widget' && (
                  <div className="bg-neutral-900/50 border border-neutral-800 rounded-xl p-5 space-y-4 text-xs text-neutral-300">
                    <div className="flex items-center gap-2 font-bold text-white text-sm">
                      <Layers className="w-4 h-4 text-emerald-400" />
                      <span>Jetpack Glance 1.1.0 AppWidget</span>
                    </div>
                    <p className="leading-relaxed text-neutral-400">
                      Declarative home screen widget matching the Pixel Watch aesthetic. Built in <code className="text-sky-300">:mobile</code> with Compose-style Glance modifiers, LazyColumn virtualization, and auto-refresh triggered by both WorkManager (every 15 min) and companion foreground sync.
                    </p>
                    <div className="space-y-2 pt-2 border-t border-neutral-800">
                      <div className="flex justify-between">
                        <span className="text-neutral-500">Framework</span>
                        <span className="font-mono text-neutral-300">androidx.glance:glance-appwidget:1.1.0</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-neutral-500">Update Trigger</span>
                        <span className="font-mono text-neutral-300">GlanceAppWidgetManager.updateAll()</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-neutral-500">Number Formatting</span>
                        <span className="font-mono text-neutral-300">Compact 1k/1M multiples</span>
                      </div>
                    </div>
                  </div>
                )}

                {activeSurface === 'mobile' && (
                  <div className="bg-neutral-900/50 border border-neutral-800 rounded-xl p-5 space-y-4 text-xs text-neutral-300">
                    <div className="flex items-center gap-2 font-bold text-white text-sm">
                      <Smartphone className="w-4 h-4 text-sky-400" />
                      <span>Material 3 Mobile Companion</span>
                    </div>
                    <p className="leading-relaxed text-neutral-400">
                      The phone companion offloads all battery-heavy operations from the watch. It polls the Yahoo Finance v8 API concurrently via KMP <code className="text-sky-300">SharedMarketClient</code> (injected with Koin) and pushes serialized quotes through Google Play Services <code className="text-sky-300">DataClient</code> without waking the watch on background syncs.
                    </p>
                    <div className="space-y-2 pt-2 border-t border-neutral-800">
                      <div className="flex justify-between">
                        <span className="text-neutral-500">Shared KMP Engine</span>
                        <span className="font-mono text-neutral-300">Ktor async / awaitAll (~800ms)</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-neutral-500">Background Cadence</span>
                        <span className="font-mono text-neutral-300">30m + off-hours 6h gate</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-neutral-500">Offloading Policy</span>
                        <span className="font-mono text-neutral-300">Non-urgent DataClient batching</span>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* Engineering & Architecture Highlights */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 pt-2">
              <div className="bg-neutral-900/50 border border-neutral-800 rounded-xl p-5 space-y-2.5">
                <div className="flex items-center gap-2 text-sm font-bold text-white">
                  <Cpu className="w-4 h-4 text-purple-400" />
                  <span>Kotlin Multiplatform (:shared)</span>
                </div>
                <p className="text-xs text-neutral-400 leading-relaxed">
                  Shared <code className="text-purple-300">commonMain</code> models, <code className="text-purple-300">SharedMarketClient</code> (concurrent Ktor <code className="text-purple-300">async/awaitAll</code>), and <code className="text-purple-300">isUsMarketWindowActive()</code> compile for Android, Wear OS, and iOS (<code className="text-purple-300">MarketWearShared</code>).
                </p>
              </div>

              <div className="bg-neutral-900/50 border border-neutral-800 rounded-xl p-5 space-y-2.5">
                <div className="flex items-center gap-2 text-sm font-bold text-white">
                  <Network className="w-4 h-4 text-sky-400" />
                  <span>Koin 4.0.0 Dependency Injection</span>
                </div>
                <p className="text-xs text-neutral-400 leading-relaxed">
                  Zero-KSP pure-Kotlin DI graph uniting <code className="text-sky-300">sharedModule</code>, <code className="text-sky-300">mobileModule</code>, and <code className="text-sky-300">wearModule</code>, with <code className="text-sky-300">initKoin()</code> for iOS Swift, <code className="text-sky-300">koinViewModel()</code> for Compose, and <code className="text-sky-300">KoinComponent</code> for Workers and Complications.
                </p>
              </div>

              <div className="bg-neutral-900/50 border border-neutral-800 rounded-xl p-5 space-y-2.5">
                <div className="flex items-center gap-2 text-sm font-bold text-white">
                  <BatteryCharging className="w-4 h-4 text-emerald-400" />
                  <span>Battery &amp; Radio Optimizations</span>
                </div>
                <p className="text-xs text-neutral-400 leading-relaxed">
                  Market-hours sync gating skips overnight/weekend polling when cache is &lt;6h old, Room freshness guards prevent redundant fetches, retry-storm suppression avoids backoff wakeups, and complications read offline Room DB (<code className="text-emerald-300">3600s</code> period).
                </p>
              </div>
            </div>
          </div>
        )}

        {/* TAB CONTENT: System Diagrams */}
        {activeTab === 'architecture' && (
          <div className="space-y-6">
            <SystemDiagrams />
          </div>
        )}

        {/* TAB CONTENT: Complication Formats */}
        {activeTab === 'config' && (
          <div className="space-y-6">
            <ComplicationConfigurator
              spx={marketState.spx}
              ndx={marketState.ndx}
              selectedFormat={selectedFormat}
              onFormatChange={setSelectedFormat}
              complicationSlots={complicationSlots}
              onSlotChange={handleSlotChange}
            />
          </div>
        )}

        {/* TAB CONTENT: Source Code */}
        {activeTab === 'source' && (
          <div className="space-y-6">
            <AndroidCodeViewer />
          </div>
        )}

        {/* TAB CONTENT: Installation */}
        {activeTab === 'install' && (
          <div className="space-y-6">
            <InstallationGuide />
          </div>
        )}
      </main>

      {/* Global Footer */}
      <footer className="border-t border-neutral-900 bg-neutral-950 py-6 px-6 text-xs text-neutral-500">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <span>MarketWear Multi-Device Financial Ecosystem</span>
            <span>·</span>
            <span>Wear OS 5 / Android 14+</span>
            <span>·</span>
            <a
              href="https://github.com/mohitb117/MarketWearWatch"
              target="_blank"
              rel="noreferrer"
              className="text-sky-400 hover:underline"
            >
              GitHub Repository
            </a>
          </div>
          <div>
            Built with Jetpack Compose &amp; Glance
          </div>
        </div>
      </footer>
    </div>
  );
}
