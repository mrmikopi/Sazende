package com.example.madrabaz.myapplication;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);


        Intent intent = getIntent();
        String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        TextView textView = findViewById(R.id.textView2);
        String concated = "Song makam is probably\n" + message;
        textView.setText(concated);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
