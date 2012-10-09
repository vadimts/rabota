package eu.tsvetkov.rabota.activity;

import static eu.tsvetkov.rabota.Rabota.str;

import java.util.ArrayList;
import java.util.List;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import eu.tsvetkov.rabota.R;
import eu.tsvetkov.rabota.Rabota;
import eu.tsvetkov.rabota.model.Task;

public class PreferencesFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

	private static final List<Preference> prefsWithoutSummary = new ArrayList<Preference>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);

		PreferenceScreen screen = getPreferenceScreen();
		checkSummary(screen);
		updateSummary(screen);
		screen.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		updateSummary(getPreferenceScreen());
	}

	/**
	 * Recursively iterates through all preferences and lists the ones without summary in a member array.
	 * 
	 * @param p
	 */
	private void checkSummary(Preference p) {
		if (TextUtils.isEmpty(p.getSummary())) {
			prefsWithoutSummary.add(p);
		}
		if (p instanceof PreferenceGroup) {
			PreferenceGroup group = (PreferenceGroup) p;
			for (int i = 0; i < group.getPreferenceCount(); i++) {
				checkSummary(group.getPreference(i));
			}
		}
	}

	/**
	 * Recursively iterates through preferences and sets preference values as summary on preferences with undefined summary.
	 * 
	 * @param p
	 */
	private void updateSummary(Preference p) {

		// If this is a preference group - update summaries of its nested preferences.
		if (p instanceof PreferenceGroup) {
			PreferenceGroup pCat = (PreferenceGroup) p;
			for (int i = 0; i < pCat.getPreferenceCount(); i++) {
				updateSummary(pCat.getPreference(i));
			}
		}
		else {
			// If preference was previously listed as one having no summary - set its value as summary.
			if (prefsWithoutSummary.contains(p)) {

				// Get the preference value.
				String value = null;
				if (p instanceof ListPreference) {
					value = (String) ((ListPreference) p).getEntry();
				}
				if (p instanceof EditTextPreference) {
					value = ((EditTextPreference) p).getText();
				}

				// Set summary of some preferences individually.
				if (str(R.string.pref_payment_rate).equals(p.getKey())) {
					value += " " + Rabota.pref(R.string.pref_payment_currency) + " "
							+ Task.ChargeUnit.fromInt(Rabota.prefInt(R.string.pref_payment_charge_unit));
				}
				if (str(R.string.pref_payment_tax).equals(p.getKey())) {
					value += " %";
				}

				// Set summary.
				p.setSummary(str(R.string.preference_current_value, value));
			}
		}
	}
}
