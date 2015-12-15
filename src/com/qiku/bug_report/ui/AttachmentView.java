package com.qiku.bug_report.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.qiku.bug_report.BugReportApplication;
import com.qiku.bug_report.R;
import com.qiku.bug_report.TaskMaster;
import com.qiku.bug_report.helper.DialogHelper;
import com.qiku.bug_report.helper.Util;
import com.qiku.bug_report.model.ComplainReport;
import com.qiku.bug_report.model.UserSettings;
import com.qiku.bug_report.model.UserSettings.Conf;

public class AttachmentView extends LinearLayout {
    private static String tag = "BugReportAttachmentView";
    public static final int DLG_DEL_ATTACHMENT_CONFIRM = 100;
    public static final int DLG_ATTACH_SCREENSHOT = 101;
    private Activity mParentActivity;
    private LinearLayout mAttachmentListView;
    private LayoutInflater mInflater;
    private String mSelectedAttachment;
    private LinearLayout mSelectedAttachmentView;
    private TextView mSelectedAttachmentTextView;

    private String mLogPath;
    private List<String> mPendingAttachments = new ArrayList<String>();

    private ComplainReport mCurrentReport;
    private TaskMaster mTaskMaster;
    public AttachmentView(Context context, AttributeSet attrs){
        super(context, attrs);
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        addView(LayoutInflater.from(context).inflate(R.layout.attachments_layout, null));

        mInflater = LayoutInflater.from(context);
        mAttachmentListView = (LinearLayout)findViewById(R.id.attachmentsList);

        mTaskMaster = ((BugReportApplication) getContext().getApplicationContext()).getTaskMaster();
    }

    public void createNewAttachment(Uri contentUri){
        String uriPath = null;
        String scheme = contentUri.getScheme();
        if(scheme.equals("file")){
            uriPath = contentUri.getPath();
        }else if(scheme.equals("content")){
            uriPath = Util.getRealPathFromURI(contentUri, mParentActivity);
        }
        createNewAttachment(uriPath);
    }

