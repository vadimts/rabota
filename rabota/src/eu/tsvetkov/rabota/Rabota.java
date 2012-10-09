package eu.tsvetkov.rabota;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.preference.PreferenceManager;

public class Rabota extends Application {

	public static final String TAG = Rabota.class.getSimpleName();
	public static Resources RES;
	public static long TIME_STEP;
	public static double HOURLY_RATE;
	private static Context context;

	public static Context getContext() {
		return Rabota.context;
	}

	public static String pref(int preferenceId) {
		return Prefs.get(preferenceId);
	}

	public static Double prefDouble(int preferenceId) {
		return Double.valueOf(Prefs.get(preferenceId));
	}

	public static int prefInt(int preferenceId) {
		return Integer.valueOf(Prefs.get(preferenceId));
	}

	public static long prefLong(int preferenceId) {
		return Long.valueOf(Prefs.get(preferenceId));
	}

	public static void registerPreferenceListener(OnSharedPreferenceChangeListener listener) {
		PreferenceManager.getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(listener);
	}

	public static String str(int stringId) {
		return RES.getString(stringId);
	}

	public static String str(int stringId, Object... formatArgs) {
		return RES.getString(stringId, formatArgs);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Rabota.context = getApplicationContext();
		RES = Rabota.getContext().getResources();

		PreferenceManager.setDefaultValues(context, R.xml.preferences, false);

		TIME_STEP = prefLong(R.string.pref_general_time_step);
		HOURLY_RATE = prefDouble(R.string.pref_payment_rate);
	}



	public static final class Prefs implements OnSharedPreferenceChangeListener {

		private static final Prefs INSTANCE = new Prefs();

		public static String get(int preferenceId) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
			return prefs.getString(getContext().getString(preferenceId), "");
		}

		public static void set(int preferenceId, String preferenceValue) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
			Editor prefsEditor = prefs.edit();
			prefsEditor.putString(getContext().getString(preferenceId), preferenceValue);
			prefsEditor.commit();
		}

		private Prefs() {
			Rabota.registerPreferenceListener(this);
		}

		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if (key.equals(str(R.string.pref_payment_rate))) {
				HOURLY_RATE = prefDouble(R.string.pref_payment_rate);
			}
			else if (key.equals(str(R.string.pref_general_time_step))) {
				TIME_STEP = prefLong(R.string.pref_general_time_step);
			}
		}
	}
}
