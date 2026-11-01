package kr.flowdesk.mapper;

import java.util.List;
import kr.flowdesk.domain.ChatAttachment;
import kr.flowdesk.domain.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChatMapper {

  int insertMessage(ChatMessage msg);

  List<ChatMessage> selectRecentMessages(@Param("limit") int limit);

  ChatMessage selectMessageById(@Param("id") String id);

  int insertAttachment(ChatAttachment att);

  int linkAttachments(@Param("messageId") String messageId, @Param("ids") List<String> ids);

  List<ChatAttachment> selectAttachmentsByMessageIds(@Param("messageIds") List<String> messageIds);

  List<ChatAttachment> selectAttachmentsByMessageId(@Param("messageId") String messageId);

  ChatAttachment selectAttachmentById(@Param("id") String id);

  List<ChatAttachment> selectAllAttachments();

  int deleteAttachment(@Param("id") String id);

  int deleteMessage(@Param("id") String id);

  int updateMessageBody(@Param("id") String id, @Param("bodyText") String bodyText);

  int countAttachments();
}
