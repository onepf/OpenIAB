package org.onepf.life2.oms;

import android.content.Context;

/**
 * User: Boris Minaev
 * Date: 16.04.13
 * Time: 16:42
 */
public class OpenIabHelper {
    Context mContext;
    AppstoreServiceManager mServiceManager;

    public OpenIabHelper(Context context, String googlePublicKey) {
        mContext = context;
        mServiceManager = AppstoreServiceManager.getInstance(context, googlePublicKey);
    }


}
