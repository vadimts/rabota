package eu.tsvetkov.rabota.activity;

import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import eu.tsvetkov.rabota.R;
import eu.tsvetkov.rabota.provider.TaskContract;
import eu.tsvetkov.rabota.util.Calc;
import eu.tsvetkov.rabota.view.DateTimeTextView;
import eu.tsvetkov.rabota.view.DurationTextView;

public class ViewTaskActivity extends FragmentActivity {

	private static final String TAG = ViewTaskActivity.class.getSimpleName();

	public static final int TASK_LOADER = 1;

	private Cursor mCursor;

	private CursorLoader mCursorLoader;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		CursorLoader loader = new CursorLoader(this, intent.getData(), TaskContract.Tasks.LIST_TASKS, null, null, null);
		mCursor = loader.loadInBackground();

		// Initialize view and controls.
		setContentView(R.layout.view_task);

		Button buttonOk = (Button) findViewById(R.id.button_ok);
		buttonOk.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				finish();
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mCursor.moveToFirst()) {
			long taskId = mCursor.getLong(TaskContract.Tasks.ID_POS);

			// Show task title.
			setTitle(mCursor.getString(TaskContract.Tasks.TITLE_POS));

			// Show task ID.
			((TextView) findViewById(R.id.label_task_id)).setText(String.valueOf(taskId));

			// Show task start time.
			long startMillis = mCursor.getLong(TaskContract.Tasks.START_POS);
			if (Calc.now() > startMillis) {
				((TextView) findViewById(R.id.label_task_starts)).setText(R.string.started);
			}
			((DateTimeTextView) findViewById(R.id.text_task_start)).setText(startMillis);

			// Show task end time.
			long endMillis = mCursor.getLong(TaskContract.Tasks.END_POS);
			if (Calc.now() > endMillis && endMillis > 0) {
				((TextView) findViewById(R.id.label_task_ends)).setText(R.string.ended);
			}
			DateTimeTextView textEnd = (DateTimeTextView) findViewById(R.id.text_task_end);
			if (endMillis > 0) {
				textEnd.setText(endMillis);
			}
			else {
				textEnd.setText(getResources().getString(R.string.manually));
			}

			// Show task duration.
			((DurationTextView) findViewById(R.id.text_task_duration)).setText(Calc.taskDuration(this, mCursor));

		}
	}
}
