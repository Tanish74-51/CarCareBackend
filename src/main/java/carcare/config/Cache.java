package CarCareBackend.src.main.java.carcare.config;

import CarCareBackend.src.main.java.carcare.http.Request;
import CarCareBackend.src.main.java.carcare.http.Response;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Cache {

    private final Lock cacheLock=new ReentrantLock();
    private static final long TTL_SECONDS=10;
    private static final int MAX_SIZE=500;
    private static final LinkedHashMap<Request,ResponseNode> cache=new LinkedHashMap<>(16,0.75f,true);

    public record ResponseNode(Response response,Instant expiresAt){}

    public Cache(){}

    public Response getCached(Request request){
        cacheLock.lock();
        try {
            ResponseNode node=cache.get(request);
            if (node==null) return null;
            if (node.expiresAt().isBefore(Instant.now())) {
                cache.remove(request);
                return null;
            }
            return node.response();
        } finally {
            cacheLock.unlock();
        }
    }

    public void addCache(Request request,Response response){
        cacheLock.lock();
        try {
            cache.put(request, new ResponseNode(response, Instant.now().plusSeconds(TTL_SECONDS)));
            if (cache.size()>MAX_SIZE) {
                Request eldest=cache.keySet().iterator().next();
                cache.remove(eldest);
            }
        }
        finally {
            cacheLock.unlock();
        }
    }

    public void removeCache(Request request){
        cacheLock.lock();
        try {
            cache.remove(request);
        } finally {
            cacheLock.unlock();
        }
    }




}
