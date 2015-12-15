
package com.tronxyz.bug_report.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tronxyz.bug_report.BugReportApplication;
import com.tronxyz.bug_report.Constants;
import com.tronxyz.bug_report.R;
import com.tronxyz.bug_report.TaskMaster;
import com.tronxyz.bug_report.helper.DialogHelper;
import com.tronxyz.bug_report.helper.Util;
import com.tronxyz.bug_report.model.ComplainReport;

public class ReportViewer extends Activity {

    private LinearLayout mTagSection;
    private TextView mSummaryTextView;
    private TextView mTypeTextView;
    private TextView mTagTextView;
    private TextView mDateTextView;
    private TextView mStatusTextView;
    private TextView mLogPathTextView;
    private ExpandableLinearLayout mAttachmentsView;
    private TextView mDescriptionTextView;
    private String getId = "reportid";
    private String idStr = null;

    private ComplainReport mReport;
    private TaskMaster mTaskMaster;

    private BroadcastReceiver reportStateChangedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.i("ReportViewer", "received");
            ComplainReport report;
            report = intent
                    .getParcelableExtra(Constants.BUGREPORT_INTENT_EXTRA_REPORT);
            if (report != null && report.equals(mReport))
                updateUI(report);
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.report_viewer);
        mTaskMaster = ((BugReportApplication) getApplicationContext()).getTaskMaster();

        mTagSection = (LinearLayout) findViewById(R.id.reportTagSection);
        mSummaryTextView = (TextView) findViewById(R.id.reportSummary);
        mTypeTextView = (TextView) findViewById(R.id.reportType);
        mTagTextView = (TextView) findViewById(R.id.reportTag);
        mDateTextView = (TextView) findViewById(R.id.reportDate);
        mStatusTextView = (TextView) findViewById(R.id.reportStatus);
        mLogPathTextView = (TextView) findViewById(R.id.reportLogPath);
        mAttachmentsView = (ExpandableLinearLayout) findViewById(R.id.reportAttachmentsList);
        mDescriptionTextView = (TextView) findViewById(R.id.reportDescriptionContent);
        IntentFilter intentfilter = new IntentFilter(
                Constants.BUGREPORT_INTENT_REPORT_UPDATED);
        registerReceiver(reportStateChangedReceiver, intentfilter);
        Intent intent = getIntent();
        ComplainReport report = null;
        idStr = intent.getStringExtra(getId);
        if (idStr != null) {
            try {
                long id = Long.parseLong(idStr);
                report = mTaskMaster.getBugReportDAO().getReportById(id);
            } catch (NumberFormatException e) {
                finish();
            }
        } else {
            report = intent
                    .getParcelableExtra(Constants.BUGREPORT_INTENT_EXTRA_REPORT);
        }
        if (report != null) {
            updateUI(report);
        } else {
            finish();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        ComplainReport report = null;
        idStr = intent.getStringExtra(getId);
        if (idStr != null) {
            try {
                Log.i("onNewIntent", idStr);
                long id = Long.parseLong(idStr);
                report = mTaskMaster.getBugReportDAO().getReportById(id);
            } catch (NumberFormatException e) {
                finish();
            }
        } else {
            report = intent
                    .getParcelableExtra(Constants.BUGREPORT_INTENT_EXTRA_REPORT);
        }
        if (report != null) {
            updateUI(report);
        } else {
            finish();
        }
    }

    private void updateUI(ComplainReport report) {
        mReport = report;

        mDateTextView
                .setText(Util.formatDate("MMM dd, yyyy hh:mm:ss a z", mReport.getCreateTime()));
        mStatusTextView.setText(ComplainReport.State.stateToString(this, mReport.getState()));

        mLogPathTextView.setText(mReport.getLogPath());

        if (ComplainReport.Type.AUTO == mReport.getType()) {
            setTitle(mReport.getCategory()); // The category of the dropbox auto
                                             // reports is the scenario name in
                                             // DEAM
            mSummaryTextView.setText(mReport.getCategory());
            mTypeTextView.setText(ComplainReport.Type.typeToString(this, mReport.getType()));
            mTagTextView.setText(mReport.getSummary()); // The summary of the
                                                        // dropbox auto reports
                                                        // is the tag name of
                                                        // the dropbox event
            mDescriptionTextView.setText(mReport.getFreeText());
        } else {
            if (TextUtils.isEmpty(mReport.getTitle()))
                setTitle(getString(R.string.report_list_item_no_title));
            else
                setTitle(mReport.getTitle());
            mSummaryTextView.setText(mReport.getSummary());
            mTagSection.setVisibility(View.GONE);
            mTypeTextView.setText(ComplainReport.Type.typeToString(this, mReport.getType()));
            mDescriptionTextView.setText(mReport.getFreeText());
        }

        mAttachmentsView.setData(mReport.getAttachments());
        mAttachmentsView.setTitle(getString(R.string.report_attachments));

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.report_viewer_actions, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.report_viewer_action_delete: {
                DialogFragment newFragment = AlertDialogFragment
                        .newInstance(AlertDialogFragment.DLG_DISCARD_CONFIRMATION);
                newFragment.show(getFragmentManager(), "dialog");
                return true;
            }
            case R.id.report_viewer_settings: {
                Intent intent = new Intent(this, ReportSettingActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.report_viewer_help:
                startActivity(new Intent(this, Help.class));
                return true;
            case R.id.report_viewer_about: {
                DialogFragment newFragment = AlertDialogFragment
                        .newInstance(AlertDialogFragment.DLG_ABOUT);
                newFragment.show(getFragmentManager(), "dialog");
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void deleteReport() {
        mTaskMaster.getBugReportDAO().deleteReport(mReport);
        finish();
    }

    public void onDestroy() {
        unregisterReceiver(reportStateChangedReceiver);
        super.onDestroy();
    }

    public static class AlertDialogFragment extends DialogFragment {
        public static final String DLG_ID = "ID";
        public static final int DLG_DISCARD_CONFIRMATION = 1;
        public static final int DLG_ABOUT = 2;

        public static AlertDialogFragment newInstance(int id) {
            AlertDialogFragment frag = new AlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt(DLG_ID, id);
            frag.setArguments(args);
            return frag;
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt(DLG_ID);
            switch (id) {
                case DLG_DISCARD_CONFIRMATION:
                    return new AlertDialog.Builder(getActivity())
                            .setIcon(R.drawable.alert_dialog_icon)
                            .setTitle(R.string.alert_dialog_delete_report)
                            .setPositiveButton(R.string.yes,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            ((ReportViewer) getActivity()).deleteReport();
                                        }
                                    }
                            )
                            .setNegativeButton(R.string.no, null)
                            .create();

                case DLG_ABOUT:
                    return DialogHelper.createAboutDialog(getActivity());
            }
            return null;
        }
    }
}
