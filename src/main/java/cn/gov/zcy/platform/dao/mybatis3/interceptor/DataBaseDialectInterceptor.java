package cn.gov.zcy.platform.dao.mybatis3.interceptor;

import cn.gov.zcy.platform.dao.mybatis3.dialect.IDataBaseDialect;

import org.apache.ibatis.plugin.Interceptor;

public abstract class DataBaseDialectInterceptor implements Interceptor {

  private IDataBaseDialect dataBaseDialect;

  public IDataBaseDialect getDataBaseDialect() {
    return dataBaseDialect;
  }

  public void setDataBaseDialect(IDataBaseDialect dataBaseDialect) {
    this.dataBaseDialect = dataBaseDialect;
  }


}
