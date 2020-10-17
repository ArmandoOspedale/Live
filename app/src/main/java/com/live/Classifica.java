package com.live;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.StateListDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class Classifica extends AppCompatActivity {

    String comp;
    private int tipo;
    String[] codici;
    String[] squadre;
    private Opzioni_lega opzioni;
    TextView txt;
    private ArrayList<HashMap<String, String>> normale;
    private ArrayList<HashMap<String, String>> sculo;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.action_class);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
        setContentView(R.layout.classifica);

        Intent i = getIntent();
        comp = i.getStringExtra("comp");
        tipo = i.getIntExtra("tipo", 0);
        squadre = i.getStringArrayExtra("squadre");
        codici = i.getStringArrayExtra("codici");
        opzioni = (Opzioni_lega) HttpRequest.getObject(Classifica.this, HttpRequest.lega);

        txt = new TextView(Classifica.this);
        txt.setGravity(Gravity.CENTER_HORIZONTAL);
        txt.setPadding(50, 15, 50, 15);
        txt.setBackgroundColor(Color.rgb(18, 116, 175));
        txt.setTextColor(Color.WHITE);
        txt.setTextSize(16);

        new DownloadClass().execute(comp);
    }

    @SuppressLint("StaticFieldLeak")
    private class DownloadClass extends AsyncTask<String, Integer, ArrayList<HashMap<String, String>>> {

        @Override
        protected void onPreExecute() {
            if (normale != null) cancel(true);
        }

        @Override
        protected ArrayList<HashMap<String, String>> doInBackground(String... param) {

            ArrayList<HashMap<String, String>> feedList = new ArrayList<>();

            try {
                Document doc = HttpRequest.GET("/classifica?id=" + param[0], "breadcrumb");

                JSONArray json_squadre = new JSONArray(doc.select("script[id=gyxmm02]").toString().split("jp\\('")[1].split("'\\)\\);")[0]);
                if (tipo == 2 || tipo == 3) {

                    for(int i = 0; i < json_squadre.length(); i++) {

                        JSONObject squadra = new JSONObject(json_squadre.getString(i));
                        squadra.put("id", HttpRequest.getNomeSquadra(squadra.getString("id"), codici, squadre));
                        HashMap<String, String> map = new HashMap<>();
                        map.put("NomeSquadra", squadra.getString("id"));
                        map.put("Punti", squadra.getString("p"));
                        map.put("Giocate", squadra.getString("g"));
                        map.put("Totale", squadra.getString("s_p"));

                        if(squadra.getInt("pos") == 0) {
                            feedList.add(map);
                        } else {
                            feedList.add(squadra.getInt("pos") - 1, map);
                        }
                    }
                } else {
                    ArrayList<HashMap<String, String>> feedListA = new ArrayList<>();
                    ArrayList<HashMap<String, String>> feedListB = new ArrayList<>();
                    for(int i = 0; i < json_squadre.length(); i++) {

                        JSONObject squadra = new JSONObject(json_squadre.getString(i));
                        squadra.put("id", HttpRequest.getNomeSquadra(squadra.getString("id"), codici, squadre));
                        HashMap<String, String> map = new HashMap<>();
                        map.put("NomeSquadra", squadra.getString("id"));
                        map.put("Punti", squadra.getString("p"));
                        map.put("Giocate", squadra.getString("g"));
                        map.put("Vinte", squadra.getString("v"));
                        map.put("Pareggi", squadra.getString("n"));
                        map.put("Perse", squadra.getString("pr"));
                        map.put("Golfatti", squadra.getString("gf"));
                        map.put("Golsubiti", squadra.getString("gs"));
                        map.put("Totale", squadra.getString("s_p"));

                        if(squadra.getInt("pos") == 0) {
                            if("A".equals(squadra.getString("gr")))
                                feedListA.add(map);
                            else
                                feedListB.add(map);
                        } else {
                            if("A".equals(squadra.getString("gr")))
                                feedListA.add(squadra.getInt("pos") - 1, map);
                            else
                                feedListB.add(squadra.getInt("pos") - 1, map);
                        }
                    }
                    if(!feedListB.isEmpty()) {
                        HashMap<String, String> gruppoA = new HashMap<>();
                        gruppoA.put("NomeSquadra", "Gruppo A");
                        gruppoA.put("Punti", "");
                        gruppoA.put("Giocate", "");
                        gruppoA.put("Vinte", "");
                        gruppoA.put("Pareggi", "");
                        gruppoA.put("Perse", "");
                        gruppoA.put("Golfatti", "");
                        gruppoA.put("Golsubiti", "");
                        gruppoA.put("Totale", "");
                        feedList.add(gruppoA);
                    }
                    feedList.addAll(feedListA);
                    if(!feedListB.isEmpty()) {
                        HashMap<String, String> gruppoB = new HashMap<>();
                        gruppoB.put("NomeSquadra", "Gruppo B");
                        gruppoB.put("Punti", "");
                        gruppoB.put("Giocate", "");
                        gruppoB.put("Vinte", "");
                        gruppoB.put("Pareggi", "");
                        gruppoB.put("Perse", "");
                        gruppoB.put("Golfatti", "");
                        gruppoB.put("Golsubiti", "");
                        gruppoB.put("Totale", "");
                        feedList.add(gruppoB);
                        feedList.addAll(feedListB);
                    }
                }
            }
            catch (Exception e) {e.printStackTrace();}
            return feedList;
        }

        @Override
        protected void onPostExecute(ArrayList<HashMap<String, String>> result) {
            normale = result;
            visualizza();
        }

        @Override
        protected void onCancelled() {
            visualizza();
        }

        private void visualizza() {
            (findViewById(R.id.sculo)).setVisibility(View.GONE);
            (findViewById(R.id.portrait)).setVisibility(View.VISIBLE);
            ListView list = findViewById(R.id.listView);
            list.setSelector(new StateListDrawable());
            if (tipo == 1) {
                txt.setText(R.string.view_prob_class);
                txt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        new SculoClass().execute(comp);
                    }
                });
                if (list.getFooterViewsCount() == 0) list.addFooterView(txt);
            }

            SimpleAdapter simpleAdapter = new SimpleAdapter(Classifica.this, normale, R.layout.classlist, new String[]{"NomeSquadra", "Giocate", "Punti", "Totale"}, new int[]{R.id.NomeSquadra, R.id.Giocate, R.id.Punti, R.id.Totale});
            list.setAdapter(simpleAdapter);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class SculoClass extends AsyncTask<String, Integer, ArrayList<HashMap<String, String>>> {

        @Override
        protected void onPreExecute() {
            if (sculo != null) {
                cancel(true);
            } else {
                (findViewById(R.id.portrait)).setVisibility(View.GONE);
                (findViewById(R.id.listView)).setVisibility(View.GONE);
                (findViewById(R.id.pre_execute)).setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected ArrayList<HashMap<String, String>> doInBackground(String... param) {

            ArrayList<HashMap<String, String>> feedList = new ArrayList<>();

            try {
                Document doc = HttpRequest.GET("/calendario?id=" + comp, "breadcrumb");

                JSONArray giornate = new JSONObject(new String(Base64.decode(doc.select("script[id=s001]").toString().split("dp\\('")[1].split("'\\)\\);")[0], Base64.DEFAULT))).getJSONObject("data").getJSONObject("calendario").getJSONArray("c_inc");
                for (int i = 0; i < giornate.length(); i++) {
                    if (!giornate.getJSONObject(i).getBoolean("cal")) {
                        giornate.remove(i);
                        i--;
                    }
                }

                String[][] dati = new String[squadre.length][giornate.length()];
                int[][] risultati = new int[squadre.length][giornate.length()];
                for (int i = 0; i < giornate.length(); i++) {
                    JSONArray incontri = giornate.getJSONObject(i).getJSONArray("inc");
                    JSONArray matches = new JSONArray();
                    int[] r = new int[incontri.length() * 2];
                    for (int j = 0; j < incontri.length(); j++) {
                        String[] temp = incontri.getJSONObject(j).getString("res").split("-");
                        r[j * 2] = Integer.parseInt(temp[0]);
                        r[j * 2 + 1] = Integer.parseInt(temp[1]);
                        JSONObject a = new JSONObject();
                        a.put("id", incontri.getJSONObject(j).getString("id_a"));
                        a.put("p", incontri.getJSONObject(j).getString("p_a"));
                        matches.put(a);
                        JSONObject b = new JSONObject();
                        b.put("id", incontri.getJSONObject(j).getString("id_b"));
                        b.put("p", incontri.getJSONObject(j).getString("p_b"));
                        matches.put(b);
                    }

                    for (int j = 0; j < matches.length(); j++) {
                        int k = 0;
                        boolean found = false;
                        while (!found) {
                            if (matches.getJSONObject(j).getString("id").equals(codici[k])) {
                                k--;
                                found = true;
                            }
                            k++;
                        }
                        dati[k][i] = matches.getJSONObject(j).getString("p");

                        if (j % 2 == 0) {
                            if (r[j] > r[j + 1]) {
                                risultati[k][i] = 0;
                            } else if (r[j] < r[j + 1]) {
                                risultati[k][i] = 2;
                            } else {
                                risultati[k][i] = 1;
                            }
                        } else {
                            if (r[j] > r[j - 1]) {
                                risultati[k][i] = 0;
                            } else if (r[j] < r[j - 1]) {
                                risultati[k][i] = 2;
                            } else {
                                risultati[k][i] = 1;
                            }
                        }
                    }
                }

                for (int i = 0; i < squadre.length; i++) {
                    int punti_veri = 0;
                    double punti_prob = 0;
                    HashMap<String, String> map = new HashMap<>();
                    map.put("NomeSquadra", squadre[i]);

                    for (int g = 0; g < giornate.length(); g++) {
                        double v = 0;
                        double n = 0;
                        double s = 0;
                        for (int j = 0; j < squadre.length; j++) {
                            if (j != i) {
                                switch (risultato(Double.parseDouble(dati[i][g]), Double.parseDouble(dati[j][g]))) {
                                    case 0:
                                        v++;
                                        break;
                                    case 1:
                                        n++;
                                        break;
                                    case 2:
                                        s++;
                                        break;
                                }
                            }
                        }
                        switch (risultati[i][g]) {
                            case 0:
                                punti_veri = punti_veri + 3;
                                break;
                            case 1:
                                punti_veri = punti_veri + 1;
                                break;
                            case 2:
                                break;
                        }
                        punti_prob = punti_prob + 3 * (v / (v + n + s)) + (n / (v + n + s));
                    }
                    map.put("Pv", String.valueOf(punti_veri));
                    map.put("NotRounded", String.valueOf(punti_prob));
                    double floor = Math.floor(punti_prob);
                    punti_prob = ((punti_prob - floor) > 0.4d && (punti_prob - floor) < 0.6d) ? floor + 0.5d :
                            ((punti_prob - floor) < 0.4d) ? floor : floor + 1d;
                    map.put("Pp", String.valueOf(punti_prob));
                    double temp = ((double) punti_veri) - punti_prob;
                    map.put("Diff", temp <= 0 ? String.valueOf(temp) : "+" + temp);
                    feedList.add(map);
                }

                Collections.sort(feedList, new Comparator<HashMap<String, String>>() {
                    @Override
                    public int compare(HashMap<String, String> t1, HashMap<String, String> t2) {
                        return Double.compare(Double.parseDouble(t2.get("NotRounded")), Double.parseDouble(t1.get("NotRounded")));
                    }
                });
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return feedList;
        }

        @Override
        protected void onPostExecute(ArrayList<HashMap<String, String>> result) {
            sculo = result;
            (findViewById(R.id.portrait)).setVisibility(View.GONE);
            (findViewById(R.id.listView)).setVisibility(View.VISIBLE);
            (findViewById(R.id.pre_execute)).setVisibility(View.GONE);
            visualizza();
        }

        @Override
        protected void onCancelled() {
            visualizza();
        }

        private void visualizza() {
            (findViewById(R.id.portrait)).setVisibility(View.GONE);
            (findViewById(R.id.sculo)).setVisibility(View.VISIBLE);
            ListView list = findViewById(R.id.listView);
            list.setSelector(new StateListDrawable());
            txt.setText(R.string.norm_class);
            txt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new DownloadClass().execute(comp);
                }
            });

            SimpleAdapter simpleAdapter = new SimpleAdapter(Classifica.this, sculo, R.layout.classlist, new String[]{"NomeSquadra", "Pv", "Pp", "Diff"}, new int[]{R.id.NomeSquadra, R.id.Giocate, R.id.Punti, R.id.Totale});
            list.setAdapter(simpleAdapter);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE && tipo != 2 && tipo != 3) {
            (findViewById(R.id.portrait)).setVisibility(View.GONE);
            (findViewById(R.id.sculo)).setVisibility(View.GONE);
            (findViewById(R.id.landscape)).setVisibility(View.VISIBLE);
            ListView list = findViewById(R.id.listView);
            list.setSelector(new StateListDrawable());
            list.removeFooterView(txt);

            SimpleAdapter simpleAdapter = new SimpleAdapter(Classifica.this, normale, R.layout.landlist,
                    new String[]{"NomeSquadra", "Punti", "Giocate", "Vinte", "Pareggi", "Perse", "Golfatti", "Golsubiti", "Totale"},
                    new int[]{R.id.NomeSquadra, R.id.Punti, R.id.Giocate, R.id.Vinte, R.id.Pareggi, R.id.Perse, R.id.Golfatti, R.id.Golsubiti, R.id.Totale});
            list.setAdapter(simpleAdapter);
        } else {
            (findViewById(R.id.landscape)).setVisibility(View.GONE);
            (findViewById(R.id.sculo)).setVisibility(View.GONE);
            (findViewById(R.id.portrait)).setVisibility(View.VISIBLE);
            ListView list = findViewById(R.id.listView);
            list.setSelector(new StateListDrawable());
            if (tipo == 1) {
                txt.setText(R.string.prob_class);
                txt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        new SculoClass().execute(comp);
                    }
                });
                if (list.getFooterViewsCount() == 0) list.addFooterView(txt);
            }

            SimpleAdapter simpleAdapter = new SimpleAdapter(Classifica.this, normale, R.layout.classlist, new String[]{"NomeSquadra", "Giocate", "Punti", "Totale"}, new int[]{R.id.NomeSquadra, R.id.Giocate, R.id.Punti, R.id.Totale});
            list.setAdapter(simpleAdapter);
        }
    }

    private int risultato (double casa, double trasf) {
        int [] risultato = new int [2];
        //risultato[0] = 0; risultato[1] = 0;
        if (casa >= opzioni.base) {
            double diff = casa - opzioni.base;
            while (diff >= 0) {
                risultato[0]+=1;
                diff = diff - opzioni.fascia;
            }
        }
        if (trasf >= opzioni.base) {
            double diff = trasf - opzioni.base;
            while (diff >= 0) {
                risultato[1]+=1;
                diff = diff - opzioni.fascia;
            }
        }

        if (opzioni.pareggio_checked) {
            if (risultato[0] == 0 && risultato[1] == 0 && casa >= trasf + opzioni.pareggio_value) {
                risultato[0]++;
            }
            if (risultato[0] == 0 && risultato[1] == 0 && trasf >= casa + opzioni.pareggio_value) {
                risultato[1]++;
            }
        }

        if (opzioni.intorno_checked) {
            if (risultato[0] == risultato[1] && risultato[0]!=0 && casa >= trasf + opzioni.intorno_value) {
                risultato[0]++;
            }
            if (risultato[0] == risultato[1] && risultato[0]!=0 && trasf >= casa + opzioni.intorno_value) {
                risultato[1]++;
            }
        }

        if (opzioni.differenza_checked) {
            if (casa >= trasf + opzioni.differenza_value) {
                risultato[0]++;
            } else if (trasf >= casa + opzioni.differenza_value) {
                risultato[1]++;
            }
        }
        if (risultato[0] > risultato[1]) {
            return 0;
        } else if (risultato[0] < risultato[1]) {
            return 2;
        } else {
            return 1;
        }
    }

    @Override
     public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
