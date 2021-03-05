package com.jenzabar.jxntm.integration.bizapi.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import net.authorize.Environment;
import net.authorize.api.contract.v1.ArrayOfSetting;
import net.authorize.api.contract.v1.GetHostedPaymentPageRequest;
import net.authorize.api.contract.v1.GetHostedPaymentPageResponse;
import net.authorize.api.contract.v1.MerchantAuthenticationType;
import net.authorize.api.contract.v1.MessageTypeEnum;
import net.authorize.api.contract.v1.SettingType;
import net.authorize.api.contract.v1.TransactionRequestType;
import net.authorize.api.contract.v1.TransactionTypeEnum;
import net.authorize.api.controller.GetHostedPaymentPageController;
import net.authorize.api.controller.base.ApiOperationBase;
import net.jenzabar.util.ReasonException;
import net.jenzabar.util.logging.AppLogger;

public class AuthnetPaymentGatewayIFImpl extends AbstractPaymentGatewayIFImpl {

  private static final String _className = AuthnetPaymentGatewayIFImpl.class.getName();
  private static final AppLogger _logger = AppLogger.getAppLogger(_className);
	private static final String SIGNATURE = "Signature";

  private Map _hostedPaymentReturnOptions = null;
  private Map _hostedPaymentButtonOptions = null;
  private Map _hostedPaymentStyleOptions = null;
  private Map _hostedPaymentOrderOptions = null;
  private Map _hostedPaymentSecurityOptions = null;
  private Map _hostedPaymentBillingAddressOptions = null;
  private Map _hostedPaymentCustomerOptions = null;
  
  public AuthnetPaymentGatewayIFImpl(Map hostedPaymentReturnOptions$, 
		  Map hostedPaymentButtonOptions$,
		  Map hostedPaymentStyleOptions$,
		  Map hostedPaymentSecurityOptions$,
		  Map hostedPaymentBillingAddressOptions$,
		  Map hostedPaymentCustomerOptions$,
		  Map hostedPaymentOrderOptions$
		  ) {
	  _hostedPaymentReturnOptions = hostedPaymentReturnOptions$;
	  _hostedPaymentButtonOptions = hostedPaymentButtonOptions$;
	  _hostedPaymentStyleOptions = hostedPaymentStyleOptions$;
	  _hostedPaymentSecurityOptions = hostedPaymentSecurityOptions$;
	  _hostedPaymentBillingAddressOptions = hostedPaymentBillingAddressOptions$;
	  _hostedPaymentCustomerOptions = hostedPaymentCustomerOptions$;
	  _hostedPaymentOrderOptions = hostedPaymentOrderOptions$;
  }
  public boolean is40Gateway(){
		 return false;
	  }

