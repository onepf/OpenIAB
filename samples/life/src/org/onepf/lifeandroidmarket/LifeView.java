package org.onepf.lifeandroidmarket;

import org.onepf.lifeandroidmarket.R;
import org.onepf.lifeandroidmarket.util.IabHelper;
import org.onepf.lifeandroidmarket.util.IabResult;
import org.onepf.lifeandroidmarket.util.Inventory;
import org.onepf.lifeandroidmarket.util.Purchase;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class LifeView extends SurfaceView implements Runnable {
	// ------- Billing parameters

	IabHelper mHelper;
	boolean isPremium = false;
	boolean isSubscribed = false;
	boolean loadedFromMarket = false;
	// for testing
	// static final String SKU_PREMIUM = "android.test.purchased";
	// static final String SKU_SUBSCRIBED = "android.test.purchased";
	static final String SKU_PREMIUM = "premium_account";
	static final String SKU_SUBSCRIBED = "orange_cells_subscription";
	static final String TAG = "Life";
	// (arbitrary) request code for the purchase flow
	static final int RC_REQUEST = 10001;
	// encryption?
	String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhh9ee2Ka+dO2UCkGSndfH6/5jZ/kgILRguYcp8TpkAus6SEU8r8RSjYf4umAVD0beC3e7KOpxHxjnnE0z8A+MegZ11DE7/jQw4XQ0BaGzDTezCJrNUR8PqKf/QemRIT7UaNC0DrYE07v9WFjHFSXOqChZaJpih5lC/1yxwh+54IS4wapKcKnOFjPqbxw8dMTA7b0Ti0KzpBcexIBeDV5FT6FimawfbUr/ejae2qlu1fZdlwmj+yJEFk8h9zLiH7lhzB6PIX72lLAYk+thS6K8i26XbtR+t9/wahlwv05W6qtLEvWBJ5yeNXUghAw+Hk/x8mwIlrsjWMQtt1W+pBxYQIDAQAB";

	// ------- End of billing parameters

	// SurfaceView login related members
	SurfaceHolder holder;
	Thread thread = null;
	volatile boolean running = false;
	Context context;

	// Size members
	int viewWidth;
	int viewHeight;
	int cellWidth = 0;
	int cellHeight = 0;

	// Field
	int fieldWidth;
	int fieldHeight;
	int[][] field = null;

	// Edit more
	boolean editMode = true;

	// Cell images & background
	Bitmap activeCellBitmap = null;
	Bitmap emptyCellBitmap = null;
	int backgroundColor;

	public LifeView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		mHelper = new IabHelper(context, base64EncodedPublicKey);
		mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
			public void onIabSetupFinished(IabResult result) {
				if (!result.isSuccess()) {
					Log.d("error", "Problem setting up In-app Billing: "
							+ result);
					return;
				}
				mHelper.queryInventoryAsync(mGotInventoryListener);
			}
		});

		backgroundColor = context.getResources().getColor(R.color.background);
		holder = getHolder();
		activeCellBitmap = BitmapFactory.decodeResource(getResources(),
				R.drawable.cell_active);
		emptyCellBitmap = BitmapFactory.decodeResource(getResources(),
				R.drawable.cell_empty);
		cellWidth = activeCellBitmap.getWidth();
		cellHeight = activeCellBitmap.getHeight();
	}

	// ------- Start of billing part

	boolean verifyDeveloperPayload(Purchase p) {
		String payload = p.getDeveloperPayload();
		// do smth
		return true;
	}

	// Callback for when a purchase is finished
	IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
		public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
			Log.d(TAG, "Purchase finished: " + result + ", purchase: "
					+ purchase);
			if (result.isFailure()) {
				complain("Error purchasing: " + result);
				return;
			}
			if (!verifyDeveloperPayload(purchase)) {
				alert("Error purchasing. Authenticity verification failed.");
				return;
			}

			Log.d(TAG, "Purchase successful.");

			if (purchase.getSku().equals(SKU_PREMIUM)) {
				alert("Thank you for upgrading to premium!");
				isPremium = true;
			}
			if (purchase.getSku().equals(SKU_SUBSCRIBED)) {
				alert("Thank you for subscribing!");
				isSubscribed = true;
				changeCellColor();
			}
			// check other options ...
		}
	};

	private void onBuyUpgradeEvent() {
		String payload = "";
		mHelper.launchPurchaseFlow((Activity) context, SKU_PREMIUM, RC_REQUEST,
				mPurchaseFinishedListener, payload);
	}
	
	public void onBuySubscriptionEvent() {
		if (isSubscribed) {
			alert("You have already subscribed!");
			return;
		}
		String payload = "";
		mHelper.launchPurchaseFlow((Activity) context, SKU_SUBSCRIBED, RC_REQUEST,
				mPurchaseFinishedListener, payload);
	}

	IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
		public void onQueryInventoryFinished(IabResult result,
				Inventory inventory) {
			if (result.isFailure()) {
				alert("Failed to load account information");
				return;
			}
			Purchase premiumPurchase = inventory.getPurchase(SKU_PREMIUM);
			isPremium = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));
			loadedFromMarket = true;

			Purchase subsctiption = inventory.getPurchase(SKU_SUBSCRIBED);
			isSubscribed = (subsctiption != null && verifyDeveloperPayload(subsctiption));
			if (isSubscribed) {
				changeCellColor();
			}
		}
	};

	// ------- End of billing part

	private void changeCellColor() {
		activeCellBitmap = BitmapFactory.decodeResource(getResources(),
				R.drawable.cell_active_orange);
	}
	
	public void setEditMode(boolean editMode) {
		this.editMode = editMode;
	}

	public boolean getEditMode() {
		return editMode;
	}

	public void resume() {
		running = true;
		thread = new Thread(this);
		thread.start();
	}

	public void pause() {
		running = false;
		try {
			thread.join();
		} catch (InterruptedException ignore) {
		}
	}

	void alert(String message) {
		AlertDialog.Builder bld = new AlertDialog.Builder(context);
		bld.setMessage(message);
		bld.setNeutralButton("OK", null);
		bld.create().show();
	}

	void complain(String message) {
		alert("Error: " + message);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (editMode) {
			int cellX = (int) (event.getX() / cellWidth);
			int cellY = (int) (event.getY() / cellHeight);
			int action = event.getAction();
			if (action == MotionEvent.ACTION_DOWN && cellX >= 0 && cellY >= 0
					&& cellX < fieldWidth && cellY < fieldHeight) {
				if (!loadedFromMarket) {
					alert("Information about account hasn't loaded yet.");
					return true;
				}
				if (isPremium) {
					field[cellX][cellY] = 1 - field[cellX][cellY];
				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(
							context);
					builder.setTitle("Premium account")
							.setMessage(
									"To use this option you need to have a premium account. Do you want to buy it?")
							.setIcon(android.R.drawable.ic_dialog_alert)
							.setPositiveButton("Yes",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog,
												int which) {
											onBuyUpgradeEvent();
										}
									}).setNegativeButton("No", null).show();
				}
			}
		}

		return true;
	}

	@SuppressLint("WrongCall")
	@Override
	public void run() {
		while (running) {
			if (holder.getSurface().isValid()) {
				Canvas canvas = holder.lockCanvas();
				if (!editMode) {
					updateField();
				}
				onDraw(canvas);
				holder.unlockCanvasAndPost(canvas);
				try {
					Thread.sleep(16);
				} catch (InterruptedException ignore) {
				}
			}
		}
	}

	int getNeighbour(int x, int y, int dx, int dy) {
		int x2 = x + dx;
		int y2 = y + dy;
		if (x2 < 0)
			x2 += fieldWidth;
		if (y2 < 0)
			y2 += fieldHeight;
		if (x2 >= fieldWidth)
			x2 -= fieldWidth;
		if (y2 >= fieldHeight)
			y2 -= fieldHeight;

		return field[x2][y2];
	}

	int getNeighboursCount(int x, int y) {
		int count = 0;

		// Check all the cell's neighbours
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				// except the cell itself
				if (dx != 0 || dy != 0) {
					count += getNeighbour(x, y, dx, dy);
				}
			}
		}
		return count;
	}

	void updateField() {
		int[][] field2 = new int[fieldWidth][fieldHeight];
		for (int x = 0; x < fieldWidth; x++) {
			for (int y = 0; y < fieldHeight; y++) {

				field2[x][y] = field[x][y];
				int neighbours = getNeighboursCount(x, y);

				if (neighbours < 2) {
					field2[x][y] = 0;
				} else if (neighbours == 2) {
					field2[x][y] = field[x][y];
				} else if (neighbours == 3) {
					field2[x][y] = 1;
				} else {
					field2[x][y] = 0;
				}
			}
		}
		field = field2;
	}

	@Override
	public void onDraw(Canvas canvas) {
		canvas.drawColor(backgroundColor);
		for (int x = 0; x < fieldWidth; x++) {
			for (int y = 0; y < fieldHeight; y++) {
				if (field[x][y] == 1) {
					canvas.drawBitmap(activeCellBitmap, x * cellWidth, y
							* cellHeight, null);
				} else {
					canvas.drawBitmap(emptyCellBitmap, x * cellWidth, y
							* cellHeight, null);
				}
			}
		}
	}

	@Override
	public void onSizeChanged(int w, int h, int oldW, int oldH) {
		viewWidth = w;
		viewHeight = h;
		fieldWidth = viewWidth / cellWidth;
		fieldHeight = viewHeight / cellHeight;

		initField();
	}

	void initField() {
		field = new int[fieldWidth][fieldHeight];
		for (int x = 0; x < fieldWidth; x++) {
			for (int y = 0; y < fieldHeight; y++) {
				field[x][y] = 0;
			}
		}
		field[5][5] = 1;
		field[6][5] = 1;
		field[7][5] = 1;
		field[7][4] = 1;
		field[6][3] = 1;
	}

}
