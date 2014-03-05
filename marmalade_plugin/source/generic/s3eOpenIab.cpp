/*
Generic implementation of the s3eOpenIab extension.
This file should perform any platform-indepedentent functionality
(e.g. error checking) before calling platform-dependent implementations.
*/

/*
 * NOTE: This file was originally written by the extension builder, but will not
 * be overwritten (unless --force is specified) and is intended to be modified.
 */

#include "s3eOpenIab_internal.h"
s3eResult s3eOpenIabInit()
{
    //Add any generic initialisation code here
    return s3eOpenIabInit_platform();
}

void s3eOpenIabTerminate()
{
    //Add any generic termination code here
    s3eOpenIabTerminate_platform();
}

void s3eOpenIabStart(s3eOpenIabOptions* options)
{
	s3eOpenIabStart_platform(options);
}

void s3eOpenIabStop()
{
	s3eOpenIabStop_platform();
}

s3eResult s3eOpenIabIsSupported()
{
	return s3eOpenIabIsSupported_platform();
}

void s3eOpenIabRequestPurchase(const char* productID, bool inApp, const char* developerPayLoad)
{
	s3eOpenIabRequestPurchase_platform(productID, inApp, developerPayLoad);
}

void s3eOpenIabRequestProductInformation(const char** inAppProducts, int numInAppProducts, const char** subProducts, int numSubProducts)
{
	s3eOpenIabRequestProductInformation_platform(inAppProducts, numInAppProducts, subProducts, numSubProducts);
}

void s3eOpenIabRestoreTransactions()
{
	s3eOpenIabRestoreTransactions_platform();
}

void s3eOpenIabConsumeItem(const char* purchaseToken)
{
	s3eOpenIabConsumeItem_platform(purchaseToken);
}

s3eOpenIabStoreNames* s3eOpenIabGetStoreNames()
{
	return s3eOpenIabGetStoreNames_platform();
}

void s3eOpenIabMapSku(const char* sku, const char* storeName, const char* storeSku)
{
	s3eOpenIabMapSku_platform(sku, storeName, storeSku);
}

bool s3eOpenIabIsDebugLog()
{
	return s3eOpenIabIsDebugLog_platform();
}

void s3eOpenIabEnableDebugLogging(bool enable)
{
	s3eOpenIabEnableDebugLogging_platform(enable);
}

void s3eOpenIabEnableDebugTagLogging(bool enable, const char* tag)
{
	s3eOpenIabEnableDebugTagLogging_platform(enable, tag);
}
