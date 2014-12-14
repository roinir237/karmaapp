package com.rn300.pleaseapp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import android.app.Application;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;

public class GlobalState extends Application {
	private static HashMap<String,Integer> notificationNumberMap = new HashMap<String,Integer>();
	
	final static String PREFS_APPLICATION = "applicationPrefs";
	
	final static String RINGTONE_PREF = "task_ringtone_pref";
	final static String DATEFORMAT_PREF = "date_format";
	final static String IS24HOUR_PREF = "time_format";
	final static String ENABLE_DEFAULT_REMINDER_TIME = "enable_defualt_reminder_time";
	final static String DEFAULT_REMINDER_TIME = "reminder_time";
	
	public static SharedPreferences sp;
  
	public void onCreate() {
		super.onCreate();
	     
		PreferenceManager.setDefaultValues(this, PREFS_APPLICATION, 0, R.xml.application_settings, false);
	    sp = PreferenceManager.getDefaultSharedPreferences(this);      
	}
	
	public static String getRingtone() {
		Uri defUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		return sp.getString(RINGTONE_PREF, defUri.toString());
	}    
	
	public static boolean vibrateOnTask() {
		return sp.getBoolean("task_vibrate_pref", true);
	}    
	
	public static SimpleDateFormat getDateFormat(){
		String formatString = sp.getString(DATEFORMAT_PREF, "dd/MM/yyyy");
		SimpleDateFormat sf = new SimpleDateFormat(formatString);
		sf.setTimeZone(Calendar.getInstance().getTimeZone());
		return sf;
	}
	
	public static SimpleDateFormat getTimeFormat(){
		SimpleDateFormat sf;
		
		if(is24HourFormat()){
			sf = new SimpleDateFormat("HH:mm");
		} else {
			sf = new SimpleDateFormat("hh:mm a");
		}
		
		sf.setTimeZone(Calendar.getInstance().getTimeZone());
		return sf;
	}
	
	public static boolean is24HourFormat(){
		return sp.getBoolean(IS24HOUR_PREF,true);
	}
	
	public static int getRemindBeforeTime(){
		return Integer.parseInt(sp.getString(DEFAULT_REMINDER_TIME, "5"));
	}
	
	public static boolean defaultReminderEnabled(){
		return sp.getBoolean(ENABLE_DEFAULT_REMINDER_TIME,true);
	}
	
	public static boolean vibrateOnReminder(){
		return sp.getBoolean("reminder_vibrate_pref", true);
	}
	
	public static String getReminderRingtone(){
		Uri defUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		return sp.getString("reminder_ringtone_pref", defUri.toString());
	}
	
	public static boolean notifyOnNew(){
		return sp.getBoolean("task_on_new_pref", true);
	}
	
	public static boolean notifyOnComplete(){
		return sp.getBoolean("task_on_complete_pref", true);
	}
	
	public static boolean notifyOnApproved(){
		return sp.getBoolean("task_on_approved_pref", false);
	}

	public static int updateNotificationNumber(String id){
		int newNumber = notificationNumberMap.containsKey(id) ? notificationNumberMap.get(id) + 1: 1;
		notificationNumberMap.put(id, newNumber);
		return newNumber;
	}
	
	public static void resetNotificationNumber(String id){
		notificationNumberMap.remove(id);
	}
}
