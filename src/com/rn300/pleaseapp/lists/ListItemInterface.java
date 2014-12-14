package com.rn300.pleaseapp.lists;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public interface ListItemInterface {
	public View getView(LayoutInflater inflater, View convertView, ViewGroup parent);
	public void remove();
	public int getItemViewType(int position);
}
