package cn.gov.zcy.platform.dao.mybatis3;

import cn.gov.zcy.platform.dao.mybatis3.interceptor.DataBaseDialectInterceptor;
import cn.gov.zcy.platform.dao.mybatis3.dialect.IDataBaseDialect;
import cn.gov.zcy.platform.dao.mybatis3.exception.DataBaseDialectException;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SqlSessionFactoryBean extends org.mybatis.spring.SqlSessionFactoryBean {

  private final static Logger LOG = LoggerFactory.getLogger(SqlSessionFactoryBean.class);

  private IDataBaseDialect dataBaseDialect;

  /* (non-Javadoc)
   * @see org.mybatis.spring.SqlSessionFactoryBean#buildSqlSessionFactory()
   */
  @Override
  protected SqlSessionFactory buildSqlSessionFactory() throws IOException {
    LOG.info("mybatis is starting..");
    //使用父类方法创建factory
    SqlSessionFactory factory = super.buildSqlSessionFactory();

    Configuration configuration = factory.getConfiguration();

    //设置方言
    if (dataBaseDialect == null) {
      throw new DataBaseDialectException("no set dataBaseDialect.");
    }
    configuration.setDatabaseId(dataBaseDialect.getDatabaseId());

    //向拦截器设置方言
    for (Interceptor interceptor : configuration.getInterceptors()) {
      if (interceptor instanceof DataBaseDialectInterceptor) {
        ((DataBaseDialectInterceptor) interceptor).setDataBaseDialect(getDataBaseDialect());
      }
    }

    LOG.info("mybatis ready ok. ");
    return factory;
  }

  /**
   * @return the dataBaseDialect
   */
  public IDataBaseDialect getDataBaseDialect() {
    return dataBaseDialect;
  }

  /**
   * @param dataBaseDialect the dataBaseDialect to set
   */
  public void setDataBaseDialect(IDataBaseDialect dataBaseDialect) {
    this.dataBaseDialect = dataBaseDialect;
  }

}
