package net.pierrox.lightning_launcher.wear.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.overlay.WindowService;

public class Starter extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startService(LLApp.get().getWindowServiceIntent());

        finish();
    }
}
