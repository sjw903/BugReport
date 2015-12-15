package com.qiku.bug_report.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.qiku.bug_report.model.ComplainReport;

public class DBAdapter {
    static final String tag = "BugReportDBAdapter";

    private static final String DATABASE_REPORTS_CREATE = "create table complain_reports("
            + "_id integer primary key autoincrement,"
            + "logpath text not null,"
            + "state text not null default 'WAIT_USER_INPUT',"
            + "type text not null default 'USER',"
            + "create_time long null,"
            + "category text null,"
            + "summary text null,"
            + "free_text text null,"
            + "upload_id text null,"
            + "priority integer null,"
            + "upload_paused integer not null default 0,"
            + "uploadedbytes integer null,"
            + "screenshot_path text null,"
            + "attachment text null,"
            + "ap_version text null,"
            + "bp_version text null,"
            + "delete_after_upload text null,"
            + "apk_version text null, "
            + "show_notification integer not null default 1"
            + ");";
    private static final String DATABASE_SURVEY_CREATE = "create table questions("
            + "_id integer primary key autoincrement,"
            + "report_id integer not null," + "type text not null,"
            + "required text not null," + "question text not null,"
            + "answers text null," + "user_answer text null,"
            + "FOREIGN KEY(report_id) REFERENCES complain_reports(_id)"
            + ")";

    public static final String DB_NAME = "reports.db";

    public static final String DB_TABLE_REPORTS = "complain_reports";
    public static final String DB_TABLE_REPORTS_ID = "_id";
    public static final String DB_TABLE_REPORTS_LOGPATH = "logpath";
    public static final String DB_TABLE_REPORTS_STATE = "state";
    public static final String DB_TABLE_REPORTS_TYPE = "type";
    public static final String DB_TABLE_REPORTS_CREATETIME = "create_time";
    public static final String DB_TABLE_REPORTS_CATEGORY = "category";
    public static final String DB_TABLE_REPORTS_SUMMARY = "summary";
    public static final String DB_TABLE_REPORTS_FREETEXT = "free_text";
    public static final String DB_TABLE_REPORTS_UPLOADID = "upload_id";
    public static final String DB_TABLE_REPORTS_PRIORITY = "priority";
    public static final String DB_TABLE_REPORTS_UPLOAD_PAUSED = "upload_paused";
    public static final String DB_TABLE_REPORTS_UPLOADEDBYTES = "uploadedbytes";
    public static final String DB_TABLE_REPORTS_SCREENSHOT = "screenshot_path";
    public static final String DB_TABLE_REPORTS_ATTACHEMNT = "attachment";
    public static final String DB_TABLE_REPORTS_AP_VERSION = "ap_version";
    public static final String DB_TABLE_REPORTS_BP_VERSION = "bp_version";
    public static final String DB_TABLE_REPORTS_DELETE_AFTER_UPLOAD = "delete_after_upload";
    public static final String DB_TABLE_REPORTS_APK_VERSION = "apk_version";
    public static final String DB_TABLE_REPORTS_SHOW_NOTIFICATION = "show_notification";

    public static final String DB_TABLE_SURVEY = "questions";
    public static final String DB_TABLE_SURVEY_ID = "_id";
    public static final String DB_TABLE_SURVEY_REPORT_ID = "report_id";
    public static final String DB_TABLE_SURVEY_TYPE = "type";
    public static final String DB_TABLE_SURVEY_REQUIRED = "required";
    public static final String DB_TABLE_SURVEY_QUESTION = "question";
    public static final String DB_TABLE_SURVEY_ANSWERS = "answers";
    public static final String DB_TABLE_SURVEY_USER_ANSWER = "user_answer";

    public static final int DATABASE_VERSION = 13;

