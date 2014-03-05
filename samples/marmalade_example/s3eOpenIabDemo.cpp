/**
* @page This Application demonstrates usage of the Android Google Play Billing Marmalade extension.
* The application is intended to be functional and demonstrate how to use the extension code without the distraction of complex UI.
*/

#include "s3eOpenIab.h"
#include "IwDebug.h"
#include "s3e.h"
#include "IwNUI.h"
#include "IwRandom.h"

#include "ExampleUI.h"
//#include "UnitTests.h"
#include <IwPropertyString.h>
#include "s3eDebug.h"

//UnitTests *gTests;

// Button click handlers for the UI

const char *inAppSkus[] =
{
	"sku_hat_inner",
	"sku_coin_inner"
};

const char *subSkus[] = 
{
	"sku_subs_inner"
};

// Query Shop
bool OnButton1Click(void* data, CButton* button)
{
	ExampleUI *ui = (ExampleUI*)data;
	ui->Log("Query Shop Items");
	s3eOpenIabRequestProductInformation(inAppSkus,sizeof(inAppSkus)/sizeof(const char*),subSkus,sizeof(subSkus)/sizeof(const char*));
	return true;
}

// Restore Purchases
bool OnButton2Click(void* data, CButton* button)
{
	ExampleUI *ui = (ExampleUI*)data;
	ui->Log("Attempting to Restore Purchases");
	s3eOpenIabRestoreTransactions();

	return true;
}

// Purchase a Hat
bool OnButton3Click(void* data, CButton* button)
{
	ExampleUI *ui = (ExampleUI*)data;
	ui->Log("Attempting to purchase Hat");
	string randomPayload = string_format("TestPayload%d",IwRandMinMax(1,10000)); // let's be clear this is a test - in your code either don't set it or use something sensible you can check later on a different device
	s3eOpenIabRequestPurchase(inAppSkus[0],true,randomPayload.c_str());

	return true;
}

string purchasedConsumableID;

// Purchase a Coin
bool OnButton4Click(void* data, CButton* button)
{
	ExampleUI *ui = (ExampleUI*)data;
	ui->Log("Attempting to purchase Coin");
	string randomPayload = string_format("TestPayload%d",IwRandMinMax(1,10000));
	s3eOpenIabRequestPurchase(inAppSkus[1],true,randomPayload.c_str());

	return true;
}

// Purchase a Subscription
bool OnButton5Click(void* data, CButton* button)
{
	ExampleUI *ui = (ExampleUI*)data;
	ui->Log("Attempting to purchase Subscription");
	string randomPayload = string_format("TestPayload%d",IwRandMinMax(1,10000));
	s3eOpenIabRequestPurchase(subSkus[0],false,randomPayload.c_str());

	return true;
}

void ConsumeCoin(void* data)
{
	ExampleUI *ui = (ExampleUI*)data;
	ui->Log("Attempting to consume Coin");
	if (purchasedConsumableID.length() == 0)
		ui->Log("Error: no item to consume, try Restore if you restarted the Example app");
	else
		s3eOpenIabConsumeItem(purchasedConsumableID.c_str());
}

int32 ListCallback(void *systemData,void *userData)
{
	if ((systemData) && (userData))
	{
		ExampleUI* ui = (ExampleUI*)userData; // this is a pointer passed through from when the callback was registered
		s3eOpenIabSkuResponse *skus = (s3eOpenIabSkuResponse*)systemData;
		string str;
		if (skus->m_ErrorMsg) {
			str = string_format("List Sku returned : %d, %s", (int)skus->m_Status, skus->m_ErrorMsg);
			ui->Log(str);
		}
		if (skus->m_Status == S3E_OPENIAB_RESULT_OK)
		{
			str = string_format("%d items returned",skus->m_NumProducts);
			ui->Log(str);
			for (int i=0;i<skus->m_NumProducts;i++)
			{
				ui->Log("{");
				s3eOpenIabItemInfo *item = &skus->m_Products[i];
				ui->Log(string_format("	m_ProductID		: %s",item->m_ProductID));
				ui->Log(string_format("	m_Type			: %s",item->m_Type));
				ui->Log(string_format("	m_Price			: %s",item->m_Price));
				ui->Log(string_format("	m_Title			: %s",item->m_Title));
				ui->Log(string_format("	m_Description	: %s",item->m_Description));
				ui->Log("}");
			}
		}
	}
	return true;
}

