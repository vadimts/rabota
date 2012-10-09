package eu.tsvetkov.rabota.util;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentResolver;
import android.content.Context;
import android.text.TextUtils;
import eu.tsvetkov.rabota.R;
import eu.tsvetkov.rabota.Rabota;
import eu.tsvetkov.rabota.model.Invoice;
import eu.tsvetkov.rabota.model.Task;
import eu.tsvetkov.rabota.model.Task.ChargeUnit;
import eu.tsvetkov.rabota.model.TaskPart;

public class InvoiceBuilder {

	private static final String TAG = InvoiceBuilder.class.getSimpleName();

	private final Calendar start;
	private final Calendar end;
	private int timePeriod;
	private final Invoice invoice;
	private final List<Task> tasks;
	private final ContentResolver resolver;

	public InvoiceBuilder(ContentResolver resolver, Calendar rangeStart, Calendar rangeEnd) {
		this.start = rangeStart;
		this.end = rangeEnd;
		this.resolver = resolver;
		invoice = new Invoice();
		tasks = Content.billableTasks(resolver, rangeStart, rangeEnd);
	}

	public Invoice newInvoice() {

		// Define the time period of the invoice.
		if (Calc.isMonthRange(start, end)) {
			timePeriod = Calendar.MONTH;
		}
		if (Calc.isWeekRange(start, end)) {
			timePeriod = Calendar.WEEK_OF_MONTH;
		}

		String invoiceNumber = Format.formatDate(Rabota.pref(R.string.pref_invoice_number), start);
		Date invoiceDate = Calc.date();

		if (Log.DEBUG) Log.d(TAG, String.format("Creating new invoice %s from %s", invoiceNumber, Format.date(invoiceDate)));

		List<Task> tasks = Content.billableTasks(resolver, start, end);
		long duration = Calc.duration(tasks, start, end);
		ChargeUnit chargeUnit = Task.ChargeUnit.fromInt(Rabota.prefInt(R.string.pref_payment_charge_unit));
		String durationHours = Format.duration(duration);
		String durationChargeUnits = (chargeUnit == Task.ChargeUnit.HOUR ? durationHours : Format.daysDuration(duration));
		Double rate = Rabota.prefDouble(R.string.pref_payment_rate);
		double netto = Calc.netto(duration, rate, chargeUnit);
		int tax = Rabota.prefInt(R.string.pref_payment_tax);

		Map<String, String> keyValues = new HashMap<String, String>();
		keyValues.put("invoice number", invoiceNumber);
		keyValues.put("invoice date", Format.date(invoiceDate));
		keyValues.put("time period", Format.timePeriod(start, end));
		keyValues.put("total duration", durationHours);
		keyValues.put("work duration", Format.workDuration(durationChargeUnits, chargeUnit));
		keyValues.put("rate", Format.workRate(Format.rate(rate, chargeUnit), chargeUnit));
		keyValues.put("netto", Format.price(netto));
		keyValues.put("tax", Format.tax(netto, tax));
		keyValues.put("brutto", Format.price(Calc.brutto(duration, rate, chargeUnit, tax)));
		keyValues.put("header", Rabota.pref(R.string.pref_invoice_header));
		keyValues.put("address", Rabota.pref(R.string.pref_invoice_address));
		keyValues.put("place date", Rabota.pref(R.string.pref_invoice_place_date));
		keyValues.put("title", Rabota.pref(R.string.pref_invoice_title));
		keyValues.put("text", Rabota.pref(R.string.pref_invoice_text));

		// Email parameters
		invoice.setEmailTo(Rabota.pref(R.string.pref_invoice_email_to));
		if (TextUtils.isEmpty(invoice.getEmailTo())) {
			throw new IllegalStateException("Email TO field of invoice cannot be empty");
		}
		invoice.setEmailBcc(Rabota.pref(R.string.pref_invoice_email_bcc));
		invoice.setEmailSubject(Format.substitute(Rabota.pref(R.string.pref_invoice_email_subject), keyValues));
		invoice.setEmailBody(Format.substitute(
				"<p style=\"border-bottom:1px solid black; margin-bottom:4em; page-break-before:always\">{header}</p>" +
						"<p>{address}</p>" +
						"<p style=\"margin-top:2em; text-align:right\">{place date}</p>" +
						"<h1 style=\"text-align:center\">{title}</h1>" +
						"<p>{text}</p>" +
						"<h1 style=\"margin-top:5em; page-break-before:always; text-align:center\">Timetable: {time period}</h1>",
				keyValues));

		if (timePeriod == Calendar.MONTH) {
			appendToInvoiceBody(monthTimetable(start));
		} else if (timePeriod == Calendar.WEEK_OF_MONTH) {
			// TODO, for week, also for day period.
		}

		invoice.setNumber(invoiceNumber);
		invoice.setDate(invoiceDate);
		invoice.setEmailBody(Format.substitute(invoice.getEmailBody(), keyValues));

		return invoice;
	}

