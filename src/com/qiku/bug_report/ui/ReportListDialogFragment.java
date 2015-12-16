package com.qiku.bug_report.ui;

import static com.qiku.bug_report.ui.ReportListDialogFragment.OnDialogKeyPressedListener.POSITIVE;

import java.util.ArrayList;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;

import com.qiku.bug_report.BugReportApplication;
import com.qiku.bug_report.Constants;
import com.qiku.bug_report.R;
import com.qiku.bug_report.TaskMaster;
import com.qiku.bug_report.helper.DialogHelper;
import com.qiku.bug_report.model.ComplainReport;

public class ReportListDialogFragment extends DialogFragment {

	public final static String REPORTS = "reports";
	public final static String ID = "id";
	public final static int DLG_REPORT_UNCOMPLETED = 1;
	public final static int DLG_CONTACT_REQUIRED = 2;
	public final static int DLG_INVALID_LOG_FILE = 3;
	public final static int DLG_UPLOAD_CONFIRM = 4;
	public final static int DLG_DELETE_CONFIRM = 5;
	public final static int DLG_CANCEL_CONFIRM = 6;
	private TaskMaster mTaskMaster;
	private static ReportListDialogFragment mInstance;
	private OnDialogKeyPressedListener mOnDialogKeyPressedListener;

	public interface OnDialogKeyPressedListener {
		public static final int POSITIVE = 1;
		public static final int NEGAITIVE = -1;

		public void onKeyPressed(int keyCode);
	}

	public static ReportListDialogFragment getInstance(Context context) {
		if (mInstance == null)
			mInstance = new ReportListDialogFragment(context);
		return mInstance;
	}

	private ReportListDialogFragment(Context context) {
		mTaskMaster = ((BugReportApplication) context.getApplicationContext())
				.getTaskMaster();
	}

	public void showDialog(FragmentManager fragMgr, int id,
			ArrayList<ComplainReport> reports,
			OnDialogKeyPressedListener onDialogKeyPressedListener) {
		mOnDialogKeyPressedListener = onDialogKeyPressedListener;
		Bundle args = new Bundle();
		args.putInt(ID, id);
		args.putParcelableArrayList(REPORTS, reports);
		setArguments(args);
		show(fragMgr, "dialog");
	}

	public void showDialog(FragmentManager fragMgr, int id,
			ComplainReport report,
			OnDialogKeyPressedListener onDialogKeyPressedListener) {
		ArrayList<ComplainReport> selectedReports = new ArrayList<ComplainReport>();
		selectedReports.add(report);
		showDialog(fragMgr, id, selectedReports, onDialogKeyPressedListener);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		int id = getArguments().getInt(ID);
		final ArrayList<ComplainReport> reports = getArguments()
				.getParcelableArrayList(REPORTS);
		final ComplainReport report = reports.get(0);
		switch (id) {
		case DLG_REPORT_UNCOMPLETED:
			return DialogHelper.createDialog(getActivity(),
					R.string.alert_dialog_report_incompleted, 0,
					android.R.drawable.stat_sys_warning, android.R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							Intent intent = new Intent(getActivity(),
                                        Launcher.class);
							intent.setAction(Constants.BUGREPORT_INTENT_EDIT_REPORT);
							intent.putExtra(
									Constants.BUGREPORT_INTENT_EXTRA_REPORT,
									report);
							startActivity(intent);
							if (mOnDialogKeyPressedListener != null)
								mOnDialogKeyPressedListener
										.onKeyPressed(POSITIVE);
						}
					}, android.R.string.cancel, null);
		case DLG_CONTACT_REQUIRED:
			return DialogHelper.createDialog(getActivity(),
					R.string.alert_dialog_message_contactinfo,
					R.string.alert_dialog_title_contactinfo,
					android.R.drawable.ic_dialog_alert, android.R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							Intent intent = new Intent();
							intent.setClass(getActivity(), SetupWizard.class);
							startActivity(intent);
							if (mOnDialogKeyPressedListener != null)
								mOnDialogKeyPressedListener
										.onKeyPressed(POSITIVE);
						}
					});
		case DLG_INVALID_LOG_FILE:
			return DialogHelper.createDialog(getActivity(),
					R.string.alert_dialog_message_invalid_logfile,
					R.string.alert_dialog_title_invalid_logfile, 0,
					android.R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							mTaskMaster.deleteComplainReport(report);
							if (mOnDialogKeyPressedListener != null)
								mOnDialogKeyPressedListener
										.onKeyPressed(POSITIVE);
						}
					}, android.R.string.cancel, null);
		case DLG_UPLOAD_CONFIRM:
			sendReport(report);
		case DLG_DELETE_CONFIRM:
			int msgId = reports.size() > 1 ? R.string.alert_dialog_delete_reports
					: R.string.alert_dialog_delete_report;
			return DialogHelper.createDialog(getActivity(), msgId, 0,
					R.drawable.alert_dialog_icon, R.string.yes,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							for (ComplainReport report : reports) {
								mTaskMaster.deleteComplainReport(report);
							}
							if (mOnDialogKeyPressedListener != null)
								mOnDialogKeyPressedListener
										.onKeyPressed(POSITIVE);
						}
					}, R.string.no, null);
		}
		return null;
	}

	private void sendReport(ComplainReport report) {
		Message msg = Message.obtain();
		msg.what = TaskMaster.BUG_REPORT_SEND_LOG;
		msg.obj = report;
		mTaskMaster.sendMessage(msg);
	}
}
