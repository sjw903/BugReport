package com.tronxyz.bug_report.conf.bean;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.exec.CommandLine;

import android.util.Log;

import com.tronxyz.bug_report.BugReportException;
import com.tronxyz.bug_report.model.DeamEvent;

public class Scenario {
    private static final String TAG = "BugReportScenario";
    public String mName;
    public Parser mParser;
    public Filter mFilter;
    public Actions mActions;
    public boolean mShowNotification = true;

    public static class Parser{
        public List<VariableParser> mParsers;

        public Parser(){
            mParsers = new ArrayList<VariableParser>();
        }

        public Map<String, String> parse(DeamEvent event){
            Map<String, String> parsedVars = new HashMap<String, String>();
            for(VariableParser parser : mParsers){
                parsedVars.putAll(parser.parseVariables(event));
            }
            Log.d(TAG, "Variables parsed :" + parsedVars);
            return parsedVars;
        }

        public static abstract class VariableParser extends ParentAware<Scenario>{
            public Pattern mPattern;
            public List<String> mVarNames = new ArrayList<String>();

            public Map<String, String> parseVariables(DeamEvent event) {
                Map<String, String> variables = new HashMap<String, String>();
                BufferedReader br = null;
                InputStream is = null;
                try {
                    is = createInputStream(event);
                    br = new BufferedReader(new InputStreamReader(is));
                    String line = br.readLine();
                    while(line!=null){
                        Matcher m = mPattern.matcher(line);
                        //use the first matches
                        if ( m.find() ){
                            for(int i=0; i<mVarNames.size() && mVarNames.size() <= m.groupCount(); i++){
                                variables.put(mVarNames.get(i), m.group(i + 1));
                            }
                            break;
                        }
                        line = br.readLine();
                    }
                }catch(Exception e){
                    Log.e(TAG, "Parsing failed", e);
                }finally{
                    try {if (is != null) is.close();} catch (IOException e) {/*Ignore*/}
                    try {if (br != null) br.close();} catch (IOException e) {/*Ignore*/}
                }
                return variables;
            }

            protected abstract InputStream createInputStream(DeamEvent event) throws IOException;
        }
    }

    public static class Filter {
        public Entry entry;
        public List<Exec> execs;
        public Filter(){
            execs = new ArrayList<Exec>();
        }
        public static class Entry{
            public List<String> regexs;
            public Entry(){
                regexs = new ArrayList<String>();
            }
        }
        public static class Exec{
            public CommandLine cmd;
            public String ret_val;
        }
    }

    public static class Actions extends ParentAware<Scenario>{
        public List<Attach> attaches;

        public Actions(){
            attaches = new ArrayList<Attach>();
        }

        public static abstract class Attach extends ParentAware<Actions>{
            public Size size;
            public static class Size{
                public long length = 0;
                public Priority priority = Priority.head;
                public enum Priority{
                    head,
                    tail;
                    public static Priority toPriority(String name){
                        try{
                            return Priority.valueOf(name.toLowerCase());
                        }catch(Exception e){
                            return head;
                        }
                    }
                }
            }

            // TODO Change this to a simple createInputStream() and put common handling here.
            public abstract void generateAttach(DeamEvent event, File outputDir,
                    Map<String, String> parsedVars) throws BugReportException;

            protected void grep(String regex, InputStream is, OutputStream os) throws IOException {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
                try {
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = null;
                    String line;
                    while (null != (line = br.readLine())) {
                        matcher = pattern.matcher(line);
                        if(matcher.find()){
                            bw.write(line);
                            bw.newLine();
                        }
                    }
                }finally{
                    br.close();
                    bw.close();
                }
            }

            protected void truncate(File file, Size.Priority priority, long maxLength)
                    throws IOException, BugReportException {
                // TODO Optimize buffering of input stream.
                File tmpFile = new File(file.getAbsolutePath() + ".tmp");
                BufferedOutputStream dstStream = null;
                RandomAccessFile srcRandomFile = null;
                try{
                    srcRandomFile = new RandomAccessFile(file,"rwd");
                    long truncateLength = file.length() - maxLength;

                    dstStream = new BufferedOutputStream(new FileOutputStream(tmpFile));
                    if(Size.Priority.tail.equals(priority)){
                        srcRandomFile.skipBytes((int)truncateLength);
                        dstStream.write(String.format(
                                "=== BugReport removed %d bytes from head ===\n",
                                truncateLength).getBytes());
                    }

                    byte[] buffer = null;
                    if(maxLength > 1024){
                        buffer = new byte[1024];
                    }else{
                        buffer = new byte[(int)maxLength];
                    }

                    long totalRead = 0;
                    int read = srcRandomFile.read(buffer);
                    while(read >= 0){
                        dstStream.write(buffer, 0, read);
                        totalRead += read;
                        if(totalRead >=  maxLength)
                            break;
                        read = srcRandomFile.read(buffer);
                    }

                    if(Size.Priority.head.equals(priority)){
                        dstStream.write(String.format(
                                "\n=== BugReport removed %d bytes from tail ===",
                                truncateLength).getBytes());
                    }

                    // Replace the original file with the truncated one.
                    if (!file.delete())
                        throw new BugReportException("Couldn't delete " + file.toString());
                    if (!tmpFile.renameTo(file))
                        throw new BugReportException("Couldn't rename " + tmpFile.toString());
                }finally{
                    if(dstStream != null) try{ dstStream.close(); }catch(Exception e){}
                    if(srcRandomFile != null) try{ srcRandomFile.close(); }catch(Exception e){}
                }
            }

            protected void copyFile(File src, File dst) throws IOException {
                // TODO Combine with the Util method
                // TODO Use NIO (FileChannel) methods for copying
                BufferedOutputStream dstStream = null;
                BufferedInputStream srcStream = null;
                try{
                    srcStream = new BufferedInputStream(new FileInputStream(src));
                    dstStream = new BufferedOutputStream(new FileOutputStream(dst));
                    int tmp;
                    while ((tmp = srcStream.read()) != -1) {
                        dstStream.write(tmp);
                    }
                }finally{
                    if(dstStream != null) try{ dstStream.close(); }catch(Exception e){}
                    if(srcStream != null) try{ srcStream.close(); }catch(Exception e){}
                }
            }
        }
    }
}
