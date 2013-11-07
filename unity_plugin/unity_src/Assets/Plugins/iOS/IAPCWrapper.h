#ifndef __IAPPlugin__IAPCWrapper__
#define __IAPPlugin__IAPCWrapper__

#include <iostream>
#include "IAPHelper.h"
#import "StringUtils.h"
#include "IAPPlugin.h"

extern "C" {
    
    bool canMakePayments();
    void assignIdentifiersAndCallbackGameObject(char** identifiers,int identifiersCount, char *gameObjectName);
    void loadProducts();
    //returns true in case the identifier is a valid product
    bool buyProductWithIdentifier(char* identifier);
    void restoreProducts();
    StoreKitProduct detailsForProductWithIdentifier(char *identifier);
    bool isProductPurchased(char* identifier);
}

#endif /* defined(__IAPPlugin__IAPCWrapper__) */
