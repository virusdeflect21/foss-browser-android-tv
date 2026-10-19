package de.baumann.browser.browser;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;

import de.baumann.browser.R;
import de.baumann.browser.activity.BrowserActivity;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.view.NinjaToast;
import de.baumann.browser.view.NinjaWebView;

public class NinjaWebChromeClient extends WebChromeClient {

    private final NinjaWebView ninjaWebView;

    private final BrowserActivity activity;

    public NinjaWebChromeClient(NinjaWebView ninjaWebView) {
        super();
        this.ninjaWebView = ninjaWebView;
        Context context = ninjaWebView.getContext();
        if (context instanceof BrowserActivity) {
            this.activity = (BrowserActivity) context;
        } else {
            this.activity = null;
        }
    }

    @Override
    public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
        new MaterialAlertDialogBuilder(ninjaWebView.getContext())
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm())
                .show();
        return true;
    }

    @Override
    public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
        new MaterialAlertDialogBuilder(ninjaWebView.getContext())
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm())
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> result.cancel())
                .show();
        return true;
    }

    @Override
    public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, final JsPromptResult result) {
        final EditText input = new EditText(ninjaWebView.getContext());
        input.setText(defaultValue);
        FrameLayout container = new FrameLayout(ninjaWebView.getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) (20 * ninjaWebView.getContext().getResources().getDisplayMetrics().density);
        params.leftMargin = margin;
        params.rightMargin = margin;
        input.setLayoutParams(params);
        container.addView(input);

        new MaterialAlertDialogBuilder(ninjaWebView.getContext())
                .setTitle(message)
                .setView(container)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm(input.getText().toString()))
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> result.cancel())
                .show();
        return true;
    }
    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        if (consoleMessage.message().contains("NotAllowedError: Write permission denied.")) {  //this error occurs when user copies to clipboard
            NinjaToast.show(ninjaWebView.getContext(), R.string.app_error_copy);
            return true;
        }
        return false;
    }

    @Override
    public void onProgressChanged(WebView view, int progress) {
        super.onProgressChanged(view, progress);
        String url = ninjaWebView.getUrl();
        String title = ninjaWebView.getTitle();
        ninjaWebView.updateTitle(progress);
        assert title != null;
        if (title.isEmpty()) ninjaWebView.updateTitle(HelperUnit.domain(url), url);
        else ninjaWebView.updateTitle(title,url);
    }
    @Override
    public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, Message resultMsg) {
        Context context = view.getContext();
        NinjaWebView newWebView = new NinjaWebView(context);
        view.addView(newWebView);
        WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
        transport.setWebView(newWebView);
        resultMsg.sendToTarget();
        newWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                try {
                    BrowserUnit.intentURL(context, request.getUrl());
                } catch (Exception e) {
                    Log.i(TAG, "shouldOverrideUrlLoading Exception:" + e);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(request.getUrl().toString()));
                    context.startActivity(Intent.createChooser(intent, request.getUrl().toString()));
                }
                return true;
            }
        });
        return true;
    }
    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
        NinjaWebView.getBrowserController().onShowCustomView(view, callback);
        super.onShowCustomView(view, callback);
    }
    /**
     * TV fork: fullscreen requests of video players and of the Fullscreen API are delivered
     * through this three argument variant. super must not be called here - the default
     * implementation forwards to the two argument method on some WebView versions, the
     * fullscreen view would be handed to the activity twice.
     */
    @Override
    public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback) {
        NinjaWebView.getBrowserController().onShowCustomView(view, callback);
    }
    @Override
    public void onHideCustomView() {
        NinjaWebView.getBrowserController().onHideCustomView();
        super.onHideCustomView();
    }
    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
        NinjaWebView.getBrowserController().showFileChooser(ninjaWebView.getContext(),filePathCallback);
        return true;
    }
    @Override
    public void onPermissionRequest(final PermissionRequest request) {
        final boolean needsCamera = containsResource(request.getResources(), PermissionRequest.RESOURCE_VIDEO_CAPTURE);
        final boolean needsMicrophone = containsResource(request.getResources(), PermissionRequest.RESOURCE_AUDIO_CAPTURE);
        // Szenario 1: Webseite will BEIDES (Kamera & Mikrofon)
        if (needsCamera && needsMicrophone) {
            activity.requestCameraPermission(isCameraGranted -> activity.requestMicrophonePermission(isMicGranted -> {
                // Wir erstellen eine dynamische Liste der tatsächlich erteilten Rechte
                ArrayList<String> grantedResources = new ArrayList<>();
                if (isCameraGranted) grantedResources.add(PermissionRequest.RESOURCE_VIDEO_CAPTURE);
                if (isMicGranted) grantedResources.add(PermissionRequest.RESOURCE_AUDIO_CAPTURE);
                if (!grantedResources.isEmpty()) {
                    // Gewährt nur das, was auch wirklich erlaubt wurde!
                    request.grant(grantedResources.toArray(new String[0]));
                } else {
                    request.deny();
                }
            }));
        }
        else if (needsCamera) {
            activity.requestCameraPermission(isCameraGranted -> {
                if (isCameraGranted) {
                    request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                } else {
                    request.deny();
                }
            });
        }
        else if (needsMicrophone) {
            activity.requestMicrophonePermission(isMicGranted -> {
                if (isMicGranted) {
                    request.grant(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE});
                } else {
                    request.deny();
                }
            });
        }
    }
    // Kleine Hilfsmethode, um die Schleife sauber zu ersetzen
    private boolean containsResource(String[] resources, String targetResource) {
        if (resources == null) return false;
        for (String res : resources) {
            if (res.equals(targetResource)) return true;
        }
        return false;
    }
    @Override
    public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
        activity.requestLocationPermission(isGranted -> callback.invoke(origin, isGranted, false));
    }
    @Override
    public void onReceivedIcon(WebView view, Bitmap icon) {
        String url = ninjaWebView.getUrl();
        ImageView iv = ninjaWebView.getAlbumView().findViewById(R.id.item_icon);
        if (url == null) {
            iv.setImageResource(R.drawable.icon_image_broken);
        } else if (url.equals("about:blank")) {
            iv.setImageResource(R.drawable.icon_image_broken);
        } else if (BrowserUnit.isURL(url)) {
            ninjaWebView.setFavicon(icon);
            ninjaWebView.updateFavicon(ninjaWebView.getUrl());
        } else {
            iv.setImageResource(R.drawable.icon_image_broken);
        }
        super.onReceivedIcon(view, icon);
    }
    @Override
    public void onReceivedTitle(WebView view, String sTitle) {
        super.onReceivedTitle(view, sTitle);
        String url = ninjaWebView.getUrl();
        ImageView iv = ninjaWebView.getAlbumView().findViewById(R.id.item_icon);
        if (url == null) {
            iv.setImageResource(R.drawable.icon_image_broken);
        } else if (url.equals("about:blank")) {
            iv.setImageResource(R.drawable.icon_image_broken);
        } else if (BrowserUnit.isURL(url)) {
            ninjaWebView.updateFavicon(ninjaWebView.getUrl());
        } else {
            iv.setImageResource(R.drawable.icon_image_broken);
        }
    }
}