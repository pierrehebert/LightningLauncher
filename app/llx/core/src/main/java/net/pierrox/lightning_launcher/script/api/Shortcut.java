package net.pierrox.lightning_launcher.script.api;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import net.pierrox.lightning_launcher.configuration.ShortcutConfig;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.util.AnimationDecoder;
import net.pierrox.lightning_launcher.views.Graphics;
import net.pierrox.lightning_launcher.views.SharedAsyncGraphicsDrawable;
import net.pierrox.lightning_launcher.views.item.ItemView;

import org.mozilla.javascript.ScriptRuntime;

import java.io.File;

/**
 * A shortcut is a label and an icon, tapping on it will usually launch an app. 
 * It is used for both so called 'apps' and 'shortcuts' because these two objects are really the same thing (technically) 
 */
public class Shortcut extends Item {
	
	/**
	 * @hide
	 */
	/*package*/ Shortcut(Lightning lightning, ItemView itemView) {
		super(lightning, itemView);
	}

    /**
     * Launch the intent associated with this shortcut. This generic method does nothing when the script is run in background: in this context an app or shortcut can be launched using <code>getActiveScreen().getContext().startActivity(item.getIntent));</code> instead.
     */
	public void launch() {
        mItemView.getParentItemLayout().getScreen().launchItem(mItemView);
	}

    /**
     * Returns the label of this shortcut.
     * @undeprecate
     */
    public String getLabel() {
        return ((net.pierrox.lightning_launcher.data.Shortcut) getItem()).getLabel();
    }

    /**
     * Set the label of this shortcut. Note that changes will not be persisted, and the item can still be retrieved using its original name. Same as calling <pre>setLabel(label, false);</pre>.
     * @param label new value for the label
     * @undeprecate
     */
    public void setLabel(String label) {
        super.setLabel(label);
    }

    /**
     * Set the label of this shortcut, possibly making the change persistent.
     * @param label new value for the label
     * @undeprecate
     */
    public void setLabel(String label, boolean persistent) {
        super.setLabel(label, persistent);
    }

    /**
     * Retrieve the default icon file associated to this shortcut. For an app this is the original icon defined by the package.
     */
    public Image getDefaultIcon() {
        File f = getItem().getDefaultIconFile();
        return getImage(f);
    }

    /**
     * Set a new icon for this item.
     * Saving animations is experimental.
     * @see #getDefaultIcon()
     * @param image using a null object will delete the icon
     */
    public void setDefaultIcon(Image image) {
        net.pierrox.lightning_launcher.data.Item item = getItem();
        copyImageToFile(image, item.getDefaultIconFile());
        item.notifyChanged();
    }

    /**
     * Retrieve the custom icon file associated with this shortcut. The custom icon overrides the default icon but the default icon is kept so that the shortcut can be reverted to its original state.
     * @return an icon, or null if no icon is defined
     */
    public Image getCustomIcon() {
        File f = getItem().getCustomIconFile();
        return getImage(f);
    }

    private Image getImage(File f) {
        Image image = null;
        if(f.exists()) {
            Graphics graphics = Utils.loadGraphics(f);
            if(graphics != null) {
                Bitmap bitmap = graphics.getBitmap();
                net.pierrox.lightning_launcher.data.Item item = getItem();
                if(bitmap != null) {
                    // directly create an ImageBitmap to avoid one bitmap copy because of SharedAsyncGraphicsDrawable
                    image = new ImageBitmap(mLightning, bitmap);
                    image.setSource(item, f, null);
                    return image;
                } else {
                    SharedAsyncGraphicsDrawable d = new SharedAsyncGraphicsDrawable(graphics, true);
                    image = Image.fromDrawable(mLightning, d, item, f);
                }
            }
        }

        return image;
    }

    /**
     * Set a new custom icon for this item.
     * Saving animations is experimental.
     * @see #getCustomIcon()
     */
    public void setCustomIcon(Image image) {
        net.pierrox.lightning_launcher.data.Item item = getItem();
        copyImageToFile(image, item.getCustomIconFile());
        item.notifyChanged();
    }

    /**
     * Retrieves the live image for this shortcut as displayed on a given screen
     * This is the image which results in compositing the default or custom icon with various options, such as reflection. This image is in RAM only.
     */
    public Image getImage() {
        SharedAsyncGraphicsDrawable d = ((net.pierrox.lightning_launcher.data.Shortcut) getItem()).getSharedAsyncGraphicsDrawable();
        return Image.fromDrawable(mLightning, d, getItem(), null);
    }

