package com.rn300.pleaseapp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.rn300.pleaseapp.activities.MainActivity;
import com.rn300.pleaseapp.lists.children.ChildItem;
import com.rn300.pleaseapp.lists.chores.items.choreitem.ChoreItem;

public class ApiService extends IntentService {
	private static final String TAG = "ApiService";
	private static final String SERVICE_NAME = "ApiService";
    public static final String PARAM_RESULT_RECEIVER = "ResultReceiver";
    
    public static final int RESULT_STATUS_OK = 200;
    public static final int RESULT_STATUS_ERROR = 500;
    
    public static final String USER_DETAILS_STRING = "detailsString";
    public static final String CHORE_STRING = "choreString";
    public static final String CHILD_STRING = "childString";
    public static final String USER_NUMBER = "userNumber";
    
    public static final String UPDATE_USER = "com.example.pleaseapp.UPDATE_USER";
    public static final String GET_WELCOME = "com.example.pleaseapp.GET_WELCOME";
    public static final String REGISTER_USER = "com.example.pleaseapp.REGISTER_USER";
    public static final String REGISTER_GCM = "com.example.pleaseapp.REGISTER_GCM";
    public static final String CHORE_CHANGED = "com.example.pleaseapp.CHORE_CHANGED";
    public static final String CHORE_REMOVED = "com.example.pleaseapp.CHORE_REMOVED";
    public static final String CHILD_CHANGED = "com.example.pleaseapp.CHILD_CHANGED";
    public static final String REFRESH_KARMA = "com.example.pleaseapp.REFRESH_KARMA";
    
    public static final String PROPERTY_REG_ID = "registration_id";
	private static final String PROPERTY_APP_VERSION = "appVersion";
		
	private static final String SENDER_ID = "43214442685"; 
	GoogleCloudMessaging gcm;
	AtomicInteger msgId = new AtomicInteger();
	SharedPreferences prefs;
	String regid;
      
    public static final String PREFS_NAME = "userPrefsFile";
    static SharedPreferences mDataSrc;
    SharedPreferences.Editor mDataEditor;
    
    public static final String USER_KARMA = "karma";
    public static final String USER_KARMA_TOTAL = "to";
    public static final String USER_KARMA_PLEDGED = "pl";
    
	public ApiService() {
		super(SERVICE_NAME);
		super.onCreate();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String action = intent.getAction();
		
		mDataSrc = this.getSharedPreferences(PREFS_NAME, 0);
		mDataEditor = mDataSrc.edit();
	
		if(action.equals(REGISTER_USER)){
			registerUser(intent);
		}else if(action.equals(REGISTER_GCM)){
			registerGcm(intent);
		}else if (action.equals(UPDATE_USER)){
			updateUser(intent);
		}else if (action.equals(GET_WELCOME)){
			getWelcomeChore(intent);
		}
		
	}
	
	// ---------------------- CHORES -------------------------- //
		
	public static JSONObject findChoreById(Context context, String choreId) throws JSONException{
		String childrenString = retrieveChildren(context);
		JSONArray children = new JSONArray(childrenString);
		for(int i = 0; i < children.length(); i++){
			String childChoresString = retrieveChores(context,children.getJSONObject(i).getString(ChildItem.ID));
			JSONArray childChores = new JSONArray(childChoresString);
			for(int j=0; j < childChores.length(); j++){
				if(childChores.getJSONObject(j).getString(ChoreItem.ID).equals(choreId)){
					return childChores.getJSONObject(j);
				}
			}
		}
		
		String ownChoresString = retrieveChores(context,getOwnId(context));
		JSONArray ownChores = new JSONArray(ownChoresString);
		for(int j=0; j < ownChores.length(); j++){
			if(ownChores.getJSONObject(j).getString(ChoreItem.ID).equals(choreId)){
				return ownChores.getJSONObject(j);
			}
		}
		
		return null;
	}
	
	public static String retrieveChores(Context context,String userId){
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
			
		return prefs.getString("chores_"+userId, "[]");
	}
			
