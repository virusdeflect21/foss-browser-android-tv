package de.baumann.browser.browser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Message;
import android.text.InputType;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.HttpAuthHandler;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayInputStream;
import java.util.Objects;

import de.baumann.browser.R;
import de.baumann.browser.database.Record;
import de.baumann.browser.database.RecordAction;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.unit.RecordUnit;
import de.baumann.browser.view.NinjaToast;
import de.baumann.browser.view.NinjaWebView;

public class NinjaWebViewClient extends WebViewClient {

    private final NinjaWebView ninjaWebView;
    private final Context context;
    private final SharedPreferences sp;
    private final AdBlock adBlock;

    public NinjaWebViewClient(NinjaWebView ninjaWebView) {
        this.ninjaWebView = ninjaWebView;
        this.context = ninjaWebView.getContext();
        this.sp = PreferenceManager.getDefaultSharedPreferences(context);
        this.adBlock = new AdBlock(this.context);
    }

    @Override
    public boolean onRenderProcessGone (WebView view, RenderProcessGoneDetail detail){
        String text = context.getString(R.string.app_error) + ": " + detail.toString();
        NinjaToast.show(context, text);
        view.reload();
        return true;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);

        if (ninjaWebView.isForeground()) ninjaWebView.invalidate();
        else ninjaWebView.postInvalidate();
        CookieManager.getInstance().flush();

        if (ninjaWebView.isSaveData())
            view.evaluateJavascript("var links=document.getElementsByTagName('video'); for(let i=0;i<links.length;i++){links[i].pause()};", null);

        if (ninjaWebView.isHistory()) {
            RecordAction action = new RecordAction(ninjaWebView.getContext());
            action.open(true);
            if (action.checkUrl(ninjaWebView.getUrl(), RecordUnit.TABLE_HISTORY)) action.deleteURL(ninjaWebView.getUrl(), RecordUnit.TABLE_HISTORY);
            action.addHistory(new Record(ninjaWebView.getTitle(), ninjaWebView.getUrl(), System.currentTimeMillis(), 0));
            action.close();
        }

