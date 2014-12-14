package com.rn300.pleaseapp.lists;

import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.ListView;


/**
 * Implemented by list items that need to listen to drag events, namely chore items.
 * 
 * @author roi
 *
 */
public abstract class Draggable{
	private ListView mListView;
	
	public ListView getListView(){
		return mListView;
	}
	
	public void setListView(ListView l){
		mListView = l;
	}
	
	public abstract void onTouchDown(View v);
	public abstract void onCancelDrag(View v, MotionEvent event);
	public abstract void onDrag(View v, float deltaX);
	public abstract void onStopDrag(View v, float deltaX, VelocityTracker velocityTracker, int position);
	public abstract void onStartDrag(View v);
}