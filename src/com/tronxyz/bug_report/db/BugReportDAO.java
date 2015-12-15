package com.tronxyz.bug_report.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import com.tronxyz.bug_report.Constants;
import com.tronxyz.bug_report.model.ComplainReport;
import com.tronxyz.bug_report.model.ComplainReport.State;

public class BugReportDAO {
    static final String tag = "BugReportDAO";

    DBAdapter mDbAdapter;
    Context mContext;

    public BugReportDAO(Context context) {
        mContext = context;
        mDbAdapter = new DBAdapter(context);
        mDbAdapter.open();
    }

    public long saveReport(ComplainReport report){
        long id = insertReport(report);
        report.setId(id);
        broadcastChanges(Constants.BUGREPORT_INTENT_REPORT_CREATED, report);
        return id;
    }

    private long insertReport(ComplainReport report) {
        long id = mDbAdapter.insertReport(report.getLogPath(),
                                          report.getState().name(),
                                          report.getType().name(),
                                          report.getCreateTime().getTime(),
                                          report.getCategory(),
                                          report.getSummary(),
                                          report.getFreeText(),
                                          report.getScreenshotPath(),
                                          report.getAttachment(),
                                          report.getApVersion(),
                                          report.getBpVersion(),
                                          report.getApkVersion(),
                                          report.getShowNotification());
        report.setId(id);
        return id;
    }

    public boolean updateReport(ComplainReport report){
        if(alterReport(report)){
            broadcastChanges(Constants.BUGREPORT_INTENT_REPORT_UPDATED, report);
            return true;
        }
        return false;
    }

    public boolean alterReport(ComplainReport report) {
        boolean result = mDbAdapter.updateReport(report.getId(),
                         report.getLogPath(),
                         report.getState().name(),
                         report.getType().name(),
                         report.getCreateTime().getTime(),
                         report.getCategory(),
                         report.getSummary(),
                         report.getFreeText(),
                         report.getScreenshotPath(),
                         report.getAttachment(),
                         report.getApVersion(),
                         report.getBpVersion(),
                         report.getApkVersion());
            return result;
    }

    public boolean updateReportUploadInfo(ComplainReport report, boolean updatePriority) {
        boolean success = mDbAdapter.updateReportState(report, updatePriority);
        if(success)
            broadcastChanges(Constants.BUGREPORT_INTENT_REPORT_UPDATED, report);
        return success;
    }

    public boolean updateReportState(ComplainReport report){
        boolean success =  mDbAdapter.updateReportState(report.getId(), report.getState().name());
        if(success)
            broadcastChanges(Constants.BUGREPORT_INTENT_REPORT_UPDATED, report);
        return success;
    }

    public List<ComplainReport> getAllReports() {
        Cursor cursor = mDbAdapter.fetchAllReports();
        try {
            return cursorToComplainReports(cursor);
        } finally {
            cursor.close();
        }
    }

    public ComplainReport getReportById(long id){
        Cursor cursor = mDbAdapter.fetchReport(id);
        try {
            List<ComplainReport> reports = cursorToComplainReports(cursor);
            if(reports.size() > 0)
                return reports.get(0);
            return null;
        } finally {
            cursor.close();
        }
    }

    public List<ComplainReport> getReportsByState(ComplainReport.State ... states) {
        Cursor cursor = mDbAdapter.fetchReportsByState(states);
        try {
            return cursorToComplainReports(cursor);
        } finally {
            cursor.close();
        }
    }

    public List<ComplainReport> getReportsByState(boolean paused, ComplainReport.State ... states) {
        Cursor cursor = mDbAdapter.fetchReportsByState(paused ? 1 : 0, states);
        try {
            return cursorToComplainReports(cursor);
        } finally {
            cursor.close();
        }
    }

    public boolean deleteReport(ComplainReport report) {
        if (mDbAdapter.deleteReport(report.getId())) {
            mDbAdapter.deleteQuestion(report.getId());
            report.setState(State.USER_DELETED_OUTBOX);
            broadcastChanges(Constants.BUGREPORT_INTENT_REPORT_REMOVED, report);
            return true;
        }
        return false;
    }

    public void movePriority(int fromPriority, int toPriority){
        mDbAdapter.movePriority(fromPriority, toPriority);
        Intent intent = new Intent(Constants.BUGREPORT_INTENT_UPLOAD_PRIORITY_CHANGED);
        intent.putExtra(Constants.BUGREPORT_INTENT_EXTRA_PRIORITY_FROM, fromPriority);
        intent.putExtra(Constants.BUGREPORT_INTENT_EXTRA_PRIORITY_TO, toPriority);
        mContext.sendBroadcast(intent);
    }

