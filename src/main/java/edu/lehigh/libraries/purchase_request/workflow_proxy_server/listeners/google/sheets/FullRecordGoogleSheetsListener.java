package edu.lehigh.libraries.purchase_request.workflow_proxy_server.listeners.google.sheets;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.config.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.storage.WorkflowService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Conditional(FullRecordGoogleSheetsListener.AnyStatus.class)
@ConditionalOnWebApplication
public class FullRecordGoogleSheetsListener extends GoogleSheetsListener {
    
    FullRecordGoogleSheetsListener(WorkflowService workflowService, Config config) throws IOException, GeneralSecurityException {
        super(workflowService, config);

        log.debug("FullRecordGoogleSheetsListener listening.");
    }

    @Override
    void initMetadata() {
        super.initMetadata();
        REQUESTED_SPREADSHEET_ID = config.getGoogleSheets().getFullRecord().getRequestedSpreadsheetId();
        APPROVED_SPREADSHEET_ID = config.getGoogleSheets().getFullRecord().getApprovedSpreadsheetId();
        ALL_RESOURCES_TO_TEST = resourcesToTest(new String[] {REQUESTED_SPREADSHEET_ID, APPROVED_SPREADSHEET_ID});
    }

    @Override
    List<Object> getHeaders() {
        return Arrays.asList(new Object[] {
            "ISBN", "Title", "Contributor",
        });
    }

    @Override
    void writePurchase(PurchaseRequest purchaseRequest, String spreadsheetId) {
        List<Object> recordRow = toRow(purchaseRequest);
        writeRow(recordRow, spreadsheetId);
    }

    private List<Object> toRow(PurchaseRequest purchaseRequest) {
        return Arrays.asList(new Object[] {
            formatCell(purchaseRequest.getIsbn()),
            formatCell(purchaseRequest.getTitle()),
            formatCell(purchaseRequest.getContributor()),
        });
    }

    static class AnyStatus extends AnyNestedCondition {
        
        AnyStatus() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }
    
        @ConditionalOnProperty("workflow.google-sheets.full-record.requested-spreadsheet-id")
        static class Requested {}
    
        @ConditionalOnProperty("workflow.google-sheets.full-record.approved-spreadsheet-id")
        static class Approved {}
    
    }

}
