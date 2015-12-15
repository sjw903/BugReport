package com.qiku.bug_report;

import java.util.Properties;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;

import android.os.Environment;
import android.util.Log;

public class Constants {
    private static final String TAG = "BugReportConstants";

    /*
     * BugReport-Specific Device Properties
     */
    public static final String BUGREPORT_SERVICE;
    public static String BUGREPORT_SERVER;
    static {                    // info: the default address is set in BugReportApplication.java, not here, please ignore this static part.
        Properties props = new Properties();
        BUGREPORT_SERVICE = props.getProperty("bugreport.service", "bugreport");
        // BUGREPORT_SERVER = props.getProperty("server.host", "http://192.168.68.140:8080/platform2");  //debug
        BUGREPORT_SERVER = props.getProperty("server.host", "http://192.168.69.63:8080/platform2");  //new server
        Log.d(TAG, "BUGREPORT_SERVICE=" + BUGREPORT_SERVICE + " "
                   +"BUGREPORT_SERVER=" + BUGREPORT_SERVER);
    }

    /*
     * Intents and intent parameters
     */
    public static final String BUGREPORT_INTENT_BUGREPORT_START = "tronxyz.intent.action.BUGREPORT.START";
    public static final String BUGREPORT_INTENT_BUGREPORT_ERROR = "tronxyz.intent.action.BUGREPORT.ERR";
    public static final String BUGREPORT_INTENT_BUGREPORT_END = "tronxyz.intent.action.BUGREPORT.END";
    public static final String BUGREPORT_INTENT_COLLECT_CATEGORY_LOG = "tronxyz.intent.action.BUGREPORT.COLLECT_CATEGORY_LOG";
    public static final String BUGREPORT_INTENT_EDIT_REPORT = "tronxyz.intent.action.BUGREPORT.EDIT_REPORT";
    public static final String BUGREPORT_INTENT_VIEW_REPORT = "tronxyz.intent.action.BUGREPORT.VIEW_REPORT";
    public static final String BUGREPORT_INTENT_DISCARD_REPORT = "tronxyz.intent.action.BUGREPORT.DISCARD_REPORT";
    public static final String BUGREPORT_INTENT_SAVE_REPORT = "tronxyz.intent.action.BUGREPORT.SAVE_REPORT";
    public static final String BUGREPORT_INTENT_PAUSE_UPLOAD = "tronxyz.intent.action.BUGREPORT.PAUSE_UPLOAD";
    public static final String BUGREPORT_INTENT_BATTERY_THRESHOLD_CHANGED = "tronxyz.intent.action.BUGREPORT.BATTERY_THRESHOLD_CHANGED";
    public static final String BUGREPORT_INTENT_CONFIGURATION_UPDATED = "tronxyz.intent.action.BUGREPORT.CONFIGURATION_UPDATED";
    public static final String BUGREPORT_INTENT_REPORT_CREATED = "tronxyz.intent.action.BUGREPORT.REPORT_CREATED";
    public static final String BUGREPORT_INTENT_REPORT_UPDATED = "tronxyz.intent.action.BUGREPORT.REPORT_UPDATED";
    public static final String BUGREPORT_INTENT_REPORT_REMOVED = "tronxyz.intent.action.BUGREPORT.REPORT_REMOVED";
    public static final String BUGREPORT_INTENT_UPLOAD_PRIORITY_CHANGED = "tronxyz.intent.action.BUGREPORT.UPLOAD_PRIORITY_CHANGED";
    public static final String BUGREPORT_INTENT_EXTRA_PRIORITY_FROM = "PRIORITY_FROM";
    public static final String BUGREPORT_INTENT_EXTRA_PRIORITY_TO = "PRIORITY_TO";
    public static final String BUGREPORT_INTENT_UPLOAD_PAUSED = "tronxyz.intent.action.BUGREPORT.UPLOAD_PAUSED";
    public static final String BUGREPORT_INTENT_UPLOAD_UNPAUSED = "tronxyz.intent.action.BUGREPORT.UPLOAD_UNPAUSED";
    public static final String BUGREPORT_INTENT_EXTRA_REPORT_IDS = "REPORT_IDS";

    public static final String BUGREPORT_INTENT_PARA_CATEGORY = "ISSUE_CATEGORY";
    public static final String BUGREPORT_INTENT_PARA_SUMMARY = "ISSUE_SUMMARY";
    public static final String BUGREPORT_INTENT_PARA_DETAIL = "ISSUE_DETAIL";
    public static final String BUGREPORT_INTENT_EXTRA_BATTERY_THRESHOLD = "BATTERY_THRESHOLD";
    public static final String BUGREPORT_INTENT_EXTRA_REPORT = "COMPLAINT_REPORT";

