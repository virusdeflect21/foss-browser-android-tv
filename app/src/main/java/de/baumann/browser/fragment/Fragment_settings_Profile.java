package de.baumann.browser.fragment;

import android.os.Bundle;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import java.util.Objects;
import de.baumann.browser.R;
import de.baumann.browser.browser.BannerBlock;
import de.baumann.browser.preferences.BasePreferenceFragment;

public class Fragment_settings_Profile extends BasePreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_profile_standard, rootKey);
        PreferenceManager.setDefaultValues(requireContext(), R.xml.preference_profile_standard, false);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (key != null && key.equals("profileStandard_adBlock") && sp.getBoolean("profileStandard_adBlock", false)) {
            BannerBlock.downloadBanners(getActivity());
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