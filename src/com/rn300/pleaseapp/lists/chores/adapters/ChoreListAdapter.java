package com.rn300.pleaseapp.lists.chores.adapters;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;
import static com.nineoldandroids.view.ViewHelper.setAlpha;
import static com.nineoldandroids.view.ViewHelper.setTranslationX;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.json.JSONException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ValueAnimator;
import com.rn300.pleaseapp.R;
import com.rn300.pleaseapp.lists.DraggableTouchListener;
import com.rn300.pleaseapp.lists.ListItemInterface;
import com.rn300.pleaseapp.lists.chores.items.ChoreHint;
import com.rn300.pleaseapp.lists.chores.items.ChoreSection;
import com.rn300.pleaseapp.lists.chores.items.ChoreWhen;
import com.rn300.pleaseapp.lists.chores.items.choreitem.ChoreItem;

public abstract class ChoreListAdapter extends ArrayAdapter<ListItemInterface>{
	@SuppressWarnings("unused")
	private static final String TAG = "ChoreListAdapter";
	private static LayoutInflater mInflater;
	protected boolean containsHint = false;
	protected static ListView mListView;
	
	protected int mDismissAnimationRefCount;
	protected ArrayList<DismissItem> mPendingDismisses = new ArrayList<DismissItem>();
	protected Boolean sectionsDismissed;
	protected ArrayList<ValueAnimator> collapseAnimations = new ArrayList<ValueAnimator>();
	protected RemoveItemListener mListener;
	
	public interface RemoveItemListener{
		void onDismissRows(HashMap<Integer,ListItemInterface> descendingOrderedItems);
	}
	
	public void setRemoveListener(RemoveItemListener listener){
		mListener = listener;
	}
	
	private class DismissItem  implements Comparable<DismissItem>{
		int position;
		View view; 
		int originalHeight;

		public DismissItem(int p, View v){
			this.position = p;
			this.view = v;
			this.originalHeight = v == null ? 0:v.getHeight();
		}

		@Override
		public int compareTo(DismissItem another) {
			return another.position - position;
		}
	 
		@Override
		public boolean equals(Object other){
			if(other instanceof Integer ){
				return position == (Integer)other;
			}else if(other instanceof DismissItem){
				return position == ((DismissItem)other).position;
			}else{
				return false;
			}
			
		}
	}
	
	public static enum RowTypes {
		CHORE_TITLE, CHORE_HINT, CHORE_SECTION, CHORE_WHEN, CHORE_ITEM
	}
	
	@Override 
	public int getViewTypeCount(){
		return RowTypes.values().length;
	}
	
	@Override 
	public int getItemViewType(int position){
		return this.getItem(position).getItemViewType(position);
	}
	
	public void dismiss(final View v, final int mViewWidth, final int position, long mAnimationTime) {
		++mDismissAnimationRefCount;
		 animate(v)
        .translationX(-mViewWidth)
        .alpha(0)
        .setListener(new AnimatorListenerHack(true,v,position,mAnimationTime))
        .setDuration(mAnimationTime);
	}
	
	private class AnimatorListenerHack extends AnimatorListenerAdapter{
			private boolean actualDismiss;
			private View v;
			private int position;
			private long mAnimationTime;
			
			public AnimatorListenerHack(boolean actual, View view, int pos, long animationTime){
				super();
				v = view;
				mAnimationTime = animationTime;
				position = pos;
				actualDismiss = actual;
			}
			
			public void onAnimationEnd(Animator animation) {	
            	if(actualDismiss){
            		performDismiss(v, position, mAnimationTime);
            		actualDismiss = false;
            	}
            }
	};
	
