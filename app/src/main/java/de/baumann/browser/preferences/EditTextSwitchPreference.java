package de.baumann.browser.preferences;
import de.baumann.browser.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceViewHolder;
import com.google.android.material.checkbox.MaterialCheckBox;

public class EditTextSwitchPreference extends EditTextPreference {
    private String switchKey;
    private boolean defaultValue;

    private static final int[] COLORS = {
            R.color.red, R.color.pink, R.color.purple, R.color.blue, R.color.teal,
            R.color.green, R.color.lime, R.color.yellow, R.color.orange, R.color.brown, R.color.grey
    };

    public EditTextSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.preference_checkbox_widget);
        if (attrs != null) {
            try (TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.EditTextSwitchPreference, 0, 0)) {
                switchKey = a.getString(R.styleable.EditTextSwitchPreference_editTextSwitchKey);
                defaultValue = a.getBoolean(R.styleable.EditTextSwitchPreference_editTextSwitchKeyDefaultValue, false);
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        MaterialCheckBox box = (MaterialCheckBox) holder.findViewById(R.id.custom_checkbox_widget);
        Context ctx = getContext();

        if (box != null && switchKey != null) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
            boolean isChecked = sp.getBoolean(switchKey, defaultValue);

            box.setOnCheckedChangeListener(null);
            box.setChecked(isChecked);
            box.setFocusable(true);
            box.setClickable(true);
            holder.itemView.setAlpha(isChecked ? 1.0f : 0.5f);
            box.setEnabled(true);

            box.setOnCheckedChangeListener((b, checked) -> {
                sp.edit().putBoolean(switchKey, checked).apply();
                holder.itemView.setAlpha(checked ? 1.0f : 0.5f);
            });
        }

        View.OnClickListener openDialogListener = v -> super.onClick();
        View titleView = holder.findViewById(android.R.id.title);
        if (titleView != null) {
            titleView.setOnClickListener(openDialogListener);
        }

        View summaryView = holder.findViewById(android.R.id.summary);
        if (summaryView != null) {
            summaryView.setOnClickListener(openDialogListener);
        }
        holder.itemView.setOnClickListener(null);
        holder.itemView.setClickable(false);

        if (switchKey != null && switchKey.startsWith("filter_")) {
            try {
                int num = Integer.parseInt(switchKey.substring(7));
                if (num >= 1 && num <= 11) {
                    holder.itemView.setBackgroundColor(ContextCompat.getColor(ctx, COLORS[num - 1]));
                } else if (num == 12) {
                    TypedValue tv = new TypedValue();
                    ctx.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceVariant, tv, true);
                    holder.itemView.setBackgroundColor(tv.data);
                }
            } catch (NumberFormatException ignored) {
            }
        } else {
            holder.itemView.setBackgroundColor(0);
        }
    }
    @Override
    public void onClick() {
        // Bleibt leer, um den automatischen Zeilen-Klick von AndroidX zu blockieren
    }
}