    private static String[] reports_columns = { DB_TABLE_REPORTS_ID, DB_TABLE_REPORTS_LOGPATH,
                            DB_TABLE_REPORTS_STATE,         DB_TABLE_REPORTS_TYPE,
                            DB_TABLE_REPORTS_CREATETIME,    DB_TABLE_REPORTS_CATEGORY,
                            DB_TABLE_REPORTS_SUMMARY,       DB_TABLE_REPORTS_FREETEXT,
                            DB_TABLE_REPORTS_UPLOADID,      DB_TABLE_REPORTS_PRIORITY,
                            DB_TABLE_REPORTS_SCREENSHOT,    DB_TABLE_REPORTS_ATTACHEMNT,
                            DB_TABLE_REPORTS_AP_VERSION,    DB_TABLE_REPORTS_BP_VERSION,
                            DB_TABLE_REPORTS_DELETE_AFTER_UPLOAD,
                            DB_TABLE_REPORTS_APK_VERSION,   DB_TABLE_REPORTS_UPLOADEDBYTES,
                            DB_TABLE_REPORTS_UPLOAD_PAUSED, DB_TABLE_REPORTS_SHOW_NOTIFICATION
                                              };
    private static String[] survey_columns = { DB_TABLE_SURVEY_ID, DB_TABLE_SURVEY_REPORT_ID,
                            DB_TABLE_SURVEY_TYPE, DB_TABLE_SURVEY_REQUIRED,
                            DB_TABLE_SURVEY_QUESTION, DB_TABLE_SURVEY_ANSWERS,
                            DB_TABLE_SURVEY_USER_ANSWER
                                             };

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DB_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_REPORTS_CREATE);
            db.execSQL(DATABASE_SURVEY_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(tag, "Upgrading database from version " + oldVersion + " to " + newVersion
                  + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE_REPORTS);
            db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE_SURVEY);
            onCreate(db);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(tag, "Downgrading database from version " + oldVersion + " to " + newVersion
                  + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE_REPORTS);
            db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE_SURVEY);
            onCreate(db);
        }
    }

    private final Context mCtx;
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    public DBAdapter(Context context) {
        mCtx = context;
    }

    /**
     * Open the complain_reports database. If it cannot be opened, try to create
     * a new instance of the database. If it cannot be created, throw an
     * exception to signal the failure
     *
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException
     *             if the database could be neither opened or created
     */
    public DBAdapter open() throws SQLException {
        Log.d(tag, "Connecting to database");
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        Log.d(tag, "Disconnecting to Database");
        mDbHelper.close();
    }

    /**
     * Create a new complain report using the report object provided. If the
     * report is successfully created return the new rowId for that report,
     * otherwise return a -1 to indicate failure.
     *
     * @param logPath
     *            value to set report logpath to
     * @param state
     *            value to set report state to
     * @param createTime
     *            value to set report createTime to
     * @param category
     *            value to set report category to
     * @param freetext
     *            value to set report freetext to
     * @param showNotif
     *            whether to show notification while uploading report
     * @return rowId or -1 if failed
     */
    public long insertReport(String logPath, String state, String type, long createTime, String category,
                             String summary, String freetext, String screenshotPath, String attachment, String apVersion,
                             String bpVersion, String version, int showNotif) {
        Log.v(tag, "inserting a new report to database " + logPath);
        ContentValues initialValues = new ContentValues();
        initialValues.put(DB_TABLE_REPORTS_LOGPATH, logPath);
        initialValues.put(DB_TABLE_REPORTS_STATE, state);
        initialValues.put(DB_TABLE_REPORTS_TYPE, type);
        initialValues.put(DB_TABLE_REPORTS_CREATETIME, createTime);
        initialValues.put(DB_TABLE_REPORTS_CATEGORY, category);
        initialValues.put(DB_TABLE_REPORTS_SUMMARY, summary);
        initialValues.put(DB_TABLE_REPORTS_FREETEXT, freetext);
        initialValues.put(DB_TABLE_REPORTS_SCREENSHOT,screenshotPath);
        initialValues.put(DB_TABLE_REPORTS_ATTACHEMNT, attachment);
        initialValues.put(DB_TABLE_REPORTS_AP_VERSION, apVersion);
        initialValues.put(DB_TABLE_REPORTS_BP_VERSION, bpVersion);
        initialValues.put(DB_TABLE_REPORTS_APK_VERSION, version);
        initialValues.put(DB_TABLE_REPORTS_SHOW_NOTIFICATION, showNotif);
        return mDb.insert(DB_TABLE_REPORTS, null, initialValues);
    }

    /**
     * Update the report using the details provided. The report to be updated is
     * specified using the rowId, and it is altered to use the report object
     * values passed in
     *
     * @param rowId
     *            id of report to update
     * @param logPath
     *            value to set report logpath to
     * @param state
     *            value to set report state to
     * @param createTime
     *            value to set report createTime to
     * @param category
     *            value to set report category to
     * @param freetext
     *            value to set report freetext to
     * @return true if the report was successfully updated, false otherwise
     */
    public boolean updateReport(long rowId, String logPath, String state, String type, long createTime,
                                String category, String summary, String freetext, String screenshotPath, String attachment,
                                String apVersion, String bpVersion, String apkVersion) {
        Log.v(tag, "updating report : " + rowId);
        ContentValues args = new ContentValues();
        args.put(DB_TABLE_REPORTS_LOGPATH, logPath);
        args.put(DB_TABLE_REPORTS_STATE, state);
        args.put(DB_TABLE_REPORTS_TYPE, type);
        args.put(DB_TABLE_REPORTS_CREATETIME, createTime);
        args.put(DB_TABLE_REPORTS_CATEGORY, category);
        args.put(DB_TABLE_REPORTS_SUMMARY, summary);
        args.put(DB_TABLE_REPORTS_FREETEXT, freetext);
        args.put(DB_TABLE_REPORTS_SCREENSHOT, screenshotPath);
        args.put(DB_TABLE_REPORTS_ATTACHEMNT, attachment);
        args.put(DB_TABLE_REPORTS_AP_VERSION, apVersion);
        args.put(DB_TABLE_REPORTS_BP_VERSION, bpVersion);
        args.put(DB_TABLE_REPORTS_APK_VERSION, apkVersion);
        return mDb.update(DB_TABLE_REPORTS, args, DB_TABLE_REPORTS_ID + "=" + rowId, null) > 0;
    }

    public long insertQuestion(long reportId, String type, Boolean required, String question,
                               String answers, String userAnswer) {
//        Log.v(tag, "inserting a new survey question to database for report " + reportId);
        ContentValues initialValues = new ContentValues();
        initialValues.put(DB_TABLE_SURVEY_REPORT_ID, reportId);
        initialValues.put(DB_TABLE_SURVEY_TYPE, type);
        initialValues.put(DB_TABLE_SURVEY_REQUIRED, required.toString());
        initialValues.put(DB_TABLE_SURVEY_QUESTION, question);
        initialValues.put(DB_TABLE_SURVEY_ANSWERS, answers);
        initialValues.put(DB_TABLE_SURVEY_USER_ANSWER, userAnswer);
        return mDb.insert(DB_TABLE_SURVEY, null, initialValues);
    }

    public boolean updateQuestion(long rowId, long reportId, String type, Boolean required,
                                  String question, String answers, String userAnswer) {
//        Log.v(tag, "updating question : " + rowId + " , reportId : " + reportId);
        ContentValues initialValues = new ContentValues();
        initialValues.put(DB_TABLE_SURVEY_REPORT_ID, reportId);
        initialValues.put(DB_TABLE_SURVEY_TYPE, type);
        initialValues.put(DB_TABLE_SURVEY_REQUIRED, required.toString());
        initialValues.put(DB_TABLE_SURVEY_QUESTION, question);
        initialValues.put(DB_TABLE_SURVEY_ANSWERS, answers);
        initialValues.put(DB_TABLE_SURVEY_USER_ANSWER, userAnswer);
        return mDb.update(DB_TABLE_SURVEY, initialValues, DB_TABLE_SURVEY_ID + "=" + rowId, null) > 0;
    }

    public Cursor fetchQuestions(long reportId) {
//        Log.v(tag, "fetching questions from database for report " + reportId);
        return mDb.query(DB_TABLE_SURVEY, survey_columns, DB_TABLE_SURVEY_REPORT_ID + "="
                         + reportId, null, null, null, DB_TABLE_SURVEY_ID);
    }

    public boolean deleteQuestion(long reportId) {
//        Log.v(tag, "deleting questions of a report from database : " + reportId);
        return mDb.delete(DB_TABLE_SURVEY, DB_TABLE_SURVEY_REPORT_ID + "=" + reportId, null) > 0;
    }

    /**
     * Delete the report with the given rowId
     *
     * @param rowId
     *            id of report to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteReport(long rowId) {
//        Log.v(tag, "deleting a report from database : " + rowId);
        return mDb.delete(DB_TABLE_REPORTS, DB_TABLE_REPORTS_ID + "=" + rowId, null) > 0;
    }

    /**
     * Return a Cursor over the list of all reports in the database
     *
     * @return Cursor over all reports
     */
    public Cursor fetchAllReports() {
//        Log.v(tag, "fetching reports from database ");
        return mDb.query(DB_TABLE_REPORTS, reports_columns, null, null, null, null, DB_TABLE_REPORTS_CREATETIME);
    }

    /**
     * Return a Cursor over the list of all reports in the database
     *
     * @return Cursor over all reports
     */
    public Cursor fetchReportsByState(ComplainReport.State ... states) {
//        Log.v(tag, "fetching unsent reports from database ");
        StringBuilder sb = null;
        if(states != null && states.length > 0){
            sb = new StringBuilder(DB_TABLE_REPORTS_STATE).append(" in (");

            for(int i=0; i<states.length; i++){
                if( i != 0)
                    sb.append(",");
                sb.append("'").append(states[i].name()).append("'");
            }
            sb.append(")");
        }
        Cursor mCursor = mDb.query(true,
                DB_TABLE_REPORTS,
                reports_columns,
                sb == null ? null : sb.toString(),
                null,
                null,
                null,
                DB_TABLE_REPORTS_PRIORITY +", " + DB_TABLE_REPORTS_CREATETIME + " desc",
                null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    public Cursor fetchReportsByState(int paused, ComplainReport.State ... states) {
        StringBuilder sb = new StringBuilder();
        sb.append(DB_TABLE_REPORTS_UPLOAD_PAUSED).append("=").append(paused);
        if(states != null && states.length > 0){
            sb.append(" and ").append(DB_TABLE_REPORTS_STATE).append(" in (");

            for(int i=0; i<states.length; i++){
                if( i != 0)
                    sb.append(",");
                sb.append("'").append(states[i].name()).append("'");
            }
            sb.append(")");
        }
        Cursor mCursor = mDb.query(true,
                DB_TABLE_REPORTS,
                reports_columns,
                sb == null ? null : sb.toString(),
                null,
                null,
                null,
                DB_TABLE_REPORTS_PRIORITY +", " + DB_TABLE_REPORTS_CREATETIME + " desc",
                null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    /**
     * Return a Cursor positioned at the report that matches the given rowId
     *
     * @param rowId
     *            id of report to retrieve
     * @return Cursor positioned to matching report, if found
     * @throws SQLException
     *             if report could not be found/retrieved
     */
    public Cursor fetchReport(long rowId) throws SQLException {
//        Log.v(tag, "fetching report by id from database : " + rowId);
        Cursor mCursor = mDb.query(true, DB_TABLE_REPORTS, reports_columns, DB_TABLE_REPORTS_ID
                                   + "=" + rowId, null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }

    /**
     * Update the report's state and uploadId using the parameters provided. The
     * report to be updated is specified using the rowId, and it is altered to
     * use the report object values passed in
     *
     * @param rowId
     *            id of report to update
     * @param state
     *            value to set report state to
     * @param uploadId
     *            value to set report uploadId to
     * @return true if the report was successfully updated, false otherwise
     */
    public boolean updateReportState(ComplainReport report,  boolean updatePriority) {
        Log.v(tag, new StringBuilder("update report state").append(report.getId())
              .append(", state :")
              .append(report.getState().name())
              .append(", uploadId : ")
              .append(report.getUploadId())
              .toString());
        ContentValues args = new ContentValues();
        args.put(DB_TABLE_REPORTS_LOGPATH, report.getLogPath());
        args.put(DB_TABLE_REPORTS_STATE, report.getState().name());
        args.put(DB_TABLE_REPORTS_UPLOADID, report.getUploadId());
        if(updatePriority)
            args.put(DB_TABLE_REPORTS_PRIORITY, report.getPriority());
        args.put(DB_TABLE_REPORTS_UPLOADEDBYTES, report.getUploadedBytes());
        args.put(DB_TABLE_REPORTS_UPLOAD_PAUSED, report.getUploadPaused());

        return mDb.update(DB_TABLE_REPORTS,
                args,
                DB_TABLE_REPORTS_ID + "=" + report.getId(),
                null) > 0;
    }

    public void movePriority(int from, int to) {
        if(from == to)
            return;
        Log.v(tag, "movePriority from " + from + " to " + to);
        ContentValues args = new ContentValues();
        args.put(DB_TABLE_REPORTS_PRIORITY, 0-to);
        mDb.update(DB_TABLE_REPORTS, args, DB_TABLE_REPORTS_PRIORITY + "=" + from, null);
        if(to < from){
            mDb.execSQL("update complain_reports set priority=priority+1 where priority>=" + to +
                    " and priority<" + from);
        }else if(to > from){
            mDb.execSQL("update complain_reports set priority=priority-1 where priority<=" + to +
                    " and priority>" + from);
        }
        args.put(DB_TABLE_REPORTS_PRIORITY, to);
        mDb.update(DB_TABLE_REPORTS, args, DB_TABLE_REPORTS_PRIORITY + "=-" + to, null);
    }

    public boolean updateReportPaused(int paused, long ... rowIds){
        StringBuilder sb = null;
        if(rowIds != null && rowIds.length > 0){
            sb = new StringBuilder(DB_TABLE_REPORTS_ID).append(" in (");
            for(int i=0; i<rowIds.length; i++){
                if( i != 0)
                    sb.append(",");
                sb.append(rowIds[i]);
            }
            sb.append(")");
        }
        ContentValues args = new ContentValues();
        args.put(DB_TABLE_REPORTS_UPLOAD_PAUSED, paused);
        return mDb.update(DB_TABLE_REPORTS, args, sb == null ? null : sb.toString(), null) > 0;
    }

    /**
     * Update the report's state
     *
     * @param rowId
     *            id of report to update
     * @param state
     *            value to set report state to
     * @return true if the report was successfully updated, false otherwise
     */
    public boolean updateReportState(long rowId, String state) {
        Log.v(tag, "update report state " + rowId + ", state :" + state);
        ContentValues args = new ContentValues();
        args.put(DB_TABLE_REPORTS_STATE, state);
        return mDb.update(DB_TABLE_REPORTS, args, DB_TABLE_REPORTS_ID + "=" + rowId, null) > 0;
    }
}
