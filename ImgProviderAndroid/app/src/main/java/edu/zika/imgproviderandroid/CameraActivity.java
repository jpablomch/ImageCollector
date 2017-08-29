package edu.zika.imgproviderandroid;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CameraActivity extends AppCompatActivity {

    private Uri fileUri;
    private Button cameraButton;
    private Button galleryButton;
    private Button changeButton;
    private TextView userText;
    private String username;
    private SharedPreferences prefs;

    private static final String TAG = "CamActivity";
    private static final String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE};
    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
    private static final int GALLERY_PICK_REQUEST_CODE = 200;
    private static final int PERMS_REQUEST_CODE = 100;
    private static final int MEDIA_TYPE_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        cameraButton = (Button)findViewById(R.id.picture_button);
        galleryButton = (Button)findViewById(R.id.gallery_button);
        changeButton = (Button)findViewById(R.id.change_button);
        userText = (TextView)findViewById(R.id.current_user);
        prefs = getSharedPreferences("ImgProviderAndroid", 0);

        //Ask for permissions if Android version version 6.0 or higher
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(perms, PERMS_REQUEST_CODE);
        }

        //Checks if device has a camera
        if(!getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            Toast.makeText(getApplicationContext(),"Your device doesn't support camera", Toast.LENGTH_SHORT).show();
            finish();
        }

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                startActivityForResult(captureIntent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
            }
        });

        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, GALLERY_PICK_REQUEST_CODE);
            }
        });

        changeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent welcomeIntent = new Intent(CameraActivity.this, WelcomeActivity.class);
                welcomeIntent.putExtra("source", "camera");
                welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(welcomeIntent);
            }
        });

        if(prefs.contains("username") && prefs.getString("username", null) != null) {
            username = prefs.getString("username", "no user");
            userText.setText(getString(R.string.current_user) + "\n" + username);
        }
   }

    //Enable permissions after allowing
    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults){

        switch(permsRequestCode){
            case 100 :
                boolean writeExternalAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean internetAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                boolean netstateAccepted = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                boolean readExternalAccepted = grantResults[3] == PackageManager.PERMISSION_GRANTED;
                break;
        }

    }

    //Saves file uri so that it is not lost when the camera closes
    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putParcelable("file_uri", fileUri);
    }

    //Restores file uri after camera closes
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        fileUri = savedInstanceState.getParcelable("file_uri");
    }

    //Checks for result from camera
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        //handling each request depending on where it came from
        if(resultCode == RESULT_OK){
            switch(requestCode) {
                case CAMERA_CAPTURE_IMAGE_REQUEST_CODE :
                    Intent cameraUpload;
                    if(BuildConfig.upload_config == 1){
                        cameraUpload = new Intent(CameraActivity.this, UploadActivity.class);
                    } else if(BuildConfig.upload_config == 2){
                        cameraUpload = new Intent(CameraActivity.this, Upload2Activity.class);
                    }
                    cameraUpload.putExtra("fileUri", fileUri.getPath());
                    cameraUpload.putExtra("location", "cameraButton");
                    startActivity(cameraUpload);
                    break;
                case GALLERY_PICK_REQUEST_CODE :
                    fileUri = data.getData();
                    Intent galleryUpload;
                    if(BuildConfig.upload_config == 1){
                        galleryUpload = new Intent(CameraActivity.this, UploadActivity.class);
                    } else if(BuildConfig.upload_config == 2){
                        galleryUpload = new Intent(CameraActivity.this, Upload2Activity.class);
                    }
                    galleryUpload.putExtra("fileUri", getRealPathFromUri(fileUri));
                    galleryUpload.putExtra("location", "galleryButton");
                    startActivity(galleryUpload);
                    break;
            }
        } else if(resultCode == RESULT_CANCELED){
            finish();
        } else {
            switch (requestCode) {
                case CAMERA_CAPTURE_IMAGE_REQUEST_CODE:
                    Toast.makeText(getApplicationContext(), "Failed to capture image", Toast.LENGTH_SHORT).show();
                    break;
                case GALLERY_PICK_REQUEST_CODE:
                    Toast.makeText(getApplicationContext(), "Failed to pick an image", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    protected Uri getOutputMediaFileUri(int mediaType){
        return Uri.fromFile(getOutputMediaFile(mediaType));
    }

    private File getOutputMediaFile(int mediaType){
        //Finds location of saving image
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                Utility.IMAGE_DIRECTORY_NAME);

        //If the directory doesn't exist then make one
        if(!mediaStorageDir.exists()){
            if(!mediaStorageDir.mkdir()){
                Log.d(TAG, "Failed to create " + Utility.IMAGE_DIRECTORY_NAME + " directory");
                return null;
            }
        }

        //Create the media filename
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File mediaFile;
        if(mediaType == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + timeStamp + "_" + username + ".jpg");
        } else {
            return null;
        }
        return mediaFile;
    }

    public String getRealPathFromUri(Uri contentUri){
        Cursor c = null;
        try{
            String[] proj = {MediaStore.Images.Media.DATA};
            c = getApplicationContext().getContentResolver().query(contentUri, proj, null, null, null);
            int col_index = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            c.moveToFirst();
            return c.getString(col_index);
        } finally {
            if(c != null){
                c.close();
            }
        }
    }
}
