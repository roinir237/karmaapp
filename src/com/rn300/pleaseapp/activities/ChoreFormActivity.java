package com.rn300.pleaseapp.activities;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.rn300.pleaseapp.ApiService;
import com.rn300.pleaseapp.GlobalState;
import com.rn300.pleaseapp.R;
import com.rn300.pleaseapp.lists.children.ChildItem;
import com.rn300.pleaseapp.lists.chores.items.choreitem.ChoreItem;

@SuppressLint({ "ValidFragment", "SimpleDateFormat" })
public class ChoreFormActivity extends Fragment{
	@SuppressWarnings("unused")
	private static final String TAG = "choreFormActivity";
	
	private ChildItem child;
	
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	if (container == null) {
    		return null;
    	}
    	
    	Bundle args = getArguments();
    	try {
			child = new ChildItem(getActivity(),args.getString(ApiService.CHILD_STRING));
		} catch (JSONException e) {
			e.printStackTrace();
		}
        
    	LinearLayout mainView = (LinearLayout) inflater.inflate(R.layout.chore_form, container, false);
    	
    	((Button)mainView.findViewById(R.id.thanksBtn)).setOnClickListener(new submitChore());
    	((Button)mainView.findViewById(R.id.cancelBtn)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				goBack();
			}
    	});

    	EditText timeEdt = (EditText) mainView.findViewById(R.id.chore_time_edit);
    	timeEdt.setInputType(EditorInfo.TYPE_NULL);
    	timeEdt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
    		
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(hasFocus){
					DialogFragment newFragment = new TimePickerFragment(v);
			        newFragment.show(getActivity().getSupportFragmentManager(), "timePicker");
				}
				
			}
		});
    	
    	EditText dateEdt = (EditText) mainView.findViewById(R.id.chore_date_edit);
    	dateEdt.setInputType(EditorInfo.TYPE_NULL);
    	dateEdt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
    		
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(hasFocus){
					DialogFragment newFragment = new DatePickerFragment(v);
			        newFragment.show(getActivity().getSupportFragmentManager(), "datePicker");
				}
				
			}
		});
    	
    	String karmaHint = getActivity().getResources().getString(R.string.chore_karma_hint);
    	int availableKarma = ApiService.getTotalKarma(getActivity())-ApiService.getPledgedKarma(getActivity());
    	((TextView) mainView.findViewById(R.id.chore_karma_edit)).setHint(String.format(karmaHint, availableKarma));
    	
        return mainView;
    }

    private class submitChore implements OnClickListener{
		@Override
		public void onClick(View v) {
			View mainView = getView();
	    	
	    	String origin = ApiService.getOwnId(getActivity());
	    	String destination = child.getString(ChildItem.ID);
	    	EditText nameEdt = (EditText) mainView.findViewById(R.id.chore_name_edit);
			EditText timeEdt = (EditText) mainView.findViewById(R.id.chore_time_edit);
			EditText dateEdt = (EditText) mainView.findViewById(R.id.chore_date_edit);
			EditText detailsEdt = (EditText) mainView.findViewById(R.id.chore_details_edit);
			EditText karmaEdt = (EditText) mainView.findViewById(R.id.chore_karma_edit);
			
			String title = nameEdt.getText().toString().trim();
			boolean dateTime = (timeEdt.getText().toString().equals("") && dateEdt.getText().toString().equals("")) | 
					(!timeEdt.getText().toString().equals("") && !dateEdt.getText().toString().equals(""));
			int karma = 0;
			try{
				if(!karmaEdt.getText().toString().equals(""))
					karma = Integer.parseInt(karmaEdt.getText().toString());
			}catch(NumberFormatException e){
					karma = -1;
			}

			int availableKarma = ApiService.getTotalKarma(getActivity())-ApiService.getPledgedKarma(getActivity());
			
			boolean karmaInRange = karma >= 0 && karma <= availableKarma; 
			
			if(!title.equals("") && dateTime && karmaInRange){
				JSONObject choreObject = new JSONObject();
				try {
					choreObject.put(ChoreItem.TITLE, nameEdt.getText().toString());
					String whenString = dateEdt.getText().toString() + " " + timeEdt.getText().toString();
					SimpleDateFormat formatter = new SimpleDateFormat(GlobalState.getDateFormat().toPattern()+" "+GlobalState.getTimeFormat().toPattern());
					formatter.setTimeZone(Calendar.getInstance().getTimeZone());
					try{
						choreObject.put(ChoreItem.TIME,formatter.parse(whenString).getTime());
					}catch (ParseException e1) {}
					choreObject.put(ChoreItem.DETAILS, detailsEdt.getText().toString());
					if(karma > 0) ApiService.pledgeKarma(getActivity(), karma); 
					choreObject.put(ChoreItem.KARMA, karma < 0 ? 0:karma);
					choreObject.put(ChoreItem.DESTINATION, destination);
					choreObject.put(ChoreItem.ORIGIN,origin);
					choreObject.put(ChoreItem.STATUS, ChoreItem.CHORE_STATUS_PENDING);
					choreObject.put(ChoreItem.DELIVERY, ChoreItem.CHORE_DELIVERY_PENDING);
					ApiService.putChore(getActivity(), choreObject,true);
					ApiService.updateChildren(getActivity(), child.toJSON());
			    	goBack();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			} else if(title.equals("")) {
				Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.chore_needs_title_error), Toast.LENGTH_SHORT).show();
			} else if(!dateTime) {
				Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.chore_requires_date_time_error), Toast.LENGTH_SHORT).show();
			}else if(!karmaInRange){
				String msg = String.format(getActivity().getResources().getString(R.string.chore_karma_error), availableKarma);
				Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
			}
		}	
	}
    
    public void goBack(){
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
    	getFragmentManager().popBackStackImmediate();
    }
   
    private class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {
    	private final View srcView;
    	public TimePickerFragment(View v){
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
			if(srcView instanceof TextView) {
				((TextView)srcView).setText(hour+":"+min+" "+a);
			}
		} else {
			hour = hourOfDay < 10 ? "0"+Integer.toString(hourOfDay):Integer.toString(hourOfDay);
			if(srcView instanceof TextView) {
				((TextView)srcView).setText(hour+":"+min);
			}
		}
	}
}
    
    private class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
	private final View srcView;
	public DatePickerFragment(View v){
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
		if(srcView instanceof TextView) ((TextView)srcView).setText(dateString);
	}
}

}
