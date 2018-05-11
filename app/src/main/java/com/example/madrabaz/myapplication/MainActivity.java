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
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import be.tarsos.dsp.SpectralPeakProcessor;
import be.tarsos.dsp.io.android.AndroidFFMPEGLocator;

public class MainActivity extends AppCompatActivity {

    // ISSUES
    // Ses dosyasinin analizindan temiz sonuc gelmiyor, sadece bundan oturu sonuclar hatali
    // Ikinci oktavda sikintilar, tekk oktavla halletsek olabilir

    Util util;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		
		Context appContext = getApplicationContext();
        util = new Util(appContext);
        // Run this in another thread so ui thread relaxes maybe?
        //Context context = getApplicationContext();
        try {
            util.loadMakams();
        } catch (IOException e){
            e.printStackTrace();
        }

        // Test for util.fillEmptyIndices
//        List<Float> fakeNotes = new ArrayList<>();
//        fakeNotes.add(220f);
//        fakeNotes.add(0f);
//        fakeNotes.add(261.6f);
//        fakeNotes.add(0f);
//        fakeNotes.add(0f);
//        fakeNotes.add(349.228f);
//        fakeNotes.add(391.995f);
//        fakeNotes.add(440f);
//
//        System.out.println(fakeNotes.toString());
//
//        fakeNotes = util.fillEmptyIndices(fakeNotes, 220f);
//        System.out.println(fakeNotes.toString());
//
//        fakeNotes.clear();
//        fakeNotes.add(0f);
//        fakeNotes.add(0f);
//        fakeNotes.add(0f);
//        fakeNotes.add(293.7f);
//        fakeNotes.add(329f);
//        fakeNotes.add(0f);
//        fakeNotes.add(0f);
//        fakeNotes.add(0f);
//
//        System.out.println(fakeNotes.toString());
//        fakeNotes = util.fillEmptyIndices(fakeNotes, 220f);
//        System.out.println(fakeNotes.toString());

    }

    // TODO try catch for UnsupportedAudioFileException
    protected void button2Pressed(View view){
        /* Permission Ask */
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                1);
        /* End of Permission Ask */



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

                    /* For FFMPEG Libraries */
                    new AndroidFFMPEGLocator(this);
                    /* End Libraries */

                    //TODO Stringi adam akilli cek
                    final TextView textView = (TextView) findViewById(R.id.textSelected);
                    Uri uri = Uri.parse(textView.getText().toString());
                    String path = PathFinder.getPath(getApplicationContext(), uri);

                    //Map<Float, String> possibleMakams = util.getMakams(path);
                    Map<Float, String> possibleMakams = util.getMakams(textView.getText().toString());
                    //System.out.println(possibleMakams.toString() + "\n");

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(MainActivity.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    //protected void buttonPressed(View view){
    protected void buttonPressed(View view){
        Intent intent_upload = new Intent();
        intent_upload.setType("audio/*");
        intent_upload.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent_upload,1);
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){

        if(requestCode == 1){

            if(resultCode == RESULT_OK){

                //the selected audio.
                Uri uri = data.getData();
                //TextView textSelected = (TextView) findViewById(R.id.textSelected);
                TextView textSelected = findViewById(R.id.textSelected);
                assert uri != null;
                textSelected.setText(uri.toString());
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }



}
