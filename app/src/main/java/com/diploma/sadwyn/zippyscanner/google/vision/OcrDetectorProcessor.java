/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diploma.sadwyn.zippyscanner.google.vision;

import android.util.Log;
import android.util.SparseArray;

import com.diploma.sadwyn.zippyscanner.App;
import com.diploma.sadwyn.zippyscanner.BuildConfig;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * A very simple Processor which gets detected TextBlocks and adds them to the overlay
 * as OcrGraphics.
 */
public class OcrDetectorProcessor implements Detector.Processor<TextBlock> {

    private GraphicOverlay<OcrGraphic> mGraphicOverlay;
    private String toLang;
    private String fromLang;
    public static boolean isStopped = true;
    public boolean isTranslate = false;
    public static List<String> textToCopy = new ArrayList<>();

    public OcrDetectorProcessor(GraphicOverlay<OcrGraphic> ocrGraphicOverlay, String toLang, String fromLang, boolean isTranslate) {
        mGraphicOverlay = ocrGraphicOverlay;
        this.toLang = toLang;
        this.fromLang = fromLang;
        this.isTranslate = isTranslate;
    }

    /**
     * Called by the detector to deliver detection results.
     * If your application called for it, this could be a place to check for
     * equivalent detections by tracking TextBlocks that are similar in location and content from
     * previous frames, or reduce noise by eliminating TextBlocks that have not persisted through
     * multiple detections.
     */
    @Override
    public void receiveDetections(Detector.Detections<TextBlock> detections) {
        if (!isStopped) {
            mGraphicOverlay.clear();
            textToCopy.clear();
            SparseArray<TextBlock> items = detections.getDetectedItems();
            for (int i = 0; i < items.size(); ++i) {
                TextBlock item = items.valueAt(i);
                if (item != null && item.getValue() != null) {
                    Log.d("OcrDetectorProcessor", "Text detected! " + item.getValue());
                    for (Text text : item.getComponents()) {
                        if(isTranslate) {
                            App.getApi().translate(text.getValue(), toLang, "text", fromLang, "nmt", BuildConfig.APIKEY)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(translationResult -> {
                                        OcrGraphic graphic = new OcrGraphic(mGraphicOverlay, text, translationResult.getData().getTranslations().get(0).getTranslatedText());
                                        textToCopy.add(translationResult.getData().getTranslations().get(0).getTranslatedText());
                                        mGraphicOverlay.add(graphic);
                                    }, Throwable::printStackTrace);
                        }
                        else {
                            textToCopy.add(text.getValue());
                            OcrGraphic graphic = new OcrGraphic(mGraphicOverlay, text);
                            mGraphicOverlay.add(graphic);
                        }
                    }
                }
            }
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void release() {
        mGraphicOverlay.clear();
    }
}
