#ifndef _SHOP_SCENE_H_
#define _SHOP_SCENE_H_

#include "cocos2d.h"

class ShopScene : public cocos2d::CCLayerColor
{
public:
    ShopScene() {};
	~ShopScene() {};
    virtual bool init();

	static cocos2d::CCScene* scene();
    
	virtual void backButtonCallback(cocos2d::CCObject* pSender);
	virtual void buyAmmoCallback(cocos2d::CCObject* pSender);
	virtual void buyHatCallback(cocos2d::CCObject* pSender);

	CREATE_FUNC(ShopScene);
};

#endif // _SHOP_SCENE_H_
