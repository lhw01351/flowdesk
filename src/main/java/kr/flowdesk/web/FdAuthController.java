package kr.flowdesk.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kr.flowdesk.domain.FdAuth;
import kr.flowdesk.service.FdAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 권한 마스터 조회 (부여는 회원 화면에서) */
@RestController
@RequestMapping("/api/auths")
public class FdAuthController {

  private final FdAuthService fdAuthService;

  public FdAuthController(FdAuthService fdAuthService) {
    this.fdAuthService = fdAuthService;
  }

  @GetMapping
  public List<FdAuth> list() {
    return fdAuthService.findAll();
  }

  @PutMapping("/{authCd}")
  public FdAuth update(@PathVariable String authCd, @RequestBody FdAuth body) {
    return fdAuthService.update(authCd, body);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> bad(IllegalArgumentException e) {
    Map<String, Object> body = new HashMap<>();
    body.put("ok", false);
    body.put("message", e.getMessage());
    return ResponseEntity.badRequest().body(body);
  }
}
