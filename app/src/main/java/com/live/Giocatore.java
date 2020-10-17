package com.live;

import org.json.JSONException;
import org.json.JSONObject;

class Giocatore {

    private String fantasquadra;
    private String nome;
    private char ruolo;
    private String squad;
    private double voto;
    private String status;
    private String[] bonus;
    private double fantavoto;
    private String stringaVoto;
    private String stringaVotoReale;
    static JSONObject opzioni;
    static boolean ammsv_checked;
    static double ammsv_value;

    Giocatore(String f, String n, char r, String s, double v, String[] b, String sta) {
        fantasquadra = f;
        nome = n;
        ruolo = r;
        squad = s;
        voto = v;
        status = sta;
        bonus = b;
        this.calcolaFanta();
    }

    Giocatore(String n, char r, String s, String v, String vr, String[] b) {
        nome = n;
        ruolo = r;
        squad = s;
        stringaVoto = v;
        stringaVotoReale = vr;
        bonus = b;
    }

    Giocatore(String s) {fantasquadra = s;}

    double getVoto() {return fantavoto;}

    String getStringaVoto() {return stringaVoto;}

    void setStringaVoto(String v) {stringaVoto = v;}

    String getStringaVotoReale() {return stringaVotoReale;}

    public char getRuolo() {return ruolo;}

    public String getNome() {return nome;}

    String getFanta() {return fantasquadra;}

    double getVotoReale() {return voto;}

    String[] getBonus() {return bonus;}

    String getSquad() {return squad;}

    void setStato(String s) {status = s;}

    String getStato() {return status;}

    private void calcolaFanta() {
        try {
            fantavoto = voto;
            int select = -1;
            if (bonus.length > 0) {
                switch (ruolo) {
                    case 'P':
                        select = 0;
                        break;
                    case 'D':
                        select = 1;
                        break;
                    case 'C':
                        select = 2;
                        break;
                    case 'A':
                        select = 3;
                        break;
                }
                for (String b : bonus) {
                    switch (b) {
                        case "golsubito_s":
                            fantavoto = fantavoto + Double.parseDouble(opzioni.getJSONArray("gol_subito").getString(select));
                            break;
                        case "golfatto_s":
                            fantavoto = fantavoto + Double.parseDouble(opzioni.getJSONArray("gol_segnato").getString(select));
                            break;
                        case "assist_s":
                        case "assistmovimentolvbasso_s":
                        case "assistmovimentolvmedio_s":
                        case "assistmovimentolvalto_s":
                            fantavoto = fantavoto + Double.parseDouble(opzioni.getJSONArray("assist").getString(select));
                            break;
                        case "rigoresegnato_s":
                            fantavoto = fantavoto + Double.parseDouble(opzioni.getJSONArray("rigore_segnato").getString(select));
                            break;
                        case "assistf_s":
                        case "assistfermolvbasso_s":
                        case "assistfermolvmedio_s":
                        case "assistfermolvalto_s":
                            fantavoto = fantavoto + Double.parseDouble(opzioni.getJSONArray("assist_fermo").getString(select));
                            break;
                        case "rigoreparato_s":
                            fantavoto = fantavoto + Double.parseDouble(opzioni.getJSONArray("rigore_parato").getString(select));
                            break;
                        case "rigoresbagliato_s":
                            fantavoto = fantavoto + Double.parseDouble(opzioni.getJSONArray("rigore_sbagliato").getString(select));
                            break;
                        case "amm":
                            if (voto != 0) {
                                fantavoto = fantavoto + Double.parseDouble(opzioni.getJSONArray("ammonizione").getString(select));
                            } else {
                                if (ammsv_checked) {
                                    fantavoto = ammsv_value;
                                } else {
                                    fantavoto = 0;
                                }
                            }
                            break;
                        case "esp_s":
                            fantavoto = fantavoto + Double.parseDouble(opzioni.getJSONArray("espulsione").getString(select));
                            status = "finale";
                            break;
                        case "autogol_s":
                            fantavoto = fantavoto + Double.parseDouble(opzioni.getJSONArray("autogol").getString(select));
                            break;
                        case "golvittoria_s":
                            fantavoto = fantavoto + Double.parseDouble(opzioni.getJSONArray("gol_decisivo_vittoria").getString(select));
                            break;
                        case "golpareggio_s":
                            fantavoto = fantavoto + Double.parseDouble(opzioni.getJSONArray("gol_decisivo_pareggio").getString(select));
                            break;
                        case "uscito_s":
                            status = "finale";
                            break;
                        case "portiereimbattuto_s":
                            fantavoto = fantavoto + opzioni.getDouble("portiere_imbattuto");
                            break;
                        default:
                            break;
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}

