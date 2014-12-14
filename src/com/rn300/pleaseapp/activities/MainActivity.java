package com.rn300.pleaseapp.activities;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;

import com.rn300.pleaseapp.ApiService;
import com.rn300.pleaseapp.R;

 
public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";
	
	private static int SPLASH_TIME_OUT = 3000;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.splash_screen);
        
        String userId = getUserId();
        String userName = getUserName();
        
        if(!userId.equals("") && !userName.equals("")){
        	final long start = System.currentTimeMillis();
              
            Intent intent = new Intent(this, ApiService.class);
          	intent.setAction(ApiService.REGISTER_GCM);
          	intent.putExtra(ApiService.PARAM_RESULT_RECEIVER, new ResultReceiver(new Handler()){
          		 @Override
          		 protected void onReceiveResult(int resultCode, Bundle resultData) {
          		        if(resultCode == ApiService.RESULT_STATUS_OK){
          		        	long end = System.currentTimeMillis();
          		        	long timeout = SPLASH_TIME_OUT - end + start;
          		        	
          		        	new Handler().postDelayed(new Runnable() {
          		               @Override
          		               public void run() {
          		                   	startTabsActivity(); 	
          		               }
          		           }, timeout > 0 ? timeout:0); 
          		        }else{
          		        	Log.wtf(TAG, "Shit shit shit - GCM registration failed what do I do now?!");
          		        	long end = System.currentTimeMillis();
          		        	long timeout = SPLASH_TIME_OUT - end + start;
          		        	
          		        	new Handler().postDelayed(new Runnable() {
          		               @Override
          		               public void run() {
          		                   	startTabsActivity(); 	
          		               }
          		           }, timeout > 0 ? timeout:0); 
          		        }
          		 }
          	});
          	this.startService(intent);
        }else if(userId.equals("")){
        	new Handler().postDelayed(new Runnable() {
	               @Override
	               public void run() {
	            	   startUserSignup(); 	
	               }
	           }, SPLASH_TIME_OUT); 
        	
        }else if(!userId.equals("") && userName.equals("")){
        	new Handler().postDelayed(new Runnable() {
	               @Override
	               public void run() {
	            	   startUserProfileSetup(); 	
	               }
	           }, SPLASH_TIME_OUT); 
        }
    }

	protected void onRestart(){
		super.onRestart();
		String userId = getUserId();
		String userName = getUserName();
	    if(userId == ""){
	    	startUserSignup();
	    }else{
	    	if(userName.equals("")){
	    		startUserProfileSetup();
	    	}else{
	    		startTabsActivity();
	    	}
	    }
	}
	
	@Override 
	protected void onResume(){
		super.onResume();
		/*String userId = getUserId();
		String userName = getUserName();
	    if(userId == ""){
	    	startUserSignup();
	    }else{
	    	if(userName.equals("")){
	    		startUserProfileSetup();
	    	}else{
	    		startTabsActivity();
	    	}
	    }*/
	}
	
	private void startTabsActivity(){
		Intent intent = new Intent(this,TabsActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		finish();
	}
	
	private void startUserSignup(){
		Intent intent = new Intent(this,VerifyNumberActivity.class);
		startActivity(intent);
		finish();
	}
	
	private void startUserProfileSetup(){
		Intent intent = new Intent(this,UserProfileActivity.class);
		startActivity(intent);
		finish();
	}
	
	private String getUserId(){
		return ApiService.getOwnId(this);
	}
	
	private String getUserName(){
		return ApiService.getOwnName(this);
	}
	
}