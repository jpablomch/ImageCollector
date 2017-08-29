package edu.zika.imgproviderandroid;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.WriteMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadActivity extends AppCompatActivity {

    private static final String TAG = "UploadActivity";

    private static DbxRequestConfig dbConfig;
    private static DbxClientV2 dbClient;
    private static OkHttpClient okClient;
    private SharedPreferences prefs;
    private SharedPreferences.Editor prefsEdit;
    private ProgressBar progressBar;
    private String fileUri = null;
    private String partLabel = null;
    private String speciesLabel = null;
    private String methodUsed = null;
    private TextView uploadPercentage;
    private ImageView imgView;
    private Button uploadButton;
    private Button retakeButton;
    private Button aedesLabel;
    private Button anophelesLabel;
    private Button culexLabel;
    private Button unknownLabel;
    private Button headLabel;
    private Button tailLabel;
    private Button bodyLabel;
    private Button sideLabel;
    public boolean isUploaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        //Instantiate client for http requests
        okClient = new OkHttpClient();

        //Instantiate client for dropbox requests
        dbConfig = new DbxRequestConfig("ImgProviderAndroid");
        dbClient = new DbxClientV2(dbConfig, Utility.DROPBOX_API_KEY);

        //SharedPreferences Things
        prefs = getSharedPreferences("DelayedUris", 0);
        prefsEdit = prefs.edit();

        progressBar = (ProgressBar)findViewById(R.id.progress_bar);
        uploadPercentage = (TextView)findViewById(R.id.upload_percentage);
        imgView = (ImageView)findViewById(R.id.img_preview);
        uploadButton = (Button)findViewById(R.id.upload_button);
//        uploadButton.setText(BuildConfig.config == 1 ? R.string.server_upload_button : R.string.dropbox_upload_button);
        retakeButton = (Button)findViewById(R.id.retake_button);
        aedesLabel = (Button)findViewById(R.id.aedes_label);
        anophelesLabel = (Button)findViewById(R.id.anopheles_label);
        culexLabel = (Button)findViewById(R.id.culex_label);
        unknownLabel = (Button)findViewById(R.id.unknown_label);
        headLabel = (Button)findViewById(R.id.head_label);
        tailLabel = (Button)findViewById(R.id.tail_label);
        bodyLabel = (Button)findViewById(R.id.whole_body_label);
        sideLabel = (Button)findViewById(R.id.side_view_label);
        isUploaded = false;

        //Gets fileUri from Main
        fileUri =  getIntent().getStringExtra("fileUri");
        methodUsed = getIntent().getStringExtra("location");

        if(fileUri != null){
            setImagePreview();
        } else {
            Toast.makeText(getApplicationContext(), "File path is missing", Toast.LENGTH_LONG).show();
        }

        //Change config in build.grade file to switch between uploading to Server/Dropbox
        uploadButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(Utility.isNetworkAvailable(getApplicationContext())) {
                    if(partLabel != null && speciesLabel != null) {
                        switch (BuildConfig.config) {
                            case 1:
                                UploadFileToServer();
                                break;
                            case 2:
                                UploadFileToDbx();
                                break;
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.no_label, Toast.LENGTH_SHORT).show();
                    }
                } else if(!Utility.isNetworkAvailable(getApplicationContext())) {
                    if(partLabel != null && speciesLabel != null) {
                        prefsEdit.putString(fileUri, fileUri + "/" + speciesLabel + "_" + partLabel).apply();
                        showAlert("No Internet Connectivity\n\nFile will be uploaded when there is internet");
                    } else {
                        Toast.makeText(UploadActivity.this, R.string.no_label, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        retakeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                returnToCamera();
            }
        });
        bodyLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(tailLabel.isEnabled()) {
                    partLabel = "wholebody";
                    disableOtherLabels(v);
                } else {
                    partLabel = null;
                    enableOtherLabels(v);
                }
            }
        });
        headLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(tailLabel.isEnabled()) {
                    partLabel = "head";
                    disableOtherLabels(v);
                } else {
                    partLabel = null;
                    enableOtherLabels(v);
                }
            }
        });
        tailLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(headLabel.isEnabled()) {
                    partLabel = "tail";
                    disableOtherLabels(v);
                } else {
                    partLabel = null;
                    enableOtherLabels(v);
                }
            }
        });
        sideLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(headLabel.isEnabled()) {
                    partLabel = "sideview";
                    disableOtherLabels(v);
                } else {
                    partLabel = null;
                    enableOtherLabels(v);
                }
            }
        });
        aedesLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(unknownLabel.isEnabled()) {
                    speciesLabel = "aedes";
                    disableOtherLabels(v);
                } else {
                    speciesLabel = null;
                    enableOtherLabels(v);
                }
            }
        });
        anophelesLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(unknownLabel.isEnabled()) {
                    speciesLabel = "anopheles";
                    disableOtherLabels(v);
                } else {
                    speciesLabel = null;
                    enableOtherLabels(v);
                }
            }
        });
        culexLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(unknownLabel.isEnabled()) {
                    speciesLabel = "culex";
                    disableOtherLabels(v);
                } else {
                    speciesLabel = null;
                    enableOtherLabels(v);
                }
            }
        });
        unknownLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(aedesLabel.isEnabled()) {
                    speciesLabel = "unknown";
                    disableOtherLabels(v);
                } else {
                    speciesLabel = null;
                    enableOtherLabels(v);
                }
            }
        });
    }

    //Goes back to original activity
    @Override
    public void onBackPressed(){
        returnToCamera();
        super.onBackPressed();
    }

    @Override
    public void onDestroy(){
        if(!isUploaded && Utility.isNetworkAvailable(getApplicationContext())){
            new File(fileUri).delete();
        }
        super.onDestroy();
    }

    //Previews the image before being uploaded
    private void setImagePreview(){
        imgView.setVisibility(View.VISIBLE);
        BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeFile(fileUri, options);
        Bitmap rotatedBitmap = rotateBitmap(options, bitmap);
        imgView.setImageBitmap(rotatedBitmap);
    }

    //Helper method to upload file to dropbox
    public void UploadFileToDbx() {
        new AsyncTask<Void, Integer, String>() {

            @Override
            protected void onPreExecute(){
                uploadButton.setEnabled(false);
                retakeButton.setEnabled(false);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setIndeterminate(true);
            }

            @Override
            protected String doInBackground(Void... params) {
                File file = new File(fileUri);
                String response;
                String dbxFileName = file.getName();
                try {
                    InputStream inputStream = new FileInputStream(file);
                    dbClient.files()
                            .uploadBuilder("/MosquitoCollector/" + speciesLabel + "_" + partLabel + "/" + dbxFileName)
                            .withMode(WriteMode.OVERWRITE)
                            .uploadAndFinish(inputStream);
                    isUploaded = true;
                    response = "File uploaded to Cloud";
                    return response;
                } catch (DbxException | IOException e) {
                    isUploaded = false;
                    Log.d(TAG, e.toString());
                }
                response = getString(R.string.error_dialog);
                return response;
            }

            @Override
            protected void onPostExecute(String result){
                showAlert(result);
                progressBar.setVisibility(View.INVISIBLE);
                if(!isUploaded){
                    uploadButton.setEnabled(true);
                }
                retakeButton.setEnabled(true);
                super.onPostExecute(result);
            }
        }.execute();
    }

    //Helper method to upload the file to a server
    public void UploadFileToServer() {
        new AsyncTask<Void, Integer, String>() {

            //Makes sure that the user doesn't double click it while uploading
            @Override
            protected void onPreExecute(){
                uploadButton.setEnabled(false);
            }

            @Override
            protected void onProgressUpdate (Integer...progress){
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(progress[0]);
                uploadPercentage.setText(String.valueOf(progress[0]) + "%");
                uploadPercentage.setVisibility(View.VISIBLE);
            }

            @Override
            protected String doInBackground (Void...params){
                String response;
                File file = new File(fileUri);
                HttpUrl url = HttpUrl.parse(Utility.FILE_UPLOAD_URL);
                try {
                    MultipartBody body = RequestBuilder.uploadRequestBody(file.getName(), "jpg", file);
                    CountingRequestBody monitoredRequest = new CountingRequestBody(body, new CountingRequestBody.Listener() {
                        @Override
                        public void onRequestProgress(long bytesWritten, long contentLength) {
                            float percentage = 100f * bytesWritten / contentLength;
                            publishProgress((int)percentage);
                        }
                    });
                    response = ApiCall.POST(url, monitoredRequest);
                    isUploaded = true;
                    return response;
                } catch(IOException ioe){
                    isUploaded = false;
                    Log.d(TAG, ioe.getMessage());
                }
                response = getString(R.string.error_dialog);
                return response;
            }

            @Override
            protected void onPostExecute(String result) {
                showAlert(result);
                progressBar.setVisibility(View.INVISIBLE);
                uploadPercentage.setVisibility(View.INVISIBLE);
                if(!isUploaded){
                    uploadButton.setEnabled(true);
                }
                retakeButton.setEnabled(true);
                super.onPostExecute(result);
            }
        }.execute();
    }

    //Shows an alert after server call. Pressing OK will return to camera to take another picture
    private void showAlert(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String title = BuildConfig.config == 1 ? "Server Upload" : "Cloud Upload";
        builder.setMessage(msg)
                .setTitle(title)
                .setCancelable(true)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        returnToCamera();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    //Helper method to rotate bitmap
    private Bitmap rotateBitmap(BitmapFactory.Options options, Bitmap workingBmp){
        try {
            ExifInterface exif = new ExifInterface(fileUri);
            String orientString = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
            int orientation = orientString != null ? Integer.parseInt(orientString) : ExifInterface.ORIENTATION_NORMAL;
            int rotationAngle = 0;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotationAngle = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotationAngle = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotationAngle = 270;
                    break;
            }
            Matrix matrix = new Matrix();
            matrix.setRotate(rotationAngle, (float) workingBmp.getWidth() / 2, (float) workingBmp.getHeight() / 2);
            // (TODO: ljustin): This was causing a memory error. This is a quickfix. I will add a link to better solutions on Trello.
            matrix.setScale(0.5f, 0.5f);
            return Bitmap.createBitmap(workingBmp, 0, 0, options.outWidth, options.outHeight, matrix, true);
        } catch(IOException ioe){
            Log.d(TAG, ioe.toString());
        }
        return null;
    }

    //Helper method to return to main/camera
    private void returnToCamera() {
        if(!isUploaded && Utility.isNetworkAvailable(getApplicationContext())) {
            File file = new File(fileUri);
            file.delete();
        }
        Intent returnToCamera = new Intent(UploadActivity.this, CameraActivity.class);
        returnToCamera.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(returnToCamera);
    }

    //Helper method for disabling other labels when one is chosen
    private void disableOtherLabels(View v) {
        switch(v.getId()){
            case R.id.head_label :
                bodyLabel.setEnabled(false);
                sideLabel.setEnabled(false);
                tailLabel.setEnabled(false);
                break;
            case R.id.side_view_label :
                bodyLabel.setEnabled(false);
                tailLabel.setEnabled(false);
                headLabel.setEnabled(false);
                break;
            case R.id.tail_label :
                bodyLabel.setEnabled(false);
                sideLabel.setEnabled(false);
                headLabel.setEnabled(false);
                break;
            case R.id.whole_body_label :
                tailLabel.setEnabled(false);
                headLabel.setEnabled(false);
                sideLabel.setEnabled(false);
                break;
            case R.id.aedes_label :
                anophelesLabel.setEnabled(false);
                culexLabel.setEnabled(false);
                unknownLabel.setEnabled(false);
                break;
            case R.id.anopheles_label :
                aedesLabel.setEnabled(false);
                culexLabel.setEnabled(false);
                unknownLabel.setEnabled(false);
                break;
            case R.id.culex_label :
                aedesLabel.setEnabled(false);
                anophelesLabel.setEnabled(false);
                unknownLabel.setEnabled(false);
                break;
            case R.id.unknown_label :
                aedesLabel.setEnabled(false);
                anophelesLabel.setEnabled(false);
                culexLabel.setEnabled(false);
                break;
        }
    }

    //Helper method for reenabling the labels that were not chosen
    private void enableOtherLabels(View v){
        switch(v.getId()){
            case R.id.head_label :
                bodyLabel.setEnabled(true);
                sideLabel.setEnabled(true);
                tailLabel.setEnabled(true);
                break;
            case R.id.side_view_label :
                bodyLabel.setEnabled(true);
                tailLabel.setEnabled(true);
                headLabel.setEnabled(true);
                break;
            case R.id.tail_label :
                bodyLabel.setEnabled(true);
                sideLabel.setEnabled(true);
                headLabel.setEnabled(true);
                break;
            case R.id.whole_body_label :
                tailLabel.setEnabled(true);
                headLabel.setEnabled(true);
                sideLabel.setEnabled(true);
                break;
            case R.id.aedes_label :
                anophelesLabel.setEnabled(true);
                culexLabel.setEnabled(true);
                unknownLabel.setEnabled(true);
                break;
            case R.id.anopheles_label :
                aedesLabel.setEnabled(true);
                culexLabel.setEnabled(true);
                unknownLabel.setEnabled(true);
                break;
            case R.id.culex_label :
                aedesLabel.setEnabled(true);
                anophelesLabel.setEnabled(true);
                unknownLabel.setEnabled(true);
                break;
            case R.id.unknown_label :
                aedesLabel.setEnabled(true);
                anophelesLabel.setEnabled(true);
                culexLabel.setEnabled(true);
                break;
        }
    }

    //Class to take care of Api calls
    public static class ApiCall {
        //POST Call - Only calls to server for now
        static String POST(HttpUrl url, RequestBody body) throws IOException {
            Log.d(TAG, url.toString());
            Request req = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
            Response response = okClient.newCall(req).execute();
            return response.toString();
        }
    }

    //Class to take care of Request building
    public static class RequestBuilder {
        //Creates a multipart request of the image
        static MultipartBody uploadRequestBody(String title, String imageFormat, File file){
            MediaType mediaFormat = MediaType.parse("image/" + imageFormat);
            return new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file_name", title)
                    .addFormDataPart("file", title, RequestBody.create(mediaFormat, file))
                    .build();
        }
    }
}
