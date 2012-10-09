package eu.tsvetkov.rabota.db;

import static eu.tsvetkov.rabota.model.Task.Status.FINISHED;
import static eu.tsvetkov.rabota.model.Task.Status.RUNNING;
import static eu.tsvetkov.rabota.model.Task.Status.SCHEDULED;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import eu.tsvetkov.rabota.R;
import eu.tsvetkov.rabota.Rabota;
import eu.tsvetkov.rabota.model.Task;
import eu.tsvetkov.rabota.provider.TaskContract;
import eu.tsvetkov.rabota.util.Calc;
import eu.tsvetkov.rabota.util.Log;

public class TaskHelper extends SQLiteOpenHelper {

	private static final String TAG = TaskHelper.class.getSimpleName();

	private static final String DB_NAME = "rabota.db";
	private static final int DB_VERSION = 1;

	public static SQLiteDatabase open(Context context) {
		return new TaskHelper(context).getWritableDatabase();
	}

	public TaskHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(TaskContract.Tasks.TABLE_CREATE);
		db.execSQL(TaskContract.TaskParts.TABLE_CREATE);

		// createLoremIpsum(db);
		long now = Calc.now();
		String rate = String.valueOf(Rabota.prefDouble(R.string.pref_payment_rate));
		db.execSQL(String.format(
				"INSERT INTO tasks VALUES(1, 'First look at Rabota. This test task will end in 15 minutes and can be safely deleted', %d, %d, %d, %s);",
				now, Calc.millis(now, Calendar.MINUTE, 15), Task.Status.RUNNING.getValue(), rate));
		db.execSQL(String.format("INSERT INTO task_parts VALUES(1,1,null,%d,null);", now));
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (Log.WARN) Log.w(TAG, "Upgrading database " + DB_NAME + " from version " + oldVersion + " to " + newVersion + ". Existing contents will be lost.");
		db.execSQL("DROP TABLE IF EXISTS " + TaskContract.Tasks.TABLE);
		db.execSQL("DROP TABLE IF EXISTS " + TaskContract.TaskParts.TABLE);
		onCreate(db);
	}

	private void createLoremIpsum(SQLiteDatabase db) {
		String lorem = "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. " +
				"Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
				"Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. " +
				"Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

		Calendar now = Calc.cal();
		Calendar start = Calc.add(Calendar.MONTH, -1);
		Calendar lastWeek = Calc.add(Calendar.WEEK_OF_MONTH, -1);
		Calendar yesterday = Calc.add(Calendar.DAY_OF_MONTH, -1);
		Calendar inTwoMonths = Calc.add(Calendar.MONTH, 2);

		int taskId = 1;
		int taskPartId = 1;
		Double rate = Rabota.prefDouble(R.string.pref_payment_rate);
		while (start.before(inTwoMonths)) {

			int gap = (int) (TimeUnit.HOURS.toMillis(42) * Math.random() + 8); // gap between tasks 8..50 hours
			start.setTimeInMillis(Calc.millis(start) + gap);

			if (start.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || start.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
				continue;
			}

			int dur = (int) (TimeUnit.HOURS.toMillis(48) * Math.random() + 1); // task duration 1..49 hours
			Calendar end = Calc.cal(Calc.millis(start) + dur);
			Task.Status status = (end.before(now) ? FINISHED : (start.after(now) ? SCHEDULED : RUNNING));
			long startMillis = Calc.millis(start);
			long endMillis = Calc.millis(end);
			int stat = status.getValue();

			String title = lorem.substring((int) (Math.random() * 30), (int) (Math.random() * 100 + 40)).trim();
			title = title.substring(0, 1).toUpperCase() + title.substring(1);
			String comment = lorem.substring((int) (Math.random() * 30), (int) (Math.random() * 100 + 40)).trim();
			comment = comment.substring(0, 1).toUpperCase() + comment.substring(1);

			switch (status) {
			case FINISHED:
				db.execSQL(String.format("INSERT INTO tasks VALUES(%d, '%s', %s, %d, %d, %d);", taskId, title, rate, startMillis, endMillis, stat));
				db.execSQL(String.format("INSERT INTO task_parts VALUES(%d,%d,'%s',%d,%d);", taskPartId, taskId, comment, startMillis, endMillis));
				break;
			case SCHEDULED:
				db.execSQL(String.format("INSERT INTO tasks VALUES(%d, '%s', %s, %d, %d, %d);", taskId, title, rate, startMillis, endMillis, stat));
				break;
			case RUNNING:
				db.execSQL(String.format("INSERT INTO tasks VALUES(%d, '%s', %s, %d, null, %d);", taskId, title, rate, startMillis, stat));
				db.execSQL(String.format("INSERT INTO task_parts VALUES(%d,%d,'%s',%d,null);", taskPartId, taskId, comment, startMillis));
				break;
			}

			start.setTimeInMillis(Calc.millis(start) + gap);
			taskId++;
			taskPartId++;
		}
	}
}