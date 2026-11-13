import React, { useState, useEffect, useRef } from 'react';
import { IndexQuote, MarketStatus, getComplicationText, toUnnormalizedPortfolioQuote } from '../data/marketData';
import { SectorIcon } from './SectorIcon';
import {
  RefreshCw,
  ArrowUpRight,
  ArrowDownRight,
  Layers,
  Eye,
} from 'lucide-react';

export type WatchFaceStyle = 'concentric' | 'utility' | 'digital_sport' | 'minimal';
export type ComplicationSlotId = 'top' | 'bottom' | 'left' | 'right';
export type ComplicationContent = 'sp500' | 'nasdaq' | 'dual' | 'date' | 'battery' | 'none';

interface PixelWatch4SimulatorProps {
  quotes: IndexQuote[];
  spx: IndexQuote;
  ndx: IndexQuote;
  marketStatus: MarketStatus;
  selectedFormat: 'points' | 'percent' | 'change' | 'compact';
  complicationSlots: Record<ComplicationSlotId, ComplicationContent>;
  onSlotChange: (slot: ComplicationSlotId, content: ComplicationContent) => void;
  onRefreshQuotes: () => void;
  isRefreshing: boolean;
  syncSource?: 'data_layer' | 'standalone' | 'cached';
}

export const PixelWatch4Simulator: React.FC<PixelWatch4SimulatorProps> = ({
  quotes,
  spx,
  ndx,
  marketStatus,
  selectedFormat,
  complicationSlots,
  onSlotChange,
  onRefreshQuotes,
  isRefreshing,
  syncSource = 'data_layer',
}) => {
  const [faceStyle, setFaceStyle] = useState<WatchFaceStyle>('digital_sport');
  const [isAmbient, setIsAmbient] = useState<boolean>(false);
  const [currentTime, setCurrentTime] = useState<Date>(new Date());
  const [watchAppOpen, setWatchAppOpen] = useState<boolean>(false);
  const [crownRotation, setCrownRotation] = useState<number>(0);
  const [isNormalizedMode, setIsNormalizedMode] = useState<boolean>(true);

  const scrollContainerRef = useRef<HTMLDivElement>(null);

  // Clock tick
  useEffect(() => {
    const timer = setInterval(() => setCurrentTime(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  const hours = currentTime.getHours();
  const minutes = currentTime.getMinutes();
  const seconds = currentTime.getSeconds();
  const formattedHours = hours.toString().padStart(2, '0');
  const formattedMinutes = minutes.toString().padStart(2, '0');

  // Rotary crown simulated scroll
  const handleCrownScroll = (deltaY: number) => {
    if (scrollContainerRef.current) {
      scrollContainerRef.current.scrollBy({ top: deltaY, behavior: 'smooth' });
    }
    setCrownRotation((prev) => (prev + (deltaY > 0 ? 30 : -30)) % 360);
  };

  const handleWheel = (e: React.WheelEvent) => {
    e.stopPropagation();
    handleCrownScroll(e.deltaY * 0.8);
  };

  const handleComplicationClick = (content: ComplicationContent) => {
    if (content === 'sp500' || content === 'nasdaq' || content === 'dual') {
      setWatchAppOpen(true);
    }
  };

  const portfolioQuote = quotes.find((q) => q.category === 'PORTFOLIO');
  const majorIndices = quotes.filter((q) => q.category === 'INDEX');
  const sectorQuotes = quotes.filter((q) => q.category === 'SECTOR');

  const renderPortfolioSparklineSvg = (primaryPts: number[], secondaryPts: number[], isPositive: boolean) => {
    const pts = primaryPts.length >= 2 ? primaryPts : [100.0, 100.0];
    const secPts = secondaryPts.length >= 2 ? secondaryPts : pts;
    const allVals = [...pts, ...secPts, 100.0];
    const minVal = Math.min(...allVals) - 0.08;
    const maxVal = Math.max(...allVals) + 0.08;
    const span = Math.max(0.1, maxVal - minVal);
    const width = 200;
    const height = 34;

    const yFor = (v: number) => {
      const norm = Math.min(1, Math.max(0, (v - minVal) / span));
      return (height - 6) * (1 - norm) + 3;
    };

    const baseY = yFor(100.0);
    const primaryPointsStr = pts
      .map((v, idx) => `${((idx / (pts.length - 1)) * width).toFixed(1)},${yFor(v).toFixed(1)}`)
      .join(' ');
    const secondaryPointsStr = secPts
      .map((v, idx) => `${((idx / (secPts.length - 1)) * width).toFixed(1)},${yFor(v).toFixed(1)}`)
      .join(' ');

    return (
      <svg className="w-full h-[34px] overflow-visible" viewBox={`0 0 ${width} ${height}`}>
        <line
          x1="0"
          y1={baseY}
          x2={width}
          y2={baseY}
          stroke="#546E7A"
          strokeWidth="1"
          strokeDasharray="4 4"
        />
        <polyline
          fill="none"
          stroke="#64B5F6"
          strokeOpacity="0.6"
          strokeWidth="1.25"
          strokeDasharray="3 3"
          strokeLinecap="round"
          points={secondaryPointsStr}
        />
        <polyline
          fill="none"
          stroke={isPositive ? '#4CAF50' : '#F44336'}
          strokeWidth="2"
          strokeLinecap="round"
          points={primaryPointsStr}
        />
      </svg>
    );
  };

  // Render circular complication on the dial
  const renderComplicationItem = (slot: ComplicationSlotId) => {
    const content = complicationSlots[slot];

    if (content === 'none') {
      return (
        <div className="w-16 h-16 rounded-full border border-dashed border-neutral-700/60 flex items-center justify-center text-[10px] text-neutral-500">
          Empty
        </div>
      );
    }

    if (content === 'date') {
      const dayName = currentTime.toLocaleDateString('en-US', { weekday: 'short' }).toUpperCase();
      const dayNum = currentTime.getDate();
      return (
        <div className="w-16 h-16 rounded-full bg-neutral-900/90 border border-neutral-800 flex flex-col items-center justify-center text-center">
          <span className="text-[9px] uppercase tracking-wider text-neutral-400 font-semibold">{dayName}</span>
          <span className="text-sm font-bold text-white font-mono-numbers">{dayNum}</span>
        </div>
      );
    }

    if (content === 'battery') {
      return (
        <div className="w-16 h-16 rounded-full bg-neutral-900/90 border border-neutral-800 flex flex-col items-center justify-center text-center">
          <span className="text-[9px] text-neutral-400 uppercase font-semibold">BATTERY</span>
          <span className="text-sm font-bold text-emerald-400 font-mono-numbers">86%</span>
        </div>
      );
    }

    const quote = content === 'sp500' ? spx : ndx;
    const isSpx = content === 'sp500';
    const ticker = isSpx ? 'SPX' : 'NDX';
    const isPositive = quote.change >= 0;
    const trendColor = isAmbient ? 'text-white' : isPositive ? 'text-emerald-400' : 'text-rose-400';
    const gaugeStroke = isAmbient ? '#525252' : isPositive ? '#10b981' : '#f43f5e';

    const dayRange = Math.max(1, quote.dayHigh - quote.dayLow);
    const dayProgress = Math.min(100, Math.max(0, ((quote.price - quote.dayLow) / dayRange) * 100));
    const circumference = 2 * Math.PI * 26;
    const strokeDashoffset = circumference - (dayProgress / 100) * (circumference * 0.75);

    return (
      <button
        onClick={() => handleComplicationClick(content)}
        className="group relative w-16 h-16 rounded-full bg-neutral-900/90 border border-neutral-800 hover:border-neutral-600 flex items-center justify-center transition-all cursor-pointer focus:outline-none focus:ring-2 focus:ring-sky-500"
        title={`Tap to open Wear OS app for ${quote.name}`}
      >
        <svg className="absolute inset-0 w-full h-full -rotate-135" viewBox="0 0 64 64">
          <circle
            cx="32"
            cy="32"
            r="26"
            fill="none"
            stroke="#262626"
            strokeWidth="2.5"
            strokeDasharray={`${circumference * 0.75} ${circumference * 0.25}`}
            strokeLinecap="round"
          />
          <circle
            cx="32"
            cy="32"
            r="26"
            fill="none"
            stroke={gaugeStroke}
            strokeWidth="2.5"
            strokeDasharray={`${circumference * 0.75} ${circumference * 0.25}`}
            strokeDashoffset={strokeDashoffset}
            strokeLinecap="round"
            className="transition-all duration-700 ease-out"
          />
        </svg>

        <div className="relative z-10 flex flex-col items-center justify-center text-center px-1">
          <div className="flex items-center gap-0.5">
            <span className="text-[9px] font-bold text-neutral-300 tracking-wider uppercase">{ticker}</span>
            {!isAmbient && (isPositive ? (
              <ArrowUpRight className="w-2.5 h-2.5 text-emerald-400" />
            ) : (
              <ArrowDownRight className="w-2.5 h-2.5 text-rose-400" />
            ))}
          </div>
          <span className={`text-[11px] font-bold font-mono-numbers leading-tight ${trendColor}`}>
            {getComplicationText(quote, selectedFormat)}
          </span>
        </div>
      </button>
    );
  };

  return (
    <div className="flex flex-col items-center">
      {/* Surface Selector Pills (Watch Face vs Wear OS App) */}
      <div className="flex items-center gap-2 mb-4 bg-neutral-900/90 border border-neutral-800 p-1 rounded-xl text-xs">
        <button
          onClick={() => setWatchAppOpen(false)}
          className={`px-3 py-1.5 rounded-lg font-medium transition-all cursor-pointer flex items-center gap-1.5 ${
            !watchAppOpen ? 'bg-sky-600 text-white shadow-sm' : 'text-neutral-400 hover:text-white'
          }`}
        >
          <Eye className="w-3.5 h-3.5" />
          <span>Watch Dial (Complications)</span>
        </button>
        <button
          onClick={() => setWatchAppOpen(true)}
          className={`px-3 py-1.5 rounded-lg font-medium transition-all cursor-pointer flex items-center gap-1.5 ${
            watchAppOpen ? 'bg-sky-600 text-white shadow-sm' : 'text-neutral-400 hover:text-white'
          }`}
        >
          <Layers className="w-3.5 h-3.5" />
          <span>Wear OS App (ScalingLazyColumn)</span>
        </button>
      </div>

      {/* Watch Hardware Frame (Pixel Watch 4 Chassis) */}
      <div className="relative select-none">
        {/* Subtle Outer Bezel Rim */}
        <div className="relative w-[340px] h-[340px] sm:w-[380px] sm:h-[380px] rounded-full bg-gradient-to-b from-neutral-800 via-neutral-900 to-neutral-950 p-[10px] shadow-[0_20px_60px_-15px_rgba(0,0,0,0.9),0_0_0_1px_rgba(255,255,255,0.08)] flex items-center justify-center">

          {/* Interactive Physical Rotary Crown */}
          <div
            className="absolute -right-3 top-1/2 -translate-y-1/2 w-4 h-14 bg-gradient-to-r from-neutral-700 via-neutral-500 to-neutral-600 rounded-r-md shadow-lg border-l border-neutral-900 flex flex-col justify-around py-1.5 cursor-pointer hover:brightness-125 active:scale-95 transition-all group"
            title="Rotary Crown: Click to toggle app or drag/wheel to scroll"
            onClick={() => {
              if (watchAppOpen) {
                handleCrownScroll(60);
              } else {
                setWatchAppOpen(true);
              }
            }}
          >
            <div
              className="w-full h-full flex flex-col justify-between py-1 transition-transform"
              style={{ transform: `rotate(${crownRotation}deg)` }}
            >
              <div className="w-full h-[1.5px] bg-neutral-950" />
              <div className="w-full h-[1.5px] bg-neutral-950" />
              <div className="w-full h-[1.5px] bg-neutral-950" />
              <div className="w-full h-[1.5px] bg-neutral-950" />
            </div>
          </div>

          {/* Crown Scroll Hints */}
          {watchAppOpen && (
            <div className="absolute -right-14 top-1/2 -translate-y-1/2 flex flex-col items-center gap-1 text-[9px] text-neutral-400 font-mono">
              <button
                onClick={() => handleCrownScroll(-80)}
                className="w-6 h-6 rounded bg-neutral-800 hover:bg-neutral-700 text-white flex items-center justify-center cursor-pointer shadow-sm"
                title="Rotate crown up"
              >
                ▲
              </button>
              <span className="text-[8px] text-neutral-500 uppercase tracking-tighter">Crown</span>
              <button
                onClick={() => handleCrownScroll(80)}
                className="w-6 h-6 rounded bg-neutral-800 hover:bg-neutral-700 text-white flex items-center justify-center cursor-pointer shadow-sm"
                title="Rotate crown down"
              >
                ▼
              </button>
            </div>
          )}

          {/* Inner Curved AMOLED Display (Pure Pitch Black) */}
          <div
            onWheel={handleWheel}
            className="relative w-full h-full rounded-full bg-black overflow-hidden flex items-center justify-center border border-neutral-800/80"
          >
            {/* Subtle Glass Curvature Glare Highlight */}
            <div className="absolute inset-0 rounded-full bg-gradient-to-tr from-transparent via-white/[0.02] to-white/[0.05] pointer-events-none z-30" />

            {/* 1. WEAR OS APP VIEW (ScalingLazyColumn with Rotary Crown & 11 Sectors) */}
            {watchAppOpen ? (
              <div className="absolute inset-0 z-20 bg-black text-white flex flex-col items-center">
                {/* Watch Top Status Bar */}
                <div className="w-full flex items-center justify-between text-[11px] text-neutral-400 px-8 pt-4 pb-1 font-mono-numbers z-20 shrink-0">
                  <span className="font-semibold text-neutral-300">{formattedHours}:{formattedMinutes}</span>
                  <div className="flex items-center gap-1 text-[9px] text-emerald-400 bg-emerald-500/10 px-1.5 py-0.5 rounded-full border border-emerald-500/20">
                    <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
                    <span>Data Layer</span>
                  </div>
                </div>

                {/* ScalingLazyColumn Scrollable Body */}
                <div
                  ref={scrollContainerRef}
                  className="w-full flex-1 overflow-y-auto px-6 py-2 space-y-2 scroll-smooth text-center no-scrollbar"
                  style={{ scrollbarWidth: 'none' }}
                >
                  {/* Portfolio Daily Score (100-Baseline) Card */}
                  {portfolioQuote && (() => {
                    const unnormalizedQuote = toUnnormalizedPortfolioQuote(portfolioQuote);
                    const activeQuote = isNormalizedMode ? portfolioQuote : unnormalizedQuote;
                    const secondaryQuote = isNormalizedMode ? unnormalizedQuote : portfolioQuote;
                    const isPositive = activeQuote.change >= 0;
                    const trendColor = isPositive ? 'text-[#4CAF50]' : 'text-[#F44336]';

                    return (
                      <div
                        onClick={() => setIsNormalizedMode(!isNormalizedMode)}
                        className="w-full bg-gradient-to-b from-[#132232] to-[#1E1E1E] hover:brightness-110 rounded-[14px] p-2.5 text-left border border-sky-500/20 transition-all cursor-pointer"
                        title="Tap to toggle Normalized (100%) vs Raw (55.4%) weight mode"
                      >
                        <div className="flex items-center justify-between">
                          <span className="text-[11px] font-bold text-[#90CAF9]">
                            {isNormalizedMode ? 'Portfolio (100%)' : 'Portfolio (55.4%)'}
                          </span>
                          <span className={`text-[12px] font-bold font-mono-numbers ${trendColor}`}>
                            {activeQuote.formattedPercent}
                          </span>
                        </div>
                        <div className="flex items-baseline justify-between mt-0.5">
                          <span className="text-[18px] font-extrabold text-white font-mono-numbers">
                            {activeQuote.formattedScore}
                          </span>
                          <span className="text-[10px] text-[#B0BEC5] font-mono-numbers">
                            {isNormalizedMode
                              ? `Raw: ${unnormalizedQuote.formattedScore}`
                              : `Norm: ${portfolioQuote.formattedScore}`}
                          </span>
                        </div>
                        <div className="mt-1">
                          {renderPortfolioSparklineSvg(
                            activeQuote.sparkline,
                            secondaryQuote.sparkline,
                            isPositive
                          )}
                        </div>
                        <div className="mt-0.5 text-[8px] text-[#78909C] truncate">
                          Vico 3.3.1 Chart · Tap Norm/Raw · 2h sync
                        </div>
                      </div>
                    );
                  })()}

                  {/* Category Header */}
                  <div className="pt-1 pb-1">
                    <span className="text-[10px] font-bold text-neutral-400 uppercase tracking-widest">
                      Major Indices
                    </span>
                  </div>

                  {/* Major Indices Cards */}
                  {majorIndices.map((quote) => {
                    const isPositive = quote.change >= 0;
                    const trendColor = isPositive ? 'text-[#4CAF50]' : 'text-[#F44336]';
                    return (
                      <div
                        key={quote.symbol}
                        className="w-full bg-[#1E1E1E] hover:bg-[#252525] rounded-[14px] p-2.5 text-left border border-white/5 transition-colors"
                      >
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-1.5">
                            <SectorIcon symbol={quote.symbol} className="text-white w-3.5 h-3.5" />
                            <span className="text-[13px] font-bold text-white leading-tight">{quote.name}</span>
                          </div>
                          <span className={`text-[12px] font-bold font-mono-numbers ${trendColor}`}>
                            {quote.formattedPercent}
                          </span>
                        </div>
                        <div className="flex items-baseline justify-between mt-1">
                          <span className="text-[16px] font-extrabold text-white font-mono-numbers">
                            {quote.formattedCompactPoints}
                          </span>
                          <span className={`text-[12px] font-medium font-mono-numbers ${trendColor}`}>
                            {quote.formattedCompactChange}
                          </span>
                        </div>
                      </div>
                    );
                  })}

                  {/* Sectors Divider & Header */}
                  <div className="pt-3 pb-1 flex items-center justify-center gap-1.5">
                    <span className="text-[10px] font-bold text-neutral-400 uppercase tracking-widest">
                      Sectors ({sectorQuotes.length})
                    </span>
                  </div>

                  {/* 11 GICS Sector Cards */}
                  {sectorQuotes.map((quote) => {
                    const isPositive = quote.change >= 0;
                    const trendColor = isPositive ? 'text-[#4CAF50]' : 'text-[#F44336]';
                    return (
                      <div
                        key={quote.symbol}
                        className="w-full bg-[#1E1E1E] hover:bg-[#252525] rounded-[14px] p-2.5 text-left border border-white/5 transition-colors"
                      >
                        <div className="flex items-center justify-between gap-1">
                          <div className="flex items-center gap-1.5 min-w-0">
                            <div className="shrink-0 text-white">
                              <SectorIcon symbol={quote.symbol} className="w-3.5 h-3.5" />
                            </div>
                            <span className="text-[12px] font-bold text-white leading-tight line-clamp-2">
                              {quote.name}
                            </span>
                          </div>
                          <span className={`text-[12px] font-bold font-mono-numbers shrink-0 ${trendColor}`}>
                            {quote.formattedPercent}
                          </span>
                        </div>
                        <div className="flex items-baseline justify-between mt-1.5">
                          <span className="text-[15px] font-extrabold text-white font-mono-numbers">
                            {quote.formattedCompactPoints}
                          </span>
                          <span className={`text-[11px] font-medium font-mono-numbers ${trendColor}`}>
                            {quote.formattedCompactChange}
                          </span>
                        </div>
                      </div>
                    );
                  })}

                  {/* Bottom Rotary Scroll Footer */}
                  <div className="pt-4 pb-6 flex flex-col items-center justify-center gap-2">
                    <span className="text-[9px] text-neutral-500 font-mono">
                      Cached in Room DB · Rotary Crown
                    </span>
                    <button
                      onClick={onRefreshQuotes}
                      disabled={isRefreshing}
                      className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-sky-600 hover:bg-sky-500 text-white text-[10px] font-medium transition-all cursor-pointer"
                    >
                      <RefreshCw className={`w-2.5 h-2.5 ${isRefreshing ? 'animate-spin' : ''}`} />
                      <span>{isRefreshing ? 'Delegating...' : 'Refresh Phone'}</span>
                    </button>
                    <button
                      onClick={() => setWatchAppOpen(false)}
                      className="text-[10px] text-neutral-400 hover:text-white underline cursor-pointer"
                    >
                      Back to Watch Face
                    </button>
                  </div>
                </div>
              </div>
            ) : (
              /* 2. WATCH FACE DIAL (Complications API Mode) */
              <div className={`relative w-full h-full flex items-center justify-center ${isAmbient ? 'brightness-90' : ''}`}>
                {/* Watch Face Background Ticks */}
                <div className="absolute inset-0 flex items-center justify-center">
                  {[...Array(12)].map((_, i) => (
                    <div
                      key={i}
                      className="absolute w-[2px] h-3 bg-neutral-800"
                      style={{
                        transform: `rotate(${i * 30}deg) translateY(-145px)`,
                      }}
                    />
                  ))}
                </div>

                {/* Complication Slots (Top, Bottom, Left, Right) */}
                <div className="absolute top-7">{renderComplicationItem('top')}</div>
                <div className="absolute bottom-7">{renderComplicationItem('bottom')}</div>
                <div className="absolute left-7">{renderComplicationItem('left')}</div>
                <div className="absolute right-7">{renderComplicationItem('right')}</div>

                {/* Central Time Display */}
                <div className="relative z-10 flex flex-col items-center justify-center text-center pointer-events-none">
                  <div className="text-4xl sm:text-5xl font-black tracking-tight text-white font-mono-numbers leading-none">
                    {formattedHours}:{formattedMinutes}
                  </div>
                  <div className="text-[11px] font-bold text-neutral-400 tracking-widest uppercase mt-1">
                    {currentTime.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Simulator Controls Strip */}
      <div className="mt-4 flex flex-wrap items-center justify-center gap-3 text-xs text-neutral-400">
        <button
          onClick={() => setIsAmbient(!isAmbient)}
          className={`px-3 py-1.5 rounded-lg border flex items-center gap-1.5 transition-colors cursor-pointer ${
            isAmbient
              ? 'bg-neutral-800 border-neutral-700 text-amber-400 font-semibold'
              : 'bg-neutral-900 border-neutral-800 text-neutral-300 hover:border-neutral-700'
          }`}
        >
          <Eye className="w-3.5 h-3.5" />
          <span>AOD Ambient Mode: {isAmbient ? 'ON' : 'OFF'}</span>
        </button>

        <button
          onClick={onRefreshQuotes}
          disabled={isRefreshing}
          className="px-3 py-1.5 rounded-lg bg-neutral-900 border border-neutral-800 hover:border-neutral-700 text-neutral-300 flex items-center gap-1.5 transition-colors cursor-pointer disabled:opacity-50"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin' : ''}`} />
          <span>Simulate Network Refresh</span>
        </button>
      </div>
    </div>
  );
};
