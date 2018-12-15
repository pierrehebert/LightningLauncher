package net.pierrox.lightning_launcher.views.item;

import android.content.Context;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import net.pierrox.lightning_launcher.R;
import net.pierrox.lightning_launcher.data.CustomView;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.script.Script;
import net.pierrox.lightning_launcher.script.ScriptExecutor;

import org.mozilla.javascript.NativeJavaObject;

public class CustomViewView extends ItemView implements View.OnClickListener {
    private boolean mHorizontalGrab;
    private boolean mVerticalGrab;
    private FrameLayout mContainerView;
    private View mView;

    public CustomViewView(Context context, CustomView item) {
        super(context, item);
    }

    @Override
    public void init() {
        mHorizontalGrab = mVerticalGrab = false;

        mContainerView = new FrameLayout(getContext());
        mContainerView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setView(mContainerView);

        createInnerView();
    }

    @Override
    public void destroy() {
        super.destroy();

        if(isInitDone()) {
            destroyInnerView();
        }
    }

    private Object runScript(String id_data) {
        if(id_data != null) {
            LightningEngine engine = mItem.getPage().getEngine();
            Pair<Integer, String> pair = Script.decodeIdAndData(id_data);
            int id = pair.first;
            Script script = engine.getScriptManager().getOrLoadScript(id);
            if(script != null) {
                ScriptExecutor se = engine.getScriptExecutor();
                return se.runScriptFunction(getScreen(), id, this, pair.second);
            }
        }

        return null;
    }

    private void createInnerView() {
        Object object = runScript(((CustomView)mItem).onCreate);
        if(object != null && object instanceof NativeJavaObject) {
            Object java_object = ((NativeJavaObject)object).unwrap();
            if(java_object instanceof View) {
                mView = (View) java_object;
            }
        }

        if(mView == null) {
            Button btn = new Button(getContext());
            btn.setText(R.string.cv_dfl);
            btn.setOnClickListener(this);
            mView = btn;
        }

        try {
            mContainerView.addView(mView);
        } catch (Exception e) {
            // pass, there can be issues, like already have a parent
        }
    }

    private void destroyInnerView() {
        if(mView != null) {
            runScript(((CustomView)mItem).onDestroy);
            mContainerView.removeAllViews();
            mView = null;
        }
    }

    @Override
    public void onClick(View v) {
        destroyInnerView();
        createInnerView();
    }


    public View getView() {
        return mView;
    }

    public void setHorizontalGrab(boolean grab) {
        mHorizontalGrab = grab;
    }

    public boolean hasHorizontalGrab() {
        return mHorizontalGrab;
    }

    public void setVerticalGrab(boolean grab) {
        mVerticalGrab = grab;
    }

    public boolean hasVerticalGrab() {
        return mVerticalGrab;
    }
}
