package com.rn300.pleaseapp.activities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.rn300.pleaseapp.ApiService;
import com.rn300.pleaseapp.R;
import com.rn300.pleaseapp.ServerMessagingService;
import com.rn300.pleaseapp.lists.ListItemInterface;
import com.rn300.pleaseapp.lists.contacts.ContactItem;
import com.rn300.pleaseapp.lists.contacts.ContactListAdapter;
import com.rn300.pleaseapp.lists.contacts.ContactSection;
import com.rn300.pleaseapp.lists.contacts.InviteItem;

@SuppressLint("InlinedApi")
public class ContactsActivity extends ListActivity{
	private String TAG = "ContactsActivity";
	public TextView outputText;
	
	private ArrayList<ListItemInterface> mContacts;  
	private ContactListAdapter mAdapter;
	
	private static final int RECENT_CONTACTS_LIMIT = 5; 
	
	private String COUNTRY_CODE = "";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_contacts);
		
		TextView contactsTitle = (TextView)findViewById(R.id.contacts_title);
		final Typeface font = Typeface.createFromAsset(this.getAssets(),"fonts/HelveticaNeue.ttf");
		contactsTitle.setTypeface(font);
		new matchContactsWithUsers().execute();
		mContacts = new ArrayList<ListItemInterface>(); 
		
		mAdapter = new ContactListAdapter(this, mContacts);
		setListAdapter(mAdapter);
	}

	public ArrayList<String> getAllValidNumbers() {
		ArrayList<String> numbers = new ArrayList<String>();
		// Query and loop for every phone number on the phone
		Cursor phoneCursor = getContentResolver().query(
				ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
				new String[] { ContactsContract.CommonDataKinds.Phone.NUMBER }, 
				null,
				null, 
				null);
		String ownNumber = ApiService.getOwnNumber(this);
		if(phoneCursor.moveToFirst()){	 
    		do{
    			String number = formatNumber(phoneCursor.getString(0));
    			if(!numbers.contains(number) && number.startsWith("+") && !number.equals(ownNumber)){
    				numbers.add(number);
    			}
    		} while (phoneCursor.moveToNext());
    		
		}
		phoneCursor.close();
		return numbers;
	}

    private class SetPhoto extends AsyncTask<String,Integer,Bitmap>{
    	private final ContactItem contact;
    	
    	public SetPhoto(ContactItem c){
    		contact = c;
    	}
    	
		@Override
		protected Bitmap doInBackground(String... params) {
			String number = params[0];
			int id = getContactIdFromNumber(number);
			return getPhoto(id);
		}
		
		@Override
		protected void onPostExecute(Bitmap result) {
			if(mContacts.contains(contact) && result != null){
				((ContactItem)mContacts.get(mContacts.indexOf(contact))).setBitmap(result);
				mAdapter.notifyDataSetChanged();
			}
		}
    	
    }
    
    private class matchContactsWithUsers extends AsyncTask<Void,Integer,ArrayList<JSONObject>>{
    	private ProgressDialog dialog;
    	@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialog = new ProgressDialog(ContactsActivity.this);
			dialog.setMessage("Fetching contacts...");
			dialog.setIndeterminate(true);
			dialog.setCancelable(true);
			dialog.setOnCancelListener(new OnCancelListener(){
				@Override
				public void onCancel(DialogInterface dialog) {
					finish();
				}
			});
			dialog.show();
		}
    	
		@Override
		protected ArrayList<JSONObject> doInBackground(Void... params) {
			String validNumbers = (new JSONArray(getAllValidNumbers())).toString();
		
			HttpClient httpClient = new DefaultHttpClient();  
			HttpPost httpPost = new HttpPost(ServerMessagingService.SERVER+"/user/byNumber/");
		
			try
		    {
				httpPost.setEntity(new StringEntity("{\"numbers\":"+validNumbers+"}"));
				httpPost.setHeader("Content-type", "application/json");
				
				HttpResponse response = httpClient.execute(httpPost);
		    	StatusLine statusLine = response.getStatusLine();
		    	if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
		    		Log.d(TAG,"Retrieveing contacts from server");
		    		HttpEntity entity = response.getEntity();
		    	    ByteArrayOutputStream out = new ByteArrayOutputStream();
		    	    entity.writeTo(out);
		    	    out.close();
		    	    String responseStr = out.toString();
		    	    JSONArray contacts = new JSONArray(responseStr);
		    	    int len = contacts.length();
		    	    ArrayList<JSONObject> contactObjects = new ArrayList();
		    	    for(int i = 0; i < len; i++) contactObjects.add(contacts.getJSONObject(i));
		    	    Collections.sort(contactObjects, new Comparator<JSONObject>(){

						@Override
						public int compare(JSONObject lhs, JSONObject rhs) {
							try {
								
								return lhs.getString(ContactItem.NAME_PROP).compareToIgnoreCase(rhs.getString(ContactItem.NAME_PROP));
							} catch (JSONException e) {
							}
							return 0;
						}
		    	    	
		    	    });
		    	    
		    	    return contactObjects;
		    	} else {
		    		 Log.d(TAG,"failed to sign up to server");
		    	}
		    }
		    catch(IOException e){
		    	Log.d(TAG,"exception: "+e.getMessage());
		    } catch (JSONException e) {
				e.printStackTrace();
			}
			return new ArrayList<JSONObject>();
		}
    	
		@Override
		protected void onPostExecute(ArrayList<JSONObject> result){
			mAdapter.setNotifyOnChange(false);
			if(result.size() > 0){
				mAdapter.add(new ContactSection(getBaseContext(),"Using the app"));
			}
			for(JSONObject object : result){
				if(object.has(ContactItem.NAME_PROP) && object.has(ContactItem.NUMBER_PROP)){
					ContactItem contact = new ContactItem(getBaseContext(),object);
					mAdapter.add(contact);
				}
			}
			
			
			mAdapter.add(new ContactSection(getBaseContext(),"Invite someone"));
			fetchRecentContacts();
			mAdapter.add(new InviteItem(getBaseContext()));
			mAdapter.notifyDataSetChanged();
			dialog.dismiss();
		}
    }
    
    public void fetchRecentContacts(){
    	String[] strFields = {
    	        android.provider.CallLog.Calls.NUMBER, 
    	        android.provider.CallLog.Calls.CACHED_NAME,
    	        };
    	Cursor c = getContentResolver().query(
                android.provider.CallLog.Calls.CONTENT_URI,
                strFields, 
                null, null,
                android.provider.CallLog.Calls.DATE + " DESC");
    	
    	String ownNumber = ApiService.getOwnNumber(this);
    	int count = 0;
    	if(c.moveToFirst()){	 
    		do{
    			String number = c.getString(0);
    			String name = c.getString(1);
    			
    			if(name != null && !name.equals("") && !number.equals(ownNumber)){
    				
    				ContactItem contact = new ContactItem(this,"{\"name\":\""+name+"\",\"number\":\""+number+"\"}");	
    				if(!mContacts.contains(contact)){
    					mContacts.add(contact);
    					new SetPhoto(contact).execute(number);
    					count++;
    				}
    			}
    		} while (c.moveToNext() && count < RECENT_CONTACTS_LIMIT);
    		 
    	}
    	c.close();
    }

    private String formatNumber(String number){	
    	if(number.startsWith("00")){
    		number = number.substring(2);
    		number = "+" + number.replaceAll( "[^\\d]", "" );
    	}else if(number.startsWith("+")){
    		number = "+" + number.replaceAll( "[^\\d]", "" );
    	}else{
    		if(COUNTRY_CODE.equals("")){
	    		TelephonyManager tMgr =(TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
	    		String isoCode = tMgr.getSimCountryIso();
	    		getCountryDetails(isoCode);
    		}
    		
    		if(number.startsWith("0")){
    			number = number.substring(1);
    		}
    		
    		number = "+" + COUNTRY_CODE + number.replaceAll( "[^\\d]", "" );
    	}
    	
    	return number;
    }
    
	private void getCountryDetails (String isoCode){
		XmlResourceParser xrp = this.getResources().getXml(R.xml.countries);
		
		try {
			xrp.next();
			int eventType = xrp.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
			    if (eventType == XmlPullParser.START_TAG
			            && xrp.getName().equalsIgnoreCase("country")) {
			    	if(xrp.getAttributeValue(null,"code").equals(isoCode)){
			    		COUNTRY_CODE = xrp.getAttributeValue(null,"phoneCode");
			    		break;
			    	}
			    }
			    eventType = xrp.next();
			}
			
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    
    private int getContactIdFromNumber(String contactNumber){
        contactNumber = Uri.encode(contactNumber);
        int phoneContactID = new Random().nextInt();
        Cursor contactLookupCursor = getContentResolver().query(Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,contactNumber),new String[] {PhoneLookup.DISPLAY_NAME, PhoneLookup._ID}, null, null, null);
            while(contactLookupCursor.moveToNext()){
                phoneContactID = contactLookupCursor.getInt(contactLookupCursor.getColumnIndexOrThrow(PhoneLookup._ID));
               }
            contactLookupCursor.close();

        return phoneContactID;
    }
    
	public Bitmap getPhoto(int contactId) {
	     Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
	     Uri photoUri = Uri.withAppendedPath(contactUri, Contacts.Photo.CONTENT_DIRECTORY);
	     Cursor cursor = getContentResolver().query(photoUri,
	          new String[] {Contacts.Photo.PHOTO}, null, null, null);
	     if (cursor == null) {
	         return null;
	     }
	     try {
	         if (cursor.moveToFirst()) {
	             byte[] data = cursor.getBlob(0);
	             if (data != null) {
	                 return BitmapFactory.decodeStream(new ByteArrayInputStream(data));
	             }
	         }
	     } finally {
	         cursor.close();
	     }
	     return null;
	 } 

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id){
		ListItemInterface item  = mContacts.get(position);
		if(item instanceof ContactItem || item instanceof InviteItem){
			// ListItem contact = (ContactItem) item;
			// contact has the app, take the user to the set chore page
			if(item instanceof ContactItem && ((ContactItem)item).getChildId() != ""){
				ContactItem contact = (ContactItem) item;
				Log.d(TAG,"User has the app");
				Intent it = new Intent(this,TabsActivity.class);
				it.setAction(TabsActivity.ACTION_START_CHILDCHORES);
				it.putExtra(ApiService.CHILD_STRING, contact.toString());
				startActivity(it);
				finish();
			}
			// Contact doesn't have the app, invite him by SMS!
			else if(item instanceof ContactItem) {
				ContactItem contact = (ContactItem) item;
				Log.d(TAG,"User doesn't have the app");
				Uri uri = Uri.parse("smsto:"+contact.getContactNumber()); 
		        Intent it = new Intent(Intent.ACTION_SENDTO, uri); 
		        it.putExtra("sms_body", getResources().getString(R.string.invitation_sms)); 
		        startActivity(it);
			} 
			// Press the invite button
			else if(item instanceof InviteItem){
				Uri uri = Uri.parse("smsto:"); 
		        Intent it = new Intent(Intent.ACTION_SENDTO, uri); 
		        it.putExtra("sms_body", getResources().getString(R.string.invitation_sms)); 
		        startActivity(it);
			}
		}
	}
}
