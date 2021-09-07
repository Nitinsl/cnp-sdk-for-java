package io.github.vantiv.sdk;

import java.util.Calendar;

//import com.cnp.sdk.generate.*;
import io.github.vantiv.sdk.generate.*;

public class Test {

    /**
     * @param args
     */
    public static void main(String[] args) {
        String merchantId = "07103229";

        RFRRequest rfr = new RFRRequest();
        AccountUpdateFileRequestData data = new AccountUpdateFileRequestData();
        data.setMerchantId(merchantId);
        data.setPostDay(Calendar.getInstance());
        rfr.setAccountUpdateFileRequestData(data);

        CnpRFRFileRequest rfrFile = new CnpRFRFileRequest("test-rfr.xml", rfr);
        CnpRFRFileResponse responseFile = rfrFile.sendToCnpSFTP();
        CnpRFRResponse response = responseFile.getCnpRFRResponse();
        System.out.println("RFR Response Code: " + response.getRFRResponseCode());
        System.out.println("RFR Response Message: " + response.getRFRResponseMessage());



        CnpBatchFileRequest batchfile = new CnpBatchFileRequest("testFile.xml");

        CnpBatchRequest batch = batchfile.createBatch(merchantId);

        Credit credit = new Credit();
        credit.setAmount(106L);
        credit.setOrderId("hurrrr");
        credit.setOrderSource(OrderSourceType.TELEPHONE);
        credit.setReportGroup("Planets");
        CardType card = new CardType();
        card.setType(MethodOfPaymentTypeEnum.VI);
        card.setNumber("4100000000000001");
        card.setExpDate("1210");

        credit.setCard(card);
        batch.addTransaction(credit);

        CnpBatchFileResponse fileResponse = batchfile.sendToCnpSFTP();
        CnpBatchResponse batchResponse;

        //iterate over all batch responses in the file
        while((batchResponse = fileResponse.getNextCnpBatchResponse()) != null){
            //iterate over all transactions in the file with a custom response processor
            while(batchResponse.processNextTransaction(new CnpResponseProcessor(){
                public void processAuthorizationResponse(AuthorizationResponse authorizationResponse) { }

                public void processCaptureResponse(CaptureResponse captureResponse) { }

                public void processForceCaptureResponse(ForceCaptureResponse forceCaptureResponse) { }

                public void processCaptureGivenAuthResponse(CaptureGivenAuthResponse captureGivenAuthResponse) { }

                public void processSaleResponse(SaleResponse saleResponse) { }

                public void processCreditResponse(CreditResponse creditResponse) {
                    System.out.println(creditResponse.getCnpTxnId()); 
                    System.out.println(creditResponse.getResponseTime().toString());
                    System.out.println(creditResponse.getMessage());
                }

                public void processEcheckSalesResponse(EcheckSalesResponse echeckSalesResponse) { }

                public void processEcheckCreditResponse(EcheckCreditResponse echeckCreditResponse) { }

                public void processEcheckVerificationResponse(EcheckVerificationResponse echeckVerificationResponse) { }

                public void processEcheckRedepositResponse(EcheckRedepositResponse echeckRedepositResponse) { }

                public void processAuthReversalResponse(AuthReversalResponse authReversalResponse) { }

                public void processRegisterTokenResponse(RegisterTokenResponse registerTokenResponse) { }

                public void processAccountUpdateResponse(AccountUpdateResponse accountUpdateResponse) {
                }

                public void processUpdateSubscriptionResponse(UpdateSubscriptionResponse updateSubscriptionResponse) {
                }

                public void processCancelSubscriptionResponse(CancelSubscriptionResponse cancelSubscriptionResponse) {
                }

                public void processUpdateCardValidationNumOnTokenResponse(
                        UpdateCardValidationNumOnTokenResponse updateCardValidationNumOnTokenResponse) {
                }

                public void processCreatePlanResponse(CreatePlanResponse createPlanResponse) {
                }

                public void processUpdatePlanResponse(UpdatePlanResponse updatePlanResponse) {
                }

                public void processActivateResponse(ActivateResponse activateResponse) {
                }

                public void processDeactivateResponse(DeactivateResponse deactivateResponse) {
                }

                public void processLoadResponse(LoadResponse loadResponse) {
                }

                public void processUnloadResponse(UnloadResponse unloadResponse) {
                }

                public void processBalanceInquiryResponse(BalanceInquiryResponse balanceInquiryResponse) {
                }

                public void processEcheckPreNoteSaleResponse(
                        EcheckPreNoteSaleResponse echeckPreNoteSaleResponse) {
                }

                public void processEcheckPreNoteCreditResponse(
                        EcheckPreNoteCreditResponse echeckPreNoteCreditResponse) {
                }

                public void processSubmerchantCreditResponse(
                		SubmerchantCreditResponse submerchantCreditResponse) {
                }

                public void processPayFacCreditResponse(
                        PayFacCreditResponse payFacCreditResponse) {
                }

                public void processVendorCreditResponse(
                        VendorCreditResponse vendorCreditResponse) {
                }

                public void processReserveCreditResponse(
                        ReserveCreditResponse reserveCreditResponse) {
                }

                public void processPhysicalCheckCreditResponse(
                        PhysicalCheckCreditResponse checkCreditResponse) {
                }

                public void processSubmerchantDebitResponse(
                        SubmerchantDebitResponse submerchantDebitResponse) {
                }

                public void processPayFacDebitResponse(
                        PayFacDebitResponse payFacDebitResponse) {
                }

                public void processVendorDebitResponse(
                        VendorDebitResponse vendorDebitResponse) {
                }

                public void processReserveDebitResponse(
                        ReserveDebitResponse reserveDebitResponse) {
                }

                public void processPhysicalCheckDebitResponse(
                        PhysicalCheckDebitResponse checkDebitResponse) {
                }

                public void processFundingInstructionVoidResponse(
                        FundingInstructionVoidResponse fundingInstructionVoidResponse) {
                }
                
                public void processGiftCardAuthReversalResponse(
                		GiftCardAuthReversalResponse giftCardAuthReversalResponse) {
                }
                
				public void processGiftCardCaptureResponse(GiftCardCaptureResponse giftCardCaptureResponse) {
				}
				
				public void processGiftCardCreditResponse(GiftCardCreditResponse giftCardCreditResponse) {
				}

                public void processFastAccessFundingResponse(FastAccessFundingResponse fastAccessFundingResponse) {
                }

                public void processTranslateToLowValueTokenResponse (TranslateToLowValueTokenResponse  translateToLowValueTokenResponse){
                }

                public void processCustomerCreditResponse(CustomerCreditResponse customerCreditResponse) {
                }

                public void processCustomerDebitResponse(CustomerDebitResponse customerDebitResponse) {
                }

                public void processPayoutOrgCreditResponse(PayoutOrgCreditResponse payoutOrgCreditResponse) {
                }

                public void processPayoutOrgDebitResponse(PayoutOrgDebitResponse payoutOrgDebitResponse) {
                }

                public void processDepositTransactionReversalResponse(DepositTransactionReversalResponse depositTransactionReversalResponse) {
                }

                public void processRefundTransactionReversalResponse(RefundTransactionReversalResponse refundTransactionReversalResponse) {
                }
            })){
                System.out.println("Processed another txn!");
            }
        }

    }

}
