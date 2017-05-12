package cn.gov.zcy.platform.dao.mybatis3.dialect;

/**
 * mysql
 *
 * @author: chenyun
 * @since: 2015年4月29日 下午3:20:23
 */
public class MysqlDialect implements IDataBaseDialect {

  public String getDatabaseId() {
    return "mysql";
  }

  public String generatePageSql(String querySql, int start, int limit) {
    String pagesql = querySql + " limit " + start + "," + limit;
    return pagesql;
  }

}
