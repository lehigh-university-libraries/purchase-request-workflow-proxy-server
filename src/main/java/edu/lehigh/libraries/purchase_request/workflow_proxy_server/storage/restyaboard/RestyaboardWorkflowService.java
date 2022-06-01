package edu.lehigh.libraries.purchase_request.workflow_proxy_server.storage.restyaboard;

import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.model.SearchQuery;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentType;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.storage.AbstractWorkflowService;
import lombok.extern.slf4j.Slf4j;

@Service
@ConditionalOnProperty(name="workflow.storage", havingValue="restyaboard")
@ConditionalOnWebApplication
@Slf4j
public class RestyaboardWorkflowService extends AbstractWorkflowService {

    private static final String CONTRIBUTOR_KEY = "Contributor";
    private static final String ISBN_KEY = "ISBN";
    private static final String OCLC_NUMBER_KEY = "OCLC Number";
    private static final String CALL_NUMBER_KEY = "Call Number";
    private static final String FORMAT_KEY = "Format";
    private static final String SPEED_KEY = "Speed";
    private static final String DESTINATION_KEY = "Destination";
    private static final String CLIENT_NAME_KEY = "Client";
    private static final String REPORTER_NAME_KEY = "Reporter";
    private static final String REQUESTER_USERNAME_KEY = "Requester";
    private static final String REQUESTER_ROLE_KEY = "Requester Role";
    private static final String FUND_CODE_KEY = "Fund Code";
    private static final String OBJECT_CODE_KEY = "Object Code";
    private static final String COMMENT_DELIMITER = ": ";

    private Config config;
    private RestyaboardConnection connection;

    private Long BOARD_ID;
    private Long NEW_REQUEST_LIST_ID;

    private String NEW_REQUEST_LIST_NAME;

    public RestyaboardWorkflowService(Config config) throws Exception {
        super();
        this.config = config;
        this.connection = new RestyaboardConnection(config);
        initMetadata();
        log.debug("RestyaboardWorkflowService ready.");
    }

    private void initMetadata() {
        BOARD_ID = config.getRestyaboard().getBoardId();
        NEW_REQUEST_LIST_ID = config.getRestyaboard().getNewRequestListId();

        JSONArray listsResponse = getLists();
        listsResponse.forEach(item -> {
            JSONObject list = (JSONObject)item;
            if (NEW_REQUEST_LIST_ID.longValue() == list.getLong("id")) {
                NEW_REQUEST_LIST_NAME = list.getString("name");
            }
        });
    }

    private JSONArray getLists() {
        String url = "/boards/" + BOARD_ID + "/lists.json";
        JSONObject result;
        try {
            result = connection.executeGet(url);
        }
        catch (Exception e) {
            log.error("Could not load lists: ", e);
            return null;
        }
        return result.getJSONArray("data");
    }

    @Override
    public List<PurchaseRequest> findAll() {
        log.debug("findAll()");
        throw new NotImplementedException();
        // TODO Auto-generated method stub
    }

    @Override
    public PurchaseRequest findByKey(String key) {
        log.debug("findByKey()");

        String url = "/boards/" + BOARD_ID + "/cards/" + key + ".json";
        JSONObject result;
        try {
            result = connection.executeGet(url);
        }
        catch (Exception e) {
            log.error("Could not find PR: ", e);
            return null;
        }
        PurchaseRequest purchaseRequest = toStubPurchaseRequest(result);

        url = "/boards/" + BOARD_ID 
            + "/lists/" + statusToListId(purchaseRequest.getStatus()) 
            + "/cards/" + key 
            + "/activities.json";
        try {
            result = connection.executeGet(url);
        }
        catch (Exception e) {
            log.error("Could not find activities for PR: ", e);
            return null;
        }
        addActivitiesData(purchaseRequest, result);

        return purchaseRequest;
    }

