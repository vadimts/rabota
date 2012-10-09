package eu.tsvetkov.rabota.provider;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;
import eu.tsvetkov.rabota.model.Task;

public class TaskContract {

	public static final String AUTHORITY = TaskContract.class.getPackage().getName();

	// NB: when using the 'now' Unix timestamp from SQLite, multiply it by 1000 to compare it with the Java timestamp:
	// private static final String ROUNDED_END = "case when {1}>0 then {0}+(({1}-{0})/{2}+1)*{2} else {1} end";
	// public static final String ROUNDED_DURATION =
	// "case when %2$s>0 then ((%2$s-%1$s)/%3$s+1)*%3$s else ((cast(strftime('%%s','now') as integer)*1000-%1$s)/%3$s+1)*%3$s end";

	private TaskContract() {
	}

	/**
	 * Task parts contract.
	 * 
	 * @author vadim
	 */
	public static final class TaskParts implements BaseColumns {

		// DB attributes
		public static final String TABLE = "task_parts";
		public static final String TASK_ID = "task_id";
		public static final String COMMENT = "comment";
		public static final String START = "start";
		public static final String END = "end";
		public static final String DURATION = "duration";
		public static final String TABLE_CREATE = "CREATE TABLE " + TABLE + " (" +
				_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
				TASK_ID + " INTEGER NOT NULL, " +
				COMMENT + " TEXT, " +
				START + " INTEGER NOT NULL, " +
				END + " INTEGER" +
				");";

		public static final int ID_POS = 0;
		public static final int COMMENT_POS = 1;
		public static final int START_POS = 2;
		public static final int END_POS = 3;

		public static final String TASK_PART_ID = TABLE + "." + _ID;
		public static final String TASK_PART_START = TABLE + "." + START;
		public static final String TASK_PART_END = TABLE + "." + END;
		public static final String TASK_PART_DURATION_IN_RANGE = "((min(case when %2$s>0 then %2$s else 9999999999999 end, cast(? as number), cast(strftime('%%s','now') as number)*1000)-max(%1$s, cast(? as number)))/%3$s)*%3$s";

		public static final String[] LIST_TASK_PARTS = new String[] {
				TASK_PART_ID,
				COMMENT,
				TASK_PART_START,
				TASK_PART_END
				// String.format(TASK_PART_DURATION_IN_RANGE, TASK_PART_START, TASK_PART_END, DEFAULT_TIME_STEP) + " as " + DURATION
				// MessageFormat.format(ROUNDED_END + " as {3}", TASK_PART_START, TASK_PART_END, DEFAULT_TIME_STEP, END),
				// MessageFormat.format(ROUNDED_DURATION + " as {3}", TASK_PART_START, TASK_PART_END, DEFAULT_TIME_STEP, DURATION)
		};
		public static final String LIST_TABLES = String.format("%1$s join %2$s on (%1$s.%3$s = %2$s.%4$s)",
				Tasks.TABLE, TaskParts.TABLE, _ID, TaskParts.TASK_ID);
		public static final String WHERE_TASK_PARTS_IN_RANGE = String.format(
				"(%1$s < cast(?1 as number) and ifnull(%2$s,cast(?2 as number)) > cast(?1 as number)) " +
						"or (%1$s >= cast(?1 as number) and %1$s < cast(?2 as number))",
				TaskParts.TASK_PART_START, TaskParts.TASK_PART_END);
		public static final String WHERE_RUNNING_TASK_PART = TASK_PART_END + " is null";
		public static final String LIST_SORT = TASK_PART_START;

		// REST attributes
		public static final String PATH = TABLE;
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH);
		private static final String CONTENT_SUBTYPE = "vnd.tsvetkov.rabota-task-part";
		public static final String DIR_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_SUBTYPE;
		public static final String CONTENT_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_SUBTYPE;

		private TaskParts() {
		}
	}




	public static final class Tasks implements BaseColumns {

		// DB attributes
		public static final String TABLE = "tasks";
		public static final String TITLE = "title";
		public static final String HOURLY_RATE = "hourly_rate";
		public static final String START = "start";
		public static final String END = "end";
		public static final String DURATION = "duration";
		public static final String STATUS = "status";
		public static final String TABLE_CREATE = "CREATE TABLE " + TABLE + " (" +
				_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
				TITLE + " TEXT NOT NULL," +
				START + " INTEGER NOT NULL," +
				END + " INTEGER," +
				STATUS + " INTEGER NOT NULL DEFAULT " + Task.Status.RUNNING.getValue() + "," +
				HOURLY_RATE + " REAL NOT NULL" +
				");";

		public static final int ID_POS = 0;
		public static final int TITLE_POS = 1;
		public static final int START_POS = 2;
		public static final int END_POS = 3;
		public static final int STATUS_POS = 5;

		public static final String TASK_ID = TABLE + "." + _ID;
		public static final String TASK_START = TABLE + "." + START;
		public static final String TASK_END = TABLE + "." + END;

		public static final String[] LIST_TASKS = new String[] {
				TASK_ID,
				TITLE,
				TASK_START,
				TASK_END,
				HOURLY_RATE,
				STATUS
		};
		public static final String LIST_TABLES = String.format("%1$s left join %2$s on (%1$s.%3$s = %2$s.%4$s)",
				Tasks.TABLE, TaskParts.TABLE, _ID, TaskParts.TASK_ID);

		// Use a time range between two dates (e.g. 1st of October and 1st of November to match the complete October) to select tasks whose start or end times
		// lie within this range.
		public static final String WHERE_TASKS_IN_RANGE = String.format(
				"(%1$s < cast(?1 as number) and max(ifnull(%2$s,0), ifnull(%3$s,0)) > cast(?1 as number)) " +
						"or (%1$s >= cast(?1 as number) and %1$s < cast(?2 as number))",
				Tasks.TASK_START, Tasks.TASK_END, TaskParts.TASK_PART_END);

		public static final String LIST_GROUPBY = TASK_ID;
		public static final String LIST_SORT = STATUS + ", " + TASK_START;
		public static final String WHERE_TASK_ID = TASK_ID + "=?";

		// REST attributes
		public static final String PATH = TABLE;
		public static final String PATH_TASK_PARTS = "tasks_parts";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH);
		public static final Uri CONTENT_TASK_PARTS_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH_TASK_PARTS);
		private static final String CONTENT_SUBTYPE = "vnd.tsvetkov.rabota-task";
		public static final String DIR_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_SUBTYPE;
		public static final String CONTENT_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_SUBTYPE;

		private Tasks() {
		}
	}
}
