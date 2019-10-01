package io.bdrc.ldspdi.test;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.Access;
import io.bdrc.auth.TokenValidation;
import io.bdrc.auth.UserProfile;
import io.bdrc.auth.model.Endpoint;
import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.ldspdi.service.ServiceConfig;

public class RdfAuthTestFilter implements Filter {

    public final static Logger log = LoggerFactory.getLogger(RdfAuthTestFilter.class.getName());

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        System.out.println("USE AUTH ? =" + ServiceConfig.useAuth());
        if (ServiceConfig.useAuth()) {
            HttpServletRequest req = (HttpServletRequest) request;
            boolean isSecuredEndpoint = true;
            request.setAttribute("access", new Access());
            String token = getToken(req.getHeader("Authorization"));
            TokenValidation validation = null;
            String path = req.getServletPath();
            System.out.println("PATH ? =" + path);
            Endpoint end;
            try {
                end = RdfAuthModel.getEndpoint(path);
                System.out.println("END ? =" + end);
            } catch (Exception e) {
                e.printStackTrace();
                end = null;
            }
            UserProfile prof = null;
            if (end == null) {
                isSecuredEndpoint = false;
                // endpoint is not secured - Using default (empty endpoint)
                // for Access Object end=new Endpoint();
            }
            if (token != null) {
                // User is logged on
                // Getting his profile
                System.out.println("TOKEN ? =" + token);
                validation = new TokenValidation(token);
                prof = validation.getUser();
            }
            System.out.println("SECURE ENDPOINT ? =" + isSecuredEndpoint);
            if (isSecuredEndpoint) {
                // Endpoint is secure
                if (validation == null) {
                    // no token --> access forbidden
                    return;
                } else {
                    Access access = new Access(prof, end);
                    // System.out.println("FILTER Access matchGroup >> "+access.matchGroup());
                    // System.out.println("FILTER Access matchRole >> "+access.matchRole());
                    // System.out.println("FILTER Access matchPerm >> "+access.matchPermissions());
                    if (!access.hasEndpointAccess()) {
                        return;
                    }
                    request.setAttribute("access", access);
                }
            } else {
                // end point not secured
                if (validation != null) {
                    // token present since validation is not null
                    Access acc = new Access(prof, end);
                    request.setAttribute("access", acc);
                }
            }
        }
        chain.doFilter(request, response);
    }

    String getToken(String header) {
        try {
            if (header != null) {
                return header.split(" ")[1];
            }
        } catch (Exception ex) {
            log.error(ex.getMessage());
            return null;
        }
        return null;
    }
}