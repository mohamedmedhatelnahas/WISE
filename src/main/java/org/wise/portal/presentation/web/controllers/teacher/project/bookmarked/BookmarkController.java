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
package org.wise.portal.presentation.web.controllers.teacher.project.bookmarked;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.wise.portal.domain.project.Project;
import org.wise.portal.domain.user.User;
import org.wise.portal.presentation.web.controllers.ControllerUtil;
import org.wise.portal.service.project.ProjectService;

/**
 * Controller for (un)bookmarking projects.
 * 
 * @author patrick lawler
 * @version $Id:$
 */
@Controller
public class BookmarkController {

	@Autowired
	private ProjectService projectService;

	private final static String PROJECTID = "projectId";
	
	private final static String CHECKED = "checked";
	
	private final static String RESPONSE = "response";
	
	@RequestMapping("/teacher/projects/bookmark.html")
	protected ModelAndView handleRequestInternal(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		Boolean checked = Boolean.valueOf(request.getParameter(CHECKED));
		Project project = projectService.getById(Long.parseLong(request.getParameter(PROJECTID)));
		User user = ControllerUtil.getSignedInUser();
		String outResponse;
		
		if(checked){
			projectService.addBookmarkerToProject(project, user);
			outResponse = "Project " + project.getName() + " has been added to your bookmarked projects.";
		} else {
			projectService.removeBookmarkerFromProject(project, user);
			outResponse = "Project " + project.getName() + " has been removed from your bookmarked projects";
		}
		
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.addObject(RESPONSE, outResponse);
		return modelAndView;
	}
}
