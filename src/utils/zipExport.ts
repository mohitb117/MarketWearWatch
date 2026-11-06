import JSZip from 'jszip';
import { WEAR_OS_PROJECT_FILES } from '../data/wearProjectFiles';

export async function generateWearOsProjectZip(): Promise<Blob> {
  const zip = new JSZip();
  const root = zip.folder('MarketWear-PixelWatch');

  if (!root) {
    throw new Error('Failed to initialize zip archive');
  }

  // Add all Android Studio project files
  for (const file of WEAR_OS_PROJECT_FILES) {
    root.file(file.path, file.content);
  }

  // Add root gitignore for Android
  root.file('.gitignore', `*.iml
.gradle
/local.properties
/.idea/caches
/.idea/libraries
/.idea/modules.xml
/.idea/workspace.xml
.DS_Store
/build
/captures
.externalNativeBuild
.cxx
local.properties
`);

  // Add standard Gradle wrapper files or scripts
  root.file('gradlew', `#!/usr/bin/env sh
# Standard Android Studio Gradle wrapper script
exec ./gradle/wrapper/gradlew "$@"
`);

  return await zip.generateAsync({ type: 'blob' });
}

export function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}
