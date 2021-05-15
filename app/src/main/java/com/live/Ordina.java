package com.live;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

public class Ordina extends AppCompatTextView {

    Context context;
    private boolean enabled = false;
    private boolean verso = true;
    private Drawable disabilitato;
    private Drawable up;
    private Drawable down;
    @SuppressLint("StaticFieldLeak")
    private static StatsAdapter adapter;
    private final String[] campi = new String[] {"Calciatore", "Pg", "Mv", "Mf", "G", "Ass", "Amm", "Esp"};

    public Ordina(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        behaviour();
    }

    public Ordina(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        behaviour();
    }

    private void behaviour() {
        int height = 15 * (int) context.getResources().getDisplayMetrics().density;
        int width = 10 * (int) context.getResources().getDisplayMetrics().density;
        disabilitato = ContextCompat.getDrawable(context, R.drawable.disabilitato);
        if(disabilitato != null)
            disabilitato.setBounds(0, 0, width, height);

        up = ContextCompat.getDrawable(context, R.drawable.up);
        if(up != null)
            up.setBounds(0, 0, width, height);

        down = ContextCompat.getDrawable(context, R.drawable.down);
        if(down != null)
            down.setBounds(0, 0, width, height);

        setCompoundDrawables(null, null, disabilitato, null);
        setOnClickListener(view -> {
            if (enabled) {
                if (verso) {
                    verso = false;
                    setCompoundDrawables(null, null, up, null);
                } else {
                    verso = true;
                    setCompoundDrawables(null, null, down, null);
                }
            } else {
                LinearLayout l = ((LinearLayout) view.getParent());
                for (int i = 1; i < l.getChildCount(); i++) {
                    if (((Ordina) l.getChildAt(i)).getStato()) {
                        ((Ordina) l.getChildAt(i)).disabilita();
                    }
                }

                enabled = true;
                verso = true;
                setCompoundDrawables(null, null, down, null);
            }
            adapter.sort(campi[((LinearLayout) view.getParent()).indexOfChild(view)], verso);
        });
    }

    static void setAdapter(StatsAdapter a) {
        adapter = a;
    }

    void abilita() {
        LinearLayout l = ((LinearLayout) getParent());
        for (int i = 1; i < l.getChildCount(); i++) {
            if (((Ordina) l.getChildAt(i)).getStato()) {
                ((Ordina) l.getChildAt(i)).disabilita();
            }
        }

        enabled = true;
        verso = true;
        setCompoundDrawables(null, null, down, null);
    }

    void disabilita() {
        enabled = false;
        setCompoundDrawables(null, null, disabilitato, null);
    }

    boolean getStato() {
        return enabled;
    }
}
