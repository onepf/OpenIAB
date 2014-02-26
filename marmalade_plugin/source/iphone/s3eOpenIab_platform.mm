/*
 * iphone-specific implementation of the s3eOpenIab extension.
 * Add any platform-specific functionality here.
 */
/*
 * NOTE: This file was originally written by the extension builder, but will not
 * be overwritten (unless --force is specified) and is intended to be modified.
 */

#include "IwDebug.h"
#include "s3eEdk.h"
#include "s3eOpenIab_internal.h"
#include "s3eMemory.h"
#include "s3e.h"
#include "s3eMemory.h"
#include "string.h"

#import "StringUtils.h"
#import "IAPHelper.h"
#import "IAPPlugin.h"


using namespace std;

static s3eOpenIabStoreNames* g_storeNames = NULL;

static NSMutableDictionary* g_sku2storeSkuMappings;
static NSMutableDictionary* g_storeSku2skuMappings;

static IAPPlugin* g_plugin;
static NSString* g_storeName = @"AppStore";

static NSString* StoreSku2Sku(const char* storeSku)
{
    return [g_storeSku2skuMappings valueForKey:[StringUtils CreateNSString:storeSku]];
}

static NSString* Sku2StoreSku(const char* sku)
{
    return [g_sku2storeSkuMappings valueForKey:[StringUtils CreateNSString:sku]];
}

void s3eOpenIabMapSku_platform(const char* sku, const char* storeName, const char* storeSku)
{
    if (!strcmp(storeName, s3eOpenIabGetStoreNames()->m_AppStore))
    {
        NSString *nsSku = [StringUtils CreateNSString:sku];
        NSString *nsStoreSku = [StringUtils CreateNSString:storeSku];
        
        [g_sku2storeSkuMappings setValue:nsStoreSku forKey:nsSku];
        [g_storeSku2skuMappings setValue:nsSku forKeyPath:nsStoreSku];
    }
}

s3eResult s3eOpenIabInit_platform()
{
    // Add any platform-specific initialisation code here
    g_sku2storeSkuMappings = [[NSMutableDictionary alloc] init];
    g_storeSku2skuMappings = [[NSMutableDictionary alloc] init];
    
    //g_storeName = [StringUtils CreateNSString:s3eOpenIabGetStoreNames()->m_AppStore];
    return S3E_RESULT_SUCCESS;
}

void s3eOpenIabTerminate_platform()
{
    // Add any platform-specific termination code here
    [g_sku2storeSkuMappings release];
    [g_storeSku2skuMappings release];
}


void s3eOpenIabStart_platform(s3eOpenIabOptions* options)
{
    int n = [g_sku2storeSkuMappings count];
    
    NSMutableSet *identifierSet = [NSMutableSet set];
    for (int index = 0; index < n; ++index)
    {
        NSString* identifier = [[g_sku2storeSkuMappings allValues] objectAtIndex:index];
        [identifierSet addObject:identifier];
    }
    
    if (g_plugin)
    {
        [g_plugin release];
        g_plugin = nil;
    }
    g_plugin = [[IAPPlugin alloc] initWithProductIdentifiers:identifierSet];
    
    // Request product info
    if ([IAPHelper canMakePayments])
    {
        [g_plugin requestProducts];
    }
}

void s3eOpenIabStop_platform()
{
    [g_plugin release];
    g_plugin = nil;
}

s3eResult s3eOpenIabIsSupported_platform()
{
    s3eBool isSupported = [IAPHelper canMakePayments];
    return isSupported ? S3E_RESULT_SUCCESS : S3E_RESULT_ERROR;
}

void s3eOpenIabRequestPurchase_platform(const char* productID, bool inApp, const char* developerPayLoad)
{
    NSString* storeSku = Sku2StoreSku(productID);
    [g_plugin buyProductWithIdentifier:Sku2StoreSku(productID)];
}

