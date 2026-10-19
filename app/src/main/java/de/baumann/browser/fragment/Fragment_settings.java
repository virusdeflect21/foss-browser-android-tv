package de.baumann.browser.fragment;

import static android.content.ContentValues.TAG;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import java.util.Objects;
import de.baumann.browser.R;
import de.baumann.browser.activity.*;
import de.baumann.browser.browser.AdBlock;
import de.baumann.browser.preferences.BasePreferenceFragment;

public class Fragment_settings extends BasePreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Preference sp_ad_block;
    private SharedPreferences sp;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_setting, rootKey);
        Context context = requireContext();
        sp = PreferenceManager.getDefaultSharedPreferences(context);

        initSummary(getPreferenceScreen());

        sp_ad_block = findPreference("ab_hosts");
        if (sp_ad_block != null) sp_ad_block.setSummary(AdBlock.getHostsDate(context));

        setClick("settings_profile", Settings_Profile.class);
        setClick("settings_menu", Settings_Menu.class);
        setClick("settings_filter", Settings_Filter.class);
        setClick("settings_data", Settings_Backup.class);
        setClick("settings_gesture", Settings_Gesture.class);
        setClick("settings_clear", Settings_Clear.class);

        Preference edit_standard = findPreference("edit_standard");
        if (edit_standard != null) {
            edit_standard.setOnPreferenceClickListener(p -> {
                sp.edit().putString("listToLoad", "standard").apply();
                startActivity(new Intent(getActivity(), Settings_ProfileList.class));
                return false;
            });
        }
    }

    private void setClick(String key, Class<?> cls) {
        Preference p = findPreference(key);
        if (p != null) p.setOnPreferenceClickListener(pref -> {
            startActivity(new Intent(getActivity(), cls));
            return false;
        });
    }

    private void initSummary(Preference p) {
        if (p instanceof PreferenceGroup pGrp) {
            for (int i = 0; i < pGrp.getPreferenceCount(); i++) {
                initSummary(pGrp.getPreference(i));
            }
        } else {
            try { updatePrefSummary(p); }
            catch (Exception e) { Log.i(TAG, "Settings:" + e); }
        }
    }

    private void updatePrefSummary(Preference p) {
        if (p == null) return;

        if (p instanceof EditTextPreference) {
            p.setSummaryProvider(pref -> {
                String text = ((EditTextPreference) pref).getText();
                return (text == null || text.trim().isEmpty()) ? getString(R.string.toast_input_empty) : text;
            });
        }

        if (p instanceof ListPreference listPref) {
            if (p.getSummaryProvider() == null) p.setSummary(listPref.getEntry());
            if (sp_ad_block != null) sp_ad_block.setSummary(AdBlock.getHostsDate(requireContext()));
        }

        ListPreference theme = findPreference("sp_theme");
        if (theme != null) theme.setEnabled(sp.getBoolean("useDynamicColor", false));
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, String key) {
        if (key == null) return;
        updatePrefSummary(findPreference(key));

        if (key.equals("sp_ad_block") || key.equals("ab_hosts")) {
            AdBlock.downloadHosts(getActivity());
        }
        if (key.equals("sp_theme") || key.equals("useDynamicColor")) {
            sp.edit().putInt("restart_changed", 1).apply();
            updatePrefSummary(findPreference(key));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Objects.requireNonNull(getPreferenceScreen().getSharedPreferences()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Objects.requireNonNull(getPreferenceScreen().getSharedPreferences()).unregisterOnSharedPreferenceChangeListener(this);
    }
}