package com.nova.config;

import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.support.jakarta.StatViewServlet;
import com.alibaba.druid.support.jakarta.WebStatFilter;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.SQLException;

/**
 * @Author huangpw
 * @Date 2026/1/6 15:46
 */
@Configuration
public class DruidConfig {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    /**
     * 注册 Druid 监控 Servlet
     */
    @Bean
    public ServletRegistrationBean<StatViewServlet> druidStatViewServlet() {
        ServletRegistrationBean<StatViewServlet> bean =
                new ServletRegistrationBean<>(new StatViewServlet(), "/druid/*");
        // 登录账号密码
        bean.addInitParameter("loginUsername", "admin");
        bean.addInitParameter("loginPassword", "admin123");
        // 禁止 HTML 页面的 "Reset All" 功能
        bean.addInitParameter("resetEnable", "false");
        // 允许所有访问（默认允许）
        bean.addInitParameter("allow", "");

        return bean;
    }

    /**
     * 注册 Druid WebStatFilter
     */
    @Bean
    public FilterRegistrationBean<Filter> druidWebStatFilter() {
        FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>(new WebStatFilter());
        bean.addUrlPatterns("/*");
        bean.addInitParameter(
                "exclusions",
                "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*"
        );
        // 可以开启 session 统计
        bean.addInitParameter("sessionStatEnable", "true");
        bean.addInitParameter("sessionStatMaxCount", "1000");

        return bean;
    }

    /**
     * 配置 Druid 数据源
     * 开启 SQL 监控、慢 SQL、WallFilter 防注入
     */
    @Bean
    public DruidDataSource druidDataSource() throws SQLException {
        DruidDataSource ds = new DruidDataSource();

        // TODO: 替换成你的数据库信息
        ds.setUrl(dbUrl);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName(driverClassName);

        // 最大连接数
        ds.setMaxActive(50);
        ds.setInitialSize(5);
        ds.setMinIdle(5);
        ds.setMaxWait(60000);

        // 打开监控统计功能
        ds.setFilters("stat,wall"); // stat 统计，wall 防 SQL 注入
        ds.setUseGlobalDataSourceStat(true);

        // 慢 SQL 记录（可选）
        // 配置慢 SQL
        StatFilter statFilter = new StatFilter();
        statFilter.setSlowSqlMillis(1000); // 超过 1 秒记录
        statFilter.setLogSlowSql(true);    // 打印日志
        statFilter.setMergeSql(true);       // 合并相同 SQL
        ds.getProxyFilters().add(statFilter);

        return ds;
    }
}
