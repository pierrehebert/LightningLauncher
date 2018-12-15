package net.pierrox.lightning_launcher.views;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.NinePatch;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Pair;

import net.pierrox.lightning_launcher.script.ScriptExecutor;
import net.pierrox.lightning_launcher.script.api.ImageScript;
import net.pierrox.lightning_launcher.script.api.Item;
import net.pierrox.lightning_launcher.util.AnimationDecoder;
import net.pierrox.lightning_launcher.views.svg.SvgDrawable;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;

public class SharedAsyncGraphicsDrawable extends Drawable implements Drawable.Callback {

    public interface GraphicsProvider {
        Graphics provideGraphics(SharedAsyncGraphicsDrawable sbd, Object data, int max_width, int max_height);
        boolean composeGraphics(Bitmap baseIcon, Bitmap finalIcon);
    }

    public interface SharedAsyncGraphicsDrawableListener {
        void onSharedAsyncGraphicsDrawableInvalidated(SharedAsyncGraphicsDrawable drawable);
        void onSharedAsyncGraphicsDrawableSizeChanged(SharedAsyncGraphicsDrawable drawable);
    }

    private static GraphicsProviderRunner sGraphicsProviderRunner;
    private static CachedBitmapPool sCachedBitmapPool;

    private final int mId;
    private GraphicsProvider mGraphicsProvider;
    private Object mGraphicsProviderData;
    private boolean mLoadingGraphics;

    public static final int TYPE_NOT_YET_KNOWN = 0;
    public static final int TYPE_NONE = 1;
    public static final int TYPE_BITMAP = 2;
    public static final int TYPE_NINE_PATCH = 3;
    public static final int TYPE_ANIMATED_GIF = 4;
    public static final int TYPE_SCRIPT = 5;
    public static final int TYPE_SVG = 6;

    private int mType = TYPE_NOT_YET_KNOWN;
    private CachedBitmap mCachedBitmap;
    private MyNinePatchDrawable mNinePatchDrawable;
    private SvgDrawable mSvgDrawable;
    private AnimationDecoder mAnimationDecoder;
    private Bitmap mGifBitmap;
    private Handler mHandler;
    private Runnable mAnimationRunnable;
    private long mGifStartTime = 0;
    private int mPreviousGifIndex = -1;
    private int mGifLoop = -1;
    private boolean mPlaying;

    private ScriptExecutor mScriptExecutor;
    private Scriptable mScriptObject;
    private ImageScript.DrawingContext mScriptDrawingContext;

    private int mMaxWidthHint;
    private int mMaxHeightHint;
    private int mResumeCount;

    private Paint mPaint;
    private int mIntrinsicWidth;
    private int mIntrinsicHeight;

    private ArrayList<SharedAsyncGraphicsDrawableListener> mListeners;

    public static void setPoolSize(long size) {
        sCachedBitmapPool = new CachedBitmapPool(size);
    }

    private SharedAsyncGraphicsDrawable(boolean filter) {
        if(sGraphicsProviderRunner == null) {
            sGraphicsProviderRunner = new GraphicsProviderRunner();
            sGraphicsProviderRunner.start();
        }

        mId = hashCode();
        mPaint = new Paint();
        mPaint.setFilterBitmap(filter);
    }

    public SharedAsyncGraphicsDrawable(Bitmap from, boolean filter) {
        this(filter);
        setBitmapInternal(from);
    }

    public SharedAsyncGraphicsDrawable(Graphics from, boolean filter) {
        this(filter);
        setGraphicsInternal(from);
    }

    public SharedAsyncGraphicsDrawable(GraphicsProvider bitmap_loader_provider, Object data, boolean filter) {
        this(filter);
        mGraphicsProvider = bitmap_loader_provider;
        mGraphicsProviderData = data;
    }

    public void registerListener(SharedAsyncGraphicsDrawableListener listener) {
        if(mListeners == null) {
            mListeners = new ArrayList<>(1);
        }
        if(mListeners.size() == 0 && mType == TYPE_ANIMATED_GIF) {
            configureGifHandlerRunnable();
        }

        mListeners.add(listener);
    }

