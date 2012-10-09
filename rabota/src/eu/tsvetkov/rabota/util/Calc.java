package eu.tsvetkov.rabota.util;


import static eu.tsvetkov.rabota.Rabota.TIME_STEP;
import static eu.tsvetkov.rabota.provider.TaskContract.Tasks.END_POS;
import static eu.tsvetkov.rabota.provider.TaskContract.Tasks.ID_POS;
import static eu.tsvetkov.rabota.provider.TaskContract.Tasks.START_POS;
import static eu.tsvetkov.rabota.provider.TaskContract.Tasks.STATUS_POS;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import eu.tsvetkov.rabota.model.Task;
import eu.tsvetkov.rabota.model.Task.ChargeUnit;
import eu.tsvetkov.rabota.model.TaskPart;
import eu.tsvetkov.rabota.provider.TaskContract;

public class Calc {

	private static final String TAG = Calc.class.getSimpleName();

	public static Calendar add(Calendar cal, int field, int offset) {
		Calendar newCal = cal(cal);
		newCal.add(field, offset);
		return newCal;
	}

	public static Date add(Date date, int field, int offset) {
		return add(cal(date), field, offset).getTime();
	}

	public static Calendar add(int field, int offset) {
		return add(cal(), field, offset);
	}

	public static long add(long millis, int field, int offset) {
		Calendar cal = add(cal(millis), field, offset);
		return millis(cal);
	}

	public static double brutto(long duration, Double rate, ChargeUnit chargeUnit, int taxPercentage) {
		double netto = netto(duration, rate, chargeUnit);
		return netto + tax(netto, taxPercentage);
	}

	public static Calendar cal() {
		return Calendar.getInstance();
	}

	public static Calendar cal(Calendar cal) {
		return (Calendar) cal.clone();
	}

	public static Calendar cal(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal;
	}

	public static Calendar cal(long dateMillis) {
		if (dateMillis <= 0) return null;

		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(dateMillis);
		return calendar;
	}

	public static Date date() {
		return new Date();
	}

	public static Date date(long dateMillis) {
		return (dateMillis > 0 ? new Date(dateMillis) : null);
	}

	public static long duration(List<Task> tasks, Calendar rangeStart, Calendar rangeEnd) {
		return duration(tasks, Calc.millis(rangeStart), Calc.millis(rangeEnd));
	}

	public static long duration(List<Task> tasks, long rangeStart, long rangeEnd) {
		long duration = 0;
		for (Task task : tasks) {
			duration += duration(task, rangeStart, rangeEnd);
		}
		return duration;
	}

	public static long duration(Task task, Calendar rangeStart, Calendar rangeEnd) {
		return duration(task, Calc.millis(rangeStart), Calc.millis(rangeEnd));
	}

	public static long duration(Task task, long rangeStart, long rangeEnd) {
		long duration = 0;
		if (task.getParts().isEmpty()) {
			duration = roundTime(duration(task.getStartMillis(), task.getEndMillis()));
		}
		else {
			for (TaskPart part : task.getParts()) {
				duration += duration(part, rangeStart, rangeEnd);
			}
		}

		if (Log.VERBOSE) Log.v(TAG, String.format("Task %d '%s' duration: %s", task.getId(), task.getTitle(), Format.duration(duration)));

		return duration;
	}

	public static final long duration(TaskPart taskPart, Calendar rangeStart, Calendar rangeEnd) {
		return duration(taskPart, Calc.millis(rangeStart), Calc.millis(rangeEnd));
	}

	public static final long duration(TaskPart taskPart, long rangeStart, long rangeEnd) {
		long taskPartEndMillis = taskPart.getEndMillis();
		long start = Math.max(taskPart.getStartMillis(), rangeStart);
		long end = Math.min(taskPartEndMillis > 0 ? taskPartEndMillis : now(), rangeEnd);
		long duration = end - start;
		duration = (duration > 0 ? roundTime(duration) : 0);

		if (Log.VERBOSE) Log.v(TAG, String.format("Task part %d duration: %s", taskPart.getId(), Format.duration(duration)));

		return duration;
	}

