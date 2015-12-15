package com.tronxyz.bug_report.conf.bean;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import android.text.TextUtils;
import android.util.Log;

import com.tronxyz.bug_report.BugReportException;
import com.tronxyz.bug_report.conf.bean.Scenario.Actions.Attach;
import com.tronxyz.bug_report.model.DeamEvent;

public class FileAttach extends Attach {
    private static final String TAG = "BugReportFileAttach";
    public String mName;
    public String mRegex;
    public void generateAttach(DeamEvent event, File outputDir, Map<String, String> parsedVars)
            throws BugReportException {
        if(mName == null)
            return;
        File file = new File(mName);
        if(!file.exists())
            return;

        try{
            //if the file is a directory, will copy all files in this directory without recurse.
            // TODO Make this functionality a separate tag, so that "file" only means a single
            // file.
            if(file.isDirectory()){
                File[] files = file.listFiles();

                //an empty directory
                if(files == null)
                    return;

                for(File subFile : files){
                    if(subFile.isFile()){
                        File outputFile = new File(outputDir.getAbsolutePath() + File.separator +
                                subFile.getAbsolutePath().replaceAll("/", "_")
                                .replaceFirst("_", ""));
                        copyFile(subFile, outputFile);
                    }
                }
                return;
            }

            // TODO Use folders instead of changing file names.
            File outputFile = new File(outputDir.getAbsolutePath() + File.separator +
                    file.getAbsolutePath().replaceAll("/", "_").replaceFirst("_", ""));

            // Write to disk first, so that we can truncate later if needed.
            if (TextUtils.isEmpty(mRegex)) {
                copyFile(file, outputFile);
            } else {
            //deam.tag.scenario.actions.attach.file.regex
                FileInputStream fis = null;
                FileOutputStream fos = null;
                try {
                    fis = new FileInputStream(file);
                    fos = new FileOutputStream(outputFile);
                    grep(mRegex, fis, fos);
                } finally {
                    try {if (fis != null) fis.close();} catch (IOException e) {/*Ignore*/}
                    try {if (fos != null) fos.close();} catch (IOException e) {/*Ignore*/}
                }
            }

            //deam.tag.scenario.actions.attach.exec.size
            if (this.size != null && outputFile.length() > this.size.length) {
                truncate(outputFile, this.size.priority, this.size.length);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing file", e);
        }
    }
}
