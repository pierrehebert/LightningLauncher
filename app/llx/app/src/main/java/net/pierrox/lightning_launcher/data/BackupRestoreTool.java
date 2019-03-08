package net.pierrox.lightning_launcher.data;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.util.DisplayMetrics;
import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.engine.LightningEngine;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupRestoreTool {

	private static final int BACKUP_VERSION=1;
	
	private static final String ZIP_FILE_VERSION="version";
	private static final String ZIP_FILE_MANIFEST="manifest";
	private static final String ZIP_DIR_CORE="core";
	private static final String ZIP_DIR_WIDGETS_DATA="widgets_data";
	private static final String ZIP_DIR_WALLPAPER="wallpaper";
	private static final String ZIP_FILE_WALLPAPER_BITMAP="bitmap.png";
	private static final String ZIP_DIR_FONTS="fonts";

    public static final String MANIFEST_LL_VERSION = "llVersion";
    public static final String MANIFEST_LL_PKG_NAME = "llPkgName";
    public static final String MANIFEST_SCREEN_WIDTH = "screenWidth";
    public static final String MANIFEST_SCREEN_HEIGHT = "screenHeight";
    public static final String MANIFEST_SCREEN_DENSITY = "screenDensity";
    public static final String MANIFEST_SB_HEIGHT = "sbHeight";
    public static final String MANIFEST_DEVICE_MODEL = "deviceModel";


    public static class BackupConfig {
		public Context context;
		public String pathFrom;
		public String pathTo;
		public boolean includeWidgetsData;
		public boolean includeWallpaper;
		public boolean includeFonts;
        public boolean forTemplate;
        public int statusBarHeight;
        public ArrayList<Integer> pagesToInclude;
    }
	
	public static class RestoreConfig {
		public Context context;
		public Uri uriFrom;
        public InputStream isFrom;
		public String pathTo;
		public boolean restoreWidgetsData;
		public boolean restoreWallpaper;
		public boolean restoreFonts;
	}
	
	public static Exception backup(BackupConfig backup_config) {
        FileUtils.LL_EXT_DIR.mkdirs();

		LightningEngine engine = LLApp.get().getAppEngine();
		engine.saveData();

		Exception result=null;
		File file=new File(backup_config.pathTo);
		FileOutputStream fos=null;
		ZipOutputStream zos=null;
        try {
			fos=new FileOutputStream(file);
			zos=new ZipOutputStream(fos);
			
			zos.putNextEntry(new ZipEntry(ZIP_FILE_VERSION));
			zos.write(String.valueOf(BACKUP_VERSION).getBytes());
			zos.closeEntry();

            if(backup_config.forTemplate) {
                DisplayMetrics dm = backup_config.context.getResources().getDisplayMetrics();
                JSONObject manifest = new JSONObject();
                try {
                    manifest.put(MANIFEST_LL_VERSION, Utils.getMyPackageVersion(backup_config.context));
                    manifest.put(MANIFEST_LL_PKG_NAME, backup_config.context.getPackageName());
                    manifest.put(MANIFEST_SCREEN_WIDTH, dm.widthPixels);
                    manifest.put(MANIFEST_SCREEN_HEIGHT, dm.heightPixels);
                    manifest.put(MANIFEST_SCREEN_DENSITY, dm.densityDpi);
                    manifest.put(MANIFEST_SB_HEIGHT, backup_config.statusBarHeight);
                    manifest.put(MANIFEST_DEVICE_MODEL, Build.MODEL);
                } catch(JSONException e) {
                    // pass
                }
                zos.putNextEntry(new ZipEntry(ZIP_FILE_MANIFEST));
                zos.write(manifest.toString().getBytes());
                zos.closeEntry();
            }


			backupCoreData(backup_config, zos);
			
			if(backup_config.includeWidgetsData) {
				backupWidgetsData(backup_config, zos);
			}
			
			if(backup_config.includeWallpaper) {
				backupWallpaper(backup_config, zos);
			}
			
			if(backup_config.includeFonts) {
				backupFonts(backup_config, zos);
			}
			
			// ensure the stream is flushed and and closed before to say this is ok (even if done in finally)
			zos.flush();
			zos.close();
			fos.close();
			zos=null;
			fos=null;
			result=null;
		} catch(IOException e) {
			e.printStackTrace();
			result=e;
		} finally {
			if(zos!=null) try { zos.close(); } catch(IOException e) {}
			if(fos!=null) try { fos.close(); } catch(IOException e) {}
		}

		if(result!=null) {
			file.delete();
		}
		
		return result;
	}
	
	private static void backupCoreData(BackupConfig backup_config, ZipOutputStream zos) throws IOException {
		File base_dir = new File(backup_config.pathFrom);
        putZipDirEntry(zos, ZIP_DIR_CORE);

		// regarding the global config file, it is zipped from a string (serialized json) because some values can be modified on the fly
		LightningEngine engine = LLApp.get().getAppEngine();
		GlobalConfig global_config = new GlobalConfig();
		global_config.copyFrom(engine.getGlobalConfig());
        String[] screensNamesOrig = global_config.screensNames;
        int[] screenOrderOrig = global_config.screensOrder;
		if(backup_config.forTemplate) {
			int num_dashboard_pages = 0;
			for(int p : backup_config.pagesToInclude) {
				if(Page.isDashboard(p)) {
					num_dashboard_pages++;
				}
			}
			global_config.screensNames = new String[num_dashboard_pages];
			global_config.screensOrder = new int[num_dashboard_pages];
			for(int i=0, j=0; i<screenOrderOrig.length; i++) {
				int this_page = screenOrderOrig[i];
				for(int p : backup_config.pagesToInclude) {
					if(this_page == p) {
						global_config.screensOrder[j] = this_page;
						global_config.screensNames[j] = screensNamesOrig[i];
						j++;
						break;
					}
				}
			}
			// if lock screen is not in the list of exported desktop, reset it
			boolean found = false;
			for(int p : backup_config.pagesToInclude) {
				if(global_config.lockScreen == p) {
					found = true;
					break;
				}
			}
			if(!found) {
				global_config.lockScreen = Page.NONE;
			}
		}
		zipData(backup_config, global_config.toString(), zos, FileUtils.getGlobalConfigFile(base_dir).getName(), ZIP_DIR_CORE);


		zipFile(backup_config, zos, FileUtils.getManifestFile(base_dir), ZIP_DIR_CORE);
		zipFile(backup_config, zos, FileUtils.getSystemConfigFile(backup_config.context), ZIP_DIR_CORE);
		zipFile(backup_config, zos, FileUtils.getStateFile(base_dir), ZIP_DIR_CORE);
		zipFile(backup_config, zos, FileUtils.getStatisticsFile(base_dir), ZIP_DIR_CORE);
		zipFile(backup_config, zos, FileUtils.getPinnedAppShortcutsFile(base_dir), ZIP_DIR_CORE);
		zipFile(backup_config, zos, FileUtils.getVariablesFile(base_dir), ZIP_DIR_CORE);
        File pages_dir = FileUtils.getPagesDir(base_dir);

		// TODO replace with engine.getPageManager.getAllPageIds
		File[] pages = pages_dir.listFiles();
        for(File f : pages) {
        	String name = f.getName();
        	boolean zip_dir = true;
        	if(backup_config.pagesToInclude != null) {
        		try {
	        		int p = Integer.parseInt(name);
	                zip_dir = backup_config.pagesToInclude.contains(Integer.valueOf(p));
        		} catch(NumberFormatException e) {
        			zip_dir = false;
        		}
            }
        	
        	if(zip_dir) {
        		zipDir(backup_config, zos, f, ZIP_DIR_CORE+"/pages/"+name);
        	}
        }
        zipDir(backup_config, zos, FileUtils.getStylesDir(base_dir), ZIP_DIR_CORE+"/themes");
        zipDir(backup_config, zos, engine.getScriptManager().getScriptsDir(), ZIP_DIR_CORE+"/scripts");
	}

	private static void backupWidgetsData(BackupConfig backup_config, ZipOutputStream zos) throws IOException {
        putZipDirEntry(zos, ZIP_DIR_WIDGETS_DATA);

        final Context context = backup_config.context;
		LLApp app = LLApp.get();
		LightningEngine engine = app.getAppEngine();
		AppWidgetManager app_widget_manager=AppWidgetManager.getInstance(app);
        File base_dir = engine.getBaseDir();
		// TODO replace with engine.getPageManager.getAllPageIds
		File wd=FileUtils.getPagesDir(base_dir);
        String[] pages=wd.list();

        final Semaphore sem = new Semaphore(0);

        FileUtils.LL_TMP_DIR.mkdirs();

        for(int n=0; n<pages.length; n++) {
            int p=Integer.parseInt(pages[n]);
            Page page = engine.getOrLoadPage(p);
            for(Item i : page.items) {
                if(i.getClass() == Widget.class) {
                    Widget w = (Widget) i;
                    int appWidgetId = w.getAppWidgetId();
                    AppWidgetProviderInfo app_widget_info=app_widget_manager.getAppWidgetInfo(appWidgetId);

                    if(app_widget_info != null) {
                        File tmp_widget_file = new File(FileUtils.LL_TMP_DIR, String.valueOf(appWidgetId));
                        Uri tmp_widget_uri = Uri.fromFile(tmp_widget_file);

                        Intent br = new Intent("com.buzzpia.aqua.appwidget.GET_CONFIG_DATA");
                        br.putExtra("appWidgetId", appWidgetId);
                        br.setData(tmp_widget_uri);

                        br.setComponent(app_widget_info.provider);

                        BroadcastReceiver receiver = new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                sem.release();
                            }
                        };
						try {
							context.sendOrderedBroadcast(br, null, receiver, null, Activity.RESULT_OK, null, null);
						} catch (Exception e) {	// FileUriExposedException, use Exception because not available before API 24
							// pass, new security measure on Android 7
							continue;
						}
                        try { sem.acquire(); } catch (InterruptedException e) { e.printStackTrace(); }

                        if(receiver.getResultCode() == 300) {
                            try {
                            zipFile(backup_config, zos, tmp_widget_file, ZIP_DIR_WIDGETS_DATA);
                            } catch(ZipException e) {

                            }
                        }

                        tmp_widget_file.delete();
                    }
                }
            }
        }
	}
	
	private static void backupWallpaper(BackupConfig backup_config, ZipOutputStream zos) throws IOException {
        Drawable d;
        try {
            d = backup_config.context.getWallpaper();
        } catch(Exception e) {
            d = null;
        }
		if(d instanceof BitmapDrawable) {
			putZipDirEntry(zos, ZIP_DIR_WALLPAPER);
			ZipEntry ze=new ZipEntry(ZIP_DIR_WALLPAPER+"/"+ZIP_FILE_WALLPAPER_BITMAP);
			zos.putNextEntry(ze);
			((BitmapDrawable)d).getBitmap().compress(CompressFormat.PNG, 100, zos);
			zos.closeEntry();
		}
	}
	
	private static void backupFonts(BackupConfig backup_config, ZipOutputStream zos) throws IOException {
		File from=new File(backup_config.pathFrom+"/fonts");
		if(from.exists()) {
			putZipDirEntry(zos, ZIP_DIR_FONTS);
			zipDir(backup_config, zos, from, ZIP_DIR_FONTS);
		}
	}
	
	private static void putZipDirEntry(ZipOutputStream zos, String dir_name) throws IOException {
		zos.putNextEntry(new ZipEntry(dir_name+"/"));
		zos.closeEntry();
	}
	
	private static void zipDir(BackupConfig backup_config, ZipOutputStream zos, File from, String to) throws IOException {
		File[] files=from.listFiles();
        if(files == null) {
            // some directories may not be created yet (eg themes)
            return;
        }

		boolean isAppDrawerTemplateIconDir = backup_config.forTemplate && from.getAbsolutePath().endsWith("/" + Page.APP_DRAWER_PAGE + "/icon");
		File[] keepOnly;
		if(isAppDrawerTemplateIconDir) {
			int length = FileUtils.ALL_SUFFIXES.length;
			keepOnly = new File[length];
			for(length--; length >=0; length--) {
				keepOnly[length] = new File(from, FileUtils.ALL_SUFFIXES[length]);
			}
		} else {
			keepOnly = null;
		}

        for(File f : files) {
			if(f.isDirectory()) {
				zipDir(backup_config, zos, f, to+"/"+f.getName());
			} else {
				boolean keep;
				if(keepOnly == null) {
					keep = true;
				} else {
					keep = false;
					for (File file : keepOnly) {
						if(file.equals(f)) {
							keep = true;
							break;
						}
					}
				}
				if(keep) {
					zipFile(backup_config, zos, f, to);
				}
			}
		}
	}
	
	private static void zipFile(BackupConfig backup_config, ZipOutputStream zos, File file, String to) throws IOException {
		if(!file.exists()) return;

		if(backup_config.forTemplate && file.getAbsolutePath().endsWith("/"+ Page.APP_DRAWER_PAGE+"/items")) {
			// hack: exclude items from the app drawer in a template
			return;
		}

		FileInputStream fis=new FileInputStream(file);
		zipInputStream(backup_config, fis, zos, file.getName(), to);
	}

	private static void zipData(BackupConfig backup_config, String data, ZipOutputStream zos, String name, String to) throws IOException {
		zipInputStream(backup_config, new ByteArrayInputStream(data.getBytes("utf-8")), zos, name, to);
	}

	private static void zipInputStream(BackupConfig backup_config, InputStream is, ZipOutputStream zos, String name, String to) throws IOException {
		try {
			ZipEntry ze=new ZipEntry(to+"/"+name);
			zos.putNextEntry(ze);
			FileUtils.copyStream(is, zos);
			zos.closeEntry();
		} finally {
			try { is.close(); } catch(Exception e) {/*pass*/}
		}
	}

	public static boolean restore(RestoreConfig restore_config) {
        FileUtils.LL_EXT_DIR.mkdirs();

		boolean result=false;

		Utils.deleteDirectory(new File(restore_config.pathTo), false);

		LightningEngine engine = LLApp.get().getAppEngine();
		engine.getPageManager().clear();
        engine.getScriptManager().clear();

		InputStream is=null;
        ZipInputStream zis=null;
        try {
            if(restore_config.uriFrom != null) {
                is=restore_config.context.getContentResolver().openInputStream(restore_config.uriFrom);
            } else {
                is=restore_config.isFrom;
            }
			zis=new ZipInputStream(is);
			ZipEntry ze;

			ze=zis.getNextEntry();
			if(!ze.getName().equals(ZIP_FILE_VERSION)) {
				return false;
			}

			byte[] version_bytes=new byte[30];
			int n=zis.read(version_bytes);
			int version=Integer.parseInt(new String(version_bytes, 0, n));
			if(version>BACKUP_VERSION) {
				return false;
			}

			ze=zis.getNextEntry();
			for(;;) {
				if(ze==null) {
					break;
				}

				final String name=ze.getName();
				if(name.startsWith(ZIP_DIR_CORE)) {
					ze=restoreCoreData(restore_config, zis);
				} else if(name.startsWith(ZIP_DIR_WIDGETS_DATA)) {
					ze=restoreWidgetsData(restore_config, zis);
				} else if(name.startsWith(ZIP_DIR_WALLPAPER)) {
					ze=restoreWallpaper(restore_config, zis);
				} else if(name.startsWith(ZIP_DIR_FONTS)) {
					ze=restoreFonts(restore_config, zis);
				} else {
                    // skip this unknown file
                    ze=zis.getNextEntry();
                }
			}
			result=true;
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			if(zis!=null) try { zis.close(); } catch(IOException e) {}
			if(is!=null) try { is.close(); } catch(IOException e) {}
		}

		return result;
	}

    public static JSONObject readManifest(InputStream from) {
        ZipInputStream zis=null;
        try {
            zis=new ZipInputStream(from);
            ZipEntry ze;

            ze=zis.getNextEntry();
            if(ze==null || !ze.getName().equals(ZIP_FILE_VERSION)) {
                return null;
            }

            byte[] version_bytes=new byte[30];
            int n=zis.read(version_bytes);
            int version=Integer.parseInt(new String(version_bytes, 0, n));
            if(version>BACKUP_VERSION) {
                return null;
            }

            ze=zis.getNextEntry();
            if(ze==null || !ze.getName().equals(ZIP_FILE_MANIFEST)) {
                return null;
            }
            byte[] json_data = new byte[4000];
            n = zis.read(json_data);
            return new JSONObject(new String(json_data, 0, n));
        } catch(IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            if(zis!=null) try { zis.close(); } catch(IOException e) {}
        }

        return null;
    }
	
	private static ZipEntry restoreCoreData(RestoreConfig restore_config, ZipInputStream zis) throws IOException {
			ZipEntry ze;
			for(;;) {
				ze=zis.getNextEntry();
				if(ze==null) {
					break;
				}
				
				final String name=ze.getName();
				if(!name.startsWith(ZIP_DIR_CORE)) {
					break;
				}
				
//				if(name.equals("core/pages/"+Utils.APP_DRAWER_PAGE+"/items")) {
//					// do not restore items, this is a local data
//					continue;
//				}
				
				FileOutputStream fos=null;
				final int l=ZIP_DIR_CORE.length();
				try {
					String path=restore_config.pathTo+name.substring(l);
					File out=new File(path);
					File parent=out.getParentFile();
					if(!parent.exists()) {
						parent.mkdirs();
					}
					fos=new FileOutputStream(out);
					FileUtils.copyStream(zis, fos);
				} catch(IOException e) {
					throw e;
				} finally {
					if(fos!=null) try { fos.close(); } catch(Exception e) {}
				}
			}
			
			return ze;
	}
	
	private static ZipEntry restoreWidgetsData(RestoreConfig restore_config, ZipInputStream zis) throws IOException {
		if(!restore_config.restoreWidgetsData) {
			return skipZipEntries(zis, ZIP_DIR_WIDGETS_DATA);
		}

        final int l = ZIP_DIR_WIDGETS_DATA.length();

        FileUtils.LL_TMP_DIR.mkdirs();
        ZipEntry ze;
        for(;;) {
            ze=zis.getNextEntry();
            if(ze==null) {
                break;
            }

            final String name = ze.getName();
            if(!name.startsWith(ZIP_DIR_WIDGETS_DATA)) {
                break;
            }

            FileOutputStream fos = null;
            try {
                File out = new File(FileUtils.LL_TMP_DIR, name.substring(l));
                fos=new FileOutputStream(out);
				FileUtils.copyStream(zis, fos);
            } finally {
                if(fos!=null) try { fos.close(); } catch(Exception e) {}
            }
        }

        return ze;
	}
	
	private static ZipEntry restoreWallpaper(RestoreConfig restore_config, ZipInputStream zis) throws IOException {
		if(!restore_config.restoreWallpaper) {
			return skipZipEntries(zis, ZIP_DIR_WALLPAPER);
		}
		
		ZipEntry ze=zis.getNextEntry();
		if(ze.getName().endsWith(ZIP_FILE_WALLPAPER_BITMAP)) {
			
			try {
				Bitmap wallpaper=BitmapFactory.decodeStream(zis);
				restore_config.context.setWallpaper(wallpaper);
			} catch(Throwable e) {
				
			}
			ze=zis.getNextEntry();
		}
		
		return ze;
	}
	
	private static ZipEntry restoreFonts(RestoreConfig restore_config, ZipInputStream zis) throws IOException {
		if(!restore_config.restoreFonts) {
			return skipZipEntries(zis, ZIP_DIR_FONTS);
		}
		
		ZipEntry ze;
		for(;;) {
			ze=zis.getNextEntry();
			if(ze==null) {
				break;
			}
			
			final String name=ze.getName();
			if(!name.startsWith(ZIP_DIR_FONTS)) {
				break;
			}
			
			FileOutputStream fos=null;
			final int l=ZIP_DIR_FONTS.length();
			try {
				String path=restore_config.pathTo+"/fonts"+name.substring(l);
				File out=new File(path);
				File parent=out.getParentFile();
				if(!parent.exists()) {
					parent.mkdirs();
				}
				fos=new FileOutputStream(out);
				FileUtils.copyStream(zis, fos);
			} finally {
				if(fos!=null) try { fos.close(); } catch(Exception e) {}
			}
		}
		
		return ze;
	}
	
	private static ZipEntry skipZipEntries(ZipInputStream zis, String prefix) throws IOException {
		ZipEntry ze;
		for(;;) {
			ze=zis.getNextEntry();
			if(ze==null) {
				break;
			}
			
			final String name=ze.getName();
			if(!name.startsWith(prefix)) {
				break;
			}
		}
		
		return ze;
	}
}
