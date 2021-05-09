package com.live;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MyFcmListenerService extends FirebaseMessagingService {

    private static final String TAG = "MyFcmListenerService";
    private final static String GROUP_KEY_UPS = "group_key_ups";

    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage message) {
        String from = message.getFrom();
        Map<String, String> data = message.getData();
        Log.d(TAG, "From: " + from);

        if (from.startsWith("/topics/")) {
            // message received from some topic.
        } else {
            // normal downstream message.
        }

        if (data.containsKey("giornatacalcolata")) {
            sendNotification(data.get("giornatacalcolata"));
        } else {
            String marcatore;
            if (data.containsKey("gol_fatto")) {
                marcatore = "Gol segnato: " + data.get("gol_fatto");
            } else {
                marcatore = "Rigore segnato: " + data.get("rigore_segnato");
            }
            String portiere = "Gol subito: " + data.get("gol_subito");
            String assistman = "";
            if (data.containsKey("assist")) {
                assistman = "Assist: " + data.get("assist");
            }

            // [START_EXCLUDE]
            /*
             * Production applications would usually process the message here.
             * Eg: - Syncing with server.
             *     - Store message in local database.
             *     - Update UI.
             */

            /*
             * In some cases it may be useful to show a notification indicating to the user
             * that a message was received.
             */
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            int modalita = sharedPreferences.getInt("notifiche", -1);
            List<String> messages = new ArrayList<>();
            if (modalita > 0) {
                switch (modalita) {
                    case R.id.disattivate:
                        break;
                    case R.id.tutte:
                        messages.add(marcatore);
                        if (!assistman.equals("")) {
                            messages.add(assistman);
                        }
                        messages.add(portiere);
                        messages.add("Risultato: " + data.get("risultato"));
                        sendNotification("AGGIORNAMENTO GENERALE", messages);
                    case R.id.miapartita:
                        /*if (sharedPreferences.getBoolean("avv", false)) {
                            messages = new ArrayList<>();
                            String[] formazione = (String[]) HttpRequest.getObject(this, "avv");
                            for (String g : formazione) {
                                if (marcatore.contains(g)) messages.add(marcatore);
                                if (!assistman.equals("")) {
                                    if (assistman.contains(g)) messages.add(assistman);
                                }
                            }
                            if (portiere.contains(formazione[0])) messages.add(portiere);
                            if (messages.size() > 0) {
                                messages.add("Risultato: " + data.getString("risultato"));
                                sendNotification("AGGIORNAMENTO CONTRO", messages);
                            }
                        }*/
                    case R.id.miasquadra:
                        /*if (sharedPreferences.getBoolean("mia", false)) {
                            messages = new ArrayList<>();
                            String[] formazione = (String[]) HttpRequest.getObject(this, "mia");
                            for (String g : formazione) {
                                if (marcatore.contains(g)) messages.add(marcatore);
                                if (!assistman.equals("")) {
                                    if (assistman.contains(g)) messages.add(assistman);
                                }
                            }
                            if (portiere.contains(formazione[0])) messages.add(portiere);
                            if (messages.size() > 0) {
                                messages.add("Risultato: " + data.getString("risultato"));
                                sendNotification("AGGIORNAMENTO PRO", messages);
                            }
                        }*/
                        break;
                }
            }
        }
        // [END_EXCLUDE]
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     *
     */
    private void sendNotification(String header, List<String> messages) {
        int requestCode = (int) System.currentTimeMillis();

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("live", true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, requestCode, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        notificationBuilder.setSmallIcon(R.drawable.notifica);
        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
        boolean[] icona = new boolean[]{false, false, false, false};
        for (int i=0; i<messages.size()-1; i++) {
            style.addLine(messages.get(i));
            if (messages.get(i).contains("Gol segnato")) icona[0] = true;
            if (messages.get(i).contains("Rigore segnato")) icona[1] = true;
            if (messages.get(i).contains("Assist")) icona[2] = true;
            if (messages.get(i).contains("Gol subito")) icona[3] = true;
        }
        if (icona[0]) {
            notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.golfatto_s));
        } else {
            if (icona[1]) {
                notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.rigoresegnato_s));
            } else {
                if (icona[2]) {
                    notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.assist_s));
                } else if (icona[3]) {
                    notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.golsubito_s));
                }
            }
        }
        style.setBigContentTitle(header).setSummaryText(messages.get(messages.size()-1));
        notificationBuilder
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setStyle(style)
                .setGroup(GROUP_KEY_UPS)
                .setGroupSummary(true);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(requestCode, notificationBuilder.build());
    }

    private void sendNotification(String message) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("live", false);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        notificationBuilder
                .setSmallIcon(R.drawable.notifica)
                .setContentTitle(message)
                .setContentText("Controlla i risultati")
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0, notificationBuilder.build());
    }
}