package kr.flowdesk.mapper;

import java.util.List;
import kr.flowdesk.domain.FdMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FdMenuMapper {

  List<FdMenu> selectAll();

  FdMenu selectById(@Param("menuId") String menuId);

  FdMenu selectByKey(@Param("menuKey") String menuKey);

  List<FdMenu> selectByAuthCd(@Param("authCd") String authCd);

  int countAll();

  int insert(FdMenu menu);

  int update(FdMenu menu);

  int deleteById(@Param("menuId") String menuId);

  List<String> selectAuthCdsByMenuId(@Param("menuId") String menuId);

  int deleteAuthByMenuId(@Param("menuId") String menuId);

  int deleteAuthByAuthCd(@Param("authCd") String authCd);

  int insertMenuAuth(@Param("authCd") String authCd, @Param("menuId") String menuId, @Param("createDt") String createDt);

  int countMenuAuth();
}
