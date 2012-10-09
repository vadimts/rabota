package eu.tsvetkov.rabota.model;

import java.util.Date;

public class Invoice {

	private String number;
	private Date date;
	private String emailTo;
	private String emailBcc;
	private String emailSubject;
	private String emailBody;

	public Invoice() {
	}

	public Invoice(String number, String emailTo, String emailBcc, String emailSubject, String emailBody) {
		super();
		this.number = number;
		this.emailTo = emailTo;
		this.emailBcc = emailBcc;
		this.emailSubject = emailSubject;
		this.emailBody = emailBody;
	}

	public Date getDate() {
		return date;
	}

	public String getEmailBcc() {
		return emailBcc;
	}

	public String getEmailBody() {
		return emailBody;
	}

	public String getEmailSubject() {
		return emailSubject;
	}

	public String getEmailTo() {
		return emailTo;
	}

	public String getNumber() {
		return number;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public void setEmailBcc(String emailBcc) {
		this.emailBcc = emailBcc;
	}

	public void setEmailBody(String emailBody) {
		this.emailBody = emailBody;
	}

	public void setEmailSubject(String emailSubject) {
		this.emailSubject = emailSubject;
	}

	public void setEmailTo(String emailTo) {
		this.emailTo = emailTo;
	}

	public void setNumber(String number) {
		this.number = number;
	}
}
