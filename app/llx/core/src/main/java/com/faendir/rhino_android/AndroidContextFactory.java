package com.faendir.rhino_android;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.script.ScriptExecutor;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ContinuationPending;
import org.mozilla.javascript.GeneratedClassLoader;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;

/**
 * Created by Lukas on 11.01.2016.
 * Ensures that the classLoader used is correct
 */
class AndroidContextFactory extends ContextFactory {
    @Override
    protected GeneratedClassLoader createClassLoader(ClassLoader parent) {
        return new AndroidClassLoader(parent);
    }

    @Override
    protected Object doTopCall(Callable callable, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        try {
            return super.doTopCall(callable, cx, scope, thisObj, args);
        } catch (Exception e) {
            if(e instanceof ContinuationPending) {
                throw e;
            }

            ScriptExecutor scriptExecutor = LLApp.get().getAppEngine().getScriptExecutor();
            if(e instanceof RhinoException) {
                scriptExecutor.displayScriptError((RhinoException) e);
            } else {
                scriptExecutor.displayScriptError(e.getMessage(), 0);
            }
            return null;
        }
    }
}
