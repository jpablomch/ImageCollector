package edu.zika.imgproviderandroid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class WelcomeActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private Button mNextButton;
    private EditText mUserEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        prefs = getSharedPreferences("ImgProviderAndroid", MODE_PRIVATE);
        Intent intent = getIntent();
        final String source = intent.getStringExtra("source");
        final String username = prefs.getString("username", null);

        if(source.equals("landing") && username != null) {
            startCamera();
        }

        mNextButton = (Button)findViewById(R.id.next_button);
        mUserEdit = (EditText)findViewById(R.id.user_edit_text);

        if(username != null) {
            mUserEdit.setText(username);
            mNextButton.setEnabled(true);
        }

        mUserEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if(s.length() != 0) {
                    mNextButton.setEnabled(true);
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length() != 0){
                    mNextButton.setEnabled(true);
                } else {
                    mNextButton.setEnabled(false);
                }
            }
        });

        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.edit().putString("username", mUserEdit.getText().toString()).apply();
                if(prefs.getString("username", null) != null) {
                    startCamera();
                }
            }
        });

    }

    private void startCamera(){
        Intent recognitionIntent = new Intent(WelcomeActivity.this, CameraActivity.class);
        recognitionIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(recognitionIntent);
        finish();
    }

}
