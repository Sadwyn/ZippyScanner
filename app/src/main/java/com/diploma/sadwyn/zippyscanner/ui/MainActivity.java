package com.diploma.sadwyn.zippyscanner.ui;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.diploma.sadwyn.zippyscanner.R;

import org.angmarch.views.NiceSpinner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MainActivity extends android.support.v7.app.AppCompatActivity {
    private NiceSpinner toLanguage;
    private Map<String, String> toLanguagePair;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button startScan = findViewById(R.id.startCapture);
        toLanguage = findViewById(R.id.lang_to);

        toLanguagePair = getKeyValueFromStringArray(getApplicationContext());



        List<String> countryNames = new ArrayList<>();

        for (Map.Entry<String, String> entry : toLanguagePair.entrySet()) {
            countryNames.add(entry.getKey());
        }
        Collections.sort(countryNames);

        toLanguage.attachDataSource(countryNames);

        startScan.setOnClickListener(view -> {
            String selectedLangTo = String.valueOf(toLanguage.getText().toString());
            Intent intent = new Intent(this, OcrCaptureActivity.class);
            intent.putExtra("toLanguage", toLanguagePair.get(selectedLangTo));
            startActivity(intent);
        });
    }

    private Map<String, String> getKeyValueFromStringArray(Context ctx) {
        String[] array = ctx.getResources().getStringArray(R.array.target_languages);
        Map<String, String> result = new HashMap<>();
        for (String str : array) {
            String[] splittedItem = str.split("\\|");
            result.put(splittedItem[0], splittedItem[1]);
        }
        return result;
    }
}
