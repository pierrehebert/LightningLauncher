package net.pierrox.lightning_launcher.util;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.faendir.rhino_android.RhinoAndroidHelper;
import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.engine.Screen;
import net.pierrox.lightning_launcher.plugin.IPluginService;
import net.pierrox.lightning_launcher.script.Script;
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
public class PluginService extends Service {
	private LightningEngine engine;
	private final IPluginService service = new IPluginService.Stub() {
		@Override
		public int createScript(String code, String name, String path, int flags) {
			ScriptManager scriptManager = engine.getScriptManager();
			path = ScriptManager.sanitizeRelativePath(path);
			if (scriptManager.getOrLoadScript(path, name) == null) {
				Script script = scriptManager.createScriptForFile(name, path);
				script.setSourceText(code);
				script.flags = flags;
				scriptManager.saveScript(script);
				return script.id;
			}
			return Script.NO_ID;
		}

		@Override
		public int createOrOverwriteScript(String code, String name, String path, int flags) {
			ScriptManager scriptManager = engine.getScriptManager();
			path = ScriptManager.sanitizeRelativePath(path);
			Script script = scriptManager.getOrLoadScript(path, name);
			if (script == null) {
				script = scriptManager.createScriptForFile(name, path);
			}
			script.setSourceText(code);
			script.flags = flags;
			scriptManager.saveScript(script);
			return script.id;
		}

		@Override
		public void runScript(int id, String data) {
			Screen screen = LLApp.get().getActiveScreen();
			if (screen == null) {
				LLApp.get().getScreen(Screen.Identity.BACKGROUND);
			}
			engine.getScriptExecutor().runScript(screen, id, "PLUGIN", data);
		}

		@Override
		public String runCode(String code) {
			Screen screen = LLApp.get().getActiveScreen();
			if (screen == null) {
				LLApp.get().getScreen(Screen.Identity.BACKGROUND);
			}
			ScriptExecutor executor = engine.getScriptExecutor();
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
	};

	@Override
	public void onCreate() {
		engine = LLApp.get().getAppEngine();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return service.asBinder();
	}
}
