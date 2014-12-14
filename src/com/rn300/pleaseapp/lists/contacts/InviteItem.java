	package com.rn300.pleaseapp.lists.contacts;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.rn300.pleaseapp.R;
import com.rn300.pleaseapp.lists.ListItemInterface;

public class InviteItem implements ListItemInterface{
	private final Context mCtx;
	
	public InviteItem(Context ctx){
		mCtx = ctx;
	}
	
	public View getView(LayoutInflater inflater, View convertView, ViewGroup parent) {
		View view;
		if (convertView == null) {
			view = (View) inflater.inflate(R.layout.contact_invite_view, parent, false);
		} else {
			view = convertView;
		}		
		
		final Typeface lightFont = Typeface.createFromAsset(mCtx.getAssets(),"fonts/HelveticaNeue.ttf");
		((TextView)view.findViewById(R.id.invite_text)).setTypeface(lightFont);
		return view;
	}


	@Override
	public void remove() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getItemViewType(int position) {
		return ContactListAdapter.RowType.INVITE_ITEM.ordinal();
	}
	
}
