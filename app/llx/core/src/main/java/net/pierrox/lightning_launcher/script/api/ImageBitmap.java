package net.pierrox.lightning_launcher.script.api;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.engine.*;
import net.pierrox.lightning_launcher.engine.Screen;
import net.pierrox.lightning_launcher.views.SharedAsyncGraphicsDrawable;
import net.pierrox.lightning_launcher.views.item.ItemView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Wraps a static bitmap.
 * Such an image can be loaded from file, created using {@link Image#createImage(int, int)}, or obtained from items icons.
 *
 * An instance of this object can be created with {@link Image#createImage(int, int)} or {@link Image#createTextIcon(String, int, int, int, Typeface)}; or retrieved with any function that returns an {@link Image} when that image is an ImageBitmap; or with {@link ImageAnimation#getFrameImage(int)} or {@link net.pierrox.lightning_launcher.script.api.screen.ActivityScreen#cropImage(ImageBitmap, boolean)}.
 */
public class ImageBitmap extends Image {
    private Bitmap mBitmap;
    private ImageAnimation mSourceAnimation;

    /**
     * @hide
     */
    public ImageBitmap(Lightning lightning, Bitmap bitmap) {
        super(lightning);

        mBitmap = bitmap;
    }

    /**
     * @hide
     */
    public ImageBitmap(Lightning lightning, Bitmap bitmap, ImageAnimation sourceAnimation) {
        this(lightning, bitmap);

        mSourceAnimation = sourceAnimation;
    }

    /**
     * Retrieves the Android Bitmap associated with this image.
     */
    public Bitmap getBitmap() {
        return mBitmap;
    }

    /**
     * @hide
     */
    @Override
    public Drawable toDrawable() {
        return new BitmapDrawable(mBitmap);
    }

    @Override
    public int getWidth() {
        return mBitmap.getWidth();
    }

    @Override
    public int getHeight() {
        return mBitmap.getHeight();
    }

    /**
     * Draw free content on this icon. The image must be a bitmap, it cannot work with nine patch.
     * In addition to the Canvas class, the following Android classes are currently available
     * <ul>
     *     <li>Color</li>
     *     <li>LinearGradient</li>
     *     <li>Matrix</li>
     *     <li>Paint</li>
     *     <li>Path</li>
     *     <li>PorterDuff</li>
     *     <li>RadialGradient</li>
     *     <li>RectF</li>
     *     <li>Region</li>
     *     <li>Shader</li>
     *     <li>SweepGradient</li>
     *     <li>Typeface</li>
     * </ul>
     * @see <a href="http://developer.android.com/reference/android/graphics/Canvas.html">http://developer.android.com/reference/android/graphics/Canvas.html</a>
     * @return a canvas used to draw on this icon
     */
    public Canvas draw() {
        if(mSourceAnimation != null) {
            mSourceAnimation.setModified();
        }
        setModified();
        return new Canvas(getBitmap());
    }

    /**
     * Save the modified image (with #draw) to file, and update the associated item if any.
     * Such an image is typically retrieved through
     * <ul>
     *     <li>{@link net.pierrox.lightning_launcher.script.api.Shortcut#getDefaultIcon()}</li>
     *     <li>{@link net.pierrox.lightning_launcher.script.api.Shortcut#getCustomIcon()}</li>
     *     <li>{@link Image#createImage(String)}</li>
     * Calling this method on an image without source file will do nothing.
     */
    public void save() {
        Bitmap bmp = getBitmap();
        if(bmp != null && mSourceFile != null) {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(mSourceFile);
                bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            } catch (IOException e) {
                // pass
            } finally {
                if (fos != null) try {
                    fos.close();
                } catch (Exception e) {
                }
            }
            if(mSourceItem != null) {
                mSourceItem.notifyChanged();
            }
        }
    }

    /**
     * Update the item using this image. This method only applies to images loaded in RAM, not file backed images.
     * This method need to be called in order to refresh the object on the screen after the image has been modified through #draw.
     */
    public void update() {
        if(mSourceItem != null && mSourceDrawable != null) {
            if(mSourceDrawable.getClass() == SharedAsyncGraphicsDrawable.class) {
                ((SharedAsyncGraphicsDrawable)mSourceDrawable).setBitmap(mBitmap);
            }
            net.pierrox.lightning_launcher.data.Shortcut s = (net.pierrox.lightning_launcher.data.Shortcut) mSourceItem;
            for (Screen screen : LLApp.get().getScreens()) {
                ItemView[] ivs = screen.getItemViewsForItem(s);
                for (ItemView iv : ivs) {
                    if(iv.isInitDone()) {
                        iv.getSensibleView().invalidate();
                    }
                }
            }
        }
    }

    /**
     * Save the underlying bitmap to file
     * @param path where to store the file
     * @param quality only useful for JPEG (0-100)
     * @return true if the operation succeeded
     */
    public boolean saveToFile(String path, Bitmap.CompressFormat format, int quality) {
        File file = new File(path);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            mBitmap.compress(format, quality, fos);
            return true;
        } catch(IOException e) {
            file.delete();
            return false;
        } finally {
            if(fos != null) try { fos.close(); } catch (IOException e) {}
        }
    }
}
