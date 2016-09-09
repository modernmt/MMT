package eu.modernmt.rest.framework;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import eu.modernmt.rest.framework.actions.Action;
import eu.modernmt.rest.framework.routing.RouteTemplate;
import eu.modernmt.rest.framework.routing.RouteTree;
import eu.modernmt.rest.framework.routing.TemplateException;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;

public class RESTRequest {

    public static final String HTTP_METHOD_HEADER = "X-HTTP-Method-Override";

    private static final JsonObject NULL_OBJECT = new JsonObject();
    private static final JsonArray NULL_ARRAY = new JsonArray();

    private HttpServletRequest request;
    private String path = null;
    private List<String> pathTokens = null;
    private RouteTemplate template = null;
    private HttpMethod method = null;
    private String queryString = null;
    private HashMap<String, String> parameters = null;
    private JsonObject jsonObject = null;
    private JsonArray jsonArray = null;
    private String json = null;

    public RESTRequest(HttpServletRequest request, RouteTree routes) {
        this.request = request;
        this.template = routes.get(getHttpMethod(), getPath());
    }

    public String getRemoteAddr() {
        return request.getRemoteAddr();
    }

    public String getPath() {
        if (path == null) {
            path = request.getPathInfo();

            if (path.length() > 0 && path.charAt(0) == '/')
                path = path.substring(1);
        }

        return path;
    }

    public RouteTemplate getTemplate() {
        return template;
    }

    public Class<? extends Action> getActionClass() {
        if (template != null)
            return template.getActionClass();
        else
            return null;
    }

    public HttpMethod getHttpMethod() {
        if (method == null) {
            String actual = request.getMethod();
            String header = request.getHeader(HTTP_METHOD_HEADER);

            String strMethod = header == null ? actual : header;
            try {
                this.method = HttpMethod.valueOf(strMethod.toUpperCase());
            } catch (Throwable e) {
                this.method = HttpMethod.GET;
            }
        }

        return method;
    }

    private boolean isContentType(String contentType) {
        String found = request.getContentType();

        if (found == null || found.isEmpty())
            return false;

        for (String token : found.split(";"))
            if (contentType.equalsIgnoreCase(token))
                return true;

        return false;
    }

    public int getContentLength() {
        return request.getContentLength();
    }

    public Reader getPlainTextContent() throws IOException {
        if (isContentType("text/plain")) {
            return request.getReader();
        } else {
            return null;
        }
    }

    public String getQueryString() {
        if (queryString == null) {
            String qs = request.getQueryString();
            String encoding = request.getCharacterEncoding();

            if (qs == null || qs.isEmpty()) {
                qs = "";

                if (isContentType("application/x-www-form-urlencoded"))
                    try {
                        qs = IOUtils.toString(request.getInputStream(),
                                encoding);
                    } catch (IOException e) {
                        // Ignore it
                    }
            }

            this.queryString = qs;
        }

        return queryString;
    }

    private String getJSONContent() {
        if (json == null) {
            String encoding = request.getCharacterEncoding();
            this.json = "";

            if (isContentType("application/json")) {
                try {
                    this.json = IOUtils.toString(request.getInputStream(),
                            encoding);
                } catch (IOException e) {
                }
            }
        }

        return this.json;
    }

    public String getParameter(String name) {
        if (parameters == null) {
            this.parameters = new HashMap<>();
            String encoding = request.getCharacterEncoding();
            String queryString = this.getQueryString();

            if (!queryString.isEmpty()) {
                String[] params = queryString.split("&");

                for (String param : params) {
                    String[] tokens = param.split("=");
                    if (tokens.length == 2) {
                        try {
                            this.parameters.put(tokens[0], URLDecoder
                                    .decode(tokens[1], encoding).trim());
                        } catch (UnsupportedEncodingException e) {
                            // This should never happen
                        }
                    } else {
                        this.parameters.put(tokens[0], "");
                    }
                }
            }
        }

        return parameters.get(name);
    }

    public String getPathParameter(String varname) throws TemplateException {
        if (pathTokens == null) {
            String path = getPath();
            pathTokens = RouteTemplate.tokenize(path);
        }

        String varName = ':' + varname;
        int i = template.indexOfToken(varName);
        if (i < 0)
            throw new TemplateException(varName);

        String value = pathTokens.get(i).trim();
        if (value.isEmpty())
            throw new TemplateException(varName);

        return value;
    }

    public long getPathParameterAsLong(String varname) throws TemplateException {
        try {
            return Long.parseLong(getPathParameter(varname));
        } catch (NumberFormatException e) {
            throw new TemplateException(varname);
        }
    }

    public JsonObject getJSONObject() {
        if (jsonObject == null) {
            String json = getJSONContent();
            JsonObject parsed = null;

            try {
                JsonParser parser = new JsonParser();
                parsed = parser.parse(json).getAsJsonObject();
            } catch (JsonParseException | IllegalStateException e) {
                // Skip
            }

            if (parsed == null)
                this.jsonObject = NULL_OBJECT;
            else
                this.jsonObject = parsed;
        }

        if (jsonObject == NULL_OBJECT)
            return null;
        else
            return jsonObject;
    }

    public JsonArray getJSONArray() {
        if (jsonArray == null) {
            String json = getJSONContent();
            JsonArray parsed = null;

            try {
                JsonParser parser = new JsonParser();
                parsed = parser.parse(json).getAsJsonArray();
            } catch (JsonParseException | IllegalStateException e) {
                // Skip
            }

            if (parsed == null)
                this.jsonArray = NULL_ARRAY;
            else
                this.jsonArray = parsed;
        }

        if (jsonArray == NULL_ARRAY)
            return null;
        else
            return jsonArray;
    }

}
