package eu.tsvetkov.rabota.model;

import java.util.Calendar;
import java.util.Date;

public class TaskPart {

	private final long id;
	private String comment;
	private Date start;
	private Date end;

	public TaskPart(long id, String comment, Date start, Date end) {
		super();
		this.id = id;
		this.comment = comment;
		this.start = start;
		this.end = end;
	}

	public String getComment() {
		return comment;
	}

	public Date getEnd() {
		return end;
	}

	public long getEndMillis() {
		return (end != null ? end.getTime() : 0);
	}

	public long getId() {
		return id;
	}

	public Date getStart() {
		return start;
	}

	public long getStartMillis() {
		return start.getTime();
	}

	public boolean isInRange(Calendar calStart, Calendar calEnd) {
		Date dateStart = calStart.getTime();
		Date dateEnd = calEnd.getTime();
		return (start.before(dateStart) && (end != null ? end.after(dateStart) : true))
				|| ((start.after(dateStart) || start == dateStart) && start.before(dateEnd));
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public void setEnd(Date end) {
		this.end = end;
	}

	public void setStart(Date start) {
		this.start = start;
	}
}
