package com.rn300.pleaseapp.countrylist;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.rn300.pleaseapp.R;

public class CountryListAdapter extends ArrayAdapter<String> {
	private final Context context;
	private final List<String> mCode;

	private class ViewHolder {
		TextView name;
		TextView code;
	}
	
	public CountryListAdapter(Context context,List<String> countries,List<String> codes) {
		super(context, R.layout.country_row_view, countries);
		
	    this.context = context;
	    this.mCode = codes;
	}
		
	 
	 @Override
	 public View getView(int position, View convertView, ViewGroup parent) {
		 View view;
		 ViewHolder holder;
		 if(convertView == null){
		 	LayoutInflater inflater = (LayoutInflater) context
		 			.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		 	view = inflater.inflate( R.layout.country_row_view, parent, false);
		 	holder = new ViewHolder();
		 	TextView countryName = (TextView) view.findViewById(R.id.country_name);
		 	TextView countryCode = (TextView) view.findViewById(R.id.country_code);
		 	holder.name = countryName;
		 	holder.code = countryCode;
		 	view.setTag(holder);
		 }else{
			 holder = (ViewHolder) convertView.getTag();
			 view = convertView;
		 }
			
		 holder.name.setText(getItem(position));
		 holder.code.setText(mCode.get(position));
		 
		 return view;
	  }
};