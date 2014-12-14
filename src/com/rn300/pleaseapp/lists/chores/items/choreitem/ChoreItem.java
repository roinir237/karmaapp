package com.rn300.pleaseapp.lists.chores.items.choreitem;



import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.rn300.pleaseapp.ApiService;
import com.rn300.pleaseapp.GlobalState;
import com.rn300.pleaseapp.R;
import com.rn300.pleaseapp.ReminderService;
import com.rn300.pleaseapp.lists.ListItemInterface;
import com.rn300.pleaseapp.lists.chores.adapters.ChoreListAdapter;

@SuppressLint({ "ValidFragment", "SimpleDateFormat" })
public class ChoreItem extends ChoreDraggable implements ListItemInterface{
	public static final String ID 				= "_id";
	public static final String TITLE			= "name";
	public static final String DESTINATION 		= "userId";
	public static final String ORIGIN 			= "origin";
	public static final String TIME 			= "time";
	public static final String DETAILS	 		= "details";
	public static final String KARMA 			= "karma";
	public static final String KARMA_OBTAINED 	= "karmaObtained";
	public static final String SHOW 			= "show";
	public static final String REMINDER 		= "reminder";
	public static final String REMINDER_TIME 	= "ti";
	public static final String REMINDER_ID 		= "id";
	public static final String HIGHLIGHT 		= "highlight";
	
	public static final String STATUS = "status";
	public static final int CHORE_STATUS_PENDING = 0;
    public static final int CHORE_STATUS_COMPLETE = 1;
    public static final int CHORE_STATUS_APPROVED = 2;
    
    public static final String DELIVERY = "delivery";
    public static final int CHORE_DELIVERY_PENDING = 0;
    public static final int CHORE_DELIVERY_SYNCED = 1;
    public static final int CHORE_DELIVERY_RECEIVED = 2;
    public static final int CHORE_DELIVERY_FAILED = -1;
	
	protected final JSONObject choreObject;	
	protected final Context mCtx;
	
	protected class ViewHolder{
		public ImageView delivery;
		public TextView name;
		public TextView details;
		public TextView date;
		public TextView time;
		public TextView karma;
		public View more;
		public ListView.LayoutParams layoutParams;
		
		public Button addReminder;
		public Button reminderTime;
		public Button reminderDate;
		public ImageView removeReminder;
	}
	
	public ChoreItem(Context ctx, JSONObject object){
		choreObject = object;
		mCtx = ctx;
	}
	
	public ChoreItem(Context ctx, String string) throws JSONException {
		choreObject = new JSONObject(string); 
		mCtx = ctx;
	}
	
