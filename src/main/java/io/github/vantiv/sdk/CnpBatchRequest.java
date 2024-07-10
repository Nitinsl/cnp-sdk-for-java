package io.github.vantiv.sdk;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Properties;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

import io.github.vantiv.sdk.generate.*;
import org.bouncycastle.openpgp.PGPException;

public class CnpBatchRequest {
	private BatchRequest batchRequest;
	private JAXBContext jc;
	private File file;
	private Marshaller marshaller;
	ObjectFactory objFac;
	TransactionType txn;
	String filePath;
	OutputStream osWrttxn;


	int numOfTxn;


	private final int maxTransactionsPerBatch;
	protected int cnpLimit_maxTransactionsPerBatch = 100000;
	private final CnpBatchFileRequest lbfr;

	/**
	 * This method initializes the batch level attributes of the XML and checks if the maxTransactionsPerBatch is not more than the value provided in the properties file
	 * @param merchantId
	 * @param lbfr
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 */
	CnpBatchRequest(String merchantId, CnpBatchFileRequest lbfr) throws CnpBatchException{
		this.batchRequest = new BatchRequest();
		this.batchRequest.setMerchantId(merchantId);
		this.batchRequest.setMerchantSdk(Versions.SDK_VERSION);
		this.objFac = new ObjectFactory();
		this.lbfr = lbfr;
		File tmpFile = new File(lbfr.getConfig().getProperty("batchRequestFolder")+"/tmp");
		if(!tmpFile.exists()) {
			tmpFile.mkdir();
		}
		String dateString = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss.SSS").format(new java.util.Date());
		filePath = new String(lbfr.getConfig().getProperty("batchRequestFolder")+ "/tmp/Transactions" + merchantId + dateString);
		numOfTxn = 0;
		try {
			this.jc = JAXBContext.newInstance("io.github.vantiv.sdk.generate");
			marshaller = jc.createMarshaller();
			// JAXB_FRAGMENT property required to prevent unnecessary XML info from being printed in the file during marshal.
			marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
			// Proper formatting of XML purely for aesthetic purposes.
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		} catch (JAXBException e) {
			throw new CnpBatchException("Unable to load jaxb dependencies.  Perhaps a classpath issue?", e);
		}
		this.maxTransactionsPerBatch = Integer.parseInt(lbfr.getConfig().getProperty("maxTransactionsPerBatch","10000"));
		if( maxTransactionsPerBatch > cnpLimit_maxTransactionsPerBatch ){
			throw new CnpBatchException("maxTransactionsPerBatch property value cannot exceed " + String.valueOf(cnpLimit_maxTransactionsPerBatch));
		}
	}

	BatchRequest getBatchRequest(){
		return batchRequest;
	}

