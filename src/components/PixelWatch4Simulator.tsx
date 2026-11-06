import React, { useState, useEffect } from 'react';
import { MarketIndex, MarketStatus, getComplicationText } from '../data/marketData';
import { RefreshCw, ArrowUpRight, ArrowDownRight, Smartphone, Eye, Sparkles } from 'lucide-react';

export type WatchFaceStyle = 'concentric' | 'utility' | 'digital_sport' | 'minimal';
export type ComplicationSlotId = 'top' | 'bottom' | 'left' | 'right';
export type ComplicationContent = 'sp500' | 'nasdaq' | 'dual' | 'date' | 'battery' | 'none';

interface PixelWatch4SimulatorProps {
  spx: MarketIndex;
  ndx: MarketIndex;
  marketStatus: MarketStatus;
  selectedFormat: 'points' | 'percent' | 'change' | 'compact';
  complicationSlots: Record<ComplicationSlotId, ComplicationContent>;
  onSlotChange: (slot: ComplicationSlotId, content: ComplicationContent) => void;
  onRefreshQuotes: () => void;
  isRefreshing: boolean;
}

export const PixelWatch4Simulator: React.FC<PixelWatch4SimulatorProps> = ({
  spx,
  ndx,
  marketStatus,
  selectedFormat,
  complicationSlots,
  onSlotChange,
  onRefreshQuotes,
  isRefreshing,
}) => {
  const [faceStyle, setFaceStyle] = useState<WatchFaceStyle>('digital_sport');
  const [isAmbient, setIsAmbient] = useState<boolean>(false);
  const [currentTime, setCurrentTime] = useState<Date>(new Date());
  const [watchAppOpen, setWatchAppOpen] = useState<boolean>(false);
  const [activeWatchAppIndex, setActiveWatchAppIndex] = useState<'SPX' | 'NDX'>('SPX');

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
  const formattedSeconds = seconds.toString().padStart(2, '0');

  // Analog hands angles
  const hourAngle = (hours % 12 + minutes / 60) * 30;
  const minuteAngle = (minutes + seconds / 60) * 6;
  const secondAngle = seconds * 6;

  const handleComplicationClick = (content: ComplicationContent) => {
    if (content === 'sp500') {
      setActiveWatchAppIndex('SPX');
      setWatchAppOpen(true);
    } else if (content === 'nasdaq') {
      setActiveWatchAppIndex('NDX');
      setWatchAppOpen(true);
    } else if (content === 'dual') {
      setActiveWatchAppIndex('SPX');
      setWatchAppOpen(true);
    }
  };

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

    if (content === 'dual') {
      const spxText = getComplicationText(spx, selectedFormat);
      const ndxText = getComplicationText(ndx, selectedFormat);
      const spxUp = spx.change >= 0;
      const ndxUp = ndx.change >= 0;

      return (
        <button
          onClick={() => handleComplicationClick('dual')}
          className="group relative w-16 h-16 rounded-full bg-neutral-900/95 border border-neutral-800 hover:border-sky-500/80 transition-all flex flex-col items-center justify-center p-1 cursor-pointer focus:outline-none"
          title="Dual Market Complication (Tap to inspect on watch)"
        >
          <div className="flex items-center gap-0.5 text-[8.5px] font-mono-numbers">
            <span className="text-neutral-400 font-bold">S</span>
            <span className={spxUp ? 'text-emerald-400 font-semibold' : 'text-rose-400 font-semibold'}>
              {spx.percentChange >= 0 ? '+' : ''}{spx.percentChange.toFixed(1)}%
            </span>
          </div>
          <div className="w-8 h-[1px] bg-neutral-800 my-0.5" />
          <div className="flex items-center gap-0.5 text-[8.5px] font-mono-numbers">
            <span className="text-neutral-400 font-bold">N</span>
            <span className={ndxUp ? 'text-emerald-400 font-semibold' : 'text-rose-400 font-semibold'}>
              {ndx.percentChange >= 0 ? '+' : ''}{ndx.percentChange.toFixed(1)}%
            </span>
          </div>
        </button>
      );
    }

    const isPositive = quote.change >= 0;
    const trendColor = isAmbient ? 'text-white' : (isPositive ? 'text-emerald-400' : 'text-rose-400');
    const gaugeStroke = isAmbient ? '#ffffff' : (isPositive ? '#34d399' : '#f87171');

    // Calculate position for ranged value gauge
    const range = Math.max(1, quote.dayHigh - quote.dayLow);
    const progress = Math.min(1, Math.max(0, (quote.price - quote.dayLow) / range));
    const circumference = 2 * Math.PI * 26; // radius 26
    const strokeDashoffset = circumference - (circumference * 0.75 * progress);

    return (
      <button
        onClick={() => handleComplicationClick(content)}
        className="group relative w-16 h-16 rounded-full bg-neutral-950/90 border border-neutral-800/90 hover:border-sky-400 transition-all flex items-center justify-center cursor-pointer focus:outline-none shadow-sm"
        title={`${quote.name} Complication - Tap to inspect on watch`}
      >
        {/* Ranged Value Arc */}
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

        {/* Inner Content */}
        <div className="relative z-10 flex flex-col items-center justify-center text-center px-1">
          <div className="flex items-center gap-0.5">
            <span className="text-[9px] font-bold text-neutral-300 tracking-wider uppercase">{ticker}</span>
            {!isAmbient && (
              isPositive ? (
                <ArrowUpRight className="w-2.5 h-2.5 text-emerald-400" />
              ) : (
                <ArrowDownRight className="w-2.5 h-2.5 text-rose-400" />
              )
            )}
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
      {/* Watch Hardware Frame (Pixel Watch 4 Chassis) */}
      <div className="relative select-none">
        {/* Subtle Outer Drop Shadow & Bezel Rim */}
        <div className="relative w-[340px] h-[340px] sm:w-[380px] sm:h-[380px] rounded-full bg-gradient-to-b from-neutral-800 via-neutral-900 to-neutral-950 p-[10px] shadow-[0_20px_60px_-15px_rgba(0,0,0,0.9),0_0_0_1px_rgba(255,255,255,0.08)] flex items-center justify-center">

          {/* Right Crown (Tactile Rotating Dial) */}
          <div className="absolute -right-3 top-1/2 -translate-y-1/2 w-4 h-12 bg-gradient-to-r from-neutral-700 to-neutral-500 rounded-r-md shadow-md border-l border-neutral-900 flex flex-col justify-around py-1.5 cursor-pointer hover:brightness-110 active:scale-95 transition-all"
            title="Pixel Watch Crown - Click to toggle Watch App"
            onClick={() => setWatchAppOpen(!watchAppOpen)}
          >
            <div className="w-full h-[1.5px] bg-neutral-900/60" />
            <div className="w-full h-[1.5px] bg-neutral-900/60" />
            <div className="w-full h-[1.5px] bg-neutral-900/60" />
            <div className="w-full h-[1.5px] bg-neutral-900/60" />
          </div>

          {/* Inner Curved AMOLED Display (Pure Pitch Black) */}
          <div className="relative w-full h-full rounded-full bg-black overflow-hidden flex items-center justify-center border border-neutral-800/60">

            {/* Glass Curvature Glare Highlight (Very Subtle) */}
            <div className="absolute inset-0 rounded-full bg-gradient-to-tr from-transparent via-white/[0.03] to-white/[0.06] pointer-events-none z-30" />

            {/* WATCH APP VIEW (Simulating the Jetpack Compose Screen upon Complication Tap) */}
            {watchAppOpen ? (
              <div className="absolute inset-0 z-20 bg-black text-white p-5 flex flex-col items-center justify-between animate-in fade-in zoom-in-95 duration-200">
                {/* Status Bar */}
                <div className="w-full flex items-center justify-between text-[11px] text-neutral-400 px-3 pt-2 font-mono-numbers">
                  <span>{formattedHours}:{formattedMinutes}</span>
                  <span className="text-[10px] text-emerald-400">Connected</span>
                </div>

                {/* Companion App Body */}
                <div className="w-full flex flex-col items-center text-center space-y-2 my-auto px-2">
                  <div className="text-[10px] uppercase tracking-widest text-neutral-400 font-semibold">
                    Market Indices
                  </div>

                  {/* Active Index Details */}
                  {(() => {
                    const activeQuote = activeWatchAppIndex === 'SPX' ? spx : ndx;
                    const isPositive = activeQuote.change >= 0;
                    return (
                      <div className="w-full bg-neutral-900/90 border border-neutral-800 rounded-2xl p-2.5">
                        <div className="flex items-center justify-between mb-1">
                          <span className="text-xs font-bold text-neutral-200">{activeQuote.name}</span>
                          <span className={`text-xs font-bold font-mono-numbers ${isPositive ? 'text-emerald-400' : 'text-rose-400'}`}>
                            {isPositive ? '+' : ''}{activeQuote.percentChange.toFixed(2)}%
                          </span>
                        </div>
                        <div className="text-xl font-extrabold font-mono-numbers tracking-tight text-white mb-1">
                          {activeQuote.price.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                        </div>
                        <div className="flex items-center justify-between text-[10px] text-neutral-400 font-mono-numbers">
                          <span>L: {activeQuote.dayLow.toLocaleString()}</span>
                          <span>H: {activeQuote.dayHigh.toLocaleString()}</span>
                        </div>
                      </div>
                    );
                  })()}

                  {/* Quick Index Switcher Buttons */}
                  <div className="flex items-center gap-1.5">
                    <button
                      onClick={() => setActiveWatchAppIndex('SPX')}
                      className={`px-3 py-1 text-[11px] font-semibold rounded-full transition-all ${
                        activeWatchAppIndex === 'SPX'
                          ? 'bg-neutral-100 text-black font-bold'
                          : 'bg-neutral-800 text-neutral-300 hover:bg-neutral-700'
                      }`}
                    >
                      S&P 500
                    </button>
                    <button
                      onClick={() => setActiveWatchAppIndex('NDX')}
                      className={`px-3 py-1 text-[11px] font-semibold rounded-full transition-all ${
                        activeWatchAppIndex === 'NDX'
                          ? 'bg-neutral-100 text-black font-bold'
                          : 'bg-neutral-800 text-neutral-300 hover:bg-neutral-700'
                      }`}
                    >
                      Nasdaq 100
                    </button>
                  </div>
                </div>

                {/* Bottom Action Button */}
                <div className="w-full flex items-center justify-center gap-2 pb-2">
                  <button
                    onClick={onRefreshQuotes}
                    disabled={isRefreshing}
                    className="flex items-center gap-1.5 px-4 py-1.5 rounded-full bg-sky-600 hover:bg-sky-500 text-white text-[11px] font-medium transition-all"
                  >
                    <RefreshCw className={`w-3 h-3 ${isRefreshing ? 'animate-spin' : ''}`} />
                    <span>{isRefreshing ? 'Updating...' : 'Sync Now'}</span>
                  </button>
                  <button
                    onClick={() => setWatchAppOpen(false)}
                    className="px-3 py-1.5 rounded-full bg-neutral-800 hover:bg-neutral-700 text-neutral-300 text-[11px]"
                  >
                    Dial
                  </button>
                </div>
              </div>
            ) : (
              /* WATCH FACE DIAL */
              <div className={`relative w-full h-full flex items-center justify-center ${isAmbient ? 'brightness-90' : ''}`}>

                {/* Complication: TOP Slot */}
                <div className="absolute top-4 left-1/2 -translate-x-1/2 z-10">
                  {renderComplicationItem('top')}
                </div>

                {/* Complication: BOTTOM Slot */}
                <div className="absolute bottom-4 left-1/2 -translate-x-1/2 z-10">
                  {renderComplicationItem('bottom')}
                </div>

                {/* Complication: LEFT Slot */}
                <div className="absolute left-4 top-1/2 -translate-y-1/2 z-10">
                  {renderComplicationItem('left')}
                </div>

                {/* Complication: RIGHT Slot */}
                <div className="absolute right-4 top-1/2 -translate-y-1/2 z-10">
                  {renderComplicationItem('right')}
                </div>

                {/* WATCH FACE CORE STYLES */}
                {faceStyle === 'digital_sport' && (
                  <div className="flex flex-col items-center justify-center text-center select-none">
                    <div className="text-5xl sm:text-6xl font-black tracking-tight text-white font-mono-numbers leading-none">
                      {formattedHours}
                    </div>
                    <div className="text-4xl sm:text-5xl font-extrabold text-neutral-400 font-mono-numbers leading-none mt-0.5">
                      {formattedMinutes}
                    </div>
                    {!isAmbient && (
                      <div className="text-[10px] tracking-widest text-neutral-500 uppercase font-semibold mt-1">
                        SEC {formattedSeconds}
                      </div>
                    )}
                  </div>
                )}

                {faceStyle === 'concentric' && (
                  <div className="relative w-full h-full flex items-center justify-center select-none">
                    {/* Outer minute ring marks */}
                    <div className="absolute inset-8 rounded-full border border-neutral-800/60" />
                    <div className="flex items-center gap-1">
                      <span className="text-5xl font-black text-white font-mono-numbers tracking-tighter">
                        {formattedHours}
                      </span>
                      <span className="text-3xl font-light text-neutral-400 font-mono-numbers">
                        :{formattedMinutes}
                      </span>
                    </div>
                  </div>
                )}

                {faceStyle === 'utility' && (
                  <div className="relative w-full h-full flex items-center justify-center">
                    {/* Dial Tick Marks */}
                    {Array.from({ length: 12 }).map((_, i) => (
                      <div
                        key={i}
                        className="absolute w-full h-full flex justify-center items-start pt-2 pointer-events-none"
                        style={{ transform: `rotate(${i * 30}deg)` }}
                      >
                        <div className={`w-[2px] ${i % 3 === 0 ? 'h-3.5 bg-neutral-200' : 'h-2 bg-neutral-600'}`} />
                      </div>
                    ))}

                    {/* Analog Hands */}
                    {/* Hour Hand */}
                    <div
                      className="absolute w-1.5 h-16 bg-white rounded-full origin-bottom bottom-1/2 shadow-sm"
                      style={{ transform: `rotate(${hourAngle}deg)` }}
                    />
                    {/* Minute Hand */}
                    <div
                      className="absolute w-1 h-24 bg-neutral-300 rounded-full origin-bottom bottom-1/2 shadow-sm"
                      style={{ transform: `rotate(${minuteAngle}deg)` }}
                    />
                    {/* Second Hand (hidden in ambient) */}
                    {!isAmbient && (
                      <div
                        className="absolute w-[1.5px] h-28 bg-amber-400 rounded-full origin-bottom bottom-1/2 shadow-sm"
                        style={{ transform: `rotate(${secondAngle}deg)` }}
                      >
                        <div className="w-2.5 h-2.5 rounded-full bg-amber-400 -translate-x-[3.25px] translate-y-27" />
                      </div>
                    )}
                    {/* Center Pin */}
                    <div className="w-3 h-3 rounded-full bg-neutral-900 border-2 border-white z-10" />
                  </div>
                )}

                {faceStyle === 'minimal' && (
                  <div className="flex flex-col items-center justify-center text-center select-none">
                    <span className="text-6xl font-light text-white font-mono-numbers tracking-tight">
                      {formattedHours}:{formattedMinutes}
                    </span>
                    <span className="text-xs uppercase tracking-widest text-neutral-500 mt-1">
                      {currentTime.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
                    </span>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Simulator Control Toolbar (Below Watch) */}
      <div className="mt-6 flex flex-wrap items-center justify-center gap-3 max-w-md w-full">
        {/* Watch Face Picker */}
        <div className="flex items-center gap-1 p-1 bg-neutral-900 border border-neutral-800 rounded-lg">
          <button
            onClick={() => { setFaceStyle('digital_sport'); setWatchAppOpen(false); }}
            className={`px-2.5 py-1 text-xs font-medium rounded-md transition-colors ${
              faceStyle === 'digital_sport' ? 'bg-neutral-800 text-white shadow-sm' : 'text-neutral-400 hover:text-neutral-200'
            }`}
          >
            Digital
          </button>
          <button
            onClick={() => { setFaceStyle('utility'); setWatchAppOpen(false); }}
            className={`px-2.5 py-1 text-xs font-medium rounded-md transition-colors ${
              faceStyle === 'utility' ? 'bg-neutral-800 text-white shadow-sm' : 'text-neutral-400 hover:text-neutral-200'
            }`}
          >
            Analog
          </button>
          <button
            onClick={() => { setFaceStyle('concentric'); setWatchAppOpen(false); }}
            className={`px-2.5 py-1 text-xs font-medium rounded-md transition-colors ${
              faceStyle === 'concentric' ? 'bg-neutral-800 text-white shadow-sm' : 'text-neutral-400 hover:text-neutral-200'
            }`}
          >
            Concentric
          </button>
          <button
            onClick={() => { setFaceStyle('minimal'); setWatchAppOpen(false); }}
            className={`px-2.5 py-1 text-xs font-medium rounded-md transition-colors ${
              faceStyle === 'minimal' ? 'bg-neutral-800 text-white shadow-sm' : 'text-neutral-400 hover:text-neutral-200'
            }`}
          >
            Minimal
          </button>
        </div>

        {/* Ambient Mode Toggle */}
        <button
          onClick={() => setIsAmbient(!isAmbient)}
          className={`flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-lg border transition-colors ${
            isAmbient
              ? 'bg-amber-500/10 border-amber-500/40 text-amber-300'
              : 'bg-neutral-900 border-neutral-800 text-neutral-400 hover:text-neutral-200'
          }`}
          title="Toggle Always-On Display (AOD) ambient low-power state"
        >
          <Eye className="w-3.5 h-3.5" />
          <span>{isAmbient ? 'AOD Active' : 'Active Mode'}</span>
        </button>

        {/* Launch Watch Companion App */}
        <button
          onClick={() => setWatchAppOpen(!watchAppOpen)}
          className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-lg bg-neutral-900 border border-neutral-800 text-neutral-300 hover:text-white transition-colors"
        >
          <Smartphone className="w-3.5 h-3.5 text-sky-400" />
          <span>{watchAppOpen ? 'Back to Watch Face' : 'Open Watch App'}</span>
        </button>
      </div>

      <div className="mt-3 text-center text-xs text-neutral-500">
        Tap any complication slot or click the crown to inspect Wear OS behavior.
      </div>
    </div>
  );
};
