package eu.tsvetkov.rabota;

import java.io.IOException;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import eu.tsvetkov.rabota.gae.RabotaGaeConnector;

/**
 * 
 * @author vadim
 * 
 */
@SuppressWarnings("serial")
public class RabotaServlet extends HttpServlet {

	private static final String EMAIL_TYPE = "text/html; charset=UTF-8";
	private static final String GAE_ADMIN_NAME = "Rabota admin";
	private static final String GAE_ADMIN_EMAIL = "admin@rabota-gae.appspotmail.com";

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/plain");
		resp.getWriter().println("Hello, world");
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/plain");

		String to = req.getParameter(RabotaGaeConnector.PARAM_EMAIL_TO);
		String bcc = req.getParameter(RabotaGaeConnector.PARAM_EMAIL_BCC);
		String subject = req.getParameter(RabotaGaeConnector.PARAM_EMAIL_SUBJECT);
		String body = req.getParameter(RabotaGaeConnector.PARAM_EMAIL_BODY);

		Session session = Session.getDefaultInstance(new Properties());
		MimeMessage message = new MimeMessage(session);

		try {
			message.setFrom(new InternetAddress(GAE_ADMIN_EMAIL, GAE_ADMIN_NAME));
			if (to != null && !to.trim().isEmpty()) {
				for (String address : splitAddresses(to)) {
					message.addRecipient(Message.RecipientType.TO, new InternetAddress(address));
				}
			}
			if (bcc != null && !bcc.trim().isEmpty()) {
				for (String address : splitAddresses(bcc)) {
					message.addRecipient(Message.RecipientType.BCC, new InternetAddress(address));
				}
			}
			message.setSubject(subject);
			message.setContent(body, EMAIL_TYPE);
			Transport.send(message);

			resp.setStatus(HttpServletResponse.SC_OK);
			resp.getWriter().println("Message sent");
		} catch (MessagingException e) {
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			resp.getWriter().println("Message not sent " + e);
		}
	}

	private String[] splitAddresses(String addresses) {
		return addresses.contains(";") ? addresses.split(";") : addresses.split(",");
	}
}
