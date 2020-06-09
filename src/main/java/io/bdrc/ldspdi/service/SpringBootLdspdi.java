package io.bdrc.ldspdi.service;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.ldspdi.exceptions.LdsError;
import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.ontology.service.core.OntData;
import io.bdrc.ldspdi.ontology.service.shapes.OntShapesData;
import io.bdrc.ldspdi.results.ResultsCache;
import io.bdrc.taxonomy.TaxModel;

@SpringBootApplication
@Configuration
@EnableAutoConfiguration
@Primary
@ComponentScan(basePackages = { "io.bdrc.ldspdi" })

public class SpringBootLdspdi extends SpringBootServletInitializer {

    // public final static Logger log =
    // LoggerFactory.getLogger(SpringBootLdspdi.class);
    public final static Logger log = LoggerFactory.getLogger("default");

    public static void main(String[] args) throws JsonParseException, JsonMappingException, RestException {
        final String configPath = System.getProperty("ldspdi.configpath");
        try {
            ServiceConfig.init();
        } catch (IOException e1) {
            log.error("Primary config could not be load in ServiceConfig", e1);
            throw new RestException(500, new LdsError(LdsError.MISSING_RES_ERR).setContext("Ldspdi startup and initialization", e1));
        }
        if (ServiceConfig.useAuth() && !ServiceConfig.isInChina()) {
            AuthProps.init(ServiceConfig.getProperties());
            RdfAuthModel.readAuthModel();
        }
        ResultsCache.init();
        // Pull lds-queries repo from git if not in china
        if (!ServiceConfig.isInChina()) {
            GitService.update();
        }
        TaxModel.fetchModel();
        log.info("SpringBootLdspdi has been properly initialized");
        SpringApplication.run(SpringBootLdspdi.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void doSomethingAfterStartup() {
        OntData.init();
        if (!ServiceConfig.isInChina()) {
            OntShapesData.init();
            if ("true".equals(AuthProps.getProperty("useAuth"))) {
                log.info("SpringBootLdspdi uses auth, updating auth data...");
                RdfAuthModel.init();
                RdfAuthModel.updateAuthData(AuthProps.getProperty("fusekiUrl"));
            }
        }
        log.info("SERVER IS IN CHINA {}", ServiceConfig.isInChina());
    }

}
