/*
 * android-specific implementation of the s3eOpenIab extension.
 * Add any platform-specific functionality here.
 */
/*
 * NOTE: This file was originally written by the extension builder, but will not
 * be overwritten (unless --force is specified) and is intended to be modified.
 */
#include "s3eOpenIab_internal.h"

#include "s3eEdk.h"
#include "s3eEdk_android.h"
#include <jni.h>
#include "IwDebug.h"

static jobject g_Obj;
static jmethodID g_s3eOpenIabStart;
static jmethodID g_s3eOpenIabStop;
static jmethodID g_s3eOpenIabIsSupported;
static jmethodID g_s3eOpenIabRequestPurchase;
static jmethodID g_s3eOpenIabRequestProductInformation;
static jmethodID g_s3eOpenIabRestoreTransactions;
static jmethodID g_s3eOpenIabConsumeItem;
static jmethodID g_s3eOpenIabIsDebugLog;
static jmethodID g_s3eOpenIabEnableDebugLogging;
static jmethodID g_s3eOpenIabEnableDebugTagLogging;
static s3eOpenIabStoreNames* g_storeNames = NULL;

void JNICALL s3e_OPENIAB_PURCHASE_CALLBACK( JNIEnv* env,  jobject obj, jint status, jstring errorMsg, jobject purchaseData);
void JNICALL s3e_OPENIAB_LIST_PRODUCTS_CALLBACK( JNIEnv* env,  jobject obj, jint status, jstring errorMsg, jobjectArray products);
void JNICALL s3e_OPENIAB_RESTORE_CALLBACK( JNIEnv* env,  jobject obj, jint status, jstring errorMsg, jobjectArray purchases);
void JNICALL s3e_OPENIAB_CONSUME_CALLBACK( JNIEnv* env,  jobject obj, jint status, jstring errorMsg);

static char* JStringToChar( JNIEnv* env, jstring str )
{
	if( str )
	{
		jsize len = env->GetStringUTFLength( str );

		const char* utf=env->GetStringUTFChars( str, NULL );
		char* retval = new char[ len + 1 ];
		memcpy( retval, utf, len + 1 );
		env->ReleaseStringUTFChars( str, utf );
		
		return retval;
	}
	return NULL;
}

static char* GetStaticJavaString(JNIEnv* env, jclass cls, const char* name)
{
	jfieldID fid = env->GetStaticFieldID(cls, name, "Ljava/lang/String;");
	if (fid == 0)
		return NULL;
	jstring jstr = (jstring) env->GetStaticObjectField(cls, fid);
	return JStringToChar(env, jstr);
}

s3eOpenIabStoreNames* s3eOpenIabGetStoreNames_platform()
{
	if (g_storeNames != NULL)
		return g_storeNames;

	g_storeNames = new s3eOpenIabStoreNames();

	JNIEnv* env = s3eEdkJNIGetEnv();
	
	// Get shop name constants
	jclass cls = s3eEdkAndroidFindClass("org/onepf/oms/OpenIabHelper");
	if (!cls)
		goto fail;
	
	g_storeNames->m_GooglePlay = GetStaticJavaString(env, cls, "NAME_GOOGLE");
	if (g_storeNames->m_GooglePlay == NULL)
		goto fail;
	
	g_storeNames->m_Amazon = GetStaticJavaString(env, cls, "NAME_AMAZON");
	if (g_storeNames->m_Amazon == NULL)
		goto fail;
	
	g_storeNames->m_Tizen = GetStaticJavaString(env, cls, "NAME_TSTORE");
	if (g_storeNames->m_Tizen == NULL)
		goto fail;
	
	g_storeNames->m_Samsung = GetStaticJavaString(env, cls, "NAME_SAMSUNG");
	if (g_storeNames->m_Samsung == NULL)
		goto fail;
	
	return g_storeNames;
	
fail:
    jthrowable exc = env->ExceptionOccurred();
    if (exc)
    {
        env->ExceptionDescribe();
        env->ExceptionClear();
        IwTrace(s3eOpenIab, ("s3eOpenIabGetStoreNames: One or more java string constants could not be found"));
    }
    return g_storeNames;
}

