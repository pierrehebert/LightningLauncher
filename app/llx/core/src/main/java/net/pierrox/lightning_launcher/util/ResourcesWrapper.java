package net.pierrox.lightning_launcher.util;

import net.pierrox.lightning_launcher.R;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.SparseArray;

public class ResourcesWrapper extends Resources {

	private String mTranslationsPackageName;
    private Resources mBaseResources;
	private Resources mTranslationsResources;
	private SparseArray<CharSequence> mStringTranslationMap;
	private SparseArray<String[]> mStringArrayTranslationMap;

	private static ResourcesWrapper sSharedInstance;

	public ResourcesWrapper(Resources base_resources) {
		super(base_resources.getAssets(), base_resources.getDisplayMetrics(), base_resources.getConfiguration());
        mBaseResources = base_resources;
	}

	public void setTranslationPackageName(Context context, String pkg_name) {
		if(sSharedInstance == null) {
			try {
				mTranslationsPackageName = pkg_name;
				Context translations_context = context.createPackageContext(pkg_name, 0);
				mTranslationsResources = translations_context.getResources();

				mStringTranslationMap = new SparseArray<>(2000);
				mStringArrayTranslationMap = new SparseArray<>(2000);

				// this will be reached using the App context
				sSharedInstance = this;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public String getString(int id) throws NotFoundException {
		return getText(id).toString();
	}
	
	@Override
	public String[] getStringArray(int id) throws NotFoundException {
		if(sSharedInstance == this) {
			String s[] = mStringArrayTranslationMap.get(id);
			if (s != null) {
				return s;
			}

			String[] original = super.getStringArray(id);
			String name = mBaseResources.getResourceEntryName(id);
			int translated_id = mTranslationsResources.getIdentifier(name, "array", mTranslationsPackageName);
			if (translated_id == 0) {
				s = original;
			} else {
				try {
					s = mTranslationsResources.getStringArray(translated_id);
					int lo = original.length;
					int ls = s.length;
					if (ls < lo) {
						System.arraycopy(s, 0, original, 0, ls);
						s = original;
					} else if (ls > lo) {
						s = original;
					}
				} catch (NotFoundException e) {
					s = original;
				}
			}
			mStringArrayTranslationMap.put(id, s);

			return s;
		} else {
			return sSharedInstance.getStringArray(id);
		}
	}

	@Override
	public CharSequence getText(int id) throws NotFoundException {
		if(sSharedInstance == this) {
			CharSequence s = mStringTranslationMap.get(id);
			if (s != null) {
				return s;
			}

			String name = mBaseResources.getResourceEntryName(id);
			int translated_id = mTranslationsResources.getIdentifier(name, "string", mTranslationsPackageName);
			if (translated_id == 0) {
				s = super.getText(id);
			} else {
				try {
					s = mTranslationsResources.getText(translated_id);
				} catch (NotFoundException e) {
					s = super.getText(id);
				}
			}
			mStringTranslationMap.put(id, s);

			return s;
		} else {
			return sSharedInstance.getText(id);
		}
	}

    @Override
    public Configuration getConfiguration() {
        return mBaseResources.getConfiguration();
    }

    @Override
    public DisplayMetrics getDisplayMetrics() {
        return mBaseResources.getDisplayMetrics();
    }
}
