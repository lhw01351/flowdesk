package kr.flowdesk.mapper;

import java.util.List;
import kr.flowdesk.domain.Inquiry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InquiryMapper {

  List<Inquiry> selectByCreateUser(@Param("createUser") String createUser);

  Inquiry selectById(@Param("id") String id);

  int countByCreateUser(@Param("createUser") String createUser);

  int insert(Inquiry inquiry);

  int update(Inquiry inquiry);

  int deleteById(@Param("id") String id);

  int deleteByCreateUser(@Param("createUser") String createUser);

  int backfillCreateUser(@Param("createUser") String createUser);
}