    private void createNewAttachment(final String filePath){
        if(filePath == null || TextUtils.isEmpty(filePath)){
            Log.d(tag, "Invalid attachment path : " + filePath);
            return;
        }

        final String fileName = filePath.substring(filePath.lastIndexOf(File.separator) + 1);

        if(mCurrentReport != null && mCurrentReport.getAttachment() != null){
            List<String> attachmentNames = mCurrentReport.getAttachments();
            for(String attachmentName : attachmentNames){
                if(attachmentName.equals(fileName)){
                    Toast.makeText(getContext(), "File already exists " + fileName, Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }

        if(mPendingAttachments.contains(fileName)){
            Toast.makeText(getContext(), "File already exists " + fileName, Toast.LENGTH_LONG).show();
            return;
        }

        new AsyncTask<Void, Void, Boolean>(){
            protected Boolean doInBackground(Void... params) {
                try {
                    //if the report has already been created , we will directly add the file to the report
                    if(mCurrentReport != null){
                        return ReportAttachmentUpdater.getInstance(mTaskMaster).attachFile(mCurrentReport, filePath);
                    }else{//otherwise, we will copy the file to the log path, then add it to the report when the report is created.
                        if(TextUtils.isEmpty(mLogPath)){
                            return false;
                        }
                        if(Util.copyFile(filePath, mLogPath)){
                            //add the files to pending attachment list, so it can be sync-ed with the report later
                            mPendingAttachments.add(fileName);
                            return true;
                        }
                        return false;
                    }
                } catch (Exception e) {
                    Log.e(tag, "Failed to add attachment", e);
                    return false;
                }
            }

            protected void onPostExecute(Boolean success) {
                if(!success){ //remove the file that just added to the view
                    for(int i = mAttachmentListView.getChildCount() - 1; i >= 0 ; i --){
                        View attachmentView = mAttachmentListView.getChildAt(i);
                        if(attachmentView != null && attachmentView.getId() == fileName.hashCode()){
                            mAttachmentListView.removeView(attachmentView);
                            break;
                        }
                    }
                    Toast.makeText(getContext(), "Failed to attach " + filePath, Toast.LENGTH_LONG).show();
                }else{
                    for(int i = 0; i < mAttachmentListView.getChildCount(); i ++){
                        LinearLayout attachmentView = (LinearLayout)mAttachmentListView.getChildAt(i);
                        if(attachmentView != null && attachmentView.getId() == fileName.hashCode()){
                            //set the delete button visible
                            View delBtnView = attachmentView.getChildAt(1);
                            if(delBtnView != null)
                                delBtnView.setVisibility(VISIBLE);
                            //remove the progress bar
                            if(attachmentView.getChildCount() == 3)
                                attachmentView.removeViewAt(2);
                            break;
                        }
                    }
                }
            }
        }.execute(new Void[0]);

        updateViewWithFile(fileName, false, false);
    }

    public void showAttachments(){
        if(mCurrentReport == null)
            return;
        //show attachments added by user and screen shots
        List<String> fileNames = mCurrentReport.getAttachments();
        for(String fileName : fileNames){
            updateViewWithFile(fileName, true, false);
        }
    }

    /**
     * This is called when the tr-bugreport finishes log collection
     */
    @SuppressWarnings("deprecation")
	public void onCollectorEnd(ComplainReport report){
        mCurrentReport = report;
        //update the mCurrentReport object with the files added before the log collections completes
        for(String attachedFile : mPendingAttachments){
            ReportAttachmentUpdater.getInstance(mTaskMaster).updateReportAttachNamesOnly(mCurrentReport, attachedFile);
        }
        mPendingAttachments.clear();

        //show confirmation dialog if there is a screenshot
        if(!TextUtils.isEmpty(mCurrentReport.getScreenshotPath())){
            String attachScreen = mTaskMaster.getConfigurationManager().getUserSettings().getAttachScreenshot().getValue();
            if(Conf.PROMPT.name().equalsIgnoreCase(attachScreen)){
                mParentActivity.showDialog(DLG_ATTACH_SCREENSHOT);
            }else if(Conf.TRUE.name().equalsIgnoreCase(attachScreen)){
                attachScreenshot();
            }
        }
    }

    public void attachScreenshot(){
        if(mCurrentReport == null){
            Toast.makeText(getContext(), "Screenshot is not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }
        if(!TextUtils.isEmpty(mCurrentReport.getScreenshotPath()))
            createNewAttachment(mCurrentReport.getScreenshotPath());
    }

    /**
     *
     * @param fileName
     * @param enabled should this view be enabled or not
     * @param isMainLog  Is the log file collected by the tr-bugreport?
     */
    private void updateViewWithFile(final String fileName, boolean enabled, boolean isMainLog){
        if(fileName == null)
            return;
        final LinearLayout attachmentView = (LinearLayout)mInflater.inflate(R.layout.attachment_view, null);
        attachmentView.setId(fileName.hashCode());
        mAttachmentListView.addView(attachmentView);

        final TextView textView = (TextView) attachmentView.findViewById(R.id.attachment_name);
        textView.setText(fileName);
        textView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if(mSelectedAttachmentTextView != null){
                    mSelectedAttachmentTextView.setSelected(false);
                }
                textView.setSelected(true);
                mSelectedAttachmentTextView = textView;
            }
        });
        ImageView imageView = (ImageView)attachmentView.findViewById(R.id.del_attachment);
        ProgressBar pgrsbar = (ProgressBar)attachmentView.findViewById(R.id.prgrs_add_attachment);
        if(isMainLog){
            attachmentView.removeView(imageView);
            attachmentView.removeView(pgrsbar);
            return;
        }

        imageView.setOnClickListener(new OnClickListener() {
            @SuppressWarnings("deprecation")
			public void onClick(View v) {
                mSelectedAttachment = fileName;
                mSelectedAttachmentView = attachmentView;
                mParentActivity.showDialog(DLG_DEL_ATTACHMENT_CONFIRM, null);
            }
        });
        if(enabled){
            //remove the progress bar
            attachmentView.removeView(pgrsbar);
        }else{
            imageView.setVisibility(INVISIBLE);
        }
    }

    public Dialog onCreateDialog(int id, Bundle bundle){
        switch(id){
            case DLG_DEL_ATTACHMENT_CONFIRM:
                return DialogHelper.createDialog(mParentActivity, R.string.alert_dialog_delete_attach, 0, R.drawable.alert_dialog_icon, R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                removeAttachment(mSelectedAttachment);
                            }
                        }, R.string.no, null);
            case DLG_ATTACH_SCREENSHOT:
                final CheckBox box = new CheckBox(getContext());
                box.setText(R.string.alert_dialog_dont_show_again);

                DialogInterface.OnClickListener btnYesListener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if(box.isChecked()){
                            UserSettings settings = mTaskMaster.getConfigurationManager().getUserSettings();
                            settings.setAttachScreenshot(Conf.TRUE.name());
                            mTaskMaster.getConfigurationManager().saveUserSettings(settings);
                        }
                        attachScreenshot();
                    }
                };

