package com.qiku.bug_report.model;

import java.util.Properties;

import android.text.TextUtils;
import android.util.Log;

import com.qiku.bug_report.Constants;

public class UserSettings{
    public enum Conf {
        TRUE, FALSE, PROMPT;
    }
    public static class Item<T>{
        private boolean mEditable = true;
        private T mValue;
        public Item(T value){
            mValue = value;
        }
        public boolean isEditable() {
            return mEditable;
        }
        public void setEditable(boolean editable) {
            this.mEditable = editable;
        }
        public T getValue() {
            return mValue;
        }
        public void setValue(T value) {
            this.mValue = value;
        }
        public String toString(){
            if(mValue == null)
                return null;
            return mValue.toString();
        }
    }
    public static class IntItem extends Item<Integer>{
        public IntItem(int value){
            super(value);
        }
    }
    public static class BoolItem extends Item<Boolean>{
        public BoolItem(boolean value){
            super(value);
        }
    }
    public static class StringItem extends Item<String>{
        public StringItem(String value){
            super(value);
        }
    }

    private String mFirstName;
    private String mEmail;
    private String mPhone;
    private String mCoreID;
    private String mServerAddr;

    private BoolItem mShowAdditionalInfo = new BoolItem(Constants.DEFAULT_PROVIDE_ADDITIONAL_INFO);
    private StringItem mAttachScreenshot = new StringItem(Constants.DEFAULT_ATTACH_SCREENSHOT);
    private BoolItem mCollectUserLocation = new BoolItem(Constants.DEFAULT_COLLECT_LOCATION);
    private BoolItem mWlanUploadAllowed = new BoolItem(Constants.DEFAULT_WIFI_ONLY);
    private BoolItem mDeamNotify = new BoolItem(Constants.DEFAULT_DEAM_NOTIFICATION);
    private IntItem mBatteryPercent = new IntItem(Constants.DEFAULT_BATTERY_PERCENT);
    private BoolItem mAutoReportEnabled = new BoolItem(Constants.DEFAULT_AUTO_REPORT);
    private BoolItem mAutoUploadEnabled = new BoolItem(Constants.DEFAULT_AUTO_UPLOAD);
    public UserSettings() {
    }

    public String getFirstName() {
        return mFirstName;
    }

    public void setFirstName(String mFirstaName) {
        this.mFirstName = mFirstaName;
    }

    public String getEmail() {
        return mEmail;
    }

    public void setEmail(String mEmail) {
        this.mEmail = mEmail;
    }

    public String getPhone() {
        return mPhone;
    }

    public void setPhone(String mPhone) {
        this.mPhone = mPhone;
    }

    public String getCoreID() {
        return mCoreID;
    }

    public void setCoreID(String mCoreID) {
        this.mCoreID = mCoreID;
    }

    public String getServerAddr() {
        return mServerAddr;
    }

    public void setServerAddr(String sa) {
        this.mServerAddr = sa;
    }

    public BoolItem getShowAdditionalInfo() {
        return mShowAdditionalInfo;
    }

    public void setShowAdditionalInfo(boolean showAdditionalInfo) {
        this.mShowAdditionalInfo = new BoolItem(showAdditionalInfo);
    }

    public StringItem getAttachScreenshot() {
        return mAttachScreenshot;
    }

    public void setAttachScreenshot(String attachScreenshot) {
        this.mAttachScreenshot = new StringItem(attachScreenshot);
    }

    public BoolItem getCollectUserLocation() {
        return mCollectUserLocation;
    }

    public void setCollectUserLocation(boolean collectUserLocation) {
        this.mCollectUserLocation = new BoolItem(collectUserLocation);
    }

    public BoolItem getWlanUploadAllowed() {
        return mWlanUploadAllowed;
    }

    public void setWlanUploadAllowed(boolean allowed) {
        this.mWlanUploadAllowed = new BoolItem(allowed);
    }

    public BoolItem getDeamNotify(){
        return mDeamNotify;
    }

    public void setDeamNotify(boolean deamNotify){
        this.mDeamNotify = new BoolItem(deamNotify);
    }
    public IntItem getBatteryPercent() {
        return mBatteryPercent;
    }

    public void setBatteryPercent(int batteryPercent) {
        this.mBatteryPercent = new IntItem(batteryPercent);
    }

    public BoolItem isAutoReportEnabled() {
        return mAutoReportEnabled;
    }

    public void setAutoReportEnabled(boolean mAutoEnabled) {
        this.mAutoReportEnabled = new BoolItem(mAutoEnabled);
    }

    public BoolItem isAutoUploadEnabled(){
        return mAutoUploadEnabled;
    }

    public void setAutoUploadEnabled(boolean autoUploadEnabled){
        this.mAutoUploadEnabled = new BoolItem(autoUploadEnabled);
    }
    public boolean isContactInfoComplete(){
        return !TextUtils.isEmpty(mEmail) && !TextUtils.isEmpty(mFirstName)
                && !TextUtils.isEmpty(mPhone);
    }

    private static String returnNullIfEmpty(Properties properties, String key) {
        if(TextUtils.isEmpty(properties.getProperty(key))) {
            return null;
        }
        return properties.getProperty(key);
    }

