package com.qiku.bug_report;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.DropBoxManager;
import android.util.Log;

import com.qiku.bug_report.conf.bean.Deam;
import com.qiku.bug_report.log.DropBoxEventHandler;
import com.qiku.bug_report.model.UserSettings;

public class BugReportBroadcastReceiver extends BroadcastReceiver {
	final static String tag = "BugReportBroadcastReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Log.d(tag, "Received broadcast : " + action);
		if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
			Log.d(tag, "Start service:" + action);
			sendMsgToMaster(context, TaskMaster.BUG_REPORT_DEVICE_START);
		} else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
			sendMsgToMaster(context,
					TaskMaster.BUG_REPORT_NETWORK_AVAILABLE);
		} else if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
			sendMsgToMaster(context,
					TaskMaster.BUG_REPORT_POWER_CONNECTED);
		} else if (Constants.BUGREPORT_INTENT_BATTERY_THRESHOLD_CHANGED
				.equals(action)) {
			sendMsgToMaster(context,
					TaskMaster.BUG_REPORT_BATTERY_THRESHOLD_CHANGED);
		} else if (DropBoxManager.ACTION_DROPBOX_ENTRY_ADDED.equals(action)) {
			String tagName = intent.getStringExtra(DropBoxManager.EXTRA_TAG);
			Log.d(tag, "Dropbox event : " + tagName);
			TaskMaster taskMaster = ((BugReportApplication) context
					.getApplicationContext()).getTaskMaster();
			UserSettings userSetting = taskMaster.getConfigurationManager()
					.getUserSettings();
			if (userSetting != null
					&& !userSetting.isAutoReportEnabled().getValue())
				return;
			Deam deam = taskMaster.getConfigurationManager()
					.getDeamConfiguration().get();
			if (deam == null) {
				return;
			}
			if (deam.hasTag(tagName, Deam.Tag.Type.DROPBOX)) {
				intent.setClass(context, DropBoxEventHandler.class);
				context.startService(intent);
			}
		}
	}

	private void sendMsgToMaster(Context context, int msgId) {
		TaskMaster taskMaster = ((BugReportApplication) context
				.getApplicationContext()).getTaskMaster();
		taskMaster.sendEmptyMessage(msgId);
	}
}
