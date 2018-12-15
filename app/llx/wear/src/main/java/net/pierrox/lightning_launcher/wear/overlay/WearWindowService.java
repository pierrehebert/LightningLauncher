package net.pierrox.lightning_launcher.wear.overlay;

import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

import net.pierrox.lightning_launcher.overlay.WindowService;

public class WearWindowService extends WindowService {

    @Override
    public void onCreate() {
        super.onCreate();

        final DisplayManager dm = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        dm.registerDisplayListener(new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int i) {

            }

            @Override
            public void onDisplayRemoved(int i) {

            }

            @Override
            public void onDisplayChanged(int i) {
                int state = dm.getDisplay(i).getState();
                if(state == Display.STATE_DOZE || state == Display.STATE_OFF) {
                    hideWorkspace();
                }
            }
        }, null);

        setupDataLayer();
    }

    private void setupDataLayer() {
        GoogleApiClient mGoogleAppiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.e("XXX", "onConnected: " + connectionHint);
                        // Now you can use the data layer API
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.e("XXX", "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.e("XXX", "onConnectionFailed: " + result);
                    }
                })
                .addApi(Wearable.API)
                .build();

        mGoogleAppiClient.connect();
    }
}
