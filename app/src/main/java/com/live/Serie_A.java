package com.live;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.StateListDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Serie_A extends AppCompatActivity {

    private List<HashMap<String, String>> classifica;
    private View calendario;
    private DownLoadCal downLoadCal;
    private ListView list;
    private boolean controllo = false;
    private int current;
    private String[] links;
    private String[][] giornate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("CLASSIFICA SERIE A");
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        new DownLoadClass().execute();
        downLoadCal = new DownLoadCal();
        downLoadCal.execute();
    }

    @SuppressLint("StaticFieldLeak")
    private class DownLoadClass extends AsyncTask<Void, Void, List<HashMap<String, String>>> {

        @Override
        protected List<HashMap<String, String>> doInBackground(Void... voids) {
            try {
                List<HashMap<String, String>> temp = new ArrayList<>();
                Document doc = HttpRequest.GET_nolega("http://www.legaseriea.it/it/serie-a-tim/classifica", "Ultime news su Serie A TIM");

                Elements rows = doc.select("div[id=classifiche]").select("tr");
                rows.remove(0);
                rows.remove(0);
                for (int i = 0; i < rows.size(); i++) {
                    HashMap<String, String> map = new HashMap<>();
                    map.put("Posizione", String.valueOf(i + 1));
                    map.put("Squadra", rows.get(i).child(0).select("img").get(0).attr("title"));
                    if (map.get("Squadra").equals("CHIEVOVERONA")) map.put("Squadra", "CHIEVO");
                    if (map.get("Squadra").equals("HELLAS VERONA")) map.put("Squadra", "VERONA");
                    map.put("Punti", rows.get(i).child(1).text());
                    map.put("Giocate", rows.get(i).child(2).text());
                    map.put("Vittorie", rows.get(i).child(3).text());
                    map.put("Pareggi", rows.get(i).child(4).text());
                    map.put("Sconfitte", rows.get(i).child(5).text());
                    map.put("GolFatti", rows.get(i).child(14).text());
                    map.put("GolSubiti", rows.get(i).child(15).text());

                    temp.add(map);
                }

                return temp;
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<HashMap<String, String>> result) {
            list = new ListView(Serie_A.this);
            list.setVerticalScrollBarEnabled(false);
            list.setSelector(new StateListDrawable());

            mAdapter adapter = new mAdapter(Serie_A.this, result, R.layout.class_serie_a_list, new String[]{"Posizione", "Squadra", "Giocate", "Punti"}, new int[]{R.id.Posizione, R.id.NomeSquadra, R.id.Giocate, R.id.Punti});
            list.setAdapter(adapter);

            classifica = result;
            if (!controllo) {
                invalidateOptionsMenu();
                setContentView(list);
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class DownLoadCal extends AsyncTask<Void, Void, String[]> {

        @Override
        protected String[] doInBackground(Void... voids) {
            try {
                /*List<String[]> andata = new ArrayList<>();
                List<String[]> ritorno = new ArrayList<>();
                Document doc = HttpRequest.GET_nolega("http://www.corrieredellosport.it/live/calendario-serie-a.html", "");

                Elements giornate = doc.select("table[class=main-section]");

                for (int i = 0; i < giornate.size(); i++) {
                    Elements partite = giornate.get(i).select("tr");

                    String[] temp1 = new String[partite.size() - 2];
                    String[] temp2 = new String[partite.size() - 2];
                    for (int j = 2; j < partite.size(); j++) {
                        String partita = partite.get(j).select("td[class=a-center]").text();
                        String ris_andata =  partite.get(j).select("td[class=a-center small]").get(0).text();
                        String data_andata = partite.get(j).select("td[class=a-center small]").get(0).select("strong").hasAttr("title")
                                ? partite.get(j).select("td[class=a-center small]").get(0).select("strong").attr("title")
                                : partite.get(j).select("td[class=a-center small]").get(0).select("strong").select("a").attr("title");
                        if (data_andata.equals("")) data_andata = partite.get(1).select("th[class=a-center]").get(0).attr("title");
                        String ris_ritorno =  partite.get(j).select("td[class=a-center small]").get(1).text();
                        String data_ritorno = partite.get(j).select("td[class=a-center small]").get(1).select("strong").hasAttr("title")
                                ? partite.get(j).select("td[class=a-center small]").get(1).select("strong").attr("title")
                                : partite.get(j).select("td[class=a-center small]").get(1).select("strong").select("a").attr("title");

                        temp1[j - 2] = partita + "<>" + ris_andata + "<>" + data_andata;
                        temp2[j - 2] = partita.split(" - ")[1] + " - " + partita.split(" - ")[0] + "<>" +
                                (ris_ritorno.equals("-") ? ris_ritorno : ris_ritorno.split(" - ")[1] + " - " + ris_ritorno.split(" - ")[0]) + "<>" + data_ritorno;

                        if (current_andata == -1) {
                            if (ris_andata.equals("-")) {
                                current_andata = i;
                            }
                        }

                        if (current_ritorno == -1) {
                            if (ris_ritorno.equals("-")) {
                                current_ritorno = i + giornate.size();
                            }
                        }
                    }

                    andata.add(temp1);
                    ritorno.add(temp2);
                }

                andata.addAll(ritorno);
                return andata;*/

                Document doc = HttpRequest.GET_nolega("http://www.legaseriea.it/it/serie-a-tim/calendario-e-risultati", "Ultime news su Serie A TIM");

                Elements g = doc.select("div[id=menu-giornate]").select("li");
                g.remove(20);
                g.remove(0);
                giornate = new String[g.size()][];
                links = new String[g.size()];
                for (int i = 0; i < g.size(); i++) {
                    links[i] = "http://www.legaseriea.it" + g.get(i).select("a").get(0).attr("href");
                    if (g.get(i).select("a").get(0).attr("class").equals("active")) {
                        current = i;
                    }
                }

                Elements partite = doc.select("section[class=risultati]").get(0).children();
                partite.remove(0);
                partite.remove(0);
                partite.remove(0);
                partite.remove(0);

                String[] giornata = new String[partite.size()];

                final String verona = "Hellas Verona";
                for (int i = 0; i < partite.size(); i++) {
                    Element partita = partite.get(i);
                    StringBuilder map = new StringBuilder();
                    String temp = partita.select("h4[class=nomesquadra]").get(0).text();
                    map.append(temp.equals(verona) ? "Verona" : temp);
                    map.append("<_>");
                    temp = partita.select("h4[class=nomesquadra]").get(1).text();
                    map.append(temp.equals(verona) ? "Verona" : temp);
                    map.append("<_>");
                    temp = partita.select("div[class=col-xs-6 risultatosx]").select("span").text();
                    if (temp.equals("-")) {
                        map.append(temp);
                        map.append("<_>");
                    }
                    else {
                        map.append(temp);
                        map.append(" - ");
                        map.append(partita.select("div[class=col-xs-6 risultatodx]").select("span").text());
                        map.append("<_>");
                    }
                    map.append(partita.select("div[class=datipartita]").select("span").get(0).text());

                    giornata[i] = map.toString();
                }

                return giornata;
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        @SuppressWarnings("all")
        protected void onPostExecute(String[] result) {
            calendario = getLayoutInflater().inflate(R.layout.cal_serie_a, null);
            ViewPager mPager = (ViewPager) calendario.findViewById(R.id.pager);
            PagerAdapter mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager(), result);
            mPager.setAdapter(mPagerAdapter);

            ((PagerTabStrip) calendario.findViewById(R.id.pager_tab_strip)).setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            mPager.setCurrentItem(current);

            if (controllo) {
                setTitle("CALENDARIO SERIE A");
                controllo = true;
                invalidateOptionsMenu();
                setContentView(calendario);
            }
        }
    }

    void update(int position, String[] array) {
        giornate[position] = array;
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        ScreenSlidePagerAdapter(FragmentManager fm, String[] c) {
            super(fm);
            giornate[current] = c;
        }

        @Override
        public Fragment getItem(int position) {
            GiornataFragment giornata = new GiornataFragment();
            Bundle b = new Bundle();
            b.putStringArray("giornata", giornate[position]);
            b.putString("link", links[position]);
            b.putInt("position", position);
            giornata.setArguments(b);
            return giornata;
        }

        @Override
        public int getCount() {
            return links.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return ("GIORNATA " + (position + 1));
        }
    }

    private class mAdapter extends SimpleAdapter {

        String [] squadre;

        mAdapter (Context c, List<HashMap<String, String>> d, int r, String[] f, int[] i) {
            super(c, d, r, f, i);
            squadre = new String[d.size()];
            for (int j = 0; j < d.size(); j++) {
                squadre[j] = d.get(j).get("Squadra");
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);

            ImageView im = v.findViewById(R.id.icona);
            im.setImageResource(getDrawable(Serie_A.this, squadre[position].toLowerCase().replaceAll(" ", "_")));

            return v;
        }
    }

    private int getDrawable(Context context, String name) {
        return context.getResources().getIdentifier(name,
                "drawable", context.getPackageName());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (!controllo) {
            mAdapter adapter;
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                adapter = new mAdapter(Serie_A.this, classifica, R.layout.land_serie_a_list,
                        new String[]{"Posizione", "Squadra", "Giocate", "Punti", "Vittorie", "Pareggi", "Sconfitte", "GolFatti", "GolSubiti"},
                        new int[]{R.id.Posizione, R.id.NomeSquadra, R.id.Giocate, R.id.Punti, R.id.Vinte, R.id.Pareggi, R.id.Perse, R.id.Golfatti, R.id.Golsubiti});
            } else {
                adapter = new mAdapter(Serie_A.this, classifica, R.layout.class_serie_a_list, new String[]{"Posizione", "Squadra", "Giocate", "Punti"}, new int[]{R.id.Posizione, R.id.NomeSquadra, R.id.Giocate, R.id.Punti});
            }
            list.setAdapter(adapter);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_seriea, menu);
        if (controllo) {
            menu.findItem(R.id.action_class).setVisible(true);
            menu.findItem(R.id.action_cal).setVisible(false);
        } else {
            menu.findItem(R.id.action_class).setVisible(false);
            menu.findItem(R.id.action_cal).setVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (id == R.id.action_class) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mAdapter adapter = new mAdapter(Serie_A.this, classifica, R.layout.land_serie_a_list,
                        new String[]{"Posizione", "Squadra", "Giocate", "Punti", "Vittorie", "Pareggi", "Sconfitte", "GolFatti", "GolSubiti"},
                        new int[]{R.id.Posizione, R.id.NomeSquadra, R.id.Giocate, R.id.Punti, R.id.Vinte, R.id.Pareggi, R.id.Perse, R.id.Golfatti, R.id.Golsubiti});
                list.setAdapter(adapter);

                setTitle("CLASSIFICA SERIE A");
                controllo = false;
                invalidateOptionsMenu();
                setContentView(list);
            } else {
                mAdapter adapter = new mAdapter(Serie_A.this, classifica, R.layout.class_serie_a_list, new String[]{"Posizione", "Squadra", "Giocate", "Punti"}, new int[]{R.id.Posizione, R.id.NomeSquadra, R.id.Giocate, R.id.Punti});
                list.setAdapter(adapter);

                setTitle("CLASSIFICA SERIE A");
                controllo = false;
                invalidateOptionsMenu();
                setContentView(list);
            }
        }

        if (id == R.id.action_cal) {
            if (downLoadCal.getStatus() == AsyncTask.Status.RUNNING) {
                setTitle("CALENDARIO SERIE A");
                controllo = true;
                TextView txt = new TextView(this);
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                txt.setLayoutParams(params);
                txt.setPadding(0, 50, 0, 0);
                txt.setGravity(Gravity.CENTER);
                txt.setTextSize(20);
                txt.setText(R.string.caricamento);
                setContentView(txt);
            } else {
                setTitle("CALENDARIO SERIE A");
                controllo = true;
                invalidateOptionsMenu();
                setContentView(calendario);
            }
        }

        return super.onOptionsItemSelected(item);
    }
}
