import React, { useState } from 'react';
import { Smartphone, Terminal, Watch, Check, Copy, CheckCircle2, ChevronRight, ExternalLink, ShieldCheck, Sparkles, AlertCircle } from 'lucide-react';
import { generateWearOsProjectZip, downloadBlob } from '../utils/zipExport';

export const InstallationGuide: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'github_action' | 'personal_library' | 'wireless_adb' | 'wear_installer' | 'watch_face'>('github_action');
  const [activeSubMethod, setActiveSubMethod] = useState<'internal_sharing' | 'internal_testing'>('internal_sharing');
  const [completedSteps, setCompletedSteps] = useState<Record<string, boolean>>({});
  const [watchIp, setWatchIp] = useState<string>('192.168.1.145');
  const [pairingPort, setPairingPort] = useState<string>('38491');
  const [connectPort, setConnectPort] = useState<string>('41285');
  const [copiedText, setCopiedText] = useState<string | null>(null);
  const [isExporting, setIsExporting] = useState<boolean>(false);

  const toggleStep = (stepId: string) => {
    setCompletedSteps(prev => ({ ...prev, [stepId]: !prev[stepId] }));
  };

  const copyToClipboard = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedText(id);
    setTimeout(() => setCopiedText(null), 2000);
  };

  const handleDownloadProject = async () => {
    try {
      setIsExporting(true);
      const blob = await generateWearOsProjectZip();
      downloadBlob(blob, 'MarketWear-PixelWatch-Project.zip');
    } catch (e) {
      console.error(e);
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <div className="bg-neutral-900/90 border border-neutral-800 rounded-xl p-5 sm:p-6 space-y-6">
      {/* Top Header */}
      <div className="flex flex-wrap items-center justify-between gap-4 pb-4 border-b border-neutral-800">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-xs uppercase tracking-wider font-bold text-sky-400">Pixel Watch 4 Installation</span>
            <span className="text-xs text-neutral-500">·</span>
            <span className="text-xs text-neutral-400">Wear OS 4 &amp; 5</span>
          </div>
          <h2 className="text-xl font-bold text-white mt-0.5">
            Install via Personal App Library
          </h2>
          <p className="text-xs text-neutral-400 mt-1 max-w-2xl">
            How to publish this isolated Android Wear app to your personal Google Play library so it appears directly on your Pixel Watch 4 for one-tap installation.
          </p>
        </div>

        {/* Tab Navigation */}
        <div className="flex items-center gap-1 p-1 bg-neutral-950 border border-neutral-800 rounded-lg text-xs">
          <button
            onClick={() => setActiveTab('github_action')}
            className={`px-3 py-1.5 font-medium rounded-md transition-colors ${
              activeTab === 'github_action'
                ? 'bg-sky-600 text-white shadow-sm'
                : 'text-neutral-400 hover:text-neutral-200'
            }`}
          >
            GitHub Action (Cloud Build)
          </button>
          <button
            onClick={() => setActiveTab('personal_library')}
            className={`px-3 py-1.5 font-medium rounded-md transition-colors ${
              activeTab === 'personal_library'
                ? 'bg-sky-600 text-white shadow-sm'
                : 'text-neutral-400 hover:text-neutral-200'
            }`}
          >
            Play Store Personal Library
          </button>
          <button
            onClick={() => setActiveTab('watch_face')}
            className={`px-3 py-1.5 font-medium rounded-md transition-colors ${
              activeTab === 'watch_face'
                ? 'bg-neutral-800 text-white shadow-sm'
                : 'text-neutral-400 hover:text-neutral-200'
            }`}
          >
            Add Complication to Dial
          </button>
          <button
            onClick={() => setActiveTab('wireless_adb')}
            className={`px-3 py-1.5 font-medium rounded-md transition-colors ${
              activeTab === 'wireless_adb'
                ? 'bg-neutral-800 text-white shadow-sm'
                : 'text-neutral-400 hover:text-neutral-200'
            }`}
          >
            Wireless ADB (Dev)
          </button>
        </div>
      </div>

      {/* TAB 0: GITHUB ACTIONS CLOUD BUILD */}
      {activeTab === 'github_action' && (
        <div className="space-y-6">
          <div className="p-4 rounded-xl bg-sky-950/30 border border-sky-800/40 text-xs text-sky-200 leading-relaxed flex items-start gap-3">
            <Sparkles className="w-5 h-5 text-sky-400 shrink-0 mt-0.5" />
            <div className="space-y-1">
              <strong className="text-white text-sm font-semibold block">
                Free Automated Cloud Build (Zero Android Studio Needed)
              </strong>
              <p className="text-neutral-300">
                You do not need to install Android Studio, Java, or the 3GB Android SDK on your computer. When you push this project to your GitHub account, GitHub Actions uses its pre-configured cloud runner to compile the signed <strong>Android App Bundle (.aab)</strong> in approximately 60 seconds!
              </p>
            </div>
          </div>

          {/* Step 1: Download & Git Push */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="w-5 h-5 rounded-full bg-sky-600 text-white flex items-center justify-center font-bold text-[11px]">1</span>
                <span className="text-xs font-bold text-white uppercase tracking-wider">Push Project to Your GitHub Account</span>
              </div>
              <button
                onClick={handleDownloadProject}
                disabled={isExporting}
                className="flex items-center gap-1.5 px-3 py-1 rounded bg-sky-600 hover:bg-sky-500 text-white text-xs font-medium transition-colors"
              >
                <span>{isExporting ? 'Generating...' : 'Download Project (.ZIP)'}</span>
              </button>
            </div>

            <p className="text-xs text-neutral-400">
              Download and extract the zip file, create a new repository on <a href="https://github.com/new" target="_blank" rel="noreferrer" className="text-sky-400 hover:underline">github.com/new</a> (can be private or public), then run in your terminal:
            </p>

            <div className="relative p-3.5 bg-neutral-950 border border-neutral-800 rounded-lg">
              <button
                onClick={() => copyToClipboard(`git init\ngit add .\ngit commit -m "Initial MarketWear Pixel Watch app"\ngit branch -M main\ngit remote add origin https://github.com/YOUR_USERNAME/marketwear-pixel-watch.git\ngit push -u origin main`, 'git_cmds')}
                className="absolute top-2.5 right-2.5 flex items-center gap-1 text-[11px] text-neutral-400 hover:text-white px-2 py-0.5 rounded bg-neutral-900 border border-neutral-800"
              >
                {copiedText === 'git_cmds' ? (
                  <>
                    <Check className="w-3 h-3 text-emerald-400" />
                    <span className="text-emerald-400">Copied</span>
                  </>
                ) : (
                  <>
                    <Copy className="w-3 h-3" />
                    <span>Copy Commands</span>
                  </>
                )}
              </button>
              <pre className="font-mono text-[11px] text-sky-300 overflow-x-auto leading-relaxed select-all">
{`# 1. Open the extracted project folder
cd MarketWear-PixelWatch

# 2. Initialize git repository
git init
git add .
git commit -m "Initial MarketWear Pixel Watch app"
git branch -M main

# 3. Connect to your GitHub repository and push
git remote add origin https://github.com/YOUR_USERNAME/marketwear-pixel-watch.git
git push -u origin main`}
              </pre>
            </div>
          </div>

          {/* Step 2: GitHub Action Executes */}
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <span className="w-5 h-5 rounded-full bg-sky-600 text-white flex items-center justify-center font-bold text-[11px]">2</span>
              <span className="text-xs font-bold text-white uppercase tracking-wider">GitHub Compiles the .aab in the Cloud</span>
            </div>
            <p className="text-xs text-neutral-400">
              The included workflow <code className="text-sky-300">.github/workflows/build-wear-bundle.yml</code> activates automatically on push:
            </p>

            {/* Visual Action Run Mockup */}
            <div className="p-3.5 bg-neutral-950 border border-neutral-800 rounded-lg space-y-2">
              <div className="flex items-center justify-between text-xs border-b border-neutral-800 pb-2">
                <span className="font-semibold text-white flex items-center gap-2">
                  <span className="w-2.5 h-2.5 rounded-full bg-emerald-500" />
                  <span>Build Pixel Watch Bundle (AAB &amp; APK)</span>
                </span>
                <span className="text-neutral-400 font-mono text-[11px]">Duration: ~58s</span>
              </div>
              <div className="text-xs text-neutral-300 space-y-1">
                <div>Artifacts generated for download:</div>
                <div className="flex flex-wrap items-center gap-2 pt-1">
                  <span className="px-2.5 py-1 bg-neutral-900 border border-neutral-700 rounded text-sky-300 font-mono text-[11px]">
                    📦 MarketWear-PixelWatch-Release-AAB.zip
                  </span>
                  <span className="px-2.5 py-1 bg-neutral-900 border border-neutral-700 rounded text-neutral-400 font-mono text-[11px]">
                    📦 MarketWear-PixelWatch-Release-APK.zip
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* Step 3: Next Steps */}
          <div className="p-4 bg-neutral-950 border border-neutral-800 rounded-xl flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="space-y-1 text-xs text-neutral-300">
              <strong className="text-white text-sm block font-semibold">Ready to install on Pixel Watch 4?</strong>
              <p className="text-neutral-400">
                Once downloaded from GitHub, drop the <code className="text-sky-300">app-release.aab</code> into Google Play Internal App Sharing to sync directly to your watch's Personal Library.
              </p>
            </div>
            <button
              onClick={() => setActiveTab('personal_library')}
              className="px-4 py-2 rounded-lg bg-sky-600 hover:bg-sky-500 text-white text-xs font-semibold whitespace-nowrap transition-colors"
            >
              View Play Store Library Steps &gt;
            </button>
          </div>
        </div>
      )}

      {/* TAB 1: PERSONAL APP LIBRARY WALKTHROUGH */}
      {activeTab === 'personal_library' && (
        <div className="space-y-6">
          {/* Explanation Banner */}
          <div className="p-4 rounded-xl bg-sky-950/30 border border-sky-800/40 text-xs text-sky-200 leading-relaxed flex items-start gap-3">
            <ShieldCheck className="w-5 h-5 text-sky-400 shrink-0 mt-0.5" />
            <div className="space-y-1">
              <strong className="text-white text-sm font-semibold block">
                How Wear OS Personal App Library Works
              </strong>
              <p className="text-neutral-300">
                On Pixel Watch 4, the <strong>Google Play Store</strong> syncs apps linked to your Google Account (<code className="text-sky-300">mohitb117@gmail.com</code>). When you upload the app bundle (<code className="text-neutral-200">.aab</code>) to your private Google Play Console, Google Play instantly classifies it as part of your <strong>Personal Library</strong>. It will show up on your watch under <em>&quot;Apps on your phone&quot;</em> or <em>&quot;Manage apps &gt; Library&quot;</em> without ever going through public app store review!
              </p>
            </div>
          </div>

          {/* Sub-method switcher (Internal App Sharing vs Internal Testing Track) */}
          <div className="space-y-2">
            <div className="text-xs font-semibold text-neutral-300 uppercase tracking-wider">
              Choose Your Play Console Upload Path
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <button
                onClick={() => setActiveSubMethod('internal_sharing')}
                className={`p-3.5 rounded-lg border text-left transition-all ${
                  activeSubMethod === 'internal_sharing'
                    ? 'bg-sky-950/20 border-sky-500/60 ring-1 ring-sky-500/30'
                    : 'bg-neutral-950/60 border-neutral-800 hover:border-neutral-700'
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className="font-bold text-xs text-white">Method A: Internal App Sharing (Fastest)</span>
                  {activeSubMethod === 'internal_sharing' && <Check className="w-3.5 h-3.5 text-sky-400" />}
                </div>
                <p className="text-xs text-neutral-400 mt-1">
                  Zero approval wait time. Upload the AAB or APK, click the private install link once on your phone/browser, and it immediately appears in your watch's Personal Library.
                </p>
              </button>

              <button
                onClick={() => setActiveSubMethod('internal_testing')}
                className={`p-3.5 rounded-lg border text-left transition-all ${
                  activeSubMethod === 'internal_testing'
                    ? 'bg-sky-950/20 border-sky-500/60 ring-1 ring-sky-500/30'
                    : 'bg-neutral-950/60 border-neutral-800 hover:border-neutral-700'
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className="font-bold text-xs text-white">Method B: Internal Testing Track</span>
                  {activeSubMethod === 'internal_testing' && <Check className="w-3.5 h-3.5 text-sky-400" />}
                </div>
                <p className="text-xs text-neutral-400 mt-1">
                  Full Play Console track. Add your email address to the internal testers list. Gives automatic background updates whenever you upload a new version.
                </p>
              </button>
            </div>
          </div>

          {/* Interactive Step-by-Step Checklist */}
          <div className="space-y-4 pt-2">
            <div className="text-xs font-semibold text-neutral-300 uppercase tracking-wider flex items-center justify-between">
              <span>Step-by-Step Execution Guide</span>
              <span className="text-neutral-500 font-normal">Click checkmarks as you complete each step</span>
            </div>

            {/* STEP 1 */}
            <div className={`p-4 rounded-xl border transition-all ${completedSteps['step1'] ? 'bg-neutral-950/40 border-neutral-800/60 opacity-80' : 'bg-neutral-950 border-neutral-800'}`}>
              <div className="flex items-start gap-3">
                <button
                  onClick={() => toggleStep('step1')}
                  className={`w-6 h-6 rounded-full border flex items-center justify-center shrink-0 mt-0.5 transition-colors ${
                    completedSteps['step1']
                      ? 'bg-emerald-500 border-emerald-500 text-white'
                      : 'border-neutral-700 hover:border-sky-400 text-neutral-500'
                  }`}
                >
                  {completedSteps['step1'] ? <Check className="w-3.5 h-3.5" /> : <span className="text-xs font-bold">1</span>}
                </button>

                <div className="flex-1 space-y-2">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <h3 className="text-sm font-bold text-white">
                      Download the Wear OS Android Studio Project
                    </h3>
                    <button
                      onClick={handleDownloadProject}
                      disabled={isExporting}
                      className="flex items-center gap-1.5 px-3 py-1 rounded bg-sky-600 hover:bg-sky-500 text-white text-xs font-medium transition-colors"
                    >
                      <span>{isExporting ? 'Generating...' : 'Download Project (.ZIP)'}</span>
                    </button>
                  </div>
                  <p className="text-xs text-neutral-400 leading-relaxed">
                    Unzip <code className="text-neutral-200">MarketWear-PixelWatch-Project.zip</code> on your computer and open the folder in <strong>Android Studio</strong> (Koala or newer). Android Studio will automatically sync the Gradle configuration and dependencies.
                  </p>
                </div>
              </div>
            </div>

            {/* STEP 2 */}
            <div className={`p-4 rounded-xl border transition-all ${completedSteps['step2'] ? 'bg-neutral-950/40 border-neutral-800/60 opacity-80' : 'bg-neutral-950 border-neutral-800'}`}>
              <div className="flex items-start gap-3">
                <button
                  onClick={() => toggleStep('step2')}
                  className={`w-6 h-6 rounded-full border flex items-center justify-center shrink-0 mt-0.5 transition-colors ${
                    completedSteps['step2']
                      ? 'bg-emerald-500 border-emerald-500 text-white'
                      : 'border-neutral-700 hover:border-sky-400 text-neutral-500'
                  }`}
                >
                  {completedSteps['step2'] ? <Check className="w-3.5 h-3.5" /> : <span className="text-xs font-bold">2</span>}
                </button>

                <div className="flex-1 space-y-2">
                  <h3 className="text-sm font-bold text-white">
                    Generate the Signed Android App Bundle (.aab)
                  </h3>
                  <p className="text-xs text-neutral-400 leading-relaxed">
                    In Android Studio menu bar, select:
                  </p>
                  <div className="p-2.5 bg-neutral-900 rounded-lg text-xs font-mono text-neutral-200">
                    Build &gt; Generate Signed Bundle / APK &gt; Android App Bundle (.aab)
                  </div>
                  <p className="text-xs text-neutral-400 leading-relaxed">
                    Select or create your private keystore file (save it safely) and choose release build variant. Android Studio will generate <code className="text-sky-300">app-release.aab</code> inside <code className="text-neutral-300">app/release/</code>.
                  </p>
                </div>
              </div>
            </div>

            {/* STEP 3 (DYNAMIC BASED ON METHOD A OR B) */}
            {activeSubMethod === 'internal_sharing' ? (
              <div className={`p-4 rounded-xl border transition-all ${completedSteps['step3'] ? 'bg-neutral-950/40 border-neutral-800/60 opacity-80' : 'bg-neutral-950 border-neutral-800'}`}>
                <div className="flex items-start gap-3">
                  <button
                    onClick={() => toggleStep('step3')}
                    className={`w-6 h-6 rounded-full border flex items-center justify-center shrink-0 mt-0.5 transition-colors ${
                      completedSteps['step3']
                        ? 'bg-emerald-500 border-emerald-500 text-white'
                        : 'border-neutral-700 hover:border-sky-400 text-neutral-500'
                    }`}
                  >
                    {completedSteps['step3'] ? <Check className="w-3.5 h-3.5" /> : <span className="text-xs font-bold">3</span>}
                  </button>

                  <div className="flex-1 space-y-2">
                    <div className="flex items-center justify-between">
                      <h3 className="text-sm font-bold text-white">
                        Upload to Google Play Console: Internal App Sharing
                      </h3>
                      <a
                        href="https://play.google.com/console"
                        target="_blank"
                        rel="noreferrer"
                        className="flex items-center gap-1 text-xs text-sky-400 hover:underline"
                      >
                        <span>Open Play Console</span>
                        <ExternalLink className="w-3 h-3" />
                      </a>
                    </div>
                    <ol className="text-xs text-neutral-400 space-y-1.5 list-decimal list-inside leading-relaxed">
                      <li>In Google Play Console, go to <strong>Setup &gt; Internal app sharing</strong> (or search &quot;Internal app sharing&quot;).</li>
                      <li>Under the <strong>Uploaders and testers</strong> tab, ensure your Google email (<code className="text-neutral-200">mohitb117@gmail.com</code>) is in the testers list.</li>
                      <li>Drop your generated <code className="text-sky-300">app-release.aab</code> into the upload box.</li>
                      <li>Copy the generated <strong>Share link</strong> and open it in Chrome on your phone or computer.</li>
                      <li>Click <strong>Install / Add to Library</strong> on the Google Play webpage.</li>
                    </ol>
                  </div>
                </div>
              </div>
            ) : (
              <div className={`p-4 rounded-xl border transition-all ${completedSteps['step3'] ? 'bg-neutral-950/40 border-neutral-800/60 opacity-80' : 'bg-neutral-950 border-neutral-800'}`}>
                <div className="flex items-start gap-3">
                  <button
                    onClick={() => toggleStep('step3')}
                    className={`w-6 h-6 rounded-full border flex items-center justify-center shrink-0 mt-0.5 transition-colors ${
                      completedSteps['step3']
                        ? 'bg-emerald-500 border-emerald-500 text-white'
                        : 'border-neutral-700 hover:border-sky-400 text-neutral-500'
                    }`}
                  >
                    {completedSteps['step3'] ? <Check className="w-3.5 h-3.5" /> : <span className="text-xs font-bold">3</span>}
                  </button>

                  <div className="flex-1 space-y-2">
                    <div className="flex items-center justify-between">
                      <h3 className="text-sm font-bold text-white">
                        Create Internal Testing Track in Google Play Console
                      </h3>
                      <a
                        href="https://play.google.com/console"
                        target="_blank"
                        rel="noreferrer"
                        className="flex items-center gap-1 text-xs text-sky-400 hover:underline"
                      >
                        <span>Open Play Console</span>
                        <ExternalLink className="w-3 h-3" />
                      </a>
                    </div>
                    <ol className="text-xs text-neutral-400 space-y-1.5 list-decimal list-inside leading-relaxed">
                      <li>Create an app in Google Play Console named <strong>MarketWear Complications</strong>.</li>
                      <li>Navigate to <strong>Release &gt; Testing &gt; Internal testing</strong>.</li>
                      <li>Click <strong>Create new release</strong> and upload your <code className="text-sky-300">app-release.aab</code> file.</li>
                      <li>Go to the <strong>Testers</strong> tab, create an email list with <code className="text-neutral-200">mohitb117@gmail.com</code>, and check it.</li>
                      <li>Copy the <strong>Join on the web</strong> link, open it, and click <strong>Accept invite / Opt-in</strong>.</li>
                    </ol>
                  </div>
                </div>
              </div>
            )}

            {/* STEP 4: ON THE PIXEL WATCH 4 */}
            <div className={`p-4 rounded-xl border transition-all ${completedSteps['step4'] ? 'bg-neutral-950/40 border-neutral-800/60 opacity-80' : 'bg-neutral-950 border-neutral-800'}`}>
              <div className="flex items-start gap-3">
                <button
                  onClick={() => toggleStep('step4')}
                  className={`w-6 h-6 rounded-full border flex items-center justify-center shrink-0 mt-0.5 transition-colors ${
                    completedSteps['step4']
                      ? 'bg-emerald-500 border-emerald-500 text-white'
                      : 'border-neutral-700 hover:border-sky-400 text-neutral-500'
                  }`}
                >
                  {completedSteps['step4'] ? <Check className="w-3.5 h-3.5" /> : <span className="text-xs font-bold">4</span>}
                </button>

                <div className="flex-1 space-y-3">
                  <h3 className="text-sm font-bold text-white">
                    Open Pixel Watch 4 Play Store &gt; Personal Library
                  </h3>
                  <p className="text-xs text-neutral-400 leading-relaxed">
                    Now pick up your Pixel Watch 4:
                  </p>

                  {/* Visual Pixel Watch Screen Mockup */}
                  <div className="bg-neutral-900 border border-neutral-800 rounded-xl p-3.5 max-w-sm space-y-2">
                    <div className="flex items-center justify-between text-[11px] text-neutral-400 border-b border-neutral-800 pb-2">
                      <span className="font-bold text-white flex items-center gap-1.5">
                        <Watch className="w-3.5 h-3.5 text-sky-400" />
                        <span>Google Play on Pixel Watch</span>
                      </span>
                      <span className="text-emerald-400 font-mono text-[10px]">Synced</span>
                    </div>

                    <div className="text-xs space-y-2">
                      <div className="text-neutral-300">
                        Method 1: Scroll to the bottom of the Play Store home screen to:
                        <div className="mt-1 px-2.5 py-1.5 bg-neutral-950 rounded border border-neutral-800 font-semibold text-white flex items-center justify-between">
                          <span>Apps on your phone</span>
                          <ChevronRight className="w-3.5 h-3.5 text-neutral-500" />
                        </div>
                      </div>
                      <div className="text-neutral-300">
                        Method 2: Tap your Google Profile circle at top &gt;
                        <div className="mt-1 px-2.5 py-1.5 bg-neutral-950 rounded border border-neutral-800 font-semibold text-white flex items-center justify-between">
                          <span>Manage apps &gt; Library</span>
                          <ChevronRight className="w-3.5 h-3.5 text-neutral-500" />
                        </div>
                      </div>
                    </div>

                    <div className="mt-2 pt-2 border-t border-neutral-800 flex items-center justify-between">
                      <div>
                        <div className="font-bold text-white text-xs">MarketWear Complications</div>
                        <div className="text-[10px] text-neutral-500">S&amp;P 500 &amp; Nasdaq 100</div>
                      </div>
                      <button className="px-3 py-1 bg-sky-600 text-white rounded-full text-[11px] font-semibold">
                        Install
                      </button>
                    </div>
                  </div>

                  <p className="text-xs text-neutral-400">
                    Tap <strong>Install</strong>. Wear OS will immediately download the isolated complication service onto your Pixel Watch storage!
                  </p>
                </div>
              </div>
            </div>

            {/* STEP 5: ADD TO DIAL */}
            <div className={`p-4 rounded-xl border transition-all ${completedSteps['step5'] ? 'bg-neutral-950/40 border-neutral-800/60 opacity-80' : 'bg-neutral-950 border-neutral-800'}`}>
              <div className="flex items-start gap-3">
                <button
                  onClick={() => toggleStep('step5')}
                  className={`w-6 h-6 rounded-full border flex items-center justify-center shrink-0 mt-0.5 transition-colors ${
                    completedSteps['step5']
                      ? 'bg-emerald-500 border-emerald-500 text-white'
                      : 'border-neutral-700 hover:border-sky-400 text-neutral-500'
                  }`}
                >
                  {completedSteps['step5'] ? <Check className="w-3.5 h-3.5" /> : <span className="text-xs font-bold">5</span>}
                </button>

                <div className="flex-1 space-y-2">
                  <h3 className="text-sm font-bold text-white">
                    Add S&amp;P 500 and Nasdaq 100 to Any Watch Face
                  </h3>
                  <ol className="text-xs text-neutral-400 space-y-1.5 list-decimal list-inside leading-relaxed">
                    <li>Long press your active Pixel Watch face until the watch vibrates.</li>
                    <li>Tap the <strong>Edit (Pencil)</strong> icon below the face.</li>
                    <li>Swipe left to the <strong>Complications</strong> customization view.</li>
                    <li>Tap any slot (top, bottom, left, or right), scroll down to <strong>Market Complications</strong>, and choose:
                      <ul className="pl-6 pt-1 space-y-1 list-disc text-neutral-300">
                        <li><strong>S&amp;P 500 Index</strong> (Live points, ranged gauge, or percent)</li>
                        <li><strong>Nasdaq 100 Index</strong> (Tech benchmark)</li>
                        <li><strong>Dual Market (SPX + NDX)</strong> (Both side-by-side in one slot)</li>
                      </ul>
                    </li>
                    <li>Press the rotating crown to save and enjoy live market quotes on your wrist!</li>
                  </ol>
                </div>
              </div>
            </div>
          </div>

          {/* Troubleshooting Accordion / Card */}
          <div className="p-4 bg-neutral-950 border border-neutral-800 rounded-xl space-y-2.5">
            <div className="flex items-center gap-2 text-xs font-semibold text-neutral-300">
              <AlertCircle className="w-4 h-4 text-amber-400 shrink-0" />
              <span>Personal App Library Troubleshooting Tips</span>
            </div>
            <div className="text-xs text-neutral-400 space-y-1.5 leading-relaxed">
              <p>
                <strong>If the app does not appear immediately:</strong> Google Play sometimes takes 2–5 minutes to propagate library permissions. On your Pixel Watch, go to <strong>Settings &gt; Accounts &gt; Google &gt; Sync now</strong>.
              </p>
              <p>
                <strong>Verify your account:</strong> Ensure your Pixel Watch is signed into the exact same Google Account (<code className="text-neutral-300">mohitb117@gmail.com</code>) used in Google Play Console.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: WATCH FACE CUSTOMIZATION */}
      {activeTab === 'watch_face' && (
        <div className="space-y-4 text-xs">
          <div className="p-4 rounded-lg bg-emerald-950/20 border border-emerald-800/40 text-emerald-200">
            Once installed, the complications are immediately available to <strong>every watch face</strong> on your Pixel Watch that supports complications.
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            <div className="p-3.5 bg-neutral-950 border border-neutral-800 rounded-lg space-y-1">
              <strong className="text-white block font-semibold">1. Touch &amp; Hold Watch Face</strong>
              <p className="text-neutral-400">
                On your Pixel Watch screen, touch and hold your current watch face, then tap the <strong>Edit (Pencil)</strong> icon.
              </p>
            </div>
            <div className="p-3.5 bg-neutral-950 border border-neutral-800 rounded-lg space-y-1">
              <strong className="text-white block font-semibold">2. Select Complication Slot</strong>
              <p className="text-neutral-400">
                Swipe left to the <strong>Complications</strong> screen, and tap the circular slot where you want the stock index to appear.
              </p>
            </div>
            <div className="p-3.5 bg-neutral-950 border border-neutral-800 rounded-lg space-y-1">
              <strong className="text-white block font-semibold">3. Pick S&amp;P 500 or Nasdaq 100</strong>
              <p className="text-neutral-400">
                Scroll down to <strong>Market Complications</strong> and tap <strong>S&amp;P 500 Index</strong> or <strong>Nasdaq 100 Index</strong>. Press the crown to finish!
              </p>
            </div>
          </div>
        </div>
      )}

      {/* TAB 3: WIRELESS ADB SIDELOAD */}
      {activeTab === 'wireless_adb' && (
        <div className="space-y-4">
          <p className="text-xs text-neutral-300 leading-relaxed">
            Direct developer sideload via Wi-Fi. Takes 2 minutes and bypasses Play Store entirely:
          </p>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 p-3.5 rounded-lg bg-neutral-950 border border-neutral-800">
            <div>
              <label className="text-[11px] font-semibold text-neutral-400 block mb-1">Watch IP Address</label>
              <input
                type="text"
                value={watchIp}
                onChange={(e) => setWatchIp(e.target.value)}
                className="w-full bg-neutral-900 border border-neutral-700 text-white text-xs px-2.5 py-1.5 rounded focus:outline-none focus:border-sky-500 font-mono-numbers"
                placeholder="192.168.1.145"
              />
            </div>
            <div>
              <label className="text-[11px] font-semibold text-neutral-400 block mb-1">Wireless Pairing Port</label>
              <input
                type="text"
                value={pairingPort}
                onChange={(e) => setPairingPort(e.target.value)}
                className="w-full bg-neutral-900 border border-neutral-700 text-white text-xs px-2.5 py-1.5 rounded focus:outline-none focus:border-sky-500 font-mono-numbers"
                placeholder="38491"
              />
            </div>
            <div>
              <label className="text-[11px] font-semibold text-neutral-400 block mb-1">Connect Port</label>
              <input
                type="text"
                value={connectPort}
                onChange={(e) => setConnectPort(e.target.value)}
                className="w-full bg-neutral-900 border border-neutral-700 text-white text-xs px-2.5 py-1.5 rounded focus:outline-none focus:border-sky-500 font-mono-numbers"
                placeholder="41285"
              />
            </div>
          </div>

          <div className="space-y-2">
            {[
              {
                id: 'step1',
                title: 'Step 1: Pair with Pixel Watch 4',
                desc: 'On watch: Settings > Developer options > Wireless debugging > Pair new device',
                cmd: `adb pair ${watchIp}:${pairingPort}`,
              },
              {
                id: 'step2',
                title: 'Step 2: Connect over Wi-Fi',
                desc: 'Uses the main port shown on the Wireless Debugging main screen',
                cmd: `adb connect ${watchIp}:${connectPort}`,
              },
              {
                id: 'step3',
                title: 'Step 3: Sideload & Install the Complication App',
                desc: 'Installs directly to your watch storage',
                cmd: `./gradlew assembleRelease && adb -s ${watchIp}:${connectPort} install -r app/build/outputs/apk/release/app-release.apk`,
              },
            ].map((step) => (
              <div key={step.id} className="p-3 bg-neutral-950 border border-neutral-800 rounded-lg space-y-1.5">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-white">{step.title}</span>
                  <button
                    onClick={() => copyToClipboard(step.cmd, step.id)}
                    className="flex items-center gap-1 text-[11px] text-neutral-400 hover:text-white px-2 py-0.5 rounded bg-neutral-900 border border-neutral-800"
                  >
                    {copiedText === step.id ? (
                      <>
                        <Check className="w-3 h-3 text-emerald-400" />
                        <span className="text-emerald-400">Copied</span>
                      </>
                    ) : (
                      <>
                        <Copy className="w-3 h-3" />
                        <span>Copy</span>
                      </>
                    )}
                  </button>
                </div>
                <div className="text-[11px] text-neutral-400">{step.desc}</div>
                <code className="block p-2 bg-neutral-900 rounded font-mono text-[11px] text-sky-300 overflow-x-auto">
                  {step.cmd}
                </code>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* TAB 4: WEAR INSTALLER 2 */}
      {activeTab === 'wear_installer' && (
        <div className="space-y-4 text-xs">
          <p className="text-neutral-300 leading-relaxed">
            Free mobile tool to transfer and install APKs wirelessly from your phone to your watch:
          </p>

          <ol className="space-y-2.5 list-decimal list-inside text-neutral-300">
            <li className="p-3 bg-neutral-950 border border-neutral-800 rounded-lg">
              <strong className="text-white">Install Wear Installer 2</strong> from the Google Play Store on your Android phone and Pixel Watch.
            </li>
            <li className="p-3 bg-neutral-950 border border-neutral-800 rounded-lg">
              <strong className="text-white">Download the generated APK</strong> on your phone (or transfer <code className="text-sky-300">app-release.apk</code>).
            </li>
            <li className="p-3 bg-neutral-950 border border-neutral-800 rounded-lg">
              <strong className="text-white">Open Wear Installer 2</strong>, select your Pixel Watch IP address, and choose the downloaded APK.
            </li>
            <li className="p-3 bg-neutral-950 border border-neutral-800 rounded-lg">
              <strong className="text-white">Done!</strong> The app and complication services are installed directly onto the watch.
            </li>
          </ol>
        </div>
      )}
    </div>
  );
};
