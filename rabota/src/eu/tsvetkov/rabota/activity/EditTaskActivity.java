package eu.tsvetkov.rabota.activity;

import static eu.tsvetkov.rabota.Rabota.HOURLY_RATE;
import static eu.tsvetkov.rabota.provider.TaskContract.Tasks.END_POS;
import static eu.tsvetkov.rabota.provider.TaskContract.Tasks.START_POS;
import static eu.tsvetkov.rabota.provider.TaskContract.Tasks.STATUS_POS;
import static eu.tsvetkov.rabota.provider.TaskContract.Tasks.TITLE_POS;

import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import eu.tsvetkov.rabota.R;
import eu.tsvetkov.rabota.intent.RabotaAction;
import eu.tsvetkov.rabota.model.Task;
import eu.tsvetkov.rabota.provider.TaskContract;
import eu.tsvetkov.rabota.util.Calc;
import eu.tsvetkov.rabota.util.Log;
import eu.tsvetkov.rabota.view.DateTimePicker;

public class EditTaskActivity extends Activity {

	private static final String TAG = EditTaskActivity.class.getSimpleName();

	public static final String KEY_START_DATE = "START_DATE";


	private static final int DIALOG_FROM_ID = 0;
	private static final int DIALOG_TILL_ID = 1;

	private Cursor mCursor;
	private boolean isEdit = false;
	private long defaultStartMillis;
	private long defaultEndMillis;

	private EditText mTaskTitle;
	private RadioButton mRadioStartsImmediately;
	private RadioButton mRadioStartsAt;
	private Button mButtonStart;
	private DateTimePicker mDateTimePickerStart;
	private RadioButton mRadioEndsManually;
	private RadioButton mRadioEndsAt;
	private Button mButtonEnd;
	private DateTimePicker mDateTimePickerEnd;
	private CheckBox mCheckboxFinished;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		String action = intent.getAction();
		if (RabotaAction.EDIT_TASK.equals(action)) {
			isEdit = true;
		}
		else if (Intent.ACTION_INSERT.equals(action)) {
			defaultStartMillis = intent.getLongExtra(KEY_START_DATE, Calc.nextHourMillis());
			defaultEndMillis = Calc.millis(defaultStartMillis, Calendar.HOUR_OF_DAY, 2);
		}
		else {
			if (Log.ERROR) Log.e(TAG, "Unknown action, exiting");
			finish();
			return;
		}

		// Initialize view and controls.
		setContentView(R.layout.edit_task);
		mTaskTitle = (EditText) findViewById(R.id.text_task_title);
		mRadioStartsImmediately = (RadioButton) findViewById(R.id.radio_immediately);
		mRadioStartsImmediately.setChecked(true);
		mRadioStartsAt = (RadioButton) findViewById(R.id.radio_starts_at);

