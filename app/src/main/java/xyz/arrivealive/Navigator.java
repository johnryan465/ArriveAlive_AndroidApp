package xyz.arrivealive;

import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
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
import android.widget.CheckBox;
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
import org.osmdroid.api.IMapController;

import org.osmdroid.bonuspack.location.GeocoderPhoton;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.bonuspack.routing.GraphHopperRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.DirectedLocationOverlay;
import org.osmdroid.views.overlay.Overlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class Navigator extends ActionBarActivity {
    public String data;
    public List<String> suggest;
    public AutoCompleteTextView autoComplete;
    public ArrayAdapter<String> aAdapter;
    public Button clearButton;
    public CheckBox check;
    public AutoCompleteTextView textStart,textDest;
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

    public double lat = -1.0,lon = -1.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(xyz.arrivealive.R.layout.activity_main);
        textStart = (AutoCompleteTextView)findViewById(xyz.arrivealive.R.id.Start);
        textDest   = (AutoCompleteTextView)findViewById(xyz.arrivealive.R.id.Destination);
        map = (MapView) findViewById(xyz.arrivealive.R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        IMapController mapController = map.getController();
        mapController.setZoom(7);
        map.setMinZoomLevel(7);
        startPoint = new GeoPoint(52.752, -7.953);
        mapController.setCenter(startPoint);
        directions = (TextView)findViewById(xyz.arrivealive.R.id.textView);
        clearButton = (Button)findViewById(xyz.arrivealive.R.id.button2);
        clearButton.setVisibility(View.GONE);
        directions.setVisibility(View.GONE);
        directions.setMovementMethod(new ScrollingMovementMethod());
        check = (CheckBox)findViewById(xyz.arrivealive.R.id.checkBox);
        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //is chkIos checked?
                if ((check).isChecked()) {
                    textStart.setVisibility(View.GONE);
                } else {
                    textStart.setVisibility(View.VISIBLE);
                }
            }
        });
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
        textStart.addTextChangedListener(new TextWatcher() {
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
        geocoder = new GeocoderPhoton(new Activity());
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
                directions.append(i + 1 + ":" + node.mInstructions.toString() + '\n');
            }
            directions.setVisibility(View.VISIBLE);
            clearButton.setVisibility(View.VISIBLE);
            map.invalidate();
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
            startString = textStart.getText().toString();
            Toast.makeText(context, destString, Toast.LENGTH_SHORT).show();
            if((check).isChecked()){
                point1 = _getLocation();
                if(point1.getLatitude() == -1.0 && point1.getLatitudeE6() == -1.0) {
                    Toast.makeText(context, "Failed to get GPS", Toast.LENGTH_SHORT).show();
                    valid = false;
                }
            }
        }
        protected ArrayList doInBackground(Object... params){
            if(valid == true) {
                List<Address> destBuffer1 = new ArrayList<>();
                List<Address> destBuffer2 = new ArrayList<>();
                try {
                    destBuffer1 = geocoder.getFromLocationName(startString, 1);
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
        directions.setText("");
        directions.setVisibility(View.GONE);
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
                        textStart.setAdapter(aAdapter);
                        aAdapter.notifyDataSetChanged();
                    }
                }
            });
            return null;
        }

    }
}