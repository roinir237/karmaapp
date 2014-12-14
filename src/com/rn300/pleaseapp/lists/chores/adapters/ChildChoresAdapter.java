package com.rn300.pleaseapp.lists.chores.adapters;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.joda.time.LocalDate;
import org.json.JSONException;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.ListView;

import com.rn300.pleaseapp.R;
import com.rn300.pleaseapp.lists.ListItemInterface;
import com.rn300.pleaseapp.lists.chores.items.ChoreWhen;
import com.rn300.pleaseapp.lists.chores.items.choreitem.ChildChoreItem;
import com.rn300.pleaseapp.lists.chores.items.choreitem.ChoreItem;

@SuppressLint("ViewConstructor")
public class ChildChoresAdapter extends ChoreListAdapter{

	public ChildChoresAdapter(Context context, List<ChildChoreItem> objects, ListView listView) {
		super(context, R.layout.child_chore_row_view,listView);
		setNotifyOnChange(false);
		
		// Order in the correct way according to date
		final LocalDate today = new LocalDate();
		Collections.sort(objects,new Comparator<ChoreItem>(){
				@Override
				public int compare(ChoreItem lhs, ChoreItem rhs) {
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
				}
		});
		
		// Now add when sections 
		for(int i = 0; i < objects.size(); i++){
			ChoreItem chore = objects.get(i);
			ChoreWhen when = new ChoreWhen(getContext(), R.layout.chore_child_when, chore.getString(ChoreItem.ORIGIN), chore.getTime());
		
			if(this.getPosition(when) == -1){
				super.add(when);
			}

			super.add((ListItemInterface)chore);
			
		}
		
		this.notifyDataSetChanged();
	}
	
	/**
	 * Add the chore to the correct place in the list
	 */
	@Override
	public void add(ChoreItem chore) {
		setNotifyOnChange(false);
		ChoreWhen when = new ChoreWhen(getContext(), R.layout.chore_child_when, chore.getString(ChoreItem.ORIGIN), chore.getTime());
			
		boolean added = false;
		
		for(int i = 0; !added && i < this.getCount(); i++){
			ListItemInterface item = this.getItem(i);
			if(item instanceof ChoreItem){
				ChoreItem currentChore = (ChoreItem) item;
				if(currentChore.getTime() > chore.getTime() || currentChore.getTime() == -1){
					ChoreWhen currentWhen = new ChoreWhen(getContext(), R.layout.chore_child_when, chore.getString(ChoreItem.ORIGIN), currentChore.getTime());
	
					super.insert(chore, i);
					
					if(!currentWhen.equals(when) && i > 0 && this.getItem(i-1) instanceof ChoreItem){
						ChoreItem previousChore = (ChoreItem) this.getItem(i-1);
						ChoreWhen previousWhen = new ChoreWhen(getContext(), R.layout.chore_child_when, chore.getString(ChoreItem.ORIGIN), previousChore.getTime());
						if(!previousWhen.equals(when)){
							super.insert(when, i);
						}
					}
					
					added = true;
				}
			}
		}
		
		if(!added){
			super.add((ListItemInterface)chore);
		}
		
		notifyDataSetChanged();
	}

	@Override
	protected void setupOptionsDialogue(final int position){
    	final ChildChoreItem chore = (ChildChoreItem) this.getItem(position);
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
						ChildChoresAdapter.this.dismiss(v, v.getWidth(), position, mAnimationTime);
					}
		    		break;
		    	case(2): // Resend (when not received) 
		    		try {
		    			chore.putInt(ChoreItem.DELIVERY, ChoreItem.CHORE_DELIVERY_PENDING, true);
		    			ChildChoresAdapter.this.notifyDataSetChanged();
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
}
