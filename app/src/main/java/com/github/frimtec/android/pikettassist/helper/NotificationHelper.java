package com.github.frimtec.android.pikettassist.helper;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.github.frimtec.android.pikettassist.R;
import com.github.frimtec.android.pikettassist.activity.MainActivity;
import com.github.frimtec.android.pikettassist.helper.SignalStrengthHelper.SignalLevel;

import java.util.Set;
import java.util.function.BiConsumer;

import static android.app.Notification.CATEGORY_ALARM;
import static android.app.Notification.CATEGORY_EVENT;
import static android.app.Notification.CATEGORY_REMINDER;


public class NotificationHelper {

  public static final int ALERT_NOTIFICATION_ID = 1;
  public static final int SHIFT_NOTIFICATION_ID = 2;
  public static final int SIGNAL_NOTIFICATION_ID = 3;
  public static final int MISSING_TEST_ALARM_NOTIFICATION_ID = 4;
  public static final int UPDATE_NOTIFICATION_ID = 5;
  public static final String ACTION_CLOSE_ALARM = "com.github.frimtec.android.pikettassist.CLOSE_ALARM";
  public static final String ACTION_UPDATE_NOW = "com.github.frimtec.android.pikettassist.UPDATE_NOW";
  private static final String CHANNEL_ID = "com.github.frimtec.android.pikettassist";

  public static void registerChannel(Context context) {
    CharSequence name = context.getString(R.string.channel_name);
    String description = context.getString(R.string.channel_description);
    NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH);
    channel.setDescription(description);
    NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
    notificationManager.createNotificationChannel(channel);
  }

  public static void notifyAlarm(Context context, Intent actionIntent, String action, String actionLabel, Intent notifyIntent) {
    actionIntent.setAction(action);
    PendingIntent confirmPendingIntent =
        PendingIntent.getBroadcast(context, 0, actionIntent, 0);

    notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    PendingIntent notifyPendingIntent = PendingIntent.getActivity(
        context, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT
    );

    String message = context.getString(R.string.notification_alert_text);
    Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle(context.getString(R.string.notification_alert_title))
        .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
        .setContentText(message)
        .setSmallIcon(R.drawable.ic_siren)
        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.notification_large_icon))
        .addAction(R.drawable.ic_siren, actionLabel, confirmPendingIntent)
        .setCategory(CATEGORY_ALARM)
        .setContentIntent(notifyPendingIntent)
        .setOnlyAlertOnce(true)
        .build();

    NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
    notificationManagerCompat.notify(ALERT_NOTIFICATION_ID, notification);
  }

  public static void notifyNewVersion(Context context, String newVersion, Intent updateIntent) {
    PendingIntent updatePendingIntent = PendingIntent.getBroadcast(context, 0, updateIntent, 0);
    Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle(String.format(context.getString(R.string.notification_update_available_title), newVersion))
        .setSmallIcon(R.drawable.ic_system_update_alt_black_24dp)
        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.notification_large_icon))
        .setCategory(CATEGORY_REMINDER)
        .setOnlyAlertOnce(true)
        .addAction(R.drawable.ic_siren, context.getString(R.string.notification_update_button), updatePendingIntent)
        .build();

    NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
    notificationManagerCompat.notify(UPDATE_NOTIFICATION_ID, notification);
  }

  public static void notifyMissingTestAlarm(Context context, Intent notifyIntent, Set<String> testContexts) {
    notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    PendingIntent notifyPendingIntent = PendingIntent.getActivity(
        context, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT
    );
    String message = context.getString(R.string.notification_missing_test_alert_text) + String.join(", ", testContexts);
    Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle(context.getString(R.string.notification_missing_test_alert_title))
        .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
        .setContentText(message)
        .setSmallIcon(R.drawable.ic_test_alarm)
        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.notification_large_icon))
        .setCategory(CATEGORY_ALARM)
        .setContentIntent(notifyPendingIntent)
        .setOnlyAlertOnce(true)
        .build();

    NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
    notificationManagerCompat.notify(MISSING_TEST_ALARM_NOTIFICATION_ID, notification);
  }

  public static void notifyShiftOn(Context context) {
    PendingIntent notifyPendingIntent = PendingIntent.getActivity(
        context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT
    );
    Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle(context.getString(R.string.notification_pikett_on_title))
        .setContentText(context.getString(R.string.notification_pikett_on_text))
        .setSmallIcon(R.drawable.ic_eye)
        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.notification_large_icon))
        .setCategory(CATEGORY_EVENT)
        .setOnlyAlertOnce(true)
        .setContentIntent(notifyPendingIntent)
        .build();
    NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
    notificationManagerCompat.notify(SHIFT_NOTIFICATION_ID, notification);
  }

  public static void notifySignalLow(Context context, SignalLevel level) {
    PendingIntent notifyPendingIntent = PendingIntent.getActivity(
        context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT
    );
    Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle(context.getString(R.string.notification_low_signal_title))
        .setContentText(String.format("%s: %s", context.getString(R.string.notification_low_signal_text), level.toString(context)))
        .setSmallIcon(R.drawable.ic_signal_cellular_connected_no_internet_1_bar_black_24dp)
        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.notification_large_icon))
        .setCategory(CATEGORY_EVENT)
        .setOnlyAlertOnce(true)
        .setContentIntent(notifyPendingIntent)
        .setAutoCancel(true)
        .build();

    NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
    notificationManagerCompat.notify(SIGNAL_NOTIFICATION_ID, notification);
  }

  public static void cancelNotification(Context context, int id) {
    NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
    notificationManagerCompat.cancel(id);
  }

  public static void infoDialog(Context context, int titleResourceId, int textResourceId, BiConsumer<DialogInterface, Integer> action) {
    SpannableString message = new SpannableString(Html.fromHtml(context.getString(textResourceId), Html.FROM_HTML_MODE_COMPACT));
    AlertDialog alertDialog = new AlertDialog.Builder(context)
        // set dialog message
        .setTitle(titleResourceId)
        .setMessage(message)
        .setCancelable(true)
        .setPositiveButton("OK", action::accept).create();
    alertDialog.show();
    ((TextView) alertDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
  }

  public static void requirePermissions(Context context, int titleResourceId, int textResourceId, BiConsumer<DialogInterface, Integer> action) {
    AlertDialog alertDialog = new AlertDialog.Builder(context)
        .setTitle(context.getString(R.string.permission_required) + " " + context.getString(titleResourceId))
        .setMessage(textResourceId)
        .setCancelable(true)
        .setPositiveButton("OK", action::accept)
        .create();
    alertDialog.show();
  }

  public static void areYouSure(Context context, DialogInterface.OnClickListener onYes, DialogInterface.OnClickListener onNo) {
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setMessage(R.string.general_are_you_sure)
        .setPositiveButton(R.string.general_yes, onYes)
        .setNegativeButton(R.string.general_no, onNo)
        .show();
  }
}
