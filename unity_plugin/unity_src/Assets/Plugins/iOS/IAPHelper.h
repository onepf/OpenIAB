#import <Foundation/Foundation.h>
#import <StoreKit/StoreKit.h>

@protocol IAPDelegate <NSObject>
- (void) productsLoaded:(NSArray*)products invalidIdentifiers:(NSArray*)invalidIdentifiers;
- (void) productsNotLoaded;
- (void) transactionPurchased:(SKPaymentTransaction*)transaction;
- (void) transactionFailed:(SKPaymentTransaction*)transaction;
- (void) transactionCancelled:(SKPaymentTransaction*)transaction;
- (void) transactionRestored:(SKPaymentTransaction*)transaction;
- (void) restoreProcessFailed:(NSError*)error;
- (void) restoreProcessCompleted;
@end

@interface IAPHelper : NSObject

+ (BOOL) canMakePayments;
- (id) initWithProductIdentifiers:(NSSet *)productIdentifiers iapDelegate:(id<IAPDelegate>)delegate;
- (void) requestProducts;
- (void) buyProduct:(SKProduct *)product;
- (BOOL) isProductPurchased:(NSString *)productIdentifier;
- (void) restoreCompletedTransactions;

@end
