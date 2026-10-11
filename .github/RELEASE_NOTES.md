# RaveX

## KickX Launcher v1.2.1 (Windows), built October 10, 2026

Fresh build from October 10, 2026. Settings show it as "Build: 20261010".

All the improvements below are for the Windows launcher only. The Linux launcher is a separate build: its last version is 1.3 and it is no longer developed.

Launcher
- Mod search is much faster now: the full mod list appears instantly, icons stream in after it in parallel and failed downloads are retried automatically
- Mod icons work again: Modrinth switched icons to WEBP and the launcher now decodes that format (libwebp)
- New "What's New" button: opens the changelog of the latest releases with a direct link to the GitHub release page
- Settings now show the real launcher build date and version instead of the mod version
- The launcher can no longer offer an update to a mod version by mistake, only genuine launcher builds pass the check
- Optional self-update: the launcher checks GitHub releases, downloads the setup and verifies its SHA-256 before running it
- Splash screen no longer flickers, themes apply live without restarting the launcher
- Fixed the Ping button message and the Worlds tab icon

Themes
- Two new themes: Windows 95 and Windows Vista / 7, each with its own icon set, classic buttons and Vista gloss effects
- The setup wizard can pick colors for the Custom theme and they are shared with the launcher settings
- Win95 and Vista themes ship themed logos and icons, Dark and Light themes are unchanged

Languages
- Full localization: every interface string is now translated into all 22 languages
- Added Hungarian with its flag
- Before this update half of the interface silently fell back to English on non English / Russian languages

Setup and release
- Setup wizard polish: no more label flicker, cleaner page transitions, disk icon next to the install path, autostart option
- Releases now ship SHA256SUMS.txt and VirusTotal links for every file
- Releases include kickx-version.txt which powers the launcher self-update channel
- Launcher archives are named after the launcher version
- Launcher loads on Windows 7 SP1 x64 and is fully compatible with Windows 10 / 11

RaveX client is unchanged since 1.4.9. The Linux launcher ended at its final version 1.3 and will not receive further updates. All the work this time went into the Windows launcher.

KickX Launcher v1.2.1 for Windows (build 20261010)
