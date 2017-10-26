package com.vise.bledemo.adapter;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.SectionIndexer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class MergeAdapter extends BaseAdapter implements SectionIndexer {
    protected MergeAdapter.PieceStateRoster pieces = new MergeAdapter.PieceStateRoster();

    public MergeAdapter() {
    }

    public void addAdapter(ListAdapter adapter) {
        this.pieces.add(adapter);
        adapter.registerDataSetObserver(new MergeAdapter.CascadeDataSetObserver());
    }

    public void addView(View view) {
        this.addView(view, false);
    }

    public void addView(View view, boolean enabled) {
        ArrayList list = new ArrayList(1);
        list.add(view);
        this.addViews(list, enabled);
    }

    public void addViews(List<View> views) {
        this.addViews(views, false);
    }

    public void addViews(List<View> views, boolean enabled) {
        if (enabled) {
            this.addAdapter(new MergeAdapter.EnabledSackAdapter(views));
        } else {
            this.addAdapter(new SackOfViewsAdapter(views));
        }

    }

    public Object getItem(int position) {
        int size;
        for (Iterator i$ = this.getPieces().iterator(); i$.hasNext(); position -= size) {
            ListAdapter piece = (ListAdapter) i$.next();
            size = piece.getCount();
            if (position < size) {
                return piece.getItem(position);
            }
        }

        return null;
    }

    public ListAdapter getAdapter(int position) {
        int size;
        for (Iterator i$ = this.getPieces().iterator(); i$.hasNext(); position -= size) {
            ListAdapter piece = (ListAdapter) i$.next();
            size = piece.getCount();
            if (position < size) {
                return piece;
            }
        }

        return null;
    }

    public int getCount() {
        int total = 0;

        ListAdapter piece;
        for (Iterator i$ = this.getPieces().iterator(); i$.hasNext(); total += piece.getCount()) {
            piece = (ListAdapter) i$.next();
        }

        return total;
    }

    public int getViewTypeCount() {
        int total = 0;

        MergeAdapter.PieceState piece;
        for (Iterator i$ = this.pieces.getRawPieces().iterator(); i$.hasNext(); total += piece.adapter.getViewTypeCount()) {
            piece = (MergeAdapter.PieceState) i$.next();
        }

        return Math.max(total, 1);
    }

    public int getItemViewType(int position) {
        int typeOffset = 0;
        int result = -1;

        MergeAdapter.PieceState piece;
        for (Iterator i$ = this.pieces.getRawPieces().iterator(); i$.hasNext(); typeOffset += piece.adapter.getViewTypeCount()) {
            piece = (MergeAdapter.PieceState) i$.next();
            if (piece.isActive) {
                int size = piece.adapter.getCount();
                if (position < size) {
                    result = typeOffset + piece.adapter.getItemViewType(position);
                    break;
                }

                position -= size;
            }
        }

        return result;
    }

    public boolean areAllItemsEnabled() {
        return false;
    }

    public boolean isEnabled(int position) {
        int size;
        for (Iterator i$ = this.getPieces().iterator(); i$.hasNext(); position -= size) {
            ListAdapter piece = (ListAdapter) i$.next();
            size = piece.getCount();
            if (position < size) {
                return piece.isEnabled(position);
            }
        }

        return false;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        int size;
        for (Iterator i$ = this.getPieces().iterator(); i$.hasNext(); position -= size) {
            ListAdapter piece = (ListAdapter) i$.next();
            size = piece.getCount();
            if (position < size) {
                return piece.getView(position, convertView, parent);
            }
        }

        return null;
    }

    public long getItemId(int position) {
        int size;
        for (Iterator i$ = this.getPieces().iterator(); i$.hasNext(); position -= size) {
            ListAdapter piece = (ListAdapter) i$.next();
            size = piece.getCount();
            if (position < size) {
                return piece.getItemId(position);
            }
        }

        return -1L;
    }

    public int getPositionForSection(int section) {
        int position = 0;

        ListAdapter piece;
        for (Iterator i$ = this.getPieces().iterator(); i$.hasNext(); position += piece.getCount()) {
            piece = (ListAdapter) i$.next();
            if (piece instanceof SectionIndexer) {
                Object[] sections = ((SectionIndexer) piece).getSections();
                int numSections = 0;
                if (sections != null) {
                    numSections = sections.length;
                }

                if (section < numSections) {
                    return position + ((SectionIndexer) piece).getPositionForSection(section);
                }

                if (sections != null) {
                    section -= numSections;
                }
            }
        }

        return 0;
    }

    public int getSectionForPosition(int position) {
        int section = 0;

        int size;
        for (Iterator i$ = this.getPieces().iterator(); i$.hasNext(); position -= size) {
            ListAdapter piece = (ListAdapter) i$.next();
            size = piece.getCount();
            if (position < size) {
                if (piece instanceof SectionIndexer) {
                    return section + ((SectionIndexer) piece).getSectionForPosition(position);
                }

                return 0;
            }

            if (piece instanceof SectionIndexer) {
                Object[] sections = ((SectionIndexer) piece).getSections();
                if (sections != null) {
                    section += sections.length;
                }
            }
        }

        return 0;
    }

    public Object[] getSections() {
        ArrayList sections = new ArrayList();
        Iterator i$ = this.getPieces().iterator();

        while (i$.hasNext()) {
            ListAdapter piece = (ListAdapter) i$.next();
            if (piece instanceof SectionIndexer) {
                Object[] curSections = ((SectionIndexer) piece).getSections();
                if (curSections != null) {
                    Collections.addAll(sections, curSections);
                }
            }
        }

        if (sections.size() == 0) {
            return new String[0];
        } else {
            return sections.toArray(new Object[sections.size()]);
        }
    }

    public void setActive(ListAdapter adapter, boolean isActive) {
        this.pieces.setActive(adapter, isActive);
        this.notifyDataSetChanged();
    }

    public void setActive(View v, boolean isActive) {
        this.pieces.setActive(v, isActive);
        this.notifyDataSetChanged();
    }

    protected List<ListAdapter> getPieces() {
        return this.pieces.getPieces();
    }

    private class CascadeDataSetObserver extends DataSetObserver {
        private CascadeDataSetObserver() {
        }

        public void onChanged() {
            MergeAdapter.this.notifyDataSetChanged();
        }

        public void onInvalidated() {
            MergeAdapter.this.notifyDataSetInvalidated();
        }
    }

    private static class EnabledSackAdapter extends SackOfViewsAdapter {
        public EnabledSackAdapter(List<View> views) {
            super(views);
        }

        public boolean areAllItemsEnabled() {
            return true;
        }

        public boolean isEnabled(int position) {
            return true;
        }
    }

    private static class PieceStateRoster {
        protected ArrayList<PieceState> pieces;
        protected ArrayList<ListAdapter> active;

        private PieceStateRoster() {
            this.pieces = new ArrayList();
            this.active = null;
        }

        void add(ListAdapter adapter) {
            this.pieces.add(new MergeAdapter.PieceState(adapter, true));
        }

        void setActive(ListAdapter adapter, boolean isActive) {
            Iterator i$ = this.pieces.iterator();

            while (i$.hasNext()) {
                MergeAdapter.PieceState state = (MergeAdapter.PieceState) i$.next();
                if (state.adapter == adapter) {
                    state.isActive = isActive;
                    this.active = null;
                    break;
                }
            }

        }

        void setActive(View v, boolean isActive) {
            Iterator i$ = this.pieces.iterator();

            while (i$.hasNext()) {
                MergeAdapter.PieceState state = (MergeAdapter.PieceState) i$.next();
                if (state.adapter instanceof SackOfViewsAdapter && ((SackOfViewsAdapter) state.adapter).hasView(v)) {
                    state.isActive = isActive;
                    this.active = null;
                    break;
                }
            }

        }

        List<PieceState> getRawPieces() {
            return this.pieces;
        }

        List<ListAdapter> getPieces() {
            if (this.active == null) {
                this.active = new ArrayList();
                Iterator i$ = this.pieces.iterator();

                while (i$.hasNext()) {
                    MergeAdapter.PieceState state = (MergeAdapter.PieceState) i$.next();
                    if (state.isActive) {
                        this.active.add(state.adapter);
                    }
                }
            }

            return this.active;
        }
    }

    private static class PieceState {
        ListAdapter adapter;
        boolean isActive = true;

        PieceState(ListAdapter adapter, boolean isActive) {
            this.adapter = adapter;
            this.isActive = isActive;
        }
    }
}
