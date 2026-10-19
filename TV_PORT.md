# FOSS Browser — Android TV port

This document is the deliverable for the TV adaptation of FOSS Browser.
Scope: make the existing Java/XML browser app usable end-to-end on Android TV
with a D-pad remote (10-foot UI, remote navigation, visible focus, sensible
Back behaviour, overscan-safe layout). **TV-only fork**: the app now declares
itself as an Android TV app and the launcher UI on phones is intentionally
dropped (per project decision).

The user interface stays what it is — browser chrome around a WebView — so the
Leanback UI toolkit is deliberately **not** added (the TV docs call the TV
libraries optional; Leanback targets browse/playback screens). No new
dependency, plugin or library was introduced; `app/build.gradle` is unchanged.

---

## 1. Change list (what changed and why)

### 1.1 `app/src/main/AndroidManifest.xml`

| Change | Justification |
|---|---|
| `<uses-feature android:name="android.software.leanback" android:required="true"/>` | Declares the app as an Android TV app. `required="true"` = TV-only distribution: Google Play only offers the app on Android TV OS (project decision: drop phone support). |
| `<uses-feature android:name="android.hardware.touchscreen" android:required="false"/>` | Mandatory for the app to be *recognized* as a TV app; without it the app does not appear on TV devices / Play-on-TV. |
| `android:banner="@drawable/tv_banner"` on `<application>` | TV home screen launch point. xhdpi, 320×180 px, text included in the image. |
| `<category android:name="android.intent.category.LEANBACK_LAUNCHER"/>` in the MAIN intent filter | Identifies the launcher activity for TV; without it the app is not visible in the TV user interface. |
| `android:screenOrientation="landscape"` on `BrowserActivity` | TV is a fixed 16:9 landscape screen; prevents any orientation churn. |

All four claims above are per the official "Create and run a TV app" guide
(link in §4). Note: that guide now lives at `/training/tv/get-started/create`;
the old `/training/tv/start/start` URL redirects there.

### 1.2 Build configuration

No TV-specific changes. `app/build.gradle` and the Gradle wrapper are
untouched. One compatibility edit was made later: the root `build.gradle`
pins AGP to **9.2.0** (was 9.3.2), because Android Studio 2025.3.4 rejects
AGP 9.3.2 with "incompatible version ... latest supported version is AGP 9.2.0".
Everything else stays as it was (`Gradle 9.5.0`, `compileSdk`/`targetSdk` 37,
`minSdk` 29, Java 17).

A second compatibility pass fixed the Gradle-10 deprecation warning:
`app/build.gradle` now uses `propName = value` assignment (Groovy space
assignment is deprecated) for `namespace`, `shrinkResources` and
`checkReleaseBuilds`, and three inert deprecated AGP options were removed from
`gradle.properties` (`usesSdkInManifest.disallowed`,
`sdk.defaultTargetSdkToCompileSdkIfUnset`,
`defaults.buildfeatures.resvalues` — safe because the manifest has no
`<uses-sdk>`, `targetSdk` is set explicitly and no `resValue` is used). The
remaining four legacy-behaviour AGP options are kept on purpose and commented
in `gradle.properties`.

### 1.3 New resources (all added)

| File | Purpose |
|---|---|
| `res/values/dimens.xml` | Single source of truth for TV sizing: overscan margins (48dp sides, 27dp top/bottom), focus stroke width 2dp, bottom bar 80dp, icon buttons 56dp with 28dp icons, dialog margins, list row height 72dp, remote mouse pointer size 26dp. |
| `res/color/tv_focus_ring.xml` | State list: primary color while focused/selected, transparent otherwise. Used as foreground ring / border color. |
| `res/color/tv_card_stroke.xml` | Card outline state list: primary when focused/selected, outline variant otherwise. Turns card strokes into focus indicators. |
| `res/drawable/tv_focus_ring.xml` | Round focus ring (pills/icon buttons). |
| `res/drawable/tv_focus_ring_rect.xml` | Rectangular focus ring (list rows, checkbox rows). |
| `res/drawable/tv_list_selector.xml` | ListView/GridView row selector: the platform default is far too subtle to read from 10 feet. |
| `res/drawable/tv_cursor.xml` | Remote mouse pointer artwork, crisp target style: dark contrast edge for white pages, white ring for dark pages, theme-coloured centre dot as the click point. |
| `res/drawable-xhdpi/tv_banner.png` | 320×180 banner with the app name in the image (TV launcher requirement). |

### 1.4 Themes — `res/values/themes.xml` and `res/values-night/themes.xml`

- `android:colorPrimary` + `android:windowAllowReturnTransitionOverlap` +
  `android:windowAllowEnterTransitionOverlap` on AppTheme and the three
  wallpaper themes: customizes the launcher's expanding-circle animation color
  (TV-specific requirement; the `SplashScreen` API animations are explicitly
  unsupported on TV).
- New style `Widget.Tv.Button.IconButton` (parent
  `Widget.Material3.Button.IconButton`): adds `android:foreground` focus ring
  so every icon button shows one consistent, clearly visible focus indicator.

### 1.5 Layouts (all TV-adapted in place — no `-television` variants)

