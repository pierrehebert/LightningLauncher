package net.pierrox.lightning_launcher.activities;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher_extreme.R;
import ninja.sesame.lib.bridge.v1.SesameFrontend;

public class Extensions extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Utils.setTheme(this, Utils.APP_THEME);
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences_extensions);
		SwitchPreference sesame = (SwitchPreference) findPreference("sesame");
		sesame.setChecked(SesameFrontend.getIntegrationState(this));
		sesame.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				SesameFrontend.setIntegrationState(Extensions.this, (Boolean) newValue);
				return true;
			}
		});
	}
}
