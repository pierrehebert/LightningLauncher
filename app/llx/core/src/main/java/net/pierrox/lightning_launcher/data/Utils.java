package net.pierrox.lightning_launcher.data;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.NinePatch;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.graphics.Typeface;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Parcelable;
import android.text.Html;
import android.text.Spanned;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.R;
import net.pierrox.lightning_launcher.Version;
import net.pierrox.lightning_launcher.configuration.DynamicTextConfig;
import net.pierrox.lightning_launcher.configuration.FolderConfig;
import net.pierrox.lightning_launcher.configuration.FolderConfig.FolderIconStyle;
import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.configuration.ItemConfig;
import net.pierrox.lightning_launcher.configuration.JsonFields;
import net.pierrox.lightning_launcher.configuration.PageConfig;
import net.pierrox.lightning_launcher.configuration.ShortcutConfig;
import net.pierrox.lightning_launcher.configuration.SystemConfig;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.engine.Screen;
import net.pierrox.lightning_launcher.script.Script;
import net.pierrox.lightning_launcher.util.AnimatedGifEncoder;
import net.pierrox.lightning_launcher.util.AnimationDecoder;
import net.pierrox.lightning_launcher.views.BoxLayout;
import net.pierrox.lightning_launcher.views.Graphics;
import net.pierrox.lightning_launcher.views.ItemLayout;
import net.pierrox.lightning_launcher.views.SharedAsyncGraphicsDrawable;
import net.pierrox.lightning_launcher.views.svg.SvgDrawable;
import net.pierrox.lightning_launcher.views.item.ItemView;
import net.pierrox.lightning_launcher.views.item.StopPointView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class Utils {
    public static final int USER_MENU_ITEM_ID = Page.composeItemId(Page.USER_MENU_PAGE, 0);
//	private static final int ORIENTATION_SHIFT=200;

    public static final int LAYOUT_MODE_UNDEFINED = -1;
    public static final int LAYOUT_MODE_CUSTOM = 0;
    public static final int LAYOUT_MODE_BY_NAME = 1;
    public static final int LAYOUT_MODE_FREQUENTLY_USED = 2;
    public static final int LAYOUT_MODE_RECENT_APPS = 3;
    public static final int LAYOUT_MODE_RECENTLY_UPDATED = 4;
    public static final int LAYOUT_MODE_RUNNING = 5;
    public static final int LAYOUT_MODE_LAST = LAYOUT_MODE_RUNNING;

    public static final Collator sItemNameCollator = Collator.getInstance(Locale.getDefault());

    static {
        sItemNameCollator.setStrength(Collator.PRIMARY);
        try {
            sGetDrawableForDensity=Resources.class.getMethod("getDrawableForDensity", int.class, int.class);
        } catch (NoSuchMethodException e) {
            // API level 15
        }
    }



    private static int sStandardIconSize;
    private static int sLauncherIconDensity;
    public static Method sGetDrawableForDensity;
    private static Bitmap sDefaultIcon;
    private static Rect sTmpRect1=new Rect();

    //    public static int getNextDashboardPage(int p, int direction) {
//    	int o=p/ORIENTATION_SHIFT;
//    	int bp=p-ORIENTATION_SHIFT*o;
//    	bp+=direction;
//    	if(bp<0) {
//			bp=Utils.LAST_DASHBOARD_PAGE;
//		} else if(bp>Utils.LAST_DASHBOARD_PAGE) {
//			bp=0;
//		}
//    	return bp+o*ORIENTATION_SHIFT;
//    }

    //    public static int getPageForOrientation(int p, int degrees) {
//    	return p%ORIENTATION_SHIFT + ORIENTATION_SHIFT*degrees;
//    }
    
	public static int getPageForItemId(int item_id) {
		// linked with SetupUtils.findFreeItemId
		return (item_id>>16)&0xffff;
	}

	public static int getPageForItem(Item item) {
		// linked with SetupUtils.findFreeItemId
		return getPageForItemId(item.getId());
	}

    public static final Comparator<Item> sItemComparatorByNameAsc =new Comparator<Item>() {
        @Override
        public int compare(Item arg0, Item arg1) {
            return compareItemName(arg0, arg1, 1);
        }
    };

    private static int compareItemName(Item arg0, Item arg1, int sign) {
        Class<?> c0=arg0.getClass();
        if(c0!=Shortcut.class && c0!=Folder.class) return -1;
        Class<?> c1=arg1.getClass();
        if(c1!=Shortcut.class && c1!=Folder.class) return 1;
        Shortcut s0=(Shortcut)arg0;
        Shortcut s1=(Shortcut)arg1;

        String l0 = s0.getLabel();
        Spanned h0 = Html.fromHtml(l0);
        if(h0.length() > 0) l0 = h0.toString();

        String l1 = s1.getLabel();
        Spanned h1 = Html.fromHtml(l1);
        if(h1.length() > 0) l1 = h1.toString();

        return sign * sItemNameCollator.compare(l0, l1);

    }

    public static final Comparator<Item> sItemComparatorByNameDesc=new Comparator<Item>() {
        @Override
        public int compare(Item arg0, Item arg1) {
            return compareItemName(arg0, arg1, -1);
        }
    };

    public static final Comparator<Item> sItemComparatorByLaunchCount=new Comparator<Item>() {
        @Override
        public int compare(Item arg0, Item arg1) {
            if(arg0.mLaunchCount>arg1.mLaunchCount) return -1;
            if(arg0.mLaunchCount<arg1.mLaunchCount) return 1;
            Class<?> c0=arg0.getClass();
            if(c0!=Shortcut.class && c0!=Folder.class) return -1;
            Class<?> c1=arg1.getClass();
            if(c1!=Shortcut.class && c1!=Folder.class) return 1;
            Shortcut s0=(Shortcut)arg0;
            Shortcut s1=(Shortcut)arg1;
            return s0.getLabel().compareToIgnoreCase(s1.getLabel());
        }
    };

    public static final Comparator<Item> sItemComparatorByLastUpdateTime=new Comparator<Item>() {
        @Override
        public int compare(Item arg0, Item arg1) {
            if(arg0.mLastUpdateTime>arg1.mLastUpdateTime) return -1;
            if(arg0.mLastUpdateTime<arg1.mLastUpdateTime) return 1;
            Class<?> c0=arg0.getClass();
            if(c0!=Shortcut.class && c0!=Folder.class) return -1;
            Class<?> c1=arg1.getClass();
            if(c1!=Shortcut.class && c1!=Folder.class) return 1;
            Shortcut s0=(Shortcut)arg0;
            Shortcut s1=(Shortcut)arg1;
            return s0.getLabel().compareToIgnoreCase(s1.getLabel());
        }
    };
    
    public static void sortScripts(List<Script> scripts) {
		Collections.sort(scripts, new Comparator<Script>() {
			@Override
			public int compare(Script lhs, Script rhs) {
				return Utils.sItemNameCollator.compare(lhs.name, rhs.name);
			}
		});
	}

    public static Bitmap decodeUri(ContentResolver cr, Uri selectedImage, int max_icon_size) throws FileNotFoundException {
        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(cr.openInputStream(selectedImage), null, o);

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = Utils.getBitmapSampleSize(max_icon_size, o.outWidth, o.outHeight);
        return BitmapFactory.decodeStream(cr.openInputStream(selectedImage), null, o2);
    }

	public static Drawable decodeDrawableResource(Resources rsrc, int id) {
        try {
            if(sGetDrawableForDensity!=null) {
                try {
                    Drawable d = (Drawable) sGetDrawableForDensity.invoke(rsrc, id, sLauncherIconDensity);
                    if(d != null) {
                        return d;
                    }
                } catch(Throwable e) {
                    // pass, continue with classic method
                }
            }

            // fallback to the classic method
            return rsrc.getDrawable(id);
        } catch(Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

	public static Bitmap decodeScaledBitmapResource(Resources rsrc, int id, int max_size) {
    	try {
    	    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Drawable drawable = decodeDrawableResource(rsrc, id);
                if(drawable instanceof AdaptiveIconDrawable) {
                    Bitmap bitmap = Bitmap.createBitmap(max_size, max_size, Bitmap.Config.ARGB_8888);
                    drawable.setBounds(0, 0, max_size, max_size);
                    drawable.draw(new Canvas(bitmap));
                    return bitmap;
                }
            }
    		if(sGetDrawableForDensity!=null) {
	    		try {
	    			BitmapDrawable d=(BitmapDrawable) sGetDrawableForDensity.invoke(rsrc, id, sLauncherIconDensity);
	    			return createStandardSizedIcon(d.getBitmap(), max_size);
	    		} catch(Throwable e) {
	    			// pass, continue with classic method
	    		}
    		}
    		
    		// fallback to the classic method
	        BitmapFactory.Options o = new BitmapFactory.Options();
	        o.inJustDecodeBounds = true;
	        BitmapFactory.decodeResource(rsrc, id, o);
	        
	        BitmapFactory.Options o2=new BitmapFactory.Options();
	        o2.inSampleSize = getBitmapSampleSize(max_size, o.outWidth, o.outHeight);
	        return BitmapFactory.decodeResource(rsrc, id, o2);
    	} catch(Throwable e) {
    		e.printStackTrace();
    		return null;
    	}
    }
    
    public static Bitmap createStandardSizedIcon(Bitmap from, int icon_size) {
		Bitmap icon=Bitmap.createBitmap(icon_size, icon_size, Bitmap.Config.ARGB_8888);
		Canvas canvas=new Canvas(icon);
		Matrix m=new Matrix();
		int from_w=from.getWidth();
		int from_h=from.getHeight();
		m.setRectToRect(
				new RectF(0, 0, from_w, from_h), 
				new RectF(0, 0, icon_size, icon_size), 
				Matrix.ScaleToFit.CENTER);
		Paint p=new Paint();
		p.setFilterBitmap(from_w!=icon_size || from_h!=icon_size);
		canvas.drawBitmap(from, m, p);
		return icon;
    }
    
    public static int getBitmapSampleSize(int max_size, int width, int height) {
        int scale = 1;
        while (true) {
            width /= 2;
            height /= 2;
            // stop as soon as one dimension is less or equal than the max size, or if both dimension are exactly the max size
            if ((width==max_size && height==max_size) || width < max_size || height < max_size) {
                break;
            }

            scale *= 2;
        }

        if(scale>1 && (width<max_size || height<max_size)) {
            scale /=2;
        }
        
        return scale;
    }

    public static void copyOrDeleteFile(File from, File to) {
        boolean copied = false;
        if (from.exists()) {
            copied = Utils.copyFileSafe(null, from, to);
        }
        if (!copied) {
            to.delete();
        }
    }

    public static boolean copyFileSafe(byte[] buffer, File from, File to) {
        if(from.compareTo(to) == 0) {
            return false;
        }
        if(buffer == null) {
            buffer = new byte[1024];
        }
        try {
            copyFile(buffer, from, to);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void copyFile(byte[] buffer, File from, File to)  throws IOException {
    	FileInputStream fis=null;
		FileOutputStream fos=null;
		try {
            to.getParentFile().mkdirs();
			fis=new FileInputStream(from);
			fos=new FileOutputStream(to);
			int n;
			while((n=fis.read(buffer))>0) {
				fos.write(buffer, 0, n);
			}
		} catch(IOException e) {
            to.delete();
			throw e;
		} finally {
			if(fis!=null) try { fis.close(); } catch(Exception e) {}
			if(fos!=null) try { fos.close(); } catch(Exception e) {}
		}
    }

    public static void copyDirectory(byte[] buf, File sourceLocation , File targetLocation) throws IOException {
        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }

            String[] children = sourceLocation.list();
            for (int i=0; i<children.length; i++) {
                copyDirectory(buf, new File(sourceLocation, children[i]), new File(targetLocation, children[i]));
            }
        } else {
            copyFile(buf, sourceLocation, targetLocation);
        }
    }

    public static void retrieveStandardIcon(Context context) {
    	sStandardIconSize=(int) context.getResources().getDimension(android.R.dimen.app_icon_size);
    	sLauncherIconDensity=0;
    	
    	try {
			Method getLauncherLargeIconSize=ActivityManager.class.getMethod("getLauncherLargeIconSize");
			Method getLauncherLargeIconDensity=ActivityManager.class.getMethod("getLauncherLargeIconDensity");
			ActivityManager am=(ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
			sStandardIconSize=(Integer) getLauncherLargeIconSize.invoke(am, (Object[])null);
			sLauncherIconDensity=(Integer) getLauncherLargeIconDensity.invoke(am, (Object[])null);
		} catch (Exception e) {
			// pass API level 11, 15
		}
    	
        Drawable d = context.getPackageManager().getDefaultActivityIcon();
        sDefaultIcon = Bitmap.createBitmap(Math.max(d.getIntrinsicWidth(), 1),
                Math.max(d.getIntrinsicHeight(), 1),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(sDefaultIcon);
        d.setBounds(0, 0, sDefaultIcon.getWidth(), sDefaultIcon.getHeight());
        d.draw(c);
    }
    
    // TODO: could be put as Shortcut instance method to enable per shortcut icon size
    public static int getStandardIconSize() {
        return sStandardIconSize;
	}

    public static int getLauncherIconDensity() {
        return sLauncherIconDensity;
    }

    // TODO: could be replaced by direct field access
    public static Bitmap getDefaultIcon() {
        return sDefaultIcon;
    }

    
    public static ArrayList<Item> loadAppDrawerShortcuts(Page appDrawerPage) {
        long now=System.currentTimeMillis();

        Field last_update_time;
        try {
            last_update_time = PackageInfo.class.getField("lastUpdateTime");
        } catch(Exception e) {
            // available at API 9
            last_update_time = null;
        }

    	ArrayList<Item> items=new ArrayList<Item>();
    	int id=1;
    	File icon_dir=Page.getAndCreateIconDir(appDrawerPage.getEngine().getBaseDir(), Page.APP_DRAWER_PAGE);
		PackageManager pm=LLApp.get().getPackageManager();
        Intent intent_filter=new Intent(Intent.ACTION_MAIN, null);
        intent_filter.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> ris=pm.queryIntentActivities(intent_filter, 0);
        for(ResolveInfo ri : ris) {
            ComponentName component_name=new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
            try {
                String label=ri.loadLabel(pm).toString();

                Intent intent=new Intent();
                intent.setComponent(component_name);
                intent.setAction(Intent.ACTION_MAIN);
                
                Shortcut s=new Shortcut(appDrawerPage);
                s.init(Page.composeItemId(Page.APP_DRAWER_PAGE, id++), new Rect(0, 0, 1, 1), null, label, intent);
                
                if(last_update_time == null) {
                    s.mLastUpdateTime = now;
                } else {
                    PackageInfo pi = pm.getPackageInfo(component_name.getPackageName(), 0);
                    try  {
                        s.mLastUpdateTime = last_update_time.getLong(pi);
                    } catch(Exception e) {
                        s.mLastUpdateTime = now;
                    }
                }

                Resources rsrc = pm.getResourcesForActivity(component_name);
                Bitmap icon = decodeScaledBitmapResource(rsrc, ri.getIconResource(), s.getStdIconSize());
                if(icon != null) {
	                File icon_file=s.getDefaultIconFile();
	                s.getCustomIconFile().delete();
	                saveIconToFile(icon_file, icon);
	                icon.recycle();
                }
                
                items.add(s);
            } catch (Exception e) {
                // skip this item
            }
        }
        
        Collections.sort(items, sItemComparatorByNameAsc);
        
        return items;
    }

    private static void updateOrRemoveAppDrawerShortcuts(LightningEngine engine, Handler handler, PackageManager pm, HashMap<String, ResolveInfo> all_component_names, HashSet<String> my_component_names, int p) {
        final Page page = engine.getOrLoadPage(p);

        // fix duplicate item ids
        HashSet<Integer> ids = new HashSet<Integer>();
        for(Item i : page.items) {
            int id = i.getId();
            if(ids.contains(Integer.valueOf(id))) {
                i.setId(page.findFreeItemId());
            } else {
                ids.add(Integer.valueOf(id));
            }
        }

        for(int i=page.items.size()-1; i>=0; i--) {
            Item item = page.items.get(i);
            if(item.getClass() == Shortcut.class) {
                Shortcut s=(Shortcut)item;
                ComponentName cn=s.getIntent().getComponent();
                if(cn!=null) {
                    final Item fitem = item;
                    String cns = cn.flattenToShortString();
                    my_component_names.add(cns);
                    if(all_component_names.containsKey(cns)) {
                        // update
                        try {
                            Resources rsrc = pm.getResourcesForActivity(cn);
                            ResolveInfo ri = all_component_names.get(cns);
                            Bitmap icon = decodeScaledBitmapResource(rsrc, ri.getIconResource(), s.getStdIconSize());
                            if(icon != null) {
                                File icon_file=s.getDefaultIconFile();
                                saveIconToFile(icon_file, icon);
                                icon.recycle();
                            }
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    fitem.notifyChanged();
                                }
                            });
                        } catch(Throwable t) {
                            t.printStackTrace();
                        }
                    } else {
                        // remove
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                page.removeItem(fitem, false);
                            }
                        });
                    }
                }
            } else if(item.getClass() == Folder.class) {
                updateOrRemoveAppDrawerShortcuts(engine, handler, pm, all_component_names, my_component_names, ((Folder)item).getFolderPageId());
            }
        }
    }

    private static void gatherAndRemoveDuplicateShortcuts(Handler handler, final Page page, ArrayList<Shortcut> shortcuts) {
        for(int i=page.items.size()-1; i>=0; i--) {
            final Item item = page.items.get(i);
            if(item instanceof Folder) {
                Folder f = (Folder) item;
                gatherAndRemoveDuplicateShortcuts(handler, f.getOrLoadFolderPage(), shortcuts);
            } else if(item.getClass() == Shortcut.class) {
                Shortcut s = (Shortcut) item;
                ComponentName cn = s.getIntent().getComponent();
                String label = s.getLabel();
                if(cn != null && s != null) {
                    boolean found = false;
                    for(Shortcut e : shortcuts) {
                        if(cn.compareTo(e.getIntent().getComponent())==0 && label.equals(e.getLabel())) {
                            found = true;
                            break;
                        }
                    }
                    if(found) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                page.removeItem(item, false);
                            }
                        });
                    } else {
                        shortcuts.add(s);
                    }
                }
            }
        }
    }

    public static void refreshAppDrawerShortcuts(LightningEngine engine, Handler handler) {
        long now=System.currentTimeMillis();

        HashMap<String, ResolveInfo> all_component_names=new HashMap<String, ResolveInfo>();
        HashSet<String> my_component_names=new HashSet<String>();


        // dresser la liste des components name android

        // parcourir et dresser la liste des components name sur les pages
                // si match: mise à jour
                // si pas présent: supprimer

        // pour tous les androids: si pas dans les pages, ajout sur la page app drawer


        PackageManager pm=LLApp.get().getPackageManager();
        Intent intent_filter=new Intent(Intent.ACTION_MAIN, null);
        intent_filter.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> ris=pm.queryIntentActivities(intent_filter, 0);
        for(ResolveInfo ri : ris) {
            ComponentName component_name=new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
            all_component_names.put(component_name.flattenToShortString(), ri);
        }

        updateOrRemoveAppDrawerShortcuts(engine, handler, pm, all_component_names, my_component_names, Page.APP_DRAWER_PAGE);

        final Page app_drawer_page = engine.getOrLoadPage(Page.APP_DRAWER_PAGE);

        ArrayList<Shortcut> shortcuts = new ArrayList<Shortcut>();
        gatherAndRemoveDuplicateShortcuts(handler, app_drawer_page, shortcuts);

        // add not found apps
        for(ResolveInfo ri : ris) {
            ComponentName component_name=new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
            String cns = component_name.flattenToShortString();
            if(!my_component_names.contains(cns)) {
                try {
                    synchronized (app_drawer_page.items) {
                        String label=ri.loadLabel(pm).toString();

                        Intent intent=new Intent();
                        intent.setComponent(component_name);
                        intent.setAction(Intent.ACTION_MAIN);
                        int id=app_drawer_page.findFreeItemId();
                        final Shortcut s=new Shortcut(app_drawer_page);
                        int[] cell=Utils.findFreeCell(app_drawer_page);
                        File icon_dir=app_drawer_page.getAndCreateIconDir();
                        s.init(id, new Rect(cell[0], cell[1], cell[0]+1, cell[1]+1), null, label, intent);
                        s.mLastUpdateTime = now;
                        File icon_file=s.getDefaultIconFile();
                        Resources rsrc = pm.getResourcesForActivity(component_name);
                        Bitmap icon=Utils.decodeScaledBitmapResource(rsrc, ri.getIconResource(), s.getStdIconSize());
                        Utils.saveIconToFile(icon_file, icon);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                synchronized (app_drawer_page.items) {
                                    app_drawer_page.addItem(s);
                                    app_drawer_page.items.notify();
                                }
                            }
                        });

                        app_drawer_page.items.wait();
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }
    
    public static void saveIconToFile(File icon_file, Bitmap icon) {
    	FileOutputStream fos=null;
        try {
        	fos=new FileOutputStream(icon_file);
            icon.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch(Exception e) {
        	icon_file.delete();
        } finally {
        	if(fos!=null) try { fos.close(); } catch(Exception e) {}
        }
    }
    
	public static JSONArray getMatrixAsJSONArray(Matrix transform) throws JSONException {
		float[] values=new float[9];
		transform.getValues(values);
		JSONArray json_values=new JSONArray();
		for(float v : values) json_values.put(v);
		return json_values;
	}
	
	public static void removeViewFromItsParent(View v) {
		ViewGroup vg=(ViewGroup)v.getParent();
		if(vg!=null) vg.removeView(v);
	}

    public static int[] findFreeCell(Page page) {
        int x=0, y=0;
        ArrayList<Item> items = page.items;
        PageConfig c = page.config;
        int x_max = c.gridPColumnNum;
        for(;;) {
            boolean free=true;
            for(Item i : items) {
                if(i.isAppDrawerHidden()) continue;
                sTmpRect1.set(x, y, x+1, y+1);
                if(Rect.intersects(sTmpRect1, i.getCell())) {
                    free=false;
                    break;
                }
            }

            if(free) {
                break;
            }

            if(c.scrollingDirection == PageConfig.ScrollingDirection.X) {
                x++;
                if(x==x_max) {
                    y++;
                    if(y==c.gridPRowNum) {
                        x_max+=c.gridPColumnNum;
                        y=0;
                    } else {
                        x-=c.gridPColumnNum;
                    }
                }
            } else {
                x++;
                if(x>=c.gridPColumnNum) {
                    y++;
                    x=0;
                }
            }
        }

        int[] pts=new int[2];
        pts[0]=x;
        pts[1]=y;

        return pts;
    }

    public static void layoutItemsInTable(PageConfig page_config, ArrayList<Item> items, boolean portrait) {
        int x=0, y=0;
        int max_x, max_y;
        boolean horizontal=page_config.scrollingDirection==PageConfig.ScrollingDirection.X;
        if(portrait) {
            max_x = page_config.gridPColumnNum;
            max_y = page_config.gridPRowNum;
        } else {
            max_x = page_config.gridLColumnNum;
            max_y = page_config.gridLRowNum;
        }

        // horizontal layout need a more clever algorithm
        for(Item i : items) {
            Class<? extends Item> cl = i.getClass();
            if(cl != Shortcut.class && cl != Folder.class) continue;
            if(!i.getItemConfig().onGrid) continue;
            if(i.getItemConfig().pinMode!= ItemConfig.PinMode.NONE) continue;
            if(i.isAppDrawerHidden()) continue;
            i.getCell().set(x, y, x+1, y+1);
            x++;
            if((x%max_x)==0) {
                y++;
                if(horizontal) {
                    if(y==max_y) {
                        y=0;
                    } else {
                        x-=max_x;
                    }
                } else {
                    x=0;
                }
            }
        }
    }

    public static Bitmap loadScreenSizedBitmap(WindowManager wm, String path) {
        Display d = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        try {
            // API 17
            Method getRealMetrics = d.getClass().getMethod("getRealMetrics", DisplayMetrics.class);
            getRealMetrics.invoke(d, metrics);
        } catch (Exception e) {
            d.getMetrics(metrics);
        }

        final int width = metrics.widthPixels;
        final int height = metrics.heightPixels;
        if (new File(path).exists()) {
            try {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, o);
                int sample_size = 1;
                while (o.outWidth > width || o.outHeight > height) {
                    sample_size *= 2;
                    o.outWidth /= 2;
                    o.outHeight /= 2;
                }
                o = new BitmapFactory.Options();
                o.inSampleSize = sample_size;
                return BitmapFactory.decodeFile(path, o);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static Bitmap loadBitmap(File f, int max_pixels, int max_width, int max_height) {
        if(f.exists()) {
            try {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                final String path = f.getAbsolutePath();
                BitmapFactory.decodeFile(path, o);
                int sample_size = 1;
                if(max_pixels != 0) {
                    while (o.outWidth * o.outHeight > max_pixels) {
                        sample_size *= 2;
                        o.outWidth /= 2;
                        o.outHeight /= 2;
                    }
                }
                if(max_width != 0) {
                    while (o.outWidth > max_width) {
                        sample_size *= 2;
                        o.outWidth /= 2;
                        o.outHeight /= 2;
                    }
                }
                if(max_height != 0) {
                    while (o.outHeight > max_height) {
                        sample_size *= 2;
                        o.outWidth /= 2;
                        o.outHeight /= 2;
                    }
                }
                // select the upper limit sample size
                if(sample_size>1 && (o.outWidth < max_width || o.outHeight < max_height)) {
                    sample_size /= 2;
                }
                o = new BitmapFactory.Options();
                o.inSampleSize = sample_size;
                if(Build.VERSION.SDK_INT>=11) {
                    o.inMutable = true;
                }
                Bitmap bitmap = BitmapFactory.decodeFile(path, o);
                if(bitmap != null) {
                    if(sample_size > 1 && NinePatch.isNinePatchChunk(bitmap.getNinePatchChunk())) {
                        // too bad, this is a nine patch, need to decode it again with its full size
                        bitmap.recycle();
                        o = new BitmapFactory.Options();
                        if(Build.VERSION.SDK_INT>=11) {
                            o.inMutable = true;
                        }
                        bitmap = BitmapFactory.decodeFile(path, o);
                        if(bitmap == null) {
                            return null;
                        }
                    }
                    bitmap.setDensity(Bitmap.DENSITY_NONE);
                    return bitmap;
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static Graphics loadGraphics(File f) {
        Graphics graphics = null;
        if(Utils.isGifFile(f)) {
            AnimationDecoder animationDecoder = Utils.loadGifDecoder(f);
            graphics = animationDecoder == null ? null : new Graphics(animationDecoder, animationDecoder.getWidth(), animationDecoder.getHeight());
        } else if(Utils.isSvgFile(f)) {
            SvgDrawable svgDrawable = new SvgDrawable(f);
            graphics = new Graphics(svgDrawable);
        }

        if(graphics == null) {
            Bitmap bitmap = Utils.loadBitmap(f, 0, 0, 0);
            graphics = bitmap == null ? null : new Graphics(bitmap);
        }

        return graphics;
    }

    public static SharedAsyncGraphicsDrawable loadDrawable(File f) {
        Graphics graphics = loadGraphics(f);

        if(graphics == null) {
            return null;
        } else {
            return new SharedAsyncGraphicsDrawable(graphics, true);
        }
    }

    public static AnimationDecoder loadGifDecoder(File file) {
        AnimationDecoder animationDecoder = new AnimationDecoder();
        if(animationDecoder.read(file)) {
            return animationDecoder;
        } else {
            return null;
        }
    }

    public static boolean encodeAnimationToGif(File file, SharedAsyncGraphicsDrawable drawable) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            AnimatedGifEncoder encoder = new AnimatedGifEncoder();
            encoder.start(fos);
            encoder.setRepeat(drawable.getAnimationLoopCount());
            encoder.setTransparent(0);
            int n = drawable.getAnimationFrameCount();
            for(int i=0; i<n; i++) {
                encoder.setDelay(drawable.getAnimationFrameDelay(i));
                encoder.addFrame(drawable.getAnimationFrameBitmap(i));
            }
            encoder.finish();
            return true;
        } catch(IOException e) {
            file.delete();
            return false;
        } finally {
            if(fos != null) try { fos.close(); } catch (IOException e) {}
        }
    }

    public static boolean isGifFile(File file) {
        /*
        png 89 50 4E 47 0D 0A 1A 0A
        jpg FF D8 FF
        bmp 42 4D
        gif 47 49 46 38
         */
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] header = new byte[4];
            fis.read(header);
            return header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x38;
        } catch (IOException e) {
            return false;
        } finally {
            if(fis != null) try { fis.close(); } catch(IOException e) {}
        }
    }

    public static boolean isSvgFile(File file) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] header = new byte[5];
            fis.read(header);
            return (header[0] == '<' && header[1] == '?' && header[2] == 'x' && header[3] == 'm' && header[4] == 'l') ||
                   (header[0] == '<' && header[1] == 's' && header[2] == 'v' && header[3] == 'g');
        } catch (IOException e) {
            return false;
        } finally {
            if(fis != null) try { fis.close(); } catch(IOException e) {}
        }
    }

    public static Bitmap createIconFromText(int size, String text) {
        return createIconFromText(size, text, Color.WHITE);
    }

    public static Bitmap createIconFromText(int size, String text, int color) {
        return createIconFromText(size, text, color, Color.TRANSPARENT, LLApp.get().getIconsTypeface());
    }

    public static Bitmap createIconFromText(int size, String text, int fgColor, int bgColor, Typeface typeface) {
        Bitmap icon = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        icon.eraseColor(bgColor);
        Canvas canvas = new Canvas(icon);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(fgColor);
        paint.setTextSize(size*3/4);
        paint.setTypeface(typeface);
        paint.setShadowLayer(1, 1, 1, 0x80000000);
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float w = paint.measureText(text);
        canvas.drawText(text, (size-w)/2, (size-fontMetrics.ascent-fontMetrics.descent)/2, paint);
//        canvas.drawLine(size / 2, 0, size / 2, size, paint);
//        canvas.drawLine(0, size/2, size, size/2, paint);

        return icon;
    }

    public static Bitmap createBitmapFromDrawable(Drawable drawable) {
        try {
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            drawable.setBounds(0, 0, width, height);
            drawable.draw(new Canvas(bitmap));
            return bitmap;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    public static File getTmpImageFile() {
        File tmp_dir = FileUtils.LL_TMP_DIR;
        tmp_dir.mkdirs();
        return new File(tmp_dir, "pick_me");
    }

    public static EventAction decodeEventActionFromLightningIntent(Intent intent) {
        EventAction eventAction = null;
        String s_ea = intent.getStringExtra(LightningIntent.INTENT_EXTRA_EVENT_ACTION);
        if(s_ea != null) {
            try {
                eventAction = new EventAction();
                JSONObject json = new JSONObject(s_ea);
                JsonLoader.loadFieldsFromJSONObject(eventAction, json, null);
            } catch (Exception e) {
                eventAction = null;
            }
        } else {
            int intentAction = intent.getIntExtra(LightningIntent.INTENT_EXTRA_ACTION, GlobalConfig.NOTHING);
            if(intentAction != GlobalConfig.NOTHING) {
                String intentData = intent.getStringExtra(LightningIntent.INTENT_EXTRA_DATA);
                eventAction = new EventAction(intentAction, intentData);
            }
        }
        return eventAction;
    }


    public static final class ShortcutDescription {
		public String name;
		public Bitmap icon;
		public Intent intent;
	}
	
	public static ShortcutDescription createShortcutFromIntent(Context context, Intent data, int max_icon_size) {
    	// this comes from Launcher2
        Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        Parcelable bitmap = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);

        Bitmap icon = null;
        ShortcutIconResource iconResource = null;

        if (bitmap != null && bitmap instanceof Bitmap) {
            icon = (Bitmap) bitmap;
        } else {
            Parcelable extra = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
            if (extra != null && extra instanceof ShortcutIconResource) {
                try {
                    iconResource = (ShortcutIconResource) extra;
                    final PackageManager packageManager = context.getPackageManager();
                    Resources resources = packageManager.getResourcesForApplication(iconResource.packageName);
                    final int id = resources.getIdentifier(iconResource.resourceName, null, null);
                    icon = Utils.decodeScaledBitmapResource(resources, id, max_icon_size);
                } catch (Exception e) {
                }
            }
        }

        ShortcutDescription sd=new ShortcutDescription();
        sd.name=name;
        sd.icon=icon;
        sd.intent=intent;
        
        return sd;
	}
	
	public static void deleteDirectory(File dir, boolean delete_root) {
		File[] files=dir.listFiles();
		if(files!=null) {
			for(File f : files) {
				if(f.isDirectory()) {
					deleteDirectory(f, true);
				} else {
					f.delete();
				}
			}
		}
		if(delete_root) dir.delete();
	}
	
	public static Intent getFolderIntent(int id) {
		return new Intent("net.pierrox.lightning_launcher.FOLDER", Uri.parse("llf://"+id));
	}
	
	public static void setEnabledStateOnViews(View v, boolean enabled) {
		v.setEnabled(enabled);

		if (v instanceof ViewGroup) {
			final ViewGroup vg = (ViewGroup) v;
			for (int i = vg.getChildCount() - 1; i >= 0; i--) {
				setEnabledStateOnViews(vg.getChildAt(i), enabled);
			}
		}
	}

    public static void updateFolderIconStyle(Page page) {
        for (Item i : page.items) {
            if (i instanceof Folder) {
                Folder folder = (Folder)i;
                ShortcutConfig.getIconBackFile(page.getIconDir(), folder.getId()).delete();
                Utils.updateFolderIcon(folder);
            }
        }
    }

	public static void updateFolderIcon(Folder folder) {
        Page folder_page = folder.getOrLoadFolderPage();
		FolderConfig fc = folder.getFolderConfig();
        FolderConfig.FolderIconStyle style=fc.iconStyle;
        
        Page folder_container = folder.getPage();
        File folder_container_icon_dir=folder_container.getAndCreateIconDir();
        
        File icon_effect_background;
        icon_effect_background = ShortcutConfig.getIconBackFile(folder_container_icon_dir, folder.mId);
        	
        Bitmap icon;
        if(style==FolderIconStyle.NORMAL) {
        	icon=null; // do not update the icon, keep the current one
        } else {
	        ArrayList<Item> folder_items=new ArrayList<Item>(folder_page.items);
            final float cw = folder_page.getCurrentViewCellWidth();
            final float ch = folder_page.getCurrentViewCellHeight();

            Collections.sort(folder_items, new Comparator<Item>() {
                private float[] pos_l=new float[2];
                private float[] pos_r=new float[2];
                @Override
                public int compare(Item lhs, Item rhs) {
                    if(lhs.getItemConfig().onGrid) {
                        Rect cell = lhs.getCell();
                        pos_l[0]= cell.left*cw;
                        pos_l[1]= cell.top*ch;
                    } else {
                        pos_l[0]=pos_l[1]=0;
                        lhs.getTransform().mapPoints(pos_l);
                    }
                    if(rhs.getItemConfig().onGrid) {
                        Rect cell = rhs.getCell();
                        pos_r[0]= cell.left*cw;
                        pos_r[1]= cell.top*ch;
                    } else {
                        pos_r[0]=pos_r[1]=0;
                        rhs.getTransform().mapPoints(pos_r);
                    }

                    if(pos_l[1]<pos_r[1]) {
                        return -1;
                    } else if(pos_l[1]>pos_r[1]) {
                        return 1;
                    } else {
                        if(pos_l[0]<pos_r[0]) {
                            return -1;
                        } else if(pos_l[0]>pos_r[0]) {
                            return 1;
                        }
                        return 0;
                    }
                }
            });

	        int icon_width=folder.getStdIconSize();
	        int icon_height=icon_width;
	        icon=Bitmap.createBitmap(icon_width, icon_height, Bitmap.Config.ARGB_8888);
	        Canvas canvas=new Canvas(icon);
	        
	        final float margin=0.03125f;
	        
	        canvas.drawARGB(0, 0, 0, 0);
	        
	        Paint bg_paint=new Paint(Paint.ANTI_ALIAS_FLAG);
	        bg_paint.setShader(new RadialGradient(icon_width/2, icon_height/2, (float) Math.sqrt(icon_width*icon_width+icon_height*icon_height), 0xff59514c, 0xff272424, TileMode.CLAMP));
	        canvas.drawRoundRect(new RectF(0, 0, icon_width, icon_height), margin*2*icon_width, margin*2*icon_height, bg_paint);
	        
	        
	        Paint bitmap_paint=new Paint(Paint.FILTER_BITMAP_FLAG|Paint.ANTI_ALIAS_FLAG);
	        
	        if(style==FolderIconStyle.GRID_2_2) {
	        	final int w=2;
	        	final int h=2;
	        	
	        	final float bw=icon_width/(float)w;
	        	final float bh=icon_height/(float)h;
	        	final float mw=margin*icon_width;
	        	final float mh=margin*icon_height;
	        	final int max=w*h;
		        
		        Paint line_paint=new Paint(Paint.ANTI_ALIAS_FLAG);
		        line_paint.setColor(0xff8C7F77);
		        canvas.drawLine(icon_width/2+0.5f, mh, icon_width/2+0.5f, icon_height-mh, line_paint);
		        canvas.drawLine(mw, icon_height/2+0.5f, icon_width-mh, icon_height/2+0.5f, line_paint);
		        
		        storeFolderIconBackground(icon_effect_background, folder_container_icon_dir, folder, icon);
		        
		        int l=folder_items.size(); 
		        int x=0;
		        int y=0;
		        int n=0;
		        int count=0;
		        RectF dst=new RectF();
		        
		        while(n<l) {
		        	Item item=folder_items.get(n);
		        	Bitmap b=getItemIcon(item);
		        		
					if(b!=null) {
						dst.set((int)(bw*x+mw)+0.5f, (int)(bh*y+mh)+0.5f, (int)(bw*(x+1)-mw)+0.5f, (int)(bh*(y+1)-mh)+0.5f);
						canvas.drawBitmap(b, null, dst, bitmap_paint);
		        		count++;
			        	if(count==max) {
			        		break;
			        	}
                        x++;
                        if(x==w) {
                            y++;
                            x=0;
                        }
                    }


		        	n++;
	        	}
	        	
	        	
	        } else {
	        	storeFolderIconBackground(icon_effect_background, folder_container_icon_dir, folder, icon);
		        
	        	final int max=4;
	        	final float mw=margin*icon_width;
	        	final float mh=margin*icon_height;
	        	
	        	RectF dst=new RectF();
	        	int n=folder_items.size();
	        	if(n>max) n=max;
	        	if(n==1) {
	        		dst.set(mw, mh, icon_width-mw, icon_height-mh);
	        		Bitmap b=getItemIcon(folder_items.get(0));
	        		if(b!=null) {
	        			canvas.drawBitmap(b, null, dst, bitmap_paint);
	        		}
	        	} else if(n>1) {
	        		//bitmap_paint.setAlpha(170);
	        		float scale;
	        		switch(n) {
	        		case 2 : scale=0.7f; break;
	        		case 3 : scale=0.6f; break;
	        		default: scale=0.5f;
	        		}
	        		
	        		final float dx=-(icon_width-mw*2-icon_width*scale)/(n-1);
	        		final float dy=(icon_width-mw*2-icon_width*scale)/(n-1);
	        		
	        		float right=icon_width-mw;
	        		dst.set(right-icon_width*scale, mh, right, mh+icon_height*scale);
	        		for(int i=n-1; i>=0; i--) {
	        			Bitmap b=getItemIcon(folder_items.get(i));
		        		if(b!=null) {
		        			canvas.drawBitmap(b, null, dst, bitmap_paint);
		        			dst.offset(dx, dy);
		        		}
	        		}
	        	}
	        }
        }
        
        if(icon!=null) {
			File icon_file=folder.getCustomIconFile();
			Utils.saveIconToFile(icon_file, icon);
            folder.notifyChanged();
        }
	}
	
	private static void storeFolderIconBackground(File icon_effect_background, File folder_container_icon_dir, Folder folder, Bitmap icon) {
		if(!icon_effect_background.exists()) {
        	Utils.saveIconToFile(icon_effect_background, icon);
    		ShortcutConfig new_sc = folder.modifyShortcutConfig();
    		new_sc.loadAssociatedIcons(folder_container_icon_dir, folder.mId);
        }
		icon.eraseColor(0);
	}

    public static void updateContainerIconIfNeeded(Page page) {
        if(page.isFolder() && page.id != Page.USER_MENU_PAGE) {
            ArrayList<Folder> openers = page.findAllOpeners();
            for (Folder folder : openers) {
                if(folder.getClass() == Folder.class) {
                    updateFolderIcon(folder);
                }
            }
        }
    }

    public static ItemView findItemViewInAppScreens(Item item) {
        // try to find an existing view in a screen, first in the active screen, otherwise in other screens
        ItemView[] itemViews = null;

        LLApp app = LLApp.get();
        Screen screen = app.getActiveScreen();
        if(screen != null) {
            itemViews = screen.getItemViewsForItem(item);
        }
        if(itemViews.length == 0) {
            for (Screen s : app.getScreens()) {
                itemViews = s.getItemViewsForItem(item);
                if(itemViews.length > 0) {
                    break;
                }
            }
        }

        return itemViews.length==0 ? null : itemViews[0];
    }

	private static Bitmap getItemIcon(Item item) {
		Bitmap b=null;
		File icon_file=item.getCustomIconFile();
		if(!icon_file.exists()) {
			icon_file=item.getDefaultIconFile();
		}
        Class<?> cls = item.getClass();
    	if(cls == Shortcut.class || cls ==Folder.class) {
    		try {
				b=BitmapFactory.decodeStream(new FileInputStream(icon_file));
			} catch (FileNotFoundException e) {
				b=null;
			}
    	} else if(cls == Widget.class) {
            // try to find an existing view in a screen, first in the active screen, otherwise in other screens
            View v = findItemViewInAppScreens(item);

    		if(v!=null) {
    			boolean dc=v.isDrawingCacheEnabled();
    			v.setDrawingCacheEnabled(true);
    			Bitmap cache=v.getDrawingCache(false);
    			if(cache!=null) {
    				b=Utils.createStandardSizedIcon(cache, Utils.getStandardIconSize());
    				Utils.saveIconToFile(icon_file, b);
    			} else {
    				try {
    					b=BitmapFactory.decodeStream(new FileInputStream(icon_file));
    				} catch (Throwable e) {
    					b=null;
    				}
    			}
    			v.setDrawingCacheEnabled(dc);
    		}
    	}
    	return b;
	}

	/*public static int findRootPage(Page page) {
		if(page.isFolder() && page.id != Page.USER_MENU_PAGE) {
			Folder f = page.findFirstOpener();
			if(f != null) {
				return findRootPage(f.getPage());
			} else {
				return Page.FIRST_DASHBOARD_PAGE;
			}
		} else {
			return page.id;
		}
	}*/

    public static String getPackageNameForItem(Item item) {
        String pkg = null;
        ComponentName cn = null;
        Class<?> item_class = item.getClass();
        if(item_class == Shortcut.class || item_class == DynamicText.class) {
            Intent intent = ((Shortcut) item).getIntent();
            if(intent != null) {
                cn = intent.getComponent();
            }
        } else if(item_class == Widget.class) {
            cn = ((Widget)item).getComponentName();
        }

        if(cn != null) {
            pkg = cn.getPackageName();
        }

        return pkg;
    }
	
	public static boolean startActivitySafely(Context context, Intent intent, int msg_id) {
		try {
			context.startActivity(intent);
            return true;
        } catch(Exception e) {
            if(msg_id!=0) Toast.makeText(context, msg_id, Toast.LENGTH_SHORT).show();
            return false;
        }
	}

    public static void findPagesInItems(Page page, ArrayList<Page> all_pages) {
        for(Item i : page.items) {
            if(i instanceof Folder) {
                Page folder_page = ((Folder) i).getOrLoadFolderPage();
                if(!all_pages.contains(folder_page)) {
                    all_pages.add(folder_page);
                    findPagesInItems(folder_page, all_pages);
                }
            }
        }
    }
	
	public static final String INTENT_EXTRA_ITEM_ID="i";



    public static Item cloneItem(Item from) {
        Page page = from.getPage();

        int new_id=page.findFreeItemId();

        try {
            JSONObject json = from.toJSONObject();
            json.put(JsonFields.ITEM_ID, new_id);
            if(from instanceof Widget) {
                json.put(JsonFields.WIDGET_APP_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            }

            File iconDir = page.getIconDir();
            FileUtils.copyItemFiles(from.getId(), iconDir, new_id, iconDir);

            Item to = Item.loadItemFromJSONObject(page, json);

            if(to instanceof Folder) {
                LightningEngine.PageManager pm = page.getEngine().getPageManager();
                Folder folder = (Folder) to;
                int folder_page_id = folder.getFolderPageId();
                Page folder_page = pm.getOrLoadPage(folder_page_id);

                int new_page_id = pm.clonePage(folder_page, false).id;
                folder.setFolderPageId(new_page_id);
            }

            if(to.getItemConfig().onGrid) {
                to.getCell().offset(1, 0);
            } else {
                int dx = Utils.getStandardIconSize() / 2;
                to.getTransform().postTranslate(dx, dx);
            }
            page.addItem(to);

            return to;
        } catch (JSONException e) {
            return null;
        }
    }
	
	
	public static Shortcut copyShortcut(Item shortcut, Page pageTo, int x, int y, float scale) {
		Page pageFrom = shortcut.getPage();

        int new_id=pageTo.findFreeItemId();

        Item item_to=null;

        File icon_dir_from=pageFrom .getIconDir();
        File icon_dir_to=pageTo.getAndCreateIconDir();

        FileUtils.copyItemFiles(shortcut.getId(), icon_dir_from, new_id, icon_dir_to);

        try {
            JSONObject json_item=shortcut.toJSONObject();
            json_item.put(JsonFields.ITEM_ID, new_id);
            json_item.put(JsonFields.ITEM_TYPE, Item.Type.SHORTCUT); // enforce shortcut, even if the source is a folder
            item_to=Item.loadItemFromJSONObject(pageTo, json_item);
            item_to.setAppDrawerHidden(false);
            setNewItemOnGrid(item_to, pageTo);
        } catch (JSONException e) {
            // pass, not likely to happen
        }

		setItemPosition(pageTo, item_to, x, y, scale, true);

		pageTo.addItem(item_to);
		
		return (Shortcut) item_to;
	}
	
	public static Item addAndroidShortcutFromIntent(Context context, Intent data, Page page, int x, int y, float scale) {
		int icon_size=(int)(page.config.defaultShortcutConfig.iconScale*getStandardIconSize());
		ShortcutDescription sd=createShortcutFromIntent(context, data, icon_size);

        return addShortcut(sd.name, sd.icon, sd.intent, page, x, y, scale, true);
	}

    public static Shortcut addShortcut(String label, Bitmap icon, Intent intent, Page page, float x, float y, float scale, boolean center) {
        int new_id=page.findFreeItemId();
        Shortcut shortcut=new Shortcut(page);
        shortcut.init(new_id, new Rect(0, 0, 1, 1), null, label, intent);
        shortcut.getDefaultIconFile().delete();
        shortcut.getCustomIconFile().delete();
        if(icon != null) {
            File icon_file=shortcut.getDefaultIconFile();
            icon_file.getParentFile().mkdirs();
            Utils.saveIconToFile(icon_file, icon);
            icon.recycle();
        }
        setNewItemOnGrid(shortcut, page);
        setItemPosition(page, shortcut, x, y, scale, center);

        page.addItem(shortcut);

        return shortcut;
    }

	public static Item moveItem(Item item, Page pageTo, int x, int y, float scale, int new_id) {
        Page pageFrom = item.getPage();

        if(new_id == Item.NO_ID) {
            new_id = pageTo.findFreeItemId();
        }

        Item itemTo=null;

        File icon_dir_from=pageFrom.getIconDir();
        File icon_dir_to=pageTo.getAndCreateIconDir();

        File file_from, file_to;
        for (String code : FileUtils.ALL_SUFFIXES) {
            file_from = new File(icon_dir_from, item.getId()+code);
            file_to = new File(icon_dir_to, new_id+code);
            if(file_from.exists()) {
                file_from.renameTo(file_to);
            } else {
                file_to.delete();
            }
        }

        try {
            JSONObject json_item=item.toJSONObject();
            json_item.put(JsonFields.ITEM_ID, new_id);
            itemTo=Item.loadItemFromJSONObject(pageTo, json_item);
        } catch (JSONException e) {
            e.printStackTrace();
        }

		if(item instanceof Shortcut) {
            item.getDefaultIconFile().renameTo(itemTo.getDefaultIconFile());
            item.getCustomIconFile().renameTo(itemTo.getCustomIconFile());
		}

		if(itemTo instanceof Folder) {
			Folder f=(Folder)itemTo;
			f.setIntent(getFolderIntent(new_id));
		}

		if(itemTo instanceof Widget) {
			// need to move preferences
			Widget w=(Widget)itemTo;
			int app_widget_id=w.getAppWidgetId();
			if(app_widget_id!=Widget.NO_APP_WIDGET_ID) {
				try {
					ComponentName cn=w.getComponentName();
					Context widget_context=LLApp.get().createPackageContext(cn.getPackageName(), Context.CONTEXT_INCLUDE_CODE);
					File old_shared_prefs=new File(widget_context.getFilesDir().getParent()+"/shared_prefs/"+item.getId()+".xml");
					if(old_shared_prefs.exists()) {
						File new_shared_prefs=new File(widget_context.getFilesDir().getParent()+"/shared_prefs/"+new_id+".xml");
						old_shared_prefs.renameTo(new_shared_prefs);
					}
	//						SharedPreferences old_prefs=widget_context.getSharedPreferences(String.valueOf(id), Context.MODE_WORLD_READABLE);
	//						Map<String, ?> values=old_prefs.getAll();
	//						if(values.size()>0) {
	//							SharedPreferences new_prefs=widget_context.getSharedPreferences(String.valueOf(new_id), Context.MODE_WORLD_READABLE);
	//							Editor ed=new_prefs.edit();
	//							for(String k : values.keySet()) {
	//								???
	//							}
	//							ed.commit();
	//							old_prefs.edit().clear().commit();
	//						}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

        setItemPosition(pageTo, itemTo, x, y, itemTo.getViewWidth(), itemTo.getViewHeight(), scale, true);
        Rect cell_from = item.getCell();
        Rect cell_to = itemTo.getCell();
        cell_to.right = cell_to.left + cell_from.width();
        cell_to.bottom = cell_to.top + cell_from.height();

        pageFrom.removeItem(item, true);
        pageTo.addItem(itemTo);

		return itemTo;
	}

	public static Folder addFolder(Page page, float x, float y, float scale, boolean center, String label) {
    	int new_id=page.findFreeItemId();

        File baseDir = page.getEngine().getBaseDir();
        int folder_page=Page.reservePage(baseDir, true);

        try {
            File out = Page.getPageConfigFile(baseDir, folder_page);
            PageConfig folder_page_config = new PageConfig();
            folder_page_config.copyFrom(page.config);
            folder_page_config.useDesktopSize = true;
            JSONObject o = JsonLoader.toJSONObject(folder_page_config, page.config);
            FileUtils.saveStringToFile(o.toString(), out);
        } catch(Exception e) {
            return null;
        }

    	Intent folder_intent=getFolderIntent(new_id);

        Folder f=new Folder(page);
        page.getAndCreateIconDir();
        f.init(new_id, new Rect(0, 0, 1, 1), null, label, folder_intent, folder_page);
        Bitmap icon=Utils.createIconFromText(f.getStdIconSize(), "f");
        Utils.saveIconToFile(f.getDefaultIconFile(), icon);
        setNewItemOnGrid(f, page);
        setItemPosition(page, f, x, y, scale, center);

        page.addItem(f);

        return f;
	}

	public static EmbeddedFolder addEmbeddedFolder(Page page, float x, float y, float width, float height, float scale, boolean center) {
    	int new_id=page.findFreeItemId();

        File base_dir = page.getEngine().getBaseDir();
    	int folder_page=Page.reservePage(base_dir, true);

        try {
            File out = Page.getPageConfigFile(base_dir, folder_page);
            PageConfig folder_page_config = new PageConfig();
            folder_page_config.copyFrom(page.config);
            folder_page_config.gridPRowNum = 1;
            folder_page_config.gridLRowNum = 1;
            JSONObject o = JsonLoader.toJSONObject(folder_page_config, page.config);
            FileUtils.saveStringToFile(o.toString(), out);
        } catch(Exception e) {
            return null;
        }

    	Intent folder_intent=getFolderIntent(new_id);

        EmbeddedFolder ef=new EmbeddedFolder(page);
        ef.init(new_id, new Rect(0, 0, 4, 1), null, "", folder_intent, folder_page);
        setNewItemOnGrid(ef, page);
        setItemPosition(page, ef, x, y, width, height, scale, center);
        ef.setViewWidth(Math.round(width));
        ef.setViewHeight(Math.round(height));
        ItemConfig ic = new ItemConfig();
        ic.copyFrom(ef.getItemConfig());
        ic.selectionEffect = ItemConfig.SelectionEffect.PLAIN;
        Box box = ic.box;
        int[] size = box.size;
        int[] bc = box.border_color;
        size[Box.BL] = size[Box.BR] = size[Box.BT] = size[Box.BB] = 1;
        box.ccn = box.ccs = box.ccf = Color.TRANSPARENT;
        Arrays.fill(bc, Color.WHITE);
        ic.box_s = box.toString(page.config.defaultItemConfig.box);
        ef.setItemConfig(ic);

        page.addItem(ef, 0);

        return ef;
	}

    public static Unlocker addUnlocker(Page page, int x, int y, int width, int height, float scale, boolean center) {
        int new_id=page.findFreeItemId();

        Unlocker ul=new Unlocker(page);
        ul.init(new_id, new Rect(0, 0, 1, 1), null);
        ItemConfig ic=new ItemConfig();
        ic.copyFrom(ul.getItemConfig());
        ic.box.ccs=Color.TRANSPARENT;
        ic.box_s=ic.box.toString(page.config.defaultItemConfig.box);
        ul.setItemConfig(ic);
        ul.setViewWidth(width);
        ul.setViewHeight(height);
        setNewItemOnGrid(ul, page);
        setItemPosition(page, ul, x, y, width, height, scale, center);

        page.addItem(ul);

        return ul;
    }

    public static PageIndicator addPageIndicator(Page page, float x, float y, float scale, boolean center) {
        int new_id=page.findFreeItemId();

        PageIndicator pi=new PageIndicator(page);
        pi.init(new_id, new Rect(0, 0, 1, 1), null);
        setNewItemOnGrid(pi, page);
        setItemPosition(page, pi, x, y, scale, center);

        page.addItem(pi);

        return pi;
    }

    public static CustomView addCustomView(Page page, float x, float y, float scale) {
        int new_id=page.findFreeItemId();
        CustomView cv = new CustomView(page);
        cv.init(new_id, new Rect(0, 0, 1, 1), null);
        setNewItemOnGrid(cv, page);
        setItemPosition(page, cv, x, y, scale, true);

        page.addItem(cv);

        return cv;
    }

    public static DynamicText addDynamicText(Page page, DynamicTextConfig.Source source, boolean on_grid) {
        int new_id=page.findFreeItemId();

        DynamicText dt=new DynamicText(page);
        dt.init(new_id, new Rect(0, 0, 1, 1), null, source);
        ItemConfig ic = new ItemConfig();
        ic.copyFrom(dt.getItemConfig());
        ic.onGrid = on_grid;
        dt.setItemConfig(ic);

        page.addItem(dt);

        return dt;
    }

	public static StopPoint addStopPoint(Page page, float x, float y, float scale, boolean center) {
    	int new_id=page.findFreeItemId();
    	StopPoint stop_point=new StopPoint(page);
    	stop_point.init(new_id, new Rect(0, 0, 1, 1), null);
        setNewItemOnGrid(stop_point, page);
        int s = Utils.getStandardIconSize();
        if(x != POSITION_AUTO) {
	        int hs = s/2;
	        x+=hs;
	        y+=hs;
        }
        setItemPosition(page, stop_point, x, y, s, s, scale, center);
        stop_point.setViewWidth(s);
        stop_point.setViewHeight(s);


    	page.addItem(stop_point);

        return stop_point;
	}

	public static Widget replaceAppWidget(Page page, int app_widget_id, int replaced_app_widget_id) {
		Widget replaced_app_widget = (Widget) page.findItemById(replaced_app_widget_id);
        if(replaced_app_widget == null) {
            return null;
        }

        LLApp app = LLApp.get();
        AppWidgetManager app_widget_manager=AppWidgetManager.getInstance(app);
		AppWidgetProviderInfo app_widget_info=app_widget_manager.getAppWidgetInfo(app_widget_id);
		app.getAppWidgetHost().deleteAppWidgetId(replaced_app_widget.getId());
		replaced_app_widget.setAppWidgetId(app_widget_id);
		replaced_app_widget.setComponentName(app_widget_info.provider);
        replaced_app_widget.notifyChanged();

		return replaced_app_widget;
	}

	public static Widget addAppWidget(Page page, ItemLayout il, int app_widget_id, int x, int y, float scale) {
		AppWidgetManager app_widget_manager=AppWidgetManager.getInstance(LLApp.get());
		AppWidgetProviderInfo app_widget_info=app_widget_manager.getAppWidgetInfo(app_widget_id);

		int new_id=page.findFreeItemId();
		Widget widget=Widget.createStatic(page, new_id, new Rect(0, 0, 0, 0), null, app_widget_info.provider, app_widget_id);
		ItemConfig ic=new ItemConfig();
		ic.copyFrom(widget.getItemConfig());
		ic.box.ccs=Color.TRANSPARENT;
		ic.box_s=ic.box.toString(page.config.defaultItemConfig.box);
		widget.setItemConfig(ic);

		int mw=app_widget_info.minWidth;
		int mh=app_widget_info.minHeight;
		widget.setViewWidth(mw);
		widget.setViewHeight(mh);
        setNewItemOnGrid(widget, page);
		setItemPosition(page, widget, x, y, mw, mh, scale, true);
        if(il != null && il.getCellWidth() != 0 && il.getCellHeight() != 0) {
            int cx = Math.round(mw/il.getCellWidth());
            int cy = Math.round(mh/il.getCellHeight());
            widget.mCell.offset(-(cx-1)/2, -(cy-1)/2);
        }

        page.addItem(widget);

		return widget;
	}

	public static Widget addLLWidget(Page page, ComponentName cn, int x, int y, float scale) {

		int new_id=page.findFreeItemId();
		Widget widget=Widget.createStatic(page, new_id, new Rect(0, 0, 0, 0), null, cn, Widget.NO_APP_WIDGET_ID);
        setNewItemOnGrid(widget, page);
		setItemPosition(page, widget, x, y, 0, 0, scale, true);

        page.addItem(widget);

		return widget;
	}

    public static EmbeddedFolder convertFolderToPanel(Folder folder) {
        Page page = folder.getPage();

        JSONObject json;
        try {
            json = folder.toJSONObject();
            json.put(JsonFields.ITEM_TYPE, Item.Type.EFOLDER);
            EmbeddedFolder panel = (EmbeddedFolder) Item.loadItemFromJSONObject(page, json);
            Box box=new Box();
            box.loadFromString(folder.getFolderConfig().box.toString(box), box);
            ItemConfig config = panel.modifyItemConfig();
            config.box = box;
            config.box_s = box.toString(new Box());
            int index = page.items.indexOf(folder);
            page.removeItem(folder, false);
            page.addItem(panel, index);
            return panel;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Folder convertPanelToFolder(EmbeddedFolder panel) {
        Page page = panel.getPage();

        JSONObject json;
        try {
            json = panel.toJSONObject();
            json.put(JsonFields.ITEM_TYPE, Item.Type.FOLDER);
            Folder folder = (Folder) Item.loadItemFromJSONObject(page, json);

            Box box=new Box();
            box.loadFromString(panel.getItemConfig().box.toString(box), box);
            FolderConfig folderConfig = folder.modifyFolderConfig();
            folderConfig.box = box;
            folderConfig.box_s = box.toString(new Box());

            box=new Box();
            box.loadFromString(page.config.defaultItemConfig.box.toString(box), box);
            ItemConfig itemConfig = folder.modifyItemConfig();
            itemConfig.box = box;
            itemConfig.box_s = box.toString(new Box());

            int index = page.items.indexOf(panel);
            page.removeItem(panel, false);
            page.addItem(folder, index);
            return folder;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void setNewItemOnGrid(Item item, Page page) {
        ItemConfig ic = new ItemConfig();
        ic.copyFrom(item.getItemConfig());
        ic.onGrid = page.config.newOnGrid;
        item.setItemConfig(ic);
    }

    public static final int POSITION_AUTO = Integer.MIN_VALUE;
	public static void setItemPosition(Page page, Item item, float x, float y, float scale, boolean center) {
        int w, h;
        if(item.getItemConfig().onGrid) {
            w = POSITION_AUTO;
            h = POSITION_AUTO;
        } else {
            ItemView view = item.createView(LLApp.get());
            view.init();
            int unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            view.measure(unspecified, unspecified);
            w = view.getMeasuredWidth();
            h = view.getMeasuredHeight();
            item.setViewWidth(w);
            item.setViewHeight(h);
            view.destroy();
        }
		setItemPosition(page, item, x, y, w, h, scale, center);
	}
	
	private static void setItemPosition(Page page, Item item, float x, float y, float w, float h, float scale, boolean center) {
		int cell_x, cell_y;
        float abs_x, abs_y;

        float cw = page.getCurrentViewCellWidth();
        float ch = page.getCurrentViewCellHeight();
        int cell_width = w==POSITION_AUTO ? 1 : Math.round(w/ cw);
        if(cell_width == 0) cell_width = 1;
        int cell_height = h==POSITION_AUTO ? 1 : Math.round(h/ ch);
        if(cell_height == 0) cell_height = 1;

        if(x == POSITION_AUTO || y == POSITION_AUTO) {
            int[] cell=findFreeCell(page);
            cell_x=cell[0];
            cell_y=cell[1];
            abs_x = cell_x*cw;
            abs_y = cell_y*ch;
//            RectF screen = new RectF(0, 0, page.getCurrentViewWidth(), page.getCurrentViewHeight());
//            page.getCurrentViewInverseTransform().mapRect(screen);
//            abs_x = Math.round(screen.centerX());
//            abs_y = Math.round(screen.centerY());
//            cell_x = (int) Math.floor(abs_x / cw);
//            cell_y = (int) Math.floor(abs_y / ch);
        } else {
            cell_x = (int)Math.floor(x / cw);
            cell_y = (int)Math.floor(y / ch);
            abs_x = x;
            abs_y = y;
        }

        if(item.getItemConfig().onGrid) {
            item.mCell.set(cell_x, cell_y, cell_x+cell_width, cell_y+cell_height);
        } else {
            //int s=Utils.getStandardIconSize()/2;
            Matrix transform=new Matrix();
            float is = scale==0 ? 1 : 1/scale;
            transform.postScale(is, is);
            transform.postTranslate(abs_x, abs_y);
            if(w!=POSITION_AUTO && h!=POSITION_AUTO && center) {
                transform.postTranslate(-w / 2, -h / 2);
            }
            item.setTransform(transform, false);
        }
	}

    public static void convertPinnedItemPosition(ItemLayout il, Item item, ItemConfig.PinMode prev_pin_mode, ItemConfig.PinMode new_pin_mode) {
        if((prev_pin_mode==ItemConfig.PinMode.NONE && new_pin_mode!=ItemConfig.PinMode.NONE) || (prev_pin_mode!=ItemConfig.PinMode.NONE && new_pin_mode==ItemConfig.PinMode.NONE)) {
            Matrix transform=il.getLocalTransform();
            Matrix inverse_transform=il.getLocalInverseTransform();
            if(!item.getItemConfig().onGrid) {
                Matrix t=new Matrix(item.getTransform());
                if(new_pin_mode==ItemConfig.PinMode.NONE) {
                    t.postConcat(inverse_transform);
                } else {
                    t.postConcat(transform);
                }
                item.setTransform(t, false);
            } else {
                Rect cell=item.getCell();
                float cw=il.getCellWidth();
                float ch=il.getCellHeight();
                float[] coords=new float[] { cell.left*cw, cell.top*ch };
                if(new_pin_mode==ItemConfig.PinMode.NONE) {
                    inverse_transform.mapPoints(coords);
                } else {
                    transform.mapPoints(coords);
                }
                item.getCell().offsetTo((int)(0.5f+coords[0]/cw), (int)(0.5f+coords[1]/ch));
            }
        }
    }

    public static int getVersionCode(int archAndVersionCode) {
        return archAndVersionCode % 1000;
    }

    public static int getMyPackageVersion(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            // LLX is using a version code based on <arch><version>, where version is 3 digits
            return getVersionCode(pm.getPackageInfo(context.getPackageName(), 0).versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    public interface OnTextInputDialogDone {
        public void onTextInputDone(String value);
    }

    public static Dialog createTextInputDialog(Context context, int message_res, String value, final OnTextInputDialogDone listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message_res);
        final EditText edit_text = new EditText(context);
        if(value != null) {
            edit_text.setText(value);
            edit_text.setSelection(value.length());
        }
        FrameLayout l = new FrameLayout(context);
        l.setPadding(10, 10, 10, 10);
        l.addView(edit_text);
        builder.setView(l);
        builder.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        String new_value = edit_text.getText().toString();
                        listener.onTextInputDone(new_value);
                    }
                });
        builder.setNegativeButton(android.R.string.cancel, null);
        return builder.create();
    }

    public interface OnFolderSelectionDialogDone {
        void onFolderSelected(String name, int page);
        void onNoFolderSelected();
    }

    public static Dialog createFolderSelectionDialog(Context context, LightningEngine engine, final OnFolderSelectionDialogDone listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final ArrayList<String> folders_name = new ArrayList<String>();
        final ArrayList<Integer> folders_page = new ArrayList<Integer>();

        for(int p : engine.getPageManager().getAllPagesIds()) {
            Page page = engine.getOrLoadPage(p);
            for(Item item : page.items) {
                if(item.getClass() == Folder.class) {
                    Folder f = (Folder)item;
                    folders_name.add(f.getLabel());
                    folders_page.add(f.getFolderPageId());
                }
            }
        }

        String[] items = new String[folders_name.size()];
        folders_name.toArray(items);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                listener.onFolderSelected(folders_name.get(i), folders_page.get(i));
            }
        });
        builder.setTitle(R.string.sf);
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                listener.onNoFolderSelected();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                listener.onNoFolderSelected();
            }
        });
        return builder.create();
    }

    public static void getItemViewBoundsInItemLayout(ItemView itemView, RectF outRect) {
//        itemView.getHitRect(sTmpRect1);
//        outRect.set(sTmpRect1);
        ItemLayout il = itemView.getParentItemLayout();
        Item item = itemView.getItem();
        if(item.getItemConfig().onGrid) {
            float cw = il.getCellWidth();
            float ch = il.getCellHeight();
            Rect cell = item.getCell();
            outRect.set(cell.left*cw, cell.top*ch, cell.right*cw, cell.bottom*ch);
        } else {
            outRect.set(0, 0, item.getViewWidth(), item.getViewHeight());
            item.getTransform().mapRect(outRect);
        }
        if(itemView instanceof StopPointView) {
            outRect.offset(-outRect.width()/2, -outRect.height()/2);
        }
    }

    private static Matrix sTempMatrix = new Matrix();
    private static RectF sTempRectF = new RectF();

    public static float getRotateForMatrix(Matrix from) {
        sTempMatrix.set(from);
        float[] point = new float[] {0, 0};
        sTempMatrix.mapPoints(point);
        sTempMatrix.postTranslate(-point[0], -point[1]);
        point[0] = 1; point[1] = 0;
        sTempMatrix.mapPoints(point);
        return (float)(Math.atan2(point[1], point[0])*180/Math.PI);
    }

    public static float getScaleforMatrix(Matrix from, boolean x) {
        float rotate = getRotateForMatrix(from);
        sTempMatrix.set(from);
        float[] point = new float[] {0, 0};
        sTempMatrix.postRotate(-rotate);
        sTempMatrix.mapPoints(point);
        sTempMatrix.postTranslate(-point[0], -point[1]);
        point[0] = 1; point[1] = 1;
        sTempMatrix.mapPoints(point);
        return point[x ? 0 : 1];
    }

    public static RectF getTransformedItemBoxforMatrix(Item item, Matrix from) {
        sTempRectF.set(0, 0, item.getViewWidth(), item.getViewHeight());
        from.mapRect(sTempRectF);
        return sTempRectF;
    }

    public static float getSkewforMatrix(Matrix from, boolean x) {
        float rotate = getRotateForMatrix(from);
//        float sx = getScaleforMatrix(from, true);
//        float sy = getScaleforMatrix(from, false);
        sTempMatrix.set(from);
        float[] point = new float[] {0, 0};
        sTempMatrix.postRotate(-rotate);
        sTempMatrix.mapPoints(point);
        sTempMatrix.postTranslate(-point[0], -point[1]);
        //sTempMatrix.postScale(1/sx, 1/sy);
        float[] v = new float[9];
        sTempMatrix.getValues(v);
        return v[x ? Matrix.MSKEW_X : Matrix.MSKEW_Y];
    }

    public static HashMap<String,String> jsonObjectToHashMap(JSONObject o) {
        JSONArray names = o.names();
        int l = names == null ? 0 : names.length();
        HashMap<String,String> map = new HashMap<String, String>(l);
        try {
            for (--l; l >= 0; l--) {
                String name = names.getString(l);
                map.put(name, o.getString(name));
            }
        } catch(JSONException e) {
            // unlikely
        }
        return map;
    }

    public static void startAppStore(Context context, String pkg) {
        context.startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW, Uri.parse(Version.APP_STORE_INSTALL_PREFIX + pkg)), ""));
    }

    public static String dumpStats(Context context, Throwable throwable) throws IOException {
        LLApp app = LLApp.get();
        LightningEngine.PageManager pm = app.getAppEngine().getPageManager();
        ArrayList<Page> pages = pm.getLoadedPages();
        ArrayList<Screen> screens = app.getScreens();

        int total_shortcuts=0;
        int total_widgets=0;
        int total_icon_size=0;
        int total_drawable_size=0;
        int total_widget_size=0;
        HashSet<Drawable> widgets_drawables = new HashSet<Drawable>();

        Writer result = new StringWriter(1000);


        result.write("This is a bug report. Please include as many details regarding the crash, such as steps to reproduce it or when the bug appeared, every little hint helps a lot. Thank you!\n\n");

        try {
            result.write("Android " + Build.VERSION.RELEASE + " " + Build.VERSION.CODENAME + " " + Build.VERSION.SDK_INT + " / " + Build.MODEL + " " + Build.PRODUCT + "\n\n");
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            result.write(context.getPackageName() + " v" + pi.versionName + " - " + pi.versionCode + "\n");
        } catch (PackageManager.NameNotFoundException e) {
        }

        throwable.printStackTrace(new PrintWriter(result));
        result.write("\n\n");

        result.write("Statistics\n");
        result.write("  " + pages.size() + " page(s) loaded\n");
        for(Page p : pages) {
            result.write("    page " + p.id + ": " + p.items.size() + " item(s)\n");

            HashSet<Drawable> drawables = new HashSet<Drawable>();

            int page_shortcuts=0;
            int page_widgets=0;
            int page_icon_size=0;
            int page_drawable_size=0;
            int page_widget_size=0;

            for(Item item : p.items) {
                Drawable d;
                Box box = item.getItemConfig().box;
                d = box.bgNormal;
                if(d != null) drawables.add(d);
                d = box.bgSelected;
                if(d != null) drawables.add(d);
                d = box.bgFocused;
                if(d != null) drawables.add(d);

                if(item instanceof Shortcut) {
                    page_shortcuts++;
                    Shortcut s = (Shortcut)item;
                    SharedAsyncGraphicsDrawable sb = s.getSharedAsyncGraphicsDrawable();
                    if(sb != null) {
                        page_icon_size += sb.getStorage();
                    }

                    if(item instanceof Folder) {
                        Folder f = (Folder) item;
                        d = f.getFolderConfig().box.bgFolder;
                        if(d != null) drawables.add(d);
                    }
                } else if(item instanceof Widget) {
                    page_widgets++;
                    for (Screen screen : screens) {
                        ItemView[] itemViews = screen.getItemViewsForItem(item);
                        for (ItemView itemView : itemViews) {
                            BoxLayout bl = itemView.getSensibleView();
                            View child = bl.getChildAt(0);
                            page_widget_size += computeViewBitmapSize(child);
                            searchForDrawablesInView(child, widgets_drawables);
                        }
                    }
                }
            }

            for(Drawable d : drawables) {
                page_drawable_size += getDrawableSize(d);
            }

            result.write("        " + page_shortcuts + " shortcut(s) - icons: " + Formatter.formatFileSize(context, page_icon_size) + ", drawable: "+ Formatter.formatFileSize(context, page_drawable_size) + "\n");
            result.write("        " + page_widgets + " widget(s): " + Formatter.formatFileSize(context, page_widget_size) + "\n");
//            if(page_widgets != 0) {
//                for(Item item : p.items) {
//                    if(item instanceof Widget) {
//                        ComponentName cn = ((Widget)item).getComponentName();
//                        result.write("    "+(cn==null?"unknown":cn.flattenToShortString())+"\n");
//                    }
//                }
//            }

            total_shortcuts += page_shortcuts;
            total_widgets += page_widgets;
            total_icon_size += page_icon_size;
            total_drawable_size += page_drawable_size;
            total_widget_size += page_widget_size;
        }

        int widgets_drawable_size = 0;
        for(Drawable d : widgets_drawables) {
            widgets_drawable_size += getDrawableSize(d);
        }

        result.write("  total shortcuts: " + total_shortcuts + "\n");
        result.write("  total widgets: " + total_widgets + "\n");
        result.write("  total icon size: " + Formatter.formatFileSize(context, total_icon_size) + "\n");
        result.write("  total drawable size: " + Formatter.formatFileSize(context, total_drawable_size) + "\n");
        result.write("  total widget size: " + Formatter.formatFileSize(context, total_widget_size) + "\n");
        result.write("  widgets drawable size: " + Formatter.formatFileSize(context, widgets_drawable_size) + "\n\n");

        result.write("max: " + Formatter.formatFileSize(context, Runtime.getRuntime().maxMemory()));

        return result.toString();
    }

    public static int computeViewBitmapSize(View v) {
        int size = getDrawableSize(v.getBackground());
        if(v instanceof ImageView) {
            size += getDrawableSize(((ImageView)v).getDrawable());
        }
        if(v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            int n = vg.getChildCount();
            for(int i=0; i<n; i++) {
                View c = vg.getChildAt(i);
                size += computeViewBitmapSize(c);
            }
        }

        return size;
    }

    public static void searchForDrawablesInView(View v, HashSet<Drawable> all_drawables) {
        Drawable d = v.getBackground();
        if(d != null) {
            all_drawables.add(d);
        }
        if(v instanceof ImageView) {
            d = ((ImageView)v).getDrawable();
            if(d != null) {
                all_drawables.add(d);
            }
        }
        if(v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            int n = vg.getChildCount();
            for(int i=0; i<n; i++) {
                View c = vg.getChildAt(i);
                searchForDrawablesInView(c, all_drawables);
            }
        }
    }

    private static int getDrawableSize(Drawable d) {
        if(d == null) {
            return 0;
        }
        if(d instanceof BitmapDrawable) {
            Bitmap b = ((BitmapDrawable)d).getBitmap();
            return b.getWidth()*b.getHeight()*4;
        } else if(d instanceof NinePatchDrawable) {
            NinePatchDrawable np = (NinePatchDrawable)d;
            return np.getMinimumWidth()+np.getMinimumHeight()*4;
        } else if(d instanceof SharedAsyncGraphicsDrawable) {
            return  ((SharedAsyncGraphicsDrawable)d).getStorage();
        }

        // unknown
        return 0;
    }

    public static final int APP_THEME = 0;
    public static final int APP_THEME_TRANSLUCENT = 1;
    public static final int APP_THEME_NO_ACTION_BAR = 2;
    public static final int APP_THEME_NO_ACTION_BAR_WALLPAPER = 3;

    private static int[] sThemes = new int[] {
            R.style.AppLight, R.style.AppDark,
            R.style.AppLightTranslucent, R.style.AppDarkTranslucent,
            R.style.AppLightNoActionBar, R.style.AppDarkNoActionBar,
            R.style.AppLightNoActionBarWallpaper, R.style.AppDarkNoActionBarWallpaper,
    };

    public static void setTheme(Context context, int theme) {
        boolean is_light = LLApp.get().getSystemConfig().appStyle == SystemConfig.AppStyle.LIGHT;
        int style = sThemes[theme*2+(is_light?0:1)];
        context.setTheme(style);
    }

    public static void copyResourceToFile(Resources resources, int id, File out) {
        InputStream is = null;
        FileOutputStream os = null;
        byte[] buffer = new byte[1024];
        try {
            is = resources.openRawResource(id);
            os = new FileOutputStream(out);
            int n;
            while((n=is.read(buffer))>0) {
                os.write(buffer, 0, n);
            }
        } catch(Exception e) {
            if(is!=null) try { is.close(); } catch(Exception e1) {}
            if(os!=null) try { os.close(); } catch(Exception e1) {}
        }
    }

    public static boolean hasAppDrawerMode(int all_modes, int mode) {
        if(all_modes == 0 && mode == LAYOUT_MODE_BY_NAME) {
            // failsafe
            return true;
        }
        if(mode == LAYOUT_MODE_RECENT_APPS && Build.VERSION.SDK_INT>=21) {
            return false;
        }

        return (all_modes & (1<<mode)) != 0;
    }

    public static String formatHex(long value, int digits) {
        // dumb hexadecimal formatter
        String hex = Long.toHexString(value);
        int l = hex.length();
        if(l>digits) {
            hex = hex.substring(l-digits);
        } else {
            for(;l<digits; l++) {
                hex = "0" + hex;
            }
        }
        hex="#"+hex;
        return hex;
    }

    public static String formatItemLayoutName(ItemLayout il) {
        ItemView openerItemView = il.getOpenerItemView();
        return formatPageName(il.getPage(), openerItemView==null ? null : openerItemView.getItem());
    }

    public static String formatPageName(Page page, Item opener) {
        String text;
        int container_type = 0;
        if(page.isDashboard()) {
            container_type = R.string.dashboard_t;
        } else if(page.id == Page.APP_DRAWER_PAGE) {
            container_type = R.string.app_drawer_t;
        } else if(page.isFolder()) {
            if(opener != null && opener.getClass() == EmbeddedFolder.class) {
                container_type = R.string.efolder;
            } else {
                container_type = R.string.folder;
            }
        }
        text = LLApp.get().getString(container_type);
        if(page.isDashboard()) {
            GlobalConfig gc = page.getEngine().getGlobalConfig();
            text += " '"+gc.screensNames[gc.getPageIndex(page.id)]+"'";
        }
        text += " (" + Utils.formatHex(page.id, 6)+")";
        return text;
    }

    public static String formatItemName(Item item, int maxLength, int selectedItemsCount) {
        String text = item.formatForDisplay(true, maxLength);
        if(selectedItemsCount > 1) {
            text += LLApp.get().getString(R.string.ms_m, selectedItemsCount-1);
        }
        return text;
    }
}
