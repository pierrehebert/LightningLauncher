package net.pierrox.lightning_launcher.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.R;
import net.pierrox.lightning_launcher.api.ScreenIdentity;
import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.data.EventAction;
import net.pierrox.lightning_launcher.script.Script;

public class MultiPurposeTransparentActivity extends ResourceWrapperActivity {
    private final static String INTENT_EXTRA_TYPE = "t";
    private final static String INTENT_EXTRA_MSG = "m";
    private final static String INTENT_EXTRA_TITLE = "n";
    private final static String INTENT_EXTRA_SCRIPT_ID = "i";
    private final static String INTENT_EXTRA_SCRIPT_LINE = "l";
    private final static String INTENT_EXTRA_ACTION = "a";
    private final static String INTENT_EXTRA_DATA = "d";

    private static final int TYPE_RESET_5SEC_DELAY=0;
    private static final int TYPE_SCRIPT_ERROR=1;
    private static final int TYPE_RUN_ACTION=2;

    private Dialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final LLApp app = LLApp.get();
        app.getAppEngine().setFloatingDesktopVisibility(false);

        Intent intent = getIntent();
        switch(intent.getIntExtra(INTENT_EXTRA_TYPE, 0)) {
            case TYPE_RESET_5SEC_DELAY:
                finish();
                break;

            case TYPE_SCRIPT_ERROR:
                String title = intent.getStringExtra(INTENT_EXTRA_TITLE);
                String msg = intent.getStringExtra(INTENT_EXTRA_MSG);
                final int script_id = intent.getIntExtra(INTENT_EXTRA_SCRIPT_ID, Script.NO_ID);
                final int script_line = intent.getIntExtra(INTENT_EXTRA_SCRIPT_LINE, -1);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(title);
                builder.setMessage(msg);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                if(app.hasScriptEditor()) {
                    builder.setNeutralButton(R.string.sc_view, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            app.startScriptEditor(script_id, script_line);
                            finish();
                        }
                    });
                }
                builder.setNegativeButton(R.string.sc_disable, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // should pass the base dir of the engine in the intent and use it instead of the default engine
                        LLApp.get().getAppEngine().getScriptManager().getOrLoadScript(script_id).setFlag(Script.FLAG_DISABLED, true);
                        finish();
                    }
                });
                builder.setCancelable(false);
                mDialog = builder.create();
                mDialog.show();
                break;

            case TYPE_RUN_ACTION:
                int action = intent.getIntExtra(INTENT_EXTRA_ACTION, GlobalConfig.NOTHING);
                String data = intent.getStringExtra(INTENT_EXTRA_DATA);
                app.getScreen(ScreenIdentity.BACKGROUND).runAction(app.getAppEngine(), "BACKGROUND", new EventAction(action, data));
                finish();
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        finish();
    }

    public static void startForReset5secDelay(Context context) {
        final Intent intent = new Intent(context, MultiPurposeTransparentActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void startForScriptError(Context context, String title, String message, int script_id, int line) {
        final Intent intent = new Intent(context, MultiPurposeTransparentActivity.class);
        intent.putExtra(INTENT_EXTRA_TYPE, TYPE_SCRIPT_ERROR);
        intent.putExtra(INTENT_EXTRA_TITLE, title);
        intent.putExtra(INTENT_EXTRA_MSG, message);
        intent.putExtra(INTENT_EXTRA_SCRIPT_ID, script_id);
        intent.putExtra(INTENT_EXTRA_SCRIPT_LINE, line);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static Intent getIntentForAction(Context context, int action, String data) {
        Intent intent = new Intent(context, MultiPurposeTransparentActivity.class);
        intent.putExtra(INTENT_EXTRA_TYPE, TYPE_RUN_ACTION);
        intent.putExtra(INTENT_EXTRA_ACTION, action);
        intent.putExtra(INTENT_EXTRA_DATA, data);
        return intent;
    }
}
