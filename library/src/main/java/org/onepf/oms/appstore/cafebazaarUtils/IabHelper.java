package org.onepf.oms.appstore.cafebazaarUtils;

import android.content.Context;
import android.content.Intent;

import org.jetbrains.annotations.NotNull;
import org.onepf.oms.Appstore;
import org.onepf.oms.appstore.CafeBazaar;

/**
 * CafeBazaar helper performs the exact same actions as GooglePlay In-App Billing v3. Only requires to change the intent service.
 * @author Sergio R. Lumley
 * @since 25.06.15.
 * @see <a href="https://github.com/congenialmobile/TrivialDrive/commit/395517c8d56f1afba4fb9dbe708a266ce773b0e3">Cafe Bazaar IabHelper changes</a>
 */
public class IabHelper extends org.onepf.oms.appstore.googleUtils.IabHelper {
    /**
     * Creates an instance. After creation, it will not yet be ready to use. You must perform
     * setup by calling {@link #startSetup} and wait for setup to complete. This constructor does not
     * block and is safe to call from a UI thread.
     *
     * @param ctx             Your application or Activity context. Needed to bind to the in-app billing service.
     * @param base64PublicKey Your application's public key, encoded in base64.
     *                        This is used for verification of purchase signatures. You can find your app's base64-encoded
     *                        public key in your application's page on Google Play Developer Console. Note that this
     *                        is NOT your "developer public key".
     * @param appstore        TODO
     */
    public IabHelper(@NotNull final Context ctx, final String base64PublicKey, final Appstore appstore) {
        super(ctx, base64PublicKey, appstore);
    }

    @Override
    protected Intent getServiceIntent() {
        final Intent intent = new Intent(CafeBazaar.VENDING_ACTION);
        intent.setPackage(CafeBazaar.ANDROID_INSTALLER);
        return intent;
    }
}
