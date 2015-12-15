
package com.qiku.bug_report.helper;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.qiku.bug_report.Constants;
import com.qiku.bug_report.R;
import com.qiku.bug_report.ui.ReportList;
import com.qiku.bug_report.ui.ReportViewer;
import com.qiku.bug_report.ui.SetupWizard;

import java.util.Hashtable;

public class Notifications {

    final static String tag = "BugReportNotifications";
    private static Hashtable<Integer, Long> mNotificationTimes = new Hashtable<Integer, Long>();

    /**
     * Cache the current time if no time is cached for the id.
     * 
     * @param id
     * @return the cached time if it exists, or the current time.
     */
    private static Long getNotificationTime(int id) {
        Integer key = Integer.valueOf(id);
        Long value = mNotificationTimes.get(key);
        if (value == null) {
            value = Long.valueOf(System.currentTimeMillis());
            mNotificationTimes.put(key, value);
        }
        return value;
    }

    // public static void showDEAMNotification(Context context, String scenario,
    // int id) {
    // Log.v(tag, "showDEAMNotification " + scenario);
    // CharSequence title = context.getText(R.string.bug_prompt) + scenario;
    // Notification notification = new Notification.Builder(context)
    // .setSmallIcon(android.R.drawable.stat_sys_download)
    // .setWhen(System.currentTimeMillis()).setContentTitle(title)
    // .setAutoCancel(true)
    // .setOngoing(true).build();
    // NotificationManager mNM = (NotificationManager) context
    // .getSystemService(Context.NOTIFICATION_SERVICE);
    // // mNM.notify(id, notification);
    // }

    // public static void showOnGoingNotification(Context context, String title,
    // String description, int id) {
    // Log.v(tag, "showOnGoingNotification " + id);
    // PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
    // new Intent(context, ReportList.class), 0);
    // Notification notification = new Notification.Builder(context)
    // .setSmallIcon(android.R.drawable.stat_sys_upload)
    // .setWhen(getNotificationTime(id)).setContentTitle(title)
    // .setContentText(description).setContentIntent(contentIntent)
    // .setAutoCancel(true)
    // .setOngoing(true).build();
    // NotificationManager mNM = (NotificationManager) context
    // .getSystemService(Context.NOTIFICATION_SERVICE);
    // // mNM.notify(id, notification);
    // }

    /*
     * public static void showCompleteNotification(Context context, String
     * title, String description, int id) { Log.v(tag,
     * "showCompleteNotification " + id); PendingIntent contentIntent =
     * PendingIntent.getActivity(context, 0, new Intent(context,
     * ReminderReportEditor.class), PendingIntent.FLAG_ONE_SHOT); Notification
     * notification = new Notification.Builder(context)
     * .setSmallIcon(android.R.drawable.stat_sys_upload)
     * .setWhen(getNotificationTime(id))
     * .setContentTitle(title).setContentText(description)
     * .setContentIntent(contentIntent)
     * .setAutoCancel(true)
     * //.setOngoing(true) .build(); NotificationManager mNM =
     * (NotificationManager)
     * context.getSystemService(Context.NOTIFICATION_SERVICE); mNM.notify(id,
     * notification); }
     */

    // public static void showProgressNotification(Context context, String
    // title,
    // int max, int progress, int id, boolean indeterminate) {
    // PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
    // new Intent(context, ReportList.class), 0);
    //
    // Notification notification = new Notification.Builder(context)
    // .setSmallIcon(android.R.drawable.stat_sys_upload)
    // .setWhen(getNotificationTime(id))
    // .setContentTitle(title)
    // .setContentText(context.getString(R.string.transmitting_report))
    // .setContentIntent(contentIntent).setAutoCancel(true)
    // .build();
    // NotificationManager mNM = (NotificationManager) context
    // .getSystemService(Context.NOTIFICATION_SERVICE);
    // // mNM.notify(id, notification);
    // }

    // public static void showUploadStopNotification(Context context,
    // String title, String msg, int max, int progress, int id) {
    // Log.v(tag, "showUploadCompletedNotification " + id);
    // PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
    // new Intent(context, ReminderReportEditor.class), 0);
    // Notification notification = new Notification.Builder(context)
    // .setSmallIcon(R.drawable.upload)
    // .setWhen(getNotificationTime(id)).setContentTitle(title)
    // .setContentText(msg).setContentIntent(contentIntent)
    // .setAutoCancel(true)
    // .build();
    // NotificationManager mNM = (NotificationManager) context
    // .getSystemService(Context.NOTIFICATION_SERVICE);
    // mNM.notify(id, notification);
    // }

