package com.badals.flow.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class CookieFilter implements Filter {

   private final Logger log = LoggerFactory.getLogger(CookieFilter.class);

   @Override
   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

      HttpServletRequest req = (HttpServletRequest) request;
      Cookie[] cookies = req.getCookies();
      //log.info("---------------------COOOOOOOOOOKIIIIIIIIIIIIIIIIIIIIIIEEEEEEEEEEEEEEE");
      //log.info(((HttpServletRequest) request).getRequestURI());
      if (cookies != null) {
         for (Cookie ck : cookies) {
           /* if ("nameOfMyCookie".equals(ck.getName())) {
               // read the cookie etc, etc
               // ....
               // set an object in the current request
               request.setAttribute("myCoolObject", myObject)
            }*/
            //log.info(ck.getName() + " " + ck.getValue());
         }
      }
      HttpServletResponse res = (HttpServletResponse) response;
      Cookie c = new Cookie("_temp", "_temp");
      c.setPath("/");
      c.setMaxAge(-1);
      res.addCookie(c);
      chain.doFilter(request, response);

   }

   // other methods
}