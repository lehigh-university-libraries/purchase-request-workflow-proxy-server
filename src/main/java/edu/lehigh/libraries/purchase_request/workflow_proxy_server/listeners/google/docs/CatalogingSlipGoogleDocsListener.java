package edu.lehigh.libraries.purchase_request.workflow_proxy_server.listeners.google.docs;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.docs.v1.model.Dimension;
import com.google.api.services.docs.v1.model.Document;
import com.google.api.services.docs.v1.model.EndOfSegmentLocation;
import com.google.api.services.docs.v1.model.InsertPageBreakRequest;
import com.google.api.services.docs.v1.model.InsertTableRequest;
import com.google.api.services.docs.v1.model.InsertTextRequest;
import com.google.api.services.docs.v1.model.Location;
import com.google.api.services.docs.v1.model.MergeTableCellsRequest;
import com.google.api.services.docs.v1.model.ParagraphElement;
import com.google.api.services.docs.v1.model.Range;
import com.google.api.services.docs.v1.model.ReplaceAllTextRequest;
import com.google.api.services.docs.v1.model.Request;
import com.google.api.services.docs.v1.model.StructuralElement;
import com.google.api.services.docs.v1.model.SubstringMatchCriteria;
import com.google.api.services.docs.v1.model.Table;
import com.google.api.services.docs.v1.model.TableCell;
import com.google.api.services.docs.v1.model.TableCellLocation;
import com.google.api.services.docs.v1.model.TableColumnProperties;
import com.google.api.services.docs.v1.model.TableRange;
import com.google.api.services.docs.v1.model.TableRow;
import com.google.api.services.docs.v1.model.TextStyle;
import com.google.api.services.docs.v1.model.UpdateTableColumnPropertiesRequest;
import com.google.api.services.docs.v1.model.UpdateTextStyleRequest;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.config.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.storage.WorkflowService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Conditional(CatalogingSlipGoogleDocsListener.AnyStatus.class)
@ConditionalOnWebApplication
public class CatalogingSlipGoogleDocsListener extends GoogleDocsListener {

    String TEMPLATE_DOC_ID;
    String OUTPUT_DOC_ID;
    
    CatalogingSlipGoogleDocsListener(WorkflowService workflowService, Config config) throws IOException, GeneralSecurityException {
        super(workflowService, config);

        log.debug("CatalogingSlipGoogleDocsListener listening.");
    }

    @Override
    void initMetadata() {
        super.initMetadata();
        TEMPLATE_DOC_ID = config.getGoogleDocs().getCatalogingSlip().getTemplateDocId();
        OUTPUT_DOC_ID = config.getGoogleDocs().getCatalogingSlip().getOutputDocId();
        ALL_RESOURCES_TO_TEST = resourcesToTest(new String[] {TEMPLATE_DOC_ID, OUTPUT_DOC_ID});
    }

    @Override
    public void purchaseRequested(PurchaseRequest purchaseRequest) {
        // nothing to do for requested purchases
    }

