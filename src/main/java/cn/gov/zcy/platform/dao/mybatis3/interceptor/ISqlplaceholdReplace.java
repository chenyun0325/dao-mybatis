package cn.gov.zcy.platform.dao.mybatis3.interceptor;

/**
 * Created by chenyun on 16/3/24.
 */
public interface ISqlplaceholdReplace {

  /**
   * 获取被替换的sql
   * @return
   */
  String getReplacedSql();

}