    public void unregisterListener(SharedAsyncGraphicsDrawableListener listener) {
        mListeners.remove(listener);
        if(mListeners.size() == 0 && mType == TYPE_ANIMATED_GIF) {
            mHandler.removeCallbacks(mAnimationRunnable);
            mPlaying = true;
        }
    }

    public int getType() {
        return mType;
    }

    public Paint getPaint() {
        return mType == TYPE_NINE_PATCH ? mNinePatchDrawable.getPaint() : mPaint;
    }

    @Override
    public void setFilterBitmap(boolean filter) {
        mPaint.setFilterBitmap(filter);
    }

    public void pause() {
        mResumeCount--;
        if(mResumeCount == 0) {
            if (mAnimationRunnable != null) {
                mHandler.removeCallbacks(mAnimationRunnable);
                mPlaying = false;
            } else if (mType == TYPE_SCRIPT) {
                pauseScript();
            }
        }
    }

    private void pauseScript() {
        Object pause = mScriptObject.get("pause", mScriptObject);
        if(pause instanceof Function) {
            mScriptExecutor.runFunction((Function) pause, new Object[]{mScriptDrawingContext}, false, true);
        }
    }

    public void resume() {
        mResumeCount++;
        if(mResumeCount == 1) {
            if (mAnimationRunnable != null) {
                mAnimationRunnable.run();
            } else if (mType == TYPE_SCRIPT) {
                resumeScript();
            }
        }
    }

    private void resumeScript() {
        Object resume = mScriptObject.get("resume", mScriptObject);
        if(resume instanceof Function) {
            mScriptExecutor.runFunction((Function)resume, new Object[]{mScriptDrawingContext}, false, true);
        }
    }

    public void setMaxSizeHint(int w, int h) {
        mMaxWidthHint = w;
        mMaxHeightHint = h;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (mType == TYPE_NONE || bounds.isEmpty()) {
            return;
        }

        if(mType == TYPE_NOT_YET_KNOWN) {
            if (!mLoadingGraphics) {
                loadGraphicsSync();
            } else {
                sGraphicsProviderRunner.prioritize(this);
            }
        }

        switch (mType) {
            case TYPE_BITMAP:
                ensureBitmapReadyToDraw();
                Bitmap bitmap = mCachedBitmap.bitmap;
                bitmap = mCachedBitmap.shared ? bitmap.copy(bitmap.getConfig(), false) : bitmap;
                canvas.drawBitmap(bitmap, null, bounds, mPaint);
                break;

            case TYPE_NINE_PATCH:
                mNinePatchDrawable.draw(canvas);
                break;

            case TYPE_SVG:
                mSvgDrawable.draw(canvas);
                break;

            case TYPE_ANIMATED_GIF:
                int i = 0;
                if(mAnimationRunnable != null) {
                    long relTime = (SystemClock.uptimeMillis() - mGifStartTime) % mAnimationDecoder.getTotalDuration();
                    int delay = 0;
                    int count = mAnimationDecoder.getFrameCount();
                    int maxGifLoop = mAnimationDecoder.getLoopCount();
                    if (maxGifLoop > 0 && mGifLoop >= maxGifLoop) {
                        i = count - 1;
                    } else {
                        for (; i < count; i++) {
                            delay += mAnimationDecoder.getDelay(i);
                            if (delay > relTime) break;
                        }
                    }
                }
                Bitmap baseBitmap = mAnimationDecoder.getFrame(i);
                boolean composed;
                if(mGraphicsProvider != null) {
                    mGifBitmap.eraseColor(0);
                    composed = mGraphicsProvider.composeGraphics(baseBitmap, mGifBitmap);
                } else {
                    composed = false;
                }
                canvas.drawBitmap(composed ? mGifBitmap : baseBitmap, null, bounds, mPaint);
                break;

            case TYPE_SCRIPT:
                Object draw = mScriptObject.get("draw", mScriptObject);
                if(draw instanceof Function) {
                    mScriptDrawingContext.setDrawingInfo(canvas, bounds.width(), bounds.height());
                    mScriptExecutor.runFunction((Function)draw, new Object[]{mScriptDrawingContext}, false, true);
                    mScriptDrawingContext.setDrawingInfo(null, 0, 0);
                }
                break;

            case TYPE_NONE:
                // nothing to do
                break;
        }
    }

