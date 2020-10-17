package com.live;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

class Gestione_Formazioni extends Dialog {

    private final Context context;
    private final String comp;
    private final String [] orig_codici;
    private final String [] orig_squadre;

    Gestione_Formazioni (Context cxt, String c, String[] c2, String[] s) {
        super(cxt, R.style.CustomDialog);
        setTitle("Seleziona squadra");

        context = cxt;
        comp = c;
        orig_codici = c2;
        orig_squadre = s;

        new Dati().execute();
    }

    @SuppressLint("StaticFieldLeak")
    private class Dati extends AsyncTask<String, Void, JSONArray[]> {

        @Override
        protected JSONArray[] doInBackground (String... param) {

            try {
                URL url = new URL("https://leghe.fantacalcio.it/servizi/V1_LegheCompetizione/GiornateNonCalcolate?alias_lega=" + HttpRequest.lega +
                        "&id_competizione=" + comp);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestProperty("app_key", "4ab27d6de1e92c810c6d4efc8607065a735b917f");
                connection.setRequestProperty("cookie", HttpRequest.cookie);

                BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                JSONArray giornate = new JSONObject(rd.readLine()).getJSONArray("data");

                url = new URL("https://leghe.fantacalcio.it/servizi/V1_LegheFormazioni/lista?alias_lega=" + HttpRequest.lega +
                        "&id_comp=" + comp + "&giornata=" + giornate.getString(0));

                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestProperty("app_key", "4ab27d6de1e92c810c6d4efc8607065a735b917f");
                connection.setRequestProperty("cookie", HttpRequest.cookie);

                rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                JSONArray squadre = new JSONObject(rd.readLine()).getJSONArray("data");

                return new JSONArray[]{giornate, squadre};
            } catch (IOException | JSONException e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute (JSONArray[] result) {
            if (result != null) {
                try {
                    ListView nonlist = new ListView(context);
                    JSONArray j_squadre = result[1];
                    final String[] codici = new String[j_squadre.length()];
                    String[] squadre = new String[j_squadre.length()];
                    for (int i = 0; i < j_squadre.length(); i++) {
                        codici[i] = j_squadre.getJSONObject(i).getString("id");
                        squadre[i] = HttpRequest.getNomeSquadra(codici[i], orig_codici, orig_squadre);
                    }
                    JSONArray j_giornate = result[0];
                    final String[] giornate = new String[j_giornate.length()];
                    for (int i = 0; i < giornate.length; i++) {
                        giornate[i] = j_giornate.getString(i);
                    }
                    ArrayAdapter<String> nonad = new ArrayAdapter<>(context, R.layout.row, R.id.row, squadre);
                    nonlist.setAdapter(nonad);
                    nonlist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, final int position, long l) {
                            ListView list = new ListView(context);

                            ArrayAdapter<String> ad = new ArrayAdapter<>(context, R.layout.row, R.id.row, giornate);
                            list.setAdapter(ad);

                            setTitle("Seleziona giornata");
                            setContentView(list);

                            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                    Intent intent = new Intent(context, Schiera.class);
                                    intent.putExtra("comp", comp);
                                    intent.putExtra("teamId", codici[position]);
                                    intent.putExtra("giornata", Integer.parseInt(giornate[i]));
                                    intent.putExtra("admin", 1);
                                    context.startActivity(intent);
                                    dismiss();
                                }
                            });
                        }
                    });

                    setContentView(nonlist);
                    show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(context, "ERRORE", Toast.LENGTH_SHORT).show();
                onBackPressed();
            }
        }
    }
}
