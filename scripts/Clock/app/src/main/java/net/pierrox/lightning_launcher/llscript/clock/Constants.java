package net.pierrox.lightning_launcher.llscript.clock;

/*package*/ class Constants {

    /** This script execution is disabled. */
    /*package*/ static final int FLAG_DISABLED = 1;

    /** The script appears in the lighting app menu. */
    /*package*/ static final int FLAG_APP_MENU = 2;

    /** The script appears in the item menu. */
    /*package*/ static final int FLAG_ITEM_MENU = 4;

    /** The script appears in the custom menu. */
    /*package*/ static final int FLAG_CUSTOM_MENU = 8;




    /** Mandatory: the identifier of the script, should be something like R.raw.your_script */
    /*package*/ static final String INTENT_EXTRA_SCRIPT_ID = "i";

    /** Optional: or'ed combination of the FLAG_* constants above (default is 0) */
    /*package*/ static final String INTENT_EXTRA_SCRIPT_FLAGS = "f";

    /** Optional: name of the script (default is the activity name) */
    /*package*/ static final String INTENT_EXTRA_SCRIPT_NAME = "n";

    /** Optional: execute the script right after loading it (default is false) */
    /*package*/ static final String INTENT_EXTRA_EXECUTE_ON_LOAD = "e";

    /** Optional: delete the script right after loading and (presumably) executing it.
     * This is useful when the script is meant to configure the home screen, create items or
     * install some other scripts, and is no longer needed after this initial setup (default is false).
     */
    /*package*/ static final String INTENT_EXTRA_DELETE_AFTER_EXECUTION = "d";

    /**
     * Optional: path in which to store the script (use "/" as separator).
     * @since V14b2
     */
    /*package*/ static final String INTENT_EXTRA_PATH = "p";
}
