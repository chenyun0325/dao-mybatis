package cn.gov.zcy.platform.dao.mybatis3.interceptor;


import cn.gov.zcy.platform.dao.base.para.IPageConverter;
import cn.gov.zcy.platform.dao.base.para.IPageParameter;
import cn.gov.zcy.platform.dao.base.para.PageConverterFactory;
import cn.gov.zcy.platform.dao.common.para.PageParameter;
import cn.gov.zcy.platform.dao.mybatis3.exception.PaginationException;

import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.executor.statement.BaseStatementHandler;
import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.scripting.xmltags.ForEachSqlNode;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 * 分页拦截器
 *
 * @author: chenyun
 * @since: 2015年4月29日 下午4:10:18
 */
@Intercepts({
    @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class,Integer.class})})
public class PaginationInterceptor extends DataBaseDialectInterceptor {

  private final static Logger LOG = LoggerFactory.getLogger(PaginationInterceptor.class);
  //对象映射文件中sql标识的通配
  private String pageMapper = "";
  private String pageVarName = "page";
  @Autowired(required = false)
  private PageConverterFactory pageConverterFactory;//分页转换器

  public String getPageMapper() {
    return pageMapper;
  }

  public void setPageMapper(String pageMapper) {
    this.pageMapper = pageMapper;
  }

  /**
   * @return the pageVarName
   */
  public String getPageVarName() {
    return pageVarName;
  }

  /**
   * @param pageVarName the pageVarName to set
   */
  public void setPageVarName(String pageVarName) {
    this.pageVarName = pageVarName;
  }

  public Object intercept(Invocation invoke) throws Throwable {

    Object orginalTarget = invoke.getTarget();

    Object target = getTarget(orginalTarget);

    if ( target instanceof RoutingStatementHandler) {
      RoutingStatementHandler statementHandler = (RoutingStatementHandler) target;

      BaseStatementHandler delegate = (BaseStatementHandler) ReflectHelper
          .getValueByFieldName(statementHandler, "delegate");
      MappedStatement mappedStatement = (MappedStatement) ReflectHelper
          .getValueByFieldName(delegate, "mappedStatement");
      /** 预订审核复核分页查询的SQL Mapper */
      if (mappedStatement.getId().matches(pageMapper)) {

        BoundSql boundSql = delegate.getBoundSql();
        Object parameterObject = boundSql.getParameterObject();
        if (parameterObject == null) {
          throw new PaginationException("no page parameter");
        }
        //获取分页参数 参数本身为IPageParameter类型或 属性中带来此类型均可。
        IPageConverter pageConverter = null;
        IPageParameter page = null;
        if (parameterObject instanceof IPageParameter) {//本身是分页对象
          page = (IPageParameter) parameterObject;

        } else if (parameterObject instanceof Map) {//本身是map
          Map<String, Object> map = (Map<String, Object>) parameterObject;
          page = (IPageParameter) map.get(pageVarName);
          if (page == null) {
            page = new PageParameter();
          }

        } else if (pageConverterFactory != null) {//存在分页转换器
          pageConverter = pageConverterFactory.createPageConverter(parameterObject);
          if (pageConverter != null) {
            page = pageConverter.toPage(parameterObject);
          }

        }
        if (page == null) {
          //判断是否有分页属性
          Field pageField = ReflectHelper.getFieldByFieldName(
              parameterObject, pageVarName);
          if (pageField != null) {
            page = (IPageParameter) ReflectHelper.getValueByFieldName(
                parameterObject, pageVarName);
            if (page == null) {
              page = new PageParameter();
            }

            ReflectHelper.setValueByFieldName(parameterObject, pageVarName, page);
          } else {
            throw new PaginationException("no page parameter");
          }
        }

        String sql = boundSql.getSql();
        if (page.isRequireTotal()) {//设置总行数
          Connection connection = (Connection) invoke.getArgs()[0];
          //countSql: select count(0) from (" + sql + ") myCount ;
          String countSql = generateCountSql(sql);
          LOG.debug("COUNT SQL:[{}]", countSql);
          PreparedStatement countStmt = connection
              .prepareStatement(countSql);
          try {
            setParameters(countStmt, mappedStatement, boundSql,
                          parameterObject);
            ResultSet rs = countStmt.executeQuery();
            int count = 0;
            if (rs.next()) {
              count = rs.getInt(1);
            }
            page.setTotal(count);
            if (pageConverter != null) {
              pageConverter.returnTotal(parameterObject, count);
            }

            rs.close();

          } finally {
            countStmt.close();
          }
        }
        if (this.getDataBaseDialect() == null) {
          throw new PaginationException(
              "cannot get dataBaseDialect. please check the config of PaginationInterceptor, or SpringProxy shouldn't refer to java package:'com.youzan.platform.dao.mybatis3' ");
        }
        String
            pageSql =
            this.getDataBaseDialect().generatePageSql(sql, page.getStart(), page.getLimit());
        LOG.debug("PAGE SQL:[{}]", pageSql);
        ReflectHelper.setValueByFieldName(boundSql, "sql", pageSql);
      }
    }
    return invoke.proceed();
  }

