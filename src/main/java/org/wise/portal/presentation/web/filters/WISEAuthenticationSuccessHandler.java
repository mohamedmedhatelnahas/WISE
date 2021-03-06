/**
 * Copyright (c) 2008-2014 Regents of the University of California (Regents). 
 * Created by WISE, Graduate School of Education, University of California, Berkeley.
 * 
 * This software is distributed under the GNU General Public License, v3,
 * or (at your option) any later version.
 * 
 * Permission is hereby granted, without written agreement and without license
 * or royalty fees, to use, copy, modify, and distribute this software and its
 * documentation for any purpose, provided that the above copyright notice and
 * the following two paragraphs appear in all copies of this software.
 * 
 * REGENTS SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE. THE SOFTWAREAND ACCOMPANYING DOCUMENTATION, IF ANY, PROVIDED
 * HEREUNDER IS PROVIDED "AS IS". REGENTS HAS NO OBLIGATION TO PROVIDE
 * MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 * 
 * IN NO EVENT SHALL REGENTS BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
 * SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
 * ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 * REGENTS HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.wise.portal.presentation.web.filters;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.wise.portal.dao.ObjectNotFoundException;
import org.wise.portal.domain.authentication.MutableUserDetails;
import org.wise.portal.domain.authentication.impl.StudentUserDetails;
import org.wise.portal.domain.authentication.impl.TeacherUserDetails;
import org.wise.portal.domain.portal.Portal;
import org.wise.portal.service.authentication.AuthorityNotFoundException;
import org.wise.portal.service.authentication.UserDetailsService;
import org.wise.portal.service.portal.PortalService;

/**
 * @author hirokiterashima
 * @version $Id:$
 */
public class WISEAuthenticationSuccessHandler extends
		SavedRequestAwareAuthenticationSuccessHandler {
	
	private UserDetailsService userDetailsService;
	
	@Autowired
	private PortalService portalService;
	
	@Override
	public void onAuthenticationSuccess(javax.servlet.http.HttpServletRequest request,
            javax.servlet.http.HttpServletResponse response,
            Authentication authentication)
     throws javax.servlet.ServletException,
            java.io.IOException {
		MutableUserDetails userDetails = (MutableUserDetails) authentication.getPrincipal();
        boolean userIsAdmin = false;
        if (userDetails instanceof StudentUserDetails) {        	
        	// pLT= previous login time (not this time, but last time)
        	Date lastLoginTime = ((StudentUserDetails) userDetails).getLastLoginTime();
        	long pLT = 0L; // previous last log in time
        	if (lastLoginTime != null) {
        		pLT = lastLoginTime.getTime();
        	}        
        	this.setDefaultTargetUrl(WISEAuthenticationProcessingFilter.STUDENT_DEFAULT_TARGET_PATH + "?pLT=" + pLT); 			        			
        }
        else if (userDetails instanceof TeacherUserDetails) {
	   		this.setDefaultTargetUrl(WISEAuthenticationProcessingFilter.TEACHER_DEFAULT_TARGET_PATH);

        	GrantedAuthority researcherAuth = null;
        	try {
        		researcherAuth = userDetailsService.loadAuthorityByName(UserDetailsService.RESEARCHER_ROLE);
			} catch (AuthorityNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
			for (GrantedAuthority authority : authorities) {
        		if (researcherAuth.equals(authority)) {
        			this.setDefaultTargetUrl(WISEAuthenticationProcessingFilter.RESEARCHER_DEFAULT_TARGET_PATH);
        		}
			}
	   		
        	GrantedAuthority adminAuth = null;
        	try {
				adminAuth = userDetailsService.loadAuthorityByName(UserDetailsService.ADMIN_ROLE);
			} catch (AuthorityNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			for (GrantedAuthority authority : authorities) {
        		if (adminAuth.equals(authority)) {
        			this.setDefaultTargetUrl(WISEAuthenticationProcessingFilter.ADMIN_DEFAULT_TARGET_PATH);
        			userIsAdmin = true;
        		}
			}
        }
        
        // if user is not admin and login is disallowed, log out user and redirect them to the "we are undergoing maintenance" page
        try {
			Portal portal = portalService.getById(1);
			if (!userIsAdmin && !portal.isLoginAllowed()) {
				 Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		          if (auth != null){    
		             new SecurityContextLogoutHandler().logout(request, response, auth);
		          }
		        SecurityContextHolder.getContext().setAuthentication(null);
				
				String contextPath = request.getContextPath();
				response.sendRedirect(contextPath + WISEAuthenticationProcessingFilter.LOGIN_DISABLED_MESSGE_PAGE);
		        return;
		    }
        } catch (ObjectNotFoundException e) {
			// do nothing
		} catch (IOException ioe) {
			// do nothing
		}
        
        // set user's language (if specified)
        Locale locale = null;
        String userLanguage = userDetails.getLanguage();
        if (userLanguage != null) {
        	if (userLanguage.contains("_")) {
        		String language = userLanguage.substring(0, userLanguage.indexOf("_"));
        		String country = userLanguage.substring(userLanguage.indexOf("_")+1);
            	locale = new Locale(language, country); 	
        	} else {
        		locale = new Locale(userLanguage);
        	}
        } else {
        	// user default browser locale setting if user hasn't specified locale
        	locale = request.getLocale();
        }
        request.getSession().setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, locale);
      
        /* redirect if specified in the login request */
        SavedRequest savedRequest = 
        	    new HttpSessionRequestCache().getRequest(request, response);
        if (savedRequest != null) {
        	String redirectUrl = savedRequest.getRedirectUrl();
        	if(StringUtils.hasText(redirectUrl)){
        		this.setDefaultTargetUrl(redirectUrl);
        	}
        }
   		
		((MutableUserDetails) userDetails).incrementNumberOfLogins();
		((MutableUserDetails) userDetails).setLastLoginTime(Calendar.getInstance().getTime());
		((MutableUserDetails) userDetails).setNumberOfRecentFailedLoginAttempts(0);
		userDetailsService.updateUserDetails((MutableUserDetails) userDetails);
		super.handle(request, response, authentication);
	}
	
	/**
	 * @return the userDetailsService
	 */
	public UserDetailsService getUserDetailsService() {
		return userDetailsService;
	}

	/**
	 * @param userDetailsService the userDetailsService to set
	 */
	public void setUserDetailsService(UserDetailsService userDetailsService) {
		this.userDetailsService = userDetailsService;
	}
}
