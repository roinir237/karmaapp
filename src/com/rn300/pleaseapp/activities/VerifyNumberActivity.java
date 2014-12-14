package com.rn300.pleaseapp.activities;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.rn300.pleaseapp.ApiService;
import com.rn300.pleaseapp.R;
import com.rn300.pleaseapp.countrylist.CountryListActivity;

public class VerifyNumberActivity extends Activity {
		@SuppressWarnings("unused")
		private final String TAG = "VerifyNumberActivity"; 
	
		private final int GET_COUNTRY = 1;
		private final int SIGNUP_PROCESS = 2;
		
		private String COUNTRY_NAME = "";
		private String COUNTRY_CODE = "";
		private String PHONE_NUMBER = "";
		private String FORMATED_NUMBER = "";
		
		public static final String EXTRA_NUMBER = "extraNumber";
		
		@Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.activity_verify_number);
	       
	        TelephonyManager tMgr =(TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
	        PHONE_NUMBER = tMgr.getLine1Number();
	        if(PHONE_NUMBER == null) PHONE_NUMBER = "";
	        String isoCode = tMgr.getSimCountryIso();
	        getCountryDetails(isoCode);
	        updateViews();
	        
	        final Intent getCountryCode = new Intent(this,CountryListActivity.class);
	        TextView countrySelector = (TextView) findViewById(R.id.countrySelector);
	        countrySelector.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					startActivityForResult(getCountryCode,GET_COUNTRY);
				}
	        });
	        
	        Button sendSmsBtn = (Button) findViewById(R.id.send_sms_btn);
	        sendSmsBtn.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					String number = ((EditText) findViewById(R.id.phone_number_edit)).getText().toString();
					
					String code = ((EditText) findViewById(R.id.country_code_edit)).getText().toString();
					// remove non digit characters from string
					number = number.replaceAll( "[^\\d]", "" );
					code = code.replaceAll( "[^\\d]", "" );
					
					if(!number.equals("") && !code.equals("")){
						if(number.charAt(0) == '0'){
							FORMATED_NUMBER = "+"+code+number.substring(1);
						}else{
							FORMATED_NUMBER = "+"+code+number;
						}
						
						startSignup();
						//sendSMS(FORMATED_NUMBER,MESSAGE_BODY);
					}else{
						Toast.makeText(getBaseContext(), "Bad number", Toast.LENGTH_SHORT).show();
					}
				}
	        });
	        
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
				    		COUNTRY_NAME = xrp.getAttributeValue(null, "name");
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
		
		private void updateViews(){
			TextView countrySelector = (TextView) findViewById(R.id.countrySelector);
	        COUNTRY_NAME = COUNTRY_NAME ==  "" ? "Choose country":COUNTRY_NAME;
	        countrySelector.setText(COUNTRY_NAME);
	        
	        TextView codeEdit = (TextView) findViewById(R.id.country_code_edit);
	        codeEdit.setText(COUNTRY_CODE);
	        
	        if(PHONE_NUMBER.startsWith("+"+COUNTRY_CODE)){
	        	PHONE_NUMBER = PHONE_NUMBER.substring(COUNTRY_CODE.length()+1);
	        }
	        
	        TextView phoneEdit = (TextView) findViewById(R.id.phone_number_edit);
	        phoneEdit.setText(PHONE_NUMBER);
		}
		
		protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	
			  if (requestCode == GET_COUNTRY) {
			     if(resultCode == RESULT_OK){   
			    	 COUNTRY_NAME = data.getStringExtra(CountryListActivity.RESULT_COUNTRY_NAME);
			    	 COUNTRY_CODE = data.getStringExtra(CountryListActivity.RESULT_COUNTRY_CODE);
			    	 updateViews();
			     }
			     if (resultCode == RESULT_CANCELED) {    
			         
			     }
			  }else if (requestCode == SIGNUP_PROCESS){
				  if(resultCode == ApiService.RESULT_STATUS_ERROR){
					  // only return if failed
					  AlertDialog.Builder adb = new AlertDialog.Builder(this);
					  adb.setTitle("Signup failed");
					  String msgSting = this.getResources().getString(R.string.signup_failed);
					  adb.setMessage(msgSting);
					  adb.setIcon(android.R.drawable.ic_dialog_alert);
					  adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					        public void onClick(DialogInterface dialog, int which) {
					        	dialog.dismiss();
					        } 
					  });
	
					  adb.setCancelable(false);
					  adb.show();
				  }
			  }
			  
		}
		
		private void startSignup(){
			  AlertDialog.Builder adb = new AlertDialog.Builder(this);
			  adb.setTitle("Note");
			  String formatString = this.getResources().getString(R.string.sms_warning);
			  adb.setMessage(String.format(formatString, FORMATED_NUMBER));
			  adb.setIcon(android.R.drawable.ic_dialog_alert);
			  adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int which) {
			        	Intent intent = new Intent(VerifyNumberActivity.this, SignupActivity.class);
			        	intent.putExtra(EXTRA_NUMBER, FORMATED_NUMBER);
			        	startActivityForResult(intent,SIGNUP_PROCESS);
			      } });

			  adb.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int which) {
			           dialog.dismiss();
			      } });
			  adb.show();
		}

}
