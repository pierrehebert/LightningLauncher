package net.pierrox.lightning_launcher.activities;

import android.content.Context;
import android.content.res.Resources;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.util.ResourcesWrapper;

public class ResourcesWrapperHelper {

    private Resources mBaseResources;
    private ResourcesWrapper mResourcesWrapper;

    public final Resources getResources() {
        if(mResourcesWrapper != null) {
            return mResourcesWrapper;
        } else {
            return mBaseResources;
        }

    }

    public ResourcesWrapperHelper(Context context, Resources base_resources) {
        mBaseResources = base_resources;
        String language = LLApp.get().getLanguage();
        if(language != null) {
            mResourcesWrapper = new ResourcesWrapper(mBaseResources);
            mResourcesWrapper.setTranslationPackageName(context, language);
        }
    }
}
