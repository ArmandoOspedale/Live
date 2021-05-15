package com.live;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class Formazioni extends AppCompatActivity {

    private String [] codici;
    private String [] squadre;
    private String codice;
    String comp;
    private int tipo;
    int position = -1;
    private boolean giornate = false;
    private String invisualizzazione = "";
    private boolean current = false;
    Giocatore [] casa;
    Giocatore [] trasf;
    private SwipeRefreshLayout srl;
    private FloatingActionButton fab;
    ListView list;
    private View pressed;
    private HeaderAdapter ha1;
    private HeaderAdapter ha2;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.intform);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
        setContentView(R.layout.formazioni);
        srl = findViewById(R.id.srl);
        srl.setColorSchemeColors(Color.rgb(18, 116, 175));

        Intent i = getIntent();
        squadre = i.getStringArrayExtra("squadre");
        codici = i.getStringArrayExtra("codici");
        codice = i.getStringExtra("select");
        comp = i.getStringExtra("comp");
        tipo = i.getIntExtra("tipo", 0);

        new DownloadForm().execute(codice, "");

        fab = findViewById(R.id.fab);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) fab.getLayoutParams();
            float density = getResources().getDisplayMetrics().density;
            p.setMargins((int) (1 * density), (int) (-4 * density), (int) (-4 * density), (int) (-4 * density));
            fab.setLayoutParams(p);
        }
        list = findViewById(R.id.spinner);
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, squadre);
        list.setAdapter(adapter2);
        list.setOnItemClickListener((adapterView, view, i1, l) -> {
            //if (i != 0) {
                fab.animate().rotationBy(-90).setDuration(150);
                list.setVisibility(View.GONE);
                position = i1;
                new DownloadForm().execute(codici[position], invisualizzazione);
            //}
        });
        fab.setOnClickListener(v -> {
            if (list.getVisibility() == View.VISIBLE) {
                fab.animate().rotationBy(-90).setDuration(150);
                list.setVisibility(View.GONE);
            } else {
                fab.animate().rotationBy(90).setDuration(150);
                list.setVisibility(View.VISIBLE);
            }
        });
    }

    private List<HashMap<String, String>> punteggi;
    @SuppressLint("StaticFieldLeak")
    private class DownloadForm extends AsyncTask<String, Integer, String[]> {

        HashMap<String, String> risultato;
        boolean c = true;
        boolean f = true;
        int g;

        @Override
        protected String[] doInBackground(String... params) {

            String[] result = new String[2];
            String[] tmp = null;
            punteggi = null;

            try {
                Document doc = HttpRequest.GET("/area-gioco/formazioni/" + params[1] + "?id=" + comp, "breadcumb");
                tmp = doc.select("script[id=sky12]").get(0).toString().split("\\('tmp', \"")[1].split("\"\\)")[0].split("\\|");

                URL url = new URL("https://leghe.fantacalcio.it/servizi/V1_LegheFormazioni/Pagina?id_comp=" + comp + "&r=" + (params[1].equals("") ? tmp[3] : params[1]) + "&f=" + tmp[0] + "_" + tmp[1] + ".json");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestProperty("app_key", "4ab27d6de1e92c810c6d4efc8607065a735b917f");

                BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                JSONObject json = new JSONObject(rd.readLine()).getJSONObject("data");
                String codice = params[0];
                String squadra = HttpRequest.getNomeSquadra(codice, codici, squadre);

                int ultima = Integer.parseInt(tmp[3]);
                JSONArray formazioni = json.getJSONArray("formazioni");
                if (formazioni.length() > 0) {
                    g = json.getInt("giornataLega");
                    if (g >= ultima) {
                        int confronto;
                        if (invisualizzazione.equals("")) {
                            confronto = g;
                        } else {
                            confronto = Integer.parseInt(invisualizzazione);
                        }
                        if (confronto == g) {
                            current = true;
                        }
                    }

                    risultato = new HashMap<>();
                    int i = 0;
                    boolean found = false;
                    if (!current) {
                        punteggi = new ArrayList<>();
                        HashMap<String, String> header = new HashMap<>();
                        header.put("squadra", "Classifica di giornata");
                        punteggi.add(header);
                        for (int j = 0; j < formazioni.length(); j++) {
                            JSONObject formazione = formazioni.getJSONObject(j);
                            JSONArray jsonSquadre = formazione.getJSONArray("sq");
                            for (int s = 0; s < jsonSquadre.length(); s++) {
                                HashMap<String, String> item = new HashMap<>();
                                item.put("squadra", HttpRequest.getNomeSquadra(jsonSquadre.getJSONObject(s).getString("id"), codici, squadre));
                                item.put("punti", jsonSquadre.getJSONObject(s).getString("t"));
                                punteggi.add(item);
                            }
                        }
                        Collections.sort(punteggi, (o1, o2) -> {
                            if (Objects.equals(o1.get("squadra"), "Classifica di giornata")) {
                                return 1;
                            }
                            if (Objects.equals(o2.get("squadra"), "Classifica di giornata")) {
                                return 0;
                            }
                            return Double.compare(
                                    Double.parseDouble(Objects.requireNonNull(o2.get("punti"))),
                                    Double.parseDouble(Objects.requireNonNull(o1.get("punti"))));
                        });
                    }
                    if (tipo == 2 || tipo == 3) {
                        //if (!current && punteggi == null) {
                        while (!found && i < formazioni.length()) {
                            JSONObject formazione = formazioni.getJSONObject(i);
                            if (tipo == 2) {
                                if (formazione.getJSONArray("sq").getJSONObject(0).getString("id").equals(codice)) {
                                    found = true;
                                    risultato.put("casa", squadra);
                                }
                            } else {
                                if (formazione.getJSONArray("sq").getJSONObject(0).getString("id").equals(codice)) {
                                    found = true;
                                    risultato.put("casa", squadra);
                                    if (!current) {
                                        String punti = formazione.getString("rsr");
                                        risultato.put("punteggio", (punti.contains(",") ? punti : punti + ",0"));
                                        risultato.put("risultato", formazione.getString("rsr")); //TODO verificare
                                    }
                                }
                            }
                            i++;
                        }
                        if (found) {
                            i--;
                        } else {
                            risultato.put("casa", squadra);
                            c = false;
                            casa = null;
                            f = false;
                            trasf = null;
                            result[0] = "Formazione non inserita";
                        }
                    } else {
                        while (!found) {
                            JSONObject formazione = formazioni.getJSONObject(i);
                            JSONArray form_squadre = formazione.getJSONArray("sq");
                            if (form_squadre.getJSONObject(0).getString("id").equals(codice) || form_squadre.getJSONObject(1).getString("id").equals(codice)) {
                                found = true;
                                risultato.put("casa", HttpRequest.getNomeSquadra(form_squadre.getJSONObject(0).getString("id"), codici, squadre));
                                if (!formazione.getString("r").equals("-")) {
                                    risultato.put("trasf", HttpRequest.getNomeSquadra(form_squadre.getJSONObject(1).getString("id"), codici, squadre));
                                    risultato.put("risultato", formazione.getString("r"));
                                } else {
                                    risultato.put("trasf", HttpRequest.getNomeSquadra(form_squadre.getJSONObject(1).getString("id"), codici, squadre));
                                    risultato.put("risultato", "");
                                    risultato.put("punteggio", "");
                                }
                                i--;
                            }
                            i++;
                        }
                    }

                    if (found) {
                        String [] bonus_string = {"amm", "esp_s", "golfatto_s", "golsubito_s", "assist_s", "assistf_s", "rigoreparato_s",
                                "rigoresbagliato_s", "rigoresegnato_s", "autogol_s", "golvittoria_s", "golpareggio_s",
                                "portiereimbattuto_s", "uscito_s", "entrato_s"};

                        if (tipo == 2 || tipo == 3) {
                            JSONObject json_squadra = formazioni.getJSONObject(i).getJSONArray("sq").getJSONObject(0);
                            String pl = json_squadra.getString("pl");

                            String punteggio_casa = "";
                            if (formazioni.getJSONObject(i).has("c") && !formazioni.getJSONObject(i).getString("c").equals("null") && formazioni.getJSONObject(i).getBoolean("c")) {
                                punteggio_casa = json_squadra.getString("t");
                            }

                            risultato.put("punteggio", punteggio_casa);

                            if (!pl.equals("null") && json_squadra.getInt("v") == 1) {
                                JSONArray players1 = new JSONArray(pl);
                                casa = new Giocatore[players1.length()];

                                for (int j = 0; j < players1.length(); j++) {
                                    String nome = players1.getJSONObject(j).getString("n");
                                    char ruolo = players1.getJSONObject(j).getString("r").charAt(0);
                                    String squad = players1.getJSONObject(j).getString("t").toUpperCase();
                                    String voto = players1.getJSONObject(j).getString("fv");
                                    if (voto.equals("100")) voto = "-";
                                    String votoReale = players1.getJSONObject(j).getString("vt");
                                    if (votoReale.equals("56") || votoReale.equals("55"))
                                        votoReale = "-";
                                    JSONArray bonus = players1.getJSONObject(j).getJSONArray("b");
                                    List<String> temp = new ArrayList<>();
                                    for (int k = 0; k < bonus.length(); k++) {
                                        if (bonus.getInt(k) > 0) {
                                            for (int l = 0; l < bonus.getInt(k); l++) {
                                                temp.add(bonus_string[k]);
                                            }
                                        }
                                    }
                                    switch (players1.getJSONObject(j).getString("s")) {
                                        case "E":
                                            temp.add(bonus_string[14]);
                                            break;
                                        case "U":
                                            temp.add(bonus_string[13]);
                                            break;
                                        default:
                                            break;
                                    }
                                    String[] stockArr = new String[temp.size()];
                                    stockArr = temp.toArray(stockArr);
                                    casa[j] = (new Giocatore(nome, ruolo, squad, voto, votoReale, stockArr));
                                }
                            } else if (!pl.equals("null") && json_squadra.getInt("v") == 0) {
                                c = false;
                                casa = null;
                                result[0] = "Formazione invisibile";
                            } else {
                                c = false;
                                casa = null;
                                result[0] = "Formazione non inserita";
                            }
                            f = false;
                            trasf = null;
                        } else {
                            JSONArray json_squadre = formazioni.getJSONObject(i).getJSONArray("sq");
                            String pl = json_squadre.getJSONObject(0).getString("pl");

                            String punteggio_casa = "";
                            if (formazioni.getJSONObject(i).has("c") && !formazioni.getJSONObject(i).getString("c").equals("null") && formazioni.getJSONObject(i).getBoolean("c")) {
                                punteggio_casa = json_squadre.getJSONObject(0).getString("t");
                            }

                            if (!pl.equals("null") && json_squadre.getJSONObject(0).getInt("v") == 1) {
                                JSONArray players1 = new JSONArray(pl);
                                casa = new Giocatore[players1.length()];

                                for (int j = 0; j < players1.length(); j++) {
                                    String nome = players1.getJSONObject(j).getString("n");
                                    char ruolo = players1.getJSONObject(j).getString("r").charAt(0);
                                    String squad = players1.getJSONObject(j).getString("t").toUpperCase();
                                    String voto = players1.getJSONObject(j).getString("fv");
                                    if (voto.equals("100")) voto = "-";
                                    String votoReale = players1.getJSONObject(j).getString("vt");
                                    if (votoReale.equals("56") || votoReale.equals("55"))
                                        votoReale = "-";
                                    JSONArray bonus = players1.getJSONObject(j).getJSONArray("b");
                                    List<String> temp = new ArrayList<>();
                                    for (int k = 0; k < bonus.length(); k++) {
                                        if (bonus.getInt(k) > 0) {
                                            for (int l = 0; l < bonus.getInt(k); l++) {
                                                temp.add(bonus_string[k]);
                                            }
                                        }
                                    }
                                    switch (players1.getJSONObject(j).getString("s")) {
                                        case "E":
                                            temp.add(bonus_string[14]);
                                            break;
                                        case "U":
                                            temp.add(bonus_string[13]);
                                            break;
                                        default:
                                            break;
                                    }
                                    String[] stockArr = new String[temp.size()];
                                    stockArr = temp.toArray(stockArr);
                                    casa[j] = (new Giocatore(nome, ruolo, squad, voto, votoReale, stockArr));
                                }
                            } else if (!pl.equals("null") && json_squadre.getJSONObject(0).getInt("v") == 0) {
                                c = false;
                                casa = null;
                                result[0] = "Formazione invisibile";
                            } else {
                                c = false;
                                casa = null;
                                result[0] = "Formazione non inserita";
                            }

                            pl = json_squadre.getJSONObject(1).getString("pl");

                            String punteggio_trasf = "";
                            if (formazioni.getJSONObject(i).has("c") && !formazioni.getJSONObject(i).getString("c").equals("null") && formazioni.getJSONObject(i).getBoolean("c")) {
                                punteggio_trasf = json_squadre.getJSONObject(1).getString("t");
                            }

                            risultato.put("punteggio", punteggio_casa + "  -  " + punteggio_trasf);

                            if (!pl.equals("null") && json_squadre.getJSONObject(1).getInt("v") == 1) {
                                JSONArray players2 = new JSONArray(pl);
                                trasf = new Giocatore[players2.length()];

                                for (int j = 0; j < players2.length(); j++) {
                                    String nome = players2.getJSONObject(j).getString("n");
                                    char ruolo = players2.getJSONObject(j).getString("r").charAt(0);
                                    String squad = players2.getJSONObject(j).getString("t").toUpperCase();
                                    String voto = players2.getJSONObject(j).getString("fv");
                                    if (voto.equals("100")) voto = "-";
                                    String votoReale = players2.getJSONObject(j).getString("vt");
                                    if (votoReale.equals("56") || votoReale.equals("55"))
                                        votoReale = "-";
                                    JSONArray bonus = players2.getJSONObject(j).getJSONArray("b");
                                    List<String> temp = new ArrayList<>();
                                    for (int k = 0; k < bonus.length(); k++) {
                                        if (bonus.getInt(k) > 0) {
                                            for (int l = 0; l < bonus.getInt(k); l++) {
                                                temp.add(bonus_string[k]);
                                            }
                                        }
                                    }
                                    switch (players2.getJSONObject(j).getString("s")) {
                                        case "E":
                                            temp.add(bonus_string[14]);
                                            break;
                                        case "U":
                                            temp.add(bonus_string[13]);
                                            break;
                                        default:
                                            break;
                                    }
                                    String[] stockArr = new String[temp.size()];
                                    stockArr = temp.toArray(stockArr);
                                    trasf[j] = new Giocatore(nome, ruolo, squad, voto, votoReale, stockArr);
                                }
                            } else if (!pl.equals("null") && json_squadre.getJSONObject(1).getInt("v") == 0) {
                                f = false;
                                trasf = null;
                                result[1] = "Formazione invisibile";
                            } else {
                                f = false;
                                trasf = null;
                                result[1] = "Formazione non inserita";
                            }
                        }
                    }
                } else {
                    g = Integer.parseInt(params[1].equals("") ? tmp[3] : params[1]);
                    c = false;
                    casa = null;
                    result[0] = "nessuna";
                    f = false;
                    trasf = null;
                    result[1] = "nessuna";
                }
            } catch (Exception e) {
                g = Integer.parseInt(tmp != null && params[1].equals("") ? tmp[3] : params[1]);
                c = false;
                casa = null;
                result[0] = "nessuna";
                f = false;
                trasf = null;
                result[1] = "nessuna";
            }
            return result;
        }

        @Override
        @SuppressWarnings("all")
        protected void onPostExecute(String[] result) {

            if (list.getVisibility() == View.VISIBLE) {
                fab.animate().rotationBy(-90).setDuration(150);
                list.setVisibility(View.GONE);
            }
            if (srl.isRefreshing()) srl.setRefreshing(false);
            if (!giornate) {
                LinearLayout selector = (LinearLayout) findViewById(R.id.selector);
                final HorizontalScrollView hsv = ((HorizontalScrollView) selector.getParent());
                hsv.setHorizontalScrollBarEnabled(false);
                final ViewTreeObserver viewTreeObserver = hsv.getViewTreeObserver();
                if (viewTreeObserver.isAlive()) {
                    viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            hsv.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
                            viewTreeObserver.removeOnGlobalLayoutListener(this);
                        }
                    });
                }
                for (int i = 1; i < g+1; i++) {
                    Button b = new Button(Formazioni.this);
                    float density = getResources().getDisplayMetrics().density;
                    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams((int) (50 * density), (int) (50 * density));
                    b.setLayoutParams(p);
                    if (i == g) {
                        pressed = b;
                        b.setBackground(getResources().getDrawable(R.drawable.selected_btn));
                    } else {
                        b.setBackground(getResources().getDrawable(R.drawable.custom_btn));
                    }
                    b.setText(String.valueOf(i));
                    b.setTextColor(Color.WHITE);
                    b.setTextSize(20);
                    b.setTypeface(Typeface.DEFAULT_BOLD);
                    b.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            pressed.setBackground(getResources().getDrawable(R.drawable.custom_btn));
                            pressed = view;
                            view.setBackground(getResources().getDrawable(R.drawable.selected_btn));
                            invisualizzazione = ((Button) view).getText().toString();
                            current = false;
                            new DownloadForm().execute(codice, invisualizzazione);
                        }
                    });
                    selector.addView(b);
                }
                selector.setVisibility(View.VISIBLE);
                findViewById(R.id.fab).setVisibility(View.VISIBLE);
                giornate = true;
            }

            List<HashMap<String,String>> feedlist = new ArrayList<>();
            feedlist.add(risultato);
            ListView p1 = (ListView) findViewById(R.id.risultato);
            SimpleAdapter adapter1 =
                new SimpleAdapter (Formazioni.this, feedlist, R.layout.calenlist, new String []{"casa", "trasf", "punteggio", "risultato"}, new int [] {R.id.casa, R.id.trasf, R.id.punteggio, R.id.result});
            p1.setAdapter(adapter1);

            final ListView list = (ListView) findViewById(R.id.casa);

            if (c) {
                findViewById(R.id.nessuna).setVisibility(View.GONE);
                p1.setVisibility(View.VISIBLE);
                list.setVisibility(View.VISIBLE);
                ha1 = new HeaderAdapter(Formazioni.this, casa, false);
                list.setAdapter(ha1);
                list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View v,
                                            int position, long id) {

                        LinearLayout cr = (LinearLayout) getLayoutInflater().inflate(R.layout.custom_toast, null);
                        Toast t = new Toast(Formazioni.this);
                        TextView prec = (TextView) cr.findViewById(R.id.squadra);
                        String stato = casa[position].getStato();
                        prec.setText(casa[position].getSquad() + "   " + casa[position].getStringaVotoReale() + "   ");
                        String[] bonus = casa[position].getBonus();
                        if (bonus != null) {
                            for (int i = 0; i < bonus.length; i++) {
                                ImageView image = new ImageView(Formazioni.this);
                                image.setPadding(3, 3, 3, 3);
                                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
                                layoutParams.gravity = Gravity.CENTER_VERTICAL;
                                image.setLayoutParams(layoutParams);
                                String a = bonus[i];
                                if (a.equals("amm_s")) a = "amm";
                                image.setImageResource(MainActivity.getDrawable(Formazioni.this, a));
                                cr.addView(image, i + 1);
                            }
                        }
                        if (stato != null) {
                            TextView succ = (TextView) cr.findViewById(R.id.stato);
                            succ.setText("   " + stato);
                        }
                        t.setView(cr);
                        t.show();
                    }
                });
            } else {
                if (result[0].equals("nessuna")) {
                    TextView t = (TextView) findViewById(R.id.nessuna);
                    t.setText("NESSUNA FORMAZIONE TROVATA PER LA COMPETIZIONE SELEZIONATA");
                    t.setVisibility(View.VISIBLE);
                    p1.setVisibility(View.GONE);
                    list.setVisibility(View.GONE);
                } else {
                    ArrayAdapter<String> ad = new ArrayAdapter<>(Formazioni.this, R.layout.row2, R.id.row, new String[]{result[0]});
                    list.setAdapter(ad);
                }
            }

            final ListView list2 = (ListView) findViewById(R.id.trasf);

            if (f) {
                list2.setVisibility(View.VISIBLE);
                ha2 = new HeaderAdapter(Formazioni.this, trasf, false);
                list2.setAdapter(ha2);
                list2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View v,
                                            int position, long id) {

                        LinearLayout cr = (LinearLayout) getLayoutInflater().inflate(R.layout.custom_toast, null);
                        Toast t = new Toast(Formazioni.this);
                        TextView prec = (TextView) cr.findViewById(R.id.squadra);
                        String stato = trasf[position].getStato();
                        prec.setText(trasf[position].getSquad() + "   " + trasf[position].getStringaVotoReale() + "   ");
                        String[] bonus = trasf[position].getBonus();
                        if (bonus != null) {
                            for (int i = 0; i < bonus.length; i++) {
                                ImageView image = new ImageView(Formazioni.this);
                                image.setPadding(3, 3, 3, 3);
                                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
                                layoutParams.gravity = Gravity.CENTER_VERTICAL;
                                image.setLayoutParams(layoutParams);
                                String a = bonus[i];
                                if (a.equals("amm_s")) a = "amm";
                                image.setImageResource(MainActivity.getDrawable(Formazioni.this, a));
                                cr.addView(image, i + 1);
                            }
                        }
                        if (stato != null) {
                            TextView succ = (TextView) cr.findViewById(R.id.stato);
                            succ.setText("   " + stato);
                        }
                        t.setView(cr);
                        t.show();
                    }
                });
            } else {
                if (tipo != 2 && tipo != 3) {
                    if (result[1].equals("nessuna")) {
                        list2.setVisibility(View.GONE);
                    } else {
                        ArrayAdapter<String> ad = new ArrayAdapter<>(Formazioni.this, R.layout.row2, R.id.row, new String[]{result[1]});
                        list2.setAdapter(ad);
                    }
                } else if (!current && punteggi != null) {
                    SimpleAdapter ad =
                            new SimpleAdapter (Formazioni.this, punteggi, R.layout.giornata, new String []{"squadra", "punti"}, new int [] {R.id.squadra, R.id.punti});
                    list2.setAdapter(ad);
                } else {
                    list2.setAdapter(null);
                }
            }

            if (current) {
                srl.setEnabled(true);
                final boolean[] state = new boolean[] {false, false};
                list.setOnScrollListener(new AbsListView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(AbsListView absListView, int i) {}

                    @Override
                    public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                        state[0] = canScrollUp(list);
                        srl.setEnabled(state[0] && state[1]);
                    }
                });
                list2.setOnScrollListener(new AbsListView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(AbsListView absListView, int i) {}

                    @Override
                    public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                        state[1] = canScrollUp(list2);
                        srl.setEnabled(state[0] && state[1]);
                    }
                });

                srl.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        srl.setRefreshing(true);
                        if (position == -1) {
                            new DownloadForm().execute(codice, "");
                        } else {
                            new DownloadForm().execute(codici[position], "");
                        }
                    }
                });
            } else {
                srl.setEnabled(false);
                list.setOnScrollListener(null);
                list2.setOnScrollListener(null);
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

    @SuppressLint("StaticFieldLeak")
    private class Prob extends AsyncTask<String,Void,String[][]> {

        ProgressDialog pD;
        @Override
        protected void onPreExecute () {
            pD = new ProgressDialog(Formazioni.this);
            pD.setMessage("Recupero Probabili Formazioni...");
            pD.setCancelable(false);
            pD.show();
        }

        @Override
        protected String[][] doInBackground (String... param) {
            try {
                Document doc = HttpRequest.GET_nolega("https://www.fantacalcio.it/probabili-formazioni-serie-A", "<!-- FINE CONTAINER PRIMO BLOCCO CONTENUTO  SU DUE COLONNE -->");

                Elements matches = doc.select("div[class=match-list lazy-viewport]").get(0).children();

                if (casa != null) {
                    for (Giocatore g : casa) {
                        String nome = g.getNome();
                        String[] temp = nome.split(" ");
                        nome = temp[0].charAt(0) + temp[0].substring(1).toLowerCase();
                        if (temp.length > 1) {
                            nome = nome + " " + temp[1].charAt(0) + temp[1].substring(1).toLowerCase();
                        }
                        if (temp.length > 2) {
                            nome = nome + " " + temp[2];
                        }

                        String squadra = g.getSquad();
                        int j = 0;
                        boolean found = false;
                        boolean casa = true;
                        while (!found) {
                            String [] squadre = matches.get(j).attr("id").split("-");
                            if (squadre[0].startsWith(squadra.toLowerCase())) {
                                j--;
                                casa = true;
                                found = true;
                            } else if (squadre[1].startsWith(squadra.toLowerCase())) {
                                j--;
                                casa = false;
                                found = true;
                            }
                            j++;
                        }

                        Elements team;
                        if (casa) {
                            team = matches.get(j).select("div[class=col player-list home]").select("a");
                        } else {
                            team = matches.get(j).select("div[class=col player-list away]").select("a");
                        }

                        int k = 0;
                        found = false;
                        while (!found && k < team.size()) {
                            if (team.get(k).select("span[class=player-name]").text().equals(nome.toLowerCase())) {
                                k--;
                                found = true;
                            }
                            k++;
                        }

                        if (found) {
                            Elements perc = team.get(k).select("span[class=player-percentage-value]");
                            if (k <= 11) {
                                g.setStringaVoto(perc.get(0).text());
                            } else {
                                g.setStringaVoto("sub");
                                Elements ballottaggi = matches.get(j).select("div[class=col box]");
                                for (int i = 0; i < ballottaggi.size(); i++) {
                                    if (ballottaggi.get(i).select("span").size() == 0 || !ballottaggi.get(i).select("span").get(0).text().equals("BALLOTTAGGI")) {
                                        ballottaggi.remove(i);
                                        i--;
                                    }
                                }

                                HashMap<String, String> ballot = new HashMap<>();
                                Elements p = ballottaggi.select("p");
                                for (int i = 0; i < p.size(); i++) {
                                    Elements nomi = p.get(i).select("b");
                                    String[] stemp = p.get(i).text().split(" ");
                                    List<String> percs = new ArrayList<>();
                                    for (String s : stemp) {
                                        if (s.matches("[0-9][0-9]%")) {
                                            percs.add(s);
                                        }
                                    }
                                    if (percs.size() > 0) {
                                        for (int m = 0; m < nomi.size(); m++) {
                                            ballot.put(nomi.get(m).text().replaceFirst(" ", ""), percs.get(m));
                                        }
                                    }
                                }
                                if (ballot.get(nome) != null) {
                                    g.setStringaVoto(ballot.get(nome));
                                }
                            }
                        } else {
                            g.setStringaVoto("0%");
                        }
                    }
                }

                if (trasf != null) {
                    for (Giocatore g : trasf) {
                        String nome = g.getNome();
                        String[] temp = nome.split(" ");
                        nome = temp[0].charAt(0) + temp[0].substring(1).toLowerCase();
                        if (temp.length > 1) {
                            nome = nome + " " + temp[1].charAt(0) + temp[1].substring(1).toLowerCase();
                        }
                        if (temp.length > 2) {
                            nome = nome + " " + temp[2];
                        }

                        String squadra = g.getSquad();
                        int j = 0;
                        boolean found = false;
                        boolean casa = true;
                        while (!found) {
                            String [] squadre = matches.get(j).attr("id").split("-");
                            if (squadre[0].startsWith(squadra.toLowerCase())) {
                                j--;
                                casa = true;
                                found = true;
                            } else if (squadre[1].startsWith(squadra.toLowerCase())) {
                                j--;
                                casa = false;
                                found = true;
                            }
                            j++;
                        }

                        Elements team;
                        if (casa) {
                            team = matches.get(j).select("div[class=col player-list home]").select("a");
                        } else {
                            team = matches.get(j).select("div[class=col player-list away]").select("a");
                        }

                        int k = 0;
                        found = false;
                        while (!found && k < team.size()) {
                            if (team.get(k).select("span[class=player-name]").text().equals(nome.toLowerCase())) {
                                k--;
                                found = true;
                            }
                            k++;
                        }

                        if (found) {
                            Elements perc = team.get(k).select("span[class=player-percentage-value]");
                            if (k <= 11) {
                                g.setStringaVoto(perc.get(0).text());
                            } else {
                                g.setStringaVoto("sub");
                                Elements ballottaggi = matches.get(j).select("div[class=col box]");
                                for (int i = 0; i < ballottaggi.size(); i++) {
                                    if (ballottaggi.get(i).select("span").size() == 0 || !ballottaggi.get(i).select("span").get(0).text().equals("BALLOTTAGGI")) {
                                        ballottaggi.remove(i);
                                        i--;
                                    }
                                }

                                HashMap<String, String> ballot = new HashMap<>();
                                Elements p = ballottaggi.select("p");
                                for (int i = 0; i < p.size(); i++) {
                                    Elements nomi = p.get(i).select("b");
                                    String[] stemp = p.get(i).text().split(" ");
                                    List<String> percs = new ArrayList<>();
                                    for (String s : stemp) {
                                        if (s.matches("[0-9][0-9]%")) {
                                            percs.add(s);
                                        }
                                    }
                                    if (percs.size() > 0) {
                                        for (int m = 0; m < nomi.size(); m++) {
                                            ballot.put(nomi.get(m).text().replaceFirst(" ", ""), percs.get(m));
                                        }
                                    }
                                }
                                if (ballot.get(nome) != null) {
                                    g.setStringaVoto(ballot.get(nome));
                                }
                            }
                        } else {
                            g.setStringaVoto("0%");
                        }
                    }
                }

                String [][] return_value = new String[matches.size()][2];
                for (int i = 0; i < matches.size(); i++) {
                    String [] squadre = matches.get(i).attr("id").split("-");
                    return_value[i][0] = squadre[0];
                    return_value[i][1] = squadre[1];
                }
                return return_value;
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute (String[][] matches) {
            pD.dismiss();
            if (casa != null) {
                for (Giocatore g : casa) {
                    int j = 0;
                    boolean found = false;
                    boolean lato = true;
                    while (!found) {
                        if (matches[j][0].startsWith(g.getSquad().toLowerCase())) {
                            found = true;
                        }
                        if (matches[j][1].startsWith(g.getSquad().toLowerCase())) {
                            found = true;
                            lato = false;
                        }
                        if (found) j--;
                        j++;
                    }
                    g.setStato(lato ? "in casa vs " + matches[j][1].substring(0, 3).toUpperCase() : "in trasf vs " + matches[j][0].substring(0, 3).toUpperCase());
                }
                ha1.notifyDataSetChanged();
            }

            if (trasf != null) {
                for (Giocatore g : trasf) {
                    int j = 0;
                    boolean found = false;
                    boolean lato = true;
                    while (!found) {
                        if (matches[j][0].startsWith(g.getSquad().toLowerCase())) {
                            found = true;
                        }
                        if (matches[j][1].startsWith(g.getSquad().toLowerCase())) {
                            found = true;
                            lato = false;
                        }
                        if (found) j--;
                        j++;
                    }
                    g.setStato(lato ? "in casa vs " + matches[j][1].substring(0, 3).toUpperCase() : "in trasf vs " + matches[j][0].substring(0, 3).toUpperCase());
                }
                ha2.notifyDataSetChanged();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_form, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (id == R.id.action_perc) {
            if (current) {
                if (casa == null && trasf == null) {
                    Toast.makeText(Formazioni.this, "Formazioni non inserite o invisibili", Toast.LENGTH_SHORT).show();
                } else {
                    new Prob().execute();
                }
            } else {
                Toast.makeText(Formazioni.this, "Opzione valida solo per la giornata in corso", Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        if(id == R.id.action_top) {
            String message = "Opzione non valida per la giornata in corso";
            if(punteggi != null) {
                message = punteggi.get(1).get("squadra") + ": " + punteggi.get(1).get("punti");
            }
            Toast.makeText(Formazioni.this, message, Toast.LENGTH_SHORT).show();
        }

        return super.onOptionsItemSelected(item);
    }
}
