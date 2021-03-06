/**
 * Copyright (c) 2007-2014 Regents of the University of California (Regents). 
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
package org.wise.portal.presentation.web.controllers.teacher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.wise.portal.domain.run.Run;
import org.wise.portal.domain.user.User;
import org.wise.portal.domain.workgroup.Workgroup;
import org.wise.portal.presentation.web.controllers.ControllerUtil;
import org.wise.portal.service.newsitem.NewsItemService;
import org.wise.portal.service.offering.RunService;
import org.wise.portal.service.workgroup.WorkgroupService;

/**
 * Controller for WISE Teacher index page
 *
 * @author Hiroki Terashima
 * @version $Id$
 */
@Controller
@RequestMapping("/teacher/index.html")
public class TeacherIndexController {

	@Autowired
	private Properties wiseProperties;

	@Autowired
	private RunService runService;

	@Autowired
	private WorkgroupService workgroupService;
	
	@Autowired
	private NewsItemService newsItemService;

	static final Comparator<Run> CREATED_ORDER =
	new Comparator<Run>() {
		public int compare(Run o1, Run o2) {
			return o2.getStarttime().compareTo(o1.getStarttime());
        }
	};
	
	@RequestMapping(method=RequestMethod.GET)
	protected String handleGET(
			HttpServletRequest request,
			HttpServletResponse response,
			ModelMap modelMap) throws Exception {
		
		boolean isRealTimeEnabled = false;
		
	    String isRealTimeEnabledStr = this.wiseProperties.getProperty("isRealTimeEnabled");
	    if (isRealTimeEnabledStr != null) {
	    	isRealTimeEnabled = Boolean.valueOf(isRealTimeEnabledStr);
	    }
		
		User user = ControllerUtil.getSignedInUser();
		
		List<Run> runList = this.runService.getRunListByOwner(user);
		runList.addAll(this.runService.getRunListBySharedOwner(user));
		// this is a temporary solution to filtering out runs that the logged-in user owns.
		// when the ACL entry permissions is figured out, we shouldn't have to do this filtering
		// start temporary code
		// hiroki commented out code 4/6/2008. remove after testing
		List<Run> runList2 = new ArrayList<Run>();
		for (Run run : runList) {
			if (run.getOwners().contains(user) || run.getSharedowners().contains(user)) {
				runList2.add(run);
			}
		}
		// end temporary code
		List<Run> current_run_list1 = new ArrayList<Run>();
		Map<Run, List<Workgroup>> workgroupMap = new HashMap<Run, List<Workgroup>>();
		for (Run run : runList2) {
			
			List<Workgroup> workgroupList = this.workgroupService
					.getWorkgroupListByOfferingAndUser(run, user);
	
			workgroupMap.put(run, workgroupList);
			if (!run.isEnded()) {
				current_run_list1.add(run);
			}
		}
		
		List<Run> current_run_list;
		Collections.sort(current_run_list1, CREATED_ORDER);
		if(current_run_list1.size() > 5){
			current_run_list = current_run_list1.subList(0,5);
		} else {
			current_run_list = current_run_list1;
		}
    	
		modelMap.put("user", user);
		modelMap.put("current_run_list", current_run_list);
		modelMap.put("current_run_list1", current_run_list1);
		modelMap.put("isRealTimeEnabled", isRealTimeEnabled);
		modelMap.put("workgroup_map", workgroupMap);
		modelMap.put("teacherOnlyNewsItems",newsItemService.retrieveByType("teacherOnly"));
    	
    	// if discourse is enabled for this WISE instance, add the link to the model
    	// so the view can display it
    	String discourseURL = wiseProperties.getProperty("discourse_url");
    	if (discourseURL != null && !discourseURL.isEmpty()) {
    		String discourseSSOLoginURL = discourseURL + "/session/sso";
    		modelMap.put("discourseSSOLoginURL", discourseSSOLoginURL);
    	}
    	
        return "teacher/index";
	}
}