    public static final int BUGREPORT_SHELL_LOWEST_COMPAT_VERSION = 2;
    public static final int BUGREPORT_SHELL_HIGHEST_COMPAT_VERSION = 2;
    public static final String BUGREPORT_INTENT_PARA_VERSION = "version";

    public static final String BUGREPORT_INTENT_PARA_ID = "id";
    public static final String BUGREPORT_INTENT_PARA_REQ_SIZE = "reqsize";
    public static final String BUGREPORT_INTENT_PARA_AVAIL_SIZE = "availsize";
    public static final String BUGREPORT_INTENT_PARA_ERROR_TYPE = "errortype";
    public static final String BUGREPORT_INTENT_PARA_ERROR_MSG = "errorMsg";
    public static final String BUGREPORT_INTENT_PARA_ERROR_TITLE = "ERROR_TITLE";
    public static final String BUGREPORT_INTENT_EXTRA_NOTIFICATION_TYPE = "NOTIFICATION_TYPE";
    public static final String BUGREPORT_INTENT_EXTRA_NOTIFICATION_TOAST = "NOTIFICATION_TOAST";
    public static final String BUGREPORT_INTENT_EXTRA_NOTIFICATION_BAR = "NOTIFICATION_BAR";
    public static final String BUGREPORT_INTENT_EXTRA_NOTIFICATION_DIALOG = "NOTIFICATION_DIALOG";
    public static final String BUGREPORT_SHELL_ERROR_NOSTORAGE = "nostorage";

    //Reliable Uploader Intents
    public static final String BUGREPORT_INTENT_UPLOAD_PROGRESS = "tronxyz.intent.action.upload.progress";

    public static final String BUGREPORT_INTENT_USER_SETTINGS_UPDATED = "tronxyz.intent.action.BUGREPORT.USER_SETTINGS_UPDATED";
    public static final String BUGREPORT_PERMISSION_USER_SETTINGS =  "com.tronxyz.bug_report.permission.USER_SETTINGS";

    //Report Collector
    public static final String REPORT_INFO_LABEL = "report_info";
    public static final String REPORT_LOG_PATH = "log_path";
    public static final String REPORT_LOG_CATEGORY = "log_category";
    public static final String LOG_FILES_LABEL = "files";
    public static final String LOG_SCREENSHOT_LABEL= "screenshot";
    public static final String LOG_FILES_REMOVE_LABEL ="files_to_remove";
    public static final String TIMESTAMP_LABEL = "timestamp";
    public static final String TRONXYZ_EMAIL_DOMAIN = "@tronxyz.com";

    public static final int KB = 1024;
    public static final int MB = KB * 1024;
    public static final int GB = MB * 1024;
    public static final int MIN_SPACE_REQUIRED = 50 * MB;
    public static final String BYTE_FORMAT = "%d bytes";
    public static final String KB_FORMAT = "%.1f KB";
    public static final String MB_FORMAT = "%.1f MB";
    public static final String GB_FORMAT = "%.1f GB";

    public static final int COLLECTOR_TIMEOUT = 10 * 60 * 1000;

    public static final String SECURE_STORAGE_PATH = "/data/bug_report";
    public static final String BUGREPORT_SDCARD_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
    public static final String BUGREPORT_SDCARD_STORAGE_PATH = BUGREPORT_SDCARD_PATH + "/bug_report/";
    public static final String BUGREPORT_SDCARD_EXT_PATH;
    public static final String BUGREPORT_SDCARD_EXT_STORAGE_PATH;
    static {
        String envSecondaryStorage = System.getenv("SECONDARY_STORAGE");
        if (null != envSecondaryStorage) {
            // SECONDARY_STORAGE is a colon-separate list of paths.  We're only
            // interested in the first path.
            String[] paths = envSecondaryStorage.split("[:]");
            BUGREPORT_SDCARD_EXT_PATH = paths[0];
        } else {
            // Use the Motorola alternative that existed prior to
            // SECONDARY_STORAGE.  If it's also null, then we just use null,
            // because there is no secondary storage for this device.
            BUGREPORT_SDCARD_EXT_PATH = System.getenv("EXTERNAL_ALT_STORAGE");
        }
        BUGREPORT_SDCARD_EXT_STORAGE_PATH = BUGREPORT_SDCARD_EXT_PATH != null ?
                BUGREPORT_SDCARD_EXT_PATH + "/bugreport/" : null;
    }
    public static final String BUGREPORT_SETTINGS_FILE_NAME = ".settings.properties";
    public static final String BUGREPORT_DEFAULT_SETTINGS_FILE_NAME = "default_settings";