	public String getString(String prop){
		if(choreObject.has(prop)){
			try {
				return choreObject.getString(prop);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return "";
	}
	
	public Integer getInt(String prop){
		if(choreObject.has(prop)){
			try {
				return choreObject.getInt(prop);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	private void setupStatus (View v){
		try {
			switch(choreObject.getInt(STATUS)){
			case(CHORE_STATUS_PENDING):
				// The chore was just set and no action was taken to complete it yet
				v.setBackgroundResource(R.drawable.chore_background_pending);
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
			((ViewGroup)deliveryImage.getParent()).removeView(deliveryImage);
		}
	}
	
	private void setupReminder(Button addReminderBtn,Button reminderDateBtn,Button reminderTimeBtn,ImageView removeBtn){
		if(choreObject.has(REMINDER)){
			try {
				addReminderBtn.setVisibility(View.GONE);	
				Long reminderLong = choreObject.getJSONObject(REMINDER).getLong(REMINDER_TIME); 
				String dateString = GlobalState.getDateFormat().format(new Date(reminderLong));
				reminderDateBtn.setVisibility(View.VISIBLE);	
				reminderDateBtn.setText(dateString);
				String timeString = GlobalState.getTimeFormat().format(new Date(reminderLong));
				reminderTimeBtn.setVisibility(View.VISIBLE);	
				reminderTimeBtn.setText(timeString);
				removeBtn.setVisibility(View.VISIBLE);
				removeBtn.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						removeReminder(v);
					}
				});
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
		}else {
			addReminderBtn.setVisibility(View.VISIBLE);
		}
		
		addReminderBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				try {
					long reminderLong;
					if(GlobalState.defaultReminderEnabled() && !choreObject.has(REMINDER) && 
							choreObject.has(TIME) && 
							choreObject.getLong(TIME) > Calendar.getInstance().getTimeInMillis() + 1000*60*GlobalState.getRemindBeforeTime()){
						reminderLong = choreObject.getLong(TIME) - 1000*60*GlobalState.getRemindBeforeTime();
					}else{
						reminderLong = -1L;
					}
					setReminder((View)v.getParent(),reminderLong);
				} catch (JSONException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}	
		});
		reminderDateBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				DialogFragment newFragment = new ReminderDatePickerFragment(v);
		        newFragment.show(((FragmentActivity) mCtx).getSupportFragmentManager(), "datePicker");
			}
		});
		reminderTimeBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				DialogFragment newFragment = new ReminderTimePickerFragment(v);
		        newFragment.show(((FragmentActivity) mCtx).getSupportFragmentManager(), "timePicker");
			}
		});
	}
	
	 public class ReminderTimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {
	    	private final View srcView;
	    	public ReminderTimePickerFragment(View v){
	    		super();
	    		srcView = v;
	    	}
	    	
			@SuppressWarnings("deprecation")
			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				    // Use the current time as the default values for the picker
			    final Calendar c = Calendar.getInstance();
			    int hour = c.get(Calendar.HOUR_OF_DAY);
			    int minute = c.get(Calendar.MINUTE);
			    
			    String current = "";
				if(srcView instanceof Button) current = ((Button)srcView).getText().toString();
				try {
					Date currentDate = GlobalState.getTimeFormat().parse(current);
					minute = currentDate.getMinutes();
					hour = currentDate.getHours();
				} catch (ParseException e) {
				
			}
		
		    // Create a new instance of TimePickerDialog and return it
		    return new TimePickerDialog(getActivity(), this, hour, minute,GlobalState.is24HourFormat());
		    
		}
		
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			String min = minute < 10 ? "0"+Integer.toString(minute):Integer.toString(minute); 
			String hour;
			if(!GlobalState.is24HourFormat()) {
				String a = "AM";
				if(hourOfDay > 12){
					hourOfDay = hourOfDay - 12;
					a = "PM";
				}
				hour = hourOfDay < 10 ? "0"+Integer.toString(hourOfDay):Integer.toString(hourOfDay);
				if(srcView instanceof Button) {
					((Button)srcView).setText(hour+":"+min+" "+a);
				}
			} else {
				hour = hourOfDay < 10 ? "0"+Integer.toString(hourOfDay):Integer.toString(hourOfDay);
				if(srcView instanceof Button) {
					((Button)srcView).setText(hour+":"+min);
				}
			}
			onTimeDateSet((View)srcView.getParent());
		}
	}
	    
	public class ReminderDatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
		private final View srcView;
		public ReminderDatePickerFragment(View v){
			super();
			srcView = v;
		}
		
		@SuppressWarnings("deprecation")
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
		    
			String current = "";
			final Calendar c = Calendar.getInstance();
			int year = c.get(Calendar.YEAR);
			int month = c.get(Calendar.MONTH);
			int day = c.get(Calendar.DAY_OF_MONTH);
			
			if(srcView instanceof Button) current = ((Button)srcView).getText().toString();
			try {
				Date currentDate = GlobalState.getDateFormat().parse(current);
				day = currentDate.getDate();
				year = currentDate.getYear()+1900;
				month = currentDate.getMonth();
			} catch (ParseException e) {
				
			}
			
		    // Create a new instance of DatePickerDialog and return it
		    return new DatePickerDialog(getActivity(), this, year, month, day);
		}
		
		public void onDateSet(DatePicker view, int year, int month, int day) {
			month++;
			String  dateString = GlobalState.getDateFormat().format(java.sql.Date.valueOf(year+"-"+month+"-"+day));
			if(srcView instanceof Button) ((Button)srcView).setText(dateString);
			onTimeDateSet((View)srcView.getParent());
		}
	}
	
	private void onTimeDateSet(View parent){
		String date = ((Button)parent.findViewById(R.id.reminder_date_btn)).getText().toString();
		String time = ((Button)parent.findViewById(R.id.reminder_time_btn)).getText().toString();
		
		try {
			SimpleDateFormat formatter = new SimpleDateFormat(GlobalState.getDateFormat().toPattern()+" "+GlobalState.getTimeFormat().toPattern());
			long reminderLong = formatter.parse(date + " " + time).getTime();
			setReminder(parent,reminderLong);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public long getTime(){
		try {
			return choreObject.getLong(TIME);
		} catch (JSONException e) {
			
		}
		return -1;
	}
	
	public void highlight(){
		try {
			choreObject.put(HIGHLIGHT, true);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public View getView(LayoutInflater inflater, View convertView, ViewGroup parent) {
		ViewHolder cHolder;
		View view;
		if (convertView == null) {
			final Typeface lightFont = Typeface.createFromAsset(mCtx.getAssets(),"fonts/HelveticaNeueLight.ttf"); 
			view = (View) inflater.inflate(R.layout.chore_row_view, parent, false);
			cHolder = new ViewHolder();
			
			TextView dateView = (TextView) view.findViewById(R.id.chore_date);
			TextView timeView = (TextView) view.findViewById(R.id.chore_time_view);
			TextView karmaView = (TextView) view.findViewById(R.id.chore_karma);
			TextView nameView = (TextView) view.findViewById(R.id.chore_name);
			TextView chore_details_view = (TextView) view.findViewById(R.id.chore_details_view);
			
			chore_details_view.setPaintFlags(chore_details_view.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
			dateView.setPaintFlags(dateView.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
			timeView.setPaintFlags(dateView.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
			karmaView.setPaintFlags(karmaView.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
			nameView.setPaintFlags(nameView.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
			
			chore_details_view.setTypeface(lightFont);
			dateView.setTypeface(lightFont);
			timeView.setTypeface(lightFont);
			karmaView.setTypeface(lightFont);
			nameView.setTypeface(lightFont);
			
			cHolder.delivery = (ImageView) view.findViewById(R.id.deliveryStatusImage);
			cHolder.more = view.findViewById(R.id.chore_more);
			cHolder.name = nameView;
			cHolder.karma = karmaView;
			cHolder.date = dateView;
			cHolder.time = timeView;
			cHolder.details = chore_details_view;
			
			Button addReminderBtn = (Button) view.findViewById(R.id.reminder_btn);
			Button reminderDateBtn = (Button) view.findViewById(R.id.reminder_date_btn);
			Button reminderTimeBtn = (Button) view.findViewById(R.id.reminder_time_btn);
			ImageView removeBtn = (ImageView) view.findViewById(R.id.remove_reminder_btn);
			
			reminderTimeBtn.setPaintFlags(reminderDateBtn.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
			reminderDateBtn.setPaintFlags(reminderDateBtn.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
			addReminderBtn.setPaintFlags(addReminderBtn.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
			
			reminderDateBtn.setTypeface(lightFont);
			addReminderBtn.setTypeface(lightFont);
			reminderTimeBtn.setTypeface(lightFont);
			
			cHolder.addReminder = addReminderBtn;
			cHolder.reminderDate = reminderDateBtn;
			cHolder.reminderTime = reminderTimeBtn;
			cHolder.removeReminder = removeBtn;
			
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
				cHolder.karma.setText(karma == 0? "No karma" : Integer.toString(karma));
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
		
		// setup reminder button
		setupReminder(cHolder.addReminder,cHolder.reminderDate,cHolder.reminderTime,cHolder.removeReminder);
		
		
		// show chore details
		String details = "";
		try {
			details = choreObject.getString(DETAILS);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		cHolder.details.setText(details);
		if(details.equals("")) cHolder.details.setVisibility(View.GONE);
		
		if(choreObject.has(HIGHLIGHT)){
			try {
				choreObject.remove(HIGHLIGHT);
				ApiService.putChore(mCtx, choreObject, false);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			int bottom = view.getPaddingBottom();
			int top = view.getPaddingTop();
			int right = view.getPaddingRight();
			int left = view.getPaddingLeft();
			view.setBackgroundResource( R.drawable.chore_highlight_transition );
			TransitionDrawable background = (TransitionDrawable) view.getBackground();
			view.setPadding(left, top, right, bottom);
			background.startTransition(mCtx.getResources().getInteger(R.integer.chore_highlight_time));
		}
		
		return view;
	}
	
	private void setReminder(View v, long reminderLong) throws JSONException, ParseException{
		Button addReminderBtn = (Button) v.findViewById(R.id.reminder_btn);
		addReminderBtn.setVisibility(View.GONE);
		
		String dateString = reminderLong > 0 ? GlobalState.getDateFormat().format(new Date(reminderLong)):GlobalState.getDateFormat().toPattern();
		Button reminderDateBtn = (Button) v.findViewById(R.id.reminder_date_btn);
		reminderDateBtn.setVisibility(View.VISIBLE);	
		reminderDateBtn.setText(dateString);
		
		String timeString = reminderLong > 0 ? GlobalState.getTimeFormat().format(new Date(reminderLong)):GlobalState.getTimeFormat().toPattern();
		Button reminderTimeBtn = (Button) v.findViewById(R.id.reminder_time_btn);
		reminderTimeBtn.setVisibility(View.VISIBLE);	
		reminderTimeBtn.setText(timeString);
		
		ImageView removeBtn = (ImageView) v.findViewById(R.id.remove_reminder_btn);
		removeBtn.setVisibility(View.VISIBLE);
		removeBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				removeReminder(v);
			}
		});
		
		if(reminderLong > 0){
			JSONObject reminderObject = new JSONObject();
			reminderObject.put(REMINDER_TIME, reminderLong)
			.put(REMINDER_ID, Calendar.getInstance().getTimeInMillis());
			choreObject.put(REMINDER, reminderObject);
			ApiService.putChore(mCtx, choreObject,false);
			ReminderService.putReminder(mCtx, choreObject);
			Toast.makeText(getContext(), "Scheduled a reminder for "+dateString+" "+timeString, Toast.LENGTH_SHORT).show();
		}
	}
	
	private void removeReminder(View v){
		Button addReminderBtn = (Button) ((View)v.getParent()).findViewById(R.id.reminder_btn);
		addReminderBtn.setVisibility(View.VISIBLE);
		
		Button reminderDateBtn = (Button) ((View)v.getParent()).findViewById(R.id.reminder_date_btn);
		reminderDateBtn.setVisibility(View.GONE);	
		
		Button reminderTimeBtn = (Button) ((View)v.getParent()).findViewById(R.id.reminder_time_btn);
		reminderTimeBtn.setVisibility(View.GONE);	
		
		v.setVisibility(View.GONE);
	
		try {
			ReminderService.cancelReminder(mCtx, choreObject);
			choreObject.remove(REMINDER);
			ApiService.putChore(mCtx, choreObject,false);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	@Override 
	public int hashCode() {
		return getString(ID).hashCode();
	}
	
	public boolean equals(Object obj) {
	        if (obj == null)
	            return false;
	        if (obj == this)
	            return true;
	        if (!(obj instanceof ChoreItem))
	            return false;
	       
	        ChoreItem c = (ChoreItem) obj;
	        if(c.getString(ID).equals(this.getString(ID)))
	        	return true;
	        else
	        	return false;
	 }

	public void putString(String key, String value, boolean notify) throws JSONException{
			choreObject.put(key, value);
			choreObject.put(DELIVERY, CHORE_DELIVERY_PENDING);
			ApiService.putChore(mCtx, choreObject,notify);
	}
	
	public void putInt(String key, int value, boolean notify) throws JSONException{
		choreObject.put(key, value);
		choreObject.put(DELIVERY, CHORE_DELIVERY_PENDING);
		ApiService.putChore(mCtx, choreObject,notify);
	}
	
	public void remove(){
		try {
			ApiService.removeChore(mCtx, choreObject);
			if(choreObject.getString(ORIGIN).equals(ApiService.getOwnId(mCtx)) && choreObject.getInt(STATUS) != CHORE_STATUS_APPROVED){
				int karma = choreObject.getInt(KARMA);
				ApiService.pledgeKarma(mCtx, -karma);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public void removeReminder(){
		choreObject.remove(REMINDER);
	}
	
	public JSONObject toJSON(){
		return choreObject;
	}
	
	public boolean show(){
		if(choreObject.has(SHOW)){
			try {
				return choreObject.getBoolean(SHOW);
			} catch (JSONException e) {
				e.printStackTrace();
				return false;
			}
		}else{
			return true;	
		}
	}
	
	@Override
	public String toString(){
		return choreObject.toString();
	}

	@Override
	Context getContext() {
		return mCtx;
	}

	@Override
	int getChoreStatus() {
		try {
			return choreObject.getInt(STATUS);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return -1;
	}

	@Override
	public void markStatus(View v) {
		int currentStatus = getChoreStatus();
		
		if(currentStatus < ChoreItem.CHORE_STATUS_COMPLETE){
			try {
				this.putInt(ChoreItem.STATUS, ChoreItem.CHORE_STATUS_COMPLETE,true);
				v.setBackgroundResource(R.drawable.chore_background_complete);
				notifyOnRevertEnd();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public int getItemViewType(int position) {
		return ChoreListAdapter.RowTypes.CHORE_ITEM.ordinal();
	}

}
