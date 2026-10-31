package com.larkspur.testing;

import java.util.List;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.web.client.RestClient;

public final class DownstreamClientOverrides implements BeanDefinitionRegistryPostProcessor {
  public static final List<String> NAMES = List.of(
      "offersClient", "inventoryClient", "catalogRestClient", "inventoryRestClient");

  @Override
  public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
    for (String name : NAMES) {
      if (!registry.containsBeanDefinition(name)) {
        continue;
      }
      registry.removeBeanDefinition(name);
      RootBeanDefinition definition = new RootBeanDefinition(RestClient.class);
      definition.setTargetType(RestClient.class);
      definition.setScope(BeanDefinition.SCOPE_SINGLETON);
      definition.setInstanceSupplier(() -> DownstreamServers.bind(name));
      registry.registerBeanDefinition(name, definition);
    }
  }

  @Override
  public void postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory) {
  }
}
