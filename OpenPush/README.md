OpenPush
========
Simplest possible push notifications for Unity. 

Platforms
---------
[iOS](https://developer.apple.com/notifications/), 
[Android](http://developer.android.com/google/gcm/index.html), 
[Windows Phone 8](http://msdn.microsoft.com/en-us/library/windowsphone/develop/ff402558%28v=vs.105%29.aspx)

Usage
-----

### Server

All push notification services require a custom server, which sends notification requests.

There is a simple server for your test purposes [here](PushSharp), using [PushSharp](https://github.com/Redth/PushSharp) library. But it could be implemented in any language.

Test server simply accepts following json string in POST, and then adds notification to the queue.

```{"platform":"andorid/ios/wp8", "token":""}``` 


### Client

Here is client side sample code. There is single method for the service setup and device registration.

```
using UnityEngine;
using OnePF.OpenPush;

public class PushNotifications : MonoBehaviour
{
    void OnEnable()
    {
        OpenPush.InitFinished += OpenPush_InitFinished;
    }

    void OnDisable()
    {
        OpenPush.InitFinished -= OpenPush_InitFinished;
    }

    void Start()
    {
        InitParams initParams = new InitParams("http://custom-server-url", "ANDROID_SENDER_ID");
        OpenPush.Init(initParams);
    }

    void OpenPush_InitFinished(bool success, string errorMessage)
    {
        Debug.Log(string.Format("Init Finished: {0}; {1}", success, errorMessage));
    }
}
```

Advanced
--------
To add new platform, you need only to implement ```IOpenPush``` interface and create an instance of your class:
```
static OpenPush()
{
#if UNITY_ANDROID
	_push = new OpenPush_Android();
#elif UNITY_IOS
    ...
#elif UNITY_WP8
    ...
#elif UNITY_SOME_FLAG
    // Your implementation here
    _push = new OpenPush_CustomPlatform();
#else
	...
#endif
}
```
