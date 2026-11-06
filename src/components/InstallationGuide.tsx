import React, { useState } from 'react';
import { Smartphone, Terminal, Play, Watch, Check, Copy } from 'lucide-react';

export const InstallationGuide: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'personal_library' | 'wireless_adb' | 'wear_installer' | 'watch_face'>('personal_library');
  const [watchIp, setWatchIp] = useState<string>('192.168.1.145');
  const [pairingPort, setPairingPort] = useState<string>('38491');
  const [connectPort, setConnectPort] = useState<string>('41285');
  const [copiedText, setCopiedText] = useState<string | null>(null);

  const copyToClipboard = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedText(id);
    setTimeout(() => setCopiedText(null), 2000);
  };

  return (
    <div className="bg-neutral-900/90 border border-neutral-800 rounded-xl p-5 space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3 pb-3 border-b border-neutral-800">
        <div>
          <h3 className="font-bold text-white text-base">Pixel Watch 4 Installation Guide</h3>
          <p className="text-xs text-neutral-400 mt-0.5">
            Methods for installing your isolated Wear OS complication app onto your Google Pixel Watch.
          </p>
        </div>

        {/* Tab switchers */}
        <div className="flex items-center gap-1 p-1 bg-neutral-950 border border-neutral-800 rounded-lg text-xs">
          <button
            onClick={() => setActiveTab('personal_library')}
            className={`px-3 py-1.5 font-medium rounded-md transition-colors ${
              activeTab === 'personal_library'
                ? 'bg-neutral-800 text-white shadow-sm'
                : 'text-neutral-400 hover:text-neutral-200'
            }`}
          >
            Personal App Library
          </button>
          <button
            onClick={() => setActiveTab('wireless_adb')}
            className={`px-3 py-1.5 font-medium rounded-md transition-colors ${
              activeTab === 'wireless_adb'
                ? 'bg-neutral-800 text-white shadow-sm'
                : 'text-neutral-400 hover:text-neutral-200'
            }`}
          >
            Wireless ADB Sideload
          </button>
          <button
            onClick={() => setActiveTab('wear_installer')}
            className={`px-3 py-1.5 font-medium rounded-md transition-colors ${
              activeTab === 'wear_installer'
                ? 'bg-neutral-800 text-white shadow-sm'
                : 'text-neutral-400 hover:text-neutral-200'
            }`}
          >
            Wear Installer 2 (Phone)
          </button>
          <button
            onClick={() => setActiveTab('watch_face')}
            className={`px-3 py-1.5 font-medium rounded-md transition-colors ${
              activeTab === 'watch_face'
                ? 'bg-neutral-800 text-white shadow-sm'
                : 'text-neutral-400 hover:text-neutral-200'
            }`}
          >
            Add to Watch Face
          </button>
        </div>
      </div>

      {/* TAB 1: PERSONAL APP LIBRARY */}
      {activeTab === 'personal_library' && (
        <div className="space-y-4">
          <div className="p-4 rounded-lg bg-sky-950/20 border border-sky-800/40 text-xs text-sky-200 leading-relaxed">
            <strong className="font-semibold text-white block mb-1">What is the Personal App Library on Pixel Watch?</strong>
            On Wear OS 4 and 5, your Pixel Watch connects to your personal Google account. When you upload an app to your Google Play Console under <strong>Internal Testing</strong> or <strong>Internal App Sharing</strong>, Google Play automatically links it to your watch! It appears right in the watch’s Google Play Store under <em>&quot;Apps on your phone&quot;</em> or <em>&quot;Personal library&quot;</em>.
          </div>

          <div className="grid grid-cols-1 md:grid-cols-4 gap-3 text-xs">
            <div className="bg-neutral-950 border border-neutral-800 p-3.5 rounded-lg space-y-1.5">
              <span className="w-5 h-5 rounded-full bg-neutral-800 text-white flex items-center justify-center font-bold text-[11px]">1</span>
              <strong className="text-white block font-semibold">Build Release Bundle</strong>
              <p className="text-neutral-400">
                In Android Studio, click <strong>Build &gt; Generate Signed Bundle / APK</strong>, choose <strong>Android App Bundle (.aab)</strong>.
              </p>
            </div>

            <div className="bg-neutral-950 border border-neutral-800 p-3.5 rounded-lg space-y-1.5">
              <span className="w-5 h-5 rounded-full bg-neutral-800 text-white flex items-center justify-center font-bold text-[11px]">2</span>
              <strong className="text-white block font-semibold">Play Console Track</strong>
              <p className="text-neutral-400">
                In your Google Play Console, go to <strong>Testing &gt; Internal testing</strong>, create a new release and upload the <code className="text-sky-300">.aab</code> file.
              </p>
            </div>

            <div className="bg-neutral-950 border border-neutral-800 p-3.5 rounded-lg space-y-1.5">
              <span className="w-5 h-5 rounded-full bg-neutral-800 text-white flex items-center justify-center font-bold text-[11px]">3</span>
              <strong className="text-white block font-semibold">Add Your Email</strong>
              <p className="text-neutral-400">
                Under the Testers tab, add your personal email (<code className="text-neutral-300">mohitb117@gmail.com</code>) and accept the tester opt-in link.
              </p>
            </div>

            <div className="bg-neutral-950 border border-neutral-800 p-3.5 rounded-lg space-y-1.5">
              <span className="w-5 h-5 rounded-full bg-neutral-800 text-white flex items-center justify-center font-bold text-[11px]">4</span>
              <strong className="text-white block font-semibold">Install on Watch</strong>
              <p className="text-neutral-400">
                Open <strong>Play Store</strong> on Pixel Watch, scroll down to <strong>Apps on your phone</strong>, and tap <strong>Install</strong> next to MarketWear!
              </p>
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: WIRELESS ADB SIDELOAD */}
      {activeTab === 'wireless_adb' && (
        <div className="space-y-4">
          <p className="text-xs text-neutral-300 leading-relaxed">
            The fastest method if you have a laptop on the same Wi-Fi network. Takes less than 2 minutes and does not require Play Console review.
          </p>

          {/* Interactive Command Generator */}
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

          {/* Generated ADB commands */}
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

      {/* TAB 3: WEAR INSTALLER 2 */}
      {activeTab === 'wear_installer' && (
        <div className="space-y-4 text-xs">
          <p className="text-neutral-300 leading-relaxed">
            If you do not want to use a PC command line, you can use the free app <strong>Wear Installer 2</strong> on your Android phone to install the APK directly to your Pixel Watch over Wi-Fi:
          </p>

          <ol className="space-y-2.5 list-decimal list-inside text-neutral-300">
            <li className="p-3 bg-neutral-950 border border-neutral-800 rounded-lg">
              <strong className="text-white">Install Wear Installer 2</strong> from the Google Play Store on your phone and your Pixel Watch.
            </li>
            <li className="p-3 bg-neutral-950 border border-neutral-800 rounded-lg">
              <strong className="text-white">Download the generated APK</strong> on your phone (or transfer <code className="text-sky-300">app-release.apk</code> to your phone Downloads folder).
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

      {/* TAB 4: ADDING TO WATCH FACE */}
      {activeTab === 'watch_face' && (
        <div className="space-y-3 text-xs">
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
    </div>
  );
};
