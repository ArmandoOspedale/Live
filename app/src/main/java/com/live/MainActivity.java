package com.live;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Assert;

import static android.view.View.GONE;

public class MainActivity extends AppCompatActivity {

    private Opzioni_lega opzioni;
    Competizione comp;
    private String current;
    private String squadra;
    private int selected;
    private String serieA;
    private String data;
    private ArrayList<HashMap<String, String>> feedList;
    private List<HashMap<String, String>> matches;
    private String [] ultima;
    private String [] prossima;
    private String uri;
    private String match;

    private DrawerLayout mDrawerLayout;
    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;
    private SwipeRefreshLayout srl;
    private ProgressDialog progressDialog;

    private final double[] mod_dif = new double[2];

    // TODO cambio profilo(da vedere... salvare username e password?)

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setProgressNumberFormat(null);
        progressDialog.setProgressPercentFormat(null);

        String cookie = (String) HttpRequest.getObject(this, "cookie");
        SharedPreferences sh = getSharedPreferences("RESET", MODE_PRIVATE);
        boolean reset = sh.getBoolean("reset1", true);

        if (cookie == null || reset) {
            sh.edit().putBoolean("reset1", false).apply();
            if (cookie != null) {
                logOut();
            }
            startActivityForResult(new Intent(this, LoginActivity.class), 1);
        } else {
            selezionaLega((int) HttpRequest.getObject(this, "indice_lega"), (int) HttpRequest.getObject(this, "indice_comp"));
        }
    }

    private boolean isNetworkAvailable() {
        boolean available = false;
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if(connMgr != null) networkInfo = connMgr.getActiveNetworkInfo();

        if(networkInfo !=null && networkInfo.isAvailable())
            available = true;

        return available;
    }

    private Giocatore [] downloadLive (String [] cal, JSONObject formazione) {
        String f = "";

        try {
            f = HttpRequest.getNomeSquadra(formazione.getString("id"), comp.codici[0], comp.codici[1]);
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
                boolean hasGolSubito = false;

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
                        if("golsubito_s".equals(bonus[b.getInt(k) - 1]))
                            hasGolSubito = true;
                    }
                }
                if(r == 'P' && v > 0 && !hasGolSubito
                    && Double.compare(0d, new JSONObject(opzioni.bonus).getDouble("portiere_imbattuto")) != 0) {
                    temp.add(bonus[12]);
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
        String[] cal = new String[10];
        try {
            Document doc = HttpRequest.GET_nolega("https://www.fantacalcio.it", "<!-- INIZIO CONTAINER PRIMO BLOCCO CONTENUTO SU DUE COLONNE -->");

            if(doc.select("div[class=live-strip]").size() > 0) {
                Elements live = doc.select("div[class=live-strip]");
                Elements rows;
                if(live.size() > 1)
                    rows = doc.select("div[class=live-strip]").get(1).children();
                else
                    rows = doc.select("div[class=live-strip]").get(0).children();

                serieA = rows.select("small").text().split("ª")[0];
                rows.remove(0);

                for (int i = 0; i < 10; i++) {
                    String id = rows.get(i).attr("id");

                    String teamA;
                    String teamB;
                    String temp = "";
                    String stato;

                    if (id.equals("")) {
                        teamA = rows.get(i).select("div[class=liver team-row]").get(0).select("img").attr("alt").toUpperCase();
                        teamB = rows.get(i).select("div[class=liver team-row]").get(1).select("img").attr("alt").toUpperCase();
                        stato = "grey";
                    } else {
                        teamA = rows.get(i).select("div[class=liver team-row]").get(0).attr("data-team").toUpperCase();
                        teamB = rows.get(i).select("div[class=liver team-row]").get(1).attr("data-team").toUpperCase();
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

                    String time = rows.get(i).select("div[class=match-date]").text().substring(rows.get(i).select("div[class=match-date]").text().lastIndexOf(" ") + 1);
                    if (stato.equals("status")) {
                        cal[i] = cal[i] + temp.split(",")[0] + "'" + "<>";
                    } else {
                        cal[i] = cal[i] + time + "<>";
                    }

                    cal[i] = cal[i] + rows.get(i).select("div[class=match-date]").text().split(" " + time)[0] + "<>";
                    cal[i] = cal[i] + rows.get(i).select("div[class=liver team-row]").get(0).select("span").get(2).text() + "-" +
                            rows.get(i).select("div[class=liver team-row]").get(1).select("span").get(2).text();
                }
            } else {
                Document ultima_doc = HttpRequest.GET_nolega("https://www.fantacalcio.it/serie-a/ultima-giornata", "col-xs-12 col-md-4 col-sticky hidden-xs");
                Elements all = ultima_doc.select("div[class=col-xs-12]").get(0).children();
                HashMap<Element, String> map = new LinkedHashMap<>();
                String data = "";
                for(Element el : all) {
                    if("subtitle greytxt1".equals(el.attr("class"))) {
                        data = mapWeekDay(el.text());
                    } else if ("match-row".equals(el.attr("class"))){
                        map.put(el, data);
                    }
                }

                serieA = ultima_doc.select("p[class=titalign text-right").text().split("ª")[0];

                int i = 0;
                for (Map.Entry<Element, String> entry : map.entrySet()) {
                    String teamA = entry.getKey().select("div[class=team home]").get(0).select("img").attr("alt").toUpperCase();
                    String teamB = entry.getKey().select("div[class=team away]").get(0).select("img").attr("alt").toUpperCase();
                    String stato = "grey";

                    cal[i] = teamA + teamB + "<>" + stato + "<>";

                    String time = entry.getKey().select("div[class=time-status]").select("div[class=ui-time time]").text();
                    cal[i] = cal[i] + time + "<>";

                    cal[i] = cal[i] + entry.getValue() + "<>";
                    cal[i] = cal[i] + entry.getKey().select("span[class=home]").text() + "-" +
                            entry.getKey().select("span[class=away]").text();
                    i++;
                }
            }
            return cal;
        } catch (Exception e) {
            e.printStackTrace();
            String [] squadre = getResources().getStringArray(R.array.squadre);
            for(int i = 0; i < squadre.length / 2; i++) {
                cal[i] = squadre[i * 2] + squadre[i * 2 + 1] + "<>" + "grey" + "<>" + "-<>" + "-<>" + "-<>";
            }
            return cal;
        }
    }

    private String mapWeekDay(String input) {
        return input.toLowerCase().replace("lunedì", "Lun").replace("martedì", "Mar")
                .replace("mercoledì", "Mer").replace("giovedì", "Gio")
                .replace("venerdì", "Ven").replace("sabato", "Sab")
                .replace("domenica", "Dom");
    }

    private boolean verificaLive () {
        try {
            URL url = new URL("https://leghe.fantacalcio.it/servizi/V1_LegheLive/Visualizza?alias_lega=" + HttpRequest.lega +
                    "&id_comp=" + comp.codice + "&id_squadra=" + comp.defaultcode);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestProperty("app_key", "4ab27d6de1e92c810c6d4efc8607065a735b917f");
            connection.setRequestProperty("cookie", HttpRequest.cookie);

            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            JSONObject json = new JSONObject(rd.readLine());
            boolean live = json.getBoolean("success");
            if(!json.getString("error_msgs").equals("null") && json.getJSONArray("error_msgs").getJSONObject(0).getString("id").equals("L002")) {
                HttpRequest.GET("/area-gioco/formazioni?id=" + comp.codice, "breadcumb");
                live = verificaLive();
            }

            if (!live) {
                Document home = HttpRequest.GET("?id=" + comp.codice, "");
                data = "";
                Elements scripts = home.select("script");
                for (Element e : scripts) {
                    String toString = e.toString();
                    if (toString.contains("__.s('li',")) {
                        data = toString.split("countdown: Number\\(\"")[1].split("\"\\),")[0];
                    }
                }

                Element table;
                Elements rows;
                if (comp.tipo != 0 && comp.tipo != 7 && comp.tipo != 9) {
                    feedList = new ArrayList<>();
                    table = home.select("table[class=widget-body smart-table top-ten table table-striped box raised]").select("tbody").get(0);
                    rows = table.select("tr");
                    if(home.select("table[class=widget-body smart-table top-ten table table-striped box raised]").select("tbody").size() > 1) {
                        rows.add(0, Jsoup.parse("<span span customGroupHeader=\"customGroupHeader\">Gruppo A</span>").select("span").get(0));
                        rows.add(Jsoup.parse("<span customGroupHeader=\"customGroupHeader\">Gruppo B</span>").select("span").get(0));
                        rows.addAll(home.select("table[class=widget-body smart-table top-ten table table-striped box raised]").select("tbody").get(1).select("tr"));
                    }

                    for(Element r : rows) {
                        HashMap<String, String> map = new HashMap<>();
                        if(r.hasAttr("customGroupHeader")) {
                            map.put("NomeSquadra", r.text());
                            map.put("Punti", "");
                            map.put("Giocate", "");
                            map.put("Totale", "");
                        } else{
                            map.put("NomeSquadra", r.select("td[data-key=teamName]").select("span").get(0).text());
                            if (comp.tipo != 2) {
                                map.put("Punti", r.select("td[data-key=rank-pt]").text());
                            } else {
                                map.put("Punti", "0");
                            }
                            map.put("Giocate", r.select("td[data-key=rank-g]").text());
                            map.put("Totale", r.select("td[data-key=rank-fp]").text());
                        }

                        feedList.add(map);
                    }
                }

                if (comp.tipo == 1 || comp.tipo == 4 || comp.tipo == 5 || comp.tipo == 6 || comp.tipo == 9) {
                    Elements match;
                    if (home.select("ul[class=widget-body box raised versus]").size() > 1 &&
                            home.select("ul[class=widget-body box raised versus]").get(1).parent().select("header[class=widget-header clearfix]").text().contains("Ultima Giornata")) {
                        table = home.select("ul[class=widget-body box raised versus]").get(1);
                        match = table.select("li");
                        if (match.size() > 0) {
                            if (match.get(0).text().contains("L’ADMIN NON HA ANCORA EFFETTUATO IL CALCOLO")) {
                                match.remove(0);
                                ultima = new String[match.size() - 1];
                                for (int i = 0; i < match.size() - 1; i++) {
                                    ultima[i] = match.get(i).select("h5[class=team-name").get(0).text() + "  VS  " +
                                            match.get(i).select("h5[class=team-name").get(1).text();
                                }
                            } else {
                                for (int i = 0; i < match.size(); i++) {
                                    if (match.get(i).select("h5[class=team-name").size() < 2) {
                                        match.remove(match.get(i));
                                        i--;
                                    }
                                }
                                ultima = new String[match.size()];
                                for (int i = 0; i < match.size(); i++) {
                                    ultima[i] = match.get(i).select("h5[class=team-name").get(0).text() + "/" +
                                            match.get(i).select("h5[class=team-name").get(1).text() + "/" +
                                            match.get(i).select("div[class=team-fpt]").get(0).text() + "-" +
                                            match.get(i).select("div[class=team-fpt]").get(1).text() + "/" +
                                            match.get(i).select("div[class=team-score]").get(0).text() + "-" +
                                            match.get(i).select("div[class=team-score]").get(1).text();
                                }
                            }
                        }
                    } else {
                        ultima = new String[]{home.select("div[class=alert alert-notice no-margin]").select("h4").text(),
                                home.select("div[class=alert alert-notice no-margin]").select("h3").text().replace("EuroCalendario", "")};
                    }

                    if (home.select("ul[class=widget-body box raised versus]").size() > 2 &&
                            home.select("ul[class=widget-body box raised versus]").get(2).parent().select("header[class=widget-header clearfix]").text().contains("Prossima Giornata")) {
                        table = home.select("ul[class=widget-body box raised versus]").get(2);
                        match = table.select("li");
                        for(int i = 0; i < match.size(); i++) {
                            if(match.get(i).select("h5[class=team-name").size() == 0) {
                                match.remove(match.get(i));
                                i--;
                            }
                        }
                        prossima = new String[match.size()];
                        for (int i = 0; i < match.size(); i++) {
                            prossima[i] = match.get(i).select("h5[class=team-name").get(0).text() + "  VS  " +
                                    match.get(i).select("h5[class=team-name").get(1).text();
                        }
                    } else {
                        String vincitore = home.select("strong[class=winners]").text();
                        if(StringUtil.isBlank(vincitore)) {
                            prossima = new String[]{"Competizione non ancora iniziata", ""};
                        } else{
                            vincitore = "Il vincitore è " + vincitore;
                            prossima = new String[]{"Competizione terminata", vincitore};
                        }
                    }
                }
            }

            return live;
        } catch (IOException | JSONException e) {
            return false;
        }
    }

    private double calcola (Giocatore [] f, int id) {
        double punti = 0;
        double [] fantavoto = new double[11];
        boolean [] entrato = new boolean[f.length - 11];
        int sost = opzioni.numsost;
        double por = 0;
        List<Double> dif = new ArrayList<>();

        mod_dif[id == R.id.listView1 ? 0 : 1] = 0d;

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
                    mod_dif[id == R.id.listView1 ? 0 : 1] = opzioni.mod[opzioni.mod.length - 1];
                } else if (media < 0) {
                    punti = punti + opzioni.mod[0];
                    mod_dif[id == R.id.listView1 ? 0 : 1] = opzioni.mod[0];
                } else {
                    punti = punti + opzioni.mod[(int)media + 1];
                    mod_dif[id == R.id.listView1 ? 0 : 1] = opzioni.mod[(int)media + 1];
                }
            }
        }
        return punti;
    }

    private int [] risultato (double casa, double trasf) {
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
        return risultato;
    }

    private class Menu_Adapter extends BaseAdapter {

        Context context;
        List<String> dati;

        Menu_Adapter (Context cxt, List<String> d) {
            context = cxt;
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
            return i;
        }

        @Override
        @SuppressWarnings("all")
        public View getView(int i, View view, ViewGroup viewGroup) {

            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.menu_list, null);
            }

            String azione = (String) getItem(i);
            TextView txt = (TextView) view.findViewById(R.id.item);
            txt.setText(azione);

            ImageView im = (ImageView) view.findViewById(R.id.icona);
            im.setImageResource(getDrawable(context, azione.replace(" ", "_").toLowerCase()));

            return view;
        }
    }

    private void visualizza () {
        progressDialog.dismiss();
        setContentView(R.layout.main_drawer);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        ListView mDrawerList = findViewById(R.id.left_drawer);
        mDrawerList.setVerticalScrollBarEnabled(false);
        List<String> azioni = new ArrayList<>();
        azioni.add("FORMAZIONI");
        switch (comp.tipo) {
            case 1:
            case 4:
            case 6:
                azioni.add("CLASSIFICA");
                azioni.add("CALENDARIO");
                break;
            case 2:
            case 3:
                azioni.add("CLASSIFICA");
                break;
            default:
                azioni.add("CALENDARIO");
                break;
        }
        azioni.add("ROSE");
        azioni.add("LISTA SVINCOLATI");
        azioni.add("SERIE A");
        if (opzioni.admin) {
            azioni.add("GESTIONE ROSE");
            azioni.add("GESTIONE FORMAZIONI");
        }
        azioni.add("VOTI");
        azioni.add("STATISTICHE");
        azioni.add("OPZIONI LEGA");
        //azioni.add("NOTIFICHE");
        mDrawerList.setAdapter(new Menu_Adapter(this, azioni));
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mDrawerLayout.closeDrawer(GravityCompat.START);
                Intent intent;
                switch ((String) parent.getItemAtPosition(position)) {
                    case "FORMAZIONI":
                        intent = new Intent(getApplicationContext(), Formazioni.class);
                        intent.putExtra("squadre", comp.codici[1]);
                        intent.putExtra("codici", comp.codici[0]);
                        intent.putExtra("select", comp.defaultcode);
                        intent.putExtra("comp", comp.codice);
                        intent.putExtra("tipo", comp.tipo);
                        startActivity(intent);
                        break;
                    case "CLASSIFICA":
                        intent = new Intent(MainActivity.this, Classifica.class);
                        intent.putExtra("comp", comp.codice);
                        intent.putExtra("tipo", comp.tipo);
                        intent.putExtra("squadre", comp.codici[1]);
                        intent.putExtra("codici", comp.codici[0]);
                        startActivity(intent);
                        break;
                    case "CALENDARIO":
                        intent = new Intent(MainActivity.this, Calendario.class);
                        intent.putExtra("comp", comp.codice);
                        intent.putExtra("squadre", comp.codici[1]);
                        intent.putExtra("codici", comp.codici[0]);
                        startActivity(intent);
                        break;
                    case "ROSE":
                        intent = new Intent(getApplicationContext(), Rose.class);
                        intent.putExtra("squadre", comp.codici[1]);
                        intent.putExtra("codici", comp.codici[0]);
                        intent.putExtra("select", comp.defaultcode);
                        intent.putExtra("admin", opzioni.admin);
                        startActivity(intent);
                        break;
                    case "GESTIONE ROSE":
                        intent = new Intent(getApplicationContext(), Gestione_Rose.class);
                        intent.putExtra("squadre", comp.codici[1]);
                        intent.putExtra("codici", comp.codici[0]);
                        intent.putExtra("select", comp.defaultcode);
                        intent.putExtra("admin", opzioni.admin);
                        intent.putExtra("max", opzioni.calciatori_per_ruolo);
                        startActivity(intent);
                        break;
                    case "GESTIONE FORMAZIONI":
                        new Gestione_Formazioni(MainActivity.this, comp.codice, comp.codici[0], comp.codici[1]);
                        break;
                    case "SERIE A":
                        intent = new Intent(getApplicationContext(), Serie_A.class);
                        startActivity(intent);
                        break;
                    case "LISTA SVINCOLATI":
                        intent = new Intent(getApplicationContext(), ListaSvincolati.class);
                        startActivity(intent);
                        break;
                    case "VOTI":
                        intent = new Intent(getApplicationContext(), Voti.class);
                        startActivity(intent);
                        break;
                    case "STATISTICHE":
                        Intent go = new Intent(getApplicationContext(), Statistiche.class);
                        startActivity(go);
                        break;
                    case "OPZIONI LEGA":
                        intent = new Intent(getApplicationContext(), Impostazioni.class);
                        intent.putExtra("opzioni", opzioni);
                        startActivity(intent);
                        break;
                    case "NOTIFICHE":
                        final Dialog d = new Dialog(MainActivity.this, R.style.CustomDialog);
                        d.setTitle("NOTIFICHE");
                        d.setContentView(R.layout.notifiche);

                        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                        int attuale = sharedPreferences.getInt("notifiche", -1);
                        RadioGroup radioGroup = d.findViewById(R.id.radio);
                        radioGroup.check(attuale);
                        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                            @Override
                            @SuppressLint("NonConstantResourceId")
                            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                                switch (i) {
                                    case R.id.disattivate:
                                        sharedPreferences.edit().putInt("notifiche", R.id.disattivate).apply();
                                        break;
                                    case R.id.tutte:
                                        sharedPreferences.edit().putInt("notifiche", R.id.tutte).apply();
                                        break;
                                    case R.id.miasquadra:
                                        sharedPreferences.edit().putInt("notifiche", R.id.miasquadra).apply();
                                        break;
                                    case R.id.miapartita:
                                        sharedPreferences.edit().putInt("notifiche", R.id.miapartita).apply();
                                        break;
                                }
                            }
                        });
                        d.show();
                        break;
                }
            }
        });
        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, R.string.action_voti, R.string.pt
        );
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setHomeButtonEnabled(true);
        }
        mDrawerToggle.syncState();

        TextView c = findViewById(R.id.comp);
        c.setText(comp.nome.toUpperCase());

        TextView c1 = findViewById(R.id.lega);
        c1.setText(HttpRequest.lega.toUpperCase());
        TypedValue outValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        c1.setBackgroundResource(outValue.resourceId);
        c1.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                final Dialog d = new Dialog(MainActivity.this);
                d.requestWindowFeature(Window.FEATURE_NO_TITLE);

                String [] leghe = (String[]) HttpRequest.getObject(MainActivity.this, "leghe");

                if (leghe.length == 1) {
                    Toast.makeText(MainActivity.this, "Sei iscritto solo a questa lega", Toast.LENGTH_SHORT).show();
                } else {
                    for (int i = 0; i < leghe.length; i++) {
                        leghe[i] = leghe[i].toUpperCase();
                    }

                    LinearLayout l = new LinearLayout(MainActivity.this);
                    l.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

                    final ListView list = new ListView(MainActivity.this);
                    list.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                    ArrayAdapter<String> adapter2 = new ArrayAdapter<>(MainActivity.this,
                            android.R.layout.select_dialog_item, leghe);
                    list.setAdapter(adapter2);

                    list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                            d.dismiss();
                            if (i != selected) selezionaLega(i, 0);
                        }
                    });

                    l.addView(list);

                    d.setContentView(l);
                    d.show();
                }

                return true;
            }
        });

        TextView c2 = findViewById(R.id.competizione);
        c2.setText(comp.nome);
        c2.setBackgroundResource(outValue.resourceId);

        c2.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                final Dialog d = new Dialog(MainActivity.this);
                d.requestWindowFeature(Window.FEATURE_NO_TITLE);

                if (opzioni.competizioni.length == 1) {
                    Toast.makeText(MainActivity.this, "Non ci sono altre competizioni attive", Toast.LENGTH_SHORT).show();
                } else {
                    LinearLayout l = new LinearLayout(MainActivity.this);
                    l.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

                    String[] competizioni = new String[opzioni.competizioni.length];
                    for (int i = 0; i < competizioni.length; i++) {
                        competizioni[i] = opzioni.competizioni[i].nome;
                    }

                    final ListView list = new ListView(MainActivity.this);
                    list.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                    ArrayAdapter<String> adapter2 = new ArrayAdapter<>(MainActivity.this,
                            android.R.layout.select_dialog_item, competizioni);
                    list.setAdapter(adapter2);

                    list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                            d.dismiss();
                            if (!(opzioni.competizioni[i].equals(comp))) selezionaLega(selected, i);
                        }
                    });

                    l.addView(list);

                    d.setContentView(l);
                    d.show();
                }

                return true;
            }
        });

        TextView c3 = findViewById(R.id.squadra);
        int j = 0;
        boolean found = false;
        while (!found && j < comp.codici[1].length) {
            if (comp.codici[1][j].equals(squadra)) {
                found = true;
                c3.setText(comp.codici[1][j]);
            }
            j++;
        }

        try {Giocatore.opzioni = new JSONObject(opzioni.bonus);} catch (JSONException e) {e.printStackTrace();}
        Giocatore.ammsv_checked = opzioni.ammsv_checked;
        Giocatore.ammsv_value = opzioni.ammsv_value;

        current = comp.defaultcode;
        new DownloadTask().execute(current);

        srl = findViewById(R.id.srl);
        srl.setColorSchemeColors(Color.rgb(18, 116, 175));

        final FloatingActionButton fab = findViewById(R.id.fab);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) fab.getLayoutParams();
            float density = getResources().getDisplayMetrics().density;
            p.setMargins(0, (int) (-10 * density), (int) (8 * density), (int) (-10 * density));
            fab.setLayoutParams(p);
        }
        final ListView list = findViewById(R.id.spinner2);
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, comp.codici[1]);
        list.setAdapter(adapter2);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //if (i != 0) {
                    fab.animate().rotationBy(90).setDuration(150);
                    list.setVisibility(GONE);
                    current = comp.codici[0][i];
                    new DownloadTask().execute(current);
                //}
            }
        });
        fab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (list.getVisibility() == View.VISIBLE) {
                    fab.animate().rotationBy(90).setDuration(150);
                    list.setVisibility(GONE);
                } else {
                    fab.animate().rotationBy(-90).setDuration(150);
                    list.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @SuppressLint("StaticFieldLeak")
    private class NoLive extends AsyncTask<Void, Void, Void> {
        boolean recap;

        @Override
        protected Void doInBackground(Void... voids) {
            String [] partite = downloadPartite();
            recap = partite.length != 1;
            if (recap) matches = diretta(partite);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            progressDialog.dismiss();
            setContentView(R.layout.nolive_drawer);

            mDrawerLayout = findViewById(R.id.drawer_layout);
            ListView mDrawerList = findViewById(R.id.left_drawer);
            mDrawerList.setVerticalScrollBarEnabled(false);
            List<String> azioni = new ArrayList<>();
            azioni.add("FORMAZIONI");
            azioni.add("INSERISCI FORMAZIONE");
            switch (comp.tipo) {
                case 1:
                case 4:
                case 6:
                    azioni.add("CLASSIFICA");
                    azioni.add("CALENDARIO");
                    break;
                case 2:
                case 3:
                    azioni.add("CLASSIFICA");
                    break;
                default:
                    azioni.add("CALENDARIO");
                    break;
            }
            azioni.add("ROSE");
            azioni.add("LISTA SVINCOLATI");
            azioni.add("SERIE A");
            if (opzioni.admin) {
                azioni.add("GESTIONE ROSE");
                azioni.add("GESTIONE FORMAZIONI");
                azioni.add("CALCOLA GIORNATA");
                azioni.add("ANNULLA CALCOLO");
            }
            azioni.add("VOTI");
            azioni.add("STATISTICHE");
            azioni.add("OPZIONI LEGA");
            //azioni.add("NOTIFICHE");
            mDrawerList.setAdapter(new Menu_Adapter(MainActivity.this, azioni));
            mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                    Intent intent;
                    switch ((String) parent.getItemAtPosition(position)) {
                        case "FORMAZIONI":
                            intent = new Intent(getApplicationContext(), Formazioni.class);
                            intent.putExtra("squadre", comp.codici[1]);
                            intent.putExtra("codici", comp.codici[0]);
                            intent.putExtra("select", comp.defaultcode);
                            intent.putExtra("comp", comp.codice);
                            intent.putExtra("tipo", comp.tipo);
                            startActivity(intent);
                            break;
                        case "INSERISCI FORMAZIONE":
                            intent = new Intent(MainActivity.this, Schiera.class);
                            intent.putExtra("comp", comp.codice);
                            intent.putExtra("teamId", comp.defaultcode);
                            intent.putExtra("admin", 0);
                            startActivity(intent);
                            break;
                        case "CLASSIFICA":
                            intent = new Intent(MainActivity.this, Classifica.class);
                            intent.putExtra("comp", comp.codice);
                            intent.putExtra("tipo", comp.tipo);
                            intent.putExtra("squadre", comp.codici[1]);
                            intent.putExtra("codici", comp.codici[0]);
                            startActivity(intent);
                            break;
                        case "CALENDARIO":
                            intent = new Intent(MainActivity.this, Calendario.class);
                            intent.putExtra("comp", comp.codice);
                            intent.putExtra("squadre", comp.codici[1]);
                            intent.putExtra("codici", comp.codici[0]);
                            startActivity(intent);
                            break;
                        case "ROSE":
                            intent = new Intent(getApplicationContext(), Rose.class);
                            intent.putExtra("squadre", comp.codici[1]);
                            intent.putExtra("codici", comp.codici[0]);
                            intent.putExtra("select", comp.defaultcode);
                            intent.putExtra("admin", opzioni.admin);
                            startActivity(intent);
                            break;
                        case "LISTA SVINCOLATI":
                            intent = new Intent(getApplicationContext(), ListaSvincolati.class);
                            startActivity(intent);
                            break;
                        case "SERIE A":
                            intent = new Intent(getApplicationContext(), Serie_A.class);
                            startActivity(intent);
                            break;
                        case "GESTIONE ROSE":
                            intent = new Intent(getApplicationContext(), Gestione_Rose.class);
                            intent.putExtra("squadre", comp.codici[1]);
                            intent.putExtra("codici", comp.codici[0]);
                            intent.putExtra("select", comp.defaultcode);
                            intent.putExtra("admin", opzioni.admin);
                            intent.putExtra("max", opzioni.calciatori_per_ruolo);
                            startActivity(intent);
                            break;
                        case "GESTIONE FORMAZIONI":
                            new Gestione_Formazioni(MainActivity.this, comp.codice, comp.codici[0], comp.codici[1]);
                            break;
                        case "CALCOLA GIORNATA":
                            Calcolo c = new Calcolo(MainActivity.this, comp.codici[1], comp.codici[0], comp.codice);
                            c.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialogInterface) {
                                    new AsyncTask<Void, Void, Void>() {
                                        @Override
                                        @SuppressLint("WrongThread")
                                        protected Void doInBackground(Void... voids) {
                                            verificaLive();
                                            return null;
                                        }

                                        @Override
                                        protected void onPostExecute(Void result) {
                                            mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
                                            mPager.setAdapter(mPagerAdapter);
                                            mPager.setCurrentItem((comp.tipo == 2 || comp.tipo == 3) ? 0 : 1);
                                        }
                                    }.execute();
                                }
                            });
                            break;
                        case "ANNULLA CALCOLO":
                            Annulla a = new Annulla(MainActivity.this, comp.codice);
                            a.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(final DialogInterface dialogInterface) {
                                    new AsyncTask<Void, Void, Void>() {
                                        @Override
                                        @SuppressLint("WrongThread")
                                        protected Void doInBackground(Void... voids) {
                                            verificaLive();
                                            return null;
                                        }

                                        @Override
                                        protected void onPostExecute(Void result) {
                                            mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
                                            mPager.setAdapter(mPagerAdapter);
                                            mPager.setCurrentItem((comp.tipo == 2 || comp.tipo == 3) ? 0 : 1);
                                        }
                                    }.execute();
                                }
                            });
                            break;
                        case "VOTI":
                            intent = new Intent(getApplicationContext(), Voti.class);
                            startActivity(intent);
                            break;
                        case "STATISTICHE":
                            Intent go = new Intent(getApplicationContext(), Statistiche.class);
                            startActivity(go);
                            break;
                        case "OPZIONI LEGA":
                            intent = new Intent(getApplicationContext(), Impostazioni.class);
                            intent.putExtra("opzioni", opzioni);
                            startActivity(intent);
                            break;
                        case "NOTIFICHE":
                            final Dialog d = new Dialog(MainActivity.this, R.style.CustomDialog);
                            d.setTitle("NOTIFICHE");
                            d.setContentView(R.layout.notifiche);

                            final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                            int attuale = sharedPreferences.getInt("notifiche", -1);
                            RadioGroup radioGroup = d.findViewById(R.id.radio);
                            radioGroup.check(attuale);
                            radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                                @Override
                                @SuppressLint("NonConstantResourceId")
                                public void onCheckedChanged(RadioGroup radioGroup, int i) {
                                    switch (i) {
                                        case R.id.disattivate:
                                            sharedPreferences.edit().putInt("notifiche", R.id.disattivate).apply();
                                            break;
                                        case R.id.tutte:
                                            sharedPreferences.edit().putInt("notifiche", R.id.tutte).apply();
                                            break;
                                        case R.id.miasquadra:
                                            sharedPreferences.edit().putInt("notifiche", R.id.miasquadra).apply();
                                            break;
                                        case R.id.miapartita:
                                            sharedPreferences.edit().putInt("notifiche", R.id.miapartita).apply();
                                            break;
                                    }
                                }
                            });
                            d.show();
                            break;
                    }
                }
            });
            ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(
                    MainActivity.this, mDrawerLayout, R.string.action_voti, R.string.pt
            );
            mDrawerLayout.addDrawerListener(mDrawerToggle);
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setDisplayHomeAsUpEnabled(true);
                ab.setHomeButtonEnabled(true);
            }
            mDrawerToggle.syncState();

            TextView c = findViewById(R.id.comp);
            c.setText(comp.nome.toUpperCase());
            TextView alert = findViewById(R.id.alert);
            alert.setText(R.string.timer);

            long intervalMillis = 0;
            if (data != null && !data.equals("")) {
                intervalMillis = Integer.parseInt(data) * 1000;
            }

            Timer t = new Timer(intervalMillis, 1000, findViewById(R.id.timer));
            t.start();

            TextView c1 = findViewById(R.id.lega);
            c1.setText(HttpRequest.lega.toUpperCase());
            TypedValue outValue = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            c1.setBackgroundResource(outValue.resourceId);
            c1.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    final Dialog d = new Dialog(MainActivity.this);
                    d.requestWindowFeature(Window.FEATURE_NO_TITLE);

                    String [] leghe = (String[]) HttpRequest.getObject(MainActivity.this, "leghe");

                    if (leghe.length == 1) {
                        Toast.makeText(MainActivity.this, "Sei iscritto solo a questa lega", Toast.LENGTH_SHORT).show();
                    } else {
                        for (int i = 0; i < leghe.length; i++) {
                            leghe[i] = leghe[i].toUpperCase();
                        }

                        LinearLayout l = new LinearLayout(MainActivity.this);
                        l.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

                        final ListView list = new ListView(MainActivity.this);
                        list.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(MainActivity.this,
                                android.R.layout.select_dialog_item, leghe);
                        list.setAdapter(adapter2);

                        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                d.dismiss();
                                if (i != selected) selezionaLega(i, 0);
                            }
                        });

                        l.addView(list);

                        d.setContentView(l);
                        d.show();
                    }

                    return true;
                }
            });

            TextView c2 = findViewById(R.id.competizione);
            c2.setText(comp.nome);
            c2.setBackgroundResource(outValue.resourceId);

            c2.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    final Dialog d = new Dialog(MainActivity.this);
                    d.requestWindowFeature(Window.FEATURE_NO_TITLE);

                    if (opzioni.competizioni.length == 1) {
                        Toast.makeText(MainActivity.this, "Non ci sono altre competizioni attive", Toast.LENGTH_SHORT).show();
                    } else {
                        LinearLayout l = new LinearLayout(MainActivity.this);
                        l.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

                        String[] competizioni = new String[opzioni.competizioni.length];
                        for (int i = 0; i < competizioni.length; i++) {
                            competizioni[i] = opzioni.competizioni[i].nome;
                        }

                        final ListView list = new ListView(MainActivity.this);
                        list.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(MainActivity.this,
                                android.R.layout.select_dialog_item, competizioni);
                        list.setAdapter(adapter2);

                        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                d.dismiss();
                                if (!(opzioni.competizioni[i].equals(comp))) selezionaLega(selected, i);
                            }
                        });

                        l.addView(list);

                        d.setContentView(l);
                        d.show();
                    }

                    return true;
                }
            });

            TextView c3 = findViewById(R.id.squadra);
            int j = 0;
            boolean found = false;
            while (!found && j < comp.codici[1].length) {
                if (comp.codici[1][j].equals(squadra)) {
                    found = true;
                    c3.setText(comp.codici[1][j]);
                }
                j++;
            }

            mPager = findViewById(R.id.pager);
            mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
            mPager.setAdapter(mPagerAdapter);
            mPager.setCurrentItem((comp.tipo == 2 || comp.tipo == 3) ? 0 : 1);

            if (recap) {
                ListView rightList = findViewById(R.id.right_drawer);
                rightList.setAdapter(new PallinoAdapter(MainActivity.this, matches, R.layout.serie_a, new String[]{"casa", "trasf", "orario", "data", "risultato"},
                        new int[]{R.id.casa, R.id.trasf, R.id.orario, R.id.data, R.id.result}));

                rightList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        if (matches.get(i).get("stato").equals("blue")) {
                            Toast.makeText(MainActivity.this, "Partita non ancora iniziata", Toast.LENGTH_SHORT).show();
                        } else if(matches.get(i).get("data").equals("-") || matches.get(i).get("risultato").equals("-")) {
                            Toast.makeText(MainActivity.this, "Live non disponibile", Toast.LENGTH_SHORT).show();
                        } else {
                            Intent fg = new Intent(MainActivity.this, PartitaLive.class);
                            fg.putExtra("title", "Voti & Cronaca");
                            fg.putExtra("giornata", serieA);
                            fg.putExtra("match", matches.get(i).get("casa").toUpperCase() + " - " + matches.get(i).get("trasf").toUpperCase());
                            startActivity(fg);
                        }
                    }
                });
            } else {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END);
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class NoComp extends AsyncTask<Void, Void, Void> {
        boolean recap;

        @Override
        protected Void doInBackground(Void... voids) {
            String [] partite = downloadPartite();
            recap = partite.length != 1;
            if (recap) matches = diretta(partite);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            progressDialog.dismiss();
            setContentView(R.layout.main_drawer);

            mDrawerLayout = findViewById(R.id.drawer_layout);
            ListView mDrawerList = findViewById(R.id.left_drawer);
            mDrawerList.setVerticalScrollBarEnabled(false);
            List<String> azioni = new ArrayList<>();
            azioni.add("ROSE");
            azioni.add("LISTA SVINCOLATI");
            azioni.add("SERIE A");
            if (opzioni.admin) {
                azioni.add("GESTIONE ROSE");
            }
            azioni.add("STATISTICHE");
            azioni.add("OPZIONI LEGA");
            //azioni.add("NOTIFICHE");

            mDrawerList.setAdapter(new Menu_Adapter(MainActivity.this, azioni));
            mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                    Intent intent;
                    String codice = "";
                    int j = 0;
                    boolean found = false;
                    while (!found && j < opzioni.squadre[1].length) {
                        if (opzioni.squadre[1][j].equals(squadra)) {
                            found = true;
                            codice = opzioni.squadre[0][j];
                        }
                        j++;
                    }
                    switch ((String) parent.getItemAtPosition(position)) {
                        case "ROSE":
                            intent = new Intent(getApplicationContext(), Rose.class);
                            intent.putExtra("squadre", opzioni.squadre[1]);
                            intent.putExtra("codici", opzioni.squadre[0]);
                            intent.putExtra("select", codice);
                            intent.putExtra("admin", opzioni.admin);
                            startActivity(intent);
                            break;
                        case "GESTIONE ROSE":
                            intent = new Intent(getApplicationContext(), Gestione_Rose.class);
                            intent.putExtra("squadre", opzioni.squadre[1]);
                            intent.putExtra("codici", opzioni.squadre[0]);
                            intent.putExtra("select", codice);
                            intent.putExtra("admin", opzioni.admin);
                            intent.putExtra("max", opzioni.calciatori_per_ruolo);
                            startActivity(intent);
                            break;
                        case "SERIE A":
                            intent = new Intent(getApplicationContext(), Serie_A.class);
                            startActivity(intent);
                            break;
                        case "LISTA SVINCOLATI":
                            intent = new Intent(getApplicationContext(), ListaSvincolati.class);
                            startActivity(intent);
                            break;
                        case "STATISTICHE":
                            Intent go = new Intent(getApplicationContext(), Statistiche.class);
                            startActivity(go);
                            break;
                        case "OPZIONI LEGA":
                            intent = new Intent(getApplicationContext(), Impostazioni.class);
                            intent.putExtra("opzioni", opzioni);
                            startActivity(intent);
                            break;
                        case "NOTIFICHE":
                            final Dialog d = new Dialog(MainActivity.this, R.style.CustomDialog);
                            d.setTitle("NOTIFICHE");
                            d.setContentView(R.layout.notifiche);

                            final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                            int attuale = sharedPreferences.getInt("notifiche", -1);
                            RadioGroup radioGroup = d.findViewById(R.id.radio);
                            radioGroup.check(attuale);
                            radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                                @Override
                                @SuppressLint("NonConstantResourceId")
                                public void onCheckedChanged(RadioGroup radioGroup, int i) {
                                    switch (i) {
                                        case R.id.disattivate:
                                            sharedPreferences.edit().putInt("notifiche", R.id.disattivate).apply();
                                            break;
                                        case R.id.tutte:
                                            sharedPreferences.edit().putInt("notifiche", R.id.tutte).apply();
                                            break;
                                        case R.id.miasquadra:
                                            sharedPreferences.edit().putInt("notifiche", R.id.miasquadra).apply();
                                            break;
                                        case R.id.miapartita:
                                            sharedPreferences.edit().putInt("notifiche", R.id.miapartita).apply();
                                            break;
                                    }
                                }
                            });
                            d.show();
                            break;
                    }
                }
            });
            ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(
                    MainActivity.this, mDrawerLayout, R.string.action_voti, R.string.pt
            );
            mDrawerLayout.addDrawerListener(mDrawerToggle);
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setDisplayHomeAsUpEnabled(true);
                ab.setHomeButtonEnabled(true);
            }
            mDrawerToggle.syncState();

            TextView c = findViewById(R.id.comp);
            c.setText(R.string.nocomp);

            TextView c1 = findViewById(R.id.lega);
            c1.setText(HttpRequest.lega.toUpperCase());
            TypedValue outValue = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            c1.setBackgroundResource(outValue.resourceId);
            c1.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    final Dialog d = new Dialog(MainActivity.this);
                    d.requestWindowFeature(Window.FEATURE_NO_TITLE);

                    String [] leghe = (String[]) HttpRequest.getObject(MainActivity.this, "leghe");

                    if (leghe.length == 1) {
                        Toast.makeText(MainActivity.this, "Sei iscritto solo a questa lega", Toast.LENGTH_SHORT).show();
                    } else {
                        for (int i = 0; i < leghe.length; i++) {
                            leghe[i] = leghe[i].toUpperCase();
                        }

                        LinearLayout l = new LinearLayout(MainActivity.this);
                        l.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

                        final ListView list = new ListView(MainActivity.this);
                        list.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(MainActivity.this,
                                android.R.layout.select_dialog_item, leghe);
                        list.setAdapter(adapter2);

                        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                d.dismiss();
                                if (i != selected) selezionaLega(i, 0);
                            }
                        });

                        l.addView(list);

                        d.setContentView(l);
                        d.show();
                    }

                    return true;
                }
            });

            TextView c2 = findViewById(R.id.competizione);
            c2.setVisibility(GONE);

            TextView c3 = findViewById(R.id.squadra);
            c3.setText(squadra);
            /*int j = 0;
            boolean found = false;
            while (!found && j < comp.codici[1].length) {
                if (comp.codici[1][j].toLowerCase().equals(squadra)) {
                    found = true;
                    c3.setText(comp.codici[1][j]);
                }
                j++;
            }*/

            srl = findViewById(R.id.srl);
            srl.setVisibility(GONE);

            FloatingActionButton fab = findViewById(R.id.fab);
            fab.setVisibility(GONE);
            ListView list = findViewById(R.id.spinner2);
            list.setVisibility(GONE);

            ProgressBar pb = findViewById(R.id.progressBar);
            pb.setVisibility(GONE);

            ListView listc = findViewById(R.id.listView1);
            listc.setVisibility(GONE);

            ListView listt = findViewById(R.id.listView2);
            listt.setVisibility(GONE);
            ListView p1 = findViewById(R.id.risultato);
            p1.setVisibility(GONE);

            if (recap) {
                ListView rightList = findViewById(R.id.right_drawer);
                rightList.setAdapter(new PallinoAdapter(MainActivity.this, matches, R.layout.serie_a, new String[]{"casa", "trasf", "orario", "data", "risultato"},
                        new int[]{R.id.casa, R.id.trasf, R.id.orario, R.id.data, R.id.result}));

                rightList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        if (matches.get(i).get("stato").equals("blue")) {
                            Toast.makeText(MainActivity.this, "Partita non ancora iniziata", Toast.LENGTH_SHORT).show();
                        } else if(matches.get(i).get("data").equals("-") || matches.get(i).get("risultato").equals("-")) {
                            Toast.makeText(MainActivity.this, "Live non disponibile", Toast.LENGTH_SHORT).show();
                        } else {
                            Intent fg = new Intent(MainActivity.this, PartitaLive.class);
                            fg.putExtra("title", "Voti & Cronaca");
                            fg.putExtra("giornata", serieA);
                            fg.putExtra("match", matches.get(i).get("casa").toUpperCase() + " - " + matches.get(i).get("trasf").toUpperCase());
                            startActivity(fg);
                        }
                    }
                });
            } else {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END);
            }
        }
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            InfoFragment info = new InfoFragment();
            Bundle b = new Bundle();
            b.putInt("position", position);
            b.putInt("comp", comp.tipo);
            b.putStringArray("ultima", ultima);
            b.putStringArray("prossima", prossima);
            b.putSerializable("classifica", feedList);
            info.setArguments(b);
            return info;
        }

        @Override
        public int getCount() {
            switch (comp.tipo) {
                case 1:
                case 4:
                case 6:
                    return 3;
                case 2:
                case 3:
                    return 1;
                default:
                    return 2;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            String return_value = "";
            switch (comp.tipo) {
                case 1:
                case 4:
                case 6:
                    switch (position) {
                        case 0:
                            return_value = "ULTIMA GIORNATA";
                            break;
                        case 1:
                            return_value = "CLASSIFICA";
                            break;
                        case 2:
                            return_value = "PROSSIMA GIORNATA";
                            break;
                    }
                    break;
                case 2:
                case 3:
                    return_value = "CLASSIFICA";
                    break;
                default:
                    switch (position) {
                        case 0:
                            return_value = "ULTIMA GIORNATA";
                            break;
                        case 1:
                            return_value = "PROSSIMA GIORNATA";
                            break;
                    }
                    break;
            }
            return return_value;
        }
    }

    private static class Timer extends CountDownTimer {
        LinearLayout text;

        Timer (final long date, long interval, View v) {
            super(date, interval);
            text = (LinearLayout) v;
        }

        @Override
        public void onFinish() {
            ((TextView)text.findViewById(R.id.d)).setText(R.string.zerozero);
            ((TextView)text.findViewById(R.id.h)).setText(R.string.zerozero);
            ((TextView)text.findViewById(R.id.m)).setText(R.string.zerozero);
            ((TextView)text.findViewById(R.id.s)).setText(R.string.zerozero);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            int days = (int) ((millisUntilFinished / 1000) / 86400);
            int hours = (int) (((millisUntilFinished / 1000)
                    - (days * 86400)) / 3600);
            int minutes = (int) (((millisUntilFinished / 1000)
                    - (days * 86400) - (hours * 3600)) / 60);
            int seconds = (int) ((millisUntilFinished / 1000) % 60);

            //String countdown = String.format("%02d : %02d : %02d : %02d", days, hours, minutes, seconds);
            ((TextView)text.findViewById(R.id.d)).setText(String.format(Locale.ITALY, "%02d", days));
            ((TextView)text.findViewById(R.id.h)).setText(String.format(Locale.ITALY, "%02d", hours));
            ((TextView)text.findViewById(R.id.m)).setText(String.format(Locale.ITALY, "%02d", minutes));
            ((TextView)text.findViewById(R.id.s)).setText(String.format(Locale.ITALY, "%02d", seconds));
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class DownloadTask extends AsyncTask<String, Integer, Giocatore [][]> {
        ProgressBar pb = findViewById(R.id.progressBar);
        boolean recap;

        @Override
        protected void onPreExecute () {
            if (findViewById(R.id.spinner2).getVisibility() == View.VISIBLE) {
                findViewById(R.id.fab).animate().rotationBy(90).setDuration(150);
                findViewById(R.id.spinner2).setVisibility(GONE);
            }
            pb.setProgress(0);
            pb.setVisibility(View.VISIBLE);
        }

        @Override
        protected Giocatore [][] doInBackground(String... params) {
            String [] partite = downloadPartite();
            recap = partite.length != 1;
            if (recap) matches = diretta(partite);
            publishProgress(15);
            try {
                URL url = new URL("https://leghe.fantacalcio.it/servizi/V1_LegheLive/Visualizza?alias_lega=" + HttpRequest.lega +
                        "&id_comp=" + comp.codice + "&id_squadra=" + params[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestProperty("app_key", "4ab27d6de1e92c810c6d4efc8607065a735b917f");
                connection.setRequestProperty("cookie", HttpRequest.cookie);

                BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                JSONObject json = new JSONObject(rd.readLine());
                if(!json.getBoolean("success") && !json.getString("error_msgs").equals("null") && json.getJSONArray("error_msgs").getJSONObject(0).getString("id").equals("L002")) {
                    HttpRequest.GET("/area-gioco/formazioni?id=" + comp.codice, "breadcumb");

                    connection = (HttpURLConnection) url.openConnection();

                    connection.setRequestProperty("app_key", "4ab27d6de1e92c810c6d4efc8607065a735b917f");
                    connection.setRequestProperty("cookie", HttpRequest.cookie);

                    rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    json = new JSONObject(rd.readLine());
                }
                JSONArray json_data = json.getJSONObject("data").getJSONArray("formazioni").getJSONObject(0).getJSONArray("sq");
                publishProgress(65);
                Giocatore[] casa = downloadLive(partite, json_data.getJSONObject(0));
                publishProgress(80);
                Giocatore[] trasf = downloadLive(partite, json_data.length() > 1 ? json_data.getJSONObject(1) : null);
                publishProgress(95);
                // TODO: vedere se c'è soluzione più efficace
                // check recupero
                return new Giocatore[][]{casa, trasf};
            } catch (IOException | JSONException e) {
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            pb.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(final Giocatore [][] result) {
            double c;
            double t;
            if (srl.isRefreshing()) srl.setRefreshing(false);

            if (recap) {
                ListView rightList = findViewById(R.id.right_drawer);
                rightList.setAdapter(new PallinoAdapter(MainActivity.this, matches, R.layout.serie_a, new String[]{"casa", "trasf", "orario", "data", "risultato"},
                        new int[]{R.id.casa, R.id.trasf, R.id.orario, R.id.data, R.id.result}));

                rightList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        if (matches.get(i).get("stato").equals("blue")) {
                            Toast.makeText(MainActivity.this, "Partita non ancora iniziata", Toast.LENGTH_SHORT).show();
                        } else if(matches.get(i).get("data").equals("-") || matches.get(i).get("risultato").equals("-")) {
                            Toast.makeText(MainActivity.this, "Live non disponibile", Toast.LENGTH_SHORT).show();
                        } else {
                            Intent fg = new Intent(MainActivity.this, PartitaLive.class);
                            fg.putExtra("title", "Voti & Cronaca");
                            fg.putExtra("giornata", serieA);
                            fg.putExtra("match", matches.get(i).get("casa").toUpperCase() + " - " + matches.get(i).get("trasf").toUpperCase());
                            startActivity(fg);
                        }
                    }
                });
            } else {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END);
            }

            HashMap<String, String> risultato = new HashMap<>();

            final ListView listc = findViewById(R.id.listView1);
            risultato.put("casa", result[0][0].getFanta());
            c = popolaLista(listc, risultato, result[0]);

            final ListView listt = findViewById(R.id.listView2);
            risultato.put("trasf", result[1][0].getFanta());
            t = popolaLista(listt, risultato, result[1]);

            final boolean[] state = new boolean[] {false, false};
            listc.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView absListView, int i) {}

                @Override
                public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    state[0] = canScrollUp(listc);
                    srl.setEnabled(state[0] && state[1]);
                }
            });
            listt.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView absListView, int i) {
                }

                @Override
                public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    state[1] = canScrollUp(listt);
                    srl.setEnabled(state[0] && state[1]);
                }
            });

            srl.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    srl.setRefreshing(true);
                    new DownloadTask().execute(current);
                }
            });

            risultato.put("punteggio", (c == 0 ? "    " : c) + (c == 0 || t == 0 ? "       " : "   -   ") + (t == 0 ? "    " : t));

            if (result[0].length > 1 && result[1].length > 1) {
                int [] score = risultato(c, t);
                risultato.put("result", score[0] + "   -   " + score[1]);

                pb.setProgress(100);
                pb.setVisibility(GONE);
            }
            /*else if (result[0].length == 1 && result[1].length == 1) {
                if (!info.equals("")) {
                    Toast.makeText(MainActivity.this, info, Toast.LENGTH_LONG).show();
                }
            }*/

            List<HashMap<String,String>> feedlist = new ArrayList<>();
            feedlist.add(risultato);
            ListView p1 = findViewById(R.id.risultato);
            SimpleAdapter adapter1 =
                    new SimpleAdapter (MainActivity.this, feedlist, R.layout.calenlist, new String []{"casa", "trasf", "punteggio", "result"}, new int [] {R.id.casa, R.id.trasf, R.id.punteggio, R.id.result});
            p1.setAdapter(adapter1);

            p1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (opzioni.mod_checked) {
                        Toast.makeText(MainActivity.this, "Modificatore " + result[0][0].getFanta() + ": " + mod_dif[0] + "\nModificatore " + result[1][0].getFanta() + ": " + mod_dif[1], Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        private double popolaLista (ListView list, HashMap<String, String> risultato, final Giocatore [] giocatori) {
            double punteggio;
            if (giocatori.length == 1) {
                risultato.put("result", "");
                ArrayAdapter<String> ad = new ArrayAdapter<>(MainActivity.this, R.layout.row, R.id.row, new String[]{""});
                list.setAdapter(ad);
                punteggio = 0;
                pb.setVisibility(GONE);
            } else {
                punteggio = calcola(giocatori, list.getId());

                HeaderAdapter simpleAdapter = new HeaderAdapter(MainActivity.this, giocatori, true);
                list.setAdapter(simpleAdapter);
                registerForContextMenu(list);

                list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View v,
                                            int position, long id) {

                        @SuppressLint("InflateParams")
                        LinearLayout cr = (LinearLayout) getLayoutInflater().inflate(R.layout.custom_toast, null);
                        Toast t = new Toast(MainActivity.this);
                        TextView prec = cr.findViewById(R.id.squadra);
                        String stato = giocatori[position].getStato();
                        String text = giocatori[position].getSquad() + "   " + (stato.equals("") ? " -- " : giocatori[position].getVotoReale() + "   ");
                        prec.setText(text);
                        String [] bonus = giocatori[position].getBonus();
                        if (bonus != null) {
                            for (int i = 0; i < bonus.length; i++) {
                                LinearLayout interno;
                                if (i < 4) {
                                    interno = cr.findViewById(R.id.riga1);
                                } else {
                                    interno = cr.findViewById(R.id.riga2);
                                    interno.setVisibility(View.VISIBLE);
                                }
                                ImageView image = new ImageView(MainActivity.this);
                                image.setPadding(3, 3, 3, 3);
                                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
                                layoutParams.gravity = Gravity.CENTER_VERTICAL;
                                image.setLayoutParams(layoutParams);
                                image.setImageResource(getDrawable(MainActivity.this, bonus[i]));
                                interno.addView(image, i % 4);
                            }
                        }
                        TextView succ = cr.findViewById(R.id.stato);
                        String temp = "   " + stato;
                        succ.setText(temp);

                        t.setView(cr);
                        t.show();
                    }
                });
            }
            return punteggio;
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

        @Override
        protected void onCancelled(Giocatore [][] result) {
            Toast.makeText(MainActivity.this, "Recupero formazioni dal sistema", Toast.LENGTH_SHORT).show();
            String codice = comp.defaultcode;
            int i = 0;
            boolean found = false;
            while (!found && i<comp.codici[1].length) {
                if (comp.codici[1][i].equals(result[0][0].getFanta())) {
                    codice = comp.codici[0][i];
                    found = true;
                }
                i++;
            }
            current = codice;
            new DownloadTask().execute(current);
        }
    }

    static int getDrawable(Context context, String name) {
        Assert.assertNotNull(context);
        Assert.assertNotNull(name);

        return context.getResources().getIdentifier(name,
                "drawable", context.getPackageName());
    }

    boolean live = false;
    @SuppressLint("StaticFieldLeak")
    private class Validation extends AsyncTask<String, String, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            return live = verificaLive();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                if (comp.codici[0].length == 0) {
                    new NoLive().execute();
                } else {
                    visualizza();
                    invalidateOptionsMenu();
                }
            } else {
                new NoLive().execute();
            }
        }
    }

    private void selezionaLega (final int l, final int c) {
        if (isNetworkAvailable()) {
            progressDialog.show();
            HttpRequest.cookie = (String) HttpRequest.getObject(MainActivity.this, "cookie");
            String [] leghe = (String[]) HttpRequest.getObject(MainActivity.this, "leghe");
            String [] squadre = (String[]) HttpRequest.getObject(MainActivity.this, "squadre");

            HttpRequest.lega = leghe[l];
            squadra = squadre[l];
            opzioni = (Opzioni_lega) HttpRequest.getObject(MainActivity.this, leghe[l]);
            if (opzioni.competizioni != null) {
                comp = opzioni.competizioni[c];
            } else {
                comp = null;
            }
            selected = l;
            HttpRequest.serialize(this, l, "indice_lega");
            HttpRequest.serialize(this, c, "indice_comp");
            invalidateOptionsMenu();
            if (comp != null) {
                if (mPagerAdapter != null) {
                    mPagerAdapter.notifyDataSetChanged();
                }
                new Validation().execute();
            } else {
                new NoComp().execute();
            }
        }
        else {
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage("Connesione internet assente")
                    .setCancelable(false)
                    .setPositiveButton("RIPROVA ", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            selezionaLega(l, c);
                        }
                    }).create().show();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class Download_opzioni extends AsyncTask<String, String, Void> {
        ProgressDialog p;

        @Override
        protected void onPreExecute() {
            p = new ProgressDialog(MainActivity.this);
            p.setMessage("Recupero opzioni calcolo " + HttpRequest.lega + "...");
            p.setCancelable(false);
            p.show();
        }

        @Override
        protected Void doInBackground(String... strings) {

            try {
                String id_utente = opzioni.id_utente;
                opzioni = new Opzioni_lega();

                Document doc = HttpRequest.GET("/gestione-lega/opzioni-rose", "btn btn-fab btn-fab-mini");
                Elements scripts = doc.select("script");
                String default_code = null;
                JSONObject bonus = null;
                for (Element e : scripts) {
                    String toString = e.toString();
                    if (toString.contains("__.s('li',")) {
                        default_code = toString.split("teamId: \"")[1].split("\"")[0];
                        String id_admin = toString.split("admins: \\[\\{\"id\":")[1].split(",")[0];
                        opzioni.id_utente = id_utente;
                        opzioni.admin = id_utente.equals(id_admin);
                    }

                    if (toString.contains("__.s('lo',")) {
                        bonus = new JSONObject(new String(Base64.decode(toString.split("'")[3].split("'")[0], Base64.DEFAULT)));
                    }
                }

                if (bonus != null) {
                    JSONObject calcolo = bonus.getJSONObject("opzioni_calcolo");
                    //JSONObject formazioni = bonus.getJSONObject("opzioni_formazioni");
                    JSONObject rose = bonus.getJSONObject("opzioni_rose");
                    opzioni.calciatori_per_ruolo[0] = rose.getJSONObject("calciatori_ruolo").getInt("p");
                    opzioni.calciatori_per_ruolo[1] = rose.getJSONObject("calciatori_ruolo").getInt("d");
                    opzioni.calciatori_per_ruolo[2] = rose.getJSONObject("calciatori_ruolo").getInt("c");
                    opzioni.calciatori_per_ruolo[3] = rose.getJSONObject("calciatori_ruolo").getInt("a");
                    opzioni.crediti = rose.getInt("crediti");
                    opzioni.bonus = calcolo.getString("bonus_malus");

                    JSONObject sostituzioni = calcolo.getJSONObject("sostituzioni");
                    JSONObject assegna_voto_sv = calcolo.getJSONObject("assegna_voto_sv");
                    opzioni.numsost = sostituzioni.getInt("numero");
                    opzioni.ammsv_checked = assegna_voto_sv.getBoolean("attivo");
                    if (opzioni.ammsv_checked)
                        opzioni.ammsv_value = assegna_voto_sv.getDouble("ammonito");

                    JSONObject soglie = calcolo.getJSONObject("soglie");
                    opzioni.base = soglie.getDouble("soglia");
                    opzioni.fascia = soglie.getJSONArray("fasce").getDouble(0);
                    opzioni.intorno_value = soglie.getDouble("intorno_interno");
                    opzioni.intorno_checked = opzioni.intorno_value != 0;
                    opzioni.pareggio_value = soglie.getDouble("controlla_pareggio");
                    opzioni.pareggio_checked = opzioni.pareggio_value != 0;
                    opzioni.differenza_value = soglie.getDouble("gol_extra");
                    opzioni.differenza_checked = opzioni.differenza_value != 0;

                    JSONObject mod = calcolo.getJSONObject("modificatori");
                    opzioni.mod_checked = mod.getBoolean("difesa_attivo");
                    if (opzioni.mod_checked) {
                        opzioni.mod = new double[3];
                        opzioni.mod[0] = mod.getJSONArray("difesa").getDouble(2);
                        opzioni.mod[1] = mod.getJSONArray("difesa").getDouble(4);
                        opzioni.mod[2] = mod.getJSONArray("difesa").getDouble(5);
                    }
                }

                doc = HttpRequest.GET("/gestione-lega/partecipanti", "container navbar invite-navbar");

                scripts = doc.select("script");
                JSONArray squads = null;
                for (Element e : scripts) {
                    String toString = e.toString();
                    if (toString.contains("__.s('lm',")) {
                        squads = new JSONArray(new String(Base64.decode(toString.split("'")[3].split("'")[0], Base64.DEFAULT)));
                        for (int j = 0; j < squads.length(); j++) {
                            if (!squads.getJSONObject(j).getBoolean("main")) {
                                squads.remove(j);
                                j--;
                            }
                        }
                    }
                }

                doc = HttpRequest.GET("/lista-competizioni", "btn btn-fab btn-fab-mini");

                /*URL image_url = new URL(doc.select("img[id=logo_lega]").attr("src"));
                Bitmap bitmap = BitmapFactory.decodeStream(image_url.openStream());
                if (bitmap != null) {
                    String imageFileName = leghe[k] + ".png";
                    File rootPath = new File(Environment.getExternalStorageDirectory(), "LIVE");
                    if (!rootPath.exists()) {
                        rootPath.mkdir();
                    }
                    File image = new File(rootPath, imageFileName);
                    FileOutputStream out = new FileOutputStream(image);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                }*/

                scripts = doc.select("script");
                JSONArray comps = null;
                for (Element e : scripts) {
                    String toString = e.toString();
                    if (toString.contains("__.s('c',")) {
                        comps = new JSONArray(new String(Base64.decode(toString.split("'")[3].split("'")[0], Base64.DEFAULT)));
                    }
                }

                if (comps != null && squads != null && comps.length() != 0 && !comps.getJSONObject(0).getString("id_lega").equals("0")) {
                    opzioni.competizioni = new Competizione[comps.length()];

                    for (int i = 0; i < comps.length(); i++) {
                        JSONObject comp = comps.getJSONObject(i);
                        String nome = comp.getString("nome");
                        String codice = comp.getString("id");

                        int tipo = comp.getInt("tipo");

                        JSONArray drop_squadre = comp.getJSONArray("squadre");
                        for (int j = 0; j < drop_squadre.length(); j++) {
                            if (!drop_squadre.getJSONObject(j).getBoolean("main")) {
                                drop_squadre.remove(j);
                                j--;
                            }
                        }
                        String[][] codici = new String[2][drop_squadre.length()];
                        for (int j = 0; j < drop_squadre.length(); j++) {
                            codici[0][j] = drop_squadre.getJSONObject(j).getString("id");
                            int t = 0;
                            boolean found = false;
                            while (!found) {
                                JSONObject squad = squads.getJSONObject(t);
                                if(squad.getString("id_squadra").equals(codici[0][j])) {
                                    codici[1][j] = squad.getString("team");
                                    found = true;
                                }
                                t++;
                            }
                        }

                        opzioni.competizioni[i] = new Competizione(nome, codice, tipo, codici, default_code);
                    }
                } else if (squads != null) {
                    opzioni.squadre = new String[2][squads.length()];
                    for (int i = 0; i < squads.length(); i++) {
                        JSONObject squad = squads.getJSONObject(i);
                        opzioni.squadre[1][i] = squad.getString("team");
                        opzioni.squadre[0][i] = squad.getString("id_squadra");
                    }
                }

                HttpRequest.serialize(MainActivity.this, opzioni, HttpRequest.lega);
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            p.cancel();
            selezionaLega((int) HttpRequest.getObject(MainActivity.this, "indice_lega"), (int) HttpRequest.getObject(MainActivity.this, "indice_comp"));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                selezionaLega(0, 0);
            } else {
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        if (live && comp != null) {
            menu.findItem(R.id.action_refresh).setVisible(true);
            menu.findItem(R.id.action_class).setVisible(true);
        } else {
            menu.findItem(R.id.action_refresh).setVisible(false);
            menu.findItem(R.id.action_class).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            if (mDrawerLayout.isDrawerOpen(GravityCompat.END)) {
                mDrawerLayout.closeDrawer(GravityCompat.END);
            } else {
                if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    mDrawerLayout.openDrawer(GravityCompat.START);
                }
            }
            return true;
        }

        if (id == R.id.action_refresh) {
            current = comp.defaultcode;
            new DownloadTask().execute(current);
            return true;
        }

        if (id == R.id.action_class) {
            Intent intent = new Intent(MainActivity.this, ClassificaGiornata.class);
            intent.putExtra("comp", comp.codice);
            intent.putExtra("tipo", comp.tipo);
            intent.putExtra("squadre", comp.codici[1]);
            intent.putExtra("codici", comp.codici[0]);
            startActivity(intent);
            return true;
        }

        if (id == R.id.action_ricarica) {
            new Download_opzioni().execute();
        }

        if (id == R.id.action_voti) {
            Intent go = new Intent(this, PartitaLive.class);
            go.putExtra("title", "fantacalcio");
            go.putExtra("uri", "https://www.fantacalcio.it/live-serie-a");
            startActivity(go);
            return true;
        }

        if (id == R.id.action_logout) {
            logOut();
            startActivityForResult(new Intent(this, LoginActivity.class), 1);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void logOut() {
        boolean ok = false;
        deleteFile("cookie");
        String [] leghe = (String[]) HttpRequest.getObject(MainActivity.this, "leghe");
        deleteFile("leghe");
        for (String lega : leghe) {
            deleteFile(lega);
        }
        File rootPath = new File(Environment.getExternalStorageDirectory(), "LIVE");
        if (rootPath.exists()) {
            File [] files = rootPath.listFiles();
            if (files != null) {
                for (File f : files) {
                    ok = f.delete();
                }
            }
            System.out.println("FILE: " + ok);
            ok = rootPath.delete();
        }
        System.out.println("FOLDER: " + ok);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getBooleanExtra("live", false)) {
            current = comp.defaultcode;
            new DownloadTask().execute(current);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.listView1 || v.getId() == R.id.listView2) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            ListView t = (ListView) v;
            String squadra = ((Giocatore) t.getAdapter().getItem(info.position)).getSquad();
            boolean found = false; int i = 0;
            String casa = "";
            String trasf = "";
            while (!found) {
                if(matches.get(i).get("casa").contains(squadra) || matches.get(i).get("trasf").contains(squadra)) {
                    casa = matches.get(i).get("casa");
                    trasf = matches.get(i).get("trasf");
                    found = true; i--;
                }
                i++;
            }
            match = casa + " - " + trasf;
            menu.setHeaderTitle(match);
            if (!matches.get(i).get("stato").equals("blue")
                    && !matches.get(i).get("data").equals("-") && !matches.get(i).get("risultato").equals("-")) {
                menu.add("Voti & Cronaca");
            }
            menu.add("Pagina fantacalcio");
            uri = "https://www.fantacalcio.it/live-serie-a/" + casa + "#voti";
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Intent fg = new Intent(this, PartitaLive.class);
        fg.putExtra("title", item.getTitle());
        fg.putExtra("uri", uri);
        fg.putExtra("giornata", serieA);
        fg.putExtra("match", match);
        startActivity(fg);
        return true;
    }

    private List<HashMap<String, String>> diretta(String[] p) {
        List<HashMap<String, String>> matches = new ArrayList<>();

        for (String partita : p) {
            String [] squadre = getResources().getStringArray(R.array.squadre);
            HashMap<String, String> map = new HashMap<>();
            String [] splitted = partita.split("<>");
            String casa = splitted[0].substring(0, 3);
            boolean found = false;
            int j = 0;
            while (!found) {
                if(squadre[j].contains(casa)) {casa = squadre[j]; found = true;}
                j++;
            }
            splitted[0] = splitted[0].replace(casa, "");
            String trasf = splitted[0].substring(0, 3);
            found = false;
            j = 0;
            while (!found) {
                if(squadre[j].contains(trasf)) {trasf = squadre[j]; found = true;}
                j++;
            }
            map.put("casa", casa);
            map.put("trasf", trasf);
            map.put("stato", splitted[1]);
            switch (splitted[1]) {
                case "grey":
                    map.put("orario", "FT");
                    break;
                case "orange":
                    map.put("orario", "HT");
                    break;
                default:
                    map.put("orario", splitted[2]);
                    break;
            }
            map.put("data", splitted[3]);
            map.put("risultato", splitted[4]);
            matches.add(map);
        }

        return matches;
    }

    private static class PallinoAdapter extends SimpleAdapter {

        String [] stato;

        PallinoAdapter(Context c, List<HashMap<String, String>> d, int r, String[] f, int[] t) {
            super(c, d, r, f, t);
            stato = new String[d.size()];
            for (int i = 0; i < d.size(); i++) {
                stato[i] = d.get(i).get("stato");
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);

            Pallino p = v.findViewById(R.id.pallino);
            switch (stato[position]) {
                case "blue":
                    p.setColor(Color.BLUE);
                    break;
                case "grey":
                    p.setColor(Color.GRAY);
                    break;
                case "orange":
                    p.setColor(Color.rgb(242, 140, 18));
                    break;
                case "status":
                    p.setColor(Color.GREEN);
                    break;
            }
            return v;
        }
    }
}