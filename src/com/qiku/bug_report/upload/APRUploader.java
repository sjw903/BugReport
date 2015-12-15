package com.qiku.bug_report.upload;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class APRUploader extends Service {
	public static final String tag = "APRUploader";
	private APRUploadWorker mUploadWorker;
	String filePath = null;

	public void onCreate() {
		Log.i(tag, "onCreate()");
		super.onCreate();
		mUploadWorker = new APRUploadWorker(this);
		// mUploaderReceiver.start();
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(tag, "onStartCommand()");
		if (intent != null)
			filePath = intent.getStringExtra("filePath");
		startNextUpload();
		return START_STICKY;
	}

	private synchronized void startNextUpload() {
		Log.d(tag, "startNextUpload");
		if (mUploadWorker.isBusy()) {
			Log.d(tag, "The uploader worker is busy, please wait ... ");
			return;
		}
		if (filePath == null) {
			Log.i(tag, "Can't upload APR this time");
			return;
		} else {
			mUploadWorker.startUpload();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public void onDestroy() {
		Log.d(tag, "Reliable Upload onDestroy().");
		stopSelf();
		super.onDestroy();
	}

}
