#import "IAPHelper.h"
#import <Foundation/Foundation.h>

typedef struct {
    char* localizedTitle;
    char* localizedDescription;
    char* priceSymbol;
    char* localPrice;
    char* identifier;
} StoreKitProduct;

extern void UnitySendMessage(const char* gameObjectName, const char* methodName, const char* argument);

@interface IAPPlugin : NSObject <IAPDelegate>

- (void) setGameObjectName:(NSString*)name;

- (id) initWithProductIdentifiers:(NSSet*)identifier;

- (void) requestProducts;

// Purchase a product
//@return true if product identifier is valid. false otherwise
- (bool) buyProductWithIdentifier:(NSString *)productIdentifier;

// Restore purchased non-consumables
- (void) restoreCompletedTransactions;

// SKU details
- (StoreKitProduct) detailsForProductWithIdentifier:(NSString*)identifier;

- (BOOL) isProductPurchased:(NSString *)productIdentifier;
@end
