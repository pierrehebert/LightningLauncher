package net.pierrox.lightning_launcher.script.api;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.configuration.ItemConfig;
import net.pierrox.lightning_launcher.data.*;
import net.pierrox.lightning_launcher.data.Box;
import net.pierrox.lightning_launcher.data.Shortcut;
import net.pierrox.lightning_launcher.script.api.screen.Screen;
import net.pierrox.lightning_launcher.views.IconLabelView;
import net.pierrox.lightning_launcher.views.ItemLayout;
import net.pierrox.lightning_launcher.views.item.ItemView;
import net.pierrox.lightning_launcher.views.item.ShortcutView;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;

/**
 * The item is the base class for other objects that can be found in a container (shortcuts, folders, etc.).
 * There are specific classes for some items (shortcut, folder, ...) when these items provides custom services. 
 * Some other item types don't have a specific class when they don't have more properties (such as widget for instance).
 *
 * Since Lightning V14 allows the same container to be displayed multiple times on the same screen, it is possible to retrieve
 * several Item objects linked with the same underlying data. The identifier (see {@link #getId()}) will then be the same.
 */
public class Item {
	
	protected Lightning mLightning;
	protected ItemView mItemView;
	private PropertySet mProperties;

	private Scriptable mMy;

	/**
	 * @hide
	 */
	public Item(Lightning lightning, ItemView itemView) {
		mLightning = lightning;
		mItemView = itemView;
	}

	/**
	 * Return a new script object, with this item as prototype
	 * @return a new script object which inherit from this item
	 */
	public Scriptable extend() {
		Scriptable scope = mLightning.getEngine().getScriptExecutor().getScriptScope();
		Scriptable object = Context.getCurrentContext().newObject(scope);
		object.setPrototype(new NativeJavaObject(scope, this, null));
		return object;
	}

