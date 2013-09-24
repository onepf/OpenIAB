using UnityEngine;
using System;
using System.Collections;
using System.Collections.Generic;
using OpenIabPlugin;

public class OpenIABEventManager : MonoBehaviour {
#if UNITY_ANDROID
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

    private void Awake() {
        // Set the GameObject name to the class name for easy access from native plugin
        gameObject.name = GetType().ToString();
        DontDestroyOnLoad(this);
    }

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
#else
    private void Awake() {
        Destroy(this);
    }
#endif // UNITY_ANDROID
}
