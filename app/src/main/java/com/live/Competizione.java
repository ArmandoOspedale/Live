package com.live;

import java.io.Serializable;

class Competizione implements Serializable {

    String nome;
    String codice;
    int tipo;
    String[][] codici;
    String defaultcode;

    Competizione (String n, String c, int t, String[][] s, String d) {
        nome = n;
        codice = c;
        tipo = t;
        codici = s;
        defaultcode = d;
    }
}
