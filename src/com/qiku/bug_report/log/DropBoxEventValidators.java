package com.qiku.bug_report.log;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.qiku.bug_report.conf.bean.Deam;
import com.qiku.bug_report.conf.bean.Deam.Tag;
import com.qiku.bug_report.conf.bean.Deam.Tag.Type;

public final class DropBoxEventValidators {
    private static final String TAG = "BugReportDropBoxEventValidators";
    private SharedPreferences mPreferences;

    public DropBoxEventValidators(Context context){
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    private class EventValidator{
        protected String mTag;
        protected int mPeriodMills;
        protected int mMaxOccurrence;
        private Queue<Long> mTimestampQueue = new LinkedList<Long>();

        protected EventValidator(String tag, int periodMills, int maxOccurrence){
            mTag = tag;
            mPeriodMills = periodMills;
            mMaxOccurrence = maxOccurrence;
        }

        protected void update(int periodMills, int maxOccurrence){
            //Empty the queue if the values have changed, otherwise the queue checks may no
            //no longer work.
            if (mPeriodMills != periodMills || mMaxOccurrence != maxOccurrence)
                mTimestampQueue.clear();
            mPeriodMills = periodMills;
            mMaxOccurrence = maxOccurrence;
        }

        // Allow only up to mMaxOccurrence in any mPeriodMills timespan.  Any
        // entry that exceeds this will be dropped and lost forever.
        public boolean exceedsMaxOccurrence(long latestTimeMills){
            //Restore the recent event timestamps from preferences to mTimestampQueue if
            //mTimestampQueue is empty but preferences isn't. This can happen if BugReport
            //is killed by user or system and then restarted
            if(mTimestampQueue.isEmpty()){
                String recentEventTimesInFile = mPreferences.getString(mTag, null);
                if(!TextUtils.isEmpty(recentEventTimesInFile)){
                    String[] recentEventTimeArray = recentEventTimesInFile.split(",");
                    if(recentEventTimeArray != null){
                        for(String eventTime : recentEventTimeArray){
                            mTimestampQueue.add(Long.parseLong(eventTime));
                        }
                    }
                }
            }

            boolean exceeded = false;
            long oldestTimeMills = (null == mTimestampQueue.peek()) ? 0 : mTimestampQueue.peek();
            long deltaTimeMills = latestTimeMills - oldestTimeMills;
            //Empty the queue if the system clock moves backwards.
            if (latestTimeMills < oldestTimeMills) {
                mTimestampQueue.clear();
            }
            // See whether we should replace the oldest entry with the latest.
            if (mTimestampQueue.size() == mMaxOccurrence) {
                if (deltaTimeMills < mPeriodMills) {
                    exceeded = true;
                    Log.w(TAG, "Ignoring tag="+mTag+" time="+latestTimeMills+" which exceeds "+
                            mMaxOccurrence + " occurrence(s) in " + mPeriodMills / 1000 + " secs");
                } else {
                    // Remove the oldest timestamp from the queue to make room for the new one.
                    mTimestampQueue.poll();
                }
            }
            if (!exceeded) {
                mTimestampQueue.add(latestTimeMills);
                //Always persist the recent event timestamps(comma-separated) to preference file,
                //so that we won't lose these recent event timestamps in case BugReport is killed
                mPreferences.edit().putString(mTag, toString(mTimestampQueue)).commit();
            }
            return exceeded;
        }

        /**
         * @param queue
         * @return a comma-separated string representing the objects in the queue
         */
        private String toString(Queue<Long> queue){
            if(queue == null)
                throw new IllegalArgumentException();
            Iterator<Long> queueIt = queue.iterator();
            StringBuilder sb = new StringBuilder();
            for(int i=0; queueIt.hasNext(); i++){
                sb.append(queueIt.next());
                if( i< (queue.size()-1) )
                    sb.append(",");
            }
            return sb.toString();
        }
    }

    //A static map that save tag --> validator
    private static Map<String, EventValidator> validators = new HashMap<String, EventValidator>();

    //call this method to validate the entry whether it should be processed
    public synchronized boolean isValid(Deam deam, String tagName, long timeMills){
        Tag tag = deam.getTag(tagName, Type.DROPBOX);
        EventValidator validator = validators.get(tagName);
        if(validator == null) {
            validator = new EventValidator(tagName, tag.mLimit.mPeriod * 1000,
                    tag.mLimit.mMaxOccurrence);
            validators.put(tagName, validator);
        } else {
            //Always update with the latest from the DEAM.
            validator.update(tag.mLimit.mPeriod * 1000, tag.mLimit.mMaxOccurrence);
        }
        return !validator.exceedsMaxOccurrence(timeMills);
    }
}
