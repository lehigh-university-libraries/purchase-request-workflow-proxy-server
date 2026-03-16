package edu.lehigh.libraries.purchase_request.workflow_proxy_server.listeners.google;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import edu.lehigh.libraries.purchase_request.workflow_proxy_server.listeners.WorkflowServiceListener;

abstract public class GoogleListener implements WorkflowServiceListener {

    protected static JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    protected static String APPLICATION_NAME = "Purchase Request Workflow Proxy Server";

    protected String CREDENTIALS_FILE_PATH;
    protected List<String> ALL_RESOURCES_TO_TEST;

    abstract protected void confirmWritePermission() throws IOException;

    abstract protected void buildService(NetHttpTransport HTTP_TRANSPORT, JsonFactory JSON_FACTORY, HttpRequestInitializer httpRequestInitializer);

    abstract protected Collection<String> getScopes();

    protected List<String> resourcesToTest(String[] ids) {
        List<String> resources = new ArrayList<String>();
        for (int i=0; i < ids.length; i++) {
            if (ids[i] != null) {
                resources.add(ids[i]);
            }
        }
        return resources;
    }

    protected void initConnection() throws GeneralSecurityException, IOException {
        NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        InputStream in = new FileInputStream(CREDENTIALS_FILE_PATH);
        GoogleCredentials credentials = GoogleCredentials.fromStream(in).createScoped(getScopes());
        in.close();
        HttpRequestInitializer httpRequestInitializer = new HttpCredentialsAdapter(credentials);
        buildService(HTTP_TRANSPORT, JSON_FACTORY, httpRequestInitializer);
        confirmWritePermission();
    }

}