	private String appendToInvoiceBody(String append) {
		invoice.setEmailBody(invoice.getEmailBody() + append);
		return invoice.getEmailBody();
	}

	private String dayTimetable(Calendar day) {
		Context context = Rabota.getContext();
		Calendar nextDay = (Calendar) day.clone();
		nextDay.add(Calendar.DAY_OF_YEAR, 1);

		String header = String.format("<tr><td colspan=\"4\"><h4 style=\"margin:0\">%s</h4>\n", Format.dayDate(day));

		String out = "";
		for (Task task : tasks) {

			// Skip tasks that are not in the time range of the given day.
			if (!task.isInRange(day, nextDay)) continue;

			// List task details.
			out += String.format("<tr><td style=\"padding-right:2em; vertical-align: top\">%s" +
					"<td style=\"padding-right:2em\">%s" +
					"<td style=\"white-space:nowrap; padding-right:2em; vertical-align: top\">%s" +
					"<td style=\"white-space:nowrap; vertical-align: top\">%s\n",
					task.getId(),
					task.getTitle(),
					Format.taskTimeRange(task, day, nextDay),
					Format.duration(Calc.duration(task, day, nextDay)));
			List<TaskPart> parts = task.getParts();

			// Task parts in invoice are relevant only when there are more than one of them.
			// When there is only one task part, skip its details as the listing of the task itself (see above) is enough.
			if (parts.size() <= 1) continue;

			// List details of task parts.
			for (TaskPart part : parts) {
				if (part.isInRange(day, nextDay)) {
					out += String.format(
							"<tr style=\"color:#999; font-size:smaller\"><td><td style=\"padding-right:2em\">- %s %d%s" +
									"<td style=\"white-space:nowrap; padding-right:2em; vertical-align: top\">%s" +
									"<td style=\"white-space:nowrap; vertical-align: top\">%s\n",
							context.getString(R.string.part),
							parts.indexOf(part) + 1,
							(!TextUtils.isEmpty(part.getComment()) ? ": " + part.getComment() : ""),
							Format.timeRange(part.getStartMillis(), part.getEndMillis(), R.string.paused),
							Format.duration(Calc.duration(part, day, nextDay)));
				}
			}
		}

		return (TextUtils.isEmpty(out) ? "" : header + out);
	}

	private String monthTimetable(Calendar month) {
		Calendar week = (Calendar) month.clone();
		Calendar nextMonth = (Calendar) month.clone();
		nextMonth.add(Calendar.MONTH, 1);

		String out = "";
		while (week.before(nextMonth)) {
			out += weekTimetable(week, nextMonth);
			week.add(Calendar.WEEK_OF_MONTH, 1);
		}

		// If invoice is monthly - add a complete timetable header, otherwise - a month section.
		String header = (timePeriod == Calendar.MONTH ? timetableHeader() : String.format("<tr><td colspan=\"4\"><h2 style=\"margin:0\">%s</h2>\n",
				Format.month(month)));
		String footer = (timePeriod == Calendar.MONTH ? timetableFooter() : "");
		return (TextUtils.isEmpty(out) ? "" : header + out + footer);
	}

	private String timetableFooter() {
		return "<tr style=\"border-top:1px solid black\"><td colspan=\"3\"><h2 style=\"margin:0\">Total</h2>" +
				"<td style=\"white-space:nowrap\"><h2 style=\"margin:0\">{total duration}</h2>" +
				"</table>";
	}

	private String timetableHeader() {
		return "<table style=\"border-collapse: collapse; width:100%\">" +
				"<tr style=\"border-bottom:1px solid black\"><td><b>ID<td><b>Task<td><b>Time<td><b>Duration";
	}

	private String weekTimetable(Calendar week, Calendar end) {
		Calendar day = (Calendar) week.clone();
		Calendar nextWeek = (Calendar) week.clone();
		nextWeek.add(Calendar.WEEK_OF_MONTH, 1);

		String out = "";
		while (day.before(nextWeek) && day.before(end)) {
			out += dayTimetable(day);
			day.add(Calendar.DAY_OF_WEEK, 1);
		}

		// If invoice is weekly - add a complete timetable header, otherwise - a week section.
		String header = (timePeriod == Calendar.WEEK_OF_MONTH ? timetableHeader() : String.format("<tr><td colspan=\"4\"><h3 style=\"margin:0\">%s</h3>\n",
				Format.week(week)));
		String footer = (timePeriod == Calendar.WEEK_OF_MONTH ? timetableFooter() : "");
		return (TextUtils.isEmpty(out) ? "" : header + out + footer);
	}


}
