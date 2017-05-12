package cn.gov.zcy.platform.dao.mybatis3.exception;

/**
 * @author: chenyun
 * @since: 2015年4月29日 下午3:29:01
 */
public class PaginationException extends MybatisWrapException {

  public PaginationException(String s) {
    super(s);

  }

  public PaginationException(Throwable root) {
    super(root);

  }

}