| File | Change / justification |
|---|---|
| `layout/activity_main.xml` | Rebuilt browser chrome: a single bottom bar (`appBar` pill + `fab_overview` + `fab_menu` inside the `appBar_buttons` row) inset to the overscan safe area; `fab_showAppBar` removed (the phone app hid the bar and brought it back with a swipe-up — unreachable on a remote; the TV port hides it on an idle timer instead and brings it back when the pointer hovers it, see §1.10); progress bar at the top edge; search-on-site bar sits above the bottom bar; every interactive element focusable with a focus ring; the WebView area stays full-bleed (background content must not be clipped to the safe area). Contains the `tv_cursor` pointer overlay (never clickable/focusable, moved into the window decor at runtime so it stays above a fullscreen video) and `clipChildren`/`clipToPadding="false"` so the circle is not clipped at the screen edges. |
| `layout/dialog_overview.xml` | Overview (tabs/bookmarks/history): overscan insets, focusable bottom navigation entries, list selector on the lists, larger rows/text. |
| `layout/dialog_search.xml` | Search dialog: larger, focusable input and result rows, list selector. |
| `layout/dialog_menu.xml`, `layout/dialog_menu_overflow.xml` | Menus: taller rows, focus rings, 10-foot text sizes. |
| `layout/dialog_fast_toggle.xml` | All 20 `<CheckBox>` rows got a `tv_focus_ring_rect` foreground (each checkbox is its own focus target). The action buttons (`ib_save`, `ib_delete`, `ib_reload`, `ib_settings`, `button_help`) were `<Button app:icon=…>` which **renders an empty button** (AppCompatButton does not read `app:icon`) — converted to `com.google.android.material.button.MaterialButton` and given focus rings. This is a real TV fix: icon-only buttons with no icon are unusable on a remote. |
| `layout/item_list.xml` | Shared row of the tab/bookmark/history/search lists: 72dp tall, bigger title/URL text, card stroke bound to `tv_card_stroke` (`duplicateParentState` so ListView selection colors the card, and the focus ring lights up when the row is focused). |
| `layout/item_menu.xml` | Menu entry rows (RecyclerView): taller, focusable, focus ring. |
| `layout/item_overflow.xml` | Overflow entry rows: taller, focusable, focus ring. |
| `layout/item_checkbox.xml` | Checkbox rows in settings: taller, focus ring. |
| `layout/activity_settings.xml`, `activity_settings_profile_list.xml`, `activity_settings_menu.xml`, `activity_settings_clear.xml`, `activity_settings_backup.xml`, `dialog_custom_searches.xml` | Overscan insets, larger text/targets, list selectors and focus rings on all controls so every settings screen is fully remote-navigable. |

### 1.6 Java

**`java/de/baumann/browser/activity/BrowserActivity.java`** — the central TV logic:

- **Browser bar with idle hiding** (§1.10): removed the `fab_showAppBar` wiring
  and the scroll-driven hide/show animation; `hideToolbar` is still forced to
  `false` in `initOmniBox()` (the phone pref is unused). The bar row
  (`appBar_buttons`) hides itself 15 s after its last use and comes back when
  the remote mouse pointer moves over the bar area (the bottom part of the
  screen). Showing/hiding goes through `showTvAppBar()`/`hideTvAppBar()`, the
  existing `appBar.setVisibility()` call sites were routed through
  `noteTvAppBarUse()`.
- **Start page** (`onCreate`): the overview is no longer shown at launch and is
  no longer used as a home screen. Restored tabs win; if there is nothing to
  restore, the browser opens `https://www.google.com`. External launches
  (`ACTION_VIEW`, share, TV search) skip the Google tab and load the passed
  content. The `start_tab`/`start_tabStart`/`show_overview` preferences are
  therefore ignored at launch (the overview stays reachable through the tabs
  button). This also bypasses the old `dispatchIntent()` fallback that loaded
  `favoriteURL` (the FOSS Browser wiki) when no tab existed.
- **Remote mouse pointer** (`initTvCursor()` and the `tvCursor*` methods): a
  circle drawn over the whole window (`tv_cursor`, 24dp: dark edge, white ring,
  theme-coloured centre dot) that turns the D-pad into a mouse:
  - Up/Down/Left/Right move the pointer while the web page has the focus. While
    a direction is held, a 60 fps timer (`tvCursorMoveRunnable`) moves it
    continuously; the speed grows from 300 dp/s to 1400 dp/s over ~0.7 s
    (mouse acceleration), diagonals are normalised. Short taps move it a few
    pixels, holding glides it across the screen — no key-repeat stepping,
    no 500 ms start delay.
  - The keys are intercepted in `BrowserActivity.dispatchKeyEvent()`, the first
    station of every key event, so the web engine can never swallow the
    directions for page scrolling first (`WebView` sends keys to the page
    before any `OnKeyListener` of the view would run).
  - OK / Enter is a real left click. The generated touch is dispatched to the
    window's decor view, so it works on the page **and** on the browser bar,
    the tabs button and the menu button — everything on screen.
  - tap = click; press and hold = the touch stays down (page long press: text
    selection, link/image menu); hold + move = drag (selection / page drag).
  - While the on-screen keyboard is visible, Enter is passed through to the
    keyboard/page instead of clicking.
  - The pointer is hidden while a dialog is open or the window is in the
    background; a click held while a dialog opens is cancelled, otherwise the
    page would keep a stuck pressed state.
  - **Idle hiding** (§1.10): after 15 s without movement the pointer is set
    `INVISIBLE`; the next direction key or click wakes it at the same position.
    A key press is routed to the pointer even while it is hidden
    (`tvCursorAllowed()`), otherwise it could never wake up again.
  - **Fullscreen videos/pages** (§1.10): while a fullscreen view is shown the
    pointer stays allowed even though the WebView is hidden (`GONE`), it is
    drawn above the fullscreen view and Back leaves fullscreen.
  - Movement also sends a `SOURCE_MOUSE` hover event, so CSS `:hover` on
    desktop-style pages reacts to the pointer.
