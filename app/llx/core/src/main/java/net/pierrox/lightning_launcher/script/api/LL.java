package net.pierrox.lightning_launcher.script.api;

import android.content.Context;
import android.content.Intent;

import net.dinglisch.android.tasker.TaskerIntent;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.engine.Screen;
import net.pierrox.lightning_launcher.script.api.screen.ActivityScreen;
import net.pierrox.lightning_launcher.script.api.screen.HomeScreen;
import net.pierrox.lightning_launcher.views.ItemLayout;

import org.mozilla.javascript.Scriptable;

/**
 * The main object to access Lightning Launcher features.
 * The LL object gives access to desktops, items or give contextual data on the event.
 *
 * @deprecated use {@link Lightning} instead.
 */
public class LL {
    private Lightning mLightning;

    /**
     * @hide
     */
    public LL(Lightning lightning) {
        mLightning = lightning;
    }



    /**
	 * Returns the currently displayed desktop.
	 */
	public Desktop getCurrentDesktop() {
        int id = mLightning.getEngine().readCurrentPage(mLightning.getConfiguration().getHomeDesktopId());
        Screen screen = LLApp.get().getScreen(Screen.Identity.HOME);
        if(screen == null) {
            screen = LLApp.get().getScreen(Screen.Identity.BACKGROUND);
        }

        screen.loadRootItemLayout(id, false, false, true);
        ItemLayout[] ils = screen.getItemLayoutsForPage(id);
        return (Desktop) mLightning.getCachedContainer(ils[0]);
	}
	
	/**
	 * Returns the home desktop.
     * As of Lightning V14, this method looks for the desktop in the home screen, then in the background desktop.
	 */
	public Desktop getHomeDesktop() {
        int id = mLightning.getConfiguration().getHomeDesktopId();
        Screen screen = LLApp.get().getScreen(Screen.Identity.HOME);
        if(screen == null) {
            screen = LLApp.get().getScreen(Screen.Identity.BACKGROUND);
        }

        screen.loadRootItemLayout(id, false, false, true);
        ItemLayout[] ils = screen.getItemLayoutsForPage(id);
        return (Desktop) mLightning.getCachedContainer(ils[0]);
	}

	/**
	 * Returns the desktop used as the lock screen, or null if not set.
     * As of Lightning V14, this method looks for the desktop in the lock screen, then in the home screen, and finally in the background desktop.
	 */
	public Desktop getLockscreenDesktop() {
        int id = mLightning.getConfiguration().getLockscreenDesktopId();
        if(id == Page.NONE) {
            return null;
        }

        Screen screen = LLApp.get().getScreen(Screen.Identity.LOCK);
        if(screen == null) {
            screen = LLApp.get().getScreen(Screen.Identity.HOME);
            if(screen == null) {
                screen = LLApp.get().getScreen(Screen.Identity.BACKGROUND);
            }
        }

        screen.loadRootItemLayout(id, false, false, true);
        ItemLayout[] ils = screen.getItemLayoutsForPage(id);
        return (Desktop) mLightning.getCachedContainer(ils[0]);
	}

	/**
	 * Returns the desktop used as the floating desktop, or null if not set.
	 */
	public Desktop getFloatingDesktop() {
        int id = mLightning.getConfiguration().getFloatingDesktopId();
        if(id == Page.NONE) {
            return null;
        }

        Screen screen = LLApp.get().getScreen(Screen.Identity.FLOATING);
        if(screen == null) {
            return null;
        }


        return (Desktop) mLightning.getCachedContainer(screen.getCurrentRootItemLayout());
	}
	
	/**
	 * Returns an array of desktop identifiers.
	 */
	public Array getAllDesktops() {
		int[] so = mLightning.getConfiguration().getAllDesktops();
		int l = so.length;
		Integer[] ids = new Integer[l];
		for(int i=0; i<l; i++) {
			ids[i] = so[i];
		}
		return new Array(ids);
	}
	
	/**
	 * Returns a container by its id.
     *
     * @deprecated use {@link net.pierrox.lightning_launcher.script.api.screen.Screen#getContainerById(int)}
	 */
	public Container getContainerById(int id) {
        Screen screen;
        if(id == Page.APP_DRAWER_PAGE) {
            screen = LLApp.get().getScreen(Screen.Identity.APP_DRAWER);
            if(screen == null) {
                screen = LLApp.get().getScreen(Screen.Identity.BACKGROUND);
            }
            return mLightning.createScreen(screen).getContainerById(id);
        } else {
            return mLightning.getScriptScreen().getContainerById(id);
        }
	}
	
