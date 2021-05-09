package com.live;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.StateListDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

public class Calendario extends AppCompatActivity {

    String comp;
    String[] squadre;
    String[] codici;
    private int current = -1;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.intcal);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        Intent i = getIntent();
        comp = i.getStringExtra("comp");
        squadre = i.getStringArrayExtra("squadre");
        codici = i.getStringArrayExtra("codici");

        new DownloadCal().execute();
    }

    @SuppressLint("StaticFieldLeak")
    private class DownloadCal extends AsyncTask<String, Integer, String [][]> {

        @Override
        protected String [][] doInBackground(String... param) {

            String [][] partite = null;

            try {
                Document doc = HttpRequest.GET("/calendario?id=" + comp, "breadcrumb");

                JSONArray json_giornate = new JSONObject(new String(Base64.decode(doc.select("script[id=s001]").toString().split("dp\\('")[1].split("'\\)\\);")[0], Base64.DEFAULT))).getJSONObject("data").getJSONObject("calendario").getJSONArray("c_inc");

                partite = new String[json_giornate.length()][json_giornate.getJSONObject(0).getJSONArray("inc").length() + 1];
                for (int i = 0; i < json_giornate.length(); i++) {
                    JSONArray incontri = json_giornate.getJSONObject(i).getJSONArray("inc");
                    partite[i][0] = json_giornate.getJSONObject(i).getString("g_l") + "-" + json_giornate.getJSONObject(i).getString("g_a");
                    for (int j = 1; j < incontri.length() + 1; j++) {
                        JSONObject incontro = incontri.getJSONObject(j - 1);
                        if(json_giornate.getJSONObject(i).getBoolean("cal")) {
                            partite[i][j] = HttpRequest.getNomeSquadra(incontro.getString("id_a"), codici, squadre) + "/" +
                                    HttpRequest.getNomeSquadra(incontro.getString("id_b"), codici, squadre) + "/" +
                                    incontro.getString("p_a") + "-" +incontro.getString("p_b") + "/" + incontro.getString("res");
                        } else {
                            if (current == -1) current = i;
                            partite[i][j] = HttpRequest.getNomeSquadra(incontro.getString("id_a"), codici, squadre) +
                                    "  " + incontro.getString("res") + "  " + HttpRequest.getNomeSquadra(incontro.getString("id_b"), codici, squadre);
                        }
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            return partite;
        }

        @Override
        protected void onPostExecute(final String [][] result) {
            final ListView calendario = new ListView(Calendario.this);
            calendario.setBackgroundColor(Color.WHITE);
            calendario.setSelector(new StateListDrawable());
            calendario.setFastScrollEnabled(true);
            mAdapter adapter = new mAdapter(result);
            calendario.setAdapter(adapter);
            setContentView(calendario);
        }
    }

    private class mAdapter extends BaseAdapter implements SectionIndexer {

        String[][] giornate;
        int partite;

        mAdapter(String [][] g) {
            giornate = g;
            partite = giornate[0].length;
        }

        @Override
        public Object getItem(int i) {
            int giornata = i/partite;
            return giornate[giornata][i - giornata*partite];
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public int getCount() {
            return giornate.length*partite;
        }

        @Override
        @SuppressWarnings("all")
        public View getView(int i, View view, ViewGroup viewGroup) {

            if (i % partite == 0) {
                view = LayoutInflater.from(Calendario.this).inflate(R.layout.row, null);

                TextView header = (TextView) view.findViewById(R.id.row);
                header.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                header.setBackgroundColor(Color.rgb(1, 174, 240));
                header.setTextColor(Color.WHITE);
                header.setTextSize(22);
                header.setText(
                Html.fromHtml("<h4>" +
                        ((String) getItem(i)).split("-")[0] + "° Giornata <small><font color=#1274AF>(" +
                        ((String) getItem(i)).split("-")[1] + "° giornata di Serie A)</font></small></h4>"));
            } else {
                if (i / partite < current) {
                    view = LayoutInflater.from(Calendario.this).inflate(R.layout.calenlist, null);

                    String match = (String) getItem(i);
                    String[] dati = match.split("/");
                    ((TextView) view.findViewById(R.id.casa)).setText(dati[0]);
                    ((TextView) view.findViewById(R.id.trasf)).setText(dati[1]);
                    ((TextView) view.findViewById(R.id.punteggio)).setText(dati[2]);
                    ((TextView) view.findViewById(R.id.result)).setText(dati[3]);
                } else {
                    view = LayoutInflater.from(Calendario.this).inflate(R.layout.row, null);

                    ((TextView) view.findViewById(R.id.row)).setText((String) getItem(i));
                }
            }

            return view;
        }

        @Override
        public Object[] getSections() {
            String [] sections = new String[giornate.length];
            for (int i = 0; i < giornate.length; i++) {
                sections[i] = String.valueOf(i + 1);
            }
            return sections;
        }

        @Override
        public int getSectionForPosition(int i) {
            return 0;
        }

        @Override
        public int getPositionForSection(int i) {
            return i * partite;
        }
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