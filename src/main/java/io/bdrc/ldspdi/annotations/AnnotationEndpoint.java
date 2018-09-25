package io.bdrc.ldspdi.annotations;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.formatters.JSONLDFormatter.DocType;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.Helpers;
import io.bdrc.ldspdi.utils.MediaTypeUtils;
import io.bdrc.ldspdi.utils.ResponseOutputStream;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;

@Path("/annotation/")
public class AnnotationEndpoint {

    public final static Logger log = LoggerFactory.getLogger(AnnotationEndpoint.class.getName());
    public String fusekiUrl = ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);

    public final static String ANN_PROFILE = "http://www.w3.org/ns/anno.jsonld";
    public final static String ANN_SERVICE = "http://www.w3.org/ns/oa#annotationService";
    public final static String ANN_PROTOCOL = "http://www.w3.org/TR/annotation-protocol/";
    public final static String LDP_RES = "http://www.w3.org/ns/ldp#Resource";
    public final static String ANN_TYPE = "http://www.w3.org/ns/oa#Annotation";
    public final static String LDP_BC = "http://www.w3.org/ns/ldp#BasicContainer";
    public final static String LDP_CB = "http://www.w3.org/ns/ldp#constrainedBy";
    public final static String LDP_PMC = "http://www.w3.org/ns/ldp#PreferMinimalContainer";
    public final static String OA_PCI = "http://www.w3.org/ns/oa#PreferContainedIRIs";
    public final static String OA_PCD = "http://www.w3.org/ns/oa#PreferContainedDescriptions";
    public final static String OA_CONTEXT = "https://www.w3.org/ns/oa.jsonld";

    public final static String OA_CT = "application/ld+json; profile=\"http://www.w3.org/ns/oa.jsonld\"";
    public final static String W3C_CT = "application/ld+json; profile=\"http://www.w3.org/ns/anno.jsonld\"";

    public final static int W3C_ANN_MODE = 0;
    public final static int OA_ANN_MODE = 1;
    public final static int DEFAULT_ANN_MODE = W3C_ANN_MODE;

    public static final String ANN_PREFIX_SHORT = "bdan";
    public static final String ANC_PREFIX_SHORT = "bdac";
    public static final String ANN_PREFIX = "http://purl.bdrc.io/annotation/";
    public static final String ANC_PREFIX = "http://purl.bdrc.io/anncollection/";

    @GET
    @Path("/{res}")
    public Response getResourceGraph(@PathParam("res") String res,
            @HeaderParam("Accept") String accept,
            @Context UriInfo info,
            @Context Request request,
            @Context HttpHeaders headers) throws RestException {
        log.info("Call to getResourceGraph() with URL: {}, accept: {}", info.getPath(), accept);
        MediaType mediaType;
        // spec says that when the Accept: header is absent, JSON-LD should be answered
        if (accept == null) {
            mediaType = MediaTypeUtils.MT_JSONLD;
        } else {
            mediaType = MediaTypeUtils.getMediaType(request, accept, MediaTypeUtils.annVariants);
            if (mediaType == null)
                return AnnotationEndpoint.mediaTypeChoiceResponse(info);
        }
        String prefixedRes = ANN_PREFIX_SHORT+':'+res;
        if (mediaType.equals(MediaType.TEXT_HTML_TYPE))
            return htmlResponse(info, prefixedRes);
        int mode = DEFAULT_ANN_MODE;
        DocType docType = DocType.ANN;
        String ext = null;
        if (mediaType.getSubtype().equals("ld+json")) {
            ext = "jsonld";
            mode = getJsonLdMode(mediaType);
            if (mode == OA_ANN_MODE) {
                mediaType = MediaTypeUtils.MT_JSONLD_OA;
                docType = DocType.OA;
            } else {
                mediaType = MediaTypeUtils.MT_JSONLD_WA;
            }
        } else {
            ext = MediaTypeUtils.getExtFromMime(mediaType);
        }
        Model model = QueryProcessor.getSimpleResourceGraph(prefixedRes, "AnnGraph.arq", fusekiUrl, null);
        if (model.size() == 0)
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes));
        ResponseBuilder builder = Response.ok(ResponseOutputStream.getModelStream(model, ext, ANN_PREFIX+res, docType));
        return setHeaders(builder, info.getPath(), ext, "Choice", null, mediaType, false).build();
    }

    @GET
    @Path("/{res}.{ext}")
    // these are always W3C web annotations, maybe there could be another endpoint for OA
    public Response getResourceGraphSuffix(@PathParam("res") String res,
            @PathParam("ext") final String ext,
            @Context UriInfo info,
            @Context Request request,
            @Context HttpHeaders headers) throws RestException {
        log.info("Call to getResourceGraphSuffix() with URL: {}", info.getPath());
        MediaType mediaType = MediaTypeUtils.getMimeFromExtension(ext);
        if (mediaType == null) {
            return AnnotationEndpoint.mediaTypeChoiceResponse(info);
        }
        if ("jsonld".equals(ext)) {
            mediaType = MediaTypeUtils.MT_JSONLD_WA;
        }
        String prefixedRes = ANN_PREFIX_SHORT+':'+res;
        Model model = QueryProcessor.getSimpleResourceGraph(prefixedRes, "AnnGraph.arq", fusekiUrl, null);
        if (model.size() == 0)
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes));
        ResponseBuilder builder = Response.ok(ResponseOutputStream.getModelStream(model, ext, ANN_PREFIX+res, DocType.ANN));
        return setHeaders(builder, info.getPath(), ext, "Choice", null, mediaType, false).build();
    }

    static int getJsonLdMode(final MediaType mediaType) {
        String profile = mediaType.getParameters().get("profile");
        if (profile.equals(OA_CONTEXT))
            return OA_ANN_MODE;
        return W3C_ANN_MODE;
    }

    public static Response mediaTypeChoiceResponse(final UriInfo info) throws RestException {
        final String html = Helpers.getMultiChoicesHtml(info.getPath(), true);
        final ResponseBuilder rb = Response.status(300).entity(html)
                .header("Content-Location", info.getBaseUri() + "choice?path=" + info.getPath());
        return setHeaders(rb, info.getPath(), null, "List", null, MediaType.TEXT_HTML_TYPE, false).build();
    }

    public static Response htmlResponse(final UriInfo info, final String res) throws RestException {
        try {
            ResponseBuilder builder = Response.seeOther(new URI(ServiceConfig.getProperty("showUrl") + res));
            return setHeaders(builder, info.getPath(), null, "Choice", null, null, false).build();
        } catch (URISyntaxException e) {
            throw new RestException(500, new LdsError(LdsError.URI_SYNTAX_ERR).setContext("getResourceGraphGet()", e));
        }
    }

    static ResponseBuilder setHeaders(ResponseBuilder builder, String url, final String ext, final String tcn,
            final String profile, final MediaType mediaType, final boolean collection) {
        final Map<String, MediaType> map = MediaTypeUtils.getResExtensionMimeMap();
        if (collection) {
            builder.header("Link", "<http://www.w3.org/ns/ldp#BasicContainer>; rel=\"type\"");
            builder.header("Link", "<http://www.w3.org/TR/annotation-protocol/>; rel=\"http://www.w3.org/ns/ldp#constrainedBy\"");
        } else {
            builder.header("Link", "<http://www.w3.org/ns/ldp#Resource>; rel=\"type\"");
            // not mandatory in the spec:
            builder.header("Link", "<http://www.w3.org/ns/oa#Annotation>; rel=\"type\"");
        }
        // TODO: spec mandates the ETag header...
        builder.header("Allow", "GET, OPTIONS, HEAD");
        if (ext != null) {
            final int dotidx = url.lastIndexOf('.');
            if (dotidx < 0) {
                builder.header("Content-Location", url + "." + ext);
            } else {
                url = url.substring(0, dotidx);
            }
        }
        final StringBuilder sb = new StringBuilder("");
        boolean first = true;
        for (Entry<String, MediaType> e : map.entrySet()) {
            if (!e.getKey().equals(ext)) {
                if (!first)
                    sb.append(",");
                sb.append("{\"" + url + "." + e.getKey() + "\" 1.000 {type " + e.getValue().toString() + "}}");
                first = false;
            }
        }
        if (mediaType != null)
            builder.header("Content-Type", mediaType);
        builder.header("Alternates", sb.toString());
        if (tcn != null)
            builder.header("TCN", tcn);
        builder.header("Vary", "Negotiate, Accept");
        return builder;
    }

}
