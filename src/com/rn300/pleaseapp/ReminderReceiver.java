package com.rn300.pleaseapp;

import org.json.JSONException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.rn300.pleaseapp.activities.TabsActivity;
import com.rn300.pleaseapp.lists.chores.items.choreitem.ChoreItem;

public class ReminderReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			String msg = intent.getStringExtra("msg");	
		    ChoreItem chore = new ChoreItem(context,msg);
		    chore.highlight();
		    chore.removeReminder();
		    ApiService.putChore(context, chore.toJSON(), false);
			sendNotification(context,intent);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
    private void sendNotification(Context context, Intent intent) throws JSONException {
    	long id = intent.getLongExtra("id", 0);
        String msg = intent.getStringExtra("msg");
    	
        ChoreItem chore = new ChoreItem(context,msg);
        String choreName = chore.getString(ChoreItem.TITLE);
        String choreDetails = chore.getString(ChoreItem.DETAILS);
        
    	NotificationManager mNotificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, TabsActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
        .setSmallIcon(com.rn300.pleaseapp.R.drawable.ic_notification_reminder)
        .setContentTitle(choreName)
        .setStyle(new NotificationCompat.BigTextStyle()
        .bigText(choreDetails))
        .setContentText(choreDetails)
        .setTicker(choreName)
        .setAutoCancel(true)
        .setSound(Uri.parse(GlobalState.getReminderRingtone()));
        
        if(GlobalState.vibrateOnReminder()) mBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
        
       
        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify((int)id, mBuilder.build());
    }

}
