package eu.tsvetkov.rabota.util;

import static eu.tsvetkov.rabota.Rabota.str;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;
import eu.tsvetkov.rabota.R;
import eu.tsvetkov.rabota.Rabota;
import eu.tsvetkov.rabota.model.Task;
import eu.tsvetkov.rabota.model.Task.ChargeUnit;
import eu.tsvetkov.rabota.model.TaskPart;

public final class Format {

	private static final String TAG = Format.class.getSimpleName();

	public static final String PER_HOUR = Rabota.str(R.string.per_hour);
	public static final String PER_DAY = Rabota.str(R.string.per_day);
	public static final String HOURS = Rabota.str(R.string.hours);
	public static final String DAYS = Rabota.str(R.string.days);
	public static final String HOURLY_RATE = Rabota.str(R.string.hourly_rate);
	public static final String DAILY_RATE = Rabota.str(R.string.daily_rate);
	public static final String FORMAT_PRICE = Rabota.str(R.string.format_price);

	public static String capitalize(String s) {
		return (!TextUtils.isEmpty(s) ? s.substring(0, 1).toUpperCase() + s.substring(1) : s);
	}

	public static String date(Calendar date) {
		return formatDate(Rabota.str(R.string.format_date), date);
	}

	public static String date(Date date) {
		return formatDate(Rabota.str(R.string.format_date), date);
	}

	public static String dateRange(Calendar start, Calendar end) {
		String dateFormat = Rabota.str(R.string.format_date);
		return string(Rabota.str(R.string.format_time_range), formatDate(dateFormat, start), formatDate(dateFormat, end));
	}

	public static String dateTime(Date date) {
		return dateTime(date.getTime());
	}

	public static String dateTime(long timeMillis) {
		if (timeMillis <= 0) return Rabota.str(R.string.not_applicable);

		Calendar calNow = Calendar.getInstance();
		Calendar calTime = Calendar.getInstance();
		calTime.setTimeInMillis(timeMillis);

		int formatId = R.string.format_time_date;

		if (Calc.isSameDay(calTime, calNow)) {
			formatId = R.string.format_time_today;
		}
		else {
			calNow.add(Calendar.DAY_OF_MONTH, -1);
			if (Calc.isSameDay(calTime, calNow)) {
				formatId = R.string.format_time_yesterday;
			}
			else {
				calNow.add(Calendar.DAY_OF_MONTH, 2);
				if (Calc.isSameDay(calTime, calNow)) {
					formatId = R.string.format_time_tomorrow;
				}
			}
		}

		return formatDate(Rabota.str(formatId), calTime);
	}

	public static String dayDate(Calendar date) {
		return formatDate(Rabota.str(R.string.format_day_date), date);
	}

	public static String daysDuration(long durationMillis) {
		double days = (double) durationMillis / Task.ChargeUnit.DAY.toMillis();
		return string(R.string.format_duration_days, days, duration(durationMillis));
	}

