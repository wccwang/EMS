package com.example.testapp;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.lang.Math;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.telephony.TelephonyManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.parse.Parse;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.codec.binary.Base64;

public class MainActivity extends FragmentActivity implements LocationListener, LocationSource{
	
	//keep track of camera capture intent
	final int MODE_SELECTION = 0;
	final int CAMERA_CAPTURE = 1;
	final int RESULT_LOAD_IMAGE = 2;
	final int SUBMIT_DATA = 3;
	//captured picture uri
	//private Uri picUri;
	
	private TextView accidentIn;
	private String modeStr;
	private EditText phoneNumIn;
	private EditText nameIn;
	private OnLocationChangedListener onLocationChangedListener;
	private LocationManager locationManager;
	private GoogleMap mMap;
	
	private ParseObject helpRequest;
	private ParseObject hashedData;
	private String locText;
	private String timeStamp;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        Parse.initialize(this, "K9iLWiyrQV3IQIh2M6brQMpDVco13plZP50ulcZN", "6jdyxDJk2OY9a6wZvdyUz8861zQWHiJThD1W85Yq"); 
        helpRequest = new ParseObject("HelpRequest");
        hashedData = new ParseObject("HashedData");
        
        // Set up location manager
        initLocationManager();
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
			    mMap.moveCamera(pino);
			    mMap.animateCamera(CameraUpdateFactory.zoomTo(14.0f));
			}
        }
        
        locText = "[" + String.format("%.5f", location.getLatitude())+ "," + String.format("%.5f", location.getLongitude()) + "]";
        helpRequest.put("location", locText);
        try {
			hashedData.put("locationHash", computeHash(locText));
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private void initLocationManager(){
    	locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //You may want to pass a different provider in as the first arg here
        //depending on the location accuracy that you desire
        //see LocationManager.getBestProvider()
        Criteria locationCriteria = new Criteria();
        locationCriteria.setAccuracy(Criteria.NO_REQUIREMENT);
        locationManager.requestLocationUpdates(locationManager.getBestProvider(locationCriteria, true), 1L, 2f, this);
        /*
        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (mMap!=null){
		    CameraUpdate pino= CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude()));
		    mMap.animateCamera(pino);
		    mMap.animateCamera(CameraUpdateFactory.zoomTo(14.0f));
		}
		*/
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
        	//mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(40.7142, -74.0064)));
        	mMap.getUiSettings().setZoomControlsEnabled(false);
        	mMap.getUiSettings().setMyLocationButtonEnabled(false);
        	//mMap.animateCamera(CameraUpdateFactory.zoomTo(14.0f));
        }
    }
    
    private void findViews()
    {
    	modeStr = "N/A";
    	accidentIn = (TextView)findViewById(R.id.accident);
    	accidentIn.setText("Click to Select Accident Type:");
    	accidentIn.setOnClickListener(chooseType);
    	
    	phoneNumIn = (EditText)findViewById(R.id.phone_number);
    	phoneNumIn.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
    	TelephonyManager tMgr =(TelephonyManager)getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
    	phoneNumIn.setText(tMgr.getLine1Number());
    	
    	nameIn = (EditText)findViewById(R.id.name);
    	
    	Button captureBtn = (Button)findViewById(R.id.upload_photo);
    	captureBtn.setOnClickListener(upload);
    	
    	Button submitBtn = (Button)findViewById(R.id.submit);
    	submitBtn.setOnClickListener(submit);
    }
    
    @Override  
    public void onCreateContextMenu(ContextMenu menu, View v,ContextMenuInfo menuInfo) {  
    	super.onCreateContextMenu(menu, v, menuInfo);  
        menu.setHeaderTitle("Upload Photo");  
        menu.add(0, v.getId(), 0, "Capture");  
        menu.add(0, v.getId(), 0, "Choose");  
    }  
    
    @Override  
    public boolean onContextItemSelected(MenuItem item) {  
        if(item.getTitle()=="Capture"){capture_photo(item.getItemId());}  
        else if(item.getTitle()=="Choose"){choose_photo(item.getItemId());}  
        else {return false;} 
        return true;  
    }  
    
	public void capture_photo(int id) {
    	try {
    	    //use standard intent to capture an image
    	    Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    	    //we will handle the returned data in onActivityResult
    	    startActivityForResult(captureIntent, CAMERA_CAPTURE);
    	}
    	catch(ActivityNotFoundException anfe){
    	    //display an error message
    	    String errorMessage = "Whoops - your device does not support capturing images!";
    	    Toast toast = Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT);
    	    toast.show();
    	}
	}
	
	public void choose_photo(int id) {
    	try{
	    	Intent intent = new Intent(
                    Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, RESULT_LOAD_IMAGE);
    	}
    	catch(ActivityNotFoundException anfe){
    	    //display an error message
    	    String errorMessage = "Whoops - your device does not have external storage!";
    	    Toast toast = Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT);
    	    toast.show();
    	}
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
        Toast.makeText(this, "Location Changed", Toast.LENGTH_SHORT).show();
    }
    
    private OnClickListener chooseType = new OnClickListener(){
    	public void onClick(View v) {
    	    if (v.getId() == R.id.accident) {
    	    	//Switch to mode selection
    	    	Intent chooseIntent = new Intent();
    	    	chooseIntent.setClass(MainActivity.this, Mode.class);
    	    	startActivityForResult(chooseIntent, MODE_SELECTION);
    	    }
    	}
    };
    
    private OnClickListener upload = new OnClickListener(){
    	public void onClick(View v) {
    		onButtonClickEvent(v);
    	}
    };
    
    public void onButtonClickEvent(View sender)
    {
        registerForContextMenu(sender); 
        openContextMenu(sender);
        unregisterForContextMenu(sender);
    }
    
    private OnClickListener submit = new OnClickListener(){
    	public void onClick(View v){
    	    if (v.getId() == R.id.submit) {
    	    	// Put Name
    	    	helpRequest.put("name", nameIn.getText().toString());
    	    	// Put phone number
	    	    helpRequest.put("phoneNumber", phoneNumIn.getText().toString());
	    	    // Put request type
	    	    helpRequest.put("requestType", modeStr);
    	    	// Put time stamp
    	    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH);
    	    	sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    	    	timeStamp = sdf.format(new Date());
    	    	helpRequest.put("timestamp", timeStamp);
    	    	
    	    	// Put MAC address in SHA-256
    	    	WifiManager wimanager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
    	    	try {
    	    		String device_id = computeHash(wimanager.getConnectionInfo().getMacAddress());
					helpRequest.put("deviceId", device_id);
					String request_id = device_id + " " + timeStamp;
					helpRequest.put("requestId", computeHash(request_id));
					
					hashedData.put("deviceId", device_id);
					hashedData.put("requestId", computeHash(request_id));
					hashedData.put("timestampHash", computeHash(timeStamp));
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
    	    	
    	    	// Put device type
    	    	String device_name = Build.MODEL;
    	    	String os_version = Build.VERSION.RELEASE;
    	    	String device_type = device_name + " (" + os_version + ")";
    	    	helpRequest.put("deviceType", device_type);
    	    	
    	    	if(modeStr != "N/A"){
    	    		helpRequest.saveInBackground();
    	    		hashedData.saveInBackground();
    	    		Intent submitIntent = new Intent();
        	    	submitIntent.setClass(MainActivity.this, Submit.class);
        	    	startActivityForResult(submitIntent, SUBMIT_DATA);
    	    	}
    	    	else{
    	    		Toast toast = Toast.makeText(getApplicationContext(), "Please Indicate the Accident Type", Toast.LENGTH_SHORT);
    	    		toast.show();
    	    	}
    	    }
    	}
    };
    
    // Convert string to SHA-256 string
    public String computeHash(String input) throws NoSuchAlgorithmException, UnsupportedEncodingException{
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.reset();

        byte[] byteData = digest.digest(input.getBytes("UTF-8"));
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < byteData.length; i++){
          sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
        	//user is returning from capturing an image using the camera
    		//retrieve a reference to the ImageView
    		ImageView picView = (ImageView)findViewById(R.id.imgView);
    		Bitmap thePic = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    		if(requestCode == MODE_SELECTION){
    			//get the returned data
        		Bundle extras = data.getExtras();
        		modeStr = extras.getString("result");
        		accidentIn.setText("Accident Type: " + modeStr);
    		}
    		else if(requestCode == CAMERA_CAPTURE){
        		//get the returned data
        		Bundle extras = data.getExtras();
        		//get the cropped bitmap
        		thePic = (Bitmap)extras.get("data");
        		//display the returned cropped image
        		picView.setImageBitmap(thePic);
        	}
	    	else if(requestCode == RESULT_LOAD_IMAGE){
        		Uri selectedImage = data.getData();
        		try {
        			thePic = BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage));
        			int width = Math.min(thePic.getWidth(), 1024);
        			int height = Math.min(thePic.getHeight(), 1024);
        			thePic = Bitmap.createScaledBitmap(thePic, width, height, false);
        			picView.setImageBitmap(thePic);
    			 } catch (FileNotFoundException e) {
    			 	// TODO Auto-generated catch block
    			 	e.printStackTrace();
    			 }
	    	}
	    	else if(requestCode == SUBMIT_DATA){
	    		nameIn.setText("");
	    		modeStr = "N/A";
	        	accidentIn.setText("Click to Select Accident Type:");
	        	picView.setImageResource(android.R.color.transparent);
	    	}
        	if(requestCode == CAMERA_CAPTURE || requestCode == RESULT_LOAD_IMAGE){
		    	//Put image to parse object
		        ByteArrayOutputStream stream = new ByteArrayOutputStream();
		        thePic.compress(Bitmap.CompressFormat.PNG, 100, stream);
		        byte[] imageData = stream.toByteArray();
				ParseFile file = new ParseFile("image.png", imageData);
				file.saveInBackground();
				helpRequest.put("imageFile", file);
				
				//Full hash
				String base64Image = Base64.encodeBase64URLSafeString(imageData);
				String fullHash = base64Image + " " + timeStamp + " " + locText;
				try {
					hashedData.put("fullHash", computeHash(fullHash));
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
        }
    }
}
