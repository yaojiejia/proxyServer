public class CachedResponse {
    private final String response;
    private final long timeStamp;
    public CachedResponse(String response, long timeStamp) {
        this.response = response;
        this.timeStamp = timeStamp;
    }
    public String getResponse() {
        return response;
    }
    public long getTimeStamp() {
        return timeStamp;
    }
    public boolean isExpired(){
        return System.currentTimeMillis() - timeStamp > 5 * 60 * 1000;
    }
}
