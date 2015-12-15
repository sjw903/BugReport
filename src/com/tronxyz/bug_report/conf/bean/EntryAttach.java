package com.tronxyz.bug_report.conf.bean;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import android.text.TextUtils;
import android.util.Log;

import com.tronxyz.bug_report.BugReportException;
import com.tronxyz.bug_report.conf.bean.Scenario.Actions.Attach;
import com.tronxyz.bug_report.helper.Util;
import com.tronxyz.bug_report.model.DeamEvent;

public class EntryAttach extends Attach {
    private static final String TAG = "BugReportEntryAttach";
    public String mRegex;
    public void generateAttach(DeamEvent event, File outputDir, Map<String, String> parsedVars)
            throws BugReportException {
        InputStream is = null;
        try{
            is = event.createEntryInputStream();
            File outputFile = null;
            if(event.isEntryText()){
                outputFile = new File(outputDir.getAbsolutePath() + File.separator + "entry.txt");
            }else{
                outputFile = new File(outputDir.getAbsolutePath() + File.separator + "entry");
            }

            // Write to disk first, so that we can truncate later if needed.
            if (TextUtils.isEmpty(mRegex)) {
                Util.saveDataToFile(new BufferedInputStream(is), outputFile.getAbsolutePath());
            } else {
            //deam.tag.scenario.actions.attach.file.regex
                if (!event.isEntryText())
                    throw new BugReportException("Cannot grep non-text entry");
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(outputFile);
                    grep(mRegex, is, fos);
                } finally {
                    try {if (fos != null) fos.close();} catch (IOException e) {/*Ignore*/}
                }
            }

            //deam.tag.scenario.actions.attach.file.size
            if(this.size != null && outputFile.length() > this.size.length){
                truncate(outputFile, this.size.priority, this.size.length);
            }
        }catch(Exception e){
            Log.e(TAG, "Error processing entry", e);
        }finally{
            try {if (is != null) is.close();} catch (IOException e) {/*Ignore*/}
        }
    }
}
