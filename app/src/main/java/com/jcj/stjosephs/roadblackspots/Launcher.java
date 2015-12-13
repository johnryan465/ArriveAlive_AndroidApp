package com.jcj.stjosephs.roadblackspots;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;


/**
 * Created by john on 12/2/15.
 */

public class Launcher extends Activity {

    /** Duration of wait **/
    private final int SPLASH_DISPLAY_LENGTH = 1000;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.launcher);
        Button routePlannerButton = (Button) findViewById(R.id.button1);
        routePlannerButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Intent navIntent = new Intent(Launcher.this,RoutePlanner.class);
                Launcher.this.startActivity(navIntent);
                Launcher.this.finish();
            }
        });
        Button navigatorButton = (Button) findViewById(R.id.button2);
        navigatorButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Intent navIntent = new Intent(Launcher.this,Navigator.class);
                Launcher.this.startActivity(navIntent);
                Launcher.this.finish();
            }
        });

        /* New Handler to start the Menu-Activity
         * and close this Splash-Screen after some seconds.*/
    }
}