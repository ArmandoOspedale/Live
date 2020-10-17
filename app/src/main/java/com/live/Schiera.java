package com.live;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class Schiera extends AppCompatActivity {

    String comp;
    private String typebench;
    private String orderbench;
    private int giornata_lega;
    private int giornata_serie_a;
    private String [] moduli;
    private String modulo;
    private String teamId;
    private int[] bench;
    int p;
    int d;
    int c;
    int a;
    private Player [] players;
    private List<Player> liberi;
    ListView l;
    private mAdapter ad;
    private LinearLayout [] campo;
    private LinearLayout panchina;
    private String[] ordine;
    private final String DNAME = "LIVE";
    private Bitmap nocampioncino;
    private FloatingActionButton fab;
    private static final int REQUEST_WRITE_STORAGE = 112;

    private final int color_up = Color.BLUE;
    private final int color_down = Color.RED;

    private int admin;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.conf_formazione);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
        Intent i = getIntent();
        comp = i.getStringExtra("comp");
        teamId = i.getStringExtra("teamId");
        admin = i.getIntExtra("admin", 0);
        if (i.getIntExtra("giornata",0) != 0) {
            giornata_lega = i.getIntExtra("giornata",0);
        }

        new Dati().execute();
    }

    @SuppressLint("StaticFieldLeak")
    private class Dati extends AsyncTask<String, Boolean, String[]> {
        String info = "Errore";
        ProgressDialog progress;

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(Schiera.this);
            progress.setMessage("Download Campioncini...");
            progress.setIndeterminate(true);
            progress.setCancelable(false);
        }

        @Override
        protected String[] doInBackground (String... param) {
            try {
                URL url = new URL("https://leghe.fantacalcio.it/servizi/V1_LegheFormazioni/Visualizza?alias_lega=" + HttpRequest.lega +
                    "&id_comp=" + comp + "&id_squadra=" + teamId + (giornata_lega != 0 ? ("&giornata_lega=" + giornata_lega) : ""));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestProperty("app_key", "4ab27d6de1e92c810c6d4efc8607065a735b917f");
                connection.setRequestProperty("cookie", HttpRequest.cookie);

                BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                JSONObject json = new JSONObject(rd.readLine());

                if (json.getString("data").equals("null")) {
                    info = json.getJSONArray("error_msgs").getJSONObject(0).getString("descrizione");
                    throw new Exception();
                }

                json = json.getJSONObject("data");

                giornata_lega = json.getInt("giornata_lega");
                giornata_serie_a = json.getJSONObject("incontro").getInt("giornata_serie_a");

                JSONObject opzioni = json.getJSONObject("opzioni").getJSONObject("formazioni");
                moduli = toStringArray(opzioni.getJSONArray("moduli"));
                typebench = opzioni.getJSONArray("panchinari").join(",").replaceAll("\"", "");
                orderbench = StringUtil.join(Arrays.asList(opzioni.getString("sequenzaPanchina").split("")), ",");
                boolean freeBench = StringUtil.isBlank(orderbench);
                if(!freeBench)
                    orderbench = orderbench.substring(1, opzioni.getString("sequenzaPanchina").length() * 2);

                JSONObject formazione = json.getJSONObject("formazione");
                modulo = formazione.getString("modulo");
                if(modulo.equals("null")) modulo = "-";

                JSONArray calciatori = json.getJSONArray("calciatori");
                players = new Player[calciatori.length()];
                liberi = new ArrayList<>();
                StringBuilder b = new StringBuilder();
                for (int i = 0; i < calciatori.length(); i++){
                    players[i] = new Player(calciatori.getJSONObject(i));
                    liberi.add(players[i]);
                    if(freeBench && i > 10)
                        b.append(",L");
                }
                if(freeBench)
                    orderbench = b.toString().substring(1);
                if("0,0,0,0".equals(typebench))
                    typebench = calciatori.length() + "," + calciatori.length() + "," + calciatori.length() + "," + calciatori.length();
                nocampioncino = Bitmap.createScaledBitmap(
                        BitmapFactory.decodeResource(Schiera.this.getResources(), R.drawable.no_campioncino),
                                getResources().getDisplayMetrics().heightPixels / 9,
                                getResources().getDisplayMetrics().heightPixels / 9, true);

                boolean hasPermission = (ContextCompat.checkSelfPermission(Schiera.this,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                if (!hasPermission) {
                    ActivityCompat.requestPermissions(Schiera.this,
                            new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_WRITE_STORAGE);
                } else {
                    File rootPath = new File(Environment.getExternalStorageDirectory(), DNAME);
                    if (!rootPath.exists()) {
                        rootPath.mkdir();
                    }
                    SharedPreferences sharedPreferences = getSharedPreferences("CAMPIONCINI", MODE_PRIVATE);
                    boolean campioncini = sharedPreferences.getBoolean(HttpRequest.lega, true);
                    if (campioncini) {
                        publishProgress(true);
                        int height = getResources().getDisplayMetrics().heightPixels / 9;
                        for (Player p : players) {
                            try {
                                URL url_camp = new URL("https://content.fantacalcio.it/web/campioncini/small/" + p.img + ".png");

                                Bitmap bitmapToScale = BitmapFactory.decodeStream(url_camp.openStream());
                                if (bitmapToScale != null) {
                                    p.campioncino = Bitmap.createScaledBitmap(bitmapToScale, height, height, true);
                                    String imageFileName = p.img + ".png";
                                    File image = new File(rootPath, imageFileName);
                                    FileOutputStream out = new FileOutputStream(image);
                                    p.campioncino.compress(Bitmap.CompressFormat.PNG, 100, out);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        sharedPreferences.edit().putBoolean(HttpRequest.lega, false).apply();
                        publishProgress(false);
                    } else {
                        for (Player p : players) {
                            p.campioncino = BitmapFactory.decodeFile(rootPath + "/" + p.img + ".png");
                        }
                    }
                }

                String titolari = formazione.getString("titolari").equals("null") ? "" : formazione.getJSONArray("titolari").toString();
                String panchinari = formazione.getString("panchinari").equals("null") ? "" : formazione.getJSONArray("panchinari").toString();
                return new String[]{titolari, panchinari};
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(Boolean... values) {
            if (values[0]) {
                progress.show();
            } else {
                progress.dismiss();
            }
        }

        @Override
        protected void onPostExecute (final String[] result) {
            if (result != null) {
                setContentView(R.layout.schiera_drawer);
                l = findViewById(R.id.right_drawer);
                ad = new mAdapter(Schiera.this, liberi);
                l.setAdapter(ad);

                String temp = "ORDINE PANCHINA: " + (orderbench.contains("L") ? "LIBERO" : orderbench);
                ((TextView) (findViewById(R.id.ordine))).setText(temp);

                campo = new LinearLayout[] {findViewById(R.id.por), findViewById(R.id.dif),
                        findViewById(R.id.cen), findViewById(R.id.att)};
                for (int i = 0; i < 4; i++) {
                    ((HorizontalScrollView) campo[i].getParent()).setHorizontalScrollBarEnabled(false);
                }
                panchina = findViewById(R.id.panchina);
                ((HorizontalScrollView) panchina.getParent()).setHorizontalScrollBarEnabled(false);
                for (int i = 0; i < orderbench.split(",").length; i++) {
                    @SuppressLint("InflateParams") View v = LayoutInflater.from(Schiera.this).inflate(R.layout.incampo, null);
                    panchina.addView(v);
                }

                fab = findViewById(R.id.fab);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) fab.getLayoutParams();
                    float density = getResources().getDisplayMetrics().density;
                    p.setMargins((int) (1 * density), (int) (-4 * density), (int) (-4 * density), (int) (-4 * density));
                    fab.setLayoutParams(p);
                }

                invalidateOptionsMenu();
                int i = 0;
                boolean found = false;
                while (!found && i < moduli.length) {
                    if (moduli[i].equals(modulo)) {
                        found = true;
                        i--;
                    }
                    i++;
                }
                if (found) {
                    bench = genera(typebench);
                    ordine = orderbench.split(",");
                    p = 1;
                    d = Integer.parseInt(modulo.substring(0, 1));
                    c = Integer.parseInt(modulo.substring(1, 2));
                    a = Integer.parseInt(modulo.substring(2, 3));
                    String[] campo = result[0].replaceAll("\\[", "").replaceAll("]", "").replaceAll("\"", "").split(",");
                    for (String s : campo) {
                        for (Player p : players) {
                            if (s.equals(p.codice)) {
                                p.schiera();
                            }
                        }
                    }
                    String[] panchina = result[1].replaceAll("\\[", "").replaceAll("]", "").replaceAll("\"", "").split(",");
                    for (String s : panchina) {
                        for (Player p : players) {
                            if (s.equals(p.codice)) {
                                p.schiera();
                            }
                        }
                    }
                }
                main(i, true);
            } else {
                Toast.makeText(Schiera.this, info, Toast.LENGTH_LONG).show(); onBackPressed();
            }
        }
    }

    private void main (final int position, boolean first) {
        if (!first) {
            bench = genera(typebench);
            ordine = orderbench.split(",");
            modulo = moduli[position];
            p = 1;
            d = Integer.parseInt(modulo.substring(0, 1));
            c = Integer.parseInt(modulo.substring(1, 2));
            a = Integer.parseInt(modulo.substring(2, 3));

            for (Player pl : players) {
                switch (pl.ruolo) {
                    case "P":
                        if (!pl.free) {
                            if (pl.incampo && (p - 1 >= 0)) {
                                p = p - 1;
                            } else if (pl.inpanchina && bench[0] - 1 >= 0) {
                                bench[0] = bench[0] - 1;
                                ordine[pl.order] = "K";
                            } else {
                                p = p - 1;
                                pl.libera();
                            }
                        }
                        break;
                    case "D":
                        if (!pl.free) {
                            if (pl.incampo && (d - 1 >= 0)) {
                                d = d - 1;
                            } else if (pl.inpanchina && bench[1] - 1 >= 0) {
                                bench[1] = bench[1] - 1;
                                ordine[pl.order] = "K";
                            } else {
                                d = d - 1;
                                pl.libera();
                            }
                        }
                        break;
                    case "C":
                        if (!pl.free) {
                            if (pl.incampo && (c - 1 >= 0)) {
                                c = c - 1;
                            } else if (pl.inpanchina && bench[2] - 1 >= 0) {
                                bench[2] = bench[2] - 1;
                                ordine[pl.order] = "K";
                            } else {
                                c = c - 1;
                                pl.libera();
                            }
                        }
                        break;
                    case "A":
                        if (!pl.free) {
                            if (pl.incampo && (a - 1 >= 0)) {
                                a = a - 1;
                            } else if (pl.inpanchina && bench[3] - 1 >= 0) {
                                bench[3] = bench[3] - 1;
                                ordine[pl.order] = "K";
                            } else {
                                a = a - 1;
                                pl.libera();
                            }
                        }
                        break;
                }
            }

            invalidateOptionsMenu();
        }

        l.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int k, long id) {
                if (modulo.equals("-")) {
                    Toast.makeText(Schiera.this, "SELEZIONA UN MODULO", Toast.LENGTH_SHORT).show();
                } else {
                    boolean found = false;
                    int i = 0;
                    int position = 0;
                    String nome = ((TextView) view.findViewById(R.id.Name)).getText().toString();
                    while (!found) {
                        if (nome.equals(players[i].nome)) {
                            position = i;
                            found = true;
                        }
                        i++;
                    }
                    players[position].schiera();
                }
            }
        });

        l.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                boolean found = false;
                int j = 0;
                int position = 0;
                String nome = ((TextView) view.findViewById(R.id.Name)).getText().toString();
                while (!found) {
                    if (nome.equals(players[j].nome)) {
                        position = j;
                        found = true;
                    }
                    j++;
                }
                new Stats().execute(position);
                return true;
            }
        });

        fab.setVisibility(View.VISIBLE);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!modulo.equals("-") && p==0 && d==0 && c==0 && a==0 && bench[0]==0 && bench[1]==0 && bench[2]==0 && bench[3]==0) {
                    String[] codici = new String[11];
                    int n = 0;
                    for (int p : genera(typebench)) { n = n + p; }
                    String[] codp = new String[n];
                    int j = 0;
                    for (Player p : players) {
                        if (p.incampo) { codici[j] = p.codice; j++; }
                        if (p.inpanchina) { codp[p.order] = p.codice; }
                    }
                    final String [] dati = new String[]{"", ""};
                    for (int i = 0; i < codici.length; i++) {
                        dati[0] = dati[0] + codici[i] + (i == codici.length - 1 ? "" : ",");
                    }
                    for (int i = 0; i < codp.length; i++) {
                        dati[1] = dati[1] + codp[i] + (i == codp.length - 1 ? "" : ",");
                    }
                    final Dialog d = new Dialog(Schiera.this);
                    d.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    d.setContentView(R.layout.dialog);
                    d.findViewById(R.id.conferma).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            d.cancel();
                            new Inserisci().execute(dati[0], dati[1], modulo, String.valueOf(((CheckBox) d.findViewById(R.id.invisibile)).isChecked()), (((CheckBox) d.findViewById(R.id.pertutte)).isChecked()) ? "1" : "0");
                        }
                    });
                    d.show();
                } else {
                    Toast.makeText(Schiera.this, "Formazione incompleta", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private int [] genera (String s) {
        String[] panch = s.split(",");
        int[] p = new int[panch.length];
        for (int i = 0; i < panch.length; i++) {
            p[i] = Integer.parseInt(panch[i]);
        }
        return p;
    }

    private class mAdapter extends BaseAdapter {

        private final List<Player> data;
        private final Context context;

        mAdapter(Context context, List<Player> p)
        {
            data = p;
            this.context = context;
        }

        @Override
        public int getCount()
        {
            return data.size();
        }

        @Override
        public Object getItem(int position)
        {
            return data.get(position);
        }

        @Override
        public long getItemId(int position)
        {
            return getItem(position).hashCode();
        }

        @Override
        @SuppressLint({"ViewHolder", "InflateParams"})
        public View getView(int position, View v, ViewGroup vg)
        {
            //if (v == null)
            //{
                v = LayoutInflater.from(context).inflate(R.layout.formlist, null);
            //}
            Player player = (Player) getItem(position);
            TextView txt = (TextView) v.findViewById(R.id.Name);
            txt.setText(player.nome);
            txt = (TextView) v.findViewById(R.id.Role);
            switch (player.ruolo) {
                case "P":txt.setBackgroundColor(Color.rgb(255, 225, 15)); break;
                case "D":txt.setBackgroundColor(Color.rgb(0,128,10)); break;
                case "C":txt.setBackgroundColor(Color.rgb(5,29,192)); break;
                case "A":txt.setBackgroundColor(Color.RED); break;
            }
            txt.setText(player.ruolo);
            txt = (TextView) v.findViewById(R.id.Team);
            txt.setText(player.squadra);

            if (player.prob != null) {
                txt = (TextView) v.findViewById(R.id.Match);
                txt.setText(player.contro);
                txt = (TextView) v.findViewById(R.id.Prob);
                txt.setText(player.prob);
                v.findViewById(R.id.next).setVisibility(View.VISIBLE);
            }

            if (player.media != null) {
                txt = (TextView) v.findViewById(R.id.Media);
                txt.setText(player.media);
                txt = (TextView) v.findViewById(R.id.Pres);
                txt.setText(player.stats[0]);
                txt = (TextView) v.findViewById(R.id.Gol);
                txt.setText(player.stats[1]);
                txt = (TextView) v.findViewById(R.id.Amm);
                txt.setText(player.stats[2]);
                txt = (TextView) v.findViewById(R.id.Esp);
                txt.setText(player.stats[3]);
                txt = (TextView) v.findViewById(R.id.Ass);
                txt.setText(player.stats[4]);
                v.findViewById(R.id.stats).setVisibility(View.VISIBLE);
            }
            return v;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class Inserisci extends AsyncTask<String,Void,String> {

        @Override
        protected String doInBackground (String... param) {
            try {
                URL url;
                if (admin == 1) {
                    url = new URL("https://leghe.fantacalcio.it/servizi/V1_LegheFormazioni/SalvaAdmin?alias_lega=" + HttpRequest.lega);
                } else {
                    url = new URL("https://leghe.fantacalcio.it/servizi/V1_LegheFormazioni/Salva?alias_lega=" + HttpRequest.lega);
                }
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setDoOutput(true);
                connection.setDoInput(true);
                if (admin == 1) {
                    connection.setRequestMethod("POST");
                } else {
                    connection.setRequestMethod("PUT");
                }
                connection.setRequestProperty("app_key", "4ab27d6de1e92c810c6d4efc8607065a735b917f");
                connection.setRequestProperty("Cookie", HttpRequest.cookie);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                JSONObject form = new JSONObject();
                form.put("azione", 0);
                form.put("capitani", new JSONArray());
                form.put("giornata_lega", giornata_lega);
                form.put("giornata_serie_a", giornata_serie_a);
                form.put("id", 0);
                form.put("id_utente", 0);
                form.put("is_insert", true);
                form.put("id_comp", comp);
                form.put("id_squadra", teamId);
                form.put("modulo", param[2]);
                form.put("titolari", new JSONArray(param[0].split(",")));
                form.put("panchinari", new JSONArray(param[1].split(",")));
                form.put("tutte_comp", !param[4].equals("0"));
                form.put("visibile", !Boolean.parseBoolean(param[3]));

                String wout = form.toString();
                byte[] postData = wout.getBytes();
                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.write(postData);

                BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                return new JSONObject(rd.readLine()).getString("success");
            }catch (IOException | JSONException e) {return null;}
        }

        @Override
        protected void onPostExecute (String result) {
            if (result != null && result.equals("true")) {Toast.makeText(Schiera.this, "Formazione inserita con successo", Toast.LENGTH_SHORT).show();}
            else {Toast.makeText(Schiera.this, "Errore", Toast.LENGTH_SHORT).show();}
        }
    }

    private class Player {
        String codice;
        String nome;
        String ruolo;
        String squadra;
        String contro;
        String img;
        boolean incampo;
        boolean inpanchina;
        Bitmap campioncino;
        boolean free;
        String prob;
        int order;
        String media;
        String [] stats;
        String stato;

        Player(JSONObject dati) throws JSONException {
            codice = dati.getString("id");
            nome = dati.getString("n");
            ruolo = dati.getString("r");
            squadra = dati.getString("s").substring(0, 3).toUpperCase();
            img = dati.getString("img");
            stato = dati.getString("st");
            free = true;
            stats = new String[]{"0", "0", "0", "0", "0"};
        }

        void schiera() {
            switch (ruolo) {
                case "P":
                    if (p - 1 >= 0) {
                        free = false;
                        incampo = true;
                        p--;
                        liberi.remove(this);
                        ad.notifyDataSetChanged();
                        inserisci(0);
                    } else if (bench[0] - 1 >= 0) {
                        int i = 0;
                        boolean found = false;
                        while (!found) {
                            if (ordine[i].equals("P") || ordine[i].equals("L")) {
                                order = i;
                                ordine[i] = "K";
                                found = true;
                            }
                            i++;
                        }
                        free = false;
                        inpanchina = true;
                        bench[0]--;
                        liberi.remove(this);
                        ad.notifyDataSetChanged();
                        inserisciPanchina();
                    }
                    break;
                case "D":
                    if (d - 1 >= 0) {
                        free = false;
                        incampo = true;
                        d--;
                        liberi.remove(this);
                        ad.notifyDataSetChanged();
                        inserisci(1);
                    } else if (bench[1] - 1 >= 0) {
                        int i = 0;
                        boolean found = false;
                        while (!found) {
                            if (ordine[i].equals("D") || ordine[i].equals("L")) {
                                order = i;
                                ordine[i] = "K";
                                found = true;
                            }
                            i++;
                        }
                        free = false;
                        inpanchina = true;
                        bench[1]--;
                        liberi.remove(this);
                        ad.notifyDataSetChanged();
                        inserisciPanchina();
                    }
                    break;
                case "C":
                    if (c - 1 >= 0) {
                        free = false;
                        incampo = true;
                        c--;
                        liberi.remove(this);
                        ad.notifyDataSetChanged();
                        inserisci(2);
                    } else if (bench[2] - 1 >= 0) {
                        int i = 0;
                        boolean found = false;
                        while (!found) {
                            if (ordine[i].equals("C") || ordine[i].equals("L")) {
                                order = i;
                                ordine[i] = "K";
                                found = true;
                            }
                            i++;
                        }
                        free = false;
                        inpanchina = true;
                        bench[2]--;
                        liberi.remove(this);
                        ad.notifyDataSetChanged();
                        inserisciPanchina();
                    }
                    break;
                case "A":
                    if (a - 1 >= 0) {
                        free = false;
                        incampo = true;
                        a--;
                        liberi.remove(this);
                        ad.notifyDataSetChanged();
                        inserisci(3);
                    } else if (bench[3] - 1 >= 0) {
                        int i = 0;
                        boolean found = false;
                        while (!found) {
                            if (ordine[i].equals("A") || ordine[i].equals("L")) {
                                order = i;
                                ordine[i] = "K";
                                found = true;
                            }
                            i++;
                        }
                        free = false;
                        inpanchina = true;
                        bench[3]--;
                        liberi.remove(this);
                        ad.notifyDataSetChanged();
                        inserisciPanchina();
                    }
                    break;
            }
        }

        private void inserisci(int k) {
            @SuppressLint("InflateParams") View v = LayoutInflater.from(Schiera.this).inflate(R.layout.incampo, null);
            if (campioncino != null) {
                ((ImageView) v.findViewById(R.id.campioncino)).setImageBitmap(campioncino);
            } else {
                ((ImageView) v.findViewById(R.id.campioncino)).setImageBitmap(nocampioncino);
            }
            ((TextView) v.findViewById(R.id.nome)).setText(nome);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    libera();
                }
            });
            if (prob != null) {
                ((TextView) v.findViewById(R.id.prob)).setText(prob);
                if (prob.contains("I.T.") && Integer.parseInt(prob.split("I.T. ")[1].split("%")[0]) >= 50) {
                    ((TextView) v.findViewById(R.id.prob)).setTextColor(color_up);
                } else {
                    ((TextView) v.findViewById(R.id.prob)).setTextColor(color_down);
                }
                ((TextView) v.findViewById(R.id.match)).setText(contro.substring(0, contro.length() - 7));
                ((TextView) v.findViewById(R.id.squadra)).setText(contro.substring(contro.length() - 6));
                v.findViewById(R.id.info).setVisibility(View.VISIBLE);
            }
            campo[k].addView(v);
        }

        private void inserisciPanchina() {
            if (campioncino != null) {
                ((ImageView) ((panchina.getChildAt(order)).findViewById(R.id.campioncino))).setImageBitmap(campioncino);
            } else {
                ((ImageView) ((panchina.getChildAt(order)).findViewById(R.id.campioncino))).setImageBitmap(nocampioncino);
            }
            ((TextView) ((panchina.getChildAt(order)).findViewById(R.id.nome))).setText(nome);
            panchina.getChildAt(order).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    libera();
                }
            });
            if (prob != null) {
                ((TextView) ((panchina.getChildAt(order)).findViewById(R.id.prob))).setText(prob);
                if (prob.contains("I.T.") && Integer.parseInt(prob.split("I.T. ")[1].split("%")[0]) >= 50) {
                    ((TextView) ((panchina.getChildAt(order)).findViewById(R.id.prob))).setTextColor(color_up);
                } else {
                    ((TextView) ((panchina.getChildAt(order)).findViewById(R.id.prob))).setTextColor(color_down);
                }
                ((TextView) ((panchina.getChildAt(order)).findViewById(R.id.match))).setText(contro.substring(0, contro.length() - 7));
                ((TextView) ((panchina.getChildAt(order)).findViewById(R.id.squadra))).setText(contro.substring(contro.length() - 6));
                ((panchina.getChildAt(order)).findViewById(R.id.info)).setVisibility(View.VISIBLE);
            }
        }

        private void aggiorna () {
            int k = -1;
            if (incampo) {
                switch (ruolo) {
                    case "P":
                        k=0;
                        break;
                    case "D":
                        k=1;
                        break;
                    case "C":
                        k=2;
                        break;
                    case "A":
                        k=3;
                        break;
                }
                int i = 0;
                boolean found = false;
                while (!found) {
                    if (((TextView) ((campo[k].getChildAt(i)).findViewById(R.id.nome))).getText().toString().equals(nome)) {
                        found = true;
                        ((TextView) (campo[k].getChildAt(i)).findViewById(R.id.prob)).setText(prob);
                        if (prob.contains("I.T.") && Integer.parseInt(prob.split("I.T. ")[1].split("%")[0]) >= 50) {
                            ((TextView) (campo[k].getChildAt(i)).findViewById(R.id.prob)).setTextColor(color_up);
                        } else {
                            ((TextView) (campo[k].getChildAt(i)).findViewById(R.id.prob)).setTextColor(color_down);
                        }
                        ((TextView) (campo[k].getChildAt(i)).findViewById(R.id.match)).setText(contro.substring(0, contro.length() - 7));
                        ((TextView) (campo[k].getChildAt(i)).findViewById(R.id.squadra)).setText(contro.substring(contro.length() - 6));
                        (campo[k].getChildAt(i)).findViewById(R.id.info).setVisibility(View.VISIBLE);
                    }
                    i++;
                }
            } else if (inpanchina) {
                ((TextView) ((panchina.getChildAt(order)).findViewById(R.id.prob))).setText(prob);
                if (prob.contains("I.T.") && Integer.parseInt(prob.split("I.T. ")[1].split("%")[0]) >= 50) {
                    ((TextView) ((panchina.getChildAt(order)).findViewById(R.id.prob))).setTextColor(color_up);
                } else {
                    ((TextView) ((panchina.getChildAt(order)).findViewById(R.id.prob))).setTextColor(color_down);
                }
                ((TextView) ((panchina.getChildAt(order)).findViewById(R.id.match))).setText(contro.substring(0, contro.length() - 7));
                ((TextView) ((panchina.getChildAt(order)).findViewById(R.id.squadra))).setText(contro.substring(contro.length() - 6));
                (panchina.getChildAt(order)).findViewById(R.id.info).setVisibility(View.VISIBLE);
            }
        }

        private void elimina(int k) {
            int i = 0;
            boolean found = false;
            while (!found) {
                if (((TextView) ((campo[k].getChildAt(i)).findViewById(R.id.nome))).getText().toString().equals(nome)) {
                    found = true;
                    campo[k].removeViewAt(i);
                }
                i++;
            }
        }

        void libera() {
            switch (ruolo) {
                case "P":
                    if (incampo) {
                        p++;
                        free = true;
                        incampo = false;
                        liberi.add(this);
                        Collections.sort(liberi, Ruolo);
                        ad.notifyDataSetChanged();
                        elimina(0);
                    } else if (inpanchina) {
                        bench[0]++;
                        ordine[order] = (orderbench.contains("L") ? "L" : "P");
                        free = true;
                        inpanchina = false;
                        liberi.add(this);
                        Collections.sort(liberi, Ruolo);
                        ad.notifyDataSetChanged();
                        ((ImageView) ((panchina.getChildAt(order)).findViewById(R.id.campioncino))).setImageBitmap(null);
                        ((TextView) ((panchina.getChildAt(order)).findViewById(R.id.nome))).setText("");
                        (panchina.getChildAt(order)).findViewById(R.id.info).setVisibility(View.GONE);
                    }
                    break;
                case "D":
                    if (incampo) {
                        d++;
                        free = true;
                        incampo = false;
                        liberi.add(this);
                        Collections.sort(liberi, Ruolo);
                        ad.notifyDataSetChanged();
                        elimina(1);
                    } else if (inpanchina) {
                        bench[1]++;
                        ordine[order] = (orderbench.contains("L") ? "L" : "D");
                        free = true;
                        inpanchina = false;
                        liberi.add(this);
                        Collections.sort(liberi, Ruolo);
                        ad.notifyDataSetChanged();
                        ((ImageView) ((panchina.getChildAt(order)).findViewById(R.id.campioncino))).setImageBitmap(null);
                        ((TextView) ((panchina.getChildAt(order)).findViewById(R.id.nome))).setText("");
                        (panchina.getChildAt(order)).findViewById(R.id.info).setVisibility(View.GONE);
                    }
                    break;
                case "C":
                    if (incampo) {
                        c++;
                        free = true;
                        incampo = false;
                        liberi.add(this);
                        Collections.sort(liberi, Ruolo);
                        ad.notifyDataSetChanged();
                        elimina(2);
                    } else if (inpanchina) {
                        bench[2]++;
                        ordine[order] = (orderbench.contains("L") ? "L" : "C");
                        free = true;
                        inpanchina = false;
                        liberi.add(this);
                        Collections.sort(liberi, Ruolo);
                        ad.notifyDataSetChanged();
                        ((ImageView) ((panchina.getChildAt(order)).findViewById(R.id.campioncino))).setImageBitmap(null);
                        ((TextView) ((panchina.getChildAt(order)).findViewById(R.id.nome))).setText("");
                        (panchina.getChildAt(order)).findViewById(R.id.info).setVisibility(View.GONE);
                    }
                    break;
                case "A":
                    if (incampo) {
                        a++;
                        free = true;
                        incampo = false;
                        liberi.add(this);
                        Collections.sort(liberi, Ruolo);
                        ad.notifyDataSetChanged();
                        elimina(3);
                    } else if (inpanchina) {
                        bench[3]++;
                        ordine[order] = (orderbench.contains("L") ? "L" : "A");
                        free = true;
                        inpanchina = false;
                        liberi.add(this);
                        Collections.sort(liberi, Ruolo);
                        ad.notifyDataSetChanged();
                        ((ImageView) ((panchina.getChildAt(order)).findViewById(R.id.campioncino))).setImageBitmap(null);
                        ((TextView) ((panchina.getChildAt(order)).findViewById(R.id.nome))).setText("");
                        (panchina.getChildAt(order)).findViewById(R.id.info).setVisibility(View.GONE);
                    }
                    break;
            }
        }
    }

    private static final Comparator<Player> Ruolo
            = new Comparator<Player>() {

        public int compare(Player g1, Player g2) {

            String ruolo1 = g1.ruolo;
            String ruolo2 = g2.ruolo;

            return ruolo2.compareTo(ruolo1);
        }
    };

    private boolean partite = false;
    String [][] matches;
    @SuppressLint("StaticFieldLeak")
    private class Prob extends AsyncTask<String,Void,String[]> {
        ProgressDialog p;

        @Override
        protected void onPreExecute () {
            p = new ProgressDialog(Schiera.this);
            p.setMessage("Recupero Probabili Formazioni...");
            p.setCancelable(false);
            p.show();
        }

        @Override
        protected String[] doInBackground (String... param) {
            try {
                Document doc = HttpRequest.GET_nolega("https://www.fantacalcio.it/probabili-formazioni-serie-A", "<!-- FINE CONTAINER PRIMO BLOCCO CONTENUTO  SU DUE COLONNE -->");

                Elements el = doc.select("div[id=sqtab]").get(0).children();

                if (!Schiera.this.partite) {
                    matches = new String[el.size()][2];
                    for (int i = 0; i < el.size(); i++) {
                        String [] squadre = el.get(i).attr("id").split("-");
                        matches[i][0] = squadre[0];
                        matches[i][1] = squadre[1];
                    }
                }

                String[] prob = new String[players.length];
                for (int i = 0; i < players.length; i++) {
                    String nome = players[i].nome;
                    String[] temp = nome.split(" ");
                    nome = temp[0].substring(0, 1) + temp[0].substring(1).toLowerCase();
                    if (temp.length > 1) {
                        nome = nome + " " + temp[1].substring(0, 1) + temp[1].substring(1).toLowerCase();
                    }
                    if (temp.length > 2) {
                        nome = nome + " " + temp[2];
                    }

                    String squadra = players[i].squadra;
                    int j = 0;
                    boolean found = false;
                    boolean casa = true;
                    while (!found) {
                        String[] squadre = el.get(j).attr("id").split("-");
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
                        team = el.get(j).select("div[class=pgroup lf]");
                    } else {
                        team = el.get(j).select("div[class=pgroup rt]");
                    }

                    int k = 0;
                    found = false;
                    while (!found && k < team.size()) {
                        if (team.get(k).select("a").text().equals(nome.toUpperCase())) {
                            k--;
                            found = true;
                        }
                        k++;
                    }

                    if (found) {
                        Elements perc = team.get(k).select("span[class=perc]");
                        if (perc.size() == 1) {
                            prob[i] = "I.T. " + perc.get(0).text();
                        } else {
                            prob[i] = "I.S. ";
                            if (casa) {
                                prob[i] = prob[i] + team.get(k).select("div[class=is pull-left bold]").text().replaceAll("I.S. ", "");
                            } else {
                                prob[i] = prob[i] + team.get(k).select("div[class=is pull-right bold]").text().replaceAll("I.S. ", "");
                            }
                            Elements ballottaggi = el.get(j).select("div[class=pgroup]");
                            for (int m = 0; m < ballottaggi.size(); m++) {
                                if (!ballottaggi.get(m).select("span").get(0).text().equals("BALLOTTAGGI")) {
                                    ballottaggi.remove(m);
                                    m--;
                                }
                            }

                            HashMap<String, String> ballot = new HashMap<>();
                            Elements p = ballottaggi.select("p");
                            for (int l = 0; l < p.size(); l++) {
                                Elements nomi = p.get(l).select("span[class=bold]");
                                String[] stemp = p.get(l).text().split(" ");
                                List<String> percs = new ArrayList<>();
                                for (String s : stemp) {
                                    if (s.matches("[0-9][0-9]%")) {
                                        percs.add(s);
                                    }
                                }
                                if (percs.size() > 0) {
                                    for (int m = 0; m < nomi.size(); m++) {
                                        ballot.put(nomi.get(m).text(), percs.get(m));
                                    }
                                }
                            }
                            if (ballot.get(nome) != null) {
                                prob[i] = "I.T. " + ballot.get(nome);
                            }
                        }
                    } else {
                        switch (players[i].stato) {
                            case ("2"):
                                prob[i] = "INF";
                                break;
                            case ("3"):
                                prob[i] = "SQ";
                                break;
                            default:
                                prob[i] = "0%";
                                break;
                        }
                    }
                }
                return prob;
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute (String[] result) {
            p.dismiss();
            for (int i = 0; i < result.length; i++) {
                players[i].prob = result[i];
                if (!Schiera.this.partite) {
                    int j = 0;
                    boolean found = false;
                    boolean lato = true;
                    while (!found) {
                        if (matches[j][0].startsWith(players[i].squadra.toLowerCase())) {
                            found = true;
                        }
                        if (matches[j][1].startsWith(players[i].squadra.toLowerCase())) {
                            found = true;
                            lato = false;
                        }
                        if (found) j--;
                        j++;
                    }
                    players[i].contro = (lato ? "in casa vs " + matches[j][1].substring(0, 3).toUpperCase() : "in trasf vs " + matches[j][0].substring(0, 3).toUpperCase());
                }
                players[i].aggiorna();
            }
            Schiera.this.partite = true;
            ad.notifyDataSetChanged();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class Stats extends AsyncTask<Integer,Void,Void> {
        ProgressDialog p;

        @Override
        protected void onPreExecute () {
            p = new ProgressDialog(Schiera.this);
            p.setMessage("Caricamento Statistiche...");
            p.setCancelable(false);
            p.show();
        }

        @Override
        protected Void doInBackground (Integer... param) {
            try {
                String [] nomisquadre = getResources().getStringArray(R.array.squadre);
                String squadra = "";
                int j = 0;
                boolean found = false;
                while (!found) {
                    if (nomisquadre[j].startsWith(players[param[0]].squadra)) {
                        squadra = nomisquadre[j];
                        found = true;
                    }
                    j++;
                }
                Document doc = HttpRequest.GET_nolega("https://www.fantacalcio.it/squadre/" + squadra + "/" + players[param[0]].nome.replace(" ", "-") + "/" + players[param[0]].codice, "<!-- FINE CONTAINER PRIMO BLOCCO CONTENUTO  SU DUE COLONNE -->");

                Elements stats = doc.select("div[id=fantastatistiche]");

                Elements num = stats.select("div[class=row no-gutter]").get(1).children();
                for (int i = 0; i < num.size() - 1; i++) {
                    if (i == 1) {
                        int gol = Integer.parseInt(num.get(i).select("p[class=nbig]").text());
                        int rigori = Integer.parseInt(num.get(5).select("p[class=nbig]").text().split("su")[0]);
                        players[param[0]].stats[i] = (gol + rigori) + (rigori == 0 ? "" : "(" + rigori + ")");
                    } else if (i == 4) {
                        int assist = Integer.parseInt(num.get(i).select("p[class=nbig]").text().split(" ")[0]);
                        int assistf = Integer.parseInt(num.get(i).select("p[class=nbig]").text().split(" ")[2]);
                        players[param[0]].stats[i] = (assist + assistf) + (assistf == 0 ? "" : "(" + assistf + ")");
                    } else {
                        players[param[0]].stats[i] = num.get(i).select("p[class=nbig]").text();
                    }
                }

                players[param[0]].media = stats.select("div[id=chart1div]").select("p[class=nbig2]").get(0).text();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute (Void result) {
            p.dismiss();
            findViewById(R.id.legenda).setVisibility(View.VISIBLE);
            int height = ((TextView) findViewById(R.id.MV)).getLineHeight();
            RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) l.getLayoutParams();
            p.setMargins(0, 0, 0, height + (int) (13 * getResources().getDisplayMetrics().density));
            l.setLayoutParams(p);
            ad.notifyDataSetChanged();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class AggiornaCampioncini extends AsyncTask<Void, Void, Void> {

        ProgressDialog p;
        @Override
        protected void onPreExecute() {
            p = new ProgressDialog(Schiera.this);
            p.setMessage("Aggiornamento Campioncini...");
            p.setIndeterminate(false);
            p.setCancelable(false);
            p.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            File rootPath = new File(Environment.getExternalStorageDirectory(), DNAME);
            if (!rootPath.exists()) {
                rootPath.mkdir();
            }
            int height = getResources().getDisplayMetrics().heightPixels / 9;
            for (Player p : players) {
                try {
                    URL url_camp = new URL("https://content.fantacalcio.it/web/campioncini/small/" + p.img + ".png");

                    Bitmap bitmapToScale = BitmapFactory.decodeStream(url_camp.openStream());
                    if (bitmapToScale != null) {
                        p.campioncino = Bitmap.createScaledBitmap(bitmapToScale, height, height, true);
                        String imageFileName = p.img + ".png";
                        File image = new File(rootPath, imageFileName);
                        FileOutputStream out = new FileOutputStream(image);
                        p.campioncino.compress(Bitmap.CompressFormat.PNG, 100, out);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            SharedPreferences sharedPreferences = getSharedPreferences("CAMPIONCINI", MODE_PRIVATE);
            boolean campioncini = sharedPreferences.getBoolean(HttpRequest.lega, true);
            if (campioncini) {
                sharedPreferences.edit().putBoolean(HttpRequest.lega, false).apply();
            }
            p.dismiss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_schiera, menu);
        if (moduli != null) {
            menu.findItem(R.id.action_moduli).setVisible(true);
            SubMenu legheMenu = menu.findItem(R.id.action_moduli).getSubMenu();
            for (int i = 0; i < moduli.length; i++) {
                legheMenu.add(0, i, Menu.NONE, moduli[i]);
            }
        }
        if (modulo != null) {
            menu.findItem(R.id.action_moduli).setTitle("MODULO: " + modulo);
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                new AggiornaCampioncini().execute();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id < moduli.length) {
            main(id, false);
        }

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (id == R.id.action_info) {
            new Prob().execute();
            return true;
        }

        if (id == R.id.action_camp) {
            new AggiornaCampioncini().execute();
            return true;
        }

        if (id == R.id.action_prob) {
            if (partite) {
                final Dialog d = new Dialog(this, R.style.CustomDialog);
                d.setTitle("PROBABILI FORMAZIONI");

                ListView list = new ListView(this);
                final String[] toAdapter = new String[matches.length];
                for (int i = 0; i < matches.length; i++) {
                    toAdapter[i] = matches[i][0] + "-" + matches[i][1];
                    toAdapter[i] = toAdapter[i].toUpperCase();
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.row, R.id.row, toAdapter);
                list.setAdapter(adapter);

                list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        Intent fg = new Intent(Schiera.this, PartitaLive.class);
                        fg.putExtra("title", "fantacalcio");
                        fg.putExtra("uri", "https://www.fantacalcio.it/probabili-formazioni-serie-a" + "#" + toAdapter[i].toLowerCase());
                        startActivity(fg);
                        d.dismiss();
                    }
                });

                d.setContentView(list);
                d.show();
            } else {
                Intent fg = new Intent(this, PartitaLive.class);
                fg.putExtra("title", "fantacalcio");
                fg.putExtra("uri", "https://www.fantacalcio.it/probabili-formazioni-serie-a");
                startActivity(fg);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private String[] toStringArray(JSONArray array) {
        if(array==null)
            return null;

        String[] arr = new String[array.length()];
        for(int i = 0; i < arr.length; i++) {
            arr[i]=array.optString(i);
        }
        return arr;
    }
}