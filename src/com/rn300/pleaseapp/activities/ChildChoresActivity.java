package com.rn300.pleaseapp.activities;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ListFragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.rn300.pleaseapp.ApiService;
import com.rn300.pleaseapp.R;
import com.rn300.pleaseapp.ServerMessagingService;
import com.rn300.pleaseapp.lists.ListItemInterface;
import com.rn300.pleaseapp.lists.children.ChildItem;
import com.rn300.pleaseapp.lists.chores.adapters.ChildChoresAdapter;
import com.rn300.pleaseapp.lists.chores.adapters.ChoreListAdapter.RemoveItemListener;
import com.rn300.pleaseapp.lists.chores.items.ChildTitleItem;
import com.rn300.pleaseapp.lists.chores.items.choreitem.ChildChoreItem;
import com.rn300.pleaseapp.lists.chores.items.choreitem.ChoreItem;

public class ChildChoresActivity extends ListFragment  implements RemoveItemListener{
	@SuppressWarnings("unused")
	private static final String TAG = "ChildChoresActivity";
	private PopupWindow mUndoPopup;
	private boolean showPopup = true;
	private TextView mUndoText;
	private HashMap<Integer,ListItemInterface> itemsToRemove = new HashMap<Integer,ListItemInterface>();
	private float mDensity;
	
	private static ChildChoresAdapter mAdapter;
	private static ChildItem child;
	
	private int mAutoHideDelay = 5000;
	private int mDelayedMsgId = 0;
	private HideUndoPopupHandler mHandler; 
	
	private IntentFilter choreStatusfilter = new IntentFilter(ApiService.CHORE_CHANGED);
	
	public BroadcastReceiver choreStatusReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			if(action.equals(ApiService.CHORE_CHANGED) || action.equals(ServerMessagingService.PROCESSED_CHORE)){
				String id = child.getString(ChildItem.ID);
				String choreString = intent.getStringExtra(ApiService.CHORE_STRING);
				ChildChoreItem choreItem;
				try {
					choreItem = new ChildChoreItem (getActivity(),new JSONObject(choreString));
					if(choreItem.getString(ChoreItem.DESTINATION).equals(id)){
						if(action.equals(ServerMessagingService.PROCESSED_CHORE)){
							abortBroadcast();
							setResultCode(Activity.RESULT_CANCELED);
						}
					
						if(mAdapter.getPosition(choreItem) != -1){
							final int index = mAdapter.getPosition(choreItem);
							if(choreItem.show()){
								mAdapter.set(index, choreItem);
							}else{
								int vPos = index - getListView().getFirstVisiblePosition();
								if(vPos >= 0){
									View v = getListView().getChildAt(index);
									int mAnimationTime = getActivity().getResources().getInteger(android.R.integer.config_shortAnimTime);
									showPopup = false;
									mAdapter.dismiss(v, v.getWidth(), index, mAnimationTime);
								}else{
									mAdapter.remove(mAdapter.getItem(index));
								}
							}
						}else if(choreItem.show()){
							mAdapter.add(choreItem);
							mAdapter.setHint(R.layout.chore_hint_swipe);
						}
						
					}
						
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
	};
					
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
     	
    	RelativeLayout mainView = (RelativeLayout) inflater.inflate(R.layout.activity_child_chores, container, false);
        return mainView;
    }

    public void onActivityCreated(Bundle savedInstanceState){
    	super.onActivityCreated(savedInstanceState);
     
        try {
        	ArrayList<ChildChoreItem> mChores = getChores();
			
			mAdapter = new ChildChoresAdapter(getActivity(), mChores,getListView());
        
        	if(mChores.size() > 0){
				mAdapter.setHint(R.layout.chore_hint_swipe);
			}else{
				mAdapter.setHint(R.layout.chore_hint_no_chores_child);
			}
        
        	mAdapter.insert(new ChildTitleItem(getActivity(),child.getString(ChildItem.NAME),child.getProfilePic(),new ShowChoreForm()), 0);
        	
        	setListAdapter(mAdapter);    
        	
        } catch (JSONException e1) {
			e1.printStackTrace();
		} 
        
        LoadUndoPopup();
        mAdapter.setRemoveListener(this);
    }
      
    @Override
	public void onResume(){
    	super.onResume();
       	choreStatusfilter.addAction(ServerMessagingService.PROCESSED_CHORE);
       	choreStatusfilter.setPriority(10);
    	getActivity().registerReceiver(choreStatusReceiver, choreStatusfilter);
    }
  
    private ArrayList<ChildChoreItem> getChores () throws JSONException{
    	ArrayList<ChildChoreItem> choreList = new ArrayList<ChildChoreItem>();
    	
    	String id = child.getString(ChildItem.ID);
    	JSONArray choresArray = new JSONArray(ApiService.retrieveChores(getActivity(), id));
    	
    	for(int i = 0; i < choresArray.length(); i++ ){
    		ChildChoreItem chore = new ChildChoreItem(getActivity(),choresArray.getJSONObject(i));
			choreList.add(chore);
    	}
  
    	return choreList;
    }
    
    private class ShowChoreForm implements OnClickListener{

		@Override
		public void onClick(View v) {
			((TabsActivity) getActivity()).startChoreFormActivity(ChildChoresActivity.this,child);
		}
    	
    }
    
