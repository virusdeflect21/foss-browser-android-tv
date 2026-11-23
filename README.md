# FOSS Browser — Android TV port

**Unofficial Android TV fork of [FOSS Browser](https://codeberg.org/Gaukler_Faun/FOSS_Browser)**,
the privacy friendly web browser that renders pages with the Android WebView. This fork
makes the whole app usable with a TV remote: a small on-screen mouse pointer is driven by
the D-pad, so links, buttons, menus, keyboards and dialogs work like on a desktop — no
touchscreen needed.

| | |
|---|---|
| Upstream project | [FOSS Browser](https://codeberg.org/Gaukler_Faun/FOSS_Browser) by Gaukler Faun, licensed AGPL-3.0-or-later |
| This fork | TV-only port — the phone launcher UI is intentionally dropped |
| License | GNU AGPL-3.0-or-later ([LICENSE](LICENSE), notes and library credits in [LICENSE.md](LICENSE.md)) |
| Minimum | Android TV 10 (API 29) |
| Detailed change list | [TV_PORT.md](TV_PORT.md) |

## Features

Everything from upstream FOSS Browser (tabs, bookmarks, history, adblock, per-domain
settings, backups, …) plus the TV adaptation:

- **Remote mouse pointer** — the D-pad moves a mouse cursor over the screen,
  OK/center is a real left click:
  - tap = click at the pointer position,
  - a stationary hold through the long-press timeout = the current page's actions
    menu (overflow) opens; the synthetic click is cancelled, so releasing does not
    click through,
  - hold and move = drag (starting to move before the long-press timeout cancels
    the menu timer),
  - pages receive real mouse hover events.
- **Edge scrolling** — push the pointer to the very top of the screen, or to the free
  strip at the very bottom below the browser bar, to scroll the page. The browser bar
  and its buttons never scroll.
- **Idle hiding** — the browser bar and the pointer become invisible after 15 seconds
  without use. Hovering the bar brings it back, any pointer movement (or D-pad press)
  wakes the pointer. Nothing hides while a click is held or the bar is hovered.
- **Fullscreen video** — fullscreen players work; Back leaves fullscreen and the
  pointer comes back with the page.
- **Back never leaves the app** — Back first exits fullscreen, otherwise it navigates
  the browsing history.
- **Remote friendly UI** — 10-foot layout, focus rings on every control, overscan safe
  margins, all dialogs navigable with the D-pad.
- **Privacy as upstream** — built-in offline adblock (bundled hosts list), search
  engine selectable in the settings (Google is the default of this fork), no tracking,
  no analytics.

## Install

Grab the APK from the [releases](../../releases) page and sideload it:

```
adb install foss-browser-tv-v24-debug.apk
```

or copy the APK to the TV and open it with any file manager / APK installer. The app
then shows up on the Android TV home screen (Leanback launcher, banner attached).

The attached APK is a **debug build** (signed with the standard Android debug key), so
it can be installed on any device with "unknown sources" enabled. It is TV-only by
design: the manifest requires `android.software.leanback` and does not require a
touchscreen, so phones will not offer it.

## Build

Requirements: Android Studio (with its bundled JDK) or JDK 17+, Android SDK 37.

```
./gradlew :app:assembleDebug
```

Once the dependencies are cached an offline build works too:
`./gradlew :app:assembleDebug --offline`. The build produces
`app/build/outputs/apk/debug/app-debug.apk`. `compileSdk`/`targetSdk` 37, `minSdk` 29;
the TV port added no new dependency (`app/build.gradle` is unchanged apart from
upstream).

## Changes compared to upstream

This is a **modified version** of FOSS Browser. All modifications were made in
October 2026 and are published under the same license as the original work
(GNU AGPL-3.0-or-later). The complete change list with reasons, verification notes and
the manual TV test checklist is in [TV_PORT.md](TV_PORT.md). Summary:

- `AndroidManifest.xml` — declares an Android TV app (`android.software.leanback`
  required, `android.hardware.touchscreen` not required), adds the
  `LEANBACK_LAUNCHER` category and the TV banner, locks the activity to landscape.
- TV theme, layouts, drawables — 10-foot UI with focus highlighting, overscan safe
  margins, larger controls, remote friendly dialogs.
- `BrowserActivity` — remote mouse pointer (move / click / hold / drag / hover), edge
  scrolling with real mouse wheel events, idle hiding of the bar and the pointer,
  fullscreen fixes (custom view callback handling, focus return, Back), Back =
  browser history, Google as the default search engine, all dialogs remote navigable.
- "Gandalf" redirect/interstitial pages removed, including a one-time settings
  migration for existing installations.
- AdBlock uses a bundled offline hosts list (`app/src/main/assets/hosts.txt`).
- Removed the touch-only swipe handling and three noisy startup error logs.
- Root `build.gradle` pins AGP 9.2.0 (Android Studio 2025.3.4 compatibility); the app
  module build file is unchanged.

## License

FOSS Browser is licensed under the **GNU Affero General Public License v3.0 or later**.
This fork keeps that license; the full text is in [LICENSE](LICENSE).

- Original work: Copyright © the FOSS Browser project — Gaukler Faun and contributors,
  <https://codeberg.org/Gaukler_Faun/FOSS_Browser>
- Modifications of this fork: October 2026, documented in this README and in
  [TV_PORT.md](TV_PORT.md)

The AGPL is a strong copyleft license: any redistribution of this app, or of a version
derived from it, must include the complete corresponding source code of that version
under the same license. The source of this fork is this repository — every release tag
also carries an automatically generated "Source code (zip / tar.gz)" archive.

## Credits

- [FOSS Browser](https://codeberg.org/Gaukler_Faun/FOSS_Browser) — the upstream browser
  this fork is based on (Gaukler Faun and contributors)
- [Material Design Icons](https://fonts.google.com/icons) — icon set used by the
  upstream app
