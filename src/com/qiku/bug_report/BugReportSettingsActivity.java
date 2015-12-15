package com.qiku.bug_report;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;

import com.qiku.bug_report.R;
import com.qiku.bug_report.helper.Util;
import com.qiku.bug_report.model.UserSettings;
import com.qiku.bug_report.ui.SetupWizard;
import com.qiku.bug_report.upload.ReliableUploader;

public class BugReportSettingsActivity extends PreferenceActivity implements OnPreferenceChangeListener {
    private static final String TAG = "BugReportSettingsActivity";
    //Summary
    private TaskMaster mTaskMaster;
    private Preference mContactinfo;
    private ListPreference mAttachScreenshot;
    private CheckBoxPreference mCollectLocationCheckbox;
    private CheckBoxPreference mAutoUploadCheckbox;
    private CheckBoxPreference mMobileUploadAllowedCheckbox;
    private SeekBarPreference mBatteryLevelSeek;
    private CheckBoxPreference mAutoReportCheckbox;

    @SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "BugReport Settings activity onCreate ");
        super.onCreate(savedInstanceState);
        // Load the XML preferences file
        addPreferencesFromResource(R.xml.bugreport_settings);

        mTaskMaster = ((BugReportApplication) getApplicationContext()).getTaskMaster();
        mContactinfo = (Preference)getPreferenceScreen().findPreference("settings_contact_category");
        mAttachScreenshot = (ListPreference)getPreferenceScreen().findPreference("settings_add_screenshot");
        mCollectLocationCheckbox = (CheckBoxPreference)getPreferenceScreen().findPreference("settings_checkbox_collect_location");
        mAutoUploadCheckbox = (CheckBoxPreference)getPreferenceScreen().findPreference("settings_checkbox_auto_upload");
        mMobileUploadAllowedCheckbox = (CheckBoxPreference)getPreferenceScreen().findPreference("settings_checkbox_mobile_upload");
        mBatteryLevelSeek = (SeekBarPreference)getPreferenceScreen().findPreference("settings_battery_level");
        mAutoReportCheckbox =(CheckBoxPreference)getPreferenceScreen().findPreference("settings_checkbox_auto_report");
        addListeners();

        initData();
    }

    private void initData() {
        UserSettings settings = mTaskMaster.getConfigurationManager().getUserSettings();
        if (settings == null) {
            mAutoReportCheckbox.setChecked(Constants.DEFAULT_AUTO_REPORT);
            mAttachScreenshot.setValueIndex(1);
            mAttachScreenshot.setSummary(getEntryByEntryValue(mAttachScreenshot, mAttachScreenshot.getEntryValues()[1]));
            mCollectLocationCheckbox.setChecked(Constants.DEFAULT_COLLECT_LOCATION);
            mAutoUploadCheckbox.setChecked(Constants.DEFAULT_AUTO_UPLOAD);
            mMobileUploadAllowedCheckbox.setChecked(Constants.DEFAULT_MOBILE_UPLOAD_ALLOWED);
            mBatteryLevelSeek.setProgress(Constants.DEFAULT_BATTERY_PERCENT);
            return;
        } else {
            mAutoReportCheckbox.setChecked(settings.isAutoReportEnabled().getValue());
            mAutoReportCheckbox.setEnabled(settings.isAutoReportEnabled().isEditable());
            //set the screenshot attachment option
            mAttachScreenshot.setValue(String.valueOf(settings.getAttachScreenshot()));
            mAttachScreenshot.setSummary(getEntryByEntryValue(mAttachScreenshot, mAttachScreenshot.getValue()));
            mAttachScreenshot.setEnabled(settings.getAttachScreenshot().isEditable());

            mCollectLocationCheckbox.setChecked(settings.getCollectUserLocation().getValue());
            mCollectLocationCheckbox.setEnabled(settings.getCollectUserLocation().isEditable());

            mAutoUploadCheckbox.setChecked(settings.isAutoUploadEnabled().getValue());
            mAutoUploadCheckbox.setEnabled(settings.isAutoUploadEnabled().isEditable());

//            mMobileUploadAllowedCheckbox.setChecked(settings.getMobileUploadAllowed().getValue());
//            mMobileUploadAllowedCheckbox.setEnabled(settings.getMobileUploadAllowed().isEditable());

            mBatteryLevelSeek.setProgress(settings.getBatteryPercent().getValue());
            mBatteryLevelSeek.setEnabled(settings.getBatteryPercent().isEditable());
        }
    }

    private void addListeners() {
        mContactinfo.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        startActivity(new Intent(BugReportSettingsActivity.this, SetupWizard.class));
                        return true;
                    }
                });

        mAutoUploadCheckbox.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                UserSettings settings = mTaskMaster.getConfigurationManager().getUserSettings();
                settings.setAutoUploadEnabled(Boolean.valueOf(newValue.toString()));
                //mTaskMaster.getConfigurationManager().saveUserSettings(settings);

                if(settings.isAutoUploadEnabled().getValue()){
                    //start the upload service
                    startService(new Intent(BugReportSettingsActivity.this, ReliableUploader.class));
                } else {
                    //stop the upload service
                    sendBroadcast(new Intent(Constants.BUGREPORT_INTENT_PAUSE_UPLOAD));
                }
                return true;
            }
        });

        mMobileUploadAllowedCheckbox.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                //save 2g upload allowed settings