    @Override
    public PurchaseRequest save(PurchaseRequest purchaseRequest) {
        log.debug("save()");

        JSONObject card = toCard(purchaseRequest);
        String url = "/boards/" + BOARD_ID + "/lists/" + NEW_REQUEST_LIST_ID + "/cards.json";

        JSONObject result;
        try {
            result = connection.executePost(url, card);
        }
        catch (Exception e) {
            log.error("Could not save PR: ", e);
            return null;
        }

        String id = result.getString("id");
        PurchaseRequest savedRequest = findByKey(id);

        enrichMapCommentIfPresent(savedRequest, CONTRIBUTOR_KEY, purchaseRequest.getContributor());
        enrichMapCommentIfPresent(savedRequest, ISBN_KEY, purchaseRequest.getIsbn());
        enrichMapCommentIfPresent(savedRequest, OCLC_NUMBER_KEY, purchaseRequest.getOclcNumber());
        enrichMapCommentIfPresent(savedRequest, CALL_NUMBER_KEY, purchaseRequest.getCallNumber());
        enrichMapCommentIfPresent(savedRequest, FORMAT_KEY, purchaseRequest.getFormat());
        enrichMapCommentIfPresent(savedRequest, SPEED_KEY, purchaseRequest.getSpeed());
        enrichMapCommentIfPresent(savedRequest, DESTINATION_KEY, purchaseRequest.getDestination());
        enrichMapCommentIfPresent(savedRequest, CLIENT_NAME_KEY, purchaseRequest.getClientName());
        enrichMapCommentIfPresent(savedRequest, REQUESTER_USERNAME_KEY, purchaseRequest.getRequesterUsername());
        enrichMapCommentIfPresent(savedRequest, REQUESTER_ROLE_KEY, purchaseRequest.getRequesterRole());
        enrichMapCommentIfPresent(savedRequest, REPORTER_NAME_KEY, purchaseRequest.getReporterName());

        // TODO set assignee name

        savedRequest = findByKey(id);
        return savedRequest;
    }

    @Override
    public List<PurchaseRequest> search(SearchQuery query) {
        log.debug("search()");
        throw new NotImplementedException();
    }

    @Override
    public void enrich(PurchaseRequest purchaseRequest, EnrichmentType type, Object data) {
        if (EnrichmentType.LOCAL_HOLDINGS == type) {
            enrichComment(purchaseRequest, (String)data);
        }
        else if (EnrichmentType.OCLC_NUMBER == type) {
            enrichMapCommentIfPresent(purchaseRequest, OCLC_NUMBER_KEY, (String)data);
        }
        else if (EnrichmentType.CALL_NUMBER == type) {
            enrichMapCommentIfPresent(purchaseRequest, CALL_NUMBER_KEY, (String)data);
        }
        else if (EnrichmentType.PRICING == type) {
            enrichComment(purchaseRequest, (String)data);
        }
        else if (EnrichmentType.REQUESTER_ROLE == type) {
            enrichMapCommentIfPresent(purchaseRequest, REQUESTER_ROLE_KEY, (String)data);
        }
        // TODO else if (EnrichmentType.LIBRARIANS == type) {
        //     enrichAssignee(purchaseRequest, data);
        // }
        else if (EnrichmentType.FUND_CODE == type) {
            enrichMapCommentIfPresent(purchaseRequest, FUND_CODE_KEY, (String)data);
        }
        else if (EnrichmentType.OBJECT_CODE == type) {
            enrichMapCommentIfPresent(purchaseRequest, OBJECT_CODE_KEY, (String)data);
        }
        else {
            throw new IllegalArgumentException("Unknown enrichment type " + type);
        }
    }

    private void enrichMapCommentIfPresent(PurchaseRequest purchaseRequest, String key, String data) {
        if (data != null) {
            enrichComment(purchaseRequest, key + COMMENT_DELIMITER + data);
        }
    }

