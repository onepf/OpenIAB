#import "IAPHelper.h"
#import <StoreKit/StoreKit.h>

@interface IAPHelper () <SKProductsRequestDelegate, SKPaymentTransactionObserver>
@end

@implementation IAPHelper
{
    SKProductsRequest * _productsRequest;
    NSSet * _productIdentifiers;
    NSMutableSet * _purchasedProductIdentifiers;
    id<IAPDelegate> _delegate;
}

- (id) initWithProductIdentifiers:(NSSet *)productIdentifiers iapDelegate:(id<IAPDelegate>)delegate
{
    if ((self = [super init])) {
        _delegate = delegate;
        
        // Store product identifiers
        _productIdentifiers = productIdentifiers;
        
        // Check for previously purchased products
        _purchasedProductIdentifiers = [NSMutableSet set];
        [_purchasedProductIdentifiers retain];
        for (NSString * productIdentifier in _productIdentifiers) {
            BOOL productPurchased = [[NSUserDefaults standardUserDefaults] boolForKey:productIdentifier];
            if (productPurchased) {
                [_purchasedProductIdentifiers addObject:productIdentifier];
                NSLog(@"Previously purchased: %@", productIdentifier);
            } else {
                NSLog(@"Not purchased: %@", productIdentifier);
            }
        }
        
        // Add self as transaction observer
        [[SKPaymentQueue defaultQueue] addTransactionObserver:self];
        
    }
    return self;
}

- (void) dealloc
{
    // Remove self as transaction observer
    [[SKPaymentQueue defaultQueue] removeTransactionObserver:self];
    _productsRequest = nil;
    _productIdentifiers = nil;
    [_purchasedProductIdentifiers release];
    _purchasedProductIdentifiers = nil;
    _delegate = nil;
    [super dealloc];
}

+ (BOOL) canMakePayments
{
    return [SKPaymentQueue canMakePayments];
}

- (void) requestProducts
{
    _productsRequest = [[SKProductsRequest alloc] initWithProductIdentifiers:_productIdentifiers];
    _productsRequest.delegate = self;
    [_productsRequest start];
}

- (BOOL) isProductPurchased:(NSString *)productIdentifier
{
    return [_purchasedProductIdentifiers containsObject:productIdentifier];
}

- (void) buyProduct:(SKProduct *)product
{
    NSLog(@"Buying %@...", product.productIdentifier);
    
    SKPayment * payment = [SKPayment paymentWithProduct:product];
    [[SKPaymentQueue defaultQueue] addPayment:payment];
}

- (void) restoreCompletedTransactions
{
    NSLog(@"Restoring completed transactions");
    [[SKPaymentQueue defaultQueue] restoreCompletedTransactions];
}

#pragma mark - SKProductsRequestDelegate

- (void) productsRequest:(SKProductsRequest *)request didReceiveResponse:(SKProductsResponse *)response
{
    NSLog(@"Loaded list of products...");
    _productsRequest = nil;
    
    NSArray * skProducts = response.products;
    for (SKProduct * skProduct in skProducts) {
        NSLog(@"Found product: %@ %@ %0.2f",
              skProduct.productIdentifier,
              skProduct.localizedTitle,
              skProduct.price.floatValue);
    }
    
    [_delegate productsLoaded:response.products invalidIdentifiers:response.invalidProductIdentifiers];
}

#pragma mark - SKRequestDelegate

- (void) request:(SKRequest *)request didFailWithError:(NSError *)error
{
    NSLog(@"Failed to load list of products: %@", [error localizedDescription]);
    _productsRequest = nil;
    
    [_delegate productsNotLoaded];
}

#pragma mark SKPaymentTransactionObserver

- (void) paymentQueue:(SKPaymentQueue *)queue updatedTransactions:(NSArray *)transactions
{
    for (SKPaymentTransaction * transaction in transactions) {
        switch (transaction.transactionState)
        {
            case SKPaymentTransactionStatePurchased:
                [self completeTransaction:transaction];
                break;
            case SKPaymentTransactionStateFailed:
                [self failedTransaction:transaction];
                break;
            case SKPaymentTransactionStateRestored:
                [self restoreTransaction:transaction];
            default:
                break;
        }
    };
}

- (void) completeTransaction:(SKPaymentTransaction *)transaction
{
    NSLog(@"completeTransaction...");
    
    // TODO: add stub for server side verification
    
    [self provideContentForProductIdentifier:transaction.payment.productIdentifier];
    [_delegate transactionPurchased:transaction];
    [[SKPaymentQueue defaultQueue] finishTransaction:transaction];
}

- (void) restoreTransaction:(SKPaymentTransaction *)transaction
{
    NSLog(@"restoreTransaction...");
    
    [self provideContentForProductIdentifier:transaction.originalTransaction.payment.productIdentifier];
    [_delegate transactionRestored:transaction];
    [[SKPaymentQueue defaultQueue] finishTransaction:transaction];
}

- (void) failedTransaction:(SKPaymentTransaction *)transaction
{
    NSLog(@"failedTransaction...");
    if (transaction.error.code != SKErrorPaymentCancelled)
    {
        NSLog(@"Transaction error: %@", transaction.error.localizedDescription);
    }
    if (transaction.error.code == SKErrorPaymentCancelled ||
        transaction.error.code == SKErrorPaymentNotAllowed) {
        [_delegate transactionCancelled:transaction];
    } else {
        [_delegate transactionFailed:transaction];
    }
    [[SKPaymentQueue defaultQueue] finishTransaction: transaction];
}

- (void) provideContentForProductIdentifier:(NSString *)productIdentifier
{
    [_purchasedProductIdentifiers addObject:productIdentifier];
    [[NSUserDefaults standardUserDefaults] setBool:YES forKey:productIdentifier];
    [[NSUserDefaults standardUserDefaults] synchronize];
}

@end