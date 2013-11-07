using UnityEngine;
using System;
using System.Collections;
using System.Collections.Generic;
using OnePF;

public class OpenIABEventManager : MonoBehaviour {
    // Fired after init is called when billing is supported on the device
    public static event Action billingSupportedEvent;
    // Fired after init is called when billing is not supported on the device
    public static event Action<string> billingNotSupportedEvent;
    // Fired when the inventory and purchase history query has returned
    public static event Action<Inventory> queryInventorySucceededEvent;
    // Fired when the inventory and purchase history query fails
    public static event Action<string> queryInventoryFailedEvent;
    // Fired when a purchase of a product or a subscription succeeds
    public static event Action<Purchase> purchaseSucceededEvent;
    // Fired when a purchase fails
    public static event Action<string> purchaseFailedEvent;
    // Fired when a call to consume a product succeeds
    public static event Action<Purchase> consumePurchaseSucceededEvent;
    // Fired when a call to consume a product fails
    public static event Action<string> consumePurchaseFailedEvent;
	// Fired when transaction was restored
	public static event Action<string> transactionRestoredEvent;
	// Fired when transaction restoration process failed
	public static event Action<string> restoreFailedEvent;
	// Fired when transaction restoration process succeeded
	public static event Action restoreSucceededEvent;
	
    private void Awake() {
        // Set the GameObject name to the class name for easy access from native plugin
        gameObject.name = GetType().ToString();
        DontDestroyOnLoad(this);
    }

#if UNITY_ANDROID
    private void OnBillingSupported(string empty) {
        if (billingSupportedEvent != null)
            billingSupportedEvent();
    }

    private void OnBillingNotSupported(string error) {
        if (billingNotSupportedEvent != null)
            billingNotSupportedEvent(error);
    }

    private void OnQueryInventorySucceeded(string json) {
        if (queryInventorySucceededEvent != null) {
            Inventory inventory = new Inventory(json);
            queryInventorySucceededEvent(inventory);
        }
    }

    private void OnQueryInventoryFailed(string error) {
        if (queryInventoryFailedEvent != null)
            queryInventoryFailedEvent(error);
    }

    private void OnPurchaseSucceeded(string json) {
        if (purchaseSucceededEvent != null)
            purchaseSucceededEvent(new Purchase(json));
    }

    private void OnPurchaseFailed(string error) {
        if (purchaseFailedEvent != null)
            purchaseFailedEvent(error);
    }

    private void OnConsumePurchaseSucceeded(string json) {
        if (consumePurchaseSucceededEvent != null)
            consumePurchaseSucceededEvent(new Purchase(json));
    }

    private void OnConsumePurchaseFailed(string error) {
        if (consumePurchaseFailedEvent != null)
            consumePurchaseFailedEvent(error);
    }

    public void OnTransactionRestored(string sku) {
        if (transactionRestoredEvent != null) {
            transactionRestoredEvent(sku);
        }
    }

    public void OnRestoreTransactionFailed(string error) {
        if (restoreFailedEvent != null) {
            restoreFailedEvent(error);
        }
    }

    public void OnRestoreTransactionSucceeded(string message) {
        if (restoreSucceededEvent != null) {
            restoreSucceededEvent();
        }
    }
#endif
	
#if UNITY_IOS 
	private void OnBillingSupported(string productIdentifiers) {
		string[] delimiters = new string[] { ";" };
		string[] identifiers = productIdentifiers.Split(delimiters,System.StringSplitOptions.RemoveEmptyEntries);
		
		OnePF.StoreKitProduct[] productArray = new OnePF.StoreKitProduct[identifiers.Length];
		int index = 0;
		foreach ( string identifier in identifiers) {
			productArray[index] = OpenIAB_iOS.detailsForProductWithIdentifier(identifier);
			index++;
		}

		OpenIAB_iOS.CreateInventory(productArray);

		if (billingSupportedEvent != null) {
			billingSupportedEvent();
		}
	}

	private void OnBillingNotSupported(string error) {
		if (billingNotSupportedEvent != null)
			billingNotSupportedEvent(error);
	}

	private void OnQueryInventorySucceeded(string json) {
		if (queryInventorySucceededEvent != null) {
			Inventory inventory = new Inventory(json);
			queryInventorySucceededEvent(inventory);
		}
	}

	public static void OnQueryInventorySucceeded(Inventory inventory) {
		if (queryInventorySucceededEvent != null) {
			queryInventorySucceededEvent(inventory);
		}
	}

	private void OnQueryInventoryFailed(string error) {
		if (queryInventoryFailedEvent != null)
			queryInventoryFailedEvent(error);
	}
	
	private void OnPurchaseSucceeded(string sku) {
		if (purchaseSucceededEvent != null) {
			purchaseSucceededEvent(Purchase.CreateFromSku(OpenIAB_iOS.StoreSku2Sku(sku)));
		}
	}
	
	private void OnPurchaseFailed(string identifierAndError) {
		string[] delimiters = new string[1];
		delimiters[0] = ";";
		string[] input = identifierAndError.Split(delimiters,System.StringSplitOptions.None);
		//string identifier = input[0];
		string error = "";
		if(input.Length == 2) {
			error = input[1];
		}
		
		if(purchaseFailedEvent != null) {
			purchaseFailedEvent(error);
		}
	}

	private void OnConsumePurchaseSucceeded(string json) {
		if (consumePurchaseSucceededEvent != null)
			consumePurchaseSucceededEvent(new Purchase(json));
	}

	private void OnConsumePurchaseFailed(string error) {
		if (consumePurchaseFailedEvent != null)
			consumePurchaseFailedEvent(error);
	}
	
	public void OnTransactionRestored(string sku) {
		if (transactionRestoredEvent != null) {
			transactionRestoredEvent(sku);
		}
	}
	
	public void OnRestoreTransactionFailed(string error) {
		if (restoreFailedEvent != null) {
			restoreFailedEvent(error);
		}
	}
	
	public void OnRestoreTransactionSucceeded(string message) {
		if (restoreSucceededEvent != null) {
			restoreSucceededEvent();
		}
	}
#endif
}
