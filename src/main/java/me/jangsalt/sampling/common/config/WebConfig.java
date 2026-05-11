package me.jangsalt.sampling.common.config;

import lombok.RequiredArgsConstructor;
import me.jangsalt.sampling.common.component.mask.MaskingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final MaskingInterceptor maskingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(maskingInterceptor);
    }
}
