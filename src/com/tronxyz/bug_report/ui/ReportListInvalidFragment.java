package com.tronxyz.bug_report.ui;

import com.tronxyz.bug_report.R;
import com.tronxyz.bug_report.model.ComplainReport.State;

public class ReportListInvalidFragment extends AbsReportListFragment {

    public State[] getSupportedStates(){
        return new State[]{State.BUILD_FAILED, State.COMPRESS_FAILED, State.TRANSMIT_FAILED,
                State.COMPLETE_FAILED, State.USER_DELETED_OUTBOX, State.USER_DELETED_DRAFT};
    }

    public ActionModeCallback createActionModeCallback(){
        return new ActionModeCallback(R.menu.report_list_sent_action_mode_actions);
    }
}
