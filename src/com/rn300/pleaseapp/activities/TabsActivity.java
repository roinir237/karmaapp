package com.rn300.pleaseapp.activities;

import org.json.JSONException;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnDismissListener;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

import com.rn300.pleaseapp.ApiService;
import com.rn300.pleaseapp.GlobalState;
import com.rn300.pleaseapp.R;
import com.rn300.pleaseapp.ServerMessagingService;
import com.rn300.pleaseapp.lists.children.ChildItem;

public class TabsActivity extends FragmentActivity implements OnTabChangeListener {
	 
    private static final String TAG = "FragmentTabs";
    
    public static final String ACTION_START_CHILDCHORES = "com.example.pleaseapp.START_CHILDCHORES";
    public static final String ACTION_START_TODO = "com.example.pleaseapp.START_TODO";
    
    public static final String TAB_PEOPLE = "people";
    public static final String TAB_TODO = "todo";
    public static final String TAB_CHILDCHORE = "childChore";
    public static final String TAB_CHOREFORM = "choreForm";
    
    private TabHost mTabHost;
    public int mCurrentTab = 0;
    
    private CountDownTimer karmaTicker = null;
 
    private IntentFilter karmaRefreshFilter = new IntentFilter(ApiService.REFRESH_KARMA);
	
	private BroadcastReceiver karmaRefereshReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			if(action.equals(ApiService.REFRESH_KARMA)){
				if(karmaTicker != null){
					karmaTicker.cancel();
					karmaTicker = null;
				}
				
				final TextView karmaView = (TextView) findViewById(R.id.myKarma);
				final int oldKarma = Integer.parseInt(karmaView.getText().toString());
				final int newKarma = ApiService.getTotalKarma(getBaseContext());
				final int amount = newKarma - oldKarma;
				
				if(amount != 0){
					karmaView.setTextColor(getResources().getColor(R.color.updating_karma));
					karmaTicker = new CountDownTimer(5000, 5000/Math.abs(amount)) {
						int ticks = 0;
					     public void onTick(long millisUntilFinished) {
					    	if(amount > 0) ticks++;
					    	else ticks--;
					    	karmaView.setText(Integer.toString(ticks+oldKarma));
					     }
	
					     public void onFinish() {
					    	karmaView.setText(Integer.toString(newKarma));
					    	karmaView.setTextColor(getResources().getColor(R.color.myKarma));
					     }
					}.start();
				}
			
			}
		}
	};
    
    public TabHost getTabHost(){
    	return mTabHost;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tabs_layout);
        
        setupTabs();
        
        mTabHost.setOnTabChangedListener(this);
            
        mTabHost.getTabWidget().getChildAt(1).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				onTabChanged(TAB_PEOPLE);
				mTabHost.setCurrentTab(1);
			}
        });
        
        if(getIntent().hasExtra(ServerMessagingService.EXTRA_NOTIFICATION_ID)){
        	NotificationManager mNotificationManager = (NotificationManager)
	                getSystemService(Context.NOTIFICATION_SERVICE);
        	int noteId = getIntent().getIntExtra(ServerMessagingService.EXTRA_NOTIFICATION_ID,0);
        	mNotificationManager.cancel(noteId);
        	GlobalState.resetNotificationNumber(String.valueOf(noteId));
    	}
        
        String action = getIntent().getAction();
        Intent i = getIntent();
        if(action != null && action.equals(ACTION_START_CHILDCHORES) && i.hasExtra(ApiService.CHILD_STRING)){
			try {
				ChildItem child = new ChildItem(this,i.getStringExtra(ApiService.CHILD_STRING));
				mTabHost.setCurrentTab(1);
				startChildChoreActivity(null,child);
			} catch (JSONException e) {
				updateTab(TAB_TODO, R.id.tab_1);
	        	mTabHost.setCurrentTab(mCurrentTab);
			}
        	
        }else{
        	updateTab(TAB_TODO, R.id.tab_1);
        	mTabHost.setCurrentTab(mCurrentTab);
        }
     	  	
        final Typeface font = Typeface.createFromAsset(this.getAssets(),"fonts/HelveticaNeueBold.ttf");
        ((TextView) findViewById(R.id.myKarma)).setTypeface(font);
        ((TextView) findViewById(R.id.myKarma)).setText(String.valueOf(ApiService.getTotalKarma(this)));
    }

    @Override
    protected void onResume(){
    	super.onResume();
    	this.registerReceiver(karmaRefereshReceiver, karmaRefreshFilter);
    }
    
    private void setupTabs() {
    	mTabHost = (TabHost)findViewById(R.id.tabhost);
        mTabHost.setup(); // you must call this before adding your tabs!
        mTabHost.addTab(newTab(TAB_TODO, R.layout.todo_tab, R.id.tab_1));
        mTabHost.addTab(newTab(TAB_PEOPLE, R.layout.people_tab, R.id.tab_2));   
    }
 
    private TabSpec newTab(String tag, int layoutId, int tabContentId) {
        Log.d(TAG, "buildTab(): tag=" + tag);
 
        ViewGroup parent = (ViewGroup) findViewById(android.R.id.tabs);
        
        View indicator = getLayoutInflater().inflate(layoutId,parent, false);
        
        TabSpec tabSpec = mTabHost.newTabSpec(tag);
        tabSpec.setIndicator(indicator);
        tabSpec.setContent(tabContentId);
        return tabSpec;
    }
 
    @Override
    public void onTabChanged(String tabId) {
        if (TAB_TODO.equals(tabId)) {
            updateTab(tabId, R.id.tab_1);
            int delay = this.getResources().getInteger(R.integer.fade_time);
            findViewById(R.id.tab_2).setAnimation(AnimationUtils.loadAnimation(this,R.animator.fade_out));
            Animation fadeIn = AnimationUtils.loadAnimation(this,R.animator.fade_in);
        	fadeIn.setStartOffset(delay);
            findViewById(R.id.tab_1).setAnimation(fadeIn);
            mCurrentTab = 0;
            return;
        }
        if (TAB_PEOPLE.equals(tabId)) {
            updateTab(tabId, R.id.tab_2);
            if(mCurrentTab != 1){
            	int delay = this.getResources().getInteger(R.integer.fade_time);
            	findViewById(R.id.tab_1).setAnimation(AnimationUtils.loadAnimation(this,R.animator.fade_out));
            	Animation fadeIn = AnimationUtils.loadAnimation(this,R.animator.fade_in);
            	fadeIn.setStartOffset(delay);
            	findViewById(R.id.tab_2).setAnimation(fadeIn);
            }
            mCurrentTab = 1;
            return;
        }
    }
        
    private void updateTab(String tabId, int placeholder) {
        FragmentManager fm = getSupportFragmentManager();
        
        // If the fragment wasn't already opened
        Fragment frag = fm.findFragmentByTag(tabId);
        if (frag == null || !frag.isInLayout()) {
        	if(tabId.equals(TAB_TODO)){
        		Fragment peopleFrag = fm.findFragmentByTag(TAB_CHILDCHORE);
        		if(peopleFrag instanceof ChildChoresActivity){
        			((ChildChoresActivity)peopleFrag).discardItemToRemove();
        		}
        		fm.beginTransaction()
                .replace(placeholder, new MyToDoActivity(), tabId)
                .commit();
        	}else if(tabId.equals(TAB_PEOPLE)){
        		MyToDoActivity toDoFrag = (MyToDoActivity)fm.findFragmentByTag(TAB_TODO);
        		if(toDoFrag != null) {
        			toDoFrag.discardItemToRemove();
        		}
        		fm.beginTransaction()
                .replace(placeholder,new PeopleActivity(), tabId)
                .commit();
        	}
        }
    }
    
    public void startChoreFormActivity(Fragment toRemove, ChildItem child){
    	Fragment childChoreFrag = getSupportFragmentManager().findFragmentByTag(TAB_CHILDCHORE);
    	if(childChoreFrag instanceof ChildChoresActivity){
			((ChildChoresActivity)childChoreFrag).discardItemToRemove();
		}
		
    	Bundle args = new Bundle();
		args.putString(ApiService.CHILD_STRING, child.toString());
		
		Fragment fragment = new ChoreFormActivity();
		fragment.setArguments(args);
		
		getSupportFragmentManager().beginTransaction()
		.setCustomAnimations(R.animator.translate_right_in, R.animator.translate_right_out, R.animator.translate_left_in, R.animator.translate_left_out)
		.add(R.id.tab_2, fragment,TAB_CHOREFORM)
		.remove(toRemove)
		.addToBackStack(null)
		.commit();
    }
    
    public void startChildChoreActivity(Fragment toRemove,ChildItem child){
    	Bundle args = new Bundle();
    	args.putString(ApiService.CHILD_STRING, child.toString());
    	
    	Fragment fragment = new ChildChoresActivity();
    	fragment.setArguments(args);
    	if(toRemove != null){
	    	getSupportFragmentManager().beginTransaction()
	    	.setCustomAnimations(R.animator.fade_in, R.animator.fade_out,R.animator.fade_in, R.animator.fade_out)
	    	.remove(toRemove)
	    	.add(R.id.tab_2, fragment, TAB_CHILDCHORE)
	    	.addToBackStack(null)
	    	.commit();
    	} else {
    		getSupportFragmentManager().beginTransaction()
	    	.setCustomAnimations(R.animator.fade_in, R.animator.fade_out,R.animator.fade_in, R.animator.fade_out)
	    	.replace(R.id.tab_2, fragment, TAB_CHILDCHORE)
	    	.commit();
    	}
    }
    
    @Override
    public void onBackPressed() {
    	FragmentManager fm = getSupportFragmentManager();
    	Fragment frag = fm.findFragmentByTag(TAB_PEOPLE);
    	if(frag != null && frag.isAdded() && mCurrentTab != 0){
    		
    		onTabChanged(TAB_TODO);
    		mTabHost.setCurrentTab(0);
    	}else if(mCurrentTab == 0){
    		 moveTaskToBack (true);
    	}else{
    		Fragment childChoreFrag = fm.findFragmentByTag(TAB_CHILDCHORE);
    		if(childChoreFrag instanceof ChildChoresActivity){
    			((ChildChoresActivity)childChoreFrag).discardItemToRemove();
    		}
    		
    		super.onBackPressed();
    	}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	if(android.os.Build.VERSION.SDK_INT < 11){
        	getMenuInflater().inflate(R.menu.main, menu);
    	}
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.settings:
        	Intent i = new Intent(this,SettingsActivity.class);
        	startActivity(i);
            return true;
        case R.id.newContact:
        	Intent contactsInt = new Intent(this,ContactsActivity.class);
        	startActivity(contactsInt);
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    protected void onPause(){
    	super.onPause();
    	this.unregisterReceiver(karmaRefereshReceiver);
    }
   
    @SuppressLint("NewApi")
	public void showPopup(View v) {
    	if(android.os.Build.VERSION.SDK_INT > 10){
	        PopupMenu popup = new PopupMenu(this, v);
	        final ImageView iv = ((ImageView) v);
	        iv.setImageResource(R.drawable.ic_menu_moreoverflow_normal_holo_dark_selected);
	        MenuInflater inflater = popup.getMenuInflater();
	        inflater.inflate(R.menu.actions, popup.getMenu());
	        
	        popup.setOnMenuItemClickListener(new OnMenuItemClickListener(){

				@Override
				public boolean onMenuItemClick(MenuItem item) {
					onOptionsItemSelected(item);
					return false;
				}
	        	
	        });
	        popup.setOnDismissListener(new OnDismissListener(){

				@Override
				public void onDismiss(PopupMenu menu) {
					iv.setImageResource(R.drawable.ic_menu_moreoverflow_normal_holo_dark);
				}
	        	
	        });
	        popup.show();
    	}
    }

    @Override
    public boolean onKeyUp(int keycode, KeyEvent e) {
        switch(keycode) {
            case KeyEvent.KEYCODE_MENU:
            	if(android.os.Build.VERSION.SDK_INT > 10){
            		showPopup(findViewById(R.id.menu_popup));
            		return true;
            	}
        }

        return super.onKeyUp(keycode, e);
    }
}