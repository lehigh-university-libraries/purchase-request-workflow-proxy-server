package edu.lehigh.libraries.purchase_request.workflow_proxy_server.match;

import java.util.LinkedList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.workflow_proxy_server.config.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.connection.ConnectionUtil;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.connection.OclcConnection;
import lombok.extern.slf4j.Slf4j;

@Service
@ConditionalOnWebApplication
@Slf4j
public class OclcLocalMatchService implements MatchService {

    private static final String SCOPE = "wcapi";

    private final OclcConnection oclcConnection;

    private final String LOCAL_OCLC_SYMBOL;

    OclcLocalMatchService(Config config) throws Exception {
        this.oclcConnection = new OclcConnection(config, SCOPE);

        LOCAL_OCLC_SYMBOL = config.getOclc().getLocalInstitutionSymbol();
    }

    @Override
    public List<Match> search(MatchQuery query) {
        List<String> queryParameters = new LinkedList<String>();
        if (query.getTitle() != null) {
            queryParameters.add("ti:" + ConnectionUtil.encodeUrl(query.getTitle()));
        }
        if (query.getContributor() != null) {
            queryParameters.add("au:" + ConnectionUtil.encodeUrl(query.getContributor()));
        }
        if (query.getIsbn() != null) {
            queryParameters.add("bn:" + query.getIsbn());
        }
        String queryString = String.join(ConnectionUtil.encodeUrl(" AND "), queryParameters);

        String url = OclcConnection.WORLDCAT_BASE_URL 
            + "/bibs"
            + "?heldBySymbol=" + LOCAL_OCLC_SYMBOL
            + "&q=(" + queryString + ")";

        JsonObject responseObject;
        try {
            log.debug("Searching for matches: " + url);
            responseObject = oclcConnection.execute(url);
        }
        catch (Exception e) {
            log.error("Caught exception getting local holdings from OCLC: ", e);
            return null;
        }

        long totalRecords = responseObject.get("numberOfRecords").getAsLong();
        if (totalRecords == 0) {
            log.debug("No WorldCat records found, cannot enrich.");
            return new LinkedList<Match>();
        }

        JsonArray bibRecords = responseObject.getAsJsonArray("bibRecords");
        List<Match> matches = new LinkedList<Match>();
        for (JsonElement bibRecordElement : bibRecords) {
            JsonObject bibRecord = (JsonObject)bibRecordElement;
            Match match = parseBibRecord(bibRecord);
            matches.add(match);
        }

        log.debug("Found " + totalRecords + " matches.");
        return matches;
    }

    private Match parseBibRecord(JsonObject bibRecord) {
        Match match = new Match();
        addIdentifiers(bibRecord, match);
        addTitle(bibRecord, match);
        addContributor(bibRecord, match);
        return match;
    }

    private void addIdentifiers(JsonObject bibRecord, Match match) {
        JsonObject identifier = bibRecord.getAsJsonObject("identifier");

        String oclcNumber = identifier.get("oclcNumber").getAsString();
        match.setOclcNumber(oclcNumber);

        List<String> isbns = new LinkedList<String>();
        JsonArray isbnsArray = identifier.getAsJsonArray("isbns");
        for (int i=0; i < isbnsArray.size(); i++) {
            isbns.add(isbnsArray.get(i).getAsString());
        }
        match.setIsbns(isbns);
    }
    
    private void addTitle(JsonObject bibRecord, Match match) {
        JsonObject title = bibRecord.getAsJsonObject("title");
        JsonArray mainTitles = title.getAsJsonArray("mainTitles");
        JsonObject mainTitle = mainTitles.get(0).getAsJsonObject();
        String titleText = mainTitle.get("text").getAsString();
        match.setTitle(titleText);
    }

    private void addContributor(JsonObject bibRecord, Match match) {
        JsonObject contributor = bibRecord.getAsJsonObject("contributor");
        try {
            JsonObject statementOfResponsibility = contributor.getAsJsonObject("statementOfResponsibility");
            String contributorText = statementOfResponsibility.get("text").getAsString();
            match.setContributor(contributorText);
        }
        catch (Exception e) {
            log.warn("Could not access statement of responsibility, leaving null contributor.");
        }
    }

}
