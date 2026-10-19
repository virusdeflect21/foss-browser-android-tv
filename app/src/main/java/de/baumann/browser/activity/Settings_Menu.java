package de.baumann.browser.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import de.baumann.browser.R;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.view.AdapterSettingsMenu;
import de.baumann.browser.view.MenuItem;

public class Settings_Menu extends AppCompatActivity {

    private List<MenuItem> masterList;
    private AdapterSettingsMenu adapter;
    private SharedPreferences sharedPreferences;

    public static final String PREF_NAME = "MenuPreferences";
    public static final String KEY_LIST = "MenuList";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HelperUnit.initTheme(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings_menu);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadList();

        RecyclerView recyclerView = findViewById(R.id.recyclerViewSettings);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (masterList != null) {
            for (MenuItem item : masterList) {
                String aktuellerTitel = item.getTitle();
                if (aktuellerTitel == null) continue;
                if (aktuellerTitel.equals(getString(R.string.menu_openFav))) {item.setIconResId(R.drawable.icon_fav);}
                else if (aktuellerTitel.equals(getString(R.string.main_menu_new_tabOpen))) {item.setIconResId(R.drawable.icon_tab_plus);}
                else if (aktuellerTitel.equals(getString(R.string.main_menu_new_tab))) {item.setIconResId(R.drawable.icon_tab_background);}
                else if (aktuellerTitel.equals(getString(R.string.menu_other_searchSite))) {item.setIconResId(R.drawable.icon_search_site);}
                else if (aktuellerTitel.equals(getString(R.string.menu_closeTab))) {item.setIconResId(R.drawable.icon_tab_remove);}
                else if (aktuellerTitel.equals(getString(R.string.menu_reload))) {item.setIconResId(R.drawable.icon_refresh);}
                else if (aktuellerTitel.equals(getString(R.string.menu_share_link))) {item.setIconResId(R.drawable.icon_share);}
                else if (aktuellerTitel.equals(getString(R.string.menu_shareClipboard))) {item.setIconResId(R.drawable.icon_clipboard);}
                else if (aktuellerTitel.equals(getString(R.string.dialog_postOnWebsite))) {item.setIconResId(R.drawable.icon_post);}
                else if (aktuellerTitel.equals(getString(R.string.menu_save_bookmark))) {item.setIconResId(R.drawable.icon_bookmark);}
                else if (aktuellerTitel.equals(getString(R.string.menu_save_pdf))) {item.setIconResId(R.drawable.icon_file);}
                else if (aktuellerTitel.equals(getString(R.string.menu_save_as))) {item.setIconResId(R.drawable.icon_menu_save);}
                else if (aktuellerTitel.equals(getString(R.string.menu_shareOpenWith))) {item.setIconResId(R.drawable.icon_share_open_with);}
                else if (aktuellerTitel.equals(getString(R.string.menu_sc))) {item.setIconResId(R.drawable.icon_home);}
                else if (aktuellerTitel.equals(getString(R.string.menu_download))) {item.setIconResId(R.drawable.icon_download);}
                else if (aktuellerTitel.equals(getString(R.string.menu_quit))) {item.setIconResId(R.drawable.icon_close);}
                else if (aktuellerTitel.equals(getString(R.string.setting_label))) {item.setIconResId(R.drawable.icon_settings);}
                else if (aktuellerTitel.equals(getString(R.string.menu_restart))) {item.setIconResId(R.drawable.icon_restart);}
                else if (aktuellerTitel.equals(getString(R.string.menu_edit))) {item.setIconResId(R.drawable.icon_edit);}
                else if (aktuellerTitel.equals(getString(R.string.menu_delete))) {item.setIconResId(R.drawable.icon_delete);}
                else if (aktuellerTitel.equals(getString(R.string.menu_delete_entry))) {item.setIconResId(R.drawable.icon_delete_alt);}
                else if (aktuellerTitel.equals(getString(R.string.app_help))) {item.setIconResId(R.drawable.icon_help);}
                else if (aktuellerTitel.equals(getString(R.string.menu_fav))) {item.setIconResId(R.drawable.icon_fav_plus);}
            }
        }
        adapter = new AdapterSettingsMenu(masterList);
        recyclerView.setAdapter(adapter);

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getBindingAdapterPosition();
                int toPosition = target.getBindingAdapterPosition();
                if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) {
                    return false;
                }
                Collections.swap(masterList, fromPosition, toPosition);
                adapter.notifyItemMoved(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
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
            Uri webpage = Uri.parse("https://codeberg.org/Gaukler_Faun/FOSS_Browser/wiki/Menu");
            BrowserUnit.intentURL(this, webpage);
        }
        return true;
    }

    // Garantiert, dass die Liste gespeichert wird, wenn die Activity in den Hintergrund tritt oder geschlossen wird
    @Override
    protected void onPause() {
        super.onPause();
        saveList();
    }

    private void loadList() {
        masterList = new ArrayList<>();
        String jsonString = sharedPreferences.getString(KEY_LIST, null);

        if (jsonString != null) {
            try {
                org.json.JSONArray jsonArray = new org.json.JSONArray(jsonString);
                for (int i = 0; i < jsonArray.length(); i++) {
                    org.json.JSONObject jsonObject = jsonArray.getJSONObject(i);

                    String title = jsonObject.optString("title", "");
                    int iconResId = jsonObject.optInt("iconResId", 0);
                    boolean isSelected = jsonObject.optBoolean("isSelected", true);

                    masterList.add(new MenuItem(title, iconResId, isSelected));
                }
            } catch (org.json.JSONException e) {
                android.util.Log.e("Storage", "Fehler beim Laden der JSON-Menüliste", e);
                masterList = new ArrayList<>();
            }
        }
    }

    private void saveList() {
        if (masterList == null) return;
        try {
            org.json.JSONArray jsonArray = new org.json.JSONArray();
            for (MenuItem item : masterList) {
                org.json.JSONObject jsonObject = new org.json.JSONObject();

                jsonObject.put("title", item.getTitle());
                jsonObject.put("iconResId", item.getIconResId());
                jsonObject.put("isSelected", item.isSelected());

                jsonArray.put(jsonObject);
            }

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_LIST, jsonArray.toString());
            editor.apply();
        } catch (org.json.JSONException e) {
            android.util.Log.e("Storage", "Fehler beim Speichern der JSON-Menüliste", e);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}