	public static String duration(long durationMillis) {
		long hours = TimeUnit.MILLISECONDS.toHours(durationMillis);
		long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis - TimeUnit.HOURS.toMillis(hours));
		long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis - TimeUnit.HOURS.toMillis(hours) - TimeUnit.MINUTES.toMillis(minutes));

		String shours = Format.number(Rabota.str(R.string.format_duration_hours), hours);
		String sminutes = Format.number(Rabota.str(R.string.format_duration_minutes), minutes);
		String sseconds = Format.number(Rabota.str(R.string.format_duration_seconds), seconds);

		StringBuffer sb = new StringBuffer(shours);
		sb.append(sb.length() > 0 ? " " : "").append(sminutes).append(sb.length() > 0 ? " " : "").append(sseconds);
		return sb.toString();
	}

	public static String formatDate(String format, Calendar cal) {
		return (cal != null ? formatDate(format, cal.getTime()) : "");
	}

	public static String formatDate(String format, Date date) {
		return new SimpleDateFormat(format).format(date);
	}

	public static String month(Calendar month) {
		return formatDate(Rabota.str(R.string.format_month_year), month);
	}

	public static String monthYear(Calendar cal) {
		return formatDate(Rabota.str(R.string.format_month_year), cal.getTime());
	}

	public static String now() {
		return dateTime(Calc.now());
	}

	public static String number(String format, Number... params) {
		boolean nullParams = true;
		for (Number param : params) {
			if (param != null && param.intValue() > 0) {
				nullParams = false;
				break;
			}
		}
		return (nullParams ? "" : String.format(format, (Object[]) params));
	}

	public static String price(Number price) {
		try {
			NumberFormat format = NumberFormat.getCurrencyInstance();
			format.setCurrency(Currency.getInstance(Rabota.pref(R.string.pref_payment_currency)));
			return format.format(price);
		} catch (Exception e) {
			if (Log.WARN) Log.w(TAG, String.format("Failed to format %s as currency %s", String.valueOf(price), R.string.pref_payment_currency));
			return Rabota.str(R.string.error_value);
		}
	}

	public static String rate(double rate, Task.ChargeUnit chargeUnit) {
		return string(Rabota.str(R.string.format_rate), price(rate), chargeUnit.toString());
	}

	public static String string(int formatId, Object... params) {
		return string(str(formatId), params);
	}

	public static String string(String format, Object... params) {
		boolean nullParams = true;
		for (Object param : params) {
			if (param != null) {
				nullParams = false;
				break;
			}
		}
		return (nullParams ? "" : String.format(format, params));
	}

	public static String substitute(String text, Map<String, String> keyValues) {
		Matcher matcher = Pattern.compile("\\{(.+?)\\}").matcher(text);
		StringBuffer buffer = new StringBuffer();
		while (matcher.find()) {
			String key = matcher.group(1);
			String value = keyValues.get(key);
			matcher.appendReplacement(buffer, (value != null ? value : matcher.group()));
		}
		matcher.appendTail(buffer);
		return buffer.toString();
	}

	public static String taskTimeDateRange(Calendar startCal, Calendar endCal, Task.Status taskStatus) {
		return taskTimeDateRange(Calc.millis(startCal), Calc.millis(endCal), taskStatus);
	}

	public static String taskTimeDateRange(long startMillis, long endMillis, Task.Status taskStatus) {
		return timeDateRange(startMillis, endMillis, (taskStatus == Task.Status.RUNNING ? R.string.running : R.string.paused));
	}

	public static String taskTimeRange(long startMillis, long endMillis, Task.Status taskStatus) {
		return timeRange(startMillis, endMillis, (taskStatus == Task.Status.RUNNING ? R.string.running : R.string.paused));
	}

	public static String taskTimeRange(Task task, Calendar rangeStart, Calendar rangeEnd) {
		// First, define the task time range basing on the start end end times of the task itself.
		long startMillis = Calc.max(task.getStartMillis(), Calc.millis(rangeStart));
		long endMillis = Calc.min(task.getEndMillis(), Calc.millis(rangeEnd));
		// Then, if there are any task parts, refine the task time range basing on their start and end times.
		for (TaskPart part : task.getParts()) {
			if (part.isInRange(rangeStart, rangeEnd)) {
				startMillis = Calc.max(part.getStartMillis(), startMillis);
				endMillis = Calc.min(endMillis, part.getEndMillis());
			}
		}
		return taskTimeRange(startMillis, endMillis, task.getStatus());
	}

	public static String tax(double netto, int taxPercentage) {
		return string(Rabota.str(R.string.format_tax), taxPercentage, Format.price(Calc.tax(netto, taxPercentage)));
	}

	public static String timeDateRange(Date start, Date end) {
		return timeDateRange(start.getTime(), end.getTime());
	}

	public static String timeDateRange(long startMillis, long endMillis) {
		return timeDateRange(startMillis, endMillis, R.string.running);
	}

	public static String timeDateRange(long startMillis, long endMillis, int noEndTimeFormatId) {
		return timeDateRange(startMillis, endMillis, noEndTimeFormatId, true);
	}

	public static String timeDateRange(long startMillis, long endMillis, int noEndTimeFormatId, boolean showDate) {
		Calendar calStart = Calc.cal(startMillis);
		Calendar calEnd = Calc.cal();
		String endString;
		// Get the format string for the time of day.
		String startDateFormat = Rabota.str(R.string.format_time);

		// If the end time of the range has been specified - show the end date in brackets.
		if (endMillis > 0) {
			calEnd.setTimeInMillis(endMillis);
			String endDateFormat = (calEnd.get(Calendar.HOUR_OF_DAY) == calEnd.getMinimum(Calendar.HOUR_OF_DAY)
					&& calEnd.get(Calendar.MINUTE) == calEnd.getMinimum(Calendar.MINUTE) ? Rabota.str(R.string.format_time_24) : Rabota
					.str(R.string.format_time));
			endString = formatDate(endDateFormat + (showDate ? Rabota.str(R.string.format_date_suffix) : ""), calEnd);
		}
		// Else, if the task is still running - show the keyword defined by the noEndTimeFormatId parameter, e.g. "running".
		else {
			endString = Rabota.str(noEndTimeFormatId) + (showDate ? formatDate(Rabota.str(R.string.format_date_suffix), calEnd) : "");
		}

		// Add the date of start in brackets if it's different from the date of end.
		if (!Calc.isSameDay(calStart, calEnd) && showDate) {
			startDateFormat += Rabota.str(R.string.format_date_suffix);
		}

		// Join end and start dates in one string.
		String startString = formatDate(startDateFormat, calStart);
		return string(Rabota.str(R.string.format_time_range), startString, endString);
	}

	public static String timePeriod(Calendar start, Calendar end) {
		if (Calc.isMonthRange(start, end)) {
			// Output month and year.
			return month(start);
		}
		if (Calc.isWeekRange(start, end)) {
			// Output week of year + date range in brackets.
			return string(Rabota.str(R.string.format_week_date_range), week(start), dateRange(start, end));
		}
		// Output date range.
		return dateRange(start, end);
	}

	public static String timeRange(long startMillis, long endMillis) {
		return timeRange(startMillis, endMillis, R.string.not_applicable);
	}

	public static String timeRange(long startMillis, long endMillis, Calendar rangeStart, Calendar rangeEnd) {
		startMillis = Calc.max(startMillis, Calc.millis(rangeStart));
		endMillis = (endMillis > 0 ? Calc.min(endMillis, Calc.millis(rangeEnd)) : Calc.millis(rangeEnd));
		return timeRange(startMillis, endMillis);
	}

	public static String timeRange(long startMillis, long endMillis, int noEndTimeFormatId) {
		return timeDateRange(startMillis, endMillis, noEndTimeFormatId, false);
	}

	public static String week(Calendar week) {
		return formatDate(Rabota.str(R.string.format_week_year), week);
	}

	public static String workDuration(String duration, ChargeUnit chargeUnit) {
		return string(Rabota.str(R.string.format_work_duration), capitalize(chargeUnit.nameMultiple()), duration);
	}

	public static String workRate(String rate, ChargeUnit chargeUnit) {
		return string(Rabota.str(R.string.format_work_rate), capitalize(chargeUnit.rate()), rate);
	}
}
