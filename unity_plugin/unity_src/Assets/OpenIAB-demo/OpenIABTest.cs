using UnityEngine;
using OnePF;
using System.Collections.Generic;

public class OpenIABTest : MonoBehaviour
{
    const string SKU = "sku";

    const string SKU_AMMO = "sku_ammo_general";
    const string SKU_MEDKIT = "sku_medkit_general";
    const string SKU_SUBSCRIPTION = "sku_sub_general";

#pragma warning disable 0414
    string _label = "";
#pragma warning restore 0414

    bool _isInitialized = false;

    private void OnEnable()
    {
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
    private void OnDisable()
    {
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

    private void Start()
    {
        // Map skus for different stores
        OpenIAB.mapSku(SKU, OpenIAB_Android.STORE_GOOGLE, "sku");

        OpenIAB.mapSku(SKU_AMMO, OpenIAB_WP8.STORE, "sku_ammo");
        OpenIAB.mapSku(SKU_MEDKIT, OpenIAB_WP8.STORE, "sku_medkit");
        OpenIAB.mapSku(SKU_SUBSCRIPTION, OpenIAB_WP8.STORE, "sku_sub");
    }

#if UNITY_ANDROID
    private void OnGUI()
    {
        float yPos = 5.0f;
        float xPos = 5.0f;
        GUI.skin.button.fontSize = (Screen.width >= 800 || Screen.height >= 800) ? 34 : 14;
        float width = (Screen.width >= 800 || Screen.height >= 800) ? 380 : 160;
        float height = (Screen.width >= 800 || Screen.height >= 800) ? 100 : 40;
        float heightPlus = height + 10.0f;

        if (GUI.Button(new Rect(xPos, yPos, width, height), "Initialize OpenIAB"))
        {
            // Application public key
            var public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqibEPHCtfPm3Rn26gbE6vhCc1d6A072im+oWNlkUAJYV//pt1vCkYLqkkw/P2esPSWaw1nt66650vfVYc3sYY6L782n/C+IvZWQt0EaLrqsSoNfN5VqPhPeGf3wqsOvbKw9YqZWyKL4ddZUzRUPex5xIzjHHm3qIJI5v7iFJHOxOj0bLuEG8lH0Ljt/w2bNe4o0XXoshYDqpzIKmKy6OYNQOs8iBTJlfSmPrlGudmldW6CsuAKeVGm+Z+2xx3Xxsx3eSwEgEaUc1ZsMWSGsV6dXgc3JrUvK23JRJUu8X5Ec1OQLyxL3VelD5f0iKVTJ1kw59tMAVZ7DDpzPggWpUkwIDAQAB";

            var options = new Options();
            options.verifyMode = OptionsVerifyMode.VERIFY_SKIP;
            options.storeKeys = new Dictionary<string, string> {
                {OpenIAB_Android.STORE_GOOGLE, public_key}
            };

            // Transmit options and start the service
            OpenIAB.init(options);
        }

        if (!_isInitialized)
            return;

        if (GUI.Button(new Rect(xPos, yPos += heightPlus, width, height), "Test Purchase"))
        {
            OpenIAB.purchaseProduct("android.test.purchased");
        }

        if (GUI.Button(new Rect(xPos, yPos += heightPlus, width, height), "Test Refund"))
        {
            OpenIAB.purchaseProduct("android.test.refunded");
        }

        if (GUI.Button(new Rect(xPos, yPos += heightPlus, width, height), "Test Item Unavailable"))
        {
            OpenIAB.purchaseProduct("android.test.item_unavailable");
        }

        xPos = Screen.width - width - 5.0f;
        yPos = 5.0f;

        if (GUI.Button(new Rect(xPos, yPos, width, height), "Test Purchase Canceled"))
        {
            OpenIAB.purchaseProduct("android.test.canceled");
        }

        if (GUI.Button(new Rect(xPos, yPos += heightPlus, width, height), "Query Inventory"))
        {
            OpenIAB.queryInventory(new string[] { SKU });
        }

        if (GUI.Button(new Rect(xPos, yPos += heightPlus, width, height), "Purchase Real Product"))
        {
            OpenIAB.purchaseProduct(SKU);
        }

        if (GUI.Button(new Rect(xPos, yPos += heightPlus, width, height), "Consume Real Product"))
        {
            OpenIAB.consumeProduct(Purchase.CreateFromSku(SKU));
        }
    }
#endif

#if UNITY_WP8
    void OnGUI()
    {
        if (GUI.Button(new Rect(10, 10, Screen.width * 0.3f, Screen.height * 0.1f), "QUERY INVENTORY"))
        {
            OpenIAB.queryInventory(new string[] { SKU_AMMO, SKU_MEDKIT, SKU_SUBSCRIPTION });
        }
        if (GUI.Button(new Rect(20 + Screen.width * 0.3f, 10, Screen.width * 0.3f, Screen.height * 0.1f), "Purchase"))
        {
            OpenIAB.purchaseProduct(SKU_MEDKIT);
        }
        if (GUI.Button(new Rect(30 + Screen.width * 0.6f, 10, Screen.width * 0.3f, Screen.height * 0.1f), "Consume"))
        {
            OpenIAB.consumeProduct(Purchase.CreateFromSku(SKU_MEDKIT));
        }
        GUI.Label(new Rect(10, 20 + Screen.height * 0.1f, Screen.width, Screen.height), _label);
    }
#endif

    private void billingSupportedEvent()
    {
        _isInitialized = true;
        Debug.Log("billingSupportedEvent");
    }
    private void billingNotSupportedEvent(string error)
    {
        Debug.Log("billingNotSupportedEvent: " + error);
    }
    private void queryInventorySucceededEvent(Inventory inventory)
    {
        Debug.Log("queryInventorySucceededEvent: " + inventory);
        if (inventory != null)
            _label = inventory.ToString();
    }
    private void queryInventoryFailedEvent(string error)
    {
        Debug.Log("queryInventoryFailedEvent: " + error);
        _label = error;
    }
    private void purchaseSucceededEvent(Purchase purchase)
    {
        Debug.Log("purchaseSucceededEvent: " + purchase);
        _label = "PURCHASED:" + purchase.ToString();
    }
    private void purchaseFailedEvent(int errorCode, string errorMessage)
    {
        Debug.Log("purchaseFailedEvent: " + errorMessage);
        _label = "Purchase Failed: " + errorMessage;
    }
    private void consumePurchaseSucceededEvent(Purchase purchase)
    {
        Debug.Log("consumePurchaseSucceededEvent: " + purchase);
        _label = "CONSUMED: " + purchase.ToString();
    }
    private void consumePurchaseFailedEvent(string error)
    {
        Debug.Log("consumePurchaseFailedEvent: " + error);
        _label = "Consume Failed: " + error;
    }
}