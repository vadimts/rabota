package eu.tsvetkov.rabota.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
import eu.tsvetkov.rabota.model.Task;
import eu.tsvetkov.rabota.util.Format;

/**
 * This {@link TextView} displays two given timestamps as a time range.
 * 
 * @author vadim
 */
public class TaskTimeRangeTextView extends NotEmptyTextView {

	private static final String TAG = TaskTimeRangeTextView.class.getSimpleName();

	public TaskTimeRangeTextView(Context context) {
		super(context);
	}

	public TaskTimeRangeTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public TaskTimeRangeTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setText(long startMillis, long endMillis, Task.Status taskStatus) {
		super.setText(Format.taskTimeDateRange(startMillis, endMillis, taskStatus));
	}
}
