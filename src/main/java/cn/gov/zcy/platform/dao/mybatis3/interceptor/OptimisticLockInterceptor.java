package cn.gov.zcy.platform.dao.mybatis3.interceptor;

import cn.gov.zcy.platform.dao.mybatis3.exception.OptimisticLockException;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Statement;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * 乐观锁
 *
 * @author: chenyun
 * @since: 2015年4月29日 下午4:07:33
 */
@Intercepts({
    @Signature(type = StatementHandler.class, method = "update", args = {Statement.class})})
public class OptimisticLockInterceptor implements Interceptor {

  private final static Logger LOG = LoggerFactory.getLogger(OptimisticLockInterceptor.class);
  private String fieldName = "version";//版本控制的字段名

  public Object intercept(Invocation invoke) throws Throwable {
    Object rsObj = invoke.proceed();
    if (invoke.getTarget() instanceof StatementHandler) {
      StatementHandler statementHandler = (StatementHandler) invoke.getTarget();
      String sql = statementHandler.getBoundSql().getSql().toLowerCase();
      String lowerFieldName = fieldName.toLowerCase();
      //正规表达式需要匹配： u.version = u.version + 1 或 version=version+1
      Pattern pattern = Pattern.compile("\\W" + lowerFieldName
                                        + "\\s{0,}=\\s{0,}\\w*\\.?"
                                        + lowerFieldName + "\\s{0,}\\+\\s{0,}1");
      if (pattern.matcher(sql).find()) {//需要验证乐观锁
        LOG.debug("Optimistic Locking checking");
        Number row = (Number) rsObj;
        if (row.intValue() < 1) {//修改记录为0，表示产生了冲突
          throw new OptimisticLockException("Optimistic Locking conflict");

        }
        LOG.debug("Optimistic Locking Passed .");
      }
    }
    return rsObj;

  }

  public Object plugin(Object arg0) {
    return Plugin.wrap(arg0, this);
  }

  public void setProperties(Properties p) {

  }

  /**
   * @param fieldName the fieldName to set
   */
  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }


}
