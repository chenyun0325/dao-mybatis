package cn.gov.zcy.platform.dao.mybatis3.dialect;

import org.apache.ibatis.mapping.DatabaseIdProvider;

import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

/**
 * 使用方言中定义的databaseId来标识数据库类型 使用场景：通过databaseid将数据插入不同数据库
 *
 * @author: chenyun
 * @since: 2015年4月29日 下午3:31:03
 */
public class DialectDatabaseIdProvider implements DatabaseIdProvider {

  private IDataBaseDialect dialect;

  public IDataBaseDialect getDialect() {
    return dialect;
  }

  public void setDialect(IDataBaseDialect dialect) {
    this.dialect = dialect;
  }

  public void setProperties(Properties p) {
  }

  public String getDatabaseId(DataSource dataSource) throws SQLException {
    if (dialect == null) {
      System.err.println("no config database's dialect");
      return null;
    }
    return dialect.getDatabaseId();
  }

}
