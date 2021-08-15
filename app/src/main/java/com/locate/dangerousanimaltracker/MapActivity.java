package com.locate.dangerousanimaltracker;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback{

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
       // Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();
        mMap = googleMap;

        if (mLocationPermissionGranted) {
            getDeviceLocation();
            // Mark Location
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED)
            {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            init();

        }

    }

    private static final String TAG = "MapActivity";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static  final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final float DEFAULT_ZOOM = 15f;

    // Widgets
    private EditText mSearchText;
    private ImageView ic_magnify;
    private ImageView mGps;
    private ImageView mHome;
    private EditText mAddDesc;
    private RelativeLayout relLayout2;


    // vars
    private boolean mLocationPermissionGranted = false;
    private static final int code = 1;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private boolean loadedMap = false;

    //String[] ID = {};

    // Connecting to Databsde
    private FirebaseDatabase db = FirebaseDatabase.getInstance();
    private DatabaseReference root = db.getReference().child("Locationinfo");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mSearchText = (EditText) findViewById(R.id.input_search);
        ic_magnify = (ImageView) findViewById(R.id.ic_magnify);
        mGps = (ImageView) findViewById(R.id.ic_gps);
        mHome = (ImageView) findViewById(R.id.ic_home);
        mAddDesc = (EditText) findViewById(R.id.input_desc);
        relLayout2 = (RelativeLayout) findViewById(R.id.relLayout2);

        getLocationPermission();

        if(!loadedMap)
        {
            loadMapFromDB();
            Toast.makeText(this, "Loading Map", Toast.LENGTH_SHORT).show();
            loadedMap = true;
        }
        else
        {
            Toast.makeText(this, "Locations Already loaded", Toast.LENGTH_SHORT).show();
        }



    }

    //*********************************************************************************************************
    // initializes location, layouts, and other fields/ widgets used on the Map Acticity
    private void init()
    {
        relLayout2.setEnabled(false);
        relLayout2.setVisibility(View.GONE);

        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) // ||event.getAction() == event.ACTION_DOWN || event.getAction() == event.KEYCODE_ENTER
                {
                    relLayout2.setEnabled(true);
                    relLayout2.setVisibility(View.VISIBLE);
                    mAddDesc.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            if(actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) // ||event.getAction() == event.ACTION_DOWN || event.getAction() == event.KEYCODE_ENTER
                            {
                               geoLocate();

                               // Reset Everything
                                relLayout2.setEnabled(false);
                                relLayout2.setVisibility(View.GONE);
                                mAddDesc.setText("");
                                mSearchText.setText("");

                            }

                            return false;
                        }
                    });

                }

                return false;
            }
        });

        // Other widgets to be init
        // Gps button
        mGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDeviceLocation();
            }
        });

        // Go back to home page
        mHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent home = new Intent(MapActivity.this, Account.class);
                startActivity(home);
            }
        });



    }


    //*********************************************************************************************************
    // Finds places typed into search and submits to datbase
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void geoLocate()
    {
        // Define date
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();

        //Define vars
        String searchString = mSearchText.getText().toString();
        String description = mAddDesc.getText().toString().trim() + " Reported on: " + formatter.format(date);
        Geocoder geocoder = new Geocoder(MapActivity.this);
        List<Address> list = new ArrayList<>();

        try
        {
            list = geocoder.getFromLocationName(searchString, 1);
        }
        catch(IOException e)
        {

        }

        if(list.size() > 0)
        {
            Address address = list.get(0);
            //Toast.makeText(this, address.toString(), Toast.LENGTH_SHORT).show();

            // change position
            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), DEFAULT_ZOOM, description);

            // Save cords to db
            String latCord = String.valueOf(address.getLatitude());
            String lonCord = String.valueOf(address.getLongitude());
            checkLocationDBAndSubmit(latCord,lonCord,description);

        }

    }
    //*********************************************************************************************************
    // Loads in all current places from database
    private void loadMapFromDB()
    {
        // Read each Lat and Lon
        // Plot each Lat, Long and Description

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Locationinfo");


       reference.addListenerForSingleValueEvent(new ValueEventListener() {
           int totalCords;

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists())
                {
                    // vars
                    // Array that has all IDs
                    String[] ID = {"-McUdurGMJ5mRrfxXnuc", "-McUiSWORFDdGPNtvsCV", "-McUiytblzwyPOla2qTo",
                            "-McdsEy181abd-cQdwHP", "-Mcdt2Vsa91Rey25pkOr", "-McdvvTbv4Y66HZRdGUe", "-McdwGucgsOkbaVEVJsd",
                            "-MdwgQ9B35HBqIgdHw2P", "-MdwhSoK_6TfoGdZ2QZd", "-MdwiPjqrJacqxL1maHG", "-MdwoED5A0jqWuhFz0uI",
                            "-MdwoSKoXw6rN-2xJF2Y", "-Me0-iiPd26rTmJrNo69", "-Mg29dZO5zNM6kb5Fku3"};
                    // get total number of cords
                    totalCords = (int) snapshot.getChildrenCount();

                    if(ID.length < totalCords)
                    {
                        totalCords = ID.length;
                        String printCords = String.valueOf(totalCords) + " Reports";
                        Toast.makeText(MapActivity.this, printCords, Toast.LENGTH_SHORT).show();
                        // Loop thru each cord
                        for(int i = 0; i < totalCords; i++)
                        {
                            //String tempID = snapshot.child("Locationinfo").getRef().getRoot().push().getKey();


                            String latFromDB = (String) snapshot.child(ID[i]).child("Lat").getValue();
                            String lonFromDB = (String) snapshot.child(ID[i]).child("Lon").getValue();
                            String desc = (String) snapshot.child(ID[i]).child("Desc").getValue();
                            //String desc = (String) snapshot.child(ID[i]).child("Desc").getValue();

                            // Convert to double
                            Double latAsDouble = Double.valueOf(latFromDB);
                            Double lonAsDouble = Double.valueOf(lonFromDB);


                            // Put the points on the map
                            LatLng location = new LatLng(latAsDouble, lonAsDouble);
                            MarkerOptions points = new MarkerOptions().position(location).title(desc);
                            mMap.addMarker(points);

                        }


                    }
                    else
                    {
                        String printCords = String.valueOf(totalCords) + " Reports";
                        Toast.makeText(MapActivity.this, printCords, Toast.LENGTH_SHORT).show();
                        // Loop thru each cord
                        for(int i = 0; i < totalCords; i++)
                        {
                            //String tempID = snapshot.child("Locationinfo").getRef().getRoot().push().getKey();


                            String latFromDB = (String) snapshot.child(ID[i]).child("Lat").getValue();
                            String lonFromDB = (String) snapshot.child(ID[i]).child("Lon").getValue();
                            String desc = (String) snapshot.child(ID[i]).child("Desc").getValue();
                            //String desc = (String) snapshot.child(ID[i]).child("Desc").getValue();

                            // Convert to double
                            Double latAsDouble = Double.valueOf(latFromDB);
                            Double lonAsDouble = Double.valueOf(lonFromDB);


                            // Put the points on the map
                            LatLng location = new LatLng(latAsDouble, lonAsDouble);
                            MarkerOptions points = new MarkerOptions().position(location).title(desc);
                            mMap.addMarker(points);


                        }

                    }


                }
                else
                {
                    // No Locations exist
                    totalCords = 0;
                    String printCords = String.valueOf(totalCords);
                    Toast.makeText(MapActivity.this, printCords, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

    }
    //*********************************************************************************************************
    // Makes sure same place is not already in database
    private void checkLocationDBAndSubmit(String lat, String lon, String desc)
    {
        // Check db for identical cords

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Locationinfo");

        // First check Lat
        Query checkInfo = reference.orderByChild("Lat").equalTo(lat);
        checkInfo.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists())
                {
                    // Lat Exists
                    // Next check Lon
                    Query checkInfo2 = reference.orderByChild("Lon").equalTo(lon);
                    checkInfo2.addListenerForSingleValueEvent(new ValueEventListener() {

                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.exists())
                            {
                                // Lon & Lat Exists
                                Toast.makeText(MapActivity.this, "Location already exists", Toast.LENGTH_SHORT).show();
                            }
                            else
                            { }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) { }
                    });

                }
                else
                {
                    // Lat and Lon does not exist submit to DB
                    HashMap<String, String> locationData = new HashMap<>();
                    locationData.put("Lat", lat);
                    locationData.put("Lon", lon);
                    locationData.put("Desc", desc);
                    root.push().setValue(locationData);
                    Toast.makeText(MapActivity.this, "Submitted to DB", Toast.LENGTH_SHORT).show();

                    // Generate a reference to a new location and add some data using push()
                    DatabaseReference pushedPostRef = root.push();
                    // Get the unique ID generated by a push()
                    String postId = pushedPostRef.getKey();
                    Toast.makeText(MapActivity.this, postId, Toast.LENGTH_SHORT).show();

                    // Add to ID array
                    //ID[ID.length] = postId;

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

    }
    //*********************************************************************************************************
    // Finds the users current location
    private void getDeviceLocation()
    {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try
        {
            if(mLocationPermissionGranted)
            {
                Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful())
                        {
                            // Location was found
                            Location currentLocation = (Location) task.getResult();

                            // Call methods for moving the camera
                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM, "My Location");

                        }
                        else
                        {
                            Toast.makeText(MapActivity.this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

        }
        catch(SecurityException e)
        {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }
    //*********************************************************************************************************
    // Repositions camera
    private void moveCamera(LatLng latLng, float zoom, String title)
    {
        Log.d(TAG, "moveCamera: Moving camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        // code for dropping pin

        MarkerOptions options = new MarkerOptions().position(latLng).title(title);
        // Does not add marker on my location
        if(options.getTitle() != "My Location"){
            mMap.addMarker(options);
        }


    }




    //*********************************************************************************************************
    private void initMap()
    {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

     // ->
        mapFragment.getMapAsync((OnMapReadyCallback) MapActivity.this);

    }


    //*********************************************************************************************************
    private void getLocationPermission(){
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(), COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                // set a boolean
                mLocationPermissionGranted = true;
                initMap();
            }else{
                ActivityCompat.requestPermissions(this, permissions, code);
            }
        }else{
            ActivityCompat.requestPermissions(this, permissions, code);
        }
    }

    //*********************************************************************************************************
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mLocationPermissionGranted = false;

        switch(requestCode){

            case code:{
                if(grantResults.length > 0){
                    for(int i = 0; i > grantResults.length; i++)
                    {
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED)
                        {
                            mLocationPermissionGranted = false;
                            return;
                        }
                    }

                    mLocationPermissionGranted = true;
                    // initilize map
                    initMap();
                }

            }

        }
    }

}
