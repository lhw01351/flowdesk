package kr.flowdesk.mapper;

import java.util.List;
import kr.flowdesk.domain.FdAuth;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FdAuthMapper {

  List<FdAuth> selectAll();

  FdAuth selectByCd(@Param("authCd") String authCd);

  int countAll();

  int insert(FdAuth auth);

  int update(FdAuth auth);

  int deleteByCd(@Param("authCd") String authCd);
}
