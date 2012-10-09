package eu.tsvetkov.rabota.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import eu.tsvetkov.rabota.R;

public class UI {

	public static void showOkCancelDialog(Context context, int messageId, final Runnable command) {
		showOkCancelDialog(context, context.getResources().getString(messageId), command);
	}

	public static void showOkCancelDialog(Context context, String message, final Runnable command) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(message).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		}).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				command.run();
				dialog.dismiss();
			}
		});
		builder.show();
	}

	public static void showOkDialog(Context context, int messageId) {
		showOkDialog(context, context.getResources().getString(messageId));
	}

	public static void showOkDialog(Context context, String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(message).setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.show();
	}
}
