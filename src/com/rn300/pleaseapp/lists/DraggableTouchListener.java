package com.rn300.pleaseapp.lists;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AbsListView;
import android.widget.ListView;

/**
 * A touch listener to apply to a list of Draggable items. 
 * 
 * @author roi
 *
 */
public class DraggableTouchListener implements View.OnTouchListener {
	@SuppressWarnings("unused")
	private static final String TAG = "DraggableTouchListener";
	
    // Cached ViewConfiguration and system-wide constant values
    private int mSlop;
 
    // Fixed properties
    private ListView mListView;
   
    // Transient properties
    private float mDownX;
    private boolean mSwiping;
    private VelocityTracker mVelocityTracker;
    private int mDownPosition;
    private View mDownView;
    private boolean mPaused;
    private Draggable draggable; 
  
    public DraggableTouchListener(ListView listView) {
        ViewConfiguration vc = ViewConfiguration.get(listView.getContext());
        mSlop = vc.getScaledTouchSlop();
        mListView = listView;
    }
    
    /**
     * Enables or disables (pauses or resumes) watching for swipe-to-dismiss gestures.
     *
     * @param enabled Whether or not to watch for gestures.
     */
    public void setEnabled(boolean enabled) {
        mPaused = !enabled;
    }
    
    /**
     * Returns an {@link android.widget.AbsListView.OnScrollListener} to be added to the
     * {@link ListView} using
     * {@link ListView#setOnScrollListener(android.widget.AbsListView.OnScrollListener)}.
     * If a scroll listener is already assigned, the caller should still pass scroll changes
     * through to this listener. This will ensure that this
     * {@link choreTouchListener} is paused during list view scrolling.</p>
     *
     * @see {@link choreTouchListener}
     */
    public AbsListView.OnScrollListener makeScrollListener() {
        return new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                setEnabled(scrollState != AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
            }
 
            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {
            }
        };
    }
 
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                if (mPaused) {
                    return false;
                }
                
                findDownView(motionEvent);
                
                if (mDownView != null) startDragging(motionEvent);
                   
                view.onTouchEvent(motionEvent);
                return true;
            }
            case MotionEvent.ACTION_UP: {
            	if(draggable != null && mVelocityTracker != null){
            		stopDragging(motionEvent);
            	}
            	
                break;
            }
            case MotionEvent.ACTION_MOVE: {
            	if(draggable != null && mVelocityTracker != null && !mPaused){
            		return dragging(motionEvent); 
            	}
            }
        }
        return false;
    }
    
    private void findDownView(MotionEvent motionEvent){
    	Rect rect = new Rect();
        int childCount = mListView.getChildCount();
        int[] listViewCoords = new int[2];
        mListView.getLocationOnScreen(listViewCoords);
        int x = (int) motionEvent.getRawX() - listViewCoords[0];
        int y = (int) motionEvent.getRawY() - listViewCoords[1];
        View child;
        for (int i = 0; i < childCount; i++) {
            child = mListView.getChildAt(i);
            
            child.getHitRect(rect);
            if (rect.contains(x, y)) {
                mDownView = child;
                mDownPosition =  mListView.getPositionForView(mDownView);;
                break;
            }
        }
    }
    
    private void startDragging(MotionEvent motionEvent){
		mDownX = motionEvent.getRawX();
		mDownPosition = mListView.getPositionForView(mDownView);
		mVelocityTracker = VelocityTracker.obtain();
		mVelocityTracker.addMovement(motionEvent);
		
		Object o = mListView.getItemAtPosition(mDownPosition);
		if(o instanceof Draggable){
			draggable = (Draggable) o;
			// trigger onTouchDown
			draggable.setListView(mListView);
			draggable.onTouchDown(mDownView);
		}else draggable = null;
   
    }
    
    private boolean dragging(MotionEvent motionEvent){
    	  mVelocityTracker.addMovement(motionEvent);
          float deltaX = motionEvent.getRawX() - mDownX;
          if (Math.abs(deltaX) > mSlop) {
              mSwiping = true;
              mListView.requestDisallowInterceptTouchEvent(true);
              // Cancel ListView's touch (un-highlighting the item)
              MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
              cancelEvent.setAction(MotionEvent.ACTION_CANCEL |(motionEvent.getActionIndex() << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
              mListView.onTouchEvent(cancelEvent);
              cancelEvent.recycle();
              // trigger onStart
              draggable.onStartDrag(mDownView);
          }
          
          if (mSwiping) {
              animate(mDownView).translationX(deltaX).setDuration(0);
              // trigger onDrag
              draggable.onDrag(mDownView, deltaX);
              return true;
          }
          
          return false;
    }
    
    private void stopDragging(MotionEvent motionEvent){
        float deltaX = motionEvent.getRawX() - mDownX;
        mVelocityTracker.addMovement(motionEvent);
        mVelocityTracker.computeCurrentVelocity(1000);
        if(mSwiping){
        	// lunch onStop
        	draggable.onStopDrag(mDownView,deltaX, mVelocityTracker,mDownPosition);
        }else{
        	// trigger onCancel
            draggable.onCancelDrag(mDownView,motionEvent);
        }
        
        // Reseting
        mVelocityTracker.recycle();
        mDownX = 0;
        mDownView = null;
        mDownPosition = ListView.INVALID_POSITION;
        mSwiping = false;
        draggable = null;
    }
    
}