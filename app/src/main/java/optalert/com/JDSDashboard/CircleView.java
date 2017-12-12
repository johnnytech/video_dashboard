package optalert.com.JDSDashboard;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;

public class CircleView extends View {
    private static final String COLOR_HEX = "#E74300";
    private static final String COLOR_GREEN = "#00FF00";
    private final Paint drawPaint;
    private float circleWidth = 5.0f;
    private float size;

    public CircleView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        drawPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeWidth(circleWidth);
        drawPaint.setColor(Color.parseColor(COLOR_HEX));
        drawPaint.setAntiAlias(true);
        setOnMeasureCallback();
    }

    public void setColor(int color) {
        if (color == Color.GREEN) {
            drawPaint.setColor(color);
        } else {
            drawPaint.setColor(Color.parseColor(COLOR_HEX));
        }
        invalidate();
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(size, size, size - circleWidth, drawPaint);
    }

    private void setOnMeasureCallback() {
        ViewTreeObserver vto = getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                removeOnGlobalLayoutListener(this);
                size = getMeasuredWidth() / 2;
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void removeOnGlobalLayoutListener(ViewTreeObserver.OnGlobalLayoutListener listener) {
        getViewTreeObserver().removeOnGlobalLayoutListener(listener);
    }
}
