package com.example.accidentsystem;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class preparation_plan extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preparation_plan);
        Button buttonpolicer = findViewById(R.id.buttonpolice);
        buttonpolicer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view){
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tel:112"));
                startActivity(intent);
            }
        });

        Button button911 = findViewById(R.id.buttonnineoneone);
        button911.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tel:119"));
                startActivity(intent);
            }
        });

        Button buttonhy = findViewById(R.id.buttonhy);
        buttonhy.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tel:15885656"));
                startActivity(intent);
            }
        });
        Button buttonkb = findViewById(R.id.buttonkb);
        buttonkb.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tel:15440114"));
                startActivity(intent);
            }
        });
        Button buttoncr = findViewById(R.id.buttoncr);
        buttoncr.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tel:1566-0300"));
                startActivity(intent);
            }
        });
        Button buttondb = findViewById(R.id.buttondb);
        buttondb.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tel:1588-0100"));
                startActivity(intent);
            }
        });
        Button buttonss = findViewById(R.id.buttonss);
        buttonss.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tel:1588-5114"));
                startActivity(intent);
            }
        });
        Button buttonaxa = findViewById(R.id.buttonaxa);
        buttonaxa.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tel:1566-2266"));
                startActivity(intent);
            }
        });
        Button buttonelse = findViewById(R.id.elsecompany);
        buttonelse.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://m.naver.com"));
                startActivity(intent);
            }
        });
    }
}