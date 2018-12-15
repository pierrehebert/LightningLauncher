package net.pierrox.lightning_launcher.data;

import android.content.Context;

import net.pierrox.lightning_launcher.views.item.EmbeddedFolderView;
import net.pierrox.lightning_launcher.views.item.ItemView;

public class EmbeddedFolder extends Folder {

    public EmbeddedFolder(Page page) {
        super(page);
    }

//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        getOrLoadFolderPage().destroy();
//    }

    @Override
    public ItemView createView(Context context) {
        return new EmbeddedFolderView(context, this);
    }
}
