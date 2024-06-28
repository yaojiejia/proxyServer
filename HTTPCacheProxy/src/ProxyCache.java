import java.util.HashMap;
import java.util.Map;

public class ProxyCache {
    private final Map<String, CachedResponse> cache = new HashMap<>();

    public void cacheResponse(String url, String response){
        cache.put(url, new CachedResponse(response, System.currentTimeMillis()));
    }
    public String getCacheResponse(String url){
        CachedResponse cachedResponse = cache.get(url);
        if (cachedResponse != null && !cachedResponse.isExpired()){
            System.out.println("Cache hit for " + url);
            return cachedResponse.getResponse();
        }
        return null;
    }
}
