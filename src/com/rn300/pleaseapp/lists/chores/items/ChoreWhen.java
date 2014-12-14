package com.rn300.pleaseapp.lists.chores.items;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.rn300.pleaseapp.R;
import com.rn300.pleaseapp.lists.ListItemInterface;
import com.rn300.pleaseapp.lists.chores.adapters.ChoreListAdapter;

public class ChoreWhen implements ListItemInterface{
	@SuppressWarnings("unused")
	private static final String TAG = "ChoreWhen";
	
	private final Context mCtx;
	private final String mWhen;
	private final String mId;
	private final int mResId;
	
	private class ViewHolder{
		public TextView whenView;
	}
	
	public String getId(){
		return mId;
	}
	
	public String getWhen(){
		return mWhen;
	}
	
	public ChoreWhen(Context ctx, int resId ,String id, long when){
		mCtx = ctx;
		mResId = resId;
		mId = id;
		
		if(when != -1){
			LocalDate today = new LocalDate();
			LocalDate date = new LocalDate(when);
			
			if(today.isBefore(date) || today.equals(date)){
				int days = Days.daysBetween(today, date).getDays();
				switch(days){
				case(0):
					mWhen = "TODAY";
					break;
				case(1):
					mWhen = "TOMORROW";
					break;
				default:
					mWhen = "IN THE FUTURE";
					break;
				}
			} else {
				mWhen = "OVERDUE";
			}
		} else {
			mWhen = "WHENEVER";
		}
		
	}

	@Override
	public View getView(LayoutInflater inflater, View convertView, ViewGroup parent) {
		Typeface font = Typeface.createFromAsset(mCtx.getAssets(),"fonts/HelveticaNeue.ttf"); 
		View view;
		ViewHolder holder;
		if (convertView == null) {
			view = (View) inflater.inflate(mResId, parent, false);
			overrideFonts(mCtx,view,font);
			
			holder = new ViewHolder();
			holder.whenView = (TextView)view.findViewById(R.id.chore_when_title);
			view.setTag(holder);
		} else {
			holder = (ViewHolder)convertView.getTag();
			view = convertView;
		}
		
		holder.whenView.setText(mWhen);
		
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
	        }
	    } catch (Exception e) {
	 }
	 }
	
	@Override
	public int hashCode(){
		return mId.hashCode() ^ mWhen.hashCode();
	}
	
	public boolean equals(Object obj) {
	        if (obj == null)
	            return false;
	        if (obj == this)
	            return true;
	        if (!(obj instanceof ChoreWhen))
	            return false;
	       
	        ChoreWhen c = (ChoreWhen) obj;
	        if(c.getId().equals(this.getId()) && c.getWhen().equals(this.getWhen()))
	        	return true;
	        else
	        	return false;
	 }

	@Override
	public void remove() {
		// TODO run animation
		
	}

	@Override
	public int getItemViewType(int position) {
		return ChoreListAdapter.RowTypes.CHORE_WHEN.ordinal();
	}
}
