package net.pierrox.lightning_launcher.util;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.faendir.rhino_android.RhinoAndroidHelper;
import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.api.ScreenIdentity;
import net.pierrox.lightning_launcher.api.ScreenNotAvailableException;
import net.pierrox.lightning_launcher.api.Script;
import net.pierrox.lightning_launcher.api.ScriptExistsException;
import net.pierrox.lightning_launcher.api.ScriptNotFoundException;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.engine.Screen;
import net.pierrox.lightning_launcher.plugin.IPluginService;
import net.pierrox.lightning_launcher.plugin.IPluginService_Stub;
import net.pierrox.lightning_launcher.script.ScriptExecutor;
import net.pierrox.lightning_launcher.script.ScriptManager;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import static net.pierrox.lightning_launcher.script.ScriptExecutor.PROPERTY_EVENT_SCREEN;

/**
 * @author lukas
 * @since 05.02.19
 */
public class PluginService extends Service implements IPluginService {
	private LightningEngine mEngine;

	@Override
	public void onCreate() {
		mEngine = LLApp.get().getAppEngine();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return new IPluginService_Stub(this);
	}

	@Override
	public int createScript(Script script) throws ScriptExistsException {
		ScriptManager scriptManager = mEngine.getScriptManager();
		script.setPath(ScriptManager.sanitizeRelativePath(script.getPath()));
		if (scriptManager.getOrLoadScript(script.getPath(), script.getName()) != null) {
			throw new ScriptExistsException();
		}
		net.pierrox.lightning_launcher.script.Script s = scriptManager.createScriptForFile(script.getName(), script.getPath());
		s.setSourceText(script.getText());
		s.flags = script.getFlags();
		scriptManager.saveScript(s);
		return s.id;
	}

	@Override
	public int updateScript(Script script) {
		ScriptManager scriptManager = mEngine.getScriptManager();
		script.setPath(ScriptManager.sanitizeRelativePath(script.getPath()));
		net.pierrox.lightning_launcher.script.Script s;
		if (script.getId() != Script.NO_ID) {
			if (script.getId() < 0) {
				//don't allow changing internal scripts
				throw new SecurityException();
			}
			s = scriptManager.getOrLoadScript(script.getId());
		} else {
			s = scriptManager.getOrLoadScript(script.getPath(), script.getName());
		}
		if (s == null) {
			s = scriptManager.createScriptForFile(script.getName(), script.getPath());
		}
		s.setSourceText(script.getText());
		s.name = script.getName();
		s.setRelativePath(script.getPath());
		s.flags = script.getFlags();
		scriptManager.saveScript(s);
		return s.id;
	}

	@Override
	public Script getScript(int id) {
		net.pierrox.lightning_launcher.script.Script script = mEngine.getScriptManager().getOrLoadScript(id);
		return script != null ? new Script(script.id, script.mSourceText, script.name, script.getRelativePath(), script.flags) : null;
	}

	@Override
	public void runScript(int id, String data, ScreenIdentity screenIdentity) throws ScreenNotAvailableException, ScriptNotFoundException {
		Screen screen = LLApp.get().getScreen(screenIdentity);
		if (screen == null) {
			throw new ScreenNotAvailableException();
		}
		net.pierrox.lightning_launcher.script.Script script = mEngine.getScriptManager().getOrLoadScript(id);
		if (script == null) {
			throw new ScriptNotFoundException();
		}
		mEngine.getScriptExecutor().runScript(screen, script, "PLUGIN", data, null, null);
	}

	@Override
	public String runCode(String code, ScreenIdentity screenIdentity) throws ScreenNotAvailableException {
		Screen screen = LLApp.get().getScreen(screenIdentity);
		if (screen == null) {
			throw new ScreenNotAvailableException();
		}
		ScriptExecutor executor = mEngine.getScriptExecutor();
		if (executor.canRunScriptGlobally()) {
			Scriptable scope = executor.prepareScriptScope();
			org.mozilla.javascript.Context cx = RhinoAndroidHelper.prepareContext();

			ScriptableObject.putProperty(scope, PROPERTY_EVENT_SCREEN, screen);
			ScriptableObject.putProperty(scope, "ev_se", "DIRECT_PLUGIN");
			ScriptableObject.putProperty(scope, "ev_d", null);
			ScriptableObject.putProperty(scope, "ev_t", System.currentTimeMillis());
			ScriptableObject.putProperty(scope, "ev_il", null);
			ScriptableObject.putProperty(scope, "ev_iv", null);
			try {
				String script = "javascript:(function() {var _event = createEvent(ev_sc, ev_se, ev_d, ev_t, ev_il, ev_iv); var getEvent = function() { return _event;};\n" + code + "\n})();";
				cx.setOptimizationLevel(-1);
				org.mozilla.javascript.Script compiledScript = cx.compileString(script, null, 0, null);
				if (compiledScript != null) {
					return String.valueOf(compiledScript.exec(cx, scope));
				}
			} catch (RhinoException e) {
				e.printStackTrace();
			} finally {
				// Exit from the context.
				org.mozilla.javascript.Context.exit();
			}
		}
		return null;
	}
}
