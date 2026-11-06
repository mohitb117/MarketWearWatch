import React from 'react';
import { MarketIndex, getComplicationText } from '../data/marketData';
import { ComplicationSlotId, ComplicationContent } from './PixelWatch4Simulator';
import { Sliders, Code2, Check } from 'lucide-react';

interface ComplicationConfiguratorProps {
  spx: MarketIndex;
  ndx: MarketIndex;
  selectedFormat: 'points' | 'percent' | 'change' | 'compact';
  onFormatChange: (format: 'points' | 'percent' | 'change' | 'compact') => void;
  complicationSlots: Record<ComplicationSlotId, ComplicationContent>;
  onSlotChange: (slot: ComplicationSlotId, content: ComplicationContent) => void;
}

export const ComplicationConfigurator: React.FC<ComplicationConfiguratorProps> = ({
  spx,
  ndx,
  selectedFormat,
  onFormatChange,
  complicationSlots,
  onSlotChange,
}) => {
  const formats: { id: 'points' | 'percent' | 'change' | 'compact'; label: string; spxExample: string; ndxExample: string }[] = [
    {
      id: 'points',
      label: 'Index Points',
      spxExample: Math.round(spx.price).toLocaleString(),
      ndxExample: Math.round(ndx.price).toLocaleString(),
    },
    {
      id: 'percent',
      label: 'Percent Delta (%)',
      spxExample: `${spx.change >= 0 ? '+' : ''}${spx.percentChange.toFixed(2)}%`,
      ndxExample: `${ndx.change >= 0 ? '+' : ''}${ndx.percentChange.toFixed(2)}%`,
    },
    {
      id: 'change',
      label: 'Net Points Delta',
      spxExample: `${spx.change >= 0 ? '+' : ''}${spx.change.toFixed(1)} pts`,
      ndxExample: `${ndx.change >= 0 ? '+' : ''}${ndx.change.toFixed(1)} pts`,
    },
    {
      id: 'compact',
      label: 'Compact Abbreviated',
      spxExample: `${(spx.price / 1000).toFixed(2)}k`,
      ndxExample: `${(ndx.price / 1000).toFixed(2)}k`,
    },
  ];

  const slotOptions: { id: ComplicationContent; label: string }[] = [
    { id: 'sp500', label: 'S&P 500 Index' },
    { id: 'nasdaq', label: 'Nasdaq 100 Index' },
    { id: 'dual', label: 'Dual Glance (SPX + NDX)' },
    { id: 'date', label: 'Calendar Date' },
    { id: 'battery', label: 'Watch Battery' },
    { id: 'none', label: 'None (Empty Slot)' },
  ];

  // Sample active payload inspector
  const activeSamplePayload = {
    complicationType: 'ComplicationType.SHORT_TEXT',
    symbol: 'SPX',
    title: 'SPX',
    text: getComplicationText(spx, selectedFormat),
    contentDescription: `S&P 500: ${getComplicationText(spx, selectedFormat)} (${spx.change >= 0 ? '+' : ''}${spx.percentChange.toFixed(2)}%)`,
    monochromaticImage: spx.change >= 0 ? 'R.drawable.ic_trend_up' : 'R.drawable.ic_trend_down',
    rangedValue: {
      min: spx.dayLow,
      max: spx.dayHigh,
      current: spx.price,
    },
    tapAction: 'PendingIntent -> MainActivity(extra: "SPX")',
    wearOsUpdatePeriod: '900s (15 min cache)',
  };

  return (
    <div className="bg-neutral-900/90 border border-neutral-800 rounded-xl p-5 space-y-6">
      <div className="flex items-center justify-between pb-3 border-b border-neutral-800">
        <div className="flex items-center gap-2">
          <Sliders className="w-4 h-4 text-sky-400" />
          <h3 className="font-bold text-white text-base">Watch Face Complication Settings</h3>
        </div>
        <span className="text-xs text-neutral-400">Wear OS 4 / 5 AndroidX API</span>
      </div>

      {/* Format Selection */}
      <div className="space-y-3">
        <label className="text-xs font-semibold text-neutral-300 uppercase tracking-wider block">
          Complication Numeric Format
        </label>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
          {formats.map((fmt) => {
            const isSelected = selectedFormat === fmt.id;
            return (
              <button
                key={fmt.id}
                onClick={() => onFormatChange(fmt.id)}
                className={`flex items-start justify-between p-3 rounded-lg border text-left transition-all ${
                  isSelected
                    ? 'bg-sky-950/30 border-sky-500/60 ring-1 ring-sky-500/30'
                    : 'bg-neutral-950/60 border-neutral-800 hover:border-neutral-700'
                }`}
              >
                <div>
                  <div className="flex items-center gap-1.5 font-medium text-xs text-white">
                    <span>{fmt.label}</span>
                    {isSelected && <Check className="w-3.5 h-3.5 text-sky-400" />}
                  </div>
                  <div className="mt-1 flex items-center gap-3 text-xs font-mono-numbers text-neutral-400">
                    <span>SPX: <strong className="text-neutral-200">{fmt.spxExample}</strong></span>
                    <span>NDX: <strong className="text-neutral-200">{fmt.ndxExample}</strong></span>
                  </div>
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* Watch Slot Placements */}
      <div className="space-y-3">
        <label className="text-xs font-semibold text-neutral-300 uppercase tracking-wider block">
          Complication Slot Assignments (Pixel Watch Dial)
        </label>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {(['top', 'bottom', 'left', 'right'] as ComplicationSlotId[]).map((slot) => (
            <div key={slot} className="bg-neutral-950/80 border border-neutral-800 rounded-lg p-2.5 space-y-1.5">
              <span className="text-[11px] font-bold text-neutral-400 uppercase tracking-wider block">
                {slot} Slot
              </span>
              <select
                value={complicationSlots[slot]}
                onChange={(e) => onSlotChange(slot, e.target.value as ComplicationContent)}
                className="w-full bg-neutral-900 border border-neutral-700 text-white rounded px-2 py-1 text-xs focus:outline-none focus:border-sky-500"
              >
                {slotOptions.map((opt) => (
                  <option key={opt.id} value={opt.id}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>
          ))}
        </div>
      </div>

      {/* Live ComplicationData Payload Inspector */}
      <div className="space-y-2 pt-2 border-t border-neutral-800">
        <div className="flex items-center gap-2 text-xs font-semibold text-neutral-300">
          <Code2 className="w-3.5 h-3.5 text-neutral-400" />
          <span>Active AndroidX ComplicationData Output Payload</span>
        </div>
        <pre className="p-3 bg-neutral-950 border border-neutral-800/80 rounded-lg text-[11px] font-mono-numbers text-sky-300 overflow-x-auto leading-relaxed">
          {JSON.stringify(activeSamplePayload, null, 2)}
        </pre>
      </div>
    </div>
  );
};
