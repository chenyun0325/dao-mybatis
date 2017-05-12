package cn.gov.zcy.platform.dao.mybatis3.interceptor;

import cn.gov.zcy.platform.dao.base.id.IDbidGenerator;
import cn.gov.zcy.platform.dao.common.id.MemoryDbidGenerator;
import cn.gov.zcy.platform.dao.mybatis3.exception.IDGenException;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.sql.Statement;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

/**
 * 主键生成器
 *
 */
@Intercepts({
    @Signature(type = StatementHandler.class, method = "parameterize", args = {Statement.class})})
public class IdGenInterceptor implements Interceptor {

  private final static Logger LOG = LoggerFactory.getLogger(IdGenInterceptor.class);
  private String fieldName = "id";//主键字段名称
  private IDbidGenerator dbidGenerator;

  public Object intercept(Invocation invoke) throws Throwable {

    if (invoke.getTarget() instanceof StatementHandler) {
      LOG.debug("IdGenInterceptor start.");
      StatementHandler statementHandler = (StatementHandler) invoke
          .getTarget();
      ParameterHandler handler = statementHandler.getParameterHandler();
      Object parameterObject = handler.getParameterObject();
      if (!(parameterObject instanceof Collection || parameterObject instanceof Map)) {//参数不是集合类
        String sql = statementHandler.getBoundSql().getSql();

        if (sql.toLowerCase().contains("insert ")) {
          String pkName = this.getPkName(handler);
          if (pkName != null) {//配置取不到元信息，则取默认项。
            fieldName = pkName;
          }
          try {
            Object id = ReflectHelper.getValueByFieldName(parameterObject, fieldName);
            if (id == null) {//已经设置过ID的则不进行设置
              Serializable genId = getDbidGenerator().getNextId();
              ReflectHelper.setValueByFieldName(parameterObject, fieldName, genId);
              LOG.debug("Gen a ID value:{}", genId);
            }
          } catch (Exception e) {
            throw new IDGenException(e);
          }
        }
      }

      LOG.debug("IdGenInterceptor end.");

    }
    Object rsObj = invoke.proceed();
    return rsObj;

  }

  /**
   * 主键属性名
   */
  private String getPkName(ParameterHandler handler) {
    try {
      MappedStatement
          mappedStatement =
          (MappedStatement) ReflectHelper.getValueByFieldName(handler, "mappedStatement");
      String sqlId = mappedStatement.getId();
      String mapId = sqlId.substring(0, sqlId.lastIndexOf('.')) + ".BaseResultMap";
      Configuration
          config =
          (Configuration) ReflectHelper.getValueByFieldName(handler, "configuration");
      ResultMap resultMap = config.getResultMap(mapId);
      String propertyName = resultMap.getIdResultMappings().get(0).getProperty();
      return propertyName;
    } catch (Exception e) {
      //无法获取则返回空
      return null;
    }

  }

  /**
   * @return the dbidGenerator
   */
  public IDbidGenerator getDbidGenerator() {
    if (dbidGenerator == null) {
      dbidGenerator = new MemoryDbidGenerator();
    }
    return dbidGenerator;
  }

  /**
   * @param dbidGenerator the dbidGenerator to set
   */
  public void setDbidGenerator(IDbidGenerator dbidGenerator) {
    this.dbidGenerator = dbidGenerator;
  }

  public Object plugin(Object arg0) {
    return Plugin.wrap(arg0, this);
  }

  public void setProperties(Properties p) {

  }

  /**
   * @return the fieldName
   */
  public String getFieldName() {
    return fieldName;
  }

  /**
   * @param fieldName the fieldName to set
   */
  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }


}
