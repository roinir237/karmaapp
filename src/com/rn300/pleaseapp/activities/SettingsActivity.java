package com.rn300.pleaseapp.activities;

import java.util.List;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;

import com.rn300.pleaseapp.GlobalState;
import com.rn300.pleaseapp.R;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SettingsActivity extends PreferenceActivity {
	
	@SuppressWarnings("unused")
	private static final String TAG = "SettingsActivity";

	final static String ACTION_APPLICATION = "com.example.prefs.APPLICATION";
	final static String ACTION_PROFILE = "com.example.prefs.PROFILE";
	final static String ACTION_REMINDER = "com.example.prefs.REMINDER";
	
	
	public static class ContextualPreferenceChangeListener implements OnSharedPreferenceChangeListener{
		private final Context mCtx;
		private PreferenceActivity activity = null;
		private PreferenceFragment fragment = null;
		
		public ContextualPreferenceChangeListener(Context ctx,PreferenceFragment f){
			fragment = f;
			activity = null;
			mCtx = ctx;
		}
		
		public ContextualPreferenceChangeListener(Context ctx,PreferenceActivity a){
			activity = a;
			fragment = null;
			mCtx = ctx;
		}
		
		@Override
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {
			updatePreference(key);
		}
		
		@SuppressWarnings("deprecation")
		private void updatePreference(String key){
			Preference pref = null;
			if(activity != null){
				pref = activity.findPreference(key);
			}else if(fragment != null){
				pref = fragment.findPreference(key);
			}
	   	     
	   	 	if(pref != null){
		   	    if (pref instanceof ListPreference) {
		   	        ListPreference listPref = (ListPreference) pref;
		   	        pref.setSummary(listPref.getEntry());
		   	        return;
		   	    }       
		   	     
		   	    if (pref instanceof EditTextPreference){
		   	        EditTextPreference editPref =  (EditTextPreference) pref;
		   	        editPref.setSummary(editPref.getText());
		   	        return;
		   	    }
		   	     
		   	    if (pref instanceof RingtonePreference) {
		   	        Uri ringtoneUri = Uri.parse(GlobalState.getRingtone());
		   	        Ringtone ringtone = RingtoneManager.getRingtone(mCtx, ringtoneUri);
		   	        if (ringtone != null) pref.setSummary(ringtone.getTitle(mCtx));
		   	    }       
		   	 if (pref instanceof CheckBoxPreference) {
		   	        if(pref.getKey().equals("enable_defualt_reminder_time") && fragment != null && fragment instanceof reminderSettings){
		   	        	ListPreference defaultReminderTime = (ListPreference) fragment.getPreferenceScreen().findPreference("reminder_time");
		   	        	defaultReminderTime.setEnabled(((CheckBoxPreference)pref).isChecked());
		   	        }else if(pref.getKey().equals("enable_defualt_reminder_time") && activity != null){
		   	        	ListPreference defaultReminderTime = (ListPreference) activity.getPreferenceScreen().findPreference("reminder_time");
		   	        	defaultReminderTime.setEnabled(((CheckBoxPreference)pref).isChecked());
		   	        }
		   	    }       
	   	 	}
	   	}
	};

	private ContextualPreferenceChangeListener legacyPrefChangeListener;
	
	/** Called when the activity is first created. */
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    //setContentView(R.layout.activity_settings);
	    String action = getIntent().getAction();
	    
	    if (action != null && action.equals(ACTION_APPLICATION)) {
	        addPreferencesFromResource(R.xml.application_settings);
	        ListPreference dateFormatList = (ListPreference) getPreferenceScreen().findPreference("date_format");
		       dateFormatList.setSummary(dateFormatList.getEntry());
	    } else if (action != null && action.equals(ACTION_REMINDER)) {
	    	addPreferencesFromResource(R.xml.reminder_settings);
	    	ListPreference defaultReminderTime = (ListPreference) getPreferenceScreen().findPreference("reminder_time");
	    	CheckBoxPreference enableDefaultReminderTime = (CheckBoxPreference) getPreferenceScreen().findPreference("enable_defualt_reminder_time");
		       
		    defaultReminderTime.setEnabled(enableDefaultReminderTime.isChecked());
	    	defaultReminderTime.setSummary(defaultReminderTime.getEntry());
	    } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
	        // Load the legacy preferences headers
	        addPreferencesFromResource(R.xml.settings_headers_legacy);
	    }
	}
	 
	/**
     * Populate the activity with the top-level headers.
     */
    @SuppressLint("NewApi")
	@Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.settings_headers, target);
    }
	
    @SuppressWarnings("deprecation")
   	@Override
   public void onResume() {
       super.onResume();
       PreferenceScreen screen = getPreferenceScreen();
       if(screen!=null){ // Only pre honeycomb....
    	   legacyPrefChangeListener = new ContextualPreferenceChangeListener(this,this);
	       screen.getSharedPreferences()
	       	.registerOnSharedPreferenceChangeListener(legacyPrefChangeListener);
       }
   }
       
   @SuppressWarnings("deprecation")
   @Override
   public void onPause() {
       super.onPause();
       PreferenceScreen screen = getPreferenceScreen();
       if(screen!=null){	
	       screen.getSharedPreferences()
	       	.unregisterOnSharedPreferenceChangeListener(legacyPrefChangeListener);
       }
   }
   	  
   
    @SuppressLint("NewApi")
	public static class applicationSettings extends PreferenceFragment {
    	private ContextualPreferenceChangeListener prefChangeListener;
    	
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.application_settings);
        }
		
	   	@Override
		   public void onResume() {
		       super.onResume();
		       PreferenceScreen screen = getPreferenceScreen();
		       if(screen!=null){	
		    	   prefChangeListener = new ContextualPreferenceChangeListener(getActivity(),this);
			       screen.getSharedPreferences()
			       	.registerOnSharedPreferenceChangeListener(prefChangeListener);
			       ListPreference dateFormatList = (ListPreference) getPreferenceScreen().findPreference("date_format");
			       dateFormatList.setSummary(dateFormatList.getEntry());
		       }
		   }
		       
		   @Override
		   public void onPause() {
		       super.onPause();
		       PreferenceScreen screen = getPreferenceScreen();
		       if(screen!=null){	
			       screen.getSharedPreferences()
			       	.unregisterOnSharedPreferenceChangeListener(prefChangeListener);
		       }
		   }
    
    }
    
    @SuppressLint("NewApi")
   public static class reminderSettings extends PreferenceFragment {
    	private ContextualPreferenceChangeListener prefChangeListener;
    	
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.reminder_settings);
        }
		
		
	   	@Override
	    public void onResume() {
	       super.onResume();
	       PreferenceScreen screen = getPreferenceScreen();
		       if(screen!=null){	
		    	   prefChangeListener = new ContextualPreferenceChangeListener(getActivity(),this);
			       screen.getSharedPreferences()
			       	.registerOnSharedPreferenceChangeListener(prefChangeListener);
			       
			       CheckBoxPreference enableDefaultReminderTime = (CheckBoxPreference) getPreferenceScreen().findPreference("enable_defualt_reminder_time");
			       ListPreference defaultReminderTime = (ListPreference) getPreferenceScreen().findPreference("reminder_time");
			       
			       defaultReminderTime.setEnabled(enableDefaultReminderTime.isChecked());
			       defaultReminderTime.setSummary(defaultReminderTime.getEntry());
		       }
		   }
		       
		   @Override
		   public void onPause() {
		       super.onPause();
		       PreferenceScreen screen = getPreferenceScreen();
		       if(screen!=null){	
			       screen.getSharedPreferences()
			       	.unregisterOnSharedPreferenceChangeListener(prefChangeListener);
		       }
		   }
    
    }
}