- **Edge scrolling**: with the pointer in the top band (13% of the screen
  height, at least 80dp) the page scrolls up; from the band above the browser
  bar down to the bottom screen edge it scrolls down. Pushing the pointer to
  the bottom edge is the natural gesture, so the bar itself scrolls as well —
  its buttons keep working (a click does not move them) and scrolling pauses
  while OK is held. Implemented with real mouse-wheel events (`ACTION_SCROLL`
  with `AXIS_VSCROLL`, `SOURCE_MOUSE`, dispatched at the pointer position every
  70 ms): `WebView.scrollBy()` does nothing in current WebView versions because
  Chromium scrolls the page itself — that was the reason edge scrolling
  appeared broken. As with a real wheel, the container under the pointer
  scrolls (inner scroll containers included).
- **Remote substitutes for touch gestures**:
  - `appBar` click = open search; hold OK over it = page menu.
  - `fab_overview` click = overview; hold OK = configured gesture.
  - `fab_menu` click = main menu; hold OK = configured gesture.
  - `attachRemoteGestureKeys(view, prefix)` (direction-key mapping on the two
    FABs) is kept, but with the pointer owning the D-pad the FABs no longer
    receive focus, so it is not reachable in practice; the long-press
    gestures above still work through the pointer.
- **Web view focus**: the WebView keeps the D-pad focus while browsing (the
  activity intercepts the directions for the pointer before the web engine sees
  them, see `dispatchKeyEvent()`). Its focus listener
  shows/hides the pointer; `showAlbum()` and `updateOmniBox()` re-assert the
  focus after tab switches and page loads.
- **Back behaviour**: `KEYCODE_BACK` is handled in `dispatchKeyEvent()` before
  the window and view hierarchy see it, because the WebView (Chromium)
  swallows Back in some situations and the `OnBackPressedCallback` would then
  never run. The callback: search-on-site visible → close it; chrome has focus
  → return focus to the page; page can go back → page history; otherwise show
  the "No previous page." toast and stay. Back never closes the tab and never
  exits the app. Dialogs and the on-screen keyboard are separate windows and
  keep their own Back handling.
- **Dialogs**: dismiss listeners return focus to the web page, so focus never
  vanishes after a dialog closes.
- **Overview**: `initOverview()` makes the Material `BottomNavigationView`
  entries focusable (`FOCUS_AFTER_DESCENDANTS`) — the widget is touch-first
  and its entries are not focusable by default.

**`java/de/baumann/browser/unit/HelperUnit.java`** — `setupDialog()`:
dialogs were anchored to the bottom edge (phone bottom-sheet style) and could
be clipped or hard to read on TV. They are now centered overlays whose window
is sized to screen minus the overscan safe area (48dp sides / 27dp top-bottom).

**`java/de/baumann/browser/view/AdapterTabs.java`** — the tab list is a plain
container (no ListView selection), so each tab row is made focusable itself
(`FOCUS_BLOCK_DESCENDANTS`) and draws the rectangular focus ring; the close
icon is not focusable (closing a tab from a remote works through the row's
long-press menu, and a tiny close target next to a long list is a focus trap).

**Deleted: `java/de/baumann/browser/view/SwipeTouchListener.java`** —
phone-only gesture class, superseded by the remote mouse pointer and the
long-press mappings; no references remain.

### 1.7 Verification already performed

- All 134 XML files under `app/src/main` parse.
- Every project `R.id.*` referenced from Java resolves to a defined id (the
  only unmatched names are framework/library ids: `android.R.id.*`,
  `com.google.android.material.R.id.*`).
- Every `@dimen/tv_*`, `@drawable/tv_*`, `@color/tv_*` and
  `@style/Widget.Tv.*` reference resolves to a definition.
- `SwipeTouchListener`, `fab_showAppBar`, `AnimatorSet`, `ObjectAnimator` have
  no remaining references.
- `./gradlew :app:compileDebugJavaWithJavac` succeeds (AGP 9.2.0, Gradle
  9.5.0, Studio JBR 21), including AAPT2 resource processing of the new
  `tv_cursor` drawable/layout/id.
- `./gradlew :app:assembleDebug` succeeds; the debug APK was installed on the
  "Android TV (1080p)" AVD and started there. Runtime smoke test (adb, real
  D-pad key events, UI dumps):
  - the app starts on `https://www.google.com` (bar title `Google`), no
    overview/start page, no crash;
  - the pointer is visible in the middle of the screen on a fresh start;
  - injected D-pad taps move the pointer (~10 px per tap ≈ one 16 ms frame at
    the start speed) and the page does not scroll — the key interception in
    `dispatchKeyEvent()` works against the real WebView;
  - a click with the pointer over the browser bar opens the search dialog,
    the pointer hides while the dialog is open and reappears at the same
    position after Back (the first Back closes the keyboard, as usual);
  - the crash buffer of the emulator stays empty.

