package net.pierrox.lightning_launcher.plugin;

interface IPluginService {

    /**
     * @returns id of the newly created script, or -1 if the name/path combination already exists
     */
    int createScript(String code, String name, String path, int flags);

    /**
     * @returns id of the newly created or overwritten script
     */
    int createOrOverwriteScript(String code, String name, String path, int flags);


    /**
     * runs the script with the given id and optional data
     */
    void runScript(int id, String data);

    /**
     * runs the given code. Code must end with a return statement
     */
    String runCode(String code);
}