    private void enrichComment(PurchaseRequest purchaseRequest, String comment) {
        long listId = statusToListId(purchaseRequest.getStatus());
        String url = "/boards/" + BOARD_ID 
            + "/lists/" + listId 
            + "/cards/" + purchaseRequest.getKey()
            + "/comments.json";
        JSONObject data = new JSONObject();
        data.put("comment", comment);
        
        try {
            connection.executePost(url, data);
        }
        catch (Exception e) {
            log.error("Could not save comment: ", e);
            return;
        }
    }
    
    private JSONObject toCard(PurchaseRequest purchaseRequest) {
        JSONObject card = new JSONObject();
        card.put("board_id", config.getRestyaboard().getBoardId());
        if (purchaseRequest.getStatus() == null) {
            card.put("list_id", config.getRestyaboard().getNewRequestListId());
        }
        card.put("name", purchaseRequest.getTitle());
        if (purchaseRequest.getRequesterComments() != null) {
            card.put("description", "Patron Comment: " + purchaseRequest.getRequesterComments());
        }
        return card;
    }

    private PurchaseRequest toStubPurchaseRequest(JSONObject card) {
        PurchaseRequest purchaseRequest = new PurchaseRequest();
        purchaseRequest.setKey(Long.toString(card.getLong("id")));
        purchaseRequest.setTitle(card.getString("title"));
        purchaseRequest.setCreationDate(card.getString("created"));
        purchaseRequest.setStatus(listIdToStatus(card.getLong("list_id")));
        return purchaseRequest;
    }

    private void addActivitiesData(PurchaseRequest purchaseRequest, JSONObject activitiesResult) {
        JSONArray data = activitiesResult.getJSONArray("data");
        data.forEach(eventObject -> {
            JSONObject event = (JSONObject)eventObject;
            String type = event.getString("type");
            if ("add_comment".equals(type)) {
                String comment = event.getString("comment");
                String[] pair = comment.split(COMMENT_DELIMITER, 2);
                if (pair.length == 2) {
                    String key = pair[0];
                    String value = pair[1];
                    if (CONTRIBUTOR_KEY.equals(key)) {
                        purchaseRequest.setContributor(value);
                    }
                    else if (ISBN_KEY.equals(key)) {
                        purchaseRequest.setIsbn(value);
                    }
                    else if (OCLC_NUMBER_KEY.equals(key)) {
                        purchaseRequest.setOclcNumber(value);
                    }
                    else if (CALL_NUMBER_KEY.equals(key)) {
                        purchaseRequest.setCallNumber(value);
                    }
                    else if (FORMAT_KEY.equals(key)) {
                        purchaseRequest.setFormat(value);
                    }
                    else if (SPEED_KEY.equals(key)) {
                        purchaseRequest.setSpeed(value);
                    }
                    else if (DESTINATION_KEY.equals(key)) {
                        purchaseRequest.setDestination(value);
                    }
                    else if (CLIENT_NAME_KEY.equals(key)) {
                        purchaseRequest.setClientName(value);
                    }
                    else if (REQUESTER_USERNAME_KEY.equals(key)) {
                        purchaseRequest.setRequesterUsername(value);
                    }
                    else if (REQUESTER_ROLE_KEY.equals(key)) {
                        purchaseRequest.setRequesterRole(value);
                    }
                    else if (FUND_CODE_KEY.equals(key)) {
                        purchaseRequest.setFundCode(value);
                    }
                    else if (OBJECT_CODE_KEY.equals(key)) {
                        purchaseRequest.setObjectCode(value);
                    }
                }
            }
        });
    }

    private String listIdToStatus(long listId) {
        if (NEW_REQUEST_LIST_ID.longValue() == listId) {
            return NEW_REQUEST_LIST_NAME;
        }
        else {
            throw new IllegalArgumentException("Unknown list ID: " + listId);
        }
    }

    private long statusToListId(String status) {
        if (NEW_REQUEST_LIST_NAME.equals(status)) {
            return NEW_REQUEST_LIST_ID.longValue();
        }
        else {
            throw new IllegalArgumentException("Unknown status: " + status);
        }
    }

}
