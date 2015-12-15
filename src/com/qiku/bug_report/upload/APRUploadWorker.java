package com.qiku.bug_report.upload;

import java.io.File;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

import com.qiku.bug_report.Constants;
import com.qiku.bug_report.helper.Util;
import com.qiku.bug_report.http.upload.GUS;
import com.qiku.bug_report.http.upload.GusData;
import com.qiku.bug_report.http.upload.GusJob;
import com.qiku.bug_report.http.upload.IGusCallback;
import com.qiku.bug_report.http.upload.GUS.ReturnCode;

/**
 * This is the actual report uploader that will upload file to the BugReport
 * serve. It can upload only one file at a time.
 * 
 * @author gkp374
 * 
 */
public class APRUploadWorker implements IGusCallback {
	private static final String tag = "APRUploadWorker";
	private static final int PROGRESS_UPLOAD_SIZE = 20480;
	private GUS mGus;
	private APRUploader mUploader;
	// private TaskMaster mTaskMaster;
	private PowerManager.WakeLock mWakeLock;

	enum Result {
		INVALID, CANCELLED, FAILED, SUCCESSFUL;
	}

	enum CompressSubstate {
		PREPARE, COMPRESS, CHANGE_STATE, REMOVE_FILES, CHANGE_LOGPATH, START_TO_UPLOAD
	}

	public APRUploadWorker(APRUploader uploader) {
		mUploader = uploader;
		mGus = new GUS(uploader, Constants.BUGREPORT_URL_GETUPLOADID,
				Constants.BUGREPORT_URL_UPLOAD, Constants.BUGREPORT_URL_RESUME,
				Constants.BUGREPORT_URL_LOGON, Constants.BUGREPORT_URL_LOGOFF,
				Constants.BUGREPORT_URL_FILELIST,
				Constants.BUGREPORT_URL_FILEUPLOAD,
				Constants.BUGREPORT_URL_FOLDERCREATE);
		mWakeLock = ((PowerManager) uploader
				.getSystemService(Context.POWER_SERVICE)).newWakeLock(
				PowerManager.PARTIAL_WAKE_LOCK, tag);
		// mTaskMaster = ((BugReportApplication)
		// uploader.getApplicationContext())
		// .getTaskMaster();
	}

	public void startUpload() {
             Log.d(tag, "startUpload ....");
		if (isBusy()) {
			throw new IllegalStateException(
					"UploadWorker can only upload one at a time.");
		}

		new Thread() {
			public void run() {
				startUploadJob();
			}
		}.start();
	}

	@SuppressLint("SdCardPath")
	private void startUploadJob() {
	    Log.d(tag, "startUploadJob ....");
	    for (File file : mUploader.getFilesDir().listFiles()) {
	        if (file.exists() && file.getName().contains(android.os.Build.MODEL)) {
	            int progressSize = Math.max((int) file.length() / 100,
	                    PROGRESS_UPLOAD_SIZE);
	            GusData uploadData = new GusData(file.getAbsolutePath(),
	                    Constants.BUGREPORT_INTENT_UPLOAD_PROGRESS, null, progressSize);
	            GusJob job = new GusJob(uploadData, this);
	            mGus.start(job, null);
	        }
        }
	}

	/**
	 * This is a call back method called when the GUS login the server before
	 * actual uploading
	 */
	public void onLogonReturned(GusJob job, String session) {

	}

	/**
	 * This is a call back method called when the upload fails or completes
	 */
	public void done(GusJob job, ReturnCode code) {

		try {
			if (job.mUploadId != null) {
			}
			switch (code) {

			case SUCCESS: // The log file has been successfully uploaded, send
				// request to create a report record on the server
//			    String[] segs =job.mUploadData.mFilePath.split(File.separator);
//			    String uploadSuccessFileName = segs[segs.length - 1];
			    if (!job.mUploadData.mFilePath.equals(mUploader.filePath)) {
			        boolean flag = Util.removeFile(job.mUploadData.mFilePath);
	                Log.d(tag, job.mUploadData.mFilePath + "has been deleted " + flag);
			    }
				break;
			case UPLOAD_DELETED: // the upload job stops because user removed it

				break;
			case UPLOAD_PAUSED:

				break;
			case BATTERY_LOW:
			case NETWORK_DISCONNECTED:
			case UNAUTHORIZED:
			case BAD_REQUEST:
			case SERVER_ERROR:
			default:
			    Log.d(tag, "APRUploadWorker done() process as SERVER_ERROR");
				break;
			}
		} catch (Exception e) {
			Log.e(tag, "Error occured while processing response from Server", e);

		} finally {
			Log.i("APRUploader: ", "finished");
		}
	}

	public synchronized void cancel(ReturnCode reason) {

	}

	public boolean isBusy() {
		return mWakeLock.isHeld();
	}
}
