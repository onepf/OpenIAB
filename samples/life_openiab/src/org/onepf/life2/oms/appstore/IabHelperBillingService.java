package org.onepf.life2.oms.appstore;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import com.android.vending.billing.IInAppBillingService;
import org.onepf.life2.oms.appstore.googleUtils.IabHelper;
import org.onepf.life2.oms.appstore.googleUtils.IabResult;


/**
 * Author: Yury Vasileuski
 * Date: 18.05.13
 */

public class IabHelperBillingService {
    public String LOG_PREFIX = "[Google Billing Service] ";

    private IInAppBillingService mBillingService;
    private ServiceConnection mServiceConn;
    private Context mContext;
    private IabHelper mIabHelper;

    public IabHelperBillingService(Context context) {
        mContext = context;
    }

    public IabHelperBillingService(IInAppBillingService billingService, Context context) {
        mContext = context;
        mBillingService = billingService;
    }

    public int isBillingSupported(int apiVersion, String packageName, String type) throws RemoteException {
        return mBillingService.isBillingSupported(apiVersion, packageName, type);
    }

    public Bundle getSkuDetails(int apiVersion, String packageName, String type, Bundle skusBundle) throws RemoteException {
        return mBillingService.getSkuDetails(apiVersion, packageName, type, skusBundle);
    }

    public Bundle getBuyIntent(int apiVersion, String packageName, String sku, String type, String developerPayload) throws RemoteException {
        return mBillingService.getBuyIntent(apiVersion, packageName, sku, type, developerPayload);
    }

    public Bundle getPurchases(int apiVersion, String packageName, String type, String continuationToken) throws RemoteException {
        return mBillingService.getPurchases(apiVersion, packageName, type, continuationToken);
    }

    public int consumePurchase(int apiVersion, String packageName, String purchaseToken) throws RemoteException {
        return mBillingService.consumePurchase(apiVersion, packageName, purchaseToken);
    }

    public void bindService(final IabHelper.OnIabSetupFinishedListener listener) {
        mServiceConn = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
                mIabHelper.logDebug(LOG_PREFIX + "disconnected.");
                didServiceDisconnected();
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mIabHelper.logDebug(LOG_PREFIX + "connected.");
                didServiceConnected(service);

                String packageName = mIabHelper.getPackageName();
                try {
                    mIabHelper.logDebug(LOG_PREFIX + "Checking for in-app billing 3 support.");

                    // check for in-app billing v3 support
                    int response = isBillingSupported(3, packageName, IabHelper.ITEM_TYPE_INAPP);
                    if (response != IabHelper.BILLING_RESPONSE_RESULT_OK) {
                        if (listener != null) listener.onIabSetupFinished(new IabResult(response, "Error checking for billing v3 support."));

                        // if in-app purchases aren't supported, neither are subscriptions.
                        mIabHelper.setSubscriptionsSupported(false);
                        return;
                    }
                    mIabHelper.logDebug(LOG_PREFIX + "In-app billing version 3 supported for " + packageName);

                    // check for v3 subscriptions support
                    response = isBillingSupported(3, packageName, IabHelper.ITEM_TYPE_SUBS);
                    if (response == IabHelper.BILLING_RESPONSE_RESULT_OK) {
                        mIabHelper.logDebug(LOG_PREFIX + "Subscriptions AVAILABLE.");
                        mIabHelper.setSubscriptionsSupported(true);
                    }
                    else {
                        mIabHelper.logDebug(LOG_PREFIX + "Subscriptions NOT AVAILABLE. Response: " + response);
                    }

                    mIabHelper.setSetupDone(true);
                }
                catch (RemoteException e) {
                    if (listener != null) {
                        listener.onIabSetupFinished(new IabResult(IabHelper.IABHELPER_REMOTE_EXCEPTION, "RemoteException while setting up in-app billing."));
                    }
                    e.printStackTrace();
                    return;
                }

                if (listener != null) {
                    listener.onIabSetupFinished(new IabResult(IabHelper.BILLING_RESPONSE_RESULT_OK, "Setup successful."));
                }
            }
        };

        Intent serviceIntent = getServiceIntent();

        if (!mContext.getPackageManager().queryIntentServices(serviceIntent, 0).isEmpty()) {
            // service available to handle that Intent
            mContext.bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
        }
        else {
            // no service available to handle that Intent
            if (listener != null) {
                listener.onIabSetupFinished(new IabResult(mIabHelper.BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE, "Billing service unavailable on device."));
            }
        }
    }

    public void dispose() {
        if (mServiceConn != null) {
            mIabHelper.logDebug(LOG_PREFIX + "Unbinding from service.");
            if (mContext != null) mContext.unbindService(mServiceConn);
        }
    }

    protected void didServiceConnected(android.os.IBinder service) {
        mBillingService = IInAppBillingService.Stub.asInterface(service);
    }

    protected void didServiceDisconnected() {
        mBillingService = null;
    }

    protected Intent getServiceIntent() {
        return new Intent("com.android.vending.billing.InAppBillingService.BIND");
    }

    public boolean isDataSignatureSupported() {
        return true;
    }

    public static String getResponseDesc(int code) {
        return null;
    }

    public void setIabHelper(IabHelper iabHelper) {
        mIabHelper = iabHelper;
    }
}
