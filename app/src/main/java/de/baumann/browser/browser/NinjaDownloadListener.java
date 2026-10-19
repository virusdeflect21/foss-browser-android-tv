package de.baumann.browser.browser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import de.baumann.browser.R;
import de.baumann.browser.activity.BrowserActivity;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.view.NinjaToast;

public class NinjaDownloadListener implements DownloadListener {
    private final WebView webView;
    public NinjaDownloadListener(WebView webView) {
        super();
        this.webView = webView;
    }
    private String getExtension(String mimeType) {
        if (mimeType == null) return "bin";
        if (mimeType.contains("pdf")) return "pdf";
        if (mimeType.contains("image/png")) return "png";
        if (mimeType.contains("image/jpeg")) return "jpg";
        if (mimeType.contains("zip")) return "zip";
        return "bin";
    }
    private String getExtensionFromBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 4) return "bin";
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            hex.append(String.format("%02X", bytes[i]));
        }
        String magic = hex.toString();
        if (magic.startsWith("25504446")) return "pdf";
        if (magic.startsWith("89504E47")) return "png";
        if (magic.startsWith("FFD8FF"))   return "jpg";
        if (magic.startsWith("47494638")) return "gif";
        if (magic.startsWith("504B0304")) return "zip";
        return "bin";
    }

    @Override
    public void onDownloadStart(final String url, String userAgent, final String contentDisposition, final String mimeType, long contentLength) {
        final String downloadUrl = (url != null) ? url : "";
        Context context = webView.getContext();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        String generatedFileName = HelperUnit.domain(webView.getUrl()) + "_" + timestamp + "." + getExtension(mimeType);

        if (downloadUrl.startsWith("blob:")) {
            String d = context.getString(R.string.dialog_title_download) + " - " + generatedFileName;
            HelperUnit.showCustomSnackbarWithTwoActions(
                    context, webView, null,
                    webView.getTitle(), d, downloadUrl,
                    R.drawable.icon_check, () -> {
                        // FIX: fetch() nutzen anstatt XMLHttpRequest, um CORS/Same-Origin-Sperren zu umgehen
                        String jsCode = "javascript: (function() {" +
                                "   fetch('" + downloadUrl + "')" +
                                "   .then(response => response.blob())" +
                                "   .then(blob => {" +
                                "       var reader = new FileReader();" +
                                "       reader.onloadend = function() {" +
                                "           var base64data = reader.result;" + // Enthält das komplette Data-URL Präfix
                                "           if (window.AndroidInterface) {" +
                                "               window.AndroidInterface.processBlob(base64data, '" + mimeType + "', '" + generatedFileName + "');" +
                                "           }" +
                                "       };" +
                                "       reader.readAsDataURL(blob);" +
                                "   }).catch(err => console.error(err));" +
                                "})();";
                        webView.evaluateJavascript(jsCode, null);
                        return true;
                    },
                    R.drawable.icon_close, () -> true
            );
        } else if (downloadUrl.startsWith("data:")) {
            int commaIndex = downloadUrl.indexOf(",");
            if (commaIndex == -1) return;

            String base64Data = downloadUrl.substring(commaIndex + 1).replace(" ", "").replace("\n", "");
            byte[] decodedBytes;
            try {
                decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
            } catch (IllegalArgumentException e) {
                NinjaToast.show(context, context.getString(R.string.app_error) + " Base64");
                return;
            }

            String realExtension = getExtensionFromBytes(decodedBytes);
            String finalFileName = generatedFileName;
            if (finalFileName.contains(".")) {
                finalFileName = finalFileName.substring(0, finalFileName.lastIndexOf(".")) + "." + realExtension;
            } else {
                finalFileName = finalFileName + "." + realExtension;
            }

            String d = context.getString(R.string.dialog_title_download) + " - " + finalFileName;
            String savedFileName = finalFileName;

            HelperUnit.showCustomSnackbarWithTwoActions(
                    context, webView, null,
                    webView.getTitle(), d, downloadUrl,
                    R.drawable.icon_check, () -> {

                        new Thread(() -> {
                            try {
                                android.content.ContentValues values = new android.content.ContentValues();
                                values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, savedFileName);
                                values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType);
                                values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                                android.content.ContentResolver resolver = context.getContentResolver();
                                android.net.Uri uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

                                if (uri != null) {
                                    try (java.io.OutputStream os = resolver.openOutputStream(uri)) {
                                        if (os != null) {
                                            os.write(decodedBytes);
                                            os.flush();
                                        }
                                    }
                                } else {
                                    throw new java.io.IOException("Failed to create MediaStore entry.");
                                }

                                webView.post(() -> {
                                    String text = context.getString(R.string.app_done) + ". " + context.getString(R.string.menu_download) + "?";
                                    Snackbar snackbar = Snackbar.make(webView, text, Snackbar.LENGTH_SHORT);
                                    HelperUnit.makeSnackbarRound(snackbar);
                                    snackbar.setAction(context.getString(R.string.app_ok), (v -> context.startActivity(Intent.createChooser(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS), null))));
                                    snackbar.show();
                                });
                            } catch (Exception e) {
                                webView.post(() -> {
                                    String textToShow = context.getString(R.string.app_error) + ": " + e.getMessage();
                                    NinjaToast.show(context, textToShow);
                                });
                            }
                        }).start();
                        return true;
                    },
                    R.drawable.icon_close, () -> true
            );
        } else {
            String filename = URLUtil.guessFileName(downloadUrl, contentDisposition, mimeType);
            if (filename.length() > 150) {
                filename = filename.substring(0, 150) + " [...]?\"";
            }
            String m = context.getString(R.string.dialog_title_download) + " - " + filename;
            String finalFilename = filename;
            HelperUnit.showCustomSnackbarWithTwoActions(
                    context, webView, null,
                    webView.getTitle(), m, downloadUrl,
                    R.drawable.icon_check, () -> {
                        BrowserUnit.download(context, downloadUrl, finalFilename, mimeType);
                        return true;
                    },
                    R.drawable.icon_close, () -> true
            );
        }
    }
    @SuppressLint("SetTextI18n")
    public static void saveAs(final Activity activity, final String url, final String name, Dialog dialogParent) {
        if (url != null && url.startsWith("blob:")) {
            NinjaToast.show(activity, activity.getString(R.string.app_error) + ": Blob-Download");
            if (dialogParent != null) dialogParent.cancel();
            return;
        }

        try {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);

            View dialogView = View.inflate(activity, R.layout.dialog_edit, null);
            TextInputLayout editBottomLayout = dialogView.findViewById(R.id.editBottomLayout);
            TextInputLayout editTopLayout = dialogView.findViewById(R.id.editTopLayout);
            editBottomLayout.setHint(activity.getString(R.string.dialog_extension_hint));
            editTopLayout.setHint(activity.getString(R.string.dialog_title_hint));
            EditText editTop = dialogView.findViewById(R.id.editTop);
            EditText editBottom = dialogView.findViewById(R.id.editBottom);
            String filename = name != null ? name : URLUtil.guessFileName(url, null, null);

            if (filename.contains("?")) filename = filename.substring(0, filename.indexOf("?"));
            if (filename.contains("#")) filename = filename.substring(0, filename.indexOf("#"));

            String prefix = filename;
            String extension = ".bin";

            int dotIndex = filename.lastIndexOf(".");
            if (dotIndex > 0 && dotIndex < filename.length() - 1) {
                prefix = filename.substring(0, dotIndex);
                extension = filename.substring(dotIndex);
            }

            editTop.setText(prefix);
            if (extension.length() <= 8) {
                editBottom.setText(extension);
            } else {
                editBottom.setText(".bin");
            }

            builder.setTitle(R.string.menu_save_as);
            builder.setIcon(R.drawable.icon_menu_save);
            builder.setView(dialogView);

            AlertDialog dialog = builder.create();
            dialog.show();
            HelperUnit.setupDialog(activity, dialog);

            Button ib_cancel = dialogView.findViewById(R.id.editCancel);
            ib_cancel.setOnClickListener(view -> dialog.cancel());
            Button ib_ok = dialogView.findViewById(R.id.editOK);
            ib_ok.setOnClickListener(view12 -> {

                String title = editTop.getText().toString().trim();
                String extension1 = editBottom.getText().toString().trim();

                // Stellt sicher, dass die vom Nutzer eingegebene Endung immer mit einem Punkt startet
                if (!extension1.isEmpty() && !extension1.startsWith(".")) {
                    extension1 = "." + extension1;
                }

                String finalFileName = title + extension1;

                if (title.isEmpty() || extension1.isEmpty() || extension1.equals(".")) {
                    NinjaToast.show(activity, activity.getString(R.string.toast_input_empty));
                } else {
                    try {
                        assert url != null;
                        if (url.startsWith("data:")) {
                            DataURIParser dataURIParser = new DataURIParser(url);
                            byte[] dataBytes = dataURIParser.getImagedata();

                            String cleanExt = extension1.substring(1).toLowerCase();
                            String mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(cleanExt);
                            if (mimeType == null) mimeType = "application/octet-stream";

                            android.content.ContentValues values = new android.content.ContentValues();
                            values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, finalFileName);
                            values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType);
                            values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                            android.content.ContentResolver resolver = activity.getContentResolver();
                            android.net.Uri uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

                            if (uri != null) {
                                try (java.io.OutputStream os = resolver.openOutputStream(uri)) {
                                    if (os != null) {
                                        os.write(dataBytes);
                                        os.flush();
                                    }
                                }
                                String text = activity.getString(R.string.app_done) + ". " + activity.getString(R.string.menu_download) + "?";
                                Snackbar snackbar = Snackbar.make(BrowserActivity.getView(), text, Snackbar.LENGTH_LONG);
                                snackbar.setAction(activity.getString(R.string.app_ok), v -> activity.startActivity(Intent.createChooser(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS), null)));
                                snackbar.show();
                            } else {
                                throw new java.io.IOException("MediaStore entry creation failed.");
                            }
                        } else {
                            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                            CookieManager cookieManager = CookieManager.getInstance();
                            String cookie = cookieManager.getCookie(url);

                            request.addRequestHeader("Cookie", cookie);
                            request.addRequestHeader("Accept", "text/html, application/xhtml+xml, */*");
                            request.addRequestHeader("Accept-Language", "en-US,en;q=0.7,he;q=0.3");
                            request.addRequestHeader("Referer", url);
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                            request.setTitle(finalFileName);

                            String cleanExt = extension1.substring(1).toLowerCase();
                            String guessedMime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(cleanExt);
                            request.setMimeType(guessedMime != null ? guessedMime : "*/*");

                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, finalFileName);
                            DownloadManager manager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
                            if (manager != null) {
                                manager.enqueue(request);
                            }
                        }
                    } catch (Exception e) {
                        Log.e("FOSS Browser", "SaveAs Processing Error: " + e);
                        Toast.makeText(activity, activity.getString(R.string.app_error) + ": " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }

                    try {
                        dialog.cancel();
                    } catch (Exception e) {
                        Log.i("FOSS Browser", "SaveAs Dialog Cancel:" + e);
                    }
                    if (dialogParent != null) {
                        dialogParent.cancel();
                    }
                }
            });
        } catch (Exception e) {
            Log.e("FOSS Browser", "SaveAs Outer Error: " + e);
        }
    }
    public static class DataURIParser {
        private final byte[] imagedata;
        public DataURIParser(String url) {
            if (url == null || !url.startsWith("data:")) {
                this.imagedata = new byte[0];
                return;
            }

            int commaIndex = url.indexOf(",");
            String data = (commaIndex != -1) ? url.substring(commaIndex + 1) : "";
            data = data.replace(" ", "").replace("\n", "").replace("\r", "");

            byte[] decodedBytes;
            try {
                if (url.contains(";base64")) {
                    decodedBytes = Base64.decode(data, Base64.DEFAULT);
                } else {
                    decodedBytes = android.net.Uri.decode(data).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                }
            } catch (Exception e) {
                decodedBytes = new byte[0];
            }
            this.imagedata = decodedBytes;
        }

        public byte[] getImagedata() {
            return imagedata;
        }
    }
}