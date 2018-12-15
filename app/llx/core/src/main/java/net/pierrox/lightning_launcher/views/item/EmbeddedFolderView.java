package net.pierrox.lightning_launcher.views.item;

import android.content.Context;

import net.pierrox.lightning_launcher.data.EmbeddedFolder;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.views.ItemLayout;

public class EmbeddedFolderView extends ItemView {
    private ItemLayout mItemLayout;

    public EmbeddedFolderView(Context context, EmbeddedFolder embeddedFolder) {
        super(context, embeddedFolder);
    }

    @Override
    public void init() {
        mItemLayout = new ItemLayout(getContext(), null);
        mItemLayout.setEmbedded(true);
        mItemLayout.setOpenerItemView(this);

        setView(mItemLayout);

        Page page = ((EmbeddedFolder)mItem).getOrLoadFolderPage();
        getScreen().takeItemLayoutOwnership(mItemLayout);
        mItemLayout.setPage(page);
        page.notifyLoaded(mItemLayout);
    }

    @Override
    public void destroy() {
        super.destroy();

        if(isInitDone()) {
            mItemLayout.getScreen().releaseItemLayout(mItemLayout);
        }
    }

    @Override
    public void pause() {
        super.pause();
        mItemLayout.pause();
    }

    @Override
    public void resume() {
        super.resume();
        mItemLayout.resume();
    }

    @Override
    public void setEnabled(boolean d) {
        mItemLayout.setEnabled(d);
    }

    @Override
    public void evaluateEnabledState() {
        ItemLayout il = getParentItemLayout();
        boolean editMode = il.getEditMode();
        setEnabled(editMode || mItem.getItemConfig().enabled);
    }

    public ItemLayout getEmbeddedItemLayout() {
        return mItemLayout;
    }
}
