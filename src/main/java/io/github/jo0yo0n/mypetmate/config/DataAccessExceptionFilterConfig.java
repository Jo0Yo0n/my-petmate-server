package io.github.jo0yo0n.mypetmate.config;

import io.github.jo0yo0n.mypetmate.web.DataAccessExceptionFilter;
import io.github.jo0yo0n.mypetmate.web.error.SecurityProblemDetailWriter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class DataAccessExceptionFilterConfig {

  private final SecurityProblemDetailWriter writer;

  public DataAccessExceptionFilterConfig(SecurityProblemDetailWriter writer) {
    this.writer = writer;
  }

  @Bean
  public FilterRegistrationBean<DataAccessExceptionFilter> filterRegistrationBean() {

    FilterRegistrationBean<DataAccessExceptionFilter> filterRegistrationBean =
        new FilterRegistrationBean<>(new DataAccessExceptionFilter(writer));

    filterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);

    return filterRegistrationBean;
  }
}
