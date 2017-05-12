package cn.gov.zcy.platform.dao.mybatis3.scanner;

import org.mybatis.spring.mapper.ClassPathMapperScanner;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import java.util.Arrays;
import java.util.Set;

class ConfigurableClassPathMapperScanner extends ClassPathMapperScanner {

    ConfigurableClassPathMapperScanner(BeanDefinitionRegistry registry) {
        super(registry);
    }

    @Override
    public Set<BeanDefinitionHolder> doScan(String... basePackages) {
        Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);
        if (beanDefinitions.isEmpty()) {
            logger.warn("No MyBatis mapper was found in '" + Arrays.toString(basePackages) + "' package. Please check your configuration.");
            return beanDefinitions;
        }

        for (BeanDefinitionHolder beanDefinition : beanDefinitions) {
            processBeanDefinition(beanDefinition);
        }
        //beanDefinitions.stream().forEach(this::processBeanDefinition);
        return beanDefinitions;
    }

    protected void processBeanDefinition(BeanDefinitionHolder beanDefinitionHolder) {}
}
