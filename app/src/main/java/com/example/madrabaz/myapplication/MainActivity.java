package com.example.madrabaz.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import java.io.IOException;

import be.tarsos.dsp.io.android.AndroidFFMPEGLocator;

public class MainActivity extends AppCompatActivity {

    String uri;
    Util util;
    Context appContext;
    public static final String EXTRA_MESSAGE = "com.example.sazende.MESSAGE";
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        getSupportActionBar().setTitle("Your Activity Title"); // for set actionbar title
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        appContext = getApplicationContext();
        util = new Util(appContext);
        try {
            util.loadMakams();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    // TODO try catch for UnsupportedAudioFileException
    protected void button2Pressed(View view) {

        /* For FFMPEG Libraries */
        new AndroidFFMPEGLocator(this);
        /* End Libraries */
        if (uri != null) {
            //textView.setVisibility(View.VISIBLE);
            String possibleMakam = util.getMakams(this.uri);
            Intent intent = new Intent(this, ResultActivity.class);
            intent.putExtra(EXTRA_MESSAGE, possibleMakam);
            startActivity(intent);
        } else {
            Toast.makeText(MainActivity.this, R.string.no_song_press, Toast.LENGTH_SHORT).show();
        }
    }




    /* Permission method */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Intent intent_upload = new Intent();
                    intent_upload.setType("audio/*");
                    intent_upload.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(intent_upload,1);
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(MainActivity.this, R.string.perm_deny, Toast.LENGTH_SHORT).show();
                }
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    //protected void buttonPressed(View view){
    protected void buttonPressed(View view){

        /* Permission Ask */
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                1);
        /* End of Permission Ask */


    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){

        if(requestCode == 1){

            if(resultCode == RESULT_OK){

                //the selected audio.
                Uri uri1 = data.getData();
                TextView textSelected = (TextView) findViewById(R.id.textSelected);
                assert uri1 != null;
                this.uri = uri1.toString();
                textSelected.setText(R.string.song_selected);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }



}
