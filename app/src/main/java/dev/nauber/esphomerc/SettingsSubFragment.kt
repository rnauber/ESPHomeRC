package dev.nauber.esphomerc

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceFragmentCompat


class SettingsSubFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val viewModel: ControlCommViewModel by viewModels({ requireActivity() })
        preferenceManager.preferenceDataStore = viewModel.settingsDataStore
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}