//                UserSettings settings = mTaskMaster.getConfigurationManager().getUserSettings();
//                settings.setMobileUploadAllowed(Boolean.valueOf(newValue.toString()));
                //mTaskMaster.getConfigurationManager().saveUserSettings(settings);
//                Log.d(TAG, "allow 3G/2G upload: " + settings.getMobileUploadAllowed().getValue());

//                if(settings.getMobileUploadAllowed().getValue()){
//                    //when 3g/2g upload is allowed, start the upload service
//                    Log.d(TAG, "start upload service");
//                    startService(new Intent(BugReportSettingsActivity.this, ReliableUploader.class));
//                } else {
//                    //when 3g/2g upload is not allowed and phone is connected to 3g/2g, stop the upload service
//                    if(Util.is2GConnected(BugReportSettingsActivity.this)
//                       || Util.is2GConnected(BugReportSettingsActivity.this)){
//                        Log.d(TAG, "stop upload service since uploading on 3G/2G is not allowed");
//                        sendBroadcast(new Intent(Constants.BUGREPORT_INTENT_PAUSE_UPLOAD));
//                    }
//                }
                return true;
            }
        });

        mAttachScreenshot.setOnPreferenceChangeListener(this);

        mCollectLocationCheckbox.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                //save auto report allowed setting
                UserSettings settings = mTaskMaster.getConfigurationManager().getUserSettings();
                settings.setCollectUserLocation(Boolean.valueOf(newValue.toString()));
                //mTaskMaster.getConfigurationManager().saveUserSettings(settings);
                Log.d(TAG, "allow Collect User Location: " + settings.isAutoReportEnabled().getValue());
                return true;
            }
        });

        mAutoReportCheckbox.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                //save auto report allowed setting
                UserSettings settings = mTaskMaster.getConfigurationManager().getUserSettings();
                settings.setAutoReportEnabled(Boolean.valueOf(newValue.toString()));
                //mTaskMaster.getConfigurationManager().saveUserSettings(settings);
                Log.d(TAG, "allow auto report: " + settings.isAutoReportEnabled().getValue());
                return true;
            }
        });
    }

    private void saveUserSettings(){
        UserSettings settings = getSettings();
        mTaskMaster.getConfigurationManager().saveUserSettings(settings);
    }

    public void onPause(){
        Log.d(TAG, "onPause()");
        saveUserSettings();
        super.onPause();
    }

    private UserSettings getSettings() {
        UserSettings settings = mTaskMaster.getConfigurationManager().getUserSettings();
        if (settings == null) {
            settings = new UserSettings();
        }
        return settings;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if(newValue == null)
            return true;

        UserSettings settings = mTaskMaster.getConfigurationManager().getUserSettings();
        if(preference instanceof ListPreference){
            CharSequence entry = getEntryByEntryValue((ListPreference)preference, newValue);
            if(entry != null){
                preference.setSummary(entry);
                settings.setAttachScreenshot(newValue.toString());
                //mTaskMaster.getConfigurationManager().saveUserSettings(settings);
            }
        }
        return true;
    }

    private CharSequence getEntryByEntryValue(ListPreference preference, Object entryValue){
        CharSequence[] entryValues = preference.getEntryValues();
        for(int i=0; entryValues!=null && i<entryValues.length; i++){
            if(entryValues[i].equals(entryValue.toString().toUpperCase())){
                return (preference).getEntries()[i];
            }
        }
        return null;
    }
}
