package com.rn300.pleaseapp.lists.children;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.rn300.pleaseapp.R;
import com.rn300.pleaseapp.lists.ListItemInterface;

public class AddChildItem implements ListItemInterface{
	private final Context mCtx;
	
	public AddChildItem(Context ctx){
		mCtx = ctx;
	}
		
	 	
	public View getView(LayoutInflater inflater, View convertView, ViewGroup parent) {
		final Typeface lightFont = Typeface.createFromAsset(mCtx.getAssets(),"fonts/HelveticaNeue.ttf");
		View view;
		if (convertView == null) {
			view = (View) inflater.inflate(R.layout.child_add_view, parent, false);
	        // Do some initialization
		} else {
			view = convertView;
		}
		overrideFonts(mCtx,view,lightFont);
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
		
	}


	@Override
	public int getItemViewType(int position) {
		return 0;
	}
}
