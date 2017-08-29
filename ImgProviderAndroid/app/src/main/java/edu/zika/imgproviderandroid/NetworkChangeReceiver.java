package edu.zika.imgproviderandroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

//Receiver to check whether internet connection returns
public class NetworkChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager conn =  (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conn.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            Intent serviceIntent = new Intent(context, DelayedUploadService.class);
            context.startService(serviceIntent);
        } else if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
            Intent serviceIntent = new Intent(context, DelayedUploadService.class);
            context.startService(serviceIntent);
        }
    }
}
