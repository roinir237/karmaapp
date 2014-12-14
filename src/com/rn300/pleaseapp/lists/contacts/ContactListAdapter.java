package com.rn300.pleaseapp.lists.contacts;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.rn300.pleaseapp.R;
import com.rn300.pleaseapp.lists.ListItemInterface;

public class ContactListAdapter extends ArrayAdapter<ListItemInterface>{
	private LayoutInflater mInflater;
	
	public enum RowType{
		SECTION_TITLE,ITEM,INVITE_ITEM
	}
	
	public ContactListAdapter(Context context,List<ListItemInterface> objects) {
		super(context, R.layout.contact_row_view, objects);
			
		mInflater = LayoutInflater.from(context);
	}
		 
	 @Override
	 public View getView(int position, View convertView, ViewGroup parent) {
		 return getItem(position).getView(mInflater, convertView, parent);
	 }
	 
	 @Override
	 public int getItemViewType(int position){
		 return getItem(position).getItemViewType(position);
	 }
	 
	 @Override
	 public int getViewTypeCount(){
		 return RowType.values().length;
	 }
};
