package com.example.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;

import java.time.Duration;

@Configuration
public class SoapConfig {

    @Value("${soap.service.url:http://10.29.60.95:8080/easws/sharedservice/AuthorizationSharedService}")
    private String serviceUrl;

    @Value("${soap.connection.timeout:30000}")
    private int connectionTimeout;

    @Value("${soap.read.timeout:60000}")
    private int readTimeout;

    @Bean
    public WebServiceTemplate webServiceTemplate() {
        WebServiceTemplate template = new WebServiceTemplate();
        template.setDefaultUri(serviceUrl);
        
        HttpUrlConnectionMessageSender messageSender = new HttpUrlConnectionMessageSender();
        messageSender.setConnectionTimeout(Duration.ofMillis(connectionTimeout));
        messageSender.setReadTimeout(Duration.ofMillis(readTimeout));
        
        template.setMessageSender(messageSender);
        
        return template;
    }
}