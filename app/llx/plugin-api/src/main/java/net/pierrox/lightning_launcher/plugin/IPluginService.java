package net.pierrox.lightning_launcher.plugin;

import net.pierrox.lightning_launcher.api.ScreenIdentity;
import net.pierrox.lightning_launcher.api.ScreenNotAvailableException;
import net.pierrox.lightning_launcher.api.Script;
import net.pierrox.lightning_launcher.api.ScriptExistsException;
import net.pierrox.lightning_launcher.api.ScriptNotFoundException;
import remoter.annotations.ParamIn;
import remoter.annotations.Remoter;

/**
 * @author lukas
 * @since 08.02.19
 */
@Remoter
public interface IPluginService {
	String PERMISSION = "net.pierrox.lightning_launcher.USE_PLUGIN_SERVICE";

	/**
	 * Creates a script
	 *
	 * @param script the script to create
	 * @return the created script id
	 * @throws ScriptExistsException if the script already exists
	 */
	int createScript(@ParamIn Script script) throws ScriptExistsException;

	/**
	 * Creates or updates a script
	 *
	 * @param script the script to store
	 * @return the updated script id
	 */
	int updateScript(@ParamIn Script script);

	/**
	 * Fetches a script
	 *
	 * @param id script id to fetch
	 * @return the script or null if it does not exist
	 */
	Script getScript(int id);

	/**
	 * runs the script with the given id and optional data
	 */
	void runScript(int id, String data_, @ParamIn ScreenIdentity screen) throws ScreenNotAvailableException, ScriptNotFoundException;

	/**
	 * runs the given code. Code must end with a return statement
	 */
	String runCode(String code, @ParamIn ScreenIdentity screen) throws ScreenNotAvailableException;
}
