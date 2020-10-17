package com.live;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.StateListDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class ClassificaGiornata extends AppCompatActivity {

    String comp;
    //private int tipo;
    String[] codici;
    String[] squadre;
    private Opzioni_lega opzioni;
    private ArrayList<HashMap<String, String>> normale;
    private SwipeRefreshLayout srl;
    //private final String arrow_up = "↑";
    //private final String arrow_down = "↓";
    //private final String equal = "=";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.action_class_g);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
        setContentView(R.layout.classifica_g);
        srl = findViewById(R.id.srl);
        srl.setColorSchemeColors(Color.rgb(18, 116, 175));

        srl.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                srl.setRefreshing(true);
                new DownloadClass().execute(comp);
            }
        });

        Intent i = getIntent();
        comp = i.getStringExtra("comp");
        //tipo = i.getIntExtra("tipo", 0);
        squadre = i.getStringArrayExtra("squadre");
        codici = i.getStringArrayExtra("codici");
        opzioni = (Opzioni_lega) HttpRequest.getObject(ClassificaGiornata.this, HttpRequest.lega);

        new DownloadClass().execute(comp);
    }

    @SuppressLint("StaticFieldLeak")
    private class DownloadClass extends AsyncTask<String, Integer, ArrayList<HashMap<String, String>>> {

        @Override
        protected ArrayList<HashMap<String, String>> doInBackground(String... param) {

            ArrayList<HashMap<String, String>> feedList = new ArrayList<>();

            try {
                String [] partite = downloadPartite();
                for (String codice : codici) {
                    URL url = new URL("https://leghe.fantacalcio.it/servizi/V1_LegheLive/Visualizza?alias_lega=" + HttpRequest.lega +
                            "&id_comp=" + comp + "&id_squadra=" + codice);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                    connection.setRequestProperty("app_key", "4ab27d6de1e92c810c6d4efc8607065a735b917f");
                    connection.setRequestProperty("cookie", HttpRequest.cookie);

                    BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    JSONObject json = new JSONObject(rd.readLine());
                    JSONArray json_data = json.getJSONObject("data").getJSONArray("formazioni").getJSONObject(0).getJSONArray("sq");
                    Giocatore[] casa = downloadLive(partite, json_data.getJSONObject(0));
                    Giocatore[] trasf = downloadLive(partite, json_data.length() > 1 ? json_data.getJSONObject(1) : null);

                    double c = calcola(casa);
                    double f = calcola(trasf);

                    HashMap<String, String> map = new HashMap<>();
                    map.put("NomeSquadra", casa[0].getFanta());
                    //map.put("Posizione", equal);
                    map.put("Punti", String.valueOf(c));
                    if (!feedList.contains(map))
                        feedList.add(map);
                    HashMap<String, String> map2 = new HashMap<>();
                    map2.put("NomeSquadra", trasf[0].getFanta());
                    //map2.put("Posizione", equal);
                    map2.put("Punti", String.valueOf(f));
                    if (!feedList.contains(map2))
                        feedList.add(map2);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            Collections.sort(feedList, new Comparator<HashMap<String, String>>() {
                @Override
                public int compare(HashMap<String, String> stringStringHashMap, HashMap<String, String> t1) {
                    return Double.compare(Double.parseDouble(t1.get("Punti")),
                            Double.parseDouble(stringStringHashMap.get("Punti")));
                }
            });
            return feedList;
        }

        @Override
        protected void onPostExecute(ArrayList<HashMap<String, String>> result) {
            normale = result;
            visualizza();
        }

        private void visualizza() {
            if (srl.isRefreshing()) srl.setRefreshing(false);

            ListView list = findViewById(R.id.listView);
            list.setSelector(new StateListDrawable());

            SimpleAdapter simpleAdapter = new SimpleAdapter(ClassificaGiornata.this, normale, R.layout.classlist_g, new String[]{"NomeSquadra", "Posizione", "Punti"}, new int[]{R.id.NomeSquadra, R.id.Posizione, R.id.Punti});
            list.setAdapter(simpleAdapter);
        }
    }

    private Giocatore [] downloadLive (String [] cal, JSONObject formazione) {
        String f = "";

        try {
            f = HttpRequest.getNomeSquadra(formazione.getString("id"), codici, squadre);
            JSONArray pls = formazione.getJSONArray("pl");

            String [] bonus = {"amm", "esp_s", "golfatto_s", "golsubito_s", "assist_s", "assistf_s", "rigoreparato_s",
                    "rigoresbagliato_s", "rigoresegnato_s", "autogol_s", "golvittoria_s", "golpareggio_s",
                    "portiereimbattuto_s", "uscito_s", "entrato_s", "golannullatovar_s", "infortunato_s", "", "", "assistmovimentolvbasso_s",
                    "assistmovimentolvmedio_s", "assistmovimentolvalto_s", "assistfermolvbasso_s", "assistfermolvmedio_s",
                    "assistfermolvalto_s"};

            Giocatore[] squadra = new Giocatore[pls.length()];

            for (int i = 0; i < pls.length(); i++) {
                JSONObject pl = pls.getJSONObject(i);
                String n = pl.getString("n");
                char r = pl.getString("r").charAt(0);
                String s = pl.getString("t").toUpperCase();

                int j = -1;
                boolean found = false;
                while (!found) {
                    j++;
                    if (cal[j].contains(s)) {
                        found = true;
                    }
                }
                double v;
                String voto = pl.getString("vt");
                if (voto.equals("56.0") || voto.equals("55.0")) voto = "0.0";
                JSONArray b = pl.getJSONArray("b");
                String status;
                if (voto.contains(",")) {
                    voto = voto.replace(",", ".");
                }
                if ((voto.equals("0.0") && cal[j].contains("grey")) || (voto.equals("0.0") && (cal[j].contains("status") || cal[j].contains("orange")))) {
                    v = 0;
                    if (cal[j].contains("status") || cal[j].contains("orange")) {
                        status = "in panchina";
                    } else {
                        status = "finale";
                    }
                } else if (voto.equals("0.0") && !cal[j].contains("grey")) {
                    v = 6;
                    status = "";
                } else {
                    v = Double.parseDouble(voto);
                    if (cal[j].contains("status") || cal[j].contains("orange")) {
                        status = "in campo";
                    } else if (cal[j].contains("grey")) {
                        status = "finale";
                    } else {
                        status = "titolare";
                    }
                }
                List<String> temp = new ArrayList<>();
                //for (int k = 0; k < b.length(); k++) {
                //    if(b.getInt(k) > 0) {
                //        for (int l = 0; l < b.getInt(k); l++) {
                //            temp.add(bonus[k]);
                //        }
                //    }
                //}
                for (int k = 0; k < b.length(); k++) {
                    if(b.getInt(k) > 0) {
                        temp.add(bonus[b.getInt(k) - 1]);
                    }
                }

                switch (pl.getString("s")) {
                    case "E":
                        temp.add(bonus[14]);
                        break;
                    case "U":
                        temp.add(bonus[13]);
                        break;
                    case "":
                    default:
                        break;
                }
                String[] stockArr = new String[temp.size()];
                stockArr = temp.toArray(stockArr);
                Giocatore g = new Giocatore(f, n, r, s.substring(0, 3), v, stockArr, status);
                squadra[i] = g;
            }
            return squadra;
        } catch (Exception e) {
            e.printStackTrace();
            return (new Giocatore[] {new Giocatore(f)});
        }
    }

    private String [] downloadPartite () {
        try {
            Document doc = HttpRequest.GET_nolega("https://www.fantacalcio.it", "<!-- INIZIO CONTAINER PRIMO BLOCCO CONTENUTO SU DUE COLONNE -->");

            Elements live = doc.select("div[class=col-lg-12 col-md-12 col-sm-12 col-xs-12 rel no-gutter item active]");

            Elements rows = live.select("div[class=matchs]");

            String [] cal = new String[10];
            for (int i = 0; i < 10; i++) {
                String id = rows.get(i).attr("id");

                String teamA;
                String teamB;
                String temp = "";
                String stato;

                if (id.equals("")) {
                    teamA = rows.get(i).select("div[class=liver]").get(0).select("img").attr("alt").toUpperCase();
                    teamB = rows.get(i).select("div[class=liver]").get(1).select("img").attr("alt").toUpperCase();
                    stato = "grey";
                } else {
                    teamA = rows.get(i).select("div[class=liver]").get(0).attr("data-team").toUpperCase();
                    teamB = rows.get(i).select("div[class=liver]").get(1).attr("data-team").toUpperCase();
                    temp = HttpRequest.GET_nodocument("https://www.fantacalcio.it/api/livestatus/" + id.substring(1));
                    temp = temp.substring(1, temp.length() - 1);
                    stato = temp.split(",")[1];
                    switch (stato) {
                        case "0":
                            stato = "blue";
                            break;
                        case "1":
                        case "3":
                            stato = "status";
                            break;
                        case "2":
                            stato = "orange";
                            break;
                        case "4":
                            stato = "grey";
                            break;
                    }
                }

                cal[i] = teamA + teamB + "<>" + stato + "<>";

                String time = rows.get(i).select("div[class=dlabel]").text().split(" ")[3];
                if (stato.equals("status")) {
                    cal[i] = cal[i] + temp.split(",")[0] + "'" + "<>";
                } else {
                    cal[i] = cal[i] + time + "<>";
                }

                cal[i] = cal[i] + rows.get(i).select("div[class=dlabel]").text().split(" " + time)[0] + "<>";
                cal[i] = cal[i] + rows.get(i).select("div[class=liver]").get(0).select("span").get(2).text() + "-" +
                        rows.get(i).select("div[class=liver]").get(1).select("span").get(2).text();
            }

            return cal;
        } catch (Exception e) {
            e.printStackTrace();
            return new String[] {"JUVENTUSROMALAZIONAPOLIFIORENTINASAMPDORIAGENOATORINOINTERMILANPALERMOUDINESEEMPOLISASSUOLOCAGLIARICHIEVOPESCARAATALANTABOLOGNACROTONEgrey"};
        }
    }

    private double calcola (Giocatore [] f) {
        double punti = 0;
        double [] fantavoto = new double[11];
        boolean [] entrato = new boolean[f.length - 11];
        int sost = opzioni.numsost;
        double por = 0;
        List<Double> dif = new ArrayList<>();

        for (int i = 10; i > -1; i--) {
            fantavoto[i] = f[i].getVoto();
            if (fantavoto[i] == 0 && sost > 0) {
                char r = f[i].getRuolo();
                boolean found = false;
                int k = 11;
                while (!found && k < f.length) {
                    if(f[k].getRuolo() == r && f[k].getVoto() != 0 && !entrato[k - 11]) {
                        found = true;
                        fantavoto[i] = f[k].getVoto();
                        if (f[k].getRuolo() == 'P') por = f[k].getVotoReale();
                        if (f[k].getRuolo() == 'D') dif.add(f[k].getVotoReale());
                        entrato[k - 11] = true;
                        sost--;
                    }
                    k++;
                }
            } else if (fantavoto[i] != 0) {
                if (f[i].getRuolo() == 'P') por = f[i].getVotoReale();
                if (f[i].getRuolo() == 'D') dif.add(f[i].getVotoReale());
            }
            punti = punti + fantavoto[i];
        }

        if (opzioni.mod_checked) {
            double media = por;
            if (dif.size() > 3) {
                Collections.sort(dif, Collections.<Double>reverseOrder());
                for (int i = 0; i < 3; i++) {
                    media = media + dif.get(i);
                }
                media = media / 4;
                int fasce_max = (int) ((opzioni.mod_max - opzioni.mod_min) / 0.25d);
                media = (media - opzioni.mod_min) / 0.25d;
                if (media >= fasce_max) {
                    punti = punti + opzioni.mod[opzioni.mod.length - 1];
                } else if (media < 0) {
                    punti = punti + opzioni.mod[0];
                } else {
                    punti = punti + opzioni.mod[(int)media + 1];
                }
            }
        }
        return punti;
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
