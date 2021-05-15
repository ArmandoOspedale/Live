package com.live;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ListaSvincolati extends AppCompatActivity {

    private final List<String[]> por = new ArrayList<>();
    private final List<String[]> dif = new ArrayList<>();
    private final List<String[]> cen = new ArrayList<>();
    private final List<String[]> att = new ArrayList<>();
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("LISTA SVINCOLATI");
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        new Lista().execute();
    }

    @SuppressLint("StaticFieldLeak")
    private class Lista extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                URL url = new URL("https://leghe.fantacalcio.it/servizi/V1_LegheCalciatori/listaSvincolatiNoMercato?alias_lega=" + HttpRequest.lega + "&t=0");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestProperty("app_key", "4ab27d6de1e92c810c6d4efc8607065a735b917f");
                connection.setRequestProperty("cookie", HttpRequest.cookie);

                BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                return new JSONObject(rd.readLine()).getString("data");
            } catch (IOException | JSONException e) {
                return "";
            }
        }

        @Override
        protected void onPostExecute(String s) {
            try {
                JSONArray ja = new JSONArray(s);
                for (int j = 0; j < ja.length(); j++) {
                    JSONObject jo = (JSONObject) ja.get(j);

                    String[] temp = new String[5];
                    temp[0] = jo.getString("r").toUpperCase();
                    temp[1] = jo.getString("n");
                    temp[2] = jo.getString("si");
                    temp[3] = jo.getString("id");
                    temp[4] = jo.getString("ca");
                    switch (temp[0]) {
                        case "P":
                            por.add(temp);
                            break;
                        case "D":
                            dif.add(temp);
                            break;
                        case "C":
                            cen.add(temp);
                            break;
                        case "A":
                            att.add(temp);
                            break;
                    }
                }
                setContentView(R.layout.svincolati);

                viewPager = findViewById(R.id.viewpager);
                viewPager.setAdapter(new myPagerAdapter(getSupportFragmentManager()));
                final TabLayout tabLayout = findViewById(R.id.sliding_tabs);
                tabLayout.setBackgroundColor(Color.rgb(1, 174, 240));
                tabLayout.setTabTextColors(Color.argb(138,255,255,255), Color.argb(222,255,255,255));
                tabLayout.post(() -> tabLayout.setupWithViewPager(viewPager));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class myPagerAdapter extends FragmentStatePagerAdapter {
        final int PAGE_COUNT = 4;
        private final String[] tabTitles = new String[] { "POR", "DIF", "CEN", "ATT" };

        myPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return PAGE_COUNT;
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            SvincolatiFragment fragment = new SvincolatiFragment();
            Bundle b = new Bundle();
            switch (position) {
                case 0:
                    b.putSerializable("giocatori", por.toArray(new String[por.size()][5]));
                    break;
                case 1:
                    b.putSerializable("giocatori", dif.toArray(new String[dif.size()][5]));
                    break;
                case 2:
                    b.putSerializable("giocatori", cen.toArray(new String[cen.size()][5]));
                    break;
                case 3:
                    b.putSerializable("giocatori", att.toArray(new String[att.size()][5]));
                    break;
            }

            fragment.setArguments(b);
            return fragment;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return tabTitles[position];
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return POSITION_NONE;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ordina, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (id == R.id.action_nome) {
            Collections.sort(por, Nome);
            Collections.sort(dif, Nome);
            Collections.sort(cen, Nome);
            Collections.sort(att, Nome);
            Objects.requireNonNull(viewPager.getAdapter()).notifyDataSetChanged();
        }

        if (id == R.id.action_squadra) {
            Collections.sort(por, Squadra);
            Collections.sort(dif, Squadra);
            Collections.sort(cen, Squadra);
            Collections.sort(att, Squadra);
            Objects.requireNonNull(viewPager.getAdapter()).notifyDataSetChanged();
        }

        if (id == R.id.action_quot) {
            Collections.sort(por, Quotazione);
            Collections.sort(dif, Quotazione);
            Collections.sort(cen, Quotazione);
            Collections.sort(att, Quotazione);
            Objects.requireNonNull(viewPager.getAdapter()).notifyDataSetChanged();
        }

        return super.onOptionsItemSelected(item);
    }

    public static Comparator<String[]> Nome
            = (g1, g2) -> {

                String nome1 = g1[1];
                String nome2 = g2[1];

                return nome1.compareTo(nome2);
            };

    private static final Comparator<String[]> Squadra
            = (g1, g2) -> {

                String squadra1 = g1[2];
                String squadra2 = g2[2];

                return squadra1.compareTo(squadra2);
            };

    private static final Comparator<String[]> Quotazione
            = (g1, g2) -> {

                double quot1 = Double.parseDouble(g1[4]);
                double quot2 = Double.parseDouble(g2[4]);

                return (int)(quot2 - quot1);
            };
}
