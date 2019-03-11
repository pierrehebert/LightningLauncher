package net.pierrox.lightning_launcher.script.api;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.NinePatch;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.data.FileUtils;
import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.util.AnimationDecoder;
import net.pierrox.lightning_launcher.views.Graphics;
import net.pierrox.lightning_launcher.views.MyNinePatchDrawable;
import net.pierrox.lightning_launcher.views.SharedAsyncGraphicsDrawable;
import net.pierrox.android.lsvg.SvgDrawable;

import org.mozilla.javascript.Scriptable;

import java.io.File;
import java.lang.reflect.Field;

/**
 * Utility class to manipulate images (icons, backgrounds, etc.).
 * This is the base class for various kind of images. Each image type (bitmap, nine patch, animation, scripted) have their own set of features.
 * Please refer to their respective documentation.
 */
public abstract class Image {
    protected Lightning mLightning;
    protected net.pierrox.lightning_launcher.data.Item mSourceItem;
    protected File mSourceFile;
    protected Drawable mSourceDrawable;
    private boolean mModified;

    /**
     * @hide
     */
    public Image(Lightning lightning) {
        mLightning = lightning;
    }

    /**
     * @hide
     */
    /*package*/ boolean isModified() {
        return mModified;
    }

    /**
     * @hide
     */
    /*package*/ void setModified() {
        mModified = true;
    }

    /**
     * Return the image width
     */
    public abstract int getWidth();

    /**
     * Return the image height
     */
    public abstract int getHeight();

    /**
     * Delete this image from persistent storage (if loaded from a file).
     */
    public void delete() {
        if(mSourceFile != null) {
            mSourceFile.delete();
            mSourceItem.notifyChanged();
        }
    }

    /**
     * @deprecated use #getType instead
     */
    public boolean isNinePatch() {
        return this.getClass() == ImageNinePatch.class;
    }

    /**
     * Return the type of this image.
     * @return one of BITMAP, NINE_PATCH, ANIMATION, SVG
     */
    public String getType() {
        Class<? extends Image> cls = getClass();
        if(cls == ImageBitmap.class) {
            return "BITMAP";
        } else if(cls == ImageNinePatch.class) {
            return "NINE_PATCH";
        } else if(cls == ImageSvg.class) {
            return "SVG";
        } else {
            return "ANIMATION";
        }
    }

    /**
     * @hide
     */
    public void setSource(Item item, File file, Drawable drawable) {
        mSourceItem = item;
        mSourceFile = file;
        mSourceDrawable = drawable;
    }

    /**
     * @hide
     */
	/*package*/ File getSourceFile() {
        return mSourceFile;
    }

    /**
     * @hide
     */
    public abstract Drawable toDrawable();


    private static Field sNinePatchDrawableNinePatch;

    /**
     * @hide
     */
    public static Image fromDrawable(Lightning ll, Drawable d, Item sourceItem, File sourceFile) {
        if(d == null) {
            return null;
        }

        Image image = null;

        if(d instanceof BitmapDrawable) {
            image = new ImageBitmap(ll, ((BitmapDrawable) d).getBitmap());
        } else if(d instanceof MyNinePatchDrawable) {
            image = new ImageNinePatch(ll, ((MyNinePatchDrawable) d).getNinePatch());
        } else if(d instanceof NinePatchDrawable) {
            // really awful hack
            if(sNinePatchDrawableNinePatch == null) {
                try {
                    sNinePatchDrawableNinePatch = NinePatchDrawable.class.getDeclaredField("mNinePatch");
                    sNinePatchDrawableNinePatch.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    // pass
                }
            }
            try {
                NinePatch ninePatch = (NinePatch) sNinePatchDrawableNinePatch.get(d);
                if(ninePatch != null) {
                    image = new ImageNinePatch(ll, ninePatch);
                }
            } catch (Exception e) {
                // pass
            }

        } else if(d instanceof SvgDrawable) {
            image = new ImageSvg(ll, (SvgDrawable)d);
        } else if(d instanceof SharedAsyncGraphicsDrawable) {
            SharedAsyncGraphicsDrawable sd = (SharedAsyncGraphicsDrawable) d;
            if(sd.needToLoadGraphics()) {
                sd.loadGraphicsSync();
            }
            switch(sd.getType()) {
                case SharedAsyncGraphicsDrawable.TYPE_BITMAP:
                    Bitmap bitmap = sd.getBitmap();
                    image = new ImageBitmap(ll, bitmap.copy(bitmap.getConfig(), true));
                    break;

                case SharedAsyncGraphicsDrawable.TYPE_NINE_PATCH:
                    image = new ImageNinePatch(ll, sd.getNinePatch());
                    break;

                case SharedAsyncGraphicsDrawable.TYPE_ANIMATED_GIF:
                    image = new ImageAnimation(ll, sd);
                    break;

                case SharedAsyncGraphicsDrawable.TYPE_SVG:
                    image = new ImageSvg(ll, sd.getSvgDrawable());
                    break;

                case SharedAsyncGraphicsDrawable.TYPE_SCRIPT:
                    image = new ImageScript(ll, sd.getScriptObject(), sd.getIntrinsicWidth(), sd.getIntrinsicHeight());
                    break;

                default:
                    image = null;
            }
        }

        if(image != null) {
            image.setSource(sourceItem, sourceFile, d);
        }

        return image;
    }

