package org.onepf.oms.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.text.TextUtils;

import org.jetbrains.annotations.NotNull;

/**
 * Created by krozov on 08.08.14.
 */
public final class Utils {
    private Utils() {
    }

    /**
     * Checks if the AndroidManifest contains a permission.
     *
     * @param permission The permission to test.
     * @return true if the permission is requested by the application.
     */
    public static boolean hasRequestedPermission(@NotNull Context context, @NotNull final String permission) {
        if (TextUtils.isEmpty(permission)) {
            throw new IllegalArgumentException("Permission can't be null or empty.");
        }

        try {
            PackageInfo info = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
            if (!CollectionUtils.isEmpty(info.requestedPermissions)) {
                for (String requestedPermission : info.requestedPermissions) {
                    if (permission.equals(requestedPermission)) {
                        return true;
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Logger.e(e, "Error during checking permission ", permission);
        }
        return false;
    }

    /**
     * Checks if an application is installed.
     *
     * @return true if the current thread it the UI thread.
     */
    public static boolean packageInstalled(@NotNull final Context context,@NotNull final String packageName) {
        final PackageManager packageManager = context.getPackageManager();
        try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException ignore) {}
        return false;
    }

    /**
     * Checks if an application with the passed name is the installer of the calling app.
     *
     * @param  packageName The package name of the tested application.
     * @return true if the application with the passed package is the installer.
     */
    public static boolean isPackageInstaller(@NotNull final Context context, final String packageName) {
        final PackageManager packageManager = context.getPackageManager();
        final String installerPackageName = packageManager.getInstallerPackageName(context.getPackageName());
        return TextUtils.equals(installerPackageName, packageName);
    }

    /**
     * Checks if a thread is the UI thread.
     *
     * @return true if the current thread it the UI thread.
     */
    public static boolean uiThread() {
        return Thread.currentThread() == Looper.getMainLooper().getThread();
    }
}
