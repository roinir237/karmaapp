package com.rn300.pleaseapp.lists.chores.items.choreitem;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;
import android.content.Context;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup.LayoutParams;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.rn300.pleaseapp.R;
import com.rn300.pleaseapp.lists.Draggable;
import com.rn300.pleaseapp.lists.chores.adapters.ChoreListAdapter;

public abstract class ChoreDraggable extends Draggable{
	@SuppressWarnings("unused")
	private static final String TAG = "ChoreDraggable";
	
	private static long mAnimationTime = -1;
	private boolean notifyChange = false;
	int currentColor;
	
	abstract Context getContext();
	abstract int getChoreStatus();
	
	private long downTime;
	
	protected void notifyOnRevertEnd(){
		notifyChange = true;
	}
	
	@Override
	public void onTouchDown(View v) {
		downTime = System.currentTimeMillis();
	}

	@Override
	public void onCancelDrag(View v, MotionEvent event) {
		if(System.currentTimeMillis() - downTime < ViewConfiguration.getLongPressTimeout()){
			if(v.getLayoutParams().height != LayoutParams.WRAP_CONTENT) v.getLayoutParams().height = LayoutParams.WRAP_CONTENT;
			View more = v.findViewById(R.id.chore_more);
			int visibility = more.getVisibility() == View.VISIBLE ? View.GONE:View.VISIBLE; 
			more.setVisibility(visibility);
		}
	}
	
	@Override
	public void onDrag(View v, float deltaX) {

	}
	 
	@Override
	public void onStopDrag(View v, float deltaX, VelocityTracker velocityTracker,int position) {
		int mViewWidth = v.getWidth();
		float dis = deltaX/mViewWidth;
		if(dis > 0.5){
			markStatus(v);
			revertPosition(v);
		}else if (toDismiss(mViewWidth, deltaX, velocityTracker)) {  
			ChoreListAdapter adapter = (ChoreListAdapter) getListView().getAdapter();
            adapter.dismiss(v,mViewWidth,position,mAnimationTime);
        } else {
        	revertPosition(v);
        }
	}
	
	abstract void markStatus(View v);
	
	private boolean toDismiss(int mViewWidth,float deltaX, VelocityTracker velocityTracker){
		float velocityX = velocityTracker.getXVelocity(); // needs to be directional
        float velocityY = Math.abs(velocityTracker.getYVelocity());
        ViewConfiguration vc = ViewConfiguration.get(getContext());
        int  mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        int mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        if (-deltaX > mViewWidth / 2) {
            return true;
        } else if (mMinFlingVelocity*10 <= -velocityX && -velocityX <= mMaxFlingVelocity
                && velocityY < Math.abs(velocityX)) {
            return true;
        }
        
        return false;
	}
	
	@Override
	public void onStartDrag(View v) {
		if(mAnimationTime < 0)
			mAnimationTime = getContext().getResources().getInteger(android.R.integer.config_shortAnimTime);
	}

	// TODO This revert hack is due to some bug with the onAnimationEnd event! Need to find a better workaround maybe.
    private boolean revert = false;
    private void revertPosition(final View downView){
    	revert = true;
    	animate(downView)
        .translationX(0)
        .setDuration(mAnimationTime)     
        .setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
            		if(notifyChange && revert) {
            			revert = false;
            			refresh();
            		}
            }
        });
    }
    
    private void refresh(){
    	ChoreListAdapter adapter = (ChoreListAdapter) getListView().getAdapter();
		adapter.notifyDataSetChanged();
    }
}
