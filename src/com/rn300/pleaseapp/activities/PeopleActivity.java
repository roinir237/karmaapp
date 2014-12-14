package com.rn300.pleaseapp.activities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.rn300.pleaseapp.ApiService;
import com.rn300.pleaseapp.R;
import com.rn300.pleaseapp.lists.ListItemInterface;
import com.rn300.pleaseapp.lists.children.AddChildItem;
import com.rn300.pleaseapp.lists.children.ChildItem;
import com.rn300.pleaseapp.lists.children.ChildListAdapter;

public class PeopleActivity extends ListFragment {
	private static final String TAG = "PeopleActivity";
	
	private ChildListAdapter mAdapter;
	private ArrayList<ListItemInterface> mChildren;
	
	/** Called when the activity is first created. */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		if (container == null) {
    		return null;
    	}
	    LinearLayout mainView = (LinearLayout) inflater.inflate(R.layout.activity_people, container, false);
	    
	    return mainView;
	}
	
    public void onActivityCreated(Bundle savedInstanceState){
    	super.onActivityCreated(savedInstanceState);
    	
    	mChildren = new ArrayList<ListItemInterface>();
    	mAdapter = new ChildListAdapter(getActivity(),mChildren);
        setListAdapter(mAdapter);
    	
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				setupOptionsDialogue(position);
				return true;
			}
        });
        
        try {
			if(getChildren() == 0){
				mAdapter.add(new AddChildItem(getActivity()));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
        
    }

    private int getChildren () throws JSONException{
    	int counter = 0;
    	JSONArray childArray = new JSONArray(ApiService.retrieveChildren(getActivity()));
    	
    	for(int i = 0; i < childArray.length(); i++ ){
			ChildItem item = new ChildItem(getActivity(),childArray.getJSONObject(i));
			// No duplicates or blocked users
			if(!mChildren.contains(item) && !item.isBlocked() && !item.isHidden()){
				mChildren.add(item);
				counter++;
			}
    	}
    	
    	Collections.sort(mChildren, new Comparator<ListItemInterface>(){
			@Override
			public int compare(ListItemInterface lhs, ListItemInterface rhs) {
				if(lhs instanceof ChildItem && rhs instanceof ChildItem){
					ChildItem lchild = (ChildItem) lhs;
					ChildItem rchild = (ChildItem) rhs;
					return lchild.getString(ChildItem.NAME).compareToIgnoreCase(rchild.getString(ChildItem.NAME));
				}else return 0;
			}
    	});
    	
    	mAdapter.notifyDataSetChanged();
    	return counter;
    }
    
    @Override
  	public void onListItemClick(ListView listView, View view, int position, long id) {
    	ListItemInterface item = mAdapter.getItem(position);
    	if(item instanceof ChildItem){
    		ChildItem child = (ChildItem) item;
    		((TabsActivity) getActivity()).startChildChoreActivity(this,child);
    	}else if(item instanceof AddChildItem){
    		Intent contactsInt = new Intent(getActivity(),ContactsActivity.class);
        	startActivity(contactsInt);
    	}
    } 
    
    private void setupOptionsDialogue(int position){
    	ListItemInterface item = mAdapter.getItem(position);
    	if(item instanceof ChildItem){
	    	final ChildItem child = (ChildItem) item;
	    	AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
	    	ab.setTitle(R.string.child_options_title);
	    	ab.setItems(R.array.child_options, new DialogInterface.OnClickListener() {
		    	public void onClick(DialogInterface d, int choice) {
			    	switch(choice){
			    	case(0):  // Set Task
			    		break;
			    	case(1):  // Remove
			    		try {
							ApiService.removeChildAndChores(getActivity(),child.getString(ChildItem.ID));
							mAdapter.remove(child);
							if(mAdapter.getCount() == 0){
								mAdapter.add(new AddChildItem(getActivity()));
							}
							mAdapter.notifyDataSetChanged();
						} catch (JSONException e) {
							e.printStackTrace();
						}
			    		break;
			    	case(2):  // Block
			    		Log.d(TAG, "block "+child.getString(ChildItem.NAME));
		    			child.block(true);
		    			mAdapter.remove(child);
		    			mAdapter.notifyDataSetChanged();
			    		break;
			    	}
			    	
			    	d.dismiss();
		    	}
	    	});
	    	ab.show();
    	}
    }
 
}
