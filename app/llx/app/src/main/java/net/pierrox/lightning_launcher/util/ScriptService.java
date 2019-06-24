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
import net.pierrox.lightning_launcher.plugin.IScriptService_Stub;
import net.pierrox.lightning_launcher.script.ScriptExecutor;
import net.pierrox.lightning_launcher.script.ScriptManager;

import java.util.ArrayList;
import java.util.List;

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
		return new IScriptService_Stub(this);
	}

	@Override
	public int createScript(Script script) {
		ScriptManager scriptManager = mEngine.getScriptManager();
		script.setPath(ScriptManager.sanitizeRelativePath(script.getPath()));
		if (findScript(scriptManager, script) != null) {
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
		net.pierrox.lightning_launcher.script.Script s = findScript(scriptManager, script);
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
	public void deleteScript(Script script) {
		ScriptManager scriptManager = mEngine.getScriptManager();
		net.pierrox.lightning_launcher.script.Script s = findScript(scriptManager, script);
		if (s == null) {
			throw new ScriptException("Script not found");
		}
		scriptManager.deleteScript(s);
	}

	@Override
	public Script getScript(Script script) {
		return map(findScript(mEngine.getScriptManager(), script));
	}

	@Override
	public List<Script> getScriptsMatching(int flags) {
		List<net.pierrox.lightning_launcher.script.Script> list = mEngine.getScriptManager().getAllScriptMatching(flags);
		List<Script> result = new ArrayList<>();
		for (net.pierrox.lightning_launcher.script.Script s : list) {
			result.add(map(s));
		}
		return result;
	}

	@Override
	public void runScript(Script script, String data, ScreenIdentity screenIdentity) throws ScriptException {
		Screen screen = LLApp.get().getScreen(screenIdentity);
		if (screen == null) {
			throw new ScriptException("Screen not available");
		}
		net.pierrox.lightning_launcher.script.Script s = findScript(mEngine.getScriptManager(), script);
		if (s == null) {
			throw new ScriptException("Script with not found");
		}
		mEngine.getScriptExecutor().runScript(screen, s, "PLUGIN", data, null, null);
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

	private net.pierrox.lightning_launcher.script.Script findScript(ScriptManager scriptManager, Script script) {
		script.setPath(ScriptManager.sanitizeRelativePath(script.getPath()));
		net.pierrox.lightning_launcher.script.Script s;
		if (script.getId() != Script.NO_ID) {
			if (script.getId() < 0) {
				//don't allow changing internal scripts
				throw new ScriptException("Access to internal scripts not permitted");
			}
			s = scriptManager.getOrLoadScript(script.getId());
		} else {
			s = scriptManager.getOrLoadScript(script.getPath(), script.getName());
		}
		return s;
	}

	private Script map(net.pierrox.lightning_launcher.script.Script s) {
		return s != null ? new Script(s.id, s.mSourceText, s.name, s.getRelativePath(), s.flags) : null;
	}
}
