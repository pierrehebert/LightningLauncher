package net.pierrox.lightning_launcher.activities;

import android.app.ListActivity;
import android.content.res.Resources;

public abstract class ResourceWrapperListActivity extends ListActivity {
    private ResourcesWrapperHelper mResourcesWrapperHelper;

    @Override
    public final Resources getResources() {
        if(mResourcesWrapperHelper == null) {
            mResourcesWrapperHelper = new ResourcesWrapperHelper(this, super.getResources());
        }
        return mResourcesWrapperHelper.getResources();
    }
}
