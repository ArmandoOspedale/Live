package com.live;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class Calcolo extends Dialog {

    Context context;
    String [] squadre;
    private String [] bonus;
    private final String [] codici;
    private boolean [] boolrinvio;
    String comp;

    private final HashMap<String, List<String>> rinvio = new HashMap<>();
    private final List<EditText> valori = new ArrayList<>();

    Calcolo (Context cxt, String[] s, String[] c, String competiozione) {
        super(cxt, R.style.CustomDialog);
        setTitle(R.string.calcola);

        context = cxt;
        codici = c;
        comp = competiozione;

        squadre = new String[s.length];
        for (int i = 0; i < squadre.length; i++) {
            squadre[i] = s[i].toUpperCase();
        }

        new Dati().execute();
    }

    @SuppressWarnings("all")
    private class mAdapter extends BaseAdapter {

        private Context context = null;
        String[] squadre;

        public mAdapter(Context context, String[] s) {
            this.context = context;
            squadre = s;
        }

        @Override
        public int getCount() {
            return squadre.length;
        }

        @Override
        public Object getItem(int position) {
            return squadre[position];
        }

        @Override
        public long getItemId(int posizione) {
            return getItem(posizione).hashCode();
        }

        @Override
        public View getView(final int position, View v, ViewGroup vg) {
            if (v == null) {
                v = LayoutInflater.from(context).inflate(R.layout.bonus, null);
            }
            String squadra = (String) getItem(position);
            TextView txt = (TextView) v.findViewById(R.id.squadra);
            txt.setText(squadra);
            EditText valore = (EditText) v.findViewById(R.id.valore);
            bonus[position] = "0";
            valore.setText("0");
            if (!valori.contains(valore)) {
                valori.add(valore);
            }
            return v;
        }
    }

    @SuppressWarnings("all")
    private class mAdapter2 extends BaseAdapter {

        private Context context = null;
        String[] squadre;

        public mAdapter2(Context context, String[] s) {
            this.context = context;
            squadre = s;
        }

        @Override
        public int getCount() {
            return squadre.length;
        }

        @Override
        public Object getItem(int position) {
            return squadre[position];
        }

        @Override
        public long getItemId(int posizione) {
            return getItem(posizione).hashCode();
        }

        @Override
        public View getView(final int position, View v, ViewGroup vg) {
            if (v == null) {
                v = LayoutInflater.from(context).inflate(R.layout.rinvio, null);
            }
            String squadra = (String) getItem(position);
            TextView txt = (TextView) v.findViewById(R.id.squadra);
            txt.setText(squadra);
            CheckBox checked = (CheckBox) v.findViewById(R.id.checkbox);
            checked.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    boolrinvio[position]=!boolrinvio[position];
                }
            });

            return v;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class Dati extends AsyncTask<String, Void, Boolean> {

        String[] noncalcolate;

        String POST;
        @Override
        protected Boolean doInBackground (String... param) {

            try {
                URL url = new URL("https://leghe.fantacalcio.it/servizi/V1_LegheCalcolo/Giornate?alias_lega=" + HttpRequest.lega +
                        "&id_competizione=" + comp);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestProperty("app_key", "4ab27d6de1e92c810c6d4efc8607065a735b917f");
                connection.setRequestProperty("cookie", HttpRequest.cookie);

                BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                JSONObject json = new JSONObject(rd.readLine());

                if (json.getBoolean("success")) {

                    JSONArray json_data = json.getJSONArray("data");
                    List<String> temp = new ArrayList<>();
                    for(int i = 0; i < json_data.length(); i++) {
                        if (!json_data.getJSONObject(i).getBoolean("c")) {
                            temp.add(json_data.getJSONObject(i).getString("g"));

                            if (!json_data.getJSONObject(i).getString("r").equals("null")) {
                                List<String> temp_rinvio = new ArrayList<>();
                                JSONArray json_rinvio = json_data.getJSONObject(i).getJSONArray("r");
                                for (int j = 0; j < json_rinvio.length(); j++) {
                                    temp_rinvio.add(json_rinvio.getJSONObject(j).getString("id_a"));
                                    temp_rinvio.add(json_rinvio.getJSONObject(j).getString("id_b"));
                                }
                                rinvio.put(temp.get(i), temp_rinvio);
                            }
                        }
                    }

                    if (temp.size() > 0) {
                        noncalcolate = new String[temp.size()];
                        noncalcolate = temp.toArray(noncalcolate);
                    } else {
                        POST = "Nessuna giornata da calcolare";
                        return false;
                    }

                    return true;
                } else {
                    POST = "Nessuna giornata da calcolare";
                    return false;
                }

            } catch (IOException | JSONException e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute (Boolean result) {
            if (result) {
                ListView nonlist = new ListView(context);
                ArrayAdapter<String> nonad = new ArrayAdapter<>(context, R.layout.row, R.id.row, noncalcolate);
                nonlist.setAdapter(nonad);
                nonlist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        setContentView(R.layout.calcolo);
                        final String temp = "GIORNATA " + noncalcolate[i];
                        ((TextView) findViewById(R.id.header)).setText(temp);
                        final ListView list = findViewById(R.id.squadre);
                        bonus = new String[squadre.length];
                        mAdapter ad = new mAdapter(context, squadre);
                        list.setAdapter(ad);

                        if (rinvio.get(noncalcolate[i]) != null) {
                            int [] ids = context.getResources().getIntArray(R.array.FG);
                            String [] serieA = context.getResources().getStringArray(R.array.squadre);
                            String [] array_rinvio = new String[rinvio.get(noncalcolate[i]).size()];
                            boolrinvio = new boolean[rinvio.get(noncalcolate[i]).size()];
                            for (int k = 0; k < rinvio.get(noncalcolate[i]).size(); k++) {
                                int f = 0;
                                boolean found = false;
                                while (!found) {
                                    if(ids[f] == Integer.parseInt(rinvio.get(noncalcolate[i]).get(k))) {
                                        array_rinvio[k] = serieA[f];
                                        found = true;
                                    }
                                    f++;
                                }
                                boolrinvio[k] = false;
                            }
                            ListView list2 = findViewById(R.id.squadrerinvio);
                            mAdapter2 ad2 = new mAdapter2(context, array_rinvio);
                            list2.setAdapter(ad2);
                            list2.setVisibility(View.VISIBLE);
                            findViewById(R.id.rinvio).setVisibility(View.VISIBLE);
                        }

                        findViewById(R.id.conferma).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                for(int i = 0; i < valori.size() - 1; i++) {
                                    bonus[i] = valori.get(i).getText().toString();
                                }

                                new Post().execute(temp.split("GIORNATA ")[1]);
                            }
                        });
                    }
                });
                setContentView(nonlist);
                show();
            } else {
                Toast.makeText(context, POST, Toast.LENGTH_SHORT).show();
                onBackPressed();
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class Post extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground (String... param) {

            try {
                URL url = new URL("https://leghe.fantacalcio.it/servizi/V1_LegheCalcolo/CalcolaGiornata?alias_lega=" + HttpRequest.lega +
                        "&id_competizione=" + comp + "&giornata=" + param[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setRequestProperty("app_key", "4ab27d6de1e92c810c6d4efc8607065a735b917f");
                connection.setRequestProperty("Cookie", HttpRequest.cookie);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                JSONObject form = new JSONObject();
                JSONArray array_bonus = new JSONArray();
                for (int i = 0; i < squadre.length; i++) {
                    JSONObject json_bonus = new JSONObject();
                    json_bonus.put("bonus", bonus[i]);
                    json_bonus.put("id", codici[i]);
                    array_bonus.put(json_bonus);
                }
                form.put("bonus", array_bonus);
                JSONArray array_rinvio = new JSONArray();
                if (boolrinvio != null) {
                    for (int i = 0; i < boolrinvio.length; i++) {
                        if (boolrinvio[i]) {
                            array_rinvio.put(rinvio.get(param[0]).get(i));
                        }
                    }
                }
                form.put("ufficio", array_rinvio);

                String wout = form.toString();
                byte[] postData = wout.getBytes();
                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.write(postData);

                BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                JSONObject json = new JSONObject(rd.readLine());
                if (json.getBoolean("success")) {
                    return "Giornata calcolata con successo";
                } else {
                    return "Errore";
                }
            } catch (Exception e) {
                return "Errore";
            }
        }

        @Override
        protected void onPostExecute (String result) {
            Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
            dismiss();
        }
    }
}
