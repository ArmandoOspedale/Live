package com.live;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class PartitaLive_Fragment extends Fragment {

    private SwipeRefreshLayout srl;
    private ListView listA;
    private ListView listB;
    private WebView webview;
    private ListView eventi;
    private String partita;
    private int idA;
    private int idB;
    String giornata;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        assert getArguments() != null;
        final int position = getArguments().getInt("position");
        partita = getArguments().getString("partita");
        idA = getArguments().getInt("teamA");
        idB = getArguments().getInt("teamB");
        giornata = getArguments().getString("giornata");

        ViewGroup rootView;
        switch (position) {
            case 0:
                rootView = (ViewGroup) inflater.inflate(R.layout.live_fragment, container, false);

                srl = rootView.findViewById(R.id.srl);
                srl.setColorSchemeColors(Color.rgb(18, 116, 175));

                srl.setOnRefreshListener(() -> {
                    srl.setRefreshing(true);
                    new Partita().execute(position);
                });

                listA = rootView.findViewById(R.id.listA);
                listB = rootView.findViewById(R.id.listB);
                listA.setSelector(android.R.color.transparent);
                listB.setSelector(android.R.color.transparent);

                final boolean[] state = new boolean[] {false, false};
                listA.setOnScrollListener(new AbsListView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(AbsListView absListView, int i) {}

                    @Override
                    public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                        state[0] = canScrollUp(listA);
                        srl.setEnabled(state[0] && state[1]);
                    }
                });
                listB.setOnScrollListener(new AbsListView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(AbsListView absListView, int i) {}

                    @Override
                    public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                        state[1] = canScrollUp(listB);
                        srl.setEnabled(state[0] && state[1]);
                    }
                });

                new Partita().execute(position);
                return rootView;
            case 1:
                srl = new SwipeRefreshLayout(Objects.requireNonNull(getActivity()));

                eventi = new ListView(getActivity());
                eventi.setSelector(android.R.color.transparent);

                srl.addView(eventi);
                srl.setColorSchemeColors(Color.rgb(18, 116, 175));

                srl.setOnRefreshListener(() -> {
                    srl.setRefreshing(true);
                    new Partita().execute(position);
                });

                new Partita().execute(position);
                return srl;
            case 2:
                srl = new SwipeRefreshLayout(Objects.requireNonNull(getActivity()));

                webview = new WebView(getActivity());

                srl.addView(webview);
                srl.setColorSchemeColors(Color.rgb(18, 116, 175));

                srl.setOnRefreshListener(() -> {
                    srl.setRefreshing(true);
                    new Partita().execute(position);
                });

                new Partita().execute(position);
                return srl;
        }

        return null;
    }

    @SuppressLint("StaticFieldLeak")
    private class Partita extends AsyncTask<Integer, Void, String[]> {

        int position;

        @Override
        protected String[] doInBackground(Integer... ints) {
            try {
                position = ints[0];
                String votiA = HttpRequest.GET_nodocument("https://www.fantacalcio.it/api/live/" + idA + "?g=" + giornata + "&i=15");
                String votiB = HttpRequest.GET_nodocument("https://www.fantacalcio.it/api/live/" + idB + "?g=" + giornata + "&i=15");
                String eventi = HttpRequest.GET_nodocument("https://www.fantacalcio.it/api/live?g=" + giornata + "&a=" + idA + "&b=" + idB + "&i=15");

                return new String[] {
                        HttpRequest.GET_nodocument("https://m.tuttomercatoweb.com" + partita), votiA, votiB, eventi};
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String[] strings) {
            Document doc = Jsoup.parse(strings[0]);

            Elements rows = doc.select("tr[class=live_cronaca]");
            StringBuilder builder = new StringBuilder();
            builder.append("<table>");
            for (Element r : rows) {
                builder.append(r.toString());
            }
            builder.append("</table");

            ((TextView) Objects.requireNonNull(getActivity()).findViewById(R.id.result)).setText(doc.select("td[style=width: 25%; text-align: center; font-size:180%; font-weight: bold; color: #086203;]").text());

            Element temp = doc.select("td[style=text-align: center; font-weight:bold;]").get(0);
            String tempo;
            if (temp.children().size() == 2) {
                tempo = temp.child(1).text();
                temp.child(1).remove();
            } else {
                tempo = temp.child(0).text();
                temp.child(0).remove();
            }
            if ((tempo.toLowerCase().contains("primo") || tempo.toLowerCase().contains("secondo")) && !tempo.toLowerCase().contains("fine")) {
                String[] temp2 = temp.text().split("'");
                int minuto = Integer.parseInt(temp2[0]);
                if (tempo.toLowerCase().contains("secondo")) minuto = minuto + 45;
                String text = minuto + "'" + temp2[1];
                ((TextView) getActivity().findViewById(R.id.minuto)).setText(text);
            } else {
                if (tempo.toLowerCase().contains("primo")) {
                    String HT = "HT";
                    ((TextView) getActivity().findViewById(R.id.minuto)).setText(HT);
                } else {
                    String FT = "FT";
                    ((TextView) getActivity().findViewById(R.id.minuto)).setText(FT);
                }
            }

            if (srl.isRefreshing()) srl.setRefreshing(false);
            List<HashMap<String, String>> casa = new ArrayList<>();
            List<HashMap<String, String>> trasf = new ArrayList<>();
            List<HashMap<String, String>> events = new ArrayList<>();
            try {
                JSONArray ja = new JSONArray(strings[1]);
                for (int i = 0; i < ja.length(); i++) {
                    HashMap<String, String> map = new HashMap<>();
                    JSONObject jo = ja.getJSONObject(i);
                    map.put("nome", jo.getString("nome"));
                    map.put("ruolo", jo.getString("ruolo"));
                    map.put("voto", jo.getString("voto"));
                    map.put("evento", jo.getString("evento"));
                    casa.add(map);
                }
                ja = new JSONArray(strings[2]);
                for (int i = 0; i < ja.length(); i++) {
                    HashMap<String, String> map = new HashMap<>();
                    JSONObject jo = ja.getJSONObject(i);
                    map.put("nome", jo.getString("nome"));
                    map.put("ruolo", jo.getString("ruolo"));
                    map.put("voto", jo.getString("voto"));
                    map.put("evento", jo.getString("evento"));
                    trasf.add(map);
                }
                ja = new JSONObject(strings[3]).getJSONArray("data");
                HashMap<String, String> info = new HashMap<>();
                info.put("info", "tempo");
                info.put("tempo", "1");
                info.put("punteggio",  "0-0");
                events.add(info);
                for (int j = 0; j < ja.length(); j++) {
                    JSONArray valori = ja.getJSONArray(j);
                    HashMap<String, String> map = new HashMap<>();
                    map.put("minuto", valori.getString(0));
                    map.put("tempo", valori.getString(6));
                    if (!Objects.equals(info.get("tempo"), valori.getString(6))) {
                        info = new HashMap<>();
                        info.put("info", "tempo");
                        info.put("tempo", valori.getString(6));
                        info.put("punteggio", valori.getString(3));
                        events.add(info);
                    }
                    map.put("squadra", valori.getString(1));
                    map.put("bonus", valori.getString(2));
                    map.put("nome", valori.getString(4));
                    events.add(map);
                    if (!Objects.equals(info.get("punteggio"), valori.getString(3))) {
                        info = new HashMap<>();
                        info.put("info", "punteggio");
                        info.put("tempo", valori.getString(6));
                        info.put("punteggio", valori.getString(3));
                        events.add(info);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            switch (position) {
                case 0:
                    mAdapter adapterA = new mAdapter(getActivity(), casa);
                    listA.setAdapter(adapterA);

                    mAdapter adapterB = new mAdapter(getActivity(), trasf);
                    listB.setAdapter(adapterB);
                    break;
                case 1:
                    mAdapter2 adapter = new mAdapter2(getActivity(), events);
                    eventi.setAdapter(adapter);
                    break;
                case 2:
                    webview.loadDataWithBaseURL("http:///android_asset/", builder.toString(), "text/html", "utf-8", null);
                    break;
            }
        }
    }

    @SuppressWarnings("all")
    private class mAdapter extends BaseAdapter {

        Context cxt;
        List<HashMap<String, String>> dati;
        String [] bonus = {"amm", "esp_s", "golfatto_s", "golsubito_s", "assist_s", "assistf_s", "rigoreparato_s",
                "rigoresbagliato_s", "rigoresegnato_s", "autogol_s", "golvittoria_s", "golpareggio_s",
                "", "uscito_s", "entrato_s", "golannullatovar_s", "infortunato_s", "", "", "assistmovimentolvbasso_s",
                "assistmovimentolvmedio_s", "assistmovimentolvalto_s", "assistfermolvbasso_s", "assistfermolvmedio_s",
                "assistfermolvalto_s"};

        mAdapter (Context c, List<HashMap<String, String>> d) {
            cxt = c;
            dati = d;
        }

        @Override
        public int getCount() {
            return dati.size();
        }

        @Override
        public Object getItem(int i) {
            return dati.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View v = getActivity().getLayoutInflater().inflate(R.layout.live_list, null);

            TextView txt = (TextView) v.findViewById(R.id.Ruolo);
            txt.setText(dati.get(i).get("ruolo"));
            switch (dati.get(i).get("ruolo")) {
                case "P": txt.setBackgroundColor(Color.rgb(255, 225, 15)); break;
                case "D": txt.setBackgroundColor(Color.rgb(0,128,10)); break;
                case "C": txt.setBackgroundColor(Color.rgb(5,29,192)); break;
                case "A": txt.setBackgroundColor(Color.RED); break;
                case "ALL": txt.setBackgroundColor(Color.LTGRAY); break;
            }

            txt = (TextView) v.findViewById(R.id.Nome);
            txt.setText(dati.get(i).get("nome"));

            txt = (TextView) v.findViewById(R.id.Voto);
            txt.setText(dati.get(i).get("voto"));

            LinearLayout layout = (LinearLayout) v.findViewById(R.id.bonus);

            if (!dati.get(i).get("evento").equals("")) {
                String[] eventi_player = dati.get(i).get("evento").split(",");
                for (int j = 0; j < eventi_player.length; j++) {
                    ImageView image = new ImageView(cxt);
                    image.setPadding(3, 3, 3, 3);
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT, 1);
                    layoutParams.gravity = Gravity.CENTER_VERTICAL;
                    image.setLayoutParams(layoutParams);
                    image.setImageResource(getDrawable(cxt, bonus[Integer.parseInt(eventi_player[j]) - 1]));
                    layout.addView(image, j);
                }
                layout.setVisibility(View.VISIBLE);
            }

            return v;
        }
    }

    @SuppressWarnings("all")
    private class mAdapter2 extends BaseAdapter {

        Context cxt;
        List<HashMap<String, String>> dati;
        String [] bonus = {"amm", "esp_s", "golfatto_s", "golsubito_s", "assist_s", "assistf_s", "rigoreparato_s",
                "rigoresbagliato_s", "rigoresegnato_s", "autogol_s", "golvittoria_s", "golpareggio_s",
                "", "uscito_s", "entrato_s", "golannullatovar_s", "infortunato_s", "", "", "assistmovimentolvbasso_s",
                "assistmovimentolvmedio_s", "assistmovimentolvalto_s", "assistfermolvbasso_s", "assistfermolvmedio_s",
                "assistfermolvalto_s"};

        mAdapter2 (Context c, List<HashMap<String, String>> d) {
            cxt = c;
            dati = d;
        }

        @Override
        public int getCount() {
            return dati.size();
        }

        @Override
        public Object getItem(int i) {
            return dati.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            HashMap<String, String> map = (HashMap<String, String>) getItem(i);
            if (map.containsKey("info")) {
                if (map.get("info").equals("tempo")) {
                    TextView t = new TextView(cxt);
                    t.setTextSize(22);
                    t.setTextColor(Color.WHITE);
                    t.setBackgroundColor(Color.rgb(18, 116, 175));
                    t.setGravity(Gravity.CENTER_HORIZONTAL);
                    t.setText(map.get("tempo") + "° TEMPO");

                    return t;
                } else {
                    View v = getActivity().getLayoutInflater().inflate(R.layout.punteggio, null);

                    if ((i % 2) == 1) v.setBackgroundColor(Color.rgb(239, 239, 239));
                    ((TextView) v.findViewById(R.id.punteggio)).setText(map.get("punteggio"));

                    return v;
                }
            } else {
                View v;
                if (map.get("squadra").equals("1")) {
                    v = getActivity().getLayoutInflater().inflate(R.layout.left, null);
                } else {
                    v = getActivity().getLayoutInflater().inflate(R.layout.right, null);
                }

                if ((i % 2) == 1) v.setBackgroundColor(Color.rgb(239, 239, 239));

                TextView txt = (TextView) v.findViewById(R.id.min);
                int min = Integer.valueOf(map.get("minuto").substring(0, 2).replaceAll("\\.", ""));
                String tempo = map.get("tempo");
                if (tempo.equals("1") && min > 45) {
                    txt.setText("45+" + String.valueOf(min - 45) + "'");
                } else if (tempo.equals("2") && min > 90) {
                    txt.setText("90+" + String.valueOf(min - 90) + "'");
                } else {
                    txt.setText(String.valueOf(min) + "'");
                }

                ((ImageView) v.findViewById(R.id.icona)).setImageResource(getDrawable(cxt, bonus[Integer.valueOf(map.get("bonus")) - 1]));

                txt = (TextView) v.findViewById(R.id.nome);
                txt.setText(map.get("nome"));

                return v;
            }
        }
    }

    private boolean canScrollUp (View view) {
        if (view instanceof AbsListView) {
            final AbsListView absListView = (AbsListView) view;
            return !(absListView.getChildCount() > 0
                    && (absListView.getFirstVisiblePosition() > 0 || absListView
                    .getChildAt(0).getTop() < absListView.getPaddingTop()));
        } else {
            return !(view.getScrollY() > 0);
        }
    }

    private int getDrawable(Context context, String name) {
        return context.getResources().getIdentifier(name,
                "drawable", context.getPackageName());
    }
}
