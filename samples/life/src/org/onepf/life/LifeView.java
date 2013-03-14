package org.onepf.life;

import android.content.SharedPreferences;
import android.graphics.Paint;
import android.widget.Toast;
import com.amazon.inapp.purchasing.PurchasingManager;
import org.onepf.life.util.IabHelper;
import org.onepf.life.util.Purchase;

import android.annotation.SuppressLint;
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
	//boolean isPremium = false;
	boolean loadedFromMarket = false;
	// for testing
	// static final String SKU_PREMIUM = "android.test.purchased";
	//static final String SKU_PREMIUM = "org.onepf.life.premium_account";
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
	GameActivity baseActivity;

	// Size members
	int viewWidth;
	int viewHeight;
	int cellWidth = 0;
	int cellHeight = 0;

	// Field
	int fieldWidth;
	int fieldHeight;
	int[][] field = null;

	// Edits
    boolean editMode = true;
	int changeCount;

	// Cell images & background
	Bitmap activeCellBitmap = null;
	Bitmap emptyCellBitmap = null;
	int backgroundColor;

	public LifeView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.baseActivity = (GameActivity)context;
        final SharedPreferences settings = baseActivity.getSharedPreferencesForCurrentUser();
        changeCount = settings.getInt(GameActivity.CHANGES, 50);
//		mHelper = new IabHelper(context, base64EncodedPublicKey);
//		mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
//			public void onIabSetupFinished(IabResult result) {
//				if (!result.isSuccess()) {
//					Log.d("error", "Problem setting up In-app Billing: "
//							+ result);
//					return;
//				}
//				mHelper.queryInventoryAsync(mGotInventoryListener);
//			}
//		});

		backgroundColor = baseActivity.getResources().getColor(R.color.background);
		holder = getHolder();
		activeCellBitmap = BitmapFactory.decodeResource(getResources(),
				R.drawable.cell_active_green);
		emptyCellBitmap = BitmapFactory.decodeResource(getResources(),
				R.drawable.cell_empty);
		cellWidth = activeCellBitmap.getWidth();
		cellHeight = activeCellBitmap.getHeight();
	}

	// -------  Start of billing part

	boolean verifyDeveloperPayload(Purchase p) {
		String payload = p.getDeveloperPayload();
		// do smth
		return true;
	}

	// Callback for when a purchase is finished
//	IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
//		public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
//			Log.d(TAG, "Purchase finished: " + result + ", purchase: "
//					+ purchase);
//			if (result.isFailure()) {
//				complain("Error purchasing: " + result);
//				return;
//			}
//			if (!verifyDeveloperPayload(purchase)) {
//				alert("Error purchasing. Authenticity verification failed.");
//				return;
//			}
//
//			Log.d(TAG, "Purchase successful.");
//
//			if (purchase.getSku().equals(SKU_PREMIUM)) {
//				alert("Thank you for upgrading to premium!");
//				isPremium = true;
//			}
//			// check other options ...
//		}
//	};

	public void onBuyUpgradeEvent() {
//		String payload = "";
//		mHelper.launchPurchaseFlow((Activity) context, SKU_PREMIUM, RC_REQUEST,
//				mPurchaseFinishedListener, payload);
        String requestId = PurchasingManager.initiatePurchaseRequest(getResources().getString(R.string.consumable_sku));
        Log.d(TAG, "Buy requestId = " + requestId);
        baseActivity.storeRequestId(requestId, GameActivity.CHANGES);
    }

//	IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
//		public void onQueryInventoryFinished(IabResult result,
//				Inventory inventory) {
//			if (result.isFailure()) {
//				alert("Failed to load account information");
//				return;
//			}
//			Purchase premiumPurchase = inventory.getPurchase(SKU_PREMIUM);
//			isPremium = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));
//			loadedFromMarket = true;
//		}
//	};

	// ------- End of billing part

    public void setActiveCellBitmap(Bitmap bitmap) {
        activeCellBitmap = bitmap;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    public boolean getEditMode() {
        return editMode;
    }

	public void setChangeCount(int changeCount) {
		this.changeCount = changeCount;
	}

	public int getChangeCount() {
		return changeCount;
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
		AlertDialog.Builder bld = new AlertDialog.Builder(baseActivity);
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
//				if (!loadedFromMarket) {
//					alert("Information about account hasn't loaded yet.");
//					return true;
//				}

				if (getChangeCount() > 0) {
                    changeCount--;
                    final SharedPreferences.Editor editor = baseActivity.getSharedPreferencesForCurrentUser().edit();
                    editor.putInt(GameActivity.CHANGES, changeCount);
                    editor.commit();
					field[cellX][cellY] = 1 - field[cellX][cellY];
				} else {
                    showBuyChangesDialog();
				}
			}
		}

		return true;
	}

    public void showBuyChangesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                baseActivity);
        builder.setTitle(R.string.changes_ended)
                .setMessage(R.string.changes_ended_message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(
                                    DialogInterface dialog,
                                    int which) {
                                onBuyUpgradeEvent();
                            }
                        }).setNegativeButton(android.R.string.no, null).show();
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
	public synchronized void onDraw(Canvas canvas) {
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
        Paint paint = new Paint();
        paint.setColor(android.R.color.primary_text_dark);
        paint.setAlpha(160);
        paint.setTextSize((float)0.7 * cellHeight);
        canvas.drawText("Changes: " + changeCount, cellWidth / 2, cellHeight, paint);
	}

	@Override
	public synchronized void onSizeChanged(int w, int h, int oldW, int oldH) {
        Log.d(TAG, "Old size: w = " + fieldWidth + ", h = " + fieldHeight);
        if (field == null) {
            viewHeight = w;
            viewWidth = h;
            fieldHeight = viewWidth / cellWidth;
            fieldWidth = viewHeight / cellHeight;
            initField();
        } else {
            int newFieldWidth = w / cellWidth;
            int newFieldHeight = h / cellHeight;
            Log.d(TAG, "New size: w = " + newFieldWidth + ", h = " + newFieldHeight);
            int[][] newField = new int[newFieldWidth][newFieldHeight];
            for (int x = 0; x < newFieldWidth; x++) {
                for (int y = 0; y < newFieldHeight; y++) {
                    newField[x][y] = 0;
                }
            }
            for (int x = 0; x < Math.min(fieldWidth, newFieldWidth); x++) {
                System.arraycopy(field[x], 0, newField[x], 0, Math.min(fieldHeight, newFieldHeight));
            }

            viewHeight = w;
            viewWidth = h;
            field = newField;
            fieldHeight = newFieldHeight;
            fieldWidth = newFieldWidth;
        }
	}

    public void clearField() {
        for (int x = 0; x < fieldWidth; x++) {
            for (int y = 0; y < fieldHeight; y++) {
                field[x][y] = 0;
            }
        }
    }

	public void initField() {
		field = new int[fieldWidth][fieldHeight];
        clearField();
        drawFigure(Figures.glider);
	}

    public void drawFigure(int[][] figure) {
        final int delta = 3;
        if (figure != null && (figure.length + delta > fieldHeight || figure[0].length + delta > fieldWidth)) {
            Toast.makeText(baseActivity.getApplicationContext(), R.string.not_enough_space, Toast.LENGTH_SHORT).show();
        } else {
            clearField();
            if (figure != null) {
                for (int y = 0; y < figure.length; y++) {
                    for (int x = 0; x < figure[0].length; x++) {
                        field[x + delta][y + delta] = figure[y][x];
                    }
                }
            }
        }
    }
}