        String profile = NinjaWebView.getProfile();
        if (sp.getBoolean(profile + "_deny_cookie_banners",false)){
            //click opt-out if possible
            String bannerBlockScript = BannerBlock.getBannerBlockScriptPageFinished();
            if (bannerBlockScript != null) view.evaluateJavascript(bannerBlockScript,null);
        }
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {

        ninjaWebView.setStopped(false);
        ninjaWebView.resetFavicon();

        super.onPageStarted(view, url, favicon);

        String profile = NinjaWebView.getProfile();
        if (sp.getBoolean(profile + "_deny_cookie_banners",false)){
            //click opt-out if possible
            String bannerBlockScript = BannerBlock.getBannerBlockScriptPageStarted();
            if (bannerBlockScript != null) view.evaluateJavascript(bannerBlockScript,null);
        }

        if (ninjaWebView.isFingerPrintProtection()) {
            //Block WebRTC requests which can reveal local IP address
            //Tested with https://diafygi.github.io/webrtc-ips/
            view.evaluateJavascript("""
                    ['createOffer', 'createAnswer','setLocalDescription', 'setRemoteDescription'].forEach(function(method) {
                        webkitRTCPeerConnection.prototype[method] = function() {
                          console.log('webRTC snoop');
                          return null;
                        };
                      });""", null);

            //Prevent canvas fingerprinting by randomizing
            //can be tested e.g. at https://webbrowsertools.com
            //
            //The Javascript part below is taken from "Canvas Fingerprint Defender", Firefox plugin, Version 0.1.9, by ilGur
            //The source code has been published originally under Mozilla Public License V2.0. You can obtain a copy of the license at https://mozilla.org/MPL/2.0/
            //The author has given explicit written permission to use his code under GPL V3 in this project.

            view.evaluateJavascript("""
                    
                      const toBlob = HTMLCanvasElement.prototype.toBlob;
                      const toDataURL = HTMLCanvasElement.prototype.toDataURL;
                      const getImageData = CanvasRenderingContext2D.prototype.getImageData;
                      //
                      var noisify = function (canvas, context) {
                        if (context) {
                          const shift = {
                            'r': Math.floor(Math.random() * 10) - 5,
                            'g': Math.floor(Math.random() * 10) - 5,
                            'b': Math.floor(Math.random() * 10) - 5,
                            'a': Math.floor(Math.random() * 10) - 5
                          };
                          //
                          const width = canvas.width;
                          const height = canvas.height;
                          if (width && height) {
                            const imageData = getImageData.apply(context, [0, 0, width, height]);
                            for (let i = 0; i < height; i++) {
                              for (let j = 0; j < width; j++) {
                                const n = ((i * (width * 4)) + (j * 4));
                                imageData.data[n + 0] = imageData.data[n + 0] + shift.r;
                                imageData.data[n + 1] = imageData.data[n + 1] + shift.g;
                                imageData.data[n + 2] = imageData.data[n + 2] + shift.b;
                                imageData.data[n + 3] = imageData.data[n + 3] + shift.a;
                              }
                            }
                            //
                            window.top.postMessage("canvas-fingerprint-defender-alert", '*');
                            context.putImageData(imageData, 0, 0);\s
                          }
                        }
                      };
                      //
                      Object.defineProperty(HTMLCanvasElement.prototype, "toBlob", {
                        "value": function () {
                          noisify(this, this.getContext("2d"));
                          return toBlob.apply(this, arguments);
                        }
                      });
                      //
                      Object.defineProperty(HTMLCanvasElement.prototype, "toDataURL", {
                        "value": function () {
                          noisify(this, this.getContext("2d"));
                          return toDataURL.apply(this, arguments);
                        }
                      });
                      //
                      Object.defineProperty(CanvasRenderingContext2D.prototype, "getImageData", {
                        "value": function () {
                          noisify(this.canvas, this);
                          return getImageData.apply(this, arguments);
                        }
                      });\
                    """, null);

            //Prevent WebGL fingerprinting by randomizing
            //can be tested e.g. at https://webbrowsertools.com
            //
            //The Javascript part below is taken from "WebGL Fingerprint Defender", Firefox plugin, Version 0.1.5, by ilGur
            //The source code has been published originally under Mozilla Public License V2.0. You can obtain a copy of the license at https://mozilla.org/MPL/2.0/
            //The author has given explicit written permission to use his code under GPL V3 in this project.

            view.evaluateJavascript("""
                    
                      var config = {
                        "random": {
                          "value": function () {
                            return Math.random();
                          },
                          "item": function (e) {
                            var rand = e.length * config.random.value();
                            return e[Math.floor(rand)];
                          },
                          "number": function (power) {
                            var tmp = [];
                            for (var i = 0; i < power.length; i++) {
                              tmp.push(Math.pow(2, power[i]));
                            }
                            /*  */
                            return config.random.item(tmp);
                          },
                          "int": function (power) {
                            var tmp = [];
                            for (var i = 0; i < power.length; i++) {
                              var n = Math.pow(2, power[i]);
                              tmp.push(new Int32Array([n, n]));
                            }
                            /*  */
                            return config.random.item(tmp);
                          },
                          "float": function (power) {
                            var tmp = [];
                            for (var i = 0; i < power.length; i++) {
                              var n = Math.pow(2, power[i]);
                              tmp.push(new Float32Array([1, n]));
                            }
                            /*  */
                            return config.random.item(tmp);
                          }
                        },
                        "spoof": {
                          "webgl": {
                            "buffer": function (target) {
                              var proto = target.prototype ? target.prototype : target.__proto__;
                              const bufferData = proto.bufferData;
                              Object.defineProperty(proto, "bufferData", {
                                "value": function () {
                                  var index = Math.floor(config.random.value() * arguments[1].length);
                                  var noise = arguments[1][index] !== undefined ? 0.1 * config.random.value() * arguments[1][index] : 0;
                                  //
                                  arguments[1][index] = arguments[1][index] + noise;
                                  window.top.postMessage("webgl-fingerprint-defender-alert", '*');
                                  //
                                  return bufferData.apply(this, arguments);
                                }
                              });
                            },
                            "parameter": function (target) {
                              var proto = target.prototype ? target.prototype : target.__proto__;
                              const getParameter = proto.getParameter;
                              Object.defineProperty(proto, "getParameter", {
                                "value": function () {
                                  window.top.postMessage("webgl-fingerprint-defender-alert", '*');
                                  //
                                  if (arguments[0] === 3415) return 0;
                                  else if (arguments[0] === 3414) return 24;
                                  else if (arguments[0] === 36348) return 30;
                                  else if (arguments[0] === 7936) return "WebKit";
                                  else if (arguments[0] === 37445) return "Google Inc.";
                                  else if (arguments[0] === 7937) return "WebKit WebGL";
                                  else if (arguments[0] === 3379) return config.random.number([14, 15]);
                                  else if (arguments[0] === 36347) return config.random.number([12, 13]);
                                  else if (arguments[0] === 34076) return config.random.number([14, 15]);
                                  else if (arguments[0] === 34024) return config.random.number([14, 15]);
                                  else if (arguments[0] === 3386) return config.random.int([13, 14, 15]);
                                  else if (arguments[0] === 3413) return config.random.number([1, 2, 3, 4]);
                                  else if (arguments[0] === 3412) return config.random.number([1, 2, 3, 4]);
                                  else if (arguments[0] === 3411) return config.random.number([1, 2, 3, 4]);
                                  else if (arguments[0] === 3410) return config.random.number([1, 2, 3, 4]);
                                  else if (arguments[0] === 34047) return config.random.number([1, 2, 3, 4]);
                                  else if (arguments[0] === 34930) return config.random.number([1, 2, 3, 4]);
                                  else if (arguments[0] === 34921) return config.random.number([1, 2, 3, 4]);
                                  else if (arguments[0] === 35660) return config.random.number([1, 2, 3, 4]);
                                  else if (arguments[0] === 35661) return config.random.number([4, 5, 6, 7, 8]);
                                  else if (arguments[0] === 36349) return config.random.number([10, 11, 12, 13]);
                                  else if (arguments[0] === 33902) return config.random.float([0, 10, 11, 12, 13]);
                                  else if (arguments[0] === 33901) return config.random.float([0, 10, 11, 12, 13]);
                                  else if (arguments[0] === 37446) return config.random.item(["Graphics", "HD Graphics", "Intel(R) HD Graphics"]);
                                  else if (arguments[0] === 7938) return config.random.item(["WebGL 1.0", "WebGL 1.0 (OpenGL)", "WebGL 1.0 (OpenGL Chromium)"]);
                                  else if (arguments[0] === 35724) return config.random.item(["WebGL", "WebGL GLSL", "WebGL GLSL ES", "WebGL GLSL ES (OpenGL Chromium"]);
                                  //
                                  return getParameter.apply(this, arguments);
                                }
                              });
                            }
                          }
                        }
                      };
                      //
                      config.spoof.webgl.buffer(WebGLRenderingContext);
                      config.spoof.webgl.buffer(WebGL2RenderingContext);
                      config.spoof.webgl.parameter(WebGLRenderingContext);
                      config.spoof.webgl.parameter(WebGL2RenderingContext);\
                    """, null);

            //Prevent AudioContext fingerprinting by randomizing
            //can be tested e.g. at https://webbrowsertools.com
            //
            //The Javascript part below is taken from "AudioContext Fingerprint Defender", Firefox plugin, Version 0.1.6, by ilGur
            //The source code has been published originally under Mozilla Public License V2.0. You can obtain a copy of the license at https://mozilla.org/MPL/2.0/
            //The author has given explicit written permission to use his code under GPL V3 in this project.

            view.evaluateJavascript("""
                    
                        const context = {
                        "BUFFER": null,
                        "getChannelData": function (e) {
                          const getChannelData = e.prototype.getChannelData;
                          Object.defineProperty(e.prototype, "getChannelData", {
                            "value": function () {
                              const results_1 = getChannelData.apply(this, arguments);
                              if (context.BUFFER !== results_1) {
                                context.BUFFER = results_1;
                                for (var i = 0; i < results_1.length; i += 100) {
                                  let index = Math.floor(Math.random() * i);
                                  results_1[index] = results_1[index] + Math.random() * 0.0000001;
                                }
                              }
                              //
                              return results_1;
                            }
                          });
                        },
                        "createAnalyser": function (e) {
                          const createAnalyser = e.prototype.__proto__.createAnalyser;
                          Object.defineProperty(e.prototype.__proto__, "createAnalyser", {
                            "value": function () {
                              const results_2 = createAnalyser.apply(this, arguments);
                              const getFloatFrequencyData = results_2.__proto__.getFloatFrequencyData;
                              Object.defineProperty(results_2.__proto__, "getFloatFrequencyData", {
                                "value": function () {
                                  const results_3 = getFloatFrequencyData.apply(this, arguments);
                                  for (var i = 0; i < arguments[0].length; i += 100) {
                                    let index = Math.floor(Math.random() * i);
                                    arguments[0][index] = arguments[0][index] + Math.random() * 0.1;
                                  }
                                  //
                                  return results_3;
                                }
                              });
                              //
                              return results_2;
                            }
                          });
                        }
                      };
                      //
                      context.getChannelData(AudioBuffer);
                      context.createAnalyser(AudioContext);
                      context.getChannelData(OfflineAudioContext);
                      context.createAnalyser(OfflineAudioContext);  \
                    """, null);

            //Prevent Font fingerprinting by randomizing
            //can be tested e.g. at https://webbrowsertools.com
            //
            //The Javascript part below is taken from "Font Fingerprint Defender", Firefox plugin, Version 0.1.3, by ilGur
            //The source code has been published originally under Mozilla Public License V2.0. You can obtain a copy of the license at https://mozilla.org/MPL/2.0/
            //The author has given explicit written permission to use his code under GPL V3 in this project.

            view.evaluateJavascript("""
                    
                      var rand = {
                        "noise": function () {
                          var SIGN = Math.random() < Math.random() ? -1 : 1;
                          return Math.floor(Math.random() + SIGN * Math.random());
                        },
                        "sign": function () {
                          const tmp = [-1, -1, -1, -1, -1, -1, +1, -1, -1, -1];
                          const index = Math.floor(Math.random() * tmp.length);
                          return tmp[index];
                        }
                      };
                      //
                      Object.defineProperty(HTMLElement.prototype, "offsetHeight", {
                        get () {
                          const height = Math.floor(this.getBoundingClientRect().height);
                          const valid = height && rand.sign() === 1;
                          const result = valid ? height + rand.noise() : height;
                          //
                          if (valid && result !== height) {
                            window.top.postMessage("font-fingerprint-defender-alert", '*');
                          }
                          //
                          return result;
                        }
                      });
                      //
                      Object.defineProperty(HTMLElement.prototype, "offsetWidth", {
                        get () {
                          const width = Math.floor(this.getBoundingClientRect().width);
                          const valid = width && rand.sign() === 1;
                          const result = valid ? width + rand.noise() : width;
                          //
                          if (valid && result !== width) {
                            window.top.postMessage("font-fingerprint-defender-alert", '*');
                          }
                          //
                          return result;
                        }
                      });\
                    """, null);

            //Spoof screen resolution, color depth: set values like in Tor browser, random values for device memory, hardwareConcurrency, remove battery, network connection, keyboard, media devices info, prevent sendBeacon

            //noinspection ConcatenationWithEmptyString
            view.evaluateJavascript("" +
                    "Object.defineProperty(window, 'devicePixelRatio',{value:1});" +
                    "Object.defineProperty(window.screen, 'width',{value:1000});" +
                    "Object.defineProperty(window.screen, 'availWidth',{value:1000});" +
                    "Object.defineProperty(window.screen, 'height',{value:900});" +
                    "Object.defineProperty(window.screen, 'availHeight',{value:900});" +
                    "Object.defineProperty(window.screen, 'colorDepth',{value:24});" +
                    "Object.defineProperty(window, 'outerWidth',{value:1000});" +
                    "Object.defineProperty(window, 'outerHeight',{value:900});" +
                    "Object.defineProperty(window, 'innerWidth',{value:1000});" +
                    "Object.defineProperty(window, 'innerHeight',{value:900});" +
                    "Object.defineProperty(navigator, 'getBattery',{value:function(){}});" +
                    "const ram=Math.pow(2,Math.floor(Math.random() * 4));Object.defineProperty(navigator, 'deviceMemory',{value:ram});" +
                    "const hw=Math.pow(2,Math.floor(Math.random() * 4));Object.defineProperty(navigator, 'hardwareConcurrency',{value:hw});" +
                    "Object.defineProperty(navigator, 'connection',{value:null});" +
                    "Object.defineProperty(navigator, 'keyboard',{value:null});" +
                    "Object.defineProperty(navigator, 'sendBeacon',{value:null});", null);

            if (!ninjaWebView.isCamera())
                //noinspection ConcatenationWithEmptyString
                view.evaluateJavascript("" + "Object.defineProperty(navigator, 'mediaDevices',{value:null});", null);
        }
    }

