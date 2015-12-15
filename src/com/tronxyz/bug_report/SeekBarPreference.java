package com.tronxyz.bug_report;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.tronxyz.bug_report.model.UserSettings;

public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "BugReportSeekBarPreference";
    private static final int MAX_VALUE = 100;
    private static final String PERCENT_SUFFIX = "%";
    private SeekBar mSeekBar;
    private TextView mValueText;
    private int mValue = Constants.DEFAULT_BATTERY_PERCENT;
    private int mTempValue = mValue;
    private TaskMaster mTaskMaster = null;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTaskMaster = ((BugReportApplication)context.getApplicationContext()).getTaskMaster();
    }

    @SuppressWarnings("deprecation")
	@Override
    protected View onCreateDialogView() {
        LinearLayout.LayoutParams params;
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(6, 6, 6, 6);

        mValueText = new TextView(getContext());
        mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
        mValueText.setTextSize(32);
        params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.addView(mValueText, params);

        mSeekBar = new SeekBar(getContext());
        mSeekBar.setOnSeekBarChangeListener(this);
        layout.addView(mSeekBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        if(mValue == 0)
            mValueText.setText("0%");

        mSeekBar.setMax(MAX_VALUE);
        mSeekBar.setProgress(mValue);
        return layout;
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        mSeekBar.setMax(MAX_VALUE);
        if(mValue == 0)
            mValueText.setText("0%");
        mSeekBar.setProgress(mValue);
    }

    public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
        String t = String.valueOf(value);
        mValueText.setText(PERCENT_SUFFIX == null ? t : t.concat(PERCENT_SUFFIX));
        mTempValue = value;
        callChangeListener(value);
    }

    public void onClick(DialogInterface dialog, int which){
        //save the changes only if user press the OK and the value is indeed changed
        if(DialogInterface.BUTTON_POSITIVE == which && mValue != mTempValue){
            Log.d(TAG, "Broadcasting battery threshold changes : " + mValue + " to " + mTempValue);
            mValue = mTempValue;
            //save the battery threshold to user settings
            UserSettings userSettings = mTaskMaster.getConfigurationManager().getUserSettings();
            userSettings.setBatteryPercent(mValue);
            //mTaskMaster.getConfigurationManager().saveUserSettings(userSettings);
            //broadcasts the battery threshold changes
            Intent intent = new Intent(Constants.BUGREPORT_INTENT_BATTERY_THRESHOLD_CHANGED);
            intent.putExtra(Constants.BUGREPORT_INTENT_EXTRA_BATTERY_THRESHOLD, mValue);
            getContext().sendBroadcast(intent);
        }
    }

    public void onStartTrackingTouch(SeekBar seek) {
    }

    public void onStopTrackingTouch(SeekBar seek) {
    }

    public void setProgress(int progress) {
        mValue = progress;
        mTempValue = mValue;
        if (mSeekBar != null)
            mSeekBar.setProgress(progress);
    }

    public int getProgress() {
        return mValue;
    }
}
