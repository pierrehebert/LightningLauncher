package net.pierrox.lightning_launcher.data;

import android.content.Intent;
import android.graphics.Rect;
import net.pierrox.lightning_launcher.configuration.FolderConfig;
import net.pierrox.lightning_launcher.configuration.FolderConfigStylable;
import net.pierrox.lightning_launcher.configuration.JsonFields;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

public class Folder extends Shortcut implements FolderConfigStylable{
	private int mPageId = Page.NONE;
	private FolderConfig mFolderConfig;
    private boolean mSharedFolderConfig;

    public Folder(Page page) {
        super(page);
    }

    @Override
    public void onRemove(boolean keepResources) {
        super.onRemove(keepResources);
        // always keep the user menu, desktop and app drawer pages: they can be linked from a folder, but they cannot be removed here
        if(!keepResources && mPageId != Page.USER_MENU_PAGE && mPageId != Page.APP_DRAWER_PAGE && !Page.isDashboard(mPageId)) {
            // delete the page once there is no more usage
            if(getOrLoadFolderPage().findAllOpeners().size() == 1) {
                mPage.getEngine().getPageManager().removePage(mPageId);
            }
        }
    }

    public int getFolderPageId() {
		return mPageId;
	}

    public Page getOrLoadFolderPage() {
        return mPage.getEngine().getOrLoadPage(mPageId);
    }
	
	public void setFolderPageId(int id) {
        if(id != mPageId) {
            int oldPageId = mPageId;
            mPageId = id;
            mPage.onFolderPageIdChanged(this, oldPageId);
        }
	}

    public boolean hasSharedFolderConfig() {
        return mSharedFolderConfig;
    }

    @Override
	public FolderConfig getFolderConfig() {
		return mFolderConfig;
	}

    @Override
	public void setFolderConfig(FolderConfig fc) {
        mSharedFolderConfig=(mSharedFolderConfig && fc==mFolderConfig);
		mFolderConfig=fc;
	}

    @Override
    public FolderConfig modifyFolderConfig() {
        if(mSharedFolderConfig) {
            // copy on write
            FolderConfig fc = new FolderConfig();
            fc.copyFrom(mFolderConfig);
            mFolderConfig = fc;
            mSharedFolderConfig = false;
        }
        return mFolderConfig;
    }

    @Override
	public void createFromJSONObject(JSONObject o) throws JSONException {
        super.createFromJSONObject(o);

        JSONObject json_configuration=o.optJSONObject(JsonFields.FOLDER_CONFIGURATION);
        mSharedFolderConfig = json_configuration == null;
        FolderConfig defaultFolderConfig = mPage.config.defaultFolderConfig;
        if (mSharedFolderConfig) {
            mFolderConfig= defaultFolderConfig;
        } else {
            mFolderConfig= FolderConfig.readFromJsonObject(json_configuration, defaultFolderConfig);
            mFolderConfig.loadAssociatedIcons(mPage.getIconDir(), mId);
        }

        mPageId =o.getInt(JsonFields.FOLDER_PAGE);
	}
	
	public void init(int id, Rect cell_p, Rect cell_l, String label, Intent intent, int page) {
		super.init(id, cell_p, cell_l, label, intent);
		mPageId =page;
		mFolderConfig=mPage.config.defaultFolderConfig;
        mSharedFolderConfig=true;
	}

    @Override
    public void getIconFiles(File icon_dir, ArrayList<File> out_files) {
        super.getIconFiles(icon_dir, out_files);
        out_files.add(Box.getBoxBackgroundFolder(icon_dir, mId));
    }

    public boolean hasWidget() {
        Page folderPage = mPage.getEngine().getOrLoadPage(mPageId);
        for(Item item : folderPage.items) {
            if(item instanceof Widget) {
                return true;
            } else if(item instanceof Folder && item != this) { // avoid endless recursion in case the folde is in itself
                boolean has_widget = ((Folder)item).hasWidget();
                if(has_widget) {
                    return true;
                }
            }
        }
        return false;
    }
}
