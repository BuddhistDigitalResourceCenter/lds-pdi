package io.bdrc.ldspdi.test.annotations;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.apache.jena.fuseki.embedded.FusekiServer;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.bdrc.ldspdi.annotations.AnnotationEndpoint;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.test.Utils;
import io.bdrc.restapi.exceptions.RestExceptionMapper;

public class AnnotationTest extends JerseyTest {

    private static FusekiServer server;
    private static Dataset srvds = DatasetFactory.createTxnMem();
    private static Model model = ModelFactory.createDefaultModel();
    public static String fusekiUrl;
    public final static Logger log = LoggerFactory.getLogger(AnnotationTest.class.getName());
    public final static String JsonLdCTWithAnnoProfile = "application/ld+json; profile=\"http://www.w3.org/ns/anno.jsonld\"";
    public final static String JsonLdCTWithOaProfile = "application/ld+json; profile=\"http://www.w3.org/ns/oa.jsonld\"";

    @BeforeClass
    public static void init() {
        fusekiUrl = "http://localhost:2248/bdrcrw";
        ServiceConfig.initForTests(fusekiUrl);
        Utils.loadDataInModel(model);
        srvds.setDefaultModel(model);
        // Creating a fuseki server
        server = FusekiServer.create().setPort(2248).add("/bdrcrw", srvds).build();
        server.start();
    }

    @AfterClass
    public static void close() {
        server.stop();
        server.join();
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(AnnotationEndpoint.class).register(RestExceptionMapper.class);
    }

    @Test
    public void basicContentType() throws JsonProcessingException, IOException {
        final Response res = target("/annotation/AN123")
                .request()
                .header("Accept", JsonLdCTWithAnnoProfile)
                .get();
        System.out.println(res.readEntity(String.class));
        assertTrue(res.getStatus() == 200);
        assertTrue(res.getHeaderString("Content-Type").equals(JsonLdCTWithAnnoProfile));
    }

}
