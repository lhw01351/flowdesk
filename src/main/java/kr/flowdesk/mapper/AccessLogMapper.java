package kr.flowdesk.mapper;

import java.util.List;
import kr.flowdesk.domain.AccessLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AccessLogMapper {

  List<AccessLog> selectAll();

  List<AccessLog> selectByFilter(
      @Param("from") String from,
      @Param("to") String to,
      @Param("username") String username,
      @Param("eventType") String eventType);

  int insert(AccessLog log);

  int deleteByIds(@Param("ids") List<String> ids);

  int deleteAll();

  int countAll();
}
