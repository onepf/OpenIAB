#include "AppDelegate.h"
#include "HelloWorldScene.h"
#include "s3eOpenIab.h"

USING_NS_CC;

const char *sku[] =
{
	"sku_ammo",
	"sku_hat"
};

const char *publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsuZkS7TaJkmAKHfIdmBKqphBc6ydmYHGC7/CBRXS8SGTYYkSEi4SRvx1U+iUzauuRWHcHlAjeGTaLBfgGH73nQ84huwzsXOClCv2iMaDTrmVJ1vg9HU1fva+Q9AlrIOWv7pnGSa57jHEqZM3/HF1gEu8dq1VHhxd2DD4cSEy8/kMMbEtDjvx+w7OAsyfQV7pW6OzHln/vR/bAxtDUIXyxcHKVaKjXzykBK6VvGlNRV798G6GpARbugNhH3imXjiA2BcluyPNG8NnCeb+Z9Z0NJdJIinLIWo1wW5F4u8BeBgQNRgmqIt8Uk1qAudQ+cSxn491sJG5ROcx5yKP3KL/9QIDAQAB";

const char *purchasedConsumableID;

void ConsumeAmmo()
{
	CCLOG("Attempting to consume Ammo");
	if (strcmp(purchasedConsumableID, "") == 0)
	{
		CCLOG("Error: no item to consume");
	}
	else
	{
		s3eOpenIabConsumeItem(purchasedConsumableID);
		AppDelegate::instance()->addAmmo(5);
	}
}

int32 ListCallback(void *systemData, void *userData)
{
	//if ((systemData) && (userData))
	//{
	//	ExampleUI* ui = (ExampleUI*)userData; // this is a pointer passed through from when the callback was registered
	//	s3eOpenIabSkuResponse *skus = (s3eOpenIabSkuResponse*) systemData;
	//	string str;
	//	if (skus->m_ErrorMsg) {
	//		str = string_format("List Sku returned : %d, %s", (int)skus->m_Status, skus->m_ErrorMsg);
	//		ui->Log(str);
	//	}
	//	if (skus->m_Status == S3E_OPENIAB_RESULT_OK)
	//	{
	//		str = string_format("%d items returned",skus->m_NumProducts);
	//		ui->Log(str);
	//		for (int i=0;i<skus->m_NumProducts;i++)
	//		{
	//			ui->Log("{");
	//			s3eOpenIabItemInfo *item = &skus->m_Products[i];
	//			ui->Log(string_format("	m_ProductID		: %s",item->m_ProductID));
	//			ui->Log(string_format("	m_Type			: %s",item->m_Type));
	//			ui->Log(string_format("	m_Price			: %s",item->m_Price));
	//			ui->Log(string_format("	m_Title			: %s",item->m_Title));
	//			ui->Log(string_format("	m_Description	: %s",item->m_Description));
	//			ui->Log("}");
	//		}
	//	}
	//}
	return true;
}

int32 RestoreCallback(void *systemData, void *userData)
{
	if ((systemData) && (userData))
	{
		s3eOpenIabRestoreResponse *rr = (s3eOpenIabRestoreResponse*)systemData;
		if (rr->m_ErrorMsg) 
		{
			CCLOG("Restore returned : %d, %s", (int)rr->m_Status, rr->m_ErrorMsg);
		}
		if (rr->m_Status == S3E_OPENIAB_RESULT_OK)
		{
			CCLOG("%d items returned",rr->m_NumPurchases);
			for (int i=0; i< rr->m_NumPurchases; ++i)
			{
				s3eOpenIabPurchase *item = &rr->m_Purchases[i];
				if (strcmp(sku[0], item->m_ProductId) == 0)
				{
					ConsumeAmmo();
				} 
				else if (strcmp(sku[1], item->m_ProductId) == 0)
				{
					AppDelegate::instance()->setHatPurchased();
				}
			}
		}
	}
	
	// Create a scene. it's an autorelease object
	CCScene *pScene = HelloWorld::scene();
	// Run game
	CCDirector::sharedDirector()->runWithScene(pScene);
	
	return true;
}

int32 PurchaseCallback(void *systemData, void *userData)
{
	if ((systemData) && (userData))
	{
		s3eOpenIabPurchaseResponse *pr = (s3eOpenIabPurchaseResponse*)systemData;
		
		if (pr->m_ErrorMsg) {
			CCLOG("Purchase returned : %d, %s", (int)pr->m_Status, pr->m_ErrorMsg);
		}
		if (pr->m_Status == S3E_OPENIAB_RESULT_OK)
		{
			s3eOpenIabPurchase *item = pr->m_PurchaseDetails;

			if (strcmp(sku[0], item->m_ProductId) == 0)
			{
				purchasedConsumableID = item->m_PurchaseToken;
				ConsumeAmmo();
			} 
			else if (strcmp(sku[1], item->m_ProductId) == 0)
			{
				AppDelegate::instance()->setHatPurchased();
			}
		}
	}
	return true;
}

