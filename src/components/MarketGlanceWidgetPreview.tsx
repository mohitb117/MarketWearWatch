import React from 'react';
import { IndexQuote } from '../data/marketData';
import { SectorIcon } from './SectorIcon';
import { RefreshCw, Smartphone } from 'lucide-react';

interface MarketGlanceWidgetPreviewProps {
  quotes: IndexQuote[];
  lastUpdatedText: string;
  onRefresh: () => void;
  isRefreshing: boolean;
}

export const MarketGlanceWidgetPreview: React.FC<MarketGlanceWidgetPreviewProps> = ({
  quotes,
  lastUpdatedText,
  onRefresh,
  isRefreshing,
}) => {
  const indices = quotes.filter((q) => q.category === 'INDEX');
  const sectors = quotes.filter((q) => q.category === 'SECTOR');

  return (
    <div className="flex flex-col items-center">
      {/* Widget Container simulating Android 4x2 Home Screen Widget */}
      <div className="relative w-full max-w-[360px] sm:max-w-[400px] bg-[#121212] rounded-[24px] p-4 border border-neutral-800 shadow-2xl text-white">
        {/* Widget Top Header Bar */}
        <div className="flex items-center justify-between pb-3 border-b border-white/5">
          <div className="flex items-center gap-2">
            <div className="w-2 h-2 rounded-full bg-emerald-400" />
            <span className="text-[14px] font-bold tracking-tight text-white">Market Indices</span>
            <span className="text-[10px] text-neutral-400 font-mono">Glance 1.1.0</span>
          </div>

          <div className="flex items-center gap-2">
            <span className="text-[10px] text-neutral-400 font-mono-numbers">{lastUpdatedText}</span>
            <button
              onClick={onRefresh}
              disabled={isRefreshing}
              className="p-1 rounded-full hover:bg-neutral-800 text-neutral-300 transition-colors cursor-pointer disabled:opacity-50"
              title="Refresh Glance Widget"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>

        {/* Scrollable Widget Content */}
        <div className="max-h-[300px] overflow-y-auto pt-3 space-y-2 pr-1 no-scrollbar" style={{ scrollbarWidth: 'none' }}>
          {/* Major Indices */}
          {indices.map((quote) => {
            const isPositive = quote.change >= 0;
            const trendColor = isPositive ? 'text-[#4CAF50]' : 'text-[#F44336]';
            return (
              <div
                key={quote.symbol}
                className="bg-[#1E1E1E] rounded-[10px] p-2.5 flex items-center justify-between border border-white/5"
              >
                <div className="flex items-center gap-2">
                  <SectorIcon symbol={quote.symbol} className="text-white w-4 h-4" />
                  <span className="text-[13px] font-bold text-white">{quote.name}</span>
                </div>
                <div className="flex items-center gap-3 text-right font-mono-numbers">
                  <span className="text-[14px] font-extrabold text-white">{quote.formattedPoints}</span>
                  <span className={`text-[12px] font-bold ${trendColor}`}>{quote.formattedPercent}</span>
                </div>
              </div>
            );
          })}

          {/* Sectors Header */}
          <div className="pt-2 pb-1 text-[11px] font-bold text-neutral-400 uppercase tracking-wider">
            Sectors ({sectors.length})
          </div>

          {/* Sector Items */}
          {sectors.map((quote) => {
            const isPositive = quote.change >= 0;
            const trendColor = isPositive ? 'text-[#4CAF50]' : 'text-[#F44336]';
            return (
              <div
                key={quote.symbol}
                className="bg-[#1E1E1E] rounded-[10px] p-2 flex items-center justify-between border border-white/5"
              >
                <div className="flex items-center gap-2 min-w-0">
                  <div className="shrink-0 text-neutral-300">
                    <SectorIcon symbol={quote.symbol} className="w-3.5 h-3.5" />
                  </div>
                  <span className="text-[12px] font-bold text-white truncate">{quote.name}</span>
                </div>
                <div className="flex items-center gap-2 shrink-0 text-right font-mono-numbers">
                  <span className="text-[13px] font-bold text-white">{quote.formattedCompactPoints}</span>
                  <span className={`text-[11px] font-semibold ${trendColor}`}>{quote.formattedPercent}</span>
                </div>
              </div>
            );
          })}
        </div>

        {/* Bottom Glance Watermark */}
        <div className="mt-3 pt-2 border-t border-white/5 flex items-center justify-between text-[10px] text-neutral-500">
          <span>Android Home Screen AppWidget</span>
          <span>Auto-updates every 15 min</span>
        </div>
      </div>
    </div>
  );
};