	public Screen getScreen() {
		return mLightning.createScreen(mItemView.getParentItemLayout().getScreen());
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
	 * Use an accessor, don't keep this value as an instance field as it can change when the page is reloaded
	 * @hide
	 */
	public net.pierrox.lightning_launcher.data.Item getItem() {
		return mItemView.getItem();
	}

	/**
	 * Returns the unique item identifier.
	 * Each item has its own unique id in the app, meaning that it is not possible to find 2 items in 2 containers with the same id. However the item id will change if it is moved from one desktop to another, or moved in/out of a folder or panel. It may also change when loaded from a template.  
	 */
    public int getId() {
		return getItem().getId();
	}

	/**
	 * Returns the item type has a text 
	 * @return one of: Shortcut, Folder, Panel, Widget, StopPoint, DynamicText, Unlocker, PageIndicator or Unknown
	 */
	public String getType() {
		// needed because of obfuscation
		Class<?> cls = getItem().getClass();
		if(cls == net.pierrox.lightning_launcher.data.Shortcut.class) {
			return "Shortcut";
		} else if(cls == net.pierrox.lightning_launcher.data.Folder.class) {
			return "Folder";
		} else if(cls == net.pierrox.lightning_launcher.data.EmbeddedFolder.class) {
			return "Panel";
		} else if(cls == net.pierrox.lightning_launcher.data.Widget.class) {
			return "Widget";
		} else if(cls == net.pierrox.lightning_launcher.data.StopPoint.class) {
			return "StopPoint";
		} else if(cls == net.pierrox.lightning_launcher.data.DynamicText.class) {
			return "DynamicText";
		} else if(cls == net.pierrox.lightning_launcher.data.Unlocker.class) {
			return "Unlocker";
        } else if(cls == net.pierrox.lightning_launcher.data.PageIndicator.class) {
            return "PageIndicator";
        } else if(cls == net.pierrox.lightning_launcher.data.CustomView.class) {
            return "CustomView";
		} else {
			return "Unknown"; 
		}
	}
	
	/**
	 * Hide or show an item.
	 * @see #isVisible()
	 * @param visible true to make it visible
	 */
	public void setVisibility(boolean visible) {
		getItem().setVisible(visible);
	}
	
	/**
	 * Returns true if the item is visible.
	 * @see #setVisibility(boolean)
	 */
	public boolean isVisible() {
		return getItem().isVisible();
	}
	
	
	/**
	 * @hide
	 */
	public String getLabel() {
		net.pierrox.lightning_launcher.data.Item item = getItem();
		if(item instanceof Shortcut) {
            return ((Shortcut) item).getLabel();
        } else {
            return getName();
        }
	}
	
	/**
	 * @hide
	 */
	public void setLabel(String label) {
        if(getItem() instanceof Shortcut) {
            setLabel(label, false);
        } else {
            setName(label);
        }

	}
	
	/**
	 * @hide
	 */
	public void setLabel(String label, boolean persistent) {
		net.pierrox.lightning_launcher.data.Item item = getItem();
		if(item instanceof Shortcut) {
            Shortcut s = (Shortcut) item;

            if(persistent) {
                s.setLabel(label);
            } else {
				for (net.pierrox.lightning_launcher.engine.Screen screen : LLApp.get().getScreens()) {
					ItemView[] itemViews = screen.getItemViewsForItem(item);
					for (ItemView itemView : itemViews) {
						IconLabelView il = ((ShortcutView)itemView).getIconLabelView();
						TextView tv = il.getTextView();
						if(tv != null) {
							tv.setText(label);
						}
					}
				}
			}
        } else {
            setName(label);
        }
	}

    public String getName() {
        return getItem().getName();
    }

    public void setName(String name) {
		net.pierrox.lightning_launcher.data.Item item = getItem();
		item.setName(name);
    }

    private void itemGeometryUpdated(boolean fast) {
        getItem().getPage().setModified();
		for (net.pierrox.lightning_launcher.engine.Screen screen : LLApp.get().getScreens()) {
			ItemView[] itemViews = screen.getItemViewsForItem(getItem());
			for (ItemView itemView : itemViews) {
				ItemLayout il = itemView.getParentItemLayout();
				if (il != null) {
					if(fast) {
						il.layoutItemView(itemView);
					} else {
						il.requestLayout();
					}
				}
			}
		}
    }

	/**
	 * Set the position of the item (only when detached from the grid).
	 * Warning: any changes made through this method may be saved, meaning that the item position will have to be manually reset.
	 * @param x
	 * @param y
	 */
	public void setPosition(float x, float y) {
        if(Float.isNaN(x) || Float.isNaN(y)) {
            throw ScriptRuntime.constructError("setPosition", "Bad argument(s)");
        }

        getItem().updateGeometryValue(net.pierrox.lightning_launcher.data.Item.GEOMETRY_CTRL_POSITION, x, y, true);
        itemGeometryUpdated(true);
	}
	
	/**
	 * @return this item X position (only available when the script is not run in background).
	 */
	public float getPositionX() {
		return getPositionXOrY(true);
	}

	/**
	 * @return this item Y position (only available when the script is not run in background).
	 */
	public float getPositionY() {
		return getPositionXOrY(false);
	}

	private float getPositionXOrY(boolean is_x) {
		net.pierrox.lightning_launcher.data.Item item = getItem();
		if (item.getItemConfig().onGrid) {
            ItemLayout il = mItemView.getParentItemLayout();
			android.graphics.Rect r = item.getCell();
			return is_x ? r.left * il.getCellWidth() : r.top * il.getCellHeight();
        } else {
            RectF r = Utils.getTransformedItemBoxforMatrix(item, item.getTransform());
            return is_x ? r.left : r.top;
        }
	}
	
	/**
	 * Set the scale of the item (only when detached from the grid).
	 * Warning: any changes made through this method may be saved, meaning that the item scale will have to be manually reset.
	 * @param scaleX
	 * @param scaleY
	 */
	public void setScale(float scaleX, float scaleY) {
        if(Float.isNaN(scaleX) || Float.isNaN(scaleY)) {
            throw ScriptRuntime.constructError("setScale", "Bad argument(s)");
        }

        if(scaleX == 0 || scaleY == 0) {
            return;
        }

        getItem().updateGeometryValue(net.pierrox.lightning_launcher.data.Item.GEOMETRY_CTRL_SCALE, scaleX, scaleY, true);
        itemGeometryUpdated(true);
	}
	
	/**
	 * Returns the X scale for this item (only when detached from the grid).
	 */
	public float getScaleX() {
		return Utils.getScaleforMatrix(getItem().getTransform(), true);
	}
	
	/**
	 * Returns the Y scale for this item (only when detached from the grid).
	 */
	public float getScaleY() {
		return Utils.getScaleforMatrix(getItem().getTransform(), false);
	}
	
	/**
	 * Set the skew of the item (only when detached from the grid).
	 * Warning: any changes made through this method may be saved, meaning that the item skew will have to be manually reset.
	 * @param skewX
	 * @param skewY
	 */
	public void setSkew(float skewX, float skewY) {
        if(Float.isNaN(skewX) || Float.isNaN(skewY)) {
            throw ScriptRuntime.constructError("setSkew", "Bad argument(s)");
        }

        getItem().updateGeometryValue(net.pierrox.lightning_launcher.data.Item.GEOMETRY_CTRL_SKEW, skewX, skewY, true);
        itemGeometryUpdated(true);
	}
	
	/**
	 * Returns the X skew for this item (only when detached from the grid).
	 */
	public float getSkewX() {
		return Utils.getSkewforMatrix(getItem().getTransform(), true);
	}
	
	/**
	 * Returns the Y skew for this item (only when detached from the grid).
	 */
	public float getSkewY() {
		return Utils.getSkewforMatrix(getItem().getTransform(), false);
	}
	
	/**
	 * Set the size of the item (only when detached from the grid).
	 * Warning: any changes made through this method may be saved, meaning that the item size will have to be manually reset.
	 * @param width
	 * @param height
	 */
	public void setSize(float width, float height) {
        if(Float.isNaN(width) || Float.isNaN(height)) {
            throw ScriptRuntime.constructError("setSize", "Bad argument(s)");
        }

		if(getItem().getItemConfig().onGrid) return;

        getItem().updateGeometryValue(net.pierrox.lightning_launcher.data.Item.GEOMETRY_CTRL_SIZE, width, height, false);
        itemGeometryUpdated(false);
	}
	
	/**
	 * Returns this item view width.
	 */
	public int getWidth() {
		return getItem().getViewWidth();
	}
	
	/**
	 * Returns this item view height.
	 */
	public int getHeight() {
		return getItem().getViewHeight();
	}
	
	/**
	 * Set the rotation of the item around its center (only when detached from the grid).
	 * Warning: any changes made through this method may be saved, meaning that the item angle will have to be manually reset.
	 * @param angle in degrees
	 */
	public void setRotation(float angle) {
        if(Float.isNaN(angle)) {
            throw ScriptRuntime.constructError("setRotation", "Bad argument");
        }

		net.pierrox.lightning_launcher.data.Item item = getItem();
		if(item.getItemConfig().onGrid) return;

        item.updateGeometryValue(net.pierrox.lightning_launcher.data.Item.GEOMETRY_CTRL_ROTATE, angle, 0, true);
        itemGeometryUpdated(true);
	}

	/**
	 * Returns the rotation angle in degrees.
	 */
	public float getRotation() {
		return Utils.getRotateForMatrix(getItem().getTransform());
	}
	
	/**
	 * Set the cell allocated to the item on the grid (only works when attached to the grid).
     * These size and position are transcient and will be lost when the container is reloaded. Use the alternate setCell method to persist changes.
	 * @param left left column
	 * @param top top row
	 * @param right right column
	 * @param bottom bottom row
	 */
	public void setCell(int left, int top, int right, int bottom) {
		net.pierrox.lightning_launcher.data.Item item = getItem();
		if(!item.getItemConfig().onGrid) return;

		item.setCellT(new android.graphics.Rect(left, top, right, bottom));
        itemGeometryUpdated(false);
	}

    /**
     * Set the cell allocated to the item on the grid (only works when attached to the grid), persistently
     * @param left left column
     * @param top top row
     * @param right right column
     * @param bottom bottom row
     * @param portrait if true will set the portrait position (and landscape if not using dual position), if false will set the landscape position (only if using dual position)
     */
    public void setCell(int left, int top, int right, int bottom, boolean portrait) {
		net.pierrox.lightning_launcher.data.Item item = getItem();
		if(!item.getItemConfig().onGrid) return;

        android.graphics.Rect cell = portrait ? item.getCellP() : item.getCellL();
        cell.set(left, top, right, bottom);
        itemGeometryUpdated(false);
    }
	
	/**
	 * Returns the cell allocated to this item (only works when attached to the grid).
	 */
	public RectL getCell() {
		net.pierrox.lightning_launcher.data.Item item = getItem();
		if(item.getItemConfig().onGrid) {
			return new RectL(item.getCell());
		} else {
			return null;
		}
	}
	
	/**
	 * Returns the container containing this item.
	 */
	public Container getParent() {
		return mLightning.getCachedContainer(mItemView.getParentItemLayout());
	}

	/**
	 * Retrieve properties for this item. Please note that modifying properties is expensive and not well suited for item animation.
	 */
	public PropertySet getProperties() {
		if(mProperties == null) {
			mProperties = new PropertySet(mLightning, this);
		}
		return mProperties;
	}

    /**
     * Retrieve the image currently displayed as the box background.
     * This image can be shared amongst several items.
     * @param state one of "n" (normal), "s" (selected) or "f" (focused)
     * @return an image, or null if this is a nine patch or if there is no image
     */
    public Image getBoxBackground(String state) {
        Drawable d;
        File f;
		net.pierrox.lightning_launcher.data.Item item = getItem();
		File icon_dir = item.getPage().getIconDir();
        int id = item.getId();
        if (state.equals("s")) {
            d = item.getItemConfig().box.bgSelected;
            f = net.pierrox.lightning_launcher.data.Box.getBoxBackgroundSelected(icon_dir, id);
        } else if (state.equals("f")) {
            d = item.getItemConfig().box.bgFocused;
            f = net.pierrox.lightning_launcher.data.Box.getBoxBackgroundFocused(icon_dir, id);
        } else {
            d = item.getItemConfig().box.bgNormal;
            f = net.pierrox.lightning_launcher.data.Box.getBoxBackgroundNormal(icon_dir, id);
        }

		return Image.fromDrawable(mLightning, d, item, f);
    }

    /**
     * Set a background image, in memory only, changes will not be persisted and will be lost when the app is restarted.box
     * @param image
     * @param state a combination of "n" (normal), "s" (selected), "f" (focused)
     */
    public void setBoxBackground(Image image, String state) {
        Drawable d = image == null ? null : image.toDrawable();
		net.pierrox.lightning_launcher.data.Item item = getItem();
		ItemConfig ic = item.modifyItemConfig();
		Box box = ic.box;
		if(state.indexOf('n') != -1) box.bgNormal = d;
        if(state.indexOf('s') != -1) box.bgSelected = d;
        if(state.indexOf('f') != -1) box.bgFocused = d;
		for (net.pierrox.lightning_launcher.engine.Screen screen : LLApp.get().getScreens()) {
			ItemView[] itemViews = screen.getItemViewsForItem(item);
			for (ItemView itemView : itemViews) {
				if(itemView.isInitDone()) {
					itemView.getSensibleView().setBox(box);
				}
			}
		}
    }


    /**
     * Set a background image, optionally saving the change to file. Note that saving the image to file is much slower than setting it in memory only, hence this is not suitable for animations.
     * @param image
     * @param state a combination of "n" (normal), "s" (selected), "f" (focused)
     * @param persistent whether to save changes and affect only this item, when set to false the image will be reset upon the next launcher restart
     */
    public void setBoxBackground(Image image, String state, boolean persistent) {
		boolean stateN = state.indexOf('n') != -1;
		boolean stateS = state.indexOf('s') != -1;
		boolean stateF = state.indexOf('f') != -1;
		if(!stateN && !stateS && !stateF) {
			return;
		}

		if(persistent) {
			net.pierrox.lightning_launcher.data.Item item = getItem();
			Page page = item.getPage();
            File icon_dir = page.getIconDir();
            int id = item.getId();

			if (stateN) {
                copyImageToFile(image, Box.getBoxBackgroundNormal(icon_dir, id));
            }

			if (stateS) {
                copyImageToFile(image, Box.getBoxBackgroundSelected(icon_dir, id));
            }

			if (stateF) {
                copyImageToFile(image, Box.getBoxBackgroundFocused(icon_dir, id));
            }

			page.setModified();
        }
		setBoxBackground(image, state);
	}


    /**
     * @hide
     */
    protected void copyImageToFile(Image image, File f) {
        if(image == null) {
			f.delete();
		} else {
			// try direct file copy first
			boolean directFileCopy = false;
			File sourceFile = image.getSourceFile();
			if(!image.isModified() && sourceFile != null && sourceFile.exists()) {
				// directly copy the file
				Utils.copyFileSafe(null, sourceFile, f);
				directFileCopy = true;
			}

			if(!directFileCopy) {
				if (image.getClass() == ImageBitmap.class) {
					Bitmap bmp = ((ImageBitmap) image).getBitmap();
					FileOutputStream fos = null;
					try {
						f.getParentFile().mkdirs();
						fos = new FileOutputStream(f);
						bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
					} catch (IOException e) {
						f.delete();
					} finally {
						if (fos != null) try {
							fos.close();
						} catch (IOException e) {
						}
					}
				} else if (image.getClass() == ImageAnimation.class) {
					ImageAnimation animation = (ImageAnimation) image;
					if (animation.isModified()) {
						Utils.encodeAnimationToGif(f, animation.toDrawable());
					}
				}
			}
        }
    }

    /**
     * Set a persistent data for this item.
     * This is the same as setTag(null, value)
     * @see #setTag(String, String)
     */
	public void setTag(String value) {
		setTag(null, value);
	}

    /**
     * Returns the value associated with this item.
     * This is the same as getTag(null);
     * @see #getTag(String)
     */
	public String getTag() {
		return getTag(null);
	}

    /**
     * Set a persistent data for this item. This string value can be used to store non volatile data. Use JSON to store complex structures.
     * The data to store is associated with an identifier. This identifier is useful to reduce conflicts when multiple scripts need to use data for an item.
     * Using a null id is the same as calling {@link #setTag(String)} without the id argument.
     * Use setTag(id, null) to clear a tag.
     * Example of use:
     * <pre>
     *
     * // store some simple text
     * var name = prompt("What is your name ?", "your name here");
     * item.setTag("my_value", name);
     *
     * // later, retrieve this text and display it on a shortcut
     * var name = item.getTag("my_value");
     * other_item.setLabel(name);
     *
     * // store more than a string
     * var complex_data = new Object();
     * complex_data.date = new Date();
     * complex_data.x = 4;
     * complex_data.done = true;
     * item.setTag("my_value", JSON.stringify(complex_data));
     *
     * // load these data
     * var complex_data = JSON.parse(item.getTag("my_value"));
     * </pre>
     *
     * @param id identifier for this tag, valid characters are a-z, A-Z, 0-9 and _ (underscore). The identifier "_" is reserved and shouldn't be used.
     * @param value value to store
     */
    public void setTag(String id, String value) {
		net.pierrox.lightning_launcher.data.Item item = getItem();
		item.setTag(id, value);
    }

    /**
     * Returns the value associated with this item and id. Can be undefined if it has never been set.
     * Using a null id is the same as calling {@link #getTag()} without argument
     * @see #setTag(String, String)
     */
    public String getTag(String id) {
        return getItem().getTag(id);
    }

	/**
	 * @hide
	 * same as #getRootView but with clear internal type for the return value
	 */
	public ItemView getItemView() {
		return mItemView;
	}

    /**
     * Retrieve the root android View for this item.
     */
    public View getRootView() {
        return mItemView;
    }

    /**
     * Retrieve the list of configured bindings.
     * @return an array of bindings
	 * @deprecated use #getBindings instead
     */
    public Array getAllBindings() {
        return new Array(getBindings());
    }

	public Binding[] getBindings() {
		ItemConfig ic = getItem().getItemConfig();
		int length = ic.bindings==null ? 0 : ic.bindings.length;
		Binding[] bindings = new Binding[length];
		for(int i=0; i<length; i++) {
			bindings[i] = new Binding(ic.bindings[i]);
		}
		return bindings;
	}

    /**
     * Retrieve a binding by its key, the taget.
     * @return a binding object, or null if no binding found for this target
     */
    public Binding getBindingByTarget(String target) {
        ItemConfig ic = getItem().getItemConfig();
        int length = ic.bindings==null ? 0 : ic.bindings.length;
        for(int i=0; i<length; i++) {
            net.pierrox.lightning_launcher.engine.variable.Binding b = ic.bindings[i];
            if(b.target.equals(target)) {
                return new Binding(b);
            }
        }

        return null;
    }

    /**
     * Set a binding (same as #setBinding(String, String, boolean).
	 * It is forbidden to modify bindings in a script itself triggered by a binding.
     * @param binding
     * @see #setBinding(String, String, boolean)
     */
    public void setBinding(Binding binding) {
        setBinding(binding.getTarget(), binding.getFormula(), binding.isEnabled());
    }

    /**
     * Add or update a binding for this item.
     * It is forbidden to modify bindings in a script itself triggered by a binding.
     * The target name may be a property name, but not always. Although using any known property may work, only the list below is supported.
     * <br>Common item properties
     * <table>
     * 	<thead><tr><td>Name</td><td>Description</td></tr></thead>
     * 	<tbody>
     * 		<tr><td>i.alpha</td><td>Transparency</td></tr>
     * 		<tr><td>i.visibility</td><td>Visibility</td></tr>
     * 		<tr><td>i.enabled</td><td>Enabled</td></tr>
     * 		<tr><td>i.zindex</td><td>Z-Index</td></tr>
     * 		<tr><td>i.box</td><td>Box</td></tr>
     * 		<tr><td>v</td><td>Dummy target</td></tr>
     * 	</tbody>
     * </table>
     * <br>Item geometry, attached to the grid
     * <table>
     * 	<thead><tr><td>Name</td><td>Description</td></tr></thead>
     * 	<tbody>
     * 		<tr><td>c.l</td><td>Cell left</td></tr>
     * 		<tr><td>c.t</td><td>Cell top</td></tr>
     * 		<tr><td>c.r</td><td>Cell right</td></tr>
     * 		<tr><td>c.b</td><td>Cell bottom</td></tr>
     * 	</tbody>
     * </table>
     * <br>Item geometry, detached from the grid
     * <table>
     * 	<thead><tr><td>Name</td><td>Description</td></tr></thead>
     * 	<tbody>
     * 		<tr><td>t.x</td><td>Left (X)</td></tr>
     * 		<tr><td>t.y</td><td>Top (Y)</td></tr>
     * 		<tr><td>t.w</td><td>Width</td></tr>
     * 		<tr><td>t.h</td><td>Height</td></tr>
     * 		<tr><td>t.a</td><td>Angle</td></tr>
     * 		<tr><td>t.sx</td><td>Scale X</td></tr>
     * 		<tr><td>t.sy</td><td>Scale Y</td></tr>
     * 		<tr><td>t.kx</td><td>Skew X</td></tr>
     * 		<tr><td>t.ky</td><td>Skew Y</td></tr>
     * 	</tbody>
     * </table>
     * <br>Text properties
     * <table>
     * 	<thead><tr><td>Name</td><td>Description</td></tr></thead>
     * 	<tbody>
     * 		<tr><td>s.label</td><td>Label</td></tr>
     * 		<tr><td>s.labelVisibility</td><td>Label visibility</td></tr>
     * 		<tr><td>s.labelFontColor</td><td>Label color (normal state)</td></tr>
     * 		<tr><td>s.selectionColorLabel</td><td>Label color (selected state)</td></tr>
     * 		<tr><td>s.focusColorLabel</td><td>Label color (focused state)</td></tr>
     * 		<tr><td>s.labelFontSize</td><td>Label font size</td></tr>
     * 		<tr><td>s.labelFontStyle</td><td>Label font style</td></tr>
     * 	</tbody>
     * </table>
     * <br>Icon properties
     * <table>
     * 	<thead><tr><td>Name</td><td>Description</td></tr></thead>
     * 	<tbody>
     * 		<tr><td>s.iconVisibility</td><td>Icon visibility</td></tr>
     * 		<tr><td>s.iconScale</td><td>Icon scale</td></tr>
     * 		<tr><td>s.iconReflection</td><td>Icon reflection</td></tr>
     * 		<tr><td>s.iconColorFilter</td><td>Icon color filter</td></tr>
     * 	</tbody>
     * </table>
     * <br>Folder window properties
     * <table>
     * 	<thead><tr><td>Name</td><td>Description</td></tr></thead>
     * 	<tbody>
     * 		<tr><td>f.titleVisibility</td><td>Title visibility</td></tr>
     * 		<tr><td>f.titleFontColor</td><td>Title font color</td></tr>
     * 		<tr><td>f.titleFontSize</td><td>Title font size</td></tr>
     * 		<tr><td>f.wAH</td><td>Horizontal alignment</td></tr>
     * 		<tr><td>f.wAV</td><td>Vertical alignment</td></tr>
     * 		<tr><td>f.wX</td><td>Left</td></tr>
     * 		<tr><td>f.wY</td><td>Top</td></tr>
     * 		<tr><td>f.wW</td><td>Width</td></tr>
     * 		<tr><td>f.wH</td><td>Height</td></tr>
     * 		<tr><td>f.box</td><td>Box</td></tr>
     * 	</tbody>
     * </table>
     * @see #unsetBinding(String)
     *
     * @param target the item's property to change (no check is done on this parameter)
     * @param formula the value for the target, a constant value, a short or complex script
     * @param enabled whether this binding is active
     */
    public void setBinding(String target, String formula, boolean enabled) {
        if(target == null || formula == null) {
            mLightning.scriptError("neither target nor formula can be null");
            return;
        }

        ItemConfig ic = getItem().modifyItemConfig();
        boolean found = false;
        if(ic.bindings != null) {
            for (net.pierrox.lightning_launcher.engine.variable.Binding b : ic.bindings) {
                if (b.target.equals(target)) {
                    b.target = target;
                    b.formula = formula;
                    b.enabled = enabled;
                    found = true;
                    break;
                }
            }
        }

        if(!found) {
            int length = ic.bindings == null ? 0 : ic.bindings.length;
            net.pierrox.lightning_launcher.engine.variable.Binding[] bindings = new net.pierrox.lightning_launcher.engine.variable.Binding[length + 1];
            for(int i=0; i<length; i++) {
                bindings[i] = ic.bindings[i];
            }
            bindings[length] = new net.pierrox.lightning_launcher.engine.variable.Binding(target, formula, enabled);
            ic.bindings = bindings;
        }

		mLightning.getEngine().getVariableManager().updateBindings(mItemView, ic.bindings, true, mItemView.getParentItemLayout().getScreen(), true);
		getItem().getPage().setModified();
    }

    /**
     * Removes a binding for this item.
	 * It is forbidden to modify bindings in a script itself triggered by a binding.
     * @see #setBinding(String, String, boolean)
     * @param target target property for which to remove the binding
     */
    public void unsetBinding(String target) {
        ItemConfig ic = getItem().getItemConfig();
        if(ic.bindings == null) {
            return;
        }

        int length = ic.bindings.length;
        int i;
        for(i=0; i<length; i++) {
            if(ic.bindings[i].target.equals(target)) {
                break;
            }
        }

        if(i < length) {
            net.pierrox.lightning_launcher.engine.variable.Binding[] bindings = new net.pierrox.lightning_launcher.engine.variable.Binding[length - 1];
            int j;
            for(i=0, j=0; i<length; i++) {
                if(!ic.bindings[i].target.equals(target)) {
                    bindings[j++] = ic.bindings[i];
                }
            }
            ic.bindings = bindings;

            mLightning.getEngine().getVariableManager().updateBindings(mItemView, ic.bindings, true, mItemView.getParentItemLayout().getScreen(), true);
			getItem().getPage().setModified();
//            page.notifyItemChanged(mItem);
        }
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return getType()+" "+ getItem().getId();
	}
}