    public static void showUploadManualStopNotification(Context context,
            String title, String msg, int max, int progress, long id) {
        if (Util.isUserVersion()) {
            return ;
        }
        int newId = (int) id;
        Intent viewReport = new Intent(context, ReportViewer.class);
        viewReport.setAction(Constants.BUGREPORT_INTENT_VIEW_REPORT);
        viewReport.putExtra("reportid", String.valueOf(id));
        Log.i("Notification", String.valueOf(id));
        PendingIntent contentIntent;
        if (msg.equals(R.string.transmit_paused)) {
            contentIntent = PendingIntent.getActivity(context, 0,
                    new Intent(context, ReportList.class), 0);
        } else {
            contentIntent = PendingIntent.getActivity(context, newId,
                    viewReport, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        Notification notification = new Notification.Builder(context)
                .setSmallIcon(R.drawable.upload)
                .setWhen(getNotificationTime(newId)).setContentTitle(title)
                .setContentText(msg).setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build();
        NotificationManager mNM = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNM.notify(newId, notification);
    }

    public static void showUploadErrorNotification(Context context,
            String title, int message, int id) {
        if (Util.isUserVersion()) {
            return ;
        }
        Log.v(tag, "showUploadErrorNotification " + id);
        Intent intent = new Intent(context, ReportList.class);
        intent.setAction(Constants.BUGREPORT_INTENT_BUGREPORT_END);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                intent, 0);
        Notification notification = new Notification.Builder(context)
                .setSmallIcon(R.drawable.upload_failed).setContentTitle(title)
                .setContentText(context.getText(message))
                .setContentIntent(contentIntent).setAutoCancel(true)
                .build();
        NotificationManager mNM = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNM.notify(id, notification);
    }

    public static void showCachedReportNotification(Context context,
            int numOfReports) {
        if (Util.isUserVersion()) {
            return ;
        }
        Log.d(tag, "showCachedReportNotification");
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, ReportList.class), 0);
        CharSequence text = context
                .getText(R.string.notofication_cached_report_title);
        Resources res = context.getResources();
        Notification notification = new Notification.Builder(context)
                .setSmallIcon(R.drawable.bug_notify_small)
                .setLargeIcon(
                        BitmapFactory.decodeResource(res,
                                R.drawable.bug_notify_normal))
                .setWhen(System.currentTimeMillis())
                .setContentTitle(text)
                .setContentText(
                        String.format(
                                context.getText(
                                        R.string.notofication_cached_report_info)
                                        .toString(),
                                numOfReports,
                                numOfReports > 1
                                        && context.getResources()
                                                .getConfiguration().locale
                                                .getCountry().equals("EN") ? "s"
                                        : "")).setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build();
        NotificationManager mNM = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNM.notify(R.layout.report_list_row, notification); // notification
    }

    public static void showNetworkNotification(Context context, int numOfReports) {
        if (Util.isUserVersion()) {
            return ;
        }
        Log.d(tag, "showNetworkNotification");
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, ReportList.class), 0);
        CharSequence text = context
                .getText(R.string.notification_cached_report_network_title);
        Resources res = context.getResources();
        Notification notification = new Notification.Builder(context)
                .setSmallIcon(R.drawable.bug_notify_small)
                .setLargeIcon(
                        BitmapFactory.decodeResource(res,
                                R.drawable.bug_notify_normal))
                .setWhen(System.currentTimeMillis())
                .setContentTitle(text)
                .setContentText(
                        String.format(
                                context.getText(
                                        R.string.notofication_cached_report_info)
                                        .toString(),
                                numOfReports,
                                numOfReports > 1
                                        && context.getResources()
                                                .getConfiguration().locale
                                                .getCountry().equals("EN") ? "s"
                                        : "")).setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build();
        NotificationManager mNM = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNM.notify(R.layout.report_list_row, notification); // notification
    }

    public static void showNotification(Context context, int title, int msg,
            int id) {
        if (Util.isUserVersion()) {
            return ;
        }
        Log.d(tag, "showNotification");
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                new Intent(), 0);
        Resources res = context.getResources();
        Notification notification = new Notification.Builder(context)
                .setSmallIcon(R.drawable.bug_notify_small)
                .setLargeIcon(
                        BitmapFactory.decodeResource(res,
                                R.drawable.bug_notify_normal))
                .setWhen(getNotificationTime(id))
                .setContentTitle(context.getText(title))
                .setContentText(context.getText(msg))
                .setContentIntent(contentIntent).setAutoCancel(true)
                .build();

        NotificationManager mNM = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNM.notify(id, notification);
    }

    public static void showUserInfoMissingNotification(Context context) {
        if (Util.isUserVersion()) {
            return ;
        }
        Log.d(tag, "showUserInfoMissingNotification");
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, SetupWizard.class), 0);
        CharSequence title = context
                .getText(R.string.notification_title_incomplete_setup);
        CharSequence msg = context
                .getText(R.string.notification_msg_incomplete_setup);
        Resources res = context.getResources();
        Notification notification = new Notification.Builder(context)
                .setSmallIcon(R.drawable.bug_notify_small)
                .setLargeIcon(
                        BitmapFactory.decodeResource(res,
                                R.drawable.bug_notify_normal))
                .setWhen(System.currentTimeMillis()).setContentTitle(title)
                .setContentText(msg).setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build();

        NotificationManager mNM = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNM.notify(R.string.notification_title_incomplete_setup, notification);
    }

    public static void cancel(Context context, int id) {
        NotificationManager mNM = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNM.cancel(id);
        mNotificationTimes.remove(Integer.valueOf(id));
    }
}
