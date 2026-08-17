package edu.lehigh.libraries.purchase_request.workflow_proxy_server.listeners.google.docs;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    public void purchaseReceived(PurchaseRequest purchaseRequest) {
        if (OUTPUT_DOC_ID != null) {
            log.debug("Writing received purchase.");
            writePageBreak();
            writePurchase(purchaseRequest);
        }
    }

    void writePurchase(PurchaseRequest purchaseRequest) {
        Document templateDoc = loadTemplateDoc();
        List<Request> pendingRequests = new ArrayList<>();
        int index = getOutputDocEndIndex();

        for (StructuralElement element : templateDoc.getBody().getContent()) {
            if (element.getParagraph() != null) {
                index = copyParagraphElements(pendingRequests, element.getParagraph().getElements(), index);
            }
            else if (element.getTable() != null) {
                if (!pendingRequests.isEmpty()) {
                    executeBatchUpdate(pendingRequests);
                    pendingRequests.clear();
                }
                copyTable(element.getTable());
                index = getOutputDocEndIndex();
            }
        }

        mergeTemplateValues(pendingRequests, purchaseRequest);
        executeBatchUpdate(pendingRequests);
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
        int startIndex = getOutputDocEndIndex();
        executeBatchUpdate(Collections.singletonList(new Request()
            .setInsertPageBreak(new InsertPageBreakRequest()
                .setLocation(new Location().setIndex(startIndex))
            )
        ));
    }

    private int getOutputDocEndIndex() {
        Document outputDoc;
        try {
            outputDoc = docsService.documents().get(OUTPUT_DOC_ID).execute();
        }
        catch (IOException e) {
            throw new RuntimeException("Could not get output doc end index.", e);
        }
        List<StructuralElement> elements = outputDoc.getBody().getContent();
        return elements.get(elements.size() - 1).getEndIndex() - 1;
    }

    private void copyTable(Table templateTable) {
        int rows = templateTable.getTableRows().size();
        int cols = templateTable.getTableRows().get(0).getTableCells().size();

        // Phase A: Insert empty table structure
        executeBatchUpdate(Collections.singletonList(new Request().setInsertTable(
            new InsertTableRequest()
                .setEndOfSegmentLocation(new EndOfSegmentLocation())
                .setRows(rows)
                .setColumns(cols)
        )));

        // Phase B: Fetch output doc to get actual cell start indexes
        Document outputDoc;
        try {
            outputDoc = docsService.documents().get(OUTPUT_DOC_ID).execute();
        }
        catch (IOException e) {
            throw new RuntimeException("Could not read output doc after table insert.", e);
        }
        StructuralElement tableElement = findLastTableElement(outputDoc);
        Table outputTable = tableElement.getTable();
        Location tableStartLocation = new Location().setIndex(tableElement.getStartIndex());

        // Phase C: Fill cells in reverse position order (highest index first) so each
        // insertion doesn't shift the pre-read positions of cells at lower indexes.
        List<Request> cellRequests = new ArrayList<>();
        ListIterator<TableRow> rowIterator = templateTable.getTableRows().listIterator(rows);
        while (rowIterator.hasPrevious()) {
            int rowIndex = rowIterator.previousIndex();
            TableRow templateRow = rowIterator.previous();
            TableRow outputRow = outputTable.getTableRows().get(rowIndex);

            ListIterator<TableCell> cellIterator = templateRow.getTableCells().listIterator(templateRow.getTableCells().size());
            while (cellIterator.hasPrevious()) {
                int cellIndex = cellIterator.previousIndex();
                TableCell templateCell = cellIterator.previous();
                TableCell outputCell = outputRow.getTableCells().get(cellIndex);

                int cellStartIndex = outputCell.getContent().get(0).getStartIndex();
                copyCellContent(cellRequests, templateCell.getContent(), cellStartIndex);
                copyTableColumnSpan(cellRequests, templateCell, tableStartLocation, rowIndex, cellIndex);
                if (rowIndex == 0) {
                    copyTableColumnWidths(cellRequests, templateTable, tableStartLocation, cellIndex);
                }
            }
        }
        if (!cellRequests.isEmpty()) {
            executeBatchUpdate(cellRequests);
        }
    }

    private StructuralElement findLastTableElement(Document doc) {
        List<StructuralElement> elements = doc.getBody().getContent();
        for (int i = elements.size() - 1; i >= 0; i--) {
            if (elements.get(i).getTable() != null) {
                return elements.get(i);
            }
        }
        throw new RuntimeException("No table found in output doc after table insert.");
    }

    private void copyCellContent(List<Request> docsRequests, List<StructuralElement> cellElements, int startIndex) {
        int index = startIndex;
        for (StructuralElement element : cellElements) {
            if (element.getParagraph() != null) {
                index = copyParagraphElements(docsRequests, element.getParagraph().getElements(), index);
            }
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

    private int copyParagraphElements(List<Request> docsRequests, List<ParagraphElement> paragraphElements, int parentIndex) {
        int index = parentIndex;
        for (ParagraphElement paragraphElement : paragraphElements) {
            if (paragraphElement.getTextRun() != null) {
                String raw = paragraphElement.getTextRun().getContent();
                String content = raw.endsWith("\n") ? raw.substring(0, raw.length() - 1) : raw;
                if (!content.isEmpty()) {
                    docsRequests.add(new Request()
                        .setInsertText(new InsertTextRequest()
                            .setText(content)
                            .setLocation(new Location().setIndex(index))
                        )
                    );
                    if (paragraphElement.getTextRun().getTextStyle() != null) {
                        copyTextStyle(docsRequests, paragraphElement.getTextRun().getTextStyle(), index, content);
                    }
                    index += content.length();
                }
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
        mergeTemplateValue(docsRequests, "permanent_location", purchaseRequest.getPermanentLocation());
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
