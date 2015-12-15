package com.tronxyz.bug_report.ui;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.tronxyz.bug_report.R;
import com.tronxyz.bug_report.model.ComplainReport;
import com.tronxyz.bug_report.model.ComplainReport.State;

public class ReportListSentFragment extends AbsReportListFragment {

	public State[] getSupportedStates() {
		return new State[] { State.READY_TO_ARCHIVE, State.ARCHIVED_FULL,
				State.ARCHIVED_PARTIAL };
	}

	public ActionModeCallback createActionModeCallback() {
		return new ActionModeCallback(
				R.menu.report_list_sent_action_mode_actions);
	}

	protected void loadData() {
		mReports = mTaskMaster.getBugReportDAO().getReportsByState(
				getSupportedStates());
		System.out.println("已发送报告： " + mReports.size());
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
}
