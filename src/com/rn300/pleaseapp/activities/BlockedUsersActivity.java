package com.rn300.pleaseapp.activities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.ListActivity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.rn300.pleaseapp.ApiService;
import com.rn300.pleaseapp.R;
import com.rn300.pleaseapp.lists.ListItemInterface;
import com.rn300.pleaseapp.lists.children.ChildItem;
import com.rn300.pleaseapp.lists.children.ChildListAdapter;

public class BlockedUsersActivity extends ListActivity {
	@SuppressWarnings("unused")
	private static final String TAG = "PeopleActivity";
	
	private ChildListAdapter mAdapter;
	private ArrayList<ListItemInterface> mChildren;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_blocked_people);
	    
	    mChildren = new ArrayList<ListItemInterface>();
    	mAdapter = new ChildListAdapter(this,mChildren);
        setListAdapter(mAdapter);
        TextView blockedTitle = (TextView)findViewById(R.id.blocked_title);
		final Typeface font = Typeface.createFromAsset(this.getAssets(),"fonts/HelveticaNeue.ttf");
		blockedTitle.setTypeface(font);
		
        try {
        	getBlockedChildren();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
    private void getBlockedChildren () throws JSONException{
    	JSONArray childArray = new JSONArray(ApiService.retrieveChildren(this));
    	
    	for(int i = 0; i < childArray.length(); i++ ){
			ChildItem item = new ChildItem(this,childArray.getJSONObject(i));
			// No duplicates or blocked users
			if(!mChildren.contains(item) && item.isBlocked()){
				mChildren.add(item);
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
    }
    
    @Override
  	public void onListItemClick(ListView listView, View view, int position, long id) {
    	ListItemInterface item = mAdapter.getItem(position);
	    if(item instanceof ChildItem){
    		ChildItem child = (ChildItem) item;
	    	child.block(false);
			mAdapter.remove(child);
			mAdapter.notifyDataSetChanged();
	    }
    } 
  
    
}
