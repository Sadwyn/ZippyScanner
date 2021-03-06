package com.diploma.sadwyn.zippyscanner.ui;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.ArrayRes;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.widget.Button;
import android.widget.CheckBox;

import com.diploma.sadwyn.zippyscanner.R;

import org.angmarch.views.NiceSpinner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MainActivity extends android.support.v7.app.AppCompatActivity {
    private NiceSpinner toLanguage;
    private NiceSpinner fromLanguage;
    private Map<String, String> toLanguagePair;
    private Map<String, String> fromLanguagePair;
    private boolean isTranslate;
    private Button startScan;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startScan = findViewById(R.id.startCapture);
        toLanguage = findViewById(R.id.lang_to);
        fromLanguage = findViewById(R.id.lang_from);

        toLanguagePair = getKeyValueFromStringArray(getApplicationContext(), R.array.target_languages);
        fromLanguagePair = getKeyValueFromStringArray(getApplicationContext(), R.array.source_languages);

        List<String> languageToDisplayNames = new ArrayList<>();
        List<String> languageFromDisplayNames = new ArrayList<>();

        setDisplayLanguage(languageToDisplayNames, toLanguagePair.entrySet());
        setDisplayLanguage(languageFromDisplayNames, fromLanguagePair.entrySet());

        toLanguage.attachDataSource(languageToDisplayNames);
        fromLanguage.attachDataSource(languageFromDisplayNames);

        startScan.setOnClickListener(view -> {

            String selectedLangTo = String.valueOf(toLanguage.getText().toString());
            String selectedLangFrom = String.valueOf(fromLanguage.getText().toString());
            Intent intent = new Intent(this, OcrCaptureActivity.class);
            intent.putExtra("toLanguage", toLanguagePair.get(selectedLangTo));
            intent.putExtra("fromLanguage", fromLanguagePair.get(selectedLangFrom));
            intent.putExtra("isTranslate", isTranslate);
            startActivity(intent);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dots_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        CheckBox checkBox = (CheckBox) menu.getItem(0).getActionView();
        checkBox.setPadding(0, 0, 20, 0);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isTranslate = isChecked;
            startScan.setText(isChecked ? "Start Translate" : "Start Recognize");
        });
        checkBox.setText(menu.getItem(0).getTitle());
        startScan.setText(checkBox.isChecked() ? "Start Translate" : "Start Recognize");
        return true;
    }

    private void setDisplayLanguage(List<String> languageNames, Set<Map.Entry<String, String>> entries) {
        for (Map.Entry<String, String> entry : entries) {
            languageNames.add(entry.getKey());
        }
        Collections.sort(languageNames);
    }

    private Map<String, String> getKeyValueFromStringArray(Context ctx, @ArrayRes int arrayRes) {
        String[] array = ctx.getResources().getStringArray(arrayRes);
        Map<String, String> result = new HashMap<>();
        for (String str : array) {
            String[] splitItem = str.split("\\|");
            result.put(splitItem[0], splitItem[1]);
        }
        return result;
    }
}
