package eu.tsvetkov.rabota.view;

import java.util.Calendar;
import java.util.Date;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.TabHost;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;
import eu.tsvetkov.rabota.R;
import eu.tsvetkov.rabota.util.Calc;
import eu.tsvetkov.rabota.util.Format;

public class DateTimePicker extends TabHost implements OnDateChangedListener, OnTimeChangedListener {

	private boolean timeFirst = true;
	private final DatePicker datePicker;
	private final TimePicker timePicker;

	public DateTimePicker(Context context, AttributeSet attrs) {
		this(context, attrs, Calc.nextHourDate());
	}

	public DateTimePicker(Context context, AttributeSet attrs, Date date) {
		super(context, attrs);

		Calendar cal = Calc.cal(date);

		((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.date_time_picker, this);

		// FIXME custom attribute doesn't work
		// boolean timeFirst = context.obtainStyledAttributes(attrs,
		// R.styleable.DateTimePicker).getBoolean(R.styleable.DateTimePicker_time_first,
		// false);

		setup();

		TabSpec tabTime = newTabSpec("Time");
		tabTime.setIndicator("Time");
		tabTime.setContent(R.id.tabTime);
		datePicker = (DatePicker) findViewById(R.id.datePicker);
		datePicker.init(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), this);

		TabSpec tabDate = newTabSpec("Date");
		tabDate.setIndicator("Date");
		tabDate.setContent(R.id.tabDate);
		timePicker = (TimePicker) findViewById(R.id.timePicker);
		timePicker.setIs24HourView(true);
		timePicker.setCurrentHour(cal.get(Calendar.HOUR_OF_DAY));
		timePicker.setCurrentMinute(cal.get(Calendar.MINUTE));
		timePicker.setOnTimeChangedListener(this);

		if (timeFirst) {
			addTab(tabTime);
			addTab(tabDate);
		} else {
			addTab(tabDate);
			addTab(tabTime);
		}

	}

	public long getMillis() {
		return getTime().getTime();
	}

	public Date getTime() {
		Calendar cal = Calc.cal();
		cal.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(), timePicker.getCurrentHour(), timePicker.getCurrentMinute(), 0);
		return cal.getTime();
	}

	public CharSequence getTimeString() {
		return Format.dateTime(getTime());
	}

	public boolean isTimeFirst() {
		return timeFirst;
	}

	public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
		onTimeChanged(getMillis());
	}

	public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
		onTimeChanged(getMillis());
	}

	public void setTime(Date time) {
		setTime(time.getTime());
	}

	public void setTime(long millis) {
		Calendar cal = Calc.cal(millis);
		datePicker.updateDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
		timePicker.setCurrentHour(cal.get(Calendar.HOUR_OF_DAY));
		timePicker.setCurrentMinute(cal.get(Calendar.MINUTE));
		onTimeChanged(millis);
	}

	public void setTimeFirst(boolean timeFirst) {
		this.timeFirst = timeFirst;
	}

	protected void onTimeChanged(long millis) {
	}
}