		mButtonStart = (Button) findViewById(R.id.button_from_time);
		mButtonStart.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				((RadioButton) findViewById(R.id.radio_starts_at)).setChecked(true);
				showDialog(DIALOG_FROM_ID);
			}
		});
		mDateTimePickerStart = new DateTimePicker(this, null) {
			@Override
			protected void onTimeChanged(long millis) {
				mButtonStart.setText(getTimeString());
				mDateTimePickerEnd.setTime(Calc.add(millis, Calendar.HOUR_OF_DAY, 2));
			}
		};

		mRadioEndsManually = (RadioButton) findViewById(R.id.radio_manually);
		mRadioEndsManually.setChecked(true);
		mRadioEndsAt = (RadioButton) findViewById(R.id.radio_ends_at);

		mButtonEnd = (Button) findViewById(R.id.button_till_time);
		mButtonEnd.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				((RadioButton) findViewById(R.id.radio_ends_at)).setChecked(true);
				showDialog(DIALOG_TILL_ID);
			}
		});
		mDateTimePickerEnd = new DateTimePicker(this, null) {
			@Override
			protected void onTimeChanged(long millis) {
				mButtonEnd.setText(getTimeString());
			}
		};

		mCheckboxFinished = (CheckBox) findViewById(R.id.checkbox_finished);

		Button buttonEditTask = (Button) findViewById(R.id.button_edit_task);
		buttonEditTask.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				update(mTaskTitle.getText().toString(), mRadioStartsImmediately.isChecked(), mDateTimePickerStart.getTime(), mRadioEndsManually.isChecked(),
						mDateTimePickerEnd.getTime(), mCheckboxFinished.isChecked(), HOURLY_RATE);
				finish();
			}
		});
		buttonEditTask.setVisibility(isEdit ? View.VISIBLE : View.GONE);

		Button buttonAddTask = (Button) findViewById(R.id.button_add_task);
		buttonAddTask.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				insert(mTaskTitle.getText().toString(), mRadioStartsImmediately.isChecked(), mDateTimePickerStart.getTime(), mRadioEndsManually.isChecked(),
						mDateTimePickerEnd.getTime(), mCheckboxFinished.isChecked(), HOURLY_RATE);
				finish();
			}
		});
		buttonAddTask.setVisibility(isEdit ? View.GONE : View.VISIBLE);

		Button buttonCancel = (Button) findViewById(R.id.button_cancel);
		buttonCancel.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				cancelEdit();
				finish();
			}
		});
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog dialog;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		switch (id) {
		case DIALOG_FROM_ID:
			builder.setMessage("Set from time").setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					mButtonStart.setText(mDateTimePickerStart.getTimeString());
				}
			});
			dialog = builder.create();
			dialog.setView(mDateTimePickerStart);
			break;
		case DIALOG_TILL_ID:
			builder.setMessage("Set till time").setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					mButtonEnd.setText(mDateTimePickerEnd.getTimeString());
				}
			});
			dialog = builder.create();
			dialog.setView(mDateTimePickerEnd);
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	@Override
	protected void onResume() {
		super.onResume();
		Uri uri = getIntent().getData();

		if (isEdit) {
			mCursor = managedQuery(uri, TaskContract.Tasks.LIST_TASKS, null, null, null);
			mCursor.moveToFirst();
			if (!mCursor.isFirst())
			{
				if (Log.ERROR) Log.e(TAG, "Failed to locate row for editing at " + uri);
				finish();
				return;
			}

			setTitle(getResources().getString(R.string.edit_task));

			mTaskTitle.setTextKeepState(mCursor.getString(TITLE_POS));

			long startMillis = mCursor.getLong(START_POS);
			long endMillis = mCursor.getLong(END_POS);
			mRadioStartsAt.setChecked(startMillis > 0);
			mRadioEndsAt.setChecked(endMillis > 0);

			mDateTimePickerStart.setTime(startMillis > 0 ? startMillis : defaultStartMillis);
			mDateTimePickerEnd.setTime(endMillis > 0 ? endMillis : defaultEndMillis);

			mCheckboxFinished.setChecked(mCursor.getInt(STATUS_POS) == Task.Status.FINISHED.getValue());
		}
		else {
			setTitle(getResources().getString(R.string.add_new_task));

			mTaskTitle.requestFocus();
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
			mDateTimePickerStart.setTime(defaultStartMillis);
			mDateTimePickerEnd.setTime(defaultEndMillis);
		}
	}

	private final void cancelEdit() {
		setResult(RESULT_CANCELED);
		finish();
	}

	private final void delete() {
		if (mCursor != null) {
			mCursor.close();
			mCursor = null;
		}
	}

	private final void insert(String title, boolean startsImmediately, Date startDate, boolean endsManually, Date endDate, boolean finished, double hourlyRate) {

		// Collect details of the new task.
		ContentValues values = new ContentValues();
		values.put(TaskContract.Tasks.TITLE, title);
		if (startsImmediately) {
			values.put(TaskContract.Tasks.STATUS, Task.Status.RUNNING.toString());
			startDate = new Date();
		}
		else if (finished) {
			values.put(TaskContract.Tasks.STATUS, Task.Status.FINISHED.toString());
			if (endsManually) {
				endDate = new Date();
			}
		}
		else {
			values.put(TaskContract.Tasks.STATUS, Task.Status.SCHEDULED.toString());
		}
		values.put(TaskContract.Tasks.START, String.valueOf(startDate.getTime()));
		if (!endsManually || (finished && endsManually)) {
			values.put(TaskContract.Tasks.END, String.valueOf(endDate.getTime()));
		}
		values.put(TaskContract.Tasks.HOURLY_RATE, hourlyRate);

		// Insert the task.
		Uri uri = getContentResolver().insert(TaskContract.Tasks.CONTENT_URI, values);

		if (uri == null) {
			Log.e(TAG, "Failed to insert new row into " + uri);
			finish();
			return;
		}

		// If the task starts immediately or is alredy finished - a task part needs to be inserted.
		if (startsImmediately || finished) {
			// Collect values for the task part.
			String taskId = uri.getLastPathSegment();
			values = new ContentValues();
			values.put(TaskContract.TaskParts.TASK_ID, taskId);
			values.put(TaskContract.TaskParts.COMMENT, "");
			values.put(TaskContract.TaskParts.START, String.valueOf(startDate.getTime()));
			if (finished) {
				values.put(TaskContract.TaskParts.END, String.valueOf(endDate.getTime()));
			}
			getContentResolver().insert(Uri.withAppendedPath(TaskContract.Tasks.CONTENT_TASK_PARTS_URI, taskId), values);
		}

		setResult(RESULT_OK, new Intent().setAction(uri.toString()));
	}

	private final void update(String title, boolean startsImmediately, Date startDate, boolean endsManually, Date endDate, boolean finished, double hourlyRate) {
		ContentValues values = new ContentValues();
		values.put(TaskContract.Tasks.TITLE, title);

		if (startsImmediately) {
			values.put(TaskContract.Tasks.STATUS, Task.Status.RUNNING.toString());
			startDate = new Date();
		}
		else if (endsManually) {
			values.put(TaskContract.Tasks.STATUS, Task.Status.FINISHED.toString());
			if (endsManually) {
				endDate = new Date();
			}
		}
		else if (finished) {
			values.put(TaskContract.Tasks.STATUS, Task.Status.FINISHED.toString());
		}
		else {
			values.put(TaskContract.Tasks.STATUS, Task.Status.SCHEDULED.toString());
		}
		values.put(TaskContract.Tasks.START, String.valueOf(startDate.getTime()));
		if (!endsManually || (finished && endsManually)) {
			values.put(TaskContract.Tasks.END, String.valueOf(endDate.getTime()));
		}
		values.put(TaskContract.Tasks.HOURLY_RATE, hourlyRate);

		getContentResolver().update(getIntent().getData(), values, null, null);
	}
}
