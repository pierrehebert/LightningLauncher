package com.faendir.rhino_android;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.SecurityController;
import org.mozilla.javascript.WrapFactory;

/**
 * Created by Lukas on 11.01.2016.
 * Helps to prepare a Rhino Context for usage on android.
 */
public final class RhinoAndroidHelper {
    private RhinoAndroidHelper() {
    }

    /**
     * call this instead of Context.enter()
     *
     * @return a context prepared for android
     */
    public static Context prepareContext() {
        if (!SecurityController.hasGlobal())
            SecurityController.initGlobal(new NoSecurityController());
        getContextFactory();
        Context context = Context.enter();
//        context.setWrapFactory(new WrapFactory() {
//            @Override public Object wrap(Context cx, Scriptable scope, Object obj, Class<?> staticType) {
//                final Object ret = super.wrap(cx, scope, obj, staticType);
//                if (ret instanceof Scriptable) {
//                    final Scriptable sret = (Scriptable) ret;
//                    if (sret.getPrototype() == null) sret.setPrototype(new NativeObject());
//                }
//                return ret;
//            }
//        });
        return context;
    }

    public static ContextFactory getContextFactory() {
        ContextFactory factory;
        if (!ContextFactory.hasExplicitGlobal()) {
            factory = new AndroidContextFactory();
            ContextFactory.getGlobalSetter().setContextFactoryGlobal(factory);
        } else if (!(ContextFactory.getGlobal() instanceof AndroidContextFactory)) {
            throw new IllegalStateException("Cannot initialize factory for Android Rhino: There is already another factory");
        } else {
            factory = ContextFactory.getGlobal();
        }
        return factory;
    }
}
