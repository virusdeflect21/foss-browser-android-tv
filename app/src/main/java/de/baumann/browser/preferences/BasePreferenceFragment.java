package de.baumann.browser.preferences;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.util.Objects;
import de.baumann.browser.R;
import de.baumann.browser.unit.HelperUnit;

public class BasePreferenceFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        // Bleibt leer oder lädt das standardmäßige XML-Layout in den Sub-Klassen
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        if (preference instanceof ListPreference) {
            showListPreference((ListPreference) preference);
        } else if (preference instanceof EditTextPreference) {
            showEditTextPreference((EditTextPreference) preference);
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    public void showListPreference(@NonNull ListPreference preference) {
        int selectionIndex = preference.findIndexOfValue(preference.getValue());
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(preference.getTitle())
                .setNegativeButton(R.string.app_cancel, null)
                .setSingleChoiceItems(preference.getEntries(), selectionIndex, (dialog, index) -> {
                    String newValue = preference.getEntryValues()[index].toString();
                    if (preference.callChangeListener(newValue)) {
                        preference.setValue(newValue);
                    }
                    dialog.dismiss();
                })
                .show();
    }

    public void showEditTextPreference(@NonNull EditTextPreference preference) {
        View dialogView = View.inflate(requireContext(), R.layout.dialog_edit_text, null);
        TextInputEditText input = dialogView.findViewById(R.id.textInput);
        input.setText(preference.getText());
        Dialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(preference.getTitle())
                .setView(dialogView)
                .setNegativeButton(R.string.app_cancel, null)
                .setPositiveButton(R.string.app_ok, (d, i) -> {
                    String newValue = Objects.requireNonNull(input.getText()).toString();
                    if (preference.callChangeListener(newValue)) {
                        preference.setText(newValue);
                    }
                    d.dismiss();
                })
                .create();

        dialog.show();
        HelperUnit.setupDialog(requireContext(), dialog);
    }
}