package com.vise.bledemo.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

public class SackOfViewsAdapter extends BaseAdapter {
    private List<View> views = null;

    public SackOfViewsAdapter(int count) {
        this.views = new ArrayList(count);

        for (int i = 0; i < count; ++i) {
            this.views.add(null);
        }

    }

    public SackOfViewsAdapter(List<View> views) {
        this.views = views;
    }

    public Object getItem(int position) {
        return this.views.get(position);
    }

    public int getCount() {
        return this.views.size();
    }

    public int getViewTypeCount() {
        return this.getCount();
    }

    public int getItemViewType(int position) {
        return position;
    }

    public boolean areAllItemsEnabled() {
        return false;
    }

    public boolean isEnabled(int position) {
        return false;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View result = this.views.get(position);
        if (result == null) {
            result = this.newView(position, parent);
            this.views.set(position, result);
        }

        return result;
    }

    public long getItemId(int position) {
        return (long) position;
    }

    public boolean hasView(View v) {
        return this.views.contains(v);
    }

    protected View newView(int position, ViewGroup parent) {
        throw new RuntimeException("You must override newView()!");
    }
}
