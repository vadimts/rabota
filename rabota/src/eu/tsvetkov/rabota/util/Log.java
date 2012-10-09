package eu.tsvetkov.rabota.util;

public class Log {

	public static final boolean DEBUG = false;
	public static final boolean ERROR = false;
	public static final boolean INFO = false;
	public static final boolean VERBOSE = false;
	public static final boolean WARN = false;

	public static final void d(String tag, String message) {
		if (DEBUG || android.util.Log.isLoggable(tag, android.util.Log.DEBUG)) {
			android.util.Log.d(tag, message);
		}
	}

	public static final void e(String tag, String message) {
		if (ERROR || android.util.Log.isLoggable(tag, android.util.Log.ERROR)) {
			android.util.Log.e(tag, message);
		}
	}

	public static final void e(String tag, String message, Throwable e) {
		if (ERROR || android.util.Log.isLoggable(tag, android.util.Log.ERROR)) {
			android.util.Log.e(tag, message, e);
		}
	}

	public static final void i(String tag, String message) {
		if (INFO || android.util.Log.isLoggable(tag, android.util.Log.INFO)) {
			android.util.Log.i(tag, message);
		}
	}

	public static final void v(String tag, String message) {
		if (VERBOSE || android.util.Log.isLoggable(tag, android.util.Log.VERBOSE)) {
			android.util.Log.v(tag, message);
		}
	}

	public static final void w(String tag, String message) {
		if (WARN || android.util.Log.isLoggable(tag, android.util.Log.WARN)) {
			android.util.Log.w(tag, message);
		}
	}
}
