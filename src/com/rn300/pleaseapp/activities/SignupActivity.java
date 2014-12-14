package com.rn300.pleaseapp.activities;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.ResultReceiver;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.TextView;

import com.rn300.pleaseapp.ApiService;
import com.rn300.pleaseapp.R;

public class SignupActivity extends Activity{
	public static final String TAG = "SignupActivity";
    
	private String FORMATED_NUMBER = "";
	private String MESSAGE_BODY = "Hello, please tasker";
	private CountDownTimer smsReceiveTimer = new CountDownTimer(10000,5000){

		@Override
		public void onFinish() {
			publishProgress(STAGES.EXCEPTION);
			SignupActivity.this.setResult(ApiService.RESULT_STATUS_ERROR);
        	SignupActivity.this.finish();
		}

		@Override
		public void onTick(long millisUntilFinished) {
		}
		
	};
	private SmsReceiver smsReceiver = null;
	private BroadcastReceiver smsSendListen = null;
	
	private int ticks = 0;
	private Timer _timer = null;
	
	private static class MsgHandler extends Handler{
		private final WeakReference<SignupActivity> mActivity;
		
		public MsgHandler(SignupActivity activity) {
		      mActivity = new WeakReference<SignupActivity>(activity);
		}
		
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
		    
			SignupActivity activity = mActivity.get();
			if(activity != null){
			    final TextView progressText = (TextView) activity.findViewById(R.id.progress_text);
			        
			    String add = "";
			    String current = progressText.getText().toString();
			    int end = current.indexOf(" .");
			    if(end < 0) end = current.length();
				switch(msg.what){
				case(0):
					current = current.substring(0, end);
					add = "";
					break;
				case(1):
					current = current.substring(0, end);
					add = " .";
					break;
				case(2):
					current = current.substring(0, end);
					add = " ..";
					break;
				case(3):
					current = current.substring(0, end);
					add = " ...";
					break;
				}
					
				
				progressText.setText(current+add);
			}
		 }
	}
	
	private final MsgHandler _handler = new MsgHandler(this);
	
	private enum STAGES{
		SMS_SENT,SMS_RECEIVED,CREATING_USER,REGISTER_GCM,GETTING_WELCOME,REGISTRATION_DONE,EXCEPTION
	}

	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        
        Typeface statusFont = Typeface.createFromAsset(getAssets(),"fonts/HelveticaNeue.ttf");
        
        ((TextView)findViewById(R.id.progress_text)).setTypeface(statusFont);
        String userId = ApiService.getOwnId(this);
        if(userId == null || userId.equals("")){
        	FORMATED_NUMBER = getIntent().getStringExtra(VerifyNumberActivity.EXTRA_NUMBER);
        	MESSAGE_BODY = this.getResources().getString(R.string.invitation_sms);
        	sendSMS(FORMATED_NUMBER,MESSAGE_BODY);
        }else{
        	startUserProfileSetup();
        }
        
	}
	
	/**
	 * Unregister receivers and roll back changes.
	 */
	protected void onPause(){
		super.onPause();
		
		if(smsReceiver != null)
			unregisterReceiver(smsReceiver);
		if(smsSendListen != null)
			unregisterReceiver(smsSendListen);
		
		smsReceiver = null;
		smsSendListen = null;
	}
	
	/**
	 *  --------STAGE 1----------------
	 *  Send an SMS to the phone number and register the receiver to intercept the sms.
	 *  
	 * @param phoneNumber
	 * @param message
	 */
    private void sendSMS(String phoneNumber, String message){  
    	smsReceiver = new SmsReceiver();
		IntentFilter smsFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
		smsFilter.setPriority(2147483647);
    	registerReceiver(smsReceiver,smsFilter);
		
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";
 
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
            new Intent(SENT), 0);
 
        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
            new Intent(DELIVERED), 0);
        
        smsSendListen = new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                    	publishProgress(STAGES.SMS_SENT);
                    	if(smsReceiveTimer != null) smsReceiveTimer.start();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    	publishProgress(STAGES.EXCEPTION);
                    	SignupActivity.this.setResult(ApiService.RESULT_STATUS_ERROR);
    		        	SignupActivity.this.finish();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                    	publishProgress(STAGES.EXCEPTION);
                    	SignupActivity.this.setResult(ApiService.RESULT_STATUS_ERROR);
    		        	SignupActivity.this.finish();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                    	publishProgress(STAGES.EXCEPTION);
                    	SignupActivity.this.setResult(ApiService.RESULT_STATUS_ERROR);
    		        	SignupActivity.this.finish();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                    	publishProgress(STAGES.EXCEPTION);
                    	SignupActivity.this.setResult(ApiService.RESULT_STATUS_ERROR);
    		        	SignupActivity.this.finish();
                        break;
                }
            }
        };
        registerReceiver(smsSendListen,new IntentFilter(SENT));  
        
        
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI); 
    }

    /**
     *  --------STAGE 2----------------
     *  The definition of the receiver that intercepts the SMS 
     *  and compare the sender of the SMS and the phone number entered.
     * @author roi
     *
     */
    public class SmsReceiver extends BroadcastReceiver{
    	@Override
    	public void onReceive(Context context, Intent intent) {
    		abortBroadcast();
    		smsReceiveTimer.cancel();
    		smsReceiveTimer = null;
    		Object[] pdus=(Object[])intent.getExtras().get("pdus");
    		SmsMessage msg=SmsMessage.createFromPdu((byte[]) pdus[0]);
    		String body = msg.getMessageBody();
    		if(msg.getMessageBody().equals(MESSAGE_BODY)){
    			publishProgress(STAGES.SMS_RECEIVED);
    			processSmsResult(msg.getOriginatingAddress());
    		}else{
    			publishProgress(STAGES.EXCEPTION);
    			SignupActivity.this.setResult(ApiService.RESULT_STATUS_ERROR);
	        	SignupActivity.this.finish();
    		}
    	}
    }

    private void processSmsResult(String originNumber){
    	if(originNumber.equals(FORMATED_NUMBER)){
    		
    		Log.d(TAG,"Success permit signup!");
    		createUser();
    	}
    }
    
    /**
     * ---------- STAGE 3 ---------------
     * Start the API service to conntect to the server and add a user with the given
     * phone number. At the end of this stage the user details record has an id field.  
     *
     */
    private void createUser(){
    	publishProgress(STAGES.CREATING_USER);
    	Intent intent = new Intent(getBaseContext(), ApiService.class);
    	intent.setAction(ApiService.REGISTER_USER);
    	intent.putExtra(ApiService.USER_NUMBER, FORMATED_NUMBER);
    	intent.putExtra(ApiService.PARAM_RESULT_RECEIVER, new ResultReceiver(new Handler()){
    		 @Override
    		 protected void onReceiveResult(int resultCode, Bundle resultData) {
    		        if(resultCode == ApiService.RESULT_STATUS_OK){
    		         	registerGcm();
    		        }else{
    		        	publishProgress(STAGES.EXCEPTION);
    		        	
    		        	SignupActivity.this.setResult(ApiService.RESULT_STATUS_ERROR);
    		        	SignupActivity.this.finish();
    		        }
    		 }
    	});
    
    	this.startService(intent);
    }

    /** 
     * ---------- STAGE 4 ---------------
     * Start the API service to register a GCM code with the server. At the end of this stage the server holds a 
     * record of the number. 
     */
    private void registerGcm(){
    	publishProgress(STAGES.REGISTER_GCM);
    	Intent intent = new Intent(getBaseContext(), ApiService.class);
    	intent.setAction(ApiService.REGISTER_GCM);
    	intent.putExtra(ApiService.PARAM_RESULT_RECEIVER, new ResultReceiver(new Handler()){
    		 @Override
    		 protected void onReceiveResult(int resultCode, Bundle resultData) {
    		        if(resultCode == ApiService.RESULT_STATUS_OK){
    		        	getWelcomeChore();
    		        }else{
    		        	publishProgress(STAGES.EXCEPTION);
    		        	SignupActivity.this.setResult(ApiService.RESULT_STATUS_ERROR);
    		        	SignupActivity.this.finish();
    		        }
    		 }
    	});
    	this.startService(intent);
    }
    
    /** 
     * ---------- STAGE 5 ---------------
     * Start the API service to register a GCM code with the server. At the end of this stage the server holds a 
     * record of the number. 
     */
    private void getWelcomeChore(){
    	publishProgress(STAGES.GETTING_WELCOME);
    	Intent intent = new Intent(getBaseContext(), ApiService.class);
    	intent.setAction(ApiService.GET_WELCOME);
    	intent.putExtra(ApiService.PARAM_RESULT_RECEIVER, new ResultReceiver(new Handler()){
    		 @Override
    		 protected void onReceiveResult(int resultCode, Bundle resultData) {
    		        if(resultCode == ApiService.RESULT_STATUS_OK){
    		        	startUserProfileSetup();
    		        }else{
    		        	publishProgress(STAGES.EXCEPTION);
    		        	SignupActivity.this.setResult(ApiService.RESULT_STATUS_ERROR);
    		        	SignupActivity.this.finish();
    		        }
    		 }
    	});
    	this.startService(intent);
    }
    
    
    private void startUserProfileSetup(){
		Intent intent = new Intent(this,UserProfileActivity.class);
		startActivity(intent);
		finish();
	}
    
    private void publishProgress(STAGES stage){
    	final TextView progressText = (TextView) findViewById(R.id.progress_text);
    	
    	switch(stage){
    	case SMS_SENT:
    		progressText.setText("Equilibrating Chakras");
    		break;
    	case SMS_RECEIVED:
    		progressText.setText("Accepted");
    		break;
    	case CREATING_USER:
    		progressText.setText("Retrieving past cycles");
    		break;
    	case REGISTER_GCM:
    		progressText.setText("Forming a connection");
    		break;
    	case EXCEPTION:
    		progressText.setText("Oops!");
    		break;
    	case REGISTRATION_DONE:
    		break;
    	case GETTING_WELCOME:
    		progressText.setText("Welcome!");
    		break;
    	}
    	
    	if(_timer == null){
    		_timer = new Timer();
    		_timer.schedule(new TimerTask(){

				@Override
				public void run() {
					_handler.sendEmptyMessage(ticks%4);
					ticks++;
				}
    			
    		}, 0,500);
    	}
    }

}
