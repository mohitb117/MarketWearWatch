import React, { useState } from 'react';
import { IndexQuote } from '../data/marketData';
import { RefreshCw, Monitor, X, Play, Pin, Sparkles, CheckCircle2 } from 'lucide-react';

interface MacDesktopPreviewProps {
  quotes: IndexQuote[];
  lastUpdatedText: string;
  onRefresh: () => void;
  isRefreshing: boolean;
}

export const MacDesktopPreview: React.FC<MacDesktopPreviewProps> = ({
  quotes,
  lastUpdatedText,
  onRefresh,
  isRefreshing,
}) => {
  const [isNotchExpanded, setIsNotchExpanded] = useState<boolean>(true);
  const [isDeskWidgetVisible, setIsDeskWidgetVisible] = useState<boolean>(true);
  const [selectedScenario, setSelectedScenario] = useState<string>('All 37');

  const portfolioQuote = quotes.find((q) => q.category === 'PORTFOLIO');
  const spx = quotes.find((q) => q.symbol === 'SPX');
  const ndx = quotes.find((q) => q.symbol === 'NDX');
  const sectors = quotes.filter((q) => q.category === 'SECTOR');

  const price = portfolioQuote ? portfolioQuote.price : 100.84;
  const change = portfolioQuote ? portfolioQuote.change : 0.84;
  const pct = portfolioQuote ? portfolioQuote.percentChange : 0.84;
  const isPositive = pct >= 0;

  const sparklinePts = portfolioQuote && portfolioQuote.sparkline.length >= 2
    ? portfolioQuote.sparkline
    : [100.0, 100.12, 99.94, 100.08, 100.24, 100.19, 100.38, 100.52, 100.46, 100.68, 100.61, 100.84];

  const minVal = Math.min(...sparklinePts, 100.0) - 0.08;
  const maxVal = Math.max(...sparklinePts, 100.0) + 0.08;
  const span = Math.max(0.1, maxVal - minVal);
  const chartW = 320;
  const chartH = 44;

  const yFor = (v: number) => {
    const norm = Math.min(1, Math.max(0, (v - minVal) / span));
    return (chartH - 6) * (1 - norm) + 3;
  };
  const ptsString = sparklinePts
    .map((v, i) => `${((i / (sparklinePts.length - 1)) * chartW).toFixed(1)},${yFor(v).toFixed(1)}`)
    .join(' ');

  return (
    <div className="space-y-6">
      {/* Header and Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-neutral-900/60 border border-neutral-800 rounded-2xl p-4">
        <div>
          <div className="text-xs text-sky-400 font-semibold uppercase tracking-wider flex items-center gap-1.5">
            <Monitor className="w-4 h-4" />
            <span>macOS Native Modalities</span>
          </div>
          <div className="text-sm font-bold text-white mt-0.5">
            Dynamic Island Notch &amp; Floating Obsidian Desktop Widget
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => setIsDeskWidgetVisible(!isDeskWidgetVisible)}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all border ${
              isDeskWidgetVisible
                ? 'bg-sky-600 border-sky-500 text-white shadow-sm'
                : 'bg-neutral-950 border-neutral-800 text-neutral-400 hover:text-white'
            }`}
          >
            {isDeskWidgetVisible ? 'Hide Desktop Widget' : 'Show Desktop Widget'}
          </button>
          <button
            onClick={onRefresh}
            disabled={isRefreshing}
            className="px-3 py-1.5 rounded-lg text-xs font-semibold bg-neutral-800 hover:bg-neutral-700 text-neutral-200 border border-neutral-700 flex items-center gap-1.5 transition-all"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin text-sky-400' : ''}`} />
            <span>{isRefreshing ? 'Syncing...' : 'Sync Live'}</span>
          </button>
        </div>
      </div>

      {/* Screen Frame Mockup */}
      <div className="relative rounded-3xl border border-neutral-800 bg-gradient-to-b from-neutral-950 via-neutral-900 to-black p-6 overflow-hidden shadow-2xl min-h-[480px]">
        {/* Subtle macOS wallpaper backdrop effect */}
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(14,165,233,0.12),transparent_50%),radial-gradient(circle_at_bottom_left,rgba(99,102,241,0.08),transparent_50%)] pointer-events-none" />

        {/* 1. Dynamic Island Notch Surface */}
        <div className="relative z-10 flex flex-col items-center">
          <div className="w-full max-w-xl mx-auto flex flex-col items-center">
            {/* Notch Shell */}
            <div
              className={`bg-black border border-neutral-800/80 rounded-b-2xl shadow-2xl transition-all duration-300 overflow-hidden ${
                isNotchExpanded ? 'w-full p-4' : 'w-72 px-4 py-1.5 cursor-pointer hover:bg-neutral-950'
              }`}
              onClick={() => {
                if (!isNotchExpanded) setIsNotchExpanded(true);
              }}
            >
              {!isNotchExpanded ? (
                // Collapsed Notch Ear View
                <div className="flex items-center justify-between text-xs">
                  <div className="flex items-center gap-2">
                    <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                    <span className="font-bold text-neutral-200 tracking-tight">MarketWear</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="font-mono font-bold text-white">{price.toFixed(2)}</span>
                    <span className={`px-1.5 py-0.5 rounded text-[10px] font-bold ${
                      isPositive ? 'bg-emerald-950/80 text-emerald-400 border border-emerald-800/50' : 'bg-red-950/80 text-red-400 border border-red-800/50'
                    }`}>
                      {isPositive ? `+${pct.toFixed(2)}%` : `${pct.toFixed(2)}%`}
                    </span>
                  </div>
                </div>
              ) : (
                // Expanded Island View
                <div className="space-y-3.5">
                  <div className="flex items-center justify-between border-b border-neutral-900 pb-2">
                    <div className="flex items-center gap-2">
                      <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-pulse" />
                      <span className="text-xs font-bold text-neutral-200">MarketWear Dynamic Island</span>
                      <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-sky-950 text-sky-400 border border-sky-800/40">
                        37/37 Holdings
                      </span>
                    </div>

                    <div className="flex items-center gap-1.5 text-xs">
                      <button
                        onClick={() => setIsDeskWidgetVisible(!isDeskWidgetVisible)}
                        className={`px-2 py-0.5 rounded text-[10px] font-bold border transition-colors ${
                          isDeskWidgetVisible ? 'bg-sky-950 text-sky-400 border-sky-800/60' : 'bg-neutral-900 text-neutral-400 border-neutral-800'
                        }`}
                      >
                        DESK
                      </button>
                      <button
                        onClick={() => setIsNotchExpanded(false)}
                        className="w-5 h-5 rounded-full bg-neutral-900 hover:bg-neutral-800 text-neutral-400 hover:text-white flex items-center justify-center text-[10px]"
                        title="Collapse Notch"
                      >
                        ✕
                      </button>
                    </div>
                  </div>

                  <div className="flex items-baseline justify-between">
                    <div>
                      <div className="text-[10px] uppercase font-bold tracking-wider text-sky-400">Portfolio Score</div>
                      <div className="text-2xl font-black font-mono tracking-tight text-white">{price.toFixed(2)}</div>
                    </div>
                    <div className={`px-2 py-1 rounded-lg text-xs font-bold ${
                      isPositive ? 'bg-emerald-950 text-emerald-400 border border-emerald-800/40' : 'bg-red-950 text-red-400 border border-red-800/40'
                    }`}>
                      {isPositive ? `+${change.toFixed(2)} (+${pct.toFixed(2)}%)` : `${change.toFixed(2)} (${pct.toFixed(2)}%)`}
                    </div>
                  </div>

                  {/* Sparkline in Drawer */}
                  <div className="h-11 w-full bg-neutral-950/80 rounded-xl p-1 border border-neutral-900">
                    <svg className="w-full h-full overflow-visible" viewBox={`0 0 ${chartW} ${chartH}`}>
                      <polyline
                        fill="none"
                        stroke={isPositive ? '#10b981' : '#f43f5e'}
                        strokeWidth="2"
                        points={ptsString}
                      />
                    </svg>
                  </div>

                  {/* Preset Chips */}
                  <div className="flex items-center gap-1.5 pt-1 text-[11px] overflow-x-auto">
                    {['All 37', 'Top 10', 'Bull', 'Bear', 'Reset %'].map((preset) => (
                      <button
                        key={preset}
                        onClick={() => setSelectedScenario(preset)}
                        className={`px-2.5 py-1 rounded-md font-medium transition-all ${
                          selectedScenario === preset
                            ? 'bg-sky-900/60 text-sky-200 border border-sky-700/50'
                            : 'bg-neutral-900 hover:bg-neutral-800 text-neutral-400 hover:text-white border border-neutral-800'
                        }`}
                      >
                        {preset}
                      </button>
                    ))}
                    <div className="ml-auto">
                      <button
                        onClick={onRefresh}
                        className="px-2.5 py-1 rounded-md font-medium bg-neutral-900 hover:bg-neutral-800 text-sky-400 border border-sky-900/40 text-[10px]"
                      >
                        {isRefreshing ? 'Syncing...' : 'Sync'}
                      </button>
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* 2. Floating Obsidian Desktop Widget */}
        {isDeskWidgetVisible && (
          <div className="mt-8 flex justify-end">
            <div className="w-80 bg-neutral-950/90 backdrop-blur-xl border border-neutral-800/90 rounded-2xl p-4 shadow-2xl relative animate-in fade-in zoom-in-95 duration-200">
              {/* Header */}
              <div className="flex items-center justify-between pb-2 border-b border-neutral-900 text-xs">
                <div className="flex items-center gap-2">
                  <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                  <span className="font-bold uppercase tracking-wider text-sky-400 text-[10px]">PORTFOLIO</span>
                  <span className="text-[10px] text-neutral-500 font-mono">37/37</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className={`px-1.5 py-0.5 rounded text-[10px] font-bold ${
                    isPositive ? 'bg-emerald-950 text-emerald-400 border border-emerald-800/40' : 'bg-red-950 text-red-400 border border-red-800/40'
                  }`}>
                    {isPositive ? `+${pct.toFixed(2)}%` : `${pct.toFixed(2)}%`}
                  </span>
                  <button
                    onClick={() => setIsDeskWidgetVisible(false)}
                    className="w-4 h-4 rounded-full bg-neutral-900 hover:bg-neutral-800 text-neutral-400 hover:text-white flex items-center justify-center text-[9px]"
                  >
                    ✕
                  </button>
                </div>
              </div>

              {/* Price Row */}
              <div className="mt-2.5 flex items-baseline justify-between">
                <span className="text-2xl font-black font-mono text-white">{price.toFixed(2)}</span>
                <button
                  onClick={onRefresh}
                  className="px-2 py-0.5 rounded text-[10px] font-bold bg-sky-950 text-sky-400 border border-sky-800/40 flex items-center gap-1"
                >
                  <RefreshCw className={`w-2.5 h-2.5 ${isRefreshing ? 'animate-spin' : ''}`} />
                  <span>{isRefreshing ? 'SYNCING' : 'SYNC'}</span>
                </button>
              </div>

              {/* Bipolar gauge simulation */}
              <div className="mt-2 w-full h-1.5 bg-neutral-900 rounded-full overflow-hidden flex">
                <div className="w-1/2 flex justify-end">
                  {!isPositive && <div className="h-full bg-red-500 rounded-l-full" style={{ width: `${Math.min(100, Math.abs(pct) * 35)}%` }} />}
                </div>
                <div className="w-0.5 h-full bg-neutral-600" />
                <div className="w-1/2 flex justify-start">
                  {isPositive && <div className="h-full bg-emerald-500 rounded-r-full" style={{ width: `${Math.min(100, pct * 35)}%` }} />}
                </div>
              </div>

              {/* Intraday Sparkline */}
              <div className="mt-2.5 h-12 w-full bg-neutral-900/40 rounded-lg p-1 border border-neutral-900/60">
                <svg className="w-full h-full overflow-visible" viewBox={`0 0 ${chartW} ${chartH}`}>
                  <polyline
                    fill="none"
                    stroke={isPositive ? '#10b981' : '#f43f5e'}
                    strokeWidth="1.75"
                    points={ptsString}
                  />
                </svg>
              </div>

              {/* Core Index Pills */}
              <div className="mt-2.5 flex items-center gap-2 text-[10px] font-mono">
                {spx && (
                  <div className="flex-1 bg-neutral-900/80 px-2 py-1 rounded border border-neutral-800 flex items-center justify-between">
                    <span className="text-sky-400 font-bold">SPX</span>
                    <span className="text-neutral-200">{spx.price.toFixed(0)}</span>
                    <span className={spx.percentChange >= 0 ? 'text-emerald-400' : 'text-red-400'}>
                      {spx.percentChange >= 0 ? `+${spx.percentChange.toFixed(2)}%` : `${spx.percentChange.toFixed(2)}%`}
                    </span>
                  </div>
                )}
                {ndx && (
                  <div className="flex-1 bg-neutral-900/80 px-2 py-1 rounded border border-neutral-800 flex items-center justify-between">
                    <span className="text-sky-400 font-bold">NDX</span>
                    <span className="text-neutral-200">{ndx.price.toFixed(0)}</span>
                    <span className={ndx.percentChange >= 0 ? 'text-emerald-400' : 'text-red-400'}>
                      {ndx.percentChange >= 0 ? `+${ndx.percentChange.toFixed(2)}%` : `${ndx.percentChange.toFixed(2)}%`}
                    </span>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Architecture Highlights Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-neutral-900/40 border border-neutral-800 rounded-xl p-4 space-y-1.5">
          <div className="text-xs font-bold text-sky-400 flex items-center gap-1.5">
            <CheckCircle2 className="w-3.5 h-3.5" />
            <span>Zero Swift Networking</span>
          </div>
          <p className="text-xs text-neutral-400 leading-relaxed">
            All Yahoo Finance API fetching, mutex coalescing, JSON parsing, and portfolio math are encapsulated entirely inside the Kotlin Multiplatform (:shared) core engine.
          </p>
        </div>

        <div className="bg-neutral-900/40 border border-neutral-800 rounded-xl p-4 space-y-1.5">
          <div className="text-xs font-bold text-sky-400 flex items-center gap-1.5">
            <CheckCircle2 className="w-3.5 h-3.5" />
            <span>Loopback Bridge Daemon</span>
          </div>
          <p className="text-xs text-neutral-400 leading-relaxed">
            A background KMP daemon runs on port 49172, providing real-time snapshot streams and atomic disk updates to `/tmp/marketwear_island_state.json` for instant restarts.
          </p>
        </div>

        <div className="bg-neutral-900/40 border border-neutral-800 rounded-xl p-4 space-y-1.5">
          <div className="text-xs font-bold text-sky-400 flex items-center gap-1.5">
            <CheckCircle2 className="w-3.5 h-3.5" />
            <span>Market-Open Invalidation</span>
          </div>
          <p className="text-xs text-neutral-400 leading-relaxed">
            Cross-platform `hasMarketOpenedSince()` invalidates pre-market caches the exact second regular trading begins at 9:30 AM ET across watch, phone, and desktop.
          </p>
        </div>
      </div>
    </div>
  );
};
