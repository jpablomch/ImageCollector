package edu.zika.imgproviderandroid;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

//Class for different things that the app might use
public class Utility {

    //Directory name for images captures
    public static final String IMAGE_DIRECTORY_NAME = "ImgProviderAndroid";

    //URL needed to access local WAMP server
    public static final String FILE_UPLOAD_URL = "http://192.168.1.9/ImgProviderAndroid/fileUpload.php";

    //Dropbox API Key
    public static final String DROPBOX_API_KEY = BuildConfig.dropboxAPI_KEY;

    //Checks internet connection
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
