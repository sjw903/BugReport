package com.tronxyz.bug_report.ui;

import static com.tronxyz.bug_report.helper.Util.formatFileSize;

import java.io.File;
import java.text.NumberFormat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.tronxyz.bug_report.R;
import com.tronxyz.bug_report.Constants;
import com.tronxyz.bug_report.model.ComplainReport;
import com.tronxyz.bug_report.model.ComplainReport.State;

public class ProgressLayout extends LinearLayout {
    private final static String TAG = "BugReportProgressLayout";
    private ComplainReport mUploadReport;
    private boolean mRegistered;
    private ProgressBar mProgressBar;
    private TextView mStatusView;
    private TextView mPercentageView;
    private int mFileLength;

    BroadcastReceiver mStatusUpdateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive " +  intent.getAction());
            ComplainReport report = intent.getParcelableExtra(Constants.BUGREPORT_INTENT_EXTRA_REPORT);
            synchronized(this){
                if(mUploadReport.equals(report)){
                    mUploadReport = report;
                    updateStatus(mUploadReport.getUploadedBytes());
                }
            }
        }
    };

    public ProgressLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setData(ComplainReport report){
        this.mUploadReport = report;
        File file = new File(mUploadReport.getLogPath());
        mFileLength = (int)file.length();
        updateStatus(mUploadReport.getUploadedBytes());
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        if( mUploadReport != null ){
            mRegistered = true;
            mProgressBar = (ProgressBar)findViewById(R.id.progress_bar);
            mStatusView = (TextView)findViewById(R.id.status_text);
            mPercentageView = (TextView)findViewById(R.id.percentage_text);
            updateStatus(mUploadReport.getUploadedBytes());
            IntentFilter filter = new IntentFilter();
            filter.addAction(Constants.BUGREPORT_INTENT_REPORT_UPDATED);
            filter.addAction(Constants.BUGREPORT_INTENT_REPORT_REMOVED);
            getContext().registerReceiver(mStatusUpdateReceiver, filter);
        }
    }

    private void updateStatus(int progress){
        if(mProgressBar == null)
            return;
        if(progress <= 0){
            mProgressBar.setIndeterminate(false);
            mProgressBar.setMax(100);
            mProgressBar.setProgress(0);
            Log.d("test", "progress <= 0    mUploadReport.getState()-->"+mUploadReport.getState());
            mStatusView.setText(State.categoryToString(getContext(), mUploadReport.getState()));
            mPercentageView.setText(null);
        }else{
            mProgressBar.setIndeterminate(false);
            File file = new File(mUploadReport.getLogPath());
            long length = file.length();
            mProgressBar.setMax((int)length);
            mProgressBar.setProgress(progress);
            Log.d("test", "progress > 0     mUploadReport.getState()-->"+mUploadReport.getState());
            mStatusView.setText(State.categoryToString(getContext(), mUploadReport.getState()));
            String percentage = formatFileSize(progress) + "/" + formatFileSize(mFileLength)+"  ";
            percentage += NumberFormat.getPercentInstance().format((double) progress/length);
            mPercentageView.setText(percentage);
        }
    }

    public void onDetachedFromWindow() {
        if(mRegistered){
            mRegistered = false;
            getContext().unregisterReceiver(mStatusUpdateReceiver);
            mProgressBar = null;
            mStatusView = null;
        }
        super.onDetachedFromWindow();
    }
}
