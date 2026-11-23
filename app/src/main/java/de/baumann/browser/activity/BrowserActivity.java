package de.baumann.browser.activity;

import static android.content.ContentValues.TAG;
import static android.os.Build.VERSION.SDK_INT;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Outline;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;

import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.webkit.WebViewFeature;
import androidx.activity.OnBackPressedCallback;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import de.baumann.browser.R;
import de.baumann.browser.browser.AlbumController;
import de.baumann.browser.browser.BannerBlock;
import de.baumann.browser.browser.BrowserContainer;
import de.baumann.browser.browser.BrowserController;
import de.baumann.browser.browser.List_standard;
import de.baumann.browser.browser.NinjaDownloadListener;
import de.baumann.browser.database.FaviconHelper;
import de.baumann.browser.database.Record;
import de.baumann.browser.database.RecordAction;
import de.baumann.browser.objects.CustomRedirect;
import de.baumann.browser.objects.CustomSearchesHelper;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.DeleteUtils;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.unit.PermissionCallback;
import de.baumann.browser.unit.PermissionUtils;
import de.baumann.browser.unit.RecordUnit;
import de.baumann.browser.view.AdapterCustomSearches;
import de.baumann.browser.view.AdapterMenu;
import de.baumann.browser.view.AdapterSearch;
import de.baumann.browser.view.GridAdapter;
import de.baumann.browser.view.GridItem;
import de.baumann.browser.view.MenuItem;
import de.baumann.browser.view.NinjaToast;
import de.baumann.browser.view.NinjaWebView;
import de.baumann.browser.view.AdapterRecord;

public class BrowserActivity extends AppCompatActivity implements BrowserController {

    private PermissionCallback activeCallback;