void s3eOpenIabRequestProductInformation_platform(const char** inAppProducts, int numInAppProducts, const char** subProducts, int numSubProducts)
{
    // TODO: combine inapps and subs. Store sku info somewhere
    //[g_plugin detailsForProductWithIdentifier:<#(NSString *)#>]
}

void s3eOpenIabRestoreTransactions_platform()
{
    // Should be separate method for iOS
    //[g_plugin restoreCompletedTransactions];
}

static void s3eAGC_DeallocateConsume(uint32 extID, int32 notification, void *systemData, void *instance, int32 returnCode, void *completeData)
{
	s3eOpenIabConsumeResponse *cr = (s3eOpenIabConsumeResponse*)systemData;
	if (cr->m_ErrorMsg)
		delete []cr->m_ErrorMsg;
	delete cr;
}

void s3eOpenIabConsumeItem_platform(const char* purchaseToken)
{
    // TODO: check token
    s3eOpenIabConsumeResponse *cr = new s3eOpenIabConsumeResponse;
    cr->m_ErrorMsg = [StringUtils CStringCopy:@""];
	cr->m_Status = S3E_OPENIAB_RESULT_OK;
	
	s3eEdkCallbacksEnqueue(S3E_EXT_OPENIAB_HASH, S3E_OPENIAB_CONSUME_CALLBACK, cr, 0, NULL, false, s3eAGC_DeallocateConsume, cr);
}

s3eOpenIabStoreNames* s3eOpenIabGetStoreNames_platform()
{
    if (g_storeNames != NULL)
		return g_storeNames;
    
	g_storeNames = new s3eOpenIabStoreNames();
    
	g_storeNames->m_GooglePlay = new char[strlen("") + 1];
    strcpy(g_storeNames->m_GooglePlay, "");
    
    g_storeNames->m_Amazon = new char[strlen("") + 1];
    strcpy(g_storeNames->m_Amazon, "");
    
    g_storeNames->m_Tizen = new char[strlen("") + 1];
    strcpy(g_storeNames->m_Tizen, "");
    
	g_storeNames->m_Samsung = new char[strlen("") + 1];
    strcpy(g_storeNames->m_Samsung, "");
    
    g_storeNames->m_AppStore = new char[strlen("AppStore")+1];
    strcpy(g_storeNames->m_AppStore, "AppStore");
    
    return g_storeNames;
}

/// Deallocation
void DeletePurchase(s3eOpenIabPurchase *p)
{
	delete []p->m_OrderID;
	delete []p->m_PackageID;
	delete []p->m_ProductId;
	delete []p->m_PurchaseToken;
	delete []p->m_DeveloperPayload;
	delete []p->m_JSON;
	delete []p->m_Signature;
  	delete []p->m_AppstoreName;
}

static void s3eAGC_DeallocatePurchase(uint32 extID, int32 notification, void *systemData, void *instance, int32 returnCode, void *completeData)
{
	s3eOpenIabPurchaseResponse *pr = (s3eOpenIabPurchaseResponse*)systemData;
	if (pr->m_PurchaseDetails)
		DeletePurchase(pr->m_PurchaseDetails);
	if (pr->m_ErrorMsg)
		delete []pr->m_ErrorMsg;
	delete pr;
}


/// IAPPlugin
@interface IAPPlugin()
{
    IAPHelper *_iapHelper;
    NSMutableDictionary *_identifierProductMapping;
}
@end

@implementation IAPPlugin
- (id) initWithProductIdentifiers:(NSSet *)identifiers
{
    self = [super init];
    if (self)
    {
        _identifierProductMapping = [[NSMutableDictionary alloc] init];
        _iapHelper = [[IAPHelper alloc] initWithProductIdentifiers:identifiers iapDelegate:self];
    }
    
    return self;
}

- (void) dealloc
{
    [_iapHelper release];
    [_identifierProductMapping release];
    [super dealloc];
}

#pragma mark - Public methods
- (void) requestProducts
{
    [_iapHelper requestProducts];
}

