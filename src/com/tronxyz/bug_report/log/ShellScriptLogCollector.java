package com.tronxyz.bug_report.log;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.util.Log;

import com.tronxyz.bug_report.BugReportException;
import com.tronxyz.bug_report.Constants;
import com.tronxyz.bug_report.TaskMaster;
import com.tronxyz.bug_report.helper.Util;
import com.tronxyz.bug_report.model.ComplainReport;
import com.tronxyz.bug_report.model.ComplainReport.State;
import com.tronxyz.bug_report.model.ComplainReport.Type;

@SuppressLint("SimpleDateFormat")
public class ShellScriptLogCollector{
    private static final String TAG = "BugReportLogCollector";
    private TaskMaster mTaskMaster;
	private SimpleDateFormat mTimestampFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss.SSSZ");
    private SimpleDateFormat mOldTimestampFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    public ShellScriptLogCollector(TaskMaster taskMaster) {
        mTaskMaster = taskMaster;
    }

    public boolean removeLog(String filePath) {
        Log.d(TAG, "Deleting log file : " + filePath);
        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            return file.delete();
        }
        return false;
    }

    public void collectUntrackedReport() throws BugReportException{
        List<String> paths = searchInfoFiles();
        for(String filePath : paths){
            if(filePath != null){
                Properties reportInfo = Util.readPropertiesFromFile(filePath);
                String sVersion = reportInfo.getProperty(Constants.BUGREPORT_INTENT_PARA_VERSION, "0");
                int shellVersion = Integer.parseInt(sVersion);
                //We should skip the report and remove the .info and the log files if the version is incompatible.
                if(!Util.isShellCompatible(shellVersion)){
                    String files = reportInfo.getProperty(Constants.LOG_FILES_REMOVE_LABEL);
                    if(!TextUtils.isEmpty(files))
                        Util.removeFiles(files.replaceAll("[ ]+", " ").split(" "));
                    continue;
                }
                ComplainReport report = createReport(reportInfo);
                if(report != null){
                    Log.i(TAG, String.format("Untracked report found, %s", report.getLogPath()));
                    //save report to database
                    mTaskMaster.getBugReportDAO().saveReport(report);
                    //Remove the temporary log files and report info file for
                    //they have been correctly processed and saved to database
                    String files = reportInfo.getProperty(Constants.LOG_FILES_REMOVE_LABEL);
                    if(!TextUtils.isEmpty(files))
                        Util.removeFiles(files.replaceAll("[ ]+", " ").split(" "));
                }else{
                    Log.d(TAG, String.format("Untracked report info found but failed to create a report: %s", filePath));
                }
            }
        }
    }

    /**
     * Find the info files that name matches the given regex in the possible paths
     * @return a list of full path of the info files
     */
    private List<String> searchInfoFiles(){
        Log.d(TAG, "searchInfoFiles()");
        List<String> infoFiles = new ArrayList<String>();
        // TODO Use a dedicated directory so we don't waste time wading through other files.
        String[] possiblePaths = {
                Constants.SECURE_STORAGE_PATH,
                Constants.BUGREPORT_SDCARD_EXT_STORAGE_PATH,
                Constants.BUGREPORT_SDCARD_STORAGE_PATH};
        try {
            for(String path : possiblePaths){
                if(path == null)
                    continue;
                if(!path.endsWith(File.separator)) {
                    path += File.separator;
                }
                File dir = new File(path);
                String[] filenames = dir.list();
                if (filenames != null) {
                    for (int k = 0; k < filenames.length; k++) {
                        if (isInfoFile(filenames[k])) {
                            infoFiles.add(path+filenames[k]);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error searching untracked report info file", e);
        };
        return infoFiles;
    }

    private boolean isInfoFile(String path) {
        return "info".equals(path.substring(path.lastIndexOf('.') + 1, path.length()));
    }

    public ComplainReport createReport(Properties reportInfo) throws BugReportException{
        if(reportInfo == null || reportInfo.size() == 0)
            throw new BugReportException("Invalid report description file");
        Date date = parseTimestamp(reportInfo.getProperty(Constants.TIMESTAMP_LABEL));
        String logFiles = reportInfo.getProperty(Constants.LOG_FILES_LABEL);
        String screenshotPath = reportInfo.getProperty(Constants.LOG_SCREENSHOT_LABEL);
        return generateReport(logFiles, date, screenshotPath);
    }

    /**
    *
    * @param logFiles the log files that will be compressed into the zip file, comma separated.
    * @param time the report time
    * @param screenshot the screen capture file path
    * @return
    * @throws BugReportException
    */
    private ComplainReport generateReport(String logFiles, Date date, String screenshot) throws BugReportException{
        if(logFiles == null || logFiles.isEmpty())
            return null;
        LocationService.getInstance(mTaskMaster).saveLocationInfo(
                logFiles + File.separator +"device_location.txt");

        ComplainReport report = new ComplainReport();
        report.setState(State.WAIT_USER_INPUT);
        report.setType(Type.USER);
        report.setLogPath(logFiles);
        report.setApVersion(Util.getSystemProperty(Constants.DEVICE_PROPERTY_APVERSION, ""));
        report.setBpVersion(Util.getSystemProperty(Constants.DEVICE_PROPERTY_BPVERSION, ""));
        report.setApkVersion(Util.getAppVersionName(mTaskMaster.getApplicationContext()));

        if(screenshot!= null && new File(screenshot).exists())
            report.setScreenshotPath(screenshot);

        report.setCreateTime(date);
        return report;
    }

    private Date parseTimestamp(String timestamp) throws BugReportException {
        try {
            return mTimestampFormat.parse(timestamp);
        } catch (ParseException e) {
            try {
                return mOldTimestampFormat.parse(timestamp);
            } catch (ParseException e2) {
                throw new BugReportException("Couldn't parse timestamp from " + timestamp);
            }
        }
    }
}
