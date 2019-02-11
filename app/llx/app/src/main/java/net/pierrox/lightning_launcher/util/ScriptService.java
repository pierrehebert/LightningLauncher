package net.pierrox.lightning_launcher.util;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.api.ScreenIdentity;
import net.pierrox.lightning_launcher.api.Script;
import net.pierrox.lightning_launcher.api.ScriptException;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.engine.Screen;
import net.pierrox.lightning_launcher.plugin.IScriptService;
import net.pierrox.lightning_launcher.plugin.IPluginService_Stub;
import net.pierrox.lightning_launcher.script.ScriptExecutor;
import net.pierrox.lightning_launcher.script.ScriptManager;

/**
 * @author lukas
 * @since 05.02.19
 */
public class ScriptService extends Service implements IScriptService {
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
	public int createScript(Script script) {
		ScriptManager scriptManager = mEngine.getScriptManager();
		script.setPath(ScriptManager.sanitizeRelativePath(script.getPath()));
		if (scriptManager.getOrLoadScript(script.getPath(), script.getName()) != null) {
			return Script.NO_ID;
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
	public void runScript(int id, String data, ScreenIdentity screenIdentity) throws ScriptException {
		Screen screen = LLApp.get().getScreen(screenIdentity);
		if (screen == null) {
			throw new ScriptException("Screen not available");
		}
		net.pierrox.lightning_launcher.script.Script script = mEngine.getScriptManager().getOrLoadScript(id);
		if (script == null) {
			throw new ScriptException("Script with id " + id + " not found");
		}
		mEngine.getScriptExecutor().runScript(screen, script, "PLUGIN", data, null, null);
	}

	@Override
	public String runCode(String code, ScreenIdentity screenIdentity) throws ScriptException {
		Screen screen = LLApp.get().getScreen(screenIdentity);
		if (screen == null) {
			throw new ScriptException("Screen not available");
		}
		ScriptExecutor executor = mEngine.getScriptExecutor();
		return String.valueOf(executor.runScriptAsFunction(screen, code, "", new Object[0], false, true));
	}
}