	public static void putChore(Context context, JSONObject chore, boolean upload) throws JSONException{
		String destination = chore.getString(ChoreItem.DESTINATION);
		JSONArray chores = new JSONArray(retrieveChores(context,destination));
		// chore has an id so it must be an existing chore.
		if(chore.has(ChoreItem.ID)){
			String id = chore.getString(ChoreItem.ID);
			boolean updated = false;
			for(int i = 0; i < chores.length() && !updated; i++){
				if(chores.getJSONObject(i).getString(ChoreItem.ID).equals(id)){
					chores.put(i, chore);
					updated = true;
				}
			}			
			if(!updated){
				chores.put(chore);
			}
		}
		// chore doesn't have an id so must be new.
		else{
			// Set an id to the chore
			String choreId = (new ObjectId()).toString();
			chore.put(ChoreItem.ID, choreId);
			// Store the data
			chores.put(chore);
		}
		
		storeChores(context,destination,chores.toString());
		
		if(upload){
			Intent messageIntent = new Intent(context,ServerMessagingService.class);
			messageIntent.setAction(ServerMessagingService.UPLOAD_CHORE);
			messageIntent.putExtra(ApiService.CHORE_STRING, chore.toString());
			context.startService(messageIntent);
		}
	}
				
	public static void storeChores(Context context, String userId, String chores){
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("chores_"+userId, chores);
		editor.commit();
	}
	
	public static void removeChore(Context context, JSONObject chore) throws JSONException{
		String userId = chore.getString("userId");
		final String choreId = chore.getString("_id");
		// Update local record
		JSONArray choreArray = new JSONArray(retrieveChores(context,userId));
		JSONArray newChoreArray = new JSONArray();
		for(int i = 0; i < choreArray.length(); i++){
			if(!choreArray.getJSONObject(i).getString("_id").equals(choreId)){
				newChoreArray.put(choreArray.getJSONObject(i));
			}
		}
		storeChores(context,userId,newChoreArray.toString());
		// Sync with server 
		chore.put("show", false);
		Intent messageIntent = new Intent(context,ServerMessagingService.class);
		messageIntent.setAction(ServerMessagingService.UPLOAD_CHORE);
		messageIntent.putExtra(ApiService.CHORE_STRING, chore.toString());
		context.startService(messageIntent);
	}
	
		
	// --------------------- CHILDREN ------------------------ //
	
	public static String retrieveChildren(Context context){
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
			
		return prefs.getString("children", "[]");
	}

	public static String getChildProp(Context context, String id, String prop) throws JSONException{
		JSONArray children = new JSONArray(retrieveChildren(context));
		for(int i = 0; i < children.length(); i++){
			if(children.getJSONObject(i).getString("_id").equals(id)){
				JSONObject child = children.getJSONObject(i);
				if(child.has(prop)){
					return child.getString(prop);
				}else
					return null;
			}
		}
		
		return null;
	}
	
