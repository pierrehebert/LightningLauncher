package net.pierrox.lightning_launcher.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.data.LightningIntent;
import net.pierrox.lightning_launcher.engine.Screen;
import net.pierrox.lightning_launcher.script.Script;
import net.pierrox.lightning_launcher.engine.variable.Variable;
import net.pierrox.lightning_launcher.engine.variable.VariableManager;

import java.net.URISyntaxException;


public class FireReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if(com.twofortyfouram.locale.Intent.ACTION_FIRE_SETTING.equals(intent.getAction())) {
            Bundle bundle = intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);

            int action = bundle.getInt(LightningIntent.INTENT_EXTRA_ACTION, GlobalConfig.UNSET);
            if (action == GlobalConfig.RUN_SCRIPT) {
                String id_data = bundle.getString(LightningIntent.INTENT_EXTRA_DATA);
                int target = bundle.getInt(LightningIntent.INTENT_EXTRA_TARGET, Script.TARGET_DESKTOP);
                if(target == Script.TARGET_BACKGROUND) {
                    Pair<Integer,String> pair = Script.decodeIdAndData(id_data);
                    runBackgroundScript(pair.first, pair.second);
                } else {
                    Class<?> cls = LLApp.get().getActivityClassForScriptExecutionTarget(target);
                    if(cls != null) {
                        // in case of script execution, extract data from the bundle (for host variable replacement), then rebuild an intent
                        Intent i = new Intent(context, cls);
                        i.putExtra(LightningIntent.INTENT_EXTRA_ACTION, GlobalConfig.RUN_SCRIPT);
                        i.putExtra(LightningIntent.INTENT_EXTRA_DATA, id_data);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(i);
                    }
                }
            } else if(action == GlobalConfig.SET_VARIABLE) {
                String data = bundle.getString(LightningIntent.INTENT_EXTRA_DATA);
                Variable v = Variable.decode(data);
                if(v != null) {
                    VariableManager vm = LLApp.get().getAppEngine().getVariableManager();
                    vm.edit();
                    vm.setVariable(v.name.trim(), v.value);
                    vm.commit();
                }
            } else {
                try {
                    String uri = bundle.getString("i");
                    Intent i = Intent.parseUri(uri, 0);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(i);
                } catch (URISyntaxException e) {
                    // pass
                }
            }
        }
    }

    private void runBackgroundScript(Integer id, String data) {
        LLApp app = LLApp.get();
        app.getAppEngine().getScriptExecutor().runScript(app.getScreen(Screen.Identity.BACKGROUND), id, "BACKGROUND", data);
    }
}
