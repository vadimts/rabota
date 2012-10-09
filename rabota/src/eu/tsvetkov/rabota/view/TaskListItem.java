package eu.tsvetkov.rabota.view;

import android.content.Context;
import android.database.Cursor;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class TaskListItem extends RelativeLayout {

	private Cursor taskParts;

	public TaskListItem(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public TaskListItem(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public TaskListItem(Context context) {
		super(context);
	}

	public Cursor getTaskParts() {
		return taskParts;
	}

	public void setTaskParts(Cursor taskParts) {
		this.taskParts = taskParts;
	}
}
