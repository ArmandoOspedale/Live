package com.live;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

public class PartitaLive extends AppCompatActivity {

    private ViewPager viewPager;
    private String partita;
    private String teamA;
    private String teamB;
    private int idA;
    private int idB;
    String giornata;

    @SuppressLint("SetJavaScriptEnabled")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("LIVE MATCH");
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        Intent i = getIntent();
        String title = i.getStringExtra("title");
        String match = i.getStringExtra("match");

        if (title.contains("fantacalcio")) {
            if (ab != null) {
                ab.hide();
            }
            String url = i.getStringExtra("uri");

            final WebView webview = new WebView(this);
            webview.getSettings().setJavaScriptEnabled(true);
            webview.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    return false;
                }
            });

            setContentView(webview);
            webview.loadUrl(url);
        } else {
            new Info().execute(match, i.getStringExtra("giornata"));
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class Info extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {
            try {
                Document doc = HttpRequest.GET_nolega("https://m.tuttomercatoweb.com/live-partite", "");

                Elements partite = doc.select("div[class=tcc-link]");

                String toFind = strings[0].replaceAll(" ", "").toLowerCase();
                String [] squadre = toFind.split("-");

                String [] serieA = getResources().getStringArray(R.array.squadre);
                boolean found = false;
                int j = 0;
                while (!found) {
                    if(serieA[j].equals(squadre[0].toUpperCase())) {teamA = serieA[j]; found = true; j--;}
                    j++;
                }

                found = false;
                int k = 0;
                while (!found) {
                    if(serieA[k].equals(squadre[1].toUpperCase())) {teamB = serieA[k]; found = true; k--;}
                    k++;
                }

                int [] ids = getResources().getIntArray(R.array.FG);
                idA = ids[j];
                idB = ids[k];
                giornata = strings[1];

                if (squadre[0].equals("chievo")) squadre[0] = "chievo-verona";
                if (squadre[1].equals("verona")) squadre[1] = "hellas-verona";
                toFind = squadre[0] + "-" + squadre[1];

                for (int i = 0; i < partite.size(); i++) {
                    if (partite.get(i).attr("onclick").contains(toFind)) {
                        partita = partite.get(i).attr("onclick").split("'")[1];
                        return null;
                    }
                }
                return null;
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Void v) {
            setContentView(R.layout.partita_live);

            ((TextView) findViewById(R.id.casa)).setText(teamA);
            ((TextView) findViewById(R.id.trasf)).setText(teamB);

            ImageView im = findViewById(R.id.icona_casa);
            im.setImageResource(getDrawable(PartitaLive.this, teamA.toLowerCase()));

            im = findViewById(R.id.icona_trasf);
            im.setImageResource(getDrawable(PartitaLive.this, teamB.toLowerCase()));

            viewPager = findViewById(R.id.viewpager);
            viewPager.setOffscreenPageLimit(2);
            PagerAdapter pagerAdapter = new myPagerAdapter(getSupportFragmentManager());
            viewPager.setAdapter(pagerAdapter);
            final TabLayout tabLayout = findViewById(R.id.sliding_tabs);
            tabLayout.setBackgroundColor(Color.rgb(1, 174, 240));
            tabLayout.setTabTextColors(Color.argb(138, 255, 255, 255), Color.argb(222, 255, 255, 255));
            tabLayout.post(() -> tabLayout.setupWithViewPager(viewPager));
        }
    }

    private class myPagerAdapter extends FragmentStatePagerAdapter {

        myPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            PartitaLive_Fragment fragment = new PartitaLive_Fragment();
            Bundle b = new Bundle();
            b.putInt("position", position);
            b.putString("partita", partita);
            b.putInt("teamA", idA);
            b.putInt("teamB", idB);
            b.putString("giornata", giornata);

            fragment.setArguments(b);
            return fragment;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return "VOTI";
                case 1: return "EVENTI";
                case 2: return "CRONACA";
                default: return null;
            }
        }
    }

    private int getDrawable(Context context, String name) {
        return context.getResources().getIdentifier(name,
                "drawable", context.getPackageName());
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
