
package com.tronxyz.bug_report.log;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.tronxyz.bug_report.BugReportException;
import com.tronxyz.bug_report.conf.bean.Deam;
import com.tronxyz.bug_report.conf.bean.Deam.Tag;
import com.tronxyz.bug_report.conf.bean.Scenario;
import com.tronxyz.bug_report.conf.bean.Scenario.Actions.Attach;
import com.tronxyz.bug_report.helper.Notifications;
import com.tronxyz.bug_report.helper.Util;
import com.tronxyz.bug_report.model.DeamEvent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeamEventHandler {
    private static final String TAG = "BugReportDeamEventHandler";
    private static final int DESC_MAX_BYTES = 256;
    @SuppressLint("SimpleDateFormat")
    private SimpleDateFormat mTimestampFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss.SSSZ");

    /**
     * @param event the DeamEvent to be processed
     * @param deam the Deam that will be used to handle the event
     * @param outputDir the directory where the ouput kis
     * @param notify whether to show notification during processing
     * @param ctx the Android Context, this is required only if notify is true.
     * @return
     * @throws BugReportException
     */
    public Results handle(DeamEvent event, Deam deam, File outputDir, boolean notify, Context ctx)
            throws BugReportException {
        // find the Tag handler for the event
        Tag tag = deam.getTag(event.getTag(), event.getType());
        Results results = new Results();
        if (tag != null) {
            Log.d(TAG, "Handle " + event.toString() + " as DEAM tag " + tag.key);

            for (Scenario scenario : tag.mScenarios) {

                if (scenario == null)
                    continue;

                // deam.tag.scenario.parsers
                Map<String, String> parsedVars;
                if (scenario.mParser != null) {
                    parsedVars = scenario.mParser.parse(event);
                } else {
                    parsedVars = Collections.emptyMap();
                }

                // deam.tag.scenario.filters
                ScenarioFilter filter = new ScenarioFilter();
                boolean match;
                if (scenario.mFilter == null) {
                    match = true;
                } else {
                    try {
                        // deam.tag.scenario.filters.entry
                        match = filter.doEntryFilter(scenario.mFilter.entry, event);
                        if (match) {
                            // deam.tag.scenario.filters.exec
                            match = filter.doExecFilter(scenario.mFilter.execs);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error executing filter", e);
                        match = false;
                    }
                }

                if (match) {
                    try {
                        results.isHandled = true;
                        results.scenarioName = TextUtils.isEmpty(scenario.mName) ?
                                tag.key.mName : scenario.mName;
                        results.description = parseDescription(event);
                        results.processName = parseProcessName(event);
                        results.showNotification = scenario.mShowNotification;
                        // if(notify && scenario.mShowNotification)
                        // Notifications.showDEAMNotification(ctx,
                        // results.scenarioName,
                        // (int)event.getTimeMillis());

                        if (outputDir == null) {
                            // Create the log directory only when it is
                            // necessary
                            outputDir = makeLogDir(event.getTag(), event.getTimeMillis());
                            results.logDir = outputDir.getAbsolutePath();
                        }
                        // deam.tag.scenario.actions
                        Log.d(TAG, "Identified scenario: " + results.scenarioName);
                        if (scenario.mActions != null && !scenario.mActions.attaches.isEmpty()) {
                            for (Attach attach : scenario.mActions.attaches) {
                                attach.generateAttach(event, outputDir, parsedVars);
                            }
                        }

                        try {
                            // copy the system DEAM file to the log directory
                            String dstFile =
                                    outputDir.getAbsolutePath() + File.separator + "deam.xml";
                            Util.saveDataToFile(ctx.getAssets().open("deam.xml"), dstFile);
                        } catch (IOException e) {
                            Log.e(TAG, "Failed to copy deam.xml ", e);
                        }
                        return results;
                    } finally {
                        if (notify && scenario.mShowNotification)
                            Notifications.cancel(ctx, (int) event.getTimeMillis());
                    }
                }
            }
        }
        return results;
    }

    private String parseDescription(DeamEvent event) {
        if (!event.isEntryText()) {
            return "<Cannot display binary content>";
        }

        String regex = null;
        int outLineNum = 0;     // 0 means no limit for line number.
        int targetLineNum = 0;     // 0 means no limit for line number.
        if(event.getTag().contains("crash")) {
            regex = "Exception";
            targetLineNum = 6;
        }
        if(event.getTag().contains("anr")) {
            regex = "Subject:";
            outLineNum = 1;
        }
        if(regex != null) {
            Scanner scanner = null;
            try{
                scanner = new Scanner(event.createEntryInputStream());
                Pattern pattern =  Pattern.compile(regex);
                boolean isFind = false;
                String line = null;
                int tln = 0;
                while(scanner.hasNextLine()){
                    line = scanner.nextLine();
                    Matcher matcher = pattern.matcher(line);
                    if(matcher.find()){
                        isFind = true;
                        break;
                    }
                    if(targetLineNum != 0) {
                        if(++tln == targetLineNum) {
                            isFind = true;
                            break;
                        }
                    }
                }
                if(isFind) {
                    String out = line;
                    if(outLineNum != 0) {
                        int i = 1;
                        while(scanner.hasNextLine() && i < outLineNum) {
                            out = out + scanner.nextLine();
                            i++;
                        }
                    } else {
                        while(scanner.hasNextLine() && out.length() < DESC_MAX_BYTES) {
                            out = out + scanner.nextLine();
                        }
                    }
                    return out;
                }
            } catch (IOException e) {
                Log.w(TAG, "Unable to parse description from entry: " + e.getMessage());
            }finally{
                scanner.close();
            }
        }

        InputStream is = null;
        try {
            is = event.createEntryInputStream();
            byte[] data = new byte[DESC_MAX_BYTES];
            int totalBytesRead = 0;
            while (DESC_MAX_BYTES > totalBytesRead) {
                int bytesRead = is.read(data, totalBytesRead, DESC_MAX_BYTES - totalBytesRead);
                if (-1 == bytesRead) {
                    break;
                } else {
                    totalBytesRead += bytesRead;
                }
            }
            return new String(data, 0, totalBytesRead);
        } catch (IOException e) {
            Log.w(TAG, "Unable to parse description from entry: " + e.getMessage());
        } finally {
            if (is != null)
                try {
                    is.close();
                } catch (IOException e) {
                }
        }
        return null;
    }

    private String parseProcessName(DeamEvent event) {
        if (!event.isEntryText()) {
            return "";
        }

        String regex = null;
        if(event.getTag().contains("crash") || event.getTag().contains("anr")) {
            regex = "Process: (.*)";
        }
        if(regex != null) {
            Scanner scanner = null;
            try{
                scanner = new Scanner(event.createEntryInputStream());
                Pattern pattern =  Pattern.compile(regex);
                while(scanner.hasNextLine()){
                    Matcher matcher = pattern.matcher(scanner.nextLine());
                    if(matcher.find()){
                        return matcher.group(1);
                    }
                }
            } catch (IOException e) {
                Log.w(TAG, "Unable to parse description from entry: " + e.getMessage());
            }finally{
                scanner.close();
            }
        }

        return "";
    }

    private File makeLogDir(String tag, long time) throws BugReportException {
        String logHome = Util.getLogPath();
        if (logHome == null) {
            throw new BugReportException("No space left on device or storage path not writable");
        }

        // Try really hard to create a unique directory for storing this report,
        // but don't use an
        // infinite loop. Use "dropbox" to separate from other types of reports
        // (ex: user), plus
        // the full timestamp. Also add an index to help resolve collisions if
        // necessary. It's
        // unlikely that two DropBox events of the same tag would ever have the
        // same timestamp, but
        // it's possible that an invalid system clock could cause it to happen.
        String logPath = null;
        int MAX_COLLISION_ATTEMPTS = 50;

        // TODO Is this deprecated?
        String timestamp = mTimestampFormat.format(new Date(time));

        for (int i = 0; i < MAX_COLLISION_ATTEMPTS; i++) {
            logPath = logHome + File.separator + "dropbox-" + tag + "@" + timestamp;
            if (i > 0)
                logPath = logPath + "-" + i;
            File logPathFile = new File(logPath);
            if (!logPathFile.exists()) {
                if (logPathFile.mkdirs())
                    break;
                else
                    throw new BugReportException("Failed to create directory for report"
                            + " (tag=" + tag + ", ts=" + time + ")");
            }
        }

        if (null == logPath)
            throw new BugReportException("Failed to create directory for report"
                    + " (tag=" + tag + ", ts=" + time + ")"
                    + " after " + MAX_COLLISION_ATTEMPTS + " attempts");

        return new File(logPath);
    }

    public class Results {
        public boolean isHandled = false;
        public String logDir = null;
        public String scenarioName = null;
        public boolean showNotification = true;
        public String description = null;
        public String processName = null;
        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            String NEW_LINE = System.getProperty("line.separator");
            result.append(this.getClass().getName() + " Object {" + NEW_LINE);
            result.append(" isHandled: " + isHandled + NEW_LINE);
            result.append(" logDir: " + logDir + NEW_LINE);
            result.append(" scenarioName: " + scenarioName + NEW_LINE);
            result.append(" showNotification: " + showNotification + NEW_LINE);
            result.append(" description: " + description + NEW_LINE);
            result.append(" processName: " + processName + NEW_LINE);
            result.append("}");
            return result.toString();
        }
    }
}
