package com.live;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class Filter_Adapter extends BaseAdapter implements Filterable {

    private final Context context;
    private final ItemFilter mFilter = new ItemFilter();
    private final List<HashMap<String, String>> originalData;
    private List<HashMap<String, String>> filteredData;
    String squadra;

    Filter_Adapter(Context context, List<HashMap<String, String>> giocatori) {
        this.context = context;
        originalData = giocatori;
        filteredData = originalData;
    }

    @Override
    public int getCount() {
        return filteredData.size();
    }

    @Override
    public Object getItem(int position) {
        return filteredData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }

    @Override
    @SuppressWarnings("all")
    public View getView(final int position, View v, ViewGroup vg) {
        if (v == null) {
            v = LayoutInflater.from(context).inflate(R.layout.list_item, null);
        }
        HashMap<String, String> gioc = (HashMap<String, String>) getItem(position);
        TextView txt = (TextView) v.findViewById(R.id.nome);
        txt.setText(gioc.get("Nome"));
        txt = (TextView) v.findViewById(R.id.squadra);
        txt.setText(gioc.get("Squadra"));
        txt = (TextView) v.findViewById(R.id.quot);
        txt.setText(String.valueOf(gioc.get("Quot")));

        return v;
    }

    void clearTextFilter () {
        filteredData = originalData;
        notifyDataSetChanged();
    }

    public Filter getFilter() {
        return mFilter;
    }

    private class ItemFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            String filterString = constraint.toString().toLowerCase();

            FilterResults results = new FilterResults();

            int count = originalData.size();
            final List<HashMap<String, String>> nlist = new ArrayList<>(count);

            String filterableString;

            for (int i = 0; i < count; i++) {
                if (filterString.equals("")) {
                    filterableString = originalData.get(i).get("Squadra");
                    if (filterableString.startsWith(squadra)) {
                        nlist.add(originalData.get(i));
                    }
                } else {
                    filterableString = originalData.get(i).get("Nome");
                    if (filterableString.toLowerCase().startsWith(filterString)) {
                        nlist.add(originalData.get(i));
                    }
                }
            }

            results.values = nlist;
            results.count = nlist.size();

            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredData = (List<HashMap<String, String>>) results.values;
            notifyDataSetChanged();
        }
    }
}

