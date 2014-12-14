package com.rn300.pleaseapp.lists.contacts;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.rn300.pleaseapp.R;
import com.rn300.pleaseapp.lists.ListItemInterface;
import com.rn300.pleaseapp.lists.children.ChildItem;

public class ContactItem implements ListItemInterface{
	private JSONObject contactObject;
	private Bitmap picBmp = null; 
	public static final String ID_PROP = "_id";
	public static final String NAME_PROP = "name";
	public static final String NUMBER_PROP = "number";
	public static final String PIC_PROP = "pic";
	private final Context mCtx;
	
	private class ViewHolder{
		TextView name;
		TextView number;
		ImageView pic;
	}
	
	public ContactItem(Context ctx,String string) {
		mCtx = ctx;
		try {
			contactObject = new JSONObject(string);
		} catch (JSONException e) {
			contactObject = new JSONObject();
			e.printStackTrace();
		}
		if(contactObject.has(PIC_PROP)){
			try {
				setBitmap(contactObject.getString(PIC_PROP));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
			picBmp = BitmapFactory.decodeResource(mCtx.getResources(), R.drawable.ic_contact_picture);
		}
	}
	
	public String getChildId(){
		if(contactObject.has(ID_PROP)){
			try {
				return contactObject.getString(ID_PROP);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return "";
	}
	
	public ContactItem(Context ctx,JSONObject object){
		mCtx = ctx;
		contactObject = object;
		if(contactObject.has(PIC_PROP)){
			try {
				setBitmap(contactObject.getString(PIC_PROP));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	} 
	
	public String getContactName(){
		try {
			return contactObject.getString(NAME_PROP);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return "";
	}
	
	public void setBitmap(String image64){
		byte[] imageAsBytes = Base64.decode(image64, Base64.NO_WRAP);
		Bitmap image = BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);
		if(image != null){
			image = ChildItem.getCroppedBitmap(image);
		} else image = BitmapFactory.decodeResource(mCtx.getResources(), R.drawable.ic_contact_picture);
		this.picBmp = image;
	}
	
	public void setBitmap(Bitmap image){
		image = ChildItem.getCroppedBitmap(image);
		this.picBmp = image;
	}
	
	public String getContactNumber(){
		try {
			return contactObject.getString(NUMBER_PROP);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return "";
	}
	
    
	public View getView(LayoutInflater inflater, View convertView, ViewGroup parent) {
		View view;
		ViewHolder holder;
		if (convertView == null) {
			view = (View) inflater.inflate(R.layout.contact_row_view, parent, false);
			final Typeface lightFont = Typeface.createFromAsset(mCtx.getAssets(),"fonts/HelveticaNeue.ttf");
			TextView nameView = (TextView) view.findViewById(R.id.contact_name);
			nameView.setPaintFlags(nameView.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
			nameView.setTypeface(lightFont);
			TextView numberView = (TextView) view.findViewById(R.id.contact_number);
			numberView.setPaintFlags(numberView.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
			numberView.setTypeface(lightFont);
			ImageView image = (ImageView) view.findViewById(R.id.contact_image);
			
			holder = new ViewHolder();
			holder.name = nameView;
			holder.number = numberView;
			holder.pic = image;
			
			view.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
			view = convertView;
		}
		
		
		try {
			holder.name.setText(contactObject.getString(NAME_PROP));
			holder.number.setText(contactObject.getString(NUMBER_PROP));
		} catch (JSONException e) {
			holder.name.setText("Couldn't load contact name");
			e.printStackTrace();
		}
		
		if(picBmp != null){
			holder.pic.setImageBitmap(picBmp);
		}
		
		return view;
	}

	@Override 
	public int hashCode() {
		return this.getContactNumber().hashCode();
	}
	
	@Override 
	public boolean equals(Object obj) {
	        if (obj == null)
	            return false;
	        if (obj == this)
	            return true;
	        if (!(obj instanceof ContactItem))
	            return false;
	       
	        ContactItem c = (ContactItem) obj;
	        if(c.getContactNumber().equals(this.getContactNumber()))
	        	return true;
	        else
	        	return false;
	 }
	
	@Override
	public int getItemViewType(int position) {
		return ContactListAdapter.RowType.ITEM.ordinal();
	}
	
	@Override
	public String toString(){
		return contactObject.toString();
	}

	@Override
	public void remove() {
		// TODO Auto-generated method stub
		
	}

}