    @Override
    public void onPause (){
    	String ticker = ChildItem.DEFAULT_TICKER;
   
    	int set = 0;
		int complete = 0;
    	int itemCount = mAdapter.getCount();
    	for(int i  = 0; i < itemCount; i++ ){
    		ListItemInterface item = mAdapter.getItem(i);
    		if(item instanceof ChoreItem){
				ChoreItem chore = (ChoreItem) item;
    			if(chore.getInt(ChoreItem.STATUS) == ChoreItem.CHORE_STATUS_COMPLETE){
					complete++;
				} else if(chore.getInt(ChoreItem.STATUS) == ChoreItem.CHORE_STATUS_PENDING){
					set++;
				}
    		}
    	}
    	
    	if(set == 0 && complete == 0) ticker = "No tasks set";
    	else if(set == 0 && complete == 1) ticker = String.valueOf(complete) + " task to approve";
    	else if(set == 0 && complete > 1) ticker = String.valueOf(complete) + " tasks to approve";
    	else if(set == 1 && complete == 0) ticker = String.valueOf(set) + " task set";
    	else if(set > 1 && complete == 0) ticker = String.valueOf(set) + " tasks set";
    	else if(set == 1 && complete == 1) ticker = String.valueOf(set) + " task set, and " + String.valueOf(complete) + " task to approve";
    	else if(set > 1 && complete > 1) ticker = String.valueOf(set) + " tasks set, and " + String.valueOf(complete) + " tasks to approve";
    		
    	child.setTicker(getActivity(), ticker);
    	getActivity().unregisterReceiver(choreStatusReceiver);
    	
    	discardItemToRemove();
    	
    	super.onPause();
    }

    private static class HideUndoPopupHandler extends Handler {
    	private final WeakReference<ChildChoresActivity> mActivity;
    	
    	public HideUndoPopupHandler(ChildChoresActivity fragment){
    		mActivity = new WeakReference<ChildChoresActivity>(fragment);
    	}
    	
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
		    
			ChildChoresActivity activity = mActivity.get();
			if(activity != null && msg.what == activity.mDelayedMsgId){
				activity.discardItemToRemove();
			}
		}
		
	}
	 
    public void discardItemToRemove(){
    	if(mUndoPopup != null) mUndoPopup.dismiss();
    	for(ListItemInterface itemToRemove : itemsToRemove.values()){
			itemToRemove.remove();
		}
		
		itemsToRemove.clear();
    }
    
    private void LoadUndoPopup(){
    	mHandler = new HideUndoPopupHandler(this);
    	mDensity = getListView().getResources().getDisplayMetrics().density;
		LayoutInflater inflater = (LayoutInflater) getListView().getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.undo_popup, null);
		Button mUndoButton = (Button)v.findViewById(R.id.undo);
		mUndoButton.setOnClickListener(new UndoHandler());
		mUndoText = (TextView)v.findViewById(R.id.text);
		
		mUndoPopup = new PopupWindow(v);
		mUndoPopup.setAnimationStyle(R.style.fade_animation);
		
        int xdensity = (int)(getListView().getContext().getResources().getDisplayMetrics().widthPixels / mDensity);
        if(xdensity < 300) {
    		mUndoPopup.setWidth((int)(mDensity * 280));
        } else if(xdensity < 350) {
            mUndoPopup.setWidth((int)(mDensity * 300));
        } else if(xdensity < 500) {
            mUndoPopup.setWidth((int)(mDensity * 330));
        } else {
            mUndoPopup.setWidth((int)(mDensity * 450));
        }
		mUndoPopup.setHeight((int)(mDensity * 56));
		
		Typeface font = Typeface.createFromAsset(getActivity().getAssets(),"fonts/HelveticaNeue.ttf");
		mUndoText.setTypeface(font);
		mUndoText.setPaintFlags(mUndoText.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
    }
    
    private class UndoHandler implements View.OnClickListener {

		public void onClick(View v) {
			for(Map.Entry<Integer,ListItemInterface> pair : itemsToRemove.entrySet()){
				mAdapter.insert(pair.getValue(), pair.getKey());
			}
				
			itemsToRemove.clear();
			mUndoPopup.dismiss();
		}
	}

	@Override
	public void onDismissRows(HashMap<Integer,ListItemInterface> descendingOrderedItems) {
		for(ListItemInterface itemToRemove : itemsToRemove.values()){
			itemToRemove.remove();
		}
		
		itemsToRemove.clear();
		
		itemsToRemove = descendingOrderedItems;
		ChoreItem choreToRemove = null;
		for(ListItemInterface newItemToRemove: itemsToRemove.values()){
			if(newItemToRemove instanceof ChoreItem){
				choreToRemove = (ChoreItem) newItemToRemove;
			}
		}
		
		if(showPopup){
			mUndoPopup.showAtLocation(getListView(), 
					Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM,
					0, (int)(mDensity * 15));
			
			String undoMsg = "Removed task";
			if(choreToRemove != null) undoMsg = String.format(getResources().getString(R.string.undo), choreToRemove.getString(ChoreItem.TITLE));
			mUndoText.setText(undoMsg);
			mDelayedMsgId++;
			mHandler.sendMessageDelayed(mHandler.obtainMessage(mDelayedMsgId), 
					mAutoHideDelay);
		}else{
			discardItemToRemove();
			showPopup = true;
		}
	}
}
