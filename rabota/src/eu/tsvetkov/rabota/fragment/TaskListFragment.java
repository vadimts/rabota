package eu.tsvetkov.rabota.fragment;


import static eu.tsvetkov.rabota.Rabota.TIME_STEP;
import static eu.tsvetkov.rabota.Rabota.str;
import static eu.tsvetkov.rabota.provider.TaskContract.Tasks.END_POS;
import static eu.tsvetkov.rabota.provider.TaskContract.Tasks.ID_POS;
import static eu.tsvetkov.rabota.provider.TaskContract.Tasks.START_POS;
import static eu.tsvetkov.rabota.provider.TaskContract.Tasks.STATUS_POS;
import static eu.tsvetkov.rabota.provider.TaskContract.Tasks.TITLE_POS;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragmentLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import eu.tsvetkov.rabota.R;
import eu.tsvetkov.rabota.activity.TaskListActivity;
import eu.tsvetkov.rabota.intent.RabotaAction;
import eu.tsvetkov.rabota.model.Task;
import eu.tsvetkov.rabota.provider.TaskContract;
import eu.tsvetkov.rabota.util.Calc;
import eu.tsvetkov.rabota.util.Content;
import eu.tsvetkov.rabota.util.Format;
import eu.tsvetkov.rabota.util.Log;
import eu.tsvetkov.rabota.view.DurationTextView;
import eu.tsvetkov.rabota.view.TaskTimeRangeTextView;

