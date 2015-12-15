package com.tronxyz.bug_report.log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;

import android.util.Log;

import com.tronxyz.bug_report.conf.bean.Scenario.Filter.Entry;
import com.tronxyz.bug_report.conf.bean.Scenario.Filter.Exec;
import com.tronxyz.bug_report.model.DeamEvent;

public class ScenarioFilter {
    public static final String TAG = "BugReportScenarioFilter";
    private List<String> mMatches;

    public ScenarioFilter(){
        mMatches = new ArrayList<String>();
    }

    public boolean doEntryFilter(Entry entryFilter, DeamEvent event) throws IOException {
        if(entryFilter == null || entryFilter.regexs.size() == 0)
            return true;

        StringBuilder regexs = new StringBuilder("");
        for(int i=0; i < entryFilter.regexs.size(); i++){
            String regex = entryFilter.regexs.get(i);
            if(regex != null && !regex.isEmpty()){
                if(regexs.length() > 0)
                    regexs.append("|");
                regexs.append(regex);
            }
        }

        Scanner scanner = new Scanner(event.createEntryInputStream());
        try{
            Pattern pattern =  Pattern.compile(regexs.toString());
            Matcher matcher = null;
            while(scanner.hasNextLine()){
                String line = scanner.nextLine();
                matcher = pattern.matcher(line);
                if(matcher.find()){
                    mMatches.add(regexs.toString());
                    return true;
                }
            }
        }finally{
            scanner.close();
        }

        return false;
    }

    public boolean doExecFilter(List<Exec> execs){
        if(execs == null || execs.isEmpty())
            return true;
        DefaultExecutor executor = new DefaultExecutor();
        for(Exec exec : execs){
            int retVal;
            try {
                retVal = executor.execute(exec.cmd);
            } catch (ExecuteException e) {
                retVal = e.getExitValue();
            } catch (IOException e) {
                Log.e(TAG, String.format("Error executing %s", exec.cmd.toString()), e);
                return false;
            }
            Log.d(TAG, String.format("Executed '%s', retVal : %d, expected retVal : %s",
                    exec.cmd.toString(), retVal, exec.ret_val));
            if(!String.valueOf(retVal).equals(exec.ret_val))
                return false;
        }
        return true;
    }

    public List<String> getMatches(){
        return mMatches;
    }
}
