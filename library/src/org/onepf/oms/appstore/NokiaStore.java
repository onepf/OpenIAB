/** This file is part of OpenIAB **
 *
 * Copyright (C) 2013-2014 Nokia Corporation and/or its subsidiary(-ies). All rights reserved. *
 *
 * This software, including documentation, is protected by copyright controlled
 * by Nokia Corporation. All rights are reserved. Copying, including reproducing,
 * storing, adapting or translating, any or all of this material requires the prior
 * written consent of Nokia Corporation. This material also contains confidential
 * information which may not be disclosed to others * without the prior written
 * consent of Nokia.
 *
 */

package org.onepf.oms.appstore;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import org.onepf.oms.Appstore;
import org.onepf.oms.AppstoreInAppBillingService;
import org.onepf.oms.DefaultAppstore;
import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.nokiaUtils.NokiaStoreHelper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

public class NokiaStore extends DefaultAppstore {

	private static final String TAG = NokiaStore.class.getSimpleName();

	private final Context context;

	private static final boolean          IS_DEBUG_MODE  = false;
	private              NokiaStoreHelper billingService = null;

	//This is the expected SHA1 finger-print in HEX format
	private static final String EXPECTED_SHA1_FINGERPRINT = "C476A7D71C4CB92641A699C1F1CAC93CA81E0396";

	public static final String NOKIA_INSTALLER = "com.nokia.payment.iapenabler";
	public static final String VENDING_ACTION  = "com.nokia.payment.iapenabler.InAppBillingService.BIND";

	public NokiaStore(final Context context) {
		logInfo("NokiaStore.NokiaStore");

		this.context = context;
	}

	/**
	 * Tells whether in-app billing is ready to work with specified app
	 * For OpenStore app: if any in-app item for this app published in store
	 */
	@Override
	public boolean isBillingAvailable(final String packageName) {
		logInfo("NokiaStore.isBillingAvailable");
		logDebug("packageName = " + packageName);

		if (IS_DEBUG_MODE) {
			return IS_DEBUG_MODE;
		}

		final PackageManager packageManager = context.getPackageManager();
		final List<PackageInfo> allPackages = packageManager.getInstalledPackages(0);

		for (final PackageInfo packageInfo : allPackages) {

			if (NOKIA_INSTALLER.equals(packageInfo.packageName)) {
				return verifyFingreprint();
			}
		}

		return false;
	}

	/**
	 * Returns true only if actual installer for specified app
	 */
	@Override
	public boolean isPackageInstaller(final String packageName) {
		logInfo("NokiaStore.isPackageInstaller");
		logDebug("packageName = " + packageName);

		if (IS_DEBUG_MODE) {
			return true;
		}

		final PackageManager packageManager = context.getPackageManager();
		final String installerPackageName = packageManager.getInstallerPackageName(packageName);

		logDebug("installerPackageName = " + installerPackageName);

		return (NOKIA_INSTALLER.equals(installerPackageName));
	}

	@Override
	public int getPackageVersion(final String packageName) {
		logInfo("NokiaStore.getPackageVersion");
		logDebug("packageName = " + packageName);

		return Appstore.PACKAGE_VERSION_UNDEFINED;
	}

	@Override
	public String getAppstoreName() {
		return OpenIabHelper.NAME_NOKIA;
	}

	@Override
	public AppstoreInAppBillingService getInAppBillingService() {
		logInfo("NokiaStore.getInAppBillingService");

		if (billingService == null) {
			billingService = new NokiaStoreHelper(context, this);
		}
		return billingService;
	}

	/**
	 * Checks SHA1 fingerprint of the enabler
	 *
	 * @return true if signature matches, false if package is not found or signature does not match.
	 */
	private boolean verifyFingreprint() {

		try {
			PackageInfo info = context
				.getPackageManager()
				.getPackageInfo(NOKIA_INSTALLER, PackageManager.GET_SIGNATURES);

			if (info.signatures.length == 1) {

				byte[] cert = info.signatures[0].toByteArray();
				MessageDigest digest;
				digest = MessageDigest.getInstance("SHA1");
				byte[] ENABLER_FINGERPRINT = digest.digest(cert);
				byte[] EXPECTED_FINGERPRINT = hexStringToByteArray(EXPECTED_SHA1_FINGERPRINT);

				if (Arrays.equals(ENABLER_FINGERPRINT, EXPECTED_FINGERPRINT)) {
					Log.i("isBillingAvailable", "NIAP signature verified");
					return true;
				}
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}

	private static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

	private void logDebug(String msg) {
		if (IS_DEBUG_MODE) {
			Log.d(TAG, msg);
		}
	}

	private void logInfo(String msg) {
		if (IS_DEBUG_MODE) {
			Log.i(TAG, msg);
		}
	}

	private void logError(String msg) {
		Log.e(TAG, msg);
	}

}
