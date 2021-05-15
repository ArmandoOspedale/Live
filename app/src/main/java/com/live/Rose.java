package com.live;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.StateListDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class Rose extends AppCompatActivity {

    Document doc;

    private String [] codici;
    String [] squadre;
    private LinearLayout l;
    private TextView text1;
    private TextView text2;

    private int crediti_squadra;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("ROSE");
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();
        squadre = intent.getStringArrayExtra("squadre");
        codici = intent.getStringArrayExtra("codici");
        String codice = intent.getStringExtra("select");

        setContentView(R.layout.rose);

        final FloatingActionButton fab = findViewById(R.id.fab);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) fab.getLayoutParams();
            float density = getResources().getDisplayMetrics().density;
            p.setMargins(0, (int) (-10 * density), (int) (8 * density), (int) (-10 * density));
            fab.setLayoutParams(p);
        }
        final ListView list = findViewById(R.id.spinner2);
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, squadre);
        list.setAdapter(adapter2);
        list.setOnItemClickListener((adapterView, view, i, l) -> {
            //if (i != 0) {
                fab.animate().rotationBy(90).setDuration(150);
                list.setVisibility(View.GONE);
                new Download().execute(codici[i]);
            //}
        });
        fab.setOnClickListener(v -> {
            if (list.getVisibility() == View.VISIBLE) {
                fab.animate().rotationBy(90).setDuration(150);
                list.setVisibility(View.GONE);
            } else {
                fab.animate().rotationBy(-90).setDuration(150);
                list.setVisibility(View.VISIBLE);
            }
        });

        l = new LinearLayout(Rose.this);
        l.setOrientation(LinearLayout.HORIZONTAL);

        text1 = new TextView(Rose.this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.weight = 1;
        text1.setLayoutParams(params);
        text1.setBackgroundColor(Color.rgb(224, 224, 224));
        text1.setTextColor(Color.rgb(17, 17, 17));
        text1.setPadding(0, 10, 0, 10);
        text1.setGravity(Gravity.CENTER_HORIZONTAL);
        text1.setTextSize(16);

        text2 = new TextView(Rose.this);
        text2.setLayoutParams(params);
        text2.setBackgroundColor(Color.rgb(224, 224, 224));
        text2.setTextColor(Color.rgb(17, 17, 17));
        text2.setPadding(0, 10, 0, 10);
        text2.setGravity(Gravity.CENTER_HORIZONTAL);
        text2.setTextSize(16);

        l.addView(text1);
        l.addView(text2);

        new Download().execute(codice);
    }

    @SuppressLint("StaticFieldLeak")
    private class Download extends AsyncTask<String, Void, List<HashMap<String, String>>> {
        String squadra;

        @Override
        protected List<HashMap<String, String>> doInBackground(String... params) {
            int i = 0;
            boolean found = false;
            while (!found) {
                if (codici[i].equals(params[0])) {found = true; i--;}
                i++;
            }
            try {
                squadra = squadre[i];
                if (doc == null) doc = HttpRequest.GET("/rose/" + params[0], "col-xs-12 no-padding");
                Element roster = doc.select("ul[class=list-rosters]").select("li[data-id=" + params[0] + "]").get(0);

                List<HashMap<String, String>> giocatori = new ArrayList<>();
                Elements nomi = roster.select("td[data-key=name]");
                Elements ruoli = roster.select("td[data-key=role]");
                Elements squadre = roster.select("td[data-key=team]");
                Elements costi = roster.select("td[data-key=price]");
                Elements quots = roster.select("td[data-key=cost]");
                for (int j = 0; j < nomi.size(); j++) {
                    HashMap<String, String> giocatore = new HashMap<>();
                    giocatore.put("Nome", nomi.get(j).text().toUpperCase());
                    giocatore.put("Ruolo", ruoli.get(j).text());
                    giocatore.put("Squadra", squadre.get(j * 2).text().substring(0, 3).toUpperCase());
                    giocatore.put("Costo", costi.get(j).text());
                    giocatore.put("Quot", quots.get(j).text());
                    giocatori.add(giocatore);
                }

                JSONArray json = new JSONObject(new String(Base64.decode(doc.select("script[id=cio87a]").toString().split("dp\\('")[1].split("'\\)\\);")[0], Base64.DEFAULT))).getJSONArray("data");
                i = 0;
                found = false;
                while (!found) {
                    if(json.getJSONObject(i).getString("id").equals(params[0])) {
                        crediti_squadra = json.getJSONObject(i).getInt("crediti");
                        found = true;
                    }
                    i++;
                }

                return giocatori;
            } catch (IOException | JSONException e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<HashMap<String, String>> giocatori) {
            ((TextView) findViewById(R.id.squadra)).setText(squadra.toUpperCase());
            invalidateOptionsMenu();
            ListView list = findViewById(R.id.lista);
            list.setSelector(new StateListDrawable());

            int costo = 0;
            for (HashMap<String, String> g : giocatori) {
                costo = costo + Integer.parseInt(Objects.requireNonNull(g.get("Costo")));
            }
            String temp = "Crediti spesi: " + costo;
            text1.setText(temp);

            int quot = 0;
            for (HashMap<String, String> g : giocatori) {
                quot = quot + Integer.parseInt(Objects.requireNonNull(g.get("Quot")));
            }
            temp = "Valore rosa: " + quot;
            text2.setText(temp);

            if (list.getFooterViewsCount() == 0) {
                list.addFooterView(l);
            }
            list.setAdapter(new mAdapter(Rose.this, giocatori));
        }
    }

    private static class mAdapter extends BaseAdapter {

        private final Context context;
        List<HashMap<String, String>> giocatori;

        mAdapter(Context context, List<HashMap<String, String>> g) {
            this.context = context;
            giocatori = g;
        }

        @Override
        public int getCount()
        {
            return giocatori.size();
        }

        @Override
        public Object getItem(int position) {
            return giocatori.get(position);
        }

        @Override
        public long getItemId(int position)
        {
            return getItem(position).hashCode();
        }

        @Override
        @SuppressWarnings("all")
        public View getView(int position, View v, ViewGroup vg)
        {
            if (v==null)
            {
                v= LayoutInflater.from(context).inflate(R.layout.giocatore, null);
            }
            HashMap<String, String> giocatore = (HashMap<String, String>) getItem(position);
            TextView txt = (TextView) v.findViewById(R.id.nome);
            txt.setText(giocatore.get("Nome"));
            txt = (TextView) v.findViewById(R.id.squadra);
            txt.setText(giocatore.get("Squadra"));
            txt = (TextView) v.findViewById(R.id.ruolo);
            switch (giocatore.get("Ruolo")) {
                case "P": txt.setBackgroundColor(Color.rgb(255, 225, 15)); break;
                case "D": txt.setBackgroundColor(Color.rgb(0,128,10)); break;
                case "C": txt.setBackgroundColor(Color.rgb(5,29,192)); break;
                case "A": txt.setBackgroundColor(Color.RED); break;
            }
            txt.setText(giocatore.get("Ruolo"));

            txt = (TextView) v.findViewById(R.id.prezzo);
            txt.setText(giocatore.get("Costo"));
            //txt = (TextView) v.findViewById(R.id.quot);
            //txt.setText(giocatore.get("Quot"));

            return v;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_rose, menu);
        menu.findItem(R.id.action_crediti).setVisible(true);
        menu.findItem(R.id.action_crediti).setEnabled(false);
        menu.findItem(R.id.action_crediti).setTitle("CREDITI: " + crediti_squadra);

        return true;
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
