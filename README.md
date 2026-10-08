<p align="center">
  <img src="GkTeLogo.png" width="180" alt="GkTeLogo">
</p>

<h1 align="center">&#x1D5DA;&#x1D5F8;&#x1D601;&#x1D5F2;&#x1D5E7;&#x1D5FC;&#x1D5F8;</h1>

GkteTok is a customization module for a popular video app. It fine-tunes feed behaviour, media saving, link handling and the interface, directly on your device. All settings open inside the app itself — there is no separate companion app.

Module version: **GkteTok-1.6** (`versionCode 52`).

> ⚠️ Customising an app outside its official behaviour may carry risks, including account restrictions. Use at your own responsibility.

## What it adjusts

**Feed**
- Content filters for the main feed, launch screen and video lists
- Optional hiding of live streams and suggested-acquaintance items
- Seek bar on every video, including short ones

**Comments & profile**
- Extended comment and sharing tools
- Profile layout options, including a full-width banner
- Various interface experiments that are not rolled out to every account

**Media saving**
- Extra save options, including a watermark-free variant where available
- Audio/video choice, quality option, date-prefixed file names, custom folder
- Clean share links: tracking parameters (`_r`, `_t`, `u_code`, etc.) are removed
- Screenshots and screen recording left unobstructed

**Extras**
- Region preference with ~190 countries
- Optional streak keeper (off by default)
- AMOLED theme, accent colour, custom logo/font, branded splash screen
- Settings backup, profiles and a one-file diagnostic report

## Start

1. Install [JDK 21](https://adoptium.net/temurin/releases/?version=21).
2. Download `lspatch*.jar` from the LSPatch releases page and put it in the repo root.
3. Put a clean (never modified) APK of the target app in the repo root.
4. Run `Start-Patch.bat`. Output: `GkteTok-1.6-lspatched.apk`.
5. On the phone: remove any previous modified build, install the new APK and sign in as usual.

Alternative (module only, for rooted/LSPosed setups):

```shell
cd module-baseline
./gradlew testDebugUnitTest assembleRelease
```

Requires JDK 17 and Android SDK 36. Enable the resulting APK as a module and restart the target app.

## Where are the settings

Open the target app → Profile → ☰ → Settings and privacy → **GkTe Tool** (the very first row). The screen follows the app's theme and language (12 languages).

Some options apply only after the app restarts; the settings screen shows a button for that.

## Verification

```shell
bash dev/verify.sh            # compile + 111 unit tests (JDK/android.jar downloaded automatically)
bash dev/check_fixes_sync.sh  # checks that the fixes/ copy is up to date
```

CI (`.github/workflows/android.yml`) runs the same steps on every push and uploads debug/release APKs as artifacts.

## Version

- Module `versionName = "GkteTok-1.6 Release"`, `versionCode = 52` (`module-baseline/app/build.gradle.kts`).
- `Start-Patch.bat` validates the source by the `GKTETOK-V11` code tag and the `GkteTok-1.6` version string; if you change them, update the script too.

## Planned work

See [TODO.md](TODO.md).

## License

Apache License 2.0 — see [LICENSE](LICENSE).

---

<p align="center"><sub>Created by SametGkTe's GkTe Archive Tool</sub></p>
