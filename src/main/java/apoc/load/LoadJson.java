package apoc.load;

import apoc.Description;
import apoc.result.MapResult;
import apoc.result.ObjectResult;
import apoc.util.JsonUtil;
import apoc.util.MapUtil;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.net.URI;
import java.util.*;
import java.util.stream.Stream;

public class LoadJson {

    private static final String AUTH_HEADER_KEY = "Authorization";

    @Context
    public GraphDatabaseService db;

    @SuppressWarnings("unchecked")
    @Procedure
    @Description("apoc.load.jsonArray('url') YIELD value - load array from JSON URL (e.g. web-api) to import JSON as stream of values")
    public Stream<ObjectResult> jsonArray(@Name("url") String url) {
        Object value = JsonUtil.loadJson(url);
        if (value instanceof List) {
            return ((List) value).stream().map(ObjectResult::new);
        }
        throw new RuntimeException("Incompatible Type " + (value == null ? "null" : value.getClass()));
    }

    @Procedure
    @Description("apoc.load.json('url') YIELD value -  import JSON as stream of values if the JSON was an array or a single value if it was a map")
    public Stream<MapResult> json(@Name("url") String url) {
        return jsonParams(url,null,null);
    }

    @SuppressWarnings("unchecked")
    @Procedure
    @Description("apoc.load.jsonParams('url',{header:value},payload) YIELD value - load from JSON URL (e.g. web-api) while sending headers / payload to import JSON as stream of values if the JSON was an array or a single value if it was a map")
    public Stream<MapResult> jsonParams(@Name("url") String url, @Name("headers") Map<String,Object> headers, @Name("payload") String payload) {
        return loadJsonStream(url, headers, payload);
    }

    public static Stream<MapResult> loadJsonStream(@Name("url") String url, @Name("headers") Map<String, Object> headers, @Name("payload") String payload) {
        headers = null != headers ? headers : new HashMap<>();
        headers.putAll(extractCredentialsIfNeeded(url));
        Object value = JsonUtil.loadJson(url,headers,payload);
        if (value instanceof Map) {
            return Stream.of(new MapResult((Map) value));
        }
        if (value instanceof List) {
            return ((List) value).stream().map((v) -> new MapResult((Map) v));
        }
        throw new RuntimeException("Incompatible Type " + (value == null ? "null" : value.getClass()));
    }

    private static Map<String, Object> extractCredentialsIfNeeded(String url) {
        try {
            URI uri = new URI(url);
            String authInfo = uri.getUserInfo();
            if (null != authInfo) {
                String[] parts = authInfo.split(":");
                if (2 == parts.length) {
                    String token = new String(Base64.getEncoder().encode(authInfo.getBytes()));
                    return MapUtil.map(AUTH_HEADER_KEY, "Basic " + token);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return Collections.emptyMap();
    }
}
