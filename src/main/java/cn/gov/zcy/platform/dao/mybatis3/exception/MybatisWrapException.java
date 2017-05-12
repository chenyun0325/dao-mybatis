package cn.gov.zcy.platform.dao.mybatis3.exception;


import cn.gov.zcy.platform.dao.base.exception.DAOComponentException;

/**
 * mybatis包装器异常，mybatis扩展功能的异常均继承自此异常类
 *
 * @author: chenyun
 * @since: 2015年4月29日 下午3:28:01
 */
public class MybatisWrapException extends DAOComponentException {

  public MybatisWrapException(String s) {
    super(s);
  }

  public MybatisWrapException(String string, Throwable root) {
    super(string, root);
  }

  public MybatisWrapException(Throwable root) {
    super(root);
  }


}
