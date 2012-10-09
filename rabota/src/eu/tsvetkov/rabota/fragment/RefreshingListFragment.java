package eu.tsvetkov.rabota.fragment;

import java.util.Timer;
import java.util.TimerTask;

import android.support.v4.app.ListFragment;
import eu.tsvetkov.rabota.util.Format;
import eu.tsvetkov.rabota.util.Log;

public class RefreshingListFragment extends ListFragment {

	private static final String TAG = RefreshingListFragment.class.getSimpleName();

	private Timer refreshTimer;
	private long timeout = -1;

	@Override
	public void onPause() {
		super.onPause();
		// stopTimer();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (refreshTimer == null) {
			startTimer();
		}
	}

	@Override
	public void onStop() {
		stopTimer();
		super.onStop();
	}

	public void setTimeout(long timeoutMillis) {
		this.timeout = timeoutMillis;
		if (refreshTimer != null) {
			stopTimer();
			startTimer();
		}
	}

	protected void onRefresh() {
	}

	private void startTimer() {

		if (Log.DEBUG)
			Log.d(TAG, String.format("Starting %s refresh timer in %s", Format.duration(timeout), RefreshingListFragment.this));

		refreshTimer = new Timer();
		refreshTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				getActivity().runOnUiThread(new Runnable() {
					public void run() {
						if (Log.VERBOSE) Log.v(TAG, "Refreshing the list view in " + RefreshingListFragment.this);
						getListView().invalidateViews();
						onRefresh();
					}
				});
			}
		}, 1000, timeout);
	}

	private void stopTimer() {

		Log.d(TAG, String.format("Stopping refresh timer in %s", RefreshingListFragment.this));

		refreshTimer.cancel();
		refreshTimer.purge();
		refreshTimer = null;
	}
}
