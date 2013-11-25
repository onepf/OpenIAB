package com.openiab;

import android.content.Intent;
import android.util.Log;
import com.unity3d.player.UnityPlayerActivity;

public class BillingActivity extends UnityPlayerActivity {

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(OpenIAB.TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);

        // Pass on the activity result to the helper for handling
        if (!OpenIAB.instance().getHelper().handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Log.d(OpenIAB.TAG, "onActivityResult handled by IABUtil.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        OpenIAB.instance().unbindService();
    }
} 