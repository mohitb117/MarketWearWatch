import React from 'react';
import { IndexQuote, MarketStatus } from '../data/marketData';
import { SectorIcon } from './SectorIcon';
import { TrendingUp, TrendingDown, Clock, Activity, Zap, Layers } from 'lucide-react';

interface MarketIndicesStripProps {
  spx: IndexQuote;
  ndx: IndexQuote;
  quotes?: IndexQuote[];
  marketStatus: MarketStatus;
  statusText: string;
  onSimulateTick: () => void;
  onRefreshQuotes: () => void;
  isRefreshing: boolean;
}

export const MarketIndicesStrip: React.FC<MarketIndicesStripProps> = ({
  spx,
  ndx,
  quotes = [],
  marketStatus,
  statusText,
  onSimulateTick,
  onRefreshQuotes,
  isRefreshing,
}) => {
  const sectors = quotes.filter((q) => q.category === 'SECTOR');

  const renderIndexCard = (quote: IndexQuote) => {
    const isPositive = quote.change >= 0;
    const sign = isPositive ? '+' : '';
    const trendColor = isPositive ? 'text-emerald-400' : 'text-rose-400';
    const trendBg = isPositive ? 'bg-emerald-500/10' : 'bg-rose-500/10';
    const trendBorder = isPositive ? 'border-emerald-500/20' : 'border-rose-500/20';

    const spark = quote.sparkline && quote.sparkline.length > 1 ? quote.sparkline : [quote.price * 0.99, quote.price];
    const min = Math.min(...spark);
    const max = Math.max(...spark);
    const range = Math.max(1, max - min);
    const width = 120;
    const height = 36;
    const points = spark
      .map((val, idx) => {
        const x = (idx / (spark.length - 1)) * width;
        const y = height - ((val - min) / range) * (height - 8) - 4;
        return `${x},${y}`;
      })
      .join(' ');

    const dayRange = Math.max(1, quote.dayHigh - quote.dayLow);
    const dayProgress = Math.min(100, Math.max(0, ((quote.price - quote.dayLow) / dayRange) * 100));

    return (
      <div className="flex-1 bg-neutral-900/90 border border-neutral-800 rounded-xl p-4 flex flex-col justify-between">
        <div className="flex items-center justify-between">
          <div>
            <div className="flex items-center gap-2">
              <span className="font-bold text-white text-base">{quote.name}</span>
              <span className="text-xs text-neutral-400 font-mono-numbers">{quote.symbol}</span>
            </div>
            <div className="text-xs text-neutral-500">Benchmark Index</div>
          </div>

          <div className={`flex items-center gap-1 px-2.5 py-1 rounded-md border text-xs font-semibold font-mono-numbers ${trendBg} ${trendBorder} ${trendColor}`}>
            {isPositive ? <TrendingUp className="w-3.5 h-3.5" /> : <TrendingDown className="w-3.5 h-3.5" />}
            <span>{sign}{quote.percentChange.toFixed(2)}%</span>
          </div>
        </div>

        <div className="my-3 flex items-baseline justify-between">
          <div>
            <div className="text-2xl font-black font-mono-numbers tracking-tight text-white">
              {quote.formattedPoints}
            </div>
            <div className={`text-xs font-semibold font-mono-numbers ${trendColor}`}>
              {sign}{quote.change.toFixed(2)} pts today
            </div>
          </div>

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
    <div className="space-y-4">
      {/* Session status banner */}
      <div className="flex flex-wrap items-center justify-between gap-3 px-4 py-2.5 bg-neutral-900/60 border border-neutral-800/80 rounded-xl text-xs">
        <div className="flex items-center gap-2">
          <span className="relative flex h-2.5 w-2.5">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75" />
            <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-emerald-500" />
          </span>
          <span className="font-semibold text-neutral-200">{statusText}</span>
          <span className="text-neutral-500">· 15-min Room DB Cache</span>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={onSimulateTick}
            className="px-2.5 py-1 rounded bg-neutral-800 hover:bg-neutral-700 text-neutral-200 text-xs font-medium transition-colors cursor-pointer"
          >
            Simulate Price Tick
          </button>
        </div>
      </div>

      {/* Major Benchmark Cards */}
      <div className="flex flex-col md:flex-row gap-4">
        {renderIndexCard(spx)}
        {renderIndexCard(ndx)}
      </div>

      {/* 11 GICS Sectors Performance Bar */}
      {sectors.length > 0 && (
        <div className="bg-neutral-900/80 border border-neutral-800 rounded-xl p-3">
          <div className="flex items-center justify-between mb-2 px-1">
            <div className="flex items-center gap-1.5 text-xs font-bold text-neutral-300">
              <Layers className="w-3.5 h-3.5 text-sky-400" />
              <span>GICS Sector Performance (11 Sectors)</span>
            </div>
            <span className="text-[10px] text-neutral-500 font-mono">Compact 1k/1M multiples</span>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-2">
            {sectors.map((sector) => {
              const isPositive = sector.change >= 0;
              const trendColor = isPositive ? 'text-emerald-400' : 'text-rose-400';
              return (
                <div
                  key={sector.symbol}
                  className="bg-neutral-950/80 border border-neutral-800/60 rounded-lg p-2 flex flex-col justify-between text-xs"
                >
                  <div className="flex items-center gap-1 text-[11px] font-semibold text-neutral-300 truncate">
                    <SectorIcon symbol={sector.symbol} className="w-3 h-3 text-neutral-400 shrink-0" />
                    <span className="truncate">{sector.name}</span>
                  </div>
                  <div className="flex items-baseline justify-between mt-1 pt-1 border-t border-neutral-900 font-mono-numbers">
                    <span className="font-bold text-white text-[11px]">{sector.formattedCompactPoints}</span>
                    <span className={`text-[10px] font-semibold ${trendColor}`}>{sector.formattedPercent}</span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
};
