package kr.flowdesk.mapper;

import java.util.List;
import kr.flowdesk.domain.AppUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AppUserMapper {

  List<AppUser> selectAll();

  AppUser selectById(@Param("id") String id);

  AppUser selectByUsername(@Param("username") String username);

  int countAll();

  int insert(AppUser user);

  int update(AppUser user);

  int updateProfileImage(@Param("id") String id, @Param("profileImage") String profileImage);

  int deleteById(@Param("id") String id);
}
