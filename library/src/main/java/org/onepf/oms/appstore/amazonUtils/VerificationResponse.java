package org.onepf.oms.appstore.amazonUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class VerificationResponse {

    public static VerificationResponse fromJson(final String json) throws JSONException {
        return fromJson(new JSONObject(json));
    }

    public static VerificationResponse fromJson(final JSONObject jsonObject) throws JSONException {
        final Long purchaseDate = jsonObject.getLong("purchaseDate");
        final Long cancelDate = jsonObject.getLong("cancelDate");
        final String receiptId = jsonObject.getString("receiptId");
        final String productId = jsonObject.getString("productId");
        final String parentProductId = jsonObject.getString("parentProductId");
        final String productType = jsonObject.getString("productType");
        final Integer quantity = jsonObject.getInt("quantity");
        final Boolean betaProduct = jsonObject.getBoolean("betaProduct");
        final Boolean testTransaction = jsonObject.getBoolean("testTransaction");

        return new VerificationResponse(purchaseDate, cancelDate, receiptId, productId,
                parentProductId, productType, quantity, betaProduct, testTransaction);
    }


    private final Long purchaseDate;

    private final Long cancelDate;

    private final String receiptId;

    private final String productId;

    private final String parentProductId;

    private final String productType;

    private final Integer quantity;

    private final Boolean betaProduct;

    private final Boolean testTransaction;

    public VerificationResponse(
            final Long purchaseDate,
            final Long cancelDate,
            final String receiptId,
            final String productId,
            final String parentProductId,
            final String productType,
            final Integer quantity,
            final Boolean betaProduct,
            final Boolean testTransaction
    ) {
        this.purchaseDate = purchaseDate;
        this.receiptId = receiptId;
        this.productId = productId;
        this.parentProductId = parentProductId;
        this.productType = productType;
        this.cancelDate = cancelDate;
        this.quantity = quantity;
        this.betaProduct = betaProduct;
        this.testTransaction = testTransaction;
    }

    public Long getPurchaseDate() {
        return purchaseDate;
    }

    public Long getCancelDate() {
        return cancelDate;
    }

    public String getReceiptId() {
        return receiptId;
    }

    public String getProductId() {
        return productId;
    }

    public String getParentProductId() {
        return parentProductId;
    }

    public String getProductType() {
        return productType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Boolean getBetaProduct() {
        return betaProduct;
    }

    public Boolean getTestTransaction() {
        return testTransaction;
    }
}
