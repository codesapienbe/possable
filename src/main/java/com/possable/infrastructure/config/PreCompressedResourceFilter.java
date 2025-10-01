package com.possable.infrastructure.config;

import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class PreCompressedResourceFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PreCompressedResourceFilter.class);

    @Autowired
    private ResourceLoader resourceLoader;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String uri = request.getRequestURI();
        // Only intercept static VAADIN build assets
        if (!(uri.startsWith("/VAADIN/") || uri.endsWith(".js") || uri.endsWith(".css"))) {
            filterChain.doFilter(request, response);
            return;
        }

        String accept = request.getHeader("Accept-Encoding");
        if (accept == null || !(accept.contains("br") || accept.contains("gzip"))) {
            filterChain.doFilter(request, response);
            return;
        }

        // prefer brotli
        if (accept.contains("br")) {
            Resource br = findPrefixedResource(uri + ".br");
            if (br != null && br.exists()) {
                serveCompressedResource(br, "br", uri, response);
                return;
            }
        }

        // fallback to gzip
        if (accept.contains("gzip")) {
            Resource gz = findPrefixedResource(uri + ".gz");
            if (gz != null && gz.exists()) {
                serveCompressedResource(gz, "gzip", uri, response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Resource findPrefixedResource(String pathWithExt) {
        try {
            // packaged Vaadin build is under classpath:/META-INF/VAADIN/webapp
            String classpathLocation = "classpath:META-INF/VAADIN/webapp" + pathWithExt;
            Resource r = resourceLoader.getResource(classpathLocation);
            if (r.exists()) return r;
        } catch (Exception e) {
            log.debug("Failed to find resource {}", pathWithExt, e);
        }
        return null;
    }

    private void serveCompressedResource(Resource resource, String encoding, String originalUri, HttpServletResponse response) throws IOException {
        response.setHeader("Content-Encoding", encoding);
        String contentType = getServletContext().getMimeType(originalUri);
        if (contentType != null) response.setContentType(contentType);
        // it's safe to set caching headers (let browser cache) - reuse whatever Vaadin/Gzip bundling expects
        response.setHeader("Cache-Control", "max-age=31536000, public");
        try (InputStream in = resource.getInputStream(); OutputStream out = response.getOutputStream()) {
            in.transferTo(out);
            out.flush();
        }
    }
} 