package net.pierrox.lightning_launcher.data;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.Drawable;

import net.pierrox.lightning_launcher.configuration.JsonFields;
import net.pierrox.lightning_launcher.configuration.ShortcutConfig;
import net.pierrox.lightning_launcher.configuration.ShortcutConfigStylable;
import net.pierrox.lightning_launcher.util.AnimationDecoder;
import net.pierrox.lightning_launcher.views.Graphics;
import net.pierrox.lightning_launcher.views.SharedAsyncGraphicsDrawable;
import net.pierrox.lightning_launcher.views.svg.SvgDrawable;
import net.pierrox.lightning_launcher.views.item.ItemView;
import net.pierrox.lightning_launcher.views.item.ShortcutView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class Shortcut extends Item implements ShortcutConfigStylable, SharedAsyncGraphicsDrawable.GraphicsProvider {
    // Android 7.1 app shortcuts
    public static final String INTENT_ACTION_APP_SHORTCUT = "net.pierrox.lightning_launcher.APP_SHORTCUT";
    public static final String INTENT_EXTRA_APP_SHORTCUT_ID = "id";
    public static final String INTENT_EXTRA_APP_SHORTCUT_PKG = "pkg";
    public static final String INTENT_EXTRA_APP_SHORTCUT_DISABLED_MSG = "dis_msg";

    private String mLabel;
	private Intent mIntent;
	protected ShortcutConfig mShortcutConfig;
    protected boolean mSharedShortcutConfig;

    private SharedAsyncGraphicsDrawable mSharedAsyncGraphicsDrawable;

    private Matrix mNormalizationMatrix;

    public Shortcut(Page page) {
        super(page);
    }

    public void setLabel(String label) {
        mLabel=label;
        mPage.setModified();
        mPage.onShortcutLabelChanged(this);
    }

    public String getLabel() {
        return mLabel;
    }

	public void setIntent(Intent intent) {
		mIntent=intent;
        mPage.setModified();
	}
	
    public Intent getIntent() {
        return mIntent;
    }

    public boolean hasSharedShortcutConfig() {
        return mSharedShortcutConfig;
    }

    @Override
	public ShortcutConfig getShortcutConfig() {
		return mShortcutConfig;
	}

    @Override
	public void setShortcutConfig(ShortcutConfig c) {
        mSharedShortcutConfig=(mSharedShortcutConfig && c==mShortcutConfig);
		mShortcutConfig=c;
	}

    @Override
    public ShortcutConfig modifyShortcutConfig() {
        if(mSharedShortcutConfig) {
            // copy on write
            ShortcutConfig sc = mShortcutConfig.clone();
            mShortcutConfig = sc;
            mSharedShortcutConfig = false;
        }
        return mShortcutConfig;
    }

    @Override
    public void getIconFiles(File icon_dir, ArrayList<File> out_files) {
        super.getIconFiles(icon_dir, out_files);
        out_files.add(getCustomIconFile());
        out_files.add(ShortcutConfig.getIconBackFile(icon_dir, mId));
        out_files.add(ShortcutConfig.getIconOverFile(icon_dir, mId));
        out_files.add(ShortcutConfig.getIconMaskFile(icon_dir, mId));
    }

    public void deleteCustomIconFiles(File icon_dir) {
        getCustomIconFile().delete();
        ShortcutConfig.getIconBackFile(icon_dir, mId).delete();
        ShortcutConfig.getIconOverFile(icon_dir, mId).delete();
        ShortcutConfig.getIconMaskFile(icon_dir, mId).delete();
    }



    @Override
    public void createFromJSONObject(JSONObject o) throws JSONException {
		readItemFromJSONObject(o);
		
		JSONObject json_configuration=o.optJSONObject(JsonFields.SHORTCUT_CONFIGURATION);
        mSharedShortcutConfig = json_configuration == null;
        ShortcutConfig defaultShortcutConfig = mPage.config.defaultShortcutConfig;
        if (mSharedShortcutConfig) {
            mShortcutConfig= defaultShortcutConfig;
        } else {
            mShortcutConfig= ShortcutConfig.readFromJsonObject(json_configuration, defaultShortcutConfig);
            mShortcutConfig.loadAssociatedIcons(mPage.getIconDir(), mId);
        }

        mLabel=o.optString(JsonFields.SHORTCUT_LABEL, null);
        if(mLabel == null) {
            mLabel = mName;
        }
		try {
			mIntent=Intent.parseUri(o.getString(JsonFields.SHORTCUT_INTENT), 0);
		} catch (URISyntaxException e) {
			throw new JSONException(e.getMessage());
		}
	}

    @Override
    public ItemView createView(Context context) {
        mNormalizationMatrix = null;
        int iconSize = Utils.getStandardIconSize();
        int std_icon_width = computeTotalWidth(iconSize);
        int std_icon_height = computeTotalHeight(iconSize);

        return new ShortcutView(context, this, std_icon_width, std_icon_height);
    }

    public void init(int id, Rect cell_p, Rect cell_l, String label, Intent intent) {
        super.init(id, cell_p, cell_l);

        mShortcutConfig=mPage.config.defaultShortcutConfig;
        mSharedShortcutConfig=true;

    	mLabel=label;
    	mIntent=intent;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mSharedAsyncGraphicsDrawable = null;
    }

    public SharedAsyncGraphicsDrawable getSharedAsyncGraphicsDrawable() {
        if(mSharedAsyncGraphicsDrawable == null) {
            mSharedAsyncGraphicsDrawable = new SharedAsyncGraphicsDrawable(this, null, mShortcutConfig.iconFilter);
        }
        return mSharedAsyncGraphicsDrawable;
    }

    public void recreateSharedAsyncGraphicsDrawable() {
        mSharedAsyncGraphicsDrawable = new SharedAsyncGraphicsDrawable(this, null, mShortcutConfig.iconFilter);
    }
    
    protected void copyTo(Item item) {
    	super.copyTo(item);
    	Shortcut s=(Shortcut)item;
    	s.mLabel=mLabel;
    	s.mIntent=mIntent;
    	s.mShortcutConfig=mShortcutConfig;
        s.mSharedShortcutConfig=mSharedShortcutConfig;
    }
    
    @Override
	public Item clone() {
		Shortcut s=new Shortcut(mPage);
		copyTo(s);
		return s;
	}

    public int getStdIconSize() {
    	return computeTotalWidth(Utils.getStandardIconSize());
    }

    @Override
    public Graphics provideGraphics(SharedAsyncGraphicsDrawable sbd, Object data, int max_width, int max_height) {
        File icon_file=getCustomIconFile();
        if(!icon_file.exists()) {
            icon_file=getDefaultIconFile();
        }

        Graphics graphics = null;
        if(Utils.isGifFile(icon_file)) {
            AnimationDecoder animationDecoder = Utils.loadGifDecoder(icon_file);
            if(animationDecoder != null) {
                int[] size = getComposedImageSize(animationDecoder.getBitmap(), max_width, max_height);
                graphics = new Graphics(animationDecoder, size[0], size[1]);
            }
        } else if(Utils.isSvgFile(icon_file)) {
            SvgDrawable svgDrawable = new SvgDrawable(icon_file);
            graphics = new Graphics(svgDrawable);
        }

        if(graphics == null) {
            Bitmap baseIcon = null;
            try {
                baseIcon = BitmapFactory.decodeStream(new FileInputStream(icon_file));
                if(mShortcutConfig.iconSizeMode == ShortcutConfig.IconSizeMode.NORMALIZED) {
                    RectF box = computeNonEmptyIconBox(baseIcon);
                    if(box != null) {
                        final int width = baseIcon.getWidth();
                        final int height = baseIcon.getHeight();
                        int margin_w = width / 48;
                        int margin_h = height / 48;
                        RectF normalized = new RectF(0, 0, width, height);
                        normalized.inset(margin_w, margin_h);

                        mNormalizationMatrix = new Matrix();
                        mNormalizationMatrix.setRectToRect(box, normalized, Matrix.ScaleToFit.CENTER);
                    }
                }
            } catch(Throwable e) {
                // catches IOException and OOM Error
            }
            if(baseIcon == null) {
                baseIcon = Utils.getDefaultIcon();
            }

            Bitmap finalIcon = createComposedBitmap(baseIcon, max_width, max_height);

            graphics = new Graphics(finalIcon);
        }

        return graphics;
    }

    private RectF computeNonEmptyIconBox(Bitmap bitmap) {

        final int w = bitmap.getWidth();
        final int h = bitmap.getHeight();
        final int max_margin = Math.min(w, h) / 2;

        final int ALPHA_THRESHOLD = 160;
        final int FILLED_THRESHOLD_W = Math.round(0.05f * w);
        final int FILLED_THRESHOLD_H = Math.round(0.05f * h);

        RectF box = new RectF();
        box.right = w;
        box.bottom = h;

        boolean t=true, l=true, b=true, r=true;

        int opaque_count;
        for(int margin=0; margin<max_margin; margin++) {
            if(t) {
                opaque_count = 0;
                for (int x = margin; x < w - margin; x++) {
                    int pixel = bitmap.getPixel(x, margin);
                    if (Color.alpha(pixel) > ALPHA_THRESHOLD) {
                        opaque_count++;
                        if (opaque_count > FILLED_THRESHOLD_W) {
                            box.top = margin;
                            t = false;
                            break;
                        }
                    }
                }
            }
            if(b) {
                opaque_count = 0;
                for (int x = margin; x < w - margin; x++) {
                    final int y = h - 1 - margin;
                    int pixel = bitmap.getPixel(x, y);
                    if (Color.alpha(pixel) > ALPHA_THRESHOLD) {
                        opaque_count++;
                        if (opaque_count > FILLED_THRESHOLD_W) {
                            box.bottom = y;
                            b = false;
                            break;
                        }
                    }
                }
            }

            if(l) {
                opaque_count = 0;
                for (int y = margin; y < h - margin; y++) {
                    int pixel = bitmap.getPixel(margin, y);
                    if (Color.alpha(pixel) > ALPHA_THRESHOLD) {
                        opaque_count++;
                        if (opaque_count > FILLED_THRESHOLD_H) {
                            box.left = margin;
                            l = false;
                            break;
                        }
                    }
                }
            }

            if(r) {
                opaque_count = 0;
                for (int y = margin; y < h - margin; y++) {
                    final int x = w - 1 - margin;
                    int pixel = bitmap.getPixel(x, y);
                    if (Color.alpha(pixel) > ALPHA_THRESHOLD) {
                        opaque_count++;
                        if (opaque_count > FILLED_THRESHOLD_H) {
                            box.right = x;
                            r = false;
                            break;
                        }
                    }
                }
            }
        }

        return t && l && b && r ? null : box;
    }

    private static int[] mTmpSize = new int[2];
    private int[] getComposedImageSize(Bitmap baseBitmap, int maxWidth, int maxHeight) {
        int width, height;
        switch (mShortcutConfig.iconSizeMode) {
            case STANDARD:
            case NORMALIZED:
                int iconSize = Utils.getStandardIconSize();
                width = computeTotalWidth(iconSize);
                height = computeTotalHeight(iconSize);
                break;

            case FULL_SCALE_RATIO:
                width = computeTotalWidth(baseBitmap.getWidth());
                height = computeTotalHeight(baseBitmap.getHeight());
                float scale = Math.min(maxWidth/(float)width, maxHeight/(float)height);
                if(scale < 1) {
                    width = Math.round(width * scale);
                    height = Math.round(height * scale);
                }
                break;

            case REAL:
            case FULL_SCALE:
            default:
                width = Math.min(maxWidth, computeTotalWidth(baseBitmap.getWidth()));
                height = Math.min(maxHeight, computeTotalHeight(baseBitmap.getHeight()));
                break;
        }

        mTmpSize[0] = width;
        mTmpSize[1] = height;

        return mTmpSize;
    }

    private Bitmap createComposedBitmap(Bitmap baseBitmap, int maxWidth, int maxHeight) {
        int[] size = getComposedImageSize(baseBitmap, maxWidth, maxHeight);
        int width = size[0], height = size[1];

        Bitmap finalBitmap = null;
        try {
            finalBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            composeGraphics(baseBitmap, finalBitmap);
        } catch(Throwable e) {
            // catches OOM Error
        }
        if(finalBitmap == null) {
            finalBitmap = Utils.getDefaultIcon();
        }

        return finalBitmap;
    }


    public int computeTotalWidth(int baseWidth) {
        return (int)Math.ceil(baseWidth * mShortcutConfig.iconScale);
    }

    public int computeTotalHeight(int baseHeight) {
    	final ShortcutConfig c = mShortcutConfig;
        float scaledHeight = baseHeight * c.iconScale;
    	float total_height;
    	if(c.iconReflection) {
    		total_height=scaledHeight*(1+c.iconReflectionSize*c.iconReflectionScale-c.iconReflectionOverlap);
    	} else {
    		total_height=scaledHeight;
    	}
    	return (int)Math.ceil(total_height);
    }

    @Override
    public boolean composeGraphics(Bitmap baseIcon, Bitmap finalIcon) {
        final ShortcutConfig c=mShortcutConfig;

        final Drawable back = c.iconBack;
        final Drawable over = c.iconOver;
        final Drawable mask = c.iconMask;

        final int stdIconSize = getStdIconSize();
        final int baseWidth = baseIcon.getWidth();
        final int baseHeight = baseIcon.getHeight();
        final int finalWidth = finalIcon.getWidth();
        final int finalHeight = finalIcon.getHeight();


        // the box of the layer stack (background, overlay, mask), as it should be for a 1:1 ratio image
        Rect boxLayer = new Rect();

        // the box of the image, which can be different from the layer box if the ratio is not 1:1
        RectF boxImage = new RectF();

        int box_x, box_y, box_w, box_h;
        switch (c.iconSizeMode) {
            case STANDARD:
            case NORMALIZED:
                // force 1:1 ratio with one dimension set to std size
                int innerSize = (int) Math.ceil(stdIconSize * c.iconEffectScale);
                if(baseWidth > baseHeight) {
                    box_w = innerSize;
                    box_x = (stdIconSize - box_w)/2;
                    float scale = box_w / (float)baseWidth;
                    box_h = (int)Math.ceil(baseHeight*scale);
                    box_y = (stdIconSize - box_h)/2;
                } else {
                    box_h = innerSize;
                    box_y = (stdIconSize - box_h)/2;
                    float scale = box_h / (float)baseHeight;
                    box_w = (int)Math.ceil(baseWidth*scale);
                    box_x = (stdIconSize - box_w)/2;
                }
                boxImage.set(box_x, box_y, box_x+box_w, box_y+box_h);
                if(mNormalizationMatrix != null) {
                    mNormalizationMatrix.mapRect(boxImage);
                }
                boxLayer.set(0, 0, stdIconSize, stdIconSize);
                break;

            case REAL:
                box_w = (int)(baseWidth*c.iconScale*c.iconEffectScale);
                box_h = (int)(baseHeight*c.iconScale*c.iconEffectScale);
//                box_x = (finalWidth - box_w) / 2;
//                if(box_x < 0) box_x = 0;
//                box_y =(finalHeight - box_h) / 2;
//                if(box_y < 0) box_y = 0;
//                boxImage.set(box_x, box_y, box_x+box_w, box_y+box_h);
                boxImage.set(0, 0, box_w, box_h);

                box_w = (int)(baseWidth*c.iconScale);
                box_h = (int)(baseHeight*c.iconScale);
                boxLayer.set(0, 0, box_w, box_h);
                break;

            case FULL_SCALE:
            case FULL_SCALE_RATIO:
            default:
                float div = 1 + c.iconReflectionSize * c.iconReflectionScale - c.iconReflectionOverlap;
                if(div == 0) div = 1;
                int h = c.iconReflection ? (int)Math.ceil(finalHeight / div) : finalHeight;
                boolean keep_ratio = c.iconSizeMode == ShortcutConfig.IconSizeMode.FULL_SCALE_RATIO;
                if(keep_ratio) {
                    float scale = Math.min(finalWidth/(float)baseWidth, h/(float)baseHeight);
                    box_w = Math.round(scale * baseWidth * c.iconEffectScale);
                    box_h = Math.round(scale * baseHeight * c.iconEffectScale);
                } else {
                    box_w = (int) (finalWidth * c.iconEffectScale);
                    box_h = (int) (h * c.iconEffectScale);
                }
                box_x = (finalWidth - box_w) / 2;
                box_y = (h - box_h) / 2;
                boxImage.set(box_x, box_y, box_x+box_w, box_y+box_h);

                if(keep_ratio) {
//                    boxLayer.set(box_x, box_y, box_x+box_w, box_y+box_h);
                    boxLayer.set(0, 0, finalWidth, h);
                } else {
                    boxLayer.set(0, 0, finalWidth, h);
                }
                break;
        }



        Canvas canvas=new Canvas(finalIcon);

        Paint paint=new Paint();

        final int color_filter = c.iconColorFilter;
        if(color_filter != 0xffffffff) {
            final float r = Color.red(color_filter) / 255f;
            final float g = Color.green(color_filter) / 255f;
            final float b = Color.blue(color_filter) / 255f;
            final float s = Color.alpha(color_filter) / 255f;

            final float invSat = 1 - s;
            final float R = 0.213f * invSat;
            final float G = 0.715f * invSat;
            final float B = 0.072f * invSat;

            ColorMatrix colorMatrix = new ColorMatrix(new float[]{
                    R + s, G, B, 0, 0,
                    R, G + s, B, 0, 0,
                    R, G, B + s, 0, 0,
                    0, 0, 0, 1, 0}
            );

            ColorMatrix colorMatrix2 = new ColorMatrix(new float[]{
                    r, 0, 0, 0, 0,
                    0, g, 0, 0, 0,
                    0, 0, b, 0, 0,
                    0, 0, 0, 1, 0}
            );

            colorMatrix.postConcat(colorMatrix2);

            paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        }

        if(c.iconReflection) {
            float reflection_height = boxLayer.height()*c.iconReflectionSize*c.iconReflectionScale;

            // alays filter the reflection (minor overhead compared with other processing involved)
            paint.setFilterBitmap(true);

            canvas.save(Canvas.MATRIX_SAVE_FLAG);
            canvas.translate(0, boxLayer.bottom+boxLayer.bottom*c.iconReflectionScale-boxImage.height()*c.iconReflectionOverlap);
            canvas.scale(1, -c.iconReflectionScale);
            Paint mirror_icon_paint=new Paint();
            LinearGradient linear_shader=new LinearGradient(0, 0, 0, reflection_height, 0xa0ffffff , 0x00ffffff, TileMode.CLAMP);
            mirror_icon_paint.setShader(linear_shader);
            mirror_icon_paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));

            if(back!=null) {
                back.setBounds(boxLayer);
                back.draw(canvas);
            }
            canvas.saveLayer(0, 0, finalWidth, finalHeight, null, Canvas.ALL_SAVE_FLAG);
            canvas.save(Canvas.MATRIX_SAVE_FLAG);
            canvas.drawBitmap(baseIcon, null, boxImage, paint);
            canvas.restore();
            if(mask!=null) {
                mask.setBounds(boxLayer);
                mask.draw(canvas);
            }
            if(over!=null) {
                over.setBounds(boxLayer);
                over.draw(canvas);
            }
            canvas.restore(); // layer

            canvas.restore(); // matrix

            canvas.save(Canvas.MATRIX_SAVE_FLAG);
            canvas.translate(0, boxLayer.bottom-boxLayer.height()*c.iconReflectionOverlap+1);
            canvas.drawRect(0, 0, boxLayer.width(), reflection_height, mirror_icon_paint);
            canvas.restore();
        }

        paint.setFilterBitmap(baseWidth != boxImage.width() || baseHeight != boxImage.height());

        if(back!=null) {
            back.setBounds(boxLayer);
            back.draw(canvas);
        }
        canvas.saveLayer(0, 0, finalWidth, finalHeight, null, Canvas.ALL_SAVE_FLAG);
        canvas.save(Canvas.MATRIX_SAVE_FLAG);
        canvas.drawBitmap(baseIcon, null, boxImage, paint);
        canvas.restore();
        if(mask!=null) {
            mask.setBounds(boxLayer);
            mask.draw(canvas);
        }
        if(over!=null) {
            over.setBounds(boxLayer);
            over.draw(canvas);
        }
        canvas.restore();

        return true;
    }
}
