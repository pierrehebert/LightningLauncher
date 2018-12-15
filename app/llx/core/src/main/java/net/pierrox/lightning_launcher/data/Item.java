package net.pierrox.lightning_launcher.data;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.Html;
import android.text.Spanned;

import net.pierrox.lightning_launcher.R;
import net.pierrox.lightning_launcher.configuration.DynamicTextConfig;
import net.pierrox.lightning_launcher.configuration.FolderConfig;
import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.configuration.ItemConfig;
import net.pierrox.lightning_launcher.configuration.ItemConfigStylable;
import net.pierrox.lightning_launcher.configuration.JsonFields;
import net.pierrox.lightning_launcher.configuration.PageConfig;
import net.pierrox.lightning_launcher.configuration.ShortcutConfig;
import net.pierrox.lightning_launcher.views.item.ItemView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public abstract class Item implements ItemConfigStylable {
    public static final int GEOMETRY_CTRL_SIZE=1;
    public static final int GEOMETRY_CTRL_POSITION=2;
    public static final int GEOMETRY_CTRL_ROTATE=3;
    public static final int GEOMETRY_CTRL_SCALE=4;
    public static final int GEOMETRY_CTRL_SKEW=5;


    public interface OnItemEventListener {
        void onItemPaused(Item item);
        void onItemResumed(Item item);
        void onItemVisibilityChanged(Item item);
        void onItemAlphaChanged(Item item);
        void onItemTransformChanged(Item item, boolean fast);
        void onItemCellChanged(Item item);
        void onItemBindingsChanged(Item item, boolean apply);
        void onShortcutLabelChanged(Shortcut shortcut);
        void onFolderPageIdChanged(Folder folder, int oldPageId);
    }

    public static final int NO_ID = -1;
	
	public enum Type {
		SHORTCUT,
		WIDGET,
		FOLDER,
		STOP_POINT,
        DTEXT,
        EFOLDER,
        UNLOCKER,
        PAGE_INDICATOR,
        CUSTOM_VIEW
	}
	


    protected Page mPage;
	protected int mId;
	
	protected HashMap<String,String> mTags;

	protected String mName;
	
	// used in grid layout mode
	protected Rect mCell;
	protected Rect mCellP;
	protected Rect mCellL;
    protected Rect mCellT;
	
	// used in free layout mode
	protected Matrix mTransform;
	protected Matrix mTransformP;
	protected Matrix mTransformL;
	
    protected Point mViewSize;
    protected Point mViewSizeP;
    protected Point mViewSizeL;

    public static final int APP_DRAWER_HIDDEN_ALL = 0;
    public static final int APP_DRAWER_HIDDEN_ONLY_VISIBLE = 1;
    public static final int APP_DRAWER_HIDDEN_ONLY_HIDDEN = 2;
	private boolean mAppDrawerHidden;        // persistently hidden app in the app drawer
    private int mAppDrawerHiddenHandling = Item.APP_DRAWER_HIDDEN_ONLY_VISIBLE;
    private boolean mVisible = true;
	private int mAlpha = -1;


	protected ItemConfig mItemConfig;
    private boolean mSharedItemConfig;
	

    // rather a hack: saved here to ease sorting of items
    public int mLaunchCount;
    public long mLastUpdateTime;

    public Item(Page page) {
        mPage = page;
    }

    public Page getPage() {
        return mPage;
    }

    protected void copyTo(Item item) {
		// this is not a full deep copy, only fields that will be modified are deeply cloned
		item.mCellP=new Rect(mCellP);
		item.mCellL=mCellL==null ? null : new Rect(mCellL);
		item.mCell=item.mCellP;
		item.mTransformP=new Matrix(mTransformP);
		item.mTransformL=mTransformL==null ? null : new Matrix(mTransformL);
		item.mTransform=item.mTransformP;
        item.mViewSizeP=new Point(mViewSizeP);
        item.mViewSizeL=mViewSizeL==null ? null : new Point(mViewSizeL);
        item.mViewSize=item.mViewSizeP;
        if(mSharedItemConfig) {
            item.mItemConfig = mItemConfig;
        } else {
            // when not shared, make a copy so that both items do not share the same ItemConfig reference
            item.mItemConfig = new ItemConfig();
            item.mItemConfig.copyFrom(mItemConfig);
        }
        item.mSharedItemConfig=mSharedItemConfig;
		item.mTags=new HashMap<>(mTags);
	}

    // FIXME not implemented in all subclasses
	public abstract Item clone();

    public void resume() {
        mPage.onItemResumed(this);
    }

    public void pause() {
        mPage.onItemPaused(this);
    }

    public void onCreate() {

    }

    public void onDestroy() {

    }

    public void onRemove(boolean keepResources) {

    }

	public int getId() {
		return mId;
	}
	
	public void setId(int id) {
		mId=id;
	}
	
	public void setName(String name) {
		mName = name;
        mPage.setModified();
	}
	
	public String getName() {
		return mName;
	}

    @Override
    public String toString() {
        return formatForDisplay(false, 0);
    }

    public static File getDefaultIconFile(File icon_dir, int id) {
    	return new File(icon_dir, String.valueOf(id));
    }

    public File getDefaultIconFile() {
    	return getDefaultIconFile(mPage.getIconDir(), mId);
    }
    
    public static File getCustomIconFile(File icon_dir, int id) {
    	return new File(icon_dir, id+"c");
    }

    public File getCustomIconFile() {
    	return getCustomIconFile(mPage.getIconDir(), mId);
    }

    public void getIconFiles(File icon_dir, ArrayList<File> out_files) {
        out_files.add(getDefaultIconFile());
        out_files.add(Box.getBoxBackgroundNormal(icon_dir, mId));
        out_files.add(Box.getBoxBackgroundSelected(icon_dir, mId));
        out_files.add(Box.getBoxBackgroundFocused(icon_dir, mId));
    }

    public Rect getCellP() {
        return mCellP;
    }

    public Rect getCellL() {
        return mCellL;
    }

    public Rect getCell() {
        return mCellT==null ? mCell : mCellT;
	}

    public void setCellT(Rect cell_t) {
        mCellT=cell_t;
    }

	public boolean isAppDrawerHidden() {
		return mAppDrawerHidden;
	}
	
	public void setAppDrawerHidden(boolean h) {
		mAppDrawerHidden = h;
        mPage.onItemVisibilityChanged(this);
	}

    public int getAppDrawerHiddenHandling() {
        return mAppDrawerHiddenHandling;
    }

    public void setAppDrawerHiddenHandling(int appDrawerHiddenHandling) {
        if(mAppDrawerHiddenHandling != appDrawerHiddenHandling) {
            mAppDrawerHiddenHandling = appDrawerHiddenHandling;
            mPage.onItemVisibilityChanged(this);
        }
    }

    public void setVisible(boolean visible) {
        if(visible != mVisible) {
            mVisible = visible;
            mPage.onItemVisibilityChanged(this);
        }
    }

    public boolean isVisible() {
        return mVisible;
    }


	public void setAlpha(int alpha) {
		if(alpha != mAlpha) {
            mAlpha = alpha;
            mPage.onItemAlphaChanged(this);
		}
	}

	public int getAlpha() {
		return mAlpha;
	}

	public Matrix getTransform() {
		return mTransform;
	}
	
	public void setOrientation(int orientation) {
        PageConfig pageConfig = mPage.config;
        if(mItemConfig.onGrid) {
			mCell=(orientation==Configuration.ORIENTATION_PORTRAIT || !pageConfig.allowDualPosition ? mCellP : (mCellL==null ? mCellP : mCellL));
		} else {
			mTransform=(orientation==Configuration.ORIENTATION_PORTRAIT || !pageConfig.allowDualPosition ? mTransformP : (mTransformL==null ? mTransformP : mTransformL));
            mViewSize=(orientation==Configuration.ORIENTATION_PORTRAIT || !pageConfig.allowDualPosition ? mViewSizeP : (mViewSizeL==null ? mViewSizeP : mViewSizeL));
            mPage.onItemTransformChanged(this, false);
		}
	}
	
	public void differentiatePosition(int orientation) {
		if(orientation==Configuration.ORIENTATION_LANDSCAPE) {
			if(mItemConfig.onGrid) {
				if(mCellL==null) {
					mCellL=new Rect(mCellP);
				}
			} else {
				if(mTransformL==null) {
					mTransformL=new Matrix(mTransformP);
                }
                if(mViewSizeL==null) {
                    mViewSizeL=new Point(mViewSizeP);
                }
            }
			setOrientation(orientation);
		}
	}
	
	public void setTransform(Matrix transform, boolean fast) {
		mTransform.set(transform);
        mPage.onItemTransformChanged(this, fast);
    }

    private static RectF mTempRectF = new RectF();
    public void updateGeometryValue(int what, float value_x, float value_y, boolean fast) {
        float sx, sy;
        float[] values = new float[9];
        boolean on_grid = mItemConfig.onGrid;
        Matrix i = mTransform;
        i.getValues(values);

        mTempRectF.set(Utils.getTransformedItemBoxforMatrix(this, i));
        float r = Utils.getRotateForMatrix(i);
        Rect cell = getCell();
        switch(what) {
            case GEOMETRY_CTRL_POSITION:
                if(on_grid) {
                    cell.offsetTo(Math.round(value_x), Math.round(value_y));
                } else {
                    sx = mTempRectF.left;
                    sy = mTempRectF.top;
                    i.postTranslate(value_x - sx, value_y - sy);
                }
                break;
            case GEOMETRY_CTRL_SIZE:
                if(on_grid) {
                    if(value_x<1) value_x = 1;
                    if(value_y<1) value_y = 1;
                    cell.right = cell.left + Math.round(value_x);
                    cell.bottom = cell.top + Math.round(value_y);
                } else {
                    setViewWidth((int) value_x);
                    setViewHeight((int) value_y);
                }
                notifyCellChanged();
                break;
            case GEOMETRY_CTRL_SCALE:
                i.postTranslate(-mTempRectF.centerX(), -mTempRectF.centerY());
                i.postRotate(-r);
                sx = value_x / Utils.getScaleforMatrix(i, true);
                //if(Math.abs(sx) < 0.001) sx = 1;
                sy = value_y / Utils.getScaleforMatrix(i, false);
                //if(Math.abs(sy) < 0.001) sy = 1;
                i.postScale(sx, sy);
                i.postRotate(r);
                i.postTranslate(mTempRectF.centerX(), mTempRectF.centerY());
                break;

            case GEOMETRY_CTRL_ROTATE:
                i.postRotate(value_x - r, mTempRectF.centerX(), mTempRectF.centerY());
                break;

            case GEOMETRY_CTRL_SKEW:
//                                        sx = getScaleforMatrix(i, true);
//                                        sy = getScaleforMatrix(i, false);
                i.postTranslate(-mTempRectF.centerX(), -mTempRectF.centerY());
                i.postRotate(-r);
                //i.postScale(1/sx, 1/sy);
                float okx = Utils.getSkewforMatrix(i, true);
                float kx = value_x - okx;
                float oky = Utils.getSkewforMatrix(i, false);
                float ky = value_y - oky;
                i.postSkew(kx, ky);
                //i.postScale(sx, sy);
                i.postRotate(r);
                i.postTranslate(mTempRectF.centerX(), mTempRectF.centerY());
                break;

        }
        setTransform(i, fast);
    }

    public void notifyCellChanged() {
        if(mItemConfig != mPage.config.defaultItemConfig) {
            mItemConfig.box.loadAssociatedDrawables(mPage.getIconDir(), mId, true);
        }
        mPage.onItemCellChanged(this);
    }

    public void notifyBindingsChanged(boolean apply) {
        mPage.onItemBindingsChanged(this, apply);
    }
	
	public int getViewWidth() {
		return mViewSize.x;
	}
	
	public void setViewWidth(int width) {
		mViewSize.x=width;
	}
	
	public int getViewHeight() {
		return mViewSize.y;
	}
	
	public void setViewHeight(int height) {
		mViewSize.y=height;
	}

    @Override
	public ItemConfig getItemConfig() {
		return mItemConfig;
	}

    @Override
	public void setItemConfig(ItemConfig c) {
        mSharedItemConfig=(mSharedItemConfig && c==mItemConfig);
		mItemConfig=c;
	}

    @Override
    public ItemConfig modifyItemConfig() {
        if(mSharedItemConfig) {
            // copy on write
            ItemConfig ic = new ItemConfig();
            ic.copyFrom(mItemConfig);
            mItemConfig = ic;
            mSharedItemConfig = false;
        }
        return mItemConfig;
    }

    public boolean hasSharedItemConfig() {
        return mSharedItemConfig;
    }

    public abstract ItemView createView(Context context);

    protected void readItemFromJSONObject(JSONObject o) throws JSONException {
    	mId=o.getInt(JsonFields.ITEM_ID);

        JSONObject tags = o.optJSONObject(JsonFields.ITEM_TAGS);
        if(tags != null) {
            mTags = Utils.jsonObjectToHashMap(tags);
        }
        // compatibility
    	String tag=o.optString(JsonFields.ITEM_TAG, null);
        if(tag != null) {
            if(mTags == null) mTags = new HashMap<>(1);
            mTags.put("_", tag);
        }
    	
    	int x=o.getInt(JsonFields.ITEM_CEll_P_X);
    	int y=o.getInt(JsonFields.ITEM_CEll_P_Y);
    	int w=o.optInt(JsonFields.ITEM_CEll_P_WIDTH, 1);
    	int h=o.optInt(JsonFields.ITEM_CEll_P_HEIGHT, 1);
    	mCellP=new Rect(x, y, x+w, y+h);
    	if(o.has(JsonFields.ITEM_CEll_L_X)) {
    		x=o.getInt(JsonFields.ITEM_CEll_L_X);
        	y=o.getInt(JsonFields.ITEM_CEll_L_Y);
        	w=o.getInt(JsonFields.ITEM_CEll_L_WIDTH);
        	h=o.getInt(JsonFields.ITEM_CEll_L_HEIGHT);
        	mCellL=new Rect(x, y, x+w, y+h);
    	}
    	mCell=mCellP;
    	
    	mTransformP=readMatrixFromJsonArray(o.optJSONArray(JsonFields.ITEM_TRANSFORM_P));
        if(mTransformP == null) {
            mTransformP = new Matrix();
        }
        mTransformL=readMatrixFromJsonArray(o.optJSONArray(JsonFields.ITEM_TRANSFORM_L));
    	mTransform=mTransformP;
//    	float[] values=new float[9];
//    	mTransform.getValues(values);
//    	if(values[Matrix.MTRANS_X]>100000 || values[Matrix.MTRANS_X]<-100000 || values[Matrix.MTRANS_Y]>100000 || values[Matrix.MTRANS_Y]<-100000) {
//    		values[Matrix.MTRANS_X]=100;
//    		values[Matrix.MTRANS_Y]=100;
//    		mTransform.setValues(values);
//    	}
		
        mViewSizeP = new Point(o.optInt(JsonFields.ITEM_WIDTH_P, 0), o.optInt(JsonFields.ITEM_HEIGHT_P, 0));
        if(o.has(JsonFields.ITEM_WIDTH_L)) {
            mViewSizeL = new Point(o.getInt(JsonFields.ITEM_WIDTH_L), o.getInt(JsonFields.ITEM_HEIGHT_L));
        }
        mViewSize = mViewSizeP;

		mAppDrawerHidden = o.optBoolean(JsonFields.ITEM_SHOULD_BE_HIDDEN, false);

        mLastUpdateTime=o.optLong(JsonFields.ITEM_LAST_UPDATE_TIME, 0);

        mName = o.optString(JsonFields.ITEM_NAME, null);

        mVisible = o.optBoolean(JsonFields.ITEM_VISIBLE, true);

		JSONObject json_configuration=o.optJSONObject(JsonFields.ITEM_CONFIGURATION);
        mSharedItemConfig = json_configuration == null;
        ItemConfig defaultItemConfig = mPage.config.defaultItemConfig;
        if (mSharedItemConfig) {
            mItemConfig = defaultItemConfig;
        } else {
            mItemConfig = ItemConfig.readFromJsonObject(json_configuration, defaultItemConfig);
            mItemConfig.loadAssociatedIcons(mPage.getIconDir(), mId);
        }

        mAlpha = mItemConfig.alpha;
    }

    public void init(int id, Rect cell_p, Rect cell_l) {
        mItemConfig = mPage.config.defaultItemConfig;
        mSharedItemConfig=true;
        mId=id;
        mCellP=cell_p;
        mCellL=cell_l;
        mCell=mCellP;
        mAppDrawerHidden =false;
        mTransformP=new Matrix();
        mTransformL=null;
        mTransform=mTransformP;
        mViewSizeP=new Point();
        mViewSizeL=null;
        mViewSize=mViewSizeP;
        mAlpha = mItemConfig.alpha;
    }

    public abstract void createFromJSONObject(JSONObject o) throws JSONException;

    private Matrix readMatrixFromJsonArray(JSONArray transform) throws JSONException {
        if(transform==null) {
            return null;
        }
    	int l=transform.length();
		float[] values=new float[l];
		for(int i=0; i<l; i++) { 
			values[i]=(float)transform.getDouble(i); 
		}
		Matrix m=new Matrix();
		m.setValues(values);
		return m;
    }


    
    public void setTag(String id, String tag) {
        if(id == null) id = "_";
        if (tag == null) {
            if(mTags != null) {
                mTags.remove(id);
            }
        } else {
            if (mTags == null) mTags = new HashMap<>(1);
            mTags.put(id, tag);
        }
        mPage.setModified();
    }

    public String getTag(String id) {
        if(id == null) id = "_";
        return mTags == null ? null : mTags.get(id);
    }
    
    public static Item loadItemFromJSONObject(Page page, JSONObject o) throws JSONException {
        Type type;
        try {
            type = Type.valueOf(o.getString(JsonFields.ITEM_TYPE));
        } catch (Exception e) {
            return null;
        }

        Item item;
        switch (type) {
            case SHORTCUT: item = new Shortcut(page); break;
            case WIDGET: item = new Widget(page); break;
            case FOLDER: item = new Folder(page); break;
            case STOP_POINT: item = new StopPoint(page); break;
            case DTEXT: item = new DynamicText(page); break;
            case EFOLDER: item = new EmbeddedFolder(page); break;
            case UNLOCKER: item = new Unlocker(page); break;
            case PAGE_INDICATOR: item = new PageIndicator(page); break;
            case CUSTOM_VIEW: item = new CustomView(page); break;
            default: return null;
        }
        item.createFromJSONObject(o);

        return item;
    }
	








    public JSONObject toJSONObject() throws JSONException {
        JSONObject json_item=new JSONObject();

        json_item.put(JsonFields.ITEM_ID, mId);

        if(mTags != null) json_item.put(JsonFields.ITEM_TAGS, new JSONObject(mTags));
        
        Rect cell=mCellP;
        json_item.put(JsonFields.ITEM_CEll_P_X, cell.left);
        json_item.put(JsonFields.ITEM_CEll_P_Y, cell.top);
        int width = cell.width();
        if(width != 1) json_item.put(JsonFields.ITEM_CEll_P_WIDTH, width);
        int height = cell.height();
        if(height != 1) json_item.put(JsonFields.ITEM_CEll_P_HEIGHT, height);

        cell=mCellL;
        if(cell!=null) {
            json_item.put(JsonFields.ITEM_CEll_L_X, cell.left);
            json_item.put(JsonFields.ITEM_CEll_L_Y, cell.top);
            json_item.put(JsonFields.ITEM_CEll_L_WIDTH, cell.width());
            json_item.put(JsonFields.ITEM_CEll_L_HEIGHT, cell.height());
        }

        if(mAppDrawerHidden) {
            json_item.put(JsonFields.ITEM_SHOULD_BE_HIDDEN, true);
        }

        if(!mTransformP.isIdentity()) {
            json_item.put(JsonFields.ITEM_TRANSFORM_P, Utils.getMatrixAsJSONArray(mTransformP));
        }
        if(mTransformL!=null && !mTransformL.isIdentity()) {
            json_item.put(JsonFields.ITEM_TRANSFORM_L, Utils.getMatrixAsJSONArray(mTransformL));
        }

        json_item.put(JsonFields.ITEM_WIDTH_P, mViewSizeP.x);
        json_item.put(JsonFields.ITEM_HEIGHT_P, mViewSizeP.y);
        if(mViewSizeL!=null) {
            json_item.put(JsonFields.ITEM_WIDTH_L, mViewSizeL.x);
            json_item.put(JsonFields.ITEM_HEIGHT_L, mViewSizeL.y);
        }

        json_item.put(JsonFields.ITEM_LAST_UPDATE_TIME, mLastUpdateTime);
        json_item.put(JsonFields.ITEM_NAME, mName);
        if(!mVisible) {
            json_item.put(JsonFields.ITEM_VISIBLE, false);
        }

        if(!mSharedItemConfig) {
            JSONObject value = JsonLoader.toJSONObject(mItemConfig, mPage.config.defaultItemConfig);
            json_item.put(JsonFields.ITEM_CONFIGURATION, value);
        }

        // TODO should split this as abstract method implemented by each subclass
        Class<? extends Item> cls = getClass();
        if(this instanceof Shortcut) {
            Type type = Type.SHORTCUT;
            Shortcut s=(Shortcut)this;
            if(this instanceof Folder) {
                type = cls == Folder.class ? Type.FOLDER : Type.EFOLDER;
                Folder f=(Folder)this;
                final FolderConfig fc=f.getFolderConfig();
                final FolderConfig dfc=f.mPage.config.defaultFolderConfig;
                if(dfc!=fc) {
                    json_item.put(JsonFields.FOLDER_CONFIGURATION, JsonLoader.toJSONObject(fc, dfc));
                }
                json_item.put(JsonFields.FOLDER_PAGE, f.getFolderPageId());
            } else if(this instanceof DynamicText) {
                type = Type.DTEXT;
                DynamicText dt=(DynamicText)this;
                final DynamicTextConfig dtc=dt.getDynamicTextConfig();
                final DynamicTextConfig ddtc=dt.getPage().config.defaultDynamicTextConfig;
                if(ddtc!=dtc) {
                    json_item.put(JsonFields.DYNAMIC_TEXT_CONFIGURATION, JsonLoader.toJSONObject(dtc, ddtc));
                }
            }
            json_item.put(JsonFields.ITEM_TYPE, type);
            json_item.put(JsonFields.SHORTCUT_LABEL, s.getLabel());
            json_item.put(JsonFields.SHORTCUT_INTENT, s.getIntent().toUri(0));

            final ShortcutConfig sc=s.getShortcutConfig();
            final ShortcutConfig dsc=s.getPage().config.defaultShortcutConfig;
            if(dsc!=sc) {
                json_item.put(JsonFields.SHORTCUT_CONFIGURATION, JsonLoader.toJSONObject(sc, dsc));
            }
        } else if(cls ==Widget.class) {
            Widget w=(Widget)this;
            json_item.put(JsonFields.ITEM_TYPE, Type.WIDGET);
            json_item.put(JsonFields.WIDGET_APP_WIDGET_ID, w.getAppWidgetId());
            ComponentName cn=w.getComponentName();
            if(cn!=null) {
                json_item.put(JsonFields.WIDGET_COMPONENT_NAME, cn.flattenToString());
            }
            String label = w.getAppWidgetLabel();
            if(label != null) {
                json_item.put(JsonFields.WIDGET_LABEL, label);
            }
        } else if(cls ==Unlocker.class) {
            json_item.put(JsonFields.ITEM_TYPE, Type.UNLOCKER);
        } else if(cls ==StopPoint.class) {
            StopPoint sp = (StopPoint) this;
            json_item.put(JsonFields.ITEM_TYPE, Type.STOP_POINT);
            json_item.put(JsonFields.STOP_POINT_DIRECTION, sp.getDirection());
            json_item.put(JsonFields.STOP_POINT_MATCH_EDGE, sp.getMatchEdge());
            json_item.put(JsonFields.STOP_POINT_WHAT, sp.getWhat());
            json_item.put(JsonFields.STOP_POINT_BARRIER, sp.isBarrier());
            json_item.put(JsonFields.STOP_POINT_DESKTOP_WIDE, sp.isDesktopWide());
            json_item.put(JsonFields.STOP_POINT_SNAPPING, sp.isSnapping());
            EventAction ea = sp.getReachedAction();
            if (ea.action != GlobalConfig.UNSET) {
                json_item.put(JsonFields.STOP_POINT_REACHED_EVENT_ACTION, JsonLoader.toJSONObject(ea, null));
            }
        } else if(cls == PageIndicator.class) {
            PageIndicator pi = (PageIndicator) this;
            json_item.put(JsonFields.ITEM_TYPE, Type.PAGE_INDICATOR);
            final ShortcutConfig sc=pi.getShortcutConfig();
            final ShortcutConfig dsc=pi.getPage().config.defaultShortcutConfig;
            if(dsc!=sc) {
                json_item.put(JsonFields.SHORTCUT_CONFIGURATION, JsonLoader.toJSONObject(sc, dsc));
            }
            JsonLoader.toJSONObject(json_item, pi, null);
        } else if(cls == CustomView.class) {
            CustomView cv = (CustomView) this;
            json_item.put(JsonFields.ITEM_TYPE, Type.CUSTOM_VIEW);
            JsonLoader.toJSONObject(json_item, cv, null);
        }

        return json_item;
    }

    private static String[] sItemNames;

    public static void loadItemNames(Resources resources) {
        String[] some_names = resources.getStringArray(R.array.dialog_action_values);
        sItemNames = new String[] {
                some_names[1],
                some_names[2],
                some_names[4],
                some_names[5],
                resources.getString(R.string.efolder),
                resources.getString(R.string.dtext),
                resources.getString(R.string.i_ul),
                resources.getString(R.string.pi),
                resources.getString(R.string.cv),
        };
    }

    public String formatForDisplay(boolean id_after, int maxLength) {
        String type;
        Class<?> cls = getClass();
        if(cls == Shortcut.class) {
            type = sItemNames[0];
        } else if(cls == Widget.class) {
            type = sItemNames[1];
        } else if(cls == Folder.class) {
            type = sItemNames[2];
        } else if(cls == StopPoint.class) {
            type = sItemNames[3];
        } else if(cls == EmbeddedFolder.class) {
            type = sItemNames[4];
        } else if(cls == DynamicText.class) {
            type = sItemNames[5];
        } else if(cls == Unlocker.class) {
            type = sItemNames[6];
        } else if(cls == PageIndicator.class) {
            type = sItemNames[7];
        } else if(cls == CustomView.class) {
            type = sItemNames[8];
        } else {
            type = "";
        }
        String n = getName();
        if("".equals(n)) n=null;
        if(this instanceof Shortcut) {
            String l = ((Shortcut)this).getLabel();
            Spanned h = Html.fromHtml(l);
            if(h.length() > 0) l = h.toString();
            if(l != null) {
                if (maxLength > 0 && l.length() > maxLength) l = l.substring(0, maxLength) + "…";
                n = n == null || l.equals(n) ? l : l + " (" + n + ")";
            }
        }
        if("".equals(n)) n=null;
        String id = Utils.formatHex(mId, 6);
        String text = type+(n==null ? "": " '"+n+"'");
        if(id_after) {
            return text+" ("+id+")";
        } else {
            return id+": "+text;
        }
    }

    public void notifyChanged() {
        mPage.notifyItemChanged(this);
    }
}
