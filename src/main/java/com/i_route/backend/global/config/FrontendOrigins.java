package com.i_route.backend.global.config;

import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Locale;

final class FrontendOrigins {
    private final String additionalOrigin;

    FrontendOrigins(@Value("${app.frontend-additional-origin:}") String configuredOrigin) {
        String origin = configuredOrigin.strip();
        additionalOrigin = origin.isEmpty() ? "" : validate(origin);
    }

    String[] includeIn(String... existingOrigins) {
        if (additionalOrigin.isEmpty()) {
            return existingOrigins;
        }
        String[] origins = Arrays.copyOf(existingOrigins, existingOrigins.length + 1);
        origins[existingOrigins.length] = additionalOrigin;
        return origins;
    }

    private static String validate(String origin) {
        try {
            URI uri = new URI(origin);
            if ("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && uri.getRawPath().isEmpty() && uri.getRawQuery() == null && uri.getRawFragment() == null) {
                int port = uri.getPort();
                String authority = uri.getHost() + (port == -1 ? "" : ":" + port);
                if (port != 0 && port <= 65535 && authority.equals(uri.getRawAuthority())) {
                    String canonicalOrigin = "https://" + uri.getHost().toLowerCase(Locale.ROOT);
                    return port == -1 || port == 443 ? canonicalOrigin : canonicalOrigin + ":" + port;
                }
            }
        } catch (URISyntaxException ignored) {
            // Report the required shape without echoing the configured value.
        }
        throw new IllegalArgumentException("FRONTEND_ADDITIONAL_ORIGIN must be a single HTTPS origin without a path");
    }
}
