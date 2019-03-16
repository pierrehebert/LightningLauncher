package net.pierrox.lightning_launcher.script.api;

import android.content.Intent;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

import com.faendir.rhino_android.RhinoAndroidHelper;

import java.util.ArrayList;
import java.util.HashMap;

import net.pierrox.lightning_launcher.R;
import net.pierrox.lightning_launcher.data.EmbeddedFolder;
import net.pierrox.lightning_launcher.data.IconPack;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.views.ItemLayout;
import net.pierrox.lightning_launcher.views.item.ItemView;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContinuationPending;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;

/**
 * The container can be either a desktop, a folder or a panel. Its role is to manage items inside.
 *
 * An instance of this object can be retrieved with {@link Event#getContainer()}, {@link Item#getParent()}, {@link Folder#getContainer()}, {@link Panel#getContainer()}, {@link net.pierrox.lightning_launcher.script.api.screen.Screen#getContainerById(int)}, {@link net.pierrox.lightning_launcher.script.api.screen.Screen#getAllContainersById(int)} or {@link Screen#getFocusedContainer()}; or by using directly the special variable 'container' (which is the current Container) when running a 'Menu' event.
 */
public class Container {
	public static final int NONE = Page.NONE;

	protected Lightning mLightning;
	protected ItemLayout mItemLayout;
	private PropertySet mProperties;

	private Scriptable mMy;

	/**
	 * @hide
     */
	/*package*/ Container(Lightning lightning, ItemLayout itemLayout) {
		mLightning = lightning;
		mItemLayout = itemLayout;
	}

	/**
	 * @hide
     */
	/*package*/ Page getPage() {
		return mItemLayout.getPage();
	}

	/**
	 * Provides a script object (possibly cached) by looking up the item view in the item layout (backed by this container) for a given data item
	 * @hide
	 */
	private Item getCachedItem(net.pierrox.lightning_launcher.data.Item item) {
		ItemView itemView = mItemLayout.getItemView(item);
		return mLightning.getCachedItem(itemView);
	}

	/**
	 * Return a scriptable object that can be used to store live data. These data won't be stored, unlike tags.
	 */
	public Scriptable getMy() {
		if(mMy == null) {
			mMy = Context.getCurrentContext().newObject(mLightning.getEngine().getScriptExecutor().getScriptScope());
		}
		return mMy;
	}

	/**
	 * Return a new script object, with this container as prototype
	 * @return a new script object which inherit from this container
     */
	public Scriptable extend() {
		Scriptable scope = mLightning.getEngine().getScriptExecutor().getScriptScope();
		Scriptable object = Context.getCurrentContext().newObject(scope);
		object.setPrototype(new NativeJavaObject(scope, this, null));
		return object;
	}

	/**
	 * Returns the container unique identifier.
	 */
	public int getId() {
		return getPage().id;
	}
	
	/**
	 * Returns the list of items in this container
	 * @deprecated use #getAllItems instead
	 */
	public Array getItems() {
		return new Array(getAllItems());
	}

	/**
	 * Returns the list of items in this container
	 */
	public Item[] getAllItems() {
		ArrayList<net.pierrox.lightning_launcher.data.Item> source_items = getPage().items;
		int n = source_items.size();
		Item[] items = new Item[n];
		for(n--; n>=0; n--) {
			items[n] = getCachedItem(source_items.get(n));
		}
		return items;
	}
	
	/**
	 * Retrieve an item using its identifier.
	 * @param id item identifier
	 * @return an item, or undefined if not found
	 */
	public Item getItemById(int id) {
		ItemView itemView = mItemLayout.getItemView(id);
		if(itemView == null) {
			return null;
		} else {
			return mLightning.getCachedItem(itemView);
		}
	}
	
	/**
	 * @deprecated Use #getItemByName instead
	 */
	public Item getItemByLabel(String label) {
		return getItemByName(label);
	}

    /**
	 * Retrieve an item using its name. If two or more items have the same name, the first is returned.
	 * @param name the name set in the "+" tab in the item settings, and if name is not set use the label for shortcuts and folders
	 * @return an item, or undefined if not found.
	 */
	public Item getItemByName(String name) {
		for(net.pierrox.lightning_launcher.data.Item item : getPage().items) {
            String item_name = item.getName();
            if((item_name==null || item_name.equals("")) && item instanceof net.pierrox.lightning_launcher.data.Shortcut) {
                item_name = ((net.pierrox.lightning_launcher.data.Shortcut) item).getLabel();
            }
            if(name.equals(item_name)) {
				return getCachedItem(item);
			}
		}
		return null;
	}

