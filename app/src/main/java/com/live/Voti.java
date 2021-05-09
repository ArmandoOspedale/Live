package com.live;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;

import org.jsoup.nodes.Document;

import java.io.IOException;

public class Voti extends AppCompatActivity {

    String [] squadre;
    String tvstamp;
    String ultima;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("VOTI SERIE A");

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        squadre = getResources().getStringArray(R.array.FG);
        new Votes().execute("https://www.fantacalcio.it/voti-fantacalcio-serie-a");
    }

    @SuppressLint("StaticFieldLeak")
    private class Votes extends AsyncTask<String, Void, String[]> {

        @Override
        protected String[] doInBackground(String... strings) {
            try {
                Document doc = HttpRequest.GET_nolega(strings[0], "col-lg-3 col-md-3 col-sm-12 col-xs-12 col-right");
                tvstamp = doc.select("input[id=tvstamp").attr("value");
                ultima = doc.select("input[id=ultimaC]").attr("value");

                String header = "https://www.fantacalcio.it/Servizi/Voti.ashx?s=2017-18" +
                        "&g=" + ultima + "&tv=" + tvstamp + "&t=1";

                System.out.println(HttpRequest.GET_nolega(header, "").toString());

                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String[] strings) {
            super.onPostExecute(strings);
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
