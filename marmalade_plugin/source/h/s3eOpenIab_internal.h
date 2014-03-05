/*
 * Internal header for the s3eOpenIab extension.
 *
 * This file should be used for any common function definitions etc that need to
 * be shared between the platform-dependent and platform-indepdendent parts of
 * this extension.
 */

/*
 * NOTE: This file was originally written by the extension builder, but will not
 * be overwritten (unless --force is specified) and is intended to be modified.
 */


#ifndef S3EOPENIAB_INTERNAL_H
#define S3EOPENIAB_INTERNAL_H

#include "s3eTypes.h"
#include "s3eOpenIab.h"
#include "s3eOpenIab_autodefs.h"


/**
 * Initialise the extension.  This is called once then the extension is first
 * accessed by s3eregister.  If this function returns S3E_RESULT_ERROR the
 * extension will be reported as not-existing on the device.
 */
s3eResult s3eOpenIabInit();

/**
 * Platform-specific initialisation, implemented on each platform
 */
s3eResult s3eOpenIabInit_platform();

/**
 * Terminate the extension.  This is called once on shutdown, but only if the
 * extension was loader and Init() was successful.
 */
void s3eOpenIabTerminate();

/**
 * Platform-specific termination, implemented on each platform
 */
void s3eOpenIabTerminate_platform();

void s3eOpenIabStart_platform(s3eOpenIabOptions* options);

void s3eOpenIabStop_platform();

s3eResult s3eOpenIabIsSupported_platform();

void s3eOpenIabRequestPurchase_platform(const char* productID, bool inApp, const char* developerPayLoad);

void s3eOpenIabRequestProductInformation_platform(const char** inAppProducts, int numInAppProducts, const char** subProducts, int numSubProducts);

void s3eOpenIabRestoreTransactions_platform();

void s3eOpenIabConsumeItem_platform(const char* purchaseToken);

s3eOpenIabStoreNames* s3eOpenIabGetStoreNames_platform();

void s3eOpenIabMapSku_platform(const char* sku, const char* storeName, const char* storeSku);

bool s3eOpenIabIsDebugLog_platform();

void s3eOpenIabEnableDebugLogging_platform(bool enable);

void s3eOpenIabEnableDebugTagLogging_platform(bool enable, const char* tag);

#endif /* !S3EOPENIAB_INTERNAL_H */