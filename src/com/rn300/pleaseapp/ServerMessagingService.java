package com.rn300.pleaseapp;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.rn300.pleaseapp.activities.TabsActivity;
import com.rn300.pleaseapp.lists.children.ChildItem;
import com.rn300.pleaseapp.lists.chores.items.choreitem.ChoreItem;

public class ServerMessagingService extends IntentService {
	private static final String TAG = "ServerMessagingService";
	private static final String SERVICE_NAME = "ServerMessagingService";
	public static final String EXTRA_NOTIFICATION_ID = "notificationIdExtra";
	
	public static final int NEWCHORE_NOTIFICATION = 1;
	public static final int CHORECOMPLETE_NOTIFICATION = 2;
	public static final int CHOREAPPROVED_NOTIFICATION = 3;
	
	NotificationCompat.Builder builder;
	
	public static final String SERVER = "http://www.karmapleaseapp.com:8000"; // used to be "http://protected-eyrie-2343.herokuapp.com";
	public static final String SERVER_USER = "000000000000000000000000";
	
	public static final String UPLOAD_CHORE = "com.example.pleaseapp.UPLOAD_CHORE";
	public static final String RECEIVED_CHORE = "com.example.pleaseapp.RECEIVED_CHORE";
	public static final String PROCESSED_CHORE = "com.example.pleaseapp.PROCESSED_CHORE";
	public static final String DELIVERED_CHORE = "com.example.pleaseapp.DELIVERED_CHORE";
	public static final String UPDATE_CHILD = "com.example.pleaseapp.UPDATE_CHILD";
	
