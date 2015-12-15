
package com.tronxyz.bug_report;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import com.tronxyz.bug_report.upload.APRUploader;
import com.tronxyz.bug_report.helper.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Date;
import java.text.SimpleDateFormat;

@SuppressLint({
        "SdCardPath", "SimpleDateFormat"
})
public class APRHelper extends Service {
    private AlarmManager am;
    private PendingIntent mAlarmSender;
    protected static final String tag = "APRHelper";
    public static final String CURRENT_FILE_NAME = "current_file_name";
    public static final String CURRENT_FILE_PATH = "current_file_path";
    private int count = 1;
    private String mCurrentFileName;
    private String mFilesDirPath;
    // private String mPath;
    public String mCurrentFilePath;
    WakeLock mWakeLock = null;
    private boolean AutoReportEnabled;
    private boolean isMobileUploadAllowed;
    private TaskMaster mTaskMaster;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(tag, "start service");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mFilesDirPath = getFilesDir().toString();
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(Intent.ACTION_SHUTDOWN);
        mFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
        registerReceiver(mReceiver, mFilter);
        mTaskMaster = ((BugReportApplication) getApplicationContext()).getTaskMaster();
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(tag, "current intent action is "+intent.getAction());
            start();
        } else { //this may be called by alarm or real exception(killed by system)
            if (null != intent) {
                Log.d(tag, "intent is not contains BOOT_COMPLETED, current intent action is "+intent.getAction());
            }
            startAfterException();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SHUTDOWN.equals(action)) {
                try {
                    if (Float.parseFloat(Util.getTime()) >= 0.05 && mFilesDirPath != null) {
                        Log.i(tag, "shutdown then update file");
                        updateFile();
                    } else {
                        Log.i(tag, "power on time is too short and ignore it");
                    }
                } catch (NumberFormatException e) {
                    Log.i(tag, "ignore this time");
                } catch (IOException e) {
                    Log.i(tag, "ignore this time");
                }

            }
        }
    };

    public void start() {
        try {
            if (count == 1) {
                setCurrentAlarm();
                creatFile();
                count = count + 1;
            }
            updateFile();
            uploadAPR();
        } catch (FileNotFoundException e) {
            return;
        } catch (IOException e) {
            return;
        }
    }

    public void startAfterException() {
        try {
            if (isExists() && Float.parseFloat(Util.getTime()) >= 0.05) {
                Log.i("Exception:", "error intent is null or other");
                count = 2;
                start();
            } else {
                count = 1;
                Log.i("Exception:", "Short time or file not exists");
                start();
            }
        } catch (NumberFormatException e) {
            count = 2;
            start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void uploadAPR() {
        new Thread() {
            public void run() {
                upload();
            }
        }.start();
    }

    private void upload() {
        if (null == mCurrentFilePath) {
            mCurrentFilePath = android.provider.Settings.System.getString(this.getContentResolver(), CURRENT_FILE_PATH);
            if (null == mCurrentFilePath) {
                Log.d(tag, "mCurrentFilePath is still null, from contentresolver is null");
                return;
            }
        }
        File uploadFile = new File(mCurrentFilePath);
        isMobileUploadAllowed = !mTaskMaster.getConfigurationManager().getUserSettings()
                .getWlanUploadAllowed().getValue();
        AutoReportEnabled = mTaskMaster.getConfigurationManager().getUserSettings()
                .isAutoReportEnabled().getValue();
        if (!uploadFile.exists() || uploadFile.length() == 0) {
            PrintWriter out;
            try {
                uploadFile.createNewFile();
                out = new PrintWriter(new FileOutputStream(uploadFile, false));
                Log.i(tag, "update file");
                out.write(Util.getTime() + "*"+ Util.getUpTime());
                out.close();
                checkSettingsAndUpload();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            checkSettingsAndUpload();
        }
    }

    public void checkSettingsAndUpload() {
        if (AutoReportEnabled) {
            /*if (!isMobileUploadAllowed && Util.isWIFIConnected(this)) {
                Intent intent = new Intent(this, APRUploader.class);
                intent.putExtra("filePath", filePath);
                startService(intent);
            } else if (isMobileUploadAllowed && Util.isNetworkAvailable(this)) {
                Intent intent = new Intent(this, APRUploader.class);
                intent.putExtra("filePath", filePath);
                startService(intent);
            }*/
            Intent intent = new Intent(this, APRUploader.class);
            intent.putExtra("filePath", mCurrentFilePath);
            startService(intent);
        }
    }

    @Override
    public void onDestroy() {
        // mthread.interrupt();
        // releaseWakeLock();
        if (am != null) {
            am.cancel(mAlarmSender);
            Log.i(tag, "cancel alarm");
        }
        unregisterReceiver(mReceiver);
        stopSelf();
        super.onDestroy();

    }
/*
    public String getTime() throws IOException {
        float totalttime = SystemClock.elapsedRealtime();
        BigDecimal b = new BigDecimal(totalttime / (1000 * 60 * 60));
        float time1 = b.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
        return String.valueOf(time1);
    }

    public String getTimeNow(String style) {
        SimpleDateFormat formatter = new SimpleDateFormat(style);
        Date curDate = new Date(System.currentTimeMillis());// 获取当前时间
        String currenttime = formatter.format(curDate);
        return currenttime;
    }

    // 版本
    private String getVersion() {
        Class<?> systemPropertiesClass = null;
        try {
            systemPropertiesClass = Class
                    .forName("android.os.SystemProperties");
            Method get = systemPropertiesClass.getMethod("get", String.class);
            String version = (String) get.invoke(systemPropertiesClass,
                    "ro.build.display.id");
            if (TextUtils.isEmpty(version))
                return "unknown";
            else
                return version;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return "unknow";
    }

    // 型号
    private String getModel() {
        String model = android.os.Build.MODEL;
        return model;
    }
*/
    private String getIMEI() {
        TelephonyManager mTm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        String IMEI = mTm.getDeviceId();
        return IMEI;
    }

    private void creatFile() throws IOException {
        // path表示你所创建文件的路径
        File f = new File(mFilesDirPath);
        if (!f.exists() || f.isFile()) {
            f.mkdirs();
        }
        // fileName表示你创建的文件名；为txt类型；

//        mCurrentFileName = Util.getModel() + "_" + Util.getVersion() + "_" + getIMEI() + "@"
//                + Util.getTimeNow("yyyy-MM-dd_HH-mm-ss") + ".txt";
        mCurrentFileName = Util.getModel() + "_" + Util.getVersion() + "_" + getIMEI() + ".txt";
        final File file = new File(f, mCurrentFileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
                PrintWriter out = new PrintWriter(new FileOutputStream(file, false));
                out.write(Util.getTime() + ":0.0"+ "*" + Util.getUpTime() + ":0.0");
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mCurrentFilePath = file.getAbsolutePath();
        android.provider.Settings.System.putString(this.getContentResolver(), CURRENT_FILE_NAME, mCurrentFileName);
        android.provider.Settings.System.putString(this.getContentResolver(), CURRENT_FILE_PATH, mCurrentFilePath);
    }

    private void updateFile() throws FileNotFoundException, IOException {
        File f = new File(mFilesDirPath);
        if (!f.exists() || f.isFile()) {
            f.mkdirs();
        }
        if (null == mCurrentFileName) {
            mCurrentFileName = android.provider.Settings.System.getString(this.getContentResolver(), CURRENT_FILE_NAME);
            if (null == mCurrentFileName) {
                Log.d(tag, "mCurrentFileName is still null, from contentresolver is null");
                return;
            }
        }
        File myfile = new File(f, mCurrentFileName);
        if (myfile.exists()) {
            float uptime = 0.0f;
            float deltaUptime = 0.0f;
            float elaspedtime = 0.0f;
            float deltaElaspedtime = 0.0f;
            try
            {
                BufferedReader in = new BufferedReader(new FileReader(myfile));
                String str;
                while ((str = in.readLine()) != null)
                {
                    try{
                        elaspedtime = Float.parseFloat( str.substring(0, str.indexOf(':')) );
                        deltaElaspedtime = Float.parseFloat( str.substring(str.indexOf(':') + 1,str.indexOf('*')) );
                        deltaUptime = Float.parseFloat( str.substring(str.lastIndexOf(':') + 1) );
                        uptime = Float.parseFloat( str.substring(str.indexOf('*') + 1, str.lastIndexOf(':')) );
                        Log.d(tag, elaspedtime+ ":" + deltaElaspedtime + "*" + uptime + ':' + deltaUptime) ;
                    }catch (NumberFormatException e) {
                        Log.d(tag, "error parsing sync error: " + str);
                    }
                }
                in.close();
            } catch (IOException e){
                e.getStackTrace();
            }

            PrintWriter out = new PrintWriter(new FileOutputStream(myfile, false));
            Log.i(tag, "update file");
            if((Float.parseFloat(Util.getTime()) - elaspedtime) > 0)
                deltaElaspedtime = Float.parseFloat(Util.getTime()) - elaspedtime + deltaElaspedtime;
            if((Float.parseFloat(Util.getUpTime()) - uptime) > 0)
                deltaUptime = Float.parseFloat(Util.getUpTime()) - uptime + deltaUptime;
            out.write(Util.getTime() + ":" + deltaElaspedtime +  "*"+ Util.getUpTime() + ':' + deltaUptime);
            out.close();
            Log.d(tag, "after updated is" + Util.getTime() + ":" + deltaElaspedtime +  "*"+ Util.getUpTime() + ':' + deltaUptime) ;
        } else {
            myfile.createNewFile();
            PrintWriter out = new PrintWriter( new FileOutputStream(myfile, false));
            Log.i(tag, "update file");
            out.write(Util.getTime() + ":0.0"+ "*" + Util.getUpTime() + ":0.0");
            out.close();
        }
    }

    public boolean isExists() {
        File filelist = new File(mFilesDirPath);
        if (filelist.exists() && filelist.isDirectory()) {
            File[] listfiles = filelist.listFiles();
            for (int i = 0; i < listfiles.length; i++) {
                if (listfiles[i].getName().contains(Util.getVersion())
                        && listfiles[i].getName().contains(Util.getModel())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void setCurrentAlarm() {
        Log.i(tag, "set alarm");
        mAlarmSender = PendingIntent.getService(APRHelper.this, 0, new Intent(
                APRHelper.this, APRHelper.class), 0);
        am = (AlarmManager) getSystemService(ALARM_SERVICE);
        long interval = DateUtils.MINUTE_IN_MILLIS * 2;
        long firstWake = System.currentTimeMillis() + interval;
        am.setRepeating(AlarmManager.RTC_WAKEUP, firstWake, 60 * 60 * 1000,
                mAlarmSender);
        Log.i(tag, "alarm set ok");
    }

    @Override
    public IBinder onBind(Intent paramIntent) {
        return null;
    }

}
