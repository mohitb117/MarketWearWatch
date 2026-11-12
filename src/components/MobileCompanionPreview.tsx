import React from 'react';
import { IndexQuote } from '../data/marketData';
import { SectorIcon } from './SectorIcon';
import { RefreshCw, Watch, CheckCircle2, TrendingUp, TrendingDown } from 'lucide-react';

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
  const indices = quotes.filter((q) => q.category === 'INDEX');
  const sectors = quotes.filter((q) => q.category === 'SECTOR');

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