                DialogInterface.OnClickListener btnNoListener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if(box.isChecked()){
                            UserSettings settings = mTaskMaster.getConfigurationManager().getUserSettings();
                            settings.setAttachScreenshot(Conf.FALSE.name());
                            mTaskMaster.getConfigurationManager().saveUserSettings(settings);
                        }
                    }
                };

                return DialogHelper.createDialog(getContext(), R.string.alert_dialog_message_attach_screenshot, R.string.alert_dialog_title_attach_screenshot, android.R.drawable.ic_dialog_alert, R.string.yes, btnYesListener, R.string.no, btnNoListener, box);
        }
        return null;
    }

    private void removeAttachment(final String fileName){
        if(fileName != null){
            //To remove the attachment from the zip file.
            new AsyncTask<Void, Void, Boolean>(){
                @Override
                protected Boolean doInBackground(Void... params) {
                    try {
                        if(mCurrentReport != null)
                            return ReportAttachmentUpdater.getInstance(mTaskMaster).unattachFile(mCurrentReport, fileName);
                        else if (!TextUtils.isEmpty(mLogPath)){
                            if(Util.removeFile(mLogPath + File.separator + fileName)){
                                mPendingAttachments.remove(fileName);
                                return true;
                            }
                        }
                        return false;
                    } catch (Exception e) {
                        Log.e(tag, "Failed to add attachment", e);
                        return false;
                    }
                }
                protected void onPostExecute(Boolean success) {
                    if(!success){
                        Toast.makeText(getContext(), "Failed to delete " + fileName, Toast.LENGTH_SHORT).show();
                    }
                }
            }.execute(new Void[0]);
            //remove the view
            mAttachmentListView.removeView(mSelectedAttachmentView);
        }
    }

    public void setParentActivity(Activity activity){
        this.mParentActivity = activity;
    }

    public void setLogPath(String logPath){
        mLogPath = logPath;
    }

    public void setComplainReport(ComplainReport report){
        mCurrentReport = report;
    }

    public void reset(){
        mAttachmentListView.removeAllViews();
        mCurrentReport = null;
        mSelectedAttachment = null;
        mSelectedAttachmentView = null;
        mSelectedAttachmentTextView = null;
        mLogPath = null;
        mPendingAttachments.clear();
    }

    /**
     * A thread-safe zip file updater that ensures zip will not be modified dirty.
     */
    public static class ReportAttachmentUpdater{
        private static ReportAttachmentUpdater instance;
        private TaskMaster mTaskMaster;

        private ReportAttachmentUpdater(TaskMaster taskMaster){
            this.mTaskMaster = taskMaster;
        }

        public static synchronized ReportAttachmentUpdater getInstance(TaskMaster taskMaster){
            if(instance == null){
                instance = new ReportAttachmentUpdater(taskMaster);
            }
             return instance;
        }

        /**
         * This method is used to sync the report object with the files that attached before
         * the log collection completes. It will add the attachment info to the report object
         * and update them to database.
         * object has not been aware of this attachment.
         * @param report
         * @param attachName
         * @return
         */
        public boolean updateReportAttachNamesOnly(ComplainReport report, String attachName){
            if(report == null || attachName == null)
                return false;

            if(TextUtils.isEmpty(report.getAttachment())){
                report.setAttachment(attachName);
            }else{
                List<String> attachmentNames = report.getAttachments();
                for(String attachmentName : attachmentNames){
                    if(attachmentName.equals(attachName)){
                        return false;
                    }
                }
                report.setAttachment(attachName + "^|^" + report.getAttachment());
            }
            //update all change to database
            mTaskMaster.getBugReportDAO().updateReport(report);
            return true;
        }

        public boolean attachFile(ComplainReport report, String filePath){
            if(report == null || filePath == null)
                return false;
            String fileName = filePath.substring(filePath.lastIndexOf(File.separator) + 1);
            boolean success = false;
            try{
                if(updateReportAttachNamesOnly(report, fileName))
                    success = Util.copyFile(filePath, report.getLogPath());
            }catch(Exception e){
                Log.e(tag, e.getMessage(), e);
            }finally{
                if(!success){
                    List<String> attachmentNames = report.getAttachments();
                    report.setAttachment("");
                    for(String attachmentName : attachmentNames){
                        if(!attachmentName.equals(fileName)){
                            report.setAttachment(attachmentName + "^|^" + report.getAttachment());
                        }
                    }
                    //update all change to database
                    mTaskMaster.getBugReportDAO().updateReport(report);
                }
            }
            return success;
        }

        public boolean unattachFile(ComplainReport report, String fileName){
            if(report == null || fileName == null)
                return false;
            boolean success = false;
            try{
                List<String> attachmentNames = report.getAttachments();
                report.setAttachment("");
                for(String attachmentName : attachmentNames){
                    if(!attachmentName.equals(fileName)){
                        report.setAttachment(attachmentName + "^|^" + report.getAttachment());
                    }
                }
                //update all change to database
                mTaskMaster.getBugReportDAO().updateReport(report);
                success = Util.removeFile(report.getLogPath() + File.separator + fileName);
            }finally{
                //if deleting fails, add the attachment back to the report if the report is not a screenshot
                if(!success){
                    report.setAttachment(fileName + "^|^" + report.getAttachment());
                    //update all change to database
                    mTaskMaster.getBugReportDAO().updateReport(report);
                }
            }
            return success;
        }
    }
}
