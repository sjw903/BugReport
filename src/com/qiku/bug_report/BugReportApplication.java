package com.qiku.bug_report;

import java.io.IOException;

import android.util.Log;
import android.text.TextUtils;
import android.telephony.TelephonyManager;

import com.qiku.bug_report.model.UserSettings;

public class BugReportApplication extends android.app.Application {
    private static final String TAG = "BugReportApplication";
    TaskMaster taskMaskter = null;

    public BugReportApplication() {
        super();
        Log.i(TAG, "BugReport Application is initialized");
    }

    public void onCreate(){
        super.onCreate();
        fixPermissions();

        //Initialize the taskMaster in the main thread
        UserSettings settings = getTaskMaster().getConfigurationManager().getUserSettings();

        // set default contact info first time.
        if (!settings.isContactInfoComplete()) {
            settings.setCoreID("unknown");
            settings.setEmail("unknown");
            settings.setFirstName("unknown");
            TelephonyManager mTm = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
            String num = mTm.getLine1Number();
            if(TextUtils.isEmpty(num)) {
                settings.setPhone("unknown");
            } else {
                settings.setPhone(num);
            }
            getTaskMaster().getConfigurationManager().saveUserSettings(settings);
        }
        if(TextUtils.isEmpty(settings.getServerAddr())) {
            settings.setServerAddr("http://192.168.69.64:8080/PanicReport");
            getTaskMaster().getConfigurationManager().saveUserSettings(settings);
        }

        // init server address.
        Constants.BUGREPORT_SERVER = settings.getServerAddr();
        Log.d(TAG, "Constants.BUGREPORT_SERVER=" + Constants.BUGREPORT_SERVER);
        Constants.BUGREPORT_URL_LOGON         = Constants.BUGREPORT_SERVER+"/AuthService?";
        Constants.BUGREPORT_URL_LOGOFF        = Constants.BUGREPORT_SERVER+"/cloud-api/login/clientLogout?";
        Constants.BUGREPORT_URL_FILELIST      = Constants.BUGREPORT_SERVER+"/FileListService?";
        Constants.BUGREPORT_URL_FILEUPLOAD    = Constants.BUGREPORT_SERVER+"/FileListService?"; // GetSalUrl
        Constants.BUGREPORT_URL_FOLDERCREATE  = Constants.BUGREPORT_SERVER+"/cloud-api/folder/create?";
        Constants.BUGREPORT_URL_GETUPLOADID    = Constants.BUGREPORT_SERVER+"/cloud-api/files/getuploadid?";
        Constants.BUGREPORT_URL_UPLOAD  = Constants.BUGREPORT_SERVER+"/cloud-api/files/upload?";
        Constants.BUGREPORT_URL_RESUME  = Constants.BUGREPORT_SERVER+"/cloud-api/files/resume?";
    }

    private void fixPermissions() {
        String rootStoragePath = getFilesDir().getParent();
        String cmd = "chmod "+Integer.toOctalString(0700)+" "+rootStoragePath;
        try {
            Process chmodProc = Runtime.getRuntime().exec(cmd);
            chmodProc.waitFor();
        } catch (IOException e) {
            Log.e(TAG, cmd+" failed", e);
        } catch (InterruptedException e) {
            Log.e(TAG, cmd+" interrupted", e);
        }
    }

    public TaskMaster getTaskMaster() {
        if (taskMaskter == null) {
            taskMaskter = new TaskMaster(this);
        }
        return taskMaskter;
    }
}