    public void setReportUploadPaused(boolean paused, long ... ids){
        if(ids == null || ids.length == 0)
            return;
        mDbAdapter.updateReportPaused(paused ? 1 : 0, ids);
        Intent intent = new Intent();
        if(paused)
            intent = new Intent(Constants.BUGREPORT_INTENT_UPLOAD_PAUSED);
        else
            intent = new Intent(Constants.BUGREPORT_INTENT_UPLOAD_UNPAUSED);
        intent.putExtra(Constants.BUGREPORT_INTENT_EXTRA_REPORT_IDS, ids);
        mContext.sendBroadcast(intent);
    }

    private List<ComplainReport> cursorToComplainReports(Cursor cursor) {
        List<ComplainReport> reports = new ArrayList<ComplainReport>();
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndex(DBAdapter.DB_TABLE_REPORTS_ID));
                String category = cursor.getString(cursor.getColumnIndex(DBAdapter.DB_TABLE_REPORTS_CATEGORY));
                String logPath = cursor.getString(cursor.getColumnIndex(DBAdapter.DB_TABLE_REPORTS_LOGPATH));
                long createTime = cursor.getLong(cursor.getColumnIndex(DBAdapter.DB_TABLE_REPORTS_CREATETIME));
                String state = cursor.getString(cursor.getColumnIndex(DBAdapter.DB_TABLE_REPORTS_STATE));
                String type = cursor.getString(cursor.getColumnIndex(DBAdapter.DB_TABLE_REPORTS_TYPE));
                String summary = cursor.getString(cursor.getColumnIndex(DBAdapter.DB_TABLE_REPORTS_SUMMARY));
                String freeText = cursor.getString(cursor.getColumnIndex(DBAdapter.DB_TABLE_REPORTS_FREETEXT));
                String uploadId = cursor.getString(cursor.getColumnIndex(DBAdapter.DB_TABLE_REPORTS_UPLOADID));
                int priority = cursor.getInt(cursor.getColumnIndex(DBAdapter.DB_TABLE_REPORTS_PRIORITY));
                int uploadPaused = cursor.getInt(cursor.getColumnIndex(DBAdapter.DB_TABLE_REPORTS_UPLOAD_PAUSED));
                int uploadedBytes = cursor.getInt(cursor.getColumnIndex(DBAdapter.DB_TABLE_REPORTS_UPLOADEDBYTES));
                String screenshotPath = cursor.getString(cursor.getColumnIndex(DBAdapter.DB_TABLE_REPORTS_SCREENSHOT));
                String attachment = cursor.getString(cursor.getColumnIndex(DBAdapter.DB_TABLE_REPORTS_ATTACHEMNT));
                String apVersion = cursor.getString(cursor.getColumnIndex(DBAdapter.DB_TABLE_REPORTS_AP_VERSION));
                String bpVersion = cursor.getString(cursor.getColumnIndex(DBAdapter.DB_TABLE_REPORTS_BP_VERSION));
                String apkVersion = cursor.getString(cursor.getColumnIndex(DBAdapter.DB_TABLE_REPORTS_APK_VERSION));
                int showNotif = cursor.getInt(cursor.getColumnIndex(DBAdapter.DB_TABLE_REPORTS_SHOW_NOTIFICATION));

                ComplainReport report = new ComplainReport();
                report.setId(id);
                report.setCategory(category);
                report.setLogPath(logPath);
                report.setCreateTime(new Date(createTime));
                report.setState(ComplainReport.State.valueOf(state));
                report.setType(ComplainReport.Type.valueOf(type));
                report.setSummary(summary);
                report.setFreeText(freeText);
                report.setUploadId(uploadId);
                report.setPriority(priority);
                report.setUploadPaused(uploadPaused);
                report.setUploadedBytes(uploadedBytes);
                report.setScreenshotPath(screenshotPath);
                report.setAttachment(attachment);
                report.setApVersion(apVersion);
                report.setBpVersion(bpVersion);
                report.setApkVersion(apkVersion);
                report.setShowNotification(showNotif);

                reports.add(report);
            } while (cursor.moveToNext());
        }
        return reports;
    }

    public void close() {
        mDbAdapter.close();
    }

    private void broadcastChanges(String action, ComplainReport report){
        //broadcast the changes
        Intent intent = new Intent(action);
        intent.putExtra(Constants.BUGREPORT_INTENT_EXTRA_REPORT, report);
        mContext.sendBroadcast(intent);
    }
}
