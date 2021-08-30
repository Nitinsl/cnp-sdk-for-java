package io.github.vantiv.sdk;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import io.github.vantiv.sdk.generate.CnpResponse;

/**
 * Provides abstraction over Cnp Requests containing  batch requests and Cnp Requests containing RFR requests
 * @author ahammond
 *
 */

abstract class CnpFileResponse implements AutoCloseable {

    protected JAXBContext jc;
    protected CnpResponse cnpResponse;
    protected Unmarshaller unmarshaller;
    protected File xmlFile;
    ResponseFileParser responseFileParser;

    public CnpFileResponse(File xmlFile) throws CnpBatchException{
        // convert from xml to objects

        try {
            this.xmlFile = xmlFile;
            responseFileParser = new ResponseFileParser(xmlFile);
            String cnpResponseXml = responseFileParser.getNextTag("cnpResponse");

            jc = JAXBContext.newInstance("com.cnp.sdk.generate");
            unmarshaller = jc.createUnmarshaller();
            cnpResponse = (CnpResponse) unmarshaller.unmarshal(new StringReader(cnpResponseXml));
        } catch (JAXBException e) {
            throw new CnpBatchException("There was an exception while unmarshalling the response file. Check your JAXB dependencies.", e);
        } catch (Exception e) {
            throw new CnpBatchException("There was an exception while reading the Cnp response file. The response file might not have been generated. Try re-sending the request file or contact us.", e);
        }
    }

    public File getFile() {
        return xmlFile;
    }

    public long getCnpSessionId() {
        return this.cnpResponse.getCnpSessionId();
    }

    public String getVersion() {
        return this.cnpResponse.getVersion();
    }

    public String getResponse() {
        return this.cnpResponse.getResponse();
    }

    public String getMessage() {
        return this.cnpResponse.getMessage();
    }

    public String getId() {
        return this.cnpResponse.getId();
    }

    public ResponseFileParser getResponseFileParser() {
        return responseFileParser;
    }

    public void setResponseFileParser(ResponseFileParser responseFileParser) {
        this.responseFileParser = responseFileParser;
    }

    @Override
    public void close() throws IOException {
        this.responseFileParser.close();
    }

    /**
     * Closes the resources held by this CnpFileResponse. Use close() instead.
     * @throws IOException in case an exception is raised while closing resources
     */
    @Deprecated
    public void closeResources() throws IOException {
        close();
    }
}