  private SettingType getSettings(String settingsName$, Map settingsMap$) {
      SettingType setting = new SettingType();
      setting.setSettingName(settingsName$);
      StringBuffer strMap = new StringBuffer();
      strMap.append("{");
      boolean first = true;
      for(Object key : settingsMap$.keySet()) {
    	  if (!first)  strMap.append(",");    		  
    	  strMap.append("\"");
    	  strMap.append(key.toString());
    	  strMap.append("\":");
    	  if (!(settingsMap$.get(key) instanceof Boolean))
        	  strMap.append("\"");
    	  strMap.append(settingsMap$.get(key).toString());
    	  if (!(settingsMap$.get(key) instanceof Boolean))
        	  strMap.append("\"");
    	  first = false;
      }
      strMap.append("}");
      _logger.debug(strMap.length() + " :" + strMap);      
      setting.setSettingValue(strMap.toString());
	  return setting;
  }
  private String getToken(String paymentAmount$, final Map<String, String> appMap$) throws Exception {
	  ApiOperationBase.setEnvironment(Environment.SANDBOX);
	  String transactionKey = getConfigValue("transaction_key");
	  String loginID = getConfigValue("api_login_id");

      MerchantAuthenticationType merchantAuthenticationType  = new MerchantAuthenticationType() ;
      merchantAuthenticationType.setName(loginID);
      merchantAuthenticationType.setTransactionKey(transactionKey);
      ApiOperationBase.setMerchantAuthentication(merchantAuthenticationType);
      
      // Create the payment transaction request
      TransactionRequestType txnRequest = new TransactionRequestType();
      txnRequest.setTransactionType(TransactionTypeEnum.AUTH_CAPTURE_TRANSACTION.value());
      txnRequest.setAmount(new BigDecimal(paymentAmount$).setScale(2, RoundingMode.CEILING));

      ArrayOfSetting alist = new ArrayOfSetting();
      _hostedPaymentReturnOptions.put("url", getReturnURL("1", appMap$));
      _hostedPaymentReturnOptions.put("cancelUrl", getReturnURL("0", appMap$));
      alist.getSetting().add(getSettings("hostedPaymentReturnOptions", _hostedPaymentReturnOptions));
      alist.getSetting().add(getSettings("hostedPaymentButtonOptions", _hostedPaymentButtonOptions));
      alist.getSetting().add(getSettings("hostedPaymentStyleOptions", _hostedPaymentStyleOptions));
      alist.getSetting().add(getSettings("hostedPaymentSecurityOptions", _hostedPaymentSecurityOptions));
      alist.getSetting().add(getSettings("hostedPaymentBillingAddressOptions", _hostedPaymentBillingAddressOptions));
      alist.getSetting().add(getSettings("hostedPaymentCustomerOptions", _hostedPaymentCustomerOptions));
      alist.getSetting().add(getSettings("hostedPaymentOrderOptions", _hostedPaymentOrderOptions));

      GetHostedPaymentPageRequest apiRequest = new GetHostedPaymentPageRequest();
      apiRequest.setTransactionRequest(txnRequest);
      apiRequest.setHostedPaymentSettings(alist);

      GetHostedPaymentPageController controller = new GetHostedPaymentPageController(apiRequest);
      controller.execute();
     
      GetHostedPaymentPageResponse response = new GetHostedPaymentPageResponse();
      response = controller.getApiResponse();

		if (response!=null) {
           if (response.getMessages().getResultCode() == MessageTypeEnum.OK) {
        	   _logger.debug(response.getMessages().getMessage().get(0).getCode());
        	   _logger.debug(response.getMessages().getMessage().get(0).getText());
        	   return response.getToken();
          }
          else
          {
        	_logger.info("Failed to get hosted payment page:  " + response.getMessages().getResultCode());
        	_logger.info(response.getMessages().getMessage().get(0).getCode());
        	_logger.info(response.getMessages().getMessage().get(0).getText());
          }
      }
	  throw new Exception("Unable to generate token");
	
  }
  private String getReturnURL(String returnCode$, final Map<String, String> appMap$) {
	  String url= appMap$.get("CallBackURL") + "?PaymentGatewayActivityID=" + appMap$.get("PaymentGatewayActivityID") + "%26ResponseCode=" + returnCode$ + "%26" + SIGNATURE + "=" + sign(appMap$.get("PaymentGatewayActivityID"));
	return url;
  }
  /**
   * This is called before we start authorizing the payment.
   * Generate a Map with key value pairs to generate the form
   * 
   * @return Map of name/value pairs for the form 
   */ 
  protected Map<String, String> beforeAuthorizePayment(final Map<String, String> appMap$) throws ReasonException{
	  	Map<String, String> paramsMap = new HashMap<String, String>();
	  	try
		{
	  		String netPaymentAmount = appMap$.get("TransactionAmount");
	  		paramsMap.put("token", getToken(netPaymentAmount, appMap$));
		}
		catch(Exception e){
			_logger.debug(e.getMessage(), e);
		}
		return paramsMap; 	 
  }
  
  private String sign(Object paymentGatewayActivityID$) {
  	try
  	{  		
  		String transactionKey = getConfigValue("transaction_key");
  		String loginID = getConfigValue("api_login_id");
  		String inputstring = transactionKey + "^" + loginID + "^" + paymentGatewayActivityID$.toString() + "^";
  		return String.valueOf(inputstring.hashCode());
  	}catch(Exception e){
  		throw new RuntimeException(e);
  	}
  }

  protected boolean isResponseAuthentic(Map result$) {
		String sent_signature = (String)result$.get(SIGNATURE);
		String calculatedSignature = sign(result$.get("PaymentGatewayActivityID"));	
		_logger.debug("'sent_signature=" + sent_signature + "'\n 'calculatedSignature=" + calculatedSignature + "'");		
		return (null != sent_signature && sent_signature.trim().equals(calculatedSignature.trim()));
  }
  
	 public String getPaymentGatewayName(){
		 return "authnetnew";
	 }
}
