package com.midtrans.sdk.uikit.activities;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import com.midtrans.sdk.corekit.callback.TransactionCallback;
import com.midtrans.sdk.corekit.core.Constants;
import com.midtrans.sdk.corekit.core.Logger;
import com.midtrans.sdk.corekit.core.MidtransSDK;
import com.midtrans.sdk.corekit.models.TransactionResponse;
import com.midtrans.sdk.corekit.utilities.Utils;
import com.midtrans.sdk.uikit.R;
import com.midtrans.sdk.uikit.constants.AnalyticsEventName;
import com.midtrans.sdk.uikit.fragments.InstructionEpayBriFragment;
import com.midtrans.sdk.uikit.fragments.WebviewFragment;
import com.midtrans.sdk.uikit.utilities.SdkUIFlowUtil;
import com.midtrans.sdk.uikit.widgets.DefaultTextView;
import com.midtrans.sdk.uikit.widgets.FancyButton;

public class EpayBriActivity extends BaseActivity implements View.OnClickListener {

    private static final int PAYMENT_WEB_INTENT = 150;
    private static final String STATUS_FRAGMENT = "status";
    private static final String HOME_FRAGMENT = "home";
    private FancyButton btConfirmPayment = null;
    private Toolbar toolbar = null;
    private MidtransSDK midtransSDK = null;
    private InstructionEpayBriFragment instructionEpayBriFragment;
    private TransactionResponse transactionResponse;
    private String currentFragmentName = HOME_FRAGMENT;
    private String errorMessage;
    private TransactionResponse transactionResponseFromMerchant;
    private DefaultTextView textTitle, textTotalAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_epay_bri);
        midtransSDK = MidtransSDK.getInstance();
        if (midtransSDK == null) {
            SdkUIFlowUtil.showToast(EpayBriActivity.this, Constants
                    .ERROR_SDK_IS_NOT_INITIALIZED);
            finish();
        }

        initializeViews();
        setUpFragment();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void initializeViews() {
        btConfirmPayment = (FancyButton) findViewById(R.id.btn_confirm_payment);
        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        textTitle = (DefaultTextView) findViewById(R.id.text_title);
        textTotalAmount = (DefaultTextView) findViewById(R.id.text_amount);

        initializeTheme();
        setSupportActionBar(toolbar);
        prepareToolbar();
        btConfirmPayment.setVisibility(View.VISIBLE);
        btConfirmPayment.setOnClickListener(this);
        bindData();
    }

    private void prepareToolbar() {
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_back);
        MidtransSDK midtransSDK =MidtransSDK.getInstance();
        if (midtransSDK.getColorTheme() != null && midtransSDK.getColorTheme().getPrimaryDarkColor() != 0) {
            drawable.setColorFilter(
                    midtransSDK.getColorTheme().getPrimaryDarkColor(),
                    PorterDuff.Mode.SRC_ATOP);
        }
        toolbar.setNavigationIcon(drawable);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    private void bindData() {
        textTitle.setText(getString(R.string.epay_bri));
        if (midtransSDK != null) {
            if (midtransSDK.getSemiBoldText() != null) {
                btConfirmPayment.setCustomTextFont(midtransSDK.getSemiBoldText());
            }
            textTotalAmount.setText(getString(R.string.prefix_money,
                    Utils.getFormattedAmount(midtransSDK.getTransactionRequest().getAmount())));
        }
    }

    private void setUpFragment() {
        //track page BRI Epay
        midtransSDK.trackEvent(AnalyticsEventName.PAGE_BRI_EPAY);

        // setup  fragment
        instructionEpayBriFragment = new InstructionEpayBriFragment();
        replaceFragment(instructionEpayBriFragment, R.id.instruction_container, false, false);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_confirm_payment) {
            makeTransaction();
        }
    }

    private void makeTransaction() {
        //track BRI Epay confirm payment
        midtransSDK.trackEvent(AnalyticsEventName.BTN_CONFIRM_PAYMENT);

        SdkUIFlowUtil.showProgressDialog(this, getString(R.string.processing_payment), false);
        midtransSDK.paymentUsingEpayBRI(midtransSDK.readAuthenticationToken(), new TransactionCallback() {
            @Override
            public void onSuccess(TransactionResponse response) {
                //track page status pending
                MidtransSDK.getInstance().trackEvent(AnalyticsEventName.PAGE_STATUS_PENDING);

                SdkUIFlowUtil.hideProgressDialog();
                if (response != null &&
                        !TextUtils.isEmpty(response.getRedirectUrl())) {
                    transactionResponse = response;
                    Intent intentPaymentWeb = new Intent(EpayBriActivity.this, PaymentWebActivity.class);
                    intentPaymentWeb.putExtra(Constants.WEBURL, response.getRedirectUrl());
                    intentPaymentWeb.putExtra(Constants.TYPE, WebviewFragment.TYPE_EPAY_BRI);
                    startActivityForResult(intentPaymentWeb, PAYMENT_WEB_INTENT);
                    if (MidtransSDK.getInstance().getUIKitCustomSetting()!=null
                            && MidtransSDK.getInstance().getUIKitCustomSetting().isEnabledAnimation()) {
                        overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
                    }
                } else {
                    SdkUIFlowUtil.showApiFailedMessage(EpayBriActivity.this, getString(R.string.empty_transaction_response));
                }
            }

            @Override
            public void onFailure(TransactionResponse response, String reason) {
                //track page status failed
                MidtransSDK.getInstance().trackEvent(AnalyticsEventName.PAGE_STATUS_FAILED);

                SdkUIFlowUtil.hideProgressDialog();
                EpayBriActivity.this.errorMessage = getString(R.string.message_payment_failed);
                if (response != null && response.getStatusCode().equals(getString(R.string.failed_code_400))) {
                    setResultCode(RESULT_OK);
                    setResultAndFinish();
                } else {
                    SdkUIFlowUtil.showApiFailedMessage(EpayBriActivity.this, errorMessage);
                }
            }

            @Override
            public void onError(Throwable error) {
                //track page status failed
                MidtransSDK.getInstance().trackEvent(AnalyticsEventName.PAGE_STATUS_FAILED);

                SdkUIFlowUtil.hideProgressDialog();
                EpayBriActivity.this.errorMessage = getString(R.string.message_payment_failed);
                SdkUIFlowUtil.showApiFailedMessage(EpayBriActivity.this, errorMessage);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Logger.i("reqCode:" + requestCode + ",res:" + resultCode);
        Drawable closeIcon = ContextCompat.getDrawable(this, R.drawable.ic_close);
        closeIcon.setColorFilter(ContextCompat.getColor(this, R.color.dark_gray), PorterDuff.Mode.MULTIPLY);
        if (resultCode == RESULT_OK) {
            currentFragmentName = STATUS_FRAGMENT;
            transactionResponseFromMerchant = transactionResponse;
            RESULT_CODE = RESULT_OK;
            setResultAndFinish();
        } else if (resultCode == RESULT_CANCELED) {
            currentFragmentName = STATUS_FRAGMENT;
            RESULT_CODE = RESULT_OK;
            transactionResponseFromMerchant = transactionResponse;
            setResultAndFinish();
        }
    }

    public void setResultAndFinish() {
        Intent data = new Intent();
        if (transactionResponseFromMerchant != null) {
            Logger.i("transactionResponseFromMerchant:" + transactionResponseFromMerchant.getString());
            data.putExtra(getString(R.string.transaction_response), transactionResponseFromMerchant);
        }
        data.putExtra(getString(R.string.error_transaction), errorMessage);
        setResult(RESULT_CODE, data);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (currentFragmentName.equals(
                STATUS_FRAGMENT)) {
            setResultCode(RESULT_OK);
            setResultAndFinish();
        } else {
            super.onBackPressed();
        }
    }
}