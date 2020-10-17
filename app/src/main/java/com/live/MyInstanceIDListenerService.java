package com.live;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MyInstanceIDListenerService extends FirebaseInstanceIdService {

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. This call is initiated by the
     * InstanceID provider.
     */

    private static final String TAG = "RegIntentService";

    // [START refresh_token]
    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);
        //Implement this method to send any registration to your app's servers.
        sendRegistrationToServer(refreshedToken);

        // You should store a boolean that indicates whether the generated token has been
        // sent to your server. If the boolean is false, send the token to your server,
        // otherwise your server should have already received the token.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putBoolean("sentTokenToServer", true).apply();
        sharedPreferences.edit().putInt("notifiche", R.id.disattivate).apply();
    }
    // [END refresh_token]

    /**
     * Persist registration to third-party servers.
     *
     * Modify this method to associate the user's GCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    static public void sendRegistrationToServer(String token) {
        // Add custom implementation, as needed.
        try {
            URL download = new URL("https://content.dropboxapi.com/2/files/download");
            HttpURLConnection downconn = (HttpURLConnection) download.openConnection();

            downconn.setDoInput(true);
            downconn.setRequestMethod("POST");
            downconn.setRequestProperty("Content-Type", "");
            downconn.setRequestProperty("Authorization", "Bearer 1MIfTuXqWL0AAAAAAAABIGYxo8M56LIBmH9Cej9H8ywqUoYYOa4CtwUvNTqGhK-n");
            downconn.setRequestProperty("Dropbox-API-Arg", "{\"path\": \"/gcm.txt\"}");

            String gcm = (new BufferedReader(new InputStreamReader(downconn.getInputStream()))).readLine();

            URL upload = new URL("https://content.dropboxapi.com/2/files/upload");
            HttpURLConnection upconn = (HttpURLConnection) upload.openConnection();

            upconn.setDoInput(true);
            upconn.setRequestMethod("POST");
            upconn.setRequestProperty("Content-Type", "application/octet-stream");
            upconn.setRequestProperty("Authorization", "Bearer 1MIfTuXqWL0AAAAAAAABIGYxo8M56LIBmH9Cej9H8ywqUoYYOa4CtwUvNTqGhK-n");
            upconn.setRequestProperty("Dropbox-API-Arg", "{\"path\": \"/gcm.txt\", \"mode\": \"overwrite\"}");

            byte[] postData = (gcm + token + "<_>").getBytes();
            DataOutputStream wr = new DataOutputStream(upconn.getOutputStream());
            wr.write(postData);

            String response = (new BufferedReader(new InputStreamReader(upconn.getInputStream()))).readLine();
            Log.v(TAG, response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}