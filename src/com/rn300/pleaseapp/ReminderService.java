package com.rn300.pleaseapp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.rn300.pleaseapp.lists.chores.items.choreitem.ChoreItem;

public class ReminderService extends IntentService{
	static final String serviceName = "ReminderService";
	
	public static final String CREATE = "CREATE";
	public static final String BOOT = "BOOT";
    public static final String CANCEL = "CANCEL";
     
    private IntentFilter matcher;
	
	public ReminderService() {
		super(serviceName);
		matcher = new IntentFilter();
        matcher.addAction(CREATE);
        matcher.addAction(BOOT);
        matcher.addAction(CANCEL);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String action = intent.getAction();
        
        if (matcher.matchAction(action)) {          
            try {
            	if(action.equals(BOOT)){
            		Log.d(serviceName, "Resseting all reminders");
            		resetAllReminders();
            	}else{
            		String choreString = intent.getStringExtra(ApiService.CHORE_STRING);
            		execute(action, choreString);
            	}
			} catch (JSONException e) {
				e.printStackTrace();
			}
        }
	}
	
	private void resetAllReminders() throws JSONException{
		String ownId = ApiService.getOwnId(this);
		String choreString = ApiService.retrieveChores(this,ownId);
		
		JSONArray chores = new JSONArray(choreString);
		
		for(int i = 0; i < chores.length(); i++){
			JSONObject chore = chores.getJSONObject(i);
			if(chore.has(ChoreItem.REMINDER)){
				execute(this.CREATE,chore.toString());
			}
		}
	}
	
	private void execute(String action, String choreString) throws JSONException {
		JSONObject chore = new JSONObject(choreString);
		AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		PendingIntent pi = createPendingIntent(this,chore);
		
		JSONObject reminderObject = chore.getJSONObject(ChoreItem.REMINDER);
        long time = reminderObject.getLong(ChoreItem.REMINDER_TIME);
        
        if (CREATE.equals(action)) {
            am.set(AlarmManager.RTC_WAKEUP, time, pi);          
        } else if (CANCEL.equals(action)) {
            
        }
	}
	
	public static void putReminder(Context ctx, JSONObject chore) throws JSONException{
		AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

		PendingIntent pi = createPendingIntent(ctx, chore);
		
        long time = chore.getJSONObject(ChoreItem.REMINDER).getLong(ChoreItem.REMINDER_TIME);
        
        am.set(AlarmManager.RTC_WAKEUP, time, pi);          
	}
	
	public static void cancelReminder(Context ctx, JSONObject chore) throws JSONException{
		AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		PendingIntent pi = createPendingIntent(ctx, chore);
		
		am.cancel(pi);       
	}
	
	
	private static PendingIntent createPendingIntent(Context ctx, JSONObject chore) throws JSONException{    
        Intent i = new Intent(ctx, ReminderReceiver.class);
        JSONObject reminderObject = chore.getJSONObject(ChoreItem.REMINDER);
       
        i.putExtra("id", reminderObject.getLong(ChoreItem.REMINDER_ID));
        i.putExtra("msg", chore.toString());
        // TODO need maybe a different way of differentiating the intents? 
        i.addCategory(chore.getString(ChoreItem.ID));
        return PendingIntent.getBroadcast(ctx, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
	}
}
