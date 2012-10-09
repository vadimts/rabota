package eu.tsvetkov.rabota.util;



import static eu.tsvetkov.rabota.model.Task.Status.FINISHED;
import static eu.tsvetkov.rabota.model.Task.Status.PAUSED;
import static eu.tsvetkov.rabota.provider.TaskContract.TaskParts.LIST_TASK_PARTS;
import static eu.tsvetkov.rabota.provider.TaskContract.Tasks.CONTENT_TASK_PARTS_URI;
import static eu.tsvetkov.rabota.provider.TaskContract.Tasks.CONTENT_URI;
import static eu.tsvetkov.rabota.provider.TaskContract.Tasks.END_POS;
import static eu.tsvetkov.rabota.provider.TaskContract.Tasks.ID_POS;
import static eu.tsvetkov.rabota.provider.TaskContract.Tasks.LIST_SORT;
import static eu.tsvetkov.rabota.provider.TaskContract.Tasks.LIST_TASKS;
import static eu.tsvetkov.rabota.provider.TaskContract.Tasks.START_POS;
import static eu.tsvetkov.rabota.provider.TaskContract.Tasks.STATUS;
import static eu.tsvetkov.rabota.provider.TaskContract.Tasks.STATUS_POS;
import static eu.tsvetkov.rabota.provider.TaskContract.Tasks.TITLE_POS;
import static eu.tsvetkov.rabota.provider.TaskContract.Tasks.WHERE_TASKS_IN_RANGE;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.CursorLoader;
import android.text.TextUtils;
import eu.tsvetkov.rabota.model.Task;
import eu.tsvetkov.rabota.model.TaskPart;
import eu.tsvetkov.rabota.provider.TaskContract;

public class Content {

	private static final String TAG = Content.class.getSimpleName();

	public static List<Task> billableTasks(ContentResolver resolver, Calendar rangeStart, Calendar rangeEnd) {
		List<Task> tasks = new ArrayList<Task>();
		String where = String.format("%1$s=%2$d or %1$s=%3$d", STATUS, FINISHED.getValue(), PAUSED.getValue());
		Cursor taskCursor = Content.taskCursor(resolver, rangeStart, rangeEnd, where);
		if (taskCursor.moveToFirst()) {
			do {
				long taskId = taskCursor.getLong(ID_POS);
				Task task = new Task(taskId, taskCursor.getString(TITLE_POS), Task.Status.fromInt(taskCursor.getInt(STATUS_POS)),
						Calc.date(taskCursor.getLong(START_POS)), Calc.date(taskCursor.getLong(END_POS)));
				tasks.add(task);

				Cursor taskParts = Content.taskPartCursor(resolver, taskId);
				if (taskParts.moveToFirst()) {
					do {
						TaskPart part = new TaskPart(taskParts.getLong(TaskContract.TaskParts.ID_POS), taskParts.getString(TaskContract.TaskParts.COMMENT_POS),
								Calc.date(taskParts.getLong(TaskContract.TaskParts.START_POS)), Calc.date(taskParts.getLong(TaskContract.TaskParts.END_POS)));
						task.addPart(part);
					} while (taskParts.moveToNext());
				}
			} while (taskCursor.moveToNext());
		}
		return tasks;
	}

	public static void deleteTask(ContentResolver contentResolver, long taskId) {

		if (Log.DEBUG) Log.d(TAG, String.format("Deleting task '%s'", taskId));

		Uri taskUri = ContentUris.withAppendedId(TaskContract.Tasks.CONTENT_URI, taskId);
		Uri taskPartsUri = Uri.withAppendedPath(TaskContract.Tasks.CONTENT_TASK_PARTS_URI, String.valueOf(taskId));
		contentResolver.delete(taskPartsUri, null, null);
		contentResolver.delete(taskUri, null, null);
	}

