package com.locate.dangerousanimaltracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.security.MessageDigest;
import java.util.Random;

public class ResetPassword extends AppCompatActivity {

    private EditText mEmail, mCode;
    private Button mSendCode, mCheckcode, mOk;
    private boolean codeIsCorrect = false;
    private TextView mMessage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);
        // Hooks
        mEmail = (EditText) findViewById(R.id.emailForSending);

        mCode = findViewById(R.id.code);
        mCheckcode = findViewById(R.id.checkCode);
        mSendCode = findViewById(R.id.sendCode);
        mMessage = findViewById(R.id.msg);
        mOk = findViewById(R.id.ok);

        

        // Send Code Button Clicked
        mSendCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userEmail = mEmail.getText().toString();
                int code = generateCode();
                sendEmail(userEmail, code);


                // Check Code
                mCheckcode.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String enteredCode = mCode.getText().toString();
                        String codeAsAString = String.valueOf(code);
                        if(enteredCode.equals(codeAsAString))
                        {

                            Toast.makeText(ResetPassword.this, "Correct!", Toast.LENGTH_SHORT).show();
                            codeIsCorrect = true;
                            mMessage.setText("Support will contact you soon");
                        }
                        else
                        {
                            Toast.makeText(ResetPassword.this, "Wrong!", Toast.LENGTH_SHORT).show();
                            codeIsCorrect = false;
                        }
                    }
                });// Code checker


            }
        });

        mOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              Intent logIn = new Intent(ResetPassword.this, Login.class);
              startActivity(logIn);

            }
        });






    }
    //**********************************************************************************************************
    private int generateCode()
    {
        Random rand = new Random();

        int low = 100;
        int high = 10000;
        int result = rand.nextInt(high-low) + low;

        return result;
    }
    //**********************************************************************************************************

    private void sendEmail(String emailTo, int codeTo) {
        // Make sure email exists in db
        // Check db for identical email
        //String emailValue = mEmail.getText().toString();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");

        Query checkUser = reference.orderByChild("email").equalTo(emailTo);
        checkUser.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists())
                {
                    // If the email is in the database then send the email
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("message/rfc822");
                    i.putExtra(Intent.EXTRA_EMAIL  , new String[]{emailTo});
                    i.putExtra(Intent.EXTRA_SUBJECT, "Password Reset Code");
                    i.putExtra(Intent.EXTRA_TEXT   , "Your reset code is: " + codeTo);
                    try {
                        startActivity(Intent.createChooser(i, "Send mail..."));
                        mSendCode.setText("Email Sent!");
                    } catch (android.content.ActivityNotFoundException ex) {
                        Toast.makeText(ResetPassword.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                    }

                }
                else
                {
                    // No other email exists, submit the data
                    Toast.makeText(ResetPassword.this, "Your Email does not exist in the database", Toast.LENGTH_SHORT).show();

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });


    }


}