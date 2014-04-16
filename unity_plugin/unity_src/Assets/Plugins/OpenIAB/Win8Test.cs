using UnityEngine;
using System.Collections;
using OnePF;

public class Win8Test : MonoBehaviour
{
    string _label;

    void Start()
    {
        OpenIABEventManager.queryInventorySucceededEvent += OpenIABEventManager_queryInventorySucceededEvent;
    }

    void OpenIABEventManager_queryInventorySucceededEvent(Inventory inventory)
    {
        _label = inventory.ToString();
    }

    void Store_PurchaseFailed(string error)
    {
        _label = error;
    }

    void Store_PurchaseSucceeded(string sku, string payload)
    {
        _label = sku;
    }

    void OnGUI()
    {
        if (GUILayout.Button("BUY SOMETHING"))
        {
            OpenIAB.purchaseProduct("SKU", "payload");
        }
        GUILayout.Label(_label);
    }
}
