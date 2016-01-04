package xyz.arrivealive;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.api.IMapController;

import org.osmdroid.bonuspack.location.GeocoderPhoton;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.bonuspack.routing.GraphHopperRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.DirectedLocationOverlay;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.MyLocationOverlay;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class Navigator extends ActionBarActivity {
    private final static int INTERVAL = 1000;
    Handler mHandler;
    Runnable mHandlerTask;
    public String data;
    public List<String> suggest;
    public AutoCompleteTextView autoComplete;
    public ArrayAdapter<String> aAdapter;
    public Button clearButton;
    public AutoCompleteTextView textDest;
    public String destString,startString;
    public TextView directions;
    public static Road mRoad;
    public Polyline mRoadOverlay;
    public MapView map;
    public GeoPoint startPoint, destinationPoint;
    public DirectedLocationOverlay myLocationOverlay;
    public ArrayList<GeoPoint> waypoints;
    public RoadManager roadManager;
    public GeocoderPhoton geocoder;
    public GeoPoint currentLocation;
    public LocationListener locationListener;
    public OverlayItem myCurrentLocationOverlayItem;
    public ItemizedOverlay currentLocationOverlay;
    public Drawable myCurrentLocationMarker;
    public int currpoint;
    public TextToSpeech ttobj;
    public int MapOverlayIndex = -1;
    IMapController mapController;
    LocationManager locationManager;
    ArrayList<OverlayItem> overlayItemArray;

    public double lat = -1.0,lon = -1.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigator);
        textDest   = (AutoCompleteTextView)findViewById(xyz.arrivealive.R.id.Destination);
        map = (MapView) findViewById(xyz.arrivealive.R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        //directions = findViewById(xyz.arrivealive.R.id.)
        mapController = map.getController();
        mapController.setZoom(7);
        map.setMinZoomLevel(7);
        myCurrentLocationMarker = this.getResources().getDrawable(R.drawable.current);
        DefaultResourceProxyImpl defaultResourceProxyImpl
                = new DefaultResourceProxyImpl(this);
        //ItemizedIconOverlay myItemizedIconOverlay
        //        = new ItemizedIconOverlay(
        //        overlayItemArray, null, defaultResourceProxyImpl);
        //map.getOverlays().add(myItemizedIconOverlay);
        startPoint = new GeoPoint(52.752, -7.953);
        mapController.setCenter(startPoint);
        suggest = new ArrayList<String>();
        textDest.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable editable) {
                // TODO Auto-generated method stub
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String newText = s.toString();
                new getJson().execute(newText);
            }
        });
        locationListener = new MyLocationListener();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Toast.makeText(this, "GPS is Enabled in your devide", Toast.LENGTH_SHORT).show();
        }else{
            showGPSDisabledAlertToUser();
        }
        if( location != null ) {
            currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
        }
        geocoder = new GeocoderPhoton(new Activity());
        ttobj=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
            }
        }
        );
        mHandler = new Handler();
        mHandlerTask = new Runnable()
        {
            @Override
            public void run() {
                turnbyturn();
                mHandler.postDelayed(mHandlerTask, INTERVAL);
            }
        };
        ttobj.setLanguage(Locale.UK);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(xyz.arrivealive.R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == xyz.arrivealive.R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class UpdateRoadTask extends AsyncTask<Object, Void, Road> {
        protected Context context;

        public UpdateRoadTask(Context context) {
            this.context = context;
        }

        protected Road doInBackground(Object... params) {
            roadManager = new GraphHopperRoadManager("");
            return roadManager.getRoad(waypoints);
        }

        protected void onPostExecute(Road result) {
            mRoad = result;
            if (mRoad.mStatus != Road.STATUS_OK){
                Toast.makeText(context, "Failed to retrieve route, check addresses and your internet connetion ", Toast.LENGTH_LONG).show();
            }
            List<Overlay> mapOverlays = map.getOverlays();
            if (mRoad == null)
                return;
            if (mRoadOverlay != null) {
                mapOverlays.remove(mRoadOverlay);
                mRoadOverlay = null;
            }
            mRoadOverlay = roadManager.buildRoadOverlay(mRoad, context);
            mapOverlays.add(mRoadOverlay);
            for (int i=0; i< mRoad.mNodes.size(); i++){
                RoadNode node = mRoad.mNodes.get(i);
                Log.d("Content", node.mLocation.toString());

                //directions.append(i + 1 + ":" + node.mInstructions.toString() + '\n');
            }
            Toast.makeText(context, "Route Recieved ", Toast.LENGTH_LONG).show();
            mapController.setZoom(15);
            mapController.animateTo((mRoad.mNodes.get(0).mLocation));
            currpoint = 0;
            map.invalidate();
            startRepeatingTask();
        }
    }
    private class getRoadTask extends AsyncTask<Object, Void, ArrayList> {
        protected Context context;
        protected boolean valid = true;
        protected GeoPoint point1 = null ,point2 = null;
        public getRoadTask(Context context) {
            this.context = context;
        }

        protected void onPreExecute() {
            destString = textDest.getText().toString();
            Toast.makeText(context, destString, Toast.LENGTH_SHORT).show();
            point1 = _getLocation();
            if(point1.getLatitude() == -1.0 && point1.getLatitudeE6() == -1.0) {
                Toast.makeText(context, "Failed to get GPS", Toast.LENGTH_SHORT).show();
                valid = false;
            }
        }
        protected ArrayList doInBackground(Object... params){
            if(valid == true) {
                List<Address> destBuffer1 = new ArrayList<>();
                List<Address> destBuffer2 = new ArrayList<>();
                try {
                    destBuffer2 = geocoder.getFromLocationName(destString, 1);
                } catch (IOException ie) {
                    return null;
                }
                if (point1 == null) {
                    point1 = new GeoPoint(destBuffer1.get(0).getLatitude(), destBuffer1.get(0).getLongitude());
                }
                point2 = new GeoPoint(destBuffer2.get(0).getLatitude(), destBuffer2.get(0).getLongitude());
                ArrayList<GeoPoint> ways = new ArrayList<GeoPoint>();
                ways.add(point1);
                ways.add(point2);
                return ways;
            }
            return null;
        }
        protected void onPostExecute(ArrayList ways) {
            if(ways == null){
                Toast.makeText(context, "Failed to reverse geocode", Toast.LENGTH_SHORT).show();
            }
            else {
                waypoints = ways;
                new UpdateRoadTask(context).execute();
            }
        }
    }
    public void getRouteHandler(View view) {
        new getRoadTask(this).execute();
    }
    public void clear(View view) {
        //directions.setText("");
        //directions.setVisibility(View.GONE);
        clearButton.setVisibility(View.GONE);
    }
    private GeoPoint _getLocation() {
        LocationManager locationManager = (LocationManager)
                getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String bestProvider = locationManager.getBestProvider(criteria, false);
        Location location = locationManager.getLastKnownLocation(bestProvider);
        LocationListener loc_listener = new LocationListener() {
            public void onLocationChanged(Location l) {}
            public void onProviderEnabled(String p) {}
            public void onProviderDisabled(String p) {}
            public void onStatusChanged(String p, int status, Bundle extras) {}
        };
        locationManager.requestLocationUpdates(bestProvider, 0, 0, loc_listener);
        location = locationManager.getLastKnownLocation(bestProvider);
        try {
            lat = location.getLatitude();
            lon = location.getLongitude();
        } catch (NullPointerException e) {
            lat = -1.0;
            lon = -1.0;
        }
        return new GeoPoint(lat,lon);
    }
    class getJson extends AsyncTask<String,String,String>{

        @Override
        protected String doInBackground(String... key) {
            String newText = key[0];
            newText = newText.trim();
            newText = newText.replace(" ", "+");
            try{
                HttpClient hClient = new DefaultHttpClient();
                HttpGet hGet = new HttpGet("http://arrivealive.xyz:8443/api?limit=8&q=" + newText);
                ResponseHandler<String> rHandler = new BasicResponseHandler();
                data = hClient.execute(hGet,rHandler);
                suggest = new ArrayList<String>();
                JsonParser parser = new JsonParser();
                JsonElement json = parser.parse(data);
                JsonObject jResult = json.getAsJsonObject();
                Log.d("Content", json.toString());
                Log.d("Test", jResult.get("features").getAsJsonArray().get(0).getAsJsonObject().get("properties").getAsJsonObject().get("name").toString());
                for(int i=0;i<8;i++){
                    String SuggestKey = jResult.get("features").getAsJsonArray().get(i).getAsJsonObject().get("properties").getAsJsonObject().get("name").toString();
                    SuggestKey = SuggestKey.replaceAll("\"","");
                    suggest.add(SuggestKey);
                }
                Log.d("Content",suggest.toString());

            }catch(Exception e){
            }
            runOnUiThread(new Runnable(){
                public void run(){
                    aAdapter = new ArrayAdapter<String>(getApplicationContext(), xyz.arrivealive.R.layout.item, suggest);
                    if(!aAdapter.equals(null)) {
                        textDest.setAdapter(aAdapter);
                        aAdapter.notifyDataSetChanged();
                    }
                }
            });
            return null;
        }

    }
    public class MyLocationListener implements LocationListener {

        public void onLocationChanged(Location location) {
            currentLocation = new GeoPoint(location);
            mapController.animateTo(currentLocation);
            displayMyCurrentLocationOverlay();
        }

        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }
    public void displayMyCurrentLocationOverlay(){
        Marker startMarker = new Marker(map);
        startMarker.setIcon(this.getResources().getDrawable(R.drawable.current));
        startMarker.setPosition(currentLocation);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        if(MapOverlayIndex != -1) {
            map.getOverlays().remove(MapOverlayIndex);
        }
        map.getOverlays().add(startMarker);
        MapOverlayIndex = map.getOverlays().size() - 1;
        map.invalidate();
    }


    void startRepeatingTask()
    {
        mHandlerTask.run();
    }

    void stopRepeatingTask()
    {
        mHandler.removeCallbacks(mHandlerTask);
    }
    void turnbyturn() {
        if(currpoint == mRoad.mNodes.size()){
            stopRepeatingTask();
        }
        if(near(mRoad.mNodes.get(currpoint))){
            ttobj.speak(mRoad.mNodes.get(currpoint).mInstructions, TextToSpeech.QUEUE_FLUSH, null);
            currpoint += 1;
        }
    }
    boolean near(RoadNode rn){
        Location locationA = new Location("point A");

        locationA.setLatitude(rn.mLocation.getLatitude());
        locationA.setLongitude(rn.mLocation.getLongitude());

        Location locationB = new Location("point B");

        locationB.setLatitude(currentLocation.getLatitude());
        locationB.setLongitude(currentLocation.getLongitude());

        float distance = locationA.distanceTo(locationB);
        Log.d("content", String.valueOf(distance));
        if(distance < 30){
            return true;
        }
        return false;
    }
    private void showGPSDisabledAlertToUser(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("GPS is disabled in your device. Would you like to enable it?")
                .setCancelable(false)
                .setPositiveButton("Goto Settings Page To Enable GPS",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent callGPSSettingIntent = new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(callGPSSettingIntent);
                            }
                        });
        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

}