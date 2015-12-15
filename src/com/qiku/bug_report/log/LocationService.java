package com.qiku.bug_report.log;

import java.util.Date;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import com.qiku.bug_report.TaskMaster;
import com.qiku.bug_report.helper.Util;

public class LocationService{
    private static String tag = "BugReportLocationService";
    private static LocationService instance = null;
    private TaskMaster mTaskMaster;
    private LocationManager mLocationManager;
    private StringBuilder mCellLocationInfo;

    public static synchronized LocationService getInstance(TaskMaster taskMaster){
        if(instance == null){
            instance = new LocationService(taskMaster);
        }
        return instance;
    }

    private LocationService(TaskMaster taskMaster){
        this.mTaskMaster =  taskMaster;
        mLocationManager = (LocationManager)mTaskMaster.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
    }

    private void getPhoneCellInfo(){
        TelephonyManager mTelephonyManager = (TelephonyManager)mTaskMaster.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        mCellLocationInfo.append("Network Information:\n");
        mCellLocationInfo.append("\tNetwork Operator(MCC+MNC) : ").append(
                        mTelephonyManager.getNetworkOperator()).append("\n");
        mCellLocationInfo.append("\tNetwork Operator Name : ").append(
                        mTelephonyManager.getNetworkOperatorName()).append("\n");
        mCellLocationInfo.append("\tISO country code : ").append(
                        mTelephonyManager.getNetworkCountryIso()).append("\n");

        Context context = mTaskMaster.getApplicationContext();
        if (Util.isWIFIConnected(context)) {
            mCellLocationInfo.append("\tWifi Connected : ").append(true).append("\n");
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo != null) {
                mCellLocationInfo.append("\tWifi Mac Address : ").append(
                                connectionInfo.getMacAddress()).append("\n");
                mCellLocationInfo.append("\tWifi BSSID : ").append(
                                connectionInfo.getBSSID()).append("\n");
                mCellLocationInfo.append("\tWifi SSID : ").append(
                                connectionInfo.getSSID()).append("\n");
            }else{
                mCellLocationInfo.append("\tWifi info : N/A\n");
            }
        }else{
            mCellLocationInfo.append("\tWifi Connected : ").append(false).append("\n");
        }

        mCellLocationInfo.append("\nDevice Cell Location Information:\n");
        if(mTelephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA){
            Log.v(tag, "Start collecting CDMA base station info");
            CdmaCellLocation cdmaLocation = (CdmaCellLocation) mTelephonyManager.getCellLocation();
            mCellLocationInfo.append("\tPhone Type : CDMA\n");
            if(cdmaLocation != null){
                mCellLocationInfo.append("\tBase Station ID : ").append(cdmaLocation.getBaseStationId()).append("\n");
                mCellLocationInfo.append("\tBase Station Latitude : ").append(cdmaLocation.getBaseStationLatitude()).append("\n");
                mCellLocationInfo.append("\tBase Station Longitude : ").append(cdmaLocation.getBaseStationLongitude()).append("\n");
            }else{
                mCellLocationInfo.append("\tN\\A\n");
            }
        }else if(mTelephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM){
            Log.v(tag, "Start collecting GSM cell info");
            GsmCellLocation gsmLocation = (GsmCellLocation) mTelephonyManager.getCellLocation();
            mCellLocationInfo.append("\tPhone Type : GSM\n");
            if(gsmLocation != null){
                mCellLocationInfo.append("\tGSM Cell ID : ").append(gsmLocation.getCid()).append("\n");
                mCellLocationInfo.append("\tGSM Area Code : ").append(gsmLocation.getLac()).append("\n");
            }else{
                mCellLocationInfo.append("\tN\\A\n");
            }
        }else{
            mCellLocationInfo.append("\tN\\A\n");
        }
    }

    public void saveLocationInfo(final String filePath){
        Log.d(tag, "Start collecting location information");
        mCellLocationInfo =  new StringBuilder();
        //Always retrieve the cell location
        getPhoneCellInfo();
        StringBuilder phoneLocationInfo = new StringBuilder();
        phoneLocationInfo.append(mCellLocationInfo).append("\nDevice Location Information:\n");
        //Retrieve the phone location if user allows BugReport to do so
        if(mTaskMaster.getConfigurationManager().getUserSettings().getCollectUserLocation().getValue()){
            if( mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ){
                toLocationString(mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER), phoneLocationInfo);
            }
            if( mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ){
                toLocationString(mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER), phoneLocationInfo);
            }
        }else{
            phoneLocationInfo.append("\tNot allowed to collect the data");
        }
        Util.saveDataToFile(phoneLocationInfo.toString().getBytes(), filePath);
    }


    @SuppressWarnings("deprecation")
	private void toLocationString(Location location, StringBuilder phoneLocationInfo){
        if(location != null){
            phoneLocationInfo.append("\tProvider : ").append(location.getProvider()).append("\n");
            phoneLocationInfo.append("\tLatitude : ").append(location.getLatitude()).append("\n");
            phoneLocationInfo.append("\tLongitude : ").append(location.getLongitude()).append("\n");
            phoneLocationInfo.append("\tTime : ").append(location.getTime()).append("(")
                .append(new Date(location.getTime()).toGMTString()).append(")\n\n");
        }
    }

    /**
     * This class is used to dynamically get the latest location fix, but currently not used.
     * @author gkp374
     *
     */
    /*
    class BugReportLocationListener extends Thread{

        private Location mCurrentBestLocation;
        private LocationListener mLocationListener;
        private Looper mLooper;
        private String mSaveFilePath;

        public void run(){
            Looper.prepare();
            mLooper = Looper.myLooper();
            startListeningLocationUpdate();
            Looper.loop();
        }

        private void startListeningLocationUpdate() {
            Log.d(tag, "Started listening location update");
            // Define a listener that responds to location updates
            mLocationListener = new LocationListener() {
                // Called when a new location is found by the network location provider.
                public void onLocationChanged(Location location) {
                    Log.i(tag, "Received a better location : latitude " + location.getLatitude() + ", longitude " + location.getLongitude());
                    mCurrentBestLocation = location;
                    mPhoneLocationInfo.append("GPS Location : ").append("\n");
                    mPhoneLocationInfo.append("Provider : ").append(mCurrentBestLocation.getProvider()).append("\n");
                    mPhoneLocationInfo.append("Latitude : ").append(mCurrentBestLocation.getLatitude()).append("\n");
                    mPhoneLocationInfo.append("Longitude : ").append(mCurrentBestLocation.getLongitude()).append("\n");
                    mPhoneLocationInfo.append("Time : ").append(mCurrentBestLocation.getTime()).append("\n");
                    //stop listening once we get the first update
                    stopListening();
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {}
                public void onProviderEnabled(String provider) {}
                public void onProviderDisabled(String provider) {
                    if(!mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) &&
                            !mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                        stopListening();
                    }
                }
            };
            // Register the listener with the Location Manager to receive location updates
            if(mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
            if(mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
        }

        private void stopListening() {
            Log.i(tag, "Stopped listening location update");
            mLocationManager.removeUpdates(mLocationListener);
            //save location information to the given file
            Util.saveDataToFile(mLocationInfo.toString().getBytes(), mSaveFilePath);
            //quit from this looper
            mLooper.quit();
        }
    }*/
}
