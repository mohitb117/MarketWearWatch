import React from 'react';
import { MarketIndex, MarketStatus } from '../data/marketData';
import { TrendingUp, TrendingDown, Clock, Activity, Zap } from 'lucide-react';

interface MarketIndicesStripProps {
  spx: MarketIndex;
  ndx: MarketIndex;
  marketStatus: MarketStatus;
  statusText: string;
  onSimulateTick: () => void;
  onRefreshQuotes: () => void;
  isRefreshing: boolean;
}

export const MarketIndicesStrip: React.FC<MarketIndicesStripProps> = ({
  spx,
  ndx,
  marketStatus,
  statusText,
  onSimulateTick,
  onRefreshQuotes,
  isRefreshing,
}) => {
  const renderIndexCard = (quote: MarketIndex) => {
    const isPositive = quote.change >= 0;
    const sign = isPositive ? '+' : '';
    const trendColor = isPositive ? 'text-emerald-400' : 'text-rose-400';
    const trendBg = isPositive ? 'bg-emerald-500/10' : 'bg-rose-500/10';
    const trendBorder = isPositive ? 'border-emerald-500/20' : 'border-rose-500/20';

    // Sparkline SVG path
    const min = Math.min(...quote.sparkline);
    const max = Math.max(...quote.sparkline);
    const range = Math.max(1, max - min);
    const width = 120;
    const height = 36;
    const points = quote.sparkline.map((val, idx) => {
      const x = (idx / (quote.sparkline.length - 1)) * width;
      const y = height - ((val - min) / range) * (height - 8) - 4;
      return `${x},${y}`;
    }).join(' ');

    // Range calculation
    const dayRange = Math.max(1, quote.dayHigh - quote.dayLow);
    const dayProgress = Math.min(100, Math.max(0, ((quote.price - quote.dayLow) / dayRange) * 100));

    return (
      <div className="flex-1 bg-neutral-900/90 border border-neutral-800 rounded-xl p-4 flex flex-col justify-between">
        {/* Top Header */}
        <div className="flex items-center justify-between">
          <div>
            <div className="flex items-center gap-2">
              <span className="font-bold text-white text-base">{quote.name}</span>
              <span className="text-xs text-neutral-400 font-mono-numbers">{quote.ticker}</span>
            </div>
            <div className="text-xs text-neutral-500">Benchmark Index</div>
          </div>

          <div className={`flex items-center gap-1 px-2.5 py-1 rounded-md border text-xs font-semibold font-mono-numbers ${trendBg} ${trendBorder} ${trendColor}`}>
            {isPositive ? <TrendingUp className="w-3.5 h-3.5" /> : <TrendingDown className="w-3.5 h-3.5" />}
            <span>{sign}{quote.percentChange.toFixed(2)}%</span>
          </div>
        </div>

        {/* Main Price & Sparkline */}
        <div className="my-3 flex items-baseline justify-between">
          <div>
            <div className="text-2xl font-black font-mono-numbers tracking-tight text-white">
              {quote.price.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            </div>
            <div className={`text-xs font-semibold font-mono-numbers ${trendColor}`}>
              {sign}{quote.change.toFixed(2)} pts today
            </div>
          </div>

          {/* Sparkline Graphic */}
          <div className="w-[120px] h-[36px]">
            <svg width={width} height={height} className="overflow-visible">
              <polyline
                fill="none"
                stroke={isPositive ? '#34d399' : '#f87171'}
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
                points={points}
              />
            </svg>
          </div>
        </div>

        {/* Day Range Bar */}
        <div className="space-y-1.5 pt-2 border-t border-neutral-800/80">
          <div className="flex justify-between text-[11px] font-mono-numbers text-neutral-400">
            <span>Low {quote.dayLow.toLocaleString()}</span>
            <span className="text-neutral-500">Day Range</span>
            <span>High {quote.dayHigh.toLocaleString()}</span>
          </div>
          <div className="relative w-full h-1.5 bg-neutral-800 rounded-full overflow-hidden">
            <div
              className={`h-full rounded-full transition-all duration-500 ${isPositive ? 'bg-emerald-500' : 'bg-rose-500'}`}
              style={{ width: `${dayProgress}%` }}
            />
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="space-y-3">
      {/* Session Header Status */}
      <div className="flex flex-wrap items-center justify-between gap-3 px-1 text-xs">
        <div className="flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
          <span className="font-semibold text-neutral-200">{statusText}</span>
          <span className="text-neutral-500">·</span>
          <span className="text-neutral-400">Refreshes every 15 min on Pixel Watch (battery optimized)</span>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={onSimulateTick}
            className="flex items-center gap-1.5 px-3 py-1 rounded-md bg-neutral-800 hover:bg-neutral-700 text-neutral-300 text-xs font-medium transition-colors"
            title="Simulate realistic market movement"
          >
            <Zap className="w-3.5 h-3.5 text-amber-400" />
            <span>Simulate Tick</span>
          </button>
          <button
            onClick={onRefreshQuotes}
            disabled={isRefreshing}
            className="flex items-center gap-1.5 px-3 py-1 rounded-md bg-sky-600 hover:bg-sky-500 text-white text-xs font-medium transition-colors disabled:opacity-50"
          >
            <Activity className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin' : ''}`} />
            <span>{isRefreshing ? 'Syncing...' : 'Sync Market Data'}</span>
          </button>
        </div>
      </div>

      {/* S&P 500 and Nasdaq 100 Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {renderIndexCard(spx)}
        {renderIndexCard(ndx)}
      </div>
    </div>
  );
};
