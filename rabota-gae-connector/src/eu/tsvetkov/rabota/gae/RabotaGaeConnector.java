package eu.tsvetkov.rabota.gae;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import eu.tsvetkov.rabota.gae.exception.RabotaGaeConnectionException;

public class RabotaGaeConnector {

	public static final String PARAM_EMAIL_BODY = "body";
	public static final String PARAM_EMAIL_SUBJECT = "subject";
	public static final String PARAM_EMAIL_TO = "to";
	public static final String PARAM_EMAIL_BCC = "bcc";
	private static final String URL_RABOTA_GAE = "http://rabota-gae.appspot.com/rabota";

	public static void submitInvoice(String to, String bcc, String subject, String body) throws RabotaGaeConnectionException {
		HttpClient httpClient = new DefaultHttpClient();
		HttpPost post = new HttpPost(URL_RABOTA_GAE);

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(RabotaGaeConnector.PARAM_EMAIL_TO, to));
		params.add(new BasicNameValuePair(RabotaGaeConnector.PARAM_EMAIL_BCC, bcc));
		params.add(new BasicNameValuePair(RabotaGaeConnector.PARAM_EMAIL_SUBJECT, subject));
		params.add(new BasicNameValuePair(RabotaGaeConnector.PARAM_EMAIL_BODY, body));

		try {
			post.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
			httpClient.execute(post);
		} catch (IOException e) {
			throw new RabotaGaeConnectionException(String.format("Failed to post '%s' to rabota-gae:\n%s", subject, e.getMessage()));
		}
	}
}
