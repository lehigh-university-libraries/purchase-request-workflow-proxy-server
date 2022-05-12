package edu.lehigh.libraries.purchase_request.workflow_proxy_server.listeners.google_sheets;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.WorkflowService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MatchMarcGoogleSheetsListener extends GoogleSheetsListener {
    
    MatchMarcGoogleSheetsListener(WorkflowService workflowService, Config config) throws IOException, GeneralSecurityException {
        super(workflowService, config);

        log.debug("MatchMarcListener listening.");
    }

    @Override
    void initMetadata() {
        super.initMetadata();
        REQUESTED_SPREADSHEET_ID = config.getGoogleSheets().getMatchMarc().getRequestedSpreadsheetId();
        APPROVED_SPREADSHEET_ID = config.getGoogleSheets().getMatchMarc().getApprovedSpreadsheetId();
        ALL_SHEETS_TO_TEST = sheetsToTest(new String[] {REQUESTED_SPREADSHEET_ID, APPROVED_SPREADSHEET_ID});
    }

    @Override
    List<Object> getHeaders() {
        return Arrays.asList(new Object[] {
            "ISBN", "OCLC", "Fund", "Object", "Destination",
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
            formatCell(purchaseRequest.getOclcNumber()),
            formatCell(purchaseRequest.getFundCode()),
            formatCell(purchaseRequest.getObjectCode()),
            formatCell(purchaseRequest.getDestination()),
        });
    }

}
