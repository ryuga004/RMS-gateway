package com.rms.gateway.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;

/**
 * Wraps an HttpServletRequest and allows adding/overriding headers
 * so the downstream chain sees them (e.g. Authorization: Bearer &lt;token&gt;).
 */
public class HeaderMutableHttpServletRequest extends HttpServletRequestWrapper {

    private final Map<String, String> addedHeaders = new HashMap<>();

    public HeaderMutableHttpServletRequest(HttpServletRequest request) {
        super(Objects.requireNonNull(request, "request"));
    }

    public void addHeader(String name, String value) {
        if (name != null && value != null) {
            addedHeaders.put(name, value);
        }
    }

    @Override
    public String getHeader(String name) {
        String added = addedHeaders.get(name);
        if (added != null) {
            return added;
        }
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        String added = addedHeaders.get(name);
        if (added != null) {
            return Collections.enumeration(Collections.singletonList(added));
        }
        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Vector<String> names = new Vector<>(Collections.list(super.getHeaderNames()));
        for (String name : addedHeaders.keySet()) {
            if (!names.contains(name)) {
                names.add(name);
            }
        }
        return names.elements();
    }
}
