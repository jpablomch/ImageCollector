package edu.zika.imgproviderandroid;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.WriteMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class DelayedUploadService extends IntentService {

    private SharedPreferences prefs;
    private SharedPreferences.Editor prefsEditor;
    private DbxRequestConfig dbConfig;
    private DbxClientV2 dbClient;

    public DelayedUploadService() {
        super("DelayedUploadService");
        dbConfig = new DbxRequestConfig("ImgAndroidProvider");
        dbClient = new DbxClientV2(dbConfig, Utility.DROPBOX_API_KEY);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        prefs = getSharedPreferences("DelayedUris", 0);
        prefsEditor = prefs.edit();
        Map<String, ?> uris = prefs.getAll();
        for(Map.Entry<String, ?> entry : uris.entrySet()){
            String[] parts = entry.getValue().toString().split("/");
            String imageLabel = parts[parts.length-1];
            File file = new File(entry.getKey());
            if(file.exists()){
                String dbxFileName = file.getName();
                try{
                    InputStream inputStream = new FileInputStream(file);
                    dbClient.files()
                            .uploadBuilder("/MosquitoCollector/" + imageLabel + "/" + dbxFileName)
                            .withMode(WriteMode.OVERWRITE)
                            .uploadAndFinish(inputStream);
                    prefsEditor.remove(entry.getKey()).apply();
                } catch (DbxException | IOException e) {}
            } else {
                prefsEditor.remove(entry.getKey()).apply();
            }
        }
    }


}
