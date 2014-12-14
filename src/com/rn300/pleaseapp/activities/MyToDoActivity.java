package com.rn300.pleaseapp.activities;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;

import android.annotation.SuppressLint;
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
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.rn300.pleaseapp.ApiService;
import com.rn300.pleaseapp.R;
import com.rn300.pleaseapp.ServerMessagingService;
import com.rn300.pleaseapp.lists.ListItemInterface;
import com.rn300.pleaseapp.lists.chores.adapters.ChoreListAdapter.RemoveItemListener;
import com.rn300.pleaseapp.lists.chores.adapters.MyChoresAdapter;
import com.rn300.pleaseapp.lists.chores.items.TitleItem;
import com.rn300.pleaseapp.lists.chores.items.choreitem.ChoreItem;
 
@SuppressLint("ValidFragment")
public class MyToDoActivity extends ListFragment implements RemoveItemListener{
	@SuppressWarnings("unused")
	private static final String TAG = "MyToDoActivity";
	private PopupWindow mUndoPopup;
	private boolean showPopup = true;
	private TextView mUndoText;
	private HashMap<Integer,ListItemInterface> itemsToRemove = new HashMap<Integer,ListItemInterface>();
	private float mDensity;
	
	MyChoresAdapter mAdapter;
	private ArrayList<ChoreItem> mChores;
	private int mAutoHideDelay = 5000;
	private int mDelayedMsgId = 0;
	private HideUndoPopupHandler mHandler; 
			
	private IntentFilter choreStatusfilter = new IntentFilter(ApiService.CHORE_CHANGED);
	
	public BroadcastReceiver choreStatusReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if(action.equals(ApiService.CHORE_CHANGED) || action.equals(ServerMessagingService.PROCESSED_CHORE)){
				String id = ApiService.getOwnId(getActivity());
				String choreString = intent.getStringExtra(ApiService.CHORE_STRING);
				ChoreItem choreItem;
				try {
					choreItem = new ChoreItem (getActivity(),choreString);
					if(choreItem.getString(ChoreItem.DESTINATION).equals(id)){
						if(action.equals(ServerMessagingService.PROCESSED_CHORE)){
							abortBroadcast();
							setResultCode(Activity.RESULT_CANCELED);
						}
					
						if(mAdapter.getPosition(choreItem) != -1){
							final int index = mAdapter.getPosition(choreItem);
							if(choreItem.show()){
								mAdapter.set(index, choreItem);
								mChores.set(mChores.indexOf(choreItem), choreItem);
							}else{
								View v = getListView().getChildAt(index);
								int mAnimationTime = getActivity().getResources().getInteger(android.R.integer.config_shortAnimTime);
								showPopup = false;
								mAdapter.dismiss(v, v.getWidth(), index, mAnimationTime);
							}
						}else if(choreItem.show()){
							choreItem.highlight();
							mAdapter.add(choreItem);
							mChores.add(choreItem);
							mAdapter.setHint(R.layout.chore_hint_reminder);
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
 
    	RelativeLayout mainView = (RelativeLayout) inflater.inflate(R.layout.activity_main, container, false);
        return mainView;
    }

    public void onActivityCreated(Bundle savedInstanceState){
    	super.onActivityCreated(savedInstanceState);
    		      
        try {
			mChores = getChores();
		} catch (JSONException e) {
			e.printStackTrace();
		}
  
        mAdapter = new MyChoresAdapter(getActivity(), mChores, getListView());
       
        mAdapter.insert(new TitleItem(getActivity()),0);
        

		if(mChores.size() > 0 ){
			mAdapter.setHint(R.layout.chore_hint_reminder);
		}else{
			mAdapter.setHint(R.layout.chore_hint_no_chores);
		}
    
        setListAdapter(mAdapter);
        
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
    
    private ArrayList<ChoreItem> getChores () throws JSONException{
    	ArrayList<ChoreItem> choreList = new ArrayList<ChoreItem>();
    	
    	String id = ApiService.getOwnId(getActivity());
    	JSONArray choresArray = new JSONArray(ApiService.retrieveChores(getActivity(), id));
    	
    	for(int i = 0; i < choresArray.length(); i++ ){
			ChoreItem chore = new ChoreItem(getActivity(),choresArray.getJSONObject(i));
			choreList.add(chore);
    	}
  
    	return choreList;
    }
     
    @Override
    public void onPause (){
    	super.onPause();
    	getActivity().unregisterReceiver(choreStatusReceiver);
    	discardItemToRemove();
    }
    
    private static class HideUndoPopupHandler extends Handler {
    	private final WeakReference<MyToDoActivity> mActivity;
    	
    	public HideUndoPopupHandler(MyToDoActivity fragment){
    		mActivity = new WeakReference<MyToDoActivity>(fragment);
    	}
    	
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
		    
			MyToDoActivity activity = mActivity.get();
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
