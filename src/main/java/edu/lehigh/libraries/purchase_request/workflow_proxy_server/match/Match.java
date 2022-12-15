package edu.lehigh.libraries.purchase_request.workflow_proxy_server.match;

import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @EqualsAndHashCode @ToString
public class Match {

    private String oclcNumber;

    private String title;

    private String contributor;

    private List<String> isbns;

}