	public static Calendar firstWorkDay(Calendar month) {
		Calendar cal = cal(month);
		// TODO instead of getMinimum use the first working week day depending on preferences.
		cal.set(Calendar.DAY_OF_MONTH, cal.getMinimum(Calendar.DAY_OF_MONTH));
		cal.set(Calendar.HOUR_OF_DAY, firstWorkHour());
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		return cal;
	}

	public static long firstWorkDayMillis(Calendar month) {
		return firstWorkDay(month).getTimeInMillis();
	}

	public static int firstWorkHour() {
		// TODO put to preferences.
		return 10;
	}

	/**
	 * Returns true if given calendars are boundaries of a month.
	 * 
	 * @param start
	 * @param end
	 * @return
	 */
	public static boolean isMonthRange(Calendar start, Calendar end) {
		int day = Calendar.DAY_OF_MONTH;
		int month = Calendar.MONTH;
		int year = Calendar.YEAR;
		int firstDay = start.getMinimum(Calendar.DAY_OF_MONTH);

		// Return true if period starts on first day of one month and ends on first day of the next month
		return (start.get(day) == firstDay && end.get(day) == firstDay && (start.get(month) == end.get(month) - 1)
		// or starts on 1st of December and ends on 1st of January next year:
		|| (start.get(month) == start.getMaximum(month) && end.get(month) == end.getMinimum(month) && start.get(year) == end.get(year) - 1));
	}

