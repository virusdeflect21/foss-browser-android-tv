/*
    This file is part of the browser WebApp.

    browser WebApp is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    browser WebApp is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with the browser webview app.

    If not, see <http://www.gnu.org/licenses/>.
 */

package de.baumann.browser.unit;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.NestedScrollView;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import de.baumann.browser.R;
import de.baumann.browser.activity.BrowserActivity;
import de.baumann.browser.activity.Settings_Menu;
import de.baumann.browser.browser.BrowserController;
import de.baumann.browser.browser.List_standard;
import de.baumann.browser.database.FaviconHelper;
import de.baumann.browser.view.GridItem;
import de.baumann.browser.view.MenuItem;
import de.baumann.browser.view.NinjaWebView;

public class HelperUnit {
    private static SharedPreferences sp;

    public static void createShortcut(Context context, String title, String url) {
        if (context == null || url == null) return;

        Icon icon;
        try (FaviconHelper faviconHelper = new FaviconHelper(context)) {
            Bitmap favicon = faviconHelper.getFavicon(url);
            if (favicon != null && !favicon.isRecycled()) {
                icon = Icon.createWithBitmap(favicon);
            } else {
                int appIconResId = context.getApplicationInfo().icon;
                icon = Icon.createWithResource(context, appIconResId != 0 ? appIconResId : android.R.drawable.sym_def_app_icon);
            }
        } catch (Exception e) {
            android.util.Log.e("FOSS Browser", "Shortcut-Icon Fehler: " + e.getMessage());
            int appIconResId = context.getApplicationInfo().icon;
            icon = Icon.createWithResource(context, appIconResId != 0 ? appIconResId : android.R.drawable.sym_def_app_icon);
        }

        ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
        if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
            Intent shortcutIntent = new Intent(context, BrowserActivity.class)
                    .setAction(Intent.ACTION_VIEW)
                    .setData(Uri.parse(url));

            ShortcutInfo pinShortcutInfo = new ShortcutInfo.Builder(context, url)
                    .setShortLabel(title)
                    .setLongLabel(title)
                    .setIcon(icon)
                    .setIntent(shortcutIntent)
                    .build();

            shortcutManager.requestPinShortcut(pinShortcutInfo, null);
        }
    }

    public static String domain(String url) {
        if (url == null) {
            return "";
        } else {
            try {
                return Objects.requireNonNull(Uri.parse(url).getHost()).replace("www.", "").trim();
            } catch (Exception e) {
                return "";
            }
        }
    }
    public static void initTheme(Activity context) {
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        if (sp.getBoolean("useDynamicColor", false)) {
            switch (Objects.requireNonNull(sp.getString("sp_theme", "1"))) {
                case "2":
                    context.setTheme(R.style.AppTheme_wallpaper_day);
                    break;
                case "3":
                    context.setTheme(R.style.AppTheme_wallpaper_night);
                    break;
                default:
                    context.setTheme(R.style.AppTheme_wallpaper);
                    break;
            }
        } else {
            context.setTheme(R.style.AppTheme);
        }
    }

    public static void addFilterItems(Activity activity, List<GridItem> gridList) {
        if (activity == null || gridList == null) return;

        int[] colorResIds = {
                R.string.color_red, R.string.color_pink, R.string.color_purple, R.string.color_blue,
                R.string.color_teal, R.string.color_green, R.string.color_lime, R.string.color_yellow,
                R.string.color_orange, R.string.color_brown, R.string.color_grey, R.string.setting_theme_system
        };

        for (int i = 1; i <= 12; i++) {
            String suffix = String.format(java.util.Locale.US, "%02d", i);
            if (sp.getBoolean("filter_" + suffix, true)) {
                String defaultColorName = activity.getString(colorResIds[i - 1]);
                String title = sp.getString("icon_" + suffix, defaultColorName);
                int itemId = 12 - i;
                gridList.add(new GridItem(title, itemId));
            }
        }
    }

    public static void setFilterIcons(Context context, MaterialCardView ib_icon, long newIcon) {
        if (context == null || ib_icon == null) return;
        int iconIndex = (int) (newIcon & 15);
        int[] colors = {
                0, // Platzhalter für Index 0
                R.color.grey,   // 1
                R.color.brown,  // 2
                R.color.orange, // 3
                R.color.yellow, // 4
                R.color.lime,   // 5
                R.color.green,  // 6
                R.color.teal,   // 7
                R.color.blue,   // 8
                R.color.purple, // 9
                R.color.pink,   // 10
                R.color.red     // 11
        };
        if (iconIndex >= 1 && iconIndex <= 11) {
            ib_icon.setCardBackgroundColor(ContextCompat.getColor(context, colors[iconIndex]));
        } else if (iconIndex == 0) {
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSecondaryContainer, typedValue, true);
            ib_icon.setCardBackgroundColor(typedValue.data);
        }
    }
    public static void showSoftKeyboard(EditText editText) {
        if (editText == null) return;
        editText.requestFocus();
        editText.postDelayed(() -> {
            Context context = editText.getContext();
            if (context instanceof Activity activity) {
                WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView());
                controller.show(WindowInsetsCompat.Type.ime());
            } else {
                InputMethodManager imm = context.getSystemService(InputMethodManager.class);
                if (imm != null) {
                    imm.showSoftInput(editText, 0);
                }
            }
            editText.selectAll();
        }, 200);
    }

    public static void setupDialog(Context context, Dialog dialog) {
        TypedValue tv = new TypedValue();
        context.getTheme().resolveAttribute(androidx.appcompat.R.attr.colorError, tv, true);
        ImageView iv = dialog.findViewById(android.R.id.icon);
        if (iv != null) iv.setColorFilter(tv.data, PorterDuff.Mode.SRC_IN);
        Window window = dialog.getWindow();
        if (window == null) return;
        // TV: dialogs are centred overlays. The phone version anchored them to the bottom edge
        // (bottom sheet). The window size keeps them inside the overscan safe area of 48dp on
        // the sides and 27dp on top and bottom, see
        // https://developer.android.com/design/ui/tv/guides/styles/layouts
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        window.setGravity(Gravity.CENTER);
        window.setLayout(
                metrics.widthPixels - 2 * convertDpToPixel(48, context),
                metrics.heightPixels - 2 * convertDpToPixel(27, context));
    }
    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static int convertDpToPixel(float dp, Context context){
        return Math.round(dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public static void setHighLightedText(Context context, TextView tv, String url, String textToHighlight) {
        if (tv == null || textToHighlight == null || textToHighlight.isEmpty()) return;

        String originalText = tv.getText().toString();
        String lowerText = originalText.toLowerCase(java.util.Locale.ROOT);
        String lowerSearch = textToHighlight.toLowerCase(java.util.Locale.ROOT);

        Spannable wordToSpan = new SpannableString(originalText);

        TypedValue tvError = new TypedValue();
        context.getTheme().resolveAttribute(androidx.appcompat.R.attr.colorError, tvError, true);
        int colorError = tvError.data;

        TypedValue tvOnSurface = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, tvOnSurface, true);
        int colorOnSurface = tvOnSurface.data;

        boolean isWhite = false;
        try {
            isWhite = new List_standard(context).isWhite(url);
        } catch (Exception e) {
            Log.e(TAG, "Error loading lists: " + e);
        }

        int finalColor = isWhite ? colorError : colorOnSurface;
        int searchLength = lowerSearch.length();
        int ofe = lowerText.indexOf(lowerSearch);

        while (ofe != -1) {
            wordToSpan.setSpan(new ForegroundColorSpan(finalColor), ofe, ofe + searchLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ofe = lowerText.indexOf(lowerSearch, ofe + searchLength);
        }
        tv.setText(wordToSpan, TextView.BufferType.SPANNABLE);
        tv.setMaxLines(1);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        tv.setSingleLine(true);
        tv.setHorizontallyScrolling(true);
    }

    public static void setHighLightedTextSearch(Context context, TextView tv, String textToHighlight) {
        if (tv == null || textToHighlight == null || textToHighlight.isEmpty()) return;

        String originalText = tv.getText().toString();
        String lowerText = originalText.toLowerCase(java.util.Locale.ROOT);
        String lowerSearch = textToHighlight.toLowerCase(java.util.Locale.ROOT);

        Spannable wordToSpan = new SpannableString(originalText);
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(androidx.appcompat.R.attr.colorError, typedValue, true);
        int accentColor = typedValue.data;

        int searchLength = lowerSearch.length();
        int ofe = lowerText.indexOf(lowerSearch);
        while (ofe != -1) {
            wordToSpan.setSpan(new ForegroundColorSpan(accentColor), ofe, ofe + searchLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ofe = lowerText.indexOf(lowerSearch, ofe + searchLength);
        }
        tv.setText(wordToSpan, TextView.BufferType.SPANNABLE);
        tv.setMaxLines(1);
        tv.setEllipsize(TextUtils.TruncateAt.END);
    }
    public static void initAndLoadMenu(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Settings_Menu.PREF_NAME, Context.MODE_PRIVATE);
        String jsonString = sharedPreferences.getString(Settings_Menu.KEY_LIST, null);
        List<MenuItem> list = new ArrayList<>();

        if (jsonString != null) {
            try {
                org.json.JSONArray jsonArray = new org.json.JSONArray(jsonString);
                for (int i = 0; i < jsonArray.length(); i++) {
                    org.json.JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String title = jsonObject.optString("title", "");
                    int iconResId = jsonObject.optInt("iconResId", 0);
                    boolean isSelected = jsonObject.optBoolean("isSelected", true);
                    list.add(new MenuItem(title, iconResId, isSelected));
                }
            } catch (org.json.JSONException e) {
                android.util.Log.e("SettingsMenu", "Fehler beim Initialisieren der Liste", e);
                list.clear();
            }
        }

        if (list.isEmpty()) {
            list.add(new MenuItem(context.getString(R.string.menu_openFav), R.drawable.icon_fav, true));
            list.add(new MenuItem(context.getString(R.string.menu_fav), R.drawable.icon_fav_plus, true));
            list.add(new MenuItem(context.getString(R.string.main_menu_new_tabOpen), R.drawable.icon_tab_plus, true));
            list.add(new MenuItem(context.getString(R.string.main_menu_new_tab), R.drawable.icon_tab_background, true));
            list.add(new MenuItem(context.getString(R.string.menu_closeTab), R.drawable.icon_tab_remove, true));
            list.add(new MenuItem(context.getString(R.string.menu_other_searchSite), R.drawable.icon_search_site, true));
            list.add(new MenuItem(context.getString(R.string.menu_reload), R.drawable.icon_refresh, true));
            list.add(new MenuItem(context.getString(R.string.menu_share_link), R.drawable.icon_share, true));
            list.add(new MenuItem(context.getString(R.string.menu_shareClipboard), R.drawable.icon_clipboard, true));
            list.add(new MenuItem(context.getString(R.string.dialog_postOnWebsite), R.drawable.icon_post, true));
            list.add(new MenuItem(context.getString(R.string.menu_save_bookmark), R.drawable.icon_bookmark, true));
            list.add(new MenuItem(context.getString(R.string.menu_save_pdf), R.drawable.icon_file, true));
            list.add(new MenuItem(context.getString(R.string.menu_save_as), R.drawable.icon_menu_save, true));
            list.add(new MenuItem(context.getString(R.string.menu_shareOpenWith), R.drawable.icon_share_open_with, true));
            list.add(new MenuItem(context.getString(R.string.menu_sc), R.drawable.icon_home, true));
            list.add(new MenuItem(context.getString(R.string.menu_download), R.drawable.icon_download, true));
            list.add(new MenuItem(context.getString(R.string.setting_label), R.drawable.icon_settings, true));
            list.add(new MenuItem(context.getString(R.string.menu_restart), R.drawable.icon_restart, true));
            list.add(new MenuItem(context.getString(R.string.menu_quit), R.drawable.icon_close, true));
            list.add(new MenuItem(context.getString(R.string.menu_edit), R.drawable.icon_edit, true));
            list.add(new MenuItem(context.getString(R.string.menu_delete), R.drawable.icon_delete, true));
            list.add(new MenuItem(context.getString(R.string.menu_delete_entry), R.drawable.icon_delete_alt, true));
            list.add(new MenuItem(context.getString(R.string.app_help), R.drawable.icon_help, true));

            try {
                org.json.JSONArray jsonArray = new org.json.JSONArray();
                for (MenuItem item : list) {
                    org.json.JSONObject jsonObject = new org.json.JSONObject();
                    jsonObject.put("title", item.getTitle());
                    jsonObject.put("iconResId", item.getIconResId());
                    jsonObject.put("isSelected", item.isSelected());
                    jsonArray.put(jsonObject);
                }
                sharedPreferences.edit().putString(Settings_Menu.KEY_LIST, jsonArray.toString()).apply();
            } catch (org.json.JSONException e) {
                android.util.Log.e("SettingsMenu", "Fehler beim Speichern der Defaults", e);
            }
        }
    }
    public static void showCustomSnackbarWithTwoActions(
            Context context,
            View parentView,
            View anchorView,
            String title,
            String text,
            String link,
            int firstIconResId,
            BooleanSupplier firstActionListener,
            int secondIconResId,
            BooleanSupplier secondActionListener) {

        SpannableStringBuilder snackbarText = new SpannableStringBuilder();
        if (title != null && !title.isEmpty()) {
            snackbarText.append(title);
            snackbarText.setSpan(
                    new StyleSpan(Typeface.BOLD),
                    0,
                    title.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        if (text != null && !text.isEmpty()) {
            if (!TextUtils.isEmpty(snackbarText)) {
                snackbarText.append("\n\n");
            }
            snackbarText.append(text);
        }

        int currentNightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        int backgroundColor;
        ColorStateList contentColor;

        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            backgroundColor = Color.parseColor("#E0E0E0");
            contentColor = ColorStateList.valueOf(Color.BLACK);
        } else {
            backgroundColor = Color.parseColor("#323232");
            contentColor = ColorStateList.valueOf(Color.WHITE);
        }

        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimaryInverse, typedValue, true);
        int inversePrimaryColor = typedValue.data;

        LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams")
        View customView = inflater.inflate(R.layout.custom_snackbar, null);

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setColor(backgroundColor);
        shape.setCornerRadius(60f);
        customView.setBackground(shape);

        TextView textView = customView.findViewById(R.id.custom_snackbar_text);
        TextView urlTextView = customView.findViewById(R.id.custom_snackbar_url);

        Snackbar snackbar = Snackbar.make(parentView, "", Snackbar.LENGTH_INDEFINITE);
        if (anchorView != null) { snackbar.setAnchorView(anchorView); }

        ViewGroup layout = (ViewGroup) snackbar.getView();
        layout.removeAllViews();
        layout.setPadding(0, 0, 0, 0);
        layout.setBackgroundColor(Color.TRANSPARENT);

        NestedScrollView scrollView = customView.findViewById(R.id.snackbar_scrollview);

        if (textView != null) {
            textView.setText(snackbarText);
            textView.setTextColor(contentColor.getDefaultColor());
            textView.setTextIsSelectable(true);
            textView.setOnClickListener(null);
        }

        if (urlTextView != null) {
            if (link != null && !link.isEmpty()) {
                urlTextView.setVisibility(View.VISIBLE);
                urlTextView.setText(link);
                urlTextView.setTextColor(inversePrimaryColor);

                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) urlTextView.getLayoutParams();
                if (params != null) {
                    params.topMargin = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 16, context.getResources().getDisplayMetrics());
                    urlTextView.setLayoutParams(params);
                }

                if (!link.startsWith("data:") && !link.startsWith("blob:")) {
                    urlTextView.setClickable(true);
                    urlTextView.setFocusable(true);

                    TypedValue rippleValue = new TypedValue();
                    context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, rippleValue, true);
                    urlTextView.setBackgroundResource(rippleValue.resourceId);

                    urlTextView.setOnClickListener(v -> {
                        BrowserController browserController = NinjaWebView.getBrowserController();
                        if (browserController != null) {
                            browserController.hideOverflow();
                            browserController.showOverflow(null, urlTextView, 1, title, link, null, null, 0);
                        }
                        snackbar.dismiss();
                    });
                }
            } else {
                urlTextView.setVisibility(View.GONE);
            }
        }

        if (scrollView != null && textView != null) {
            scrollView.post(() -> {
                int maxPx = (parentView.getHeight() * 2) / 3;
                if (scrollView.getHeight() > maxPx) {
                    scrollView.getLayoutParams().height = maxPx;
                    scrollView.requestLayout();
                }
            });
        }

        MaterialButton firstButton = customView.findViewById(R.id.custom_btn_one);
        if (firstButton != null && firstIconResId != 0) {
            firstButton.setIconResource(firstIconResId);
            firstButton.setIconTint(contentColor);
            if (firstActionListener != null) {
                firstButton.setOnClickListener(v -> {
                    if (firstActionListener.getAsBoolean()) {
                        snackbar.dismiss();
                    }
                });
            }
        }

        MaterialButton secondButton = customView.findViewById(R.id.custom_btn_two);
        if (secondButton != null && secondIconResId != 0) {
            secondButton.setIconResource(secondIconResId);
            secondButton.setIconTint(contentColor);
            if (secondActionListener != null) {
                secondButton.setOnClickListener(v -> {
                    if (secondActionListener.getAsBoolean()) {
                        snackbar.dismiss();
                    }
                });
            }
        }

        layout.addView(customView, 0);
        snackbar.show();
    }
    public static void makeSnackbarRound (Snackbar snackbar) {
        View snackbarView = snackbar.getView();
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (textView != null) {
            textView.setMaxLines(30);
        }
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(60f);
        if (snackbarView.getBackground() instanceof ColorDrawable) {
            background.setColor(((ColorDrawable) snackbarView.getBackground()).getColor());
        } else {
            background.setColor(Color.parseColor("#323232"));
        }
        snackbarView.setBackground(background);
        snackbar.setTextMaxLines(100);
    }
}