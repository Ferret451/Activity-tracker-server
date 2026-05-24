package com.bsuir.Activity_tracker_server.demo

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(DemoDataProperties::class)
class DemoDataConfig