int32 RestoreCallback(void *systemData,void *userData)
{
	if ((systemData) && (userData))
	{
		ExampleUI* ui = (ExampleUI*)userData; // this is a pointer passed through from when the callback was registered
		s3eOpenIabRestoreResponse *rr = (s3eOpenIabRestoreResponse*)systemData;
		string str;
		if (rr->m_ErrorMsg) {
			str = string_format("Restore returned : %d, %s", (int)rr->m_Status, rr->m_ErrorMsg);
			ui->Log(str);
		}
		if (rr->m_Status == S3E_OPENIAB_RESULT_OK)
		{
			str = string_format("%d items returned",rr->m_NumPurchases);
			ui->Log(str);
			for (int i=0;i<rr->m_NumPurchases;i++)
			{
				ui->Log("{");
				s3eOpenIabPurchase *item = &rr->m_Purchases[i];
				ui->Log(string_format("	m_OrderID			: %s",item->m_OrderID));
				ui->Log(string_format("	m_PackageID			: %s",item->m_PackageID));
				ui->Log(string_format("	m_ProductId			: %s",item->m_ProductId));
				ui->Log(string_format("	m_PurchaseTime		: %d",item->m_PurchaseTime));
				ui->Log(string_format("	m_PurchaseState		: %d",item->m_PurchaseState));
				ui->Log(string_format("	m_PurchaseToken		: %s",item->m_PurchaseToken));				
				ui->Log(string_format("	m_DeveloperPayload	: %s",item->m_DeveloperPayload));				
				ui->Log(string_format("	m_JSON				: %s",item->m_JSON));
				ui->Log(string_format("	m_Signature			: %s",item->m_Signature));
				ui->Log("}");

				if (strcmp(inAppSkus[0], item->m_ProductId) == 0)
				{
					ui->buttons[2]->SetAttribute("enabled", false);
				} 
				else if (strcmp(inAppSkus[1], item->m_ProductId) == 0)
				{
					purchasedConsumableID = item->m_PurchaseToken;
					ui->SetConsumableText(purchasedConsumableID);
					ui->buttons[3]->SetAttribute("enabled", false);
					ConsumeCoin(userData);
				} 
				else if (strcmp(subSkus[0], item->m_ProductId) == 0) 
				{
					ui->buttons[4]->SetAttribute("enabled", false);
				}
			}
		}
	}
	return true;
}

int32 PurchaseCallback(void *systemData,void *userData)
{
	if ((systemData) && (userData))
	{
		ExampleUI* ui = (ExampleUI*)userData; // this is a pointer passed through from when the callback was registered
		s3eOpenIabPurchaseResponse *pr = (s3eOpenIabPurchaseResponse*)systemData;
		string str;
		if (pr->m_ErrorMsg) {
			str = string_format("Purchase returned : %d, %s", (int)pr->m_Status, pr->m_ErrorMsg);
			ui->Log(str);
		}
		if (pr->m_Status == S3E_OPENIAB_RESULT_OK)
		{
			s3eOpenIabPurchase *item = pr->m_PurchaseDetails;
			ui->Log(string_format("	m_OrderID			: %s",item->m_OrderID));
			ui->Log(string_format("	m_PackageID			: %s",item->m_PackageID));
			ui->Log(string_format("	m_ProductId			: %s",item->m_ProductId));
			ui->Log(string_format("	m_PurchaseTime		: %d",item->m_PurchaseTime));
			ui->Log(string_format("	m_PurchaseState		: %d",item->m_PurchaseState));
			ui->Log(string_format("	m_PurchaseToken		: %s",item->m_PurchaseToken));				
			ui->Log(string_format("	m_DeveloperPayload	: %s",item->m_DeveloperPayload));				
			ui->Log(string_format("	m_JSON				: %s",item->m_JSON));
			ui->Log(string_format("	m_Signature			: %s",item->m_Signature));

			if (strcmp(inAppSkus[0], item->m_ProductId) == 0)
			{
				ui->buttons[2]->SetAttribute("enabled", false);
			} 
			else if (strcmp(inAppSkus[1], item->m_ProductId) == 0)
			{
				purchasedConsumableID = item->m_PurchaseToken;
				ui->SetConsumableText(purchasedConsumableID);
				ui->buttons[3]->SetAttribute("enabled", false);
				ConsumeCoin(userData);
			} 
			else if (strcmp(subSkus[0], item->m_ProductId) == 0) 
			{
				ui->buttons[4]->SetAttribute("enabled", false);
			}
		}
	}
	return true;
}

int32 ConsumeCallback(void *systemData,void *userData)
{
	if ((systemData) && (userData))
	{
		ExampleUI* ui = (ExampleUI*)userData; // this is a pointer passed through from when the callback was registered
		s3eOpenIabConsumeResponse *cr = (s3eOpenIabConsumeResponse*)systemData;
		string str;
		if (cr->m_ErrorMsg) {
			str = string_format("Purchase returned : %d, %s", (int)cr->m_Status, cr->m_ErrorMsg);
			ui->Log(str);
		}
		if (cr->m_Status == S3E_OPENIAB_RESULT_OK) {
			ui->SetConsumableText("None");
		}
		ui->buttons[3]->SetAttribute("enabled", true);
	}	
	return true;
}

// note this is the public license key provided by Google, not the one you sign this app with, it's in the developer console under Services & APIs

