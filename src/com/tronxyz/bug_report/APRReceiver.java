package com.tronxyz.bug_report;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class APRReceiver extends BroadcastReceiver {

	public void onReceive(Context paramContext, Intent paramIntent) {
		String action = paramIntent.getAction();
		Log.d("APRReceiver", "system boot completed and received-->"+action);
		if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
			Intent serviceLauncher = new Intent(paramContext, APRHelper.class);
			serviceLauncher.setAction(Intent.ACTION_BOOT_COMPLETED);
			paramContext.startService(serviceLauncher);
			Log.i("APRHelper: ", "APRHelper loaded at start");
		}
	}
}
