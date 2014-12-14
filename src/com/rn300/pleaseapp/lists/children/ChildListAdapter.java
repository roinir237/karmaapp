package com.rn300.pleaseapp.lists.children;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.rn300.pleaseapp.R;
import com.rn300.pleaseapp.lists.ListItemInterface;

public class ChildListAdapter extends ArrayAdapter<ListItemInterface>{

	private LayoutInflater mInflater;
	
	public ChildListAdapter(Context context, List<ListItemInterface> objects) {
		super(context, R.layout.child_row_view, objects);
		mInflater = LayoutInflater.from(context);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return getItem(position).getView(mInflater, convertView, parent);
	}
}