	public static boolean isChildBlocked(Context context, String id){
		JSONArray children;
		try {
			children = new JSONArray(retrieveChildren(context));
			for(int i = 0; i < children.length(); i++){
				if(children.getJSONObject(i).getString("_id").equals(id)){
					JSONObject child = children.getJSONObject(i);
					if(child.has(ChildItem.BLOCK)){
						return child.getBoolean(ChildItem.BLOCK);
					}else
						return false;
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public static JSONObject findChildById(Context context, String id){
		JSONArray children;
		try {
			children = new JSONArray(retrieveChildren(context));
			for(int i = 0; i < children.length(); i++){
				if(children.getJSONObject(i).getString("_id").equals(id)){
					return children.getJSONObject(i);
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static void updateChildren(Context context, JSONObject child) throws JSONException{
		boolean updated = false;
		
		JSONArray childArray = new JSONArray(retrieveChildren(context));
		for(int i = 0; i < childArray.length() && !updated; i++){
			if(childArray.getJSONObject(i).getString("_id").equals(child.getString("_id"))){
				childArray.put(i, child);
				updated = true;
			}
		}
		if(!updated){
			childArray.put(child);
		}
		storeChildren(context,childArray.toString());	
	}
		
	public static void storeChildren(Context context, String children){
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("children", children);
		editor.commit();
	}
	
		
	// ------------------- USER DETAILS ---------------------- //
	
	private void storeUserDetails(String details){
		mDataEditor.putString("userDetails", details);
		mDataEditor.commit();
	}
	
	private static void storeUserDetails(Context context, String details){
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor =  prefs.edit();
		editor.putString("userDetails", details);
		editor.commit();
	}
	
	private String retrieveUserDetails(){
		return mDataSrc.getString("userDetails", "{}");
	}
	
	private void updateStoredUserDetails(String update){
		try {
			JSONObject updateObject = new JSONObject(update);
			JSONObject storedObject = new JSONObject(retrieveUserDetails());
			Iterator<?> keys = updateObject.keys();
			
			while(keys.hasNext()){
				String key = (String)keys.next();
				storedObject.put(key,updateObject.get(key));
			}
			
			storeUserDetails(storedObject.toString());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void updateUser (Intent intent){
		  	String userId = getOwnId(this);
		  	String update = intent.getStringExtra(USER_DETAILS_STRING);
			final HttpClient httpClient = new DefaultHttpClient();  
			final HttpPut httpPut = new HttpPut(ServerMessagingService.SERVER+"/user/"+userId);
			final ResultReceiver resultReceiver = intent.getParcelableExtra(PARAM_RESULT_RECEIVER);
			
	
			try{
			 	httpPut.setEntity(new StringEntity(update));
				httpPut.addHeader("Content-Type", "application/json");
				httpPut.addHeader("Accept", "application/json");
		    	 HttpResponse response = httpClient.execute(httpPut);
		    	 StatusLine statusLine = response.getStatusLine();
		    	 if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
		    		Log.d(TAG,"updated user on server");
		    		Bundle resultData = new Bundle();
		    		resultReceiver.send(RESULT_STATUS_OK, resultData);
		    		updateStoredUserDetails(update);
		    		stopSelf();
		    	 } else {
		    		 Log.d(TAG,"failed to update user");
		    		 Bundle resultData = new Bundle();
			   		 resultReceiver.send(RESULT_STATUS_ERROR, resultData);
		    		 stopSelf();
		    	 }
		  
		    }catch(IOException e){
		    	Bundle resultData = new Bundle();
	   		 	resultReceiver.send(RESULT_STATUS_ERROR, resultData);
		    	Log.d(TAG,"exception: "+e.getMessage());
		    	stopSelf();
		    }
		   
		};
	
	private static JSONObject retrieveUserDetails(Context context){
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		String userDetails = prefs.getString("userDetails", "{}");
		try {
			JSONObject details = new JSONObject(userDetails);
			return details;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static String getOwnPic(Context context){
		JSONObject details = retrieveUserDetails(context);
		try {
			return details.getString("pic");
		} catch (JSONException e) {

			e.printStackTrace();
		}
		return "";
	}
	
	public static String getOwnId(Context context){		
		JSONObject details = retrieveUserDetails(context);
			
		try {
			return details.getString("id");
		} catch (JSONException e) {
		}
		return "";
	}
	
	public static void removeOwnId(Context context){		
		JSONObject details = retrieveUserDetails(context);
		try {
			details.put("id", "");
			storeUserDetails(context,details.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public static String getOwnName(Context context){		
		JSONObject details = retrieveUserDetails(context);
			
		try {
			return details.getString("name");
		} catch (JSONException e) {

		//	e.printStackTrace();
		}
		return "";
	}
	
	public static String getOwnNumber(Context context){		
		JSONObject details = retrieveUserDetails(context);
			
		try {
			return details.getString("number");
		} catch (JSONException e) {

			e.printStackTrace();
		}
		return "";
	}
	
	public static int getPledgedKarma(Context context){
		JSONObject details = retrieveUserDetails(context);
		try {
			JSONObject karmaObject = details.getJSONObject(USER_KARMA);
			return karmaObject.getInt(USER_KARMA_PLEDGED);
		} catch (JSONException e) {
		}
		return 0;
	}
	
	public static int getTotalKarma(Context context){
		JSONObject details = retrieveUserDetails(context);
		try {
			JSONObject karmaObject = details.getJSONObject(USER_KARMA);
			return karmaObject.getInt(USER_KARMA_TOTAL);
		} catch (JSONException e) {
		}
		return 0;
	}
	
	public static void pledgeKarma(Context context, int amount){
		JSONObject details = retrieveUserDetails(context);
		if(details.has(USER_KARMA)){
			try {
				JSONObject karmaObject = details.getJSONObject(USER_KARMA);
				karmaObject.put(USER_KARMA_PLEDGED, karmaObject.getInt(USER_KARMA_PLEDGED)+amount);
				details.put(USER_KARMA, karmaObject);
				storeUserDetails(context,details.toString());
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void addKarma(Context context, int amount){
		JSONObject details = retrieveUserDetails(context);
		JSONObject karmaObject = null;
		if(details.has(USER_KARMA)){
			try {
				karmaObject = details.getJSONObject(USER_KARMA);
				karmaObject.put(USER_KARMA_TOTAL, karmaObject.getInt(USER_KARMA_TOTAL)+amount);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}else{
			karmaObject = new JSONObject();
			try {
				karmaObject.put(USER_KARMA_TOTAL, amount);
				karmaObject.put(USER_KARMA_PLEDGED, 0);
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		try {
			if(karmaObject != null)
				details.put(USER_KARMA, karmaObject);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		storeUserDetails(context,details.toString());
	}

	 public static void removeChildAndChores(Context context, String id) throws JSONException{
	    	JSONArray children = new JSONArray(ApiService.retrieveChildren(context));
	    	JSONArray newChildren = new JSONArray();
	    	for(int i = 0; i < children.length(); i++){
	    		JSONObject child = children.getJSONObject(i);
	    		if(!child.getString(ChildItem.ID).equals(id)){
	    			newChildren.put(child);
	    		}
	    	}
	    	storeChildren(context, newChildren.toString());
	    	
	    	SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
	    	SharedPreferences.Editor editor = prefs.edit();
	    	
	    	// remove all chores set for that user
	    	editor.remove("chores_"+id);
	    	editor.commit();
	    	
	    	// remove all chores set by me to that user
	    	// TODO for the moment its not very important as they will be inaccessible anyway...
			
	}
	
	private void registerUser(Intent intent){
		final String number = intent.getStringExtra(USER_NUMBER);
		final ResultReceiver resultReceiver = intent.getParcelableExtra(PARAM_RESULT_RECEIVER);
		
		HttpClient httpClient = new DefaultHttpClient();  
		HttpPost httpPost = new HttpPost(ServerMessagingService.SERVER+"/user");
	
		try
	    {
			httpPost.setEntity(new StringEntity("{\"number\":\""+number+"\"}"));
			httpPost.setHeader("Content-type", "application/json");
	    	
			HttpResponse response = httpClient.execute(httpPost);
	    	StatusLine statusLine = response.getStatusLine();
	    	if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
	    		Log.d(TAG,"Signed up to server");
	    		HttpEntity entity = response.getEntity();
	    	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    	    entity.writeTo(out);
	    	    out.close();
	    	    String responseStr = out.toString();
	    	    storeUserDetails(responseStr);
	    	    Bundle resultData = new Bundle();
	    		resultReceiver.send(RESULT_STATUS_OK, resultData);
	    		stopSelf();
	    	} else {
	    		 Log.d(TAG,"failed to sign up to server");
	    		 Bundle resultData = new Bundle();
	    		 resultReceiver.send(RESULT_STATUS_ERROR, resultData);
	    		 stopSelf();
	    	}
	    }
	    catch(IOException e){
	    	// Error
	    	Log.d(TAG,"exception: "+e.getMessage());
	    	Bundle resultData = new Bundle();
   		 	resultReceiver.send(RESULT_STATUS_ERROR, resultData);
   		 	stopSelf();
	    }
	    
	}
	
	private void getWelcomeChore(Intent intent){	
		String userId = getOwnId(this);
		HttpClient httpClient = new DefaultHttpClient();  
		HttpGet httpGet = new HttpGet(ServerMessagingService.SERVER+"/welcome-chore/"+userId	);
		final ResultReceiver resultReceiver = intent.getParcelableExtra(PARAM_RESULT_RECEIVER);
		try
	    {
			httpGet.setHeader("Content-type", "application/json");
	    	
			HttpResponse response = httpClient.execute(httpGet);
	    	StatusLine statusLine = response.getStatusLine();
	    	if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
	    		Log.d(TAG,"Got welcome chore");
	    		HttpEntity entity = response.getEntity();
	    	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    	    entity.writeTo(out);
	    	    out.close();
	    	    String responseStr = out.toString();
	    	    JSONObject resObj = new JSONObject(responseStr);
	    	    String welcomeChoreId = resObj.getString("id");
	    	    Log.d(TAG,"Facking receiving chore");
	    		Intent fakeReceive = new Intent(this,ServerMessagingService.class);
	    		fakeReceive.putExtra("choreId", welcomeChoreId);
	    		fakeReceive.setAction(ServerMessagingService.RECEIVED_CHORE);
	    		this.startService(fakeReceive);
	    		Bundle resultData = new Bundle();
	    		resultReceiver.send(RESULT_STATUS_OK, resultData);
	    	    stopSelf();
	    	} else {
	    		 Log.d(TAG,"failed to recevie welcome chore");
	    		 Bundle resultData = new Bundle();
	    		 resultReceiver.send(RESULT_STATUS_ERROR, resultData);
	    	}
	    }
	    catch(IOException e){
	    	Log.d(TAG,"exception: "+e.getMessage());
	    	Bundle resultData = new Bundle();
   		 	resultReceiver.send(RESULT_STATUS_ERROR, resultData);
	    } catch (JSONException e) {
			Bundle resultData = new Bundle();
   		 	resultReceiver.send(RESULT_STATUS_ERROR, resultData);
			e.printStackTrace();
		}
	    
	}

	/**
	 * 
	 * @param intent
	 */
	private void registerGcm(Intent intent){
		String regid = getRegistrationId(getBaseContext());
		String userId = getOwnId(this);
		ResultReceiver resultReceiver = intent.getParcelableExtra(PARAM_RESULT_RECEIVER);
		String serverRegId = checkRegIdOnServer(userId);
		if(regid.length() == 0 || !serverRegId.equals(regid)){
			Log.d(TAG, "Registering to GCM");
			registerBackground(userId,resultReceiver);
		}else {
			Log.d(TAG, "No need to register");
			Bundle resultData = new Bundle();
			resultReceiver.send(RESULT_STATUS_OK, resultData);
			stopSelf();
		}
	}
	
	private String checkRegIdOnServer(String id){
		HttpClient httpClient = new DefaultHttpClient();  
		HttpGet httpGet = new HttpGet(ServerMessagingService.SERVER+"/gcm/"+id);
	
		try
	    {
			httpGet.setHeader("Content-type", "text/plain");
			HttpResponse response = httpClient.execute(httpGet);
	    	StatusLine statusLine = response.getStatusLine();
	    	if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
	    		Log.d(TAG,"Got regId from server");
	    		HttpEntity entity = response.getEntity();
	    	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    	    entity.writeTo(out);
	    	    out.close();
	    	    String serverRegId = out.toString();
	    	    JSONArray regId = new JSONArray(serverRegId);
	    	    Log.d(TAG,"server returned regId = "+regId.getString(0));
	    	    return regId.getString(0);
	    	} else {
	    		 Log.d(TAG,"failed to recevie welcome chore");
	    	}
	    }
	    catch(IOException e){
	    	// Error
	    	Log.d(TAG,"exception: "+e.getMessage());
	    } catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return "";
	}
	
	 /**
  	* Gets the current registration id for application on GCM service.
 * <p>
 * If result is empty, the registration has failed.
 *
 * @return registration id, or empty string if the registration is not
 *         complete.
 */	
	public static String getRegistrationId(Context context) {
	    final SharedPreferences prefs = getGCMPreferences(context);
	    String registrationId = prefs.getString(PROPERTY_REG_ID, "");
	    if (registrationId.length() == 0) {
	        Log.v(TAG, "Registration not found.");
	        return "";
	    }
	    // check if app was updated; if so, it must clear registration id to
	    // avoid a race condition if GCM sends a message
	    int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
	    int currentVersion = getAppVersion(context);
	    if (registeredVersion != currentVersion) {
	        Log.v(TAG, "App version changed or registration expired.");
	        return "";
	    }
	    return registrationId;
	}
    
    /**
     * @return Application's {@code SharedPreferences}.
     */
    private static SharedPreferences getGCMPreferences(Context context) {
        return context.getSharedPreferences(MainActivity.class.getSimpleName(), 
                Context.MODE_PRIVATE);
    }
    
    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }
        
    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration id, app versionCode, and expiration time in the 
     * application's shared preferences.
     */
    private void registerBackground(final String userId,final ResultReceiver receiver) {
    	final Context context = (Context) this;
        String msg = "";
        try {
            if (gcm == null) {
                gcm = GoogleCloudMessaging.getInstance(context);
            }
            regid = gcm.register(SENDER_ID);
            msg = "Device registered, registration id=" + regid;
            Log.d(TAG, msg);
            // You should send the registration ID to your server over HTTP,
            // so it can use GCM/HTTP or CCS to send messages to your app.

            // Save the regid - no need to register again.
            setRegistrationId(context, regid);
            sendRegId(regid,userId,receiver);
        } catch (IOException ex) {
            msg = "Error :" + ex.getMessage();
            Log.d(TAG,msg);
            Bundle resultData = new Bundle();
   		 	receiver.send(RESULT_STATUS_ERROR, resultData);
            stopSelf();
        }
    }
      
    protected void sendRegId (String regid,String userId,final ResultReceiver receiver){
		final HttpClient httpClient = new DefaultHttpClient();  
		final HttpPost httpPost = new HttpPost(ServerMessagingService.SERVER+"/gcm/"+userId+"/"+regid);
	   
		 try{
    	 HttpResponse response = httpClient.execute(httpPost);
    	 StatusLine statusLine = response.getStatusLine();
	    	 if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
	    		Log.d(TAG,"upadted regid");
	    		Bundle resultData = new Bundle();
	    		receiver.send(RESULT_STATUS_OK, resultData);
	    		stopSelf();
	    	 } else {
	    		 Log.d(TAG,"failed to update regid");
	    		 Bundle resultData = new Bundle();
	    		 receiver.send(RESULT_STATUS_ERROR, resultData);
	    		 stopSelf();
	    	 }
	  
	    }catch(IOException e){
	    	Bundle resultData = new Bundle();
   		 	receiver.send(RESULT_STATUS_ERROR, resultData);
	    	Log.d(TAG,"exception: "+e.getMessage());
	    	stopSelf();
	    }
	   
	};
    
    /**
     * Stores the registration id, app versionCode, and expiration time in the
     * application's {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration id
     */
    private void setRegistrationId(Context context, String regId) {
	        final SharedPreferences prefs = getGCMPreferences(context);
	        int appVersion = getAppVersion(context);
	        Log.v(TAG, "Saving regId on app version " + appVersion);
	        SharedPreferences.Editor editor = prefs.edit();
	        editor.putString(PROPERTY_REG_ID, regId);
	        editor.putInt(PROPERTY_APP_VERSION, appVersion);  
	       editor.commit();
	    }
}
