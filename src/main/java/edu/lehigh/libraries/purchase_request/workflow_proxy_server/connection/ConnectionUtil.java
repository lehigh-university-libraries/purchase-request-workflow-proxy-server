package edu.lehigh.libraries.purchase_request.workflow_proxy_server.connection;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ConnectionUtil {

    public static String encodeUrl(String raw) {
        return URLEncoder.encode(raw, StandardCharsets.UTF_8);
    }

}
