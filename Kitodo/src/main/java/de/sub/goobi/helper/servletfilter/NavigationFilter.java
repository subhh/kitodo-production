/*
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General Public License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * GPL3-License.txt file that was distributed with this source code.
 */

package de.sub.goobi.helper.servletfilter;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.springframework.web.filter.GenericFilterBean;

@Named("NavigationFilter")
@SessionScoped
public class NavigationFilter extends GenericFilterBean implements Serializable {

    private static final String DEFAULT_LINK = "/pages/desktop.jsf";

    private static String backlink = DEFAULT_LINK;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;

        String referer = httpServletRequest.getHeader("referer");
        String requestURI = httpServletRequest.getRequestURI();
        if (isNavigationRequest(referer) && isNavigationRequest(requestURI) && !referer.contains(requestURI)) {
            // since we have two nested edit forms (for processes & tasks) we have to consider this special case
            // separately: going to or returning from taskEdit should _not_ update the backlink!
            if ( !referer.contains("taskEdit") && !requestURI.contains("taskEdit") ) {
                // ignore page reloads
                if (!referer.contains(requestURI)) {
                    backlink = referer.replace(getBaseURLWithContextPath(httpServletRequest), "");
                }
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isNavigationRequest(String url) {
        return Objects.nonNull(url)
                && url.contains(".jsf")
                && !(url.contains(".js.jsf") || url.contains(".css.jsf") || url.contains(".svg.jsf"));
    }

    private static String getBaseURLWithContextPath(HttpServletRequest request) {
        return request.getScheme() + "://"
                + request.getServerName() + ":"
                + request.getServerPort()
                + request.getContextPath();
    }

    /**
     * Return referer path of given HttpServletRequest 'request' without base URL and context path,
     * so that it can be used as a navigation outcome.
     *
     * @param request HttpServletRequest whose referer path is returned
     * @return referer path of given HttpServletRequest
     */
    public static String getLocalRefererPath(HttpServletRequest request) {
        String referer = request.getHeader("referer");
        if (!referer.isEmpty()) {
            return referer.replace(getBaseURLWithContextPath(request), "");
        }
        return DEFAULT_LINK;
    }

    /**
     * Return back link to last visited view in Kitodo. If variable 'backlink' is empty, return DEFAULT_LINK
     * to desktop view instead.
     *
     * @return back link to last visited view
     */
    public static String getBacklink() {
        if (backlink.isEmpty()) {
            return DEFAULT_LINK;
        } else {
            return backlink;
        }
    }
}
