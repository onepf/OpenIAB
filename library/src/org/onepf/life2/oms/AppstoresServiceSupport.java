package org.onepf.life2.oms;

import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Boris Minaev
 * Date: 29.04.13
 * Time: 17:32
 */
class AppstoresServiceSupport {

    class ServiceInfo {
        boolean installer;
        boolean supported;
        Intent intent;

        ServiceInfo(boolean installer, boolean supported, Intent intent) {
            this.installer = installer;
            this.supported = supported;
            this.intent = intent;
        }
    }

    List<ServiceInfo> serviceInfo;
    int count = 0;

    public AppstoresServiceSupport(int count) {
        this.count = count;
        serviceInfo = new ArrayList<ServiceInfo>();
    }

    public void add(boolean installer, boolean supported, Intent intent) {
        serviceInfo.add(new ServiceInfo(installer, supported, intent));
    }

    public boolean isReady() {
        return serviceInfo.size() >= count;
    }

    public void getServiceIntent(ServiceFounder serviceFounder) {
        Intent intent = null;
        for (ServiceInfo info : serviceInfo) {
            if (info.installer) {
                intent = info.intent;
            }
        }
        if (intent != null) {
            serviceFounder.onServiceFound(intent, true);
        } else {
            for (ServiceInfo info : serviceInfo) {
                if (info.supported) {
                    intent = info.intent;
                }
            }
            if (intent != null) {
                serviceFounder.onServiceFound(intent, false);
            } else {
                serviceFounder.onServiceNotFound();
            }
        }
    }

}

