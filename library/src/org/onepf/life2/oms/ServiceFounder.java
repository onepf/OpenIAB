package org.onepf.life2.oms;

import android.content.Intent;

/**
 * User: Boris Minaev
 * Date: 29.04.13
 * Time: 17:31
 */

public interface ServiceFounder {
    void onServiceFound(Intent intent, boolean installer);

    void onServiceNotFound();
}