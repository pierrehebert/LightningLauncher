/*
MIT License

Copyright (c) 2022 Pierre Hébert

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

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