    @Override
    public void onLoadResource(WebView view, String url) {
        if (ninjaWebView.isFingerPrintProtection()) {
            view.evaluateJavascript("var test=document.querySelector(\"a[ping]\"); if(test!==null){test.removeAttribute('ping')};", null);
            //do not allow ping on http only pages (tested with http://tests.caniuse.com)
            if (view.getSettings().getUseWideViewPort() && (view.getWidth() < 1300)){
                view.evaluateJavascript("document.querySelector('meta[name=\"viewport\"]').setAttribute('content', 'width=1200px');", null);
            }
            //  Client-side detection for GlobalPrivacyControl
            view.evaluateJavascript("if (navigator.globalPrivacyControl === undefined) { Object.defineProperty(navigator, 'globalPrivacyControl', { value: true, writable: false,configurable: false});} else {try { navigator.globalPrivacyControl = true;} catch (e) { console.error('globalPrivacyControl is not writable: ', e); }};", null);
            //  Script taken from:
            //
            //  donotsell.js
            //  DuckDuckGo
            //
            //  Copyright © 2020 DuckDuckGo. All rights reserved.
            //
            //  Licensed under the Apache License, Version 2.0 (the "License");
            //  you may not use this file except in compliance with the License.
            //  You may obtain a copy of the License at
            //
            //  http://www.apache.org/licenses/LICENSE-2.0
            //
            //  Unless required by applicable law or agreed to in writing, software
            //  distributed under the License is distributed on an "AS IS" BASIS,
            //  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
            //  See the License for the specific language governing permissions and
            //  limitations under the License.
            //
            view.evaluateJavascript("if (navigator.doNotTrack === null) { Object.defineProperty(navigator, 'doNotTrack', { value: 1, writable: false,configurable: false});} else {try { navigator.doNotTrack = 1;} catch (e) { console.error('doNotTrack is not writable: ', e); }};", null);
            view.evaluateJavascript("if (window.doNotTrack === undefined) { Object.defineProperty(window, 'doNotTrack', { value: 1, writable: false,configurable: false});} else {try { window.doNotTrack = 1;} catch (e) { console.error('doNotTrack is not writable: ', e); }};", null);
            view.evaluateJavascript("if (navigator.msDoNotTrack === undefined) { Object.defineProperty(navigator, 'msDoNotTrack', { value: 1, writable: false,configurable: false});} else {try { navigator.msDoNotTrack = 1;} catch (e) { console.error('msDoNotTrack is not writable: ', e); }};", null);
        }
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        final Uri uri = request.getUrl();
        String url = uri.toString();
        if (url.startsWith("http:") || url.startsWith("https:")) {
            // The web view loads http(s) links itself (returning false keeps form posts
            // and the request data). The old "navigate to the previous history entry when
            // the same url comes again after Back" workaround was removed together with
            // the custom redirects: it sent the user to an older page on ordinary links.
            view.loadUrl(url);
            return false;
        } else{
            // 1. Prüfen, ob es sich um eine fehlerhafte lokale Google-Weiterleitung handelt
            if (url.startsWith("file:///httpservice/")) {
                // Ignorieren, da es ein ungültiger lokaler Pfad ist
                return true;
            }

            // 2. Sicherheits-Check für alle anderen file:// URLs
            if (url.startsWith("file://")) {
                // Option A: Einfach blockieren, damit es nicht abstürzt
                // return true;

                // Option B: Wenn du lokale Dateien absichtlich teilen willst,
                // musst du sie über den FileProvider jagen (wie in der vorherigen Nachricht beschrieben).
                // Für den Google-Fehler reicht es aber, das oben in Schritt 1 abzufangen.
                return true;
            }

            // Normaler Ablauf für http/https Links
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            context.startActivity(Intent.createChooser(intent, url));
            return true;
        }
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        if (ninjaWebView.isAdBlock() && adBlock.isAd(request.getUrl().toString()))
            return new WebResourceResponse(
                    BrowserUnit.MIME_TYPE_TEXT_PLAIN,
                    BrowserUnit.URL_ENCODING,
                    new ByteArrayInputStream("".getBytes())
            );
        return super.shouldInterceptRequest(view, request);
    }
    @Override
    public void onFormResubmission(WebView view, @NonNull final Message doNotResend, final Message resend) {
        HelperUnit.showCustomSnackbarWithTwoActions(
                context, view, null,
                view.getTitle(), context.getString(R.string.dialog_content_resubmission),view.getUrl(),
                R.drawable.icon_check, () -> {
                    resend.sendToTarget();
                    return true;
                },
                R.drawable.icon_close, () -> {
                    doNotResend.sendToTarget();
                    return true;
                }
        );
    }

