package com.rn300.pleaseapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmSetter extends BroadcastReceiver {
		 
	@Override
	public void onReceive(Context context, Intent intent) {
	    Intent service = new Intent(context, ReminderService.class);
	    service.setAction(ReminderService.BOOT);
	    context.startService(service);
	}
	 
}