	protected void performDismiss(final View dismissView, final int dismissPosition, long mAnimationTime){
		final LayoutParams lp = (LayoutParams) dismissView.getLayoutParams();
	    final int originalHeight = dismissView.getHeight();
	       
        ValueAnimator animator = ValueAnimator.ofInt(originalHeight, 1).setDuration(mAnimationTime);
	 
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                --mDismissAnimationRefCount;
                if (mDismissAnimationRefCount == 0) {
                	ChoreListAdapter adapter = ChoreListAdapter.this;
                	Collections.sort(mPendingDismisses);
                	HashMap<Integer,ListItemInterface> descendingOrderedItems = new HashMap<Integer,ListItemInterface>();
                	int i = 0;
                	for(DismissItem item : mPendingDismisses){
                		ListItemInterface itemToRemove = adapter.getItem(item.position);
                		descendingOrderedItems.put(item.position, itemToRemove);
                		adapter.remove(itemToRemove);
                		i++;
                	}
                	if(mListener != null) mListener.onDismissRows(descendingOrderedItems);
                	adapter.notifyDataSetChanged();
                    
                	ViewGroup.LayoutParams nlp;
                	for(DismissItem item : mPendingDismisses){
                	    // Reset view presentation
                		nlp = item.view.getLayoutParams();
                		nlp.height = item.originalHeight;
                		
                		setAlpha(item.view, 1f);
                        setTranslationX(item.view, 0);
                        item.view.setAnimation(null);
                        item.view.setLayoutParams(nlp);
                	}
                	
                    mPendingDismisses.clear();
                    
                }
            }
        });
 
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                lp.height = (Integer) valueAnimator.getAnimatedValue();
                dismissView.setLayoutParams(lp);
            }
        });
        
        mPendingDismisses.add(new DismissItem(dismissPosition, dismissView));
        
        collapseAnimations.add(animator);
       
        if(dismissPosition != getCount() && getItem(dismissPosition) instanceof ChoreItem){
        	int nextAbove = dismissPosition - 1;
        	while(mPendingDismisses.contains(new DismissItem(nextAbove,null))){
        		nextAbove--;
        	}
        	int nextBelow = dismissPosition + 1;
        	while(mPendingDismisses.contains(new DismissItem(nextBelow,null))){
        		nextBelow++;
        	}
        	
    		if(!(getItem(nextAbove) instanceof ChoreItem) &&
    		 (nextBelow == getCount() || !(getItem(nextBelow) instanceof ChoreItem)) ){
    			int headerPosition = nextAbove  - mListView.getFirstVisiblePosition();
    			if(headerPosition >= 0 ) {
    				View v = mListView.getChildAt(headerPosition);
    				dismiss(v, v.getWidth(), nextAbove, mAnimationTime);
    				sectionsDismissed = false;
    			}else{
    				sectionsDismissed = true;
    			}
    		}else{
    			sectionsDismissed = true;
    		}
    	}else if(dismissPosition != getCount() && getItem(dismissPosition) instanceof ChoreWhen){
    		int nextAbove = dismissPosition - 1;
	    	while(mPendingDismisses.contains(new DismissItem(nextAbove,null))){
	    		nextAbove--;
	    	}
	    	int nextBelow = dismissPosition + 1;
	    	while(mPendingDismisses.contains(new DismissItem(nextBelow,null))){
	    		nextBelow++;
	    	}
	    	if((getItem(nextAbove) instanceof ChoreSection) &&
	       		 (nextBelow == getCount() || (getItem(nextBelow) instanceof ChoreSection) || (getItem(nextBelow) instanceof ChoreHint)) ){
	       			int headerPosition = nextAbove  - mListView.getFirstVisiblePosition();
	       			if(headerPosition >= 0 ) {
	       				View v = mListView.getChildAt(headerPosition);
	       				dismiss(v, v.getWidth(), nextAbove, mAnimationTime);
	       				sectionsDismissed = false;
	       			}else{
	       				sectionsDismissed = true;
	       			}
	    	}else{
	    		sectionsDismissed = true;
	    	}
		}else{
    		sectionsDismissed = true;
    	}
	   
	   if(sectionsDismissed){
	    	for(ValueAnimator animation:collapseAnimations){
	        	animation.start();	
	        } 
	    	collapseAnimations.clear();
        }
	   //     	animator.start();
	}
		
	public ChoreListAdapter(Context context, int resid, ListView listView) {
		super(context, resid);	
		mInflater = LayoutInflater.from(context);
		final DraggableTouchListener touchListener = new DraggableTouchListener(listView);
        listView.setOnTouchListener(touchListener);
        listView.setOnScrollListener(touchListener.makeScrollListener());
        
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				ListItemInterface item = ChoreListAdapter.this.getItem(position);
				if(item instanceof ChoreItem){
					setupOptionsDialogue(position);
				}
				return true;
			}
        });
	
        mListView = listView;
	}
	
	public void setHint(int resid){
		this.setNotifyOnChange(false);
		if(containsHint){
			int hintPos = getCount()-1;
			while(!(getItem(hintPos) instanceof ChoreHint)){
				hintPos--;
			}
			
			this.remove(this.getItem(hintPos));
		}else{
			containsHint = true;
		}
		
		super.add(new ChoreHint(getContext(),resid));
		this.notifyDataSetChanged();
	}
	
	public void removeHint(){
		if(containsHint){
			this.remove(this.getItem(this.getCount()-1));
			containsHint = false;
		}
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return getItem(position).getView(mInflater, convertView,parent);	
	}

	public abstract void add(ChoreItem chore);
	
	protected abstract void setupOptionsDialogue(int position);

    public void set(int index, ListItemInterface item){
    	super.setNotifyOnChange(false);
    	super.remove(item);
    	super.insert(item, index);
    	super.notifyDataSetChanged();
    }
}