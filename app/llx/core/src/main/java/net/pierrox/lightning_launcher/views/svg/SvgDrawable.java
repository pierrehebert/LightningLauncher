package net.pierrox.lightning_launcher.views.svg;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.Drawable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SvgDrawable extends Drawable {
    public static final int SET_COLOR_NOTHING = 0;
    public static final int SET_COLOR_SAME = 1;
    public static final int SET_COLOR_DONE = 2;

    private Matrix mMatrix = new Matrix();
    private SvgSvg mSvgRoot;
    private Paint mOverrideFillPaint;
    private Paint mOverrideStrokePaint;
    private Integer mOverrideStrokeColor;
    private boolean mEmboss;
    private int mEmbossColor;

    public static boolean isSvgObject(Context context, Object assetOrResourceId) {
        if(assetOrResourceId instanceof String) {
            return true;
        }

        if(!(assetOrResourceId instanceof Integer)) {
            return false;
        }
        int resourceId = (Integer) assetOrResourceId;
        return context.getResources().getResourceTypeName(resourceId).equals("raw");
    }

    public static SvgDrawable load(Context context, Object assetOrResourceId) {
        if (assetOrResourceId instanceof String) {
            return new SvgDrawable(context, (String) assetOrResourceId);
        } else {
            return new SvgDrawable(context, (Integer) assetOrResourceId);
        }
    }

	public SvgDrawable(File file) {
        parse(file);
        updateMatrix();
	}

	public SvgDrawable(Context context, String assetName) {
        parse(context, assetName);
        updateMatrix();
	}

	public SvgDrawable(Context context, int rawResourceId) {
        parse(context, rawResourceId);
        updateMatrix();
	}

	@Override
	public int getIntrinsicHeight() {
		return mSvgRoot==null ? 0 : (int) mSvgRoot.height;
	}

	@Override
	public int getIntrinsicWidth() {
		return mSvgRoot==null ? 0 : (int) mSvgRoot.width;
	}

    @Override
    public void setBounds(Rect bounds) {
        super.setBounds(bounds);

        updateMatrix();
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);

        updateMatrix();
    }


    private Bitmap mCachedBitmap;

    private void updateMatrix() {
        if(mSvgRoot == null) return;
        mMatrix.reset();
        Rect bounds = getBounds();
        RectF src = new RectF(0, 0, mSvgRoot.width, mSvgRoot.height);
        RectF dst = new RectF(bounds);
        mMatrix.setRectToRect(src, dst, Matrix.ScaleToFit.FILL);
//        int width = bounds.width();
//        int height = bounds.height();
//        if(width > 0 && height > 0) {
//            mCachedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//            Canvas canvas = new Canvas(mCachedBitmap);
//            canvas.concat(mMatrix);
//            drawSvgElement(canvas, mSvgRoot);
//        }
    }

    private static final float EMBOSS_SHADOW_RATIO = 300f;
    private static final float EMBOSS_VALUE_SCALE = 0.4f;

    @Override
	public void draw(Canvas canvas) {
        if(mSvgRoot == null) return;

//        if(mCachedBitmap != null) {
//            canvas.drawBitmap(mCachedBitmap, 0, 0, null);
//        }
        canvas.save(Canvas.MATRIX_SAVE_FLAG);

        if(mEmboss) {
            float[] hsv = new float[3];
            Color.colorToHSV(mEmbossColor, hsv);

            float v = hsv[2];

            float dx = getIntrinsicWidth() / EMBOSS_SHADOW_RATIO;
//            canvas.drawARGB(255, 154, 154, 154);

            canvas.save(Canvas.MATRIX_SAVE_FLAG);
            canvas.translate(-dx, -dx);
            canvas.concat(mMatrix);
            hsv[2] = v * EMBOSS_VALUE_SCALE;
            setOverrideColor(Color.HSVToColor(hsv));
            drawSvgElement(canvas, mSvgRoot);
            canvas.restore();

            canvas.save(Canvas.MATRIX_SAVE_FLAG);
            canvas.translate(dx,  dx);
            canvas.concat(mMatrix);
            hsv[2] = v / EMBOSS_VALUE_SCALE;
            setOverrideColor(Color.HSVToColor(hsv));
            drawSvgElement(canvas, mSvgRoot);
            canvas.restore();

            canvas.concat(mMatrix);
            setOverrideColor(mEmbossColor);
            drawSvgElement(canvas, mSvgRoot);
        } else {
            canvas.concat(mMatrix);
            drawSvgElement(canvas, mSvgRoot);
        }

        canvas.restore();
	}

    private void drawSvgElement(Canvas canvas, SvgElement e) {
        if(!e.isVisible()) {
            return;
        }
        if(e instanceof SvgGroup) {
            drawSvgGroup(canvas, (SvgGroup)e);
        } else if(e instanceof SvgPath) {
            drawSvgPath(canvas, (SvgPath)e);
        }
    }

    private void drawSvgGroup(Canvas canvas, SvgGroup group) {
        Matrix t = group.getTransform();
        if(t != null) {
            canvas.save(Canvas.MATRIX_SAVE_FLAG);
            canvas.concat(t);
        }
        for(SvgElement e : group.getChildren()) {
            drawSvgElement(canvas, e);
        }
        if(t != null) {
            canvas.restore();
        }
    }

    private void drawSvgPath(Canvas canvas, SvgPath path) {
        Matrix t = path.getTransform();
        if(t != null) {
            canvas.save(Canvas.MATRIX_SAVE_FLAG);
            canvas.concat(t);
        }
        if(path.fillPaint != null) {
            canvas.drawPath(path.getPath(), mOverrideFillPaint != null ? mOverrideFillPaint : path.fillPaint);
        }
        if(path.strokePaint != null) {
            int oldColor = 0;
            Paint strokePaint;

            if(mOverrideStrokePaint == null) {
                strokePaint = path.strokePaint;
            } else {
                strokePaint = mOverrideStrokePaint;
                strokePaint.setStrokeWidth(path.strokePaint.getStrokeWidth()/2);
//                strokePaint.setStrokeCap(path.strokePaint.getStrokeCap());
//                strokePaint.setStrokeJoin(path.strokePaint.getStrokeJoin());
                strokePaint.setColor(path.strokePaint.getColor());
            }

            if(mOverrideStrokeColor != null) {
                oldColor = strokePaint.getColor();
                strokePaint.setColor(mOverrideStrokeColor);
            }

            canvas.drawPath(path.getPath(), strokePaint);

            if(mOverrideStrokeColor != null) {
                strokePaint.setColor(oldColor);
            }
        }
        if(t != null) {
            canvas.restore();
        }
    }

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}

	@Override
	public void setAlpha(int arg0) {
		// pass
	}

	@Override
	public void setColorFilter(ColorFilter arg0) {
		// pass
	}

    // one of SET_COLOR_*
    public int setColorAt(float x, float y, int color) {
        SvgPath path = pickPathAt(x, y);
        if(path != null) {
            Paint fp = path.fillPaint;
            if(fp != null) {
                if(fp.getColor() != color) {
                    fp.setColor(color);
                    return SET_COLOR_DONE;
                } else {
                    return SET_COLOR_SAME;
                }
            }
        }

        return SET_COLOR_NOTHING;
    }

    public void setDashedStroke(boolean dashed) {
        if(dashed) {
            float length = getIntrinsicWidth() / 60;
            mOverrideStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mOverrideStrokePaint.setStyle(Paint.Style.STROKE);
            mOverrideStrokePaint.setPathEffect(new DashPathEffect(new float[]{ length, length }, 0));
        } else {
            mOverrideStrokePaint = null;
        }
    }

    public void setOverrideColor(int color) {
        mOverrideFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mOverrideFillPaint.setColor(color);
        mOverrideStrokeColor = color;
    }

    public void setOverrideFillPaint(Paint paint) {
        mOverrideFillPaint = paint;
    }

    public void setEmbossColor(int embossColor) {
        mEmbossColor = embossColor;
        invalidateSelf();
    }

    public void setEmboss(boolean emboss) {
        mEmboss = emboss;
        invalidateSelf();
    }

    public SvgSvg getSvgRoot() {
        return mSvgRoot;
    }

    private SvgPath pickPathAt(float x, float y) {
        RectF clip = new RectF(0, 0, (int)mSvgRoot.width, (int)mSvgRoot.height);
        return pickPathAt(mSvgRoot, x, y, clip);
    }

    private Rect mTmpRect = new Rect();

    private SvgPath pickPathAt(SvgGroup group, float x, float y, RectF clipRect) {
        RectF localClipRect = new RectF(clipRect);
        Region r = new Region();
        if(group.getTransform() != null) {
            float[] pts = new float[] { x, y };
            Matrix inv = new Matrix();
            group.getTransform().invert(inv);
            inv.mapRect(localClipRect);
            inv.mapPoints(pts);
            x = pts[0];
            y = pts[1];
        }
        localClipRect.roundOut(mTmpRect);
        Region clip = new Region(mTmpRect);

        int sx = (int) Math.round(x);
        int sy = (int) Math.round(y);
        for(int i = group.getChildren().size()-1; i>=0; i--) {
            SvgElement e = group.getChildren().get(i);
            if(e instanceof SvgGroup) {
                SvgPath p = pickPathAt((SvgGroup) e, x, y, localClipRect);
                if(p != null) {
                    return p;
                }
            } else if(e instanceof SvgPath) {
                SvgPath p = (SvgPath)e;
                if(p.fillPaint != null) {
                    r.setPath(p.getPath(), clip);
                    if(r.contains(sx, sy)) {
                        return p;
                    }
                }
            }
        }

        return null;
    }


    private void parse(File file) {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            parse(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(is != null) try { is.close(); } catch(IOException e) {}
        }
    }

    private void parse(Context context, int rawResourceId) {
        InputStream is = null;
        try {
            is = context.getResources().openRawResource(rawResourceId);
            parse(is);
        } catch (IOException e) {
        } finally {
            if(is != null) try { is.close(); } catch(IOException e) {}
        }
    }

    private void parse(Context context, String assetName) {
        InputStream is = null;
        try {
            is = context.getAssets().open(assetName);
            parse(is);
        } catch (IOException e) {
        } finally {
            if(is != null) try { is.close(); } catch(IOException e) {}
        }
    }

    private void parse(InputStream is) throws IOException {
        SvgSvg root = new SvgSvg();
        try {
            root.parse(is);
        } catch (Exception e) {
            // pass
        }
        root.setHolderDrawable(this);
        mSvgRoot = root;
    }
}
