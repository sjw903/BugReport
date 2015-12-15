
package com.qiku.bug_report.helper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;
import android.net.Uri;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.qiku.bug_report.Constants;
import com.qiku.bug_report.R;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.environment.EnvironmentUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONStringer;

import java.math.BigDecimal;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@SuppressLint({
        "SimpleDateFormat", "UseValueOf"
})
public class Util {
    static String tag = "BugReportUtil";
    private static Class<?> systemPropertiesClass = null;
    public static SimpleDateFormat sdf = null;
    private static String VERSION_NAME = null;
    private static String errorTag = "Failed to get system property : %s";

    public static String getAppVersionName(Context ctx) {
        try {
            if (VERSION_NAME == null) {
                PackageInfo packageInfo = ctx.getPackageManager()
                        .getPackageInfo(ctx.getPackageName(),
                                PackageManager.GET_META_DATA);
                VERSION_NAME = packageInfo.versionName;
            }
            return VERSION_NAME;
        } catch (NameNotFoundException e) {
            Log.e(tag, ctx.getPackageName() + " not found", e);
            return null;
        }
    }

    public static String getSystemProperty(String key, String defaultValue) {
        try {
            if (systemPropertiesClass == null)
                systemPropertiesClass = Class
                        .forName("android.os.SystemProperties");
            Method get = systemPropertiesClass.getMethod("get", String.class);
            String value = (String) get.invoke(systemPropertiesClass, key);
            Log.i(tag, "get system property key = " + key+ ", value = " + value);
            if (TextUtils.isEmpty(value))
                return defaultValue;
            else
                return value;
        } catch (ClassNotFoundException e) {
            Log.e(tag,
                    String.format(errorTag, key), e);
            return defaultValue;
        } catch (NoSuchMethodException e) {
            Log.e(tag,
                    String.format(errorTag, key), e);
            return defaultValue;
        } catch (IllegalAccessException e) {
            Log.e(tag,
                    String.format(errorTag, key), e);
            return defaultValue;
        } catch (IllegalArgumentException e) {
            Log.e(tag,
                    String.format(errorTag, key), e);
            return defaultValue;
        } catch (InvocationTargetException e) {
            Log.e(tag,
                    String.format(errorTag, key), e);
            return defaultValue;
        }
    }

