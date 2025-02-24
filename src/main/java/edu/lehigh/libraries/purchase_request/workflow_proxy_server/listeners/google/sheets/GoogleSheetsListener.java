package edu.lehigh.libraries.purchase_request.workflow_proxy_server.listeners.google.sheets;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.config.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.listeners.google.GoogleListener;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.storage.WorkflowService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
abstract class GoogleSheetsListener extends GoogleListener {

    static String VALUE_INPUT_OPTION_RAW = "RAW";

    String REQUESTED_SPREADSHEET_ID;
    String APPROVED_SPREADSHEET_ID;

    Config config;
    Sheets sheetsService;

    GoogleSheetsListener(WorkflowService workflowService, Config config) throws IOException, GeneralSecurityException {
        this.config = config;
        initMetadata();
        initConnection();

        workflowService.addListener(this);
    }

    void initMetadata() {
        CREDENTIALS_FILE_PATH = config.getGoogleSheets().getCredentialsFilePath();
    }

    abstract List<Object> getHeaders();

    protected void buildService(NetHttpTransport HTTP_TRANSPORT, JsonFactory JSON_FACTORY, Credential credential) {
        sheetsService = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
            .setApplicationName(APPLICATION_NAME)
            .build();
    } 

    @Override
    protected Collection<String> getScopes() {
        return Collections.singletonList(SheetsScopes.SPREADSHEETS);
    }

    protected void confirmWritePermission(Credential credential) throws IOException {
        // Set the column header as both a convenience and a start-time test of write permissions.
        ValueRange body = valueRange(getHeaders());
        for (String spreadsheetId: ALL_RESOURCES_TO_TEST) {
            sheetsService.spreadsheets().values()
                .update(spreadsheetId, "A1:Z1", body)
                .setValueInputOption(VALUE_INPUT_OPTION_RAW)
                .execute();
        }
    }

    @Override
    public void purchaseRequested(PurchaseRequest purchaseRequest) {
        if (REQUESTED_SPREADSHEET_ID != null) {
            log.debug("Writing purchase request.");
            writePurchase(purchaseRequest, REQUESTED_SPREADSHEET_ID);
        }
    }

    @Override
    public void purchaseApproved(PurchaseRequest purchaseRequest) {
        if (APPROVED_SPREADSHEET_ID != null) {
            log.debug("Writing approved purchase.");
            writePurchase(purchaseRequest, APPROVED_SPREADSHEET_ID);
        }
    }

    @Override
    public void purchaseDenied(PurchaseRequest purchaseRequest) {
        // nothing to do for denied purchases        
    }

    @Override
    public void purchaseArrived(PurchaseRequest purchaseRequest) {
        // nothing to do for arrived purchases        
    }

    abstract void writePurchase(PurchaseRequest purchaseRequest, String spreadsheetId);

    static ValueRange valueRange(List<Object> row) {
        List<List<Object>> values = Arrays.asList(row);
        return new ValueRange().setValues(values);
    }

    static ValueRange singleValueRange(String value) {
        List<List<Object>> values = Arrays.asList(
            Arrays.asList(new Object[] { value } )
        );
        return new ValueRange().setValues(values);
    }


    static String formatCell(String value) {
        // Google skips columns for null, requires an empty string.
        return value == null ? "" : value;
    }

    protected void writeRow(List<Object> recordRow, String spreadsheetId) {
        ValueRange body = valueRange(recordRow);
        try {
            sheetsService.spreadsheets().values()
                .append(spreadsheetId, "A1:A1", body)
                .setValueInputOption(VALUE_INPUT_OPTION_RAW)
                .execute();
            log.debug("Wrote purchase to Google Sheet: " + recordRow);
        }
        catch (IOException ex) {
            log.error("Caught IOException: ", ex);
            return;
        }
    }

}
