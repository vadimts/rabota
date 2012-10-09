package eu.tsvetkov.rabota.activity;

import static eu.tsvetkov.rabota.Rabota.str;
import static eu.tsvetkov.rabota.provider.TaskContract.Tasks.ID_POS;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import eu.tsvetkov.rabota.R;
import eu.tsvetkov.rabota.Rabota;
import eu.tsvetkov.rabota.fragment.RefreshingListFragment;
import eu.tsvetkov.rabota.fragment.TaskListFragment;
import eu.tsvetkov.rabota.gae.RabotaGaeConnector;
import eu.tsvetkov.rabota.intent.RabotaAction;
import eu.tsvetkov.rabota.intent.RabotaGaeConnectionService;
import eu.tsvetkov.rabota.model.Invoice;
import eu.tsvetkov.rabota.provider.TaskContract;
import eu.tsvetkov.rabota.util.Calc;
import eu.tsvetkov.rabota.util.Content;
import eu.tsvetkov.rabota.util.InvoiceBuilder;
import eu.tsvetkov.rabota.util.Log;
import eu.tsvetkov.rabota.util.UI;
import eu.tsvetkov.rabota.view.InfiniteFragmentPager;

/**
 * DEEDS:
 * <ul>
 * <li>Preferences
 * <li>IntentService + BroadcastReceiver for POSTing request to rabota-gae (Google AppEngine component), which then emails the generated invoice.
 * <li>Infinite scrolling in ViewPager with 3 fragments and current item being always the middle fragment.
 * <li>ViewPager integration.
 * <li>Custom layout for the canned {@link ListFragment} in {@link TaskListFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, Bundle)}.
 * <li>ContentProvider of tasks and task parts.
 * </ul>
 * 
 * TODO's:
 * <ul>
 * <li>Check where this warning is coming from: "W/CursorWrapperInner( 1352): Cursor finalized without prior close()". (see ContentResolver.CursorWrapperInner)
 * <li>Refresh task list while in background.
 * <li>Think of better alternative for coupling InfiniteFragmentPager + abstract Adapter and method.
 * {@link InfiniteFragmentPager#init(eu.tsvetkov.rabota.view.InfiniteFragmentPager.Adapter)}.
 * <li>Scroll the ViewPager and reload TaskListFragments in it without flickering (check getLoaderManager().restartLoader() for TaskListFragment).
 * <li>Look at ORMLite, possibly integrate it with existing ContentProvider.
 * <li>Custom XML attributes on custom view (DateTimePicker).
 * <li>Minute steps for the TimePicker, e.g. 15 min granularity.
 * <li>TaskProvider is an overkill (for learning purposes) since Rabota doesn't provide data to other apps.
 * <li>URI Matcher in TaskProvider: find out if REST-like URIs, e.g. "authority/tasks/#/items", are possible.
 * <li>Extend editing of existing tasks: reopen, change start and end dates, individual per-task rate.
 * <li>Edit comments on task parts.
 * <li>Scale task part icon proportionally with android:layout_alignBottom (height AND width) in task_parts_list_item.xml.
 * </ul>
 * 
 * GOTCHA's
 * <ul>
 * <li>{@link ContentProvider#query(android.net.Uri, String[], String, String[], String)} - use "cast(? as number)" for integer selectionArgs!
 * <li>Log level defined in local.prop doesn't seem to work as desribed in <a
 * href="http://developer.android.com/reference/android/util/Log.html#isLoggable%28java.lang.String,%20int%29">http://developer.android.com</a>
 * </ul>
 * 
 * TASK FLOWs:
 * 
 * <pre>
 * <ul>
 * <li>
 * Task (starts immediately, ends manually):    14:11--------------15:00----------------16:00----------------17:00
 * 1 task part:                                 14:11--------------15:00----------------16:00----------------17:00
 * At 14:11, on "Add task" click:         task status=RUNNING, task start=14:11, 1st task part's start=14:11.
 * At 17:00, on "Finish task" click:      task status=FINISHED, task end=17:00, 1st task part's end=17:00.
 * </li><li>
 * Task (starts 14:00, ends manually):        14:00----------------15:00----------------16:00----------------17:00
 * 2 task parts:                              14:00---------------------------15:30     16:00----------------17:00
 * At 13:00, after task scheduling:       task status=SCHEDULED, no task parts.
 * At 14:00, automatically:               task status=RUNNING, task start=14:00, 1st task part's start=14:00.
 * At 15:30, on task click (pause):       task status=PAUSED, 1st task part's end=15:30.
 * At 16:00, on task click (resume):      task status=RUNNING, 2nd task part's start=16:30.
 * At 17:00, on "Finish task" click:      task status=FINISHED, task end=17:00, 2nd task part's end=17:00.
 * </li><li>
 * Task (starts 14:00, ends 17:00):           14:00----------------15:00----------------16:00----------------17:00
 * 2 task parts:                              14:00---------------------------15:30     16:00----------------17:00
 * At 17:00 task status -> FINISHED
 * </li><li>
 * Task (starts 14:00, ends 17:00):           14:00----------------15:00----------------16:00----------------17:00
 * 2 task parts:                                         14:30----------------15:30     16:00------------------- ...
 * At 17:00 task status -> FINISHED, 2nd task part's end -> 17:00.
 * </li>
 * </ul>
 * </pre>
 * 
 * @author vadim
 */
