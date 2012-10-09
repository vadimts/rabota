package eu.tsvetkov.rabota.provider;

import static eu.tsvetkov.rabota.Rabota.HOURLY_RATE;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import eu.tsvetkov.rabota.R;
import eu.tsvetkov.rabota.db.TaskHelper;
import eu.tsvetkov.rabota.model.Task;
import eu.tsvetkov.rabota.util.Log;

public class TaskProvider extends ContentProvider {

	private static final String TAG = TaskProvider.class.getSimpleName();

	private TaskHelper mDb;

	public static final int TASKS = 1;
	public static final int TASK_ID = 2;
	public static final int TASK_PARTS = 3;
	public static final int TASK_PART_ID = 4;
	private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		URI_MATCHER.addURI(TaskContract.AUTHORITY, TaskContract.Tasks.PATH, TASKS);
		URI_MATCHER.addURI(TaskContract.AUTHORITY, TaskContract.Tasks.PATH + "/#", TASK_ID);
		URI_MATCHER.addURI(TaskContract.AUTHORITY, TaskContract.Tasks.PATH_TASK_PARTS + "/#", TASK_PARTS);
		URI_MATCHER.addURI(TaskContract.AUTHORITY, TaskContract.TaskParts.PATH + "/#", TASK_PART_ID);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int count = 0;
		String id = uri.getLastPathSegment();

		Log.d(TAG, "Delete: " + uri + ", selection: " + selection + " " + selectionArgs);