    /**
     * Replaces the current live image on a given screen.
     * This is done in RAM only and has no impact on default or custom icons.
     */
    public void setImage(Image image) {
        SharedAsyncGraphicsDrawable d = ((net.pierrox.lightning_launcher.data.Shortcut) getItem()).getSharedAsyncGraphicsDrawable();
        if(image instanceof ImageBitmap) {
            d.setBitmap(((ImageBitmap)image).getBitmap());
        } else if(image instanceof ImageNinePatch) {
            d.setGraphics(new Graphics(((ImageNinePatch)image).getNinePatch().getBitmap()));
        } else if(image instanceof ImageAnimation) {
            AnimationDecoder decoder = ((ImageAnimation) image).getDecoder();
            d.setGraphics(new Graphics(decoder, decoder.getWidth(), decoder.getHeight()));
        } else if(image instanceof ImageScript) {
            ImageScript is = (ImageScript) image;
            d.setScriptObject(is.getScriptExecutor(), is.getObject(), is.getWidth(), is.getHeight(), this);
        } else {
            File file = image.getSourceFile();
            if(file != null) {
                d.setGraphics(Utils.loadGraphics(file));
            }
        }
    }

    /**
     * Return the intent associated with this shortcut
     */
    public Intent getIntent() {
        net.pierrox.lightning_launcher.data.Shortcut s = (net.pierrox.lightning_launcher.data.Shortcut) getItem();
        return s.getIntent();
    }

    /**
     * Set the intent launched by this shortcut.
     */
    public void setIntent(Intent intent) {
        if(intent == null) {
            throw ScriptRuntime.constructError("setIntent", "Argument 'intent' cannot be null");
        }
        ((net.pierrox.lightning_launcher.data.Shortcut) getItem()).setIntent(intent);
    }

    /**
     * Retrieve the image currently used in a shortcut icon layer.
     * This image can be shared amongst several items.
     * @param which either "b" for background, "o" for overlay, "m" for mask
     * @return an image, or null if there is no image
     */
    public Image getIconLayer(String which) {
        Drawable d;
        File f;
        net.pierrox.lightning_launcher.data.Item item = getItem();
        File icon_dir = item.getPage().getIconDir();
        int id = item.getId();
        net.pierrox.lightning_launcher.data.Shortcut shortcut = (net.pierrox.lightning_launcher.data.Shortcut) item;
        ShortcutConfig sc = shortcut.getShortcutConfig();
        if (which.equals("b")) {
            d = sc.iconBack;
            f = ShortcutConfig.getIconBackFile(icon_dir, id);
        } else if (which.equals("o")) {
            d = sc.iconOver;
            f = ShortcutConfig.getIconOverFile(icon_dir, id);
        } else {
            d = sc.iconMask;
            f = ShortcutConfig.getIconMaskFile(icon_dir, id);
        }
        return Image.fromDrawable(mLightning, d, item, f);
    }

    /**
     * Set an icon layer image (background, overlay, mask).
     * Note: since the image is set on the shortcut configuration, which can be shared by several items, it may be that the modification is visible on other items
     * Usage of ImageAnimation is currently not supported.
     * @param image
     * @param which either "b" for background, "o" for overlay, "m" for mask
     */
    public void setIconLayer(Image image, String which) {
        Drawable d = image == null ? null : image.toDrawable();
        net.pierrox.lightning_launcher.data.Item item = getItem();
        net.pierrox.lightning_launcher.data.Shortcut shortcut = (net.pierrox.lightning_launcher.data.Shortcut) item;
        ShortcutConfig sc = shortcut.getShortcutConfig();
        if("b".equals(which)) sc.iconBack = d;
        else if("o".equals(which)) sc.iconOver = d;
        else if("m".equals(which)) sc.iconMask = d;
        item.notifyChanged();
    }


    /**
     * Set an icon layer image (background, overlay, mask), persistently.
     * This method behaves differently according to the persistent parameter. If set to false, the method will behave as #setIconLayer(Image,String), otherwise the modification will be persistent and will only affect this item.
     * Usage of ImageAnimation is currently not supported (only the first frame will be displayed).
     * @param image
     * @param which either "b" for background, "o" for overlay, "m" for mask
     * @param persistent whether to save changes and affect only this item
     */
    public void setIconLayer(Image image, String which, boolean persistent) {
        if(persistent) {
            Drawable d = image == null ? null : image.toDrawable();

            net.pierrox.lightning_launcher.data.Item item = getItem();
            Page page = item.getPage();
            File icon_dir = page.getIconDir();
            int id = item.getId();

            net.pierrox.lightning_launcher.data.Shortcut shortcut = (net.pierrox.lightning_launcher.data.Shortcut) item;
            ShortcutConfig sc = shortcut.modifyShortcutConfig();

            if ("b".equals(which)) {
                sc.iconBack = d;
                copyImageToFile(image, ShortcutConfig.getIconBackFile(icon_dir, id));
            } else if ("o".equals(which)) {
                sc.iconOver = d;
                copyImageToFile(image, ShortcutConfig.getIconOverFile(icon_dir, id));
            } else if ("m".equals(which)) {
                sc.iconMask = d;
                copyImageToFile(image, ShortcutConfig.getIconMaskFile(icon_dir, id));
            }

            page.setModified();
        }
        setIconLayer(image, which);
    }

    /**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return getType()+" '"+getLabel()+"' "+ getItem().getId();
	}
}