const char *publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApe2d3t+F+v1C5OUGPNW2+au8kyf2jAeK9QDg1MpFKgk9x+zh/ye/Y7JAv3gCVPb31CWTemywQLLRosm/DmqOFkPqMuexyKzm72X9cKbLupUI3iPXyASKh92R2+/p25iFRLKdWijh/ESkk0ii3PDBg3RIqjswlYiQ7g4SQ3YeVw+sOTyTIGik+yqz8bR7WEKbbFdsGMTUFwPgn273itimYMQ7vH6WtkCTzo5z1+Ab8DIygx6hCqKCvKOEDRBFB8/rJ7pl4jJsCsQHV5Q0x98fBN0Y9+jtKD0+M5Sm/u2+kNIukFF4khbBhdqisfL/8bbTsxE68tYh2GWWv10MqSv7JQIDAQAB";
//const char *publicKey = "Put your public key here / if you dont do this / payments will succeed but the extension will still report them as having failed / also this string needs to be Base64 clean";

int main()
{
	char animatingText[] = "... Some Animating Text ...";
	uint64 animatingTextTimer;

	// seed RNG
	int32 ms = (int32)s3eTimerGetMs();
	IwRandSeed(ms);

	// create our Marmalade UI interface
	ExampleUI *ui = new ExampleUI(); 
	ui->Log("main()");
	//ui->EnableAllButtons(false);

	s3eOpenIabStoreNames* storeNames = s3eOpenIabGetStoreNames();

	// Map SKUs
	s3eOpenIabMapSku(inAppSkus[0], storeNames->m_GooglePlay, "sku_hat");
	s3eOpenIabMapSku(inAppSkus[1], storeNames->m_GooglePlay, "sku_coin");
	s3eOpenIabMapSku(subSkus[0], storeNames->m_GooglePlay, "sku_subs");


	// Options
	s3eOpenIabOptions* options = new s3eOpenIabOptions();

	options->m_NumStores = 1;
	options->m_StoreNames = new const char*[options->m_NumStores];
	options->m_StoreKeys = new const char*[options->m_NumStores];
	options->m_StoreNames[0] = storeNames->m_GooglePlay;
	options->m_StoreKeys[0] = publicKey;

	options->m_VerifyMode = VERIFY_SKIP;

	options->m_CheckInventory = false;

	options->m_CheckInventoryTimeoutMs = 20000;

	options->m_DiscoveryTimeoutMs = 10000;

	options->m_NumPrefferedStoreNames = 1;
	options->m_PrefferedStoreNames = new const char*[options->m_NumPrefferedStoreNames];
	options->m_PrefferedStoreNames[0] = storeNames->m_GooglePlay;

	options->m_SamsungCertificationRequestCode = REQUEST_CODE_IS_IAP_PAYMENT;

	// Start up the Store interface
	s3eOpenIabStart(options);

	IwTrace(ANDROIDGOOGLEPLAYBILLING_VERBOSE, ("LOG ENABLED: %d", (int) s3eOpenIabIsDebugLog()));
	s3eOpenIabEnableDebugLogging(false);
	s3eOpenIabEnableDebugLogging(true);

	s3eOpenIabEnableDebugTagLogging(false, "MY_TAG");
	s3eOpenIabEnableDebugTagLogging(true, "MY_TAG");
	IwTrace(ANDROIDGOOGLEPLAYBILLING_VERBOSE, ("LOG ENABLED: %d", (int) s3eOpenIabIsDebugLog()));

	// register callbacks and pass in our UI pointer which the callback
	s3eOpenIabRegister(S3E_OPENIAB_LIST_PRODUCTS_CALLBACK,ListCallback,ui); 
	s3eOpenIabRegister(S3E_OPENIAB_RESTORE_CALLBACK,RestoreCallback,ui); 
	s3eOpenIabRegister(S3E_OPENIAB_PURCHASE_CALLBACK,PurchaseCallback,ui); 
	s3eOpenIabRegister(S3E_OPENIAB_CONSUME_CALLBACK,ConsumeCallback,ui);

	ui->SetStatusText((s3eOpenIabIsSupported())?"Initialised":"Unitialised");

	// create the Unit Test singleton
	//gTests = new UnitTests(ui); // DH: Not implemented for this extension yet

	animatingTextTimer = s3eTimerGetMs();

	// run the app
	while (1)
	{
		//gTests->Update(); // update the tests if they're running

		//s3eOpenIabIsSupported()

		// animate the text
		if (s3eTimerGetMs() > animatingTextTimer + 20)
		{
			int len = strlen(animatingText);
			char c = animatingText[0];
			memmove(animatingText,animatingText+1,len-1);
			animatingText[len-1] = c;
			ui->SetAnimatingText(animatingText);
			animatingTextTimer = s3eTimerGetMs();
		}

		//ui->SetStatusText((s3eOpenIabIsSupported())?"Initialised":"Unitialised"); // annoying log spam
		ui->Update(); // update the UI
		s3eDeviceYield();
	}

	return 0;
}
