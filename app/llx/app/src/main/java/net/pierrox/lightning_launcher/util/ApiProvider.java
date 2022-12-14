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

package net.pierrox.lightning_launcher.util;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.DisplayMetrics;
import android.view.View.MeasureSpec;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.activities.MultiPurposeTransparentActivity;
import net.pierrox.lightning_launcher.data.*;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.views.ItemLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class ApiProvider extends ContentProvider {
	public static final Uri CONTENT_URI=Uri.parse("content://net.pierrox.lightning_launcher_extreme.api/");
	public static final String PREVIEW_FILE="screenshot.jpg";
	
	private static final String PATH_PREVIEW = "preview";
	private static final String PATH_RELOAD_APP_DRAWER = "rad";
	private static final String PATH_RESET_5SEC_DELAY = "reset5secs";

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
        if(PATH_RESET_5SEC_DELAY.equals(uri.getLastPathSegment()) /*&& getCallingPackage().equals("")*/) {
            MultiPurposeTransparentActivity.startForReset5secDelay(getContext());
            return 1;
        } else {
            return 0;
        }
	}

	@Override
	public String getType(Uri uri) {
		return "image/png";
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public boolean onCreate() {
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		String[] path_elems = uri.getPath().split("/");
		if(PATH_PREVIEW.equals(path_elems[1])) {
			MatrixCursor c = new MatrixCursor(projection, 1);
			int l = projection.length;
			Object[] values = new Object[l];
			for(int i=0; i<l; i++) {
				if(OpenableColumns.DISPLAY_NAME.equals(projection[i])) {
					values[i] = "lightning_launcher_theme.jpg";
				}
			}
			c.addRow(values);
			return c;
		} else if(PATH_RELOAD_APP_DRAWER.equals(path_elems[1])) {
			Utils.refreshAppDrawerShortcuts(LLApp.get().getAppEngine(), new Handler());
			MatrixCursor c = new MatrixCursor(projection, 1);
			return c;
		}
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	synchronized public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
		String[] path_elems = uri.getPath().split("/");
		
		if(PATH_PREVIEW.equals(path_elems[1])) {
			Context context = getContext();
			String theme_id = path_elems[2];
			PreviewSize preview_size = PreviewSize.valueOf(path_elems[3]);
			int page = Integer.parseInt(path_elems[4]);
			
			boolean use_system_wallpaper = false;
			File theme_dir = FileUtils.getExternalThemeDir(theme_id);
			if(!theme_dir.exists()) {
				theme_dir = LLApp.get().getAppEngine().getBaseDir();
				use_system_wallpaper = true;
			}

			LightningEngine engine = LLApp.get().getEngine(theme_dir, true);
			
            Page p = engine.getOrLoadPage(page);
			
			int view_width, view_height;
			DisplayMetrics metrics = context.getResources().getDisplayMetrics();
			view_width = metrics.widthPixels;
			view_height = metrics.heightPixels - (p.config.statusBarHide ? 38 : 0); // XXX compute status bar height (might need to get it from caller since there is no window here)
			
			float size_ratio;
			switch(preview_size) {
			case THUMBNAIL: size_ratio = 6; break;
			case MEDIUM:size_ratio = 2; break;
			default:size_ratio = 1; break;
			}
			
			ItemLayout il = new ItemLayout(context, null);
			// FIXME need to set a dummy screen here
			il.setAllowDelayedViewInit(false);
			il.setPage(p);
	
			il.measure(MeasureSpec.makeMeasureSpec(view_width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(view_height, MeasureSpec.EXACTLY));
			il.layout(0, 0, view_width, view_height);
			
			Bitmap bitmap = Bitmap.createBitmap((int)(view_width/size_ratio), (int)(view_height/size_ratio), Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			canvas.scale(1/size_ratio, 1/size_ratio);
			canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));
			
			int color = p.config.bgColor;
			int alpha = Color.alpha(color);
			if(alpha < 255) {
				// use the system wallpaper for the current theme, and use the saved bitmap for themes on th external storage
				Bitmap wallpaper = null;
				try {
					if(use_system_wallpaper) {
						Drawable d = context.getWallpaper();
						if(d!=null && d instanceof BitmapDrawable) {
							wallpaper = ((BitmapDrawable)d).getBitmap();
						}
					} else {
	        			wallpaper = BitmapFactory.decodeFile(new File(theme_dir, FileUtils.WALLPAPER_DIR+"/"+FileUtils.SYSTEM_WALLPAPER_BITMAP_FILE).getAbsolutePath(), null);
		        	}
				} catch(Throwable e) {
					// pass
				}
				
				if(wallpaper != null) {
					canvas.drawBitmap(wallpaper, 0, 0, null);
				}
			}
			
			if(alpha != 0) {
				canvas.drawARGB(alpha, Color.red(color), Color.green(color), Color.blue(color));
			}
			
			il.draw(canvas);

			File out=new File(context.getCacheDir(), PREVIEW_FILE);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 85, new FileOutputStream(out));
			bitmap.recycle();
	
			if (out.exists()) {
				ParcelFileDescriptor pfd=ParcelFileDescriptor.open(out, ParcelFileDescriptor.MODE_READ_ONLY);
				out.delete();
				return pfd;
			}
		}

		throw new FileNotFoundException(uri.getPath());
	}
}