- (bool) buyProductWithIdentifier:(NSString *)productIdentifier
{
    bool result = false;
    
    SKProduct *product = [_identifierProductMapping valueForKey:productIdentifier];
    
    if (product) {
        result = true;
        [_iapHelper buyProduct:product];
    } else {
        NSLog(@"Invalid identifier %@",productIdentifier);
    }
    
    return result;
}

// Restore product
- (void) restoreCompletedTransactions
{
    [_iapHelper restoreCompletedTransactions];
}

- (StoreKitProduct) detailsForProductWithIdentifier:(NSString*)identifier
{
    SKProduct *product = [_identifierProductMapping valueForKey:identifier];
    StoreKitProduct result;
    if (product) {
        result.localizedTitle = [StringUtils CStringCopy:product.localizedTitle];
        result.localizedDescription = [StringUtils CStringCopy:product.localizedDescription];
        NSLocale *priceLocale = product.priceLocale;
        NSNumberFormatter *formatter = [[NSNumberFormatter alloc] init];
        formatter.locale = priceLocale;
        result.priceSymbol = [StringUtils CStringCopy:[formatter currencySymbol]];
        result.localPrice = [StringUtils CStringCopy:[product.price stringValue]];
        [formatter release];
        result.identifier = [StringUtils CStringCopy:product.productIdentifier];
    }
    
    return result;
}

- (BOOL) isProductPurchased:(NSString *)productIdentifier
{
    return [_iapHelper isProductPurchased:productIdentifier];
}

#pragma mark - IAPDelegate
- (void) productsLoaded:(NSArray*)products invalidIdentifiers:(NSArray*)invalidIdentifiers
{
    for (SKProduct *product in products) {
        [_identifierProductMapping setValue:product forKey:product.productIdentifier];
    }
    
    NSArray *productIdentifierArray = [_identifierProductMapping allKeys];
    NSMutableString *string = [NSMutableString string];
    
    for (int index = 0; index < productIdentifierArray.count; index++) {
        [string appendString:[productIdentifierArray objectAtIndex:index]];
        
        if (index != productIdentifierArray.count - 1) {
            [string appendString:@";"];
        }
    }
    // OnBillingSupported
}

- (void) productsNotLoaded
{
    NSString *error = @"Failed to load products";
    // OnBillingNotSupported
}

- (void) transactionPurchased:(SKPaymentTransaction*)transaction
{
    NSString *productIdentifier = transaction.payment.productIdentifier;
    s3eOpenIabPurchaseResponse *pr = new s3eOpenIabPurchaseResponse;
    
    pr->m_ErrorMsg = [StringUtils CStringCopy:g_storeName];
    
    // TODO: maybe move to separate method
    pr->m_Status = S3E_OPENIAB_RESULT_OK;
    pr->m_PurchaseDetails->m_AppstoreName = [StringUtils CStringCopy:g_storeName];
    pr->m_PurchaseDetails->m_ProductId = [StringUtils CStringCopy:transaction.payment.productIdentifier];
    pr->m_PurchaseDetails->m_PurchaseToken = [StringUtils CStringCopy:transaction.transactionIdentifier];
    
    // TODO:
    pr->m_PurchaseDetails->m_PurchaseTime = 0;
    pr->m_PurchaseDetails->m_PurchaseState = 0;
    pr->m_PurchaseDetails->m_OrderID = [StringUtils CStringCopy:@""];
    pr->m_PurchaseDetails->m_PackageID = [StringUtils CStringCopy:@""];
    pr->m_PurchaseDetails->m_DeveloperPayload = [StringUtils CStringCopy:@""];
    pr->m_PurchaseDetails->m_JSON = [StringUtils CStringCopy:@""];
    pr->m_PurchaseDetails->m_Signature = [StringUtils CStringCopy:@""];
    
	s3eEdkCallbacksEnqueue(S3E_EXT_OPENIAB_HASH, S3E_OPENIAB_PURCHASE_CALLBACK, pr, 0, NULL, false, s3eAGC_DeallocatePurchase, pr);
}

