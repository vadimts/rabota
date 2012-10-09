package eu.tsvetkov.rabota.intent;

import android.app.IntentService;
import android.content.Intent;
import eu.tsvetkov.rabota.activity.StartActivity.InvoiceSubmissionResultReceiver;
import eu.tsvetkov.rabota.gae.RabotaGaeConnector;
import eu.tsvetkov.rabota.gae.exception.RabotaGaeConnectionException;

public class RabotaGaeConnectionService extends IntentService {

	public RabotaGaeConnectionService() {
		super("RabotaGaeConnectionService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		// Submit invoice data.
		String to = intent.getStringExtra(RabotaGaeConnector.PARAM_EMAIL_TO);
		String bcc = intent.getStringExtra(RabotaGaeConnector.PARAM_EMAIL_BCC);
		String subject = intent.getStringExtra(RabotaGaeConnector.PARAM_EMAIL_SUBJECT);
		String body = intent.getStringExtra(RabotaGaeConnector.PARAM_EMAIL_BODY);

		Intent result;
		try {
			RabotaGaeConnector.submitInvoice(to, bcc, subject, body);
			result = new Intent(InvoiceSubmissionResultReceiver.ACTION_INVOICE_SUBMITTED);
		} catch (RabotaGaeConnectionException e) {
			result = new Intent(InvoiceSubmissionResultReceiver.ACTION_INVOICE_SUBMISSION_ERROR);
			result.putExtra(InvoiceSubmissionResultReceiver.ERROR_MESSAGE, e.getMessage());
		}

		// Broadcast result.
		sendBroadcast(result);
	}
}
