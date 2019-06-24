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
import java.util.*;

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

	private static class LegacyIconPackResIdFetcher {
		private final Resources resources;
		private final String packageName;
		private List<String> iconNames;
		private final Map<ComponentName, Integer> cache = new HashMap<>();

		private LegacyIconPackResIdFetcher(Resources resources, String packageName) {
			this.resources = resources;
			this.packageName = packageName;
		}

		private List<String> getIconNames() {
			if (iconNames == null) {
				int iconPackResId = resources.getIdentifier("icon_pack", "array", packageName);
				if (iconPackResId != 0) {
					iconNames = Arrays.asList(resources.getStringArray(iconPackResId));
				} else {
					iconNames = new ArrayList<>();
				}
			}
			return iconNames;
		}

		public Integer tryFindDrawableIdForComponent(ComponentName cn) {
			if (cache.containsKey(cn)) {
				return cache.get(cn);
			}
			// try to find an id, either with or without the activity name (try with the activity name first, otherwise try only the package)
			String className = cn.getClassName();
			String iconName = className.replace('.', '_').toLowerCase();
			Integer drawableId = getDrawableIdForName(iconName);
			if (drawableId == null) {
				int pos = className.lastIndexOf('.') + 1; // if not found will produce 0 which is fine
				iconName = (cn.getPackageName() + "_" + className.substring(pos)).replace('.', '_').toLowerCase();
				drawableId = getDrawableIdForName(iconName);
				if (drawableId == null) {
					iconName = cn.getPackageName().replace('.', '_').toLowerCase();
					drawableId = getDrawableIdForName(iconName);
				}
			}
			if (drawableId != null) {
				cache.put(cn, drawableId);
			}
			return drawableId;
		}

		private Integer getDrawableIdForName(String iconName) {
			if (getIconNames().contains(iconName)) {
				try {
					return resources.getIdentifier(iconName, "drawable", packageName);
				} catch (Resources.NotFoundException e) {
					// pass, error in input array
				}
			}
			return null;
		}


	}

	private static class AppFilterIconPackResIdFetcher {
    	private final Map<String, String> items = new TreeMap<>();
    	private final List<String> iconBacks = new ArrayList<>();
		private final Resources resources;
		private final String packageName;
		private String iconUpon = null;
    	private String iconMask = null;
    	private float scale = 1;
    	private List<Bitmap> iconBackCache;
    	private Bitmap iconUponCache;
    	private Bitmap iconMaskCache;
    	private final Map<ComponentName, Integer> itemCache = new TreeMap<>();

		public AppFilterIconPackResIdFetcher(Context iconPack, String packageName) {
			this.resources = iconPack.getResources();
			this.packageName = packageName;
			InputStream appfilterStream = null;
			try {
				XmlPullParser parser;
				int appfilter_id = resources.getIdentifier("appfilter", "xml", packageName);
				if(appfilter_id != 0) {
					parser = resources.getXml(appfilter_id);
				} else {
					try {
						appfilterStream = iconPack.getAssets().open("appfilter.xml");
						parser = Xml.newPullParser();
						parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
						parser.setInput(appfilterStream, null);
					} catch(IOException e) {
						parser = null;
					}
				}
				if(parser != null) {
					int eventType = parser.next();
					while (eventType != XmlPullParser.END_DOCUMENT) {
						if (eventType == XmlPullParser.START_TAG) {
							String name = parser.getName();
							switch (name) {
								case "iconback": {
									String n = parser.getAttributeValue(null, "img");
									if (n == null) {
										for (int i = 1; i < 30; i++) {
											n = parser.getAttributeValue(null, "img" + i);
											if (n == null) {
												break;
											}
											iconBacks.add(n);
										}
									} else {
										iconBacks.add(n);
									}
									break;
								}
								case "iconupon": {
									String n = parser.getAttributeValue(null, "img");
									if (n == null) {
										n = parser.getAttributeValue(null, "img1");
									}
									iconUpon = n;
									break;
								}
								case "iconmask": {
									String n = parser.getAttributeValue(null, "img");
									if (n == null) {
										n = parser.getAttributeValue(null, "img1");
									}
									iconMask = n;
									break;
								}
								case "scale":
									scale = Float.parseFloat(parser.getAttributeValue(null, "factor"));
									break;
								case "item":
									String component = parser.getAttributeValue(null, "component");
									String drawable = parser.getAttributeValue(null, "drawable");
									try {
										int start = component.indexOf('{') + 1;
										String componentName = component.substring(start, component.lastIndexOf('}'));
										if (componentName.indexOf('/', start) == -1) {
											componentName = componentName.substring(0, componentName.lastIndexOf('.') - 1) + "/" + componentName;
										}
										items.put(componentName, drawable);
									} catch (Exception e) {
										Log.i("LL", "unable to decode " + component);
										// pass, error in input array
									}
									break;
							}
						}
						eventType = parser.next();
					}
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			} finally {
				if(appfilterStream != null) try { appfilterStream.close(); } catch (IOException e) {}
			}
		}

		public List<Bitmap> getIconBacks() {
			if(iconBackCache == null) {
				iconBackCache = new ArrayList<>();
				for (String iconBack : iconBacks) {
					Bitmap b = loadBitmapFromDrawable(resources, packageName, iconBack);
					if(b != null) {
						iconBackCache.add(b);
					}
				}
			}
			return iconBackCache;
		}

		public Bitmap getIconUpon() {
			if(iconUponCache == null && iconUpon != null) {
				iconUponCache = loadBitmapFromDrawable(resources, packageName, iconUpon);
			}
			return iconUponCache;
		}

		public Bitmap getIconMask() {
			if(iconMaskCache == null && iconMask != null) {
				iconMaskCache = loadBitmapFromDrawable(resources, packageName, iconMask);
			}
			return iconMaskCache;
		}

		public float getScale() {
			return scale;
		}

		public Integer tryFindDrawableIdForComponent(ComponentName cn) {
			if(itemCache.containsKey(cn)) {
				return itemCache.get(cn);
			}
			Integer drawableId = null;
			String flatten = cn.flattenToString();
			if(items.containsKey(flatten)) {
				drawableId = resources.getIdentifier(items.get(flatten), "drawable", packageName);
			}
			if(drawableId != null) {
				itemCache.put(cn, drawableId);
			}
			return drawableId;
		}


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
        LegacyIconPackResIdFetcher legacyIconPackResIdFetcher = new LegacyIconPackResIdFetcher(res, package_name);
        AppFilterIconPackResIdFetcher appFilterIconPackResIdFetcher = new AppFilterIconPackResIdFetcher(icon_pack, package_name);

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
		List<Bitmap> icon_effect_back = appFilterIconPackResIdFetcher.getIconBacks();

        File icon_dir = page.getAndCreateIconDir();
        icon_dir.mkdirs();
        if(item_id == Item.NO_ID) {
            File icon_effect_back_file = new File(icon_dir, "b");
            if (icon_effect_back.size() > 0) saveBitmapToFile(icon_effect_back.get(0), icon_effect_back_file); else icon_effect_back_file.delete();
            File icon_effect_over_file = new File(icon_dir, "o");
            Bitmap icon_effect_over = appFilterIconPackResIdFetcher.getIconUpon();
            if (icon_effect_over != null) saveBitmapToFile(icon_effect_over, icon_effect_over_file); else icon_effect_over_file.delete();
            File icon_effect_mask_file = new File(icon_dir, "m");
            Bitmap icon_effect_mask = appFilterIconPackResIdFetcher.getIconMask();
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
                    Integer drawable_id = appFilterIconPackResIdFetcher.tryFindDrawableIdForComponent(cn);
                    if(drawable_id == null) {
                    	drawable_id = legacyIconPackResIdFetcher.tryFindDrawableIdForComponent(cn);
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
                    s.modifyShortcutConfig().iconEffectScale = appFilterIconPackResIdFetcher.getScale();
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
