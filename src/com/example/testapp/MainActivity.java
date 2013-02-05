package com.example.testapp;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

public class MainActivity extends FragmentActivity implements LocationListener, LocationSource{
	
	//keep track of camera capture intent
	final int CAMERA_CAPTURE = 1;
	final int RESULT_LOAD_IMAGE = 2;
	//captured picture uri
	//private Uri picUri;
	
	private OnLocationChangedListener onLocationChangedListener;
	private LocationManager locationManager;
	private GoogleMap mMap;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /*
        Parse.initialize(this, "K9iLWiyrQV3IQIh2M6brQMpDVco13plZP50ulcZN", "6jdyxDJk2OY9a6wZvdyUz8861zQWHiJThD1W85Yq"); 
        ParseObject testObject = new ParseObject("TestObject");
        testObject.put("foo", "bar");
        testObject.saveInBackground();
        */
        // Set up location manager
        initLocation();
        
        // Run Google Map API
        initMap();

        //retrieve a reference to the UI button
        findViews();
    }
    
    @Override
    public void onLocationChanged(Location location) 
    {
        if(onLocationChangedListener!=null )
        {
			onLocationChangedListener.onLocationChanged(location);
			//Move the camera to the user's location once it's available!
			//only if locatingMe is true
			if (mMap!=null){
			    CameraUpdate pino= CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude()));
			    mMap.animateCamera(pino);
			}
        }
    }
    
    private void initLocation(){
    	locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        //You may want to pass a different provider in as the first arg here
        //depending on the location accuracy that you desire
        //see LocationManager.getBestProvider()
        Criteria locationCriteria = new Criteria();
        locationCriteria.setAccuracy(Criteria.NO_REQUIREMENT);
        locationManager.requestLocationUpdates(locationManager.getBestProvider(locationCriteria, true), 1L, 2F, this);
    }
    
    private void initMap(){
    	try {
    		MapsInitializer.initialize(this);
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    	mMap = ((SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        if(mMap!=null){
        	mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        	mMap.setMyLocationEnabled(true);
        	mMap.setLocationSource(this);
        	mMap.animateCamera(CameraUpdateFactory.zoomTo(14.0f));
        }
    }
    
    private void findViews()
    {
    	Button captureBtn = (Button)findViewById(R.id.take_photo);
    	Button chooseBtn = (Button)findViewById(R.id.choose_photo);
    	captureBtn.setOnClickListener(takePics);
    	chooseBtn.setOnClickListener(choosePics);
    }
    
    
    @Override
    public void activate(OnLocationChangedListener listener) 
    {
        onLocationChangedListener = listener;
    }

    @Override
    public void deactivate() 
    {
        onLocationChangedListener = null;
    }
    
    @Override
    public void onProviderDisabled(String provider) 
    {
        // TODO Auto-generated method stub
        Toast.makeText(this, "provider disabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderEnabled(String provider) 
    {
        // TODO Auto-generated method stub
        Toast.makeText(this, "provider enabled", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) 
    {
        // TODO Auto-generated method stub
        Toast.makeText(this, "status changed", Toast.LENGTH_SHORT).show();
    }
    
    private OnClickListener takePics = new OnClickListener(){
    	public void onClick(View v) {
    	    if (v.getId() == R.id.take_photo) {
    	    	try {
    	    	    //use standard intent to capture an image
    	    	    Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    	    	    //we will handle the returned data in onActivityResult
    	    	    startActivityForResult(captureIntent, CAMERA_CAPTURE);
    	    	}
    	    	catch(ActivityNotFoundException anfe){
    	    	    //display an error message
    	    	    String errorMessage = "Whoops - your device doesn't support capturing images!";
    	    	    Toast toast = Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT);
    	    	    toast.show();
    	    	}
    	    }
    	}
    };
    
    private OnClickListener choosePics = new OnClickListener(){
    	public void onClick(View v) {
    	    if (v.getId() == R.id.choose_photo) {
    	    	Intent intent = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, RESULT_LOAD_IMAGE);
    	    }
    	}
    };
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
        	//user is returning from capturing an image using the camera
    		//retrieve a reference to the ImageView
    		ImageView picView = (ImageView)findViewById(R.id.imgView);
        	if(requestCode == CAMERA_CAPTURE){
        		//get the returned data
        		Bundle extras = data.getExtras();
        		//get the cropped bitmap
        		Bitmap thePic = (Bitmap)extras.get("data");
        		//display the returned cropped image
        		picView.setImageBitmap(thePic);
        	}
        	else if(requestCode == RESULT_LOAD_IMAGE){
        		Uri selectedImage = data.getData();
                String[] filePathColumn = { MediaStore.Images.Media.DATA };
     
                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                cursor.moveToFirst();
     
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String picturePath = cursor.getString(columnIndex);
                cursor.close();
                
                picView.setImageBitmap(BitmapFactory.decodeFile(picturePath));
        	}
        }
    }
    
    
    private void openOptionsDialog() 
    {
    	new AlertDialog.Builder(MainActivity.this)
    						.setTitle(R.string.about_title)
    						.setMessage(R.string.about_msg)
    						.setPositiveButton(R.string.ok_label,
    								new DialogInterface.OnClickListener(){
    									public void onClick(
    											DialogInterface dialoginterface, int i){
    									}
    						})
    						.setNegativeButton(R.string.homepage_label,
    								new DialogInterface.OnClickListener(){
    									public void onClick(DialogInterface dialoginterface, int i){
    										//go to URL
    										Uri uri = Uri.parse(getString(R.string.homepage_uri));
    										Intent intent = new Intent(Intent.ACTION_VIEW, uri);
    										startActivity(intent);
    							}
    						})
    						.show();
    }

    protected static final int MENU_ABOUT = Menu.FIRST;
    protected static final int MENU_Quit = Menu.FIRST+1;
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	menu.add(0, MENU_ABOUT, 0, "About");
    	menu.add(0, MENU_Quit, 0, "End");
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        super.onOptionsItemSelected(item);
        switch(item.getItemId()){
             case MENU_ABOUT:
                  openOptionsDialog();
                  break;
             case MENU_Quit:
                  finish();
                 break;
        }
        return true;
    }
    
}
