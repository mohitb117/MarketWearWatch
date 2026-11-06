import React, { useState, useEffect } from 'react';
import {
  INITIAL_MARKET_STATE,
  MarketState,
  calculateMarketStatus,
} from './data/marketData';
import {
  PixelWatch4Simulator,
  ComplicationSlotId,
  ComplicationContent,
} from './components/PixelWatch4Simulator';
import { MarketIndicesStrip } from './components/MarketIndicesStrip';
import { ComplicationConfigurator } from './components/ComplicationConfigurator';
import { AndroidCodeViewer } from './components/AndroidCodeViewer';
import { InstallationGuide } from './components/InstallationGuide';
import { generateWearOsProjectZip, downloadBlob } from './utils/zipExport';
import { Download, Watch, Shield, Radio, CheckCircle2 } from 'lucide-react';

export default function App() {
  const [activeTab, setActiveTab] = useState<'simulator' | 'config' | 'source' | 'install'>('simulator');
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
      // Small realistic market fluctuation between -0.15% and +0.15%
      const spxDeltaPct = (Math.random() - 0.48) * 0.2;
      const ndxDeltaPct = (Math.random() - 0.47) * 0.3;

      const newSpxPrice = Number((prev.spx.price * (1 + spxDeltaPct / 100)).toFixed(2));
      const spxChange = Number((newSpxPrice - prev.spx.prevClose).toFixed(2));
      const spxPct = Number(((spxChange / prev.spx.prevClose) * 100).toFixed(2));
      const newSpxHigh = Math.max(prev.spx.dayHigh, newSpxPrice);
      const newSpxLow = Math.min(prev.spx.dayLow, newSpxPrice);
      const newSpxSpark = [...prev.spx.sparkline.slice(1), newSpxPrice];

      const newNdxPrice = Number((prev.ndx.price * (1 + ndxDeltaPct / 100)).toFixed(2));
      const ndxChange = Number((newNdxPrice - prev.ndx.prevClose).toFixed(2));
      const ndxPct = Number(((ndxChange / prev.ndx.prevClose) * 100).toFixed(2));
      const newNdxHigh = Math.max(prev.ndx.dayHigh, newNdxPrice);
      const newNdxLow = Math.min(prev.ndx.dayLow, newNdxPrice);
      const newNdxSpark = [...prev.ndx.sparkline.slice(1), newNdxPrice];

      return {
        ...prev,
        spx: {
          ...prev.spx,
          price: newSpxPrice,
          change: spxChange,
          percentChange: spxPct,
          dayHigh: newSpxHigh,
          dayLow: newSpxLow,
          sparkline: newSpxSpark,
          lastUpdated: 'Just now',
        },
        ndx: {
          ...prev.ndx,
          price: newNdxPrice,
          change: ndxChange,
          percentChange: ndxPct,
          dayHigh: newNdxHigh,
          dayLow: newNdxLow,
          sparkline: newNdxSpark,
          lastUpdated: 'Just now',
        },
        lastSyncTime: new Date(),
      };
    });
    showToast('Market quotes updated');
  };

  const handleRefreshQuotes = async () => {
    setIsRefreshing(true);
    // Simulate network query to financial endpoint
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

  return (
    <div className="min-h-screen bg-neutral-950 text-neutral-100 flex flex-col antialiased">
      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed bottom-5 right-5 z-50 bg-neutral-900 border border-neutral-700 text-white px-4 py-2.5 rounded-lg shadow-xl text-xs flex items-center gap-2 animate-in fade-in slide-in-from-bottom-2">
          <CheckCircle2 className="w-4 h-4 text-emerald-400" />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* Mandatory Top Bar Contract: 3 Zones: Brand Title — Nav Links — 1 Primary Action */}
      <header className="flex items-center justify-between gap-8 px-6 py-4 border-b border-neutral-800 bg-neutral-950/80 backdrop-blur sticky top-0 z-40">
        {/* Zone 1: Brand Wordmark */}
        <div className="flex items-center gap-2.5 whitespace-nowrap shrink-0">
          <div className="w-7 h-7 rounded-full bg-neutral-900 border border-neutral-700 flex items-center justify-center">
            <Watch className="w-4 h-4 text-sky-400" />
          </div>
          <span className="text-lg font-extrabold tracking-tight text-white">
            MarketWear
          </span>
        </div>

        {/* Zone 2: 4 Single-Line Text Navigation Links */}
        <nav className="hidden md:flex items-center gap-6 text-sm font-medium text-neutral-400">
          <button
            onClick={() => setActiveTab('simulator')}
            className={`transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
              activeTab === 'simulator' ? 'text-white font-semibold' : 'hover:text-neutral-200'
            }`}
          >
            Watch Simulator
          </button>
          <button
            onClick={() => setActiveTab('config')}
            className={`transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
              activeTab === 'config' ? 'text-white font-semibold' : 'hover:text-neutral-200'
            }`}
          >
            Complication Formats
          </button>
          <button
            onClick={() => setActiveTab('source')}
            className={`transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
              activeTab === 'source' ? 'text-white font-semibold' : 'hover:text-neutral-200'
            }`}
          >
            Wear OS Source Code
          </button>
          <button
            onClick={() => setActiveTab('install')}
            className={`transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
              activeTab === 'install' ? 'text-white font-semibold' : 'hover:text-neutral-200'
            }`}
          >
            Pixel Watch 4 Setup
          </button>
        </nav>

        {/* Zone 3: 1 Primary Action */}
        <div className="flex items-center gap-3 shrink-0">
          <button
            onClick={handleQuickExport}
            disabled={isExporting}
            className="flex items-center gap-2 px-4 py-2 text-xs font-semibold text-white bg-sky-600 rounded-lg hover:bg-sky-500 transition-colors whitespace-nowrap shrink-0 cursor-pointer disabled:opacity-50"
          >
            <Download className="w-3.5 h-3.5" />
            <span>{isExporting ? 'Exporting...' : 'Export Wear Project'}</span>
          </button>
        </div>
      </header>

      {/* Main Content Container */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
        {/* Editorial Subtitle & Context */}
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 pb-4 border-b border-neutral-900">
          <div>
            <div className="flex items-center gap-2 text-xs text-neutral-400 mb-1">
              <span>Google Pixel Watch 4</span>
              <span aria-hidden="true">·</span>
              <span>Wear OS 4 / 5</span>
              <span aria-hidden="true">·</span>
              <span>Personal App Library Ready</span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-white">
              S&amp;P 500 &amp; Nasdaq 100 Complications
            </h1>
            <p className="text-sm text-neutral-400 mt-1 max-w-2xl">
              Clean, isolated Android Wear OS app providing complication data sources for Google Pixel Watch dials with zero background battery drain.
            </p>
          </div>

          {/* Quick tab switcher for mobile/desktop viewports */}
          <div className="flex items-center gap-1 p-1 bg-neutral-900 border border-neutral-800 rounded-lg self-start md:self-auto">
            <button
              onClick={() => setActiveTab('simulator')}
              className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors ${
                activeTab === 'simulator' ? 'bg-neutral-800 text-white' : 'text-neutral-400 hover:text-white'
              }`}
            >
              Watch Face
            </button>
            <button
              onClick={() => setActiveTab('config')}
              className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors ${
                activeTab === 'config' ? 'bg-neutral-800 text-white' : 'text-neutral-400 hover:text-white'
              }`}
            >
              Settings
            </button>
            <button
              onClick={() => setActiveTab('source')}
              className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors ${
                activeTab === 'source' ? 'bg-neutral-800 text-white' : 'text-neutral-400 hover:text-white'
              }`}
            >
              Source
            </button>
            <button
              onClick={() => setActiveTab('install')}
              className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors ${
                activeTab === 'install' ? 'bg-neutral-800 text-white' : 'text-neutral-400 hover:text-white'
              }`}
            >
              Installation
            </button>
          </div>
        </div>

        {/* Live Market Indices Bar (Always visible across all tabs for instant glance) */}
        <MarketIndicesStrip
          spx={marketState.spx}
          ndx={marketState.ndx}
          marketStatus={marketState.status}
          statusText={marketState.statusText}
          onSimulateTick={simulateTick}
          onRefreshQuotes={handleRefreshQuotes}
          isRefreshing={isRefreshing}
        />

        {/* TAB CONTENT */}
        {activeTab === 'simulator' && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
            {/* Left 6 cols: Pixel Watch 4 Circular Hardware Simulator */}
            <div className="lg:col-span-6 bg-neutral-900/50 border border-neutral-800/80 rounded-2xl p-6 flex flex-col items-center justify-center">
              <div className="w-full flex items-center justify-between text-xs text-neutral-400 mb-6">
                <span className="font-semibold text-neutral-200">Pixel Watch 4 AMOLED Simulator</span>
                <span className="font-mono-numbers">450 × 450 px · 320 DPI</span>
              </div>

              <PixelWatch4Simulator
                spx={marketState.spx}
                ndx={marketState.ndx}
                marketStatus={marketState.status}
                selectedFormat={selectedFormat}
                complicationSlots={complicationSlots}
                onSlotChange={handleSlotChange}
                onRefreshQuotes={handleRefreshQuotes}
                isRefreshing={isRefreshing}
              />
            </div>

            {/* Right 6 cols: Complication Quick Controls & Specs */}
            <div className="lg:col-span-6 space-y-6">
              <ComplicationConfigurator
                spx={marketState.spx}
                ndx={marketState.ndx}
                selectedFormat={selectedFormat}
                onFormatChange={setSelectedFormat}
                complicationSlots={complicationSlots}
                onSlotChange={handleSlotChange}
              />

              {/* Hardware Spec Card */}
              <div className="bg-neutral-900/40 border border-neutral-800/80 rounded-xl p-4 text-xs text-neutral-400 space-y-2">
                <div className="flex items-center gap-2 font-semibold text-neutral-200">
                  <Shield className="w-4 h-4 text-sky-400" />
                  <span>Wear OS Complication Specifications</span>
                </div>
                <p className="leading-relaxed">
                  Both services implement <code className="text-sky-300">SuspendingComplicationDataSourceService</code> with metadata type flags for <code className="text-neutral-300">SHORT_TEXT</code>, <code className="text-neutral-300">RANGED_VALUE</code>, and <code className="text-neutral-300">LONG_TEXT</code>. Tapping any complication issues a PendingIntent directly into the isolated watch activity.
                </p>
              </div>
            </div>
          </div>
        )}

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

        {activeTab === 'source' && (
          <div className="space-y-6">
            <AndroidCodeViewer />
          </div>
        )}

        {activeTab === 'install' && (
          <div className="space-y-6">
            <InstallationGuide />
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="border-t border-neutral-900 bg-neutral-950 py-6 px-6 text-xs text-neutral-500">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <span>MarketWear for Wear OS</span>
            <span>·</span>
            <span>Target SDK 34 (Android 14 / Wear OS 5)</span>
            <span>·</span>
            <span>Standalone Complication Provider</span>
          </div>
          <div>
            Built for Google Pixel Watch (1, 2, 3, 4)
          </div>
        </div>
      </footer>
    </div>
  );
}