void s3eOpenIabMapSku_platform(const char* sku, const char* storeName, const char* storeSku)
{
	JNIEnv* env = s3eEdkJNIGetEnv();

	jmethodID mapSkuMethodID;
	jclass cls = s3eEdkAndroidFindClass("org/onepf/oms/OpenIabHelper");
	if (!cls)
		goto fail;

    mapSkuMethodID = env->GetStaticMethodID(cls, "mapSku", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
    if (!mapSkuMethodID)
    	goto fail;

    env->CallStaticVoidMethod(cls, mapSkuMethodID, 
    	env->NewStringUTF(sku),
    	env->NewStringUTF(storeName),
    	env->NewStringUTF(storeSku));

    return;

fail:
    jthrowable exc = env->ExceptionOccurred();
    if (exc)
    {
        env->ExceptionDescribe();
        env->ExceptionClear();
        IwTrace(s3eOpenIab, ("s3eOpenIabMapSku: java class or method could not be found"));
    }
}

s3eResult s3eOpenIabInit_platform()
{
    // Get the environment from the pointer
    JNIEnv* env = s3eEdkJNIGetEnv();
    jobject obj = NULL;
    jmethodID cons = NULL;
	
	// Get the extension class
    jclass cls = s3eEdkAndroidFindClass("org/onepf/marmalade/s3eOpenIab/s3eOpenIab");
    if (!cls)
        goto fail;

    // Get its constructor
    cons = env->GetMethodID(cls, "<init>", "()V");
    if (!cons)
        goto fail;
		
    // Construct the extension java class
    obj = env->NewObject(cls, cons);
    if (!obj)
        goto fail;

    // Get all the extension methods
    g_s3eOpenIabStart = env->GetMethodID(cls, "s3eOpenIabStart", "(Lorg/onepf/oms/OpenIabHelper$Options;)I");
    if (!g_s3eOpenIabStart)
        goto fail;

    g_s3eOpenIabStop = env->GetMethodID(cls, "s3eOpenIabStop", "()V");
    if (!g_s3eOpenIabStop)
        goto fail;

    g_s3eOpenIabIsSupported = env->GetMethodID(cls, "s3eOpenIabIsSupported", "()I");
    if (!g_s3eOpenIabIsSupported)
        goto fail;

    g_s3eOpenIabRequestPurchase = env->GetMethodID(cls, "s3eOpenIabRequestPurchase", "(Ljava/lang/String;ZLjava/lang/String;)V");
    if (!g_s3eOpenIabRequestPurchase)
        goto fail;

    g_s3eOpenIabRequestProductInformation = env->GetMethodID(cls, "s3eOpenIabRequestProductInformation", "([Ljava/lang/String;[Ljava/lang/String;)V");
    if (!g_s3eOpenIabRequestProductInformation)
        goto fail;

    g_s3eOpenIabRestoreTransactions = env->GetMethodID(cls, "s3eOpenIabRestoreTransactions", "()V");
    if (!g_s3eOpenIabRestoreTransactions)
        goto fail;

    g_s3eOpenIabConsumeItem = env->GetMethodID(cls, "s3eOpenIabConsumeItem", "(Ljava/lang/String;)V");
    if (!g_s3eOpenIabConsumeItem)
        goto fail;

    g_s3eOpenIabIsDebugLog = env->GetMethodID(cls, "s3eOpenIabIsDebugLog", "()Z");
    if (!g_s3eOpenIabIsDebugLog)
    	goto fail;

    g_s3eOpenIabEnableDebugLogging = env->GetMethodID(cls, "s3eOpenIabEnableDebugLogging", "(Z)V");
    if (!g_s3eOpenIabEnableDebugLogging)
    	goto fail;

    g_s3eOpenIabEnableDebugTagLogging = env->GetMethodID(cls, "s3eOpenIabEnableDebugTagLogging", "(ZLjava/lang/String;)V");
    if (!g_s3eOpenIabEnableDebugTagLogging)
    	goto fail;

	// Non-autogenerated - register the native hooks
    {
        static const JNINativeMethod methods[]=
        {
			{"native_PURCHASE_CALLBACK",			"(ILjava/lang/String;Lorg/onepf/marmalade/s3eOpenIab/s3eOpenIab$S3eBillingPurchase;)V",								(void*)s3e_OPENIAB_PURCHASE_CALLBACK},		
			{"native_LIST_PRODUCTS_CALLBACK",		"(ILjava/lang/String;[Lorg/onepf/marmalade/s3eOpenIab/s3eOpenIab$S3eBillingItemInfo;)V",							(void*)s3e_OPENIAB_LIST_PRODUCTS_CALLBACK},
			{"native_RESTORE_CALLBACK",				"(ILjava/lang/String;[Lorg/onepf/marmalade/s3eOpenIab/s3eOpenIab$S3eBillingPurchase;)V",							(void*)s3e_OPENIAB_RESTORE_CALLBACK},
			{"native_CONSUME_CALLBACK",				"(ILjava/lang/String;)V",																							(void*)s3e_OPENIAB_CONSUME_CALLBACK},
        };
        jint ret = env->RegisterNatives(cls, methods, sizeof(methods)/sizeof(methods[0]));
		if (ret)
		{
			IwTrace(s3eOpenIab, ("s3eOpenIab RegisterNatives failed error:%d",ret));
            goto fail;
		}
    }

    IwTrace(s3eOpenIab, ("s3eOpenIab init success"));
    g_Obj = env->NewGlobalRef(obj);
    env->DeleteLocalRef(obj);
    env->DeleteGlobalRef(cls);

    // Add any platform-specific initialisation code here
    return S3E_RESULT_SUCCESS;

fail:
    jthrowable exc = env->ExceptionOccurred();
    if (exc)
    {
        env->ExceptionDescribe();
        env->ExceptionClear();
        IwTrace(s3eOpenIab, ("s3eOpenIab: One or more java methods could not be found"));
    }
    return S3E_RESULT_ERROR;

}

/** Some helper functions to handle potentially large arrays of strings */

struct s3eAndroidJNIFrame
{
    JNIEnv* env;
	
    s3eAndroidJNIFrame(JNIEnv* env,jint capacity)
    :env(env)
    {
        env->PushLocalFrame(capacity);
    }
    ~s3eAndroidJNIFrame()
    {
        env->PopLocalFrame(NULL);
    }
    JNIEnv* operator->()
    {
        return env;
    }
    JNIEnv* operator()()
    {
        return env;
    }
};

static jobjectArray makeStringArray(const char** strings,int n)
{
    //the parent holds an s3eAndroidJNIFrame
    JNIEnv* env=s3eEdkJNIGetEnv();
    jobjectArray j_strings=env->NewObjectArray(n,env->FindClass("java/lang/String"),NULL);
    for(int i=0;i<n;++i)
    {
        env->SetObjectArrayElement(j_strings,i,env->NewStringUTF(strings[i]));
    }
    return j_strings;
}

void s3eOpenIabTerminate_platform()
{
    // Add any platform-specific termination code here
}

void s3eOpenIabStart_platform(s3eOpenIabOptions* openIabOptions)
{
    JNIEnv* env = s3eEdkJNIGetEnv();
    
	jfieldID fid = NULL;
	jmethodID mid = NULL;
	
	// Options
	jclass optionsClass = NULL;
	jobject options = NULL;
	jmethodID optionsConstructor = NULL;
	
	// storeKeys HashMap
	jclass mapClass;
	jsize mapLen;
	jmethodID mapClassConstructor;
	jobject hashMap;
	jmethodID hashMapPut;
	
	// Get Options class
	optionsClass = s3eEdkAndroidFindClass("org/onepf/oms/OpenIabHelper$Options");
	if (!optionsClass)
		goto fail;
		
	// Get Options class constructor
	optionsConstructor = env->GetMethodID(optionsClass, "<init>", "()V");
	if (!optionsConstructor)
		goto fail;
	
	// Construct Options instance
	options = env->NewObject(optionsClass, optionsConstructor);
	if (!options)
		goto fail;

	// Options.discoveryTimeoutMs
	if (openIabOptions->m_DiscoveryTimeoutMs > 0)
	{
		fid = env->GetFieldID(optionsClass, "discoveryTimeoutMs", "I");
		if (fid == 0) goto fail;
		env->SetIntField(options, fid, (jint) openIabOptions->m_DiscoveryTimeoutMs);
	}

	// Options.checkInventory
	if (!openIabOptions->m_CheckInventory)
	{
		fid = env->GetFieldID(optionsClass, "checkInventory", "Z");
		if (fid == 0) goto fail;
		env->SetBooleanField(options, fid, openIabOptions->m_CheckInventory);
	}

	// Options.checkInventoryTimeoutMs
	if (openIabOptions->m_CheckInventoryTimeoutMs > 0)
	{
		fid = env->GetFieldID(optionsClass, "checkInventoryTimeoutMs", "I");
		if (fid == 0) goto fail;
		env->SetIntField(options, fid, (jint) openIabOptions->m_CheckInventoryTimeoutMs);
	}

	// Options.verifyMode
	if (openIabOptions->m_VerifyMode > 0)
	{
		fid = env->GetFieldID(optionsClass, "verifyMode", "I");
		if (fid == 0) goto fail;
		env->SetIntField(options, fid, (jint) openIabOptions->m_VerifyMode);
	}

	// Options.storeKeys
	if (openIabOptions->m_NumStores > 0)
	{
		mapClass = env->FindClass("java/util/HashMap");
		if (mapClass == NULL) goto fail;
		mapLen = openIabOptions->m_NumStores;
		mapClassConstructor = env->GetMethodID(mapClass, "<init>", "(I)V");
		hashMap = env->NewObject(mapClass, mapClassConstructor, mapLen);
		hashMapPut = env->GetMethodID(mapClass, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

		for (int i = 0; i < mapLen; ++i)
		{
			jstring key = env->NewStringUTF(openIabOptions->m_StoreNames[i]);
			jstring value = env->NewStringUTF(openIabOptions->m_StoreKeys[i]);
			
			env->CallObjectMethod(hashMap, hashMapPut, key, value);
			
			env->DeleteLocalRef(key);
			env->DeleteLocalRef(value);
		}
		
		fid = env->GetFieldID(optionsClass, "storeKeys", "Ljava/util/Map;");
		if (fid == 0)
			goto fail;
		env->SetObjectField(options, fid, hashMap);
	}
	
	// Options.prefferedStoreNames
	if (openIabOptions->m_NumPrefferedStoreNames > 0)
	{
		jobjectArray stringArray = makeStringArray(openIabOptions->m_PrefferedStoreNames, openIabOptions->m_NumPrefferedStoreNames);
		fid = env->GetFieldID(optionsClass, "prefferedStoreNames", "[Ljava/lang/String;");
		if (fid == 0) goto fail;
		env->SetObjectField(options, fid, stringArray);
	}

	// Options.samsungCertificationRequestCode
	if (openIabOptions->m_SamsungCertificationRequestCode > 0)
	{
		fid = env->GetFieldID(optionsClass, "samsungCertificationRequestCode", "I");
		if (fid == 0) goto fail;
		env->SetIntField(options, fid, (jint) openIabOptions->m_SamsungCertificationRequestCode);
	}
	
	// Pass options at last
	env->CallIntMethod(g_Obj, g_s3eOpenIabStart, options);

	// Clear references
	if (openIabOptions->m_NumStores > 0)
		env->DeleteLocalRef(hashMap);
	env->DeleteLocalRef(options);

	return;
	
fail:
    jthrowable exc = env->ExceptionOccurred();
    if (exc)
    {
        env->ExceptionDescribe();
        env->ExceptionClear();
        IwTrace(s3eOpenIab, ("s3eOpenIabStart: One or more java methods could not be found"));
    }
}

void s3eOpenIabStop_platform()
{
    JNIEnv* env = s3eEdkJNIGetEnv();
    env->CallVoidMethod(g_Obj, g_s3eOpenIabStop);
}

s3eResult s3eOpenIabIsSupported_platform()
{
    JNIEnv* env = s3eEdkJNIGetEnv();
    return (s3eResult)env->CallIntMethod(g_Obj, g_s3eOpenIabIsSupported);
}

void s3eOpenIabRequestPurchase_platform(const char* productID, bool inApp, const char* develperPayLoad)
{
    JNIEnv* env = s3eEdkJNIGetEnv();
    jstring productID_jstr = env->NewStringUTF(productID);
    jstring develperPayLoad_jstr = env->NewStringUTF(develperPayLoad);
    env->CallVoidMethod(g_Obj, g_s3eOpenIabRequestPurchase, productID_jstr, inApp, develperPayLoad_jstr);
}

void s3eOpenIabRequestProductInformation_platform(const char** inAppProducts,int numInAppProducts, const char** subProducts, int numSubProducts)
{
 	s3eAndroidJNIFrame env(s3eEdkJNIGetEnv(),numInAppProducts + numSubProducts); // this local frame is needed to make sure there is enough stack to hold the strings
 	jobjectArray inAppArray = makeStringArray( inAppProducts, numInAppProducts );
	jobjectArray subsArray = makeStringArray( subProducts, numSubProducts );	
	env->CallVoidMethod(g_Obj, g_s3eOpenIabRequestProductInformation, inAppArray ,subsArray);
}

void s3eOpenIabRestoreTransactions_platform()
{
    JNIEnv* env = s3eEdkJNIGetEnv();
    env->CallVoidMethod(g_Obj, g_s3eOpenIabRestoreTransactions);
}

void s3eOpenIabConsumeItem_platform(const char* purchaseToken)
{
    JNIEnv* env = s3eEdkJNIGetEnv();
    jstring purchaseToken_jstr = env->NewStringUTF(purchaseToken);
    env->CallVoidMethod(g_Obj, g_s3eOpenIabConsumeItem, purchaseToken_jstr);
}

bool s3eOpenIabIsDebugLog_platform()
{
	JNIEnv* env = s3eEdkJNIGetEnv();
	return env->CallBooleanMethod(g_Obj, g_s3eOpenIabIsDebugLog);
}

void s3eOpenIabEnableDebugLogging_platform(bool enable)
{
	JNIEnv* env = s3eEdkJNIGetEnv();
	env->CallVoidMethod(g_Obj, g_s3eOpenIabEnableDebugLogging, enable);
}

void s3eOpenIabEnableDebugTagLogging_platform(bool enable, const char* tag)
{
	JNIEnv* env = s3eEdkJNIGetEnv();
	jstring tag_jstr = env->NewStringUTF(tag);
	env->CallVoidMethod(g_Obj, g_s3eOpenIabEnableDebugLogging, enable, tag_jstr);
}

static char* JStringFieldToChar(jobject obj,jfieldID field)
{
    JNIEnv* env=s3eEdkJNIGetEnv();
    jstring str=(jstring)env->GetObjectField(obj,field);
    if(!str)
    {
        return NULL;
    }
    jsize len=env->GetStringUTFLength(str);
    const char* utf=env->GetStringUTFChars(str,NULL);
    char* retval=new char[len+1];
    memcpy(retval,utf,len+1); //faster strcpy
    env->ReleaseStringUTFChars(str,utf);
    return retval;
}

void ConvertPurchase(JNIEnv* env,  jobject purchaseData, s3eOpenIabPurchase *p)
{
    jclass j_orderclass=env->FindClass("org/onepf/marmalade/s3eOpenIab/s3eOpenIab$S3eBillingPurchase");
    jfieldID jf_m_OrderID  			= env->GetFieldID(j_orderclass,"m_OrderID"  		,"Ljava/lang/String;");	
    jfieldID jf_m_PackageID  		= env->GetFieldID(j_orderclass,"m_PackageID"  		,"Ljava/lang/String;");	
    jfieldID jf_m_ProductId  		= env->GetFieldID(j_orderclass,"m_ProductId"  		,"Ljava/lang/String;");	
    jfieldID jf_m_PurchaseTime      = env->GetFieldID(j_orderclass,"m_PurchaseTime" 	,"J"				 );
    jfieldID jf_m_PurchaseState     = env->GetFieldID(j_orderclass,"m_PurchaseState"    ,"I"				 );
    jfieldID jf_m_PurchaseToken     = env->GetFieldID(j_orderclass,"m_PurchaseToken"    ,"Ljava/lang/String;");
    jfieldID jf_m_DeveloperPayload  = env->GetFieldID(j_orderclass,"m_DeveloperPayload" ,"Ljava/lang/String;");
    jfieldID jf_m_JSON		        = env->GetFieldID(j_orderclass,"m_JSON"    			,"Ljava/lang/String;");
    jfieldID jf_m_Signature		    = env->GetFieldID(j_orderclass,"m_Signature"   		,"Ljava/lang/String;");
	
	p->m_OrderID 				= JStringFieldToChar(purchaseData,jf_m_OrderID);
	p->m_PackageID 				= JStringFieldToChar(purchaseData,jf_m_PackageID);
	p->m_ProductId				= JStringFieldToChar(purchaseData,jf_m_ProductId);
	p->m_PurchaseTime			= env->GetLongField(purchaseData,jf_m_PurchaseTime);
	p->m_PurchaseState			= env->GetIntField(purchaseData,jf_m_PurchaseState);
	p->m_PurchaseToken			= JStringFieldToChar(purchaseData,jf_m_PurchaseToken);
	p->m_DeveloperPayload		= JStringFieldToChar(purchaseData,jf_m_DeveloperPayload);
	p->m_JSON					= JStringFieldToChar(purchaseData,jf_m_JSON);
	p->m_Signature				= JStringFieldToChar(purchaseData,jf_m_Signature);
}

void DeletePurchase(s3eOpenIabPurchase *p)
{
	delete []p->m_OrderID;
	delete []p->m_PackageID;
	delete []p->m_ProductId;
	delete []p->m_PurchaseToken;
	delete []p->m_DeveloperPayload;
	delete []p->m_JSON;
	delete []p->m_Signature;
}

static void s3eAGC_DeallocatePurchase(uint32 extID, int32 notification, void *systemData, void *instance, int32 returnCode, void *completeData)
{
	s3eOpenIabPurchaseResponse *pr = (s3eOpenIabPurchaseResponse*)systemData;
	if (pr->m_PurchaseDetails)
		DeletePurchase(pr->m_PurchaseDetails);
	if (pr->m_ErrorMsg)
		delete []pr->m_ErrorMsg;
	delete pr;
}

// Our Native callbacks - these are called from Java

void JNICALL s3e_OPENIAB_PURCHASE_CALLBACK( JNIEnv* env,  jobject obj, jint status, jstring errorMsg, jobject purchaseData)
{
	s3eOpenIabPurchaseResponse *pr = new s3eOpenIabPurchaseResponse;
	pr->m_ErrorMsg = JStringToChar(env, errorMsg);
	pr->m_Status = (s3eOpenIabResult)status;
	
	if (purchaseData)
	{
		pr->m_PurchaseDetails = new s3eOpenIabPurchase;
		ConvertPurchase(env, purchaseData, pr->m_PurchaseDetails);
	}
	else
		pr->m_PurchaseDetails = NULL;
	
	s3eEdkCallbacksEnqueue(S3E_EXT_OPENIAB_HASH,S3E_OPENIAB_PURCHASE_CALLBACK,pr,0,NULL,false,s3eAGC_DeallocatePurchase,pr);
}

void ConvertSku(JNIEnv* env,  jobject itemData, s3eOpenIabItemInfo *p)
{
    jclass j_orderclass=env->FindClass("org/onepf/marmalade/s3eOpenIab/s3eOpenIab$S3eBillingItemInfo");
    jfieldID jf_m_ProductID			= env->GetFieldID(j_orderclass,"m_ProductID"  		,"Ljava/lang/String;");	
    jfieldID jf_m_Type		  		= env->GetFieldID(j_orderclass,"m_Type"  			,"Ljava/lang/String;");	
    jfieldID jf_m_Price		  		= env->GetFieldID(j_orderclass,"m_Price"  			,"Ljava/lang/String;");	
    jfieldID jf_m_Title      		= env->GetFieldID(j_orderclass,"m_Title" 			,"Ljava/lang/String;");
    jfieldID jf_m_Description     	= env->GetFieldID(j_orderclass,"m_Description"    	,"Ljava/lang/String;");

	p->m_ProductID 				= JStringFieldToChar(itemData,jf_m_ProductID);
	p->m_Type	 				= JStringFieldToChar(itemData,jf_m_Type);
	p->m_Price					= JStringFieldToChar(itemData,jf_m_Price);
	p->m_Title					= JStringFieldToChar(itemData,jf_m_Title);
	p->m_Description			= JStringFieldToChar(itemData,jf_m_Description);
}

void DeleteItemData(s3eOpenIabItemInfo *p)
{
	delete []p->m_ProductID;
	delete []p->m_Type;
	delete []p->m_Price;
	delete []p->m_Title;
	delete []p->m_Description;
}

static void s3eAGC_DeallocateItemList(uint32 extID, int32 notification, void *systemData, void *instance, int32 returnCode, void *completeData)
{
	s3eOpenIabSkuResponse *item = (s3eOpenIabSkuResponse*)systemData;
	for (int i=0;i<item->m_NumProducts;i++)
		DeleteItemData(&item->m_Products[i]);
	if (item->m_Products)
		delete []item->m_Products;
	if (item->m_ErrorMsg)
		delete []item->m_ErrorMsg;
	delete item;
}

void JNICALL s3e_OPENIAB_LIST_PRODUCTS_CALLBACK( JNIEnv* env,  jobject obj, jint status, jstring errorMsg, jobjectArray products)
{
	s3eOpenIabSkuResponse *sr = new s3eOpenIabSkuResponse;
	sr->m_ErrorMsg = JStringToChar(env, errorMsg);
	sr->m_Status = (s3eOpenIabResult)status;
	
	if ((products) && (env->GetArrayLength(products)))
	{
		sr->m_NumProducts = env->GetArrayLength(products);
		sr->m_Products = new s3eOpenIabItemInfo[sr->m_NumProducts];
		for (int i=0;i<sr->m_NumProducts;i++)
		{
			jobject j_item=env->GetObjectArrayElement(products,i);
			ConvertSku(env,j_item,&sr->m_Products[i]);
		}
	}
	else
	{
		sr->m_NumProducts = 0;
		sr->m_Products = NULL;
	}
	
	s3eEdkCallbacksEnqueue(S3E_EXT_OPENIAB_HASH,S3E_OPENIAB_LIST_PRODUCTS_CALLBACK,sr,0,NULL,false,s3eAGC_DeallocateItemList,sr);
}

static void s3eAGC_DeallocatePurchases(uint32 extID, int32 notification, void *systemData, void *instance, int32 returnCode, void *completeData)
{
	s3eOpenIabRestoreResponse *rr = (s3eOpenIabRestoreResponse*)systemData;
	for (int i=0;i<rr->m_NumPurchases;i++)
		DeletePurchase(&rr->m_Purchases[i]);
	if (rr->m_Purchases)
		delete []rr->m_Purchases;
	if (rr->m_ErrorMsg)
		delete []rr->m_ErrorMsg;
	delete rr;
}

void JNICALL s3e_OPENIAB_RESTORE_CALLBACK( JNIEnv* env,  jobject obj, jint status, jstring errorMsg, jobjectArray purchases)
{
	s3eOpenIabRestoreResponse *rr = new s3eOpenIabRestoreResponse;
	rr->m_ErrorMsg = JStringToChar(env, errorMsg);
	rr->m_Status = (s3eOpenIabResult)status;
	
	if ((purchases) && (env->GetArrayLength(purchases)))
	{
		rr->m_NumPurchases = env->GetArrayLength(purchases);
		rr->m_Purchases = new s3eOpenIabPurchase[rr->m_NumPurchases];
		for (int i=0;i<rr->m_NumPurchases;i++)
		{
			jobject j_purchase=env->GetObjectArrayElement(purchases,i);
			ConvertPurchase(env,j_purchase,&rr->m_Purchases[i]);
		}
	}
	else
	{
		rr->m_NumPurchases = 0;
		rr->m_Purchases = NULL;
	}
	
	s3eEdkCallbacksEnqueue(S3E_EXT_OPENIAB_HASH,S3E_OPENIAB_RESTORE_CALLBACK,rr,0,NULL,false,s3eAGC_DeallocatePurchases,rr);
}

static void s3eAGC_DeallocateConsume(uint32 extID, int32 notification, void *systemData, void *instance, int32 returnCode, void *completeData)
{
	s3eOpenIabConsumeResponse *cr = (s3eOpenIabConsumeResponse*)systemData;
	if (cr->m_ErrorMsg)
		delete []cr->m_ErrorMsg;
	delete cr;
}

void JNICALL s3e_OPENIAB_CONSUME_CALLBACK( JNIEnv* env,  jobject obj, jint status, jstring errorMsg)
{
	s3eOpenIabConsumeResponse *cr = new s3eOpenIabConsumeResponse;
	cr->m_ErrorMsg = JStringToChar(env, errorMsg);
	cr->m_Status = (s3eOpenIabResult)status;
	
	s3eEdkCallbacksEnqueue(S3E_EXT_OPENIAB_HASH,S3E_OPENIAB_CONSUME_CALLBACK,cr,0,NULL,false,s3eAGC_DeallocateConsume,cr);
}
