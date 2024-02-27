package org.esupportail.esupsgcclient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:esupsgcclient-version.properties")
public class AppVersion {

    @Value("${version}")
    String version;

    @Value("${build.date}")
    String buildDate;


    public String getVersion() {
        return version;
    }

    public String setVersion(String version) {
        return this.version = version;
    }

    public String getBuildDate() {
        return buildDate;
    }

    public String setBuildDate(String buildDate) {
        return this.buildDate = buildDate;
    }

}
