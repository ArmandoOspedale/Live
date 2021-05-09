package com.live;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class Gestione_Rose extends AppCompatActivity {

    private String [] codici;
    String [] squadre;
    private String current;
    private int [] max;
    private int crediti_squadra;
    private List<HashMap<String, String>> giocatori;
    private mAdapter adapter;

    JSONArray rosters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("GESTIONE ROSE");
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();
        squadre = intent.getStringArrayExtra("squadre");
        codici = intent.getStringArrayExtra("codici");
        current = intent.getStringExtra("select");
        max = intent.getIntArrayExtra("max");

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
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //if (i != 0) {
                    fab.animate().rotationBy(90).setDuration(150);
                    list.setVisibility(View.GONE);
                    current = codici[i];
                    new Download().execute(codici[i]);
                //}
            }
        });
        fab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (list.getVisibility() == View.VISIBLE) {
                    fab.animate().rotationBy(90).setDuration(150);
                    list.setVisibility(View.GONE);
                } else {
                    fab.animate().rotationBy(-90).setDuration(150);
                    list.setVisibility(View.VISIBLE);
                }
            }
        });

        new Download().execute(current);
    }

    @SuppressLint("StaticFieldLeak")
    private class Download extends AsyncTask<String, Void, Void> {
        String squadra;

        @Override
        protected Void doInBackground(String... params) {
            try {
                Document doc = HttpRequest.GET("/gestione-lega/gestione-rose", "btn btn-fab btn-fab-mini");
                rosters = new JSONArray(new String(Base64.decode(doc.select("script[id=red0h]").toString().split("d\\(\"")[1].split("\"\\)\\);")[0], Base64.DEFAULT)));

                JSONObject roster = null;
                int i = 0;
                boolean found = false;
                while (!found) {
                    roster = rosters.getJSONObject(i);
                    if(roster.getString("id_team").equals(params[0])) {
                        found = true;
                    }
                    i++;
                }
                squadra = HttpRequest.getNomeSquadra(params[0], codici, squadre);

                JSONArray players = roster.getJSONArray("calciatori");
                int pagato = 0;
                giocatori = new ArrayList<>();
                String [] labels = new String[]{"p", "d", "c", "a"};
                for (int k = 0; k < 4; k++) {
                    JSONArray in_roles = new JSONArray();
                    for (int l = 0; l < players.length(); l++) {
                        if (players.getJSONObject(l).getString("r").equals(labels[k].toUpperCase())) {
                            in_roles.put(players.getJSONObject(l));
                        }
                    }
                    for (int j = 0; j < max[k]; j++) {
                        int lunghezza = in_roles.length();
                        HashMap<String, String> giocatore = new HashMap<>();
                        if (j < lunghezza) {
                            giocatore.put("Nome", in_roles.getJSONObject(j).getString("n"));
                            giocatore.put("Ruolo", in_roles.getJSONObject(j).getString("r"));
                            giocatore.put("Squadra", in_roles.getJSONObject(j).getString("s").toUpperCase().substring(0, 3));
                            giocatore.put("Costo", in_roles.getJSONObject(j).getString("cacq"));
                            pagato = pagato + Integer.parseInt(giocatore.get("Costo"));
                            giocatore.put("Codice", in_roles.getJSONObject(j).getString("id"));
                        } else {
                            giocatore.put("Nome", "");
                            giocatore.put("Ruolo", labels[k].toUpperCase());
                            giocatore.put("Squadra", "");
                            giocatore.put("Costo", "");
                            giocatore.put("Codice", "");
                        }
                        giocatori.add(giocatore);
                    }
                }

                URL url = new URL("https://leghe.fantacalcio.it/servizi/V1_LegheCrediti/crediti?alias_lega=" + HttpRequest.lega);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestProperty("app_key", "4ab27d6de1e92c810c6d4efc8607065a735b917f");
                connection.setRequestProperty("cookie", HttpRequest.cookie);

                BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                JSONArray squadre = new JSONObject(rd.readLine()).getJSONArray("data");
                i = 0;
                found = false;
                while (!found) {
                    if(squadre.getJSONObject(i).getString("id").equals(params[0])) {
                        crediti_squadra = squadre.getJSONObject(i).getInt("crediti");
                        found = true;
                    }
                    i++;
                }

                return null;
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            ((TextView) findViewById(R.id.squadra)).setText(squadra.toUpperCase());
            invalidateOptionsMenu();
            ListView list = findViewById(R.id.lista);
            adapter = new mAdapter(Gestione_Rose.this, giocatori);
            list.setAdapter(adapter);
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, final int position, long l) {
                    if (giocatori.get(position).get("Nome").equals("")) {
                        new Lista().execute(giocatori.get(position).get("Ruolo"), String.valueOf(position));
                    } else {
                        final Dialog d = new Dialog(Gestione_Rose.this, R.style.CustomDialog);
                        d.setTitle(giocatori.get(position).get("Nome") + " (" + giocatori.get(position).get("Costo") + ")");
                        d.setContentView(R.layout.svincola);
                        (d.findViewById(R.id.conferma)).setOnClickListener(new View.OnClickListener() {
                            @SuppressLint("NonConstantResourceId")
                            @Override
                            public void onClick(View v) {
                                int scelta_id = ((RadioGroup) d.findViewById(R.id.radio)).getCheckedRadioButtonId();
                                String prezzo = "";
                                switch (scelta_id) {
                                    case R.id.meta: scelta_id = 1; break;
                                    case R.id.intero: scelta_id = 2; break;
                                    case R.id.zero: scelta_id = 3; break;
                                    case R.id.scelta:
                                        prezzo = ((EditText) d.findViewById(R.id.editText)).getText().toString();
                                        if (TextUtils.isEmpty(prezzo)) {scelta_id = 0;}
                                        else {scelta_id = 4;}
                                        break;
                                    default: scelta_id = -1; break;
                                }
                                if (scelta_id > 0) {
                                    d.cancel();
                                    if (scelta_id < 4) {
                                        new Svincola(scelta_id, giocatori.get(position), position).execute();
                                    } else {
                                        new Svincola(scelta_id, giocatori.get(position), position).execute(prezzo);
                                    }
                                } else {
                                    if (scelta_id == 0) {
                                        Toast.makeText(Gestione_Rose.this, "Inserire un valore", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(Gestione_Rose.this, "Selezionare un'opzione", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        });

                        d.show();
                    }
                }
            });
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class Svincola extends AsyncTask<String, String, Void> {
        int prezzo;
        JSONObject form;
        String ruolo;
        int posizione;

        Svincola(int m, HashMap<String, String> g, int p) {
            int price = Integer.parseInt(g.get("Costo"));
            switch (m) {
                case 1: prezzo = (price / 2 == 0 ? 1 : price / 2); break;
                case 2: prezzo = price; break;
                case 3: prezzo = 0; break;
                case 4: prezzo = -1;
            }
            try {
                form = new JSONObject();
                form.put("id_squadra", current);
                form.put("ids", g.get("Codice"));
                form.put("costi", String.valueOf(prezzo));
            } catch (JSONException e) {e.printStackTrace();}
            ruolo = g.get("Ruolo");
            posizione = p;
        }

        @Override
        protected Void doInBackground(String... params) {
            try {
                if (prezzo == -1) {
                    form.put("costi", params[0]);
                    prezzo = Integer.parseInt(params[0]);
                }

                URL url = new URL("https://leghe.fantacalcio.it/servizi/v1_leghemercatoOrdinarioAdmin/svincola?alias_lega=" + HttpRequest.lega);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setRequestMethod("DELETE");
                connection.setRequestProperty("app_key", "4ab27d6de1e92c810c6d4efc8607065a735b917f");
                connection.setRequestProperty("Cookie", HttpRequest.cookie);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                String wout = form.toString();
                byte[] postData = wout.getBytes();
                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.write(postData);

                connection.getInputStream();
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            HashMap<String, String> g = new HashMap<>();
            g.put("Nome", "");
            g.put("Ruolo", ruolo);
            g.put("Squadra", "");
            g.put("Costo", "");
            g.put("Codice", "");
            giocatori.set(posizione, g);
            adapter.notifyDataSetChanged();
            crediti_squadra = crediti_squadra + prezzo;
            invalidateOptionsMenu();
            Toast.makeText(Gestione_Rose.this, "Giocatore svincolato", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class Carica extends AsyncTask<String, String, Void> {

        HashMap<String, String> giocatore;
        int posizione;

        Carica (HashMap<String, String> g, int p) {
            giocatore = g;
            posizione = p;
        }

        @Override
        protected Void doInBackground(String... params) {
            try {
                URL url = new URL("https://leghe.fantacalcio.it/servizi/v1_leghemercatoOrdinarioAdmin/salva?alias_lega=" + HttpRequest.lega);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setRequestMethod("PUT");
                connection.setRequestProperty("app_key", "4ab27d6de1e92c810c6d4efc8607065a735b917f");
                connection.setRequestProperty("Cookie", HttpRequest.cookie);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                JSONObject form = new JSONObject();
                form.put("id_squadra", current);
                form.put("ids", giocatore.get("Codice"));
                form.put("costi", giocatore.get("Costo"));

                String wout = form.toString();
                byte[] postData = wout.getBytes();
                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.write(postData);

                connection.getInputStream();
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            giocatori.set(posizione, giocatore);
            crediti_squadra = crediti_squadra - (int) Float.parseFloat(giocatore.get("Costo"));
            adapter.notifyDataSetChanged();
            invalidateOptionsMenu();
            Toast.makeText(Gestione_Rose.this, "Giocatore assegnato", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class Lista extends AsyncTask<String, Void, String> {
        String reparto;
        int posizione;

        @Override
        protected String doInBackground(String... params) {
            try {
                reparto = params[0];
                posizione = Integer.parseInt(params[1]);
                URL url = new URL("https://leghe.fantacalcio.it/servizi/V1_LegheCalciatori/listaSvincolatiNoMercato?alias_lega=" + HttpRequest.lega + "&t=0");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestProperty("app_key", "4ab27d6de1e92c810c6d4efc8607065a735b917f");
                connection.setRequestProperty("cookie", HttpRequest.cookie);

                BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                return new JSONObject(rd.readLine()).getString("data");

            } catch (Exception e) {
                return "";
            }
        }

        @Override
        @SuppressWarnings("all")
        protected void onPostExecute(String s) {
            final List<HashMap<String,String>> players = new ArrayList<>();
            try {
                JSONArray ja = new JSONArray(s);
                for (int j = 0; j < ja.length(); j++) {
                    JSONObject jo = (JSONObject) ja.get(j);

                    if (jo.getString("r").equals(reparto)) {
                        HashMap<String, String> g = new HashMap<>();
                        g.put("Ruolo", jo.getString("r"));
                        g.put("Nome", jo.getString("n"));
                        g.put("Squadra", jo.getString("s").toUpperCase());
                        g.put("Codice", jo.getString("id"));
                        g.put("Quot", jo.getString("ca"));
                        players.add(g);
                    }
                }

                Collections.sort(players, new Comparator<HashMap<String, String>>() {
                    @Override
                    public int compare(HashMap<String, String> g1, HashMap<String, String> g2) {
                        String nome1 = g1.get("Nome");
                        String nome2 = g2.get("Nome");

                        return nome1.compareTo(nome2);
                    }
                });

                final Dialog d = new Dialog(Gestione_Rose.this);
                d.requestWindowFeature(Window.FEATURE_NO_TITLE);
                WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT);
                params.height = (int) ((float)getResources().getDisplayMetrics().heightPixels * 0.85f);
                params.width = (int) ((float)getResources().getDisplayMetrics().widthPixels * 0.85f);
                d.setContentView(LayoutInflater.from(Gestione_Rose.this).inflate(R.layout.aggiungi, null), params);
                ListView lista = (ListView) d.findViewById(R.id.svincolati);
                lista.setVerticalScrollBarEnabled(false);
                final Filter_Adapter filter_adapter = new Filter_Adapter(Gestione_Rose.this, players);
                lista.setAdapter(filter_adapter);

                lista.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, final View view, final int position, long id) {
                        final Dialog d2 = new Dialog(Gestione_Rose.this, R.style.CustomDialog);
                        final HashMap<String, String> g = (HashMap<String, String>) filter_adapter.getItem(position);
                        d2.setTitle(g.get("Nome") + " (" + g.get("Quot") + ")");
                        d2.setContentView(R.layout.ingaggia);

                        (d2.findViewById(R.id.conferma)).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                int scelta_id = ((RadioGroup) d2.findViewById(R.id.radio)).getCheckedRadioButtonId();
                                int prezzo;
                                switch (scelta_id) {
                                    case R.id.quot: prezzo = (int) Float.parseFloat(g.get("Quot")); break;
                                    case R.id.scelta: String p = ((EditText) d2.findViewById(R.id.prezzo)).getText().toString();
                                        prezzo = (TextUtils.isEmpty(p) ? 0 : Integer.parseInt(p));
                                        break;
                                    default: prezzo = -1; break;
                                }
                                if (prezzo > 0 && crediti_squadra - prezzo >= 0) {
                                    d2.cancel();
                                    d.cancel();
                                    int i = Collections.binarySearch(players, g, new Comparator<HashMap<String, String>>() {
                                                @Override
                                                public int compare(HashMap<String, String> g1, HashMap<String, String> g2) {
                                                    String nome1 = g1.get("Nome");
                                                    String nome2 = g2.get("Nome");

                                                    return nome1.compareTo(nome2);
                                                }
                                            });
                                    players.get(i).put("Costo", String.valueOf(prezzo));
                                    new Carica(players.get(i), posizione).execute();
                                } else {
                                    if (prezzo == 0) {
                                        Toast.makeText(Gestione_Rose.this, "Inserire un valore valido", Toast.LENGTH_SHORT).show();
                                    } else if (prezzo == -1) {
                                        Toast.makeText(Gestione_Rose.this, "Selezionare un'opzione", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(Gestione_Rose.this, "Crediti insufficienti", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        });

                        d2.show();
                    }
                });

                final Filter filter = filter_adapter.getFilter();
                SearchView searchView = (SearchView) d.findViewById(R.id.searchView);
                ListView spinner = (ListView) d.findViewById(R.id.spinner);
                spinner.setVerticalScrollBarEnabled(false);
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        if (TextUtils.isEmpty(newText)) {
                            filter_adapter.clearTextFilter();
                        } else {
                            filter.filter(newText);
                        }
                        return true;
                    }
                });

                String [] serieA = getResources().getStringArray(R.array.squadre);
                for (int i = 0; i < serieA.length; i++) {
                    serieA[i] = serieA[i].substring(0, 3);
                }
                ArrayAdapter<String> array_adapter = new ArrayAdapter<>(Gestione_Rose.this, android.R.layout.simple_spinner_dropdown_item, serieA);
                spinner.setAdapter(array_adapter);
                spinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                        filter_adapter.squadra = getResources().getStringArray(R.array.squadre)[position];
                        filter.filter("");
                    }
                });

                d.show();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class Crediti extends AsyncTask<String, Void, String> {
        int diff;

        @Override
        protected String doInBackground(String... params) {
            try {
                diff = Integer.parseInt(params[1]);
                URL url = new URL("https://leghe.fantacalcio.it/servizi/V1_LegheCrediti/crediti?alias_lega=" + HttpRequest.lega);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setRequestProperty("app_key", "4ab27d6de1e92c810c6d4efc8607065a735b917f");
                connection.setRequestProperty("Cookie", HttpRequest.cookie);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                JSONObject form = new JSONObject();
                form.put("id", params[0]);
                form.put("bonus_malus", params[1]);
                JSONArray enc = new JSONArray();
                enc.put(form);

                String wout = enc.toString();
                byte[] postData = wout.getBytes();
                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.write(postData);

                connection.getInputStream();
                return "Crediti aggiornati con successo";
            } catch (IOException | JSONException e) {
                return "";
            }
        }

        @Override
        protected void onPostExecute(String s) {
            if(!s.equals("")) {
                crediti_squadra = crediti_squadra + diff;
                invalidateOptionsMenu();
            }
            Toast.makeText(Gestione_Rose.this, s.equals("") ? "Errore" : s, Toast.LENGTH_SHORT).show();
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
            txt.setText(giocatore.get("Squadra").equals("") ? "" : giocatore.get("Squadra").substring(0, 3));
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

            return v;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_form, menu);
        if (giocatori != null) {
            menu.findItem(R.id.action_perc).setVisible(true);
            menu.findItem(R.id.action_perc).setTitle("CREDITI: " + crediti_squadra);
        } else {
            menu.findItem(R.id.action_perc).setVisible(false);
        }

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
            final Dialog d = new Dialog(Gestione_Rose.this, R.style.CustomDialog);
            d.setTitle("MODIFICA CREDITI");
            d.setContentView(R.layout.ingaggia);

            d.findViewById(R.id.radio).setVisibility(View.GONE);
            ((EditText) d.findViewById(R.id.prezzo)).setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
            ((Button) d.findViewById(R.id.conferma)).setText(R.string.conferma);

            (d.findViewById(R.id.conferma)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String c = ((EditText) d.findViewById(R.id.prezzo)).getText().toString();
                    if (!c.equals("") && !c.equals("0")) {
                        d.cancel();
                        new Crediti().execute(current, c);
                    } else {
                        if (c.equals(""))
                            Toast.makeText(Gestione_Rose.this, "Inserire un valore", Toast.LENGTH_SHORT).show();
                        if (c.equals("0"))
                            Toast.makeText(Gestione_Rose.this, "Inserire un valore diverso da 0", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            d.show();
        }

        return super.onOptionsItemSelected(item);
    }
}
