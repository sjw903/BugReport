package com.qiku.bug_report.ui;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.EditTextPreference;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.qiku.bug_report.BugReportApplication;
import com.qiku.bug_report.BugReportSettingsActivity;
import com.qiku.bug_report.Constants;
import com.qiku.bug_report.R;
import com.qiku.bug_report.SeekBarPreference;
import com.qiku.bug_report.TaskMaster;
import com.qiku.bug_report.helper.DialogHelper;
import com.qiku.bug_report.helper.Util;
import com.qiku.bug_report.model.UserSettings;
import com.qiku.bug_report.upload.ReliableUploader;

public class ReportSettingFragment extends PreferenceFragment implements OnPreferenceChangeListener{

    private Context mContext;
    private TaskMaster mTaskMaster;
    private Preference mReportManagerPreference;
    private CheckBoxPreference mCommitInWifiCheckbox;
    private CheckBoxPreference mUserPlanCheckbox;
    private Preference mReportHelpPreference;
    private Preference mReportAboutPreference;
    private ListPreference mServerAddrPreference;
    private EditTextPreference mUserNamePreference;
    private EditTextPreference mUserPhonePreference;
    private EditTextPreference mUserEmailPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        addPreferencesFromResource(R.xml.bugreport_settings);
        mTaskMaster = ((BugReportApplication) mContext.getApplicationContext()).getTaskMaster();
        mReportManagerPreference = (Preference)getPreferenceScreen().findPreference("settings_report_manager");
        if (Util.isUserVersion()) {
            getPreferenceScreen().removePreference(mReportManagerPreference);
        }
        mCommitInWifiCheckbox = (CheckBoxPreference)getPreferenceScreen().findPreference("settings_report_commit_in_wifi");
        mUserPlanCheckbox = (CheckBoxPreference)getPreferenceScreen().findPreference("settings_report_user_plan");
        mReportHelpPreference = (Preference)getPreferenceScreen().findPreference("settings_report_help");
//        mReportAboutPreference = (Preference)getPreferenceScreen().findPreference("settings_report_about");

        mServerAddrPreference = (ListPreference)getPreferenceScreen().findPreference("settings_report_server_address");
        mUserNamePreference = (EditTextPreference)getPreferenceScreen().findPreference("settings_report_user_name");
        mUserPhonePreference = (EditTextPreference)getPreferenceScreen().findPreference("settings_report_user_phone_number");
        mUserEmailPreference = (EditTextPreference)getPreferenceScreen().findPreference("settings_report_user_email");

        addListeners();
        initData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    private void addListeners() {
        mReportManagerPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // TODO Auto-generated method stub
                //进入管理报告
                mContext.startActivity(new Intent(mContext, ReportList.class));
                return true;
            }
        });

        mCommitInWifiCheckbox.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                // TODO Auto-generated method stub
                UserSettings settings = mTaskMaster.getConfigurationManager().getUserSettings();
                settings.setWlanUploadAllowed(Boolean.valueOf(newValue.toString()));
//                mTaskMaster.getConfigurationManager().saveUserSettings(settings);

                if(settings.getWlanUploadAllowed().getValue()){
                  //stop the upload service
                    if (Util.isWIFIConnected(mContext)) {
                        mContext.startService(new Intent(mContext, ReliableUploader.class));
                    } else {
                        mContext.sendBroadcast(new Intent(Constants.BUGREPORT_INTENT_PAUSE_UPLOAD));
                    }
                  //仅在WiFi下上传,2g/3g 不上传
                } else {
                  //start the upload service
                    mContext.startService(new Intent(mContext, ReliableUploader.class));
                    //移动网络和wifi都上传
                }
                return true;
            }
        });

        mUserPlanCheckbox.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                // TODO Auto-generated method stub
                UserSettings settings = mTaskMaster.getConfigurationManager().getUserSettings();
                settings.setAutoReportEnabled(Boolean.valueOf(newValue.toString()));
                //用户体验计划
                return true;
            }
        });

        mReportHelpPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // TODO Auto-generated method stub
                //进入帮助
                mContext.startActivity(new Intent(mContext, Help.class));
                return true;
            }
        });

        /*mReportAboutPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // TODO Auto-generated method stub
                //进入关于
                DialogHelper.createAboutDialog(mContext).show();
                return true;
            }
        });
        */

        mServerAddrPreference.setOnPreferenceChangeListener(this);
        mUserNamePreference.setOnPreferenceChangeListener(this);
        mUserPhonePreference.setOnPreferenceChangeListener(this);
        mUserEmailPreference.setOnPreferenceChangeListener(this);
    }

    private void initData() {
        UserSettings settings = mTaskMaster.getConfigurationManager().getUserSettings();
        if (settings == null) {
            mCommitInWifiCheckbox.setChecked(Constants.DEFAULT_WIFI_ONLY);
            mUserPlanCheckbox.setChecked(Constants.DEFAULT_AUTO_REPORT);
            return;
        } else {
            mCommitInWifiCheckbox.setChecked(settings.getWlanUploadAllowed().getValue());
            mCommitInWifiCheckbox.setEnabled(settings.getWlanUploadAllowed().isEditable());

            mUserPlanCheckbox.setChecked(settings.isAutoReportEnabled().getValue());
            mUserPlanCheckbox.setEnabled(settings.isAutoReportEnabled().isEditable());

            mServerAddrPreference.setSummary(settings.getServerAddr());
            mUserNamePreference.setSummary(settings.getFirstName());
            mUserPhonePreference.setSummary(settings.getPhone());
            mUserEmailPreference.setSummary(settings.getEmail());
        }
    }

    @Override
    public void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        saveUserSettings();
    }

    private UserSettings getSettings() {
        UserSettings settings = mTaskMaster.getConfigurationManager().getUserSettings();
        if (settings == null) {
            settings = new UserSettings();
        }
        return settings;
    }

    private void saveUserSettings(){
        UserSettings settings = getSettings();
        mTaskMaster.getConfigurationManager().saveUserSettings(settings);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if(newValue == null)
            return true;

        String v = (String)newValue;
        UserSettings settings = mTaskMaster.getConfigurationManager().getUserSettings();
        if(preference == mServerAddrPreference){
            settings.setServerAddr(v);
            mServerAddrPreference.setSummary(v);
        }
        if(preference == mUserNamePreference) {
            settings.setFirstName(v);
            mUserNamePreference.setSummary(v);
        }
        if(preference == mUserPhonePreference) {
            settings.setPhone(v);
            mUserPhonePreference.setSummary(v);
        }
        if(preference == mUserEmailPreference) {
            settings.setEmail(v);
            mUserEmailPreference.setSummary(v);
        }

        // mTaskMaster.getConfigurationManager().saveUserSettings(settings);
        return false;
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
