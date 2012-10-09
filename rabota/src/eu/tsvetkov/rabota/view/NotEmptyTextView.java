package eu.tsvetkov.rabota.view;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

/**
 * {@link TextView} that is completely hidden when displaying empty text.
 * 
 * @author vadim
 */
public class NotEmptyTextView extends TextView {

	public NotEmptyTextView(Context context) {
		super(context);
	}

	public NotEmptyTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public NotEmptyTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void setText(CharSequence text, BufferType type) {
		super.setText(text, type);
		setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
	}
}