	/**
	 * This method is used to add transaction to a particular batch
	 * @param transactionType
	 * @return
	 * @throws FileNotFoundException
	 * @throws JAXBException
	 */
	public TransactionCodeEnum addTransaction(CnpTransactionInterface transactionType) throws CnpBatchException {
        if (numOfTxn == 0) {
            Properties properties = lbfr.getConfig();
            this.file = new File(filePath);
            try {
                if ("true".equalsIgnoreCase(properties.getProperty("useEncryption"))) {
                    osWrttxn = PgpHelper.encryptionStream(filePath, properties.getProperty("PublicKeyPath"));
                } else {
                    osWrttxn = new FileOutputStream(file.getAbsolutePath());
                }
            } catch (FileNotFoundException e) {
                throw new CnpBatchException("There was an exception while trying to create a Request file. Please check if the folder: " + properties.getProperty("batchRequestFolder") + " has read and write access. ");
            } catch (IOException ioe) {
                throw new CnpBatchException("Could not read merchant public key at " + properties.getProperty("PublicKeyPath") +
                        "\nMake sure that the provided public key path is correct", ioe);
            } catch (PGPException pgpe) {
                throw new CnpBatchException("There was an error while trying to read merchant public key at " + properties.getProperty("PublicKeyPath") +
                        "\nMake sure that the provided public key path contains a valid public key", pgpe);
            }
        }

        if (numOfTxn > 0 && batchRequest.getNumAccountUpdates().intValue() != numOfTxn
                && (transactionType instanceof AccountUpdate)) {
            throw new CnpBatchException("An account update cannot be added to a batch containing transactions other than other AccountUpdates.");
        } else if (numOfTxn > 0 && batchRequest.getNumAccountUpdates().intValue() == numOfTxn &&
                !(transactionType instanceof AccountUpdate)) {
            throw new CnpBatchException("Transactions that are not AccountUpdates cannot be added to a batch containing AccountUpdates.");
        }

        TransactionCodeEnum batchFileStatus = verifyFileThresholds();
        if (batchFileStatus == TransactionCodeEnum.FILEFULL) {
            Exception e = new Exception();
            throw new CnpBatchFileFullException("Batch File is already full -- it has reached the maximum number of transactions allowed per batch file.", e);
        } else if (batchFileStatus == TransactionCodeEnum.BATCHFULL) {
            Exception e = new Exception();
            throw new CnpBatchBatchFullException("Batch is already full -- it has reached the maximum number of transactions allowed per batch.", e);
        }

        //Adding 1 to the number of transaction. This is on the assumption that we are adding one transaction to the batch at a time.
        BigInteger numToAdd = new BigInteger("1");
        boolean transactionAdded = false;

        JAXBElement<?> transaction;

        if (transactionType instanceof Sale) {
            batchRequest.setNumSales(batchRequest.getNumSales().add(BigInteger.valueOf(1)));
            batchRequest.setSaleAmount(batchRequest.getSaleAmount().add(BigInteger.valueOf(((Sale) transactionType).getAmount())));
            transaction = objFac.createSale((Sale) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if (transactionType instanceof Authorization) {
            batchRequest.setNumAuths(batchRequest.getNumAuths().add(BigInteger.valueOf(1)));
            batchRequest.setAuthAmount(batchRequest.getAuthAmount().add(BigInteger.valueOf(((Authorization) transactionType).getAmount())));
            transaction = objFac.createAuthorization((Authorization) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if (transactionType instanceof Credit) {
            batchRequest.setNumCredits(batchRequest.getNumCredits().add(BigInteger.valueOf(1)));
            batchRequest.setCreditAmount(batchRequest.getCreditAmount().add(BigInteger.valueOf(((Credit) transactionType).getAmount())));
            transaction = objFac.createCredit((Credit) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if (transactionType instanceof RegisterTokenRequestType) {
            batchRequest.setNumTokenRegistrations(batchRequest.getNumTokenRegistrations().add(BigInteger.valueOf(1)));
            transaction = objFac.createRegisterTokenRequest((RegisterTokenRequestType) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if (transactionType instanceof CaptureGivenAuth) {
            batchRequest.setNumCaptureGivenAuths(batchRequest.getNumCaptureGivenAuths().add(BigInteger.valueOf(1)));
            batchRequest.setCaptureGivenAuthAmount(batchRequest.getCaptureGivenAuthAmount().add(BigInteger.valueOf(((CaptureGivenAuth) transactionType).getAmount())));
            transaction = objFac.createCaptureGivenAuth((CaptureGivenAuth) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if (transactionType instanceof ForceCapture) {
            batchRequest.setNumForceCaptures(batchRequest.getNumForceCaptures().add(BigInteger.valueOf(1)));
            batchRequest.setForceCaptureAmount(batchRequest.getForceCaptureAmount().add(BigInteger.valueOf(((ForceCapture) transactionType).getAmount())));
            transaction = objFac.createForceCapture((ForceCapture) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if (transactionType instanceof AuthReversal) {
            batchRequest.setNumAuthReversals(batchRequest.getNumAuthReversals().add(BigInteger.valueOf(1)));
            batchRequest.setAuthReversalAmount(batchRequest.getAuthReversalAmount().add(BigInteger.valueOf(((AuthReversal) transactionType).getAmount())));
            transaction = objFac.createAuthReversal((AuthReversal) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if (transactionType instanceof DepositTransactionReversal) {
            batchRequest.setNumDepositTransactionReversals(batchRequest.getNumDepositTransactionReversals().add(BigInteger.valueOf(1)));
            batchRequest.setDepositTransactionReversalAmount(batchRequest.getDepositTransactionReversalAmount().add(BigInteger.valueOf(((DepositTransactionReversal) transactionType).getAmount())));
            transaction = objFac.createDepositTransactionReversal((DepositTransactionReversal) transactionType);
            transactionAdded = true;
            numOfTxn++;
        }else if (transactionType instanceof RefundTransactionReversal) {
            batchRequest.setNumRefundTransactionReversals(batchRequest.getNumRefundTransactionReversals().add(BigInteger.valueOf(1)));
            batchRequest.setRefundTransactionReversalAmount(batchRequest.getRefundTransactionReversalAmount().add(BigInteger.valueOf(((RefundTransactionReversal) transactionType).getAmount())));
            transaction = objFac.createRefundTransactionReversal((RefundTransactionReversal) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if (transactionType instanceof Capture) {
            batchRequest.setNumCaptures(batchRequest.getNumCaptures().add(BigInteger.valueOf(1)));
            batchRequest.setCaptureAmount(batchRequest.getCaptureAmount().add(BigInteger.valueOf(((Capture) transactionType).getAmount())));
            transaction = objFac.createCapture((Capture) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if (transactionType instanceof EcheckVerification) {
            batchRequest.setNumEcheckVerification(batchRequest.getNumEcheckVerification().add(BigInteger.valueOf(1)));
            batchRequest.setEcheckVerificationAmount(batchRequest.getEcheckVerificationAmount().add(BigInteger.valueOf(((EcheckVerification) transactionType).getAmount())));
            transaction = objFac.createEcheckVerification((EcheckVerification) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if (transactionType instanceof EcheckCredit) {
            batchRequest.setNumEcheckCredit(batchRequest.getNumEcheckCredit().add(BigInteger.valueOf(1)));
            batchRequest.setEcheckCreditAmount(batchRequest.getEcheckCreditAmount().add(BigInteger.valueOf(((EcheckCredit) transactionType).getAmount())));
            transaction = objFac.createEcheckCredit((EcheckCredit) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if (transactionType instanceof EcheckRedeposit) {
            batchRequest.setNumEcheckRedeposit(batchRequest.getNumEcheckRedeposit().add(BigInteger.valueOf(1)));
            transaction = objFac.createEcheckRedeposit((EcheckRedeposit) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if (transactionType instanceof EcheckSale) {
            batchRequest.setNumEcheckSales(batchRequest.getNumEcheckSales().add(BigInteger.valueOf(1)));
            batchRequest.setEcheckSalesAmount(batchRequest.getEcheckSalesAmount().add(BigInteger.valueOf(((EcheckSale) transactionType).getAmount())));
            transaction = objFac.createEcheckSale((EcheckSale) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if (transactionType instanceof UpdateCardValidationNumOnToken) {
            batchRequest.setNumUpdateCardValidationNumOnTokens(batchRequest.getNumUpdateCardValidationNumOnTokens().add(BigInteger.valueOf(1)));
            transaction = objFac.createUpdateCardValidationNumOnToken((UpdateCardValidationNumOnToken) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if (transactionType instanceof UpdateSubscription) {
            batchRequest.setNumUpdateSubscriptions(batchRequest.getNumUpdateSubscriptions().add(BigInteger.valueOf(1)));
            transaction = objFac.createUpdateSubscription((UpdateSubscription) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if (transactionType instanceof CancelSubscription) {
            batchRequest.setNumCancelSubscriptions(batchRequest.getNumCancelSubscriptions().add(BigInteger.valueOf(1)));
            transaction = objFac.createCancelSubscription((CancelSubscription) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if (transactionType instanceof CreatePlan) {
            batchRequest.setNumCreatePlans(batchRequest.getNumCreatePlans().add(BigInteger.valueOf(1)));
            transaction = objFac.createCreatePlan((CreatePlan) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if (transactionType instanceof UpdatePlan) {
            batchRequest.setNumUpdatePlans(batchRequest.getNumUpdatePlans().add(BigInteger.valueOf(1)));
            transaction = objFac.createUpdatePlan((UpdatePlan) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if (transactionType instanceof Activate) {
            batchRequest.setNumActivates(batchRequest.getNumActivates().add(BigInteger.valueOf(1)));
            batchRequest.setActivateAmount(batchRequest.getActivateAmount().add(BigInteger.valueOf(((Activate) transactionType).getAmount())));
            transaction = objFac.createActivate((Activate) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if (transactionType instanceof Deactivate) {
            batchRequest.setNumDeactivates(batchRequest.getNumDeactivates().add(BigInteger.valueOf(1)));
            transaction = objFac.createDeactivate((Deactivate) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if (transactionType instanceof Load) {
            batchRequest.setNumLoads(batchRequest.getNumLoads().add(BigInteger.valueOf(1)));
            batchRequest.setLoadAmount(batchRequest.getLoadAmount().add(BigInteger.valueOf(((Load) transactionType).getAmount())));
            transaction = objFac.createLoad((Load) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if (transactionType instanceof Unload) {
            batchRequest.setNumUnloads(batchRequest.getNumUnloads().add(BigInteger.valueOf(1)));
            batchRequest.setUnloadAmount(batchRequest.getUnloadAmount().add(BigInteger.valueOf(((Unload) transactionType).getAmount())));
            transaction = objFac.createUnload((Unload) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if (transactionType instanceof BalanceInquiry) {
            batchRequest.setNumBalanceInquirys(batchRequest.getNumBalanceInquirys().add(BigInteger.valueOf(1)));
            transaction = objFac.createBalanceInquiry((BalanceInquiry) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if (transactionType instanceof EcheckPreNoteSale) {
            batchRequest.setNumEcheckPreNoteSale(batchRequest.getNumEcheckPreNoteSale().add(BigInteger.valueOf(1)));
            transaction = objFac.createEcheckPreNoteSale((EcheckPreNoteSale) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if (transactionType instanceof EcheckPreNoteCredit) {
            batchRequest.setNumEcheckPreNoteCredit(batchRequest.getNumEcheckPreNoteCredit().add(BigInteger.valueOf(1)));
            transaction = objFac.createEcheckPreNoteCredit((EcheckPreNoteCredit) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if (transactionType instanceof PayFacCredit) {
            batchRequest.setNumPayFacCredit(batchRequest.getNumPayFacCredit().add(BigInteger.valueOf(1)));
            batchRequest.setPayFacCreditAmount((batchRequest.getPayFacCreditAmount().add(BigInteger.valueOf(((PayFacCredit) transactionType).getAmount()))));
            transaction = objFac.createPayFacCredit((PayFacCredit) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if (transactionType instanceof SubmerchantCredit) {
            batchRequest.setNumSubmerchantCredit(batchRequest.getNumSubmerchantCredit().add(BigInteger.valueOf(1)));
            batchRequest.setSubmerchantCreditAmount((batchRequest.getSubmerchantCreditAmount().add(BigInteger.valueOf(((SubmerchantCredit) transactionType).getAmount()))));
            transaction = objFac.createSubmerchantCredit((SubmerchantCredit) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if (transactionType instanceof VendorCredit) {
            batchRequest.setNumVendorCredit(batchRequest.getNumVendorCredit().add(BigInteger.valueOf(1)));
            batchRequest.setVendorCreditAmount((batchRequest.getVendorCreditAmount().add(BigInteger.valueOf(((VendorCredit) transactionType).getAmount()))));
            transaction = objFac.createVendorCredit((VendorCredit) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if(transactionType instanceof ReserveCredit) {
            batchRequest.setNumReserveCredit(batchRequest.getNumReserveCredit().add(BigInteger.valueOf(1)));
            batchRequest.setReserveCreditAmount((batchRequest.getReserveCreditAmount().add(BigInteger.valueOf(((ReserveCredit) transactionType).getAmount()))));
            transaction = objFac.createReserveCredit((ReserveCredit)transactionType);
            transactionAdded = true;
            numOfTxn ++;
        } else if(transactionType instanceof PhysicalCheckCredit) {
            batchRequest.setNumPhysicalCheckCredit(batchRequest.getNumPhysicalCheckCredit().add(BigInteger.valueOf(1)));
            batchRequest.setPhysicalCheckCreditAmount((batchRequest.getPhysicalCheckCreditAmount().add(BigInteger.valueOf(((PhysicalCheckCredit) transactionType).getAmount()))));
            transaction = objFac.createPhysicalCheckCredit((PhysicalCheckCredit)transactionType);
            transactionAdded = true;
            numOfTxn ++;
        } else if(transactionType instanceof PayFacDebit) {
            batchRequest.setNumPayFacDebit(batchRequest.getNumPayFacDebit().add(BigInteger.valueOf(1)));
            batchRequest.setPayFacDebitAmount((batchRequest.getPayFacDebitAmount().add(BigInteger.valueOf(((PayFacDebit) transactionType).getAmount()))));
            transaction = objFac.createPayFacDebit((PayFacDebit)transactionType);
            transactionAdded = true;
            numOfTxn ++;
        } else if(transactionType instanceof SubmerchantDebit) {
            batchRequest.setNumSubmerchantDebit(batchRequest.getNumSubmerchantDebit().add(BigInteger.valueOf(1)));
            batchRequest.setSubmerchantDebitAmount((batchRequest.getSubmerchantDebitAmount().add(BigInteger.valueOf(((SubmerchantDebit) transactionType).getAmount()))));
            transaction = objFac.createSubmerchantDebit((SubmerchantDebit)transactionType);
            transactionAdded = true;
            numOfTxn ++;
        } else if(transactionType instanceof VendorDebit) {
            batchRequest.setNumVendorDebit(batchRequest.getNumVendorDebit().add(BigInteger.valueOf(1)));
            batchRequest.setVendorDebitAmount((batchRequest.getVendorDebitAmount().add(BigInteger.valueOf(((VendorDebit) transactionType).getAmount()))));
            transaction = objFac.createVendorDebit((VendorDebit)transactionType);
            transactionAdded = true;
            numOfTxn ++;
        } else if(transactionType instanceof ReserveDebit) {
            batchRequest.setNumReserveDebit(batchRequest.getNumReserveDebit().add(BigInteger.valueOf(1)));
            batchRequest.setReserveDebitAmount((batchRequest.getReserveDebitAmount().add(BigInteger.valueOf(((ReserveDebit) transactionType).getAmount()))));
            transaction = objFac.createReserveDebit((ReserveDebit)transactionType);
            transactionAdded = true;
            numOfTxn ++;
        } else if(transactionType instanceof PhysicalCheckDebit) {
            batchRequest.setNumPhysicalCheckDebit(batchRequest.getNumPhysicalCheckDebit().add(BigInteger.valueOf(1)));
            batchRequest.setPhysicalCheckDebitAmount((batchRequest.getPhysicalCheckDebitAmount().add(BigInteger.valueOf(((PhysicalCheckDebit) transactionType).getAmount()))));
            transaction = objFac.createPhysicalCheckDebit((PhysicalCheckDebit)transactionType);
            transactionAdded = true;
            numOfTxn ++;
        } else if (transactionType instanceof AccountUpdate){
            batchRequest.setNumAccountUpdates(batchRequest.getNumAccountUpdates().add(BigInteger.valueOf(1)));
            transaction = objFac.createAccountUpdate((AccountUpdate)transactionType);
            transactionAdded = true;
            numOfTxn ++;
        } else if (transactionType instanceof FundingInstructionVoid){
            batchRequest.setNumFundingInstructionVoid(batchRequest.getNumFundingInstructionVoid().add(BigInteger.valueOf(1)));
            transaction = objFac.createFundingInstructionVoid((FundingInstructionVoid)transactionType);
            transactionAdded = true;
            numOfTxn ++;
        } else if (transactionType instanceof GiftCardAuthReversal) {
        	batchRequest.setNumGiftCardAuthReversals(batchRequest.getNumGiftCardAuthReversals().add(BigInteger.valueOf(1)));
        	batchRequest.setGiftCardAuthReversalOriginalAmount((batchRequest.getGiftCardAuthReversalOriginalAmount()
        			.add(BigInteger.valueOf(((GiftCardAuthReversal) transactionType).getOriginalAmount()))));
            transaction = objFac.createGiftCardAuthReversal((GiftCardAuthReversal)transactionType);
            transactionAdded = true;
            numOfTxn ++;
        } else if (transactionType instanceof GiftCardCapture) {
        	batchRequest.setNumGiftCardCaptures(batchRequest.getNumGiftCardCaptures().add(BigInteger.valueOf(1)));
        	batchRequest.setGiftCardCaptureAmount((batchRequest.getGiftCardCaptureAmount()
        			.add(BigInteger.valueOf(((GiftCardCapture) transactionType).getCaptureAmount()))));
            transaction = objFac.createGiftCardCapture((GiftCardCapture)transactionType);
            transactionAdded = true;
            numOfTxn ++;
        } else if (transactionType instanceof GiftCardCredit) {
        	batchRequest.setNumGiftCardCredits(batchRequest.getNumGiftCardCredits().add(BigInteger.valueOf(1)));
        	batchRequest.setGiftCardCreditAmount((batchRequest.getGiftCardCreditAmount()
        			.add(BigInteger.valueOf(((GiftCardCredit) transactionType).getCreditAmount()))));
            transaction = objFac.createGiftCardCredit((GiftCardCredit)transactionType);
            transactionAdded = true;
            numOfTxn ++;
        } else if(transactionType instanceof FastAccessFunding) {
            batchRequest.setNumFastAccessFunding(batchRequest.getNumFastAccessFunding().add(BigInteger.valueOf(1)));
            batchRequest.setFastAccessFundingAmount((batchRequest.getFastAccessFundingAmount().add(BigInteger.valueOf(((FastAccessFunding) transactionType).getAmount()))));
            transaction = objFac.createFastAccessFunding((FastAccessFunding)transactionType);
            transactionAdded = true;
            numOfTxn ++;
        } else if(transactionType instanceof TranslateToLowValueTokenRequestType){
		    batchRequest.setNumTranslateToLowValueTokenRequests(batchRequest.getNumTranslateToLowValueTokenRequests().add(BigInteger.valueOf(1)));
		    transaction = objFac.createTranslateToLowValueTokenRequest((TranslateToLowValueTokenRequestType) transactionType);
		    transactionAdded = true;
		    numOfTxn++;
        } else if(transactionType instanceof CustomerCredit){
            batchRequest.setNumCustomerCredit(batchRequest.getNumCustomerCredit().add(BigInteger.valueOf(1)));
            batchRequest.setCustomerCreditAmount(batchRequest.getCustomerCreditAmount().add(BigInteger.valueOf(((CustomerCredit) transactionType).getAmount())));
            transaction = objFac.createCustomerCredit((CustomerCredit) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if(transactionType instanceof CustomerDebit) {
            batchRequest.setNumCustomerDebit(batchRequest.getNumCustomerDebit().add(BigInteger.valueOf(1)));
            batchRequest.setCustomerDebitAmount(batchRequest.getCustomerDebitAmount().add(BigInteger.valueOf(((CustomerDebit) transactionType).getAmount())));
            transaction = objFac.createCustomerDebit((CustomerDebit) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if(transactionType instanceof PayoutOrgCredit) {
            batchRequest.setNumPayoutOrgCredit(batchRequest.getNumPayoutOrgCredit().add(BigInteger.valueOf(1)));
            batchRequest.setPayoutOrgCreditAmount(batchRequest.getPayoutOrgCreditAmount().add(BigInteger.valueOf(((PayoutOrgCredit) transactionType).getAmount())));
            transaction = objFac.createPayoutOrgCredit((PayoutOrgCredit) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else if(transactionType instanceof PayoutOrgDebit) {
            batchRequest.setNumPayoutOrgDebit(batchRequest.getNumPayoutOrgDebit().add(BigInteger.valueOf(1)));
            batchRequest.setPayoutOrgDebitAmount(batchRequest.getPayoutOrgDebitAmount().add(BigInteger.valueOf(((PayoutOrgDebit) transactionType).getAmount())));
            transaction = objFac.createPayoutOrgDebit((PayoutOrgDebit) transactionType);
            transactionAdded = true;
            numOfTxn++;
        } else {
            transaction = objFac.createTransaction(new TransactionType());
        }

        try {
            marshaller.marshal(transaction, osWrttxn);
        } catch (JAXBException e) {
            throw new CnpBatchException("There was an exception while marshalling the transaction object.", e);
        }

        batchFileStatus = verifyFileThresholds();
        if( batchFileStatus == TransactionCodeEnum.FILEFULL){
            return TransactionCodeEnum.FILEFULL;
        } else if( batchFileStatus == TransactionCodeEnum.BATCHFULL ){
            return TransactionCodeEnum.BATCHFULL;
        }

        if (transactionAdded) {
            return TransactionCodeEnum.SUCCESS;
        } else {
            return TransactionCodeEnum.FAILURE;
        }
	}

	/**
	 * This method makes sure that the maximum number of transactions per batch and file is not exceeded
	 * This is to ensure Performance.
	 * @return
	 */
	TransactionCodeEnum verifyFileThresholds(){
		if( this.lbfr.getNumberOfTransactionInFile() == this.lbfr.getMaxAllowedTransactionsPerFile()){
			return TransactionCodeEnum.FILEFULL;
		}
		else if( getNumberOfTransactions() == this.maxTransactionsPerBatch ){
			return TransactionCodeEnum.BATCHFULL;
		}
		return TransactionCodeEnum.SUCCESS;
	}

	/**
	 * Returns the number of transactions in the batch
	 * @return
	 */
	public int getNumberOfTransactions(){
		return (numOfTxn);
	}

	/**
	 * Gets whether the batch is full per the size specification
	 * @return boolean indicating whether the batch is full
	 */
	public boolean isFull() {
		return (getNumberOfTransactions() == this.maxTransactionsPerBatch);
	}

	/**
	 * Closes the batch output file
	 * @throws IOException
	 */
	public void closeFile() throws IOException {
		osWrttxn.close();
	}

	/**
	 * Grabs the request file
	 * @return the request file
	 */
	public File getFile() {
		return this.file;
	}

    public Marshaller getMarshaller() {
        return marshaller;
    }

    public void setMarshaller(Marshaller marshaller) {
        this.marshaller = marshaller;
    }

    public int getNumOfTxn() {
        return numOfTxn;
    }

    public void setNumOfTxn(int numOfTxn) {
        this.numOfTxn = numOfTxn;
    }



}