	/**
	 * Returns a desktop by its name, as set in the "Configure desktop" screen. The desktop name can be retrieved using {@link Desktop#getName()}.
	 * This method will return null if no desktop by that name can be found
	 * 
	 * @param name name of the desktop
	 */
	public Desktop getDesktopByName(String name) {
        Screen dashboardScreen = LLApp.get().getScreen(Screen.Identity.HOME);
        if(dashboardScreen != null) {
            return new HomeScreen(mLightning, dashboardScreen).getDesktopByName(name);
        } else {
            return null;
        }
	}
	
	/**
	 * Go to a specified desktop, without changing the current position in this desktop.
     *
	 * @param id desktop identifier
	 */
	public void goToDesktop(int id) {
		LLApp.get().displayPagerPage(id, false);
	}
	
	/**
	 * Go to a specified desktop and set the current absolute position in this desktop, setting a scale of 1 and using animations. This method does nothing when the script is run in background.
     *
	 * @param id desktop identifier
	 * @param x absolute X position, in pixel
	 * @param y absolute Y position, in pixel
	 */
	public void goToDesktopPosition(int id, float x, float y) {
		goToDesktopPosition(id, x, y, 1, true);
	}
	
	/**
	 * Go to a specified desktop and set the current absolute position in this desktop. This method does nothing when the script is run in background.
     *
	 * @param id desktop identifier
	 * @param x absolute X position, in pixel
	 * @param y absolute Y position, in pixel
	 * @param scale zoom factor (1=100%, 0.5=50%, negative values are acceptable, 0 is not very useful)
	 * @param animate whether to animate the move
	 */
	public void goToDesktopPosition(int id, float x, float y, float scale, boolean animate) {
        net.pierrox.lightning_launcher.script.api.screen.Screen screen = mLightning.getScriptScreen();
        screen.getScreen().goToDesktopPosition(id, -x * scale, -y * scale, scale, animate);
	}

	/**
	 * Returns the event object associated with this script execution.
	 * The event provides contextual data, such as the current desktop, which item has been tapped, and so on.
     *
     * @deprecated use {@link Lightning#getEvent_()}
	 */
	public Event getEvent() {
		return mLightning.findEventInStack();
	}

    private static final String EVENT_SOURCE_SCRIPT = "RUN_SCRIPT";

    /**
     * Same as #runAction(int,String) with a null data.
     */
    public void runAction(int action) {
        mLightning.getScriptScreen().runAction(action);
    }

    /**
     * Same as #runAction(int,Item,String) with a null item and data.
     */
    public void runAction(int action, String data) {
        mLightning.getScriptScreen().runAction(action, data);
    }

    /**
     * Run a Lightning action. This method does nothing when the script is run in background.
     *
     * @param action action code (one of the values defined in {@link net.pierrox.lightning_launcher.script.api.EventHandler}
     * @param item item to be used as the target (only useful with actions requiring an item)
     * @param data optional data to send to be used by the action, use null if none
     */
    public void runAction(int action, Item item, String data) {
        mLightning.getScriptScreen().runAction(action, item, data);
    }

    /**
     * Run another script.
     * Optional data can be transmitted to the called script and retrieved using {@link Event#getData()}.
     *
     * @param name name of the script as found in the script editor
     * @param data optional data to send to the script. Use JSON to pass more than a string.
     *
     * @deprecated use {@link net.pierrox.lightning_launcher.script.api.screen.Screen#runScript(String, String)}
     */
    public void runScript(final String name, final String data) {
        mLightning.getScriptScreen().runScript(name, data);
    }
    
    /**
     * @deprecated use Script#setTag(String) instead together with #getCurrentScript
     */
    public void setScriptTag(String tag) {
    	getCurrentScript().setTag(tag);
    }
    
    /**
     * @deprecated use Script#getTag() instead
     */
    public String getScriptTag() {
    	return getCurrentScript().getTag();
    }

    /**
     * Retrieve the currently executed script.
     */
    public Script getCurrentScript() {
        return mLightning.getCurrentScript();
    }

    /**
     * Retrieve a script by name
     *
     * @param name as given by Script#getName()
     * @return a script or null if not found
     */
    public Script getScriptByName(String name) {
        return mLightning.getScriptByName(name);
    }

    /**
     * Retrieve a script by its unique identifier
     *
     * @param id identifier as given by Script#getId()
     * @return a script or null if no script with this id
     */
    public Script getScriptById(String id) {
        return mLightning.getScriptById(id);
    }