- Second runtime pass (after the fixes in §1.9, same AVD, adb key injection
  and UI dumps/screenshots):
  - `https://www.youtube.com` loads the real site (`m.youtube.com/`, title
    `YouTube`) — no Invidious/Gandalf page;
  - `ACTION_WEB_SEARCH` resolves through the default search engine:
    `https://www.google.com/search?q=…`;
  - a long article (Wikipedia "Europe") scrolls all the way down with the
    pointer parked at the bottom edge (over the bar) and back to the top with
    the pointer in the top band;
  - clicking a search-result link and pressing Back once returns to the search
    page (`canGoBack=true` in the callback);
  - Back on a page without history shows the "No previous page." toast and the
    activity stays resumed (three Back presses in a row, no exit, no crash);
  - the overflow menu and the fast toggle dialog inflate and are D-pad
    navigable; the removed redirects row is gone;
  - the start-up migration drops the stale Gandalf tab (`openTabs` becomes
    `https://www.google.com/`) and removes the old redirect preferences
    (`tv_redirects_cleaned=true`).

- Third runtime pass (after §1.10, same AVD, adb key injection + UI dumps):
  see the list at the end of §1.10 — bar/pointer idle hiding, hover reveal,
  clicking the hidden bar, and the fullscreen fix (cursor visible in a
  fullscreen video, Back exits fullscreen, pointer alive afterwards).

### 1.8 Start-up crash that was fixed

The first build without the start-up overview crashed on launch:

`Unable to start activity ... NullPointerException: Attempt to invoke virtual
method 'void android.widget.ListView.setVisibility(int)' on a null object
reference` at `initOverview()` → `setSelectedTab()` → navigation listener.

Reason: a dialog attaches its content view only when it is shown for the first
time, so `dialogOverview.findViewById(R.id.list_overView)` returned `null`
once the overview was no longer shown during `onCreate` (previously the
`dialogOverview.show()` at start-up had attached the view). The overview dialog
now keeps its inflated layout in the `dialogViewOverview` field like
`dialogViewSearch` already did, and `initOverview()` takes the `ListView` from
there (with a null guard).

### 1.9 "Gandalf" redirects removed, Google as default, working ad blocker

- **Custom redirects removed (the "Gandalf" bug)**: the app had a user-editable
  URL rewriting feature whose defaults sent `youtube.com`/`m.youtube.com` to
  third-party front ends. For YouTube the default target,
  `invidious.nerdvpn.de`, answered with an anti-bot page titled *"Gandalf - You
  shall not pass!"* — it looked as if the browser redirected every YouTube
  visit. Removed: `CustomRedirectsHelper`, `CustomRedirectsDialog`,
  `AdapterCustomRedirect`, `custom_redirects_list.xml`,
  `create_new_redirect.xml`, the redirect row in the fast toggle dialog and the
  `BrowserUnit.redirectURL()` call in `NinjaWebView.loadUrl()`. The separate
  **custom search shortcuts** (search dialog → search a site) are kept and use
  the new `dialog_custom_searches.xml`.
- **One-time start-up migration** (`tv_redirects_cleaned`): removes the
  `customRedirects` / `saved_redirect_ok` preferences and drops restored tabs
  and the saved current URL when they still point at `.gandalf` or
  `invidious.` pages (the user's device had the Gandalf URL stored as the open
  tab, so without it the app would have restored that page forever).
- **Default search engine is Google**: new entry/value `2` in both
  `search_engine_*` arrays (Google first, also the label default in
  `preference_setting.xml`), and `BrowserUnit.queryWrapper()` defaults to
  `https://www.google.com/search?q=`. The Startpage values (0/1) stay for users
  who already saved them.
- **Ad blocker really works, also offline**: the per-profile ad block switch
  was already on by default, but `AdBlock` opened `assets/hosts.txt`, which did
  not exist — no host list was loaded until a background download succeeded.
  The stripped StevenBlack host list is now bundled as `assets/hosts.txt`
  (~1.7 MB), and `AdBlock.loadHosts()` strips the `0.0.0.0 `/`127.0.0.1 `
  prefixes of that file format. `BannerBlock` no longer throws on a malformed
  cookie-banner rules file (it logs and disables itself) and the pointless
  "Downloading cookie-banner-rules." toast at start-up is gone.

### 1.10 Idle hiding for bar + pointer, and working fullscreen

Two related user asks: *"the bar goes idle after 15 seconds of not using it, hover
over it to un-idle it (idle = invisible)"*, *"add fullscreen so you can go
fullscreen"*, *"after I went into a video the cursor disappeared and never
appeared"*, *"cursor also goes idle after 15 s of not moving it, un-idle once you
move it"*. The bugs found while investigating (all fixed):

1. **Fullscreen could not be left with the remote.** Back only logged
   `FOSS Browser in fullscreen mode` in the `OnBackPressedCallback` and in the
   `performGesture("03")` branch, so a fullscreen video trapped the user.
