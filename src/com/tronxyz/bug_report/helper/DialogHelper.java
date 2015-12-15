package com.tronxyz.bug_report.helper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.tronxyz.bug_report.R;

public class DialogHelper {

	public static AlertDialog createDialog(Context ctx, int msg, int icon,
			boolean cancelable) {
		return new AlertDialog.Builder(ctx).setMessage(msg)
				.setCancelable(cancelable).setIcon(icon)
				.setPositiveButton(android.R.string.ok, null).create();
	}

	public static AlertDialog createDialog(Context ctx, int msg, int title,
			int icon, boolean cancelable) {
		return new AlertDialog.Builder(ctx).setMessage(msg).setTitle(title)
				.setCancelable(cancelable).setIcon(icon)
				.setPositiveButton(android.R.string.ok, null).create();
	}

	public static AlertDialog createDialog(Context ctx, int msg, int title,
			int icon, int btn1, OnClickListener btn1Listener) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(ctx)
				.setMessage(msg);
		if (title > 0)
			dialog.setTitle(title);
		if (icon > 0)
			dialog.setIcon(icon);
		if (btn1 > 0)
			dialog.setPositiveButton(btn1, btn1Listener);
		return dialog.create();
	}

	public static AlertDialog createDialog(Context ctx, int msg, int title,
			int icon, int btn1, OnClickListener btn1Listener, int btn2,
			OnClickListener btn2Listener) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(ctx)
				.setMessage(msg);
		if (title > 0)
			dialog.setTitle(title);
		if (icon > 0)
			dialog.setIcon(icon);
		if (btn1 > 0)
			dialog.setPositiveButton(btn1, btn1Listener);
		if (btn2 > 0)
			dialog.setNegativeButton(btn2, btn2Listener);
		return dialog.create();
	}

	public static AlertDialog createDialog(Context ctx, int msg, int title,
			int icon, int btn1, OnClickListener btn1Listener, int btn2,
			OnClickListener btn2Listener, View customView) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(ctx)
				.setMessage(msg);
		if (customView != null)
			dialog.setView(customView);
		if (title > 0)
			dialog.setTitle(title);
		if (icon > 0)
			dialog.setIcon(icon);
		if (btn1 > 0)
			dialog.setPositiveButton(btn1, btn1Listener);
		if (btn2 > 0)
			dialog.setNegativeButton(btn2, btn2Listener);
		return dialog.create();
	}

	public static AlertDialog createDialog(Context ctx, int msg, int title,
			int icon, int btn1, OnClickListener btn1Listener, int btn2,
			OnClickListener btn2Listener, int btn3, OnClickListener btn3Listener) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(ctx)
				.setMessage(msg);
		if (title > 0)
			dialog.setTitle(title);
		if (icon > 0)
			dialog.setIcon(icon);
		if (btn1 > 0)
			dialog.setPositiveButton(btn1, btn1Listener);
		if (btn2 > 0)
			dialog.setNeutralButton(btn2, btn2Listener);
		if (btn3 > 0)
			dialog.setNegativeButton(btn3, btn3Listener);
		return dialog.create();
	}

	public static AlertDialog createAboutDialog(Context ctx) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(ctx);
		dialog.setTitle(R.string.alert_dialog_about_title);

		LayoutInflater inflater = LayoutInflater.from(ctx);
		View contentView = inflater.inflate(R.layout.about, null);
		dialog.setView(contentView);

		String version = null;
		PackageInfo pInfo;
		try {
			pInfo = ctx.getPackageManager().getPackageInfo(
					ctx.getPackageName(), 0);
			version = pInfo.versionName;
		} catch (NameNotFoundException e) {
			// ignore...
		}
		String deviceId = DeviceID.getInstance().getId(ctx);
//		String authors = ctx.getString(R.string.alert_dialog_about_author);
		String format = ctx.getString(R.string.alert_dialog_about_msg);
//		String content = String.format(format, version, deviceId, authors);
		String content = String.format(format, version, deviceId);

		TextView view = (TextView) contentView.findViewById(R.id.about_content);
		view.setMovementMethod(LinkMovementMethod.getInstance());
		view.setText(content);

		dialog.setPositiveButton(android.R.string.ok, null);
		return dialog.create();
	}
}
