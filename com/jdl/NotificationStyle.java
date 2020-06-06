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
import android.os.StrictMode;

@DesignerComponent(version = 1, description = "Notification Style <br> Developed by Jarlisson", category = ComponentCategory.EXTENSION, nonVisible = true, iconName = "aiwebres/notification.png", helpUrl = "https://github.com/jarlisson2/NotificationStyleAIX")
@SuppressWarnings("deprecation")
@SimpleObject(external = true)
@UsesAssets(fileNames = "favorite_border.png,favorite.png,next.png,pause.png,play.png,previous.png,reply.png")

public class NotificationStyle extends AndroidNonvisibleComponent {
    public Activity activity;
    public Context context;
    public ComponentContainer container;
    public NotificationManager notifManager;
    public Notification.Builder builder;
    private MediaSession mediaSession;
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
        public synchronized void onReceive(Context arg0, Intent arg1) {
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
        public synchronized void onReceive(Context arg0, Intent arg1) {
            Bundle remoteInput = RemoteInput.getResultsFromIntent(arg1);
            if (remoteInput != null) {
                CharSequence replyText = remoteInput.getCharSequence("key_text_reply");

                long timestampR = System.currentTimeMillis();
                Message answer = new Message(replyText, null, timestampR);
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

    public NotificationStyle(ComponentContainer container) {
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

    }

    private void sendNotification(String title, String subtitle, boolean bigtext, String bigPicture, String largeIcon,
            String[] listButtons, String startValue, int NOTIFY_ID) {
        initChannelNotification(SetPriority(importanceChannel, true), "Notif");

        Bitmap icon = getBitmap(iconNotification, false);
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)builder.setPriority(SetPriority(priorityNotification, false));
        builder.setColor(colorNoti);

        int requestID = (int) System.currentTimeMillis();
        Intent myIntent = new Intent();
        String myApp = context.getPackageName();
        myIntent.setClassName(myApp, myApp + ".Screen1");
        myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        myIntent.putExtra("APP_INVENTOR_START", startValue);
        PendingIntent launchIntent = PendingIntent.getActivity(context, requestID, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(launchIntent);

        BroadcastReceiver actionBroad = new BroadcastReceiver() {
            @Override
            public synchronized void onReceive(Context arg0, Intent arg1) {
                int notificationId = arg1.getIntExtra("notificationId", 0);
                if (notificationId > 0) {
                    String nameAction = arg1.getAction();
                    String url = arg1.getStringExtra("url");
                    if (notifManager != null)
                        notifManager.cancel(notificationId);
                    if (url.contains("://")) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        container.$context().startActivity(intent);
                    }
                    CallbackButtonAction(nameAction);
                    activity.unregisterReceiver(this);
                }
            }
        };

        for (String button : listButtons) {
            String nameButton;
            String url = "";
            if (button.contains("|")) {
                nameButton = button.substring(0, button.indexOf("|"));
                url = button.substring(button.indexOf("|") + 1);
            } else
                nameButton = button;
            Intent buttonIntent = new Intent(nameButton);
            buttonIntent.putExtra("notificationId", NOTIFY_ID);
            buttonIntent.putExtra("url", url);
            PendingIntent btnAction = PendingIntent.getBroadcast(activity, 0, buttonIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(0, nameButton, btnAction);
            activity.registerReceiver(actionBroad, new IntentFilter(nameButton));
        }
        Bitmap largeIconBitmap = getBitmap(largeIcon, false);
        if (largeIconBitmap != null)
            builder.setLargeIcon(largeIconBitmap);
        Bitmap bigPictureBitmap = getBitmap(bigPicture, false);
        if (bigPictureBitmap != null)
            builder.setStyle(new Notification.BigPictureStyle().bigPicture(bigPictureBitmap));

        Notification notification = builder.build();
        notifManager.notify(NOTIFY_ID, notification);

    }

    private void musicNotification(String title, String subtitle, String largeIcon, boolean pause, boolean favoriteB) {
        initChannelNotification(NotificationManager.IMPORTANCE_LOW, "NotifMusic");

        Bitmap icon = getBitmap(iconNotification, false);
        if (icon != null)
            builder.setSmallIcon(Icon.createWithBitmap(icon));
        else
            builder.setSmallIcon(android.R.drawable.ic_menu_info_details);
        builder.setShowWhen(false);
        builder.setContentTitle(title);
        builder.setContentText(subtitle);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)builder.setPriority(Notification.PRIORITY_DEFAULT);
        builder.setAutoCancel(pause ? true : false);
        builder.setOngoing(pause ? false : true);

        int requestID = (int) System.currentTimeMillis();
        Intent myIntent = new Intent();
        String myApp = context.getPackageName();
        myIntent.setClassName(myApp, myApp + ".Screen1");
        myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        myIntent.putExtra("APP_INVENTOR_START", "Music");
        PendingIntent launchIntent = PendingIntent.getActivity(context, requestID, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(launchIntent);

        Bitmap largeIconBitmap = getBitmap(largeIcon, false);
        if (largeIconBitmap != null)
            builder.setLargeIcon(largeIconBitmap);

        PendingIntent FAVORITE = PendingIntent.getBroadcast(activity, 0, new Intent("MUSIC_FAVORITE"),
                PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent PAUSE = PendingIntent.getBroadcast(activity, 0, new Intent("MUSIC_PAUSE"),
                PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent PLAY = PendingIntent.getBroadcast(activity, 0, new Intent("MUSIC_PLAY"),
                PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent MUSIC_PREVIOUS = PendingIntent.getBroadcast(activity, 0, new Intent("MUSIC_PREVIOUS"),
                PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent MUSIC_NEXT = PendingIntent.getBroadcast(activity, 0, new Intent("MUSIC_NEXT"),
                PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setStyle(new Notification.MediaStyle().setShowActionsInCompactView(1, 2, 3)
                .setMediaSession(mediaSession.getSessionToken()));

        Bitmap bitmapFavorite = getBitmap(favoriteB ? "favorite.png" : "favorite_border.png", true);
        Bitmap bitmapPrevious = getBitmap("previous.png", true);
        Bitmap bitmapPause = getBitmap(pause ? "play.png" : "pause.png", true);
        Bitmap bitmapNext = getBitmap("next.png", true);

        Notification.Action favorite = new Notification.Action.Builder(Icon.createWithBitmap(bitmapFavorite),
                "Favorite", FAVORITE).build();
        Notification.Action previous = new Notification.Action.Builder(Icon.createWithBitmap(bitmapPrevious),
                "Previous", MUSIC_PREVIOUS).build();
        Notification.Action play = new Notification.Action.Builder(Icon.createWithBitmap(bitmapPause), "Pause",
                pause ? PLAY : PAUSE).build();
        Notification.Action next = new Notification.Action.Builder(Icon.createWithBitmap(bitmapNext), "Next",
                MUSIC_NEXT).build();

        builder.addAction(favorite);
        builder.addAction(previous);
        builder.addAction(play);
        builder.addAction(next);

        builder.setSubText(subtitle);
        builder.setDefaults(Notification.DEFAULT_ALL);

        Notification notification = builder.build();
        notifManager.notify(33333, notification);

    }

    private void notificationMessage(String group, String message, String sender, long timestamp) {
        initChannelNotification(NotificationManager.IMPORTANCE_HIGH, "NotifMesseg");
        int requestID = (int) System.currentTimeMillis();
        Intent myIntent = new Intent();
        String myApp = context.getPackageName();
        myIntent.setClassName(myApp, myApp + ".Screen1");
        myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        myIntent.putExtra("APP_INVENTOR_START", "Message");
        PendingIntent launchIntent = PendingIntent.getActivity(context, requestID, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(launchIntent);

        RemoteInput remoteInput = new RemoteInput.Builder("key_text_reply").setLabel("Your answer...").build();
        Intent replyIntent;
        PendingIntent replyPendingIntent = null;

        replyIntent = new Intent("MESSAGE_REPLY");
        replyIntent.putExtra("group", group);
        replyPendingIntent = PendingIntent.getBroadcast(context, 0, replyIntent, 0);

        Bitmap bitmapReply = getBitmap("reply.png", true);
        Notification.Action replyAction = new Notification.Action.Builder(Icon.createWithBitmap(bitmapReply), "Reply",
                replyPendingIntent).addRemoteInput(remoteInput).build();
        Notification.MessagingStyle messagingStyle = new Notification.MessagingStyle("Me");
        messagingStyle.setConversationTitle(group);

        for (Message chatMessage : MESSAGES) {
            Notification.MessagingStyle.Message notificationMessage = new Notification.MessagingStyle.Message(
                    chatMessage.getText(), chatMessage.getTimestamp(), chatMessage.getSender());
            messagingStyle.addMessage(notificationMessage);
        }
        Bitmap icon = getBitmap(iconNotification, false);
        if (icon != null)
            builder.setSmallIcon(Icon.createWithBitmap(icon));
        else
            builder.setSmallIcon(android.R.drawable.ic_menu_info_details);

        builder.setStyle(messagingStyle);
        builder.addAction(replyAction);
        builder.setColor(colorNoti);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)builder.setPriority(Notification.PRIORITY_HIGH);
        builder.setCategory(Notification.CATEGORY_MESSAGE);
        builder.setAutoCancel(true);
        builder.setOnlyAlertOnce(true);

        Notification notification = builder.build();
        notifManager.notify(44444, notification);
    }

    private Bitmap getBitmap(String nameImage, Boolean external) {
        Bitmap bitmap = null;
        try {
            if (external) {
                InputStream in = form.openAssetForExtension(NotificationStyle.this, nameImage);
                bitmap = BitmapFactory.decodeStream(in);
            } else if (nameImage != "") {
                InputStream in = null;
                if (nameImage.contains("://")) {
                    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                    StrictMode.setThreadPolicy(policy);
                    URL url = new URL(nameImage);
                    bitmap = BitmapFactory.decodeStream((InputStream) url.getContent());
                } else {
                    in = nameImage.contains("/")
                            ? context.getContentResolver().openInputStream(Uri.fromFile(new File(nameImage)))
                            : container.$form().openAsset(nameImage);
                    bitmap = BitmapFactory.decodeStream(in);
                }
            }
            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return bitmap;
        }

    }

    private void initChannelNotification(int importance, String id) {
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

    private int SetPriority(int p, boolean channelBoolean) {
        int priority = channelBoolean ? Notification.PRIORITY_DEFAULT : NotificationManager.IMPORTANCE_DEFAULT;

        switch (p) {
            case 0:
                priority = channelBoolean ? NotificationManager.IMPORTANCE_MIN : Notification.PRIORITY_MIN;
            case 1:
                priority = channelBoolean ? NotificationManager.IMPORTANCE_LOW : Notification.PRIORITY_LOW;
            case 2:
                priority = channelBoolean ? NotificationManager.IMPORTANCE_DEFAULT : Notification.PRIORITY_DEFAULT;
            case 3:
                priority = channelBoolean ? NotificationManager.IMPORTANCE_HIGH : Notification.PRIORITY_HIGH;
            case 4:
                priority = channelBoolean ? NotificationManager.IMPORTANCE_MAX : Notification.PRIORITY_MAX;
        }
        return priority;
    }

    private void cancelNotification(int id) {
        NotificationManager nMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        nMgr.cancel(id);
    }

    private void cancelAllNotification() {
        NotificationManager nMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        nMgr.cancelAll();
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Set Icon notification.")
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_ASSET, defaultValue = iconNotificationDefault)
    public void IconNotification(String path) {
        iconNotification = path;
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Starting in Android 8.0 (API level 26), all notifications must be assigned to a channel.")
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = channelDefault)
    public void Channel(String channel) {
        this.channel = channel;
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Set priority channel.")
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = importanceChannelDefault
            + "")
    public void ImportanceChannel(int importanceChannel) {
        this.importanceChannel = importanceChannel;
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Set priority notification.")
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = priorityNotificationDefault
            + "")
    public void PriorityNotification(int priorityNotification) {
        this.priorityNotification = priorityNotification;
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Set priority notification.")
    @DesignerProperty(editorType = "color", defaultValue = Component.DEFAULT_VALUE_COLOR_BLACK)
    public void ColorNotification(int argb) {
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
    public void SimpleNotification(String title, String subtitle, boolean bigText, String startValue, int id) {
        sendNotification(title, subtitle, bigText, "", "", new String[] {}, startValue, id);
    }

    @SimpleFunction(description = "Displays a large icon in the notification with title and subtitle.")
    public void LargeIconNotification(String title, String subtitle, boolean bigText, String largeIcon,
            String startValue, int id) {
        sendNotification(title, subtitle, bigText, "", largeIcon, new String[] {}, startValue, id);
    }

    @SimpleFunction(description = "Creates a notification with a big picture and in addition it is possible to add title, subtitle and large icon.")
    public void BigPictureNotification(String title, String subtitle, String bigPicture, String largeIcon,
            String startValue, int id) {
        sendNotification(title, subtitle, false, bigPicture, largeIcon, new String[] {}, startValue, id);
    }

    @SimpleFunction(description = "Cancels a specific message.")
    public void CancelNotification(int id) {
        cancelNotification(id);
    }

    @SimpleFunction(description = "With this block it is possible to create up to three action buttons in the notification and in addition, it is possible to add as a \"hyperlink\".")
    public void ActionNotification(String title, String subtitle, boolean bigText, String bigPicture, String largeIcon,
            YailList listButtons, String startValue, int id) {
        sendNotification(title, subtitle, bigText, bigPicture, largeIcon, listButtons.toStringArray(), startValue, id);
    }

    @SimpleFunction(description = "Starts the MediaStyle notification variables.")
    public void SetupMusicNotification(String title, String subtitle, String largeIcon, boolean favorite) {
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
    public void ReceiverMessageNotification(String group, String message, String sender, long timestamp) {
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
    public void CallbackButtonAction(String nameAction) {
        EventDispatcher.dispatchEvent(this, "CallbackButtonAction", nameAction);
    }

    @SimpleEvent(description = "When clicking on any button of the media style notification, the name of the Action is returned in this block.")
    public void CallbackMusicPlayer(String nameAction) {
        EventDispatcher.dispatchEvent(this, "CallbackMusicPlayer", nameAction);
    }

    @SimpleEvent(description = "Return of the message typed in the notification.")
    public void CallbackMessage(String message, long timestamp) {
        EventDispatcher.dispatchEvent(this, "CallbackMessage", message, timestamp);
    }

}