	/**
	 * Ends a running task part thast belongs to the given task. If there are no running task parts - nothing happens.
	 * 
	 * @param taskId
	 * @param endMillis
	 */
	public static void endRunningTaskPart(ContentResolver contentResolver, long taskId, long endMillis) {

		if (Log.DEBUG) Log.d(TAG, String.format("Finishing running part of task '%s' at %s", taskId, Format.dateTime(endMillis)));

		ContentValues values = new ContentValues();
		values.put(TaskContract.TaskParts.END, endMillis);
		Uri taskPartsUri = Uri.withAppendedPath(TaskContract.Tasks.CONTENT_TASK_PARTS_URI, String.valueOf(taskId));
		contentResolver.update(taskPartsUri, values, TaskContract.TaskParts.WHERE_RUNNING_TASK_PART, null);
	}

	public static void finishTask(ContentResolver contentResolver, long taskId, long endMillis) {

		if (Log.DEBUG) Log.d(TAG, String.format("Finishing task '%s' at %s", taskId, Format.dateTime(endMillis)));

		endRunningTaskPart(contentResolver, taskId, endMillis);
		// Set task status to FINISHED and set the end time.
		ContentValues values = new ContentValues();
		values.put(TaskContract.Tasks.END, endMillis);
		updateTask(contentResolver, taskId, Task.Status.FINISHED, values);
	}

	public static void pauseTask(ContentResolver contentResolver, long taskId) {

		if (Log.DEBUG) Log.d(TAG, String.format("Pausing task '%s' at %s", taskId, Format.now()));

		endRunningTaskPart(contentResolver, taskId, Calc.now());
		setTaskStatus(contentResolver, taskId, Task.Status.PAUSED);
	}

	public static void resetTask(ContentResolver contentResolver, long taskId) {

		if (Log.DEBUG) Log.d(TAG, String.format("Resetting task '%s'", taskId));

		Uri taskPartsUri = Uri.withAppendedPath(TaskContract.Tasks.CONTENT_TASK_PARTS_URI, String.valueOf(taskId));
		contentResolver.delete(taskPartsUri, null, null);
		setTaskStatus(contentResolver, taskId, Task.Status.SCHEDULED);
	}

	public static void resumeTask(ContentResolver contentResolver, long taskId, long startMillis) {

		if (Log.DEBUG) Log.d(TAG, String.format("Resuming task '%s' at %s", taskId, Format.dateTime(startMillis)));

		// Insert new running task part.
		Uri taskPartsUri = Uri.withAppendedPath(TaskContract.Tasks.CONTENT_TASK_PARTS_URI, String.valueOf(taskId));
		ContentValues values = new ContentValues();
		values.put(TaskContract.TaskParts.TASK_ID, taskId);
		values.put(TaskContract.TaskParts.START, startMillis);
		contentResolver.insert(taskPartsUri, values);
		// Set task status to RUNNING and null the end time.
		values = new ContentValues();
		values.putNull(TaskContract.Tasks.END);
		updateTask(contentResolver, taskId, Task.Status.RUNNING, values);
	}

	public static Cursor runningTasks(Context context, Calendar rangeStart, Calendar rangeEnd) {
		CursorLoader loader = tasksLoader(context, rangeStart, rangeEnd, TaskContract.Tasks.STATUS + "=" + Task.Status.RUNNING.getValue());
		return loader.loadInBackground();
	}

	public static void setTaskStatus(ContentResolver contentResolver, long taskId, Task.Status status) {
		updateTask(contentResolver, taskId, status, new ContentValues());
	}