    /**
     * Delete a script.
     */
    public void deleteScript(Script script) {
        mLightning.deleteScript(script);
    }

    /**
     * Create a new script. Use this API wisely.
     */
    public Script createScript(String name, String text, int flags) {
        return mLightning.createScript(name, text, flags);
    }

    /**
     * Return the collection of scripts matching some flags.
     * @param flags see Script#FLAG_*
     */
    public Array getAllScriptMatching(int flags) {
        Script[] array = mLightning.getAllScriptMatching(flags);
        return new Array(array);
    }

    /**
     * Unlock the screen.
     */
    public void unlock() {
        mLightning.unlock();
    }
    
    /**
     * Returns true if the screen is currently locked using the Lightning lock screen.
     */
    public boolean isLocked() {
    	return mLightning.isLocked();
    }
    
    /**
     * Write data to a file. This is for logging and debug purpose only. The path is not configurable and is: {@literal <}external storage{@literal >}/LightningLauncher/script/log.txt.
     * Please note that this method won't add newlines automatically when appending data.
     *
     * @param data text to write to the file
     * @param append whether to clear the file before to write data, or append data to the existing content 
     */
    public void writeToLogFile(String data, boolean append) {
    	mLightning.writeToLogFile(data, append);
    }
    
    /**
     * Returns whether the current context is paused. It often means that Lightning Launcher is not displayed, for instance because another app is running.
     * When the script is executed in the background this method always returns true.
     */
    public boolean isPaused() {
        boolean paused = true;
        for (Screen screen : LLApp.get().getScreens()) {
            if(!screen.isPaused()) {
                paused = false;
                break;
            }
        }
        return paused;
    }
    
    /**
     * Returns the list of currently open folders. This function returns the opener item, not the container itself.
     * This method will return null when the script is executed in the background.
     *
     * @return an Array of Folder items, sorted top to bottom (topmost folder is at index 0).
     */
    public Array getOpenFolders() {
        net.pierrox.lightning_launcher.script.api.screen.Screen screen = mLightning.getActiveScreen();
        if(screen != null) {
            return new Array(screen.getOpenFolders());
        } else {
            return new Array(new Folder[0]);
        }
    }
    
    /**
     * Returns an item by its id. This is a shortcut avoiding to traverse the list of all desktops and folders.
     *
     * @param id item identifier
     * @return an item, or null if this id is not known.
     *
     * @deprecated use {@link net.pierrox.lightning_launcher.script.api.screen.Screen#getItemById(int)}
     */
    public Item getItemById(int id) {
        return mLightning.getScriptScreen().getItemById(id);
    }

    /**
     * Create a blank new image of the specified size. Pixel format is always ARGB 8888.
     * Take care when creating images since it can rapidly exhaust memory and lead to a crash.
     *
     * @return can return null if not enough memory
     */
    public ImageBitmap createImage(int width, int height) {
        return Image.createImage(width, height);
    }

    /**
     * Create an image from the specified file.
     *
     * @param path path of the image
     * @return can return null if an image cannot be read from the file (not an image or not enough memory)
     */
    public Image createImage(String path) {
        return Image.createImage(path);
    }

    /**
     * Create an image from a package and a resource name.
     * For instance: <code>LL.createImage("net.pierrox.lightning_launcher_extreme", "icon")</code>
     * The density used is either the one given by ActivityManager.getLauncherLargeIconDensity if available, or the current one.
     *
     * @param pkg name of the package, use "android" to access system resources.
     * @param name name of the drawable resource
     * @return can return null if an image cannot be read from the package (unknown package, wrong resource name or not enough memory)
     */
    public Image createImage(String pkg, String name) {
        return Image.createImage(pkg, name);
    }

    /**
     * Create an image whose content is drawn using a script.
     * This can be used for memory efficient graphics and animations.
     * Please refer to {@link ImageScript} for the documentation on how to use it.
     *
     * @param object the set of functions
     * @param width the prefered image width, use -1 for as big as possible
     * @param height the prefered image height, use -1 for as big as possible
     */
    public ImageScript createImage(Scriptable object, int width, int height) {
        return Image.createImage(object, width, height);
    }

    /**
     * Create a blank animation: frames are created fully transparent and need to be drawn.
     * Notes: animations created this way are memory expensive and cannot be persisted (yet). This means that Shortcut.setCustomIcon() wont't work, but Shortcut.setImage() will.
     *
     * @param width image width
     * @param height image height
     * @param count number of frames to allocate
     * @param duration default frame duration
     * @param loopCount initial number of loops to play, use 0 for infinite
     * @return an animation or null in case of error (most likely out of memory)
     */
    public ImageAnimation createAnimation(int width, int height, int count, int duration, int loopCount) {
        return Image.createAnimation(width, height, count, duration, loopCount);
    }

