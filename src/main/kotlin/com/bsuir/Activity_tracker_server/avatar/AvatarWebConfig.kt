package com.bsuir.Activity_tracker_server.avatar

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Paths

@Configuration
class AvatarWebConfig(
    private val props: AvatarProperties,
) : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val location = Paths.get(props.baseDir).toAbsolutePath().normalize().toUri().toString()
        val publicBase = props.publicBaseUrl.trimEnd('/')

        registry.addResourceHandler("$publicBase/**")
            .addResourceLocations(location)
    }
}