	public static void startTask(ContentResolver contentResolver, long taskId, long startMillis) {

		if (Log.DEBUG) Log.d(TAG, String.format("Starting task '%s' at %s", taskId, Format.dateTime(startMillis)));

		// Insert new running task part.
		Uri taskPartsUri = Uri.withAppendedPath(TaskContract.Tasks.CONTENT_TASK_PARTS_URI, String.valueOf(taskId));
		ContentValues values = new ContentValues();
		values.put(TaskContract.TaskParts.TASK_ID, taskId);
		values.put(TaskContract.TaskParts.START, startMillis);
		contentResolver.insert(taskPartsUri, values);
		// Set task status to RUNNING and null the end time.
		values = new ContentValues();
		values.put(TaskContract.Tasks.START, startMillis);
		values.putNull(TaskContract.Tasks.END);
		updateTask(contentResolver, taskId, Task.Status.RUNNING, values);
	}

	public static Cursor taskCursor(ContentResolver resolver, Calendar rangeStart, Calendar rangeEnd, String selection) {
		String where = String.format("(%s) and (%s)", WHERE_TASKS_IN_RANGE, (!TextUtils.isEmpty(selection) ? selection : "1=1"));
		String[] selectionArgs = new String[] { Calc.milliStr(rangeStart), Calc.milliStr(rangeEnd) };
		return resolver.query(CONTENT_URI, LIST_TASKS, where, selectionArgs, LIST_SORT);
	}

	public static Cursor taskPartCursor(ContentResolver resolver, long taskId) {
		return resolver.query(ContentUris.withAppendedId(CONTENT_TASK_PARTS_URI, taskId), LIST_TASK_PARTS, null, null, null);
	}

	public static Cursor taskParts(Context context, long taskId) {
		return taskParts(context, taskId, 0, 0);
	}

	public static Cursor taskParts(Context context, long taskId, Calendar start, Calendar end) {
		return taskParts(context, taskId, Calc.millis(start), Calc.millis(end));
	}

	public static Cursor taskParts(Context context, long taskId, long start, long end) {
		CursorLoader loader = taskPartsLoader(context, taskId, start, end);
		return loader.loadInBackground();
	}

	public static CursorLoader taskPartsLoader(Context context, long taskId) {
		return taskPartsLoader(context, taskId, 0, 0);
	}

	public static CursorLoader taskPartsLoader(Context context, long taskId, long start, long end) {
		String selection = (start > 0 && end > 0 ? TaskContract.TaskParts.WHERE_TASK_PARTS_IN_RANGE : null);
		String[] args = (start > 0 && end > 0 ? new String[] { String.valueOf(start), String.valueOf(end) } : null);
		return new CursorLoader(context, taskPartsUri(taskId), TaskContract.TaskParts.LIST_TASK_PARTS, selection, args, null);
	}

	public static Uri taskPartsUri(long taskId) {
		return ContentUris.withAppendedId(TaskContract.Tasks.CONTENT_TASK_PARTS_URI, taskId);
	}

	public static CursorLoader tasksLoader(Context context, Calendar rangeStart, Calendar rangeEnd, String where) {
		return new CursorLoader(context, TaskContract.Tasks.CONTENT_URI,
				TaskContract.Tasks.LIST_TASKS,
				String.format("(%s) and (%s)", TaskContract.Tasks.WHERE_TASKS_IN_RANGE, (!TextUtils.isEmpty(where) ? where : "1=1")),
				new String[] { String.valueOf(Calc.millis(rangeStart)), String.valueOf(Calc.millis(rangeEnd)) }, null);
	}

	public static Uri tasksUri() {
		return TaskContract.Tasks.CONTENT_URI;
	}

	public static Uri taskUri(long taskId) {
		return ContentUris.withAppendedId(TaskContract.Tasks.CONTENT_URI, taskId);
	}

	public static void updateTask(ContentResolver contentResolver, long taskId, Task.Status status, ContentValues values) {

		if (Log.DEBUG) Log.d(TAG, String.format("Updating task '%s' to status %s, values: %s", taskId, status.name(), values));

		Uri taskUri = ContentUris.withAppendedId(TaskContract.Tasks.CONTENT_URI, taskId);
		values.put(TaskContract.Tasks.STATUS, status.getValue());
		contentResolver.update(taskUri, values, null, null);
	}
}