public class TaskListFragment extends RefreshingListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

	private static final String TAG = TaskListFragment.class.getSimpleName();

	public static final int LIST_LOADER = 1;
	public static final String TIME_RANGE_START = "TIME_RANGE_START";
	public static final String TIME_RANGE_END = "TIME_RANGE_END";

	private final Calendar mStart;
	private final Calendar mEnd;
	ResourceCursorAdapter mAdapter;
	Map<Long, Cursor> taskParts = new HashMap<Long, Cursor>();
	ContentResolver resolver;

	public TaskListFragment(Calendar start, Calendar end) {
		super();
		this.mStart = start;
		this.mEnd = end;
		setTimeout(TIME_STEP);
	}

	public void clearTaskParts() {
		taskParts.clear();
	}

	public Calendar getEnd() {
		return mEnd;
	}

	public Calendar getStart() {
		return mStart;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		resolver = getActivity().getContentResolver();

		mAdapter = new ResourceCursorAdapter(getActivity(), R.layout.task_list_item, null, false) {

			@Override
			public void bindView(View view, Context context, Cursor cursor) {
				long taskId = cursor.getLong(ID_POS);
				String title = cursor.getString(TITLE_POS);
				long startMillis = cursor.getLong(START_POS);
				long endMillis = cursor.getLong(END_POS);
				Task.Status status = Task.Status.fromInt(cursor.getInt(STATUS_POS));

				if (!taskParts.containsKey(taskId)) {
					taskParts.put(taskId, Content.taskParts(getActivity(), taskId));
				}

				((TextView) view.findViewById(R.id.text_task_title)).setText(title);
				((TaskTimeRangeTextView) view.findViewById(R.id.text_task_time_range)).setText(startMillis, endMillis, status);

				String sDuration = (status == Task.Status.SCHEDULED ? str(R.string.scheduled) : Format.duration(Calc.taskDurationFromParts(taskParts
						.get(taskId))));
				((DurationTextView) view.findViewById(R.id.text_task_duration)).setText(sDuration);

				ImageView icon = (ImageView) view.findViewById(R.id.icon_task);
				icon.setVisibility(status == Task.Status.SCHEDULED ? View.INVISIBLE : View.VISIBLE);
				if (status == Task.Status.RUNNING) {
					icon.setImageResource(R.drawable.play);
				}
				else if (status == Task.Status.PAUSED) {
					icon.setImageResource(R.drawable.pause);
				}
				else if (status == Task.Status.FINISHED) {
					icon.setImageResource(R.drawable.check);
				}

				if (Log.VERBOSE) Log.v(TAG, String.format("Binding %s task '%s' with %d part(s): start %s, end %s, duration %s",
						status.name(), cursor.getString(TITLE_POS), taskParts.get(taskId).getCount(),
						Format.dateTime(startMillis), Format.dateTime(endMillis), sDuration));
			};
		};
		setListAdapter(mAdapter);
		getLoaderManager().initLoader(LIST_LOADER, null, this);

		registerForContextMenu(getListView());
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		} catch (ClassCastException e) {
			Log.e(TAG, "Invalid menuInfo", e);
			return false;
		}

		long taskId = info.id;

		boolean b = true;
		switch (item.getItemId()) {
		case R.id.task_list_context_finish:
			Content.finishTask(resolver, taskId, Calc.now());
			break;
		case R.id.task_list_context_resume:
			Content.resumeTask(resolver, taskId, Calc.now());
			break;
		case R.id.task_list_context_view:
			viewTask(taskId);
			break;
		case R.id.task_list_context_edit:
			editTask(taskId);
			break;
		case R.id.task_list_context_reset:
			Content.resetTask(resolver, taskId);
			break;
		case R.id.task_list_context_delete:
			Content.deleteTask(resolver, taskId);
			break;
		default:
			b = super.onContextItemSelected(item);
		}
		onDataSetChange();
		return b;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		} catch (ClassCastException e) {
			Log.e(TAG, "Invalid menuInfo", e);
			return;
		}

		Cursor cursor = (Cursor) mAdapter.getItem(info.position);
		if (cursor == null) {
			return;
		}

		getActivity().getMenuInflater().inflate(R.menu.task_list_context_menu, menu);
		menu.setHeaderTitle(cursor.getString(TaskContract.Tasks.TITLE_POS));

		// Hide irrelevant menu items depending on task status.
		menu.findItem(R.id.task_list_context_resume).setVisible(false);
		int status = cursor.getInt(TaskContract.Tasks.STATUS_POS);
		if (status == Task.Status.SCHEDULED.getValue()) {
		}
		else if (status == Task.Status.RUNNING.getValue()) {
		}
		else if (status == Task.Status.PAUSED.getValue()) {
		}
		else if (status == Task.Status.FINISHED.getValue()) {
			menu.findItem(R.id.task_list_context_resume).setVisible(true);
			menu.findItem(R.id.task_list_context_finish).setVisible(false);
		}

		// Append to the menu items for any other activities that can do stuff with it as well.
		// This does a query on the system for any activities that implement the ALTERNATIVE_ACTION for our data, adding a menu item for each one that
		// is found.
		Intent intent = new Intent(null, Uri.withAppendedPath(TaskContract.Tasks.CONTENT_URI, Integer.toString((int) info.id)));
		intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
		menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0, new ComponentName(getActivity(), TaskListActivity.class), null, intent, 0,
				null);
	}

	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return Content.tasksLoader(getActivity(), getStart(), getEnd(), null);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.task_list, null);
		ListFragmentLayout.setupIds(view);

		TextView textHeader = (TextView) view.findViewById(R.id.header_task_list);
		textHeader.setText(Format.monthYear(getStart()));

		return view;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		onTaskClick(position, id);
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
		// mAdapter.notifyDataSetChanged();
		if (data.getCount() == 0) {
			setEmptyText(getResources().getString(R.string.no_tasks));
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setEmptyText(null);
	}

	protected void onDataSetChange() {

		if (Log.DEBUG) Log.d(TAG, String.format("Invoking onDataChange in %s", this));

		getLoaderManager().restartLoader(LIST_LOADER, null, this);
		mAdapter.notifyDataSetChanged();
		clearTaskParts();
	}

	@Override
	protected void onRefresh() {
		if (mAdapter == null || mAdapter.isEmpty()) return;

		if (Log.DEBUG) Log.d(TAG, String.format("Invoking onRefresh in %s", this));

		for (int i = 0; i < mAdapter.getCount(); i++) {
			Cursor cursor = (Cursor) mAdapter.getItem(i);
			startTasksOnRefresh(cursor);
			endTasksOnRefresh(cursor);
		}
	}

	private void editTask(long taskId) {
		Uri taskUri = ContentUris.withAppendedId(TaskContract.Tasks.CONTENT_URI, taskId);
		startActivity(new Intent(RabotaAction.EDIT_TASK, taskUri));
	}

	private void endTasksOnRefresh(Cursor task) {
		long endMillis = task.getLong(END_POS);
		if (task.getInt(STATUS_POS) != Task.Status.FINISHED.getValue() && endMillis > 0 && endMillis < Calc.now()) {

			if (Log.DEBUG) Log.d(TAG, String.format("Task '%s' will be automatically finished", task.getString(TITLE_POS)));

			Content.finishTask(resolver, task.getLong(ID_POS), endMillis);
			onDataSetChange();
		}
	}

	private boolean onTaskClick(int position, long taskId) {

		Cursor task = (Cursor) mAdapter.getItem(position);

		if (Log.DEBUG) Log.d(TAG, String.format("Clicked the task %s", task.getString(TITLE_POS)));

		int status = task.getInt(TaskContract.Tasks.STATUS_POS);
		// If the task is running - put it on pause:
		if (status == Task.Status.RUNNING.getValue()) {
			Content.pauseTask(resolver, taskId);
		}
		// Else if the task is finished - open its details view:
		else if (status == Task.Status.FINISHED.getValue()) {
			viewTask(taskId);
		}
		// Else if the task has only been scheduled - start it:
		else if (status == Task.Status.SCHEDULED.getValue()) {
			Content.startTask(resolver, taskId, Calc.now());
		}
		// Else if task is on pause - resume it:
		else if (status == Task.Status.PAUSED.getValue()) {
			Content.resumeTask(resolver, taskId, Calc.now());
		}
		onDataSetChange();
		return true;
	}

	private void startTasksOnRefresh(Cursor task) {
		// If task has a fixed start time and it's due - start task.
		long startMillis = task.getLong(START_POS);
		if (task.getInt(STATUS_POS) == Task.Status.SCHEDULED.getValue() && startMillis > 0 && startMillis <= Calc.now()) {

			if (Log.DEBUG) Log.d(TAG, String.format("Task '%s' will be automatically started", task.getString(TITLE_POS)));

			Content.startTask(resolver, task.getLong(ID_POS), startMillis);
			onDataSetChange();
		}
	}

	private void viewTask(long taskId) {
		Intent intent = new Intent(RabotaAction.VIEW_TASK, Content.taskUri(taskId));
		intent.putExtra(TIME_RANGE_START, getStart().getTimeInMillis());
		intent.putExtra(TIME_RANGE_END, getEnd().getTimeInMillis());
		startActivity(intent);
	}
}