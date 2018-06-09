package com.diploma.sadwyn.zippyscanner.ui;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Button;

import com.diploma.sadwyn.zippyscanner.R;

public final class MainActivity extends android.support.v7.app.AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button startScan = findViewById(R.id.startCapture);
        startScan.setOnClickListener(view -> {
            Intent intent = new Intent(this, OcrCaptureActivity.class);
            startActivity(intent);
        });
    }
}