    public static boolean setSystemProperty(String key, String value) {
        try {
            if (systemPropertiesClass == null)
                systemPropertiesClass = Class
                        .forName("android.os.SystemProperties");
            Method set = systemPropertiesClass.getMethod("set", String.class,
                    String.class);
            Log.i(tag, "set system property key = " + key+ ", value = " + value);
            set.invoke(systemPropertiesClass, key, value);
            return true;
        } catch (ClassNotFoundException e) {
            Log.e(tag,
                    String.format(errorTag, key), e);
            return false;
        } catch (NoSuchMethodException e) {
            Log.e(tag,
                    String.format(errorTag, key), e);
            return false;
        } catch (IllegalAccessException e) {
            Log.e(tag,
                    String.format(errorTag, key), e);
            return false;
        } catch (IllegalArgumentException e) {
            Log.e(tag,
                    String.format(errorTag, key), e);
            return false;
        } catch (InvocationTargetException e) {
            Log.e(tag,
                    String.format(errorTag, key), e);
            return false;
        }
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info != null)
            return info.isConnected();
        return false;
    }

    public static boolean isWIFIConnected(Context context) {
        ConnectivityManager conMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMgr
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (netInfo != null && netInfo.isConnected()) {
            Log.d(tag,
                    String.format("TYPE_WIFI: %s, WIFI connected",
                            netInfo.toString()));
            return true;
        }
        return false;
    }

    public static boolean is3GConnected(Context context) {
        ConnectivityManager conMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        TelephonyManager telephonyMgr = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        NetworkInfo netInfo = conMgr
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (netInfo != null) {
            Log.d(tag, "TYPE_MOBILE: " + netInfo.toString());
            int netSubtype = netInfo.getSubtype();
            Log.d(tag, "SUBTYPE: " + netSubtype);
            if (!telephonyMgr.isNetworkRoaming() && netInfo.isConnected()) {
                if (netSubtype == TelephonyManager.NETWORK_TYPE_EHRPD
                        || netSubtype == TelephonyManager.NETWORK_TYPE_HSDPA
                        || netSubtype == TelephonyManager.NETWORK_TYPE_HSPA
                        || netSubtype == TelephonyManager.NETWORK_TYPE_HSPAP
                        || netSubtype == TelephonyManager.NETWORK_TYPE_HSUPA
                        || netSubtype == TelephonyManager.NETWORK_TYPE_LTE
                        || netSubtype == TelephonyManager.NETWORK_TYPE_UMTS) {
                    Log.d(tag, "UMTS 3G connected");
                    return true;
                }
                if (netSubtype == TelephonyManager.NETWORK_TYPE_EVDO_0
                        || netSubtype == TelephonyManager.NETWORK_TYPE_EVDO_A
                        || netSubtype == TelephonyManager.NETWORK_TYPE_EVDO_B) {
                    Log.d(tag, "CDMA 3G connected");
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean is2GConnected(Context context) {
        if (isWIFIConnected(context)) {
            return false;
        }

        ConnectivityManager conMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        TelephonyManager telephonyMgr = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        NetworkInfo netInfo = conMgr
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (netInfo != null) {
            Log.d(tag, "TYPE_MOBILE: " + netInfo.toString());
            int netSubtype = netInfo.getSubtype();
            Log.d(tag, "SUBTYPE: " + netSubtype);
            if (!telephonyMgr.isNetworkRoaming() && netInfo.isConnected()) {
                if (netSubtype == TelephonyManager.NETWORK_TYPE_1xRTT
                        || netSubtype == TelephonyManager.NETWORK_TYPE_CDMA
                        || netSubtype == TelephonyManager.NETWORK_TYPE_EDGE
                        || netSubtype == TelephonyManager.NETWORK_TYPE_GPRS
                        || netSubtype == TelephonyManager.NETWORK_TYPE_IDEN) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isShellCompatible(int shellVersion) {
        if ((shellVersion < Constants.BUGREPORT_SHELL_LOWEST_COMPAT_VERSION)
                || (shellVersion > Constants.BUGREPORT_SHELL_HIGHEST_COMPAT_VERSION)) {
            Log.e(tag,
                    "Shell version="
                            + shellVersion
                            + " not compatible;"
                            + " requires at least version="
                            + Constants.BUGREPORT_SHELL_LOWEST_COMPAT_VERSION
                            + " and at most version="
                            + Constants.BUGREPORT_SHELL_HIGHEST_COMPAT_VERSION);
            return false;
        }
        return true;
    }

    synchronized public static String formatDate(String format, Date date) {
        if (sdf == null)
            sdf = new SimpleDateFormat(format);
        else
            sdf.applyPattern(format);
        return sdf.format(date);
    }

    public static String formatShorterDate(Date date) {
        Calendar formatDate = Calendar.getInstance();
        formatDate.setTime(date);

        Calendar endOfYesterday = Calendar.getInstance();
        endOfYesterday.set(Calendar.HOUR_OF_DAY, 0);
        endOfYesterday.set(Calendar.MINUTE, 0);
        endOfYesterday.set(Calendar.SECOND, 0);
        endOfYesterday.set(Calendar.MILLISECOND, 0);

        if (endOfYesterday.after(formatDate)) {
            return Util.formatDate("MMM dd", date);
        } else {
            return Util.formatDate("h:mma", date).toLowerCase();
        }
    }

    public static Date parseDate(String format, String date) {
        if (sdf == null)
            sdf = new SimpleDateFormat(format);
        else
            sdf.applyPattern(format);
        try {
            return sdf.parse(date);
        } catch (ParseException e) {
            Log.e(tag, String.format("Invalid params : %s , %s", format, date),
                    e);
            return null;
        }
    }

    public static String capitalize(String source) {
        if (source == null)
            return null;
        if (source.length() == 1)
            return source.toUpperCase();
        return Character.toUpperCase(source.charAt(0))
                + source.substring(1).toLowerCase();
    }

    public static boolean mkdirs(String dir) {
        if (TextUtils.isEmpty(dir))
            return false;
        File file = new File(dir);
        if (file.exists())
            return false;
        return file.mkdirs();
    }

    @SuppressLint("NewApi")
    public static boolean isSpaceAvailable(String path) {
        if (TextUtils.isEmpty(path))
            return false;
        File file = new File(path);
        return file.exists() && file.canWrite()
                && file.getUsableSpace() >= Constants.MIN_SPACE_REQUIRED;
    }

    public static String getTempPath() {
        if (isSpaceAvailable(Constants.BUGREPORT_SDCARD_EXT_PATH)) {
            return Constants.BUGREPORT_SDCARD_EXT_STORAGE_PATH + "/tmp";
        } else if (isSpaceAvailable(Constants.BUGREPORT_SDCARD_PATH)) {
            return Constants.BUGREPORT_SDCARD_STORAGE_PATH + "/tmp";
        } else
            return null;
    }

    public static String getLogPath() {
        if (isSpaceAvailable(Constants.BUGREPORT_SDCARD_EXT_PATH)) {
            return Constants.BUGREPORT_SDCARD_EXT_STORAGE_PATH;
        } else if (isSpaceAvailable(Constants.BUGREPORT_SDCARD_PATH)) {
            return Constants.BUGREPORT_SDCARD_STORAGE_PATH;
        } else
            return null;
    }

    /**
     * Remove the multiples files specified in the files array. If the files
     * contain directory, it will remove it recursively.
     * 
     * @param files file paths to be removed
     */
    public static void removeFiles(String[] files) {
        if (files == null)
            return;
        for (String file : files) {
            removeFile(file);
        }
    }

    /**
     * Remove the file for the give filePath. If the the filePath is a
     * directory, it will remove it recursively.
     * 
     * @param filePath file path to be removed
     */
    public static boolean removeFile(String filePath) {
        Log.d(tag, String.format("removeFile() : %s", filePath));
        if (filePath == null)
            return true;
        File file = new File(filePath);
        if (!file.exists()) {
            Log.w(tag, String.format("File Not Exist : %s", filePath));
            return true;
        }

        if (file.isDirectory()) {
            if (!filePath.endsWith(File.separator)) {
                filePath = filePath + File.separator;
            }
            String[] children = file.list();
            for (int i = 0; children != null && i < children.length; i++) {
                boolean success = removeFile(filePath + children[i]);
                if (!success) {
                    return false;
                }
            }
        }
        return file.delete();
    }

    public static Properties readPropertiesFromFile(String filePath) {
        Log.i(tag, String.format("readPropertiesFromFile %s", filePath));
        Properties properties = new Properties();
        if (TextUtils.isEmpty(filePath)) {
            Log.i(tag, "Invalid file path");
            return properties;
        }
        File file = new File(filePath);
        // load the existing properties
        if (file.exists() && file.isFile()) {
            FileInputStream is = null;
            try {
                is = new FileInputStream(file);
                properties.load(is);
            } catch (FileNotFoundException e) {
                Log.e(tag,
                        String.format("File not found %s",
                                file.getAbsolutePath()), e);
            } catch (IOException e) {
                Log.e(tag, String.format(
                        "Error occured while loading properties file %s",
                        file.getAbsolutePath()));
            } finally {
                if (is != null)
                    try {
                        is.close();
                    } catch (IOException e) {
                        Log.v(tag, e.getMessage());
                    }
            }
        }
        return properties;
    }

    public static Properties readPropertiesFromAsserts(String fileName,
            Context context) {
        Log.i(tag, String.format("readPropertiesFromAsserts %s", fileName));
        InputStream is = null;
        Properties properties = new Properties();
        try {
            is = context.getAssets().open(fileName);
            properties.load(is);
        } catch (IOException e) {
            Log.e(tag, "Error occured when parsing detect_bug.xml from assets",
                    e);
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException ex) {
                Log.v(tag, ex.getMessage());
            }

        }
        return properties;
    }

    public static void savePropertiesToFile(Properties properties,
            String filePath) {
        Log.i(tag, String.format("savePropertiesToFile %s", filePath));
        if (properties == null) {
            Log.i(tag, "The properties to be saved is a null object");
            return;
        }
        OutputStream os = null;
        try {
            File file = new File(filePath);
            File parentFile = file.getParentFile();
            if (parentFile != null && !parentFile.exists())
                parentFile.mkdirs();
            os = new FileOutputStream(file);
            properties.store(os,
                    "Auto-generated by BugReport at " + new Date().toString());
        } catch (IOException e) {
            Log.e(tag, String.format("Failed to save properties to file %s",
                    filePath), e);
        } finally {
            try {
                if (os != null) {
                    os.close();
                    os = null;
                }
            } catch (IOException ex) {
                Log.e(tag, "Error closing Output Stream", ex);
            }
        }
    }

    public static void saveDataToFile(byte[] data, String filePath) {
        Log.d(tag, String.format("saveDataToFile %s", filePath));
        if (data == null) {
            Log.d(tag, "The data to be saved is a null object");
            return;
        }
        OutputStream os = null;
        try {
            File file = new File(filePath);
            File parentFile = file.getParentFile();
            if (parentFile != null && !parentFile.exists())
                parentFile.mkdirs();
            os = new FileOutputStream(file);
            os.write(data);
            os.flush();
        } catch (IOException e) {
            Log.e(tag,
                    String.format("Failed to save data to file %s", filePath),
                    e);
        } finally {
            try {
                if (os != null) {
                    os.close();
                    os = null;
                }
            } catch (IOException ex) {
                Log.e(tag, "Error closing Output Stream", ex);
            }
        }
    }

    /**
     * While calling this method, you must close the is yourself.
     * 
     * @param is
     * @param filePath
     */
    public static void saveDataToFile(InputStream is, String filePath) {
        // TODO Use Buffered streams
        Log.d(tag, String.format("saveDataToFile %s", filePath));
        if (is == null || filePath == null) {
            Log.d(tag, "Invalid data or file path");
            return;
        }
        OutputStream os = null;
        try {
            File file = new File(filePath);
            File parentFile = file.getParentFile();
            if (parentFile != null && !parentFile.exists())
                parentFile.mkdirs();
            os = new FileOutputStream(file);
            byte[] data = new byte[1024];
            int read = is.read(data);
            while (read >= 0) {
                os.write(data, 0, read);
                read = is.read(data);
            }
            os.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null) {
                    os.close();
                    os = null;
                }
            } catch (IOException ex) {
                Log.e(tag, "Error closing Output Stream", ex);
            }
        }
    }

    public static byte[] readDataFromFile(String filePath) {
        ByteArrayOutputStream bArrayOut = null;
        InputStream is = null;
        try {
            is = new FileInputStream(filePath);
            bArrayOut = new ByteArrayOutputStream();
            byte[] data = new byte[1024];
            int length = is.read(data, 0, 1024);
            while (length != -1) {
                bArrayOut.write(data, 0, length);
                length = is.read(data, 0, 1024);
            }
            return bArrayOut.toByteArray();
        } catch (IOException e) {
            Log.e(tag, "Error reading file : " + filePath, e);
        } finally {
            if (is != null)
                try {
                    is.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            if (bArrayOut != null)
                try {
                    bArrayOut.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
        }
        return null;
    }

    /**
     * @param srcFile the source file, can not be a directory
     * @param dstFile the destination file, can be a directory.
     * @throws Exception
     */
    public static boolean copyFile(String srcFile, String dstFile) {
        // TODO Use Buffered streams
        if (srcFile == null || dstFile == null) {
            return false;
        }
        File _srcFile = new File(srcFile);
        // does not support directories
        if (!_srcFile.exists() || _srcFile.isDirectory())
            return false;
        File _dstFile = new File(dstFile);
        if (_dstFile.exists() && _dstFile.isDirectory()) {
            _dstFile = new File(dstFile + File.separator + _srcFile.getName());
        }
        InputStream is = null;
        try {
            is = new FileInputStream(_srcFile);
            saveDataToFile(is, _dstFile.getAbsolutePath());
            return true;
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
            return false;
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static boolean compressPicture(String srcFile, String dstFile) {
        boolean result;
        result = copyFile(srcFile, dstFile);
        File _srcFile = new File(srcFile);
        String targetPath = dstFile + File.separator + _srcFile.getName();
        File _dstFile = new File(targetPath);
        if (_dstFile.length() > 100000) {
            Bitmap bitmap = createImageThumbnail(targetPath);
            saveBitmapFile(bitmap, targetPath);
        }
        return result;
    }

    public static void saveBitmapFile(Bitmap bitmap, String _dstFile) {
        File file = new File(_dstFile);// 将要保存图片的路径
        try {
            BufferedOutputStream bos = new BufferedOutputStream(
                    new FileOutputStream(file));
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Bitmap createImageThumbnail(String filePath) {
        Bitmap bitmap = null;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, opts);
        opts.inSampleSize = computeSampleSize(opts, -1, 128 * 128);
        opts.inJustDecodeBounds = false;
        bitmap = BitmapFactory.decodeFile(filePath, opts);
        return bitmap;
    }

    public static int computeSampleSize(BitmapFactory.Options options,
            int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength,
                maxNumOfPixels);
        int roundedSize;
        if (initialSize <= 8) {
            roundedSize = 3;
        } else {
            roundedSize = (initialSize + 7) / 8 * 4;
        }
        return roundedSize;
    }

    private static int computeInitialSampleSize(BitmapFactory.Options options,
            int minSideLength, int maxNumOfPixels) {
        double w = options.outWidth;
        double h = options.outHeight;
        int lowerBound = (maxNumOfPixels == -1) ? 1 : (int) Math.ceil(Math
                .sqrt(w * h / maxNumOfPixels));
        int upperBound = (minSideLength == -1) ? 128 : (int) Math.min(
                Math.floor(w / minSideLength), Math.floor(h / minSideLength));
        if (upperBound < lowerBound) {
            return lowerBound;
        }
        if ((maxNumOfPixels == -1) && (minSideLength == -1)) {
            return 1;
        } else if (minSideLength == -1) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }

    public static String formatFileSize(long bytes) {
        if (bytes < Constants.KB) {
            return String.format(Constants.BYTE_FORMAT, (int) bytes);
        } else if (bytes < Constants.MB) {
            return String.format(Constants.KB_FORMAT, ((float) bytes)
                    / Constants.KB);
        } else if (bytes < Constants.GB) {
            return String.format(Constants.MB_FORMAT, ((float) bytes)
                    / Constants.MB);
        }
        return String.format(Constants.GB_FORMAT, ((float) bytes)
                / Constants.GB);
    }

    public static byte[] getFileContent(String file) throws IOException {
        if (file == null)
            throw new IllegalArgumentException("Invalid file path");
        ByteArrayOutputStream baos = null;
        InputStream is = null;
        try {
            is = new FileInputStream(new File(file));
            baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read = is.read(buffer);
            while (read >= 0) {
                baos.write(buffer, 0, read);
                read = is.read(buffer);
            }
            return baos.toByteArray();
        } finally {
            if (is != null)
                try {
                    is.close();
                } catch (IOException e) {
                }
            if (baos != null)
                try {
                    baos.close();
                } catch (IOException e) {
                }
        }
    }

    public static String getRealPathFromURI(Uri contentUri, Activity context) {
        String[] proj = {
                MediaStore.Images.Media.DATA
        };
        @SuppressWarnings("deprecation")
        Cursor cursor = context
                .managedQuery(contentUri, proj, null, null, null);
        if (cursor == null)
            return null;
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String filePath = cursor.getString(column_index);
        if (!TextUtils.isEmpty(filePath) && filePath.startsWith("file://")) {
            return filePath.substring("file://".length());
        }
        return filePath;
    }

    @SuppressWarnings("unchecked")
    public static int executeCommand(String cmd, Map<String, String> envs,
            OutputStream os, String... args) throws ExecuteException,
            IOException {
        if (cmd == null)
            throw new IllegalArgumentException("Invalid executable program");

        CommandLine command = new CommandLine(cmd);
        for (int i = 0; args != null && i < args.length; i++) {
            command.addArgument(args[i]);
        }

        // Add more environment variable if any
        @SuppressWarnings("rawtypes")
        Map env = EnvironmentUtils.getProcEnvironment();
        if (envs != null && envs.size() > 0) {
            Iterator<Map.Entry<String, String>> entryIt = envs.entrySet()
                    .iterator();
            while (entryIt.hasNext()) {
                @SuppressWarnings("rawtypes")
                Map.Entry entry = entryIt.next();
                env.put(entry.getKey(), entry.getValue());
            }
        }

        DefaultExecutor exec = new DefaultExecutor();
        exec.setWatchdog(new ExecuteWatchdog(Constants.BUGREPORT_EXEC_TIMEOUT));
        // setup the output
        if (os != null) {
            PumpStreamHandler psh = new PumpStreamHandler(os);
            exec.setStreamHandler(psh);
        }
        Log.d(tag, "Executing " + command.toString());
        return exec.execute(command, env);
    }

    public static String getMagicKeyNames(Context ctx) {
        String keyConf = Util.getSystemProperty("ro.bugreport.magickeys", null);
        if (TextUtils.isEmpty(keyConf))
            return null;
        String[] keycodes = keyConf.trim().split(",");
        try {
            StringBuilder keyNames = new StringBuilder();
            for (int i = 0; i < keycodes.length; i++) {
                String keycode = keycodes[i];
                Field resField = R.string.class.getField("KEYCODE_" + keycode);
                int resId = resField.getInt(null);
                String keyName = ctx.getString(resId);
                if (i == 0) {
                    keyNames.append(keyName);
                } else if (i == keycodes.length - 1) {
                    if (keycodes.length > 2)
                        keyNames.append(", ");
                    keyNames.append(ctx.getString(R.string.and))
                            .append(keyName);
                } else {
                    keyNames.append(", ").append(keyName);
                }
            }
            return keyNames.toString();
        } catch (NoSuchFieldException e) {
            Log.e(tag,
                    "Error finding key name for codes : "
                            + Arrays.toString(keycodes), e);
            return null;
        } catch (IllegalAccessException e) {
            Log.e(tag,
                    "Error finding key name for codes : "
                            + Arrays.toString(keycodes), e);
            return null;
        } catch (IllegalArgumentException e) {
            Log.e(tag,
                    "Error finding key name for codes : "
                            + Arrays.toString(keycodes), e);
            return null;
        }
    }

    private static final int BYTE_SIZE = 1024;

    public static String getRequestJson(List<NameValuePair> params) {
        JSONStringer js = new JSONStringer();
        try {
            js.object();
            for (int i = 0; i < params.size(); i++) {
                js.key(params.get(i).getName()).value(params.get(i).getValue());
            }
            js.endObject();
        } catch (JSONException e) {
            return null;
        }
        return js.toString();
    }

    public static String sizeToM(String size1) {
        Float size = Float.valueOf(size1);

        DecimalFormat df = new DecimalFormat("###.##");
        float f;
        if (size < BYTE_SIZE * BYTE_SIZE) {
            f = (float) ((float) size / (float) BYTE_SIZE);
            return df.format(new Float(f).doubleValue()) + "KB";
        } else if (size < BYTE_SIZE * BYTE_SIZE
                || size >= BYTE_SIZE * BYTE_SIZE * BYTE_SIZE) {
            f = (float) ((float) size / (float) (BYTE_SIZE * BYTE_SIZE * BYTE_SIZE));
            return df.format(new Float(f).doubleValue()) + "G";
        } else {
            f = (float) ((float) size / (float) (BYTE_SIZE * BYTE_SIZE));
            return df.format(new Float(f).doubleValue()) + "MB";
        }
    }

    public static String sizeToG(String size1) {
        Double size = Double.valueOf(size1);
        DecimalFormat df = new DecimalFormat("###.#");
        double result = (double) ((double) size / (double) (BYTE_SIZE
                * BYTE_SIZE * BYTE_SIZE));
        if (result >= BYTE_SIZE) {
            result = result / (double) BYTE_SIZE;
            return df.format(new Double(result).doubleValue()) + "T";
        }
        return df.format(new Double(result).doubleValue()) + "G";
    }

    public static String sendRequestFromHttpClient(String path,
            Map<String, String> params, String enc) {
        String strResult = null;
        List<NameValuePair> paramPairs = new ArrayList<NameValuePair>();
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                paramPairs.add(new BasicNameValuePair(entry.getKey(), entry
                        .getValue()));
            }
        }
        UrlEncodedFormEntity entitydata;
        try {
            entitydata = new UrlEncodedFormEntity(paramPairs,
                    enc);
            HttpPost post = new HttpPost(path); // form
            post.setEntity(entitydata);
            DefaultHttpClient client = new DefaultHttpClient();
            // client.setCookieStore(GlobalConstant.getCookieStore());
            HttpResponse response = client.execute(post);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                Header errorCode = response.getFirstHeader("RetCode");
                // Header msg = response.getFirstHeader("RetMsg");
                String sign = errorCode.getValue().toLowerCase();
                // if (errorCode != null && sign.endsWith("e")) {
                if (sign.endsWith("e")) {
                    return sign;
                }
                strResult = EntityUtils.toString(response.getEntity(), enc);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return strResult;
    }

    public static String sendGETRequest(String path, Map<String, String> params) {
        String result = "";
        StringBuilder sb = new StringBuilder();
        String tmp1 = "?";
        String tmp2 = "=";
        String tmp3 = "&";
        sb.append(path).append(tmp1);
        if (params != null & params.size() != 0) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                try {
                    sb.append(entry.getKey()).append(tmp2)
                            .append(URLEncoder.encode(entry.getValue(), "utf-8")).append(tmp3);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            sb.deleteCharAt(sb.length() - 1);
        }
        URL url = null;
        HttpURLConnection conn = null;
        try {
            url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            try {
                conn.setRequestMethod("GET");
            } catch (ProtocolException e1) {
                e1.printStackTrace();
            }
            conn.setDoInput(true);
            conn.setConnectTimeout(10000);
            conn.setRequestProperty("accept", "*/*");
            conn.connect();
            if (conn.getResponseCode() == 200) {
                InputStream inputStream = conn.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(
                        inputStream);
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String inputLine = null;

                while ((inputLine = reader.readLine()) != null) {
                    result += inputLine + "\n";
                }
                reader.close();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        conn.disconnect();
        return result;
    }

    public static String getFileSha1(File file) throws OutOfMemoryError,
            IOException {

        FileInputStream in = new FileInputStream(file);
        MessageDigest messagedigest;
        try {
            messagedigest = MessageDigest.getInstance("SHA-1");

            byte[] buffer = new byte[1024 * 1024 * 10];
            int len = 0;
            while ((len = in.read(buffer)) > 0) {
                messagedigest.update(buffer, 0, len);
            }
            return byte2hex(messagedigest.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            throw e;
        } finally {
            in.close();
        }
        return null;
    }

    public static String byte2hex(byte[] b) {
        StringBuffer hs = new StringBuffer(b.length);
        String stmp = "";
        int len = b.length;
        for (int n = 0; n < len; n++) {
            stmp = Integer.toHexString(b[n] & 0xFF);
            if (stmp.length() == 1) {
                hs = hs.append("0").append(stmp);
            } else {
                hs = hs.append(stmp);
            }
        }
        return String.valueOf(hs);
    }

    public static void showToast(Context context, int messageId) {
        Toast.makeText(context, messageId, Toast.LENGTH_SHORT).show();
    }

    public static boolean getConfigBool(Context context, int key) {
        return context.getResources().getBoolean(key);
    }
    public static boolean isUserVersion() {
        try {
            Class<?> systemPropertiesClass = null;
            systemPropertiesClass = Class
                    .forName("android.os.SystemProperties");
            Method get = systemPropertiesClass.getMethod("get", String.class);
            String versionType = (String) get.invoke(systemPropertiesClass, "ro.build.type");
            Log.d(tag, "versionType----->"+versionType);
            if ("user".equals(versionType)) {
                return true;
            } else {
                return false;
            }
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    public static String getTime() throws IOException {
        float totalttime = SystemClock.elapsedRealtime();
        BigDecimal b = new BigDecimal(totalttime / (1000 * 60 * 60));
        float time1 = b.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
        return String.valueOf(time1);
    }

    public static String getUpTime() throws IOException {
        float totalttime = SystemClock.uptimeMillis();
        BigDecimal b = new BigDecimal(totalttime / (1000 * 60 * 60));
        float time1 = b.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
        return String.valueOf(time1);
    }
    public static String getTimeNow(String style) {
        SimpleDateFormat formatter = new SimpleDateFormat(style);
        Date curDate = new Date(System.currentTimeMillis());// 获取当前时间
        String currenttime = formatter.format(curDate);
        return currenttime;
    }

    // 版本
    public static String getVersion() {
        Class<?> systemPropertiesClass = null;
        try {
            systemPropertiesClass = Class
                    .forName("android.os.SystemProperties");
            Method get = systemPropertiesClass.getMethod("get", String.class);
            String version = (String) get.invoke(systemPropertiesClass,
                    "ro.build.display.id");
            if (TextUtils.isEmpty(version))
                return "unknown";
            else
                return version;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return "unknow";
    }

    // 型号
    public static String getModel() {
        String model = android.os.Build.MODEL;
        return model;
    }
}
