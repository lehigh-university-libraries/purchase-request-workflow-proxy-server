package edu.lehigh.libraries.purchase_request.workflow_proxy_server.listeners.google;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;

import edu.lehigh.libraries.purchase_request.workflow_proxy_server.listeners.WorkflowServiceListener;

abstract public class GoogleListener implements WorkflowServiceListener {
    
    protected static JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    protected static String APPLICATION_NAME = "Purchase Request Workflow Proxy Server";

    protected String CREDENTIALS_FILE_PATH;
    protected List<String> ALL_RESOURCES_TO_TEST;

    abstract protected void confirmWritePermission(Credential credential) throws IOException;

    abstract protected void buildService(NetHttpTransport HTTP_TRANSPORT, JsonFactory JSON_FACTORY, Credential credential);

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
        Credential credential = GoogleAuthorizeUtil.getCredential(CREDENTIALS_FILE_PATH, HTTP_TRANSPORT, getScopes());
        buildService(HTTP_TRANSPORT, JSON_FACTORY, credential);
        
        confirmWritePermission(credential);
    }

    abstract public static class GoogleAuthorizeUtil {
    
        /**
         * Return the credential to access Google APIs.
         * 
         * Ignores deprecation per https://stackoverflow.com/questions/64135720/how-do-you-access-a-google-sheet-with-a-service-account-from-java 
         */
        @SuppressWarnings("deprecation")
        public static Credential getCredential(final String CREDENTIALS_FILE_PATH, 
            final NetHttpTransport HTTP_TRANSPORT, final Collection<String> SCOPES)
            throws IOException, GeneralSecurityException {
            InputStream in = new FileInputStream(CREDENTIALS_FILE_PATH);
            com.google.api.client.googleapis.auth.oauth2.GoogleCredential credential = 
                com.google.api.client.googleapis.auth.oauth2.GoogleCredential.fromStream(in).createScoped(SCOPES);
            in.close();
            return credential;
        }

    }

}