int32 ConsumeCallback(void *systemData, void *userData)
{
	if ((systemData) && (userData))
	{
		s3eOpenIabConsumeResponse *cr = (s3eOpenIabConsumeResponse*)systemData;
		AppDelegate* game = (AppDelegate*) userData;
		if (cr->m_ErrorMsg) {
			CCLOG("Purchase returned : %d, %s", (int)cr->m_Status, cr->m_ErrorMsg);
		}
		if (cr->m_Status == S3E_OPENIAB_RESULT_OK) {
			game->addAmmo(5);
		}
	}
	return true;
}

AppDelegate::AppDelegate() : _isHatPurchased(false)
{
}

AppDelegate::~AppDelegate() 
{
	// Store ammo value
	CCUserDefault::sharedUserDefault()->setIntegerForKey("ammo", _ammo);
	CCUserDefault::sharedUserDefault()->flush();

	// Stop billing service
	s3eOpenIabStop();
}

bool AppDelegate::applicationDidFinishLaunching() {
	// Restore ammo value
	_ammo = CCUserDefault::sharedUserDefault()->getIntegerForKey("ammo", 10);

    // initialize director
    CCDirector *pDirector = CCDirector::sharedDirector();
    
    pDirector->setOpenGLView(CCEGLView::sharedOpenGLView());
    
    CCSize screenSize = CCEGLView::sharedOpenGLView()->getFrameSize();
    CCSize designSize = CCSizeMake(480, 320);
    std::vector<std::string> searchPaths;
    
    if (screenSize.height > 320)
    {
        searchPaths.push_back("hd");
        searchPaths.push_back("sd");
        pDirector->setContentScaleFactor(640.0f/designSize.height);
    }
    else
    {
        searchPaths.push_back("sd");
        pDirector->setContentScaleFactor(320.0f/designSize.height);
    }
    
    CCFileUtils::sharedFileUtils()->setSearchPaths(searchPaths);
    
#if (CC_TARGET_PLATFORM == CC_PLATFORM_WINRT) || (CC_TARGET_PLATFORM == CC_PLATFORM_WP8)
    CCEGLView::sharedOpenGLView()->setDesignResolutionSize(designSize.width, designSize.height, kResolutionShowAll);
#else
	CCEGLView::sharedOpenGLView()->setDesignResolutionSize(designSize.width, designSize.height, kResolutionNoBorder);
#endif

    // turn on display FPS
    pDirector->setDisplayStats(true);

    // set FPS. the default value is 1.0/60 if you don't call this
    pDirector->setAnimationInterval(1.0 / 60);

	// Get store names
	s3eOpenIabStoreNames* storeNames = s3eOpenIabGetStoreNames();

	// Map SKUs - example
	s3eOpenIabMapSku(sku[0], storeNames->m_GooglePlay, "sku_ammo");

	// Options
	s3eOpenIabOptions* options = new s3eOpenIabOptions();
	
	options->m_NumStores = 1;
	options->m_StoreNames = new const char*[options->m_NumStores];
	options->m_StoreKeys = new const char*[options->m_NumStores];
	options->m_StoreNames[0] = storeNames->m_GooglePlay;
	options->m_StoreKeys[0] = publicKey;

	// Start billing service
	s3eOpenIabStart(options);

	// Register billing callbacks
	s3eOpenIabRegister(S3E_OPENIAB_RESTORE_CALLBACK, RestoreCallback, this);
	s3eOpenIabRegister(S3E_OPENIAB_LIST_PRODUCTS_CALLBACK, ListCallback, this); 
	s3eOpenIabRegister(S3E_OPENIAB_PURCHASE_CALLBACK, PurchaseCallback, this); 
	s3eOpenIabRegister(S3E_OPENIAB_CONSUME_CALLBACK, ConsumeCallback, this);

	if (s3eOpenIabAvailable())
		s3eOpenIabRestoreTransactions();
	else
	{
		// Create a scene. it's an autorelease object
		CCScene *pScene = HelloWorld::scene();
		// Run game
		pDirector->runWithScene(pScene);
	}

    return true;
}

// This function will be called when the app is inactive. When comes a phone call,it's be invoked too
void AppDelegate::applicationDidEnterBackground() 
{
    CCDirector::sharedDirector()->stopAnimation();

    // if you use SimpleAudioEngine, it must be pause
    // CocosDenshion::SimpleAudioEngine::sharedEngine()->pauseBackgroundMusic();
}

// this function will be called when the app is active again
void AppDelegate::applicationWillEnterForeground() 
{
    CCDirector::sharedDirector()->startAnimation();

    // if you use SimpleAudioEngine, it must resume here
    // CocosDenshion::SimpleAudioEngine::sharedEngine()->resumeBackgroundMusic();
}

void AppDelegate::purchaseAmmo()
{
	s3eOpenIabRequestPurchase(sku[0], true);
}

void AppDelegate::purchaseHat()
{
	s3eOpenIabRequestPurchase(sku[1], true);
}