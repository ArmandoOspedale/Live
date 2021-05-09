package com.live;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.StateListDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Statistiche extends AppCompatActivity {

    private StatsAdapter adapter;
    private boolean initialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("STATISTICHE");
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        new Stats().execute("https://www.fantacalcio.it/statistiche-serie-a");
    }

    @SuppressLint("StaticFieldLeak")
    private class Stats extends AsyncTask<String, Void, String[]> {

        @Override
        protected String[] doInBackground(String... strings) {
            try {
                String data_stamp = HttpRequest.GET_nolega(strings[0], "col-lg-3 col-md-3 col-sm-12 col-xs-12 col-right").select("li[data-role=portieri").attr("data-stamp");

                String header = "https://content.fantacalcio.it/web/statistiche/tabelle/2020-21/fantacalcio/riepilogo/";
                String header2 = "https://content.fantacalcio.it/web/statistiche/tabelle/2020-21/fantacalcio/rigori/";
                String[] urls = new String[]{header + "portieri" + data_stamp + ".txt",
                        header + "difensori" + data_stamp + ".txt", header + "centrocampisti" + data_stamp + ".txt",
                        header + "attaccanti" + data_stamp + ".txt", header2 + "centrocampisti" + data_stamp + ".txt",
                        header2 + "attaccanti" + data_stamp + ".txt"};

                String [] stats = new String[6];

                for (int i = 0; i < 6; i++) {
                    stats[i] = HttpRequest.GET_nodocument(urls[i]);
                }

                return stats;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String[] strings) {
            JSONArray[] jas = new JSONArray[4];
            final List<List<HashMap<String, String>>> stats = new ArrayList<>();

            for (int i = 0; i < 4; i++) {
                try {
                    jas[i] = new JSONObject(strings[i]).getJSONArray("data");
                    JSONArray jar = null;
                    if (i == 2 || i == 3) jar = new JSONObject(strings[i + 2]).getJSONArray("data");

                    List<HashMap<String, String>> temp = new ArrayList<>();
                    for (int j = 0; j < jas[i].length(); j++) {
                        JSONArray valori = jas[i].getJSONArray(j);
                        HashMap<String, String> map = new HashMap<>();
                        map.put("Calciatore", Jsoup.parse(valori.getString(0)).text());
                        map.put("Pg", valori.getString(1));
                        map.put("Mv", valori.getString(2));
                        map.put("Mf", valori.getString(3));
                        if (i == 2 || i == 3) {
                            JSONArray rigori = jar.getJSONArray(j);
                            map.put("G", String.valueOf(Integer.parseInt(valori.getString(4)) + Integer.parseInt(rigori.getString(3))));
                        } else {
                            map.put("G", valori.getString(4));
                        }
                        map.put("Ass", valori.getString(5));
                        map.put("Amm", valori.getString(6));
                        map.put("Esp", valori.getString(7));
                        temp.add(map);
                    }

                    stats.add(temp);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            setContentView(R.layout.stats);

            findViewById(R.id.header).setBackgroundColor(Color.rgb(255, 225, 15));

            ArrayAdapter<String> spadapter = new ArrayAdapter<>(Statistiche.this, R.layout.spinner_item,
                    new String[]{"Portieri", "Difensori", "Centrocampisti", "Attaccanti"});
            spadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            ((Spinner) findViewById(R.id.spinner)).setAdapter(spadapter);

            ListView l = findViewById(R.id.listView);
            l.setSelector(new StateListDrawable());
            adapter = new StatsAdapter(Statistiche.this, stats.get(0));
            Ordina.setAdapter(adapter);
            ((Ordina) findViewById(R.id.enabled)).abilita();
            adapter.sort("Mv", true);

            ((Ordina) findViewById(R.id.enabled)).abilita();

            ((Spinner) findViewById(R.id.spinner)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    if (initialized) {
                        switch (i) {
                            case 0:
                                findViewById(R.id.header).setBackgroundColor(Color.rgb(255, 225, 15));
                                break;
                            case 1:
                                findViewById(R.id.header).setBackgroundColor(Color.rgb(0, 128, 10));
                                break;
                            case 2:
                                findViewById(R.id.header).setBackgroundColor(Color.rgb(5, 29, 192));
                                break;
                            case 3:
                                findViewById(R.id.header).setBackgroundColor(Color.RED);
                                break;
                        }

                        adapter.setData(stats.get(i));
                        ((Ordina) findViewById(R.id.enabled)).abilita();
                        adapter.sort("Mv", true);
                    } else {
                        initialized = true;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            l.setAdapter(adapter);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_stats, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        if(searchManager != null) searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        SearchView.OnQueryTextListener textChangeListener = new SearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextChange(String newText)
            {
                adapter.getFilter().filter(newText);
                return true;
            }
            @Override
            public boolean onQueryTextSubmit(String query)
            {
                adapter.getFilter().filter(query);
                searchView.clearFocus();
                return true;
            }
        };
        searchView.setOnQueryTextListener(textChangeListener);

        return super.onCreateOptionsMenu(menu);
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
