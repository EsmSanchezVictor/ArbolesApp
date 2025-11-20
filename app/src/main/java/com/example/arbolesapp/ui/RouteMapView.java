package com.example.arbolesapp.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.arbolesapp.utils.ExcelHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RouteMapView extends View {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint startPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Map<String, Paint> speciesPaintCache = new HashMap<>();

    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;

    private float scaleFactor = 1f;
    private float translateX = 0f;
    private float translateY = 0f;
    private float focusX;
    private float focusY;

    private final List<ExcelHelper.TreePoint> points = new ArrayList<>();

    public RouteMapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        linePaint.setColor(Color.DKGRAY);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(4f);
        linePaint.setPathEffect(new DashPathEffect(new float[]{12f, 12f}, 0f));

        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(28f);

        startPaint.setColor(Color.parseColor("#1B5E20"));
        startPaint.setStyle(Paint.Style.FILL);

        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        focusX = w / 2f;
        focusY = h / 2f;
    }

    public void setPoints(List<ExcelHelper.TreePoint> treePoints) {
        points.clear();
        if (treePoints != null) {
            points.addAll(treePoints);
        }
        resetTransformations();
        invalidate();
    }

    private void resetTransformations() {
        scaleFactor = 1f;
        translateX = 0f;
        translateY = 0f;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean scaleHandled = scaleDetector.onTouchEvent(event);
        boolean gestureHandled = gestureDetector.onTouchEvent(event);
        return scaleHandled || gestureHandled || super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (points.isEmpty()) {
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Sin puntos con GPS", getWidth() / 2f, getHeight() / 2f, textPaint);
            textPaint.setTextAlign(Paint.Align.LEFT);
            return;
        }

        canvas.save();
        canvas.translate(translateX, translateY);
        canvas.scale(scaleFactor, scaleFactor, focusX, focusY);

        float padding = 80f;
        float availableWidth = getWidth() - (2 * padding);
        float availableHeight = getHeight() - (2 * padding);

        if (availableWidth <= 0 || availableHeight <= 0) {
            canvas.restore();
            return;
        }

        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;

        for (ExcelHelper.TreePoint point : points) {
            minLat = Math.min(minLat, point.latitud);
            maxLat = Math.max(maxLat, point.latitud);
            minLon = Math.min(minLon, point.longitud);
            maxLon = Math.max(maxLon, point.longitud);
        }

        double latRange = Math.max(0.000001, maxLat - minLat);
        double lonRange = Math.max(0.000001, maxLon - minLon);

        Path path = new Path();
        boolean first = true;
        for (ExcelHelper.TreePoint point : points) {
            float x = (float) (((point.longitud - minLon) / lonRange) * availableWidth + padding);
            float y = (float) (availableHeight - ((point.latitud - minLat) / latRange) * availableHeight + padding);

            if (first) {
                path.moveTo(x, y);
                first = false;
            } else {
                path.lineTo(x, y);
            }
        }

        canvas.drawPath(path, linePaint);

        Paint startOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
        startOutline.setStyle(Paint.Style.STROKE);
        startOutline.setStrokeWidth(6f);
        startOutline.setColor(Color.WHITE);

        for (int i = 0; i < points.size(); i++) {
            ExcelHelper.TreePoint point = points.get(i);
            float x = (float) (((point.longitud - minLon) / lonRange) * availableWidth + padding);
            float y = (float) (availableHeight - ((point.latitud - minLat) / latRange) * availableHeight + padding);

            Paint circlePaint = getPaintForSpecies(point.especie);
            canvas.drawCircle(x, y, 14f, circlePaint);
            if (i == 0) {
                canvas.drawCircle(x, y, 20f, startOutline);
                canvas.drawCircle(x, y, 16f, startPaint);
                drawLabel(canvas, x, y - 26f, "Inicio", startPaint.getColor());
            }

            String label = point.especie + " | H: " + point.altura + " R: " + point.radioCopa;
            drawLabel(canvas, x + 18f, y + 6f, label, circlePaint.getColor());
        }

        canvas.restore();
    }

    private void drawLabel(Canvas canvas, float x, float y, String text, int color) {
        Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(Color.argb(180, 255, 255, 255));

        textPaint.setColor(Color.BLACK);
        textPaint.setTextAlign(Paint.Align.LEFT);

        float padding = 8f;
        float textWidth = textPaint.measureText(text);

        canvas.drawRoundRect(x - padding, y + textPaint.ascent() - padding,
                x + textWidth + padding, y + textPaint.descent() + padding,
                12f, 12f, backgroundPaint);

        textPaint.setColor(color);
        canvas.drawText(text, x, y, textPaint);
        textPaint.setColor(Color.BLACK);
    }

    private Paint getPaintForSpecies(String species) {
        String key = species == null || species.trim().isEmpty() ? "_" : species.trim();
        if (speciesPaintCache.containsKey(key)) {
            return speciesPaintCache.get(key);
        }

        float[] hsv = new float[]{Math.abs(key.hashCode()) % 360, 0.65f, 0.95f};
        int color = Color.HSVToColor(220, hsv);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        speciesPaintCache.put(key, paint);
        return paint;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 8f));
            focusX = detector.getFocusX();
            focusY = detector.getFocusY();
            invalidate();
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            translateX -= distanceX;
            translateY -= distanceY;
            invalidate();
            return true;
        }
    }
}