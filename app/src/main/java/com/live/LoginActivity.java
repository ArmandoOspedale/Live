package com.live;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class LoginActivity extends Activity {

    String user;
    String id_utente;
    private String[] leghe;
    String[] squadre;
    ProgressDialog p;
    //private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        final Button login = findViewById(R.id.btn_login);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                p = new ProgressDialog(LoginActivity.this);
                p.setCancelable(false);
                p.setMessage("Verifica login...");
                p.show();
                EditText un = findViewById(R.id.et_un);
                InputMethodManager imm1 = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if(imm1 != null) imm1.hideSoftInputFromWindow(un.getWindowToken(), 0);

                user = un.getText().toString();
                EditText pw = findViewById(R.id.et_pw);

                String pass = pw.getText().toString();

                new Login().execute(user, pass);
            }
        });
    }

    @SuppressLint("StaticFieldLeak")
    private class Login extends AsyncTask<String, Void, Boolean> {
        String info = "ERRORE";

        @Override
        protected Boolean doInBackground(String... strings) {
            try {
                String path = "https://leghe.fantacalcio.it/api/v1/v1_utente/login?&alias_lega=";
                //path = path + "username=" + strings[0];
                //path = path + "&password=" + strings[1];
                URL url = new URL(path);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setRequestMethod("PUT");
                connection.setRequestProperty("app_key", "4ab27d6de1e92c810c6d4efc8607065a735b917f");
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                JSONObject form = new JSONObject();
                form.put("username", strings[0]);
                form.put("password", strings[1]);

                String wout = form.toString();
                byte[] postData = wout.getBytes();
                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.write(postData);

                BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                JSONObject response = new JSONObject(new String(Base64.decode(new JSONObject(rd.readLine()).getString("data"), Base64.DEFAULT)));
                Boolean lgresult = response.getBoolean("success");
                rd.close();
                if (lgresult) {

                    Map<String, List<String>> map = connection.getHeaderFields();
                    List<String> fields = map.get("Set-Cookie");
                    String c = fields.get(2);

                    JSONArray json_leghe = new JSONObject(response.getString("data")).getJSONArray("leghe");
                    id_utente = new JSONObject(response.getString("data")).getJSONObject("utente").getString("id");
                    leghe = new String[json_leghe.length()];
                    squadre = new String[json_leghe.length()];
                    for (int j = 0; j < leghe.length; j++) {
                        leghe[j] = json_leghe.getJSONObject(j).getString("nome").replaceAll(" ", "-");
                    }

                    HttpRequest.cookie = c;
                    HttpRequest.serialize(LoginActivity.this, leghe, "leghe");
                    HttpRequest.serialize(LoginActivity.this, c, "cookie");
                } else if (!response.getString("error_msgs").equals("null")){
                    info = response.getJSONArray("error_msgs").getJSONObject(0).getString("descrizione");
                }
                return lgresult;

            } catch (IOException | JSONException e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                new Download_opzioni().execute();
            } else {
                p.cancel();
                Toast.makeText(getBaseContext(), info, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class Download_opzioni extends AsyncTask<String, String, Void> {

        @Override
        protected Void doInBackground(String... strings) {

            for (int k = 0; k < leghe.length; k++) {
                try {
                    HttpRequest.lega = leghe[k];
                    Opzioni_lega opzioni = new Opzioni_lega();

                    publishProgress("Recupero opzioni calcolo " + leghe[k] + "...");
                    Document doc = HttpRequest.GET("/gestione-lega/opzioni-rose", "btn btn-fab btn-fab-mini");
                    Elements scripts = doc.select("script");
                    String squadra;
                    String default_code = null;
                    JSONObject bonus = null;
                    for (Element e : scripts) {
                        String toString = e.toString();
                        if (toString.contains("__.s('li',")) {
                            squadra = toString.split("teamName: \"")[1].split("\",")[0];
                            squadre[k] = squadra;
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
                            opzioni.mod = new double[mod.getJSONArray("difesa").length()];
                            for (int m = 0; m < opzioni.mod.length; m++) {
                                opzioni.mod[m] = mod.getJSONArray("difesa").getDouble(m);
                            }
                            opzioni.mod_min = mod.getDouble("difesa_limits_min");
                            opzioni.mod_max = mod.getDouble("difesa_limits_max");
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

                    HttpRequest.serialize(LoginActivity.this, opzioni, leghe[k]);
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
            HttpRequest.serialize(LoginActivity.this, squadre, "squadre");
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            p.setMessage(values[0]);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            p.cancel();
            new AsyncTask<Void, Void, Void>() {
                @Override
                @SuppressLint("WrongThread")
                protected Void doInBackground(Void... params) {
                    MyInstanceIDListenerService.sendRegistrationToServer(user);
                    return null;
                }
            }.execute();
            //if (checkPlayServices()) {
            /*SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
            boolean sentToken = sharedPreferences
                    .getBoolean("sentTokenToServer", false);
            if (!sentToken) {
                Intent intent = new Intent(LoginActivity.this, RegistrationIntentService.class);
                startService(intent);
            }*/
            //}
            setResult(RESULT_OK, new Intent().putExtra("leghe", leghe));
            onBackPressed();
        }
    }

    /*private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                finish();
            }
            return false;
        }
        return true;
    }*/
}
