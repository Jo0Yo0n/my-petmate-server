package io.github.jo0yo0n.mypetmate.config;

import io.github.jo0yo0n.mypetmate.web.RequestIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class RequestIdConfig {

  @Bean
  FilterRegistrationBean<RequestIdFilter> requestIdFilterRegistration() {

    FilterRegistrationBean<RequestIdFilter> filterRegistrationBean =
        new FilterRegistrationBean<>(new RequestIdFilter());

    filterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);

    return filterRegistrationBean;
  }
}
