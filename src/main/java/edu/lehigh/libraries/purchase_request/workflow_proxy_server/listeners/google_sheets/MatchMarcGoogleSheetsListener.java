package edu.lehigh.libraries.purchase_request.workflow_proxy_server.listeners.google_sheets;

import java.io.IOException;
import java.security.GeneralSecurityException;

import com.google.api.services.sheets.v4.model.ValueRange;

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
    void writePurchase(PurchaseRequest purchaseRequest, String spreadsheetId) {
        try {
            String isbn = purchaseRequest.getIsbn();
            if (isbn == null) {
                log.warn("Purchase approved with empty ISBN; cannot add to Google Sheet.");
                return;
            }
            ValueRange body = singleValueRange(isbn);
            sheetsService.spreadsheets().values()
                .append(spreadsheetId, "A1:A1", body)
                .setValueInputOption(VALUE_INPUT_OPTION_RAW)
                .execute();
            log.debug("Wrote purchase to Google Sheet: " + isbn);
        }
        catch (IOException ex) {
            log.error("Caught IOException: ", ex);
            return;
        }
    }

}
