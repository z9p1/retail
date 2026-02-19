package com.retail.util;

import javax.servlet.http.HttpServletRequest;

public final class WebUtil {

    public static Long getUserId(HttpServletRequest request) {
        Object v = request.getAttribute("userId");
        return v == null ? null : (Long) v;
    }

    public static String getRole(HttpServletRequest request) {
        Object v = request.getAttribute("role");
        return v == null ? null : (String) v;
    }
}
