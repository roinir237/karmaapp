package com.rn300.pleaseapp.lists.chores.items;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.rn300.pleaseapp.R;
import com.rn300.pleaseapp.lists.ListItemInterface;
import com.rn300.pleaseapp.lists.chores.adapters.ChoreListAdapter;

public class ChildTitleItem implements ListItemInterface{
	private final Bitmap mProfile;
	private final String mName;
	private final OnClickListener mListener;	
	private final Context mCtx;
		
	public ChildTitleItem (Context ctx, String childName, Bitmap profile, OnClickListener listen){
		mCtx = ctx;
		mProfile = profile;
		mListener = listen; 
		mName = childName;
	}	
	
	@Override
	public void remove() {
		// Never removed
	}

	@Override
	public View getView(LayoutInflater inflater, View convertView, ViewGroup parent) {
    	View view;
		
		if (convertView == null) {
			view = (View) inflater.inflate(R.layout.child_chores_title, parent, false);
			Typeface font = Typeface.createFromAsset(mCtx.getAssets(),"fonts/HelveticaNeue.ttf");
	    	Typeface lightfont = Typeface.createFromAsset(mCtx.getAssets(),"fonts/HelveticaNeueLight.ttf");
			TextView nameView = (TextView) view.findViewById(R.id.title);
	        nameView.setText(mName);
	        nameView.setPaintFlags(nameView.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
	        nameView.setTypeface(font);
	          
	        ImageView profilePic = (ImageView) view.findViewById(R.id.child_image);
	        profilePic.setImageBitmap(mProfile);
	         
	        Button pleaseBtn = (Button) view.findViewById(R.id.pleaseBtn);
	        pleaseBtn.setTypeface(lightfont);
	        pleaseBtn.setPaintFlags(pleaseBtn.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
	        pleaseBtn.setOnClickListener(mListener);
			
		} else {
			view = convertView;
		}
		
		
		return view;
	}

	@Override
	public int getItemViewType(int position) {
		// TODO Auto-generated method stub
		return ChoreListAdapter.RowTypes.CHORE_TITLE.ordinal();
	}

}
