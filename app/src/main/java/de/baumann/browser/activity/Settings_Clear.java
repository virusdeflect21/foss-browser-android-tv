package de.baumann.browser.activity;

import de.baumann.browser.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;

import java.util.Objects;

import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.DeleteUtils;
import de.baumann.browser.unit.HelperUnit;

public class Settings_Clear extends AppCompatActivity {

    private MaterialCheckBox cbCache, cbCookies, cbDatabase, cbSettings, cbOther, cbAutoDelete;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        HelperUnit.initTheme(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings_clear);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        prefs = getSharedPreferences("clear_data_settings", Context.MODE_PRIVATE);
        cbCache = findViewById(R.id.cb_cache);
        cbCookies = findViewById(R.id.cb_cookies);
        cbDatabase = findViewById(R.id.cb_database);
        cbSettings = findViewById(R.id.cb_settings);
        cbOther = findViewById(R.id.cb_other_webview);
        cbAutoDelete = findViewById(R.id.cb_auto_delete_exit);
        MaterialButton btnClearNow = findViewById(R.id.btn_clear_now);
        loadPrefs();
        setupCheckboxListeners();

        btnClearNow.setOnClickListener(v -> {
            LinearLayout root = findViewById(R.id.root);
            Snackbar snackbarBottom = Snackbar.make(root, R.string.hint_database, Snackbar.LENGTH_SHORT);
            HelperUnit.makeSnackbarRound(snackbarBottom);
            snackbarBottom.setAction(this.getString(R.string.app_ok), r -> {

                DeleteUtils.executeDeletion(
                        this,
                        cbCache.isChecked(),
                        cbCookies.isChecked(),
                        cbDatabase.isChecked(),
                        cbSettings.isChecked(),
                        cbOther.isChecked()
                );

                if (cbSettings.isChecked()) {
                    finish();
                }
            });

            snackbarBottom.show();
        });
    }

    private void loadPrefs() {
        cbCache.setChecked(prefs.getBoolean("delete_cache", false));
        cbCookies.setChecked(prefs.getBoolean("delete_cookies", false));
        cbDatabase.setChecked(prefs.getBoolean("delete_database", false));
        cbSettings.setChecked(prefs.getBoolean("delete_settings", false));
        cbOther.setChecked(prefs.getBoolean("delete_other", false));
        cbAutoDelete.setChecked(prefs.getBoolean("auto_delete_exit", false));
    }

    private void setupCheckboxListeners() {
        cbCache.setOnCheckedChangeListener((b, checked) -> prefs.edit().putBoolean("delete_cache", checked).apply());
        cbCookies.setOnCheckedChangeListener((b, checked) -> prefs.edit().putBoolean("delete_cookies", checked).apply());
        cbDatabase.setOnCheckedChangeListener((b, checked) -> prefs.edit().putBoolean("delete_database", checked).apply());
        cbSettings.setOnCheckedChangeListener((b, checked) -> prefs.edit().putBoolean("delete_settings", checked).apply());
        cbOther.setOnCheckedChangeListener((b, checked) -> prefs.edit().putBoolean("delete_other", checked).apply());
        cbAutoDelete.setOnCheckedChangeListener((b, checked) -> prefs.edit().putBoolean("auto_delete_exit", checked).apply());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_help, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) finish();
        else if (menuItem.getItemId() == R.id.menu_help) {
            Uri webpage = Uri.parse("https://codeberg.org/Gaukler_Faun/FOSS_Browser/wiki/Delete");
            BrowserUnit.intentURL(this, webpage);
        }
        return true;
    }
}