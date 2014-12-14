package com.rn300.pleaseapp;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;

public class GcmBroadcastReceiver extends WakefulBroadcastReceiver {
	@SuppressWarnings("unused")
	private static final String TAG = "GcmBroadcastReceiver";
	
	private static final int CHORE = 126;
	private static final int DELIVERY = 124;
	private static final int CHILD_PROFILE_UPDATED = 128;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		// Explicitly specify that GcmIntentService will handle the intent.
        ComponentName comp = new ComponentName(context.getPackageName(),
                ServerMessagingService.class.getName());
        
        Bundle extras = intent.getExtras();
    	int type = Integer.parseInt(extras.getString("type"));
    	switch (type){
    	case(CHORE):
    		intent.setAction(ServerMessagingService.RECEIVED_CHORE);
     		// Start the service, keeping the device awake while it is launching.
	        startWakefulService(context, (intent.setComponent(comp)));
	        setResultCode(Activity.RESULT_OK);
    		break;
    	case(DELIVERY):
    		intent.setAction(ServerMessagingService.DELIVERED_CHORE);
 			// Start the service, keeping the device awake while it is launching.
        	startWakefulService(context, (intent.setComponent(comp)));
        	setResultCode(Activity.RESULT_OK);
		
    		break;
    	case(CHILD_PROFILE_UPDATED):
    		intent.setAction(ServerMessagingService.UPDATE_CHILD);
			// Start the service, keeping the device awake while it is launching.
    		startWakefulService(context, (intent.setComponent(comp)));
    		setResultCode(Activity.RESULT_OK);
    		break;
    	default:
    		break;
    	}		
	}
}