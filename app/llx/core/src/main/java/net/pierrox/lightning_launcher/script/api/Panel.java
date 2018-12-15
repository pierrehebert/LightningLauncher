package net.pierrox.lightning_launcher.script.api;

import net.pierrox.lightning_launcher.views.item.EmbeddedFolderView;
import net.pierrox.lightning_launcher.views.item.ItemView;

/**
 * The panel is a special kind of item, providing access to items contained in this panel.
 */
public class Panel extends Item {

	/**
	 * @hide
	 */
	public Panel(Lightning lightning, ItemView itemView) {
		super(lightning, itemView);
	}

	/**
	 * Returns the container holding items for this panel.
	 */
	public Container getContainer() {
		return mLightning.getCachedContainer(((EmbeddedFolderView)mItemView).getEmbeddedItemLayout());
	}

	/**
	 * Change the container of this panel.
	 * That is: use an alternate set of items in the panel.
	 */
	public void setContainerId(int id) {
		//if(Page.isFolder(id)) {
		net.pierrox.lightning_launcher.data.Item item = getItem();
		((net.pierrox.lightning_launcher.data.Folder) item).setFolderPageId(id);
		item.getPage().setModified();
		//}
	}
}
