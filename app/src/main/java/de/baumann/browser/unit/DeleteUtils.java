package de.baumann.browser.unit;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewDatabase;
import android.widget.Toast;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DeleteUtils {

    public static void executeDeletion(Context context, boolean cache, boolean cookies,
                                       boolean database, boolean settings, boolean otherWebview) {

        File webviewDir = new File(context.getFilesDir().getParent(), "app_webview/Default");

        if (cache) {
            new WebView(context).clearCache(true);
            deleteDir(context.getCacheDir());
            deleteDir(context.getExternalCacheDir());
        }

        if (cookies) {
            CookieManager cookieManager = CookieManager.getInstance();

            // Whitelist-Logik: Wir durchsuchen die Haupt-SharedPreferences nach Profilen
            SharedPreferences sharedPrefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
            Map<String, ?> allEntries = sharedPrefs.getAll();
            Map<String, String> savedCookies = new HashMap<>();

            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                String key = entry.getKey();
                // Prüfen, ob der Schlüssel auf das Profil-Cookie-Muster passt und aktiviert ist
                if (key.endsWith("_cookies") && entry.getValue() instanceof Boolean && (Boolean) entry.getValue()) {
                    String domainProfile = key.substring(0, key.lastIndexOf("_cookies"));

                    // URL-Format für den CookieManager sicherstellen
                    String url = domainProfile.startsWith("http") ? domainProfile : "https://" + domainProfile;

                    String cookieStr = cookieManager.getCookie(url);
                    if (cookieStr != null && !cookieStr.isEmpty()) {
                        savedCookies.put(url, cookieStr);
                    }
                }
            }

            // Alle Cookies aus dem System entfernen
            cookieManager.removeAllCookies(null);
            cookieManager.flush();

            // Cookies der erlaubten Profile wiederherstellen
            for (Map.Entry<String, String> entry : savedCookies.entrySet()) {
                String[] individualCookies = entry.getValue().split(";");
                for (String cookie : individualCookies) {
                    cookieManager.setCookie(entry.getKey(), cookie.trim());
                }
            }
            cookieManager.flush();
        }

        if (database) {
            WebStorage.getInstance().deleteAllData();
            if (webviewDir.exists()) {
                deleteDir(new File(webviewDir, "databases"));
                deleteDir(new File(webviewDir, "IndexedDB"));
            }

            String[] databases = context.databaseList();
            for (String db : databases) {
                // Verhindert das Löschen von System- und Preference-Datenbanken
                if (!db.contains("prefs") && !db.contains("webview")) {
                    context.deleteDatabase(db);
                }
            }
        }

        if (otherWebview) {
            WebViewDatabase webViewDb = WebViewDatabase.getInstance(context);
            webViewDb.clearHttpAuthUsernamePassword();
            android.webkit.GeolocationPermissions.getInstance().clearAll();

            if (webviewDir.exists()) {
                deleteDir(new File(webviewDir, "Local Storage"));
                deleteDir(new File(webviewDir, "Session Storage"));
                deleteDir(new File(webviewDir, "blob_storage"));
                deleteDir(new File(webviewDir, "Service Worker"));
                deleteDir(new File(webviewDir, "VideoDecodeStats"));
                deleteDir(new File(webviewDir, "Web Data"));
                deleteDir(new File(webviewDir, "Web Data-journal"));
            }
        }

        if (settings) {
            // Alle SharedPreferences-Dateien leeren, außer die Konfiguration des Löschmenüs selbst
            File sharedPrefsDir = new File(context.getFilesDir().getParent(), "shared_prefs");
            if (sharedPrefsDir.exists() && sharedPrefsDir.isDirectory()) {
                for (String file : Objects.requireNonNull(sharedPrefsDir.list())) {
                    if (!file.equals("clear_data_settings.xml")) {
                        context.getSharedPreferences(file.replace(".xml", ""), Context.MODE_PRIVATE)
                                .edit().clear().apply();
                    }
                }
            }
        }
    }

    // Hilfsmethode zum rekursiven Löschen von Dateien und Ordnerstrukturen
    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            assert children != null;
            for (String child : children) {
                if (!deleteDir(new File(dir, child))) return false;
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        }
        return false;
    }

    // Prüft beim App-Beenden, ob der Auto-Delete aktiv ist und führt ihn aus
    public static void checkAndExecuteAutoDelete(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("clear_data_settings", Context.MODE_PRIVATE);
        if (prefs.getBoolean("auto_delete_exit", false)) {
            executeDeletion(
                    context,
                    prefs.getBoolean("delete_cache", false),
                    prefs.getBoolean("delete_cookies", false),
                    prefs.getBoolean("delete_database", false),
                    prefs.getBoolean("delete_settings", false),
                    prefs.getBoolean("delete_other", false)
            );
            Toast.makeText(context, "Daten beim Beenden automatisch bereinigt", Toast.LENGTH_SHORT).show();
        }
    }
}