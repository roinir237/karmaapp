package com.rn300.pleaseapp.countrylist;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.ListActivity;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.rn300.pleaseapp.R;

public class CountryListActivity extends ListActivity {
	@SuppressWarnings("unused")
	private String TAG = "CountryListActivity";
	private CountryListAdapter mAdapter;
	private List<String> name;
	private List<String> code;
	
	public static final String RESULT_COUNTRY_CODE = "countryCode";
	public static final String RESULT_COUNTRY_NAME = "countryName";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_country_list);
	
		XmlResourceParser xrp = this.getResources().getXml(R.xml.countries);
		name = new ArrayList<String>();
		code = new ArrayList<String>();
		try {
			xrp.next();
			int eventType = xrp.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
			    if (eventType == XmlPullParser.START_TAG
			            && xrp.getName().equalsIgnoreCase("country")) {
			    	
			       code.add(xrp.getAttributeValue(null,"phoneCode"));
			       name.add(xrp.getAttributeValue(null, "name"));
			    }
			    eventType = xrp.next();
			}
			
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		mAdapter = new CountryListAdapter(this,name,code);
		setListAdapter(mAdapter);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id){
		Intent returnIntent = new Intent();
		returnIntent.putExtra(RESULT_COUNTRY_CODE,code.get(position));
		returnIntent.putExtra(RESULT_COUNTRY_NAME,name.get(position));
		setResult(RESULT_OK,returnIntent);     
		finish();
	}
}
