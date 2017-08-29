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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.WriteMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/*
 * Activity : Alternate upload activity that uses spinners and has 3 label choices
 */
public class Upload2Activity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private static DbxRequestConfig dbConfig;
    private static DbxClientV2 dbClient;
    private SharedPreferences prefs;
    private ProgressBar mProgressBar;
    private Spinner mSpeciesSpinner;
    private Spinner mBodySpinner;
    private Spinner mStageSpinner;
    private String fileUri;
    private ImageView mImgView;
    private Button uploadButton;
    private Button retakeButton;
    private String partLabel = null;
    private String speciesLabel = null;
    private String stageLabel = null;
    private boolean isUploaded;

    @Override
    public void onCreate(Bundle savedInstanceState){
        setContentView(R.layout.activity_upload2);
        super.onCreate(savedInstanceState);

        //Instantiate client for dropbox requests
        dbConfig = new DbxRequestConfig("ImgProviderAndroid");
        dbClient = new DbxClientV2(dbConfig, Utility.DROPBOX_API_KEY);

        //SharedPreferences
        prefs = getSharedPreferences("DelayedUris", 0);

        mProgressBar = (ProgressBar)findViewById(R.id.progress_bar);
        mImgView = (ImageView)findViewById(R.id.img_preview);
        uploadButton = (Button)findViewById(R.id.upload_button);
        retakeButton = (Button)findViewById(R.id.retake_button);
        mBodySpinner = (Spinner)findViewById(R.id.body_spinner);
        mSpeciesSpinner = (Spinner)findViewById(R.id.species_spinner);
        mStageSpinner = (Spinner)findViewById(R.id.stage_spinner);
        final ArrayAdapter<CharSequence> bodyAdapter = ArrayAdapter.createFromResource(this, R.array.body_part, android.R.layout.simple_spinner_dropdown_item);
        ArrayAdapter<CharSequence> speciesAdapter = ArrayAdapter.createFromResource(this, R.array.species, android.R.layout.simple_spinner_dropdown_item);
        ArrayAdapter<CharSequence> stageAdapter = ArrayAdapter.createFromResource(this, R.array.stage, android.R.layout.simple_spinner_dropdown_item);
        mBodySpinner.setAdapter(bodyAdapter);
        mBodySpinner.setOnItemSelectedListener(this);
        mSpeciesSpinner.setAdapter(speciesAdapter);
        mSpeciesSpinner.setOnItemSelectedListener(this);
        mStageSpinner.setAdapter(stageAdapter);
        mStageSpinner.setOnItemSelectedListener(this);

        //Gets fileUri from Main
        fileUri =  getIntent().getStringExtra("fileUri");

        if(fileUri != null){
            setImagePreview();
        } else {
            Toast.makeText(getApplicationContext(), "File path is missing", Toast.LENGTH_LONG).show();
        }

        uploadButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(Utility.isNetworkAvailable(getApplicationContext())) {
                    if(!partLabel.equals("none") && !speciesLabel.equals("none") && !stageLabel.equals("none")) {
                        UploadFileToDbx();
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.no_label, Toast.LENGTH_SHORT).show();
                    }
                } else if(!Utility.isNetworkAvailable(getApplicationContext())) {
                    if(!partLabel.equals("none") && !speciesLabel.equals("none") && !stageLabel.equals("none")) {
                        prefs.edit().putString(fileUri, fileUri + "/" + speciesLabel + "_" + partLabel + "_" + stageLabel).apply();
                        showAlert("No Internet Connectivity\n\nFile will be uploaded when there is internet");
                    } else {
                        Toast.makeText(Upload2Activity.this, R.string.no_label, Toast.LENGTH_SHORT).show();
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
        mImgView.setVisibility(View.VISIBLE);
        BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeFile(fileUri, options);
        Bitmap rotatedBitmap = rotateBitmap(options, bitmap);
        mImgView.setImageBitmap(rotatedBitmap);
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
        } catch(IOException ioe){}
        return null;
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

    //Helper method to upload file to dropbox
    public void UploadFileToDbx() {
        new AsyncTask<Void, Integer, String>() {

            @Override
            protected void onPreExecute(){
                uploadButton.setEnabled(false);
                retakeButton.setEnabled(false);
                mProgressBar.setVisibility(View.VISIBLE);
                mProgressBar.setIndeterminate(true);
            }

            @Override
            protected String doInBackground(Void... params) {
                File file = new File(fileUri);
                String response;
                String dbxFileName = file.getName();
                try {
                    InputStream inputStream = new FileInputStream(file);
                    dbClient.files()
                            .uploadBuilder("/MosquitoCollector/" + speciesLabel + "_" + partLabel + "_" + stageLabel + "/" + dbxFileName)
                            .withMode(WriteMode.OVERWRITE)
                            .uploadAndFinish(inputStream);
                    isUploaded = true;
                    response = "File uploaded to Cloud";
                    return response;
                } catch (DbxException | IOException e) {
                    isUploaded = false;
                }
                response = getString(R.string.error_dialog);
                return response;
            }

            @Override
            protected void onPostExecute(String result){
                showAlert(result);
                mProgressBar.setVisibility(View.INVISIBLE);
                if(!isUploaded){
                    uploadButton.setEnabled(true);
                }
                retakeButton.setEnabled(true);
                super.onPostExecute(result);
            }
        }.execute();
    }

    //Helper method to return to main/camera
    private void returnToCamera() {
        if(!isUploaded && Utility.isNetworkAvailable(getApplicationContext())) {
            File file = new File(fileUri);
            file.delete();
        }
        Intent returnToCamera = new Intent(Upload2Activity.this, CameraActivity.class);
        returnToCamera.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(returnToCamera);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch(parent.getId()) {
            case R.id.body_spinner:
                partLabel = parent.getItemAtPosition(position).toString().toLowerCase().replaceAll("\\s*", "");
                break;
            case R.id.species_spinner:
                speciesLabel = parent.getItemAtPosition(position).toString().toLowerCase().replaceAll("\\s*", "");
                break;
            case R.id.stage_spinner:
                stageLabel = parent.getItemAtPosition(position).toString().toLowerCase().replaceAll("\\s*", "");
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}
}
