package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment;

public enum EnrichmentType {
    OCLC_NUMBER,
    CALL_NUMBER,
    LOCAL_HOLDINGS,
    PRICING,
    REQUESTER_INFO,
    LIBRARIANS,
    FUND_CODE,
    OBJECT_CODE,
    LINKS,
    PRIORITY,

    /* Fields set during the initial PR save, but may be changed later via enrichment. */
    TITLE,
    CONTRIBUTOR; 
}