    /**
     * Create a new shortcut with the default icon and size.
     * @param label shortcut text
     * @param intent shortcut action, see http://developer.android.com/reference/android/content/Intent.html
     * @param x absolute position (rounded to nearest cell when attached to grid)
     * @param y absolute position (rounded to nearest cell when attached to grid)
     * @return the new shortcut
     */
    public Shortcut addShortcut(String label, Intent intent, float x, float y) {
        if(intent == null) {
            throw ScriptRuntime.constructError("addShortcut", "Argument 'intent' cannot be null");
        }
        if(Float.isNaN(x) || Float.isNaN(y)) {
            throw ScriptRuntime.constructError("addShortcut", "Bad argument(s)");
        }
		net.pierrox.lightning_launcher.data.Shortcut shortcut = Utils.addShortcut(label, null, intent, getPage(), x, y, 1, false);
		return (Shortcut) measureAndRetrieveCachedItem(shortcut);
    }

    /**
     * Create a new stop point with the default configuration.
     * @param x absolute position (rounded to nearest cell when attached to grid)
     * @param y absolute position (rounded to nearest cell when attached to grid)
     * @return the new stop point
     */
    public StopPoint addStopPoint(float x, float y) {
        if(Float.isNaN(x) || Float.isNaN(y)) {
            throw ScriptRuntime.constructError("addStopPoint", "Bad argument(s)");
        }
		net.pierrox.lightning_launcher.data.StopPoint stopPoint = Utils.addStopPoint(getPage(), x, y, 1, true);
		return (StopPoint) measureAndRetrieveCachedItem(stopPoint);
    }

    /**
     * Create a new empty folder with the default icon and size.
     * @param label folder name
     * @param x absolute position (rounded to nearest cell when attached to grid)
     * @param y absolute position (rounded to nearest cell when attached to grid)
     * @return the new folder
     */
    public Folder addFolder(String label, float x, float y) {
        if(Float.isNaN(x) || Float.isNaN(y)) {
            throw ScriptRuntime.constructError("addFolder", "Bad argument(s)");
        }
		if(label == null) {
			label = mLightning.getEngine().getContext().getString(R.string.default_folder_name);
		}
		net.pierrox.lightning_launcher.data.Folder folder = Utils.addFolder(getPage(), x, y, 1, false, label);
		return (Folder) measureAndRetrieveCachedItem(folder);
    }

    /**
     * Create a new empty panel. All coordinates are in pixels and rounded to the nearest cell when the item is attached to the grid.
     * @return the new panel
     */
    public Panel addPanel(float x, float y, float width, float height) {
        if(Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(width) || Float.isNaN(height)) {
            throw ScriptRuntime.constructError("addPanel", "Bad argument(s)");
        }
		EmbeddedFolder embeddedFolder = Utils.addEmbeddedFolder(getPage(), x, y, width, height, 1, false);
		return (Panel) measureAndRetrieveCachedItem(embeddedFolder);
    }

    /**
     * Create a new page indicator using default values.
     */
    public PageIndicator addPageIndicator(float x, float y) {
        if(Float.isNaN(x) || Float.isNaN(y)) {
            throw ScriptRuntime.constructError("addPageIndicator", "Bad argument(s)");
        }
		net.pierrox.lightning_launcher.data.PageIndicator pageIndicator = Utils.addPageIndicator(getPage(), x, y, 1, false);
		return (PageIndicator) measureAndRetrieveCachedItem(pageIndicator);
    }

    /**
     * Create a new custom view using default values.
     */
    public CustomView addCustomView(float x, float y) {
        if(Float.isNaN(x) || Float.isNaN(y)) {
            throw ScriptRuntime.constructError("addCustomView", "Bad argument(s)");
        }
		net.pierrox.lightning_launcher.data.CustomView customView = Utils.addCustomView(getPage(), x, y, 1);
		return (CustomView) measureAndRetrieveCachedItem(customView);
    }

