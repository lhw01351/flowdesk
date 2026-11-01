package kr.flowdesk.config;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 기본 보안 응답 헤더 (XSS/클릭재킹/MIME 스니핑 완화)
 */
@Component
@Order(0)
public class SecurityHeadersFilter extends OncePerRequestFilter {

  private static final String CSP =
      "default-src 'self'; "
          + "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; "
          + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
          + "font-src 'self' https://fonts.gstatic.com data:; "
          + "img-src 'self' data: blob: https:; "
          + "connect-src 'self' ws: wss: https://api.open-meteo.com https://geocoding-api.open-meteo.com https://nominatim.openstreetmap.org; "
          + "frame-src 'self' https://www.youtube.com https://www.youtube-nocookie.com; "
          + "child-src 'self' https://www.youtube.com https://www.youtube-nocookie.com; "
          + "media-src 'self' https: blob:; "
          + "frame-ancestors 'none'; "
          + "base-uri 'self'; "
          + "form-action 'self'";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("X-Frame-Options", "DENY");
    response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
    response.setHeader("X-XSS-Protection", "1; mode=block");
    response.setHeader("Content-Security-Policy", CSP);
    response.setHeader("Permissions-Policy", "geolocation=(self), camera=(), microphone=()");
    filterChain.doFilter(request, response);
  }
}
