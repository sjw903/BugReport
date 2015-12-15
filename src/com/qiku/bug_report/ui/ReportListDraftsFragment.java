package com.qiku.bug_report.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.MenuItem;

import com.qiku.bug_report.Constants;
import com.qiku.bug_report.R;
import com.qiku.bug_report.model.ComplainReport;
import com.qiku.bug_report.model.ComplainReport.State;

public class ReportListDraftsFragment extends AbsReportListFragment {
	private final static String TAG = "BugReportDraftsFragment";

	public State[] getSupportedStates() {
		return new State[] { State.BUILDING, State.WAIT_USER_INPUT };
	}

	public void onListItemClick(ComplainReport report) {
		Log.i(TAG, "onListItemClick");
		editReport(report);
	}

	protected void loadData() {
		mReports = mTaskMaster.getBugReportDAO().getReportsByState(
				getSupportedStates());
		List<ComplainReport> delList = new ArrayList<ComplainReport>();
		for (ComplainReport report : mReports) {
			long createTime = report.getCreateTime().getTime();
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DAY_OF_MONTH, -1);
			long timeMillis = c.getTimeInMillis();
			if (timeMillis >= createTime) {
				delList.add(report);
				mTaskMaster.deleteComplainReport(report);
			}
		}
		mReports.removeAll(delList);
		mListdapter.clear();
		mListdapter.addAll(mReports);
		mListdapter.notifyDataSetChanged();
	}

	private void sendReport(ComplainReport report) {
		if (mSelectedReports.isEmpty())
			return;

		ReportListDialogFragment dlgFrag = ReportListDialogFragment
				.getInstance(getActivity());
		if (TextUtils.isEmpty(report.getCategory())
				|| TextUtils.isEmpty(report.getSummary())
				|| TextUtils.isEmpty(report.getSummary().trim())
				|| ComplainReport.State.BUILDING == report.getState()) {
			dlgFrag.showDialog(getFragmentManager(),
					ReportListDialogFragment.DLG_REPORT_UNCOMPLETED, report,
					this);
			return;
		}

		if (!mTaskMaster.getConfigurationManager().isUserSettingsValid()) {
			ReportListDialogFragment.getInstance(getActivity())
					.showDialog(getFragmentManager(),
							ReportListDialogFragment.DLG_CONTACT_REQUIRED,
							report, this);
			return;
		}

		File file = new File(report.getLogPath());
		if (!file.exists()) {
			dlgFrag.showDialog(getFragmentManager(),
					ReportListDialogFragment.DLG_INVALID_LOG_FILE, report, this);
			return;
		}

		dlgFrag.showDialog(getFragmentManager(),
				ReportListDialogFragment.DLG_UPLOAD_CONFIRM, report, this);
	}

	private void editReport(ComplainReport report) {
		Intent intent = new Intent(getActivity(), Launcher.class);
		intent.setAction(Constants.BUGREPORT_INTENT_EDIT_REPORT);
		intent.putExtra(Constants.BUGREPORT_INTENT_EXTRA_REPORT, report);
		startActivity(intent);
	}

	public ActionModeCallback createActionModeCallback() {
		return new ActionModeCallback(
				R.menu.report_list_drafts_action_mode_actions) {

			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				if (super.onActionItemClicked(mode, item))
					return true;
				if (mSelectedReports.isEmpty())
					return false;
				switch (item.getItemId()) {
				case R.id.action_upload:
					sendReport(mSelectedReports.get(0));
					return true;
				case R.id.action_edit:
					// can edit only one at a time, so edit the first report
					// user selected
					editReport(mSelectedReports.get(0));
					mode.finish();
					return true;
				}
				return false;
			}

			public void onMultipleReportsSelected() {
				setMenuItemVisible(false, R.id.action_upload, R.id.action_edit,
						R.id.action_view);
			}

			public void onSingleReportSelected() {
				setMenuItemVisible(true, R.id.action_upload, R.id.action_edit,
						R.id.action_view);
			}
		};
	}
}
