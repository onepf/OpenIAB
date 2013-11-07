#import "IAPPlugin.h"
#import "StringUtils.h"

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
    if (self) {
        _iapHelper = [[IAPHelper alloc] initWithProductIdentifiers:identifiers iapDelegate:self];
        _identifierProductMapping = [[NSMutableDictionary alloc] init];
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

//restore product
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

#pragma mark - Unity event manager
static char* sGameObjectCString;
- (void) setGameObjectName:(NSString *)gameObjectName
{
    if (sGameObjectCString != NULL) {
        free(sGameObjectCString);
        sGameObjectCString = NULL;
    }
    if (gameObjectName != nil) {
        sGameObjectCString = [StringUtils CStringCopy:gameObjectName];
    }
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
    
    UnitySendMessage(sGameObjectCString, "OnBillingSupported", [StringUtils CStringCopy:string]);
}

- (void) productsNotLoaded
{
    NSString *error = @"Failed to load products";
    UnitySendMessage(sGameObjectCString, "OnBillingNotSupported", [StringUtils CStringCopy:error]);
}

- (void) transactionPurchased:(SKPaymentTransaction*)transaction
{
    NSString *productIdentifier = transaction.payment.productIdentifier;
    //send the identifier
    UnitySendMessage(sGameObjectCString, "OnPurchaseSucceeded", [StringUtils CStringCopy:productIdentifier]);
}

- (void) transactionFailed:(SKPaymentTransaction*)transaction
{
    NSString *errorDescription = transaction.error.localizedDescription;
    NSLog(@"transaction failed %@",errorDescription);
    NSString *productIdentifier = transaction.payment.productIdentifier;
    NSString *string = [NSString stringWithFormat:@"%@;%@",productIdentifier,errorDescription];
    
    UnitySendMessage(sGameObjectCString, "OnPurchaseFailed", [StringUtils CStringCopy:string]);
}

- (void) transactionCancelled:(SKPaymentTransaction *)transaction
{
    NSString *productIdentifier = transaction.payment.productIdentifier;
    NSString *errorDescription = @"Transaction cancelled";
    NSString *string = [NSString stringWithFormat:@"%@;%@",productIdentifier,errorDescription];
    
    UnitySendMessage(sGameObjectCString, "OnPurchaseFailed", [StringUtils CStringCopy:string]);
}

- (void) transactionRestored:(SKPaymentTransaction*)transaction
{
    NSString *productIdentifier = transaction.originalTransaction.payment.productIdentifier;
    UnitySendMessage(sGameObjectCString, "OnTransactionRestored", [StringUtils CStringCopy:productIdentifier]);
}

- (void) restoreProcessFailed:(NSError *)error
{
    UnitySendMessage(sGameObjectCString, "OnRestoreTransactionFailed",[StringUtils CStringCopy: error.localizedDescription]);
}

- (void) restoreProcessCompleted {
    UnitySendMessage(sGameObjectCString, "OnRestoreTransactionSucceeded","");
}
@end
