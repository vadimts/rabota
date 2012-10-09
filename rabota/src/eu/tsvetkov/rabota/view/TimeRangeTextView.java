package eu.tsvetkov.rabota.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
import eu.tsvetkov.rabota.util.Format;

/**
 * This {@link TextView} displays two given timestamps as a time range.
 * 
 * @author vadim
 */
public class TimeRangeTextView extends NotEmptyTextView {

	private static final String TAG = TimeRangeTextView.class.getSimpleName();

	public TimeRangeTextView(Context context) {
		super(context);
	}

	public TimeRangeTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public TimeRangeTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setText(long startMillis, long endMillis) {
		super.setText(Format.timeDateRange(startMillis, endMillis));
	}
}