    @SuppressLint("WebViewClientOnReceivedSslError")
    @Override
    public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
        String message;
        view.stopLoading();
        message = switch (error.getPrimaryError()) {
            case SslError.SSL_UNTRUSTED -> "\"Certificate authority is not trusted.\"";
            case SslError.SSL_EXPIRED -> "\"Certificate has expired.\"";
            case SslError.SSL_IDMISMATCH -> "\"Certificate Hostname mismatch.\"";
            case SslError.SSL_NOTYETVALID -> "\"Certificate is not yet valid.\"";
            case SslError.SSL_DATE_INVALID -> "\"Certificate date is invalid.\"";
            default -> "\"Certificate is invalid.\"";
        };
        String text = message + " - " + context.getString(R.string.dialog_content_ssl_error);

        HelperUnit.showCustomSnackbarWithTwoActions(
                context, view, null,
                view.getTitle(), text, ninjaWebView.getUrl(),
                R.drawable.icon_check, () -> {
                    handler.proceed();
                    ninjaWebView.reload();
                    return true;
                },
                R.drawable.icon_close, () -> {
                    handler.cancel();
                    return true;
                }
        );
    }

    @Override
    public void onReceivedHttpAuthRequest(WebView view, @NonNull final HttpAuthHandler handler, String host, String realm) {

        view.stopLoading();
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = View.inflate(context, R.layout.dialog_edit, null);

        TextInputLayout editBottomLayout = dialogView.findViewById(R.id.editBottomLayout);
        TextInputLayout editTopLayout = dialogView.findViewById(R.id.editTopLayout);
        editBottomLayout.setHint(this.context.getString(R.string.dialog_sign_in_password));
        editTopLayout.setHint(this.context.getString(R.string.dialog_sign_in_username));
        EditText editTop = dialogView.findViewById(R.id.editTop);
        EditText editBottom = dialogView.findViewById(R.id.editBottom);

        editTop.setText("");
        editBottom.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        editBottom.setText("");

        builder.setTitle(view.getTitle());
        builder.setIcon(R.drawable.icon_alert);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();
        HelperUnit.setupDialog(context, dialog);
        dialog.setOnCancelListener(dialog1 -> {
            handler.cancel();
            dialog1.cancel();
        });

        Button ib_cancel = dialogView.findViewById(R.id.editCancel);
        ib_cancel.setOnClickListener(v -> dialog.cancel());
        Button ib_ok = dialogView.findViewById(R.id.editOK);
        ib_ok.setOnClickListener(v -> {
            String user = Objects.requireNonNull(editTop.getText()).toString().trim();
            String pass = Objects.requireNonNull(editBottom.getText()).toString().trim();
            handler.proceed(user, pass);
            dialog.cancel();
        });
    }
}