    public static UserSettings fromProperties(Properties properties) {
        UserSettings settings = new UserSettings();
        if (properties != null) {
            settings.mFirstName =
                returnNullIfEmpty(properties,  Constants.USER_SETTINGS_KEY_FIRST_NAME);
            settings.mEmail = returnNullIfEmpty(properties, Constants.USER_SETTINGS_KEY_EMAIL);
            settings.mPhone = returnNullIfEmpty(properties, Constants.USER_SETTINGS_KEY_PHONE);
            settings.mCoreID = returnNullIfEmpty(properties, Constants.USER_SETTINGS_KEY_COREID);
            settings.mServerAddr = returnNullIfEmpty(properties, Constants.USER_SETTINGS_KEY_SERVER_ADDR);

            String attachScreenshot =
                returnNullIfEmpty(properties, Constants.USER_SETTINGS_KEY_ADD_SCREENSHOT);
            settings.mAttachScreenshot = attachScreenshot == null ?
                            new StringItem(Constants.DEFAULT_ATTACH_SCREENSHOT) :
                                new StringItem(attachScreenshot);

            String collectUserLocation =
                returnNullIfEmpty(properties, Constants.USER_SETTINGS_KEY_COLLECT_LOCATION);
            settings.mCollectUserLocation = collectUserLocation == null ?
                            new BoolItem(Constants.DEFAULT_COLLECT_LOCATION) :
                                new BoolItem(Boolean.parseBoolean(collectUserLocation));

            String uploadWlanAllowed = returnNullIfEmpty(properties, Constants.USER_SETTINGS_KEY_WIFI_ONLY);
            settings.mWlanUploadAllowed = uploadWlanAllowed == null ?
                            new BoolItem(Constants.DEFAULT_WIFI_ONLY) :
                                new BoolItem(Boolean.parseBoolean(uploadWlanAllowed));

            String batteryLevel =
                returnNullIfEmpty(properties, Constants.USER_SETTINGS_KEY_BATTERY_PERCENT);
            settings.mBatteryPercent = batteryLevel == null ?
                            new IntItem(Constants.DEFAULT_BATTERY_PERCENT) :
                            new IntItem(Integer.parseInt(batteryLevel));

            String isAutoReport =
                returnNullIfEmpty(properties, Constants.USER_SETTINGS_KEY_AUTO_REPORT);
            settings.mAutoReportEnabled = isAutoReport == null ?
                            new BoolItem(Constants.DEFAULT_AUTO_REPORT) :
                                new BoolItem(Boolean.parseBoolean(isAutoReport));

            String autoUpload =
                returnNullIfEmpty(properties, Constants.USER_SETTINGS_KEY_AUTO_UPLOAD);
            settings.mAutoUploadEnabled = autoUpload == null ?
                            new BoolItem(Constants.DEFAULT_AUTO_UPLOAD) :
                                new BoolItem(Boolean.parseBoolean(autoUpload));
            Log.d("UserSettings: ", 
                    "isAutoReport: " + isAutoReport
                    + " autoUpload: " + autoUpload
                    + " uploadMobileAllowed: " + uploadWlanAllowed
                    + " collectUserLocation: " + collectUserLocation
            		);
        }
        return settings;
    }

    public static Properties toProperties(UserSettings settings) {
        Properties properties = new Properties();
        if (settings != null) {
            properties.put(Constants.USER_SETTINGS_KEY_FIRST_NAME,
                            settings.mFirstName == null ? "" : settings.mFirstName);
            properties.put(Constants.USER_SETTINGS_KEY_EMAIL,
                            settings.mEmail == null ? "" : settings.mEmail);
            properties.put(Constants.USER_SETTINGS_KEY_PHONE,
                            settings.mPhone == null ? "" : settings.mPhone);
            properties.put(Constants.USER_SETTINGS_KEY_COREID,
                            settings.mCoreID == null ? "" : settings.mCoreID);
            properties.put(Constants.USER_SETTINGS_KEY_SERVER_ADDR,
                            settings.mServerAddr == null ? "" : settings.mServerAddr);

            properties.put(Constants.USER_SETTINGS_KEY_ADD_SCREENSHOT, settings.
                            mAttachScreenshot == null ? "" : settings.mAttachScreenshot.getValue());
            properties.put(Constants.USER_SETTINGS_KEY_COLLECT_LOCATION,
                            String.valueOf(settings.mCollectUserLocation.getValue()));
            properties.put(Constants.USER_SETTINGS_KEY_WIFI_ONLY,
                            String.valueOf(settings.mWlanUploadAllowed.getValue()));
            properties.put(Constants.USER_SETTINGS_KEY_BATTERY_PERCENT,
                            String.valueOf(settings.mBatteryPercent.getValue()));
            properties.put(Constants.USER_SETTINGS_KEY_AUTO_REPORT,
                            String.valueOf(settings.mAutoReportEnabled.getValue()));
            properties.put(Constants.USER_SETTINGS_KEY_AUTO_UPLOAD,
                            String.valueOf(settings.mAutoUploadEnabled.getValue()));
        }
        return properties;
    }
}
