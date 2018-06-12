
package com.diploma.sadwyn.zippyscanner.google.vision;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.google.android.gms.vision.text.Text;

public class OcrGraphic extends GraphicOverlay.Graphic {
    private int mId;

    private static final int TEXT_COLOR = Color.WHITE;

    private static Paint sTextPaint;
    private final Text mText;
    private final String textValue;

    OcrGraphic(GraphicOverlay overlay, Text mText, String textValue) {
        super(overlay);
        this.mText = mText;
        this.textValue = textValue;


        if (sTextPaint == null) {
            sTextPaint = new Paint();
            sTextPaint.setColor(TEXT_COLOR);
            sTextPaint.setTextSize(50.0f);
        }

        // Redraw the overlay, as this graphic has been added.
        postInvalidate();
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        this.mId = id;
    }

    public Text getText() {
        return mText;
    }

    public boolean contains(float x, float y) {
        if (mText == null) {
            return false;
        }
        RectF rect = new RectF(mText.getBoundingBox());
        rect.left = translateX(rect.left);
        rect.top = translateY(rect.top);
        rect.right = translateX(rect.right);
        rect.bottom = translateY(rect.bottom);
        return (rect.left < x && rect.right > x && rect.top < y && rect.bottom > y);
    }

    @Override
    public void draw(Canvas canvas) {
        if (mText == null) {
            return;
        }
        float left = translateX(mText.getBoundingBox().left);
        float bottom = translateY(mText.getBoundingBox().bottom);

        canvas.drawText(textValue, left, bottom, sTextPaint);
    }
}
