package edu.lehigh.libraries.purchase_request.workflow_proxy_server.match.folio;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.workflow_proxy_server.config.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.connection.FolioConnection;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.match.Match;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.match.MatchQuery;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.match.MatchService;
import lombok.extern.slf4j.Slf4j;

@Service
@ConditionalOnProperty(name="workflow.match.data-source", havingValue="FOLIO")
@ConditionalOnWebApplication
@Slf4j
public class FolioLocalMatchService implements MatchService {

    private static final String INSTANCES_PATH = "/search/instances";
    private static final String OCLC_NUMBER_PREFIX = "(OCoLC)";

    private final FolioConnection connection;
    private final String ISBN_IDENTIFIER_TYPE;
    private final String OCLC_NUMBER_IDENTIFIER_TYPE;

    FolioLocalMatchService(Config config) throws Exception {
        this.connection = new FolioConnection(config);

        ISBN_IDENTIFIER_TYPE = config.getFolio().getIsbnIdentifierType();
        OCLC_NUMBER_IDENTIFIER_TYPE = config.getFolio().getOclcNumberIdentifierType();

        log.debug("FolioLocalMatchService ready");
    }

    @Override
    public List<Match> search(MatchQuery query) throws IllegalArgumentException {
        validate(query);

        List<String> queryParameters = new LinkedList<String>();
        if (query.getTitle() != null) {
            queryParameters.add("title = \"" + connection.sanitize(query.getTitle()) + "\"");
        }
        if (query.getContributor() != null) {
            queryParameters.add("contributors all \"" + connection.sanitize(query.getContributor()) + "\"");
        }
        if (query.getIsbn() != null) {
            queryParameters.add("isbn = \"" + connection.sanitize(query.getIsbn()) + "\"");
        }
        String queryString = String.join(" AND ", queryParameters);
        Map<String, String> extraParams = Map.of("expandAll", Boolean.TRUE.toString());
        JSONObject responseObject;
        try {
            responseObject = connection.executeGet(INSTANCES_PATH, queryString, extraParams);
        }
        catch (Exception e) {
            log.error("Exception querying FOLIO for matches.", e);
            return null;
        }

        List<Match> matches = parseMatches(responseObject.getJSONArray("instances"));
        log.debug("Found " + matches.size() + " matches.");
        return matches;
    }

    private void validate(MatchQuery query) {
        if (query.getTitle() != null ||
            query.getContributor() != null ||
            query.getIsbn() != null) {
                return;
        }

        throw new IllegalArgumentException("At least one query parameter must be present.");
    }

    private List<Match> parseMatches(JSONArray instances) {
        List<Match> matches = new LinkedList<Match>();
        for (int i=0; i < instances.length(); i++) {
            JSONObject instance = instances.getJSONObject(i);
            Match match = parseMatch(instance);
            matches.add(match);
        }
        return matches;
    }

    private Match parseMatch(JSONObject instance) {
        Match match = new Match();
        match.setTitle(parseTitle(instance));
        match.setContributor(parseContributor(instance));
        match.setIsbns(parseIsbns(instance));
        match.setOclcNumber(parseOclcNumber(instance));
        return match;
    }

    private String parseTitle(JSONObject instance) {
        return instance.getString("title");
    }

    private String parseContributor(JSONObject instance) {
        String bestContributor = null;
        JSONArray contributors = instance.getJSONArray("contributors");
        for (int i=0; i < contributors.length(); i++ ) {
            JSONObject contributor = contributors.getJSONObject(i);
            if (bestContributor == null ||
                (contributor.has("primary") && contributor.getBoolean("primary"))) {
                
                bestContributor = contributor.getString("name");
            }
        }
        return bestContributor;
    }

    private List<String> parseIsbns(JSONObject instance) {
        List<String> isbns = new LinkedList<String>();
        JSONArray identifiers = instance.getJSONArray("identifiers");
        for (int i=0; i < identifiers.length(); i++ ) {
            JSONObject identifier = identifiers.getJSONObject(i);
            if (ISBN_IDENTIFIER_TYPE.equals(identifier.getString("identifierTypeId"))) {
                isbns.add(identifier.getString("value"));
            }
        }
        return isbns;
    }
    
    private String parseOclcNumber(JSONObject instance) {
        JSONArray identifiers = instance.getJSONArray("identifiers");
        for (int i=0; i < identifiers.length(); i++ ) {
            JSONObject identifier = identifiers.getJSONObject(i);
            if (OCLC_NUMBER_IDENTIFIER_TYPE.equals(identifier.getString("identifierTypeId"))) {
                String rawValue = identifier.getString("value");
                if (rawValue.startsWith(OCLC_NUMBER_PREFIX)) {
                    return rawValue.substring(OCLC_NUMBER_PREFIX.length());
                }
                return rawValue;
            }
        }
        return null;
    }
    
}
