package kr.flowdesk.mapper;

import java.util.List;
import kr.flowdesk.domain.Reminder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReminderMapper {

  List<Reminder> selectByUserAndRange(
      @Param("username") String username,
      @Param("from") String from,
      @Param("to") String to);

  Reminder selectById(@Param("id") String id);

  int insert(Reminder row);

  int update(Reminder row);

  int delete(@Param("id") String id, @Param("username") String username);
}
