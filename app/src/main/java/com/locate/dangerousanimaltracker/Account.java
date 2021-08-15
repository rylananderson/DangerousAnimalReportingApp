package com.locate.dangerousanimaltracker;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class Account extends AppCompatActivity {

    private TextView mEmail, mMapBtn;
    private static final String TAG = "Account";
    private static final int ERROR_DIALOG_REQUEST = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        // hooks
        mEmail = findViewById(R.id.emailLabel);

        // Gets email entered in
        Intent usr = getIntent();
        String user_email = usr.getStringExtra("email");
        mEmail.setText(user_email);


        if(isServicesOK()){
            init();
        }

    }
    //**********************************************************************************************************
    // Only runs if google services is working: allows user to switch to view map
    private void init(){
        mMapBtn = findViewById(R.id.mapBtn);
        mMapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Account.this, MapActivity.class);
                startActivity(intent);
            }
        });


    }
    //**********************************************************************************************************
    // Checks to make sure google services is working so we can load a map on the next screen
    public boolean isServicesOK(){
        Log.d(TAG, "isServicesOK: checking google services version");

        int avalible = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(Account.this);
        if(avalible == ConnectionResult.SUCCESS){
            // everything is fine, user can make map request
            Log.d(TAG, "isServicesOK: google play services is working");
            return true;

        }else if(GoogleApiAvailability.getInstance().isUserResolvableError(avalible)){
            // error occured but can resolve
            Log.d(TAG, "isServicesOK: There was an error but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(Account.this, avalible, ERROR_DIALOG_REQUEST);
            dialog.show();
        }else{
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        }

        return false;
    }


}