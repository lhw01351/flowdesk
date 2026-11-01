package kr.flowdesk.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kr.flowdesk.domain.FdMenu;
import kr.flowdesk.service.FdMenuService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 메뉴 명칭·권한 설정 */
@RestController
@RequestMapping("/api/menus")
public class FdMenuController {

  private final FdMenuService fdMenuService;

  public FdMenuController(FdMenuService fdMenuService) {
    this.fdMenuService = fdMenuService;
  }

  @GetMapping
  public List<FdMenu> list() {
    return fdMenuService.findAllWithAuth();
  }

  @PutMapping("/{menuId}")
  public FdMenu update(@PathVariable String menuId, @RequestBody FdMenu body) {
    return fdMenuService.update(menuId, body);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> bad(IllegalArgumentException e) {
    Map<String, Object> body = new HashMap<>();
    body.put("ok", false);
    body.put("message", e.getMessage());
    return ResponseEntity.badRequest().body(body);
  }
}
