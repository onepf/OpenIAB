package org.onepf.oms.appstore;

import android.content.Context;


/**
 * Author: Yury Vasileuski
 * Date: 18.05.13
 */

public class YandexStoreBillingService extends GooglePlayBillingService {

    public YandexStoreBillingService(Context context, String publicKey) {
        super(context, publicKey);
    }
}
