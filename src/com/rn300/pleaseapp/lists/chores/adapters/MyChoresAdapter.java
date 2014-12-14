package com.rn300.pleaseapp.lists.chores.adapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;

import org.joda.time.LocalDate;
import org.json.JSONException;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.rn300.pleaseapp.ApiService;
import com.rn300.pleaseapp.R;
import com.rn300.pleaseapp.lists.ListItemInterface;
import com.rn300.pleaseapp.lists.chores.items.ChoreHint;
import com.rn300.pleaseapp.lists.chores.items.ChoreSection;
import com.rn300.pleaseapp.lists.chores.items.ChoreWhen;
import com.rn300.pleaseapp.lists.chores.items.choreitem.ChoreItem;

@SuppressLint("ViewConstructor")
public class MyChoresAdapter extends ChoreListAdapter{
	private static final String TAG = "MyChoresAdapter";
	
	private HashMap <ListItemInterface, Integer> locationMap = new HashMap<ListItemInterface,Integer>();
	private HashMap <String,Integer> dailyKarmaTally = new HashMap<String,Integer>();
	
	public MyChoresAdapter(Context context, List<ChoreItem> objects, ListView listView) {
		super(context, R.layout.chore_row_view, listView);
		setNotifyOnChange(false);
		
		// Order according to date and origin 
		final LocalDate today = new LocalDate();
		Collections.sort(objects, new Comparator<ChoreItem>(){
				@Override
				public int compare(ChoreItem lhs, ChoreItem rhs) {
					if(lhs.getString(ChoreItem.ORIGIN).equals(rhs.getString(ChoreItem.ORIGIN))){
						LocalDate ldate = new LocalDate(lhs.getTime());
						LocalDate rdate = new LocalDate(rhs.getTime());
						
						if(today.equals(rdate) && today.equals(ldate)){
							return Math.round(lhs.getTime() - rhs.getTime());
						}else if(today.equals(rdate)){
							return 1;
						}else if(today.equals(ldate)){
							return -1;
						}
							
						return Math.round(lhs.getTime() - rhs.getTime());
					}else {
						return 0;
					}
				}
		});
		
		int objectsSize = objects.size();
	
		this.setNotifyOnChange(false);
		int currentPosition = 0;
		// Now add when sections and user title sections 
		for(int i = 0; i < objectsSize; i++){
			ChoreItem chore = objects.get(i);
			long choreTime = chore.getTime();
			LocalDate choreDate = choreTime != -1 ? new LocalDate(choreTime) : null;
			ChoreWhen when = new ChoreWhen(getContext(), R.layout.chore_when, chore.getString(ChoreItem.ORIGIN),choreTime);
			ChoreSection sect = new ChoreSection(getContext(), chore.getString(ChoreItem.ORIGIN));
			try {
				sect.setName(ApiService.getChildProp(getContext(), chore.getString(ChoreItem.ORIGIN), "name"));
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			if((choreTime == -1 || choreDate.isEqual(today)) && 
					chore.getInt(ChoreItem.STATUS) == ChoreItem.CHORE_STATUS_PENDING){
				updateKarmaTally(chore.getString(ChoreItem.ORIGIN), chore.getInt(ChoreItem.KARMA));
			}else{
				updateKarmaTally(chore.getString(ChoreItem.ORIGIN), -1);
			}
			
			if(this.getPosition(sect) == -1){
				super.add(sect);
				locationMap.put(sect, currentPosition++);
				super.add(when);
				locationMap.put(when, currentPosition++);
				super.add((ListItemInterface)chore);
				locationMap.put(chore, currentPosition++);
			}else if(this.getPosition(when) == -1){
				super.add(when);
				locationMap.put(when, currentPosition++);
				super.add((ListItemInterface)chore);
				locationMap.put(chore, currentPosition++);
			}else{
				super.add((ListItemInterface)chore);
				locationMap.put(chore, currentPosition++);
			}
			
		}
		
		orderAccordingToTally();
		this.notifyDataSetChanged();
	
	}

	@Override
	protected void setupOptionsDialogue(final int position){
    	final ChoreItem chore = (ChoreItem) this.getItem(position);
    	AlertDialog.Builder ab = new AlertDialog.Builder(getContext());
    	ab.setTitle(R.string.chore_options_title);
    	int resid = chore.getInt(ChoreItem.DELIVERY) == ChoreItem.CHORE_DELIVERY_RECEIVED ?
    			R.array.child_chore_options:R.array.child_chore_options_not_received; 
    	ab.setItems(resid, new DialogInterface.OnClickListener() {
	    	public void onClick(DialogInterface d, int choice) {
	    		int vPos;
		    	switch(choice){
		    	case(0): // Done
		    		vPos = position - mListView.getFirstVisiblePosition();
		    		chore.markStatus(mListView.getChildAt(vPos));
		    		break;
		    	case(1): // Remove
		    		vPos = position - mListView.getFirstVisiblePosition();
					if(vPos >= 0){
						View v = mListView.getChildAt(position);
						int mAnimationTime = mListView.getResources().getInteger(android.R.integer.config_shortAnimTime);
						MyChoresAdapter.this.dismiss(v, v.getWidth(), position, mAnimationTime);
					}
		    		break;
		    	case(2): // Resend (when not received) 
		    		try {
		    			chore.putInt(ChoreItem.DELIVERY, ChoreItem.CHORE_DELIVERY_PENDING, true);
		    			MyChoresAdapter.this.notifyDataSetChanged();
					} catch (JSONException e) {
						e.printStackTrace();
					}
		    		break;
		    	}
		    	
		    	d.dismiss();
	    	}
    	});
    	ab.show();
    }

	@Override
	public void add(ChoreItem chore) {
		this.setNotifyOnChange(false);
		String choreOrigin = chore.getString(ChoreItem.ORIGIN);
		ChoreWhen when = new ChoreWhen(getContext(), R.layout.chore_when, choreOrigin, chore.getTime());
		ChoreSection sect = new ChoreSection(getContext(), choreOrigin);
		
		// the list already contains this section
		if(locationMap.containsKey(sect)){
			int sectionIndex = locationMap.get(sect);
			
			// The list already contains this when 
			if(locationMap.containsKey(when)){
				int whenIndex = locationMap.get(when);
				boolean added = false;
				// iterate through the list of items to find the right location starting with the whenIndex 
				for(int i = whenIndex+1; i < getCount() && !added; i++){
					ListItemInterface item = getItem(i);
					// Add to the list id 
					if((item instanceof ChoreItem && ((ChoreItem)item).getTime()>chore.getTime()) ||
							!(item instanceof ChoreItem)){
						this.insert(chore,i);
						added = true;
					}				
				}
			} 
			// the list doesn't contain this when 
			else {
				if(chore.getTime() != -1){
					LocalDate today = new LocalDate();
					LocalDate tomorrow = today.plusDays(1);
					LocalDate yesterday = today.plusDays(-11);
					LocalDate choreDate = new LocalDate(chore.getTime());
					
					if(choreDate.isEqual(today)){
						// today comes just after the section
						this.insert(chore, sectionIndex+1);
						this.insert(when, sectionIndex+1);
					}else if(choreDate.isEqual(tomorrow)){
						// tomorrow comes after today, whenever , overdue
						ChoreWhen todayWhen = new ChoreWhen(getContext(), R.layout.chore_when, choreOrigin, today.toDate().getTime());
						ChoreWhen overdueWhen = new ChoreWhen(getContext(), R.layout.chore_when, choreOrigin, yesterday.toDate().getTime());
						ChoreWhen whenever = new ChoreWhen(getContext(), R.layout.chore_when, choreOrigin, -1);
						
						int whenBeforeIndex = sectionIndex + 1;
						if(locationMap.containsKey(overdueWhen)){
							whenBeforeIndex = locationMap.get(overdueWhen)+1;
						}else if(locationMap.containsKey(whenever)){
							whenBeforeIndex = locationMap.get(whenever)+1;
						}else if(locationMap.containsKey(todayWhen)){
							whenBeforeIndex = locationMap.get(todayWhen)+1;
						}
						
						boolean added = false;
						
						for(int i = whenBeforeIndex; i < getCount() && !added; i++){
							ListItemInterface item = getItem(i);
							// Add to the list id 
							if(!(item instanceof ChoreItem)){
								this.insert(chore,i);
								this.insert(when, i);
								added = true;
							}				
						}
						
					}else if(choreDate.isAfter(tomorrow)){
						// later comes after today, whenever, overdue, tomorrow
						ChoreWhen todayWhen = new ChoreWhen(getContext(), R.layout.chore_when, choreOrigin, today.toDate().getTime());
						ChoreWhen whenever = new ChoreWhen(getContext(), R.layout.chore_when, choreOrigin, -1);
						ChoreWhen tomorroWhen = new ChoreWhen(getContext(), R.layout.chore_when, choreOrigin, tomorrow.toDate().getTime());
						ChoreWhen overdueWhen = new ChoreWhen(getContext(), R.layout.chore_when, choreOrigin, yesterday.toDate().getTime());
						int whenBeforeIndex = sectionIndex + 1;
						
						if(locationMap.containsKey(tomorroWhen)){
							whenBeforeIndex = locationMap.get(tomorroWhen)+1;
						}else if(locationMap.containsKey(overdueWhen)){
							whenBeforeIndex = locationMap.get(overdueWhen)+1;
						}else if(locationMap.containsKey(whenever)){
							whenBeforeIndex = locationMap.get(whenever)+1;
						}else if(locationMap.containsKey(todayWhen)){
							whenBeforeIndex = locationMap.get(todayWhen)+1;
						}
						
						boolean added = false;
						
						for(int i = whenBeforeIndex; i < getCount() && !added; i++){
							ListItemInterface item = getItem(i);
							// Add to the list id 
							if(!(item instanceof ChoreItem)){
								this.insert(chore,i);
								this.insert(when, i);
								added = true;
							}				
						}
					}else if(choreDate.isBefore(today)){
						// overdue comes after today, whenever
						ChoreWhen todayWhen = new ChoreWhen(getContext(), R.layout.chore_when, choreOrigin, today.toDate().getTime());
						ChoreWhen whenever = new ChoreWhen(getContext(), R.layout.chore_when, choreOrigin, -1);
						
						int whenBeforeIndex = sectionIndex + 1;
						
						if(locationMap.containsKey(whenever)){
							whenBeforeIndex = locationMap.get(whenever)+1;
						}else if(locationMap.containsKey(todayWhen)){
							whenBeforeIndex = locationMap.get(todayWhen)+1;
						}
						
						boolean added = false;
						
						for(int i = whenBeforeIndex; i < getCount() && !added; i++){
							ListItemInterface item = getItem(i);
							// Add to the list id 
							if(!(item instanceof ChoreItem)){
								this.insert(chore,i);
								this.insert(when, i);
								added = true;
							}				
						}
					}else Log.wtf(TAG, "something went badly wrong!!!!!");
				}
				// chore is whenever
				else{
					// whenever comes just after today or after the section
					LocalDate today = new LocalDate();
					ChoreWhen todayWhen = new ChoreWhen(getContext(), R.layout.chore_when, choreOrigin, today.toDate().getTime());
					if(locationMap.containsKey(todayWhen)){
						boolean added = false;
						for(int i = sectionIndex+2; i < getCount() && !added; i++){
							ListItemInterface item = getItem(i);
							// Add to the list id 
							if(!(item instanceof ChoreItem)){
								this.insert(chore,i);
								this.insert(when, i);
								added = true;
							}				
						}
					}else{
						this.insert(chore, sectionIndex+1);
						this.insert(when, sectionIndex+1);
					}
				}	
			}
		} else {
			// Add the new section to the top of the list
			this.insert(chore,1);
			this.insert(when,1);
			this.insert(sect,1);
		}
		this.notifyDataSetChanged();
		//updateLocationMap();
		//orderAccordingToTally(true);
	}

	public boolean orderAccordingToTally(){
		if(dailyKarmaTally.size() == 0){
			return false;
		}
		
		// Get the values and order them in descending order
		List<Entry<String, Integer>> orderedTally = new ArrayList<Entry<String, Integer>>(dailyKarmaTally.entrySet());
		Collections.sort(orderedTally, new Comparator<Entry<String, Integer>>() {
		    public int compare(Entry<String, Integer> e1, Entry<String, Integer> e2) {
		        return e2.getValue().compareTo(e1.getValue());
		    }
		});
		// orderedTally now contains <user Id, total karma today> in descending order according to karma
		
		//Go through the adapter and move up sections as needed
		int j = 0;
		
		for(int i = 0; i < getCount(); i++){
			ListItemInterface item = getItem(i);
			if(item instanceof ChoreSection){
				Entry<String,Integer> entry = orderedTally.get(j);
				j++;
				ChoreSection correctSection = new ChoreSection(null,entry.getKey());
				
				if(!correctSection.equals(item)){
					i = moveUpSection(correctSection, i);
				}
			}
		}
		
		return true;
	}
	
	/**
	 * 
	 * @param section: the header of the section to move
	 * @param newLocation: the location to start the new section at
	 * @return the new end index of the section
	 */
	private int moveUpSection(ChoreSection section, int newLocation){
		ArrayList<ListItemInterface> sectionItems = new ArrayList<ListItemInterface>();
		int startIndex = locationMap.get(section);
		
		ListItemInterface item = getItem(startIndex);
		do{
			sectionItems.add(item);
			remove(item);
			if(startIndex<getCount()) item = getItem(startIndex);
		}while(!(item instanceof ChoreSection) && !(item instanceof ChoreHint) && ( startIndex < getCount()));
		
		ListIterator<ListItemInterface> li = sectionItems.listIterator(sectionItems.size());
		while(li.hasPrevious()) {
			insert(li.previous(),newLocation);
		}
		
		return newLocation + sectionItems.size();
	}
	
	private void updateKarmaTally(String id, int karma){
		int updatedKarma = !dailyKarmaTally.containsKey(id) ? karma: (karma == -1 ? dailyKarmaTally.get(id):dailyKarmaTally.get(id)+karma);
		dailyKarmaTally.put(id, updatedKarma);
	}
		
	private void updateLocationMap(){
		int objCount = getCount();
		for(int i = 0; i < objCount; i++){
			locationMap.put(this.getItem(i), i);
		}
	}

	@Override
	public void notifyDataSetChanged(){
		super.notifyDataSetChanged();
		updateLocationMap();
	}
}
