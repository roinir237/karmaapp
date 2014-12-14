package com.rn300.pleaseapp.activities;

import java.io.ByteArrayOutputStream;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.droid4you.util.cropimage.CropImage;
import com.rn300.pleaseapp.ApiService;
import com.rn300.pleaseapp.R;
import com.rn300.pleaseapp.lists.children.ChildItem;

public class UserProfileActivity extends Activity {
	private static final String TAG = "UserProfileActivity";
	private static final int REQUEST_CODE_PICK_IMAGE = 1;
	private static final int REQUEST_CODE_CROP_IMAGE = 2;
	private Bitmap bitmap;
		
	private EditText nameEdit;
	private ImageView picSelector;
	
	private boolean PROCESSING = false;
	private String compressedImage = "";
	
	private String userName = "";
	
	private ProgressDialog submitDialog;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_profile);
	    
	    nameEdit = (EditText) findViewById(R.id.user_name_edit);
	    picSelector = (ImageView) findViewById(R.id.profile_pic_selector);
	 
	    userName = ApiService.getOwnName(this);
	    if(userName != ""){
	    	nameEdit.setText(userName);
	    }
	    compressedImage = ApiService.getOwnPic(this);
	    if(!compressedImage.equals("")){
	    	new UnprocessImage().execute(compressedImage);
	    }
	}
	
	public void pickImage(View View) {
		// TODO allow users to choose to capture a new picture using the camera
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
              
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
    }
	
	public void submitProfile(View view){
		userName = nameEdit.getText().toString();
		
		if(!userName.equals("")){
			if(!PROCESSING){
				submitDialog = new ProgressDialog(UserProfileActivity.this);
				submitDialog.setMessage("Updating profile...");
				submitDialog.setIndeterminate(true);
				submitDialog.setCancelable(false);
				submitDialog.show();
				String update = "{\"name\":\""+userName+"\",\"pic\":\""+compressedImage+"\"}";
				Intent intent = new Intent(this, ApiService.class);
		    	intent.setAction(ApiService.UPDATE_USER);
		    	intent.putExtra(ApiService.USER_DETAILS_STRING, update);
		    	
		    	intent.putExtra(ApiService.PARAM_RESULT_RECEIVER, new ResultReceiver(new Handler()){
		    		 @Override
		    		 protected void onReceiveResult(int resultCode, Bundle resultData) {
		    		        if(resultCode == ApiService.RESULT_STATUS_OK){
		    		        	Intent i = new Intent(getBaseContext(), MainActivity.class);
		    		        	submitDialog.dismiss();
		    		        	startActivity(i);
		    		        	finish();
		    		        }else{
		    		        	Toast.makeText(getBaseContext(), "Failed!!!!!!", Toast.LENGTH_SHORT).show();
		    		        }
		    		 }
		    	});
		    	this.startService(intent);
			}else{
				Toast.makeText(this, "Still processing image", Toast.LENGTH_SHORT).show();
			}
		}else{
			Toast.makeText(this, "Please provide a name to proceed", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode != RESULT_OK) {
			return;
		}  
		
		switch(requestCode){
		case(REQUEST_CODE_PICK_IMAGE):
		    if (bitmap != null) {
                bitmap.recycle();
            }
		
			// I've changed cropImage to get the images from the imageUri and not absolute path
			String mSelectedImagePath = getPath(data.getData());//data.getData().toString();
		    runCropImage(mSelectedImagePath);
   
			break;
		case(REQUEST_CODE_CROP_IMAGE):
			Bitmap newImage = data.getParcelableExtra("data");
			Bitmap croppedImage = ChildItem.getCroppedBitmap(newImage);
			picSelector.setImageBitmap(croppedImage);
			new ProcessImage().execute(newImage);
		   
			break;
		}
         
        
    }

	private final class UnprocessImage extends AsyncTask<String,String, Bitmap> {
			private ProgressDialog dialog;
			
			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				PROCESSING = true;
				dialog = new ProgressDialog(UserProfileActivity.this);
				dialog.setMessage("Retrieving image...");
				dialog.setIndeterminate(true);
				dialog.setCancelable(false);
				dialog.show();
			}
			@Override
			protected void onProgressUpdate(String... unused) {
				super.onProgressUpdate(unused);
				String msg = unused[0];
				Log.d(TAG,msg);
			}	
			@Override
			protected void onPostExecute(Bitmap result) {
				super.onPostExecute(result);
				PROCESSING = false;
				picSelector.setImageBitmap(result);
				dialog.dismiss();
			}
			@Override
			protected Bitmap doInBackground(String... stringArray) {
				String image64 = stringArray[0];
				byte[] imageAsBytes = Base64.decode(image64, Base64.NO_WRAP);
				Bitmap image = BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);
				image = ChildItem.getCroppedBitmap(image);
				publishProgress("FINSIHED");
				return image;
			 }
	
	};
	
	class ProcessImage extends AsyncTask<Bitmap,String, String> {

		private ProgressDialog dialog;
			@Override
			protected void onPreExecute() {
				PROCESSING = true;
				dialog = new ProgressDialog(UserProfileActivity.this);
				dialog.setMessage("Processing image ...");
				dialog.setIndeterminate(true);
				dialog.setCancelable(false);
				dialog.show();
			}
			@Override
			protected String doInBackground(Bitmap... imageArray) {
				Bitmap image = imageArray[0];
				ByteArrayOutputStream bao = new ByteArrayOutputStream();
				image.compress(Bitmap.CompressFormat.JPEG, 90, bao);
				byte [] ba = bao.toByteArray();
				String image64=Base64.encodeToString(ba, Base64.NO_WRAP);
				return image64;
			 }
			
			protected void onPostExecute(String result) {
				PROCESSING = false;
				compressedImage = result;
				dialog.dismiss();
			}
		 };
		 
	public String getPath(Uri uri) {
		 Cursor cursor = null;
		  try { 
		    String[] proj = { MediaStore.Images.Media.DATA };
		    cursor = this.getContentResolver().query(uri,  proj, null, null, null);
		    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		    cursor.moveToFirst();
		    return cursor.getString(column_index);
		  } finally {
		    if (cursor != null) {
		      cursor.close();
		    }
		  }
		}	 
	
	 private void runCropImage(String sourceImage) {

		 // create explicit intent
		 Intent cropIntent = new Intent(this,CropImage.class);
		 
		 cropIntent.putExtra("circleCrop", true);
		 cropIntent.putExtra("image-path", sourceImage);
		 cropIntent.putExtra("aspectX", 1);
		 cropIntent.putExtra("aspectY", 1);
		 cropIntent.putExtra("outputX", 180);
		 cropIntent.putExtra("outputY", 180);
		 cropIntent.putExtra("scale", true);
		 cropIntent.putExtra("return-data", true);
		 

		 // start activity CropImage with certain request code and listen
		 // for result
		 startActivityForResult(cropIntent, REQUEST_CODE_CROP_IMAGE);
	 }
}