		switch (URI_MATCHER.match(uri)) {
		case TASKS:
			count = mDb.getWritableDatabase().delete(TaskContract.Tasks.TABLE, selection, selectionArgs);
			break;
		case TASK_ID:
			count = mDb.getWritableDatabase().delete(TaskContract.Tasks.TABLE, appendTaskId(selection, id), selectionArgs);
			break;
		case TASK_PARTS:
			count = mDb.getWritableDatabase().delete(TaskContract.TaskParts.TABLE, appendForeignTaskId(selection, id), selectionArgs);
			break;
		case TASK_PART_ID:
			count = mDb.getWritableDatabase().delete(TaskContract.TaskParts.TABLE, appendPartId(selection, id), selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Wrong URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		int uriType = URI_MATCHER.match(uri);
		switch (uriType) {
		case TASKS:
			return TaskContract.Tasks.DIR_CONTENT_TYPE;
		case TASK_ID:
			return TaskContract.Tasks.CONTENT_TYPE;
		case TASK_PARTS:
			return TaskContract.TaskParts.DIR_CONTENT_TYPE;
		case TASK_PART_ID:
			return TaskContract.TaskParts.CONTENT_TYPE;
		default:
			throw new IllegalArgumentException("Wrong URI " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		long id = 0;
		Uri newUri = null;
		ContentValues insertValues = (values != null ? new ContentValues(values) : new ContentValues());


		switch (URI_MATCHER.match(uri)) {
		case TASKS:
			// Set default values if needed.
			if (!insertValues.containsKey(TaskContract.Tasks.TITLE)) {
				insertValues.put(TaskContract.Tasks.TITLE,
						getContext().getString(R.string.default_task_title));
			}
			if (!insertValues.containsKey(TaskContract.Tasks.HOURLY_RATE)) {
				insertValues.put(TaskContract.Tasks.HOURLY_RATE, HOURLY_RATE);
			}
			if (!insertValues.containsKey(TaskContract.Tasks.STATUS)) {
				insertValues.put(TaskContract.Tasks.STATUS, Task.Status.SCHEDULED.toString());
			}

			if (Log.DEBUG) Log.d(TAG, "Inserting to " + uri + ", values: " + insertValues);

			id = mDb.getWritableDatabase().insert(TaskContract.Tasks.TABLE, null, insertValues);
			newUri = ContentUris.withAppendedId(TaskContract.Tasks.CONTENT_URI, id);
			break;
		case TASK_PARTS:

			if (Log.DEBUG) Log.d(TAG, "Inserting to " + uri + ", values: " + insertValues);

			id = mDb.getWritableDatabase().insert(TaskContract.TaskParts.TABLE, null, insertValues);
			newUri = ContentUris.withAppendedId(TaskContract.TaskParts.CONTENT_URI, id);
			break;
		default:
			throw new IllegalArgumentException("Wrong URI " + uri);
		}
		if (id > 0) {
			getContext().getContentResolver().notifyChange(newUri, null);
			return newUri;
		} else {
			throw new SQLException("Failed to insert a row into " + uri);
		}
	}

	@Override
	public boolean onCreate() {
		mDb = new TaskHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		String groupBy = null;

		int uriType = URI_MATCHER.match(uri);
		switch (uriType) {
		case TASKS:
			queryBuilder.setTables(TaskContract.Tasks.LIST_TABLES);
			groupBy = TaskContract.Tasks.LIST_GROUPBY;
			sortOrder = TaskContract.Tasks.LIST_SORT;
			break;
		case TASK_ID:
			queryBuilder.setTables(TaskContract.Tasks.LIST_TABLES);
			queryBuilder.appendWhere(TaskContract.Tasks.TASK_ID + "=" + uri.getLastPathSegment());
			break;
		case TASK_PARTS:
			// Join 'tasks' and 'task_parts' tables to retrieve all task parts for a task with the specified ID.
			// E.g. SELECT * FROM tasks LEFT OUTER JOIN task_parts ON (tasks._id = task_parts.task_id) WHERE task._id = ?
			queryBuilder.setTables(TaskContract.TaskParts.LIST_TABLES);
			queryBuilder.appendWhere(TaskContract.Tasks.TASK_ID + "=" + uri.getLastPathSegment());
			sortOrder = TaskContract.TaskParts.LIST_SORT;
			break;
		case TASK_PART_ID:
			queryBuilder.setTables(TaskContract.TaskParts.TABLE);
			queryBuilder.appendWhere(TaskContract.TaskParts._ID + "=" + uri.getLastPathSegment());
			break;
		default:
			throw new IllegalArgumentException("Wrong URI " + uri);
		}


		Cursor cursor = queryBuilder.query(mDb.getReadableDatabase(), projection, selection, selectionArgs, groupBy, null, sortOrder);
		cursor.setNotificationUri(getContext().getContentResolver(), uri);

		if (Log.DEBUG) {
			int position = cursor.getPosition();
			synchronized (this) {
				Log.d("", "\n");
				Log.d(TAG, String.format("Query: %s; selection args: %s",
						queryBuilder.buildQuery(projection, selection, selectionArgs, groupBy, null, sortOrder, null),
						(selectionArgs != null ? TextUtils.join(", ", selectionArgs) : null)));
				Log.d(TAG, String.format("Found %d records", cursor.getCount()));
				cursor.moveToFirst();
				while (!cursor.isAfterLast()) {
					String s = "";
					for (int p = 0; p < cursor.getColumnCount(); p++) {
						s += cursor.getColumnName(p) + " = " + cursor.getString(p) + " | ";
					}
					Log.d(TAG, s);
					cursor.moveToNext();
				}
				Log.d("", "\n");
			}
			cursor.moveToPosition(position);
		}
		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		int count;
		String id = uri.getLastPathSegment();

		Log.d(TAG, "Updating " + uri + " with values: " + values + ", selection: " + selection + ", selection args: "
				+ (selectionArgs != null ? TextUtils.join(", ", selectionArgs) : "null"));

		switch (URI_MATCHER.match(uri)) {
		case TASKS:
			count = mDb.getWritableDatabase().update(TaskContract.Tasks.TABLE, values, selection, selectionArgs);
			break;
		case TASK_ID:
			count = mDb.getWritableDatabase().update(TaskContract.Tasks.TABLE, values, appendTaskId(selection, id), selectionArgs);
			break;
		case TASK_PARTS:
			count = mDb.getWritableDatabase().update(TaskContract.TaskParts.TABLE, values, appendForeignTaskId(selection, id), selectionArgs);
			break;
		case TASK_PART_ID:
			count = mDb.getWritableDatabase().update(TaskContract.TaskParts.TABLE, values, appendPartId(selection, id), selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Wrong URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	private String appendForeignTaskId(String selection, String id) {
		return TaskContract.TaskParts.TASK_ID + " = " + id + (!TextUtils.isEmpty(selection) ? " and (" + selection + ")" : "");
	}

	private String appendPartId(String selection, String id) {
		return TaskContract.TaskParts.TASK_PART_ID + " = " + id + (!TextUtils.isEmpty(selection) ? " and (" + selection + ")" : "");
	}

	private String appendTaskId(String selection, String id) {
		return TaskContract.Tasks.TASK_ID + " = " + id + (!TextUtils.isEmpty(selection) ? " and (" + selection + ")" : "");
	}

}
