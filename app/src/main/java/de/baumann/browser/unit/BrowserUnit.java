package de.baumann.browser.unit;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import de.baumann.browser.R;
import de.baumann.browser.activity.BrowserActivity;
import de.baumann.browser.database.RecordAction;

public class BrowserUnit {

    public static final int LOADING_STOPPED = 101;  //Must be > PROGRESS_MAX !
    public static final String MIME_TYPE_TEXT_PLAIN = "text/plain";
    public static final String URL_ENCODING = "UTF-8";

    public static boolean isURL(String urlString) {
        if (urlString == null || urlString.trim().isEmpty()) {
            return false;
        }
        urlString = urlString.trim();
        if ("about:blank".equalsIgnoreCase(urlString)) {
            return true;
        }
        try {
            URI uri = new URI(urlString);
            if (uri.getScheme() != null) {
                String scheme = uri.getScheme().toLowerCase();
                return "http".equals(scheme) || "https".equals(scheme) || "file".equals(scheme) || "content".equals(scheme);
            }
            Pattern domainPattern = Pattern.compile("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$");
            if (domainPattern.matcher(urlString).matches()) {
                URI fallbackUri = new URI("http://" + urlString);
                return fallbackUri.getHost() != null && fallbackUri.getHost().contains(".");
            }
            return false;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public static String queryWrapper(Context context, String query) {

        if (query.contains(";jsessionid=")) {
            String tracking = query.substring(query.lastIndexOf(";"));
            query = query.replace(tracking, "");
        }

        if (isURL(query) || query.isEmpty()) {
            if (query.startsWith("about:blank") || query.startsWith("mailto:")) {
                return query;
            }
            if (!query.contains("://")) {
                query = "https://" + query;
            }
            return query;
        } else {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            String customSearchEngine = sp.getString("sp_search_engine_custom", "");
            String customSearches = sp.getString("sp_search_customSearches", "");
            query = query.replace("&", "%26");
            query = query.replace("#", "");
            //Override UserAgent if own UserAgent is defined
            if (!sp.contains("searchEngineSwitch")) {
                //if new switch_text_preference has never been used initialize the switch
                if (customSearchEngine.isEmpty()) {
                    sp.edit().putBoolean("searchEngineSwitch", false).apply();
                } else {
                    sp.edit().putBoolean("searchEngineSwitch", true).apply();
                }
            }

            if (!customSearches.isEmpty()) {
                return customSearches + query;
            } else if (sp.getBoolean("searchEngineSwitch", false)) {
                //if new switch_text_preference has never been used initialize the switch
                return customSearchEngine + query;
            } else {
                // TV default is Google (2). 0/1 (Startpage) stay for saved preferences.
                final int i = Integer.parseInt(Objects.requireNonNull(sp.getString("sp_search_engine", "2")));
                return switch (i) {
                    case 2 -> "https://www.google.com/search?q=" + query;
                    case 0 -> "https://startpage.com/do/search?query=" + query;
                    case 1 -> "https://startpage.com/do/search?lui=deu&language=deutsch&query=" + query;
                    case 4 -> "https://duckduckgo.com/?q=" + query;
                    case 6 -> "https://searx.be/?q=" + query;
                    case 7 -> "https://www.qwant.com/?q=" + query;
                    case 8 -> "https://www.ecosia.org/search?q=" + query;
                    default -> "https://www.mojeek.com/search?q=" + query;
                };
            }
        }
    }

    public static void download(final Context context, final String url, final String fileName, final String mimeType) {
        if (context == null || url == null || url.trim().isEmpty()) {
            return;
        }

        // Protokoll-Korrektur (Sicheres HTTPS als Fallback statt unsicherem HTTP)
        String verifiedUrl = url;
        if (!url.toLowerCase(Locale.US).startsWith("http://") && !url.toLowerCase(Locale.US).startsWith("https://")) {
            verifiedUrl = "https://" + url;
        }

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(verifiedUrl));
            CookieManager cookieManager = CookieManager.getInstance();
            String cookie = cookieManager.getCookie(verifiedUrl);
            if (cookie != null) {
                request.addRequestHeader("Cookie", cookie);
            }

            request.addRequestHeader("Accept", "text/html, application/xhtml+xml, */*");
            request.addRequestHeader("Accept-Language", Locale.getDefault().toLanguageTag());
            request.addRequestHeader("Referer", verifiedUrl);

            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setTitle(fileName);
            request.setMimeType(mimeType);

            // ENTFERNT: request.allowScanningByMediaScanner() da ab API 29 veraltet und wirkungslos

            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager != null) {
                manager.enqueue(request);
            } else {
                throw new IllegalStateException("DownloadManager not available");
            }
        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : e.toString();
            Toast.makeText(context, context.getString(R.string.app_error) + ": " + errorMessage, Toast.LENGTH_LONG).show();
            Log.e(TAG, "FOSS Browser: Error Downloading File", e);
        }
    }

    public static void clearBookmark(Context context) {
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.clearTable(RecordUnit.TABLE_BOOKMARK);
        action.close();
    }

    public static void clearHistory(Context context) {
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.clearTable(RecordUnit.TABLE_HISTORY);
        action.close();
    }

    public static void intentURL(Context context, Uri uri) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW);
        browserIntent.setData(uri);
        browserIntent.setPackage("de.baumann.browser");
        context.startActivity(browserIntent);
    }

    public static void openInBackground(Activity activity, WebView webView) {
        if (activity == null || webView == null) return;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        if (!sp.getBoolean("sp_tabBackground", false)) return;
        String dialogSetting = sp.getString("openBackground_dialog", "show");
        if ("never".equals(dialogSetting)) return;
        // Notification-Inhalt vorbereiten
        String url = webView.getUrl();
        String text = activity.getString(R.string.dialog_backGround);
        NotificationManager mNotifyMgr = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        // PendingIntent für Klick auf die Benachrichtigung
        Intent intentP = new Intent(activity, BrowserActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(activity, 0, intentP, PendingIntent.FLAG_IMMUTABLE);
        // Notification Channel erstellen (nur ab Android 8/Orest)
        if (mNotifyMgr != null) {
            NotificationChannel channel = new NotificationChannel("1", "Links background", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Open links in background -> click to open");
            channel.setShowBadge(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            mNotifyMgr.createNotificationChannel(channel);
        }
        Notification buildNotification = new NotificationCompat.Builder(activity, "1")
                .setSmallIcon(R.drawable.icon_web)
                .setAutoCancel(true)
                .setContentTitle(HelperUnit.domain(url))
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .build();
        // Verzweigung für Snackbar oder direkten Aufruf
        if ("show".equals(dialogSetting)) {
            HelperUnit.showCustomSnackbarWithTwoActions(
                    activity, webView, null, text, activity.getString(R.string.app_session), url,
                    R.drawable.icon_check, () -> {
                        sp.edit().putString("openBackground_dialog", "always").apply();
                        displayNotification(activity, mNotifyMgr, buildNotification);
                        return true;
                    },
                    R.drawable.icon_close, () -> {
                        sp.edit().putString("openBackground_dialog", "never").apply();
                        return true;
                    }
            );
        } else {
            displayNotification(activity, mNotifyMgr, buildNotification);
        }
    }

    private static void displayNotification(Activity activity, NotificationManager mNotifyMgr, Notification buildNotification) {

        if (activity == null || mNotifyMgr == null || buildNotification == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            new MaterialAlertDialogBuilder(activity)
                    .setIcon(R.drawable.icon_alert)
                    .setTitle(R.string.app_permission_notification)
                    .setMessage(R.string.app_permission)
                    .setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {
                        dialog.dismiss();
                        try {
                            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
                            activity.startActivity(intent);
                        } catch (Exception e) {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .setData(Uri.fromParts("package", activity.getPackageName(), null));
                            activity.startActivity(intent);
                        }
                    })
                    .setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel())
                    .show(); // Direkt anzeigen über Fluent-API
            return;
        }
        // Berechtigung vorhanden oder älteres Android -> Benachrichtigung senden
        mNotifyMgr.notify(4, buildNotification);
        activity.moveTaskToBack(true);
    }
}