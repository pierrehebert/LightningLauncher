package net.pierrox.lightning_launcher.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.widget.ImageView;

import net.pierrox.lightning_launcher.configuration.PageConfig;

import java.io.File;

public class ImageWallpaperView extends ImageView implements WallpaperView {
    public ImageWallpaperView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void configure(int page, File file, int tint_color, PageConfig.ScaleType scaleType) {
        Bitmap bitmap = null;
        try {
            if (file.exists()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inMutable = true;
                bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                new Canvas(bitmap).drawColor(tint_color, PorterDuff.Mode.SRC_OVER);
            } else {
                bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                bitmap.eraseColor(tint_color);

            }
        } catch(Throwable e) {
            // pass
        }

        setScaleType(scaleType == PageConfig.ScaleType.CENTER ? ScaleType.CENTER : ScaleType.FIT_XY);
        setImageBitmap(bitmap);
    }
}
