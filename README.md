OpenIAB - Open In-App Billing
=====

Uploading Android apps to all the existing Android appstores is a painful process and [AppDF](/onepf/AppDF) 
project was designed to make it easier. But what is even more difficult for the developers is 
supporting different in-purchase APIs of different appstores. There are five different In-App Purchase APIs 
already and this number is increasing. We are going to create an open source library that will wrap 
appstore in-app purchase APIs of all the stores and provide an easy way for the developers to develop 
their apps/games in a way that one APK will work in all the stores and automatically use right in-app 
purchase API under each store. Plus we are going to develop an open in-app billing API that stores 
could implement to support all the built APK files using this library.

How OpenIAB Will Work
=====
1. An Android app developer integrates OpenIAB library in his/her Android code 
2. An Android app developer implements in-app purchases using OpenIAB API (which is very close to Google Play IAB API, just few changes in source code will be needed) 
3. OpenIAB Lib detects which appstore installed the app 
4. OpenIAB Lib redirects in-app purchase calls to the corresponding appstore IAB API (OpenIAB Lib wrapps IAB APIs of severall apstores) 
5. All In-App Billing logic is handled by the corresponding appstore, OpenIAB has no code to process in-app purchases and has no UI, it just wrapps In-App Billing APIs of different stores in one library 

<img src="http://www.onepf.org/img/openiabdiagram1.png">

<img src="http://www.onepf.org/img/openiabdiagram2.png">

Current Status
=====
We have just started. We are creating a [sample game](/onepf/OpenIAB/tree/master/samples/life) that supports in-app billing of all existing 
appstores that support in-app purchasing. In the same time, we are designing 
Open In-App Billing API that appstores can use to easily integrate in-app billing functionality.

No Middle Man
=====
OpenIAB is an open source library that wraps the already existing IAB APIs as well as an open API that 
appstores could implement. It is important to understand that all payments are processes directly by 
each store and there is no a middle man staying between the app developers and the appstores. 
OpenIAB will not do payments for the appstores. It is just an API how the apps communicate with 
appstores to request in-app billing. There is a common open API all the stores can use instead of 
each new store implement their own API and developers have to integrate all these different APIs in their apps.

AppStores
=====
The following Android application stores support in-app billing today:
 * Google Play
 * Amazon AppStore
 * Samsung Apps
 * SK-Telecom T-Store  
 * NOOK (via Fortumo)
 
If you know about other Android appstores that support in-app purchasing 
please [let us know](http://groups.google.com/group/opf_openiab).

We are working on integrating their IAB APIs in one OpenIAB library.

How Can I Help?
=====
* If you are an Android app developer check <a href="https://github.com/onepf/OpenIAB/issues?labels=open+tasks&state=open">the list of open tasks</a>, check if any of these tasks is interesting for you, send a message to <a href="http://groups.google.com/group/opf_openiab">OpenIAB mailing list</a> how you want to help. <a href="https://github.com/onepf/OpenIAB">Fork OpenIAB</a> on GitHub. 
* If you are an appstore and already support In-App Billing then most probably we are already working on supporting your API in OpenIAB library, and your help is very welcome since you know your API better than anyone else! Just contact us by <a href="http://groups.google.com/group/opf_openiab">joining OpenIAB mailing list</a>. 
* If you are an appstore and do not support In-App Billing yet but plan to support it then we will be glad working with your on creating a common OpenIAB API and API. <a href="http://groups.google.com/group/opf_openiab">Join OpenIAB mailing list</a> to be involved in OpenIAB API development. 

License
=====
Source code of the OpenIAB library and the samples is available under the terms of the Apache License, Version 2.0:  
http://www.apache.org/licenses/LICENSE-2.0

The OpenIAB API specification and the related texts are available under the terms of the Creative Commons Attribution 2.5 license:  
http://creativecommons.org/licenses/by/2.5/

