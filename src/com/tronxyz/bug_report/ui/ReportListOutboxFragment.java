package com.tronxyz.bug_report.ui;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.tronxyz.bug_report.R;
import com.tronxyz.bug_report.Constants;
import com.tronxyz.bug_report.helper.Util;
import com.tronxyz.bug_report.model.ComplainReport;
import com.tronxyz.bug_report.model.UserSettings;
import com.tronxyz.bug_report.model.ComplainReport.State;
import com.tronxyz.bug_report.upload.ReliableUploader;

public class ReportListOutboxFragment extends AbsReportListFragment {
    public State[] getSupportedStates(){
        return new State[]{State.READY_TO_UPLOAD, State.READY_TO_COMPRESS, State.COMPRESSING,
                State.COMPRESSION_PAUSED, State.READY_TO_TRANSMIT,State.TRANSMITTING,
                State.READY_TO_COMPLETE, State.COMPLETING};
    }

    public void dropItem(int from , int to){
        try{
            if(from == to)
                return;
            int fromPriority = mListdapter.getItem(from).getPriority();
            int toPriority = mListdapter.getItem(to).getPriority();
            mTaskMaster.getBugReportDAO().movePriority(fromPriority, toPriority);
        }catch(Exception e){
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
        }finally{
            loadData();
        }
    }

    public void onStart(){
        super.onStart();
        ReportList activity = ((ReportList)getActivity());
        boolean enabled = mTaskMaster.getConfigurationManager().getUserSettings()
            .isAutoUploadEnabled().getValue();
        Log.d("test", "out box onStart enabled-->"+enabled);
        if (activity.getMenu() != null) {
            activity.getMenu().findItem(R.id.report_upload_switcher).setVisible(true);
            activity.getMenu().findItem(R.id.report_upload_switcher).setChecked(enabled);
        }
    }

    public boolean onOptionsItemSelected(int menuId){
        if(super.onOptionsItemSelected(menuId))
            return true;
        switch(menuId){
        case R.id.report_upload_switcher:
            final ReportList activity = ((ReportList)getActivity());
            MenuItem item = activity.getMenu().findItem(R.id.report_upload_switcher);
            //check the menu if it was unchecked before user selected it. and vice verse.
            item.setChecked(!item.isChecked());
            final boolean enabled = item.isChecked();
            //Save settings to local file in a worker thread
            new AsyncTask<Void, Void, Void>(){
                protected Void doInBackground(Void... params) {
                    //save the option to user settings file
                    UserSettings setting = mTaskMaster.getConfigurationManager().getUserSettings();
                    setting.setAutoUploadEnabled(enabled);
                    mTaskMaster.getConfigurationManager().saveUserSettings(setting);
                    //start uploader if it is enabled
                    if(enabled){
                        activity.startService(new Intent(getActivity(), ReliableUploader.class));
                    }else{//stop uploader if it is disabled
                        activity.sendBroadcast(new Intent(Constants.BUGREPORT_INTENT_PAUSE_UPLOAD));
                    }
                    return null;
                }
            }.execute(new Void[0]);
            return true;
        }
        return false;
    }

