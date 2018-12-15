package net.pierrox.lightning_launcher.data;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;

import net.pierrox.lightning_launcher.configuration.ShortcutConfig;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class IconPack {

    public interface IconPackListener {
        void onPackApplied(boolean success);
    }

    public static boolean applyIconPackSync(final Context context, final String package_name, final Page page, final int item_id) {
        boolean result = doApplyIconPackInBackground(context, package_name, page, item_id);
        doApplyIconPackPostExecute(page, item_id);
        return result;
    }

    public static void applyIconPackAsync(final Context context, final String package_name, final Page page, final int item_id, final IconPackListener listener) {
        new AsyncTask<Void, Item, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return doApplyIconPackInBackground(context, package_name, page, item_id);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                doApplyIconPackPostExecute(page, item_id);
                if(listener != null) {
                    listener.onPackApplied(result);
                }

            }

        }.execute((Void) null);
    }

    public static void removeCustomIcons(Page page) {
        File icon_dir = page.getIconDir();
        new File(icon_dir, "b").delete();
        new File(icon_dir, "o").delete();
        new File(icon_dir, "m").delete();

        page.config.defaultShortcutConfig.iconEffectScale = 1;

        for(Item i : page.items) {
            if(i.getClass() == Shortcut.class) {
                Shortcut s = (Shortcut)i;
                s.deleteCustomIconFiles(icon_dir);
                s.getShortcutConfig().iconEffectScale = 1;
            }
        }

        page.save();
        page.reload();
    }

    @SuppressLint("DefaultLocale")
    private static boolean doApplyIconPackInBackground(final Context context, final String package_name, final Page page, final int item_id) {
        Context icon_pack;
        try {
            icon_pack = context.createPackageContext(package_name, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }

        Resources res = icon_pack.getResources();
        int icon_pack_res_id = res.getIdentifier("icon_pack", "array", package_name);

        // build the cache of icon name -> drawable id (legacy way)
        HashMap<String, Integer> icon_name_ids = new HashMap<String, Integer>();
        if(icon_pack_res_id != 0) {
            String[] icon_names = res.getStringArray(icon_pack_res_id);
            for(String icon_name : icon_names) {
                try  {
                    int drawable_res_id = res.getIdentifier(icon_name, "drawable", package_name);
                    if(drawable_res_id != 0) icon_name_ids.put(icon_name, Integer.valueOf(drawable_res_id));
                } catch(Resources.NotFoundException e) {
                    // pass, error in input array
                }
            }
        }

        // build the cache of componentname -> drawable id and load icon layers if available
        HashMap<String, Integer> component_name_ids = new HashMap<String, Integer>();
        ArrayList<Bitmap> icon_effect_back = new ArrayList<Bitmap>();
        Bitmap icon_effect_over=null, icon_effect_mask=null;
        float icon_effect_scale = 1;
        InputStream appfilter_is = null;
        try {
            XmlPullParser parser;
            int appfilter_id = icon_pack.getResources().getIdentifier("appfilter", "xml", package_name);
            if(appfilter_id != 0) {
                parser = icon_pack.getResources().getXml(appfilter_id);
            } else {
                try {
                    appfilter_is = icon_pack.getAssets().open("appfilter.xml");
                    parser = Xml.newPullParser();
                    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                    parser.setInput(appfilter_is, null);
                } catch(IOException e) {
                    parser = null;
                }
            }

            if(parser == null) {
                return false;
            }

            int eventType = parser.next();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String name = parser.getName();
                    if (name.equals("iconback")) {
                        String n = parser.getAttributeValue(null, "img");
                        if(n == null) {
                            for(int i=1; i<30; i++) {
                                n = parser.getAttributeValue(null, "img"+i);
                                if(n == null) {
                                    break;
                                }
                                Bitmap b = loadBitmapFromDrawable(res, package_name, n);
                                if(b != null) {
                                    icon_effect_back.add(b);
                                }
                            }
                        } else {
                            Bitmap b = loadBitmapFromDrawable(res, package_name, n);
                            if(b != null) {
                                icon_effect_back.add(b);
                            }
                        }
                    } else if (name.equals("iconupon")) {
                        String n = parser.getAttributeValue(null, "img");
                        if(n == null) {
                            n = parser.getAttributeValue(null, "img1");
                        }
                        icon_effect_over = loadBitmapFromDrawable(res, package_name, n);
                    } else if (name.equals("iconmask")) {
                        String n = parser.getAttributeValue(null, "img");
                        if(n == null) {
                            n = parser.getAttributeValue(null, "img1");
                        }
                        icon_effect_mask = loadBitmapFromDrawable(res, package_name, n);
                    } else if (name.equals("scale")) {
                        icon_effect_scale = Float.parseFloat(parser.getAttributeValue(null, "factor"));
                    } else if (name.equals("item")) {
                        String component = parser.getAttributeValue(null, "component");
                        String drawable = parser.getAttributeValue(null, "drawable");
                        try  {
                            String component_name = component.substring(component.indexOf('{')+1, component.indexOf('}'));
                            if(component_name.indexOf('/') == -1) {
                                component_name = component_name.substring(0, component_name.lastIndexOf('.')-1) + "/" + component_name;
                            }
                            ComponentName cn = ComponentName.unflattenFromString(component_name);

                            int drawable_res_id = res.getIdentifier(drawable, "drawable", package_name);
                            if(drawable_res_id != 0) component_name_ids.put(cn.flattenToString(), Integer.valueOf(drawable_res_id));
                        } catch(Exception e) {
                            Log.i("LL", "unable to decode " + component);
                            // pass, error in input array
                        }
                    }
                }
                eventType = parser.next();
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        } finally {
            if(appfilter_is != null) try { appfilter_is.close(); } catch (IOException e) {}
        }

//				File pages_dir = FileUtils.getPagesDir(Customize.this);
//				String[] pages = pages_dir.list();
//				for(String ps : pages) {
//					int p;
//					try {
//						p = Integer.parseInt(ps);
//					} catch(NumberFormatException e) {
//						// not a page, continue
//						continue;
//					}

//                mItemIconEffectScale.setValue(icon_effect_scale);

//					page.pageConfig.defaultShortcutConfig.iconEffectScale = icon_effect_scale;
//					File out = FileUtils.getWorkspaceConfigFile(Customize.this, page.page);
//					PageConfig default_page_config = new PageConfig();
//					JSONObject o;
//					try {
//						o = FileUtils.toJSONObject(page.pageConfig, default_page_config);
//						ic.box_s = ic.box.toString(new Box());
//						o.put("defaultItemConfig", FileUtils.toJSONObject(page.pageConfig.defaultItemConfig, new ItemConfig()));
//						o.put("defaultShortcutConfig", FileUtils.toJSONObject(page.pageConfig.defaultShortcutConfig, new ShortcutConfig()));
//						o.put("defaultFolderConfig", FileUtils.toJSONObject(page.pageConfig.defaultFolderConfig, new FolderConfig()));
//						FileUtils.saveStringToFile(o.toString(), out);
//					} catch (Exception e) {
//						// pass
//					}

        File icon_dir = page.getAndCreateIconDir();
        icon_dir.mkdirs();
        if(item_id == Item.NO_ID) {
            File icon_effect_back_file = new File(icon_dir, "b");
            if (icon_effect_back.size() > 0) saveBitmapToFile(icon_effect_back.get(0), icon_effect_back_file); else icon_effect_back_file.delete();
            File icon_effect_over_file = new File(icon_dir, "o");
            if (icon_effect_over != null) saveBitmapToFile(icon_effect_over, icon_effect_over_file); else icon_effect_over_file.delete();
            File icon_effect_mask_file = new File(icon_dir, "m");
            if (icon_effect_mask != null) saveBitmapToFile(icon_effect_mask, icon_effect_mask_file); else icon_effect_mask_file.delete();

            page.setModified();
        }

        // update items custom icon
        int n = 0;
        for(Item i : page.items) {
            if(item_id != Item.NO_ID && i.mId != item_id) {
                continue;
            }

            if(i.getClass() == Shortcut.class) {
                Shortcut s = (Shortcut)i;
                boolean icon_found = false;
                ComponentName cn = s.getIntent().getComponent();
                if(cn != null) {
                    // try to get an id through the appfilter way
                    Integer drawable_id = component_name_ids.get(cn.flattenToString());
                    if(drawable_id == null) {
                        // try to find an id, either with or without the activity name (try with the activty name first, otherwise try only the package)
                        String class_name = cn.getClassName();
                        String icon_name = class_name.replace('.', '_').toLowerCase();
                        drawable_id = icon_name_ids.get(icon_name);
                        if(drawable_id == null) {
                            int pos = class_name.lastIndexOf('.')+1; // if not found will produce 0 which is fine
                            icon_name = (cn.getPackageName()+"_"+class_name.substring(pos)).replace('.', '_').toLowerCase();
                            drawable_id = icon_name_ids.get(icon_name);
                            if(drawable_id == null) {
                                icon_name = cn.getPackageName().replace('.', '_').toLowerCase();
                                drawable_id = icon_name_ids.get(icon_name);
                            }
                        }
                    }

                    if(drawable_id != null) {
                        // save the drawable using the target size (if less than the icon from the pack)
                        String default_icon_path = icon_dir+"/"+s.getId();
                        File custom_icon_file = new File(default_icon_path+"c");

                        Drawable drawable = Utils.decodeDrawableResource(res, drawable_id.intValue());

                        if(drawable != null) {
                            BitmapFactory.Options opts = new BitmapFactory.Options();
                            opts.inJustDecodeBounds = true;
                            BitmapFactory.decodeFile(default_icon_path, opts);

                            if (opts.outWidth > 0 && opts.outHeight > 0) {
                                Bitmap bitmap = Bitmap.createBitmap(opts.outWidth, opts.outHeight, Bitmap.Config.ARGB_8888);
                                Canvas canvas = new Canvas(bitmap);
                                drawable.setBounds(0, 0, opts.outWidth, opts.outHeight);
                                drawable.draw(canvas);

                                saveBitmapToFile(bitmap, custom_icon_file);
                                icon_found = true;
                            }
                        }
                    }
                }

                if(icon_found) {
                    int id = s.getId();
                    ShortcutConfig sc = s.getShortcutConfig().clone();
                    sc.iconEffectScale = 1;
                    saveEmptyBitmap(sc.getIconBackFile(icon_dir, id));
                    saveEmptyBitmap(sc.getIconOverFile(icon_dir, id));
                    saveEmptyBitmap(sc.getIconMaskFile(icon_dir, id));
                    s.setShortcutConfig(sc);
                } else if(icon_effect_back.size() > 1) {
                    Bitmap bitmap = icon_effect_back.get((n++)%icon_effect_back.size());
                    saveBitmapToFile(bitmap, ShortcutConfig.getIconBackFile(icon_dir, s.getId()));
                    s.modifyShortcutConfig().iconEffectScale = icon_effect_scale;
                }
            }
        }

        return true;
    }

    private static void doApplyIconPackPostExecute(Page page, int item_id) {
        if(item_id == Item.NO_ID) {
            page.save();
            page.reload();
        } else {
            page.findItemById(item_id).notifyChanged();
        }
    }


    private static void saveEmptyBitmap(File to) {
        FileOutputStream fos = null;
        try {
            to.delete();
            fos = new FileOutputStream(to);
            Bitmap empty = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            empty.eraseColor(0);
            empty.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (IOException e) {
            to.delete();
        } finally {
            if(fos != null) try { fos.close(); } catch (IOException e) { /*pass*/ }
        }
    }

    private static Bitmap loadBitmapFromDrawable(Resources res, String package_name, String name) {
        int id = res.getIdentifier(name, "drawable", package_name);
        try {
            Drawable d = res.getDrawable(id);
            if(d instanceof BitmapDrawable) {
                return ((BitmapDrawable)d).getBitmap();
            }
        } catch(Resources.NotFoundException e) {
            // pass
        }
        return null;
    }

    private static void saveBitmapToFile(Bitmap bitmap, File file) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch(IOException e) {
            file.delete();
        } finally {
            if(fos != null) try { fos.close(); } catch(IOException e) {}
        }
    }
}
