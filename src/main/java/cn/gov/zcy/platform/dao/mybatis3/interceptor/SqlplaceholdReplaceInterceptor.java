package cn.gov.zcy.platform.dao.mybatis3.interceptor;

import org.apache.ibatis.executor.statement.BaseStatementHandler;
import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Created by chenyun on 16/3/24.
 */
@Intercepts({
    @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class,Integer.class})})
public class SqlplaceholdReplaceInterceptor implements Interceptor {

  /**
   * 模式匹配处理类
   */
  private Map<String,ISqlplaceholdReplace> matchHandlers;

  private final static Logger LOG = LoggerFactory.getLogger(SqlplaceholdReplaceInterceptor.class);
  @Override
  public Object intercept(Invocation invoke) throws Throwable {

    Object orginalTarget = invoke.getTarget();

    Object target = getTarget(orginalTarget);

    if (target instanceof RoutingStatementHandler) {

      RoutingStatementHandler statementHandler = (RoutingStatementHandler) target;
      BaseStatementHandler delegate = (BaseStatementHandler) ReflectHelper
          .getValueByFieldName(statementHandler, "delegate");

      BoundSql boundSql = delegate.getBoundSql();

      String sql = boundSql.getSql();
      LOG.debug("sql before replace:[{}]", sql);

      /**
       * 批量替换处理
       */
      for (String matchKey : matchHandlers.keySet()) {
        Pattern pattern =  Pattern.compile(matchKey);
        if (pattern.matcher(sql).find()) {
          ISqlplaceholdReplace handler = matchHandlers.get(matchKey);
          String replacedSql = handler.getReplacedSql();
          sql = pattern.matcher(sql).replaceAll(replacedSql);
        }
      }
      LOG.debug("sql after replace:[{}]", sql);
      ReflectHelper.setValueByFieldName(boundSql, "sql", sql);

    }
    return invoke.proceed();
  }

  @Override
  public Object plugin(Object o) {
      return Plugin.wrap(o,this);
  }

  @Override
  public void setProperties(Properties properties) {

  }

  public void setMatchHandlers(
      Map<String, ISqlplaceholdReplace> matchHandlers) {
    this.matchHandlers = matchHandlers;
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
