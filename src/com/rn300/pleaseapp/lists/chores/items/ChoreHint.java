package com.rn300.pleaseapp.lists.chores.items;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.rn300.pleaseapp.lists.ListItemInterface;
import com.rn300.pleaseapp.lists.chores.adapters.ChoreListAdapter;

public class ChoreHint implements ListItemInterface{
	@SuppressWarnings("unused")
	private static final String TAG = "ChoreHint";
	
	private final Context mCtx;
	private final int mResid;
	
	public ChoreHint(Context ctx, int resid){
		mCtx = ctx;
		mResid = resid;
	}


	@Override
	public View getView(LayoutInflater inflater, View convertView, ViewGroup parent) {
		Typeface font = Typeface.createFromAsset(mCtx.getAssets(),"fonts/HelveticaNeue.ttf"); 
		View view;
		if (convertView == null || (Integer)convertView.getTag() != mResid) {
			view = (View) inflater.inflate(mResid, parent, false);
			overrideFonts(mCtx,view,font);
			view.setTag(mResid);
			
		} else {
			view = convertView;
		}
	
		return view;
	}
	
	private void overrideFonts(final Context context, final View v, final Typeface font) {
	    try {
	        if (v instanceof ViewGroup) {
	            ViewGroup vg = (ViewGroup) v;
	            for (int i = 0; i < vg.getChildCount(); i++) {
	                View child = vg.getChildAt(i);
	                overrideFonts(context, child,font);
	         }
	        } else if (v instanceof TextView ) {
	            ((TextView) v).setTypeface(font);
	            ((TextView) v).setPaintFlags(((TextView) v).getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
	        }
	    } catch (Exception e) {
	 }
	 }


	@Override
	public void remove() {
		// TODO run animation
		
	}


	@Override
	public int getItemViewType(int position) {
		return ChoreListAdapter.RowTypes.CHORE_HINT.ordinal();
	}
}
