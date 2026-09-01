package gt.muni.quejas.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfiguration {

    @Bean
    public CacheManager cacheManager(@Value("${app.cache.catalogos-ttl-minutos}") long ttlMinutos) {
        CaffeineCacheManager manager = new CaffeineCacheManager("catalogos", "ciudadanos");
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(ttlMinutos, TimeUnit.MINUTES)
                .maximumSize(2000));
        return manager;
    }
}