  /**
   * 生成对应的统计条件的SQL
   */
  private String generateCountSql(String sql) {//要注意主select from之间的子查询
    String upperSql = sql.trim().toUpperCase();
    int startIndex = 0;
    int stack = 1;//select的栈，遇from则-1,
    for (int i = 0; i < 10; i++) {
      int indexs = upperSql.indexOf("SELECT ", startIndex + 2);
      int indexf = upperSql.indexOf("FROM ", startIndex + 2);
      if (indexs < 0 || indexf < indexs) {
        stack--;
        startIndex = indexf;
      } else {
        stack++;
        startIndex = indexs;
      }
      if (stack == 0) {
        return "select count(1)  " + sql.substring(indexf);//更简捷的计数sql
      }
    }
    //通用的计数sql，但可能给数据库带来额外压力。
    return "select count(1) from (" + sql + ") myCount";
  }

  private void setParameters(PreparedStatement ps,
                             MappedStatement mappedStatement, BoundSql boundSql,
                             Object parameterObject) throws SQLException {
    ErrorContext.instance().activity("setting parameters")
        .object(mappedStatement.getParameterMap().getId());
    List<ParameterMapping> parameterMappings = boundSql
        .getParameterMappings();
    if (parameterMappings != null) {
      Configuration configuration = mappedStatement.getConfiguration();
      TypeHandlerRegistry typeHandlerRegistry = configuration
          .getTypeHandlerRegistry();
      MetaObject metaObject = parameterObject == null ? null
                                                      : configuration
                                  .newMetaObject(parameterObject);
      for (int i = 0; i < parameterMappings.size(); i++) {
        ParameterMapping parameterMapping = parameterMappings.get(i);
        if (parameterMapping.getMode() != ParameterMode.OUT) {
          Object value;
          String propertyName = parameterMapping.getProperty();
          PropertyTokenizer prop = new PropertyTokenizer(propertyName);
          if (parameterObject == null) {
            value = null;
          } else if (typeHandlerRegistry
              .hasTypeHandler(parameterObject.getClass())) {
            value = parameterObject;
          } else if (boundSql.hasAdditionalParameter(propertyName)) {
            value = boundSql.getAdditionalParameter(propertyName);
          } else if (propertyName.startsWith(ForEachSqlNode.ITEM_PREFIX)
                     && boundSql.hasAdditionalParameter(prop.getName())) {
            value = boundSql.getAdditionalParameter(prop.getName());
            if (value != null) {
              value = configuration.newMetaObject(value)
                  .getValue(
                      propertyName.substring(prop
                                                 .getName().length()));
            }
          } else {
            value = metaObject == null ? null : metaObject
                .getValue(propertyName);
          }
          TypeHandler typeHandler = parameterMapping.getTypeHandler();
          if (typeHandler == null) {
            throw new ExecutorException(
                "There was no TypeHandler found for parameter "
                + propertyName + " of statement "
                + mappedStatement.getId());
          }
          typeHandler.setParameter(ps, i + 1, value,
                                   parameterMapping.getJdbcType());
        }
      }
    }
  }

  public Object plugin(Object arg0) {
    return Plugin.wrap(arg0, this);
  }

  public void setProperties(Properties p) {

  }

  private Object getTarget(Object input){
    Object target = input;
    MetaObject
        metaStatementHandler =
        MetaObject.forObject(input, SystemMetaObject.DEFAULT_OBJECT_FACTORY,
                             SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY,new DefaultReflectorFactory());
    /**
     * 分离代理对象链(由于目标类可能被多个拦截器拦截，从而形成多次代理，通过下面的两次循环
     可以分离出最原始的的目标类)
     */
    while (metaStatementHandler.hasGetter("h")){
      Object object = metaStatementHandler.getValue("h");
      metaStatementHandler =
          MetaObject.forObject(object, SystemMetaObject.DEFAULT_OBJECT_FACTORY,
                               SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY,new DefaultReflectorFactory());
    }

    /**
     * 分离最后一个代理目标类
     */
    while (metaStatementHandler.hasGetter("target")){
      target = metaStatementHandler.getValue("target");
      metaStatementHandler =
          MetaObject.forObject(target, SystemMetaObject.DEFAULT_OBJECT_FACTORY,
                               SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY,new DefaultReflectorFactory());
    }
    /**
     * 同一方法被多次代理
     */
    if (target instanceof Proxy){
     target = getTarget(target);
    }

//    /**
//     * 处理没有被代理的第一个拦截器
//     * 用 target = input代替
//     */
//    if (target == null) {
//      target = input;
//    }

    return target;
  }
}
