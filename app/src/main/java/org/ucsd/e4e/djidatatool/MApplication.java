package org.ucsd.e4e.djidatatool;

import android.app.Application;
import android.content.Context;

import com.secneo.sdk.Helper;

public class MApplication extends Application {
    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        // Entry point for the DJI SDK.
        Helper.install(MApplication.this);
    }
}
