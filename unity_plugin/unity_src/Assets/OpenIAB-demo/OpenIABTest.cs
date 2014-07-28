/*******************************************************************************
 * Copyright 2012-2014 One Platform Foundation
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 ******************************************************************************/

using UnityEngine;
using OnePF;
using System.Collections.Generic;

/**
 * Example of OpenIAB usage
 */ 
public class OpenIABTest : MonoBehaviour
{
    const string SKU = "sku";
    const string SKU_AMMO = "sku_ammo_general";
    const string SKU_MEDKIT = "sku_medkit_general";
    const string SKU_SUBSCRIPTION = "sku_sub_general";
    const string SKU_INFINITE_AMMO = "sku_inf_ammo_general";
    const string SKU_COWBOY_HAT = "sku_hat_general";

#pragma warning disable 0414
    string _label = "";
    bool _isInitialized = false;
#pragma warning restore 0414

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
        OpenIAB.mapSku(SKU_MEDKIT, OpenIAB_iOS.STORE, "30_real");
        OpenIAB.mapSku(SKU_AMMO, OpenIAB_iOS.STORE, "75_real");
        OpenIAB.mapSku(SKU_INFINITE_AMMO, OpenIAB_iOS.STORE, "noncons_2");
        OpenIAB.mapSku(SKU_COWBOY_HAT, OpenIAB_iOS.STORE, "noncons_1");

        OpenIAB.mapSku(SKU, OpenIAB_iOS.STORE, "sku");

        OpenIAB.mapSku(SKU_AMMO, OpenIAB_WP8.STORE, "sku_ammo");
        OpenIAB.mapSku(SKU_MEDKIT, OpenIAB_WP8.STORE, "sku_medkit");
        OpenIAB.mapSku(SKU_SUBSCRIPTION, OpenIAB_WP8.STORE, "sku_sub");
    }

#if UNITY_ANDROID
    
    const float SCREEN_XY_OFFSET = 5.0f;
    const float Y_OFFSET = 10.0f;
    const int SMALL_SCREEN_SIZE = 800;
    const int LARGE_FONT_SIZE = 34;
    const int SMALL_FONT_SIZE = 34;
    const int LARGE_WIDTH = 380;
    const int SMALL_WIDTH = 160;
    const int LARGE_HEIGHT = 100;
    const int SMALL_HEIGHT = 40;

    private void OnGUI()
    {
        float yPos = SCREEN_XY_OFFSET;
        float xPos = SCREEN_XY_OFFSET;
        GUI.skin.button.fontSize = (Screen.width >= SMALL_SCREEN_SIZE || Screen.height >= SMALL_SCREEN_SIZE) ? LARGE_FONT_SIZE : SMALL_FONT_SIZE;
        float width = (Screen.width >= SMALL_SCREEN_SIZE || Screen.height >= SMALL_SCREEN_SIZE) ? LARGE_WIDTH : SMALL_WIDTH;
        float height = (Screen.width >= SMALL_SCREEN_SIZE || Screen.height >= SMALL_SCREEN_SIZE) ? LARGE_HEIGHT : SMALL_HEIGHT;
        float heightPlus = height + Y_OFFSET;

        if (GUI.Button(new Rect(xPos, yPos, width, height), "Initialize OpenIAB"))
        {
            // Application public key
            var public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqibEPHCtfPm3Rn26gbE6vhCc1d6A072im+oWNlkUAJYV//pt1vCkYLqkkw/P2esPSWaw1nt66650vfVYc3sYY6L782n/C+IvZWQt0EaLrqsSoNfN5VqPhPeGf3wqsOvbKw9YqZWyKL4ddZUzRUPex5xIzjHHm3qIJI5v7iFJHOxOj0bLuEG8lH0Ljt/w2bNe4o0XXoshYDqpzIKmKy6OYNQOs8iBTJlfSmPrlGudmldW6CsuAKeVGm+Z+2xx3Xxsx3eSwEgEaUc1ZsMWSGsV6dXgc3JrUvK23JRJUu8X5Ec1OQLyxL3VelD5f0iKVTJ1kw59tMAVZ7DDpzPggWpUkwIDAQAB";

            var options = new Options();
            options.verifyMode = OptionsVerifyMode.VERIFY_SKIP;
            options.prefferedStoreNames = new string[] { OpenIAB_Android.STORE_GOOGLE };
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

        xPos = Screen.width - width - SCREEN_XY_OFFSET;
        yPos = SCREEN_XY_OFFSET;

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

#if UNITY_IOS
    const int CONTROL_OFFSET = 10;
    const float SCREEN_Y_MULT = 0.1f;
    const float SCREEN_X_MULT = 0.3f;

    void OnGUI()
    {
        if (GUI.Button(new Rect(CONTROL_OFFSET, CONTROL_OFFSET, Screen.width * SCREEN_X_MULT, Screen.height * SCREEN_Y_MULT), "Init"))
        {
            OpenIAB.init(new Options());
        }
        if (GUI.Button(new Rect(CONTROL_OFFSET * 2 + Screen.width * SCREEN_X_MULT, CONTROL_OFFSET, Screen.width * SCREEN_X_MULT, Screen.height * SCREEN_Y_MULT), "Query inventory"))
        {
            OpenIAB.queryInventory(new string[] { SKU_MEDKIT });
        }
        if (GUI.Button(new Rect(CONTROL_OFFSET * 3 + Screen.width * SCREEN_X_MULT * 2, CONTROL_OFFSET, Screen.width * SCREEN_X_MULT, Screen.height * SCREEN_Y_MULT), "Purchase"))
        {
            OpenIAB.purchaseProduct(SKU_MEDKIT);
        }
        GUI.Label(new Rect(CONTROL_OFFSET, CONTROL_OFFSET * 2 + Screen.height * SCREEN_Y_MULT, Screen.width, Screen.height), _label);
    }
#endif

#if UNITY_WP8
    const int BUTTON_OFFSET = 10;
    const float SCREEN_Y_MULT = 0.1f;
    const float SCREEN_X_MULT = 0.3f;

    void OnGUI()
    {
        if (GUI.Button(new Rect(BUTTON_OFFSET, BUTTON_OFFSET, Screen.width * SCREEN_X_MULT, Screen.height * SCREEN_Y_MULT), "QUERY INVENTORY"))
        {
            OpenIAB.queryInventory(new string[] { SKU_AMMO, SKU_MEDKIT, SKU_SUBSCRIPTION });
        }
        if (GUI.Button(new Rect(BUTTON_OFFSET * 2 + Screen.width * SCREEN_X_MULT, BUTTON_OFFSET, Screen.width * SCREEN_X_MULT, Screen.height * SCREEN_Y_MULT), "Purchase"))
        {
            OpenIAB.purchaseProduct(SKU_MEDKIT);
        }
        if (GUI.Button(new Rect(BUTTON_OFFSET * 3 + Screen.width * SCREEN_X_MULT * 2, BUTTON_OFFSET, Screen.width * SCREEN_X_MULT, Screen.height * SCREEN_Y_MULT), "Consume"))
        {
            OpenIAB.consumeProduct(Purchase.CreateFromSku(SKU_MEDKIT));
        }
        GUI.Label(new Rect(BUTTON_OFFSET, BUTTON_OFFSET * 2 + Screen.height * SCREEN_Y_MULT, Screen.width, Screen.height), _label);
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