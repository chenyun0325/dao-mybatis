package cn.gov.zcy.platform.dao.mybatis3.exception;

/**
 * @author: chenyun
 * @since: 2015年4月29日 下午3:29:07
 */
public class OptimisticLockException extends MybatisWrapException {

  public OptimisticLockException(String s) {
    super(s);

  }

}
