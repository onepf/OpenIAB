#ifndef  _APP_DELEGATE_H_
#define  _APP_DELEGATE_H_

#include "cocos2d.h"

/**
@brief    The cocos2d Application.

The reason for implement as private inheritance is to hide some interface call by CCDirector.
*/
class  AppDelegate : private cocos2d::CCApplication
{
public:
    AppDelegate();
    virtual ~AppDelegate();

    /**
    @brief    Implement CCDirector and CCScene init code here.
    @return true    Initialize success, app continue.
    @return false   Initialize failed, app terminate.
    */
    virtual bool applicationDidFinishLaunching();

    /**
    @brief  The function be called when the application enter background
    @param  the pointer of the application
    */
    virtual void applicationDidEnterBackground();

    /**
    @brief  The function be called when the application enter foreground
    @param  the pointer of the application
    */
    virtual void applicationWillEnterForeground();

	static inline AppDelegate* instance()
	{
		return (AppDelegate*) CCApplication::sharedApplication();
	}

	// Shop
	void purchaseAmmo();
	void purchaseHat();

	inline bool isHatPurchased() { return _isHatPurchased; }
	inline void setHatPurchased() { _isHatPurchased = true; }

	inline int getAmmo() { return _ammo; }
	inline bool useAmmo() 
	{ 
		if (_ammo == 0)
		{
			return false;
		}
		else
		{
			--_ammo;
			return true;
		}
	}
	inline void addAmmo(int amount) { _ammo += amount; }

protected:
	int _ammo;
	bool _isHatPurchased;
};

#endif // _APP_DELEGATE_H_

