package com.rn300.pleaseapp.lists.contacts;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.rn300.pleaseapp.R;
import com.rn300.pleaseapp.lists.ListItemInterface;

public class ContactSection implements ListItemInterface{
	private String mTitle = "";
	private final Context mCtx;
	
	private class ViewHolder{
		TextView title;
	}
	
	public ContactSection (Context ctx,String title){
		mTitle = title;
		mCtx = ctx;
	}
	

	public View getView(LayoutInflater inflater, View convertView,ViewGroup parent) {
		ViewHolder holder;
		View view;
		if (convertView == null) {
			view = (View) inflater.inflate(R.layout.contacts_section_header, parent, false);
			TextView titleView = (TextView) view.findViewById(R.id.title);
			titleView.setPaintFlags(titleView.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
			final Typeface mFont = Typeface.createFromAsset(mCtx.getAssets(),"fonts/HelveticaNeue.ttf"); 
			titleView.setTypeface(mFont);
			
			holder = new ViewHolder();
			holder.title = titleView;
			view.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
			view = convertView;
		}
	
		holder.title.setText(mTitle);
		
		return view;
	}
	
	@Override
	public void remove() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public int getItemViewType(int position) {
		return ContactListAdapter.RowType.SECTION_TITLE.ordinal();
	}

}
