package com.studydocs.manager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@ConfigurationProperties(prefix = "security.rate-limit")
public class RateLimitProperties {


    private int loginPerMinute = 10;


    private int registerPerMinute = 5;


    private int forgotPasswordPerMinute = 5;

    public int getLoginPerMinute() {
        return loginPerMinute;
    }

    public void setLoginPerMinute(int loginPerMinute) {
        this.loginPerMinute = loginPerMinute;
    }

    public int getRegisterPerMinute() {
        return registerPerMinute;
    }

    public void setRegisterPerMinute(int registerPerMinute) {
        this.registerPerMinute = registerPerMinute;
    }

    public int getForgotPasswordPerMinute() {
        return forgotPasswordPerMinute;
    }

    public void setForgotPasswordPerMinute(int forgotPasswordPerMinute) {
        this.forgotPasswordPerMinute = forgotPasswordPerMinute;
    }
}
