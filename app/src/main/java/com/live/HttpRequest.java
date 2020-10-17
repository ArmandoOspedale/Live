package com.live;

import android.content.Context;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpRequest {

    static String lega;
    static String cookie;

    /*public static Document POST (String request, String dati, String stop) throws IOException {
        URL url = new URL("https://leghe.fantacalcio.it/" + lega + request);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setDoOutput(true);

        byte[] postData = dati.getBytes(Charset.forName("UTF-8"));

        urlConnection.setRequestMethod("POST");
        urlConnection.setRequestProperty("Cookie", cookie);
        DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
        wr.write(postData);

        BufferedReader rd = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        String line;
        StringBuilder response = new StringBuilder();
        while ((line = rd.readLine()) != null && !(line.contains(stop))) {
            response.append(line);
        }
        rd.close();

        return Jsoup.parse(response.toString());
    }*/

    static Document GET(String request, String stop) throws IOException {
        URL url = new URL("https://leghe.fantacalcio.it/" + lega + request);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setDoOutput(true);

        urlConnection.setRequestProperty("Cookie", cookie);

        BufferedReader rd = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        String line;
        StringBuilder response = new StringBuilder();
        if (stop.equals("")) {
            while ((line = rd.readLine()) != null) {
                response.append(line);
            }
        } else {
            while ((line = rd.readLine()) != null && !(line.contains(stop))) {
                response.append(line);
            }
        }
        rd.close();

        return Jsoup.parse(response.toString());
    }

    static Document GET_nolega (String path, String stop) throws IOException {
        URL url = new URL(path);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        BufferedReader rd = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        String line;
        StringBuilder response = new StringBuilder();
        if (stop.equals("")) {
            while ((line = rd.readLine()) != null) {
                response.append(line);
            }
        } else {
            while ((line = rd.readLine()) != null && !(line.contains(stop))) {
                response.append(line);
            }
        }
        rd.close();
        String page = response.toString();

        return Jsoup.parse(page);
    }

    static String GET_nodocument (String path) throws IOException {
        URL url = new URL(path);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        BufferedReader rd = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        String line;
        StringBuilder response = new StringBuilder();
        while ((line = rd.readLine()) != null) {
            response.append(line);
        }
        rd.close();

        return response.toString();
    }

    static void serialize(Context context, Object object, String nomefile) {

        FileOutputStream fos;
        ObjectOutputStream out;
        try {
            fos = context.openFileOutput(nomefile, Context.MODE_PRIVATE);
            out = new ObjectOutputStream(fos);
            out.writeObject(object);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static Object getObject(Context context, String nomefile) {

        Object object = null;
        FileInputStream fis;
        ObjectInputStream in;
        try {
            fis = context.openFileInput(nomefile);
            in = new ObjectInputStream(fis);
            object = in.readObject();
            in.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return object;
    }

    static String getNomeSquadra(String codice, String[] codici, String[] squadre) {
        if("-1".equals(codice))
            return "Riposa";

        String squadra = "";
        int k = 0;
        boolean found = false;
        while (!found) {
            if (codici[k].equals(codice)) {
                squadra = squadre[k];
                found = true;
            }
            k++;
        }
        return squadra;
    }
}
