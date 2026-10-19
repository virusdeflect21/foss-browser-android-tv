package de.baumann.browser.preferences;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceViewHolder;
import com.google.android.material.checkbox.MaterialCheckBox;
import de.baumann.browser.R;

public class ListSwitchPreference extends ListPreference {

    private String switchKey;
    private boolean defaultValue;
    private final SharedPreferences sp;

    public ListSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.preference_checkbox_widget);
        sp = PreferenceManager.getDefaultSharedPreferences(context);

        if (attrs != null) {
            try (TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ListSwitchPreference, 0, 0)) {
                switchKey = a.getString(R.styleable.ListSwitchPreference_listSwitchKey);
                defaultValue = a.getBoolean(R.styleable.ListSwitchPreference_listSwitchKeyDefaultValue, false);
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        MaterialCheckBox box = (MaterialCheckBox) holder.findViewById(R.id.custom_checkbox_widget);

        if (box != null && switchKey != null) {
            box.setOnCheckedChangeListener(null);
            box.setChecked(sp.getBoolean(switchKey, defaultValue));
            box.setEnabled(true);
            box.setOnCheckedChangeListener((b, checked) -> sp.edit().putBoolean(switchKey, checked).apply());
        }
    }
}