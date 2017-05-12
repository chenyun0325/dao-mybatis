package cn.gov.zcy.platform.dao.mybatis3.scanner;

import cn.gov.zcy.platform.dao.mybatis3.annotation.DataSource;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Created by chenyun on 16/1/11.
 */
public class MultiDaoMapperScanner extends ConfigurableClassPathMapperScanner {

  //添加multiDao的map映射
  private Map<String, String> factoryBeanNameMap;

  public MultiDaoMapperScanner(BeanDefinitionRegistry registry) {
    super(registry);
  }

  public void setFactoryBeanNameMap(Map<String, String> factoryBeanNameMap) {
    this.factoryBeanNameMap = factoryBeanNameMap;
  }

  @Override
  protected void processBeanDefinition(BeanDefinitionHolder beanDefinitionHolder) {
    super.processBeanDefinition(beanDefinitionHolder);
    try {
      //添加multiDao判断--by chenyun
      AbstractBeanDefinition beanDefinition =
          (AbstractBeanDefinition) beanDefinitionHolder.getBeanDefinition();

      String mapperInterface =
          beanDefinition.getPropertyValues().getPropertyValue("mapperInterface")
              .getValue().toString();
      Class<?> beanClass = Class.forName(mapperInterface);
      DataSource annotation = beanClass.getAnnotation(DataSource.class);
      if (annotation != null && factoryBeanNameMap != null) {
        String sqlSessionFactoryBeanName = factoryBeanNameMap.get(annotation.value());
        if (StringUtils.hasText(sqlSessionFactoryBeanName)) {
          beanDefinition.getPropertyValues().add("sqlSessionFactory",
                                                 new RuntimeBeanReference(
                                                     sqlSessionFactoryBeanName));
          if (logger.isDebugEnabled()) {
            logger.debug("Enabling autowire by type for MapperFactoryBean with name '"
                         + beanDefinitionHolder.getBeanName() + "'.");
          }
          beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        } else {
          throw new IllegalArgumentException(
              "no sqlSessionFactoryBean found for annotation " + annotation.value());
        }
      }
    } catch (ClassNotFoundException e) {
      logger.error("MultiDaoMapperScanner loadClass error:", e);
    }
  }
}
