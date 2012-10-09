package eu.tsvetkov.rabota.fragment;


import static eu.tsvetkov.rabota.Rabota.TIME_STEP;
import static eu.tsvetkov.rabota.provider.TaskContract.TaskParts.COMMENT_POS;
import static eu.tsvetkov.rabota.provider.TaskContract.TaskParts.END_POS;
import static eu.tsvetkov.rabota.provider.TaskContract.TaskParts.LIST_TASK_PARTS;
import static eu.tsvetkov.rabota.provider.TaskContract.TaskParts.START_POS;
import static eu.tsvetkov.rabota.provider.TaskContract.Tasks.CONTENT_TASK_PARTS_URI;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import eu.tsvetkov.rabota.R;
import eu.tsvetkov.rabota.activity.ViewTaskActivity;
import eu.tsvetkov.rabota.util.Calc;
import eu.tsvetkov.rabota.util.Format;
import eu.tsvetkov.rabota.util.Log;
import eu.tsvetkov.rabota.view.DurationTextView;
import eu.tsvetkov.rabota.view.TimeRangeTextView;

public class TaskPartsListFragment extends RefreshingListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

	private static final String TAG = TaskPartsListFragment.class.getSimpleName();
	public static final int LIST_LOADER = 1;

	ResourceCursorAdapter mAdapter;
	private Uri mTaskPartsUri;

	public TaskPartsListFragment() {
		super();
		setTimeout(TIME_STEP);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getActivity().getIntent();
		Uri mTaskUri = intent.getData();
		mTaskPartsUri = Uri.withAppendedPath(CONTENT_TASK_PARTS_URI, mTaskUri.getLastPathSegment());

		mAdapter = new ResourceCursorAdapter(getActivity(), R.layout.task_parts_list_item, null, false) {

			/**
			 * Binds details of task part to view.
			 */
			@Override
			public void bindView(View view, Context context, Cursor cursor) {
				String comment = cursor.getString(COMMENT_POS);
				long startMillis = cursor.getLong(START_POS);
				long endMillis = cursor.getLong(END_POS);
				long duration = Calc.taskPartDuration(startMillis, endMillis);

				((TextView) view.findViewById(R.id.text_task_part_comment)).setText(comment);
				((TimeRangeTextView) view.findViewById(R.id.text_task_part_time_range)).setText(startMillis, endMillis);
				((DurationTextView) view.findViewById(R.id.text_task_part_duration)).setText(duration);
				// Task part's icon: a tick if the task part is finished, and a play icon otherwise.
				((ImageView) view.findViewById(R.id.icon_task_part)).setImageResource(endMillis > 0 ? R.drawable.check_grey : R.drawable.play);

				if (Log.VERBOSE) Log.v(TAG, String.format("Binding task part '%s': start %d, end %d, duration %d",
						comment, startMillis, endMillis, Format.duration(duration)));
			}
		};
		setListAdapter(mAdapter);
		setListShown(false);
		getLoaderManager().initLoader(LIST_LOADER, null, this);

		setEmptyText(getResources().getString(R.string.task_parts_list_empty));

		registerForContextMenu(getListView());
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		// AdapterView.AdapterContextMenuInfo info;
		// try {
		// info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		// } catch (ClassCastException e) {
		// Log.e(TAG, "Invalid menuInfo", e);
		// return;
		// }
		//
		// Cursor cursor = (Cursor) mAdapter.getItem(info.position);
		// if (cursor == null) {
		// return;
		// }
		//
		// getActivity().getMenuInflater().inflate(R.menu.task_list_context_menu, menu);
		// menu.setHeaderTitle(cursor.getString(TaskContract.Tasks.COL_POS_TITLE));
		//
		// // Append to the menu items for any other activities that can do stuff with it as well.
		// // This does a query on the system for any activities that implement the ALTERNATIVE_ACTION for our data, adding a menu item for each one that
		// // is found.
		// Intent intent = new Intent(null, Uri.withAppendedPath(getActivity().getIntent().getData(), Integer.toString((int) info.id)));
		// intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
		// menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0, new ComponentName(getActivity(), TaskListActivity.class), null, intent, 0,
		// null);
	}

	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Intent intent = getActivity().getIntent();
		String timeRangeStart = String.valueOf(intent.getLongExtra(TaskListFragment.TIME_RANGE_START, 0));
		String timeRangeEnd = String.valueOf(intent.getLongExtra(TaskListFragment.TIME_RANGE_END, Long.MAX_VALUE));
		return new CursorLoader(getActivity(), mTaskPartsUri, LIST_TASK_PARTS, null, new String[] { /* timeRangeEnd, timeRangeStart */}, null);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Log.i(TAG, "Clicked item " + id);
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
		setListShown(true);
		FragmentActivity activity = getActivity();
		if (activity instanceof ViewTaskActivity) {
			// ((ViewTaskActivity) activity).onTaskPartsLoadFinished(loader, data);
		}
	}
}