2. **The pointer died in fullscreen and never came back.** `onShowCustomView()`
   sets the WebView `GONE` (it hides `currentAlbumController`), so
   `ninjaWebView.hasFocus()` became false → `updateTvCursorVisibility()` hid the
   pointer, and the pointer keys are only routed while it is visible → the whole
   D-pad was dead. `ninjaWebView.hasFocus()` also gated the movement loop
   (`tvCursorMoveRunnable`), so the pointer could not even be moved.
3. **The pointer was drawn behind the fullscreen view.** `tv_cursor` lived in
   `R.id.main`, the fullscreen holder is added to the window decor, so the video
   covered the circle.
4. **The cursor never returned after leaving fullscreen.** `onHideCustomView()`
   ended with `contentFrame.requestFocus()` — the container, not the WebView —
   so the WebView never regained focus and `updateTvCursorVisibility()` kept the
   pointer hidden forever (exactly the reported bug).
5. **The page was never told that fullscreen ended.** The
   `WebChromeClient.CustomViewCallback` was never stored or invoked, so the page
   stayed in its fullscreen state (stretched/frozen player).
6. **`onHideCustomView()` crashed on a second call** (`customView` was already
   `null`: `setKeepScreenOn` NPE), and both fullscreen methods cast
   `currentAlbumController` unguarded. The engine calls `onHideCustomView()`
   again after `onCustomViewHidden()`, so this was reachable.
7. **Only the two-argument `onShowCustomView` was overridden.**
   `NinjaWebChromeClient` now also forwards
   `onShowCustomView(View, int requestedOrientation, CustomViewCallback)` (the
   variant Chromium uses for video/Fullscreen-API requests on modern WebView
   versions); `super` is deliberately not called to avoid handing the same view
   to the handler twice.

Fixes / new behaviour:

- `onShowCustomView()`: stores the callback, cancels a held click, keeps the
  pointer allowed (see `tvCursorAllowed()`) and on top
  (`decorView.bringChildToFront(tvCursor)`), wakes the pointer and refreshes the
  edge scrolling.
- `onHideCustomView()`: idempotent (returns when there is nothing to hide),
  null-guarded, restores the previous album view, requests the focus for the
  WebView and wakes pointer + bar.
- `exitFullscreen()`: invokes the stored `onCustomViewHidden()` callback first
  and then hides the view; used by Back, by the `03` gesture mapping and by
  `VideoCompletionListener`.
- **Bar idle**: `tvIdleRunnable` (250 ms tick) hides the whole `appBar_buttons`
  row 15 s after its last use (`TV_IDLE_TIMEOUT_MS`), unless the pointer hovers
  it, the chrome has focus, the search-on-site bar is up or a fullscreen view
  covers it. The bar is *not* hidden while a press is held. It comes back on
  hover: the pointer movement calls `noteTvPointerActivity()` →
  `noteTvAppBarUse()`; the hover area is the bar row itself (small margin on top
  and sides, bottom bound = bar row bottom, see §1.11). A click while the bar
  is hidden reveals it *before* the touch is dispatched, so OK on a hidden bar
  hits the bar and not the page.
- **Pointer idle**: same timer; the pointer is set `INVISIBLE` 15 s after the
  last movement/click unless a press is held. The D-pad still reaches it while
  hidden, a direction key or OK wakes it at its last position. Keys keep their
  normal behaviour while the pointer is hidden (a click is still a click).
- Removed two harmless but noisy start-up NPEs the log showed (`
  updateOmniBox()` touching the static `ninjaWebView` before the first tab
  exists, and `hideSearch()` cancelling the never-created custom-search dialog).

Verified on the "Android TV (1080p)" API 29 AVD (adb, real key events, UI
dumps + app log):

- 3 s after start the bar row and the pointer are both visible; after 15 s idle
  both are invisible (they are gone from the accessibility tree, which only
  lists visible views).
- `DPAD_DOWN` wakes the pointer (bar stays hidden — it only answers to hover),
  pushing the pointer into the bottom band reveals the bar, and a click there
  opens the search dialog, i.e. the click landed on the bar.
