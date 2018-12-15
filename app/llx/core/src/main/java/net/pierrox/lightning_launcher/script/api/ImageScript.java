package net.pierrox.lightning_launcher.script.api;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import net.pierrox.lightning_launcher.script.ScriptExecutor;
import net.pierrox.lightning_launcher.views.Graphics;
import net.pierrox.lightning_launcher.views.SharedAsyncGraphicsDrawable;

import org.mozilla.javascript.Scriptable;

/**
 * ImageScript is a way to draw images or animations without the need for intermediate bitmaps.
 * Such images are scalable and memory efficient.
 * Instances of ImageScript can be created with {@link LL#createImage(Scriptable, int, int)}.
 *
 * The Scriptable object must have a "draw" function, and optionally a "pause" and "resume" functions. These functions are called with a DrawingContext instance.
 * <br/><br/>
 * <b>Note</b>: <code>pause</code> and <code>resume</code> functions are called when the drawing is assigned to item icons only, not for backgrounds not icon layers. Animated backgrounds are currently not supported.
 * <br/><br/>
 * Sample 1 : draw a static image.
 * <code><pre>
 * var img = LL.createImage({
 *     draw: function(context) {
 *         var canvas = context.getCanvas();
 *         var w = context.getWidth();
 *         var h = context.getHeight();
 *         var cx = w/2;
 *         var cy = h/2;
 *
 *         var p = new Paint();
 *         p.setAntiAlias(true);
 *
 *         var step = 1;
 *         canvas.save();
 *         for(var angle = 0; angle < 180; angle += step) {
 *             p.setColor(Color.HSVToColor([angle*2, 1, 1]));
 *             canvas.rotate(step, cx, cy);
 *             canvas.drawLine(0, cy, w, cy, p);
 *         }
 *         canvas.restore();
 *     }
 * }, -1, -1);
 * </pre></code>
 * <br/><br/>
 * Sample 2 : this is a more complex script. It keeps tracks of timers per drawing context in order to create an animation that can be shared between several items
 * <code><pre>
 * var drawing = {
 *      draw: function(context) {
 *          var canvas = context.getCanvas();
 *          var w = context.getWidth();
 *          var h = context.getHeight();
 *          var cx = w/2;
 *          var cy = h/2;
 *
 *          var p = new Paint();
 *          p.setAntiAlias(true);
 *          p.setStrokeWidth(4);
 *
 *          var step = 4;
 *          canvas.save();
 *          for(var angle = 0; angle < 180; angle += step) {
 *              var hue = ((angle+drawing.shift)*2)%360
 *              p.setColor(Color.HSVToColor([hue, 1, 1]));
 *              canvas.rotate(step, cx, cy);
 *              canvas.drawLine(0, cy, w, cy, p);
 *          }
 *          canvas.restore();
 *      },
 *
 *      resume: function(context) {
 *          var animate = function() {
 *              context.invalidate();
 *              var id = ""+context.getId();
 *              drawing.shift++;
 *              drawing.timers[id] = setTimeout(animate, 100);
 *          };
 *          animate();
 *      },
 *
 *      pause: function(context) {
 *          var id = ""+context.getId();
 *          clearTimeout(drawing.timers[id]);
 *          delete drawing.timers[id];
 *      },
 *
 *      shift: 0,
 *      timers: {}
 * };
 *
 * var img = LL.createImage(drawing, -1, -1);
 * </pre></code>
 */
public class ImageScript extends Image {

    /**
     * The DrawingContext is the link between the drawing script and the drawing target.
     * The same script can be used to draw several images. For instance an ImageScript instance can be set for icon A, B and C, the drawing context then gives infos on the currently drawn icon.
     */
    public static class DrawingContext {
        private SharedAsyncGraphicsDrawable mDrawable;
        private Item mItem;
        private Canvas mCanvas;
        private int mWidth, mHeight;

        public DrawingContext(SharedAsyncGraphicsDrawable drawable, Item item) {
            mDrawable = drawable;
            mItem = item;
        }

        public void setDrawingInfo(Canvas canvas, int width, int height) {
            mCanvas = canvas;
            mWidth = width;
            mHeight = height;
        }

        /**
         * Return an unique identifier for this context
         */
        public int getId() {
            return mDrawable.hashCode();
        }

        /**
         * Return the item for which the drawing occurs.
         * Currently this method will only return a value when drawing shortcut icons (set through {@link Shortcut#setImage(Image)}), otherwise it will return null.
         */
        public Item getItem() {
            return mItem;
        }

        /**
         * Return a canvas on which to draw
         * @return a canvas or null if not currently ready for draw
         */
        public Canvas getCanvas() {
            return mCanvas;
        }

        /**
         * Return the drawing width
         * @return the width in pixel, or 0 if not currently ready for draw
         */
        public int getWidth() {
            return mWidth;
        }

        /**
         * Return the drawing height
         * @return the height in pixel, or 0 if not currently ready for draw
         */
        public int getHeight() {
            return mHeight;
        }

        public void invalidate() {
            mDrawable.invalidateSelf();
        }
    }

    private Scriptable mObject;
    private int mWidth;
    private int mHeight;

    /**
     * @hide
     */
    public ImageScript(Lightning lightning, Scriptable object, int width, int height) {
        super(lightning);

        mObject = object;
        mWidth = width;
        mHeight = height;
    }

    /**
     * Return the object containing functions used to handle the drawing operations.
     */
    public Scriptable getObject() {
        return mObject;
    }

    /**
     * @hide
     */
    /*package*/ ScriptExecutor getScriptExecutor() {
        return mLightning.getEngine().getScriptExecutor();
    }

    @Override
    public int getWidth() {
        return mWidth;
    }

    @Override
    public int getHeight() {
        return mHeight;
    }

    /**
     * Request this image to be drawn again, at some point in the future.
     */
    public void invalidate() {
        if(mSourceDrawable != null) {
            mSourceDrawable.invalidateSelf();
        }
    }

    /**
     * @hide
     */
    @Override
    public Drawable toDrawable() {
        SharedAsyncGraphicsDrawable drawable = new SharedAsyncGraphicsDrawable((Graphics) null, false);
        drawable.setScriptObject(getScriptExecutor(), mObject, mWidth, mHeight, null);
        return drawable;
    }
}