    /**
     * This method is used to load a text from a package raw resource.
     * Instances of use of this method are:
     * <ul>
     *     <li>load scripts and set them as event handler (useful in script plugins)</li>
     *     <li>load JSON data, such as theme colors, data, etc.</li>
     * </ul>
     *
     * @param pkg package name from which to read resources
     * @param name name of the raw resource. It must not contain the extension of the raw file, this is the Android identifier.
     * @return a string or null if the resource cannot be found or read
     */
    public String loadRawResource(String pkg, String name) {
        return mLightning.loadRawResource(pkg, name);
    }

    /**
     * Persist launcher data now.
     */
    public void save() {
        mLightning.save();
    }

    /**
     * Start an activity.
     * Example:<code><pre>
     * var intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.pierrox.net/")
     * LL.startActivity(intent);</pre></code>
     *
     * @param intent intent to start the activity
     * @return true if launch is successful, false if activity not found or permission denied
     */
    public boolean startActivity(Intent intent) {
        net.pierrox.lightning_launcher.script.api.screen.Screen screen = mLightning.getActiveScreen();
        return screen.startActivity(intent);
    }

    /**
     * Start an activity and handle the result in another script. This is similar to http://developer.android.com/reference/android/app/Activity.html#onActivityResult%28int,%20int,%20android.content.Intent%29
     * This call is asynchronous and the script will continue its execution. Once the started activity ends, the "receiver" script is called with the following parameters set:
     * <ul>
     *     <li>resultCode: Integer, the activity result, most often Activity.RESULT_OK or Activity.RESULT_CANCELED</li>
     *     <li>data: Intent, data provided by the activity, if any</li>
     *     <li>token: String, optional value passed as third argument in startActivityForResult</li>
     * </ul>
     * This method cannot be called when the executing script is run in the background. In that case it will do nothing and return false
     *
     * @param intent intent to start the activity
     * @param receiver a script to execute upon activity end
     * @param token an optional string data that you can transmit to the receiver script
     * @return true if the activity has been started
     */
    public boolean startActivityForResult(Intent intent, Script receiver, String token) {
        net.pierrox.lightning_launcher.script.api.screen.Screen screen = mLightning.getActiveScreen();
        if(screen instanceof ActivityScreen) {
            return ((ActivityScreen)screen).startActivityForResult(intent, receiver, token);
        } else {
            return false;
        }
    }

    /**
     * Send a tasker intent, optionally waiting for its completion to return.
     *
     * @param intent an intent built with TaskerIntent (see http://tasker.dinglisch.net/invoketasks.html for samples)
     * @param synchronous when true, Lightning will wait for Tasker task completion before to return, otherwise it will return immediately
     * @return when synchronous is true returns true if the intent has been sent successfully and Tasker reports a success too, when synchronous is false this method always returns true..
     */
    public boolean sendTaskerIntent(TaskerIntent intent, boolean synchronous) {
        return mLightning.sendTaskerIntent(intent, synchronous);
    }

    /**
     * Return the Android Context this script is linked with (an activity context).
     * This is meant to be used with Android services.
     */
    public Context getContext() {
        Screen screen = LLApp.get().getActiveScreen();
        if(screen == null) {
            return LLApp.get();
        } else {
            return screen.getContext();
        }
    }

    /**
     * Translate a Java class into a JavaScript object.
     * This is a convenience method that avoid repeated use of fully qualified names while scripting Java.
     *
     * @param name fully qualified class name
     * @return true if the operation succeeded, false if the class cannot be loaded or if already bound
     */
    public boolean bindClass(String name) {
        return mLightning.getEngine().getScriptExecutor().bindClass(name);
    }

    /**
     * Request the user to pick a color.
     *
     * @param title text displayed at the top of the dialog
     * @param color initial color to display
     * @param hasAlpha whether to display the transparency slider. It will not enforce a fully opaque color, it only acts on the slider visibility.
     * @return either the selected color, or undefined if the dialog has been canceled
     */
    public int pickColor(String title, int color, boolean hasAlpha) {
        net.pierrox.lightning_launcher.script.api.screen.Screen screen = mLightning.getActiveScreen();
        if(screen instanceof ActivityScreen) {
            return ((ActivityScreen)screen).pickColor(title, color, hasAlpha);
        } else {
            return 0;
        }
    }

