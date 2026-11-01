package kr.flowdesk.util;

/**
 * XSS 완화용 HTML 이스케이프 / 텍스트 sanitize
 */
public final class HtmlEscape {

  private HtmlEscape() {}

  public static String escape(String input) {
    if (input == null || input.isEmpty()) {
      return input;
    }
    StringBuilder sb = new StringBuilder(input.length() + 16);
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      switch (c) {
        case '&': sb.append("&amp;"); break;
        case '<': sb.append("&lt;"); break;
        case '>': sb.append("&gt;"); break;
        case '"': sb.append("&quot;"); break;
        case '\'': sb.append("&#39;"); break;
        case '/': sb.append("&#47;"); break;
        default: sb.append(c);
      }
    }
    return sb.toString();
  }

  /**
   * 저장용: 스크립트/태그 각도괄호 제거 + 제어문자 제거 (표시는 escape 권장)
   */
  public static String sanitizeText(String input) {
    if (input == null) {
      return null;
    }
    String s = input.replace('\0', ' ').trim();
    s = s.replaceAll("[<>]", "");
    s = s.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
    return s;
  }
}
