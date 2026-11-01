package kr.flowdesk.service.system;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.annotation.PostConstruct;
import kr.flowdesk.domain.AppUser;
import kr.flowdesk.domain.FdAuth;
import kr.flowdesk.mapper.AppUserMapper;
import kr.flowdesk.util.HtmlEscape;
import kr.flowdesk.web.system.AuthController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AppUserService {

  private static final Logger log = LoggerFactory.getLogger(AppUserService.class);
  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private static final Set<String> ALLOWED_EXT =
      new HashSet<>(Arrays.asList("png", "jpg", "jpeg", "gif", "webp"));

  private final AppUserMapper appUserMapper;
  private final FdAuthService fdAuthService;

  @Value("${flowdesk.profile.upload-dir:./data/profile-images}")
  private String profileUploadDir;

  @Value("${flowdesk.profile.max-file-bytes:2097152}")
  private long maxProfileBytes;

  private Path profileRoot;

  public AppUserService(AppUserMapper appUserMapper, FdAuthService fdAuthService) {
    this.appUserMapper = appUserMapper;
    this.fdAuthService = fdAuthService;
  }

  @PostConstruct
  public void init() throws IOException {
    profileRoot = Paths.get(profileUploadDir).toAbsolutePath().normalize();
    Files.createDirectories(profileRoot);
    log.info("프로필 이미지 경로: {}", profileRoot);
  }

  public List<AppUser> findAll() {
    return appUserMapper.selectAll();
  }

  public AppUser findById(String id) {
    return appUserMapper.selectById(id);
  }

  public AppUser findByUsername(String username) {
    return appUserMapper.selectByUsername(username);
  }

  public int count() {
    return appUserMapper.countAll();
  }

  public AppUser authenticate(String username, String password) {
    AppUser user = appUserMapper.selectByUsername(username);
    if (user == null || user.getPassword() == null) {
      return null;
    }
    if (!user.getPassword().equals(password)) {
      return null;
    }
    return user;
  }

  /** auth_cd → 레거시 role (필터 호환) */
  public static String roleFromAuthCd(String authCd) {
    if (FdAuthService.AUTH_ADMIN.equals(authCd)) {
      return AuthController.ROLE_ADMIN;
    }
    return AuthController.ROLE_GUEST;
  }

  public static boolean hasProfileImage(AppUser user) {
    return user != null
        && user.getProfileImage() != null
        && !user.getProfileImage().trim().isEmpty();
  }

  @Transactional
  public AppUser create(AppUser user) {
    String username = user.getUsername() == null ? "" : user.getUsername().trim();
    String password = user.getPassword() == null ? "" : user.getPassword();
    String authCd = normalizeAuthCd(user.getAuthCd());

    if (username.isEmpty()) {
      throw new IllegalArgumentException("아이디를 입력해주세요.");
    }
    if (password.isEmpty()) {
      throw new IllegalArgumentException("비밀번호를 입력해주세요.");
    }
    if (appUserMapper.selectByUsername(username) != null) {
      throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
    }

    user.setId("u-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
    user.setUsername(HtmlEscape.sanitizeText(username));
    user.setPassword(password);
    user.setAuthCd(authCd);
    user.setRole(roleFromAuthCd(authCd));
    user.setDisplayName(HtmlEscape.sanitizeText(user.getDisplayName()));
    user.setNote(HtmlEscape.sanitizeText(user.getNote()));
    user.setCreatedAt(LocalDateTime.now().format(FMT));
    user.setProfileImage(null);
    appUserMapper.insert(user);
    return appUserMapper.selectById(user.getId());
  }

  @Transactional
  public AppUser update(String id, AppUser incoming) {
    AppUser existing = appUserMapper.selectById(id);
    if (existing == null) {
      throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
    }
    String username = incoming.getUsername() == null ? existing.getUsername() : incoming.getUsername().trim();
    if (username.isEmpty()) {
      throw new IllegalArgumentException("아이디를 입력해주세요.");
    }
    AppUser dup = appUserMapper.selectByUsername(username);
    if (dup != null && !dup.getId().equals(id)) {
      throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
    }
    existing.setUsername(HtmlEscape.sanitizeText(username));
    if (incoming.getPassword() != null && !incoming.getPassword().isEmpty()) {
      existing.setPassword(incoming.getPassword());
    }
    if (incoming.getAuthCd() != null && !incoming.getAuthCd().trim().isEmpty()) {
      String authCd = normalizeAuthCd(incoming.getAuthCd());
      existing.setAuthCd(authCd);
      existing.setRole(roleFromAuthCd(authCd));
    } else if (incoming.getRole() != null && !incoming.getRole().trim().isEmpty()) {
      // 하위호환
      existing.setRole(normalizeLegacyRole(incoming.getRole()));
      if (AuthController.ROLE_ADMIN.equals(existing.getRole())
          || AuthController.ROLE_WORK.equals(existing.getRole())) {
        existing.setAuthCd(FdAuthService.AUTH_ADMIN);
      } else {
        existing.setAuthCd(FdAuthService.AUTH_USER);
      }
    }
    existing.setDisplayName(HtmlEscape.sanitizeText(incoming.getDisplayName()));
    existing.setNote(HtmlEscape.sanitizeText(incoming.getNote()));
    appUserMapper.update(existing);
    return appUserMapper.selectById(id);
  }

  @Transactional
  public AppUser saveProfileImage(String id, MultipartFile file) throws IOException {
    AppUser existing = appUserMapper.selectById(id);
    if (existing == null) {
      throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
    }
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("이미지 파일이 비어 있습니다.");
    }
    if (file.getSize() > maxProfileBytes) {
      throw new IllegalArgumentException("프로필 사진은 최대 2MB까지 가능합니다.");
    }
    String original = file.getOriginalFilename() == null ? "photo.png" : file.getOriginalFilename();
    original = Paths.get(original).getFileName().toString();
    String ext = extension(original);
    if (!ALLOWED_EXT.contains(ext)) {
      throw new IllegalArgumentException("허용되지 않는 이미지 형식입니다. (png/jpg/gif/webp)");
    }

    String stored = existing.getId() + "." + ext;
    Path dest = profileRoot.resolve(stored).normalize();
    if (!dest.startsWith(profileRoot)) {
      throw new IllegalArgumentException("잘못된 파일 경로입니다.");
    }
    deleteProfileFile(existing.getProfileImage());
    file.transferTo(dest.toFile());
    appUserMapper.updateProfileImage(id, stored);
    return appUserMapper.selectById(id);
  }

  public Resource loadProfileImage(AppUser user) {
    if (!hasProfileImage(user)) {
      return null;
    }
    Path path = profileRoot.resolve(user.getProfileImage()).normalize();
    if (!path.startsWith(profileRoot) || !Files.isRegularFile(path)) {
      return null;
    }
    return new FileSystemResource(path);
  }

  public String contentTypeFor(AppUser user) {
    if (!hasProfileImage(user)) {
      return "application/octet-stream";
    }
    String ext = extension(user.getProfileImage());
    switch (ext) {
      case "png":
        return "image/png";
      case "gif":
        return "image/gif";
      case "webp":
        return "image/webp";
      case "jpg":
      case "jpeg":
      default:
        return "image/jpeg";
    }
  }

  @Transactional
  public void delete(String id) {
    AppUser existing = appUserMapper.selectById(id);
    if (existing != null) {
      deleteProfileFile(existing.getProfileImage());
    }
    appUserMapper.deleteById(id);
  }

  private void deleteProfileFile(String filename) {
    if (filename == null || filename.trim().isEmpty()) return;
    try {
      Path path = profileRoot.resolve(filename).normalize();
      if (path.startsWith(profileRoot)) {
        Files.deleteIfExists(path);
      }
    } catch (Exception ignored) {
    }
  }

  private static String extension(String name) {
    int i = name.lastIndexOf('.');
    if (i < 0 || i == name.length() - 1) return "";
    return name.substring(i + 1).toLowerCase(Locale.ROOT);
  }

  private String normalizeAuthCd(String authCd) {
    String cd = authCd == null ? FdAuthService.AUTH_USER : authCd.trim();
    if (cd.isEmpty()) cd = FdAuthService.AUTH_USER;
    FdAuth auth = fdAuthService.findByCd(cd);
    if (auth == null) {
      throw new IllegalArgumentException("유효하지 않은 권한코드입니다: " + cd);
    }
    return cd;
  }

  private String normalizeLegacyRole(String role) {
    String r = role.trim().toUpperCase();
    if (AuthController.ROLE_ADMIN.equals(r)
        || AuthController.ROLE_WORK.equals(r)
        || AuthController.ROLE_GUEST.equals(r)) {
      return r;
    }
    throw new IllegalArgumentException("역할이 올바르지 않습니다.");
  }
}