    /**
     * Request the user to enter a numeric value.
     * When using the "%" unit, valueType must be set to FLOAT: the dialog will scale a decimal value so that 0.75 is displayed as 75%.
     * Warning: the returned value may be subject to rounding errors.
     *
     * @param title text displayed at the top of the dialog
     * @param value initial value to display
     * @param valueType either INT or FLOAT. It will default to FLOAT if valueType is not a known value.
     * @param min minimum value for the slider
     * @param max maximum value for the slider
     * @param interval interval between values when sliding
     * @param unit text to display after the value (for instance "%" or "px"). When using "%" with FLOAT, scale value by 100
     * @return either the selected value, or undefined if the dialog has been canceled
     */
    public float pickNumericValue(String title, float value, String valueType, float min, float max, float interval, String unit) {
        net.pierrox.lightning_launcher.script.api.screen.Screen screen = mLightning.getActiveScreen();
        if(screen instanceof ActivityScreen) {
            return ((ActivityScreen)screen).pickNumericValue(title, value, valueType, min, max, interval, unit);
        } else {
            return 0;
        }
    }

    /**
     * Request the user to pick an image through the Lightning image picker screen.
     * Warning: images returned by this function may be very large, take care at memory use as exceeding limits will make the launcher crash.
     * Use the maxPixels parameter: the image will be scaled by a power of two so that its number of pixels is below or equal.
     * This function supports picking bitmaps and nine patches.
     *
     * @param maxPixels maximum number of pixels in the returned image (width x height), 0 for no limit, 1048576 is one mega pixels (1024 x 1024)
     * @return an {@link net.pierrox.lightning_launcher.script.api.Image}, or null if the user canceled the operation, or if the image cannot be loaded
     */
    public Image pickImage(int maxPixels) {
        net.pierrox.lightning_launcher.script.api.screen.Screen screen = mLightning.getActiveScreen();
        if(screen instanceof ActivityScreen) {
            return ((ActivityScreen)screen).pickImage(maxPixels);
        } else {
            return null;
        }
    }

    /**
     * Request the user to select an area in the image.
     * Warning: because images need to be persisted to file while cropping, this method may be slow.
     *
     * @param image image to be cropped
     * @param full_size handling big images can be slow or request too much memory, by setting full_size to false this will allow this method to downscale images (approx. the size of the screen)
     * @return a cropped image, or null if the operation failed or the user canceled it
     */
    public ImageBitmap cropImage(ImageBitmap image, boolean full_size) {
        net.pierrox.lightning_launcher.script.api.screen.Screen screen = mLightning.getActiveScreen();
        if(screen instanceof ActivityScreen) {
            return ((ActivityScreen)screen).cropImage(image, full_size);
        } else {
            return null;
        }
    }

    /**
     * Set a boolean variable. This is a shortcut for <code>LL.getVariables().edit().setBoolean(name, value).commit();</code>. When modifying several at once, consider using the {@link net.pierrox.lightning_launcher.script.api.PropertyEditor} object instead for best efficiency.
     */
    public void setVariableBoolean(String name, boolean value) {
        mLightning.setVariableBoolean(name, value);
    }

    /**
     * Set a boolean variable. This is a shortcut for <code>LL.getVariables().edit().setInteger(name, value).commit();</code>. When modifying several at once, consider using the {@link net.pierrox.lightning_launcher.script.api.PropertyEditor} object instead for best efficiency.
     */
    public void setVariableInteger(String name, long value) {
        mLightning.setVariableInteger(name, (int)value);
    }

    /**
     * Set a boolean variable. This is a shortcut for <code>LL.getVariables().edit().setFloat(name, value).commit();</code>. When modifying several at once, consider using the {@link net.pierrox.lightning_launcher.script.api.PropertyEditor} object instead for best efficiency.
     */
    public void setVariableFloat(String name, float value) {
        mLightning.setVariableFloat(name, value);
    }

    /**
     * Set a string variable. This is a shortcut for <code>LL.getVariables().edit().setString(name, value).commit();</code>. When modifying several at once, consider using the {@link net.pierrox.lightning_launcher.script.api.PropertyEditor} object instead for best efficiency.
     */
    public void setVariableString(String name, String value) {
        mLightning.setVariableString(name, value);
    }

    /**
     * Retrieve the whole set of known variables (builtins and user ones).
     */
    public VariableSet getVariables() {
        return mLightning.getVariables();
    }
}