public class StartActivity extends FragmentActivity implements OnSharedPreferenceChangeListener {

	private static final String TAG = StartActivity.class.getSimpleName();

	private static Calendar[] months = new Calendar[] { Calc.cal(), Calc.cal(), Calc.cal(), Calc.cal() };
	static final List<Fragment> fragments = new ArrayList<Fragment>();

	static {
		Calendar now = Calendar.getInstance();
		for (int i = 0; i < 4; i++) {
			months[i].setTimeInMillis(0);
			months[i].set(Calendar.HOUR_OF_DAY, 0);
			months[i].set(Calendar.YEAR, now.get(Calendar.YEAR));
			months[i].set(Calendar.MONTH, now.get(Calendar.MONTH));
			months[i].add(Calendar.MONTH, i - 1);
		}
	}

	private InfiniteFragmentPager mPager;
	private InvoiceSubmissionResultReceiver receiver;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.options_menu, menu);
		menu.findItem(R.id.preferences_option_item).setIntent(new Intent(this, PreferencesActivity.class));
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.preferences_option_item:
			startActivity(item.getIntent());
			break;
		}
		return true;
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		// If the update frequency has been changed - restart refreshing fragments with the new timeout.
		if (key == str(R.string.pref_general_time_step)) {
			long timeStep = Rabota.prefLong(R.string.pref_general_time_step);
			for (Fragment fragment : fragments) {
				((RefreshingListFragment) fragment).setTimeout(timeStep);
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// If a new task has been successfully added - refresh all pages.
		if (requestCode == RabotaAction.EDIT_TASK_REQUEST && resultCode == RESULT_OK) {
			mPager.onDataSetChange();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.start);

		fragments.add(new TaskListFragment(months[0], months[1]));
		fragments.add(new TaskListFragment(months[1], months[2]));
		fragments.add(new TaskListFragment(months[2], months[3]));

		mPager = (InfiniteFragmentPager) findViewById(R.id.pager_start);
		mPager.init(mPager.new Adapter(getSupportFragmentManager(), fragments) {
			@Override
			public void onScrollLeft() {
				super.onScrollLeft();
				if (Log.DEBUG) Log.d(TAG, "Scrolling calendars left");
				for (Calendar month : months) {
					month.add(Calendar.MONTH, -1);
				}
			}

			@Override
			public void onScrollRight() {
				super.onScrollRight();
				if (Log.DEBUG) Log.d(TAG, "Scrolling calendars right");
				for (Calendar month : months) {
					month.add(Calendar.MONTH, 1);
				}
			}
		});

		// Initialize "Add new task" button.
		Button buttonAddTask = (Button) findViewById(R.id.button_add_new_task);
		buttonAddTask.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_INSERT, TaskContract.Tasks.CONTENT_URI);
				Calendar startMonth = ((TaskListFragment) mPager.getCurrentFragment()).getStart();
				intent.putExtra(EditTaskActivity.KEY_START_DATE,
						(Calc.isSameMonth(startMonth, Calc.cal()) ? Calc.nextHourMillis() : Calc.firstWorkDayMillis(startMonth)));
				startActivityForResult(intent, RabotaAction.EDIT_TASK_REQUEST);
			}
		});

		// Initialize "Send report" button.
		Button buttonSendReport = (Button) findViewById(R.id.button_send_invoice);
		buttonSendReport.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				UI.showOkCancelDialog(StartActivity.this, R.string.dialog_send_invoice, new Runnable() {
					public void run() {
						submitInvoice();
					}
				});
			}
		});

		// Register receiver for the invoice intents.
		receiver = new InvoiceSubmissionResultReceiver();
		registerReceiver(receiver, new IntentFilter(InvoiceSubmissionResultReceiver.ACTION_INVOICE_SUBMITTED));
		registerReceiver(receiver, new IntentFilter(InvoiceSubmissionResultReceiver.ACTION_INVOICE_SUBMISSION_ERROR));

		// Register preference listener.
		Rabota.registerPreferenceListener(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(receiver);
	}

	/**
	 * Sends new invoice to Rabota-GAE.
	 */
	private void submitInvoice() {
		// Get the time range from currently shown task page.
		TaskListFragment fragment = (TaskListFragment) mPager.getCurrentFragment();
		Calendar start = fragment.getStart();
		Calendar end = fragment.getEnd();

		// Check if the TO field is not empty.
		if (TextUtils.isEmpty(Rabota.pref(R.string.pref_invoice_email_to))) {
			UI.showOkDialog(StartActivity.this, R.string.error_empty_invoice_email_to);
			return;
		}

		// Pause all running tasks.
		Cursor runningTasks = Content.runningTasks(StartActivity.this, start, end);
		if (runningTasks.moveToFirst()) {
			do {
				Content.pauseTask(getContentResolver(), runningTasks.getLong(ID_POS));
			} while (runningTasks.moveToNext());
		}

		// Submit invoice.
		Invoice invoice = new InvoiceBuilder(getContentResolver(), start, end).newInvoice();
		Intent intent = new Intent(StartActivity.this, RabotaGaeConnectionService.class);
		intent.putExtra(RabotaGaeConnector.PARAM_EMAIL_TO, invoice.getEmailTo());
		intent.putExtra(RabotaGaeConnector.PARAM_EMAIL_BCC, invoice.getEmailBcc());
		intent.putExtra(RabotaGaeConnector.PARAM_EMAIL_SUBJECT, invoice.getEmailSubject());
		intent.putExtra(RabotaGaeConnector.PARAM_EMAIL_BODY, invoice.getEmailBody());
		startService(intent);
	}

	public class InvoiceSubmissionResultReceiver extends BroadcastReceiver {
		public static final String ACTION_INVOICE_SUBMITTED = "eu.tsvetkov.rabota.intent.INVOICE_SUBMITTED";
		public static final String ACTION_INVOICE_SUBMISSION_ERROR = "eu.tsvetkov.rabota.intent.INVOICE_SUBMISSION_ERROR";
		public static final String ERROR_MESSAGE = "error_message";

		@Override
		public void onReceive(Context context, Intent intent) {
			String message = ACTION_INVOICE_SUBMITTED.equals(intent.getAction()) ?
					getResources().getString(R.string.invoice_sent) :
					getResources().getString(R.string.invoice_not_sent) + "\n" + intent.getStringExtra(ERROR_MESSAGE);

			UI.showOkDialog(StartActivity.this, message);
		}
	}

}
