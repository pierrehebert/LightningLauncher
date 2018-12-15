package net.pierrox.lightning_launcher.llscript.samplellxscript;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class Hello extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent data = new Intent();
        data.putExtra(Constants.INTENT_EXTRA_SCRIPT_ID, R.raw.hello);
        data.putExtra(Constants.INTENT_EXTRA_SCRIPT_NAME, getString(R.string.script_name));
        data.putExtra(Constants.INTENT_EXTRA_SCRIPT_FLAGS, Constants.FLAG_ITEM_MENU);
        data.putExtra(Constants.INTENT_EXTRA_EXECUTE_ON_LOAD, false);
        data.putExtra(Constants.INTENT_EXTRA_DELETE_AFTER_EXECUTION, false);
        data.putExtra(Constants.INTENT_EXTRA_PATH, getPackageName().replace('.', '/'));
        setResult(RESULT_OK, data);
        finish();
    }
}
