package com.live;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.StateListDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Assert;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class SvincolatiFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ListView list = new ListView(getActivity());
        list.setSelector(new StateListDrawable());
        list.setBackgroundColor(Color.WHITE);
        assert getArguments() != null;
        final String[][] giocatori = (String[][]) getArguments().get("giocatori");
        list.setAdapter(new mAdapter(getActivity(), giocatori));

        Assert.assertNotNull(giocatori);
        list.setOnItemLongClickListener((adapterView, view, i, l) -> {
            new Stats().execute(giocatori[i]);
            return true;
        });
        return list;
    }

    private static class mAdapter extends BaseAdapter {

        private final Context context;
        String[][] giocatori;

        mAdapter(Context context, String[][] g) {
            this.context = context;
            giocatori = g;
        }

        @Override
        public int getCount()
        {
            return giocatori.length;
        }

        @Override
        public Object getItem(int position) {
            return giocatori[position];
        }

        @Override
        public long getItemId(int position)
        {
            return getItem(position).hashCode();
        }

        @Override
        @SuppressWarnings("all")
        public View getView(int position, View v, ViewGroup vg)
        {
            if (v==null)
            {
                v = LayoutInflater.from(context).inflate(R.layout.giocatore, null);
                TypedValue outValue = new TypedValue();
                context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                v.setBackgroundResource(outValue.resourceId);
            }
            String[] giocatore = (String[]) getItem(position);
            TextView txt = (TextView) v.findViewById(R.id.nome);
            txt.setText(giocatore[1]);
            txt = (TextView) v.findViewById(R.id.squadra);
            txt.setText(giocatore[2]);
            txt = (TextView) v.findViewById(R.id.ruolo);
            switch (giocatore[0]) {
                case "P": txt.setBackgroundColor(Color.rgb(255, 225, 15)); break;
                case "D": txt.setBackgroundColor(Color.rgb(0,128,10)); break;
                case "C": txt.setBackgroundColor(Color.rgb(5,29,192)); break;
                case "A": txt.setBackgroundColor(Color.RED); break;
            }
            txt.setText(giocatore[0]);

            txt = (TextView) v.findViewById(R.id.prezzo);
            txt.setText(giocatore[4]);
            //txt = (TextView) v.findViewById(R.id.quot);
            //txt.setText(giocatore.get("Quot"));

            return v;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class Stats extends AsyncTask<String[], String, String[]> {
        Dialog d;
        Bitmap b;

        @Override
        protected void onPreExecute() {
            d = new Dialog(getActivity(), R.style.CustomDialog);
            d.setContentView(R.layout.statistiche);
        }

        @Override
        protected String[] doInBackground (String[]... param) {
            try {
                String [] giocatore = param[0];
                publishProgress(giocatore[1]);
                String [] nomisquadre = getResources().getStringArray(R.array.squadre);

                String squadra = "";
                int j = 0;
                boolean found = false;
                while (!found) {
                    if (nomisquadre[j].startsWith(giocatore[2])) {
                        squadra = nomisquadre[j];
                        found = true;
                    }
                    j++;
                }

                String nome = giocatore[1].replaceAll(" ", "-");
                Document doc = HttpRequest.GET_nolega("https://www.fantacalcio.it/squadre/" + squadra + "/" + nome + "/" + giocatore[3], "<!-- FINE CONTAINER PRIMO BLOCCO CONTENUTO  SU DUE COLONNE -->");

                Elements el_stats = doc.select("div[id=fantastatistiche]");

                Elements num = el_stats.select("div[class=row no-gutter]").get(1).children();

                String [] stats = new String[num.size()];
                for (int i = 0; i < num.size() - 1; i++) {
                    if (i == 1) {
                        int gol = Integer.parseInt(num.get(i).select("p[class=nbig]").text());
                        int rigori = Integer.parseInt(num.get(5).select("p[class=nbig]").text().split("su")[0]);
                        stats[i] = (giocatore[0].equals("P") ? gol : gol + rigori) +
                                (rigori == 0 ? "" : "(" + rigori + ")");
                    } else if (i == 4) {
                        int assist = Integer.parseInt(num.get(i).select("p[class=nbig]").text().split(" ")[0]);
                        int assistf = Integer.parseInt(num.get(i).select("p[class=nbig]").text().split(" ")[2]);
                        stats[i] = (assist + assistf) + (assistf == 0 ? "" : "(" + assistf + ")");
                    } else {
                        stats[i] = num.get(i).select("p[class=nbig]").text();
                    }
                }

                stats[num.size() - 1] = el_stats.select("div[id=chart1div]").select("p[class=nbig2]").get(0).text();

                try {
                    URL url = new URL("http://d22uzg7kr35tkk.cloudfront.net/web/campioncini/small/" + nome + ".png");
                    b = BitmapFactory.decodeStream(url.openStream());
                } catch (IOException e) {
                    b = BitmapFactory.decodeResource(Objects.requireNonNull(getActivity()).getResources(), R.drawable.no_campioncino);
                }

                return stats;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            d.setTitle(values[0]);
            d.show();
        }

        @Override
        protected void onPostExecute (String[] result) {
            ((ImageView) d.findViewById(R.id.campioncino)).setImageBitmap(b);
            String text = "Media Voto: " + result[5];
            ((TextView) d.findViewById(R.id.media)).setText(text);
            text = "Presenze: " + result[0];
            ((TextView) d.findViewById(R.id.pres)).setText(text);
            text = "Gol fatti/subiti: " + result[1];
            ((TextView) d.findViewById(R.id.gol)).setText(text);
            text = "Ammonizioni: " + result[2];
            ((TextView) d.findViewById(R.id.amm)).setText(text);
            text = "Espulsioni: " + result[3];
            ((TextView) d.findViewById(R.id.esp)).setText(text);
            text = "Assist: " + result[4];
            ((TextView) d.findViewById(R.id.ass)).setText(text);
        }
    }
}
