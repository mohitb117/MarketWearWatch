import React, { useState } from 'react';
import { WEAR_OS_PROJECT_FILES, ProjectFile } from '../data/wearProjectFiles';
import { generateWearOsProjectZip, downloadBlob } from '../utils/zipExport';
import { FolderTree, Copy, Check, Download, FileCode, Cpu, ShieldCheck } from 'lucide-react';

export const AndroidCodeViewer: React.FC = () => {
  const [selectedFile, setSelectedFile] = useState<ProjectFile>(WEAR_OS_PROJECT_FILES[1]); // Default to Sp500ComplicationService.kt
  const [copied, setCopied] = useState<boolean>(false);
  const [isExporting, setIsExporting] = useState<boolean>(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(selectedFile.content);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleDownloadZip = async () => {
    try {
      setIsExporting(true);
      const zipBlob = await generateWearOsProjectZip();
      downloadBlob(zipBlob, 'MarketWear-PixelWatch-Project.zip');
    } catch (err) {
      console.error('Failed to export zip:', err);
    } finally {
      setIsExporting(false);
    }
  };

  const getCategoryColor = (cat: ProjectFile['category']) => {
    switch (cat) {
      case 'kotlin':
        return 'text-amber-400 bg-amber-400/10 border-amber-400/20';
      case 'manifest':
        return 'text-sky-400 bg-sky-400/10 border-sky-400/20';
      case 'gradle':
        return 'text-purple-400 bg-purple-400/10 border-purple-400/20';
      case 'resource':
        return 'text-emerald-400 bg-emerald-400/10 border-emerald-400/20';
      case 'doc':
        return 'text-neutral-300 bg-neutral-800 border-neutral-700';
    }
  };

  return (
    <div className="bg-neutral-900/90 border border-neutral-800 rounded-xl overflow-hidden flex flex-col">
      {/* Header bar */}
      <div className="px-5 py-4 border-b border-neutral-800 flex flex-wrap items-center justify-between gap-4 bg-neutral-950/60">
        <div>
          <div className="flex items-center gap-2">
            <Cpu className="w-4 h-4 text-sky-400" />
            <h3 className="font-bold text-white text-base">Android Wear OS Project Source</h3>
          </div>
          <p className="text-xs text-neutral-400 mt-0.5">
            Production-grade Kotlin project using official AndroidX WatchFace Complications API.
          </p>
        </div>

        <button
          onClick={handleDownloadZip}
          disabled={isExporting}
          className="flex items-center gap-2 px-4 py-2 rounded-lg bg-sky-600 hover:bg-sky-500 text-white text-xs font-semibold shadow-md transition-all cursor-pointer disabled:opacity-50"
        >
          <Download className="w-3.5 h-3.5" />
          <span>{isExporting ? 'Generating ZIP...' : 'Download Android Studio Project (.ZIP)'}</span>
        </button>
      </div>

      {/* Code Browser Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 min-h-[500px]">
        {/* Left Sidebar: File Tree */}
        <div className="lg:col-span-4 border-r border-neutral-800/80 bg-neutral-950/40 p-3 space-y-1 overflow-y-auto max-h-[600px]">
          <div className="text-[11px] font-bold text-neutral-400 uppercase tracking-wider px-2 py-1 flex items-center gap-1.5">
            <FolderTree className="w-3 h-3" />
            <span>Project Explorer</span>
          </div>

          {WEAR_OS_PROJECT_FILES.map((file) => {
            const isSelected = selectedFile.path === file.path;
            return (
              <button
                key={file.path}
                onClick={() => setSelectedFile(file)}
                className={`w-full text-left px-2.5 py-2 rounded-lg transition-all flex items-start justify-between gap-2 text-xs ${
                  isSelected
                    ? 'bg-neutral-800/90 text-white font-medium shadow-sm'
                    : 'text-neutral-400 hover:bg-neutral-900 hover:text-neutral-200'
                }`}
              >
                <div className="truncate flex items-center gap-1.5 min-w-0">
                  <FileCode className={`w-3.5 h-3.5 shrink-0 ${isSelected ? 'text-sky-400' : 'text-neutral-500'}`} />
                  <span className="truncate">{file.name}</span>
                </div>
                <span className={`text-[10px] uppercase font-mono-numbers px-1.5 py-0.5 rounded border shrink-0 ${getCategoryColor(file.category)}`}>
                  {file.category}
                </span>
              </button>
            );
          })}
        </div>

        {/* Right Main Area: Code Viewer */}
        <div className="lg:col-span-8 flex flex-col bg-neutral-950">
          {/* File Top Bar */}
          <div className="px-4 py-2.5 border-b border-neutral-800 flex items-center justify-between bg-neutral-900/40">
            <div className="flex flex-col min-w-0">
              <span className="text-xs font-mono text-neutral-200 truncate">{selectedFile.path}</span>
              <span className="text-[11px] text-neutral-400 truncate">{selectedFile.description}</span>
            </div>

            <button
              onClick={handleCopy}
              className="flex items-center gap-1.5 px-3 py-1 rounded bg-neutral-800 hover:bg-neutral-700 text-neutral-200 text-xs transition-colors shrink-0 cursor-pointer"
            >
              {copied ? (
                <>
                  <Check className="w-3.5 h-3.5 text-emerald-400" />
                  <span className="text-emerald-400">Copied</span>
                </>
              ) : (
                <>
                  <Copy className="w-3.5 h-3.5 text-neutral-400" />
                  <span>Copy File</span>
                </>
              )}
            </button>
          </div>

          {/* Syntax Code Body */}
          <div className="p-4 overflow-x-auto font-mono text-[11.5px] leading-relaxed text-neutral-300 max-h-[540px] select-text">
            <pre className="whitespace-pre">{selectedFile.content}</pre>
          </div>
        </div>
      </div>

      {/* Architecture Highlights Footer */}
      <div className="p-4 bg-neutral-950/80 border-t border-neutral-800 grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs">
        <div className="flex items-start gap-2 text-neutral-400">
          <ShieldCheck className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
          <div>
            <strong className="text-neutral-200 block">Battery-Safe Architecture</strong>
            15-minute cached polling with zero awake loops, compliant with Wear OS power requirements.
          </div>
        </div>
        <div className="flex items-start gap-2 text-neutral-400">
          <ShieldCheck className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
          <div>
            <strong className="text-neutral-200 block">Offline & Ambient Ready</strong>
            Instantly displays cached quotes if watch is disconnected or running in AOD low-power mode.
          </div>
        </div>
        <div className="flex items-start gap-2 text-neutral-400">
          <ShieldCheck className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
          <div>
            <strong className="text-neutral-200 block">Standalone Watch App</strong>
            Marked <code className="text-sky-300">com.google.android.wearable.standalone=true</code> for direct watch installation.
          </div>
        </div>
      </div>
    </div>
  );
};
