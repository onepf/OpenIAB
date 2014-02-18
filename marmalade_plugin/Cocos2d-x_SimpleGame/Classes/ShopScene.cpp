#include "ShopScene.h"
#include "AppDelegate.h"

using namespace cocos2d;

CCScene* ShopScene::scene()
{
	CCScene * scene = NULL;
	do 
	{
		scene = CCScene::create();
		CC_BREAK_IF(! scene);

		ShopScene *layer = ShopScene::create();
		CC_BREAK_IF(! layer);

		scene->addChild(layer);
	} while (0);

	return scene;
}

bool ShopScene::init()
{
	if (! CCLayerColor::initWithColor( ccc4(128, 128, 128, 128) ) )
		return false;

	// Back button
	CCLabelTTF *pLabel = CCLabelTTF::create("Back", "Artial", 32);
	pLabel->setHorizontalAlignment(kCCTextAlignmentRight);
	CCMenuItemLabel *pBackButton = CCMenuItemLabel::create(pLabel, this, menu_selector(ShopScene::backButtonCallback));
	pBackButton->setAnchorPoint(ccp(1.0f, 1.0f));
	pBackButton->setColor(ccc3(255, 255, 255));

	// Buy ammo button
	CCNode *pBuyAmmoLabel = CCLabelTTF::create("Buy Ammo", "Artial", 24);
	pBuyAmmoLabel->setAnchorPoint(ccp(0.5f, 0.5f));
	CCMenuItemLabel *pBuyAmmoButton = CCMenuItemLabel::create(pBuyAmmoLabel, this, menu_selector(ShopScene::buyAmmoCallback));
	pBuyAmmoButton->setColor(ccc3(255, 255, 255));

	// Buy hat
	CCNode *pBuyHatLabel = CCLabelTTF::create("Buy Hat", "Artial", 24);
	pBuyHatLabel->setAnchorPoint(ccp(0.5f, 0.5f));
	CCMenuItemLabel *pBuyHatButton = CCMenuItemLabel::create(pBuyHatLabel, this, menu_selector(ShopScene::buyHatCallback));
	pBuyHatButton->setColor(ccc3(255, 255, 255));

    CCSize visibleSize = CCDirector::sharedDirector()->getVisibleSize();
    CCPoint origin = CCDirector::sharedDirector()->getVisibleOrigin();
        
	pBackButton->setPosition(ccp(origin.x + visibleSize.width - 15,
									origin.y + visibleSize.height - 10));

	pBuyAmmoButton->setPosition(ccp(origin.x + visibleSize.width/2,
									origin.y + visibleSize.height/2 + 40));

	pBuyHatButton->setPosition(ccp(origin.x + visibleSize.width/2,
									origin.y + visibleSize.height/2));

	if (AppDelegate::instance()->isHatPurchased())
		pBuyHatButton->setEnabled(false);

	CCMenu* pMenu = CCMenu::create(pBackButton, pBuyAmmoButton, pBuyHatButton, NULL);
	pMenu->setPosition(CCPointZero);
	if (! pMenu)
		return false;

	this->addChild(pMenu, 1);

	return true;
}

void ShopScene::buyAmmoCallback(CCObject* pSender)
{
	AppDelegate::instance()->purchaseAmmo();
}

void ShopScene::buyHatCallback(CCObject* pSender)
{
	AppDelegate::instance()->purchaseHat();
}

void ShopScene::backButtonCallback(CCObject* pSender)
{
	CCDirector::sharedDirector()->popScene();
}