package net.pierrox.lightning_launcher.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Surface;

import net.pierrox.lightning_launcher.configuration.PageConfig;

import java.io.File;

public class NativeImage {
	public static final int NO_KEY = -1;
	
	private static native void nativeInit();
	public static native void nativeClearImages();
	public static native boolean nativeHasImage(int key);
    public static native int nativeGetImageWidth(int id);
    public static native int nativeGetImageHeight(int id);
    public static native void nativeLoadImage(int id, Bitmap out);
	public static native void nativeSetImage(int key, Bitmap bitmap);
	public static native void nativeDeleteImage(int key);
	private static native void nativeDrawImageWithColorOnSurface(int key1, int color, int scaleType, Surface surface);

	private static boolean sAvailable;

	public static void init(Context context) {
        sAvailable = false;
		try {
			if(android.os.Build.VERSION.SDK_INT>=9) {
                System.loadLibrary("ll");
				nativeInit();
				sAvailable = true;
			}
		} catch (Throwable e1) {
            sAvailable = false;
		}
	}
	
	public static boolean isAvailable() {
		return sAvailable;
	}
	
	public static boolean hasImage(int key) {
		try { return nativeHasImage(key); } catch(UnsatisfiedLinkError e) { return false; }
	}
	
	public static void loadImage(int key, File file) {
		if(file.exists()) {
			Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
			if (bitmap != null && bitmap.getConfig() == Bitmap.Config.ARGB_8888) {
				nativeSetImage(key, bitmap);
				bitmap.recycle();
			}
		}
	}
	
	public static void deleteImage(int key) {
		try { nativeDeleteImage(key); } catch(UnsatisfiedLinkError e) { }
	}
	
	public static void drawImageWithColorOnSurface(int key, int color, PageConfig.ScaleType scaleType, Surface surface) {
		try { nativeDrawImageWithColorOnSurface(key, color, scaleType.ordinal(), surface); } catch(UnsatisfiedLinkError e) { }
	}
	
	public static int getWallpaperKey(int page) {
		return page | 0x70000000;
	}
}
