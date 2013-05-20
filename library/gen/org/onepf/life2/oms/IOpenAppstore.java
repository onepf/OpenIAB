/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /Users/svasilinets/work/appdf/OpenIAB/library/src/org/onepf/life2/oms/IOpenAppstore.aidl
 */
package org.onepf.life2.oms;
public interface IOpenAppstore extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.onepf.life2.oms.IOpenAppstore
{
private static final java.lang.String DESCRIPTOR = "org.onepf.life2.oms.IOpenAppstore";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.onepf.life2.oms.IOpenAppstore interface,
 * generating a proxy if needed.
 */
public static org.onepf.life2.oms.IOpenAppstore asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.onepf.life2.oms.IOpenAppstore))) {
return ((org.onepf.life2.oms.IOpenAppstore)iin);
}
return new org.onepf.life2.oms.IOpenAppstore.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_isAppAvailable:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
boolean _result = this.isAppAvailable(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_isInstaller:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
boolean _result = this.isInstaller(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_isIabServiceSupported:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
boolean _result = this.isIabServiceSupported(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_getInAppBillingServiceIntent:
{
data.enforceInterface(DESCRIPTOR);
android.content.Intent _result = this.getInAppBillingServiceIntent();
reply.writeNoException();
if ((_result!=null)) {
reply.writeInt(1);
_result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_getAppstoreName:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getAppstoreName();
reply.writeNoException();
reply.writeString(_result);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements org.onepf.life2.oms.IOpenAppstore
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
@Override public boolean isAppAvailable(java.lang.String packageName) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(packageName);
mRemote.transact(Stub.TRANSACTION_isAppAvailable, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public boolean isInstaller(java.lang.String packageName) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(packageName);
mRemote.transact(Stub.TRANSACTION_isInstaller, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public boolean isIabServiceSupported(java.lang.String packageName) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(packageName);
mRemote.transact(Stub.TRANSACTION_isIabServiceSupported, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public android.content.Intent getInAppBillingServiceIntent() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
android.content.Intent _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getInAppBillingServiceIntent, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
_result = android.content.Intent.CREATOR.createFromParcel(_reply);
}
else {
_result = null;
}
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.lang.String getAppstoreName() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getAppstoreName, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_isAppAvailable = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_isInstaller = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_isIabServiceSupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_getInAppBillingServiceIntent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_getAppstoreName = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
}
public boolean isAppAvailable(java.lang.String packageName) throws android.os.RemoteException;
public boolean isInstaller(java.lang.String packageName) throws android.os.RemoteException;
public boolean isIabServiceSupported(java.lang.String packageName) throws android.os.RemoteException;
public android.content.Intent getInAppBillingServiceIntent() throws android.os.RemoteException;
public java.lang.String getAppstoreName() throws android.os.RemoteException;
}