- Fullscreen video (`mdn.github.io/dom-examples/fullscreen-api/`, click on the
  player's *enter full screen* control): `onShowCustomView: fullscreen view
  shown`, the pointer stays visible in fullscreen (a11y dump) even though the
  WebView lost the focus, Back logs `onHideCustomView: leaving fullscreen`, the
  page is back, bar and pointer return, the pointer still moves after leaving
  fullscreen and the engine's follow-up call only logs `nothing to hide`
  (idempotent guard).

### 1.11 Edge scrolling fixed - only the very bottom screen edge scrolls

User ask: *"fix the scrolling sensitivity: it scrolls when you don't touch the
bottom, it should scroll when you touch directly the bottom and it should not
scroll when you hover over the browser bar / other buttons"*. Found bugs:

1. **The bottom scroll band started far above the bar.** It spanned
   `barTop − zone` (zone = max(80 dp, 13 % of the height) ≈ 140 px on 1080p)
   down to the screen edge, so the page scrolled while the pointer was still
   over page content.
2. **Hovering the browser bar scrolled the page.** Because the band reached the
   top of the bar row, resting on the bar or on the FAB buttons started the
   70 ms scroll loop (the old code comment called hovering-to-scroll intended;
   with a remote it makes the buttons effectively unusable).
3. **An idle pointer at an edge scrolled forever** (latent): the scroll runnable
   re-posts itself while the pointer sits in a band; when the pointer idled out
   after 15 s the page kept scrolling with an invisible pointer.

Fix (`tvCursorScrollStep()`, `isTvCursorOnAppBar()`, `tvIdleRunnable`):

- The bottom band is now the free strip **below the browser bar row** (its top
  is `barBottom`, normally the overscan strip under the bar; without a laid out
  bar a 64 dp / 6 % band at the screen edge is used). `max(fallback, barBottom)`
  guarantees the band can never reach into the bar or the buttons.
- `isTvCursorOnAppBar()` is now bounded by the bar row on all sides: the strip
  below the bar is no longer part of the bar, so pushing there to scroll neither
  wakes the hidden bar nor keeps it awake. Hovering the bar itself still does.
- The idle handler stops the edge scroll when it hides the pointer
  (`tvCursorScrollDelta = 0` + `removeCallbacks`), so nothing scrolls
  unattended.

Verified on the 1080p API 29 AVD (screen 1920×1080, bar row y 866–1026, strip
y 1026–1080, scroll steps observed through a temporary `Log.v`, later removed):

- Pointer pushed down through the bar: the bar wakes while the pointer crosses
  it and *no* scroll starts (no step change logged in the bar region).
- The first step starts exactly below the bar bottom (`scroll 0->44 y=1034`).
- Leaving the strip upwards stops it immediately (`scroll 44->0 y=1024`).
- Resting in the strip: after 15 s idle the scroll stops and pointer + bar both
  go invisible; one `DPAD_DOWN` wakes the pointer and the scroll resumes.
- Hovering the hidden bar (pointer moved onto it) still reveals it.

---

## 2. Build and run on a TV emulator / device

The app compiles and runs like any Android app; nothing special is required.

### 2.1 Android TV emulator (Android Studio)

1. **SDK**: SDK Manager → *SDK Platforms*: Android API 37 (needed for
   `compileSdk 37`); *SDK Tools*: Android Emulator + hypervisor driver if not
   installed. For the emulator image: *SDK Platforms* → any Android TV OS
   system image with API ≥ 29 (`minSdk 29`) — pick the newest TV image
   available on your machine.
2. **Create the device**: Device Manager → *Create Device* → category
   **TV** → e.g. "Android TV (1080p)" or "Google TV (1080p)" → select the TV
   system image → Finish. (This is the documented AVD flow for TV devices.)
3. **Run**: select the `app` configuration, pick the TV AVD, press Run.
4. **Control it**: the TV emulator shows an on-screen remote control panel
   (D-pad + OK + Back). Keyboard arrow keys and Enter/Escape also map to
   D-pad/OK/Back, and a long key press maps to a long press. In this port the
   D-pad moves the on-screen pointer and Enter/OK clicks, so the emulator
   controls the pointer exactly like a real remote; the emulator's own mouse
   remains usable for touch-style testing.

### 2.2 Physical Android TV / Google TV device

1. On the device: *Settings → Device → About → Build* (click ~7×) to enable
   developer mode, then *Developer options → USB debugging* (or network
   debugging on newer devices).
2. Connect via USB or `adb connect <ip>:5555`, then run from Android Studio
   (device appears in the deployment target list) or `adb install` the APK.
3. The app appears in the TV home screen's apps row with its banner
   (`tv_banner.png`) and launches from there — that is the LEANBACK_LAUNCHER
   entry point.

Note: installing by hand (`adb install`) also requires the manifest flags
above anyway; on TV devices the app simply will not show up in the launcher
without `LEANBACK_LAUNCHER`.

---

## 3. Manual test checklist (D-pad / remote)

Run through this list on the emulator or a real TV; every item is manual
(it is the project's agreed substitute for automated tests).

**Launch & home screen**
- [ ] App appears in the TV launcher apps row with the banner (icon/text visible, not blank).
- [ ] Launching shows the expanding-circle animation in the theme color.

**Startup state**
- [ ] Launching the app shows no overview/start page: google.com loads directly (or the restored tabs, if tab restore is enabled).
- [ ] The tabs button (bottom bar) opens the overview dialog centered (not glued to the bottom edge) with the whole dialog inside the screen edges.
- [ ] D-pad can reach every bottom-navigation entry (Tabs / Bookmarks / History) and every list row; the focused element shows a clearly visible ring/stroke from ~3 m.
- [ ] Overview lists scroll with Up/Down; Left/Right do not get stuck.
- [ ] Long press (D-pad center held) on a list row opens the overflow menu; every entry is reachable and shows focus.
- [ ] Selecting a bookmark/history entry loads the page and closes the overview.

**Browser bar**
- [ ] The bar hides itself 15 s after its last use (it becomes invisible; the WebView below stays usable) and comes back as soon as the pointer moves into the bottom area of the screen (from the top of the bar row down to the bottom edge). While the pointer rests on the bar the bar stays visible; while a dialog is open or the search-on-site bar is up it is not hidden.
- [ ] Clicking a bar button that is currently hidden works: the bar appears with the first key/click and the click lands on the bar (not on the page).
- [ ] Move the pointer onto the bar and click: the search dialog opens with the soft keyboard; type a URL/query, confirm → page loads. Back cancels the dialog without leaving the app.
- [ ] Hold OK over the bar: the page menu opens (share, reload, desktop mode, …); every entry is reachable.
- [ ] Pointer over the tabs button → click opens the overview; pointer over the menu button → click opens the menu.

**Remote mouse pointer (D-pad mouse)**
- [ ] At startup (google.com loaded) the pointer appears in the middle of the screen as a ring with a dot; it is drawn above page content and is not clipped at the screen edges.
- [ ] The D-pad moves the pointer and nothing else: the page does not scroll and the focus does not jump while the pointer is visible (the keys are intercepted before the web engine, see §1.6).
- [ ] Up/Down/Left/Right move the pointer smoothly (no key-repeat stepping); holding a direction accelerates it, short taps make small precise steps, diagonals are not faster than straight movement.
- [ ] The pointer stops at the screen edges and is never clipped by the app.
- [ ] Click a link on a web page → it opens.
- [ ] Click a button / search field inside a web page (e.g. a search engine box) → it reacts; a text field opens the on-screen keyboard and typing goes into the field.
- [ ] Hold OK over a paragraph → long press fires (text selection or the page's context menu); releasing ends it.
- [ ] Hold OK and move the pointer → drag (selection extends / the page drags) instead of a click.
- [ ] Pointer close to the bottom edge (from the band above the bar down to the screen edge, including over the bar) → the page scrolls down continuously; pointer in the top band → the page scrolls up; moving the pointer out of the bands stops it; holding OK pauses it.
- [ ] The pointer is hidden while a dialog is open and reappears after the dialog closes; a click held while a dialog opens does not stay stuck (the next click works normally).
- [ ] With the on-screen keyboard visible, Enter confirms the text instead of clicking at the pointer.
- [ ] Desktop-style page with hover menus: hover reacts to the pointer (best effort, see limitations).

**Back button semantics**
- [ ] While the pointer is over the browser bar, Back still navigates the page history (the pointer does not swallow Back).
- [ ] From the page, Back navigates page history — one press per entry, also directly after clicking a link; on a page without history it shows "No previous page." and the app stays open. Back never closes the tab and never exits the app.
- [ ] Back closes any open dialog/menu first; focus afterwards is back on the page or the previous dialog.

**Gestures (former swipes)**
- [ ] Settings → Gestures: assign an action to a *long press* of the tabs button or the menu button, then hold OK over that button: the action runs.
- [ ] The two corner buttons no longer take D-pad focus, so direction-key gesture mappings are not reachable (documented limitation); the long-press mappings above are.

**Overview dialog**
- [ ] Tabs tab: each open tab row is focusable with focus ring; selecting switches to that tab; long press opens the tab menu (close, etc.).
- [ ] Badge on the Tabs entry shows the tab count (badge is not clipped).
- [ ] Bookmarks/History: rows focusable, select loads, long press opens overflow.

**Fast toggle dialog** (menu → fast toggles, per the app's entry point)
- [ ] Every checkbox row is reachable with the D-pad, shows a focus ring, and toggling takes effect (JavaScript, pop-ups, location, camera, images, cookies, …).
- [ ] The icon buttons (save profile, delete, reload, settings, help) are visible (**icons rendered**, not empty buttons) and focusable; acting on them works.
- [ ] Profile button: click shows info, long press resets to the standard profile.

**Settings screens**
- [ ] All settings screens open from the menu, every switch/checkbox/entry is reachable with the D-pad, shows focus, and can be toggled.
- [ ] Profile list: entries focusable; opening a profile and editing works.
- [ ] Backup/Clear screens: every button is reachable and acts.
- [ ] Back from each settings screen returns to the browser with focus restored (no lost focus).

**Text input**
- [ ] Search dialog and search-on-site input: focus the text field, the on-screen keyboard appears, typed text goes into the field, confirming performs the search.
- [ ] Search-on-site: enable it via the menu, the bar appears above the bottom bar; its input and close button are focusable and work; Back closes it.

**Media & special pages**
- [ ] After 15 s without movement the pointer becomes invisible by itself; the next D-pad direction or OK wakes it at the same position (moving it, or clicking there when it was over a link). While OK is held it does not idle-hide.
- [ ] Video playback: the fullscreen control of the player works; in fullscreen the pointer is still visible/movable over the video and Back leaves fullscreen (the page returns, bar visible again); the pointer still works after leaving fullscreen.
- [ ] A video that ends by itself leaves fullscreen (page not stuck in its fullscreen layout).
- [ ] A long article: pointer in the top band scrolls up, near the bottom edge scrolls down; holding OK and moving the pointer drags the page.

**Overscan**
- [ ] On a TV that still crops edges (or with overscan simulation), no key element (bar, buttons, dialog content) is cut off; page content may run under the edges by design.

**Regression sanity**
- [ ] Tabs: open, switch, close works with the remote only.
- [ ] Downloads still notify (snackbar) and the "open downloads" action is reachable.
- [ ] Incognito profile behaves as before.

---

## 4. Sources (Android Developers)

Manifest, banner, launcher and TV-project requirements:
- **Create and run a TV app** — https://developer.android.com/training/tv/get-started/create
  (previously `/training/tv/start/start`)
  - `CATEGORY_LEANBACK_LAUNCHER` is what makes the app visible in the TV UI.
  - `android.software.leanback` with `required="true"` → Play only offers the app on Android TV OS.
  - `android.hardware.touchscreen required="false"` is required for the app to be considered a TV app.
  - Banner: xhdpi, 320×180 px, text in the image, one per language.
  - Launcher circle animation color via `android:colorPrimary` +
    `windowAllow/ReturnTransitionOverlap` (SplashScreen animations are not
    supported on TV).
  - TV libraries (Leanback etc.) are **optional** — basis for not adding one.
  - AVD flow for TV emulators.
- **TV app quality checklist** (for publication follow-up) — https://developer.android.com/distribute/essentials/quality/tv

Layout / 10-foot sizing:
- **Layouts (TV design guide)** — https://developer.android.com/design/ui/tv/guides/styles/layouts
  - 16:9 screens, design basis 960×540 dp at MDPI.
  - Overscan: keep primary elements inside a 5% margin — the "Ensure
    visibility" principle states 48dp left/right and 27dp top/bottom. (Note:
    the page's later "Overscan margins" paragraph says 58dp/28dp; the
    implementation uses the smaller, principle-section values, which is
    consistent with `960 × 5% = 48dp` / `540 × 5% = 27dp`. Most modern TVs no
    longer have overscan issues.)
  - Do not clip background content to the safe area (the WebView stays
    full-bleed).
  - "Center overlay" template = centered modal dialogs, which the converted
    dialogs use.

Focus:
- **Focus system (TV design guide)** — https://developer.android.com/design/ui/tv/guides/styles/focus-system
  - D-pad = arrows to move, center to select, back to return.
  - Only one focused element at a time; a focused element must be clearly
    visualized (scale, border/outline, glow, color) — implemented as the
    primary-colored focus ring and card stroke.

Redirect note: `/training/tv/start/layouts` no longer exists as an own page and
redirects to the Compose-for-TV layout content; it is not relied on here.

Statement status: all framework/manifest claims above are taken verbatim from
the linked pages. Everything about the app's own controls (which button opens
which dialog, what Back does at each level, how gesture codes map to D-pad
directions) is **project-code inference from the existing implementation**,
documented as such in the code comments.

---

## 5. Known limitations & follow-ups

1. **Localized banners**: only the default banner (`drawable-xhdpi/tv_banner.png`,
   English text) exists. The TV docs require a banner with translated text for
   each supported language; the app ships many locale folders where
   `app_name`/icon text differs. Follow-up: generate banner variants into
   `drawable-<locale>-xhdpi/` for each supported language. (Documented, not
   done.)
2. **The pointer is a touch, not a real mouse**: clicks/drags are dispatched as
   `MotionEvent` touches to the window, so pages see touch semantics (mobile
   site behaviour). Hover is best effort: a `SOURCE_MOUSE` hover event is sent
   on every pointer move, but whether CSS `:hover` / desktop hover menus react
   depends on the WebView version. Real mouse APIs are not available to apps
   on Android TV.
3. **Dialogs stay D-pad-only**: alert dialogs are separate windows, so the
   pointer cannot draw over them or click their buttons (and should not — the
   port's dialogs are already fully remote-navigable). This is intended.
4. **Long-press and drag need a remote that reports OK as a held key**: the
   pointer presses on key-down and releases on key-up, which emulators do.
   Some real remotes only send a short press; verify on the target device and
   note the model if long press/drag does not work. (The Android TV remote
   app and Bluetooth remotes normally send proper down/up pairs.)
5. **Direction-key gesture mappings are unreachable**: with the pointer owning
   the D-pad, the two FABs no longer receive focus, so
   `attachRemoteGestureKeys()` cannot fire. The long-press gesture mappings of
   both buttons still work (hold OK over the button).
6. **Edge scrolling is a synthetic mouse wheel**: the wheel event is
   dispatched at the pointer position, so Chromium scrolls the container under
   the pointer (inner scroll containers included). Sites with custom JS
   scrolling or virtualised lists can still react differently; holding OK and
   moving the pointer drags the page as a fallback. The bottom band reaches the
   screen edge, so resting the pointer on the bar keeps scrolling — move the
   pointer up to stop.
7. **`hideToolbar` preference is ignored**: the bar is never hidden by
   scrolling; it only follows its 15 s idle rule (§1.10). Any leftover setting
   that mentions hiding the toolbar has no effect on TV.
8. **No Leanback dependency**: intentional. If a future feature needs TV
   browse/playback surfaces, adding `androidx.leanback` would be the point to
   re-evaluate.
9. **Checkbox focus target size**: checkbox rows in `dialog_fast_toggle.xml`
   and `item_checkbox.xml` are individual focus targets sized by their text;
   the checkboxes have a focus ring and are individually reachable, but if a
   tester reports hard-to-see focus at 3 m, the ring stroke can be raised via
   `tv_focus_stroke_width` in `res/values/dimens.xml` (single constant).
10. **Start-page preferences are ignored at launch**: `start_tab`,
    `start_tabStart` and `show_overview` no longer decide what happens on
    launch (launch = restored tabs or `https://www.google.com`). They still
    control which tab the overview opens on when it is opened manually through
    the tabs button.
