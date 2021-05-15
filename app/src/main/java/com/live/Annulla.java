package com.live;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
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
import java.util.ArrayList;
import java.util.List;

class Annulla extends Dialog {

    Context context;
    String comp;

    Annulla (Context cxt, String competizione) {
        super(cxt, R.style.CustomDialog);
        setTitle(R.string.annulla);

        context = cxt;
        comp = competizione;

        new Dati().execute();
    }

    @SuppressLint("StaticFieldLeak")
    private class Dati extends AsyncTask<String, Void, Boolean> {

        String[] calcolate;
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
                        if (json_data.getJSONObject(i).getBoolean("c")) {
                            temp.add(json_data.getJSONObject(i).getString("g"));
                        }
                    }

                    if (temp.size() > 0) {
                        calcolate = new String[temp.size()];
                        calcolate = temp.toArray(calcolate);
                    } else {
                        POST = "Nessuna giornata da annullare";
                        return false;
                    }

                    return true;
                } else {
                    POST = "Nessuna giornata da annullare";
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
                ArrayAdapter<String> nonad = new ArrayAdapter<>(context, R.layout.row, R.id.row, calcolate);
                nonlist.setAdapter(nonad);
                nonlist.setOnItemClickListener((adapterView, view, i, l) -> {
                    String calcolata = calcolate[i];

                    new Post().execute(calcolata);
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
                URL url = new URL("https://leghe.fantacalcio.it/servizi/V1_LegheCalcolo/AnnullaCalcoloGiornata?alias_lega=" + HttpRequest.lega +
                        "&id_competizione=" + comp + "&giornata=" + param[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("PUT");
                connection.setRequestProperty("app_key", "4ab27d6de1e92c810c6d4efc8607065a735b917f");
                connection.setRequestProperty("Cookie", HttpRequest.cookie);

                BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                JSONObject json = new JSONObject(rd.readLine());
                if (json.getBoolean("success")) {
                    return "Giornata annullata con successo";
                } else {
                    return "Errore";
                }
            } catch (IOException | JSONException e) {
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
