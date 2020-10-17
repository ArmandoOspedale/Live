package com.live;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class Impostazioni extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("OPZIONI LEGA");
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();
        Opzioni_lega opzioni = (Opzioni_lega) intent.getSerializableExtra("opzioni");

        setContentView(R.layout.impostazioni);
        double portiere_imbattuto = 0;

        try {
            JSONObject bonus = new JSONObject(opzioni.bonus);

            LinearLayout l = findViewById(R.id.bonus);

            Iterator<String> it = bonus.keys();
            while (it.hasNext()) {
                String key = it.next();
                @SuppressLint("InflateParams") View v = LayoutInflater.from(this).inflate(R.layout.impost_list, null);
                TextView txt = v.findViewById(R.id.nome);
                String text = key.substring(0, 1).toUpperCase() + key.substring(1).replaceAll("_", " ") + ":";
                txt.setText(text);
                if(key.equals("portiere_imbattuto")) {
                    portiere_imbattuto = bonus.getDouble("portiere_imbattuto");
                } else if (!key.equals("tipo_assist") && !key.equals("assist_level")){
                    JSONArray ruoli = bonus.getJSONArray(key);
                    txt = v.findViewById(R.id.P);
                    txt.setText(ruoli.getString(0));
                    txt = v.findViewById(R.id.D);
                    txt.setText(ruoli.getString(1));
                    txt = v.findViewById(R.id.C);
                    txt.setText(ruoli.getString(2));
                    txt = v.findViewById(R.id.A);
                    txt.setText(ruoli.getString(3));
                    l.addView(v);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ((TextView) findViewById(R.id.sost)).setText(String.valueOf(opzioni.numsost));

        ((TextView) findViewById(R.id.base)).setText(String.valueOf(opzioni.base));

        ((TextView) findViewById(R.id.fascia)).setText(String.valueOf(opzioni.fascia));

        if (opzioni.ammsv_checked) {
            ((RadioButton) findViewById(R.id.ammsv_si)).setChecked(true);
            ((TextView) findViewById(R.id.ammsv)).setText(String.valueOf(opzioni.ammsv_value));
        } else {
            ((RadioButton) findViewById(R.id.ammsv_no)).setChecked(true);
            (findViewById(R.id.ammsv)).setVisibility(View.GONE);
        }

        if (opzioni.intorno_checked) {
            ((RadioButton) findViewById(R.id.intorno_si)).setChecked(true);
            ((TextView) findViewById(R.id.intorno)).setText(String.valueOf(opzioni.intorno_value));
        } else {
            ((RadioButton) findViewById(R.id.intorno_no)).setChecked(true);
            (findViewById(R.id.intorno)).setVisibility(View.GONE);
        }

        if (opzioni.differenza_checked) {
            ((RadioButton) findViewById(R.id.differenza_si)).setChecked(true);
            ((TextView) findViewById(R.id.differenza)).setText(String.valueOf(opzioni.differenza_value));
        } else {
            ((RadioButton) findViewById(R.id.differenza_no)).setChecked(true);
            (findViewById(R.id.differenza)).setVisibility(View.GONE);
        }

        if (opzioni.pareggio_checked) {
            ((RadioButton) findViewById(R.id.pareggio_si)).setChecked(true);
            ((TextView) findViewById(R.id.pareggio)).setText(String.valueOf(opzioni.pareggio_value));
        } else {
            ((RadioButton) findViewById(R.id.pareggio_no)).setChecked(true);
            (findViewById(R.id.pareggio)).setVisibility(View.GONE);
        }

        if (Double.compare(0d, portiere_imbattuto) != 0) {
            ((RadioButton) findViewById(R.id.pimbattuto_si)).setChecked(true);
            ((TextView) findViewById(R.id.pimbattuto)).setText(String.valueOf(portiere_imbattuto));
        } else {
            ((RadioButton) findViewById(R.id.pimbattuto_no)).setChecked(true);
            (findViewById(R.id.pimbattuto)).setVisibility(View.GONE);
        }

        if (opzioni.mod_checked) {
            ((RadioButton) findViewById(R.id.mod_si)).setChecked(true);
            for (int i = 0; i < opzioni.mod.length; i++) {
                @SuppressLint("InflateParams") View view = LayoutInflater.from(this).inflate(R.layout.mod, null);
                ((TextView) view).setText(String.valueOf(opzioni.mod[i]));
                ((LinearLayout) findViewById(R.id.mod)).addView(view);
                @SuppressLint("InflateParams") View fas_view = LayoutInflater.from(this).inflate(R.layout.mod, null);
                if (i == opzioni.mod.length - 1) {
                    String text = ">=" + opzioni.mod_max;
                    ((TextView) fas_view).setText(text);
                    ((LinearLayout) findViewById(R.id.fas)).addView(fas_view);
                } else {
                    String text = "<" + (i == 0 ? opzioni.mod_min : opzioni.mod_min + i * 0.25d - 0.01d);
                    ((TextView) fas_view).setText(text);
                    ((LinearLayout) findViewById(R.id.fas)).addView(fas_view);
                }
            }
        } else {
            ((RadioButton) findViewById(R.id.mod_no)).setChecked(true);
            (findViewById(R.id.fas)).setVisibility(View.GONE);
            (findViewById(R.id.mod)).setVisibility(View.GONE);
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
