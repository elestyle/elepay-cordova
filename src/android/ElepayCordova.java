package io.elepay.sdk.cordova.plugin;

import android.text.TextUtils;
import android.util.Log;

import jp.elestyle.androidapp.elepay.Elepay;
import jp.elestyle.androidapp.elepay.ElepayConfiguration;
import jp.elestyle.androidapp.elepay.ElepayError;
import jp.elestyle.androidapp.elepay.ElepayResult;
import jp.elestyle.androidapp.elepay.ElepayResultListener;
import jp.elestyle.androidapp.elepay.GooglePayEnvironment;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class echoes a string called from JavaScript.
 */
public class ElepayCordova extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("initElepay")) {
            JSONObject configs = args.getJSONObject(0);
            this.initElepay(configs);
            return true;
        }
        if (action.equals("handleOpenUrl")) {
            // nothing to do.
            // url-scheme callback is handled by `Activity`s defined in app's AndroidManifest.xml
            return true;
        }
        if (action.equals("handlePayment")) {
            JSONObject chargeObj = args.getJSONObject(0);
            this.handlePayment(chargeObj, callbackContext);
            return true;
        }
        return false;
    }

    private void initElepay(JSONObject configs) {
        String pKey = configs.optString("publicKey", "");
        String apiUrl = configs.optString("apiUrl", "");
        String googlePayEnvStr = configs.optString("googlePayEnvironment", "");
        GooglePayEnvironment googlePayEnv;
        if (googlePayEnvStr.toLowerCase().contains("test"))  {
            googlePayEnv = GooglePayEnvironment.TEST;
        } else {
            googlePayEnv = GooglePayEnvironment.PRODUCTION;
        }
        Elepay.setup(new ElepayConfiguration(pKey, apiUrl, googlePayEnv));
    }

    private void handlePayment(JSONObject chargeObject, CallbackContext callbackContext) {
        Elepay.processPayment(chargeObject, cordova.getActivity(), new ElepayResultListener() {
            @Override
            public void onElepayResult(ElepayResult elePayResult) {
                if (elePayResult instanceof ElepayResult.Succeeded) {
                    String id = ((ElepayResult.Succeeded)elePayResult).getPaymentId();
                    JSONObject res = new JSONObject();
                    try {
                        res.put("state", "succeeded");
                        res.put("paymentId", id);
                    } catch (JSONException e) {}
                    callbackContext.success(res);
                } else if (elePayResult instanceof ElepayResult.Canceled) {
                    String id = ((ElepayResult.Canceled) elePayResult).getPaymentId();
                    JSONObject res = new JSONObject();
                    try {
                        res.put("state", "cancelled");
                        res.put("paymentId", id);
                    } catch (JSONException e) {}
                    callbackContext.error(res);
                } else if (elePayResult instanceof ElepayResult.Failed) {
                    String id = ((ElepayResult.Failed) elePayResult).getPaymentId();
                    ElepayError error = ((ElepayResult.Failed)elePayResult).getError();
                    JSONObject res = new JSONObject();
                    try {
                        res.put("state", "failed");
                        res.put("paymentId", id);

                        JSONObject jsonError = new JSONObject();
                        if (error instanceof ElepayError.UnsupportedPaymentMethod) {
                            ElepayError.UnsupportedPaymentMethod e = ((ElepayError.UnsupportedPaymentMethod) error);
                            res.put("code", "");
                            res.put("reason", "Unsupported payment method.");
                            res.put("message", e.getPaymentMethod());
                        } else if (error instanceof ElepayError.AlreadyMakingPayment) {
                            ElepayError.AlreadyMakingPayment e = ((ElepayError.AlreadyMakingPayment) error);
                            res.put("code", "");
                            res.put("reason", "Already making payment");
                            res.put("message", e.getPaymentId());
                        } else if (error instanceof ElepayError.InvalidPayload) {
                            ElepayError.InvalidPayload e = ((ElepayError.InvalidPayload) error);
                            res.put("code", e.getErrorCode());
                            res.put("reason", "Invalid payload");
                            res.put("message", e.getMessage());
                        } else if (error instanceof ElepayError.UninitializedPaymentMethod) {
                            ElepayError.UninitializedPaymentMethod e = ((ElepayError.UninitializedPaymentMethod) error);
                            res.put("code", e.getErrorCode());
                            res.put("reason", "Uninitialized payment method");
                            res.put("message", e.getPaymentMethod() + " " + e.getMessage());
                        } else if (error instanceof ElepayError.SystemError) {
                            ElepayError.SystemError e = ((ElepayError.SystemError) error);
                            res.put("code", e.getErrorCode());
                            res.put("reason", "System error");
                            res.put("message", e.getMessage());
                        } else if (error instanceof ElepayError.PaymentFailure) {
                            ElepayError.PaymentFailure e = ((ElepayError.PaymentFailure) error);
                            res.put("code", e.getErrorCode());
                            res.put("reason", "Payment failure");
                            res.put("message", e.getMessage());
                        } else if (error instanceof ElepayError.PermissionRequired) {
                            ElepayError.PermissionRequired e = ((ElepayError.PermissionRequired) error);
                            res.put("code", "");
                            res.put("reason", "Permission required");
                            res.put("message", TextUtils.join(", ", e.getPermissions()));
                        }

                        res.put("error", jsonError);
                    } catch (JSONException e) {}
                    callbackContext.error(res);
                }
            }
        });
    }
}