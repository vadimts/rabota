package eu.tsvetkov.rabota.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
import eu.tsvetkov.rabota.util.Format;

/**
 * This {@link TextView} tries to convert its text to a duration string. E.g "313900538" will be converted to "49 hr 26 min".
 * 
 * @author vadim
 */
public class DurationTextView extends NotEmptyTextView {

	private static final String TAG = DurationTextView.class.getSimpleName();

	private boolean showSeconds = false;

	public DurationTextView(Context context) {
		super(context);
	}

	public DurationTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public DurationTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void setText(CharSequence text, BufferType type) {
		try {
			super.setText(Format.duration(Long.valueOf(text.toString())), type);
		} catch (NumberFormatException e) {
			super.setText(text, type);
		}
	}

	public void setText(long duration) {
		super.setText(Format.duration(duration));
	}

	public void setShowSeconds(boolean showSeconds) {
		this.showSeconds = showSeconds;
	}
}
