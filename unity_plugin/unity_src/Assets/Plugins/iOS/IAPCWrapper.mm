#include "IAPCWrapper.h"

static IAPPlugin *_plugin;

extern "C" {
    bool canMakePayments() {
        return [IAPHelper canMakePayments];
    }
    
    void assignIdentifiersAndCallbackGameObject(char** identifiers, int identifiersCount, char *gameObjectName) {
        
        NSMutableSet *identifierSet = [NSMutableSet set];
        for (int index = 0; index < identifiersCount; index++) {
            char* identifier = identifiers[index];
            [identifierSet addObject:[StringUtils CreateNSString:identifier]];
        }
        if (_plugin) {
            [_plugin release];
            _plugin = nil;
        }
        _plugin = [[IAPPlugin alloc] initWithProductIdentifiers:identifierSet];
        _plugin.gameObjectName = [StringUtils CreateNSString:gameObjectName];
    }
    
    void loadProducts() {
        [_plugin requestProducts];
    }
    
    bool buyProductWithIdentifier(char* identifier) {
        return [_plugin buyProductWithIdentifier:[StringUtils CreateNSString:identifier]];
    }
    
    void restoreProducts() {
        [_plugin restoreCompletedTransactions];
    }
    
    StoreKitProduct detailsForProductWithIdentifier(char *identifier) {
        StoreKitProduct result = [_plugin detailsForProductWithIdentifier:[StringUtils CreateNSString:identifier]];
        return result;
    }
    
    bool isProductPurchased(char* identifier) {
        return [_plugin isProductPurchased:[StringUtils CreateNSString:identifier]];
    }
}