package edu.zika.imgproviderandroid;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

public class LandingActivity extends AppCompatActivity {

    private Handler waitHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        waitHandler = new Handler();
        waitHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent welcomeIntent = new Intent(LandingActivity.this, WelcomeActivity.class);
                welcomeIntent.putExtra("source", "landing");
                welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(welcomeIntent);
                finish();
            }
        }, 2000);
    }
}
