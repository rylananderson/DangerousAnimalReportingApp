package com.locate.dangerousanimaltracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.security.MessageDigest;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    // Variables
    private EditText mEmail, mPassword;
    private Button mSignUp, mLogin, mViewmap;

    // Connecting to Database
    private FirebaseDatabase db = FirebaseDatabase.getInstance();
    private DatabaseReference root = db.getReference().child("Users");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // hooks
        mEmail = findViewById(R.id.email);
        mPassword = findViewById(R.id.password);
        mSignUp = findViewById(R.id.signUp);
        mLogin = findViewById(R.id.logInScreen);
        mViewmap = findViewById(R.id.viewBtn);

        // On Click event for sign Up
        mSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // First Validate the form and check entries with db
                boolean valid = validateForm();

                if(valid)
                {
                    checkEmailAndRun();

                }
                else
                {
                    Toast.makeText(MainActivity.this, "Fields are empty or you did not use an Email", Toast.LENGTH_SHORT).show();
                }


            }
        });

        // On click event for login Button
        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent login = new Intent(MainActivity.this, Login.class);
                startActivity(login);
            }
        });

        // On click event for Map
        mViewmap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent openMap = new Intent(MainActivity.this, MapActivity.class);
                startActivity(openMap);
            }
        });



    }

    //**********************************************************************************************************
    // validates email and password
    private boolean validateForm()
    {
        String emailValue = mEmail.getText().toString();
        String passwordValue = mPassword.getText().toString();


        // Check to see if value is empty
        if(emailValue.isEmpty() || passwordValue.isEmpty())
        {
            return false;
        }


        // Check to make sure email is an email
        if(!emailValue.contains("@") || !emailValue.contains("."))
        {
            return false;
        }


        return true;
    }
    //**********************************************************************************************************
    // submits email and password to database
    private void submitData()
    {
        // Get the values
        String regEmail = mEmail.getText().toString();
        String regPassword = mPassword.getText().toString();

        // Hash the password
        String hashedPassword = sha256(regPassword);


        // Add to the database
        HashMap<String, String> usermap = new HashMap<>();
        usermap.put("email", regEmail);
        usermap.put("password", hashedPassword);
        root.push().setValue(usermap);

        Toast.makeText(this, "Now Login!", Toast.LENGTH_SHORT).show();
        Intent login = new Intent(MainActivity.this, Login.class);
        startActivity(login);


    }
    //**********************************************************************************************************
    // Hashing Algorithm: password is hashed before storing in database
    public static String sha256(String base) {
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes("UTF-8"));
            StringBuffer hexString = new StringBuffer();

            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }
    //**********************************************************************************************************
    // Checks database for identical email before submitting the data
    private void checkEmailAndRun()
    {
        // Check db for identical email
        String emailValue = mEmail.getText().toString();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");

        Query checkUser = reference.orderByChild("email").equalTo(emailValue);
        checkUser.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists())
                {
                    Toast.makeText(MainActivity.this, "Email Already Exists", Toast.LENGTH_SHORT).show();

                }
                else
                {
                    // No other email exists, submit the data
                    submitData();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

    }
}