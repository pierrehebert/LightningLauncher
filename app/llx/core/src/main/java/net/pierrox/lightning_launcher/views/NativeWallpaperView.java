package net.pierrox.lightning_launcher.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import net.pierrox.lightning_launcher.configuration.PageConfig;

import java.io.File;

public class NativeWallpaperView extends SurfaceView implements SurfaceHolder.Callback, WallpaperView {
	private int mKey = NativeImage.NO_KEY;
	private int mTintColor = 0;
    private PageConfig.ScaleType mScaleType;
	
	public NativeWallpaperView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		SurfaceHolder holder = getHolder();
		holder.setFormat(PixelFormat.RGBA_8888);
		holder.addCallback(this);
	}
	
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if(mKey != NativeImage.NO_KEY || Color.alpha(mTintColor)!=0) {
        	NativeImage.drawImageWithColorOnSurface(mKey, mTintColor, mScaleType, holder.getSurface());
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
    }
    
    @Override
    public boolean isOpaque() {
    	return false;
    }
    
    @Override
    public boolean gatherTransparentRegion(Region region) {
    	return false;
    }

    @Override
    public void configure(int page, final File file, int tint_color, PageConfig.ScaleType scaleType) {
        if(!NativeImage.isAvailable()) {
            return;
        }

    	int key = NativeImage.getWallpaperKey(page);
    	
//    	if(mKey == key && mTintColor == tint_color && mScaleType == scaleType) {
//    		return;
//    	}
    	
    	mKey = key;
    	mTintColor = tint_color;
        mScaleType = scaleType;
//		new Thread() {
//    		public void run() {
    			if(!NativeImage.hasImage(mKey)) {
                    try {
                        NativeImage.loadImage(mKey, file);
                    } catch(Throwable e) {
                        Toast.makeText(getContext(), "desktop wallpaper too large", Toast.LENGTH_SHORT).show();
                    }
    			}
    			NativeImage.drawImageWithColorOnSurface(mKey, mTintColor, mScaleType, getHolder().getSurface());
//    		}
//    	}.start();
    }
}
