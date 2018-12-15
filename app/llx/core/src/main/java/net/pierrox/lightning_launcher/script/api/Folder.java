package net.pierrox.lightning_launcher.script.api;

import android.graphics.Point;
import android.graphics.drawable.Drawable;

import net.pierrox.lightning_launcher.data.*;
import net.pierrox.lightning_launcher.engine.Screen;
import net.pierrox.lightning_launcher.views.FolderView;
import net.pierrox.lightning_launcher.views.ItemLayout;
import net.pierrox.lightning_launcher.views.item.ItemView;

import java.io.File;

/**
 * The folder item extends the shortcut (because it have a label and an icon) and add folder specific services such as open/close. 
 */
public class Folder extends Shortcut {

	/**
	 * @hide
	 */
	public Folder(Lightning lightning, ItemView itemView) {
		super(lightning, itemView);
	}

	/**
	 * @hide
	 */
	@Override
	public void setLabel(String label, boolean persistent) {
		net.pierrox.lightning_launcher.data.Folder folder = (net.pierrox.lightning_launcher.data.Folder) getItem();
		folder.getPage().notifyFolderWindowChanged(folder);
		super.setLabel(label, persistent);
	}

	/**
	 * Open the folder if it is currently closed, else close it.
	 */
	public void launch() {
		if(isOpen()) {
			close();
		} else {
			open();
		}
	}

	/**
	 * Open the folder if it not already opened. Does nothing when the script is run in background.
	 */
	public void open() {
        if(!isOpen()) {
			Screen screen = mItemView.getParentItemLayout().getScreen();
			screen.openFolder(mItemView);
        }
	}

	/**
	 * Open the folder if it not already opened, using an absolute position for the start point of the animation. Does nothing when the script is run in background.
	 * @param x absolute abscissa on the screen
	 * @param y absolute ordinate on the screen
	 */
	public void openFrom(int x, int y) {
        if (!isOpen()) {
			Screen screen = mItemView.getParentItemLayout().getScreen();
			screen.openFolder((net.pierrox.lightning_launcher.data.Folder) mItemView.getItem(), mItemView, new Point(x, y), false);
        }
	}

	/**
	 * Close the folder if it is currently open. Does nothing if the script is run in the background.
	 */
	public void close() {
		ItemLayout il = mItemView.getParentItemLayout();
		if(il == null) {
			return;
		}

		net.pierrox.lightning_launcher.engine.Screen screen = il.getScreen();
		if (screen.isFolderOpened(mItemView)) {
			screen.closeFolder(mItemView);
        }
	}

	/**
	 * Returns the current folder state. This function will always return false if the script is run in the background.
	 * @return true if the folder is open
	 */
	public boolean isOpen() {
		ItemLayout il = mItemView.getParentItemLayout();
		if(il == null) {
			return false;
		}
        return il.getScreen().isFolderOpened(mItemView);
	}
	
	/**
	 * Returns the container holding items for this folder.
	 */
	public Container getContainer() {
		net.pierrox.lightning_launcher.engine.Screen screen = mItemView.getParentItemLayout().getScreen();
		FolderView folderView = screen.openFolder((net.pierrox.lightning_launcher.data.Folder) getItem(), mItemView, null, true);
		return mLightning.getCachedContainer(folderView.getItemLayout());
	}

	/**
	 * Change the container of this folder.
	 * That is: use an alternate set of items in the folder window.
	 */
	public void setContainerId(int id) {
		//if(Page.isFolder(id)) {
			((net.pierrox.lightning_launcher.data.Folder) getItem()).setFolderPageId(id);
			getItem().getPage().setModified();
		//}
	}

    /**
     * Retrieve the image currently displayed as the folder window background.
	 * Note: the object returned by this method may not be the one set through #setWindowBackground.
     * @return an image, or null if there is no image
     */
    public Image getWindowBackground() {
		net.pierrox.lightning_launcher.data.Item item = getItem();
		Drawable d = ((net.pierrox.lightning_launcher.data.Folder) item).getFolderConfig().box.bgNormal;
		File icon_dir = item.getPage().getIconDir();
		int id = item.getId();
		return Image.fromDrawable(mLightning, d, item, net.pierrox.lightning_launcher.data.Box.getBoxBackgroundFolder(icon_dir, id));
    }

    /**
     * Set a window background image.
	 * This is done in memory for a given screen and is not persisted.
	 * Note: instances of ImageAnimation are currently not supported
     */
    public void setWindowBackground(Image image) {
		if(image == null) return;

		Drawable d = image.toDrawable();
		if(d != null) {
			net.pierrox.lightning_launcher.data.Folder folder = (net.pierrox.lightning_launcher.data.Folder) getItem();
			folder.getFolderConfig().box.bgNormal = d;
			ItemLayout il = mItemView.getParentItemLayout();
			if(il != null) {
				FolderView fv = il.getScreen().findFolderView(mItemView, true);
				if (fv != null) {
					fv.invalidate();
				}
			}
		}
    }
}
