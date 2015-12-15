package com.tronxyz.bug_report.conf;

import java.io.File;
import java.util.Properties;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.tronxyz.bug_report.Constants;
import com.tronxyz.bug_report.helper.Util;
import com.tronxyz.bug_report.model.UserSettings;

public class ConfigurationManager {

    private static final String TAG = "BugReportConfigMgr";
    private Context mContext;
    private UserSettings mUserSettings;
    private DeamConfiguration mDc;
    private Properties mAppProperties = null;

    public ConfigurationManager(Context context) {
        mContext = context;
        mUserSettings = UserSettings.fromProperties(getAppProperties());
        mDc = new DeamConfiguration(context);
        mDc.start();

    }

    public void saveUserSettings(UserSettings userSettings) {
        Log.i(TAG, "saveUserSettings");
        this.mUserSettings = userSettings;
        Properties settingsProperties = UserSettings.toProperties(mUserSettings);
        updateAppProperties(settingsProperties);

        Intent intent = new Intent(Constants.BUGREPORT_INTENT_USER_SETTINGS_UPDATED);
        intent.putExtra(Constants.USER_SETTINGS_KEY_COREID, userSettings.getCoreID());
        intent.putExtra(Constants.USER_SETTINGS_KEY_PHONE, userSettings.getPhone());
        intent.putExtra(Constants.USER_SETTINGS_KEY_FIRST_NAME, userSettings.getFirstName());
        intent.putExtra(Constants.USER_SETTINGS_KEY_EMAIL, userSettings.getEmail());
        mContext.sendBroadcast(intent, Constants.BUGREPORT_PERMISSION_USER_SETTINGS);
    }

    public UserSettings getUserSettings() {
        return mUserSettings;
    }

    public DeamConfiguration getDeamConfiguration(){
        return mDc;
    }

    public boolean isUserSettingsValid(){
        if( mUserSettings == null )
            return false;

        if (TextUtils.isEmpty(mUserSettings.getCoreID()))
            return false;

        return mUserSettings.isContactInfoComplete();
    }

    private void updateAppProperties(Properties properties) {
        Log.d(TAG, "updateAppProperties");
        if (properties == null || properties.size() == 0) {
            Log.w(TAG, "The properties is null or empty");
            return;
        }

        if(mAppProperties.equals(properties))
            return;

        // update existing properties with new properties
        mAppProperties.putAll(properties);
        Util.savePropertiesToFile(mAppProperties,
                mContext.getFileStreamPath(Constants.BUGREPORT_SETTINGS_FILE_NAME).getAbsolutePath());
    }

    private synchronized Properties getAppProperties() {
    	String filePath = mContext.getFileStreamPath(Constants.BUGREPORT_SETTINGS_FILE_NAME).getAbsolutePath();
        Log.d(TAG, "getAppProperties: " + filePath);
        if(mAppProperties == null) {
            File settingsFile = new File(filePath);
            if(settingsFile.exists()) {
                Log.i(TAG, String.format("loading app properties from file %s", settingsFile.getAbsolutePath()));
                mAppProperties = Util.readPropertiesFromFile(filePath);
            }else{
                Log.i(TAG, "loading app properties from default asserts file ");
                mAppProperties = Util.readPropertiesFromAsserts(Constants.BUGREPORT_DEFAULT_SETTINGS_FILE_NAME, mContext);
            }
        }
        return mAppProperties;
    }
}
