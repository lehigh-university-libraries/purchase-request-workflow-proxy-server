package edu.lehigh.libraries.purchase_request.workflow_proxy_server.listeners.google.docs;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.DocsScopes;

import edu.lehigh.libraries.purchase_request.workflow_proxy_server.config.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.listeners.google.GoogleListener;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.storage.WorkflowService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
abstract class GoogleDocsListener extends GoogleListener {

    List<String> ALL_SHEETS_TO_TEST;

    Config config;
    Docs docsService;

    GoogleDocsListener(WorkflowService workflowService, Config config) throws IOException, GeneralSecurityException {
        this.config = config;
        initMetadata();
        initConnection();

        workflowService.addListener(this);
    }

    void initMetadata() {
        CREDENTIALS_FILE_PATH = config.getGoogleDocs().getCredentialsFilePath();
    }

    protected void buildService(NetHttpTransport HTTP_TRANSPORT, JsonFactory JSON_FACTORY, Credential credential) {
        docsService = new Docs.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
            .setApplicationName(APPLICATION_NAME)
            .build();
    }

    @Override
    protected Collection<String> getScopes() {
        return Collections.singletonList(DocsScopes.DOCUMENTS);
    }

    protected void confirmWritePermission(Credential credential) throws IOException {
        // No clear way to implement this for Google Docs.
    }

}