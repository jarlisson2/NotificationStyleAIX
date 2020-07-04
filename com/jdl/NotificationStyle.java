package com.jdl.NotificationStyle;

import static android.content.Context.NOTIFICATION_SERVICE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesAssets;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.OnDestroyListener;
import com.google.appinventor.components.runtime.util.YailList;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

@DesignerComponent(version = 1, description = "Notification Style <br> Developed by Jarlisson", category = ComponentCategory.EXTENSION, nonVisible = true, iconName = "aiwebres/notification.png", helpUrl = "https://github.com/jarlisson2/NotificationStyleAIX")
@UsesAssets(fileNames = "favorite_border.png,favorite.png,next.png,pause.png,play.png,previous.png,reply.png")
@SuppressWarnings("deprecation")
@SimpleObject(external = true)

public class NotificationStyle extends AndroidNonvisibleComponent implements OnDestroyListener {
    public Activity activity;
    public Context context;
    public ComponentContainer container;
    public NotificationManager notifManager;
    public Notification.Builder builder;
    private final MediaSession mediaSession;
    private String channel = "ChannelA";
    private int importanceChannel = 2;
    private int priorityNotification = 2;
    private int colorNoti = -16777216;
    private String iconNotification = "";
    private String title;
    private String subtitle;
    private String largeIcon;
    private boolean favorite;
    private boolean pause;
    private String group;
    private String message;
    private String sender;
    private long timestamp;
    static List<Message> MESSAGES = new ArrayList<>();
    private static final String channelDefault = "ChannelA";
    private static final String iconNotificationDefault = "";
    private static final int importanceChannelDefault = 2;
    private static final int priorityNotificationDefault = 2;

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public synchronized void onReceive(final Context arg0, final Intent arg1) {
            if (arg1.getAction().equals("MUSIC_PLAY"))
                CallbackMusicPlayer("Play");
            else if (arg1.getAction().equals("MUSIC_PAUSE"))
                CallbackMusicPlayer("Pause");
            else if (arg1.getAction().equals("MUSIC_PREVIOUS"))
                CallbackMusicPlayer("Previous");
            else if (arg1.getAction().equals("MUSIC_NEXT"))
                CallbackMusicPlayer("Next");
            else if (arg1.getAction().equals("MUSIC_FAVORITE")) {
                favorite = !favorite;
                CallbackMusicPlayer(favorite ? "Favorite" : "Unfavorite");
                musicNotification(title, subtitle, largeIcon, pause, favorite);

            }
        }
    };

    BroadcastReceiver messageBroad = new BroadcastReceiver() {
        @Override
        public synchronized void onReceive(final Context arg0, final Intent arg1) {
            final Bundle remoteInput = RemoteInput.getResultsFromIntent(arg1);
            if (remoteInput != null) {
                final CharSequence replyText = remoteInput.getCharSequence("key_text_reply");

                final long timestampR = System.currentTimeMillis();
                final Message answer = new Message(replyText, null, timestampR);
                MESSAGES.add(answer);
                CallbackMessage(replyText.toString(), timestampR);
                group = arg1.getStringExtra("group");
                message = answer.toString();
                sender = "";
                timestamp = timestampR;
                // activity.unregisterReceiver(this);
            }
        }
    };

    public NotificationStyle(final ComponentContainer container) {
        super(container.$form());
        this.container = container;
        context = (Context) container.$context();
        activity = (Activity) context;
        Channel(channelDefault);
        ImportanceChannel(importanceChannelDefault);
        PriorityNotification(priorityNotificationDefault);
        IconNotification(iconNotificationDefault);
        mediaSession = new MediaSession(context, "tag");
        cancelAllNotification();
        activity.registerReceiver(receiver, new IntentFilter("MUSIC_FAVORITE"));
        activity.registerReceiver(receiver, new IntentFilter("MUSIC_PAUSE"));
        activity.registerReceiver(receiver, new IntentFilter("MUSIC_PLAY"));
        activity.registerReceiver(receiver, new IntentFilter("MUSIC_PREVIOUS"));
        activity.registerReceiver(receiver, new IntentFilter("MUSIC_NEXT"));
        activity.registerReceiver(messageBroad, new IntentFilter("MESSAGE_REPLY"));
        form.registerForOnDestroy(this);

    }

    private void sendNotification(final String title, final String subtitle, final boolean bigtext,
            final String bigPicture, final String largeIcon, final String[] listButtons, final String startValue,
            final int NOTIFY_ID) {
        initChannelNotification(SetPriority(importanceChannel, true), "Notif");

        final Bitmap icon = getBitmap(iconNotification, false);
        if (icon != null)
            builder.setSmallIcon(Icon.createWithBitmap(icon));
        else
            builder.setSmallIcon(android.R.drawable.ic_menu_info_details);
        builder.setContentTitle(title);
        builder.setContentText(subtitle);
        if (bigtext)
            builder.setStyle(new Notification.BigTextStyle().bigText(subtitle));
        builder.setAutoCancel(true);
        builder.setDefaults(Notification.DEFAULT_ALL);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            builder.setPriority(SetPriority(priorityNotification, false));
        builder.setColor(colorNoti);

        StartApp(startValue);

        final BroadcastReceiver actionBroad = new BroadcastReceiver() {
            @Override
            public synchronized void onReceive(final Context arg0, final Intent arg1) {
                final int notificationId = arg1.getIntExtra("notificationId", 0);
                if (notificationId > 0) {
                    final String nameAction = arg1.getAction();
                    final String url = arg1.getStringExtra("url");
                    if (notifManager != null)
                        notifManager.cancel(notificationId);
                    if (url.contains("://")) {
                        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        container.$context().startActivity(intent);
                    }
                    CallbackButtonAction(nameAction);
                    activity.unregisterReceiver(this);
                }
            }
        };

        for (final String button : listButtons) {
            String nameButton;
            String url = "";
            if (button.contains("|")) {
                nameButton = button.substring(0, button.indexOf("|"));
                url = button.substring(button.indexOf("|") + 1);
            } else
                nameButton = button;
            final Intent buttonIntent = new Intent(nameButton);
            buttonIntent.putExtra("notificationId", NOTIFY_ID);
            buttonIntent.putExtra("url", url);
            final PendingIntent btnAction = PendingIntent.getBroadcast(activity, 0, buttonIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(0, nameButton, btnAction);
            activity.registerReceiver(actionBroad, new IntentFilter(nameButton));
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                final Bitmap largeIconBitmap = getBitmap(largeIcon, false);
                if (largeIconBitmap != null)
                    builder.setLargeIcon(largeIconBitmap);
                final Bitmap bigPictureBitmap = getBitmap(bigPicture, false);
                if (bigPictureBitmap != null)
                    builder.setStyle(new Notification.BigPictureStyle().bigPicture(bigPictureBitmap));

                final Notification notification = builder.build();
                notifManager.notify(NOTIFY_ID, notification);

            }
        }).start();

    }

    private void musicNotification(final String title, final String subtitle, final String largeIcon,
            final boolean pause, final boolean favoriteB) {
        initChannelNotification(NotificationManager.IMPORTANCE_LOW, "NotifMusic");

        final Bitmap icon = getBitmap(iconNotification, false);
        if (icon != null)
            builder.setSmallIcon(Icon.createWithBitmap(icon));
        else
            builder.setSmallIcon(android.R.drawable.ic_menu_info_details);
        builder.setShowWhen(false);
        builder.setContentTitle(title);
        builder.setContentText(subtitle);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            builder.setPriority(Notification.PRIORITY_DEFAULT);
        builder.setAutoCancel(pause ? true : false);
        builder.setOngoing(pause ? false : true);

        StartApp("Music");

        final PendingIntent FAVORITE = PendingIntent.getBroadcast(activity, 0, new Intent("MUSIC_FAVORITE"),
                PendingIntent.FLAG_UPDATE_CURRENT);
        final PendingIntent PAUSE = PendingIntent.getBroadcast(activity, 0, new Intent("MUSIC_PAUSE"),
                PendingIntent.FLAG_UPDATE_CURRENT);
        final PendingIntent PLAY = PendingIntent.getBroadcast(activity, 0, new Intent("MUSIC_PLAY"),
                PendingIntent.FLAG_UPDATE_CURRENT);
        final PendingIntent MUSIC_PREVIOUS = PendingIntent.getBroadcast(activity, 0, new Intent("MUSIC_PREVIOUS"),
                PendingIntent.FLAG_UPDATE_CURRENT);
        final PendingIntent MUSIC_NEXT = PendingIntent.getBroadcast(activity, 0, new Intent("MUSIC_NEXT"),
                PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setStyle(new Notification.MediaStyle().setShowActionsInCompactView(1, 2, 3)
                .setMediaSession(mediaSession.getSessionToken()));

        final Bitmap bitmapFavorite = getBitmap(favoriteB ? "favorite.png" : "favorite_border.png", true);
        final Bitmap bitmapPrevious = getBitmap("previous.png", true);
        final Bitmap bitmapPause = getBitmap(pause ? "play.png" : "pause.png", true);
        final Bitmap bitmapNext = getBitmap("next.png", true);

        final Notification.Action favorite = new Notification.Action.Builder(Icon.createWithBitmap(bitmapFavorite),
                "Favorite", FAVORITE).build();
        final Notification.Action previous = new Notification.Action.Builder(Icon.createWithBitmap(bitmapPrevious),
                "Previous", MUSIC_PREVIOUS).build();
        final Notification.Action play = new Notification.Action.Builder(Icon.createWithBitmap(bitmapPause), "Pause",
                pause ? PLAY : PAUSE).build();
        final Notification.Action next = new Notification.Action.Builder(Icon.createWithBitmap(bitmapNext), "Next",
                MUSIC_NEXT).build();

        builder.addAction(favorite);
        builder.addAction(previous);
        builder.addAction(play);
        builder.addAction(next);

        builder.setSubText(subtitle);
        builder.setDefaults(Notification.DEFAULT_ALL);

        new Thread(new Runnable() {
            @Override
            public void run() {
                final Bitmap largeIconBitmap = getBitmap(largeIcon, false);
                if (largeIconBitmap != null)
                    builder.setLargeIcon(largeIconBitmap);
                final Notification notification = builder.build();
                notifManager.notify(33333, notification);
            }
        }).start();

    }

    private void notificationMessage(final String group, final String message, final String sender,
            final long timestamp) {
        initChannelNotification(NotificationManager.IMPORTANCE_HIGH, "NotifMesseg");

        StartApp("Message");

        final RemoteInput remoteInput = new RemoteInput.Builder("key_text_reply").setLabel("Your answer...").build();
        Intent replyIntent;
        PendingIntent replyPendingIntent = null;

        replyIntent = new Intent("MESSAGE_REPLY");
        replyIntent.putExtra("group", group);
        replyPendingIntent = PendingIntent.getBroadcast(context, 0, replyIntent, 0);

        final Bitmap bitmapReply = getBitmap("reply.png", true);
        final Notification.Action replyAction = new Notification.Action.Builder(Icon.createWithBitmap(bitmapReply),
                "Reply", replyPendingIntent).addRemoteInput(remoteInput).build();
        final Notification.MessagingStyle messagingStyle = new Notification.MessagingStyle("Me");
        messagingStyle.setConversationTitle(group);

        for (final Message chatMessage : MESSAGES) {
            final Notification.MessagingStyle.Message notificationMessage = new Notification.MessagingStyle.Message(
                    chatMessage.getText(), chatMessage.getTimestamp(), chatMessage.getSender());
            messagingStyle.addMessage(notificationMessage);
        }
        final Bitmap icon = getBitmap(iconNotification, false);
        if (icon != null)
            builder.setSmallIcon(Icon.createWithBitmap(icon));
        else
            builder.setSmallIcon(android.R.drawable.ic_menu_info_details);

        builder.setStyle(messagingStyle);
        builder.addAction(replyAction);
        builder.setColor(colorNoti);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            builder.setPriority(Notification.PRIORITY_HIGH);
        builder.setCategory(Notification.CATEGORY_MESSAGE);
        builder.setAutoCancel(true);
        builder.setOnlyAlertOnce(true);

        final Notification notification = builder.build();
        notifManager.notify(44444, notification);
    }

    private Bitmap getBitmap(final String nameImage, final Boolean external) {
        Bitmap bitmap = null;
        try {
            if (external) {
                final InputStream in = form.openAssetForExtension(NotificationStyle.this, nameImage);
                bitmap = BitmapFactory.decodeStream(in);
            } else if (nameImage != "") {
                InputStream in = null;
                if (nameImage.contains("://")) {
                    final URL url = new URL(nameImage);
                    bitmap = BitmapFactory.decodeStream((InputStream) url.getContent());
                } else {
                    in = nameImage.contains("/")
                            ? context.getContentResolver().openInputStream(Uri.fromFile(new File(nameImage)))
                            : container.$form().openAsset(nameImage);
                    bitmap = BitmapFactory.decodeStream(in);
                }
            }
            return bitmap;
        } catch (final IOException e) {
            e.printStackTrace();
            return bitmap;
        }

    }

    private void StartApp(final String startValue) {
        final int requestID = (int) System.currentTimeMillis();
        final Intent myIntent = new Intent();
        final String myApp = context.getPackageName();
        final String classNameActivity = activity.getLocalClassName();
        final String classNameApp = activity.getClass().getSimpleName();
        myIntent.setClassName(myApp,
                classNameActivity.equals(classNameApp) ? myApp + "." + classNameApp : classNameActivity);
        myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        myIntent.putExtra("APP_INVENTOR_START", startValue);
        final PendingIntent launchIntent = PendingIntent.getActivity(context, requestID, myIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(launchIntent);
    }

    private void initChannelNotification(final int importance, final String id) {
        if (notifManager == null)
            notifManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = notifManager.getNotificationChannel(id);
            if (mChannel == null) {
                mChannel = new NotificationChannel(id, channel, importance);
                notifManager.createNotificationChannel(mChannel);
            }
            mChannel.setImportance(importance);
            builder = new Notification.Builder(context, id);

        } else
            builder = new Notification.Builder(context);
    }

    private int SetPriority(final int p, final boolean channelBoolean) {
        int priority;
        switch (p) {
            case 0:
                priority = channelBoolean ? NotificationManager.IMPORTANCE_MIN : Notification.PRIORITY_MIN;
                break;
            case 1:
                priority = channelBoolean ? NotificationManager.IMPORTANCE_LOW : Notification.PRIORITY_LOW;
                break;
            case 2:
                priority = channelBoolean ? NotificationManager.IMPORTANCE_DEFAULT : Notification.PRIORITY_DEFAULT;
                break;
            case 3:
                priority = channelBoolean ? NotificationManager.IMPORTANCE_HIGH : Notification.PRIORITY_HIGH;
                break;
            case 4:
                priority = channelBoolean ? NotificationManager.IMPORTANCE_MAX : Notification.PRIORITY_MAX;
                break;
            default:
                priority = channelBoolean ? Notification.PRIORITY_DEFAULT : NotificationManager.IMPORTANCE_DEFAULT;
                break;
        }
        return priority;
    }

    private void cancelNotification(final int id) {
        final NotificationManager nMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        nMgr.cancel(id);
    }

    private void cancelAllNotification() {
        final NotificationManager nMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        nMgr.cancelAll();
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Set Icon notification.")
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_ASSET, defaultValue = iconNotificationDefault)
    public void IconNotification(final String path) {
        iconNotification = path;
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Starting in Android 8.0 (API level 26), all notifications must be assigned to a channel.")
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = channelDefault)
    public void Channel(final String channel) {
        this.channel = channel;
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Set priority channel.")
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = importanceChannelDefault
            + "")
    public void ImportanceChannel(final int importanceChannel) {
        this.importanceChannel = importanceChannel;
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Set priority notification.")
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = priorityNotificationDefault
            + "")
    public void PriorityNotification(final int priorityNotification) {
        this.priorityNotification = priorityNotification;
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Set priority notification.")
    @DesignerProperty(editorType = "color", defaultValue = Component.DEFAULT_VALUE_COLOR_BLACK)
    public void ColorNotification(final int argb) {
        this.colorNoti = argb;
    }

    @SimpleProperty(description = "Get path Icon assets.")
    public String IconNotification() {
        return iconNotification;
    }

    @SimpleProperty(description = "Get channnel name.")
    public String Channel() {
        return channel;
    }

    @SimpleProperty(description = "Get priority channel.")
    public int ImportanceChannel() {
        return importanceChannel;
    }

    @SimpleProperty(description = "Get priority notification.")
    public int PriorityNotification() {
        return priorityNotification;
    }

    @SimpleProperty(description = "Get color notification.")
    public int ColorNotification() {
        return colorNoti;
    }

    @SimpleFunction(description = "Creates a simple notification with title and subtitle, capable of displaying large texts in the subtitle.")
    public void SimpleNotification(final String title, final String subtitle, final boolean bigText,
            final String startValue, final int id) {
        sendNotification(title, subtitle, bigText, "", "", new String[] {}, startValue, id);
    }

    @SimpleFunction(description = "Displays a large icon in the notification with title and subtitle.")
    public void LargeIconNotification(final String title, final String subtitle, final boolean bigText,
            final String largeIcon, final String startValue, final int id) {
        sendNotification(title, subtitle, bigText, "", largeIcon, new String[] {}, startValue, id);
    }

    @SimpleFunction(description = "Creates a notification with a big picture and in addition it is possible to add title, subtitle and large icon.")
    public void BigPictureNotification(final String title, final String subtitle, final String bigPicture,
            final String largeIcon, final String startValue, final int id) {
        sendNotification(title, subtitle, false, bigPicture, largeIcon, new String[] {}, startValue, id);
    }

    @SimpleFunction(description = "Cancels a specific message.")
    public void CancelNotification(final int id) {
        cancelNotification(id);
    }

    @SimpleFunction(description = "With this block it is possible to create up to three action buttons in the notification and in addition, it is possible to add as a \"hyperlink\".")
    public void ActionNotification(final String title, final String subtitle, final boolean bigText,
            final String bigPicture, final String largeIcon, final YailList listButtons, final String startValue,
            final int id) {
        sendNotification(title, subtitle, bigText, bigPicture, largeIcon, listButtons.toStringArray(), startValue, id);
    }

    @SimpleFunction(description = "Starts the MediaStyle notification variables.")
    public void SetupMusicNotification(final String title, final String subtitle, final String largeIcon,
            final boolean favorite) {
        this.title = title;
        this.subtitle = subtitle;
        this.largeIcon = largeIcon;
        this.favorite = favorite;
    }

    @SimpleFunction(description = "Displays notification of MediaStyle with pause button.")
    public void PlayMusicNotification() {
        pause = false;
        musicNotification(title, subtitle, largeIcon, pause, favorite);
    }

    @SimpleFunction(description = "Displays notification of MediaStyle with play button.")
    public void PauseMusicNotification() {
        pause = true;
        musicNotification(title, subtitle, largeIcon, pause, favorite);
    }

    @SimpleFunction(description = "Through this block, you can show a notification of a received message with the ability to reply in the bar itself.")
    public void ReceiverMessageNotification(final String group, final String message, final String sender,
            final long timestamp) {
        MESSAGES.add(new Message(message, sender == "" ? null : sender, timestamp));
        notificationMessage(group, message, sender, timestamp);
    }

    @SimpleFunction(description = "If the user responds to the message in the notification, it will show a loading icon until that block is used.")
    public void ConfirmSendingMessage() {
        notificationMessage(group, message, sender, timestamp);
    }

    @SimpleFunction(description = "Gets whether the song is marked as a favorite.")
    public boolean GetFavorite() {
        return favorite;
    }

    @SimpleFunction(description = "Delete MediaStyle notification.")
    public void CancelMusicNotification() {
        cancelNotification(33333);
    }

    @SimpleFunction(description = "Cancels all notifications.")
    public void CancelAllNotification() {
        cancelAllNotification();
    }

    @SimpleFunction(description = "When calling the MessageReceiverNotification method, it stores the added messages and to remove, use this method.")
    public void ClearAllMessage() {
        MESSAGES = new ArrayList<>();
    }

    @SimpleEvent(description = "When clicking on any action button, the name of the respective button will be returned by that block.")
    public void CallbackButtonAction(final String nameAction) {
        EventDispatcher.dispatchEvent(this, "CallbackButtonAction", nameAction);
    }

    @SimpleEvent(description = "When clicking on any button of the media style notification, the name of the Action is returned in this block.")
    public void CallbackMusicPlayer(final String nameAction) {
        EventDispatcher.dispatchEvent(this, "CallbackMusicPlayer", nameAction);
    }

    @SimpleEvent(description = "Return of the message typed in the notification.")
    public void CallbackMessage(final String message, final long timestamp) {
        EventDispatcher.dispatchEvent(this, "CallbackMessage", message, timestamp);
    }

    @Override
    public void onDestroy() {
        if (notifManager == null)
            notifManager.cancel(33333);
    }

}