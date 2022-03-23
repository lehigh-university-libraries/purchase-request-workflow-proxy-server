package edu.lehigh.libraries.purchase_request.workflow_proxy_server.match;

import java.util.List;

public interface MatchService {
    
    List<Match> search(MatchQuery query);

}
