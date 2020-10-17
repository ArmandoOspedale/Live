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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

class StatsAdapter extends BaseAdapter implements Filterable {

    private final Context cxt;
    private List<HashMap<String, String>> originalData;
    private List<HashMap<String, String>> filteredData;
    private final ItemFilter mFilter = new ItemFilter();

    StatsAdapter(Context cxt, List<HashMap<String, String>> d) {
        this.cxt = cxt;
        originalData = d;
        filteredData = d;
    }

    @Override
    public int getCount() {
        return filteredData.size();
    }

    @Override
    public Object getItem(int i) {
        return filteredData.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    @SuppressWarnings("all")
    public View getView(int i, View view, ViewGroup viewGroup) {

        if (view == null) {
            view = LayoutInflater.from(cxt).inflate(R.layout.stat_row, null);
        }

        HashMap<String, String> calciatore = (HashMap<String, String>) getItem(i);
        TextView txt = (TextView) view.findViewById(R.id.Calciatore);
        txt.setText(calciatore.get("Calciatore"));

        txt = (TextView) view.findViewById(R.id.Pg);
        txt.setText(calciatore.get("Pg"));

        txt = (TextView) view.findViewById(R.id.Mv);
        txt.setText(calciatore.get("Mv"));

        txt = (TextView) view.findViewById(R.id.Mf);
        txt.setText(calciatore.get("Mf"));

        txt = (TextView) view.findViewById(R.id.G);
        txt.setText(calciatore.get("G"));

        txt = (TextView) view.findViewById(R.id.Ass);
        txt.setText(calciatore.get("Ass"));

        txt = (TextView) view.findViewById(R.id.Amm);
        txt.setText(calciatore.get("Amm"));

        txt = (TextView) view.findViewById(R.id.Esp);
        txt.setText(calciatore.get("Esp"));

        return view;
    }

    void setData(List<HashMap<String, String>> d) {
        originalData = d;
        filteredData = d;
        notifyDataSetChanged();
    }

    void sort (final String campo, final boolean verso) {
        Collections.sort(filteredData, new Comparator<HashMap<String, String>>() {
            @Override
            public int compare(HashMap<String, String> t1, HashMap<String, String> t2) {
                return verso ? Double.compare(Double.parseDouble(t2.get(campo)), Double.parseDouble(t1.get(campo)))
                        : Double.compare(Double.parseDouble(t1.get(campo)), Double.parseDouble(t2.get(campo)));
            }
        });
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

            final List<HashMap<String, String>> list = originalData;

            int count = list.size();
            final List<HashMap<String, String>> nlist = new ArrayList<>(count);

            String filterableString;

            for (int i = 0; i < count; i++) {
                filterableString = list.get(i).get("Calciatore").split(" \\(")[0];
                if (filterableString.toLowerCase().contains(filterString)) {
                    nlist.add(originalData.get(i));
                }
            }

            results.values = nlist;
            results.count = nlist.size();

            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredData = (List<HashMap<String, String>>) results.values;
            notifyDataSetChanged();
        }

    }
}
