package com.example.retroreads;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import androidx.annotation.NonNull;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import java.security.MessageDigest;

public class CircularBorderTransformation extends BitmapTransformation {

    private final int borderWidth; // Largura da borda
    private final int borderColor; // Cor da borda

    public CircularBorderTransformation(Context context, int borderWidthDp, int borderColor) {
        this.borderWidth = (int) (context.getResources().getDisplayMetrics().density * borderWidthDp);
        this.borderColor = borderColor;
    }

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
        return addCircularBorder(toTransform, borderWidth, borderColor);
    }

    private Bitmap addCircularBorder(Bitmap bitmap, int borderWidth, int borderColor) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        // Desenha a imagem original
        canvas.drawBitmap(bitmap, 0, 0, null);

        // Configura a Paint para a borda
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(borderWidth);
        paint.setColor(borderColor);

        // Calcula o raio do círculo
        float radius = Math.min(width, height) / 2f - borderWidth / 2f;

        // Desenha o círculo da borda
        canvas.drawCircle(width / 2f, height / 2f, radius, paint);

        return output;
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update(("CircularBorderTransformation" + borderWidth + borderColor).getBytes());
    }
}