    //User settings properties key
    public static final String USER_SETTINGS_KEY_EMAIL = "user.email";
    public static final String USER_SETTINGS_KEY_PHONE = "user.phone";
    public static final String USER_SETTINGS_KEY_FIRST_NAME = "user.first.name";
    public static final String USER_SETTINGS_KEY_COREID = "user.coreid";
    public static final String USER_SETTINGS_KEY_SERVER_ADDR = "user.server.address";
    public static final String USER_SETTINGS_KEY_AUTO_REPORT = "user.auto.report.enabled";
    public static final String USER_SETTINGS_KEY_ADD_SCREENSHOT = "user.attach.screenshot";
    public static final String USER_SETTINGS_KEY_COLLECT_LOCATION = "user.collect.location";
    public static final String USER_SETTINGS_KEY_AUTO_UPLOAD = "user.auto.upload.enabled";
    public static final String USER_SETTINGS_KEY_WIFI_ONLY = "user.upload.wifi.only";
    public static final String USER_SETTINGS_KEY_UPLOAD_MOBILE_ALLOWED  = "user.upload.mobile.allowed";
    public static final String USER_SETTINGS_KEY_BATTERY_PERCENT = "user.battery.percent";
    public static final String USER_SETTINGS_KEY_ENGINEERING_MODE_ENABLED = "user.engineering.mode.enabled";
    public static final String USER_SETTINGS_KEY_COREID_REQUIRED = "user.coreid.required";
    public static final String USER_SETTINGS_KEY_DEAM_NOFITIFCATION = "user.deam.notification.enabled";
    //User Settings Default Value
    public static final boolean DEFAULT_PROVIDE_ADDITIONAL_INFO = true;
    public static final String DEFAULT_ATTACH_SCREENSHOT = "PROMPT";
    public static final boolean DEFAULT_COLLECT_LOCATION = true;
    public static final boolean DEFAULT_WIFI_ONLY = true;
    public static final boolean DEFAULT_MOBILE_UPLOAD_ALLOWED = false;
    public static final boolean DEFAULT_DEAM_NOTIFICATION = true;
    public static final int DEFAULT_BATTERY_PERCENT = 0;
    public static final boolean DEFAULT_AUTO_REPORT = true;
    public static final boolean DEFAULT_AUTO_UPLOAD = true;

    //BugReport server connection configuration
    public static final int BUGREPORT_CONN_TIMEOUT = 10 * 1000;
    public static final int BUGREPORT_READ_TIMEOUT = 15 * 1000; // 15 seconds
    public static String BUGREPORT_URL_LOGON         = BUGREPORT_SERVER+"/AuthService?";
    public static String BUGREPORT_URL_LOGOFF        = BUGREPORT_SERVER+"/cloud-api/login/clientLogout?";
    public static String BUGREPORT_URL_FILELIST      = BUGREPORT_SERVER+"/FileListService?";
    public static String BUGREPORT_URL_FILEUPLOAD    = BUGREPORT_SERVER+"/FileListService?"; // GetSalUrl

    public static String BUGREPORT_URL_FOLDERCREATE  = BUGREPORT_SERVER+"/cloud-api/folder/create?";
    public static String BUGREPORT_URL_GETUPLOADID    = BUGREPORT_SERVER+"/cloud-api/files/getuploadid?";
    public static String BUGREPORT_URL_UPLOAD  = BUGREPORT_SERVER+"/cloud-api/files/upload?";
    public static String BUGREPORT_URL_RESUME  = BUGREPORT_SERVER+"/cloud-api/files/resume?";

    //product-specific device id configuration properties
    //TODO Migrate to device-properties
    public static final String BUGREPORT_DEVICE_ID_CONF_KEY = "ro.bugreport.uid.type";
    public static final String BUGREPORT_DEVICE_ID_TYPE_SERIAL = "SERIAL";
    public static final String BUGREPORT_DEVICE_ID_TYPE_TELEPHONY_DEVICE_ID = "TELEPHONY_DEVICE_ID";
    public static final String BUGREPORT_DEVICE_ID_TYPE_WIFI_MAC = "WIFI_MAC";
    public static final String BUGREPORT_DEVICE_ID_DEFAULT = "INVALID_DEVICE_ID";

    //Auto Upload constants
    public static final int BUGREPORT_EXEC_TIMEOUT = 2 * 60;
    public static final String BUGREPORT_EXEC_VARIABLE_OUTPUT_DIR = "B2G_REPORT_DIR";
    public static final String BUGREPORT_EXEC_VARIABLE_TIMESTAMP = "B2G_REPORT_TIMESTAMP";


    //Device System Property Keys
    public static final String DEVICE_PROPERTY_APVERSION = "ro.build.display.id";
    public static final String DEVICE_PROPERTY_BPVERSION = "gsm.version.baseband";

    private static CookieStore cookieStore = new BasicCookieStore();

    public static CookieStore getCookieStore() {
        return cookieStore;
    }

    public static void setCookieStore(CookieStore cookieStore) {
        Constants.cookieStore = cookieStore;
    }
}