	public ServerMessagingService() {
		super(SERVICE_NAME);
		super.onCreate();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String action = intent.getAction();
		
		if(action.equals(UPLOAD_CHORE)){
			try {
				uploadChore(intent);
			} catch (JSONException e) {
				Log.e(TAG, "Failed in putting chore!");
				e.printStackTrace();
			}
		} else if(action.equals(RECEIVED_CHORE)) {
			String chore = "";
			String choreId= intent.getStringExtra("choreId");	
			chore = fetchChore(this,choreId);
			
		     if(chore != null){
	        	 try {
	 				JSONObject choreObject = new JSONObject(chore);
	 				routeChore(choreObject);
	 			} catch (JSONException e) {
	 				e.printStackTrace();
	 			}
             }else{
            	 // Remove the chore 
            	 try {
					JSONObject choreObject = ApiService.findChoreById(this, choreId);
					if(choreObject!=null){
						choreObject.put(ChoreItem.SHOW, false);
						routeChore(choreObject);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
            	 
             }
            
		} else if(action.equals(DELIVERED_CHORE)){
			String choreId = intent.getStringExtra("choreId");	
			try {
				JSONObject chore = ApiService.findChoreById(this, choreId);
				chore.put(ChoreItem.DELIVERY, ChoreItem.CHORE_DELIVERY_RECEIVED);
				if(!chore.has("show") || chore.getBoolean("show")){
			    	ApiService.putChore(this,chore,false);
			    	// Inform app of successful sync with server
					Intent broadcast = new Intent();
					broadcast.setAction(ApiService.CHORE_CHANGED);
					broadcast.putExtra(ApiService.CHORE_STRING, chore.toString());
					sendBroadcast(broadcast);
					// Release the wake lock provided by the WakefulBroadcastReceiver.
			        GcmBroadcastReceiver.completeWakefulIntent(intent);
			    }
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} else if(action.equals(UPDATE_CHILD)){
			String childId = intent.getStringExtra("childId");
			fetchChild(childId);
		}
	} 
	
	void routeChore(final JSONObject chore) throws JSONException{
		final String ownId = ApiService.getOwnId(this);
		
		final String userId = ownId.equals(chore.getString(ChoreItem.DESTINATION)) ? chore.getString(ChoreItem.ORIGIN):chore.getString(ChoreItem.DESTINATION);
		if(!isChildExists(userId)){
			fetchChild(userId);
		}
	
		if(chore.getInt(ChoreItem.STATUS) == ChoreItem.CHORE_STATUS_APPROVED && ownId.equals(chore.getString(ChoreItem.DESTINATION)) && !chore.has(ChoreItem.KARMA_OBTAINED)){
			chore.put(ChoreItem.KARMA_OBTAINED, true);
			ApiService.putChore(this, chore, false);
			int karma = chore.getInt(ChoreItem.KARMA);
			ApiService.addKarma(this, karma);
			sendBroadcast(new Intent(ApiService.REFRESH_KARMA));
		}
		
		if(!ApiService.isChildBlocked(this, userId)){
			Intent intent = new Intent();
			intent.setAction(PROCESSED_CHORE);
			intent.putExtra(ApiService.CHORE_STRING, chore.toString());
			sendOrderedBroadcast(intent, null,
				new BroadcastReceiver(){
					@Override
					public void onReceive(Context context, Intent intent) {
						if(getResultCode() != Activity.RESULT_CANCELED){
							try {
								if(!chore.has(ChoreItem.SHOW) || chore.getBoolean(ChoreItem.SHOW)){
									// user is set a new chore 
									if(ownId.equals(chore.getString(ChoreItem.DESTINATION)) && chore.getInt(ChoreItem.STATUS) == ChoreItem.CHORE_STATUS_PENDING
											&& GlobalState.notifyOnNew()){
										try {
											chore.put(ChoreItem.HIGHLIGHT, true);
											ApiService.putChore(getBaseContext(), chore, false);
										} catch (JSONException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
										String title = getResources().getString(R.string.new_chore_notification_title);
										String ticker = chore.getString(ChoreItem.TITLE);
										String text = getResources().getString(R.string.new_chore_notification_text);
										int notificationId = NEWCHORE_NOTIFICATION;
										sendNotification(title,ticker, text, notificationId,R.drawable.ic_notification_new,chore.getString(ChoreItem.ORIGIN));
									}
									// a chore that was set to user is approved as complete
									else if(ownId.equals(chore.getString(ChoreItem.DESTINATION)) && chore.getInt(ChoreItem.STATUS) == ChoreItem.CHORE_STATUS_APPROVED
											&& GlobalState.notifyOnApproved()){
										String title = String.format(getResources().getString(R.string.chore_approved_notification_title),
												ApiService.getChildProp(getBaseContext(), chore.getString(ChoreItem.ORIGIN), ChildItem.NAME));
										String ticker = chore.getString(ChoreItem.TITLE);
										String text = getResources().getString(R.string.chore_approved_notification_text);
										int notificationId = CHOREAPPROVED_NOTIFICATION;
										sendNotification(title,ticker, text, notificationId,R.drawable.ic_notification_completed,chore.getString(ChoreItem.ORIGIN));
									}
									// a chore is completed
									else if(ownId.equals(chore.getString(ChoreItem.ORIGIN)) && chore.getInt(ChoreItem.STATUS) == ChoreItem.CHORE_STATUS_COMPLETE
											&& GlobalState.notifyOnComplete()){
										JSONObject child = ApiService.findChildById(getBaseContext(), chore.getString(ChoreItem.DESTINATION));
										child.put(ChildItem.HIGHLIGHT, true);
										ApiService.updateChildren(getBaseContext(), child);
										String title = String.format(getResources().getString(R.string.chore_completed_notification_title),
												ApiService.getChildProp(getBaseContext(), chore.getString(ChoreItem.DESTINATION), ChildItem.NAME));
										String ticker = chore.getString(ChoreItem.TITLE);
										String text = getResources().getString(R.string.chore_completed_notification_text);
										int notificationId = CHORECOMPLETE_NOTIFICATION;
										updateChildTicker(chore.getString(ChoreItem.DESTINATION));
										sendNotification(title,ticker, text, notificationId,R.drawable.ic_notification_completed,chore.getString(ChoreItem.DESTINATION));
									}
										
								}else{
									ChoreItem choreItem = new ChoreItem(ServerMessagingService.this,chore);
									choreItem.remove();
									if(!chore.getString(ChoreItem.DESTINATION).equals(ownId)){
										updateChildTicker(chore.getString(ChoreItem.DESTINATION));
									}
								}
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
						// Release the wake lock provided by the WakefulBroadcastReceiver.
				        GcmBroadcastReceiver.completeWakefulIntent(intent);
					}
					
				}, null, Activity.RESULT_OK, null, null);
		}
	}
	
	private void updateChildTicker(String childId){
		String choreString = ApiService.retrieveChores(this, childId);
		JSONObject child = ApiService.findChildById(this, childId);
		try {
			int set = 0;
			int complete = 0;
			JSONArray chores = new JSONArray(choreString);
			for(int i = 0; i < chores.length(); i++){
				JSONObject chore = chores.getJSONObject(i);
				if(!chore.has(ChoreItem.SHOW) || chore.getBoolean(ChoreItem.SHOW)){
					if(chore.getInt(ChoreItem.STATUS) == ChoreItem.CHORE_STATUS_PENDING) set++;
					else if (chore.getInt(ChoreItem.STATUS) == ChoreItem.CHORE_STATUS_COMPLETE) complete++;
				}
			}
			String ticker = "No Tasks Set";
	    	if(set == 0 && complete == 1) ticker = String.valueOf(complete) + " task to approve";
	    	else if(set == 0 && complete > 1) ticker = String.valueOf(complete) + " tasks to approve";
	    	else if(set == 1 && complete == 0) ticker = String.valueOf(set) + " task set";
	    	else if(set > 1 && complete == 0) ticker = String.valueOf(set) + " tasks set";
	    	else if(set == 1 && complete == 1) ticker = String.valueOf(set) + " task set, and " + String.valueOf(complete) + " task to approve";
	    	else if(set > 1 && complete > 1) ticker = String.valueOf(set) + " tasks set, and " + String.valueOf(complete) + " tasks to approve";
	    	child.put(ChildItem.TICKER, ticker);
	    	ApiService.updateChildren(this, child);
		} catch (JSONException e) {
			e.printStackTrace();
		} 
	}
	
	private boolean isChildExists(String id) throws JSONException{
		JSONArray children = new JSONArray(ApiService.retrieveChildren(this));
		for(int i = 0; i < children.length(); i++){
			if(children.getJSONObject(i).getString(ChildItem.ID).equals(id)){
				return true;
			}
		}
		return false;
	}
	
	private void fetchChild(String id){
		try {
			HttpClient httpClient = new DefaultHttpClient();  
			HttpGet httpGet = new HttpGet(SERVER+"/children/"+id);
			
			HttpResponse response = httpClient.execute(httpGet);
		    StatusLine statusLine = response.getStatusLine();
		    if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
		    	HttpEntity entity = response.getEntity();
	    	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    	    entity.writeTo(out);
	    	    out.close();
	    	    String responseStr = out.toString();
	    	    JSONObject child = new JSONObject(responseStr);
	    	    ApiService.updateChildren(this, child);
	    		Intent broadcast = new Intent();
	    		broadcast.setAction(ApiService.CHILD_CHANGED);
	    		broadcast.putExtra(ApiService.CHILD_STRING, child.toString());
	    		sendBroadcast(broadcast);
		    } else {
		    	Log.e(TAG, "Error response from server: code " + statusLine.getStatusCode());
		    }
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}	
		
		stopSelf();  	
	}
	
 	private void uploadChore (Intent intent) throws JSONException{
		JSONObject co = new JSONObject();
		String id = ApiService.getOwnId(this);
		try {
			co = new JSONObject(intent.getStringExtra(ApiService.CHORE_STRING));
			if(!co.getString(ChoreItem.ORIGIN).equals(SERVER_USER)){
				Log.d(TAG,"uploading chore id: "+co.getString(ChoreItem.ID));
				final HttpClient httpClient = new DefaultHttpClient();  
				final HttpPut httpPut = new HttpPut(SERVER+"/chore/"+id);
				
				
				JSONObject uploadReady = co;
				if(uploadReady.has(ChoreItem.REMINDER)){
					uploadReady.remove(ChoreItem.REMINDER);
				}
				
				StringEntity test = new StringEntity(uploadReady.toString(),HTTP.UTF_8);
				test.setContentEncoding(HTTP.UTF_8);
				
				httpPut.setEntity(test);
				httpPut.addHeader("Content-Type", "application/json;charset=UTF-8");
				httpPut.addHeader("Accept", "application/json;charset=UTF-8");
				HttpResponse response = httpClient.execute(httpPut);
			    StatusLine statusLine = response.getStatusLine();
			    if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
	                co.put(ChoreItem.DELIVERY, ChoreItem.CHORE_DELIVERY_SYNCED);
				} else {
			 		co.put(ChoreItem.DELIVERY, ChoreItem.CHORE_DELIVERY_FAILED);
		    	 }
			}else{
				autoReply(co);
			}
		} catch (JSONException e) {
			co.put(ChoreItem.DELIVERY, ChoreItem.CHORE_DELIVERY_FAILED);
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			co.put(ChoreItem.DELIVERY, ChoreItem.CHORE_DELIVERY_FAILED);
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			co.put(ChoreItem.DELIVERY, ChoreItem.CHORE_DELIVERY_FAILED);
			e.printStackTrace();
		} catch (IOException e) {
			co.put(ChoreItem.DELIVERY, ChoreItem.CHORE_DELIVERY_FAILED);
			e.printStackTrace();
		}
		
	    if(!co.has("show") || co.getBoolean("show") == true){
	    	ApiService.putChore(this,co,false);
	    	// Inform app of successful sync with server
			Intent broadcast = new Intent();
			broadcast.setAction(ApiService.CHORE_CHANGED);
			broadcast.putExtra(ApiService.CHORE_STRING, co.toString());
			sendBroadcast(broadcast);
	    }
		
		stopSelf();  	
	}
	
 	private void autoReply(JSONObject chore) throws JSONException{
 		if(chore.getInt(ChoreItem.STATUS) == ChoreItem.CHORE_STATUS_COMPLETE){
	 		chore.put(ChoreItem.STATUS,ChoreItem.CHORE_STATUS_APPROVED);
	 		chore.put(ChoreItem.DELIVERY, ChoreItem.CHORE_DELIVERY_RECEIVED);
	 		ApiService.putChore(this, chore,false);  
	 		routeChore(chore);
 		}else if(chore.getInt(ChoreItem.STATUS)== ChoreItem.CHORE_STATUS_PENDING){
 			chore.put(ChoreItem.DELIVERY, ChoreItem.CHORE_DELIVERY_RECEIVED);
 			ApiService.putChore(this, chore,false);  
	 		routeChore(chore);
 		}
 	}
 	
	public static String fetchChore(Context context,String choreId){
		StringBuilder builder = new StringBuilder();
		String ownId = ApiService.getOwnId(context);
		HttpClient httpClient = new DefaultHttpClient();  
		HttpGet httpGet = new HttpGet(SERVER+"/chore/"+ownId+"/"+choreId);
			   
	    try{
	    	 HttpResponse response = httpClient.execute(httpGet);
	    	 StatusLine statusLine = response.getStatusLine();
	    	 if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
	    		 HttpEntity entity = response.getEntity();
                 InputStream content = entity.getContent();
                 BufferedReader reader = new BufferedReader(
                                 new InputStreamReader(content));
                 String line;
                 while ((line = reader.readLine()) != null) {
                         builder.append(line);
                 }
                 String chore = builder.toString();
                 JSONObject choreObject = new JSONObject(chore);
                 ApiService.putChore(context, choreObject,false);  
                 return choreObject.toString();
	    	 } else {
	    		 Log.d(TAG,"failed to fetch chore");
	    		 return null;
	    	 }
	  
	    }
	    catch(IOException e){
	    	Log.d(TAG,"exception: "+e.getMessage());
	    	return null;
	    } catch (JSONException e) {
	    	Log.d(TAG,"exception: "+e.getMessage());
	    	return null;
		}
    }
	
    // Put the GCM message into a notification and post it.
    @SuppressLint("InlinedApi")
	private void sendNotification(String head,String tickerText, String msg, int notificationId,int smallIconResid, String childId) {
    	try {
    		String noteId = String.valueOf(notificationId)+String.valueOf(childId.hashCode()).substring(3);
	    	int notificationNumber = GlobalState.updateNotificationNumber(noteId);
    			
	    	NotificationManager mNotificationManager = (NotificationManager)
	                getSystemService(Context.NOTIFICATION_SERVICE);
	    	
	    	Intent i = new Intent(this, TabsActivity.class);
	    	
	    	if(notificationId == NEWCHORE_NOTIFICATION || notificationId == CHOREAPPROVED_NOTIFICATION){
	    		i.setAction(TabsActivity.ACTION_START_TODO);
	    	}else if(notificationId == CHORECOMPLETE_NOTIFICATION ){
	    		i.setAction(TabsActivity.ACTION_START_CHILDCHORES);
	    		String childString = ApiService.findChildById(this, childId).toString();
	    		i.putExtra(ApiService.CHILD_STRING, childString);
	    	}
	    	
	    	i.putExtra(EXTRA_NOTIFICATION_ID, Integer.valueOf(noteId));
	    	
	        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,i, 0);
	        
	        NotificationCompat.Builder mBuilder;
		
			mBuilder = new NotificationCompat.Builder(this)
		      .setSmallIcon(smallIconResid)
		      .setNumber(notificationNumber)
		      .setContentTitle(head)
		      .setStyle(new NotificationCompat.BigTextStyle()
		      .bigText(msg))
		      .setContentText(msg)
		      .setTicker(tickerText)
		      .setAutoCancel(true)
		       .setSound(Uri.parse(GlobalState.getRingtone()));
			
			if(Build.VERSION.SDK_INT >= 11){
				Resources r = getResources();
				Bitmap largePic = ChildItem.getProfilePic(this, childId);
				largePic = Bitmap.createScaledBitmap(largePic, r.getDimensionPixelSize(android.R.dimen.notification_large_icon_width) , 
	    			r.getDimensionPixelSize(android.R.dimen.notification_large_icon_height) , false);
				mBuilder.setLargeIcon(largePic);
			}
			
			if(GlobalState.vibrateOnTask()) mBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
		        
		    mBuilder.setContentIntent(contentIntent);
		    
		    mNotificationManager.notify(Integer.parseInt(noteId), mBuilder.build());
		} catch (JSONException e) {
			e.printStackTrace();
		}
        
       
    }

}
