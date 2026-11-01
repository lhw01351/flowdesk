package kr.flowdesk.mapper;

import java.util.List;
import kr.flowdesk.domain.InbodyRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InbodyMapper {

  List<InbodyRecord> selectByCreateUser(@Param("createUser") String createUser);

  InbodyRecord selectById(@Param("id") String id);

  InbodyRecord selectLatestByCreateUser(@Param("createUser") String createUser);

  int countAll();

  int countByCreateUser(@Param("createUser") String createUser);

  int insert(InbodyRecord record);

  int update(InbodyRecord record);

  int deleteById(@Param("id") String id);

  int backfillCreateUser(@Param("createUser") String createUser);
}
