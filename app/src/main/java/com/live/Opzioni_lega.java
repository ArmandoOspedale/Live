package com.live;

import java.io.Serializable;

class Opzioni_lega implements Serializable {

    boolean admin;
    String bonus;
    int numsost;
    boolean ammsv_checked;
    double ammsv_value;
    double base;
    double fascia;
    boolean intorno_checked;
    double intorno_value;
    boolean pareggio_checked;
    double pareggio_value;
    boolean differenza_checked;
    double differenza_value;
    boolean mod_checked;
    double mod_min;
    double mod_max;
    double [] mod;
    Competizione[] competizioni;
    String[][] squadre;
    int[] calciatori_per_ruolo = new int[4];
    int crediti;
    String id_utente;
}

