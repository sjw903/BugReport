package com.qiku.bug_report.conf.bean;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.environment.EnvironmentUtils;

import android.text.TextUtils;
import android.util.Log;

import com.qiku.bug_report.BugReportException;
import com.qiku.bug_report.Constants;
import com.qiku.bug_report.conf.bean.Scenario.Actions.Attach;
import com.qiku.bug_report.model.DeamEvent;

public class ExecAttach extends Attach {
    private static String TAG = "BugReportExecAttach";
    public CommandLine mCmdLine;
    public String mRegex;
    public String mOutputFileName;
    public int mTimeout; //in seconds

    public ExecAttach(String program){
        mCmdLine = new CommandLine(program);
    }

    public void generateAttach(DeamEvent event, File outputDir, Map<String, String> parsedVars)
            throws BugReportException {
        //deam.tag.scenario.actions.attach.exec.cmd
        try {
            if (TextUtils.isEmpty(mCmdLine.getExecutable()))
                throw new BugReportException("No program specified");

            if (TextUtils.isEmpty(mOutputFileName)) {
                // Reroute stdout/stderr to nowhere.
                executeCmd(new NullOutputStream(), outputDir, event.getTimeMillis(), parsedVars);
            } else {
                File outputFile = new File(outputDir + File.separator + mOutputFileName);
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(outputFile);
                    if (TextUtils.isEmpty(mRegex)) {
                        // Reroute stdout/stderr directly to a file.
                        executeCmd(new BufferedOutputStream(fos), outputDir, event.getTimeMillis(),
                                parsedVars);
                    } else { //deam.tag.scenario.actions.attach.exec.regex
                        // Reroute stdout/stderr to grep, which will write to a file.  To do this,
                        // we have to run the cmd in another thread, and pipe the output back to
                        // grep in this thread.
                        PipedOutputStream pos = null;
                        PipedInputStream pis = null;
                        try {
                            pos = new PipedOutputStream();
                            pis = new PipedInputStream(pos);
                            new CommandThread(event, outputDir, pos, parsedVars).start();
                            grep(mRegex, pis, fos);
                        } finally {
                            try {if (pos != null) pos.close();} catch (IOException e) {/*Ignore*/}
                            try {if (pis != null) pis.close();} catch (IOException e) {/*Ignore*/}
                        }
                    }
                } finally {
                    try {if (fos != null) fos.close();} catch (IOException e) {/*Ignore*/}
                }

                //deam.tag.scenario.actions.attach.exec.size
                if (outputFile.exists() && this.size != null &&
                        outputFile.length() > this.size.length) {
                    truncate(outputFile, this.size.priority, this.size.length);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error capturing output from " + mCmdLine.toString(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private void executeCmd(OutputStream stdoutAndErr, File outputDir, long timestamp,
            Map<String, String> parsedVars) {
        try {
            @SuppressWarnings("rawtypes")
			Map env = EnvironmentUtils.getProcEnvironment();
            env.put(Constants.BUGREPORT_EXEC_VARIABLE_TIMESTAMP, java.lang.Long.toString(timestamp));
            env.put(Constants.BUGREPORT_EXEC_VARIABLE_OUTPUT_DIR, outputDir.getAbsolutePath());
            env.putAll(parsedVars);

            DefaultExecutor exec = new DefaultExecutor();
            exec.setStreamHandler(new PumpStreamHandler(stdoutAndErr));
            if(mTimeout > 0)
                exec.setWatchdog(new ExecuteWatchdog(mTimeout * 1000));

            Log.d(TAG, "Executing " + mCmdLine.toString() + ", timeout=" + mTimeout
                    + "\nEnv: " + env);
            exec.setWorkingDirectory(new File(outputDir.getAbsolutePath()));
            exec.execute(mCmdLine, env);
        } catch (Exception e) {
            Log.e(TAG, "Error executing " + mCmdLine.toString(), e);
        }
    }

    public class NullOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
            // Do nothing.
        }
        @Override
        public void write(byte[] b) throws IOException {
            // Do nothing.
        }
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            // Do nothing.
        }
    }

    class CommandThread extends Thread {
        private DeamEvent mEvent;
        private File mOutputDir;
        private OutputStream mStdoutAndErr;
        Map<String, String> mParsedVars;
        public CommandThread(DeamEvent event, File outputDir, OutputStream os,
                Map<String, String> parsedVars) {
            mEvent = event;
            mOutputDir = outputDir;
            mStdoutAndErr = os;
            mParsedVars = parsedVars;
        }
        public void run() {
            try {
                executeCmd(mStdoutAndErr, mOutputDir, mEvent.getTimeMillis(), mParsedVars);
            } finally {
                // We have to close this from here, because the main thread will not unblock as
                // long as it's open.
                try {if (mStdoutAndErr != null) mStdoutAndErr.close();} catch (IOException e) {}
            }
        }
    }
}