	public static boolean isSameDay(Calendar cal1, Calendar cal2) {
		if (cal1 == null || cal2 == null) return false;
		return cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR);
	}

	public static boolean isSameMonth(Calendar cal1, Calendar cal2) {
		if (cal1 == null || cal2 == null) return false;
		return cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR);
	}

	/**
	 * Returns true if given dates are boundaries of a week.
	 * 
	 * @param start
	 * @param end
	 * @return
	 */
	public static boolean isWeekRange(Calendar start, Calendar end) {
		int month = Calendar.MONTH;
		int year = Calendar.YEAR;
		int day = Calendar.DAY_OF_WEEK;
		int week = Calendar.WEEK_OF_YEAR;
		int firstDay = start.getMinimum(Calendar.DAY_OF_WEEK);

		// Return true if period starts on first day of one week and ends on first day of the next week
		return (start.get(day) == firstDay && end.get(day) == firstDay && (start.get(week) == end.get(week) - 1)
		// or starts on 1st of December and ends on 1st of January next year:
		|| (start.get(week) == start.getMaximum(week) && end.get(month) == end.getMinimum(week) && start.get(year) == end.get(year) - 1));
	}

	public static int lastWorkHour() {
		// TODO put to preferences.
		return 18;
	}

	public static Calendar max(Calendar cal1, Calendar cal2) {
		return (cal1.after(cal2) ? cal1 : cal2);
	}

	public static long max(long millis1, long millis2) {
		return (millis1 > millis2 ? millis1 : millis2);
	}

	public static long millis(Calendar calendar) {
		return calendar != null ? calendar.getTimeInMillis() : 0;
	}

	public static long millis(Date date) {
		return date != null ? date.getTime() : 0;
	}

	public static long millis(long millis, int field, int offset) {
		Calendar cal = add(cal(millis), field, offset);
		return millis(cal);
	}

	public static String milliStr(Calendar cal) {
		return String.valueOf(millis(cal));
	}

	public static Calendar min(Calendar cal1, Calendar cal2) {
		return (cal1.before(cal2) ? cal1 : cal2);
	}

	public static long min(long millis1, long millis2) {
		return (millis1 < millis2 ? millis1 : millis2);
	}

	public static double netto(long duration, Double rate, Task.ChargeUnit chargeUnit) {
		return duration * rate / chargeUnit.toMillis();
	}

	public static Calendar nextHourCal() {
		Calendar cal = Calc.cal();
		cal.add(Calendar.HOUR_OF_DAY, 1);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		return cal;
	}

	public static Date nextHourDate() {
		return nextHourCal().getTime();
	}

	public static long nextHourMillis() {
		return nextHourCal().getTimeInMillis();
	}

	public static long now() {
		return System.currentTimeMillis();
	}

	public static final long roundTime(long time) {
		long div = time / TIME_STEP;
		return (div == 0 ? 1 : div) * TIME_STEP;
	}

	public static long taskDuration(Context context, Cursor task) {
		long taskId = task.getLong(ID_POS);
		long status = task.getLong(STATUS_POS);

		// Calculate duration of scheduled tasks from their own start and end time, as they don't have task parts yet:
		if (status == Task.Status.SCHEDULED.getValue()) {
			return duration(task.getLong(START_POS), task.getLong(END_POS));
		}
		// Calculate duration of other tasks from the combined duration of their task parts:
		else {
			return taskDurationFromParts(context, taskId, 0, Long.MAX_VALUE);
		}
	}

	public static long taskDuration(Context context, long taskId, Calendar rangeStart, Calendar rangeEnd) {
		return taskDurationFromParts(context, taskId, Calc.millis(rangeStart), Calc.millis(rangeEnd));
	}

	public static long taskDurationFromParts(Context context, long taskId, long rangeStart, long rangeEnd) {
		return taskDurationFromParts(Content.taskParts(context, taskId, rangeStart, rangeEnd), rangeStart, rangeEnd);
	}

	public static long taskDurationFromParts(Cursor taskParts) {
		return taskDurationFromParts(taskParts, 0, Long.MAX_VALUE);
	}

	public static long taskDurationFromParts(Cursor taskParts, long rangeStart, long rangeEnd) {
		long duration = 0;
		if (taskParts.moveToFirst()) {
			while (!taskParts.isAfterLast()) {
				long taskPartStartMillis = taskParts.getLong(TaskContract.TaskParts.START_POS);
				long taskPartEndMillis = taskParts.getLong(TaskContract.TaskParts.END_POS);
				duration += taskPartDuration(taskPartStartMillis, taskPartEndMillis, rangeStart, rangeEnd);
				taskParts.moveToNext();
			}
		}
		return duration;
	}

	/**
	 * Calculates duration of a task part. If the task part is finished - use the difference between task part's end and start times, otherwise - between now
	 * and the task part's start. Duration is then rounded to a multiple of the time precision unit.
	 * 
	 * @param startMillis
	 * @param endMillis
	 * @return
	 */
	public static final long taskPartDuration(long startMillis, long endMillis) {
		return taskPartDuration(startMillis, endMillis, 0, Long.MAX_VALUE);
	}

	public static final long taskPartDuration(long startMillis, long endMillis, Calendar rangeStart, Calendar rangeEnd) {
		return taskPartDuration(startMillis, endMillis, Calc.millis(rangeStart), Calc.millis(rangeEnd));
	}

	public static final long taskPartDuration(long startMillis, long endMillis, long rangeStart, long rangeEnd) {
		long start = Math.max(startMillis, rangeStart);
		long end = Math.min(endMillis > 0 ? endMillis : now(), rangeEnd);
		long duration = end - start;
		return (duration > 0 ? roundTime(duration) : 0);
	}

	public static long tasksDuration(Context context, Cursor tasks, Calendar rangeStart, Calendar rangeEnd) {
		return tasksDuration(context, tasks, millis(rangeStart), millis(rangeEnd));
	}

	public static long tasksDuration(Context context, Cursor tasks, long rangeStart, long rangeEnd) {
		long duration = 0;
		if (tasks.moveToFirst()) {
			while (!tasks.isAfterLast()) {
				long taskId = tasks.getLong(TaskContract.Tasks.ID_POS);
				duration += taskDurationFromParts(context, taskId, rangeStart, rangeEnd);
				tasks.moveToNext();
			}
		}
		return duration;
	}

	public static double tax(double netto, int taxPercentage) {
		return netto * taxPercentage / 100;
	}

	private static long duration(long startMillis, long endMillis) {
		return (endMillis > startMillis ? endMillis - startMillis : 0);
	}

}
