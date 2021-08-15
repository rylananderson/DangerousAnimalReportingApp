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

public class Login extends AppCompatActivity {

    private EditText mEmail, mPassword;
    Button mLogin, mSignUpInstead, mresetBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // hooks
        mEmail = findViewById(R.id.email);
        mPassword = findViewById(R.id.password);
        mLogin = findViewById(R.id.logIn);
        mSignUpInstead = findViewById(R.id.signUpInstead);
        mresetBtn = findViewById(R.id.resetBtn);

        // Login btn pressed
        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // First Validate the form and check entries with db
                boolean valid = validateForm();

                if(valid)
                {
                    submitData();
                }
                else
                {
                    Toast.makeText(Login.this, "Fields are empty or you did not use an Email", Toast.LENGTH_SHORT).show();
                }

            }
        });

        // Sign UP Instead Btn pressed
        mSignUpInstead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signUp = new Intent(Login.this, MainActivity.class);
                startActivity(signUp);
            }
        });
        // Reset Btn pressed
        mresetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resetPswd = new Intent(Login.this, ResetPassword.class);
                startActivity(resetPswd);
            }
        });




    }
    //**********************************************************************************************************
    // Validates the email and password before checking database
    private boolean validateForm()
    {
        String emailValue = mEmail.getText().toString();
        String passwordValue = mPassword.getText().toString();

        // Check to see if value is empty
        if(emailValue.isEmpty() || passwordValue.isEmpty())
        {
            return false;
        }

        // Check db to make sure other

        // Check to make sure email is an email
        if(!emailValue.contains("@") || !emailValue.contains("."))
        {
            return false;
        }
        return true;
    }

    //**********************************************************************************************************
    // Submits email and password to database
    private void submitData()
    {
        final String userEnteredEmail = mEmail.getText().toString().trim();
        final String userEnteredPassword = mPassword.getText().toString().trim();
        final String hashedPassword = sha256(userEnteredPassword);

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        Query checkUser = reference.orderByChild("email").equalTo(userEnteredEmail);

        checkUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(snapshot.exists()){

                    for(DataSnapshot dataSnapshot : snapshot.getChildren()){

                        String passwordFromDB = dataSnapshot.child("password").getValue().toString();
                        // Check the Password after Printing Out
                        if(passwordFromDB.equals(hashedPassword)){

                            // retrieve all the values
                            String emailFromDB = dataSnapshot.child("email").getValue().toString();

                            Intent usrProfile = new Intent(Login.this, Account.class);
                            usrProfile.putExtra("email", emailFromDB);
                            startActivity(usrProfile);

                            Toast.makeText(Login.this, "Email and Password Match!",
                                    Toast.LENGTH_LONG).show();
                        }
                        else{
                            Toast.makeText(Login.this, "Password is wrong",
                                    Toast.LENGTH_LONG).show();
                        }
                    } // End Of for loop

                }else{
                    Toast.makeText(Login.this, "Email does not exist",
                            Toast.LENGTH_LONG).show();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    //**********************************************************************************************************
    // Hashing Algorithm: used to compare hashed pswd in database with user entered pswd
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

}