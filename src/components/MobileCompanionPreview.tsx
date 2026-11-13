import React, { useState } from 'react';
import { IndexQuote, toUnnormalizedPortfolioQuote } from '../data/marketData';
import { SectorIcon } from './SectorIcon';
import { RefreshCw, Watch } from 'lucide-react';

interface MobileCompanionPreviewProps {
  quotes: IndexQuote[];
  isWatchConnected: boolean;
  isSyncing: boolean;
  lastSyncedText: string;
  onRefreshAndSync: () => void;
}

export const MobileCompanionPreview: React.FC<MobileCompanionPreviewProps> = ({
  quotes,
  isWatchConnected,
  isSyncing,
  lastSyncedText,
  onRefreshAndSync,
}) => {
  const [isNormalizedMode, setIsNormalizedMode] = useState<boolean>(true);
  const portfolioQuote = quotes.find((q) => q.category === 'PORTFOLIO');
  const indices = quotes.filter((q) => q.category === 'INDEX');
  const sectors = quotes.filter((q) => q.category === 'SECTOR');

  const renderMobileSparklineSvg = (primaryPts: number[], secondaryPts: number[], isPositive: boolean) => {
    const pts = primaryPts.length >= 2 ? primaryPts : [100.0, 100.0];
    const secPts = secondaryPts.length >= 2 ? secondaryPts : pts;
    const allVals = [...pts, ...secPts, 100.0];
    const minVal = Math.min(...allVals) - 0.1;
    const maxVal = Math.max(...allVals) + 0.1;
    const span = Math.max(0.1, maxVal - minVal);
    const width = 300;
    const height = 68;

    const yFor = (v: number) => {
      const norm = Math.min(1, Math.max(0, (v - minVal) / span));
      return (height - 10) * (1 - norm) + 5;
    };

    const baseY = yFor(100.0);
    const primaryPointsStr = pts
      .map((v, idx) => `${((idx / (pts.length - 1)) * width).toFixed(1)},${yFor(v).toFixed(1)}`)
      .join(' ');
    const secondaryPointsStr = secPts
      .map((v, idx) => `${((idx / (secPts.length - 1)) * width).toFixed(1)},${yFor(v).toFixed(1)}`)
      .join(' ');

    return (
      <svg className="w-full h-[68px] overflow-visible" viewBox={`0 0 ${width} ${height}`}>
        <line
          x1="0"
          y1={baseY}
          x2={width}
          y2={baseY}
          stroke="#78909C"
          strokeWidth="1"
          strokeDasharray="6 6"
        />
        <polyline
          fill="none"
          stroke="#1E88E5"
          strokeOpacity="0.6"
          strokeWidth="1.75"
          strokeDasharray="5 5"
          strokeLinecap="round"
          points={secondaryPointsStr}
        />
        <polyline
          fill="none"
          stroke={isPositive ? '#10b981' : '#f43f5e'}
          strokeWidth="2.5"
          strokeLinecap="round"
          points={primaryPointsStr}
        />
      </svg>
    );
  };

  return (
    <div className="flex flex-col items-center">
      {/* Smartphone frame simulating Samsung Galaxy Z Fold 6 / Pixel Phone */}
      <div className="relative w-full max-w-[380px] bg-neutral-950 rounded-[32px] border-4 border-neutral-800 shadow-2xl overflow-hidden text-white flex flex-col h-[520px]">
        {/* Phone Notch / Status Bar */}
        <div className="bg-neutral-900/90 px-6 pt-3 pb-2 flex items-center justify-between text-[11px] text-neutral-400 font-mono-numbers border-b border-neutral-800">
          <span>9:41 AM</span>
          <div className="flex items-center gap-1.5">
            <span>5G</span>
            <span>100%</span>
          </div>
        </div>

        {/* Top App Bar (Material 3) */}
        <div className="bg-neutral-900 px-4 py-3 flex items-center justify-between border-b border-neutral-800/80">
          <div>
            <h2 className="text-base font-bold text-white tracking-tight">MarketWear</h2>
            <p className="text-[10px] text-neutral-400">Mobile Companion</p>
          </div>

          {/* Watch Status Chip */}
          <div
            className={`flex items-center gap-1 px-2.5 py-1 rounded-full text-[11px] font-semibold border ${
              isWatchConnected
                ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400'
                : 'bg-neutral-800 border-neutral-700 text-neutral-400'
            }`}
          >
            <Watch className="w-3 h-3" />
            <span>{isWatchConnected ? 'Pixel Watch Connected' : 'Disconnected'}</span>
          </div>
        </div>

        {/* Scrollable Dashboard Body */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4 no-scrollbar" style={{ scrollbarWidth: 'none' }}>
          {/* Portfolio Daily Score (100-Baseline) Hero Card */}
          {portfolioQuote && (() => {
            const unnormalizedQuote = toUnnormalizedPortfolioQuote(portfolioQuote);
            const activeQuote = isNormalizedMode ? portfolioQuote : unnormalizedQuote;
            const secondaryQuote = isNormalizedMode ? unnormalizedQuote : portfolioQuote;
            const isPositive = activeQuote.change >= 0;
            const trendColor = isPositive ? 'text-emerald-400' : 'text-rose-400';

            return (
              <div className="bg-neutral-900/95 border border-sky-500/25 rounded-2xl p-3.5 space-y-2.5 shadow-lg">
                <div className="flex items-center justify-between gap-2">
                  <div>
                    <h3 className="text-xs font-bold text-white">Portfolio Daily Score</h3>
                    <p className="text-[10px] text-neutral-400">
                      37 UBS &amp; Fidelity Holdings · 100.0 Baseline
                    </p>
                  </div>
                  <button
                    onClick={() => setIsNormalizedMode(!isNormalizedMode)}
                    className="px-2.5 py-1 rounded-full bg-sky-500/15 hover:bg-sky-500/25 border border-sky-500/30 text-[10px] font-bold text-sky-400 transition-colors cursor-pointer shrink-0"
                  >
                    {isNormalizedMode ? 'Normalized (100%)' : 'Raw (59.4%)'}
                  </button>
                </div>

                <div className="flex items-end justify-between">
                  <div>
                    <div className="text-2xl font-black text-white font-mono-numbers leading-none">
                      {activeQuote.formattedScore}
                    </div>
                    <div className={`text-[11px] font-semibold font-mono-numbers mt-1 ${trendColor}`}>
                      {activeQuote.formattedChange} ({activeQuote.formattedPercent}) from 100.00
                    </div>
                  </div>
                  <div className="text-right font-mono-numbers text-[10px] text-neutral-400 space-y-0.5">
                    <div>
                      {isNormalizedMode
                        ? `Raw (59.4%): ${unnormalizedQuote.formattedScore} (${unnormalizedQuote.formattedPercent})`
                        : `Norm (100%): ${portfolioQuote.formattedScore} (${portfolioQuote.formattedPercent})`}
                    </div>
                    <div>
                      Range: {activeQuote.dayLow.toFixed(2)} – {activeQuote.dayHigh.toFixed(2)}
                    </div>
                  </div>
                </div>

                <div className="bg-neutral-950/80 border border-neutral-800/80 rounded-xl p-2">
                  {renderMobileSparklineSvg(
                    activeQuote.sparkline,
                    secondaryQuote.sparkline,
                    isPositive
                  )}
                </div>

                <div className="flex items-center justify-between text-[9px] text-neutral-400 font-mono">
                  <span>
                    Vico 3.3.1 · Solid: {isNormalizedMode ? 'Norm (100%)' : 'Raw (59.4%)'} · Dashed:{' '}
                    {isNormalizedMode ? 'Raw' : 'Norm'}
                  </span>
                  <span>2h Throttle</span>
                </div>
              </div>
            );
          })()}

          {/* Major Indices Section */}
          <div className="space-y-2">
            <h3 className="text-xs font-bold text-neutral-400 uppercase tracking-wider">
              Major Benchmark Indices
            </h3>
            <div className="grid grid-cols-2 gap-2">
              {indices.map((quote) => {
                const isPositive = quote.change >= 0;
                const trendColor = isPositive ? 'text-emerald-400' : 'text-rose-400';
                return (
                  <div
                    key={quote.symbol}
                    className="bg-neutral-900/90 border border-neutral-800 rounded-xl p-3 flex flex-col justify-between"
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-neutral-300">{quote.name}</span>
                      <span className={`text-[11px] font-bold font-mono-numbers ${trendColor}`}>
                        {quote.formattedPercent}
                      </span>
                    </div>
                    <div className="mt-2">
                      <div className="text-lg font-black text-white font-mono-numbers">
                        {quote.formattedPoints}
                      </div>
                      <div className={`text-[10px] font-semibold font-mono-numbers ${trendColor}`}>
                        {quote.formattedChange} pts
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* 11 GICS Market Sectors Section */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-bold text-neutral-400 uppercase tracking-wider">
                US Market Sectors ({sectors.length})
              </h3>
              <span className="text-[10px] text-neutral-500 font-mono">GICS Standard</span>
            </div>

            <div className="space-y-1.5">
              {sectors.map((quote) => {
                const isPositive = quote.change >= 0;
                const trendColor = isPositive ? 'text-emerald-400' : 'text-rose-400';
                return (
                  <div
                    key={quote.symbol}
                    className="bg-neutral-900/80 hover:bg-neutral-900 border border-neutral-800/80 rounded-xl p-2.5 flex items-center justify-between transition-colors"
                  >
                    <div className="flex items-center gap-2.5 min-w-0">
                      <div className="p-1.5 rounded-lg bg-neutral-800 text-neutral-300">
                        <SectorIcon symbol={quote.symbol} className="w-3.5 h-3.5" />
                      </div>
                      <div className="min-w-0">
                        <div className="text-xs font-bold text-white truncate">{quote.name}</div>
                        <div className="text-[10px] text-neutral-400 font-mono">{quote.symbol}</div>
                      </div>
                    </div>

                    <div className="text-right font-mono-numbers shrink-0">
                      <div className="text-xs font-extrabold text-white">{quote.formattedCompactPoints}</div>
                      <div className={`text-[10px] font-semibold ${trendColor}`}>{quote.formattedPercent}</div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        {/* Bottom Sync Bar */}
        <div className="bg-neutral-900 border-t border-neutral-800 p-3 flex flex-col gap-2">
          <div className="text-[10px] text-neutral-400 text-center font-mono">
            Last synced to watch: {lastSyncedText}
          </div>
          <button
            onClick={onRefreshAndSync}
            disabled={isSyncing}
            className="w-full py-2.5 rounded-xl bg-sky-600 hover:bg-sky-500 text-white text-xs font-bold transition-all flex items-center justify-center gap-1.5 cursor-pointer disabled:opacity-50"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isSyncing ? 'animate-spin' : ''}`} />
            <span>{isSyncing ? 'Fetching & Pushing to Watch...' : 'Refresh & Sync to Watch'}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
