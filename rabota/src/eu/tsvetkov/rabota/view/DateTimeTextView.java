package eu.tsvetkov.rabota.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
import eu.tsvetkov.rabota.util.Format;

/**
 * This {@link TextView} tries to interpret its text as timestamp milliseconds and show it as a date-time string.
 * 
 * @author vadim
 */
public class DateTimeTextView extends NotEmptyTextView {

	private static final String TAG = DateTimeTextView.class.getSimpleName();

	public DateTimeTextView(Context context) {
		super(context);
	}

	public DateTimeTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public DateTimeTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void setText(CharSequence text, BufferType type) {
		try {
			super.setText(Format.dateTime(Long.valueOf(text.toString())), type);
		} catch (NumberFormatException e) {
			super.setText(text, type);
		}
	}

	public void setText(long timestampMillis) {
		super.setText(timestampMillis > 0 ? Format.dateTime(timestampMillis) : "");
	}
}
