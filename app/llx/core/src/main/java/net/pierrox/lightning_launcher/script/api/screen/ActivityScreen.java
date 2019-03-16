package net.pierrox.lightning_launcher.script.api.screen;


import android.content.Intent;

import com.faendir.rhino_android.RhinoAndroidHelper;

import net.pierrox.lightning_launcher.script.api.Image;
import net.pierrox.lightning_launcher.script.api.ImageBitmap;
import net.pierrox.lightning_launcher.script.api.Lightning;
import net.pierrox.lightning_launcher.script.api.Script;

import org.mozilla.javascript.ContinuationPending;
import org.mozilla.javascript.Function;

/**
 * A screen backed with an Android Activity.
 *
 * An instance of this object can be retrieved with any function that returns an {@link Screen} when that returned Screen is an ActivityScreen; or with {@link Lightning#getAppDrawerScreen()} or {@link Lightning#getLockScreen()}.
 */
public class ActivityScreen extends Screen {
    /**
     * @hide
     */
    public ActivityScreen(Lightning lightning, net.pierrox.lightning_launcher.engine.Screen screen) {
        super(lightning, screen);
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
     * @param intent intent to start the activity
     * @param receiver a script to execute upon activity end
     * @param token an optional string data that you can transmit to the receiver script
     * @return true if the activity has been started
     */
    public boolean startActivityForResult(Intent intent, Script receiver, String token) {
        return mScreen.startActivityForResultScript(mLightning.getEngine().getScriptExecutor(), intent, receiver.getId(), token);
    }

    /**
     * Request the user to pick a color.
     * @param title text displayed at the top of the dialog
     * @param color initial color to display
     * @param hasAlpha whether to display the transparency slider. It will not enforce a fully opaque color, it only acts on the slider visibility.
     * @return either the selected color, or undefined if the dialog has been canceled
     */
    public int pickColor(String title, int color, boolean hasAlpha) {
        org.mozilla.javascript.Context cx = RhinoAndroidHelper.prepareContext();
        try {
            ContinuationPending pending = cx.captureContinuation();
            mLightning.getEngine().getScriptExecutor().displayScriptPickColorDialog(mScreen, title, color, hasAlpha, pending);
            throw pending;
        } catch (IllegalStateException e) {
            // not called with continuation support
            android.widget.Toast.makeText(getContext(), "cannot display color picker in this context", android.widget.Toast.LENGTH_SHORT).show();
            return color;
        } finally {
            org.mozilla.javascript.Context.exit();
        }
    }

    /**
     * Request the user to enter a numeric value.
     * When using the "%" unit, valueType must be set to FLOAT: the dialog will scale a decimal value so that 0.75 is displayed as 75%.
     * Warning: the returned value may be subject to rounding errors.
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
        boolean is_float = !"INT".equals(valueType);

        org.mozilla.javascript.Context cx = RhinoAndroidHelper.prepareContext();
        try {
            ContinuationPending pending = cx.captureContinuation();
            mLightning.getEngine().getScriptExecutor().displayScriptPickValueDialog(mScreen, title, value, is_float, min, max, interval, unit, pending);
            throw pending;
        } catch (IllegalStateException e) {
            // not called with continuation support
            android.widget.Toast.makeText(getContext(), "cannot display value picker in this context", android.widget.Toast.LENGTH_SHORT).show();
            return value;
        } finally {
            org.mozilla.javascript.Context.exit();
        }
    }

    /**
     * Request the user to pick an image through the Lightning image picker screen.
     * Warning: images returned by this function may be very large, take care at memory use as exceeding limits will make the launcher crash.
     * Use the maxPixels parameter: the image will be scaled by a power of two so that its number of pixels is below or equal.
     * This function supports picking bitmaps and nine patches.
     * @param maxPixels maximum number of pixels in the returned image (width x height), 0 for no limit, 1048576 is one mega pixels (1024 x 1024)
     * @return an {@link net.pierrox.lightning_launcher.script.api.Image}, or null if the user canceled the operation, or if the image cannot be loaded
     */
    public Image pickImage(int maxPixels) {
        org.mozilla.javascript.Context cx = RhinoAndroidHelper.prepareContext();
        try {
            ContinuationPending pending = cx.captureContinuation();
            mLightning.getEngine().getScriptExecutor().displayScriptPickImageDialog(mScreen, maxPixels, pending);
            throw pending;
        } catch (IllegalStateException e) {
            // not called with continuation support
            android.widget.Toast.makeText(getContext(), "cannot display image picker in this context", android.widget.Toast.LENGTH_SHORT).show();
            return null;
        } finally {
            org.mozilla.javascript.Context.exit();
        }
    }

    /**
     * Request the user to select an area in the image.
     * Warning: because images need to be persisted to file while cropping, this method may be slow.
     * @param image image to be cropped
     * @param full_size handling big images can be slow or request too much memory, by setting full_size to false this will allow this method to downscale images (approx. the size of the screen)
     * @return a cropped image, or null if the operation failed or the user canceled it
     */
    public ImageBitmap cropImage(ImageBitmap image, boolean full_size) {
        org.mozilla.javascript.Context cx = RhinoAndroidHelper.prepareContext();
        try {
            ContinuationPending pending = cx.captureContinuation();
            mLightning.getEngine().getScriptExecutor().displayScriptCropImageDialog(mScreen, image, full_size, pending);
            throw pending;
        } catch (IllegalStateException e) {
            // not called with continuation support
            android.widget.Toast.makeText(getContext(), "cannot display image cropper in this context", android.widget.Toast.LENGTH_SHORT).show();
            return null;
        } finally {
            org.mozilla.javascript.Context.exit();
        }
    }

    /**
     * Display the Android action bar. Supported on Android 5.0+.
     * When used in the app drawer, the android action bar will replace the current action bar.
     * The action bar is not compatible with the system bar overlap option, which will be temporarily disabled.
     * Sample script: {@link http://www.lightninglauncher.com/wiki/doku.php?id=script_action_bar_sample}
     * @param onCreateOptionsMenu see {@link https://developer.android.com/reference/android/app/Activity.html#onCreateOptionsMenu(android.view.Menu)}
     * @param onOptionsItemSelected see {@link https://developer.android.com/reference/android/app/Activity.html#onOptionsItemSelected(android.view.MenuItem)}
     */
    public void showActionBar(Function onCreateOptionsMenu, Function onOptionsItemSelected) {
        mScreen.showAndroidActionBar(onCreateOptionsMenu, onOptionsItemSelected);
    }

    /**
     * Hide the android action bar.
     */
    public void hideActionBar() {
        mScreen.hideAndroidActionBar();
    }
}
