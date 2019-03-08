package net.pierrox.lightning_launcher.data;

import android.content.Context;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;

import net.pierrox.lightning_launcher.API;
import net.pierrox.lightning_launcher.BuildConfig;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {
	public static final String WALLPAPER_DIR = "wp";
	public static final String SYSTEM_WALLPAPER_BITMAP_FILE = "system.png";

    public static final String SUFFIX_ICON_BACK = "b";
    public static final String SUFFIX_ICON_OVER = "o";
    public static final String SUFFIX_ICON_MASK = "m";
    public static final String SUFFIX_BOX_BG_NORMAL = "n";
    public static final String SUFFIX_BOX_BG_SELECTED = "s";
    public static final String SUFFIX_BOX_BG_FOCUSED = "f";
    public static final String SUFFIX_BOX_BG_FOLDER = "r";
    public static final String SUFFIX_APP_DRAWER_AB_BACKGROUND = "a";

    public static final String[] ALL_SUFFIXES = new String[] {
            FileUtils.SUFFIX_ICON_BACK,
            FileUtils.SUFFIX_ICON_OVER,
            FileUtils.SUFFIX_ICON_MASK,
            FileUtils.SUFFIX_BOX_BG_NORMAL,
            FileUtils.SUFFIX_BOX_BG_SELECTED,
            FileUtils.SUFFIX_BOX_BG_FOCUSED,
            FileUtils.SUFFIX_BOX_BG_FOLDER,
            FileUtils.SUFFIX_APP_DRAWER_AB_BACKGROUND
    };

    public static final File LL_EXT_DIR =new File(Environment.getExternalStorageDirectory(), "LightningLauncher");
    public static final File LL_TMP_DIR =new File(LL_EXT_DIR, "tmp");
    public static final File LL_EXT_SCRIPT_DIR = new File(LL_EXT_DIR, "script");


	private static byte[] copy_buffer=new byte[4096];
	public static void copyStream(InputStream from, OutputStream to) throws IOException {
		int n;
		while((n=from.read(copy_buffer))!=-1) {
			to.write(copy_buffer, 0, n);
		}
	}

	public static void saveStringToFile(String what, File out) throws IOException {
		FileOutputStream fos=null;
		try {
			out.getParentFile().mkdirs();
			fos=new FileOutputStream(out);
			fos.write(what.getBytes("utf-8"));
		} catch(IOException e) {
			out.delete();
			throw e;
		} finally {
			if(fos!=null) try { fos.close(); } catch(Exception e) { /*pass*/ }
		}
	}

	public static JSONObject readJSONObjectFromFile(File json_file) {
	    try {
			long t1 = BuildConfig.IS_BETA ? SystemClock.uptimeMillis() : 0;
			JSONObject jsonObject = new JSONObject(FileUtils.readFileContent(json_file));
			if(BuildConfig.IS_BETA) {
				Log.i("LL", "readJSONObjectFromFile in "+(SystemClock.uptimeMillis()-t1)+"ms, length="+json_file.length()+", file="+json_file);
			}
			return jsonObject;
	    } catch(Exception e) {
	    	return null;
	    }
	}

	public static String readFileContent(File f) {
		FileInputStream fis=null;
	    try {
	    	fis=new FileInputStream(f);
	    	int file_length=(int)f.length();
	    	byte[] data=new byte[file_length];
	    	fis.read(data, 0, file_length);
	        return new String(data, "utf-8");
	    } catch(Exception e) {
	    	return null;
	    } finally {
	        if(fis!=null) try { fis.close(); } catch(Exception e) {}
	    }
	}

    public static String readInputStreamContent(InputStream is) {
        try {
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int n;
            while((n=is.read(buffer))>0) {
                baos.write(buffer, 0, n);
            }
            return baos.toString("utf8");
        } catch(Exception e) {
            return null;
        } finally {
            if(is!=null) try { is.close(); } catch(Exception e) {}
        }
    }
	
	
	public static final String THEMES_DIR = "LightningLauncher/themes";
	
	public static final File getExternalThemeDir(String theme_id) {
		return new File(Environment.getExternalStorageDirectory(), FileUtils.THEMES_DIR+"/"+theme_id);
	}
	
	public static File getGlobalConfigFile(File base_dir) {
		return new File(base_dir, "config");
	}

	public static File getSystemConfigFile(Context context) {
		return new File(context.getFilesDir(), "system");
	}
	public static File getCachedDrawablesDir(Context context) {
		return new File(context.getCacheDir(), "drawables");
	}

	public static File getManifestFile(File base_dir) {
		return new File(base_dir, "manifest");
	}

    public static File getStateFile(File base_dir) {
        return new File(base_dir, "state");
    }

    public static File getStatisticsFile(File base_dir) {
        return new File(base_dir, "statistics");
    }

    public static File getPinnedAppShortcutsFile(File base_dir) {
        return new File(base_dir, "app_shortcuts");
    }

    public static File getVariablesFile(File base_dir) {
        return new File(base_dir, "variables");
    }

    public static File getStylesDir(File base_dir) {
        return new File(base_dir, "themes");
    }

	public static File getPagesDir(File base_dir) {
		return new File(base_dir, "pages");
	}
	
	public static File getFontsDir(File base_dir) {
		return new File(base_dir, API.DIR_FONTS);
	}

	// can be applied to copy page icon files
	public static void copyIcons(byte[] buffer, File from_dir, String from_id, File to_dir, String to_id) {
		if(buffer == null) {
			buffer = new byte[512];
		}
		
        for (String code : FileUtils.ALL_SUFFIXES) {
			File icon_from = new File(from_dir, from_id + code);
			File icon_to = new File(to_dir, to_id + code);
			if (icon_from.exists()) {
				Utils.copyFileSafe(buffer, icon_from, icon_to);
            } else {
				icon_to.delete();
            }
		}
	}

	// an extended version of copyIcons for items, which will copy default and custom icons too
	public static void copyItemFiles(int from_id, File from_icon_dir, int to_id, File to_icon_dir) {
		byte[] buffer=new byte[4096];
		FileUtils.copyIcons(buffer, from_icon_dir, String.valueOf(from_id), to_icon_dir, String.valueOf(to_id));

		File file_from, file_to;
		file_from=Item.getDefaultIconFile(from_icon_dir, from_id);
		file_to = Item.getDefaultIconFile(to_icon_dir, to_id);
		if(file_from.exists()) {
			Utils.copyFileSafe(buffer, file_from, file_to);
		} else {
			file_to.delete();
		}

		file_from=Item.getCustomIconFile(from_icon_dir, from_id);
		file_to=Item.getCustomIconFile(to_icon_dir, to_id);
		if(file_from.exists()) {
			Utils.copyFileSafe(buffer, file_from, file_to);
		} else {
			file_to.delete();
		}
	}

//	public static File getGlobalConfigFile(Context context) {
//		return getGlobalConfigFile(getAppBaseDir(context));
//	}
//
//    public static File getStateFile(Context context) {
//        return getStateFile(getAppBaseDir(context));
//    }
//
//    public static File getStatisticsFile(Context context) {
//        return getStatisticsFile(getAppBaseDir(context));
//    }
//
//    public static File getStylesDir(Context context) {
//        return getStylesDir(getAppBaseDir(context));
//    }
//
//	public static File getPagesDir(Context context) {
//		return getPagesDir(getAppBaseDir(context));
//	}
//
//	public static File getPageDir(Context context, int page) {
//		return getPageDir(getAppBaseDir(context), page);
//	}
//
//	public static File getItemsFile(Context context, int page) {
//		return getItemsFile(getAppBaseDir(context), page);
//	}
//
//	public static File getPageIconFile(Context context, int page) {
//		return getPageIconFile(getAppBaseDir(context), page);
//	}
//
//	public static File getWorkspaceConfigFile(Context context, int page) {
//		return getWorkspaceConfigFile(getAppBaseDir(context), page);
//	}
//
//	public static File getIconDir(Context context, int page) {
//		return getIconDir(getAppBaseDir(context), page);
//	}
//
//	public static File getWallpaperFile(Context context, int page) {
//		return getWallpaperFile(getAppBaseDir(context), page);
//	}
//
//	public static File getFontsDir(Context context) {
//		return getFontsDir(getAppBaseDir(context));
//	}
}
