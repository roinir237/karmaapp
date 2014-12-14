package com.rn300.pleaseapp.lists.chores.items;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.rn300.pleaseapp.R;
import com.rn300.pleaseapp.lists.ListItemInterface;
import com.rn300.pleaseapp.lists.chores.adapters.ChoreListAdapter;

public class TitleItem implements ListItemInterface{
	private final Context mCtx;
	
	public TitleItem (Context ctx){
		mCtx = ctx;
	}	
	
	@Override
	public void remove() {
		// Never removed
	}

	@Override
	public View getView(LayoutInflater inflater, View convertView, ViewGroup parent) {
		View view;
		
		if (convertView == null) {
			view = (View) inflater.inflate(R.layout.my_todo_title, parent, false);
			TextView titleView = (TextView) view.findViewById(R.id.title);
			
			Typeface mFont = Typeface.createFromAsset(mCtx.getAssets(),"fonts/HelveticaNeueUltraLight.ttf"); 
			titleView.setTypeface(mFont);
			titleView.setPaintFlags(titleView.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
		} else {
			view = convertView;
		}
		
		return view;	
	}
	
	@Override
	public int getItemViewType(int position) {
		return ChoreListAdapter.RowTypes.CHORE_TITLE.ordinal();
	}
}
