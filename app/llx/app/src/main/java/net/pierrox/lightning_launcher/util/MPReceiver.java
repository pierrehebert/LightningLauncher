package net.pierrox.lightning_launcher.util;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.data.*;
import net.pierrox.lightning_launcher.engine.LightningEngine;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class MPReceiver extends BroadcastReceiver {

    private enum PkgAction {
		ADD,
		UPDATE,
		REMOVE
	}
    @Override
    public void onReceive(Context context, Intent intent) {
    	PkgAction pkg_action=null;
    	
        String action=intent.getAction();
        String package_name=intent.getData().getSchemeSpecificPart();
        
        if(context.getPackageName().equals(package_name)) {
        	return;
        }
        
        if(action.equals(Intent.ACTION_PACKAGE_ADDED)) {
            boolean replacing=intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
            if(!replacing) {
            	pkg_action=PkgAction.ADD;
            }
        } else if(action.equals(Intent.ACTION_PACKAGE_REPLACED)) {
        	pkg_action=PkgAction.UPDATE;
        } else if(action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
            boolean replacing=intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
            if(!replacing){
            	pkg_action=PkgAction.REMOVE;
            }
        }
        
        if(pkg_action!=null) {
	        // the remote context can be null if the core app has been removed but not the setup app
	        if(context!=null) {
				switch(pkg_action) {
				case ADD:
					addPackage(context, package_name);
					break;
					
				case UPDATE:
					updatePackage(context, package_name);
					break;
					
				case REMOVE:
					removePackage(context, package_name);
					break;
				}
			}
        }
    }
    
    private void addPackage(Context context, String package_name) {
    	boolean modified=false;

        Page app_drawer_page = LLApp.get().getAppEngine().getOrLoadPage(Page.APP_DRAWER_PAGE);
        File icon_dir=app_drawer_page.getAndCreateIconDir();

		PackageManager pm=context.getPackageManager();
        Intent intent_filter=new Intent(Intent.ACTION_MAIN);
        intent_filter.addCategory(Intent.CATEGORY_LAUNCHER);
        intent_filter.setPackage(package_name);
        List<ResolveInfo> ris=pm.queryIntentActivities(intent_filter, 0);
        for(ResolveInfo ri : ris) {
            ComponentName component_name=new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
            Resources rsrc;
            try {
                rsrc = pm.getResourcesForActivity(component_name);
                
                String label=ri.loadLabel(pm).toString();
                
                Intent intent=new Intent();
                intent.setComponent(component_name);
                intent.setAction(Intent.ACTION_MAIN);
//                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                
                int id=app_drawer_page.findFreeItemId();
                Shortcut s=new Shortcut(app_drawer_page);
                int[] cell=Utils.findFreeCell(app_drawer_page);
                s.init(id, new Rect(cell[0], cell[1], cell[0]+1, cell[1]+1), null, label, intent);
                //Utils.setItemPosition(app_drawer_page, s, Utils.ADD_XY_AUTO, Utils.ADD_XY_AUTO);
                s.mLastUpdateTime = System.currentTimeMillis();
                
                Bitmap icon=Utils.decodeScaledBitmapResource(rsrc, ri.getIconResource(), s.getStdIconSize());
                if(icon != null) {
	                File icon_file=s.getDefaultIconFile();
	                Utils.saveIconToFile(icon_file, icon);
	                icon.recycle();
                }
                
                app_drawer_page.addItem(s);
                modified=true;
            } catch (Exception e) {
                // skip this item
            }
        }
        
        if(modified) {
        	Collections.sort(app_drawer_page.items, Utils.sItemComparatorByNameAsc);
            // TODO should not be needed anymore because of page.addItem
        	app_drawer_page.notifyModified();
        }
    }
    
    private void updatePackage(Context context, String package_name) {
        LightningEngine engine = LLApp.get().getAppEngine();
        int[] allPagesIds = engine.getPageManager().getAllPagesIds();
        if(allPagesIds.length == 0) {
            return;
        }

        long now = System.currentTimeMillis();

    	PackageManager pm=context.getPackageManager();
        Intent intent_filter=new Intent(Intent.ACTION_MAIN);
        intent_filter.addCategory(Intent.CATEGORY_LAUNCHER);
        intent_filter.setPackage(package_name);
        List<ResolveInfo> ris=pm.queryIntentActivities(intent_filter, 0);
        for(ResolveInfo ri : ris) {
            ComponentName component_name=new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
            Resources rsrc;
            try {
                rsrc = pm.getResourcesForActivity(component_name);
                
                String label=ri.loadLabel(pm).toString();
                
                Intent intent=new Intent();
                intent.setComponent(component_name);
                intent.setAction(Intent.ACTION_MAIN);

                boolean found=false;
                
                // update currently existing matching items
                for(int p : allPagesIds) {
                    Page page = engine.getOrLoadPage(p);

                	for(Item i : page.items) {
	                	if(i instanceof Shortcut) {
	                		Shortcut s=(Shortcut)i;
	                		ComponentName cn=s.getIntent().getComponent();
	                		if(cn!=null && cn.compareTo(component_name)==0) {
	        	                File default_icon_file=s.getDefaultIconFile();
	        	                
	        	                Bitmap icon=Utils.decodeScaledBitmapResource(rsrc, ri.getIconResource(), s.getStdIconSize());
	        	                Utils.saveIconToFile(default_icon_file, icon);
	        	                // do not update items on other pages, since they may have custom labels (two labels is not supported yet)
	        	                if(p== Page.APP_DRAWER_PAGE) {
	        	                	s.setLabel(label);
	        	                }
	                        	s.setIntent(intent);
                                s.mLastUpdateTime = now;

                                s.notifyChanged();

	                        	found=true;
	                		}
	                	}
	                }
	                
                }

                // this item does not exist in the app app_drawer page yet, create a new shortcut
                if(!found) {
                    Page page = engine.getOrLoadPage(Page.APP_DRAWER_PAGE);
                    File icon_dir=page.getAndCreateIconDir();
                    int id=page.findFreeItemId();
                    int[] cell=Utils.findFreeCell(page);
                    Shortcut s=new Shortcut(page);
                    s.init(id, new Rect(cell[0], cell[1], cell[0]+1, cell[1]+1), null, label, intent);
                    s.mLastUpdateTime = now;
                    File icon_file=s.getDefaultIconFile();
                    Bitmap icon=Utils.decodeScaledBitmapResource(rsrc, ri.getIconResource(), s.getStdIconSize());
                    Utils.saveIconToFile(icon_file, icon);

                    page.addItem(s);
                }
            } catch (Exception e) {
                // skip this item
            }
        }
    }
    
    private void removePackage(Context context, String package_name) {
        LightningEngine engine = LLApp.get().getAppEngine();
    	for(int p : engine.getPageManager().getAllPagesIds()) {
            Page page = engine.getOrLoadPage(p);
	    	boolean modified=false;

			page.getAndCreateIconDir();
	    	for(int l=page.items.size()-1; l>=0; l--) {
	    		Item i=page.items.get(l);
	    		if(i instanceof Shortcut) {
					Shortcut s=(Shortcut)i;
					ComponentName cn=s.getIntent().getComponent();
					if(cn!=null && cn.getPackageName().equals(package_name)) {
						if(i.getId()!=0) {
                            page.removeItem(i, false);
							modified=true;
						}
					}
	    		}
			}
	    	
	        if(modified) {
	        	if(p== Page.APP_DRAWER_PAGE) {
	        		Collections.sort(page.items, Utils.sItemComparatorByNameAsc);
	        	}
                // TODO should not be needed anymore because of page.removeItem
	        	page.notifyModified();
	        }
    	}
    }
}