    private final ActivityResultLauncher<String> locationLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> handleSystemResult(isGranted, "Standort"));

    private final ActivityResultLauncher<String> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> handleSystemResult(isGranted, "Kamera"));

    private final ActivityResultLauncher<String> microphoneLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> handleSystemResult(isGranted, "Mikrofon"));

    public void requestLocationPermission(PermissionCallback callback) {
        if (sp.getBoolean(NinjaWebView.getProfile() + "_location", false)){
            if (PermissionUtils.hasPermission(this, PermissionUtils.PermissionType.LOCATION)) {
                callback.onPermissionResult(true);
            } else {
                this.activeCallback = callback;
                locationLauncher.launch(PermissionUtils.getManifestString(PermissionUtils.PermissionType.LOCATION));
            }
        }
    }
    public void requestCameraPermission(PermissionCallback callback) {
        if (sp.getBoolean(NinjaWebView.getProfile() + "_camera", false)){
            if (PermissionUtils.hasPermission(this, PermissionUtils.PermissionType.CAMERA)) {
                callback.onPermissionResult(true);
            } else {
                this.activeCallback = callback;
                cameraLauncher.launch(PermissionUtils.getManifestString(PermissionUtils.PermissionType.CAMERA));
            }
        }
    }
    public void requestMicrophonePermission(PermissionCallback callback) {
        if (sp.getBoolean(NinjaWebView.getProfile() + "_microphone", false)){
            if (PermissionUtils.hasPermission(this, PermissionUtils.PermissionType.MICROPHONE)) {
                callback.onPermissionResult(true);
            } else {
                this.activeCallback = callback;
                microphoneLauncher.launch(PermissionUtils.getManifestString(PermissionUtils.PermissionType.MICROPHONE));
            }
        }
    }

    private void handleSystemResult(boolean isGranted, String name) {
        if (activeCallback != null) {
            activeCallback.onPermissionResult(isGranted);
            activeCallback = null;
        }
        if (!isGranted) {
            Toast.makeText(this, name + ": " + getString(R.string.app_permission), Toast.LENGTH_SHORT).show();
        }
    }

    private AdapterRecord adapter;
    private ImageButton fab_overview;
    private ListView listView;

    // Views
    private TextInputEditText search_input;
    private TextView appBar_title;
    private EditText searchOnSiteInput;
    @SuppressLint("StaticFieldLeak")
    private static NinjaWebView ninjaWebView;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private VideoView videoView;
    private FloatingActionButton fab_menu;
    private BadgeDrawable badgeDrawable;
    private AdapterSearch adapterSearch;
    private MaterialCardView searchOnSiteLayout;

    // TV: remote controlled mouse pointer. The D-pad moves the circle, OK is a real click
    // (see initTvCursor()). Position and pressed state are kept here, the view only draws.
    private View tvCursor;
    private float tvCursorX;
    private float tvCursorY;
    private boolean tvCursorPressed;
    private boolean tvCursorImeVisible;
    private int tvCursorScrollDelta = 0;
    private long tvCursorDownTime = 0;
    // TV: holding the OK/Enter button opens the page actions menu (tvLongPressRunnable).
    // tvLongPressPending is true while the timeout is posted, tvLongPressFired is true once
    // the menu was opened for the current press (the release must then not click through).
    private boolean tvLongPressPending = false;
    private boolean tvLongPressFired = false;

    // TV: directions currently held on the remote. While at least one is held, a 60 fps
    // timer moves the pointer continuously (with acceleration). This is what makes the
    // pointer glide like a real mouse instead of jumping once per key repeat.
    private static final int TV_CURSOR_UP = 1;
    private static final int TV_CURSOR_DOWN = 2;
    private static final int TV_CURSOR_LEFT = 4;
    private static final int TV_CURSOR_RIGHT = 8;
    private int tvCursorDirections = 0;
    private boolean tvCursorMoving = false;
    private long tvCursorMoveStart = 0;
    private long tvCursorMoveLast = 0;
    private long tvCursorHoverLast = 0;
    private final Handler tvCursorHandler = new Handler(Looper.getMainLooper());

    // TV: idle handling of the browser bar and of the remote mouse pointer. Both disappear
    // after TV_IDLE_TIMEOUT_MS without being used and come back with the next interaction
    // (bar: the pointer hovers it, pointer: any D-pad movement or click). See tvIdleRunnable.
    private static final long TV_IDLE_TIMEOUT_MS = 15000L;
    private static final long TV_IDLE_TICK_MS = 250L;
    private boolean tvCursorIdleHidden = false;
    private boolean tvAppBarIdleHidden = false;
    private boolean tvCursorOnAppBar = false;
    private long tvCursorLastMove = 0;
    private long tvAppBarLastUse = 0;

    // Layouts
    private LinearProgressIndicator progressBar;
    private FrameLayout contentFrame;
    private LinearLayout tab_container;
    private FrameLayout fullscreenHolder;
    private ListView list_search;

    // Others
    private BottomNavigationView bottom_navigation;
    private String overViewTab;
    private Activity activity;
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    private SharedPreferences sp;
    private List_standard listStandard;
    private long newIcon;
    private long filterBy;
    private boolean filter;
    private AlbumController currentAlbumController = null;

    private ValueCallback<Uri[]> filePathCallback;

    private ValueCallback<Uri[]> mFilePathCallback;
    private Uri cameraPhotoUri;
    private File currentPhotoFile;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;

    private final ActivityResultLauncher<Intent> fileChooserLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (mFilePathCallback == null) return;
                Uri[] results = null;
                if (result.getResultCode() == Activity.RESULT_OK && cameraPhotoUri != null) {
                    if (currentPhotoFile != null && currentPhotoFile.exists() && currentPhotoFile.length() > 0) {
                        results = new Uri[]{cameraPhotoUri};
                    }
                }
                if (results == null && result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data.getData() != null) {
                        results = new Uri[]{data.getData()};
                    } else if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        results = new Uri[count];
                        for (int i = 0; i < count; i++) {
                            results[i] = data.getClipData().getItemAt(i).getUri();
                        }
                    }
                }
                // Ergebnis an die WebView zurückgeben
                // WICHTIG: Sendet bei Abbruch ein leeres Array, damit die WebView nicht blockiert/einfriert
                mFilePathCallback.onReceiveValue(Objects.requireNonNullElseGet(results, () -> new Uri[0]));
                mFilePathCallback = null;
                currentPhotoFile = null;
            }
    );

    public static Context getAppContext() {
        return context;
    }
    private AlertDialog dialogOverview;

    private AlertDialog dialog_overflow;
    private AlertDialog dialogSearch;
    private View dialogViewSearch;
    // The dialog content views have to be kept: a dialog only attaches its content view when
    // it is shown for the first time, Dialog.findViewById() returns null before that.
    private View dialogViewOverview;
    private AlertDialog dialogCustomSearches;
    private CardView appBar;
    // TV: whole bottom bar row (address bar card + overview + menu button). The row is the
    // view that is hidden / shown again by the idle handling.
    private View appBar_buttons;
    private View contentView;

    private AlbumController nextAlbumController(boolean next) {
        if (BrowserContainer.size() <= 1) return currentAlbumController;
        List<AlbumController> list = BrowserContainer.list();
        int index = list.indexOf(currentAlbumController);
        if (next) {
            index++;
            if (index >= list.size()) index = 0; }
        else {
            index--;
            if (index < 0) index = list.size() - 1; }
        return list.get(index);
    }

    private class VideoCompletionListener implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            return false;
        }
        @Override
        public void onCompletion(MediaPlayer mp) {
            exitFullscreen();
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = BrowserActivity.this;
        context = BrowserActivity.this;
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        //noinspection InstantiationOfUtilityClass
        new BannerBlock(context);
        HelperUnit.initTheme(activity);

        if (sp.getBoolean("sp_screenOn", false)) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (sp.getBoolean("sp_standard_restart", false)) sp.edit().putString("profile", "profileStandard").apply();

        sp.edit()
                .putInt("restart_changed", 0)
                .putBoolean("pdf_create", false)
                .putBoolean("show_overview", true)
                .putString("openBackground_dialog", "show").apply();

        switch (sp.getString("start_tab", "3")) {
            case "4":
                overViewTab = getString(R.string.album_title_history);
                break;
            case "5":
                overViewTab = getString(R.string.album_title_tab);
                break;
            default:
                overViewTab = getString(R.string.album_title_bookmarks);
                break;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        contentFrame = findViewById(R.id.main_content);

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        findViewById(R.id.main).setBackgroundColor(ContextCompat.getColor(context, com.google.android.material.R.color.design_default_color_on_secondary));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int keyboardHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            boolean isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            // TV: the remote mouse pointer needs to know when the on-screen keyboard is up
            // (then Enter belongs to the keyboard and must not click at the pointer).
            tvCursorImeVisible = isKeyboardVisible;
            int paddingBottom = isKeyboardVisible ? keyboardHeight : systemBars.bottom;
            v.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    paddingBottom
            );
            return insets;
        });

        requestCameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), this::startCustomFileChooser);

        MaterialAlertDialogBuilder builderOverview = new MaterialAlertDialogBuilder(context);
        View dialogViewOverview = View.inflate(context, R.layout.dialog_overview, null);
        builderOverview.setView(dialogViewOverview);
        this.dialogViewOverview = dialogViewOverview;
        dialogOverview = builderOverview.create();
        bottom_navigation = dialogViewOverview.findViewById(R.id.bottom_navigation);
        tab_container = dialogViewOverview.findViewById(R.id.listTabs);
        HelperUnit.setupDialog(context, dialogOverview);
        // TV fork: the overview is no longer the start page. It stays available through the
        // tabs button, the browser starts directly on the web (see below).

        MaterialAlertDialogBuilder builderSearch = new MaterialAlertDialogBuilder(context);
        dialogViewSearch = View.inflate(context, R.layout.dialog_search, null);
        builderSearch.setView(dialogViewSearch);
        dialogSearch = builderSearch.create();
        HelperUnit.setupDialog(context, dialogSearch);
        dialogSearch.show();

        // TV: when a dialog is closed (Back), the D-pad focus has to return to the web page.
        dialogOverview.setOnDismissListener(dialog -> returnFocusToPage());
        dialogSearch.setOnDismissListener(dialog -> returnFocusToPage());

        BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String text = getString(R.string.app_done) + ". " + getString(R.string.menu_download) +"?";
                Snackbar snackbar = Snackbar.make(ninjaWebView, text, Snackbar.LENGTH_SHORT);
                HelperUnit.makeSnackbarRound(snackbar);
                snackbar.setAction(context.getString(R.string.app_ok), v -> startActivity(Intent.createChooser(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS), null)));
                snackbar.show();
            }};

        if (SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), RECEIVER_EXPORTED);
        } else {
            registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }

        initOmniBox();
        initSearchOnSite();
        initOverview();
        initTvCursor();
        hideSearch();

        // TV: drop leftovers of the removed "custom redirects" feature. It rewrote
        // youtube.com to an Invidious instance whose anti bot check page ("Gandalf - You
        // shall not pass!") blocked the browser. Stored tabs that still point at such a
        // page are dropped, so the next start does not show it again.
        if (!sp.getBoolean("tv_redirects_cleaned", false)) {
            sp.edit()
                    .remove("customRedirects")
                    .remove("saved_redirect_ok")
                    .remove("sp_youTube_string_domain")
                    .remove("sp_twitter_string_domain")
                    .remove("mCurrentUrl")
                    .remove("backPressed")
                    .putBoolean("tv_redirects_cleaned", true)
                    .apply();
            String[] tabs = TextUtils.split(sp.getString("openTabs", ""), "‚‗‚");
            ArrayList<String> keepTabs = new ArrayList<>();
            for (String tab : tabs) {
                if (!tab.contains(".gandalf") && !tab.contains("invidious.")) keepTabs.add(tab);
            }
            sp.edit().putString("openTabs", TextUtils.join("‚‗‚", keepTabs)).apply();
        }

        //restore open Tabs from shared preferences if app got killed
        if (sp.getBoolean("sp_restoreTabs", false)
                || sp.getBoolean("sp_reloadTabs", false)
                || sp.getBoolean("restoreOnRestart", false)) {
            String saveDefaultProfile = sp.getString("profile", "profileStandard");
            ArrayList<String> openTabs;
            openTabs = new ArrayList<>(Arrays.asList(TextUtils.split(sp.getString("openTabs", ""), "‚‗‚")));
            if (!openTabs.isEmpty()) {
                for (int counter = 0; counter < openTabs.size(); counter++) {
                    addAlbum(getString(R.string.app_name), openTabs.get(counter), BrowserContainer.size() < 1);
                }
            }
            sp.edit().putString("profile", saveDefaultProfile).apply();
            sp.edit().putBoolean("restoreOnRestart", false).apply();
        }

        // TV fork: no overview start page. Restored tabs win; otherwise the browser starts
        // on google.com. When the app was opened for external content (shared link,
        // ACTION_VIEW, TV search, ...) that content is loaded by dispatchIntent() instead.
        Intent launchIntent = getIntent();
        String launchAction = launchIntent == null ? null : launchIntent.getAction();
        boolean launchedForContent = Intent.ACTION_VIEW.equals(launchAction)
                || Intent.ACTION_SEND.equals(launchAction)
                || Intent.ACTION_SEND_MULTIPLE.equals(launchAction)
                || Intent.ACTION_WEB_SEARCH.equals(launchAction)
                || Intent.ACTION_PROCESS_TEXT.equals(launchAction)
                || "postLink".equals(launchAction)
                || "customSearches".equals(launchAction);
        if (BrowserContainer.size() == 0 && !launchedForContent) {
            addAlbum(getString(R.string.app_name), "https://www.google.com", true);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (customView != null || fullscreenHolder != null) {
                    // TV: Back leaves the fullscreen video / fullscreen page. It used to only
                    // log, so there was no way out of a fullscreen video with the remote.
                    exitFullscreen();
                } else if (searchOnSiteLayout.getVisibility() == VISIBLE){
                    searchOnSiteInput.setText("");
                    searchOnSiteLayout.setVisibility(GONE);
                    noteTvAppBarUse();
                } else if (ninjaWebView != null && chromeHasFocus()) {
                    // TV: Back first returns the focus to the web page, it does not leave the app
                    ninjaWebView.requestFocus();
                } else if (ninjaWebView.canGoBack()){
                    ninjaWebView.goBack();
                } else {
                    // TV: Back never leaves the app (no tab close, no quit). Without a
                    // previous page it only tells the user.
                    NinjaToast.show(BrowserActivity.this, R.string.toast_no_back_history);
                }
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    /**
     * TV: the remote mouse pointer is only shown while the browser window and the web page
     * both have the focus. While a dialog is open (it is used with the D-pad) the pointer
     * waits behind it instead of drawing over it. This also cancels a held "click" when a
     * dialog opens in the middle of the press (for example the link context menu after a
     * long press), otherwise the next click would be stuck.
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) {
            // A dialog or another window took the keys: drop a pending long press and a
            // held click, their key up events may never reach this activity again.
            cancelTvLongPress();
            if (tvCursorPressed) cancelTvCursorPress();
        }
        updateTvCursorVisibility();
        updateTvCursorScroll();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sp.getInt("restart_changed", 1) == 1) {
            triggerRebirth(context);
        }
        if (sp.getBoolean("pdf_create", false)) {
            sp.edit().putBoolean("pdf_create", false).apply();
            String text = getString(R.string.app_done) + ". " + getString(R.string.menu_download) +"?";
            Snackbar snackbar = Snackbar.make(ninjaWebView, text, Snackbar.LENGTH_SHORT);
            HelperUnit.makeSnackbarRound(snackbar);
            snackbar.setAction(context.getString(R.string.app_ok), v -> startActivity(Intent.createChooser(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS), null)));
            snackbar.show();
        }
        Intent intent = getIntent();
        if (intent != null && intent.getAction() != null && !intent.getAction().isEmpty()) {
            dispatchIntent(intent);
            intent.setAction("");
            intent.setData(null);
        }
        // TV: returning to the app shows the browser bar and the remote mouse pointer again,
        // their idle timers start from the beginning.
        noteTvPointerActivity();
        noteTvAppBarUse();
        startTvIdleLoop();
    }

    @Override
    public void onDestroy() {
        cancelTvLongPress();
        tvCursorHandler.removeCallbacksAndMessages(null);
        DeleteUtils.checkAndExecuteAutoDelete(getApplicationContext());
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(1);
        BrowserContainer.clear();
        if (!sp.getBoolean("sp_reloadTabs", false) || sp.getInt("restart_changed", 1) == 1) {
            sp.edit().putString("openTabs", "").apply();
        }
        super.onDestroy();
    }

    /**
     * TV: while the remote mouse pointer is visible it owns the D-pad and the OK key.
     * The keys are intercepted here, on the activity level, and not with a key listener of
     * the web view: the WebView sends every key to the web engine first (where the D-pad
     * scrolls the page), so a key listener on the web view would never see them. The
     * activity is the first station of every key event (see DecorView.dispatchKeyEvent),
     * the pointer therefore moves no matter what the page does with the keys.
     * Dialogs are separate windows, so their own D-pad navigation is not affected.
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Back is handled here, before the window and the view hierarchy get the key: the
        // WebView (Chromium) swallows KEYCODE_BACK in some situations, so the
        // OnBackPressedCallback would never run. Dialogs and the on-screen keyboard are
        // separate windows and keep their own Back handling.
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
                getOnBackPressedDispatcher().onBackPressed();
            }
            return true;
        }
        if (tvCursorAllowed() && handleTvCursorKey(event.getKeyCode(), event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            showOverflow(null, null, 0, ninjaWebView.getTitle(), ninjaWebView.getUrl(), null, null, 0);
        }
        return false;
    }

    @Override
    public synchronized void showAlbum(AlbumController controller) {
        if (controller == null) return;

        if (currentAlbumController != null) currentAlbumController.deactivate();
        currentAlbumController = controller;
        currentAlbumController.activate();

        View av = (View) controller;
        if (av.getBackground() != null) {
            contentFrame.setBackground(av.getBackground());
        }
        if (av.getParent() != null) {
            ((ViewGroup) av.getParent()).removeView(av);
        }
        if (contentFrame.isLaidOut() && contentFrame.getChildCount() > 0 && BrowserContainer.size() > 1) {
            View oldView = contentFrame.getChildAt(0);
            oldView.animate()
                    .alpha(0f)
                    .setDuration(250)
                    .withEndAction(() -> contentFrame.removeView(oldView))
                    .start();
            av.setAlpha(0f);
            contentFrame.addView(av);
            av.animate().alpha(1f).setDuration(250).start();
        } else {
            contentFrame.removeAllViews();
            contentFrame.addView(av);
            av.setAlpha(1f);
        }
        try { updateOmniBox(); } catch (Exception ignored) {}
        // TV: after a tab switch the web page has to own the focus again, otherwise the
        // D-pad would not move the remote mouse pointer and the pointer would stay hidden.
        if (ninjaWebView != null && !ninjaWebView.hasFocus()) ninjaWebView.requestFocus();
        updateTvCursorVisibility();
    }

    @Override
    public synchronized void removeAlbum(final AlbumController controller) {

        if (BrowserContainer.size() <= 1) {
            if (!sp.getBoolean("sp_reopenLastTab", false)) {
                doubleTapsQuit();
            } else {
                ninjaWebView.loadUrl(Objects.requireNonNull(sp.getString("favoriteURL", "https://codeberg.org/Gaukler_Faun/FOSS_Browser/wiki")));
            }
        } else {
            closeTabConfirmation(() -> {
                AlbumController predecessor;
                if (controller == currentAlbumController) predecessor = ((NinjaWebView) controller).getPredecessor();
                else predecessor = currentAlbumController;
                //if not the current TAB is being closed return to current TAB
                tab_container.removeView(controller.getAlbumView());
                int index = BrowserContainer.indexOf(controller);
                BrowserContainer.remove(controller);
                if ((predecessor != null) && (BrowserContainer.indexOf(predecessor) != -1)) {
                    //if predecessor is stored and has not been closed in the meantime
                    showAlbum(predecessor);
                } else {
                    if (index >= BrowserContainer.size()) index = BrowserContainer.size() - 1;
                    showAlbum(BrowserContainer.get(index));
                }
            });
        }
        updateOmniBox();
    }

    @Override
    public synchronized void updateProgress(int progress) {
        progressBar.setProgressCompat(progress, true);
        updateOmniBox();
        if (progress < 100) {
            progressBar.setVisibility(VISIBLE);
        } else {
            progressBar.setVisibility(GONE);
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() ->
                            FaviconHelper.setFavicon(context, contentView, ninjaWebView.getUrl(), R.id.menu_icon, R.drawable.icon_image_broken),
                    500
            );}
    }

    @Override
    public void showFileChooser(Context context, ValueCallback<Uri[]> filePathCallback) {
        if (mFilePathCallback != null) {
            mFilePathCallback.onReceiveValue(null);
        }
        mFilePathCallback = filePathCallback;
        checkPermissionAndProceed();
    }
    private void checkPermissionAndProceed() {
        if (PermissionUtils.hasPermission(this, PermissionUtils.PermissionType.CAMERA)) {
            startCustomFileChooser(true);
        } else {
            String cameraPermission = PermissionUtils.getManifestString(PermissionUtils.PermissionType.CAMERA);
            requestCameraPermissionLauncher.launch(cameraPermission);
        }
    }
    private void startCustomFileChooser(boolean includeCamera) {
        Intent pickIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        pickIntent.addCategory(Intent.CATEGORY_OPENABLE);
        pickIntent.setType("*/*");

        String[] mimeTypes = {"image/*", "video/*", "*/*"};
        pickIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        pickIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        Intent chooserIntent;
        if (includeCamera) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            setupCameraUri();
            if (cameraPhotoUri != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri);
                chooserIntent = Intent.createChooser(pickIntent, null);
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takePictureIntent});
            } else {
                chooserIntent = Intent.createChooser(pickIntent, null);
            }
        } else {
            android.widget.Toast.makeText(this, "abgelehnt.", android.widget.Toast.LENGTH_LONG).show();
            chooserIntent = Intent.createChooser(pickIntent, null);
        }
        fileChooserLauncher.launch(chooserIntent);
    }
    private void setupCameraUri() {
        try {
            currentPhotoFile = createImageFile();
            cameraPhotoUri = FileProvider.getUriForFile(this, "de.baumann.browser.fileprovider", currentPhotoFile);
        } catch (IOException e) {
            Log.e("Upload", "Fehler beim Erstellen der Kamera-Datei", e);
            currentPhotoFile = null;
            cameraPhotoUri = null;
        }
    }
    private File createImageFile() throws IOException {
        String imageFileName = "JPEG_" + System.currentTimeMillis() + "_";
        File storageDir = getCacheDir();
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }
    @Override
    public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
        if (view == null) return;
        Log.v(TAG, "onShowCustomView: fullscreen view shown");
        if (customView != null) {
            // Already showing a fullscreen view: refuse the new one. The WebView expects the
            // callback of a view it never gets to see.
            if (callback != null) callback.onCustomViewHidden();
            return;
        }
        if (tvCursorPressed) cancelTvCursorPress();

        customView = view;
        customViewCallback = callback;
        fullscreenHolder = new FrameLayout(context);
        fullscreenHolder.addView(
                customView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                ));
        FrameLayout decorView = (FrameLayout) getWindow().getDecorView();
        decorView.addView(
                fullscreenHolder,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                ));
        // TV: the remote mouse pointer lives in the decor view as well, but the fullscreen
        // view was added after it and would cover it. The pointer has to stay on top - it is
        // the only way to use the remote.
        if (tvCursor != null) decorView.bringChildToFront(tvCursor);
        customView.setKeepScreenOn(true);
        if (currentAlbumController != null) ((View) currentAlbumController).setVisibility(GONE);
        setCustomFullscreen(true);

        if (view instanceof FrameLayout) {
            if (((FrameLayout) view).getFocusedChild() instanceof VideoView) {
                videoView = (VideoView) ((FrameLayout) view).getFocusedChild();
                videoView.setOnErrorListener(new VideoCompletionListener());
                videoView.setOnCompletionListener(new VideoCompletionListener());
            }
        }
        // TV: the web view is hidden while the video plays, so the page cannot own the focus
        // and the pointer would be hidden and dead. It is unlocked explicitly (see
        // tvCursorAllowed) and woken up, otherwise a fullscreen video would trap the user.
        noteTvPointerActivity();
        updateTvCursorVisibility();
        updateTvCursorScroll();
    }

    @Override
    public void onHideCustomView() {
        Log.v(TAG, "onHideCustomView: " + (customView != null ? "leaving fullscreen" : "nothing to hide"));
        // Called by the engine (video finished, page left fullscreen) and by exitFullscreen(),
        // so it has to survive being called without an active fullscreen view.
        if (customView == null && fullscreenHolder == null) return;
        if (fullscreenHolder != null && fullscreenHolder.getParent() instanceof ViewGroup) {
            ((ViewGroup) fullscreenHolder.getParent()).removeView(fullscreenHolder);
        }
        if (customView != null) customView.setKeepScreenOn(false);
        customView = null;
        customViewCallback = null;
        fullscreenHolder = null;
        if (videoView != null) {
            videoView.setOnErrorListener(null);
            videoView.setOnCompletionListener(null);
            videoView = null; }
        if (currentAlbumController != null) ((View) currentAlbumController).setVisibility(VISIBLE);
        setCustomFullscreen(false);
        // TV: the web view was GONE while the video was played and does not get the focus
        // back on its own - without this request the remote mouse pointer would stay hidden
        // and dead after every fullscreen video.
        if (ninjaWebView != null) ninjaWebView.requestFocus();
        noteTvPointerActivity();
        noteTvAppBarUse();
        updateTvCursorVisibility();
        updateTvCursorScroll();
    }

    /**
     * TV: leaves a fullscreen video or fullscreen web page (Back key, finished video). The
     * WebView has to be told that the custom view is gone, otherwise the page believes it is
     * still in fullscreen mode and stays in its fullscreen layout.
     */
    private void exitFullscreen() {
        WebChromeClient.CustomViewCallback callback = customViewCallback;
        customViewCallback = null;
        if (callback != null) callback.onCustomViewHidden();
        onHideCustomView();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initOverview() {
        // Take the list from the inflated layout: while the overview dialog is not shown its
        // content view is not attached to the dialog yet, dialogOverview.findViewById() would
        // return null (that crashed the listener of the bottom navigation on start-up).
        View overviewRoot = dialogViewOverview != null ? dialogViewOverview : dialogOverview.findViewById(R.id.list_overView);
        listView = overviewRoot != null ? overviewRoot.findViewById(R.id.list_overView) : null;
        if (listView == null) return;
        AtomicInteger intPage = new AtomicInteger();

        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimaryInverse, typedValue, true);
        int color = typedValue.data;
        TypedValue typedValue2 = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue2, true);
        int color2 = typedValue2.data;

        bottom_navigation.getOrCreateBadge(R.id.page_0).setBackgroundColor(color);
        bottom_navigation.getOrCreateBadge(R.id.page_0).setBadgeTextColor(color2);
        bottom_navigation.getOrCreateBadge(R.id.page_0).setHorizontalOffset(0);
        bottom_navigation.getOrCreateBadge(R.id.page_0).setVerticalOffset(0);

        // TV: material's bottom navigation is designed for touch input. Its entries are made
        // focusable here so that a remote can reach and confirm every destination, and focus
        // search prefers the entries over the bar itself.
        bottom_navigation.setFocusable(true);
        bottom_navigation.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        for (int i = 0; i < bottom_navigation.getMenu().size(); i++) {
            View navItem = bottom_navigation.findViewById(bottom_navigation.getMenu().getItem(i).getItemId());
            if (navItem != null) navItem.setFocusable(true);
        }

        if (BrowserContainer.size() > 1) {
            bottom_navigation.getOrCreateBadge(R.id.page_0).setNumber(BrowserContainer.size());
        }

        NavigationBarView.OnItemSelectedListener navListener = menuItem -> {

            if (menuItem.getItemId() == R.id.page_0) {
                fab_overview.setImageResource(R.drawable.icon_tab);
                overViewTab = getString(R.string.album_title_tab);
                intPage.set(R.id.page_0);
                listView.setVisibility(GONE);
                tab_container.setVisibility(VISIBLE);}

            else if (menuItem.getItemId() == R.id.page_2) {
                try {
                    RecordAction action = new RecordAction(context);
                    action.open(true);
                    // TV: this listener also fires from setSelectedTab() during start-up, when
                    // the first tab does not exist yet (the NPE was caught and logged).
                    if (ninjaWebView != null && action.checkUrl(ninjaWebView.getUrl(), RecordUnit.TABLE_BOOKMARK)) {
                        fab_overview.setImageResource(R.drawable.icon_bookmark_added);
                    } else {
                        fab_overview.setImageResource(R.drawable.icon_bookmark);
                    }
                    action.close();
                } catch (Exception e) {Log.i(TAG, "dialogCustomSearches:" + e);}
                overViewTab = getString(R.string.album_title_bookmarks);
                intPage.set(R.id.page_2);
                listView.setVisibility(VISIBLE);
                tab_container.setVisibility(GONE);

                RecordAction action = new RecordAction(context);
                action.open(false);
                final List<Record> list;
                list = action.listBookmark(activity, filter, filterBy);
                action.close();
                adapter = new AdapterRecord(context, list);
                listView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                filter = false;
                listView.setOnItemClickListener((parent, view, position, id) -> {
                    ninjaWebView.loadUrl(list.get(position).getURL());
                    hideOverview();
                });
                listView.setOnItemLongClickListener((parent, view, position, id) -> {
                    showOverflow(dialogOverview,  listView, 3, list.get(position).getTitle(), list.get(position).getURL(), adapter, list, position);
                    return true;
                }); }
            else if (menuItem.getItemId() == R.id.page_3) {
                fab_overview.setImageResource(R.drawable.icon_history);
                overViewTab = getString(R.string.album_title_history);
                intPage.set(R.id.page_3);
                listView.setVisibility(VISIBLE);
                tab_container.setVisibility(GONE);

                RecordAction action = new RecordAction(context);
                action.open(false);
                final List<Record> list;
                list = action.listHistory(context);
                action.close();
                //noinspection NullableProblems
                adapter = new AdapterRecord(context, list) {
                    @Override
                    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                        View v = super.getView(position, convertView, parent);
                        TextView record_item_time = v.findViewById(R.id.dateView);
                        record_item_time.setVisibility(VISIBLE);
                        return v;
                    }
                };
                listView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                listView.setOnItemClickListener((parent, view, position, id) -> {
                    ninjaWebView.loadUrl(list.get(position).getURL());
                    hideOverview();
                });
                listView.setOnItemLongClickListener((parent, view, position, id) -> {
                    showOverflow(dialogOverview, listView, 4, list.get(position).getTitle(), list.get(position).getURL(), adapter, list, position);
                    return true;
                }); }
            else if (menuItem.getItemId() == R.id.page_4) {
                PopupMenu popup = new PopupMenu(this, bottom_navigation.findViewById(R.id.page_2));
                popup.setForceShowIcon(true);
                popup.setOnDismissListener(menu -> setSelectedTab());
                if (bottom_navigation.getSelectedItemId() == R.id.page_0)
                    popup.inflate(R.menu.menu_help);
                else if (bottom_navigation.getSelectedItemId() == R.id.page_2)
                    popup.inflate(R.menu.menu_list_bookmark);
                else if (bottom_navigation.getSelectedItemId() == R.id.page_3)
                    popup.inflate(R.menu.menu_list_history);

                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.menu_delete) {
                        Snackbar snackbarBottom = Snackbar.make(bottom_navigation, R.string.hint_database, Snackbar.LENGTH_SHORT);
                        HelperUnit.makeSnackbarRound(snackbarBottom);
                        snackbarBottom.setAction(context.getString(R.string.app_ok), (v -> {
                            if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                                BrowserUnit.clearBookmark(context);
                                bottom_navigation.setSelectedItemId(R.id.page_2); }
                            else if (overViewTab.equals(getString(R.string.album_title_history))) {
                                BrowserUnit.clearHistory(context);
                                bottom_navigation.setSelectedItemId(R.id.page_3); }
                        }));
                        snackbarBottom.show();
                    } else if (item.getItemId() == R.id.menu_sortName) {
                        sp.edit().putString("sort_bookmark", "title").apply();
                        sp.edit().putBoolean("sort_bookmarkDomain", false).apply();
                        bottom_navigation.setSelectedItemId(R.id.page_2);
                    } else if (item.getItemId() == R.id.menu_sortIcon) {
                        sp.edit().putString("sort_bookmark", "time").apply();
                        sp.edit().putBoolean("sort_bookmarkDomain", false).apply();
                        bottom_navigation.setSelectedItemId(R.id.page_2);
                    } else if (item.getItemId() == R.id.menu_sortDate) {
                        sp.edit().putBoolean("sort_historyDomain", false).apply();
                        bottom_navigation.setSelectedItemId(R.id.page_3);
                    } else if (item.getItemId() == R.id.menu_sortDomain) {
                        if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                            sp.edit().putBoolean("sort_bookmarkDomain", true).apply();
                            bottom_navigation.setSelectedItemId(R.id.page_2); }
                        else if (overViewTab.equals(getString(R.string.album_title_history))) {
                            sp.edit().putBoolean("sort_historyDomain", true).apply();
                            bottom_navigation.setSelectedItemId(R.id.page_3);
                        }
                    } else if (item.getItemId() == R.id.menu_filter) {
                        showDialogFilter();
                    } else if (item.getItemId() == R.id.menu_help) {
                        Uri webpage = Uri.parse("https://codeberg.org/Gaukler_Faun/FOSS_Browser/wiki/Overview");
                        BrowserUnit.intentURL(this, webpage); }
                    return true;
                });
                popup.show();
            }

            return true;
        };
        bottom_navigation.setOnItemSelectedListener(navListener);
        bottom_navigation.findViewById(R.id.page_2).setOnLongClickListener(v -> {
            showDialogFilter();
            return true;
        });
        setSelectedTab();
    }

    @SuppressLint({"ClickableViewAccessibility", "UnsafeOptInUsageError"})
    private void initOmniBox() {

        appBar = findViewById(R.id.appBar);
        appBar_buttons = findViewById(R.id.appBar_buttons);
        appBar_title = findViewById(R.id.appBar_title);
        contentView = findViewById(android.R.id.content);

        // TV: the browser bar hides itself 15 s after its last use and comes back when the
        // remote mouse pointer hovers it (see tvIdleRunnable). The "hide the toolbar while
        // scrolling" pref of the phone version stays disabled - the bar only follows the
        // idle rules.
        sp.edit().putBoolean("hideToolbar", false).apply();

        appBar.setOnClickListener(view -> {
            noteTvAppBarUse();
            initSearch();
            sp.edit().putString("sp_search_customSearches", "").apply();
            search_input.setText(ninjaWebView.getUrl());
            dialogSearch.show();
            HelperUnit.showSoftKeyboard(search_input);
        });
        // TV: long press (OK held down while the remote mouse pointer is over the browser
        // bar) opens the page menu.
        appBar.setOnLongClickListener(v -> {
            noteTvAppBarUse();
            showOverflow(null, null, 0, ninjaWebView.getTitle(), ninjaWebView.getUrl(), null, null, 0);
            return true;
        });


        fab_menu = findViewById(R.id.fab_menu);
        fab_menu.setOnClickListener(view -> {
            noteTvAppBarUse();
            showOverflow(null, null, 0, ninjaWebView.getTitle(), ninjaWebView.getUrl(), null, null, 0); });
        fab_menu.setOnLongClickListener(view -> {
            noteTvAppBarUse();
            performGesture("setting_gesture_tabButton", ninjaWebView.getUrl());
            return true;
        });
        fab_overview = findViewById(R.id.fab_overview);
        fab_overview.setOnClickListener(v -> {
            noteTvAppBarUse();
            showOverview(); });
        fab_overview.setOnLongClickListener(v -> {
            noteTvAppBarUse();
            performGesture("setting_gesture_overViewButton", ninjaWebView.getUrl());
            return true;
        });

        list_search = dialogViewSearch.findViewById(R.id.list_search);
        progressBar = findViewById(R.id.main_progress_bar);
        badgeDrawable = BadgeDrawable.create(context);

        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimaryInverse, typedValue, true);
        badgeDrawable.setBackgroundColor(typedValue.data);
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
        badgeDrawable.setBadgeTextColor(typedValue.data);

        // TV: the four swipe directions of the phone version are mapped onto the four D-pad
        // directions while the button has the focus. Directions without an assigned action
        // are not consumed, so the D-pad can still leave the button.
        attachRemoteGestureKeys(fab_overview, "setting_gesture_tb");
        attachRemoteGestureKeys(fab_menu, "setting_gesture_nav");

        TextInputLayout search_textField  = dialogViewSearch.findViewById(R.id.search_textField);
        search_textField.setStartIconOnClickListener(v -> {
            if (Objects.requireNonNull(search_input.getText()).toString().isEmpty()) {
                hideSearch();
            } else {
                search_input.setText("");
            }
        });
        search_textField.setEndIconOnLongClickListener(v -> {
            String query = Objects.requireNonNull(search_input.getText()).toString().trim();
            if (!query.isEmpty() && !query.equals(ninjaWebView.getUrl())) {
                showDialogCustomSearches(query);
            } else {
                NinjaToast.show(this, R.string.toast_input_empty);
            }
            return false;
        });
        search_textField.setEndIconOnClickListener(v -> {
            String query = Objects.requireNonNull(search_input.getText()).toString().trim();
            handleFinalSearch(query);
        });

        search_input = dialogViewSearch.findViewById(R.id.search_input);
        search_input.setOnEditorActionListener((v, actionId, event) -> {
            String query = Objects.requireNonNull(search_input.getText()).toString().trim();
            handleFinalSearch(query);
            return true;
        });

        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
        ColorStateList colorActive = ColorStateList.valueOf(typedValue.data);
        ColorStateList colorInactive = ColorStateList.valueOf(Color.GRAY);

        search_input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s == null) return;
                ColorStateList tint = (!TextUtils.isEmpty(s)) ? colorActive : colorInactive;
                search_textField.setStartIconTintList(tint);
                search_textField.setEndIconTintList(tint);
                adapterSearch.getFilter().filter(s);
                sp.edit().putString("searchInput", s.toString()).apply();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * TV replacement for the swipe gestures of the phone version: while the button has the
     * D-pad focus, the four D-pad directions trigger the same configurable gesture actions.
     * A direction without an action is not consumed so that the focus can move on normally.
     */
    private void attachRemoteGestureKeys(View view, String gesturePrefix) {
        view.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            final String gesture;
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP: gesture = gesturePrefix + "_up"; break;
                case KeyEvent.KEYCODE_DPAD_DOWN: gesture = gesturePrefix + "_down"; break;
                case KeyEvent.KEYCODE_DPAD_LEFT: gesture = gesturePrefix + "_left"; break;
                case KeyEvent.KEYCODE_DPAD_RIGHT: gesture = gesturePrefix + "_right"; break;
                default: return false;
            }
            if (!hasGestureAction(gesture)) return false;
            performGesture(gesture, ninjaWebView.getUrl());
            return true;
        });
    }

    /**
     * True if the user assigned an action to the given gesture: only action codes that
     * performGesture() really handles count, everything else means "do nothing".
     */
    private boolean hasGestureAction(String gesture) {
        switch (Objects.requireNonNull(sp.getString(gesture, "0"))) {
            case "02": case "03": case "04": case "05": case "06": case "07": case "08":
            case "09": case "10": case "11": case "12": case "13": case "14": case "16":
            case "17": case "18": case "19": case "22": case "24": case "25": case "26":
            case "27": case "29": case "30": case "31": case "32":
                return true;
            default:
                return false;
        }
    }

    /**
     * TV: gives the browser chrome (address bar, tab button, menu button) the D-pad focus
     * back to the web page.
     */
    private boolean chromeHasFocus() {
        return appBar != null && (appBar.hasFocus() || fab_overview.hasFocus() || fab_menu.hasFocus());
    }

    /**
     * TV: gives the D-pad focus back to the web page, for example after a dialog was closed.
     */
    private void returnFocusToPage() {
        if (ninjaWebView != null) ninjaWebView.requestFocus();
        // TV: the bar and the pointer come back with the focus (a dialog that was opened from
        // the bar just closed), the idle timers start fresh.
        noteTvPointerActivity();
        noteTvAppBarUse();
        updateTvCursorVisibility();
    }

    // ---------------------------------------------------------------------------------------
    // TV: remote controlled mouse pointer
    //
    // The D-pad moves a small circle over the screen like a mouse: while a direction is
    // held, a 60 fps loop moves the pointer continuously and accelerates it (see
    // tvCursorMoveRunnable), so the movement is smooth instead of jumping per key repeat.
    // OK is a real left click:
    //   * tap              -> click at the pointer position
    //   * press and hold   -> the current page's actions menu opens (showOverflow, see
    //                         tvLongPressRunnable); the synthetic touch is cancelled first,
    //                         so the page gets no long click and the release no click
    //   * hold and move    -> drag, for example to select text or to drag the page
    // Touching the very top or the very bottom screen edge scrolls the page until the
    // pointer leaves the edge again; the browser bar and its buttons never scroll.
    // The generated touch is dispatched to the whole window (decor view), so the pointer can
    // also click the browser bar, the tabs button and the menu button - everything on the
    // screen, exactly like a mouse. Dialogs are separate windows and keep their own D-pad
    // focus handling.
    // ---------------------------------------------------------------------------------------

    private void initTvCursor() {
        tvCursor = findViewById(R.id.tv_cursor);
        if (tvCursor == null) return;
        // TV: the pointer is moved into the decor view, above the whole app content. As a
        // child of R.id.main it would be covered by a fullscreen video - the WebView adds
        // that view to the decor view as well, and the pointer has to stay usable there.
        ViewGroup mainRoot = findViewById(R.id.main);
        if (mainRoot != null && tvCursor.getParent() == mainRoot) {
            ViewGroup.LayoutParams cursorParams = tvCursor.getLayoutParams();
            mainRoot.removeView(tvCursor);
            ((ViewGroup) getWindow().getDecorView()).addView(tvCursor, cursorParams);
        }
        // Round outline for the elevation shadow: the shadow of a layer-list background
        // would be a square around the circle and look like a smudge.
        tvCursor.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        tvCursorX = getResources().getDisplayMetrics().widthPixels / 2f;
        tvCursorY = getResources().getDisplayMetrics().heightPixels / 2f;
        updateTvCursorPosition();
        updateTvCursorVisibility();
        // The view has no size before the first layout pass, place it once more afterwards
        // so the circle is centred exactly.
        tvCursor.post(this::updateTvCursorPosition);
        // TV: the bar and the pointer are visible after a (re)start, they hide themselves
        // again 15 s later.
        long now = SystemClock.uptimeMillis();
        tvCursorLastMove = now;
        tvAppBarLastUse = now;
        startTvIdleLoop();
    }

    /**
     * Shows the pointer while it is allowed (the page owns the focus or a fullscreen video is
     * on screen) and not idle, hides it while a dialog is open, while the app is in the
     * background or after {@link #TV_IDLE_TIMEOUT_MS} without movement.
     */
    private void updateTvCursorVisibility() {
        if (tvCursor == null) return;
        boolean visible = tvCursorAllowed() && !tvCursorIdleHidden;
        tvCursor.setVisibility(visible ? VISIBLE : View.INVISIBLE);
        if (!visible) stopTvCursorMovement();
    }

    /**
     * TV: true while the D-pad and the OK button belong to the remote mouse pointer - the web
     * page owns the focus (or a fullscreen video covers it) and the app window is in front.
     * This is deliberately independent of the idle state: a key press has to reach the
     * pointer even while it is hidden, otherwise it could never be woken up again.
     */
    private boolean tvCursorAllowed() {
        if (tvCursor == null || !hasWindowFocus()) return false;
        // While a video or a web page is in fullscreen the web view is hidden (GONE) and
        // cannot hold the focus - the pointer has to be allowed explicitly, otherwise the
        // remote would be dead inside the fullscreen view.
        if (customView != null) return true;
        return ninjaWebView != null && ninjaWebView.hasFocus();
    }

    // ---------------------------------------------------------------------------------------
    // TV: idle handling of the browser bar and of the remote mouse pointer
    //
    // The bar is hidden 15 s after it was used for the last time, the pointer is hidden 15 s
    // after it was moved for the last time. Both come back with the next interaction: the bar
    // when the pointer moves over it (hover), the pointer with the next D-pad press or click.
    // ---------------------------------------------------------------------------------------

    private final Runnable tvIdleRunnable = new Runnable() {
        @Override
        public void run() {
            long now = SystemClock.uptimeMillis();
            boolean onBar = tvCursorHoversAppBar();
            if (!tvCursorIdleHidden && !tvCursorPressed && !onBar
                    && now - tvCursorLastMove >= TV_IDLE_TIMEOUT_MS) {
                tvCursorIdleHidden = true;
                // TV: an unattended pointer resting on the top / bottom edge must not
                // scroll the page forever - the scroll stops with the pointer idle state.
                tvCursorScrollDelta = 0;
                tvCursorHandler.removeCallbacks(tvCursorScrollRunnable);
                updateTvCursorVisibility();
            }
            if (!tvAppBarIdleHidden && !onBar && !tvChromeInUse()
                    && now - tvAppBarLastUse >= TV_IDLE_TIMEOUT_MS) {
                hideTvAppBar();
            }
            tvCursorHandler.postDelayed(this, TV_IDLE_TICK_MS);
        }
    };

    private void startTvIdleLoop() {
        tvCursorHandler.removeCallbacks(tvIdleRunnable);
        tvCursorHandler.postDelayed(tvIdleRunnable, TV_IDLE_TICK_MS);
    }

    /** True while the pointer rests on the browser bar and is not idle itself. */
    private boolean tvCursorHoversAppBar() {
        return tvCursorOnAppBar && !tvCursorIdleHidden
                && tvCursor != null && tvCursor.getVisibility() == VISIBLE;
    }

    /** True while the browser bar is in use and must not be hidden. */
    private boolean tvChromeInUse() {
        if (chromeHasFocus()) return true;
        if (searchOnSiteLayout != null && searchOnSiteLayout.getVisibility() == VISIBLE) return true;
        // While a fullscreen video covers the screen the bar is unreachable anyway.
        return customView != null;
    }

    /** Registers a use of the browser bar (click, hover, page change): it stays visible. */
    private void noteTvAppBarUse() {
        tvAppBarLastUse = SystemClock.uptimeMillis();
        if (appBar_buttons == null) return;
        if (tvAppBarIdleHidden || appBar_buttons.getVisibility() != VISIBLE) {
            tvAppBarIdleHidden = false;
            showTvAppBar();
        }
    }

    private void showTvAppBar() {
        if (appBar_buttons == null) return;
        if (customView != null) return;     // covered by the fullscreen video
        appBar_buttons.animate().cancel();
        if (searchOnSiteLayout == null || searchOnSiteLayout.getVisibility() != VISIBLE) {
            // searchOnSite() hides the card while the in-page search bar is up, it must not
            // come back behind that bar.
            appBar.setVisibility(VISIBLE);
        }
        if (appBar_buttons.getVisibility() != VISIBLE) {
            appBar_buttons.setAlpha(0f);
            appBar_buttons.setVisibility(VISIBLE);
        }
        if (appBar_buttons.getAlpha() < 1f) {
            appBar_buttons.animate().alpha(1f).setDuration(180).start();
        }
    }

    private void hideTvAppBar() {
        if (appBar_buttons == null || appBar_buttons.getVisibility() != VISIBLE) return;
        tvAppBarIdleHidden = true;
        appBar_buttons.animate().cancel();
        appBar_buttons.animate()
                .alpha(0f)
                .setDuration(220)
                .withEndAction(() -> {
                    if (tvAppBarIdleHidden) appBar_buttons.setVisibility(View.INVISIBLE);
                })
                .start();
    }

    /**
     * Any pointer movement or click postpones the pointer idle timeout (and wakes the pointer
     * up again). While the pointer is over the browser bar this also keeps the bar awake and
     * brings it back after it went idle - hovering is the way to un-idle the bar.
     */
    private void noteTvPointerActivity() {
        tvCursorLastMove = SystemClock.uptimeMillis();
        if (tvCursorIdleHidden) {
            tvCursorIdleHidden = false;
            updateTvCursorVisibility();
        }
        tvCursorOnAppBar = isTvCursorOnAppBar();
        if (tvCursorOnAppBar) noteTvAppBarUse();
    }

    /**
     * Geometry of the browser bar, independent of its visibility: the pointer resting on the
     * hidden bar has to bring it back, so the bounds have to be known while it is not shown.
     * The area is the bar row itself plus a small margin on the top and on the sides.
     *
     * TV: the free strip below the bar down to the screen edge is deliberately not part of
     * the bar. That strip is the bottom scroll edge - pushing the pointer there to scroll
     * must neither wake the bar nor keep it awake.
     */
    private boolean isTvCursorOnAppBar() {
        if (appBar_buttons == null || appBar_buttons.getWidth() == 0) return false;
        int[] location = new int[2];
        appBar_buttons.getLocationOnScreen(location);
        float margin = HelperUnit.convertDpToPixel(8, context);
        return tvCursorX >= location[0] - margin
                && tvCursorX <= location[0] + appBar_buttons.getWidth() + margin
                && tvCursorY >= location[1] - margin
                && tvCursorY <= location[1] + appBar_buttons.getHeight();
    }

    /**
     * Stops the smooth pointer movement and forgets all held directions. Called whenever the
     * pointer is hidden (focus lost, dialog opened, activity paused): the key up event of a
     * direction may never arrive in those cases, and a stuck direction would make the
     * pointer drift over the screen.
     */
    private void stopTvCursorMovement() {
        tvCursorDirections = 0;
        tvCursorMoving = false;
        tvCursorHandler.removeCallbacks(tvCursorMoveRunnable);
    }

    /**
     * Key handling of the web view while browsing: the four directions move the pointer,
     * OK/Enter is the click. Everything else (typing, Back, ...) is untouched.
     */
    private boolean handleTvCursorKey(int keyCode, KeyEvent event) {
        if (tvCursor == null) return false;
        final int action = event.getAction();
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT: {
                int direction;
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) direction = TV_CURSOR_UP;
                else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) direction = TV_CURSOR_DOWN;
                else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) direction = TV_CURSOR_LEFT;
                else direction = TV_CURSOR_RIGHT;

                if (action == KeyEvent.ACTION_DOWN) {
                    // Also called for the repeated events of a held key (every ~50 ms); the
                    // direction is simply kept pressed.
                    // TV: moving the pointer while the button is held is a drag, not a menu:
                    // the first direction cancels the pending long-press timeout and the
                    // existing drag path below continues unchanged.
                    if (tvCursorPressed) cancelTvLongPress();
                    boolean wasIdle = tvCursorDirections == 0;
                    tvCursorDirections |= direction;
                    if (wasIdle) {
                        tvCursorMoveStart = SystemClock.uptimeMillis();
                        // One frame of head start so that even a very short tap moves the
                        // pointer a little bit instead of doing nothing.
                        tvCursorMoveLast = tvCursorMoveStart - 16;
                    }
                    // The repeats double as a watchdog: if the loop ever stopped while the
                    // key is still held (lost key up after a focus change), it is started
                    // again instead of leaving the pointer dead.
                    if (!tvCursorMoving) {
                        tvCursorMoving = true;
                        tvCursorHandler.removeCallbacks(tvCursorMoveRunnable);
                        tvCursorHandler.post(tvCursorMoveRunnable);
                    }
                } else if (action == KeyEvent.ACTION_UP) {
                    tvCursorDirections &= ~direction;
                    if (tvCursorDirections == 0) {
                        tvCursorMoving = false;
                        tvCursorHandler.removeCallbacks(tvCursorMoveRunnable);
                    }
                }
                return true;
            }
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER: {
                // While the on-screen keyboard is up, all center/Enter keys (D-pad center,
                // Enter, Numpad Enter) belong to the keyboard / the page - confirming typed
                // text, operating the IME - and must not be turned into a click or a menu.
                if (tvCursorImeVisible) return false;
                if (action == KeyEvent.ACTION_DOWN) {
                    // Key repeats of a held button must not restart the press (and with it
                    // the long-press timeout), only the first ACTION_DOWN counts.
                    if (event.getRepeatCount() == 0) pressTvCursor();
                } else if (action == KeyEvent.ACTION_UP) {
                    // A short press stays a normal click: the pending long press is dropped
                    // before the button is released. After a fired long press the press was
                    // already cancelled, so nothing is dispatched here and the release
                    // cannot click through.
                    cancelTvLongPress();
                    releaseTvCursor();
                    tvLongPressFired = false;
                }
                return true;
            }
            default:
                return false;
        }
    }

    private void moveTvCursor(float dx, float dy) {
        if (tvCursor == null || ninjaWebView == null) return;
        float edge = HelperUnit.convertDpToPixel(4, context);
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        tvCursorX = Math.max(edge, Math.min(screenWidth - edge, tvCursorX + dx));
        tvCursorY = Math.max(edge, Math.min(screenHeight - edge, tvCursorY + dy));
        updateTvCursorPosition();
        // TV: moving the pointer counts as using it: it wakes the pointer up and, while it is
        // over the browser bar, keeps the bar on screen.
        noteTvPointerActivity();
        // While the button is held, moving the pointer is a drag: the page must get the
        // move events. Without a pressed button only the hover position is updated (and
        // that is throttled, 60 hover events per second are not needed).
        if (tvCursorPressed) {
            dispatchTvCursorTouch(MotionEvent.ACTION_MOVE);
        } else {
            long now = SystemClock.uptimeMillis();
            if (now - tvCursorHoverLast >= 60) {
                tvCursorHoverLast = now;
                dispatchTvCursorHover();
            }
        }
        updateTvCursorScroll();
    }

    /**
     * 60 fps movement loop while at least one direction key is held. The speed grows with
     * the time the direction is held (mouse acceleration), so the pointer can be moved
     * precisely with short taps and quickly across the screen with a long press.
     */
    private final Runnable tvCursorMoveRunnable = new Runnable() {
        @Override
        public void run() {
            long now = SystemClock.uptimeMillis();
            if (tvCursorDirections == 0 || !tvCursorAllowed()) {
                tvCursorDirections = 0;
                tvCursorMoving = false;
                return;
            }
            tvCursorMoving = true;
            // Frame time in seconds; clamped so a stalled frame cannot teleport the pointer.
            float dt = Math.min((now - tvCursorMoveLast) / 1000f, 0.05f);
            tvCursorMoveLast = now;
            // 300 dp/s when the direction is first pressed, up to 1400 dp/s after ~0.7 s.
            float held = Math.min(1f, (now - tvCursorMoveStart) / 700f);
            float speed = HelperUnit.convertDpToPixel(300f + 1100f * held, context);

            float dx = 0;
            float dy = 0;
            if ((tvCursorDirections & TV_CURSOR_LEFT) != 0) dx -= 1;
            if ((tvCursorDirections & TV_CURSOR_RIGHT) != 0) dx += 1;
            if ((tvCursorDirections & TV_CURSOR_UP) != 0) dy -= 1;
            if ((tvCursorDirections & TV_CURSOR_DOWN) != 0) dy += 1;
            if (dx != 0 && dy != 0) {
                // Diagonal movement must not be faster than straight movement.
                dx *= 0.7071f;
                dy *= 0.7071f;
            }
            moveTvCursor(dx * speed * dt, dy * speed * dt);
            tvCursorHandler.postDelayed(this, 16);
        }
    };

    /**
     * Places the pointer view so that its middle sits exactly at tvCursorX / tvCursorY.
     * The view is a child of the decor view (see initTvCursor), position and padding of the
     * current parent have to be subtracted from the screen position.
     */
    private void updateTvCursorPosition() {
        if (tvCursor == null) return;
        View parent = tvCursor.getParent() instanceof View ? (View) tvCursor.getParent() : null;
        if (parent == null) return;
        int[] location = new int[2];
        parent.getLocationOnScreen(location);
        float halfWidth = tvCursor.getWidth() > 0 ? tvCursor.getWidth() / 2f : HelperUnit.convertDpToPixel(12, context);
        float halfHeight = tvCursor.getHeight() > 0 ? tvCursor.getHeight() / 2f : HelperUnit.convertDpToPixel(12, context);
        tvCursor.setX(tvCursorX - location[0] - parent.getPaddingLeft() - halfWidth);
        tvCursor.setY(tvCursorY - location[1] - parent.getPaddingTop() - halfHeight);
    }

    private void pressTvCursor() {
        if (tvCursorPressed || tvCursor == null || ninjaWebView == null) return;
        tvCursorPressed = true;
        tvLongPressFired = false;
        // TV: holding the button opens the page actions menu after the system long-press
        // timeout (see tvLongPressRunnable). The runnable is posted before the ACTION_DOWN
        // is dispatched: the page and the views under the pointer start their own
        // long-press timers when they receive the ACTION_DOWN, and this one has to be
        // ahead of them in the queue so that its ACTION_CANCEL suppresses all of them and
        // the menu opens only once, from tvLongPressRunnable.
        cancelTvLongPress();
        tvLongPressPending = true;
        tvCursorHandler.postDelayed(tvLongPressRunnable, ViewConfiguration.getLongPressTimeout());
        // TV: a click counts as using the remote. The call also wakes the pointer and reveals
        // the browser bar when the click is aimed at it - the touch is sent afterwards, so it
        // already hits the visible bar.
        noteTvPointerActivity();
        tvCursor.animate().scaleX(0.7f).scaleY(0.7f).setDuration(60).start();
        // No scrolling while the button is down, otherwise selecting text near an edge
        // would fight against the edge scrolling.
        tvCursorScrollDelta = 0;
        tvCursorHandler.removeCallbacks(tvCursorScrollRunnable);
        dispatchTvCursorTouch(MotionEvent.ACTION_DOWN);
    }

    private void releaseTvCursor() {
        if (!tvCursorPressed) return;
        tvCursorPressed = false;
        dispatchTvCursorTouch(MotionEvent.ACTION_UP);
        if (tvCursor != null) tvCursor.animate().scaleX(1f).scaleY(1f).setDuration(90).start();
        updateTvCursorScroll();
    }

    /**
     * TV: holding the OK/Enter button for the system long-press timeout opens the current
     * page's actions menu - the same overflow menu the menu button shows - at any pointer
     * position. The synthetic touch is cancelled first (ACTION_CANCEL); the runnable was
     * posted before the ACTION_DOWN was dispatched, so it runs ahead of every long-press
     * timer that the touch started (page, browser bar, FABs) and cancels them: the menu
     * opens exactly once, from here. The release of the key afterwards is swallowed as
     * well (cancelTvCursorPress already cleared tvCursorPressed, so releaseTvCursor
     * dispatches nothing): no click-through into the page behind the menu.
     * Runs at most once per press (tvLongPressFired).
     */
    private final Runnable tvLongPressRunnable = new Runnable() {
        @Override
        public void run() {
            tvLongPressPending = false;
            if (!tvCursorPressed || tvLongPressFired || ninjaWebView == null) return;
            tvLongPressFired = true;
            cancelTvCursorPress();
            showOverflow(null, null, 0, ninjaWebView.getTitle(), ninjaWebView.getUrl(), null, null, 0);
        }
    };

    /**
     * Drops a pending long-press timeout without opening the menu. Called when the key is
     * released (short press = normal click), when a drag starts (direction key while the
     * button is held), when the window loses the focus and on destroy.
     */
    private void cancelTvLongPress() {
        tvLongPressPending = false;
        tvCursorHandler.removeCallbacks(tvLongPressRunnable);
    }

    /**
     * Cancels a held click without a real button release. Needed when a dialog opens in the
     * middle of the press: the dialog takes the key events, so the following ACTION_UP never
     * reaches the web view and the press would stay stuck.
     */
    private void cancelTvCursorPress() {
        cancelTvLongPress();
        if (!tvCursorPressed) return;
        tvCursorPressed = false;
        tvCursorScrollDelta = 0;
        tvCursorHandler.removeCallbacks(tvCursorScrollRunnable);
        dispatchTvCursorTouch(MotionEvent.ACTION_CANCEL);
        if (tvCursor != null) tvCursor.animate().scaleX(1f).scaleY(1f).setDuration(90).start();
    }

    private void dispatchTvCursorTouch(int action) {
        if (ninjaWebView == null) return;
        View decorView = getWindow().getDecorView();
        int[] location = new int[2];
        decorView.getLocationOnScreen(location);
        long now = SystemClock.uptimeMillis();
        // All events of one press share the same down time, like real input events.
        if (action == MotionEvent.ACTION_DOWN) tvCursorDownTime = now;
        MotionEvent motionEvent = MotionEvent.obtain(tvCursorDownTime, now, action, tvCursorX - location[0], tvCursorY - location[1], 0);
        decorView.dispatchTouchEvent(motionEvent);
        motionEvent.recycle();
    }

    /**
     * Tells the page (and the other views) where the mouse is without pressing anything:
     * this is what makes CSS ":hover" work on desktop style pages.
     */
    private void dispatchTvCursorHover() {
        if (ninjaWebView == null || !ninjaWebView.hasWindowFocus()) return;
        View decorView = getWindow().getDecorView();
        int[] location = new int[2];
        decorView.getLocationOnScreen(location);
        long now = SystemClock.uptimeMillis();
        MotionEvent motionEvent = MotionEvent.obtain(now, now, MotionEvent.ACTION_HOVER_MOVE, tvCursorX - location[0], tvCursorY - location[1], 0);
        motionEvent.setSource(InputDevice.SOURCE_MOUSE);
        decorView.dispatchGenericMotionEvent(motionEvent);
        motionEvent.recycle();
    }

    /**
     * Edge scrolling: while the pointer sits directly on the top or on the bottom screen
     * edge, the page scrolls in that direction.
     *
     * TV: the bottom band starts at the bottom edge of the browser bar (normally the
     * overscan strip below it) and ends at the screen edge. Only that free strip scrolls -
     * the bar and its buttons keep working normally and hovering them can never start a
     * scroll. (The band used to start above the bar and began scrolling far too early.)
     *
     * @return the scroll step for one tick, 0 when no edge is active
     */
    private int tvCursorScrollStep() {
        if (tvCursor == null || ninjaWebView == null
                || !ninjaWebView.hasFocus() || !ninjaWebView.hasWindowFocus()
                || tvCursorPressed) {
            return 0;
        }
        View root = findViewById(R.id.main);
        int[] location = new int[2];
        root.getLocationOnScreen(location);
        float contentTop = location[1] + root.getPaddingTop();
        float zone = Math.max(HelperUnit.convertDpToPixel(80, context),
                getResources().getDisplayMetrics().heightPixels * 0.13f);
        int step = HelperUnit.convertDpToPixel(22, context);

        if (tvCursorY <= contentTop + zone) return -step;

        // TV: only the very bottom screen edge scrolls - the strip below the browser bar.
        // Without a laid out bar (the first frames after start) a small band at the screen
        // edge is used instead.
        float screenBottom = getResources().getDisplayMetrics().heightPixels;
        float stripTop = screenBottom - Math.max(HelperUnit.convertDpToPixel(64, context),
                screenBottom * 0.06f);
        View bar = findViewById(R.id.appBar_buttons);
        if (bar != null && bar.getWidth() > 0) {
            int[] barLocation = new int[2];
            bar.getLocationOnScreen(barLocation);
            stripTop = Math.max(stripTop, barLocation[1] + bar.getHeight());
        }
        return tvCursorY >= stripTop ? step : 0;
    }

    private void updateTvCursorScroll() {
        int step = tvCursorScrollStep();
        // Called on every frame of the movement loop: only restart the timer when the edge
        // state really changes, otherwise a running scroll would be cancelled and re-posted
        // on every frame and the page would not scroll at all.
        if (step == tvCursorScrollDelta) return;
        tvCursorScrollDelta = step;
        tvCursorHandler.removeCallbacks(tvCursorScrollRunnable);
        if (step != 0) tvCursorHandler.postDelayed(tvCursorScrollRunnable, 70);
    }

    private final Runnable tvCursorScrollRunnable = new Runnable() {
        @Override
        public void run() {
            int step = tvCursorScrollStep();
            if (step == 0 || ninjaWebView == null) {
                tvCursorScrollDelta = 0;
                return;
            }
            dispatchTvCursorWheel(step < 0);
            tvCursorHandler.postDelayed(this, 70);
        }
    };

    /**
     * Scrolls the page under the pointer like a mouse wheel does. WebView.scrollBy() does
     * not scroll the web page in current WebView versions - the page is scrolled by
     * Chromium, not by the view (that was the reason why edge scrolling did nothing). A
     * real wheel event (ACTION_SCROLL with AXIS_VSCROLL, source mouse) is sent to the view
     * under the pointer instead, exactly what a mouse with a wheel would produce.
     *
     * @param up true scrolls the page up (towards the beginning), false scrolls down.
     */
    private void dispatchTvCursorWheel(boolean up) {
        if (ninjaWebView == null) return;
        View decorView = getWindow().getDecorView();
        int[] location = new int[2];
        decorView.getLocationOnScreen(location);
        long now = SystemClock.uptimeMillis();

        // MotionEvent.setAxisValue() is not public API, the scroll amount is part of the
        // pointer coordinates of the event.
        MotionEvent.PointerProperties properties = new MotionEvent.PointerProperties();
        properties.id = 0;
        properties.toolType = MotionEvent.TOOL_TYPE_MOUSE;
        MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
        coords.x = tvCursorX - location[0];
        coords.y = tvCursorY - location[1];
        // Like a mouse wheel: a positive axis value scrolls up (the page content moves down).
        coords.setAxisValue(MotionEvent.AXIS_VSCROLL, up ? 1f : -1f);

        MotionEvent motionEvent = MotionEvent.obtain(now, now, MotionEvent.ACTION_SCROLL, 1,
                new MotionEvent.PointerProperties[]{properties}, new MotionEvent.PointerCoords[]{coords},
                0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_MOUSE, Display.DEFAULT_DISPLAY);
        decorView.dispatchGenericMotionEvent(motionEvent);
        motionEvent.recycle();
    }


    private void handleFinalSearch(String query) {
        if (!query.isEmpty() && !query.equals(ninjaWebView.getUrl())) {
            hideSearch();
            ninjaWebView.loadUrl(query);
        } else {
            NinjaToast.show(this, R.string.toast_input_empty);
        }
    }

    @SuppressLint({"UnsafeOptInUsageError"})
    public void updateOmniBox() {
        // TV: updateOmniBox() is also called while the first tab is still being created, the
        // static web view reference is null until then. The bookmark check NPE'd on every
        // start (caught, but logged as an error).
        if (overViewTab.equals(getString(R.string.album_title_bookmarks)) && ninjaWebView != null) {
            try {
                RecordAction action = new RecordAction(context);
                action.open(true);
                if (action.checkUrl(ninjaWebView.getUrl(), RecordUnit.TABLE_BOOKMARK)) fab_overview.setImageResource(R.drawable.icon_bookmark_added);
                else fab_overview.setImageResource(R.drawable.icon_bookmark);
                action.close();
            }
            catch (Exception e) {Log.i(TAG, "dialogCustomSearches:" + e);}
        }

        searchOnSiteLayout.setVisibility(GONE);
        // TV: a page change / tab switch is a use of the bar: it shows the bar and keeps it
        // for another 15 s.
        noteTvAppBarUse();
        searchOnSiteInput.setText("");

        badgeDrawable.setVisible(BrowserContainer.size() > 1);
        badgeDrawable.setNumber(BrowserContainer.size());
        BadgeUtils.attachBadgeDrawable(badgeDrawable, fab_overview);
        bottom_navigation.getOrCreateBadge(R.id.page_0).setNumber(BrowserContainer.size());

        ninjaWebView = (NinjaWebView) currentAlbumController;
        if (ninjaWebView == null) return;
        String url = ninjaWebView.getUrl();
        ninjaWebView.initPreferences(url);
        saveOpenedTabs();

        if (url != null && ninjaWebView.isForeground()) {
            progressBar.setVisibility(GONE);
            setProfileIcon(fab_menu, url);
            if (Objects.requireNonNull(ninjaWebView.getTitle()).isEmpty()) appBar_title.setText(url);
            else appBar_title.setText(ninjaWebView.getTitle());
            FaviconHelper.setFavicon(context, contentView, url, R.id.menu_icon, R.drawable.icon_image_broken);
            TextView overflowURL = findViewById(R.id.appbar_URL);
            overflowURL.setText(url);
            HelperUnit.setHighLightedText(context, overflowURL, url, HelperUnit.domain(url));
        }
        // TV: switching tabs / loading pages can change what has the focus, keep the
        // remote mouse pointer in sync with it.
        updateTvCursorVisibility();
    }

    private void initSearchOnSite () {
        searchOnSiteLayout = findViewById(R.id.searchOnSiteLayout);
        searchOnSiteInput = findViewById(R.id.searchOnSite_input);
        Button searchOnSite_buttonClose = findViewById(R.id.searchOnSite_buttonClose);
        TextInputLayout searchOnSite_textField  = findViewById(R.id.searchOnSite_textField);
        assert searchOnSite_textField != null;
        searchOnSite_buttonClose.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(searchOnSiteInput.getText())) searchOnSiteInput.setText("");
            else {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchOnSiteInput.getWindowToken(), 0);
                searchOnSiteLayout.setVisibility(GONE);
                noteTvAppBarUse();
            }
        });
        searchOnSite_textField.setStartIconOnClickListener(v -> ((NinjaWebView) currentAlbumController).findNext(false));
        searchOnSite_textField.setEndIconOnClickListener(v -> ((NinjaWebView) currentAlbumController).findNext(true));
        searchOnSiteInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) { if (currentAlbumController != null) ((NinjaWebView) currentAlbumController).findAllAsync(s.toString()); }
        });
    }
    public void initSearch() {
        RecordAction action = new RecordAction(this);
        List<Record> list = action.listEntries(activity);
        adapterSearch = new AdapterSearch(this, R.layout.item_list, list);
        list_search.setAdapter(adapterSearch);
        list_search.setTextFilterEnabled(true);
        adapterSearch.notifyDataSetChanged();
        list_search.setSelection(adapter.getCount() - 1);
        list_search.setOnItemClickListener((parent, view, position, id) -> {
            hideSearch();
            String url = ((TextView) view.findViewById(R.id.dateView)).getText().toString();
            ninjaWebView.loadUrl(url);
        });
        list_search.setOnItemLongClickListener((adapterView, view, i, l) -> {
            String title = ((TextView) view.findViewById(R.id.titleView)).getText().toString();
            String url = ((TextView) view.findViewById(R.id.dateView)).getText().toString();
            showOverflow(dialogSearch, list_search, 2, title, url, null, null, 0);
            return true;
        });
    }

    private void showOverview() {
        initOverview();
        dialogOverview.show();
    }

    public void hideSearch() {
        dialogSearch.cancel();
        // TV: the dialog is only created when it is used, "cancel" on null logged an exception
        // on every start. A null check is enough here.
        if (dialogCustomSearches != null) dialogCustomSearches.cancel();
    }

    public void hideOverview() {
        dialogOverview.cancel();
    }

    private void setSelectedTab() {
        if (overViewTab.equals(getString(R.string.album_title_tab))) bottom_navigation.setSelectedItemId(R.id.page_0);
        else if (overViewTab.equals(getString(R.string.album_title_bookmarks))) bottom_navigation.setSelectedItemId(R.id.page_2);
        else if (overViewTab.equals(getString(R.string.album_title_history))) bottom_navigation.setSelectedItemId(R.id.page_3);
    }

    public void hideOverflow () {
        try {dialog_overflow.cancel();} catch (Exception e) {Log.i(TAG, "Overflow already closed:" + e);}
    }

    private List<MenuItem> loadSelectedFromStorage() {
        SharedPreferences prefs = getSharedPreferences(Settings_Menu.PREF_NAME, Context.MODE_PRIVATE);
        String jsonString = prefs.getString(Settings_Menu.KEY_LIST, null);
        List<MenuItem> selected = new ArrayList<>();

        if (jsonString != null) {
            try {
                org.json.JSONArray jsonArray = new org.json.JSONArray(jsonString);
                for (int i = 0; i < jsonArray.length(); i++) {
                    org.json.JSONObject jsonObject = jsonArray.getJSONObject(i);
                    if (jsonObject.optBoolean("isSelected", true)) {
                        selected.add(new MenuItem(
                                jsonObject.optString("title", ""),
                                jsonObject.optInt("iconResId", 0),
                                true
                        ));
                    }
                }
            } catch (org.json.JSONException e) {
                android.util.Log.e("Storage", "Fehler beim Laden der Selektionen", e);
            }
        }
        return selected;
    }

    public void removeItemByName(String name, List<MenuItem> selectedItemsList, AdapterMenu adapter) {
        int indexToRemove = -1;
        // 1. Position des Elements in der aktuellen Grid-Liste finden
        for (int i = 0; i < selectedItemsList.size(); i++) {
            if (selectedItemsList.get(i).getTitle().equalsIgnoreCase(name)) {
                indexToRemove = i;
                break;
            }
        }
        // Wenn das Element im aktuellen Grid existiert
        if (indexToRemove != -1) {
            // 2. Aus der Liste für die Anzeige entfernen
            selectedItemsList.remove(indexToRemove);
            // 3. Den Adapter über das Entfernen informieren (zeigt eine schöne Animation)
            adapter.notifyItemRemoved(indexToRemove);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public void showOverflow(Dialog dialog, View view, int hideMenu, String title, String url, final AdapterRecord adapterRecord, List<Record> recordList, int location) {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = View.inflate(context, R.layout.dialog_menu_overflow, null);

        LinearLayout textGroup = dialogView.findViewById(R.id.textGroup);
        TextView overflowURL = dialogView.findViewById(R.id.overflowURL);
        overflowURL.setText(url);
        HelperUnit.setHighLightedText(context, overflowURL, url, HelperUnit.domain(url));
        TextView menuTitle = dialogView.findViewById(R.id.overflowTitle);
        menuTitle.setText(title);
        textGroup.setOnClickListener(v -> {
            // 1. Die Root-View deiner aktuellen Activity holen (als Parent für die Snackbar)
            // View rootView = findViewById(android.R.id.content);
            // 2. Optional: Eine View bestimmen, ÜBER der die Snackbar schweben soll (z.B. eine Bottom-Navigation)
            // Wenn sie ganz normal unten am Bildschirmrand kleben soll, übergib hier einfach 'null'.
            HelperUnit.showCustomSnackbarWithTwoActions(
                    this, dialogView, null,
                    title, "", url,
                    R.drawable.icon_share, () -> {
                        dialog_overflow.cancel();
                        hideOverview();
                        shareLink(title, url);
                        return true;
                    },
                    R.drawable.icon_close, () -> true
            );
        });

        String jsonReceived = getIntent().getStringExtra("SELECTED_DATA");
        List<MenuItem> selectedItemsList = new ArrayList<>();

        if (jsonReceived != null) {
            try {
                org.json.JSONArray jsonArray = new org.json.JSONArray(jsonReceived);
                for (int i = 0; i < jsonArray.length(); i++) {
                    org.json.JSONObject jsonObject = jsonArray.getJSONObject(i);
                    selectedItemsList.add(new MenuItem(
                            jsonObject.optString("title", ""),
                            jsonObject.optInt("iconResId", 0),
                            jsonObject.optBoolean("isSelected", true)
                    ));
                }
            } catch (org.json.JSONException e) {
                android.util.Log.e("Intent", "Fehler beim Entpacken der Intent-Daten", e);
                selectedItemsList = loadSelectedFromStorage();
            }
        } else {
            selectedItemsList = loadSelectedFromStorage();
        }

        RecyclerView recyclerView = dialogView.findViewById(R.id.recyclerViewGrid);
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        }

        for (MenuItem item : selectedItemsList) {
            String aktuellerTitel = item.getTitle();
            if (aktuellerTitel == null) continue;
            if (aktuellerTitel.equals(getString(R.string.menu_openFav))) {
                item.setIconResId(R.drawable.icon_fav);
            } else if (aktuellerTitel.equals(getString(R.string.main_menu_new_tabOpen))) {
                item.setIconResId(R.drawable.icon_tab_plus);
            } else if (aktuellerTitel.equals(getString(R.string.main_menu_new_tab))) {
                item.setIconResId(R.drawable.icon_tab_background);
            } else if (aktuellerTitel.equals(getString(R.string.menu_other_searchSite))) {
                item.setIconResId(R.drawable.icon_search_site);
            } else if (aktuellerTitel.equals(getString(R.string.menu_closeTab))) {
                item.setIconResId(R.drawable.icon_tab_remove);
            } else if (aktuellerTitel.equals(getString(R.string.menu_reload))) {
                item.setIconResId(R.drawable.icon_refresh);
            } else if (aktuellerTitel.equals(getString(R.string.menu_share_link))) {
                item.setIconResId(R.drawable.icon_share);
            } else if (aktuellerTitel.equals(getString(R.string.menu_shareClipboard))) {
                item.setIconResId(R.drawable.icon_clipboard);
            } else if (aktuellerTitel.equals(getString(R.string.dialog_postOnWebsite))) {
                item.setIconResId(R.drawable.icon_post);
            } else if (aktuellerTitel.equals(getString(R.string.menu_save_bookmark))) {
                item.setIconResId(R.drawable.icon_bookmark);
            } else if (aktuellerTitel.equals(getString(R.string.menu_save_pdf))) {
                item.setIconResId(R.drawable.icon_file);
            } else if (aktuellerTitel.equals(getString(R.string.menu_save_as))) {
                item.setIconResId(R.drawable.icon_menu_save);
            } else if (aktuellerTitel.equals(getString(R.string.menu_shareOpenWith))) {
                item.setIconResId(R.drawable.icon_share_open_with);
            } else if (aktuellerTitel.equals(getString(R.string.menu_sc))) {
                item.setIconResId(R.drawable.icon_home);
            } else if (aktuellerTitel.equals(getString(R.string.menu_download))) {
                item.setIconResId(R.drawable.icon_download);
            } else if (aktuellerTitel.equals(getString(R.string.menu_quit))) {
                item.setIconResId(R.drawable.icon_close);
            } else if (aktuellerTitel.equals(getString(R.string.setting_label))) {
                item.setIconResId(R.drawable.icon_settings);
            } else if (aktuellerTitel.equals(getString(R.string.menu_restart))) {
                item.setIconResId(R.drawable.icon_restart);
            } else if (aktuellerTitel.equals(getString(R.string.menu_edit))) {
                item.setIconResId(R.drawable.icon_edit);
            } else if (aktuellerTitel.equals(getString(R.string.menu_delete))) {
                item.setIconResId(R.drawable.icon_delete);
            } else if (aktuellerTitel.equals(getString(R.string.menu_delete_entry))) {
                item.setIconResId(R.drawable.icon_delete_alt);
            } else if (aktuellerTitel.equals(getString(R.string.app_help))) {
                item.setIconResId(R.drawable.icon_help);
            } else if (aktuellerTitel.equals(getString(R.string.menu_fav))) {
                item.setIconResId(R.drawable.icon_fav_plus);
            }
        }


        @SuppressLint("NonConstantResourceId")
        AdapterMenu adapter = new AdapterMenu(selectedItemsList, item -> {

            String favURL = Objects.requireNonNull(sp.getString("favoriteURL", "https://codeberg.org/Gaukler_Faun/FOSS_Browser/wiki"));
            RecordAction action = new RecordAction(context);

            dialog_overflow.cancel();

            int resId = item.getIconResId();

            if (resId == R.drawable.icon_fav) {
                ninjaWebView.loadUrl(favURL);
            } else if (resId == R.drawable.icon_tab_plus) {
                if (hideMenu == 2) { dialog.cancel(); }
                addAlbum(HelperUnit.domain(url), url, true);
            } else if (resId == R.drawable.icon_tab_background) {
                addAlbum(HelperUnit.domain(url), url, false);
            } else if (resId == R.drawable.icon_refresh) {
                ninjaWebView.reload();
            } else if (resId == R.drawable.icon_tab_remove) {
                removeAlbum(currentAlbumController);
                if (BrowserContainer.size() < 2) {
                    hideOverview();
                }
            } else if (resId == R.drawable.icon_close) {
                doubleTapsQuit();
            } else if (resId == R.drawable.icon_bookmark) {
                saveBookmark(title, url);
            } else if (resId == R.drawable.icon_file) {
                PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                PrintDocumentAdapter printAdapter = ninjaWebView.createPrintDocumentAdapter(title);
                Objects.requireNonNull(printManager).print(title, printAdapter, new PrintAttributes.Builder().build());
                sp.edit().putBoolean("pdf_create", true).apply();
            } else if (resId == R.drawable.icon_menu_save) {
                assert url != null;
                NinjaDownloadListener.saveAs(activity, url, null, dialog_overflow);
            } else if (resId == R.drawable.icon_fav_plus) {
                sp.edit().putString("favoriteURL", url).apply();
                NinjaToast.show(this, R.string.app_done);
            } else if (resId == R.drawable.icon_share) {
                shareLink(title, url);
            } else if (resId == R.drawable.icon_post) {
                String text = title + ": " + url;
                postLink(text, dialog_overflow);
            } else if (resId == R.drawable.icon_clipboard) {
                copyLink(url);
            } else if (resId == R.drawable.icon_share_open_with) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                context.startActivity(Intent.createChooser(intent, null));
            } else if (resId == R.drawable.icon_home) {
                HelperUnit.createShortcut(context, title, url);
            } else if (resId == R.drawable.icon_search_site) {
                searchOnSite();
            } else if (resId == R.drawable.icon_download) {
                startActivity(Intent.createChooser(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS), null));
            } else if (resId == R.drawable.icon_settings) {
                Intent settings = new Intent(BrowserActivity.this, Settings_Activity.class);
                startActivity(settings);
            } else if (resId == R.drawable.icon_restart) {
                triggerRebirth(context);
            } else if (resId == R.drawable.icon_help) {
                Uri webpage = Uri.parse("https://codeberg.org/Gaukler_Faun/FOSS_Browser/wiki/Home");
                BrowserUnit.intentURL(this, webpage);
            } else if (resId == R.drawable.icon_delete) {
                Snackbar snackbarSearch = Snackbar.make(view, R.string.hint_database, Snackbar.LENGTH_SHORT);
                HelperUnit.makeSnackbarRound(snackbarSearch);
                snackbarSearch.setAction(context.getString(R.string.app_ok), (v -> {
                    action.open(true);
                    action.deleteURL(url, RecordUnit.TABLE_START);
                    action.deleteURL(url, RecordUnit.TABLE_BOOKMARK);
                    action.deleteURL(url, RecordUnit.TABLE_HISTORY);
                    action.close();
                    initSearch();
                    String getText = Objects.requireNonNull(search_input.getText()).toString();
                    search_input.setText("");
                    search_input.setText(getText);
                    search_input.setSelection(getText.length());
                }));
                snackbarSearch.show();
            } else if (resId == R.drawable.icon_delete_alt) {
                Snackbar snackbarList = Snackbar.make(view, R.string.hint_database, Snackbar.LENGTH_SHORT);
                HelperUnit.makeSnackbarRound(snackbarList);
                snackbarList.setAction(context.getString(R.string.app_ok), (v -> {
                    Record record = recordList.get(location);
                    action.open(true);
                    if (overViewTab.equals(getString(R.string.album_title_bookmarks))) action.deleteURL(record.getURL(), RecordUnit.TABLE_BOOKMARK);
                    else if (overViewTab.equals(getString(R.string.album_title_history))) action.deleteURL(record.getURL(), RecordUnit.TABLE_HISTORY);
                    action.close();
                    recordList.remove(location);
                    adapterRecord.notifyDataSetChanged();
                    updateOmniBox();
                    dialog_overflow.cancel();
                }));
                snackbarList.show();
            } else if (resId == R.drawable.icon_edit) {

                MaterialAlertDialogBuilder builderSubMenuEdit = new MaterialAlertDialogBuilder(context);
                View dialogViewSubMenu = View.inflate(context, R.layout.dialog_edit, null);
                TextInputLayout editBottomLayout = dialogViewSubMenu.findViewById(R.id.editBottomLayout);
                TextInputLayout editTopLayout = dialogViewSubMenu.findViewById(R.id.editTopLayout);
                editBottomLayout.setHint(activity.getString(R.string.dialog_URL_hint));
                editTopLayout.setHint(activity.getString(R.string.dialog_title_hint));
                EditText editTop = dialogViewSubMenu.findViewById(R.id.editTop);
                EditText editBottom = dialogViewSubMenu.findViewById(R.id.editBottom);
                editTop.setText(title);
                editBottom.setText(url);
                MaterialCardView ib_icon = dialogViewSubMenu.findViewById(R.id.editIcon);
                ib_icon.setVisibility(VISIBLE);

                if (!overViewTab.equals(getString(R.string.album_title_bookmarks))) ib_icon.setVisibility(GONE);
                ib_icon.setOnClickListener(v -> {
                    MaterialAlertDialogBuilder builderFilter = new MaterialAlertDialogBuilder(context);
                    View dialogViewFilter = View.inflate(context, R.layout.dialog_menu, null);
                    builderFilter.setView(dialogViewFilter);
                    builderFilter.setTitle(R.string.setting_filter);
                    builderFilter.setIcon(R.drawable.icon_filter);
                    AlertDialog dialogFilter = builderFilter.create();
                    dialogFilter.show();
                    HelperUnit.setupDialog(context, dialogFilter);
                    CardView cardView = dialogViewFilter.findViewById(R.id.item_CardViewItem);
                    cardView.setVisibility(GONE);

                    GridView menuEditFilter = dialogViewFilter.findViewById(R.id.menu_grid);
                    final List<GridItem> menuEditFilterList = new LinkedList<>();
                    sp.edit().putString("showFilterDialogX", "true").apply();
                    HelperUnit.addFilterItems(activity, menuEditFilterList);
                    GridAdapter menuEditFilterAdapter = new GridAdapter(context, menuEditFilterList);
                    menuEditFilter.setNumColumns(2);
                    menuEditFilter.setHorizontalSpacing(20);
                    menuEditFilter.setVerticalSpacing(20);
                    menuEditFilter.setAdapter(menuEditFilterAdapter);
                    menuEditFilterAdapter.notifyDataSetChanged();
                    menuEditFilter.setOnItemClickListener((parent, view2, position, id) -> {
                        newIcon = menuEditFilterList.get(position).getData();
                        HelperUnit.setFilterIcons(context, ib_icon, newIcon);
                        dialogFilter.cancel();
                    });
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            WRAP_CONTENT,
                            WRAP_CONTENT
                    );
                    params.setMargins(HelperUnit.convertDpToPixel(20f, context),
                            HelperUnit.convertDpToPixel(10f, context),
                            HelperUnit.convertDpToPixel(20f, context),
                            HelperUnit.convertDpToPixel(10f, context));
                    menuEditFilter.setLayoutParams(params);
                    dialogFilter.setOnCancelListener(dialogInterface -> sp.edit().putString("showFilterDialogX", "false").apply());
                });
                newIcon = recordList.get(location).getIconColor();
                HelperUnit.setFilterIcons(context, ib_icon, newIcon);

                builderSubMenuEdit.setTitle(R.string.menu_edit);
                builderSubMenuEdit.setIcon(R.drawable.icon_edit);
                builderSubMenuEdit.setView(dialogViewSubMenu);
                Dialog dialogSubMenuEdit = builderSubMenuEdit.create();
                dialogSubMenuEdit.show();
                HelperUnit.setupDialog(context, dialogSubMenuEdit);

                Button ib_cancel = dialogViewSubMenu.findViewById(R.id.editCancel);
                ib_cancel.setOnClickListener(v -> dialogSubMenuEdit.cancel());
                Button ib_ok = dialogViewSubMenu.findViewById(R.id.editOK);
                ib_ok.setOnClickListener(v -> {
                    action.open(true);
                    action.deleteURL(url, RecordUnit.TABLE_BOOKMARK);
                    action.deleteURL(editBottom.getText().toString(), RecordUnit.TABLE_BOOKMARK);
                    action.addBookmark(new Record(editTop.getText().toString(), editBottom.getText().toString(), 0, newIcon));
                    updateOmniBox();
                    NinjaToast.show(this, R.string.app_done);
                    action.close();
                    bottom_navigation.setSelectedItemId(R.id.page_2);
                    dialogSubMenuEdit.cancel();
                });

            }
        });
        recyclerView.setAdapter(adapter);
        if (!(hideMenu == 0)) {
            removeItemByName(getString(R.string.menu_openFav), selectedItemsList, adapter);
            removeItemByName(getString(R.string.menu_reload), selectedItemsList, adapter);
            removeItemByName(getString(R.string.menu_restart), selectedItemsList, adapter);
            removeItemByName(getString(R.string.menu_quit), selectedItemsList, adapter);
            removeItemByName(getString(R.string.menu_save_pdf), selectedItemsList, adapter);
            removeItemByName(getString(R.string.menu_other_searchSite), selectedItemsList, adapter);
            removeItemByName(getString(R.string.setting_label), selectedItemsList, adapter);
            removeItemByName(getString(R.string.menu_download), selectedItemsList, adapter);
            removeItemByName(getString(R.string.app_help), selectedItemsList, adapter);
            removeItemByName(getString(R.string.menu_closeTab), selectedItemsList, adapter);
        }
        if (hideMenu == 0) {
            //Main menu
            removeItemByName(getString(R.string.menu_delete), selectedItemsList, adapter);
            removeItemByName(getString(R.string.menu_delete_entry), selectedItemsList, adapter);
            removeItemByName(getString(R.string.menu_edit), selectedItemsList, adapter);
        } else if (hideMenu == 1) {
            //Long click
            removeItemByName(getString(R.string.menu_delete), selectedItemsList, adapter);
            removeItemByName(getString(R.string.menu_delete_entry), selectedItemsList, adapter);
            removeItemByName(getString(R.string.menu_edit), selectedItemsList, adapter);
        } else if (hideMenu == 2) {
            //List search
            removeItemByName(getString(R.string.menu_edit), selectedItemsList, adapter);
            removeItemByName(getString(R.string.menu_delete_entry), selectedItemsList, adapter);
        } else if (hideMenu == 3) {
            // Bookmark
            removeItemByName(getString(R.string.menu_delete), selectedItemsList, adapter);
            removeItemByName(getString(R.string.menu_save_bookmark), selectedItemsList, adapter);
        } else if (hideMenu == 4) {
            // History
            removeItemByName(getString(R.string.menu_delete), selectedItemsList, adapter);
            removeItemByName(getString(R.string.menu_edit), selectedItemsList, adapter);
        }

        builder.setView(dialogView);
        dialog_overflow = builder.create();

        FloatingActionButton buttonProfile = dialogView.findViewById(R.id.buttonProfile);
        setProfileIcon(buttonProfile, url);
        buttonProfile.setOnClickListener(v -> {
            showDialogFastToggle(title,url, fab_menu);
            dialog_overflow.cancel();
        });
        buttonProfile.setOnLongClickListener(v -> {
            sp.edit().putString("profile", "profileStandard").apply();
            setProfileIcon(buttonProfile, url);
            dialog_overflow.cancel();
            listStandard = new List_standard(context);
            if (!listStandard.isWhite(url)){
                ninjaWebView.reload();
            }
            return false;
        });

        List_standard listStandard = new List_standard(context);
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(androidx.appcompat.R.attr.colorError, typedValue, true);
        int color = typedValue.data;
        if (listStandard.isWhite(url)) {
            buttonProfile.getDrawable().mutate().setTint(color);
        }

        HelperUnit.setupDialog(context, dialog_overflow);
        FaviconHelper.setFavicon(context, dialogView, url, R.id.menu_icon, R.drawable.icon_image_broken);
        dialog_overflow.show();
    }

    public void showDialogFastToggle(String title, String url, FloatingActionButton floatingActionButton) {

        listStandard = new List_standard(context);
        ninjaWebView = (NinjaWebView) currentAlbumController;

        String profile;
        if (listStandard.isWhite(url)) {
            profile = HelperUnit.domain(url);
        } else {
            profile = sp.getString("profile", "profileStandard");
        }

        if (url != null) {
            MaterialAlertDialogBuilder builderFastToggle = new MaterialAlertDialogBuilder(context);
            View dialogViewFastToggle = View.inflate(context, R.layout.dialog_fast_toggle, null);
            builderFastToggle.setView(dialogViewFastToggle);
            AlertDialog dialogFastToggle = builderFastToggle.create();
            HelperUnit.setupDialog(context, dialogFastToggle);

            LinearLayout textGroup = dialogViewFastToggle.findViewById(R.id.textGroup);
            TextView overflowURL = dialogViewFastToggle.findViewById(R.id.textGroup_menuURL);
            overflowURL.setText(url);
            HelperUnit.setHighLightedText(context, overflowURL, url, HelperUnit.domain(url));
            TextView overflowTitle = dialogViewFastToggle.findViewById(R.id.textGroup_menuTitle);
            overflowTitle.setText(title);
            FaviconHelper.setFavicon(context, dialogViewFastToggle, url, R.id.menu_icon, R.drawable.icon_image_broken);
            textGroup.setOnClickListener(v ->
                    HelperUnit.showCustomSnackbarWithTwoActions(
                    this, dialogViewFastToggle, null,
                    title, "", url,
                    R.drawable.icon_share, () -> {
                        shareLink(title, url);
                        return true;
                    },
                    R.drawable.icon_close, () -> true
            ));

            FloatingActionButton buttonProfile = dialogViewFastToggle.findViewById(R.id.buttonProfile);
            setProfileIcon(buttonProfile, url);
            buttonProfile.setOnClickListener(v -> {
                String cat = "    ¯\\_(ツ)_/¯    ";
                Snackbar snackbar = Snackbar.make(dialogViewFastToggle, cat, Snackbar.LENGTH_LONG);
                HelperUnit.makeSnackbarRound(snackbar);
                snackbar.show();
            });
            buttonProfile.setOnLongClickListener(v -> {
                sp.edit().putString("profile", "profileStandard").apply();
                setProfileIcon(buttonProfile, url);
                dialogFastToggle.cancel();
                if (!listStandard.isWhite(url)){
                    ninjaWebView.reload();
                }
                return true;
            });

            Button ib_save = dialogViewFastToggle.findViewById(R.id.ib_save);
            Button ib_delete = dialogViewFastToggle.findViewById(R.id.ib_delete);

            if (listStandard.isWhite(url)) {
                ib_save.setVisibility(GONE);
                ib_delete.setVisibility(VISIBLE);
            } else {
                ib_save.setVisibility(VISIBLE);
                ib_delete.setVisibility(GONE);
            }

            RelativeLayout checkbox_reset = dialogViewFastToggle.findViewById(R.id.checkbox_reset);
            ImageView icon_standard = dialogViewFastToggle.findViewById(R.id.icon_standard);

            if (sp.getBoolean("sp_standard_always", true)) {
                icon_standard.setImageResource(R.drawable.icon_check);
            } else {
                icon_standard.setImageResource(R.drawable.icon_close);
            }

            if (sp.getBoolean("sp_standard_restart", true)) {
                icon_standard.setImageResource(R.drawable.icon_restart);
            }

            checkbox_reset.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(context, checkbox_reset);
                popupMenu.getMenuInflater().inflate(R.menu.menu_standard, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(menuItem -> {
                    if (menuItem.getItemId() == R.id.menu_standardAlways) {
                        sp.edit().putBoolean("sp_standard_always", true).apply();
                        sp.edit().putBoolean("sp_standard_restart", false).apply();
                        icon_standard.setImageResource(R.drawable.icon_check);
                    } else if (menuItem.getItemId() == R.id.menu_standardNever) {
                        sp.edit().putBoolean("sp_standard_always", false).apply();
                        sp.edit().putBoolean("sp_standard_restart", false).apply();
                        icon_standard.setImageResource(R.drawable.icon_close);
                    } else if (menuItem.getItemId() == R.id.menu_standardRestart) {
                        sp.edit().putBoolean("sp_standard_always", false).apply();
                        sp.edit().putBoolean("sp_standard_restart", true).apply();
                        icon_standard.setImageResource(R.drawable.icon_restart);
                    }
                    return true;
                });
                // Showing the popup menu
                popupMenu.show();
            });

            CheckBox checkbox_screenOn = dialogViewFastToggle.findViewById(R.id.checkbox_screenOn);
            checkbox_screenOn.setChecked(sp.getBoolean("sp_screenOn", false));
            checkbox_screenOn.setOnClickListener(v -> {
                sp.edit().putBoolean("sp_screenOn", checkbox_screenOn.isChecked()).apply();
                checkbox_screenOn.setChecked(sp.getBoolean("sp_screenOn", true));
                dialogFastToggle.cancel();
                triggerRebirth(context);
            });

            CheckBox checkbox_links = dialogViewFastToggle.findViewById(R.id.checkbox_links);
            checkbox_links.setChecked(sp.getBoolean("sp_tabBackground", false));
            checkbox_links.setOnClickListener(v -> {
                sp.edit().putBoolean("sp_tabBackground", checkbox_links.isChecked()).apply();
                checkbox_links.setChecked(sp.getBoolean("sp_tabBackground", true));
            });

            TextView titleViewSettings = dialogViewFastToggle.findViewById(R.id.titleViewSettings);
            String s = context.getString(R.string.app_name) + " " + context.getString(R.string.setting_label);
            titleViewSettings.setText(s);

            CheckBox checkbox_image = dialogViewFastToggle.findViewById(R.id.checkbox_image);
            checkbox_image.setChecked(sp.getBoolean(profile + "_images", false));
            checkbox_image.setOnClickListener(v -> {
                if (listStandard.isWhite(url)){
                    sp.edit().putBoolean(profile + "_images", checkbox_image.isChecked()).apply();
                }  else if (profile.equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    setProfileIcon(buttonProfile, url);
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_images", checkbox_image.isChecked()).apply();
                } else {
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_images", checkbox_image.isChecked()).apply();
                }
            });

            CheckBox checkbox_java = dialogViewFastToggle.findViewById(R.id.checkbox_java);
            checkbox_java.setChecked(sp.getBoolean(profile + "_javascript", false));
            checkbox_java.setOnClickListener(v -> {
                if (listStandard.isWhite(url)){
                    sp.edit().putBoolean(profile + "_javascript", checkbox_java.isChecked()).apply();
                } else if (NinjaWebView.getProfile().equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    setProfileIcon(buttonProfile, url);
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_javascript", checkbox_java.isChecked()).apply();
                } else {
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_javascript", checkbox_java.isChecked()).apply();
                }
            });

            CheckBox checkbox_javaPopUp = dialogViewFastToggle.findViewById(R.id.checkbox_javaPopUp);
            checkbox_javaPopUp.setChecked(sp.getBoolean(profile + "_javascriptPopUp", false));
            checkbox_javaPopUp.setOnClickListener(v -> {
                if (listStandard.isWhite(url)){
                    sp.edit().putBoolean(profile + "_javascriptPopUp", checkbox_javaPopUp.isChecked()).apply();
                } else if (NinjaWebView.getProfile().equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    setProfileIcon(buttonProfile, url);
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_javascriptPopUp", checkbox_javaPopUp.isChecked()).apply();
                } else {
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_javascriptPopUp", checkbox_javaPopUp.isChecked()).apply();
                }
            });

            CheckBox checkbox_cookies = dialogViewFastToggle.findViewById(R.id.checkbox_cookies);
            checkbox_cookies.setChecked(sp.getBoolean(profile + "_cookies", false));
            checkbox_cookies.setOnClickListener(v -> {
                if (listStandard.isWhite(url)){
                    sp.edit().putBoolean(profile + "_cookies", checkbox_cookies.isChecked()).apply();
                } else if (NinjaWebView.getProfile().equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    setProfileIcon(buttonProfile, url);
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_cookies", checkbox_cookies.isChecked()).apply();
                } else {
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_cookies", checkbox_cookies.isChecked()).apply();
                }
            });

            CheckBox checkbox_cookiesThirdParty = dialogViewFastToggle.findViewById(R.id.checkbox_cookiesThirdParty);
            checkbox_cookiesThirdParty.setChecked(sp.getBoolean(profile + "_cookiesThirdParty", false));
            checkbox_cookiesThirdParty.setOnClickListener(v -> {
                if (listStandard.isWhite(url)){
                    sp.edit().putBoolean(profile + "_cookiesThirdParty", checkbox_cookiesThirdParty.isChecked()).apply();
                }  else if (NinjaWebView.getProfile().equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    setProfileIcon(buttonProfile, url);
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_cookiesThirdParty", checkbox_cookiesThirdParty.isChecked()).apply();
                } else {
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_cookiesThirdParty", checkbox_cookiesThirdParty.isChecked()).apply();
                }
            });

            CheckBox checkbox_cookiesBanner = dialogViewFastToggle.findViewById(R.id.checkbox_cookiesBanner);
            checkbox_cookiesBanner.setChecked(sp.getBoolean(profile + "_deny_cookie_banners", true));
            checkbox_cookiesBanner.setOnClickListener(v -> {
                if (listStandard.isWhite(url)){
                    sp.edit().putBoolean(profile + "_deny_cookie_banners", checkbox_cookiesBanner.isChecked()).apply();
                } else if (NinjaWebView.getProfile().equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    setProfileIcon(buttonProfile, url);
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_deny_cookie_banners", checkbox_cookiesBanner.isChecked()).apply();
                } else {
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_deny_cookie_banners", checkbox_cookiesBanner.isChecked()).apply();
                }
            });

            CheckBox checkbox_fingerPrint = dialogViewFastToggle.findViewById(R.id.checkbox_fingerPrint);
            checkbox_fingerPrint.setChecked(sp.getBoolean(profile + "_fingerPrintProtection", true));
            checkbox_fingerPrint.setOnClickListener(v -> {
                if (listStandard.isWhite(url)){
                    sp.edit().putBoolean(profile + "_fingerPrintProtection", checkbox_fingerPrint.isChecked()).apply();
                } else if (NinjaWebView.getProfile().equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    setProfileIcon(buttonProfile, url);
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_fingerPrintProtection", checkbox_fingerPrint.isChecked()).apply();
                } else {
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_fingerPrintProtection", checkbox_fingerPrint.isChecked()).apply();
                }
            });

            CheckBox checkbox_adBlock = dialogViewFastToggle.findViewById(R.id.checkbox_adBlock);
            checkbox_adBlock.setChecked(sp.getBoolean(profile + "_adBlock", true));
            checkbox_adBlock.setOnClickListener(v -> {
                if (listStandard.isWhite(url)){
                    sp.edit().putBoolean(profile + "_adBlock", checkbox_adBlock.isChecked()).apply();
                }  else if (NinjaWebView.getProfile().equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    setProfileIcon(buttonProfile, url);
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_adBlock", checkbox_adBlock.isChecked()).apply();
                } else {
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_adBlock", checkbox_adBlock.isChecked()).apply();
                }
            });

            CheckBox checkbox_trackingURL = dialogViewFastToggle.findViewById(R.id.checkbox_trackingURL);
            checkbox_trackingURL.setChecked(sp.getBoolean(profile + "_trackingULS", true));
            checkbox_trackingURL.setOnClickListener(v -> {
                if (listStandard.isWhite(url)){
                    sp.edit().putBoolean(profile + "_trackingULS", checkbox_trackingURL.isChecked()).apply();
                }  else if (NinjaWebView.getProfile().equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    setProfileIcon(buttonProfile, url);
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_trackingULS", checkbox_trackingURL.isChecked()).apply();
                } else {
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_trackingULS", checkbox_trackingURL.isChecked()).apply();
                }
            });

            CheckBox checkbox_saveData = dialogViewFastToggle.findViewById(R.id.checkbox_saveData);
            checkbox_saveData.setChecked(sp.getBoolean(profile + "_saveData", true));
            checkbox_saveData.setOnClickListener(v -> {
                if (listStandard.isWhite(url)){
                    sp.edit().putBoolean(profile + "_saveData", checkbox_saveData.isChecked()).apply();
                }  else if (NinjaWebView.getProfile().equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    setProfileIcon(buttonProfile, url);
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_saveData", checkbox_saveData.isChecked()).apply();
                } else {
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_saveData", checkbox_saveData.isChecked()).apply();
                }
            });

            CheckBox checkbox_history = dialogViewFastToggle.findViewById(R.id.checkbox_history);
            checkbox_history.setChecked(sp.getBoolean(profile + "_saveHistory", true));
            checkbox_history.setOnClickListener(v -> {
                if (listStandard.isWhite(url)){
                    sp.edit().putBoolean(profile + "_saveHistory", checkbox_history.isChecked()).apply();
                }  else if (NinjaWebView.getProfile().equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    setProfileIcon(buttonProfile, url);
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_saveHistory", checkbox_history.isChecked()).apply();
                } else {
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_saveHistory", checkbox_history.isChecked()).apply();
                }
            });

            CheckBox checkbox_location = dialogViewFastToggle.findViewById(R.id.checkbox_location);
            checkbox_location.setChecked(sp.getBoolean(profile + "_location", false));
            checkbox_location.setOnClickListener(v -> {
                if (listStandard.isWhite(url)){
                    sp.edit().putBoolean(profile + "_location", checkbox_location.isChecked()).apply();
                } else if (NinjaWebView.getProfile().equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    setProfileIcon(buttonProfile, url);
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_location", checkbox_location.isChecked()).apply();
                } else {
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_location", checkbox_location.isChecked()).apply();
                }
            });

            CheckBox checkbox_mic = dialogViewFastToggle.findViewById(R.id.checkbox_mic);
            checkbox_mic.setChecked(sp.getBoolean(profile + "_microphone", false));
            checkbox_mic.setOnClickListener(v -> {
                if (listStandard.isWhite(url)){
                    sp.edit().putBoolean(profile + "_microphone", checkbox_mic.isChecked()).apply();
                }  else if (NinjaWebView.getProfile().equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    setProfileIcon(buttonProfile, url);
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_microphone", checkbox_mic.isChecked()).apply();
                } else {
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_microphone", checkbox_mic.isChecked()).apply();
                }
            });

            CheckBox checkbox_camera = dialogViewFastToggle.findViewById(R.id.checkbox_camera);
            checkbox_camera.setChecked(sp.getBoolean(profile + "_camera", false));
            checkbox_camera.setOnClickListener(v -> {
                if (listStandard.isWhite(url)){
                    sp.edit().putBoolean(profile + "_camera", checkbox_camera.isChecked()).apply();
                } else if (NinjaWebView.getProfile().equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    setProfileIcon(buttonProfile, url);
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_camera", checkbox_camera.isChecked()).apply();
                } else {
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_camera", checkbox_camera.isChecked()).apply();
                }
            });

            CheckBox checkbox_dom = dialogViewFastToggle.findViewById(R.id.checkbox_dom);
            checkbox_dom.setChecked(sp.getBoolean(profile + "_dom", false));
            checkbox_dom.setOnClickListener(v -> {
                if (listStandard.isWhite(url)){
                    sp.edit().putBoolean(profile + "_dom", checkbox_dom.isChecked()).apply();
                }  else if (NinjaWebView.getProfile().equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    setProfileIcon(buttonProfile, url);
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_dom", checkbox_dom.isChecked()).apply();
                } else {
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_dom", checkbox_dom.isChecked()).apply();
                }
            });

            RelativeLayout layout_nightView = dialogViewFastToggle.findViewById(R.id.layout_nightView);
            CheckBox checkbox_nightView = dialogViewFastToggle.findViewById(R.id.checkbox_nightView);
            int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if ((nightModeFlags == Configuration.UI_MODE_NIGHT_YES) && !sp.getString("sp_theme", "1").equals("2")) {
                layout_nightView.setVisibility(VISIBLE);
            } else  {
                layout_nightView.setVisibility(GONE);
            }
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                checkbox_nightView.setChecked(sp.getBoolean(profile + "_night", true));
                checkbox_nightView.setOnClickListener(v -> {
                    if (listStandard.isWhite(url)){
                        sp.edit().putBoolean(profile + "_night", checkbox_nightView.isChecked()).apply();
                    }  else if (NinjaWebView.getProfile().equals("profileStandard")) {
                        ninjaWebView.setProfileChanged();
                        setProfileIcon(buttonProfile, url);
                        sp.edit().putBoolean(NinjaWebView.getProfile() + "_night", checkbox_nightView.isChecked()).apply();
                    } else {
                        sp.edit().putBoolean(NinjaWebView.getProfile() + "_night", checkbox_nightView.isChecked()).apply();
                    }
                });
            }

            CheckBox checkbox_desktop = dialogViewFastToggle.findViewById(R.id.checkbox_desktop);
            checkbox_desktop.setChecked(sp.getBoolean(profile + "_desktop", false));
            checkbox_desktop.setOnClickListener(v -> {
                if (listStandard.isWhite(url)){
                    sp.edit().putBoolean(profile + "_desktop", checkbox_desktop.isChecked()).apply();
                }  else if (NinjaWebView.getProfile().equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    setProfileIcon(buttonProfile, url);
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_desktop", checkbox_desktop.isChecked()).apply();
                } else {
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_desktop", checkbox_desktop.isChecked()).apply();
                }
            });

            CheckBox checkbox_drm = dialogViewFastToggle.findViewById(R.id.checkbox_drm);
            checkbox_drm.setChecked(sp.getBoolean(profile + "_drm", true));
            checkbox_drm.setOnClickListener(v -> {
                if (listStandard.isWhite(url)){
                    sp.edit().putBoolean(profile + "_drm", checkbox_drm.isChecked()).apply();
                }  else if (NinjaWebView.getProfile().equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    setProfileIcon(buttonProfile, url);
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_drm", checkbox_drm.isChecked()).apply();
                } else {
                    sp.edit().putBoolean(NinjaWebView.getProfile() + "_drm", checkbox_drm.isChecked()).apply();
                }
            });

            ib_save.setOnClickListener(v -> {
                listStandard.removeDomain(HelperUnit.domain(url));
                listStandard.addDomain(HelperUnit.domain(url));
                String profileToSave = HelperUnit.domain(url);
                sp.edit()
                        .putBoolean(profileToSave + "_saveData", checkbox_saveData.isChecked())
                        .putBoolean(profileToSave + "_images", checkbox_image.isChecked())
                        .putBoolean(profileToSave + "_adBlock", checkbox_adBlock.isChecked())
                        .putBoolean(profileToSave + "_trackingULS", checkbox_trackingURL.isChecked())
                        .putBoolean(profileToSave + "_location", checkbox_location.isChecked())
                        .putBoolean(profileToSave + "_fingerPrintProtection", checkbox_fingerPrint.isChecked())
                        .putBoolean(profileToSave + "_cookies", checkbox_cookies.isChecked())
                        .putBoolean(profileToSave + "_cookiesThirdParty", checkbox_cookiesThirdParty.isChecked())
                        .putBoolean(profileToSave + "_deny_cookie_banners", checkbox_cookiesBanner.isChecked())
                        .putBoolean(profileToSave + "_javascript", checkbox_java.isChecked())
                        .putBoolean(profileToSave + "_javascriptPopUp", checkbox_javaPopUp.isChecked())
                        .putBoolean(profileToSave + "_saveHistory", checkbox_history.isChecked())
                        .putBoolean(profileToSave + "_camera", checkbox_camera.isChecked())
                        .putBoolean(profileToSave + "_microphone", checkbox_mic.isChecked())
                        .putBoolean(profileToSave + "_dom", checkbox_dom.isChecked())
                        .putBoolean(profileToSave + "_night", checkbox_nightView.isChecked())
                        .putBoolean(profileToSave + "_desktop", checkbox_desktop.isChecked()).apply();
                if (sp.getBoolean("sp_standard_always", true)) {
                    sp.edit().putString("profile", "profileStandard").apply();
                    setProfileIcon(buttonProfile, url);
                }
                setProfileIcon(buttonProfile, url);
                dialogFastToggle.cancel();
                ninjaWebView.reload();
            });

            ib_delete.setOnClickListener(view -> {
                listStandard.removeDomain(HelperUnit.domain(url));
                String profileToSave = HelperUnit.domain(url);
                sp.edit()
                        .remove(profileToSave + "_saveData")
                        .remove(profileToSave + "_images")
                        .remove(profileToSave + "_adBlock")
                        .remove(profileToSave + "_trackingULS")
                        .remove(profileToSave + "_location")
                        .remove(profileToSave + "_fingerPrintProtection")
                        .remove(profileToSave + "_cookies")
                        .remove(profileToSave + "_cookiesThirdParty")
                        .remove(profileToSave + "_deny_cookie_banners")
                        .remove(profileToSave + "_javascript")
                        .remove(profileToSave + "_javascriptPopUp")
                        .remove(profileToSave + "_saveHistory")
                        .remove(profileToSave + "_camera")
                        .remove(profileToSave + "_microphone")
                        .remove(profileToSave + "_dom")
                        .remove(profileToSave + "_night")
                        .remove(profileToSave + "_desktop").apply();
                if (sp.getBoolean("sp_standard_always", true)) {
                    sp.edit().putString("profile", "profileStandard").apply();
                    setProfileIcon(buttonProfile, url);
                }
                setProfileIcon(buttonProfile, url);
                dialogFastToggle.cancel();
                ninjaWebView.reload();
            });

            Button ib_reload = dialogViewFastToggle.findViewById(R.id.ib_reload);
            ib_reload.setOnClickListener(view -> {
                if (ninjaWebView != null) {
                    dialogFastToggle.cancel();
                    ninjaWebView.reload();
                }
            });

            Button ib_settings = dialogViewFastToggle.findViewById(R.id.ib_settings);
            ib_settings.setOnClickListener(view -> {
                if (ninjaWebView != null) {
                    dialogFastToggle.cancel();
                    Intent settings = new Intent(BrowserActivity.this, Settings_Activity.class);
                    startActivity(settings);
                }
            });

            Button button_help = dialogViewFastToggle.findViewById(R.id.button_help);
            button_help.setOnClickListener(view -> {
                dialogFastToggle.cancel();
                Uri webpage = Uri.parse("https://codeberg.org/Gaukler_Faun/FOSS_Browser/wiki/Settings-Dialog.-");
                BrowserUnit.intentURL(this, webpage);
            });
            dialogFastToggle.setOnDismissListener(dialogInterface -> setProfileIcon(floatingActionButton,url));
            dialogFastToggle.show();

            if (SDK_INT >= Build.VERSION_CODES.TIRAMISU && sp.getBoolean("sp_tabBackground", false)) {
                int notificationAllowed = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS);
                if (notificationAllowed != PackageManager.PERMISSION_GRANTED) {
                    HelperUnit.showCustomSnackbarWithTwoActions(
                            context, dialogViewFastToggle, null,
                            getString(R.string.dialog_backGround), getString(R.string.app_permission), "",
                            R.drawable.icon_check, () -> {
                                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1234567);
                                return true;
                            },
                            R.drawable.icon_close, () -> true
                    );
                }
            }
        } else {
            NinjaToast.show(context, getString(R.string.app_error));
        }
    }
    
    public void setProfileIcon (FloatingActionButton floatingActionButton, String url) {
        String profile = sp.getString("profile", "profileStandard");
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(androidx.appcompat.R.attr.colorError, typedValue, true);
        int color = typedValue.data;
        if (profile.equals("profileStandard")) {
            floatingActionButton.setImageResource(R.drawable.icon_profile_standard);
            fab_menu.setImageResource(R.drawable.icon_profile_standard);
        } else {
            floatingActionButton.setImageResource(R.drawable.icon_profile_changed);
            fab_menu.setImageResource(R.drawable.icon_profile_changed);
        }
        listStandard = new List_standard(context);
        if (listStandard.isWhite(url)) {
            floatingActionButton.getDrawable().mutate().setTint(color);
            fab_menu.getDrawable().mutate().setTint(color);
        }
    }

    private void showDialogFilter() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = View.inflate(context, R.layout.dialog_menu, null);
        builder.setTitle(R.string.setting_filter);
        builder.setIcon(R.drawable.icon_filter);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();
        HelperUnit.setupDialog(context, dialog);
        CardView cardView = dialogView.findViewById(R.id.item_CardViewItem);
        cardView.setVisibility(GONE);

        GridView menu_grid = dialogView.findViewById(R.id.menu_grid);
        final List<GridItem> gridList = new LinkedList<>();
        sp.edit().putString("showFilterDialogX", "true").apply();
        HelperUnit.addFilterItems(activity, gridList);

        GridAdapter gridAdapter = new GridAdapter(context, gridList);
        menu_grid.setNumColumns(2);
        menu_grid.setHorizontalSpacing(20);
        menu_grid.setVerticalSpacing(20);
        menu_grid.setAdapter(gridAdapter);

        if (menu_grid.getLayoutParams() instanceof ViewGroup.MarginLayoutParams p) {
            p.setMargins(56, 56, 56, 56);
            menu_grid.requestLayout();
        }

        gridAdapter.notifyDataSetChanged();
        menu_grid.setOnItemClickListener((parent, view, position, id) -> {
            filter = true;
            filterBy = gridList.get(position).getData();
            dialog.cancel();
            bottom_navigation.setSelectedItemId(R.id.page_2);
        });
        dialog.setOnCancelListener(dialogInterface -> sp.edit().putString("showFilterDialogX", "false").apply());
    }

    private void showDialogCustomSearches(String url) {
        search_input.clearFocus();
        if (dialogOverview.isShowing()) {
            dialogOverview.cancel();
        }
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = View.inflate(context, R.layout.dialog_custom_searches, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.redirects_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        ArrayList<CustomRedirect> redirects = new ArrayList<>();
        try {
            redirects = CustomSearchesHelper.getRedirects(sp);
        } catch (JSONException e) {
            Log.e("Searches parsing", e.toString());
        }
        AdapterCustomSearches adapter = new AdapterCustomSearches(context, url, redirects);
        recyclerView.setAdapter(adapter);
        if (url.length() > 150) {
            url = url.substring(0, 150) + " [...]";
        }
        String text = "-> " + url;
        builder.setTitle(R.string.custom_searches_title);
        builder.setMessage(text);
        builder.setIcon(R.drawable.icon_search);
        builder.setPositiveButton(R.string.create_new, ((dialogInterface, i) -> {
            MaterialAlertDialogBuilder builderAddCustom = new MaterialAlertDialogBuilder(context);
            View dialogViewAddCustom = View.inflate(context, R.layout.create_new_searches, null);
            TextInputEditText source = dialogViewAddCustom.findViewById(R.id.source);
            TextInputEditText target = dialogViewAddCustom.findViewById(R.id.target);
            builderAddCustom.setTitle(R.string.custom_searches_title);
            builderAddCustom.setIcon(R.drawable.icon_search);
            builderAddCustom.setNegativeButton(R.string.app_cancel, null);
            builderAddCustom.setPositiveButton(R.string.app_ok, ((dialogInterface2, i2) -> {
                String sourceText = Objects.requireNonNull(source.getText()).toString();
                String targetText = Objects.requireNonNull(target.getText()).toString();
                if (targetText.isEmpty() || sourceText.isEmpty()) return;
                adapter.addRedirect(new CustomRedirect(sourceText, targetText));
                try {
                    CustomSearchesHelper.saveRedirects(adapter.getRedirects());
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }));
            builderAddCustom.setView(dialogViewAddCustom);
            AlertDialog dialogCustomSearchesNew = builderAddCustom.create();
            dialogCustomSearchesNew.show();
            HelperUnit.setupDialog(context, dialogCustomSearchesNew);
        }));
        builder.setNegativeButton(R.string.app_cancel, ((dialogInterface, i) -> dialogCustomSearches.cancel()));
        builder.setView(dialogView);
        dialogCustomSearches = builder.create();
        dialogCustomSearches.show();
        dialogCustomSearches.setCancelable(false);
        HelperUnit.setupDialog(context, dialogCustomSearches);
    }
    private void doubleTapsQuit() {
        if (!sp.getBoolean("sp_close_browser_confirm", true)) finishAndRemoveTask();
        else {
            Snackbar snackbar = Snackbar.make(ninjaWebView, R.string.toast_quit, Snackbar.LENGTH_SHORT);
            HelperUnit.makeSnackbarRound(snackbar);
            snackbar.setAction(context.getString(R.string.app_ok), (v -> finishAndRemoveTask()));
            snackbar.show();
        }
    }
    private void saveOpenedTabs() {
        ArrayList<String> openTabs = new ArrayList<>();
        for (int i = 0; i < BrowserContainer.size(); i++) {
            if (currentAlbumController == BrowserContainer.get(i))
                openTabs.add(0, ((NinjaWebView) (BrowserContainer.get(i))).getUrl());
            else openTabs.add(((NinjaWebView) (BrowserContainer.get(i))).getUrl()); }
        sp.edit().putString("openTabs", TextUtils.join("‚‗‚", openTabs)).apply();
    }
    private void setCustomFullscreen(boolean fullscreen) {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, !fullscreen);
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(window, window.getDecorView());
        if (fullscreen) {
            insetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            insetsController.hide(WindowInsetsCompat.Type.systemBars());
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars());
        }
    }
    private void copyLink(String url) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("text", url);
        Objects.requireNonNull(clipboard).setPrimaryClip(clip);
        NinjaToast.show(this, getString(R.string.app_done));
    }

    public void shareLink(String title, String url) {

        hideOverview();
        List_standard listStandard = new List_standard(context);
        String profile = sp.getString("profile", "profileStandard");
        if (listStandard.isWhite(url)) profile = HelperUnit.domain(url);

        boolean removeTracking = sp.getBoolean(profile + "_trackingULS", true);

        if (removeTracking && url.contains("?") && url.contains("/")) {

            String lastIndex = url.substring(url.lastIndexOf("/"));
            String tracking = url.substring(url.lastIndexOf("?"));
            String urlClean = url.replace(tracking, "");

            if (lastIndex.contains(tracking)) {

                String m = context.getString(R.string.dialog_tracking) + " \"" + tracking + "\"" + "?";

                if (m.length() > 150) {
                    m = m.substring(0, 150) + " [...]?\"";
                }

                GridItem item_01 = new GridItem(context.getString(R.string.app_ok), R.drawable.icon_check);
                GridItem item_02 = new GridItem( context.getString(R.string.app_no), R.drawable.icon_close);
                GridItem item_03 = new GridItem( context.getString(R.string.menu_edit), R.drawable.icon_edit);

                View dialogView = View.inflate(context, R.layout.dialog_menu, null);
                MaterialAlertDialogBuilder builderTrack = new MaterialAlertDialogBuilder(context);

                LinearLayout textGroup = dialogView.findViewById(R.id.textGroup);
                TextView overflowURL = dialogView.findViewById(R.id.overflowURL);
                overflowURL.setText(url);
                TextView overflowMessage = dialogView.findViewById(R.id.overflowMessage);
                overflowMessage.setText(m);
                HelperUnit.setHighLightedText(context, overflowURL, url, HelperUnit.domain(url));
                TextView menuTitle = dialogView.findViewById(R.id.overflowTitle);
                menuTitle.setText(HelperUnit.domain(url));
                textGroup.setOnClickListener(v ->
                        HelperUnit.showCustomSnackbarWithTwoActions(
                                context, dialogView, null,
                                title, "", url,
                                R.drawable.icon_share, () -> {
                                    shareLink(title, url);
                                    return true;
                                },
                                R.drawable.icon_close, () -> true
                        ));

                FloatingActionButton buttonProfile = dialogView.findViewById(R.id.buttonProfile);
                NinjaWebView.getBrowserController().setProfileIcon(buttonProfile, url);
                FaviconHelper.setFavicon(context, dialogView, url, R.id.menu_icon, R.drawable.icon_image_broken);
                buttonProfile.setOnClickListener(v -> showDialogFastToggle(title,url, buttonProfile));
                buttonProfile.setOnLongClickListener(v -> {
                    sp.edit().putString("profile", "profileStandard").apply();
                    NinjaWebView.getBrowserController().setProfileIcon(buttonProfile, url);
                    if (!listStandard.isWhite(url)){
                        ninjaWebView.reload();
                    }return false;
                });
                builderTrack.setView(dialogView);

                AlertDialog dialogTrack = builderTrack.create();
                dialogTrack.show();
                HelperUnit.setupDialog(context, dialogTrack);

                GridView menu_grid = dialogView.findViewById(R.id.menu_grid);
                final List<GridItem> gridList = new LinkedList<>();
                gridList.add(gridList.size(), item_01);
                gridList.add(gridList.size(), item_02);
                gridList.add(gridList.size(), item_03);
                GridAdapter gridAdapter = new GridAdapter(context, gridList);
                menu_grid.setAdapter(gridAdapter);
                gridAdapter.notifyDataSetChanged();
                menu_grid.setOnItemClickListener((parent, view, position, id) -> {
                    switch (position) {

                        case 0:
                            dialogTrack.cancel();
                            Intent sharingIntentClean;
                            sharingIntentClean = new Intent(Intent.ACTION_SEND);
                            sharingIntentClean.setType("text/plain");
                            sharingIntentClean.putExtra(Intent.EXTRA_SUBJECT, title);
                            sharingIntentClean.putExtra(Intent.EXTRA_TEXT, urlClean);
                            context.startActivity(Intent.createChooser(sharingIntentClean, (context.getString(R.string.menu_share_link))));
                            break;
                        case 1:
                            dialogTrack.cancel();
                            Intent sharingIntent;
                            sharingIntent = new Intent(Intent.ACTION_SEND);
                            sharingIntent.setType("text/plain");
                            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, title);
                            sharingIntent.putExtra(Intent.EXTRA_TEXT, url);
                            context.startActivity(Intent.createChooser(sharingIntent, (context.getString(R.string.menu_share_link))));
                            break;
                        case 2:
                            dialogTrack.cancel();
                            View dialogEdit = View.inflate(context, R.layout.dialog_edit, null);
                            TextInputLayout editBottomLayout = dialogEdit.findViewById(R.id.editBottomLayout);
                            TextInputLayout editTopLayout = dialogEdit.findViewById(R.id.editTopLayout);
                            editBottomLayout.setHint(activity.getString(R.string.dialog_URL_hint));
                            editTopLayout.setVisibility(GONE);
                            EditText input = dialogEdit.findViewById(R.id.editBottom);
                            input.setText(url);
                            HelperUnit.showSoftKeyboard(input);

                            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
                            builder.setTitle(context.getString(R.string.menu_edit));
                            builder.setIcon(R.drawable.icon_tracking);
                            builder.setView(dialogEdit);
                            Dialog dialog = builder.create();

                            Button ib_cancel = dialogEdit.findViewById(R.id.editCancel);
                            ib_cancel.setOnClickListener(v -> dialog.cancel());
                            Button ib_ok = dialogEdit.findViewById(R.id.editOK);
                            ib_ok.setOnClickListener(v -> {
                                dialog.dismiss();
                                String newValue = Objects.requireNonNull(input.getText()).toString();
                                Intent sharingIntentEdit;
                                sharingIntentEdit = new Intent(Intent.ACTION_SEND);
                                sharingIntentEdit.setType("text/plain");
                                sharingIntentEdit.putExtra(Intent.EXTRA_SUBJECT, title);
                                sharingIntentEdit.putExtra(Intent.EXTRA_TEXT, newValue);
                                context.startActivity(Intent.createChooser(sharingIntentEdit, (context.getString(R.string.menu_share_link))));
                            });
                            dialog.show();
                            HelperUnit.setupDialog(context, dialog);
                            break;
                    }
                });
            }
        } else {
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, title);
            sharingIntent.putExtra(Intent.EXTRA_TEXT, url);
            context.startActivity(Intent.createChooser(sharingIntent, (context.getString(R.string.menu_share_link))));
        }
    }

    private void postLink(String data, Dialog dialogParent) {
        String urlForPosting = sp.getString("urlForPosting", "");

        if (!urlForPosting.isEmpty()) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("text", data);
            Objects.requireNonNull(clipboard).setPrimaryClip(clip);
            NinjaToast.show(this, getString(R.string.app_done));
            addAlbum("", urlForPosting, true);
        } else {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            View dialogViewSubMenu = View.inflate(context, R.layout.dialog_edit, null);
            TextInputLayout editBottomLayout = dialogViewSubMenu.findViewById(R.id.editBottomLayout);
            TextInputLayout editTopLayout = dialogViewSubMenu.findViewById(R.id.editTopLayout);
            editBottomLayout.setHint(activity.getString(R.string.dialog_URL_hint));
            editTopLayout.setVisibility(GONE);

            builder.setView(dialogViewSubMenu);
            builder.setTitle(activity.getString(R.string.dialog_postOnWebsite));
            builder.setMessage(getString(R.string.dialog_postOnWebsiteHint));
            builder.setIcon(R.drawable.icon_post);

            Dialog dialog = builder.create();
            dialog.show();
            HelperUnit.setupDialog(context, dialog);

            Button ib_cancel = dialogViewSubMenu.findViewById(R.id.editCancel);
            ib_cancel.setOnClickListener(v -> dialog.cancel());
            Button ib_ok = dialogViewSubMenu.findViewById(R.id.editOK);
            ib_ok.setOnClickListener(v -> {
                EditText editBottom = dialogViewSubMenu.findViewById(R.id.editBottom);
                String shareTop = editBottom.getText().toString().trim();
                sp.edit().putString("urlForPosting", shareTop).apply();
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("text", data);
                Objects.requireNonNull(clipboard).setPrimaryClip(clip);
                NinjaToast.show(this, getString(R.string.app_done));
                addAlbum("", shareTop, true);
                dialog.cancel();
                try {
                    dialogParent.cancel();
                } catch (Exception e) {
                    Log.i(TAG, "shouldOverrideUrlLoading Exception:" + e);
                }
            });
        }
    }
    private void searchOnSite() {
        appBar.setVisibility(GONE);
        searchOnSiteLayout.setVisibility(VISIBLE);
        noteTvAppBarUse();
        HelperUnit.showSoftKeyboard(searchOnSiteInput);
    }
    private void saveBookmark(String title, String url) {
        RecordAction action = new RecordAction(context);
        action.open(true);
        String message = context.getString(R.string.app_error) + ": " + context.getString(R.string.app_error_save);
        if (action.checkUrl(url, RecordUnit.TABLE_BOOKMARK))
            NinjaToast.show(this, message);
        else {
            action.addBookmark(new Record(title, url, 0, 0));
            NinjaToast.show(this, R.string.app_done); }
        action.close();
    }

    private void performGesture(String gesture, String url) {
        String gestureAction = Objects.requireNonNull(sp.getString(gesture, "0"));
        switch (gestureAction) {
            case "01":
                break;
            case "02":
                if (ninjaWebView.canGoForward()) {
                    ninjaWebView.stopLoading();
                    WebBackForwardList mWebBackForwardList = ninjaWebView.copyBackForwardList();
                    String historyUrl = mWebBackForwardList.getItemAtIndex(mWebBackForwardList.getCurrentIndex() + 1).getUrl();
                    ninjaWebView.initPreferences(historyUrl);
                    ninjaWebView.goForward();
                }
                else NinjaToast.show(this, R.string.toast_webview_forward);
                break;
            case "03":
                if (customView != null || fullscreenHolder != null) {
                    exitFullscreen();
                } else if (ninjaWebView.canGoBack()){
                    ninjaWebView.goBack();
                } else {
                    // TV: Back never leaves the app (no tab close, no quit). Without a
                    // previous page it only tells the user.
                    NinjaToast.show(BrowserActivity.this, R.string.toast_no_back_history);
                }
                break;
            case "04":
                ninjaWebView.pageUp(true);
                break;
            case "05":
                ninjaWebView.pageDown(true);
                break;
            case "06":
                showAlbum(nextAlbumController(false));
                break;
            case "07":
                showAlbum(nextAlbumController(true));
                break;
            case "08":
                showOverview();
                break;
            case "09":
                addAlbum(getString(R.string.app_name), Objects.requireNonNull(sp.getString("favoriteURL", "https://codeberg.org/Gaukler_Faun/FOSS_Browser/wiki")), true);
                break;
            case "10":
                removeAlbum(currentAlbumController);
                break;
            case "11":
                overViewTab = getString(R.string.album_title_tab);
                setSelectedTab();
                showOverview();
                break;
            case "12":
                shareLink(ninjaWebView.getTitle(), Objects.requireNonNull(ninjaWebView.getUrl()));
                break;
            case "13":
                searchOnSite();
                break;
            case "14":
                saveBookmark(ninjaWebView.getTitle(), url);
                break;
            case "16":
                ninjaWebView.reload();
                break;
            case "17":
                ninjaWebView.loadUrl(Objects.requireNonNull(sp.getString("favoriteURL", "https://codeberg.org/Gaukler_Faun/FOSS_Browser/wiki")));
                break;
            case "18":
                bottom_navigation.setSelectedItemId(R.id.page_2);
                showOverview();
                showDialogFilter();
                break;
            case "19":
                showDialogFastToggle(ninjaWebView.getTitle(), ninjaWebView.getUrl(), fab_menu);
                break;
            case "22":
                sp.edit().putBoolean("sp_screenOn", !sp.getBoolean("sp_screenOn", false)).apply();
                triggerRebirth(context);
                break;
            case "24":
                copyLink(ninjaWebView.getUrl());
                break;
            case "25":
                Intent settings = new Intent(BrowserActivity.this, Settings_Activity.class);
                startActivity(settings);
                break;
            case "26":
                doubleTapsQuit();
                break;
            case "27":
                sp.edit().putString("profile", "profileStandard").apply();
                ninjaWebView.reload();
                break;
            case "29":
                startActivity(Intent.createChooser(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS), null));
                break;
            case "30":
                overViewTab = getString(R.string.album_title_bookmarks);
                setSelectedTab();
                showOverview();
                break;
            case "31":
                overViewTab = getString(R.string.album_title_history);
                setSelectedTab();
                showOverview();
                break;
            case "32":
                ninjaWebView.loadUrl(sp.getString("favoriteURL", "https://codeberg.org/Gaukler_Faun/FOSS_Browser/wiki"));
                break;
        }
    }

    private void closeTabConfirmation(final Runnable okAction) {
        if (!sp.getBoolean("sp_close_tab_confirm", false)) okAction.run();
        else {
            Snackbar snackbar = Snackbar.make(ninjaWebView, R.string.toast_quit_TAB, Snackbar.LENGTH_SHORT);
            HelperUnit.makeSnackbarRound(snackbar);
            snackbar.setAction(context.getString(R.string.app_ok), (v -> okAction.run()));
            snackbar.show();
        }
    }
    private File copyHtmlToCache(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            File cacheFile = new File(context.getCacheDir(), "temp_preview.html");
            FileOutputStream outputStream = new FileOutputStream(cacheFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            outputStream.close();
            return cacheFile;
        } catch (Exception e) {
            String text = context.getString(R.string.app_error) + ": " + e;
            NinjaToast.show(context, text);
            return null;
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void dispatchIntent(Intent intent) {
        String action = intent.getAction();
        String url = intent.getStringExtra(Intent.EXTRA_TEXT);
        Uri dataUri = intent.getData();
        String mimeType = intent.getType();
        if ("".equals(action)) {
            Log.i(TAG, "resumed FOSS browser");
        } else if (filePathCallback != null) {
            filePathCallback = null;
            getIntent().setAction("");
        } else if (Intent.ACTION_VIEW.equals(action) && dataUri != null) {
            getIntent().setAction("");
            String fileName = null;
            if ("content".equals(dataUri.getScheme())) {
                try (Cursor cursor = getContentResolver().query(dataUri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (nameIndex != -1) {
                            fileName = cursor.getString(nameIndex);
                        }
                    }
                } catch (Exception e) {
                    NinjaToast.show(context, getString(R.string.app_error));
                }
            } else if ("file".equals(dataUri.getScheme())) {
                fileName = dataUri.getLastPathSegment();
            }
            if (fileName == null) {
                fileName = "Unbekannte_Datei.txt";
            }

            String extension = "";
            int lastDot = fileName.lastIndexOf('.');
            if (lastDot >= 0) {
                extension = fileName.substring(lastDot + 1).toLowerCase();
            }

            List<String> allowedExtensions = Arrays.asList(
                    "html", "txt", "xml", "json", "java", "md", "js", "css", "sh", "py", "org", "gpx"
            );

            if (!allowedExtensions.contains(extension)) {
                Toast.makeText(this, getString(R.string.dialog_supported), Toast.LENGTH_SHORT).show();
                return;
            }
            // 3. Fehlerfreie Pfad-Generierung für content:// und file://
            String filePath = dataUri.getPath();
            String displayPath = filePath != null ? filePath : dataUri.toString();
            // Korrektur: Wenn die URI bereits eine Content-URI ist, belassen wir sie so, statt "file://" davorzuklatschen
            String virtualFileUrl = "file".equals(dataUri.getScheme()) && filePath != null ? "file://" + filePath : dataUri.toString();
            String fileContent = readTextFromUri(this, dataUri);

            if (!fileContent.trim().isEmpty()) {
                if ("html".equals(extension) || (mimeType != null && mimeType.contains("html"))) {
                    File localHtmlFile = copyHtmlToCache(this, dataUri);
                    if (localHtmlFile != null && localHtmlFile.exists()) {
                        addAlbum(fileName, "file://" + localHtmlFile.getAbsolutePath(), true);
                        ninjaWebView.loadUrl("file://" + localHtmlFile.getAbsolutePath());
                    } else {
                        addAlbum(fileName, "about:blank" , true);
                        ninjaWebView.loadDataWithBaseURL(null, fileContent, "text/html", "UTF-8", null);
                    }
                } else {
                    // UNIVERSAL-METHODE für XML, JSON, TXT, JAVA, MD, etc.
                    String langClass = "language-txt";
                    String formattedContent = fileContent;
                    // NEU: Inhalts- und Endungserkennung basierend auf der extrahierten Extension
                    if ("xml".equals(extension) || fileContent.trim().startsWith("<")) {
                        langClass = "language-xml";
                    } else if ("json".equals(extension) || fileContent.trim().startsWith("{") || fileContent.trim().startsWith("[")) {
                        langClass = "language-json";
                        try {
                            if (fileContent.trim().startsWith("{")) {
                                org.json.JSONObject jsonObject = new org.json.JSONObject(fileContent);
                                formattedContent = jsonObject.toString(2);
                            } else if (fileContent.trim().startsWith("[")) {
                                org.json.JSONArray jsonArray = new org.json.JSONArray(fileContent);
                                formattedContent = jsonArray.toString(2);
                            }
                        } catch (Exception ignored) {
                            NinjaToast.show(context, getString(R.string.app_error));
                        }
                    } else if ("java".equals(extension)) {
                        langClass = "language-java";
                    } else if ("md".equals(extension)) {
                        langClass = "language-markdown";
                    } else if ("js".equals(extension)) {
                        langClass = "language-javascript";
                    } else if ("css".equals(extension)) {
                        langClass = "language-css";
                    } else if ("sh".equals(extension)) {
                        langClass = "language-bash";
                    } else if ("py".equals(extension)) {
                        langClass = "language-python";
                    }
                    // HTML-Sonderzeichen maskieren
                    String escapedContent = formattedContent.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
                    String htmlWrapper = "<html><head>"
                            + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                            + "<title>" + fileName + "</title>"
                            + "<style>"
                            + "  body { margin: 0; padding: 15px; background: #fafafa; font-family: sans-serif; color: #333; }"
                            + "  .file-info { background: #eaeaea; padding: 10px; border-radius: 5px; font-size: 12px; margin-bottom: 15px; border-left: 4px solid #007bb6; word-break: break-all; }"
                            + "  .file-info b { color: #111; }"
                            + "  pre, code { font-family: monospace !important; font-size: 13px !important; white-space: pre-wrap !important; word-wrap: break-word !important; }"
                            + "</style></head><body>"
                            + "<div class='file-info'>"
                            + "  <b>Datei:</b> " + fileName + "<br/>"
                            + "  <b>Pfad:</b> " + displayPath
                            + "</div>"
                            + "<pre class='" + langClass + "'><code class='" + langClass + "'>"
                            + escapedContent
                            + "</code></pre>"
                            + "</body></html>";

                    addAlbum(fileName, virtualFileUrl, true);
                    ninjaWebView.getSettings().setDefaultTextEncodingName("utf-8");
                    ninjaWebView.loadDataWithBaseURL(virtualFileUrl, htmlWrapper, "text/html", "UTF-8", null);
                }
            } else {
                sp.edit().putBoolean("show_overview", false).apply();
                getIntent().setAction("");
                addAlbum(null, Objects.requireNonNull(getIntent().getData()).toString(), true);
                BrowserUnit.openInBackground(activity, ninjaWebView);
            }
        } else if ("postLink".equals(action)) {
            sp.edit().putBoolean("show_overview", false).apply();
            getIntent().setAction("");
            if (BrowserContainer.size() < 1) {
                addAlbum(getString(R.string.app_name), sp.getString("favoriteURL", "https://codeberg.org/Gaukler_Faun/FOSS_Browser/wiki"), true);
            }
            postLink(url, null);
        } else if ("customSearches".equals(action)) {
            sp.edit().putBoolean("show_overview", false).apply();
            getIntent().setAction("");
            if (BrowserContainer.size() < 1) {
                addAlbum(getString(R.string.app_name), sp.getString("favoriteURL", "https://codeberg.org/Gaukler_Faun/FOSS_Browser/wiki"), true);
            }
            showDialogCustomSearches(url);
        } else if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_PROCESS_TEXT)) {
            sp.edit().putBoolean("show_overview", false).apply();
            getIntent().setAction("");
            CharSequence text = getIntent().getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
            assert text != null;
            url = text.toString();
            addAlbum(null, url, true);
        } else if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_WEB_SEARCH)) {
            sp.edit().putBoolean("show_overview", false).apply();
            getIntent().setAction("");
            url = Objects.requireNonNull(intent.getStringExtra(SearchManager.QUERY));
            addAlbum(null, url, true);
        } else if (url != null && Intent.ACTION_SEND.equals(action)) {
            sp.edit().putBoolean("show_overview", false).apply();
            getIntent().setAction("");
            addAlbum(null, url, true);
        } else if (BrowserContainer.size() < 1) {
            addAlbum(getString(R.string.app_name), sp.getString("favoriteURL", "https://codeberg.org/Gaukler_Faun/FOSS_Browser/wiki"), true);
        }
    }
    private String readTextFromUri(Context context, Uri uri) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                inputStream.close();
            }
        } catch (Exception ignored) {}
        return stringBuilder.toString();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setWebView(String title, final String url, final boolean foreground) {

        ninjaWebView = new NinjaWebView(context);

        // TV: the D-pad drives the remote mouse pointer (see initTvCursor()), so the web
        // view keeps the focus while browsing. There is no focus hand off to the browser
        // bar any more: the pointer can simply click the bar, the tabs button and the
        // menu button like a mouse. The pointer keys are handled in
        // BrowserActivity.dispatchKeyEvent(), a key listener here would never see them
        // (the WebView passes keys to the web engine first).
        ninjaWebView.setFocusable(true);
        ninjaWebView.setFocusableInTouchMode(true);
        ninjaWebView.setDefaultFocusHighlightEnabled(false);
        final int webViewId = ninjaWebView.getId() == View.NO_ID ? View.generateViewId() : ninjaWebView.getId();
        ninjaWebView.setId(webViewId);
        ninjaWebView.setNextFocusUpId(R.id.appBar);
        appBar.setNextFocusDownId(webViewId);
        fab_overview.setNextFocusDownId(webViewId);
        fab_menu.setNextFocusDownId(webViewId);
        ninjaWebView.setOnFocusChangeListener((v, hasFocus) -> updateTvCursorVisibility());
        updateTvCursorVisibility();

        ninjaWebView.setOnLongClickListener(v -> {
            WebView.HitTestResult result = ninjaWebView.getHitTestResult();
            int type = result.getType();

            if (type == WebView.HitTestResult.IMAGE_TYPE) {
                String imageURL = result.getExtra();
                // Optimiertes JavaScript: Findet das Bild auch bei relativen Pfaden im HTML
                String script = "javascript:(function() {" +
                        "var allImgs = document.getElementsByTagName('img');" +
                        "var targetImg = null;" +
                        "var searchUrl = '" + imageURL + "';" +
                        "for (var i = 0; i < allImgs.length; i++) {" +
                        "   if (allImgs[i].src === searchUrl || searchUrl.endsWith(allImgs[i].getAttribute('src'))) {" +
                        "       targetImg = allImgs[i];" +
                        "       break;" +
                        "   }" +
                        "}" +
                        "if (!targetImg) return 'ERR_NOT_FOUND';" +
                        "if (!targetImg.hasAttribute('alt')) return 'ERR_NO_ALT_ATTR';" +
                        "if (targetImg.alt.trim() === '') return 'ERR_ALT_EMPTY';" +
                        "return targetImg.alt;" +
                        "})()";

                ninjaWebView.evaluateJavascript(script, value -> {
                    if (value != null) {
                        // 1. Äußere JSON-Anführungszeichen entfernen
                        value = value.replaceAll("^\"|\"$", "").trim();
                        // 2. Maskierte Anführungszeichen (\") zu normalen (") machen
                        value = value.replace("\\\"", "\"");
                        // 3. WICHTIG: Die Textzeichen \n durch einen echten System-Zeilenumbruch ersetzen
                        value = value.replace("\\n", "\n").replace("\\r", "\r");
                    }
                    final String finalValue = value;

                    runOnUiThread(() -> {
                        String textToShow;
                        assert finalValue != null;
                        switch (finalValue) {
                            case "":
                            case "null":
                            case "ERR_NOT_FOUND":
                                textToShow = context.getString(R.string.app_error) + ": ERR_ALT_NOT_FOUND";
                                NinjaToast.show(this, textToShow);
                                break;
                            case "ERR_NO_ALT_ATTR":
                                textToShow = context.getString(R.string.app_error) + ": ERR_NO_ALT_ATTR";
                                break;
                            case "ERR_ALT_EMPTY":
                                textToShow = context.getString(R.string.app_error) + ": ERR_ALT_EMPTY";
                                break;
                            default:
                                textToShow = finalValue.replace("\n", " ").replace("\r", " ");
                                break;
                        }
                        HelperUnit.showCustomSnackbarWithTwoActions(
                                this, ninjaWebView, null,
                                ninjaWebView.getTitle(), textToShow, imageURL,
                                R.drawable.icon_share, () -> {
                                    shareLink(ninjaWebView.getTitle(), textToShow);
                                    return true;
                                },
                                R.drawable.icon_close, () -> true
                        );
                    });
                });
                return true;
            }
            if (type == WebView.HitTestResult.SRC_ANCHOR_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                String urlResult = result.getExtra();
                showOverflow(null, null, 1, HelperUnit.domain(urlResult), urlResult, null, null, 0);
                return true;
            }
            return false;
        });

        if (Objects.requireNonNull(sp.getString("saved_menu", "no")).equals("no")) {
            sp.edit().putString("saved_menu", "yes").apply();
            HelperUnit.initAndLoadMenu(this);
        }

        if (Objects.requireNonNull(sp.getString("saved_key_ok", "no")).equals("no")) {
            sp.edit().putString("saved_key_ok", "yes")
                    .putString("setting_gesture_tb_up", "04")
                    .putString("setting_gesture_tb_down", "05")
                    .putString("setting_gesture_tb_left", "03")
                    .putString("setting_gesture_tb_right", "02")
                    .putString("setting_gesture_nav_up", "16")
                    .putString("setting_gesture_nav_down", "10")
                    .putString("setting_gesture_nav_left", "07")
                    .putString("setting_gesture_nav_right", "06")
                    .putString("setting_gesture_tabButton", "19")
                    .putString("setting_gesture_overViewButton", "18")
                    .putBoolean("sp_autofill", true)
                    .apply();
            ninjaWebView.setProfileDefaultValues();

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            String m = getString(R.string.app_intro_a) + " " +getString(R.string.app_intro_b);
            builder.setTitle(R.string.app_name);
            builder.setMessage(m);
            builder.setIcon(R.drawable.icon_web);
            builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            HelperUnit.setupDialog(context, dialog);
        }

        ninjaWebView.setBrowserController(this);
        ninjaWebView.setAlbumTitle(title, url);

        if (url.isEmpty()) ninjaWebView.loadUrl("about:blank");
        else ninjaWebView.loadUrl(url);

        if (currentAlbumController != null) {
            ninjaWebView.setPredecessor(currentAlbumController);
            //save currentAlbumController and use when TAB is closed via Back button
            int index = BrowserContainer.indexOf(currentAlbumController) + 1;
            BrowserContainer.add(ninjaWebView, index);
        }
        else BrowserContainer.add(ninjaWebView);

        if (!foreground) ninjaWebView.deactivate();
        else {
            hideOverview();
            ninjaWebView.setBrowserController(this);
            ninjaWebView.activate();
            dialogOverview.cancel();
            showAlbum(ninjaWebView);
        }
        View albumView = ninjaWebView.getAlbumView();
        tab_container.addView(albumView, WRAP_CONTENT, WRAP_CONTENT);
        updateOmniBox();
    }

    private synchronized void addAlbum(String title, final String url, final boolean foreground) {
        setWebView(title, url, foreground);
    }

    private void triggerRebirth(Context context) {
        sp.edit().putInt("restart_changed", 0).apply();
        sp.edit().putBoolean("restoreOnRestart", true).apply();
        Snackbar snackbar = Snackbar.make(ninjaWebView, R.string.toast_restart, Snackbar.LENGTH_SHORT);
        HelperUnit.makeSnackbarRound(snackbar);
        snackbar.setAction(context.getString(R.string.app_ok), (v -> {
            PackageManager packageManager = context.getPackageManager();
            Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
            assert intent != null;
            ComponentName componentName = intent.getComponent();
            Intent mainIntent = Intent.makeRestartActivityTask(componentName);
            context.startActivity(mainIntent);
            System.exit(0);
        }));
        snackbar.show();
    }

    public static View getView() {
        return ninjaWebView.getRootView();
    }
}