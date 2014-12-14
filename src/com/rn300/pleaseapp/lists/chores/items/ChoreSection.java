package com.rn300.pleaseapp.lists.chores.items;

import org.json.JSONException;

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

import com.rn300.pleaseapp.ApiService;
import com.rn300.pleaseapp.R;
import com.rn300.pleaseapp.lists.ListItemInterface;
import com.rn300.pleaseapp.lists.children.ChildItem;
import com.rn300.pleaseapp.lists.chores.adapters.ChoreListAdapter;

public class ChoreSection implements ListItemInterface{
	private String mName = "";
	private String mId;
	private final Context mCtx;
	
	private Bitmap sectionPic;
	
	private class ViewHolder{
		ImageView image;
		TextView name;
	}
		
	public ChoreSection (Context ctx, String id){
		mId = id;
		mCtx = ctx;
	}
	
	public void setName(String name){
		mName = name;
	}
	
	public String getId(){
		return mId;
	}

	@Override
	public void remove() {
		// TODO run animation 
	}

	@Override
	public View getView(LayoutInflater inflater, View convertView,ViewGroup parent) {
		ViewHolder holder;
		View view;
		if (convertView == null) {
			view = (View) inflater.inflate(R.layout.my_todo_section_header, parent, false);
			holder = new ViewHolder();
			
			ImageView imageView = (ImageView) view.findViewById(R.id.child_image);
			
			TextView nameView = (TextView) view.findViewById(R.id.section_name);
			nameView.setPaintFlags(nameView.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
			final Typeface mFont = Typeface.createFromAsset(mCtx.getAssets(),"fonts/HelveticaNeueLight.ttf"); 
			nameView.setTypeface(mFont);
			
			holder.image = imageView;
			holder.name = nameView;
			
			view.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
			view = convertView;
		}
		
		if(sectionPic == null){
			try {
				String image64 = ApiService.getChildProp(mCtx, mId, ChildItem.PIC);
				sectionPic = BitmapFactory.decodeResource(mCtx.getResources(), R.drawable.ic_contact_picture);
				if(image64 != null && !image64.equals("")){
					byte[] imageAsBytes = Base64.decode(image64, Base64.NO_WRAP);
					sectionPic = BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);
					sectionPic = ChildItem.getCroppedBitmap(sectionPic);
				}
				
				holder.image.setImageBitmap(sectionPic);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}else{
			holder.image.setImageBitmap(sectionPic);
		}
		
		holder.name.setText(mName);

		return view;
	}
	
	@Override 
	public int hashCode() {
		return mId.hashCode();
	}
	
	 public boolean equals(Object obj) {
	        if (obj == null)
	            return false;
	        if (obj == this)
	            return true;
	        if (!(obj instanceof ChoreSection))
	            return false;
	       
	        ChoreSection c = (ChoreSection) obj;
	        if(c.getId().equals(mId))
	        	return true;
	        else
	        	return false;
	 }

	@Override
	public int getItemViewType(int position) {
		return ChoreListAdapter.RowTypes.CHORE_SECTION.ordinal();
	}

}
