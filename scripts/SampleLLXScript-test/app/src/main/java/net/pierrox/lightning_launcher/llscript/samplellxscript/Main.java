package net.pierrox.lightning_launcher.llscript.samplellxscript;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import com.faendir.lightning_launcher.scriptlib.AsyncExecutorService;
import com.faendir.lightning_launcher.scriptlib.ResponseManager;
import com.faendir.lightning_launcher.scriptlib.ScriptManager;
import com.faendir.lightning_launcher.scriptlib.ServiceManager;
import com.faendir.lightning_launcher.scriptlib.executor.ScriptExecutor;

import java.io.IOException;

public class Main extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//            ScriptManager.loadScript(this, R.raw.test_container, "Test container", 4, new ScriptManager.Listener() {
//                @Override
//                public void OnLoadFinished(int id) {
//                    ScriptManager.runScript(Main.this, id, null, true);
//                    finish();
//                }
//            });
            final ScriptManager scriptManager = new ScriptManager(this);

            final ScriptExecutor scriptExecutor = new ScriptExecutor(1);
            scriptExecutor.setBackground(true);

            final AsyncExecutorService asyncExecutorService = scriptManager.getAsyncExecutorService();
            asyncExecutorService.add(scriptExecutor);
            asyncExecutorService.start();
    }
}
