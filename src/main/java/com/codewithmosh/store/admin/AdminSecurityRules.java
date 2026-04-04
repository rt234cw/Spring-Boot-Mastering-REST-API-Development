package com.codewithmosh.store.admin;

import com.codewithmosh.store.common.SecurityRules;
import com.codewithmosh.store.users.Role;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;


@Component
public class AdminSecurityRules implements SecurityRules {
    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        //只有符合特定權限身分的人才能讀取 （Role是我們自己寫的enum class）
                        registry.requestMatchers("/admin/**").hasRole(Role.ADMIN.name());

    }
}