    public ActionModeCallback createActionModeCallback(){
        return new ActionModeCallback(R.menu.report_list_outbox_action_mode_actions){
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if(super.onActionItemClicked(mode, item))
                    return true;
                if(mSelectedReports.isEmpty())
                    return false;
                long[] ids = new long[mSelectedReports.size()];
                for(int i=0; i<ids.length; i++){
                    ids[i] = mSelectedReports.get(i).getId();
                }
                switch (item.getItemId()) {
                    case R.id.action_upload:
                        unpauseUpload(ids);
                        mode.finish();
                        return true;
                    case R.id.action_pause:
                        pauseUpload(ids);
                        mode.finish();
                        return true;
                }
                return false;
            }

            public void onMultipleReportsSelected(){
                int pausedStateCount = 0;
                int uploadStateCount = 0;
                int reportsCount = mSelectedReports.size();
                for(ComplainReport report : mSelectedReports){
                    if(report.isUploadPaused())
                        pausedStateCount ++;
                    else
                        uploadStateCount ++;
                }
                //if upload states of all selected reports are paused, only show upload item.
                if(reportsCount == pausedStateCount){
                    setMenuItemVisible(true, R.id.action_upload);
                    setMenuItemVisible(false, R.id.action_pause);
                //if upload states of all selected reports are uploading, only show pause item.
                }else if(reportsCount == uploadStateCount){
                    setMenuItemVisible(true, R.id.action_pause);
                    setMenuItemVisible(false, R.id.action_upload);
                //show both menu items
                }else{
                    setMenuItemVisible(true, R.id.action_pause, R.id.action_upload);
                }
                setMenuItemVisible(false, R.id.action_view);
            }

            public void onSingleReportSelected(){
                ComplainReport report = mSelectedReports.get(0);
                if(report.isUploadPaused()){
                    setMenuItemVisible(true, R.id.action_upload);
                    setMenuItemVisible(false, R.id.action_pause);
                }else{
                    setMenuItemVisible(true, R.id.action_pause);
                    setMenuItemVisible(false, R.id.action_upload);
                }
                setMenuItemVisible(true, R.id.action_view);
            }
        };
    }

    protected ReportListArrayAdapter createReportListArrayAdapter(){
        return new OutboxReportListArrayAdapter(getActivity(), 0);
    }

    private void pauseUpload(long ... reportId){
        mTaskMaster.getBugReportDAO().setReportUploadPaused(true, reportId);
    }

    private void unpauseUpload(long ... reportId){
        mTaskMaster.getBugReportDAO().setReportUploadPaused(false, reportId);
        Intent uploader = new Intent(getActivity(), ReliableUploader.class);
        getActivity().startService(uploader);
    }

    protected class OutboxReportListArrayAdapter extends ReportListArrayAdapter {
        private LayoutInflater mInflater;

        public OutboxReportListArrayAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
            mInflater = LayoutInflater.from(context);
        }

        public View getView(final int position, View convertView,
                ViewGroup parent) {
            final ComplainReport report = getItem(position);
            ViewHolder holder = null;
            if (convertView == null) {
                convertView = mInflater.inflate(
                        R.layout.report_list_outbox_row, null);
                holder = new ViewHolder();
                holder.textSummary = (TextView) convertView.findViewById(R.id.textSummary);
                holder.textDate = (TextView) convertView.findViewById(R.id.textDate);
                holder.btnPause = (ImageView) convertView.findViewById(R.id.pause);
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

            holder.textDate.setText(Util.formatShorterDate(report.getCreateTime()));

            ProgressLayout uploadProgressBar = (ProgressLayout)convertView.findViewById(R.id.progressSection);
            uploadProgressBar.setData(report);
            uploadProgressBar.setVisibility(View.VISIBLE);
            uploadProgressBar.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

            final ImageView btn = holder.btnPause;
            if(report.isUploadPaused())
                btn.setBackgroundResource(android.R.drawable.ic_menu_upload);
            else
                btn.setBackgroundResource(android.R.drawable.ic_media_pause);
            btn.setOnClickListener(new OnClickListener(){
                public void onClick(View v) {
                    if(report.isUploadPaused()){
                        Log.d("test", "out box upload pause-->going");
//                        btn.setBackgroundResource(android.R.drawable.ic_menu_upload);
                        unpauseUpload(report.getId());
                    }else{
                        Log.d("test", "out box upload going-->paused");
//                        btn.setBackgroundResource(android.R.drawable.ic_media_pause);
                        pauseUpload(report.getId());
                    }
                }
            });

            if(mSelectedReports.contains(report))
                convertView.setBackgroundResource(android.R.color.holo_blue_light);
            else
                convertView.setBackgroundResource(0);

            return convertView;
        }

        class ViewHolder {
            TextView textSummary;
            TextView textDate;
            ImageView btnPause;
        }
    }
}
