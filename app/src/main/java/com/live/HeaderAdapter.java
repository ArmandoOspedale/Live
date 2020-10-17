package com.live;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.BaseAdapter;
import android.widget.TextView;

class HeaderAdapter extends BaseAdapter {

    private final Giocatore[] mdata;
    Context c;
    boolean live;

    HeaderAdapter(Context context, Giocatore[] g, boolean l) {
        c = context;
        mdata = g;
        live = l;
    }

    @Override
    public int getCount() {
        return mdata.length;
    }

    @Override
    public Object getItem(int i) {
        return mdata[i];
    }

    @Override
    public long getItemId(int i) {
        return mdata[i].hashCode();
    }

    @Override
    @SuppressWarnings("all")
    public View getView(int position, View view, ViewGroup parent) {
        if (view==null)
        {
            view = LayoutInflater.from(c).inflate(R.layout.mylist, null);
        }

        Giocatore g = mdata[position];
        if (position == 0) {
            view.findViewById(R.id.panchina).setVisibility(View.GONE);
            view.setBackgroundColor(Color.TRANSPARENT);
        }

        if (position == 11) {
            view.findViewById(R.id.panchina).setVisibility(View.VISIBLE);
        }
        else {
            view.findViewById(R.id.panchina).setVisibility(View.GONE);
        }

        TextView txt = (TextView) view.findViewById(R.id.Nome);
        txt.setText(g.getNome());

        txt = (TextView) view.findViewById(R.id.Voto);

        if (live) {
            if (g.getStato().equals("in campo")) {
                txt.setText(String.valueOf(g.getVoto()));
                txt.setTextColor(Color.RED);
                Animation anim = new AlphaAnimation(0.4f, 1.0f);
                anim.setDuration(800);
                anim.setRepeatMode(Animation.REVERSE);
                anim.setRepeatCount(Animation.INFINITE);
                txt.startAnimation(anim);
            } else if (g.getStato().equals("titolare")) {
                txt.setText(" - ");
                txt.setTextColor(Color.RED);
                txt.clearAnimation();
            } else {
                txt.setText(String.valueOf(g.getVoto()));
                txt.setTextColor(Color.rgb(51, 51, 51));
                txt.clearAnimation();
            }

            if (g.getStato().equals("")) {
                txt.setText(" - ");
            }
        } else {
            txt.setText(g.getStringaVoto());
        }

        txt = (TextView) view.findViewById(R.id.Ruolo);
        txt.setText(String.valueOf(g.getRuolo()));
        switch (g.getRuolo()) {
            case 'P': view.findViewById(R.id.Ruolo).setBackgroundColor(Color.rgb(255,225,15)); break;
            case 'D': view.findViewById(R.id.Ruolo).setBackgroundColor(Color.rgb(0,128,10)); break;
            case 'C': view.findViewById(R.id.Ruolo).setBackgroundColor(Color.rgb(5,29,192)); break;
            case 'A': view.findViewById(R.id.Ruolo).setBackgroundColor(Color.RED); break;
        }

        return view;
    }
}