	private Item measureAndRetrieveCachedItem(net.pierrox.lightning_launcher.data.Item item) {
		Item cachedItem = getCachedItem(item);

		ItemLayout il = cachedItem.mItemView.getParentItemLayout();
		if(il != null) {
			il.measure(View.MeasureSpec.makeMeasureSpec(il.getWidth(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(il.getHeight(), View.MeasureSpec.EXACTLY));
			il.layout(il.getLeft(), il.getTop(), il.getRight(), il.getBottom());
		}

		return cachedItem;
	}

    /**
     * Remove an item, without confirmation, whatever its type.
     * @param item object to remove
     */
    public void removeItem(Item item) {
        if(item != null) {
            getPage().removeItem(item.getItem(), false);
        }
    }

    /**
     * Clone an item, including containers if any, recursively.
     * Widgets are not fully cloned and need an user action to be fully restored.
     * The position of the newly created item is automatically chosen.
     * @param item object to clone
     * @return the newly created item.
     */
    public Item cloneItem(Item item) {
		net.pierrox.lightning_launcher.data.Item clone = Utils.cloneItem(item.getItem());
		return measureAndRetrieveCachedItem(clone);
    }

    /**
     * Move an item from this container to another container.
     * Warning: moving an item will change its identifier. Any reference to this item should be cleared, for instance this way:
     * <code>
     *     var item = container_a.getItemByName('some_item');
     *     item = container_a.moveItem(item, container_b);
     * </code>
     * Additionally both containers may be reloaded and references to other items in these container may become invalid too.
     * @param item the item to move
     * @param to_other_container the container in which to move the item
     * @return the new item
     */
    public Item moveItem(Item item, Container to_other_container) {
		if(to_other_container.mItemLayout == mItemLayout) {
			return item;
		}

		net.pierrox.lightning_launcher.data.Item movedItem = Utils.moveItem(item.getItem(), to_other_container.getPage(), Utils.POSITION_AUTO, Utils.POSITION_AUTO, 1, net.pierrox.lightning_launcher.data.Item.NO_ID);
		return measureAndRetrieveCachedItem(movedItem);
    }

	/**
	 * Used to identify the kind of this container.
	 * @return 'Desktop', or 'Container' for any other type of container (folder, panel).
	 */
	public String getType() {
		// obfuscation requires a clear text match
		if(getClass() == Desktop.class) {
			return "Desktop";
		} else {
			return "Container";
		}
	}
	
	/**
	 * Go to a specified absolute position in this container, setting a scale of 1 and using animations.
	 * @param x absolute X position, in pixel
	 * @param y absolute Y position, in pixel
	 */
	public void setPosition(float x, float y) {
        if(Float.isNaN(x) || Float.isNaN(y)) {
            throw ScriptRuntime.constructError("setPosition", "Bad argument(s)");
        }

		setPosition(x, y, 1, true);
	}

	/**
	 * Go to a specified absolute position in this container. Does nothing when the script is run in background.
	 * @param x absolute X position, in pixel
	 * @param y absolute Y position, in pixel
	 * @param scale zoom factor (1=100%, 0.5=50%, negative values are acceptable, 0 is not very useful)
	 * @param animate whether to animate the move
	 */
	public void setPosition(float x, float y, float scale, boolean animate) {
        if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(scale)) {
            throw ScriptRuntime.constructError("setPosition", "Bad argument(s)");
        }
		float to_x = -x * scale;
		float to_y = -y * scale;
		if(animate) {
			mItemLayout.animateZoomTo(to_x, to_y, scale);
		} else {
			mItemLayout.moveTo(to_x, to_y, scale);
		}
	}

	/**
	 * Returns the current absolute horizontal position. This function will return 0 if the script is run in background.
	 */
	public float getPositionX() {
        if(mItemLayout != null) {
            return -mItemLayout.getCurrentX() / mItemLayout.getCurrentScale();
        } else {
            return 0;
        }
	}

	/**
	 * Returns the current absolute vertical position. This function will return 0 if the script is run in background.
	 */
	public float getPositionY() {
        if(mItemLayout != null) {
            return -mItemLayout.getCurrentY() / mItemLayout.getCurrentScale();
        } else {
            return 0;
        }
	}

	/**
	 * Returns the current scale. This function will return 1 if the script is run in background.
	 */
	public float getPositionScale() {
        if(mItemLayout != null) {
            return mItemLayout.getCurrentScale();
        } else {
            return 1;
        }
	}

    /**
     * Stop inertial movement initiated by a fling. Does nothing when the script is run in background.
     */
    public void cancelFling() {
		mItemLayout.cancelFling();
    }

	/**
	 * Returns the width of this container. This function will return 0 if the script is run in background.
	 */
	public int getWidth() {
		return mItemLayout.getWidth();
	}

	/**
	 * Returns the height of this container. This function will return 0 if the script is run in background.
	 */
	public int getHeight() {
		return mItemLayout.getHeight();
	}


	/**
	 * Convert horizontal coordinates in this container into screen coordinates. This function will return y if the script is run in background.
	 */
	public float translateIntoScreenCoordX(float x) {
		float[] pos = mItemLayout.getScreen().translateItemLayoutCoordsIntoScreenCoords(mItemLayout, x, 0);
		return pos[0];
	}

	/**
	 * Convert vertical coordinates in this container into screen coordinates. This function will return y if the script is run in background.
	 */
	public float translateIntoScreenCoordY(float y) {
		float[] pos = mItemLayout.getScreen().translateItemLayoutCoordsIntoScreenCoords(mItemLayout, 0, y);
		return pos[1];
	}

	/**
	 * Retrieve container properties (configuration data).
	 * Please note that modifying properties for a container is particularly expensive and slow.
	 * @return an object to read and write properties
	 */
	public PropertySet getProperties() {
		if(mProperties == null) {
			mProperties = new PropertySet(mLightning, this);
		}
		return mProperties;
	}

	/**
	 * Move an item to another layer. This method will do nothing if the item cannot be found in this container.
	 * @param itemId item identifier as returned by {@link Item#getId()}
	 * @param index new index, starting at 0
	 */
	public void setItemZIndex(int itemId, int index) {
		Page page = getPage();
		int l = page.items.size();
		if(index<0) index = 0; else if(index>=l) index = l-1;
		for(int i=0; i<l; i++) {
			net.pierrox.lightning_launcher.data.Item item = page.items.get(i);
			if(item.getId() == itemId) {
                page.setItemZIndex(item, index);
				break;
			}
		}
	}

	/**
	 * Returns the Z-index of the item in the container.
	 * @param itemId item identifier as returned by {@link Item#getId()}
	 * @return the index or -1 if this item identifier is unknown in this container
	 */
	public int getItemZIndex(int itemId) {
		Page page = getPage();
		int l = page.items.size();
		for(int i=0; i<l; i++) {
			net.pierrox.lightning_launcher.data.Item item = page.items.get(i);
			if(item.getId() == itemId) {
				return i;
			}
		}
		
		return -1;
	}

	/**
	 * Returns the current computed cell width. It will return 0 if the script is run in background.
	 */
	public float getCellWidth() {
        if(mItemLayout != null) {
            return mItemLayout.getCellWidth();
        } else {
            return 0;
        }
	}

	/**
	 * Returns the current computed cell width. It will return 0 if the script is run in background.
	 */
	public float getCellHeight() {
        if(mItemLayout != null) {
            return mItemLayout.getCellHeight();
        } else {
            return 0;
        }
	}

	/**
	 * Returns the parent container for this container, or null if this container is a desktop.
	 */
	public Container getParent() {
		ItemView openerItemView = mItemLayout.getOpenerItemView();
		if(openerItemView != null) {
			return mLightning.getCachedContainer(openerItemView.getParentItemLayout());
		} else {
			return null;
		}
	}

	/**
	 * Returns the item used to open this container. This can either be a Folder or a Panel.
	 * @return an item, or null if this container has no opener, i.e. this is a desktop.
	 */
	public Item getOpener() {
		ItemView openerItemView = mItemLayout.getOpenerItemView();
		if(openerItemView != null) {
			return mLightning.getCachedItem(openerItemView);
		} else {
			return null;
		}
	}

	/**
	 * Returns the area covered by items in this container.
	 * This function will return an empty rect if the container has not been displayed yet or if the script is run in background.
	 */
	public RectL getBoundingBox() {
        if (mItemLayout == null) {
            return new RectL(0, 0, 0, 0);
        } else {
            return new RectL(mItemLayout.getItemsBoundingBox());
        }
    }

	/**
	 * Manually set a bounding box for this container.
	 * The bounding box is interpreted according to the "Fit desktop to items" option.
	 * @param bb a custom bounding box rect, use null to let the container compute the default bounding box.
	 */
	public void setBoundingBox(RectL bb) {
		if (mItemLayout != null) {
			Rect r = bb == null ? null : bb.toRect();
			mItemLayout.setBoundingBox(r);
		}
	}

	/**
	 * Update the items bounding box.
	 * This should be called after items have been moved or resized.
	 * Since it can be an expansive operation, it is best to call this method once for many item, not after every item geometry change (if possible).
	 * @since 12.8b1
	 */
	public void updateBoundingBox() {
		if(mItemLayout != null) {
			mItemLayout.updateBoundingBox();
		}
	}
	
	/**
	 * Set a custom and persistent data for this container.
     * Same as setTag(null, value)
	 * @see #setTag(String, String)
	 */ 
	public void setTag(String value) {
		Page page = getPage();
		page.config.tag = value;
        page.setModified();
	}
	
	/**
	 * Returns the value associated with this container.
     * Same as getTag(null)
	 * @see #getTag(String)
	 */
	public String getTag() {
        return getPage().config.tag;
	}

    /**
     * Set a custom and persistent data for this container.
     * Using a null id is the same as using {@link #setTag(String)} without the id argument.
     * @see Item#setTag(String, String)
     */
    public void setTag(String id, String value) {
        if(id == null) {
            setTag(value);
        } else {
			Page page = getPage();
			if (value == null) {
                if (page.config.tags != null) {
                    page.config.tags.remove(id);
                }
            } else {
                if (page.config.tags == null) {
                    page.config.tags = new HashMap<>(1);
                }
                page.config.tags.put(id, value);
            }
            page.setModified();
        }
    }

    /**
     * Returns the value associated with this container. Can be undefined if it has never been set.
     * Using a null id is the same as using {@link #getTag()} without argument.
     * @see Item#getTag(String)
     */
    public String getTag(String id) {
        if(id == null) {
            return getTag();
        } else {
			Page page = getPage();
			return page.config.tags == null ? null : page.config.tags.get(id);
        }
    }

	/**
	 * Retrieve the android View for this container. This function will return null if the script is run in background.
	 * @return an instance of ItemLayout or null if it is not available in this context
	 */
	public ViewGroup getView() {
		return mItemLayout;
	}

    /**
     * Display the Lightning "Add item" dialog.
     * This method will do nothing if the script is run in background.
     * The position of the item is automatically selected.
     * During the item selection process, the app can be stopped by the system. As a consequence the newly created item (if any) is not retrieved through this method, use the container "Item added" event instead.
     */
    public void showAddItemDialog() {
		mItemLayout.getScreen().showAddItemDialog(mItemLayout);
    }

    /**
     * Requests a manual application of the selected icon pack.
     *
     * The icon pack can either be configured from the settings screen or the "iconPack" container property. Setting the property "iconPack" will not automatically update icons for the container and this method need to be called afterwards.
     * Manually applying the icon pack can also be useful if shortcuts intents have been changed or if the icon pack has been updated.
     *
     * Important note: no container modification should occur during this operation. This will always be true when the method is called with the synchronous parameter set to true, but the app will be frozen. When this parameter is set to false,
     * the method will still return only when the operation is complete, but the user will be able to interact with the app in the meantime. Displaying a progress dialog will help in avoiding modifications but background operations/script executions can still take place.
     * @since 12.7b1
     *
     * @param synchronous allow the operation to be done synchronously or asynchronously. Asynchronous is unsafe due to risks of container concurrent modifications running in the background.
     */
    public void applyIconPack(boolean synchronous) {
		Page page = getPage();
		if(page.config.iconPack != null) {
			final LightningEngine engine = mLightning.getEngine();
			android.content.Context context = engine.getContext();
			if(synchronous) {
				IconPack.applyIconPackSync(context, page.config.iconPack, page, net.pierrox.lightning_launcher.data.Item.NO_ID);
            } else {
                Context cx = RhinoAndroidHelper.prepareContext();
                try {
                    final ContinuationPending pending = cx.captureContinuation();
                    IconPack.applyIconPackAsync(context, page.config.iconPack, page, net.pierrox.lightning_launcher.data.Item.NO_ID, new IconPack.IconPackListener() {
                        @Override
                        public void onPackApplied(boolean success) {
                            engine.getScriptExecutor().continuePendingContinuation(pending, success);
                        }
                    });
                    throw pending;
                } catch (IllegalStateException e) {
                    // not called with continuation support
                    IconPack.applyIconPackSync(context, page.config.iconPack, page, net.pierrox.lightning_launcher.data.Item.NO_ID);
                } finally {
                    Context.exit();
                }
            }
        }
    }

    /**
     * This method removes custom icons and layer images. It also reset the inner icon scale to 1. The purpose is to "cancel" the application of an icon pack.
     * @since 12.7b1
     */
    public void removeCustomIcons() {
        IconPack.removeCustomIcons(getPage());
    }

	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		return getType()+" "+ getPage().id;
	}
}
