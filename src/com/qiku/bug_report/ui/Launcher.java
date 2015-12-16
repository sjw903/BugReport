
package com.qiku.bug_report.ui;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.provider.Settings.System;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.qiku.bug_report.BugReportApplication;
import com.qiku.bug_report.BugReportSettingsActivity;
import com.qiku.bug_report.Constants;
import com.qiku.bug_report.R;
import com.qiku.bug_report.TaskMaster;
import com.qiku.bug_report.helper.Util;
import com.qiku.bug_report.model.ComplainReport;
import com.qiku.bug_report.newuiservice.BackUploadService;
import com.qiku.bug_report.newuiservice.NewReportService;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Launcher extends Activity {
    public static final String tag = "Launcher";
    protected TaskMaster mTaskMaster;

    protected NewReportService.State state = NewReportService.State.idle;
    protected Intent mStartItent;
    protected ComplainReport mCurrentReport = null;
    protected String logPath;

    public static final int STEP_ONE = 0;
    public static final int STEP_TWO = 1;
    public int currentStep = STEP_ONE;
    private FragmentManager mFragmentManager;
    private FragmentTransaction mFragmentTransaction;
    private ReportProblemDescriptionFragment mFragmentOne;
    public String mDescriptionText;
    public ScreenShotsAdapter mScreenShotsAdapter;

    protected List<ComplainReport> mReports;
    public String mPhoneText;
    public String mEmailText;
    private static final String stop = "ctl.stop";

    private BroadcastReceiver mBugreportServiceReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.i(tag, "Launcher received broadcast from NewReportService");
            onNewIntent(intent);
        }
    };

    private BroadcastReceiver mInputMethodReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            stopBugReportService();
            stopNewReportService();
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IntentFilter bugreportFilter = new IntentFilter(
                Constants.BUGREPORT_INTENT_BUGREPORT_ERROR);
        bugreportFilter.addAction(Constants.BUGREPORT_INTENT_BUGREPORT_START);
        bugreportFilter.addAction(Constants.BUGREPORT_INTENT_BUGREPORT_END);
        registerReceiver(mBugreportServiceReceiver, bugreportFilter);
        registerReceiver(mInputMethodReceiver,
                new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED));

        String bugreportState = Util.getSystemProperty("init.svc."
                + Constants.BUGREPORT_SERVICE, null);
        if (NewReportService.mBugS != null && bugreportState != null
                && bugreportState.equals("running")) {
            Toast.makeText(this, "正在为您收集上次日志，请稍候", Toast.LENGTH_LONG).show();
            finish();
            return;
        } else if (NewReportService.mBugS == null && bugreportState != null
                && bugreportState.equals("running")) {
            Util.setSystemProperty(stop, Constants.BUGREPORT_SERVICE);
        }
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.launcher);
        mTaskMaster = ((BugReportApplication) getApplicationContext())
                .getTaskMaster();

        mFragmentManager = getFragmentManager();
        mFragmentTransaction = mFragmentManager.beginTransaction();
        mFragmentOne = new ReportProblemDescriptionFragment();
        mFragmentTransaction.replace(R.id.launcher_container, mFragmentOne);
        mFragmentTransaction.commit();
        cleanReport();

        // if (!Util.isUserVersion()) {
        //     Util.setSystemProperty("ctl.start", Constants.BUGREPORT_SERVICE);
        // }
        // Toast.makeText(this, R.string.toast_log_collection_started,
        // Toast.LENGTH_SHORT).show();

        // to setting page directly.
        startActivity(new Intent(this, ReportSettingActivity.class));
        finish();
    }

    private void stopBugReportService() {
        String bugreportState = Util.getSystemProperty("init.svc."
                + Constants.BUGREPORT_SERVICE, null);
        if (null != bugreportState && bugreportState.equals("running")) {
            Util.setSystemProperty(stop, Constants.BUGREPORT_SERVICE);
        }
    }

    public void onNewIntent(Intent intent) {
        String action = intent.getAction();
        if (Constants.BUGREPORT_INTENT_BUGREPORT_START
                .equalsIgnoreCase(action)) {
            onCollectorStart(intent);
        } else if (Constants.BUGREPORT_INTENT_BUGREPORT_ERROR
                .equalsIgnoreCase(action)) {
            onCollectorError(intent);
        } else if (Constants.BUGREPORT_INTENT_BUGREPORT_END
                .equalsIgnoreCase(action)) {
            onCollectorEnd(intent);
        }
    }

    private void onCollectorStart(final Intent intent) {
        Log.i(tag, "Launcher activity receive start intent");
        if (!state.equals(NewReportService.State.idle)) {
            return;
        }
        state = NewReportService.State.collecting;
        mStartItent = intent;
        // The log path below will be used if user attaches files before the log
        // collection finishes.
        // If it is unavailable, Attaching files is not allowed before the log
        // collection finishes
        logPath = mStartItent.getStringExtra(Constants.REPORT_LOG_PATH);
        Log.i(tag, "logPath ="+logPath + " reportid = "+intent.getLongExtra("reportid", 0));
        mCurrentReport = mTaskMaster.getBugReportDAO().getReportById(
                intent.getLongExtra("reportid", 0));
        Log.i(tag, mCurrentReport + "  start ");
    }

    private void onCollectorError(Intent intent) {
        Log.i(tag, "Launcher activity receive error intent");
        state = NewReportService.State.idle;
        mCurrentReport = null;
        // show error message
        String errorMsg = intent
                .getStringExtra(Constants.BUGREPORT_INTENT_PARA_ERROR_MSG);
        if (errorMsg == null) {
            String errorType = intent
                    .getStringExtra(Constants.BUGREPORT_INTENT_PARA_ERROR_TYPE);
            if (Constants.BUGREPORT_SHELL_ERROR_NOSTORAGE.equals(errorType)) {
                Toast.makeText(Launcher.this,
                        R.string.alert_dialog_sd_missing_msg, Toast.LENGTH_LONG)
                        .show();
            } else {
                Toast.makeText(Launcher.this,
                        R.string.alert_dialog_collector_failed_msg,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void onCollectorEnd(Intent intent) {
        Log.i(tag, "Launcher activity receive end intent");
        if (intent != null)
            state = NewReportService.State.idle;
        // if (mCurrentReport != null) {
        // Toast.makeText(Launcher.this,
        // R.string.toast_log_collection_completed, Toast.LENGTH_LONG)
        // .show();
        // // update status
        // // mStatusView.setText(ComplainReport.State.stateToString(this,
        // // mCurrentReport.getState()));
        // // user might already attach some files during the log collection,
        // // so we need to add those attached file path to the report object
        // // mAttachmentsView.onCollectorEnd(mCurrentReport);
        // }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.launcher_actions, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.launcher_settings: {
                Intent intent = new Intent(this, BugReportSettingsActivity.class);
                startActivity(intent);
                return true;
            }
            // chenf:remove setup wizard menu tiem
            // case R.id.launcher_setup_wizard:
            // startActivity(new Intent(this, SetupWizard.class));
            // return true;
            // case R.id.launcher_help:
            // startActivity(new Intent(this, Help.class));
            // return true;
            // case R.id.launcher_about:
            // onCreateDialog(DLG_ABOUT, null);
            // return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onExit() {
        if (mStartItent != null) {
            if (mCurrentReport != null) {
                if (NewReportService.State.collecting.equals(state)) {
                    // stop collect log service
                    Util.setSystemProperty(stop, Constants.BUGREPORT_SERVICE);
                    String mExitAction = Constants.BUGREPORT_INTENT_DISCARD_REPORT;
                    Intent mExitIntent = new Intent();
                    mExitIntent.setAction(mExitAction);
                    mExitIntent.putExtra(Constants.REPORT_LOG_PATH, mStartItent
                            .getStringExtra(Constants.REPORT_LOG_PATH));
                    mExitIntent
                            .putExtra(
                                    Constants.BUGREPORT_INTENT_PARA_ID,
                                    mStartItent
                                            .getStringExtra(Constants.BUGREPORT_INTENT_PARA_ID));
                    mExitIntent.setClass(Launcher.this, NewReportService.class);
                    startService(mExitIntent);
                    Log.i("delete", "delete ongoing report");
                } else {// directly remove the report
                    Log.i("delete", "delete finished report");
                    mTaskMaster.deleteComplainReport(mCurrentReport);
                }
            }

        } else {
            Util.setSystemProperty(stop, Constants.BUGREPORT_SERVICE);
            if (NewReportService.mBugS != null) {
                NewReportService.mBugS.onDestroy();
            }
            // Toast.makeText(this, "正在创建报告，请稍候...", Toast.LENGTH_SHORT).show();
            // return;
        }
        finish();
    }

    public void onDestroy() {
        unregisterReceiver(mBugreportServiceReceiver);
        unregisterReceiver(mInputMethodReceiver);
        Log.i(tag, "Destroy");
        super.onDestroy();
        mFragmentOne = null;
        String path = "/data/bugreport/";
        File logFile = new File(path);
        if (logFile.exists() && logFile.isDirectory()) {
            File[] subFiles = logFile.listFiles();
            for (int i = 0; subFiles != null && i < subFiles.length; i++) {
                if (subFiles[i].getName().equals("screenshots")) {
                    Util.removeFile(subFiles[i].getAbsolutePath());
                    continue;
                }
                if (subFiles[i].isDirectory() && subFiles[i].list().length == 0) {
                    subFiles[i].delete();
                }
            }
        }
    }

    /*public void showFragmentByStep(int step) {
        mFragmentTransaction = mFragmentManager.beginTransaction();
        mFragmentTransaction
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        switch (step) {

            case STEP_ONE:
                if (null != mFragmentTwo) {
                    mFragmentTransaction.setCustomAnimations(
                            R.animator.fragment_slide_right_enter,
                            R.animator.fragment_slide_right_exit);
                    mPhoneText = mFragmentTwo.getmEditTextPhoneNumber().getText()
                            .toString();
                    mEmailText = mFragmentTwo.getmEditTextEmailAdress().getText()
                            .toString();
                    mFragmentTransaction.remove(mFragmentTwo);
                } else {
                    mPhoneText = System.getString(this.getContentResolver(),
                            ReportUserInfoFragment.USER_PHONE_NUMBER);
                    mEmailText = System.getString(this.getContentResolver(),
                            ReportUserInfoFragment.USER_EMAIL_ADRESS);
                }
                mFragmentOne = new ReportProblemDescriptionFragment();
                mFragmentTransaction.replace(R.id.launcher_container, mFragmentOne);
                currentStep = STEP_ONE;
                break;

            case STEP_TWO:
                if (null != mFragmentOne) {
                    mFragmentTransaction.setCustomAnimations(
                            R.animator.fragment_slide_left_enter,
                            R.animator.fragment_slide_left_exit);
                    mDescriptionText = mFragmentOne.getmEditText().getText()
                            .toString();
                    mScreenShotsAdapter = mFragmentOne.getmScreenShotsAdapter();
                    mFragmentTransaction.remove(mFragmentOne);
                }

                if (mPhoneText == null) {
                    mPhoneText = mTaskMaster.getConfigurationManager()
                            .getUserSettings().getPhone();
                }
                if (mEmailText == null) {
                    mEmailText = mTaskMaster.getConfigurationManager()
                            .getUserSettings().getEmail();
                }
                mFragmentTwo = new ReportUserInfoFragment();
                mFragmentTransaction.replace(R.id.launcher_container, mFragmentTwo);
                currentStep = STEP_TWO;
                break;
        }
        mFragmentTransaction.commit();
    }*/

    public void click(View view) {
        switch (view.getId()) {
            case R.id.ib_setting:
                Intent intent = new Intent(this, ReportSettingActivity.class);
                startActivity(intent);
                break;

            default:
                break;
        }
    }

    private void cleanReport() {
        mReports = mTaskMaster.getAllReports();
        List<ComplainReport> delList = new ArrayList<ComplainReport>();
        for (ComplainReport report : mReports) {
            if (report.getState().equals(ComplainReport.State.BUILDING)
                    || report.getState().equals(
                            ComplainReport.State.WAIT_USER_INPUT)) {
                delList.add(report);
                mTaskMaster.deleteComplainReport(report);
                continue;
            }
            long createTime = report.getCreateTime().getTime();
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_MONTH, -3);
            long timeMillis = c.getTimeInMillis();
            if (timeMillis >= createTime) {
                delList.add(report);
                mTaskMaster.deleteComplainReport(report);
            }
        }
        mReports.removeAll(delList);
    }

    protected void sendReport() {

        // if (Util.isUserVersion()) {
        //     Util.setSystemProperty("ctl.start", Constants.BUGREPORT_SERVICE);
        // }

        if (mCurrentReport != null && state.equals(NewReportService.State.idle)) {
            new Thread() {
                public void run() {
                    Message msg = Message.obtain();
                    saveUserChanges();
                    msg.what = TaskMaster.BUG_REPORT_SEND_LOG;
                    msg.obj = mCurrentReport;
                    mTaskMaster.sendMessage(msg);
                }
            }.start();
            Toast.makeText(Launcher.this, "正在努力为您提交报告", Toast.LENGTH_LONG)
                    .show();
        } else if (mCurrentReport != null
                && state.equals(NewReportService.State.collecting)) {
            new Thread() {
                public void run() {
                    saveUserChanges();
                    Intent backIntent = new Intent();
                    backIntent.putExtra(
                            Constants.BUGREPORT_INTENT_EXTRA_REPORT,
                            mCurrentReport);
                    backIntent.setClass(Launcher.this, BackUploadService.class);
                    startService(backIntent);
                }
            }.start();
            Toast.makeText(Launcher.this, "正在努力为您收集日志,稍后为您提交", Toast.LENGTH_LONG)
                    .show();
        } else if (mCurrentReport == null
                && state.equals(NewReportService.State.idle)) {
            Toast.makeText(Launcher.this, R.string.alert_dialog_sd_missing_msg,
                    Toast.LENGTH_LONG).show();
        }
        finish();
    }

    protected void saveUserChanges() {
        if (null != mFragmentOne) {
            if (null != mFragmentOne.getmScreenShotsAdapter()) {
                mScreenShotsAdapter = mFragmentOne.getmScreenShotsAdapter();
            }
            mDescriptionText = mFragmentOne.getmEditText().getText().toString();
        }
        if (!mScreenShotsAdapter.getFileList().isEmpty() && logPath != null) {
            int size = mScreenShotsAdapter.getFileList().size();
            for (int i = 0; i < size; i++) {
                String path = mScreenShotsAdapter.getShotsFile(i);
                Util.compressPicture(path, logPath);
            }
        }
        mCurrentReport.setSummary("意见反馈");
        mCurrentReport.setFreeText(mDescriptionText);
    }

    private void stopNewReportService() {
        if (NewReportService.mBugS != null && NewReportService.State.collecting.equals(state)) {
            stopService(new Intent(this, NewReportService.mBugS.getClass()));
        }
    }
}
