package net.pierrox.lightning_launcher.plugin;

import net.pierrox.lightning_launcher.api.ScreenIdentity;
import net.pierrox.lightning_launcher.api.Script;
import net.pierrox.lightning_launcher.api.ScriptException;
import remoter.annotations.ParamIn;
import remoter.annotations.Remoter;

import java.util.List;

/**
 * All methods in this class which take a {@link Script} as parameter will try to identify it first by id and the by name/path
 *
 * @author lukas
 * @since 08.02.19
 */
@Remoter
public interface IScriptService {
	String PERMISSION = "net.pierrox.lightning_launcher.USE_SCRIPT_SERVICE";

	/**
	 * Creates a script
	 *
	 * @param script the script to create
	 * @return the created script id or {@link Script#NO_ID} if the script already exists
	 */
	int createScript(@ParamIn Script script);

	/**
	 * Creates or updates a script
	 *
	 * @param script the script to store
	 * @return the updated script id
	 */
	int updateScript(@ParamIn Script script);

	/**
	 * Deletes a script
	 *
	 * @param script the script to delete
	 */
	void deleteScript(@ParamIn Script script);

	/**
	 * Fetches a script
	 *
	 * @param script the script to fetch
	 * @return the script or null if it does not exist
	 */
	Script getScript(@ParamIn Script script);

	/**
	 * Fetches all scripts matching certain flags
	 *
	 * @param flags the flags to match (e.g. {@link Script#FLAG_ALL}
	 * @return scripts found
	 */
	List<Script> getScriptsMatching(int flags);

	/**
	 * runs the script with the given id and optional data
	 */
	void runScript(@ParamIn Script script, String data_, @ParamIn ScreenIdentity screen) throws ScriptException;

	/**
	 * runs the given code. Code must end with a return statement
	 */
	String runCode(String code, @ParamIn ScreenIdentity screen) throws ScriptException;
}