- (void) transactionFailed:(SKPaymentTransaction*)transaction
{
    NSString *productIdentifier = transaction.payment.productIdentifier;
    NSString *errorDescription = transaction.error.localizedDescription;
    NSLog(@"transaction failed %@", errorDescription);
    
    s3eOpenIabPurchaseResponse *pr = new s3eOpenIabPurchaseResponse;
    pr->m_Status = S3E_OPENIAB_RESULT_ERROR;
    pr->m_ErrorMsg = [StringUtils CStringCopy:errorDescription];
    pr->m_PurchaseDetails->m_AppstoreName = [StringUtils CStringCopy:g_storeName];
    pr->m_PurchaseDetails->m_ProductId = [StringUtils CStringCopy:transaction.payment.productIdentifier];
    
    // TODO:
    pr->m_PurchaseDetails->m_PurchaseTime = 0;
    pr->m_PurchaseDetails->m_PurchaseState = 1;
    pr->m_PurchaseDetails->m_PurchaseToken = [StringUtils CStringCopy:@""];
    pr->m_PurchaseDetails->m_OrderID = [StringUtils CStringCopy:@""];
    pr->m_PurchaseDetails->m_PackageID = [StringUtils CStringCopy:@""];
    pr->m_PurchaseDetails->m_DeveloperPayload = [StringUtils CStringCopy:@""];
    pr->m_PurchaseDetails->m_JSON = [StringUtils CStringCopy:@""];
    pr->m_PurchaseDetails->m_Signature = [StringUtils CStringCopy:@""];
    
    s3eEdkCallbacksEnqueue(S3E_EXT_OPENIAB_HASH, S3E_OPENIAB_PURCHASE_CALLBACK, pr, 0, NULL, false, s3eAGC_DeallocatePurchase, pr);
}

- (void) transactionCancelled:(SKPaymentTransaction *)transaction
{
    NSString *productIdentifier = transaction.payment.productIdentifier;
    NSString *errorDescription = @"Transaction cancelled";
    NSString *string = [NSString stringWithFormat:@"%@;%@",productIdentifier,errorDescription];
    
    s3eOpenIabPurchaseResponse *pr = new s3eOpenIabPurchaseResponse;
    pr->m_Status = S3E_OPENIAB_RESULT_USER_CANCELED;
    pr->m_ErrorMsg = [StringUtils CStringCopy:errorDescription];
    pr->m_PurchaseDetails->m_AppstoreName = [StringUtils CStringCopy:g_storeName];
    pr->m_PurchaseDetails->m_ProductId = [StringUtils CStringCopy:transaction.payment.productIdentifier];
    
    // TODO:
    pr->m_PurchaseDetails->m_PurchaseTime = 0;
    pr->m_PurchaseDetails->m_PurchaseState = 1;
    pr->m_PurchaseDetails->m_PurchaseToken = [StringUtils CStringCopy:@""];
    pr->m_PurchaseDetails->m_OrderID = [StringUtils CStringCopy:@""];
    pr->m_PurchaseDetails->m_PackageID = [StringUtils CStringCopy:@""];
    pr->m_PurchaseDetails->m_DeveloperPayload = [StringUtils CStringCopy:@""];
    pr->m_PurchaseDetails->m_JSON = [StringUtils CStringCopy:@""];
    pr->m_PurchaseDetails->m_Signature = [StringUtils CStringCopy:@""];
    
    s3eEdkCallbacksEnqueue(S3E_EXT_OPENIAB_HASH, S3E_OPENIAB_PURCHASE_CALLBACK, pr, 0, NULL, false, s3eAGC_DeallocatePurchase, pr);
}

- (void) transactionRestored:(SKPaymentTransaction*)transaction
{
    NSString *productIdentifier = transaction.originalTransaction.payment.productIdentifier;
    // OnTransactionRestored
}

- (void) restoreProcessFailed:(NSError *)error
{
    // OnRestoreTransactionFailed
}

- (void) restoreProcessCompleted
{
    // OnRestoreTransactionSucceeded
}
@end

