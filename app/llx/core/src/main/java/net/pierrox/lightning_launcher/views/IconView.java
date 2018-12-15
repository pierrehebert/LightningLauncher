package net.pierrox.lightning_launcher.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.view.View;

public class IconView extends View implements SharedAsyncGraphicsDrawable.SharedAsyncGraphicsDrawableListener {
	private SharedAsyncGraphicsDrawable mSharedAsyncGraphicsDrawable;


    private IconView(Context context) {
        super(context);

        if( Build.VERSION.SDK_INT < 11) {
            setDrawingCacheEnabled(true);
        }
    }

	public IconView(Context context, Bitmap bitmap, boolean filter) {
		this(context);

        mSharedAsyncGraphicsDrawable = new SharedAsyncGraphicsDrawable(bitmap, filter);
	}

	public IconView(Context context, SharedAsyncGraphicsDrawable sharedDrawable) {
		this(context);

        setSharedAsyncGraphicsDrawable(sharedDrawable);
    }

    public void destroy() {
        mSharedAsyncGraphicsDrawable.unregisterListener(this);
    }

    @Override
	protected void onDraw(Canvas canvas) {
        mSharedAsyncGraphicsDrawable.setBounds(0, 0, getWidth(), getHeight());
        mSharedAsyncGraphicsDrawable.draw(canvas);
	}

    public void setSharedAsyncGraphicsDrawable(SharedAsyncGraphicsDrawable sharedAsyncGraphicsDrawable) {
        if(mSharedAsyncGraphicsDrawable != null) {
            mSharedAsyncGraphicsDrawable.unregisterListener(this);
        }
        mSharedAsyncGraphicsDrawable = sharedAsyncGraphicsDrawable;
        mSharedAsyncGraphicsDrawable.registerListener(this);
        invalidate();
    }

    public SharedAsyncGraphicsDrawable getSharedAsyncGraphicsDrawable() {
        return mSharedAsyncGraphicsDrawable;
    }

    @Override
    public void onSharedAsyncGraphicsDrawableInvalidated(SharedAsyncGraphicsDrawable drawable) {
        invalidate();
    }

    @Override
    public void onSharedAsyncGraphicsDrawableSizeChanged(SharedAsyncGraphicsDrawable drawable) {
        requestLayout();
    }
}