    @Override
    public void invalidateSelf() {
        if(mListeners != null) {
            for (SharedAsyncGraphicsDrawableListener listener : mListeners) {
                listener.onSharedAsyncGraphicsDrawableInvalidated(this);
            }
        }
    }

    @Override
    public void setAlpha(int alpha) {
        // pass
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        // pass
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        NativeImage.deleteImage(mId);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        if(mNinePatchDrawable != null) {
            mNinePatchDrawable.setBounds(bounds);
        } else if(mSvgDrawable != null) {
            mSvgDrawable.setBounds(bounds);
        }
        if(mMaxWidthHint == 0) mMaxWidthHint = bounds.width();
        if(mMaxHeightHint == 0) mMaxHeightHint = bounds.height();
    }

    @Override
    public int getIntrinsicWidth() {
        return mIntrinsicWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return mIntrinsicHeight;
    }

    public boolean needToLoadGraphics() {
        return mType == TYPE_NOT_YET_KNOWN && !mLoadingGraphics;
    }

    public void loadGraphicsSync() {
        Graphics graphics = getGraphicsFromProvider();
        setGraphicsInternal(graphics);
        mLoadingGraphics = false;
    }

    public void loadGraphicsAsync() {
        if(!mLoadingGraphics && mGraphicsProvider != null) {
            mLoadingGraphics = true;
            sGraphicsProviderRunner.processEntry(this);
        }
    }

    public Bitmap getBitmap() {
        if(mType == TYPE_NOT_YET_KNOWN || mType == TYPE_BITMAP) {
            if(mCachedBitmap != null) {
                ensureBitmapReadyToDraw();
                return mCachedBitmap.bitmap;
            } else {
                loadGraphicsSync();
                return mCachedBitmap == null ? null : mCachedBitmap.bitmap;
            }
        }

        return null;
    }

    public NinePatch getNinePatch() {
        return mNinePatchDrawable.getNinePatch();
    }

    public Scriptable getScriptObject() {
        return mScriptObject;
    }

    public SvgDrawable getSvgDrawable() {
        return mSvgDrawable;
    }

    public int getStorage() {
        switch (mType) {
            case TYPE_BITMAP:
                ensureBitmapReadyToDraw();
                Bitmap bitmap = mCachedBitmap.bitmap;
                return bitmap.getWidth() * bitmap.getHeight() * 4;

            case TYPE_NINE_PATCH:
                return mNinePatchDrawable.getIntrinsicWidth() * mNinePatchDrawable.getIntrinsicHeight() * 4;

            case TYPE_ANIMATED_GIF:
                int total = 0;
                for(int i = mAnimationDecoder.getFrameCount()-1; i >= 0; i--) {
                    Bitmap b = mAnimationDecoder.getFrame(i);
                    total += b.getWidth() * b.getHeight() * 4;
                }
                return total;

            default:
                return 0;
        }
    }

    private Graphics getGraphicsFromProvider() {
        return mGraphicsProvider.provideGraphics(SharedAsyncGraphicsDrawable.this, mGraphicsProviderData, mMaxWidthHint, mMaxHeightHint);
    }

    private void onGraphicsObtainedFromProvider(Graphics graphics) {
        mLoadingGraphics = false;
        setGraphicsInternal(graphics);
    }

    public void setGraphics(Graphics graphics) {
        mGraphicsProvider = null;
        setGraphicsInternal(graphics);
    }

