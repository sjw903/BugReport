package com.tronxyz.bug_report.conf;

import android.content.Context;
import android.util.Log;

import com.tronxyz.bug_report.conf.bean.Deam;

public class DeamConfiguration extends Configurable<Deam> {
    private static final String tag = "BugReportDeamConfiguration";
    private static final String mDeamFileName = "deam.xml";
    public DeamConfiguration(Context context){
        super(context, new DeamXMLParser());
    }

    public void start(){
        try {
            mConf = getParser().parse(getContext().getAssets().open(mDeamFileName));
        } catch (Exception e) {
            Log.e(tag, "Error occured while initializing DEAM configuration", e);
        }
    }
}
