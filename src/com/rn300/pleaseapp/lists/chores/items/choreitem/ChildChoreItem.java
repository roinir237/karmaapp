package com.rn300.pleaseapp.lists.chores.items.choreitem;


import java.util.Date;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.rn300.pleaseapp.ApiService;
import com.rn300.pleaseapp.GlobalState;
import com.rn300.pleaseapp.R;

@SuppressLint("ValidFragment")
public class ChildChoreItem extends ChoreItem {
	@SuppressWarnings("unused")
	private final static String TAG = "ChildChoreItem";
	
	public ChildChoreItem(Context ctx, JSONObject object) {
		super(ctx, object);
	
	}

	private void setupStatus (View v){
		try {
			switch(super.choreObject.getInt(STATUS)){
			case(CHORE_STATUS_PENDING):
				v.setBackgroundResource(R.drawable.chore_background_pending_child);
				break;
			case(CHORE_STATUS_COMPLETE):
				// The user that set it marked it as complete 
				v.setBackgroundResource(R.drawable.chore_background_complete);
				break;
			case(CHORE_STATUS_APPROVED):
				// Origin has approved the chore was complete
				v.setBackgroundResource(R.drawable.chore_background_approved);
				break;
			}
		} catch (JSONException e1) {  
			
		}
	}
	
	private void setupDelivery(ImageView deliveryImage){
		try {
			switch(choreObject.getInt(DELIVERY)){
			case(CHORE_DELIVERY_FAILED):
				deliveryImage.setImageResource(R.drawable.delivery_failed);
				break;
			case(CHORE_DELIVERY_PENDING):
				deliveryImage.setImageResource(R.drawable.delivery_pending);
				break;
			case(CHORE_DELIVERY_RECEIVED):
				deliveryImage.setImageResource(R.drawable.delivery_received);
				break;
			case(CHORE_DELIVERY_SYNCED):
				deliveryImage.setImageResource(R.drawable.delivery_sync);
				break;
			}
		} catch (JSONException e1) {
			
		}
	}

	@Override
	public View getView(LayoutInflater inflater, View convertView, ViewGroup parent) {
		ViewHolder cHolder;
		View view;
		if (convertView == null) {
			view = (View) inflater.inflate(R.layout.child_chore_row_view, parent, false);
			cHolder = new ViewHolder();
			
			final Typeface lightFont = Typeface.createFromAsset(mCtx.getAssets(),"fonts/HelveticaNeueLight.ttf"); 
			
			TextView dateView = (TextView) view.findViewById(R.id.chore_date);
			TextView timeView = (TextView) view.findViewById(R.id.chore_time_view);
			TextView karmaView = (TextView) view.findViewById(R.id.chore_karma);
			TextView nameView = (TextView) view.findViewById(R.id.chore_name);
			TextView chore_details_view = (TextView) view.findViewById(R.id.chore_details_view);
			
			nameView.setPaintFlags(nameView.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
			karmaView.setPaintFlags(karmaView.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
			dateView.setPaintFlags(dateView.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
			timeView.setPaintFlags(dateView.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
			chore_details_view.setPaintFlags(chore_details_view.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
			
			nameView.setTypeface(lightFont);
			karmaView.setTypeface(lightFont);
			dateView.setTypeface(lightFont);
			timeView.setTypeface(lightFont);
			chore_details_view.setTypeface(lightFont);
			
			cHolder.delivery = (ImageView) view.findViewById(R.id.deliveryStatusImage);
			cHolder.more = view.findViewById(R.id.chore_more);
			cHolder.name = nameView;
			cHolder.karma = karmaView;
			cHolder.date = dateView;
			cHolder.time = timeView;
			cHolder.details = chore_details_view;
	
			view.setTag(cHolder);
		} else {
			cHolder = (ViewHolder)convertView.getTag();
			cHolder.more.setVisibility(View.GONE);
			view = convertView;
		}
		
		// Show the correct status
		setupStatus(view);
		
		// Show the correct delivery  
		setupDelivery(cHolder.delivery);
		
		// Show date
		if(choreObject.has(TIME)){
			try {
				long longTime = choreObject.getLong(TIME);
				String date = GlobalState.getDateFormat().format(new Date(longTime));
				String time = GlobalState.getTimeFormat().format(new Date(longTime));
				
				LocalDate today = new LocalDate();
				LocalDate dueDate = new LocalDate(longTime);
				
				cHolder.date.setVisibility(View.VISIBLE);
				cHolder.time.setVisibility(View.VISIBLE);
				
				if(today.isAfter(dueDate) || Days.daysBetween(today, dueDate).getDays() > 1){
					cHolder.date.setText("Due: "+ date + " " + time);
					cHolder.time.setVisibility(View.GONE);
				} else {
					cHolder.time.setText(time);
					cHolder.date.setVisibility(View.GONE);
				}
				
			} catch (JSONException e) {
			
			}	
		}else{
			cHolder.date.setVisibility(View.GONE);
			cHolder.time.setVisibility(View.GONE);
		}
		
		// Show karma
		if(choreObject.has(KARMA)){
			try {
				int karma = choreObject.getInt(KARMA);
				
				cHolder.karma.setText(karma == 0 ? "No karma":Integer.toString(karma));
			} catch (JSONException e) {
				e.printStackTrace();
			}	
		}
		
		// Show the name
		try {
			cHolder.name.setText(choreObject.getString("name"));
		} catch (JSONException e) {
			cHolder.name.setText("Couldn't load chore name");
			e.printStackTrace();
		}
		
		// show chore details
		String details = "";
		try {
			details = choreObject.getString(DETAILS);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		cHolder.details.setText(details);
		
		if(details.equals("")) cHolder.details.setVisibility(View.GONE);
		
		return view;
	}
	
	@Override
	public void markStatus(View v) {
		int currentStatus = getChoreStatus();
		
		if(currentStatus < ChoreItem.CHORE_STATUS_APPROVED){
			try {
				this.putInt(ChoreItem.STATUS, ChoreItem.CHORE_STATUS_APPROVED,true);
				v.setBackgroundResource(R.drawable.chore_background_approved);
				int karma = choreObject.getInt(ChoreItem.KARMA);
				ApiService.pledgeKarma(mCtx, -karma);
				ApiService.addKarma(mCtx, -karma);
				mCtx.sendBroadcast(new Intent(ApiService.REFRESH_KARMA));
				notifyOnRevertEnd();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
}
