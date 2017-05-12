package cn.gov.zcy.platform.dao.mybatis3.dialect;

/**
 * @author: chenyun
 * @since: 2015年4月29日 下午3:18:02
 */
public interface IDataBaseDialect {

  /**
   * 数据库标识 mysql oracle db2 sqlserver.. <br>标识须用小写、无空格
   */
  String getDatabaseId();

  /**
   * 生成分页查询的sql
   *
   * @param querySql 基础查询
   * @param start    起始行. 从0开始,第n页则为:(n-1) * limit
   * @param limit    查询行数
   */
  String generatePageSql(String querySql, int start, int limit);

}
