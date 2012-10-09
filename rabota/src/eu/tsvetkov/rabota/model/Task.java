package eu.tsvetkov.rabota.model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import eu.tsvetkov.rabota.util.Format;

public class Task {

	private final long id;
	private String title;
	private final Status status;
	private boolean startsImmediately;
	private Date start;
	private boolean endsManually;
	private Date end;
	private Date latestPartEnd;
	private boolean finished;
	private final List<TaskPart> parts;

	public Task(long id, String title, Status status, Date start, Date end) {
		super();
		this.id = id;
		this.title = title;
		this.status = status;
		this.start = start;
		this.end = end;
		this.parts = new ArrayList<TaskPart>();
	}

	public void addPart(TaskPart part) {
		parts.add(part);
	}

	public Date getEnd() {
		if (end != null) {
			return end;
		} else {
			if (latestPartEnd == null) {
				Date max = new Date(0);
				boolean hasMax = false;
				for (TaskPart part : parts) {
					Date partEnd = part.getEnd();
					if (partEnd != null && max.before(partEnd)) {
						max = partEnd;
						hasMax = true;
					}
				}
				if (hasMax) {
					latestPartEnd = max;
				}
			}
			return latestPartEnd;
		}
	}

	public long getEndMillis() {
		Date end = getEnd();
		return (end != null ? end.getTime() : -1);
	}

	public long getId() {
		return id;
	}

	public List<TaskPart> getParts() {
		return parts;
	}

	public Date getStart() {
		return start;
	}

	public long getStartMillis() {
		return start.getTime();
	}

	public Status getStatus() {
		return status;
	}

	public String getTitle() {
		return title;
	}

	public boolean isInRange(Calendar calStart, Calendar calEnd) {
		// If task has no parts yet, use its own start and end timestamps.
		if (parts.isEmpty()) {
			Date dateStart = calStart.getTime();
			Date dateEnd = calEnd.getTime();
			return (start.before(dateStart) && (end != null ? end.after(dateStart) : true))
					|| ((start.after(dateStart) || start == dateStart) && start.before(dateEnd));
		}

		// If task has parts, base calculation on them.
		for (TaskPart part : parts) {
			if (part.isInRange(calStart, calEnd)) {
				return true;
			}
		}
		return false;
	}

	public void setEnd(Date end) {
		this.end = end;
	}


	public void setStart(Date start) {
		this.start = start;
	}




	public void setTitle(String title) {
		this.title = title;
	}




	public static enum ChargeUnit {
		HOUR(0), DAY(1);

		public static final long MILLIS_HOUR = TimeUnit.HOURS.toMillis(1);
		// TODO make a preference for how many hours a work day is.
		public static final long MILLIS_DAY = TimeUnit.HOURS.toMillis(8);

		public static ChargeUnit fromInt(int i) {
			switch (i) {
			case 0:
				return HOUR;
			case 1:
				return DAY;
			default:
				throw new IllegalArgumentException("Invalid ChargeUnit value " + i);
			}
		}

		private final int value;

		ChargeUnit(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

		public String nameMultiple() {
			switch (value) {
			case 0:
				return Format.HOURS;
			case 1:
				return Format.DAYS;
			default:
				throw new IllegalArgumentException("Invalid ChargeUnit value " + value);
			}
		}

		public String rate() {
			switch (value) {
			case 0:
				return Format.HOURLY_RATE;
			case 1:
				return Format.DAILY_RATE;
			default:
				throw new IllegalArgumentException("Invalid ChargeUnit value " + value);
			}
		}

		public long toMillis() {
			switch (value) {
			case 0:
				return MILLIS_HOUR;
			case 1:
				return MILLIS_DAY;
			default:
				throw new IllegalArgumentException("Invalid ChargeUnit value " + value);
			}
		}

		@Override
		public String toString() {
			switch (value) {
			case 0:
				return Format.PER_HOUR;
			case 1:
				return Format.PER_DAY;
			default:
				throw new IllegalArgumentException("Invalid ChargeUnit value " + value);
			}
		}
	}

	public static enum Status {
		RUNNING(0), PAUSED(1), SCHEDULED(2), FINISHED(3);

		public static Status fromInt(int i) {
			switch (i) {
			case 0:
				return RUNNING;
			case 1:
				return PAUSED;
			case 2:
				return SCHEDULED;
			case 3:
				return FINISHED;
			default:
				throw new IllegalArgumentException("Invalid Status value " + i);
			}
		}

		private final int value;

		Status(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

		@Override
		public String toString() {
			return String.valueOf(value);
		}
	}
}