    private static Lightning getLightning() {
        // TODO should get this data from the current script context / scope
        return LLApp.get().getAppEngine().getScriptExecutor().getLightning();
    }

    /**
     * Create an icon using text. This function is intended to be used with an icon font to create high res icons.
     * @param code character to use
     * @param size icons are square : width = height = size
     * @param fgColor foreground color
     * @param bgColor background color
     * @param typeface typeface to use, use the built-in icon typeface if null
     */
    public static ImageBitmap createTextIcon(String code, int size, int fgColor, int bgColor, Typeface typeface) {
        if(typeface == null) {
            typeface = LLApp.get().getIconsTypeface();
        }
        try {
            Bitmap bmp = Utils.createIconFromText(size, code, fgColor, bgColor, typeface);
            return new ImageBitmap(getLightning(), bmp);
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    /**
     * Create a blank new image of the specified size. Pixel format is always ARGB 8888.
     * Take care when creating images since it can rapidly exhaust memory and lead to a crash.
     * @return can return null if not enough memory
     */
    public static ImageBitmap createImage(int width, int height) {
        try {
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            return new ImageBitmap(getLightning(), bmp);
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    /**
     * Create an image from the specified file.
     * @param path path of the image
     * @return can return null if an image cannot be read from the file (not an image or not enough memory)
     */
    public static Image createImage(String path) {
        File file = new File(path);
        Graphics graphics = Utils.loadGraphics(file);
        if(graphics != null) {
            Bitmap bitmap = graphics.getBitmap();
            if(bitmap != null && !NinePatch.isNinePatchChunk(bitmap.getNinePatchChunk())) {
                // directly create an ImageBitmap to avoid one bitmap copy because of SharedAsyncGraphicsDrawable
                Image image = new ImageBitmap(getLightning(), bitmap);
                image.setSource(null, file, null);
                return image;
            } else {
                SharedAsyncGraphicsDrawable d = new SharedAsyncGraphicsDrawable(graphics, true);
                return Image.fromDrawable(getLightning(), d, null, file);
            }
        }

        return null;
    }

    /**
     * Create an image from a package and a resource name.
     * For instance:<code>Image.createImage("net.pierrox.lightning_launcher_extreme", "icon")</code>
     * The density used is either the one given by ActivityManager.getLauncherLargeIconDensity if available, or the current one.
     * @param pkg name of the package, use "android" to access system resources.
     * @param name name of the drawable resource
     * @return can return null if an image cannot be read from the package (unknown package, wrong resource name or not enough memory)
     */
    public static Image createImage(String pkg, String name) {
        Drawable drawable = null;
        try {
            Context context = getLightning().getEngine().getContext();
            android.content.Context remote_context = context.createPackageContext(pkg, 0);
            Resources rsrc = remote_context.getResources();
            int id = rsrc.getIdentifier(name, "drawable", pkg);
            if(id != 0) {
                drawable = Utils.decodeDrawableResource(rsrc, id);
                if(drawable != null) {
                    // save the resource to a file, so that its exact content can be persisted later (needed for nine patches)
                    File drawablesDir = FileUtils.getCachedDrawablesDir(context);
                    drawablesDir.mkdirs();
                    File cachedDrawableFile = new File(drawablesDir, String.valueOf(pkg + "_" + id));
                    Utils.copyResourceToFile(rsrc, id, cachedDrawableFile);

                    return Image.fromDrawable(getLightning(), drawable, null, cachedDrawableFile);
                }
            }
        } catch (Throwable e) {
        }

        return null;
    }

    /**
     * Create an image whose content is drawn using a script.
     * This can be used for memory efficient graphics and animations.
     * Please refer to {@link ImageScript} for the documentation on how to use it.
     *
     * @param object the set of functions
     * @param width the prefered image width, use -1 for as big as possible
     * @param height the prefered image height, use -1 for as big as possible
     */
    public static ImageScript createImage(Scriptable object, int width, int height) {
        return new ImageScript(getLightning(), object, width, height);
    }

    /**
     * Create a blank animation: frames are created fully transparent and need to be drawn.
     * Notes: animations created this way are memory expensive and cannot be persisted (yet). This means that Shortcut.setCustomIcon() wont't work, but Shortcut.setImage() will.
     * @param width image width
     * @param height image height
     * @param count number of frames to allocate
     * @param duration default frame duration
     * @param loopCount initial number of loops to play, use 0 for infinite
     * @return an animation or null in case of error (most likely out of memory)
     */
    public static ImageAnimation createAnimation(int width, int height, int count, int duration, int loopCount) {
        try {
            AnimationDecoder decoder = new AnimationDecoder(width, height, count, duration, loopCount);
            SharedAsyncGraphicsDrawable sd = new SharedAsyncGraphicsDrawable(new Graphics(decoder, width, height), false);
            ImageAnimation image = new ImageAnimation(getLightning(), sd);
            image.setSource(null, null, sd);
            image.setModified();
            return image;
        } catch (Throwable t) {
            return null;
        }
    }
}
