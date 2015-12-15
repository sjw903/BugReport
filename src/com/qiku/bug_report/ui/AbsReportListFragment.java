package com.qiku.bug_report.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.ListFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.qiku.bug_report.BugReportApplication;
import com.qiku.bug_report.Constants;
import com.qiku.bug_report.R;
import com.qiku.bug_report.TaskMaster;
import com.qiku.bug_report.helper.Util;
import com.qiku.bug_report.model.ComplainReport;
import com.qiku.bug_report.model.ComplainReport.State;
import com.qiku.bug_report.ui.ReportListDialogFragment.OnDialogKeyPressedListener;

public abstract class AbsReportListFragment extends ListFragment implements OnDialogKeyPressedListener{
    protected ActionMode mActionMode;
    private ActionModeCallback mActionModeCallback;
    protected List<ComplainReport> mReports;
    protected ArrayList<ComplainReport> mSelectedReports;
    protected ReportListArrayAdapter mListdapter;
    protected TaskMaster mTaskMaster;
    private BroadcastReceiver mReportStateChangedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            loadData();
        }
    };

    public AbsReportListFragment() {
        mSelectedReports = new ArrayList<ComplainReport>();
        mActionModeCallback = createActionModeCallback();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.report_list_view, null);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mTaskMaster = ((BugReportApplication)getActivity().getApplicationContext()).getTaskMaster();
        mListdapter = createReportListArrayAdapter();
        setListAdapter(mListdapter);

        final DragSortListView listView = (DragSortListView) getListView();
        listView.setDropListener(new DragSortListView.DropListener() {
            public void drop(int from, int to) {
                dropItem(from, to);
            }
        });
        listView.setOnItemLongClickListener(new OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                    int position, long id) {
                onListItemClick(position);
                return true;
            }
        });
        listView.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                listView.onTouchEvent(event);
                return getActivity().onTouchEvent(event);
            }
        });
    }

    public void onStart(){
        loadData();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.BUGREPORT_INTENT_REPORT_CREATED);
        filter.addAction(Constants.BUGREPORT_INTENT_REPORT_REMOVED);
        filter.addAction(Constants.BUGREPORT_INTENT_REPORT_UPDATED);
        filter.addAction(Constants.BUGREPORT_INTENT_UPLOAD_PAUSED);
        filter.addAction(Constants.BUGREPORT_INTENT_UPLOAD_UNPAUSED);
        getActivity().registerReceiver(mReportStateChangedReceiver, filter);
        super.onStart();
    }

    protected void loadData(){
        mReports = mTaskMaster.getBugReportDAO().getReportsByState(getSupportedStates());
        mListdapter.clear();
        mListdapter.addAll(mReports);
        mListdapter.notifyDataSetChanged();
    }

    protected State[] getSupportedStates(){
        return new State[]{};
    }

    //show report viewer when user clicks a report
    public void onListItemClick(ListView l, View v, int position, long id) {
        if(mActionMode != null){
            onListItemClick(position);
        }else{
            ComplainReport report = mListdapter.getItem(position);
            onListItemClick(report);
        }
    }

    private boolean onListItemClick(int position){
        ComplainReport report = mListdapter.getItem(position);
        if(mActionMode != null){
            if(mSelectedReports.contains(report)){
                mSelectedReports.remove(report);
                if(mSelectedReports.size() == 1)
                    mActionModeCallback.onSingleReportSelected();
            }else{
                storeSelectedReport(report);
            }
        }else{
            mActionMode = getActivity().startActionMode(mActionModeCallback);
            storeSelectedReport(report);
        }
        mListdapter.notifyDataSetChanged();
        if( mActionMode != null ){
            if(mSelectedReports.isEmpty())
                mActionMode.finish();
            else
                mActionMode.setTitle("" + mSelectedReports.size()
                    + getString(R.string.report_selected));
        }
        return true;
    }

    private void storeSelectedReport(ComplainReport report){
        mSelectedReports.add(report);
        if(mSelectedReports.size() == 1)
            mActionModeCallback.onSingleReportSelected();
        else if(mSelectedReports.size() > 1)
            mActionModeCallback.onMultipleReportsSelected();
    }

    protected void onListItemClick(ComplainReport report){
        viewReport(report);
    }

    protected void selectAll(){
        if(mActionMode != null)
            mActionMode.finish();
        for(int i=0; i<mListdapter.getCount(); i++){
            onListItemClick(i);
        }
    }

    /**
     * This is called when user drag and drop an item from one position to the other,
     * subclasses must override this method to update the data model of the ListAdapter.
     * @param from
     * @param to
     */
    public void dropItem(int from , int to){}

    public void onStop(){
        if(mActionMode != null)
            mActionMode.finish();
        getActivity().unregisterReceiver(mReportStateChangedReceiver);
        super.onStop();
    }

    public void onKeyPressed(int keyCode){
        if(keyCode ==  OnDialogKeyPressedListener.POSITIVE){
            if(mActionMode != null)
                mActionMode.finish();
        }
    }

    protected void viewReport(ComplainReport report){
        Intent intent = new Intent(getActivity(), ReportViewer.class);
        intent.setAction(Constants.BUGREPORT_INTENT_VIEW_REPORT);
        intent.putExtra(Constants.BUGREPORT_INTENT_EXTRA_REPORT, report);
        startActivity(intent);
    }

    protected void deleteReport(ArrayList<ComplainReport> reports){
        ReportListDialogFragment dlgFrag = ReportListDialogFragment.getInstance(getActivity());
        dlgFrag.showDialog(getFragmentManager(),
                ReportListDialogFragment.DLG_DELETE_CONFIRM, reports, this);
    }

    public boolean onOptionsItemSelected(int menuId){
        switch(menuId){
            case R.id.action_selectall:
                selectAll();
                return true;
        }
        return false;
    }

    public int[] getSupportedMenuItemIds(){
        return new int[]{R.id.action_selectall};
    }

    public abstract ActionModeCallback createActionModeCallback();

    public class ActionModeCallback implements ActionMode.Callback {
        private Menu mMenu;
        private int mActionMenuResId;
        public ActionModeCallback(int actionMenuResid){
            mActionMenuResId = actionMenuResid;
        }
        // Called when the action mode is created; startActionMode() was called
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            // Assumes that you have "contexual.xml" menu resources
            inflater.inflate(mActionMenuResId, menu);
            mMenu = menu;
            return true;
        }

        // Called each time the action mode is shown. Always called after
        // onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        // Called when the user exits the action mode
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            mSelectedReports.clear();
            mListdapter.notifyDataSetChanged();
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if(mSelectedReports.isEmpty())
                return false;
            switch (item.getItemId()) {
                case R.id.action_discard:
                    ArrayList<ComplainReport> reportsTodelete = new ArrayList<ComplainReport>();
                    reportsTodelete.addAll(mSelectedReports);
                    deleteReport(reportsTodelete);
                    return true;
                case R.id.action_view:
                    //can view only one at a time, so edit the first report user selected
                    viewReport(mSelectedReports.get(0));
                    mode.finish();
                    return true;
                case R.id.action_selectall:
                    selectAll();
                    return true;
            }
            return false;
        }

        protected void setMenuItemVisible(boolean visible, int ... menuItemIds){
            for(int menuItemId : menuItemIds){
               MenuItem item = mMenu.findItem(menuItemId);
               if(item != null)
                   item.setVisible(visible);
            }
        }

        public void onMultipleReportsSelected(){
            setMenuItemVisible(false, R.id.action_view);
        }

        public void onSingleReportSelected(){
            setMenuItemVisible(true, R.id.action_view);
        }
    };

    protected ReportListArrayAdapter createReportListArrayAdapter(){
        return new ReportListArrayAdapter(getActivity(), 0);
    }

    protected class ReportListArrayAdapter extends ArrayAdapter<ComplainReport> {
        private LayoutInflater mInflater;

        public ReportListArrayAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
            mInflater = LayoutInflater.from(context);
        }


        public View getView(final int position, View convertView,
                ViewGroup parent) {
            ComplainReport report = getItem(position);
            ViewHolder holder = null;
            if (convertView == null) {
                convertView = mInflater.inflate(
                        R.layout.report_list_row, null);
                holder = new ViewHolder();
                holder.textSummary = (TextView) convertView.findViewById(R.id.textSummary);
                holder.textDescription = (TextView) convertView.findViewById(R.id.textDescription);
                holder.textDate = (TextView) convertView.findViewById(R.id.textDate);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            if(TextUtils.isEmpty(report.getTitle())){
                holder.textSummary.setText(null);
                holder.textSummary.setHint(getString(R.string.report_list_item_no_title));
            }else{
                holder.textSummary.setText(report.getTitle());
            }

            if(TextUtils.isEmpty(report.getFreeText())){
                holder.textDescription.setText(null);
                holder.textDescription.setHint(getString(R.string.report_list_item_no_desc));
            }else{
                holder.textDescription.setText(report.getFreeText());
            }

            holder.textDate.setText(Util.formatShorterDate(report.getCreateTime()));

            if(mSelectedReports.contains(report))
                convertView.setBackgroundResource(android.R.color.holo_blue_light);
            else
                convertView.setBackgroundResource(0);

            return convertView;
        }

        class ViewHolder {
            TextView textSummary;
            TextView textDescription;
            TextView textDate;
        }
    }
}
