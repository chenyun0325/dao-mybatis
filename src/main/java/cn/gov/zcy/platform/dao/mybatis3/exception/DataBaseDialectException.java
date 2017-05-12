package cn.gov.zcy.platform.dao.mybatis3.exception;

/**
 * @author: chenyun
 * @since: 2015年4月29日 下午3:26:53
 */
public class DataBaseDialectException extends MybatisWrapException {

  public DataBaseDialectException(String s) {
    super(s);

  }

  public DataBaseDialectException(Throwable root) {
    super(root);

  }

}
