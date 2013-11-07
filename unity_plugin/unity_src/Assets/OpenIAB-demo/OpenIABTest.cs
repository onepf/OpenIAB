using UnityEngine;
using OnePF;
using System.Collections.Generic;

public class OpenIABTest : MonoBehaviour {
#if UNITY_ANDROID
    const string STORE_CUSTOM = "store";
    const string SKU = "sku";

    private void OnEnable() {
        // Listen to all events for illustration purposes
        OpenIABEventManager.billingSupportedEvent += billingSupportedEvent;
        OpenIABEventManager.billingNotSupportedEvent += billingNotSupportedEvent;
        OpenIABEventManager.queryInventorySucceededEvent += queryInventorySucceededEvent;
        OpenIABEventManager.queryInventoryFailedEvent += queryInventoryFailedEvent;
        OpenIABEventManager.purchaseSucceededEvent += purchaseSucceededEvent;
        OpenIABEventManager.purchaseFailedEvent += purchaseFailedEvent;
        OpenIABEventManager.consumePurchaseSucceededEvent += consumePurchaseSucceededEvent;
        OpenIABEventManager.consumePurchaseFailedEvent += consumePurchaseFailedEvent;
    }
    private void OnDisable() {
        // Remove all event handlers
        OpenIABEventManager.billingSupportedEvent -= billingSupportedEvent;
        OpenIABEventManager.billingNotSupportedEvent -= billingNotSupportedEvent;
        OpenIABEventManager.queryInventorySucceededEvent -= queryInventorySucceededEvent;
        OpenIABEventManager.queryInventoryFailedEvent -= queryInventoryFailedEvent;
        OpenIABEventManager.purchaseSucceededEvent -= purchaseSucceededEvent;
        OpenIABEventManager.purchaseFailedEvent -= purchaseFailedEvent;
        OpenIABEventManager.consumePurchaseSucceededEvent -= consumePurchaseSucceededEvent;
        OpenIABEventManager.consumePurchaseFailedEvent -= consumePurchaseFailedEvent;
    }

    private void Start() {
        // Map sku for different stores
        OpenIAB.mapSku(SKU, OpenIAB_Android.STORE_GOOGLE, "google-play.sku");
        OpenIAB.mapSku(SKU, STORE_CUSTOM, "onepf.sku");
    }

    private void OnGUI() {
        float yPos = 5.0f;
        float xPos = 5.0f;
        float width = (Screen.width >= 800 || Screen.height >= 800) ? 320 : 160;
        float height = (Screen.width >= 800 || Screen.height >= 800) ? 80 : 40;
        float heightPlus = height + 10.0f;

        if (GUI.Button(new Rect(xPos, yPos, width, height), "Initialize OpenIAB")) {
            // Application public key
            var public_key = "key";

            // Transmit list of supported stores
            OpenIAB.init(new Dictionary<string, string> {
                {OpenIAB_Android.STORE_GOOGLE, public_key},
                {OpenIAB_Android.STORE_TSTORE, public_key},
                {OpenIAB_Android.STORE_SAMSUNG, public_key},
                {OpenIAB_Android.STORE_YANDEX, public_key}
            });
        }

        if (GUI.Button(new Rect(xPos, yPos += heightPlus, width, height), "Test Purchase")) {
            OpenIAB.purchaseProduct("android.test.purchased");
        }

        if (GUI.Button(new Rect(xPos, yPos += heightPlus, width, height), "Test Refund")) {
            OpenIAB.purchaseProduct("android.test.refunded");
        }

        if (GUI.Button(new Rect(xPos, yPos += heightPlus, width, height), "Test Item Unavailable")) {
            OpenIAB.purchaseProduct("android.test.item_unavailable");
        }

        xPos = Screen.width - width - 5.0f;
        yPos = 5.0f;

        if (GUI.Button(new Rect(xPos, yPos, width, height), "Test Purchase Canceled")) {
            OpenIAB.purchaseProduct("android.test.canceled");
        }

        if (GUI.Button(new Rect(xPos, yPos += heightPlus, width, height), "Query Inventory")) {
            OpenIAB.queryInventory();
        }

        if (GUI.Button(new Rect(xPos, yPos += heightPlus, width, height), "Purchase Real Product")) {
            OpenIAB.purchaseProduct(SKU);
        }

        if (GUI.Button(new Rect(xPos, yPos += heightPlus, width, height), "Stop Billing Service")) {
            OpenIAB.unbindService();
        }
    }

    private void billingSupportedEvent() {
        Debug.Log("billingSupportedEvent");
    }
    private void billingNotSupportedEvent(string error) {
        Debug.Log("billingNotSupportedEvent: " + error);
    }
    private void queryInventorySucceededEvent(Inventory inventory) {
        Debug.Log("queryInventorySucceededEvent: " + inventory);
    }
    private void queryInventoryFailedEvent(string error) {
        Debug.Log("queryInventoryFailedEvent: " + error);
    }
    private void purchaseSucceededEvent(Purchase purchase) {
        Debug.Log("purchaseSucceededEvent: " + purchase);
    }
    private void purchaseFailedEvent(string error) {
        Debug.Log("purchaseFailedEvent: " + error);
    }
    private void consumePurchaseSucceededEvent(Purchase purchase) {
        Debug.Log("consumePurchaseSucceededEvent: " + purchase);
    }
    private void consumePurchaseFailedEvent(string error) {
        Debug.Log("consumePurchaseFailedEvent: " + error);
    }
#endif
}