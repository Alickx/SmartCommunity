package cn.goroute.smart.gateway.config.satoken;

import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.goroute.smart.common.utils.Result;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: Alickx
 * @Date: 2022/03/28/17:16
 * @Description: 鉴权
 */
@Configuration
public class SaTokenConfigure {
    @Bean
    public SaReactorFilter getSaReactorFilter() {
        return new SaReactorFilter()
                .setError(e -> Result.error())
                ;
    }
}

