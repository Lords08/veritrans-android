package com.midtrans.sdk.ui.models;

import com.midtrans.sdk.core.models.snap.SavedToken;
import com.midtrans.sdk.core.models.snap.promo.PromoResponse;
import com.midtrans.sdk.ui.constants.Constants;

import java.io.Serializable;

/**
 * Created by ziahaqi on 2/26/17.
 */

public class CreditCardDetails implements Serializable {
    private SavedToken savedToken;
    private PromoResponse promo;
    public CreditCardDetails(SavedToken savedToken, PromoResponse promo) {
        this.savedToken = savedToken;
        this.promo = promo;
    }

    public boolean hasSavedToken() {
        return savedToken != null;
    }

    public SavedToken getSavedToken() {
        return savedToken;
    }

    public boolean isOneclickMode() {
        return hasSavedToken() && savedToken.tokenType.equals(Constants.CREDIT_CARD_ONE_CLICK);
    }

    public boolean isTwoClicksMode() {
        return hasSavedToken() && savedToken.tokenType.equals(Constants.CREDIT_CARD_TWO_CLICKS);
    }

    public boolean isNormalMode() {
        return (!hasSavedToken() || !isTwoClicksMode() || !isOneclickMode());
    }

    public String getMaskedCardNumber() {
        if(hasSavedToken()){
            return getSavedToken().maskedCard;
        }
        return null;
    }
}