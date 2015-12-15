package com.qiku.bug_report.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.qiku.bug_report.Constants;
import com.qiku.bug_report.R;

public class DeviceID {
    private static final String tag = "BugReportDeviceID";
    private static String mUid = null;
    private static String mDeviceId = null;
    private static String mDeviceType = null;
    private static String mMacAddress = null;
    private static final String BUGREPORT_DEVICE_ID_CACHE_KEY = "device_id";
    private static final String BUGREPORT_DEVICE_ID_TYPE_CACHE_KEY = "device_id_type";
    private static DeviceID instance = new DeviceID();

    private DeviceID(){}

    public static DeviceID getInstance(){
        return instance;
    }

    public synchronized String getId(Context ctx) {
        if(TextUtils.isEmpty(mUid)){
            String type = getType();
            // try {
            //     if( Constants.BUGREPORT_DEVICE_ID_TYPE_SERIAL.equals(type) ){
            //         mUid = android.os.Build.SERIAL;
            //     }else if( Constants.BUGREPORT_DEVICE_ID_TYPE_TELEPHONY_DEVICE_ID.equals(type) ){
            //         mUid = getTelephonyDeviceId(ctx);
            //     }else if( Constants.BUGREPORT_DEVICE_ID_TYPE_WIFI_MAC.equals(type) ){
            //         mUid = getMacAddress(ctx);
            //     }
            //     Log.d(tag, String.format("DeviceID : %s, IDType: %s", mUid, type));
            // } catch (Throwable e) {
            //     Log.e(tag, String.format("Failed to get the device id with type %s", type), e);
            // }

            try {               // we only use serial number + wifi mac .
                mUid = "";
                if(!TextUtils.isEmpty(android.os.Build.SERIAL)){
                    mUid = "SN" + android.os.Build.SERIAL;
                }
                String mac = getMacAddress(ctx);
                if(!TextUtils.isEmpty(mac)){
                    mUid = mUid + "MAC" + mac.replaceAll(":", "");
                }
                Log.d(tag, "mUid = " + mUid);
            } catch (Throwable e) {
                Log.e(tag, "Failed to get the device id", e);
            }

            SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(ctx);
            //If no device ID returned, we should check whether there is a cached ID
            if(TextUtils.isEmpty(mUid)){
                String cachedDeviceId = preference.getString(BUGREPORT_DEVICE_ID_CACHE_KEY, null);
                String cachedDeviceIdType = preference.getString(BUGREPORT_DEVICE_ID_TYPE_CACHE_KEY, "");
                //Use the cached value only if the device ID type matches
                if(cachedDeviceIdType.equals(type) && cachedDeviceId!=null){
                    mUid = cachedDeviceId;
                }else{//If there is no value in the cache or the ID type doesn't match, return the default value.
                    // Notifications.showNotification(ctx, R.string.notification_invalid_device_id_title,
                    //         R.string.notification_invalid_device_id_msg, R.string.notification_invalid_device_id_title);
                    return Constants.BUGREPORT_DEVICE_ID_DEFAULT;
                }
            }else{
                //cache the ID to preference file once we get a valid ID.
                Editor editor = preference.edit();
                editor.putString(BUGREPORT_DEVICE_ID_CACHE_KEY, mUid);
                editor.putString(BUGREPORT_DEVICE_ID_TYPE_CACHE_KEY, type);
                editor.commit();
            }
        }
        return mUid;
    }

    public synchronized String getType(){
        if(mDeviceType == null){
            //Use the system property 'ro.bugreport.uid.type' to determine where to get the device UID,
            //If the property is not found, use BUGREPORT_DEVICE_ID_TYPE_TELEPHONY_DEVICE_ID as the UID
            mDeviceType = Util.getSystemProperty(Constants.BUGREPORT_DEVICE_ID_CONF_KEY, Constants.BUGREPORT_DEVICE_ID_TYPE_TELEPHONY_DEVICE_ID);
        }
        return mDeviceType;
    }

    public synchronized String getTelephonyDeviceId(Context ctx){
        if(mDeviceId == null){
            TelephonyManager tMgr = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            mDeviceId = tMgr.getDeviceId();
        }
        return mDeviceId;
    }

    public synchronized String getMacAddress(Context ctx){
        if(mMacAddress == null){
            WifiManager wifiMan = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInf = wifiMan.getConnectionInfo();
            if(wifiInf != null)
                mMacAddress = wifiInf.getMacAddress();
        }
        return mMacAddress;
    }
}