    public void setGraphicsInternal(Graphics graphics) {
        reset();
        if(graphics != null) {
            Bitmap bitmap = graphics.getBitmap();
            if(bitmap != null) {
                // this is a bitmap (either a 9 patch or a normal bitmap)
                byte[] chunk = bitmap.getNinePatchChunk();
                if(NinePatch.isNinePatchChunk(chunk)) {
                    mType = TYPE_NINE_PATCH;
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
                    }
                    NinePatch np = new NinePatch(bitmap, chunk, null);
                    mIntrinsicWidth = np.getWidth();
                    mIntrinsicHeight = np.getHeight();
                    mNinePatchDrawable = new MyNinePatchDrawable(np);
                    mNinePatchDrawable.setBounds(getBounds());
                } else {
                    setBitmapInternal(bitmap);
                }
            } else if(graphics.getSvgDrawable() != null) {
                mType = TYPE_SVG;
                mSvgDrawable = graphics.getSvgDrawable();
                mSvgDrawable.setCallback(this);
                mIntrinsicWidth = mSvgDrawable.getIntrinsicWidth();
                mIntrinsicHeight = mSvgDrawable.getIntrinsicHeight();
                mSvgDrawable.setBounds(getBounds());
            } else {
                // this is a gif (either an animated one or a static one)
                AnimationDecoder animationDecoder = graphics.getAnimationDecoder();
                if(animationDecoder.getFrameCount() == 1) { // if there is no listener, then there is no need to animate the gif
                    setBitmapInternal(animationDecoder.getBitmap());
                } else {
                    mType = TYPE_ANIMATED_GIF;
                    mIntrinsicWidth = graphics.getTargetWidth();
                    if(mIntrinsicWidth == 0) mIntrinsicWidth = animationDecoder.getWidth();
                    mIntrinsicHeight = graphics.getTargetHeight();
                    if(mIntrinsicHeight == 0) mIntrinsicHeight = animationDecoder.getHeight();
                    mGifBitmap = Bitmap.createBitmap(mIntrinsicWidth, mIntrinsicHeight, Bitmap.Config.ARGB_8888);
                    mAnimationDecoder = animationDecoder;
                    mGifLoop = 0;
                    mGifStartTime = 0;

                    configureGifHandlerRunnable();
                }
            }
        }
        if(mListeners != null) {
            for (SharedAsyncGraphicsDrawableListener listener : mListeners) {
                listener.onSharedAsyncGraphicsDrawableSizeChanged(this);
                listener.onSharedAsyncGraphicsDrawableInvalidated(this);
            }
        }
    }

    public void setBitmap(Bitmap bitmap) {
        reset();

        mGraphicsProvider = null;

        setBitmapInternal(bitmap);

        if(mListeners != null) {
            for (SharedAsyncGraphicsDrawableListener listener : mListeners) {
                listener.onSharedAsyncGraphicsDrawableSizeChanged(this);
                listener.onSharedAsyncGraphicsDrawableInvalidated(this);
            }
        }
    }

    public void setScriptObject(ScriptExecutor scriptExecutor, Scriptable scriptObject, int width, int height, Item item) {
        reset();

        mGraphicsProvider = null;

        mType = TYPE_SCRIPT;
        mScriptExecutor = scriptExecutor;
        mScriptObject = scriptObject;
        mScriptDrawingContext = new ImageScript.DrawingContext(this, item);
        mIntrinsicWidth = width;
        mIntrinsicHeight = height;
        if(mResumeCount > 0) {
            resumeScript();
        }
        if(mListeners != null) {
            for (SharedAsyncGraphicsDrawableListener listener : mListeners) {
                listener.onSharedAsyncGraphicsDrawableSizeChanged(this);
                listener.onSharedAsyncGraphicsDrawableInvalidated(this);
            }
        }
    }

    private void setBitmapInternal(Bitmap bitmap) {
        if(bitmap == null) {
            mType = TYPE_NONE;
        } else {
            mType = TYPE_BITMAP;
            mIntrinsicWidth = bitmap.getWidth();
            mIntrinsicHeight = bitmap.getHeight();
            if(mCachedBitmap != null) {
                // try to update the current bitmap if it is the same size
                Bitmap old_bitmap = mCachedBitmap.bitmap;
                if(old_bitmap.getWidth() == bitmap.getWidth() && old_bitmap.getHeight() == bitmap.getHeight()) {
                    // if the id is the same, copy the bitmap content and update the native cache, otherwise update the native cache only because it will be restored later into the bitmap by ensureReadyToDraw
                    int old_id = mCachedBitmap.id;
                    if(old_id == mId) {
                        old_bitmap.eraseColor(0);
                        Canvas canvas = new Canvas(old_bitmap);
                        canvas.drawBitmap(bitmap, 0, 0, null);
                    }
                    NativeImage.nativeSetImage(mId, bitmap);
                } else {
                    mCachedBitmap = sCachedBitmapPool.getCachedBitmap(mId, bitmap);
                }
            } else {
                mCachedBitmap = sCachedBitmapPool.getCachedBitmap(mId, bitmap);
            }
        }
    }

    private void configureGifHandlerRunnable() {
        if(mHandler == null) {
            mHandler = new Handler();
        }
        if(mAnimationRunnable == null) {
            mAnimationRunnable = new Runnable() {
                @Override
                public void run() {
                    long relTime = (SystemClock.uptimeMillis() - mGifStartTime) % mAnimationDecoder.getTotalDuration();
                    int delay = 0;
                    int count = mAnimationDecoder.getFrameCount();
                    int i;
                    for(i = 0; i < count; i++) {
                        delay += mAnimationDecoder.getDelay(i);
                        if(delay > relTime) break;
                    }
                    if(mPreviousGifIndex != -1 && i < mPreviousGifIndex) {
                        mGifLoop++;
                    }
                    mPreviousGifIndex = i;
                    int maxGifLoop = mAnimationDecoder.getLoopCount();
                    if(maxGifLoop == 0 || mGifLoop < maxGifLoop) {
                        mPlaying = true;
                        mHandler.postDelayed(mAnimationRunnable, delay - relTime);
                    } else {
                        mPlaying = false;
                    }
                    for (SharedAsyncGraphicsDrawableListener listener : mListeners) {
                        listener.onSharedAsyncGraphicsDrawableInvalidated(SharedAsyncGraphicsDrawable.this);
                    }
                }
            };
        }

        if(mResumeCount > 0 && mListeners != null && mListeners.size() > 0) {
            mAnimationRunnable.run();
        }
    }

    public void reset() {
        mType = TYPE_NOT_YET_KNOWN;
        mCachedBitmap = null;
        mNinePatchDrawable = null;
        mAnimationDecoder = null;
        mGifBitmap = null;
        if(mAnimationRunnable != null) {
            mHandler.removeCallbacks(mAnimationRunnable);
            mPlaying = false;
            mAnimationRunnable = null;
            mHandler = null;
        }
        mIntrinsicWidth = 0;
        mIntrinsicHeight = 0;
        if(mType == TYPE_SCRIPT) {
            pauseScript();
            mScriptExecutor = null;
            mScriptObject = null;
        }
    }

    public void startAnimation() {
        if(mAnimationRunnable != null) {
            mGifLoop = 0;
            mGifStartTime = SystemClock.uptimeMillis();
            mHandler.removeCallbacks(mAnimationRunnable);
            mAnimationRunnable.run();
        }
    }

    public void stopAnimation() {
        if(mAnimationRunnable != null) {
            mHandler.removeCallbacks(mAnimationRunnable);
            mPlaying = false;
        }
    }

    public boolean isAnimationPlaying() {
        return mPlaying;
    }

    public int getAnimationFrameCount() {
        return mType == TYPE_ANIMATED_GIF ? mAnimationDecoder.getFrameCount() : 0;
    }
    public Bitmap getAnimationFrameBitmap(int index) {
        if(mType != TYPE_ANIMATED_GIF || index < 0 || index >= mAnimationDecoder.getFrameCount()) {
            return null;
        }

        return mAnimationDecoder.getFrame(index);
    }

    public int getAnimationFrameDelay(int index) {
        if(mType != TYPE_ANIMATED_GIF || index < 0 || index >= mAnimationDecoder.getFrameCount()) {
            return 0;
        }

        return mAnimationDecoder.getDelay(index);
    }

    public void setAnimationFrameDelay(int index, int delay) {
        if(mType != TYPE_ANIMATED_GIF || index < 0 || index >= mAnimationDecoder.getFrameCount()) {
            return;
        }

        mAnimationDecoder.setDelay(index, delay);
    }

    public void setAnimationFrameCount(int count) {
        if(mType != TYPE_ANIMATED_GIF || count < 0) {
            return;
        }

        mAnimationDecoder.setFrameCount(count);
    }

    public int getAnimationLoopCount() {
        if(mType != TYPE_ANIMATED_GIF) {
            return 0;
        }

        return mAnimationDecoder.getLoopCount();
    }

    public void setAnimationLoopCount(int count) {
        if(mType == TYPE_ANIMATED_GIF) {
            mAnimationDecoder.setLoopCount(count);
        }
    }

    public AnimationDecoder getAnimationDecoder() {
        return mAnimationDecoder;
    }

    public int getAnimationTotalDuration() {
        return mAnimationDecoder.getTotalDuration();
    }

    private void ensureBitmapReadyToDraw() {
        Bitmap bitmap = mCachedBitmap.bitmap;
        if(bitmap == null) {
            // bitmap has been freed, reload it
            mCachedBitmap = sCachedBitmapPool.getCachedBitmap(mId, null);
        } else {
            if(mCachedBitmap.id != mId) {
                // bitmap allocated but not the same picture
                int id = mCachedBitmap.id;
                if(!NativeImage.hasImage(id)) {
                    NativeImage.nativeSetImage(id, bitmap);
                }
                NativeImage.nativeLoadImage(mId, bitmap);
                mCachedBitmap.id = mId;
            }
        }
    }

    private static class CachedBitmap {
        public Bitmap bitmap;
        public int id;
        public boolean shared;

        public CachedBitmap(Bitmap bitmap) {
            this.bitmap = bitmap;
        }
    }

    private static class CachedBitmapRef extends WeakReference<CachedBitmap> {
        public long size;
        public CachedBitmapRef(CachedBitmap r, long size) {
            super(r);
            this.size = size;
        }
    }

    private static class CachedBitmapPool {

        private LinkedList<CachedBitmapRef> mCachedBitmaps;

        private long mCurrentSize;
        private long mMaxSize;

        public CachedBitmapPool(long max_size) {
            mMaxSize = max_size;
            mCurrentSize = 0;
            if(max_size != 0) {
                mCachedBitmaps = new LinkedList<>();
            }
        }

        public CachedBitmap getCachedBitmap(int id, Bitmap from) {
            if(mMaxSize == 0) {
                CachedBitmap cachedBitmap = new CachedBitmap(from);
                cachedBitmap.id = id;
                return cachedBitmap;
            }
            CachedBitmapRef cached_bitmap_ref;
            CachedBitmap cached_bitmap;
            int width, height;
            if(from == null) {
                width = NativeImage.nativeGetImageWidth(id);
                height = NativeImage.nativeGetImageHeight(id);
            } else {
                width = from.getWidth();
                height = from.getHeight();
            }
            long size = getBitmapSize(width, height);

            // clear cached bitmaps that have been garbage collected
            for(int l = mCachedBitmaps.size()-1; l>=0; l--) {
                CachedBitmapRef ref = mCachedBitmaps.get(l);
                CachedBitmap cb = ref.get();
                if (cb == null) {
                    mCurrentSize -= ref.size;
                    mCachedBitmaps.remove(l);
                }
            }

            if(from != null && (mCurrentSize + size) <= mMaxSize) {
                // pool is not full, create a new entry
                cached_bitmap = new CachedBitmap(from);
                mCachedBitmaps.add(new CachedBitmapRef(cached_bitmap, size));
                mCurrentSize += size;
            } else {
                // the pool is full

                cached_bitmap_ref = null;
                cached_bitmap = null;

                // look for a matching bitmap in size
                for(CachedBitmapRef ref : mCachedBitmaps) {
                    CachedBitmap cb = ref.get();
                    final Bitmap bitmap = cb.bitmap;
                    if (bitmap.getWidth() == width && bitmap.getHeight() == height) {
                        cached_bitmap_ref = ref;
                        cached_bitmap = cb;
                        break;
                    }
                }

                if(cached_bitmap == null) {
                    // no matching bitmap found, need to free old bitmaps to make room for a new one
                    while((mCurrentSize + size) >= mMaxSize && mCachedBitmaps.size() > 1) {
                        CachedBitmapRef ref = mCachedBitmaps.removeFirst();
                        CachedBitmap cb = ref.get();
                        int removed_id = cb.id;
                        Bitmap bitmap = cb.bitmap;
                        // store the bitmap data in native memory before to free the bitmap in java memory
                        if(!NativeImage.hasImage(removed_id)) {
                            NativeImage.nativeSetImage(removed_id, bitmap);
                        }
                        mCurrentSize -= ref.size;
                        bitmap.recycle();
                        cb.bitmap = null;
                    }
                    if(from == null) {
                        // allocate a new bitmap and recover its data from native memory using its id
                        Bitmap out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                        cached_bitmap = new CachedBitmap(out);
                        NativeImage.nativeLoadImage(id, out);
                    } else {
                        // add the provided bitmap in the cache
                        cached_bitmap = new CachedBitmap(from);
                    }
                    mCachedBitmaps.add(new CachedBitmapRef(cached_bitmap, size));
                    mCurrentSize += size;
                } else {
                    // matching bitmap found, save it to native if needed
                    Bitmap bitmap = cached_bitmap.bitmap;
                    if(bitmap != from) {
                        cached_bitmap.shared = true;
                        int old_id = cached_bitmap.id;
                        if (!NativeImage.hasImage(old_id)) {
                            NativeImage.nativeSetImage(old_id, bitmap);
                        }
                        if (from == null) {
                            NativeImage.nativeLoadImage(id, bitmap);
                        } else {
                            bitmap.eraseColor(0);
                            Canvas canvas = new Canvas(bitmap);
                            canvas.drawBitmap(from, 0, 0, null);
                        }
                    }

                    // move it to front
                    mCachedBitmaps.remove(cached_bitmap_ref);
                    mCachedBitmaps.add(cached_bitmap_ref);
                }
            }

            cached_bitmap.id = id;

            return cached_bitmap;
        }

        private long getBitmapSize(int width, int height) {
            return width * height * 4;
        }
    }


    private static class GraphicsProviderRunner extends Thread {
        private Handler mHandler;
        private final LinkedList<SharedAsyncGraphicsDrawable> mEntries;

        public GraphicsProviderRunner() {
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    Pair<SharedAsyncGraphicsDrawable, Graphics> p = (Pair<SharedAsyncGraphicsDrawable, Graphics>) msg.obj;
                    p.first.onGraphicsObtainedFromProvider(p.second);
                }
            };
            mEntries = new LinkedList<>();

            setPriority(Thread.MIN_PRIORITY);
        }

        @Override
        public void run() {

            while (true) {
                SharedAsyncGraphicsDrawable entry;
                synchronized (mEntries) {
                    try {
                        if (mEntries.size() == 0) {
                            mEntries.wait();
                        }
                    } catch (InterruptedException e) {
                        return;
                    }
                    entry = mEntries.removeLast();
                }

                Graphics graphics = entry.getGraphicsFromProvider();

                Message msg = Message.obtain(mHandler, 0, new Pair<>(entry, graphics));
                mHandler.sendMessage(msg);
            }
        }

        public void processEntry(SharedAsyncGraphicsDrawable entry) {
            synchronized (mEntries) {
                mEntries.add(0, entry);
                mEntries.notify();
            }
        }

        public void prioritize(SharedAsyncGraphicsDrawable entry) {
            synchronized (mEntries) {
                mEntries.remove(entry);
                mEntries.add(entry);
            }
        }
    }

    // a child drawable has been invalidated
    @Override
    public void invalidateDrawable(Drawable drawable) {
        invalidateSelf();
    }

    @Override
    public void scheduleDrawable(Drawable drawable, Runnable runnable, long l) {

    }

    @Override
    public void unscheduleDrawable(Drawable drawable, Runnable runnable) {

    }
}
