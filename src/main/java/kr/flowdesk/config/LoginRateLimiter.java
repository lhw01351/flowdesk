package kr.flowdesk.config;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 로그인 실패 IP 기준 단순 레이트 리밋 (인메모리).
 * 연속 실패 시 일시 차단 — 재시작 시 초기화.
 */
@Component
public class LoginRateLimiter {

  private static final int MAX_FAILURES = 8;
  private static final long WINDOW_MS = 15 * 60 * 1000L;
  private static final long BLOCK_MS = 15 * 60 * 1000L;

  private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

  public boolean isBlocked(String ip) {
    if (ip == null || ip.isEmpty()) return false;
    prune();
    Bucket b = buckets.get(ip);
    if (b == null) return false;
    long now = System.currentTimeMillis();
    return b.blockedUntil > now;
  }

  public void onFailure(String ip) {
    if (ip == null || ip.isEmpty()) return;
    long now = System.currentTimeMillis();
    buckets.compute(ip, (k, old) -> {
      Bucket b = old == null ? new Bucket() : old;
      if (now - b.windowStart > WINDOW_MS) {
        b.windowStart = now;
        b.failures = 0;
      }
      b.failures++;
      if (b.failures >= MAX_FAILURES) {
        b.blockedUntil = now + BLOCK_MS;
      }
      return b;
    });
  }

  public void onSuccess(String ip) {
    if (ip == null || ip.isEmpty()) return;
    buckets.remove(ip);
  }

  private void prune() {
    if (buckets.size() < 500) return;
    long now = System.currentTimeMillis();
    Iterator<Map.Entry<String, Bucket>> it = buckets.entrySet().iterator();
    while (it.hasNext()) {
      Bucket b = it.next().getValue();
      if (b.blockedUntil < now && now - b.windowStart > WINDOW_MS) {
        it.remove();
      }
    }
  }

  private static final class Bucket {
    long windowStart = System.currentTimeMillis();
    int failures;
    long blockedUntil;
  }
}