    @Override
    public void purchaseApproved(PurchaseRequest purchaseRequest) {
        // TODO: Ultimately this should happen on the new Received status
 
        if (OUTPUT_DOC_ID != null) {
            log.debug("Writing approved purchase.");
            writePageBreak();
            writePurchase(purchaseRequest);
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

    void writePurchase(PurchaseRequest purchaseRequest) {
        // Load template
        Document templateDoc = loadTemplateDoc();
        List<StructuralElement> templateContent = templateDoc.getBody().getContent();
        
        // Build structure of new content
        List<Request> docsRequests = new ArrayList<Request>();
        int startIndex = getOutputDocStartIndex();
        copyStructuralElements(docsRequests, templateContent, startIndex);

        // Merge content of purchase request
        mergeTemplateValues(docsRequests, purchaseRequest);

        // Write output to Google Doc
        executeBatchUpdate(docsRequests);
    }

    private Document loadTemplateDoc() {
        try {
            return docsService.documents().get(TEMPLATE_DOC_ID).execute();
        }
        catch (IOException ex) {
            throw new RuntimeException("Could not load template doc.", ex);
        }
    }

    private void writePageBreak() {
        List<Request> docsRequests = new ArrayList<Request>();
        int startIndex = getOutputDocStartIndex();
        docsRequests.add(new Request()
            .setInsertPageBreak(new InsertPageBreakRequest()
                .setLocation(new Location().setIndex(startIndex))
            )
        );
        executeBatchUpdate(docsRequests);
    }

    private int getOutputDocStartIndex() {
        Document outputDoc;
        try {
            outputDoc = docsService.documents().get(OUTPUT_DOC_ID).execute();
        }
        catch (IOException e) {
            throw new RuntimeException("Could not get output doc start index.", e);
        }
        List<StructuralElement> structuralElements = outputDoc.getBody().getContent();
        return structuralElements.get(structuralElements.size() - 1).getEndIndex() - 1;
    }

    private int copyStructuralElements(List<Request> docsRequests, List<StructuralElement> structuralElements,
        int parentIndex) {
            
        int index = parentIndex;
        for (StructuralElement structuralElement : structuralElements) {
            if (structuralElement.getParagraph() != null) {
                List<ParagraphElement> paragraphElements = structuralElement.getParagraph().getElements();
                index = copyParagraphElements(docsRequests, paragraphElements, index);
            }

            if (structuralElement.getTable() != null) {
                Table table = structuralElement.getTable();
                // Cannot accurately measure index change from adding table
                copyTable(docsRequests, index, table);
            }
        }
        return index;
    }

    private void copyTable(List<Request> docsRequests, int parentIndex, Table table) {
        int tableStartIndex = parentIndex + 1;
        Location tableStartLocation = new Location().setIndex(tableStartIndex);
        docsRequests.add(new Request().setInsertTable(new InsertTableRequest()
            .setEndOfSegmentLocation(new EndOfSegmentLocation())
            .setRows(table.getTableRows().size())
            .setColumns(table.getTableRows().get(0).getTableCells().size())));

        // Recommended practice is to iterate backwards, so that content length doesn't affect the indexes
        ListIterator<TableRow> rowIterator = table.getTableRows().listIterator(table.getRows());
        while (rowIterator.hasPrevious()) {
            int rowIndex = rowIterator.previousIndex();
            TableRow row = rowIterator.previous();
            ListIterator<TableCell> cellIterator = row.getTableCells().listIterator(row.getTableCells().size());
            while (cellIterator.hasPrevious()) {
                int cellIndex = cellIterator.previousIndex();
                TableCell cell = cellIterator.previous();
                copyTableCell(docsRequests, table, cell, tableStartLocation, tableStartIndex, rowIndex, cellIndex);
            }
        }
    }

    private void copyTableCell(List<Request> docsRequests, Table table, TableCell cell, Location tableStartLocation, 
        int tableStartIndex, int rowIndex, int cellIndex) {

        // Copy the content and styles
        List<StructuralElement> cellElements = cell.getContent();
        int cellLocationIndex = tableStartIndex + 3 + (rowIndex * 5) + (cellIndex * 2);
        copyStructuralElements(docsRequests, cellElements, cellLocationIndex);
        copyTableColumnSpan(docsRequests, cell, tableStartLocation, rowIndex, cellIndex);

        // On row zero, set the column widths
        if (rowIndex == 0) {
            copyTableColumnWidths(docsRequests, table, tableStartLocation, cellIndex);
        }
    }

    private void copyTableColumnSpan(List<Request> docsRequests, TableCell cell, Location tableStartLocation, int rowIndex, int cellIndex) {
        Integer columnSpan = cell.getTableCellStyle().getColumnSpan();
        if (columnSpan != null && columnSpan.intValue() != 1) {
            docsRequests.add(new Request().setMergeTableCells(new MergeTableCellsRequest()
                .setTableRange(new TableRange()
                    .setTableCellLocation(new TableCellLocation()
                        .setTableStartLocation(tableStartLocation)
                        .setRowIndex(rowIndex)
                        .setColumnIndex(cellIndex)
                    )
                    .setColumnSpan(columnSpan)
                    .setRowSpan(1)
                )
            ));
        }
    }

    private void copyTableColumnWidths(List<Request> docsRequests, Table table, Location tableStartLocation, int cellIndex) {
        TableColumnProperties columnProperties = table.getTableStyle().getTableColumnProperties().get(cellIndex);
        docsRequests.add(new Request().setUpdateTableColumnProperties(new UpdateTableColumnPropertiesRequest()
            .setTableStartLocation(tableStartLocation)
            .setColumnIndices(Arrays.asList(new Integer[] {cellIndex}))
            .setTableColumnProperties(new TableColumnProperties()
                .setWidthType("FIXED_WIDTH")
                .setWidth(new Dimension().setMagnitude(columnProperties.getWidth().getMagnitude()).setUnit("PT"))
            )
            .setFields("width,widthType")
        ));
    }

    private int copyParagraphElements(List<Request> docsRequests, List<ParagraphElement> paragraphElements,
        int parentIndex ) {
        
        int index = parentIndex;
        for (ParagraphElement paragraphElement : paragraphElements) {
            if (paragraphElement.getTextRun() != null) {
                String content = paragraphElement.getTextRun().getContent();
                docsRequests.add(new Request()
                    .setInsertText(new InsertTextRequest()
                        // Have to leave single newlines alone, but can trim the rest
                        .setText(content.length() > 2 ? content.trim() : content)
                        .setLocation(new Location().setIndex(index))
                    )
                );
                if (paragraphElement.getTextRun().getTextStyle() != null) {
                    copyTextStyle(docsRequests, paragraphElement.getTextRun().getTextStyle(), index, content);
                }
                index += paragraphElement.getEndIndex() - paragraphElement.getStartIndex();
            }
            if (paragraphElement.getPageBreak() != null) {
                throw new RuntimeException(
                        "Cannot handle page break in template, but one is entered automatically before its content.");
            }
        }
        return index;
    }

    private void copyTextStyle(List<Request> docsRequests, TextStyle textStyle, int index, String content) {
        if (textStyle.getBold() != null && textStyle.getBold()) {
            docsRequests.add(new Request()
                .setUpdateTextStyle(new UpdateTextStyleRequest()
                    .setTextStyle(new TextStyle().setBold(true))
                    .setRange(new Range().setStartIndex(index).setEndIndex(index + content.length()))
                    .setFields("bold")
                )
            );
        }
    }

    private void mergeTemplateValues(List<Request> docsRequests, PurchaseRequest purchaseRequest) {
        mergeTemplateValue(docsRequests, "title", purchaseRequest.getTitle());
        mergeTemplateValue(docsRequests, "contributor", purchaseRequest.getContributor());
        mergeTemplateValue(docsRequests, "key", purchaseRequest.getKey());
        mergeTemplateValue(docsRequests, "requester_username", purchaseRequest.getRequesterUsername());
        mergeTemplateValue(docsRequests, "destination", purchaseRequest.getDestination());
        mergeTemplateValue(docsRequests, "description", purchaseRequest.getRequesterComments());
    }

    private void mergeTemplateValue(List<Request> docsRequests, String tokenText, String replaceText) {
        docsRequests.add(new Request()
            .setReplaceAllText(new ReplaceAllTextRequest()
                .setContainsText(new SubstringMatchCriteria()
                    .setText(tokenize(tokenText))
                )
                .setReplaceText(replaceText)
            )
        );
    }

    private String tokenize(String tokenText) {
        return "{{" + tokenText + "}}";
    }

    private void executeBatchUpdate(List<Request> docsRequests) {
        BatchUpdateDocumentRequest batchUpdateRequest = new BatchUpdateDocumentRequest().setRequests(docsRequests);
        try {
            docsService.documents().batchUpdate(OUTPUT_DOC_ID, batchUpdateRequest).execute();
        }
        catch (IOException ex) {
            throw new RuntimeException("Could not perform batch update.", ex);
        }
    }

    static class AnyStatus extends AnyNestedCondition {
        
        AnyStatus() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }
    
        @ConditionalOnProperty("workflow.google-docs.cataloging-slip.template-doc-id")
        static class Template {}
    
        @ConditionalOnProperty("workflow.google-docs.cataloging-slip.output-doc-id")
        static class Output {}
    
    }

}
