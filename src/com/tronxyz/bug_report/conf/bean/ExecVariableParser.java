package com.tronxyz.bug_report.conf.bean;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.environment.EnvironmentUtils;

import android.util.Log;

import com.tronxyz.bug_report.Constants;
import com.tronxyz.bug_report.conf.bean.Scenario.Parser.VariableParser;
import com.tronxyz.bug_report.model.DeamEvent;

public class ExecVariableParser extends VariableParser{
    private static final String TAG = "BugReportExecVariableParser";
    public CommandLine mCmdLine;

    public ExecVariableParser(String program){
        mCmdLine = new CommandLine(program);
    }

    protected InputStream createInputStream(DeamEvent event) throws IOException {
        // Create a pipe for data sent out by CommandThread.
        PipedOutputStream pos = new PipedOutputStream();
        // Receive data in this thread from other end of the pipe.
        PipedInputStream pis = null;
        try {
            pis = new PipedInputStream(pos);
        } catch (IOException e) {
            // We have to close this here, because we are not going to run CommandThread when
            // a failure occurs, and the caller isn't aware of its existence.
            try {if (pos != null) pos.close();} catch (IOException e2) {/*Ignore*/}
            throw e;
        }
        // Run the command to generate output in another thread
        new CommandThread(pos).start();
        return pis;
    }

    class CommandThread extends Thread{
        private OutputStream mOutputStream;
        public CommandThread(OutputStream os){
            mOutputStream = os;
        }
        public void run(){
            try{
                //prepare the output stream
                PumpStreamHandler psh = new PumpStreamHandler(mOutputStream);
                //prepare the executor
                DefaultExecutor exec = new DefaultExecutor();
                exec.setStreamHandler(psh);
                exec.setWatchdog(new ExecuteWatchdog(Constants.BUGREPORT_EXEC_TIMEOUT));
                Log.d(TAG, "Executing " + mCmdLine);
                //start to run the command
                exec.execute(mCmdLine, EnvironmentUtils.getProcEnvironment());
            } catch (Exception e) {
                Log.e(TAG, "Error executing : " + mCmdLine.toString(), e);
            }finally{
                try{ if(mOutputStream != null) mOutputStream.close(); }catch(Exception e){}
            }
        }
    }
}
