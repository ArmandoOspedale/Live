package com.live;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.StateListDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.jpardogo.android.googleprogressbar.library.GoogleProgressBar;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

public class GiornataFragment extends Fragment {

    private int position;
    private ListView listView = null;
    private GoogleProgressBar gpb;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        String[] giornata = getArguments().getStringArray("giornata");
        position = getArguments().getInt("position");

        LinearLayout l = new LinearLayout(getContext());
        l.setOrientation(LinearLayout.VERTICAL);
        gpb = new GoogleProgressBar(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        params.topMargin = 10 * (int)getActivity().getResources().getDisplayMetrics().density;
        params.bottomMargin = 10 * (int)getActivity().getResources().getDisplayMetrics().density;
        l.addView(gpb, params);

        listView = new ListView(getContext());
        listView.setVerticalScrollBarEnabled(false);
        listView.setSelector(new StateListDrawable());

        List<HashMap<String, String>> partite = new ArrayList<>();

        if (giornata != null) {
            for (String g : giornata) {
                HashMap<String, String> map = new HashMap<>();
                StringTokenizer st = new StringTokenizer(g, "<_>");
                map.put("casa", st.nextToken());
                if (map.get("casa").equals("Chievoverona")) {
                    map.put("casa", "Chievo");
                }
                map.put("trasf", st.nextToken());
                if (map.get("trasf").equals("Chievoverona")) {
                    map.put("trasf", "Chievo");
                }
                map.put("risultato", st.nextToken());
                if(st.hasMoreTokens()) {
                    map.put("data", st.nextToken());
                } else {
                    map.put("data", "00/00/00 00:00");
                }

                partite.add(map);
            }

            Collections.sort(partite, new Comparator<HashMap<String, String>>() {
                @Override
                public int compare(HashMap<String, String> p1, HashMap<String, String> p2) {
                    DateFormat format = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.ITALIAN);
                    try {
                        return format.parse(p1.get("data")).compareTo(format.parse(p2.get("data")));
                    } catch (ParseException e) {
                        return 0;
                    }
                }
            });

            gpb.setVisibility(View.GONE);
            mAdapter adapter = new mAdapter(getContext(), partite, R.layout.cal_serie_a_list, new String[]{"casa", "trasf", "risultato", "data"},
                    new int[]{R.id.casa, R.id.trasf, R.id.result, R.id.data});
            listView.setAdapter(adapter);
        } else {
            new Carica(this).execute(getArguments().getString("link"));
        }

        l.addView(listView);
        return l;
    }

    private class mAdapter extends SimpleAdapter {

        String [] squadre;

        mAdapter (Context c, List<HashMap<String, String>> d, int r, String[] f, int[] i) {
            super(c, d, r, f, i);
            squadre = new String[d.size()];
            for (int j = 0; j < d.size(); j++) {
                squadre[j] = d.get(j).get("casa") + "<>" + d.get(j).get("trasf");
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);

            ImageView im = v.findViewById(R.id.icona_casa);
            im.setImageResource(getDrawable(getContext(), squadre[position].split("<>")[0].toLowerCase().replaceAll(" ", "_")));

            im = v.findViewById(R.id.icona_trasf);
            im.setImageResource(getDrawable(getContext(), squadre[position].split("<>")[1].toLowerCase().replaceAll(" ", "_")));

            return v;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class Carica extends AsyncTask<String, Void, String[]> {
        Fragment fragment;

        Carica(Fragment fragment) {
            this.fragment = fragment;
        }

        @Override
        protected String[] doInBackground(String... strings) {
            try {
                System.out.println("CONNESSIONE" + "   " + strings[0]);
                Document doc = HttpRequest.GET_nolega(strings[0], "Ultime news su Serie A TIM");

                Elements partite = doc.select("section[class=risultati]").get(0).children();
                partite.remove(0);
                partite.remove(0);
                partite.remove(0);
                partite.remove(0);

                String[] giornata = new String[partite.size()];

                final String verona = "Hellas Verona";
                for (int i = 0; i < partite.size(); i++) {
                    Element partita = partite.get(i);
                    StringBuilder map = new StringBuilder();
                    String temp = partita.select("h4[class=nomesquadra]").get(0).text();
                    map.append(temp.equals(verona) ? "Verona" : temp);
                    map.append("<_>");
                    temp = partita.select("h4[class=nomesquadra]").get(1).text();
                    map.append(temp.equals(verona) ? "Verona" : temp);
                    map.append("<_>");
                    temp = partita.select("div[class=col-xs-6 risultatosx]").select("span").text();
                    if (temp.equals("-")) {
                        map.append(temp);
                        map.append("<_>");
                    }
                    else {
                        map.append(temp);
                        map.append(" - ");
                        map.append(partita.select("div[class=col-xs-6 risultatodx]").select("span").text());
                        map.append("<_>");
                    }
                    map.append(partita.select("div[class=datipartita]").select("span").get(0).text());

                    giornata[i] = map.toString();
                }

                return giornata;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (fragment.getActivity() != null) {
                ((Serie_A) fragment.getActivity()).update(position, result);
                List<HashMap<String, String>> partite = new ArrayList<>();

                for (String g : result) {
                    HashMap<String, String> map = new HashMap<>();
                    StringTokenizer st = new StringTokenizer(g, "<_>");
                    map.put("casa", st.nextToken());
                    if (map.get("casa").equals("Chievoverona")) {
                        map.put("casa", "Chievo");
                    }
                    map.put("trasf", st.nextToken());
                    if (map.get("trasf").equals("Chievoverona")) {
                        map.put("trasf", "Chievo");
                    }
                    map.put("risultato", st.nextToken());
                    if(st.hasMoreTokens()) {
                        map.put("data", st.nextToken());
                    } else {
                        map.put("data", "00/00/00 00:00");
                    }

                    partite.add(map);
                }

                Collections.sort(partite, new Comparator<HashMap<String, String>>() {
                    @Override
                    public int compare(HashMap<String, String> p1, HashMap<String, String> p2) {
                        DateFormat format = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.ITALIAN);
                        try {
                            return format.parse(p1.get("data")).compareTo(format.parse(p2.get("data")));
                        } catch (ParseException e) {
                            return 0;
                        }
                    }
                });

                gpb.setVisibility(View.GONE);
                mAdapter adapter = new mAdapter(fragment.getContext(), partite, R.layout.cal_serie_a_list, new String[]{"casa", "trasf", "risultato", "data"},
                        new int[]{R.id.casa, R.id.trasf, R.id.result, R.id.data});
                listView.setAdapter(adapter);
            }
        }
    }

    private int getDrawable(Context context, String name) {
        return context.getResources().getIdentifier(name,
                "drawable", context.getPackageName());
    }
}
