package kr.flowdesk.web.content;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpSession;
import kr.flowdesk.domain.ChatAttachment;
import kr.flowdesk.domain.ChatMessage;
import kr.flowdesk.service.content.ChatService;
import kr.flowdesk.service.system.MenuAccessService;
import kr.flowdesk.web.system.AuthController;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import kr.flowdesk.config.ChatHandshakeInterceptor;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

  private final ChatService chatService;
  private final SimpMessagingTemplate messagingTemplate;

  public ChatController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
    this.chatService = chatService;
    this.messagingTemplate = messagingTemplate;
  }

  @GetMapping("/messages")
  public List<ChatMessage> messages(@RequestParam(defaultValue = "80") int limit) {
    return chatService.recent(limit);
  }

  @PostMapping("/upload")
  public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file, HttpSession session) {
    String user = AuthController.usernameOf(session);
    if (user == null) {
      return ResponseEntity.status(401).body(map("ok", false, "message", "로그인이 필요합니다."));
    }
    try {
      return ResponseEntity.ok(chatService.upload(file, user));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(map("ok", false, "message", e.getMessage()));
    } catch (Exception e) {
      return ResponseEntity.status(500).body(map("ok", false, "message", "업로드 실패"));
    }
  }

  @GetMapping("/files/{id}")
  public ResponseEntity<Resource> file(
      @PathVariable String id,
      @RequestParam(value = "dl", required = false) String dl) {
    ChatAttachment meta = chatService.findAttachment(id);
    Resource res = chatService.loadFile(id);
    if (meta == null || res == null) {
      return ResponseEntity.notFound().build();
    }
    String ct = meta.getContentType() == null ? "application/octet-stream" : meta.getContentType();
    String name = meta.getOriginalName() == null ? id : meta.getOriginalName();
    String safe = name.replace("\"", "");
    boolean download = "1".equals(dl) || "true".equalsIgnoreCase(dl);
    String disposition = (download ? "attachment" : "inline") + "; filename=\"" + safe + "\"";
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
        .contentType(MediaType.parseMediaType(ct))
        .body(res);
  }

  /** 본인 메시지 수정 */
  @PutMapping("/messages/{id}")
  public ResponseEntity<?> updateOwn(
      @PathVariable String id, @RequestBody Map<String, Object> body, HttpSession session) {
    String user = AuthController.usernameOf(session);
    if (user == null) {
      return ResponseEntity.status(401).body(map("ok", false, "message", "로그인이 필요합니다."));
    }
    try {
      ChatMessage msg = chatService.updateOwnMessage(id, user, str(body.get("text")));
      Map<String, Object> ev = new HashMap<>();
      ev.put("type", "update");
      ev.put("message", msg);
      messagingTemplate.convertAndSend("/topic/chat", ev);
      return ResponseEntity.ok(msg);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(map("ok", false, "message", e.getMessage()));
    }
  }

  /** 본인 메시지 삭제 */
  @DeleteMapping("/messages/{id}")
  public ResponseEntity<?> deleteOwn(@PathVariable String id, HttpSession session) {
    String user = AuthController.usernameOf(session);
    if (user == null) {
      return ResponseEntity.status(401).body(map("ok", false, "message", "로그인이 필요합니다."));
    }
    boolean ok = chatService.deleteOwnMessage(id, user);
    if (!ok) {
      return ResponseEntity.status(403).body(map("ok", false, "message", "삭제할 수 없습니다."));
    }
    Map<String, Object> ev = new HashMap<>();
    ev.put("type", "delete");
    ev.put("id", id);
    messagingTemplate.convertAndSend("/topic/chat", ev);
    return ResponseEntity.ok(map("ok", true, "id", id));
  }

  /** REST 폴백 전송 (WebSocket 실패 시) */
  @PostMapping("/send")
  public ResponseEntity<?> sendRest(@RequestBody Map<String, Object> body, HttpSession session) {
    String user = AuthController.usernameOf(session);
    if (user == null) {
      return ResponseEntity.status(401).body(map("ok", false, "message", "로그인이 필요합니다."));
    }
    Object display = session.getAttribute(AuthController.SESSION_DISPLAY);
    try {
      ChatMessage msg = chatService.send(
          user,
          display == null ? user : String.valueOf(display),
          str(body.get("text")),
          toIdList(body.get("attachmentIds")));
      messagingTemplate.convertAndSend("/topic/chat", msg);
      return ResponseEntity.ok(msg);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(map("ok", false, "message", e.getMessage()));
    }
  }

  @MessageMapping("/chat.send")
  public void sendWs(@Payload Map<String, Object> body, SimpMessageHeaderAccessor headers) {
    Map<String, Object> attrs = headers.getSessionAttributes();
    String user = attrs == null ? null : String.valueOf(attrs.get(ChatHandshakeInterceptor.ATTR_USER));
    String display = attrs == null ? null : String.valueOf(attrs.get(ChatHandshakeInterceptor.ATTR_DISPLAY));
    if (user == null || "null".equals(user)) return;
    ChatMessage msg = chatService.send(
        user,
        display == null || "null".equals(display) ? user : display,
        str(body.get("text")),
        toIdList(body.get("attachmentIds")));
    messagingTemplate.convertAndSend("/topic/chat", msg);
  }

  @GetMapping("/admin/attachments")
  public ResponseEntity<?> adminList(HttpSession session) {
    if (!MenuAccessService.isManager(session)) {
      return ResponseEntity.status(403).body(map("ok", false, "message", "권한이 없습니다."));
    }
    Map<String, Object> res = new HashMap<>();
    res.put("stats", chatService.stats());
    res.put("list", chatService.listAllAttachments());
    return ResponseEntity.ok(res);
  }

  @DeleteMapping("/admin/attachments/{id}")
  public ResponseEntity<?> adminDelete(@PathVariable String id, HttpSession session) {
    if (!MenuAccessService.isManager(session)) {
      return ResponseEntity.status(403).body(map("ok", false, "message", "권한이 없습니다."));
    }
    boolean ok = chatService.deleteAttachment(id);
    if (!ok) return ResponseEntity.notFound().build();
    return ResponseEntity.ok(map("ok", true, "deleted", id));
  }

  private List<String> toIdList(Object raw) {
    if (!(raw instanceof List)) return Collections.emptyList();
    java.util.ArrayList<String> out = new java.util.ArrayList<>();
    for (Object o : (List<?>) raw) {
      if (o != null) out.add(String.valueOf(o));
    }
    return out;
  }

  private String str(Object o) {
    return o == null ? "" : String.valueOf(o);
  }

  private Map<String, Object> map(Object... kv) {
    Map<String, Object> m = new HashMap<>();
    for (int i = 0; i + 1 < kv.length; i += 2) {
      m.put(String.valueOf(kv[i]), kv[i + 1]);
    }
    return m;
  }
}
