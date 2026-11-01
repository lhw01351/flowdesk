package kr.flowdesk.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.PostConstruct;
import kr.flowdesk.domain.ChatAttachment;
import kr.flowdesk.domain.ChatMessage;
import kr.flowdesk.mapper.ChatMapper;
import kr.flowdesk.util.HtmlEscape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ChatService {

  private static final Logger log = LoggerFactory.getLogger(ChatService.class);
  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final Set<String> ALLOWED_EXT = new HashSet<>(Arrays.asList(
      "png", "jpg", "jpeg", "gif", "webp", "bmp",
      "pdf", "txt", "csv", "zip",
      "doc", "docx", "xls", "xlsx", "ppt", "pptx"));

  private final ChatMapper chatMapper;

  @Value("${flowdesk.chat.upload-dir:./data/chat-uploads}")
  private String uploadDir;

  @Value("${flowdesk.chat.max-file-bytes:5242880}")
  private long maxFileBytes;

  private Path uploadRoot;

  public ChatService(ChatMapper chatMapper) {
    this.chatMapper = chatMapper;
  }

  @PostConstruct
  public void init() throws IOException {
    uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    Files.createDirectories(uploadRoot);
    log.info("채팅 업로드 경로: {}", uploadRoot);
  }

  public List<ChatMessage> recent(int limit) {
    int lim = Math.min(Math.max(limit, 1), 200);
    List<ChatMessage> list = chatMapper.selectRecentMessages(lim);
    Collections.reverse(list);
    attachFiles(list);
    return list;
  }

  @Transactional
  public ChatMessage send(String username, String displayName, String text, List<String> attachmentIds) {
    String body = text == null ? "" : text.trim();
    if (body.length() > 4000) body = body.substring(0, 4000);
    List<String> ids = sanitizeIds(attachmentIds);
    if (body.isEmpty() && ids.isEmpty()) {
      throw new IllegalArgumentException("메시지 또는 첨부가 필요합니다.");
    }

    ChatMessage msg = new ChatMessage();
    msg.setId("cm-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
    msg.setUsername(HtmlEscape.sanitizeText(username));
    msg.setDisplayName(HtmlEscape.sanitizeText(
        displayName == null || displayName.isEmpty() ? username : displayName));
    msg.setBodyText(HtmlEscape.sanitizeText(body));
    msg.setCreatedAt(LocalDateTime.now().format(FMT));
    chatMapper.insertMessage(msg);

    if (!ids.isEmpty()) {
      chatMapper.linkAttachments(msg.getId(), ids);
    }
    msg.setAttachments(enrich(chatMapper.selectAttachmentsByMessageId(msg.getId())));
    return msg;
  }

  @Transactional
  public ChatAttachment upload(MultipartFile file, String username) throws IOException {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("파일이 비어 있습니다.");
    }
    if (file.getSize() > maxFileBytes) {
      throw new IllegalArgumentException("파일은 최대 " + (maxFileBytes / 1024 / 1024) + "MB 까지 가능합니다.");
    }
    String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
    original = Paths.get(original).getFileName().toString();
    String ext = extension(original);
    if (!ALLOWED_EXT.contains(ext)) {
      throw new IllegalArgumentException("허용되지 않는 파일 형식입니다: " + ext);
    }
    String contentType = file.getContentType();
    if (contentType == null || contentType.isEmpty()) {
      contentType = "application/octet-stream";
    }

    String id = "ca-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    String stored = id + (ext.isEmpty() ? "" : "." + ext);
    Path dest = uploadRoot.resolve(stored).normalize();
    if (!dest.startsWith(uploadRoot)) {
      throw new IllegalArgumentException("잘못된 파일 경로");
    }
    Files.copy(file.getInputStream(), dest);

    ChatAttachment att = new ChatAttachment();
    att.setId(id);
    att.setOriginalName(HtmlEscape.sanitizeText(original));
    att.setStoredName(stored);
    att.setContentType(HtmlEscape.sanitizeText(contentType));
    att.setSizeBytes(file.getSize());
    att.setUploadedBy(HtmlEscape.sanitizeText(username));
    att.setUploadedAt(LocalDateTime.now().format(FMT));
    chatMapper.insertAttachment(att);
    return enrichOne(att);
  }

  public Resource loadFile(String id) {
    ChatAttachment att = chatMapper.selectAttachmentById(id);
    if (att == null) return null;
    Path path = uploadRoot.resolve(att.getStoredName()).normalize();
    if (!path.startsWith(uploadRoot) || !Files.isRegularFile(path)) return null;
    return new FileSystemResource(path);
  }

  public ChatAttachment findAttachment(String id) {
    return enrichOne(chatMapper.selectAttachmentById(id));
  }

  public List<ChatAttachment> listAllAttachments() {
    return enrich(chatMapper.selectAllAttachments());
  }

  public int countAttachments() {
    return chatMapper.countAttachments();
  }

  @Transactional
  public boolean deleteAttachment(String id) {
    ChatAttachment att = chatMapper.selectAttachmentById(id);
    if (att == null) return false;
    chatMapper.deleteAttachment(id);
    try {
      Path path = uploadRoot.resolve(att.getStoredName()).normalize();
      if (path.startsWith(uploadRoot)) {
        Files.deleteIfExists(path);
      }
    } catch (Exception e) {
      log.warn("첨부 파일 삭제 실패 {}: {}", id, e.getMessage());
    }
    return true;
  }

  /** 작성자 본인 메시지 본문 수정 */
  @Transactional
  public ChatMessage updateOwnMessage(String id, String username, String text) {
    if (id == null || username == null || username.trim().isEmpty()) {
      throw new IllegalArgumentException("잘못된 요청입니다.");
    }
    ChatMessage msg = chatMapper.selectMessageById(id);
    if (msg == null) {
      throw new IllegalArgumentException("메시지를 찾을 수 없습니다.");
    }
    if (!username.equals(msg.getUsername())) {
      throw new IllegalArgumentException("본인 메시지만 수정할 수 있습니다.");
    }
    String body = text == null ? "" : text.trim();
    if (body.length() > 4000) body = body.substring(0, 4000);
    List<ChatAttachment> atts = chatMapper.selectAttachmentsByMessageId(id);
    boolean hasAtt = atts != null && !atts.isEmpty();
    if (body.isEmpty() && !hasAtt) {
      throw new IllegalArgumentException("메시지 내용이 비어 있습니다.");
    }
    msg.setBodyText(HtmlEscape.sanitizeText(body));
    chatMapper.updateMessageBody(id, msg.getBodyText());
    msg.setAttachments(enrich(atts));
    return msg;
  }

  /** 작성자 본인 메시지 삭제 (첨부 파일 포함) */
  @Transactional
  public boolean deleteOwnMessage(String id, String username) {
    if (id == null || username == null || username.trim().isEmpty()) return false;
    ChatMessage msg = chatMapper.selectMessageById(id);
    if (msg == null) return false;
    if (!username.equals(msg.getUsername())) return false;
    List<ChatAttachment> atts = chatMapper.selectAttachmentsByMessageId(id);
    if (atts != null) {
      for (ChatAttachment a : atts) {
        deleteAttachment(a.getId());
      }
    }
    chatMapper.deleteMessage(id);
    return true;
  }

  public Map<String, Object> stats() {
    Map<String, Object> m = new HashMap<>();
    List<ChatAttachment> all = chatMapper.selectAllAttachments();
    long bytes = 0;
    for (ChatAttachment a : all) {
      if (a.getSizeBytes() != null) bytes += a.getSizeBytes();
    }
    m.put("count", all.size());
    m.put("totalBytes", bytes);
    return m;
  }

  private void attachFiles(List<ChatMessage> list) {
    if (list == null || list.isEmpty()) return;
    List<String> ids = new ArrayList<>();
    for (ChatMessage m : list) ids.add(m.getId());
    if (ids.isEmpty()) return;
    List<ChatAttachment> atts = chatMapper.selectAttachmentsByMessageIds(ids);
    Map<String, List<ChatAttachment>> byMsg = new LinkedHashMap<>();
    for (ChatAttachment a : enrich(atts)) {
      byMsg.computeIfAbsent(a.getMessageId(), k -> new ArrayList<>()).add(a);
    }
    for (ChatMessage m : list) {
      m.setAttachments(byMsg.getOrDefault(m.getId(), Collections.emptyList()));
    }
  }

  private List<ChatAttachment> enrich(List<ChatAttachment> list) {
    if (list == null) return Collections.emptyList();
    for (ChatAttachment a : list) enrichOne(a);
    return list;
  }

  private ChatAttachment enrichOne(ChatAttachment a) {
    if (a == null) return null;
    a.setUrl("/api/chat/files/" + a.getId());
    return a;
  }

  private List<String> sanitizeIds(List<String> ids) {
    if (ids == null || ids.isEmpty()) return Collections.emptyList();
    List<String> out = new ArrayList<>();
    for (String id : ids) {
      if (id == null) continue;
      String t = id.trim();
      if (!t.isEmpty() && out.size() < 5) out.add(t);
    }
    return out;
  }

  private String extension(String name) {
    int i = name.lastIndexOf('.');
    if (i < 0 || i == name.length() - 1) return "";
    return name.substring(i + 1).toLowerCase(Locale.ROOT);
  }
}
