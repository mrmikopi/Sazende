package com.example.madrabaz.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.HashMap;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AndroidAudioPlayer;
import be.tarsos.dsp.io.android.AndroidFFMPEGLocator;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    protected void button2Pressed(View view){
        /* Permission Ask */
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                1);
        /* End of Permission Ask */

        /* For FFMPEG Libraries */
        new AndroidFFMPEGLocator(this);
        /* End Libraries */

        //TODO Stringi adam akilli cek
        final TextView textView = (TextView) findViewById(R.id.textSelected);
        Uri uri = Uri.parse(textView.getText().toString());
        final String path = PathFinder.getPath(getApplicationContext(), uri);

        // TODO Duzgun bi path check yap, null check filan
        assert path != null;
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Dosya wav ise pipe atma
                AudioDispatcher dispatcher = AudioDispatcherFactory.fromPipe(path,22500,2500,1250);
                PitchDetectionHandler pdh = new PitchDetectionHandler() {
                    final int[] mostUsed = new int[10];
                    @Override
                    public void handlePitch(PitchDetectionResult result, AudioEvent e) {

                        // TODO Inte yuvarlama, sakincali.
                        final int pitchInHz = Math.round(result.getPitch());
                        final double timeStamp = e.getSamplesProcessed();


                        // TODO Mutable Int daha mi iyi yoksa normal int mi, zaman kiyasi yap
                        // TODO Yukarida tanimla bunu.
                        final HashMap<Integer, MutableInt> occurances = new HashMap<>();
                        // TODO Alttaki sparseArray'i dene performans kiyaslamasi.
                        // final SparseIntArray occuranceMap = new SparseIntArray();

                        final MutableInt count = occurances.get(pitchInHz);
                        if (count == null) {
                            occurances.put(pitchInHz, new MutableInt());
                        }
                        else {
                            count.increment();
                            occurances.put(pitchInHz, count);
                        }

                        // TODO Burayi disarida yapacaksin cunku her seferinde calismasini istemiyorsun, bitince calisacak.
                        // Peak exxtractor filan kulan.
                        for(HashMap.Entry<Integer, MutableInt> entry1: occurances.entrySet()) {
                            Integer key1 = entry1.getKey();
                            int hash1 = System.identityHashCode(key1);
                            MutableInt value1 = entry1.getValue();
                            for (HashMap.Entry<Integer, MutableInt> entry2 : occurances.entrySet()) {
                                Integer key2 = entry2.getKey();
                                if (key1 > System.identityHashCode(key2)) continue;

                                MutableInt value2 = entry1.getValue();
                                switch (Integer.compare(value1.getValue(), value2.getValue())) {
                                    case 1:
                                        break;
                                }
                            }
                        }
                        /*
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TextView text = (TextView) findViewById(R.id.textView);
                                TextView text2 = (TextView) findViewById(R.id.textSelected);
                                text.setText("" + pitchInHz);
                                text2.setText("" + timeStamp);

                        }
                        });
                    */
                    }
                };
                // TODO Buraya direkt spectralpeakprocessor atacaksin.
                AudioProcessor p = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22500, 2500, pdh);
                dispatcher.addAudioProcessor(p);
                dispatcher.run();
            }
        }).start();
        // TODO Threadi kapatacaksin veya diger activity e gecince threadin durumunu dusuneceksin.

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
                TextView textSelected = (TextView) findViewById(R.id.textSelected);
                assert uri != null;
                textSelected.setText(uri.toString());
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /* ASDLJASDSALJDASDLJASDSALJDASDLJASDSALJDASDLJASDSALJDASDLJASDSALJDASDLJASDSALJD */

}
