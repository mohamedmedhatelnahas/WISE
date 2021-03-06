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
package org.wise.portal.presentation.web.controllers.teacher.run;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import org.wise.portal.dao.ObjectNotFoundException;
import org.wise.portal.domain.authentication.impl.TeacherUserDetails;
import org.wise.portal.domain.group.Group;
import org.wise.portal.domain.impl.DefaultPeriodNames;
import org.wise.portal.domain.module.impl.CurnitGetCurnitUrlVisitor;
import org.wise.portal.domain.project.Project;
import org.wise.portal.domain.project.ProjectMetadata;
import org.wise.portal.domain.run.Run;
import org.wise.portal.domain.run.impl.RunParameters;
import org.wise.portal.domain.user.User;
import org.wise.portal.presentation.web.controllers.ControllerUtil;
import org.wise.portal.service.mail.IMailFacade;
import org.wise.portal.service.offering.RunService;
import org.wise.portal.service.project.ProjectService;
import org.wise.portal.service.workgroup.WorkgroupService;

/**
 * Controller for the wizard to "create a run"
 * 
 * The default getTargetPage() method is used to find out which page to navigate to, so
 * the controller looks for a request parameter starting with "_target" and ending with
 * a number (e.g. "_target1"). The jsp pages should provide these parameters.
 *
 * General method invocation flow (when user clicks on "prev" and "next"): 
 *  1) onBind
 *  2) onBindAndValidate
 *  3) validatePage
 *  4) referenceData
 * Note that on user's first visit to the first page of the wizard, only referenceData will be
 * invoked, and steps 1-3 are bypassed.
 *
 * @author Hiroki Terashima
 * @version $Id: CreateRunController.java 941 2007-08-16 14:03:11Z laurel $
 */
@Controller
@RequestMapping("/teacher/run/createRun.html")
@SessionAttributes("runParameters")
public class CreateRunController {
	
	@Autowired
	private RunService runService;

	@Autowired
	private WorkgroupService workgroupService;

	@Autowired
	private ProjectService projectService = null;

	private static final String COMPLETE_VIEW_NAME = "teacher/run/create/createrunfinish";

	private static final String RUN_KEY = "run";

	@Autowired
	private IMailFacade mailService = null;

	@Autowired
	private MessageSource messageSource;

	@Autowired
	protected Properties wiseProperties;

	private Map<Long,String> postLevelTextMap;

	/* change this to true if you are testing and do not want to send mail to
	   the actual groups */
	private static final Boolean DEBUG = false;

	//set this to your email
	private static final String DEBUG_EMAIL = "youremail@email.com";

	private static final Long[] IMPLEMENTED_POST_LEVELS = {5l,1l};

	/**
	 * The default handler (page=0)
	 */
	@RequestMapping
	public String getInitialPage(
			@RequestParam("projectId") final String projectId,
			final ModelMap modelMap) {
		User user = ControllerUtil.getSignedInUser();

		Project project = null;
		try {
			project = (Project) this.projectService.getById(projectId);
			if (!projectService.canCreateRun(project, user)) {
				return "errors/accessdenied";
			}
			project.getTags().size(); // fetch the tags
		} catch (ObjectNotFoundException e) {
			e.printStackTrace();
		}
		modelMap.put("project", project);

		// add the current user as an owner of the run
		Set<User> owners = new HashSet<User>();
		owners.add(user);
		RunParameters runParameters = new RunParameters();
		runParameters.setOwners(owners);
		runParameters.setProject(project);
		runParameters.setName(project.getProjectInfo().getName());

		/* get the owners and add their usernames to the model */
		String ownerUsernames = "";
		Set<User> allOwners = project.getOwners();
		allOwners.addAll(project.getSharedowners());

		for(User currentOwner : allOwners){
			ownerUsernames += currentOwner.getUserDetails().getUsername() + ",";
		}

		modelMap.put("projectOwners", ownerUsernames.substring(0, ownerUsernames.length() - 1));

		/* determine if the project has been cleaned since last edited
		 * and that the results indicate that all critical problems
		 * have been resolved. Add relevant data to the model. */
		boolean forceCleaning = false;
		boolean isAllowedToClean = (project.getOwners().contains(user) || project.getSharedowners().contains(user));
		ProjectMetadata metadata = project.getMetadata();

		if(metadata != null){
			Date lastCleaned = metadata.getLastCleaned();
			//Long lcTime = lastCleaned.getLong("timestamp");
			Date lastEdited = metadata.getLastEdited();

			/* if it has been edited since it was last cleaned, we need to force cleaning */
			if(lastCleaned != null && lastEdited != null && lastCleaned.before(lastEdited)) {
				forceCleaning = true;
			}
		}

		forceCleaning = false;  //TODO: Jon remove when cleaning is stable

		modelMap.put("user", user);
		modelMap.put("currentUsername", user.getUserDetails().getUsername());
		modelMap.put("forceCleaning", forceCleaning);
		modelMap.put("isAllowedToClean", isAllowedToClean);

		modelMap.addAttribute("runParameters", runParameters);
		// populate the model Map as needed
		return "teacher/run/create/createrunconfirm";
	}

	@RequestMapping(params = "_cancel")
	public String processCancel(final HttpServletRequest request,
			final HttpServletResponse response,
			final SessionStatus status) {
		status.setComplete();
		return "redirect:/teacher/index.html";
	}

	/**
	 * For page 1, show list of existing runs for this user and lets them select
	 * which ones to archive
	 */
	@RequestMapping(params = "_page=1")
	public String processFirstPage(
			final @ModelAttribute("runParameters") RunParameters runParameters,
			final BindingResult result,
			final ModelMap modelMap) {
		
		User user = ControllerUtil.getSignedInUser();
		
		// for page 2 of the wizard, display existing runs for this user
		List<Run> allRuns = runService.getRunListByOwner(user);

		// this is a temporary solution to filtering out runs that the logged-in user owns.
		// when the ACL entry permissions is figured out, we shouldn't have to do this filtering
		// start temporary code
		List<Run> currentRuns = new ArrayList<Run>();
		for (Run run : allRuns) {
			if (run.getOwners().contains(user) &&
					!run.isEnded()) {
				currentRuns.add(run);
			}
		}
		// end temporary code
		modelMap.put("existingRunList", currentRuns);

		return "teacher/run/create/createrunarchive";
	}

	/**
	 * Second page handler. Allow user to specify periods that they will use
	 * the run for
	 */
	@RequestMapping(params = "_page=2")
	public String processSecondPage(
			final @ModelAttribute("runParameters") RunParameters runParameters,
			final BindingResult result,
			final ModelMap modelMap) {

		modelMap.put("periodNames", DefaultPeriodNames.values());

		return "teacher/run/create/createrunperiods";
	}

	/**
	 * Third page handler. This is where user selects postLevel and real time settings
	 */
	@RequestMapping(params = "_page=3")
	public String processThirdPage(
			final @ModelAttribute("runParameters") RunParameters runParameters,
			final BindingResult result,
			final ModelMap modelMap,
			final HttpServletRequest request) {
		modelMap.put("periodNames", DefaultPeriodNames.values());
		
		if (runParameters.getPeriodNames() == null || 
		runParameters.getPeriodNames().size() == 0) {
			if (runParameters.getManuallyEnteredPeriods() == ""){
				result.rejectValue("periodNames", "setuprun.error.selectperiods", "You must select one or more periods or manually" +
						" create your period names.");
			} else {
				// check if manually entered periods is an empty string or just "," If yes, throw error
				if (runParameters.getManuallyEnteredPeriods() == null || 
						StringUtils.trim(runParameters.getManuallyEnteredPeriods()).length() == 0 ||
						StringUtils.trim(runParameters.getManuallyEnteredPeriods()).equals(",")) {
					result.rejectValue("periodNames", "setuprun.error.selectperiods", "You must select one or more periods or manually" +
							" create your period names.");
				} else {
					String[] parsed = StringUtils.split(runParameters.getManuallyEnteredPeriods(), ",");
					if (parsed.length == 0) {
						result.rejectValue("periodNames", "setuprun.error.whitespaceornonalphanumeric", "Manually entered" +
								" periods cannot contain whitespace or non-alphanumeric characters.");
						return "teacher/run/create/createrunperiods";
					}
					Set<String> parsedAndTrimmed = new TreeSet<String>();
					for(String current : parsed){
						String trimmed = StringUtils.trim(current);
						if(trimmed.length() == 0 || StringUtils.contains(trimmed, " ") 
								|| !StringUtils.isAlphanumeric(trimmed)
								|| trimmed.equals(",")){
							result.rejectValue("periodNames", "setuprun.error.whitespaceornonalphanumeric", "Manually entered" +
									" periods cannot contain whitespace or non-alphanumeric characters.");
							break;
						} else {
							parsedAndTrimmed.add(trimmed);
						}
					}
					runParameters.setPeriodNames(parsedAndTrimmed);
					runParameters.setManuallyEnteredPeriods("");
				}
			} 
		} else if (runParameters.getManuallyEnteredPeriods() != "") {
			result.rejectValue("periodNames", "setuprun.error.notsupported", "Selecting both periods AND" +
					" manually entering periods is not supported.");	    			
		}
		if (result.hasErrors()) {
			return "teacher/run/create/createrunperiods";
		}
		
		

		postLevelTextMap = new HashMap<Long,String>();
		String defaultPostLevelHighMessage = messageSource.getMessage("presentation.web.controllers.teacher.run.CreateRunController.postLevelHighMessage", 
				null, Locale.US);
		String postLevelHighMessage = messageSource.getMessage("presentation.web.controllers.teacher.run.CreateRunController.postLevelHighMessage", 
				null,defaultPostLevelHighMessage, request.getLocale());
		String defaultPostLevelLowMessage = messageSource.getMessage("presentation.web.controllers.teacher.run.CreateRunController.postLevelLowMessage", 
				null, Locale.US);
		String postLevelLowMessage = messageSource.getMessage("presentation.web.controllers.teacher.run.CreateRunController.postLevelLowMessage", 
				null,defaultPostLevelLowMessage, request.getLocale());

		postLevelTextMap.put(5l, postLevelHighMessage);
		postLevelTextMap.put(1l, postLevelLowMessage);

		Project project = runParameters.getProject();
		String maxWorkgroupSizeStr = wiseProperties.getProperty("maxWorkgroupSize", "3");
		int maxWorkgroupSize = Integer.parseInt(maxWorkgroupSizeStr);
		runParameters.setMaxWorkgroupSize(maxWorkgroupSize);
		modelMap.put("maxWorkgroupSize", maxWorkgroupSize);
		modelMap.put("implementedPostLevels", IMPLEMENTED_POST_LEVELS);
		modelMap.put("postLevelTextMap", postLevelTextMap);
		modelMap.put("minPostLevel", this.getMinPostLevel(project));
		runParameters.setEnableRealTime(true);		
		return "teacher/run/create/createrunconfigure";
	}


	/**
	 * Fourth step handler
	 */
	@RequestMapping(params = "_page=4")
	public String processFourthPage(
			final @ModelAttribute("runParameters") RunParameters runParameters,
			final BindingResult result,
			final ModelMap modelMap) {
		
		Project project = runParameters.getProject();
		String relativeProjectFilePath = (String) project.getCurnit().accept(new CurnitGetCurnitUrlVisitor());  // looks like this: "/109/new.project.json"
		int ndx = relativeProjectFilePath.lastIndexOf("/");
		String projectJSONFilename = relativeProjectFilePath.substring(ndx + 1, relativeProjectFilePath.length());  // looks like this: "new.project.json"

		//get the project name
		String projectName = project.getName();

		//replace ' with \' so when the project name is displayed on the jsp page, it won't short circuit the string
		projectName = projectName.replaceAll("\\'", "\\\\'");

		modelMap.put("projectId", project.getId());
		modelMap.put("projectType", project.getProjectType());
		modelMap.put("projectName", projectName);
		modelMap.put("projectJSONFilename", projectJSONFilename);

		return "teacher/run/create/createrunreview";
	}


	/**
	 * Retrieves the post level from the project metadata if it exists and determines
	 * the minimum post level that the user can set for the run.
	 * 
	 * @param project
	 * @return
	 */
	private Long getMinPostLevel(Project project){
		Long level = 1l;

		ProjectMetadata metadata = project.getMetadata();

		if(metadata != null && metadata.getPostLevel() != null) {
			level = metadata.getPostLevel();
		}

		return level;
	}

	/**
	 * Creates a run.
	 * 
	 * This method is called if there is a submit that validates and contains the "_finish"
	 * request parameter.
	 * 
	 * @see org.springframework.web.servlet.mvc.AbstractWizardFormController#processFinish(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@RequestMapping(params = "_finish")
	protected ModelAndView processFinish(
			final @ModelAttribute("runParameters") RunParameters runParameters,
			final BindingResult result,
			final ModelMap modelMap,
			final HttpServletRequest request,
			final SessionStatus status)
					throws Exception {

		Run run = null;
		try {
			// get newProjectId from request and use that to set up the run
			String newProjectId = request.getParameter("newProjectId");
			Project newProject = projectService.getById(new Long(newProjectId));
			runParameters.setProject(newProject);
			Locale userLocale = request.getLocale();
			runParameters.setLocale(userLocale);
			run = this.runService.createRun(runParameters);

			// create a workgroup for the owners of the run (teacher)
			workgroupService.createWISEWorkgroup("teacher", runParameters.getOwners(), run, null);

		} catch (ObjectNotFoundException e) {
			result.rejectValue("curnitId", "error.curnit-not_found",
					new Object[] { runParameters.getCurnitId() }, 
					"Curnit Not Found.");
			//return showForm(request, response, errors);
			return null;
		}
		ModelAndView modelAndView = new ModelAndView(COMPLETE_VIEW_NAME);
		modelAndView.addObject(RUN_KEY, run);
		Set<String> runIdsToArchive = runParameters.getRunIdsToArchive();
		if(runIdsToArchive != null) {
			for(String runIdStr : runIdsToArchive) {
				Long runId = Long.valueOf(runIdStr);
				Run runToArchive = runService.retrieveById(runId);
				runService.endRun(runToArchive);
			}
		}

		// send email to the recipients in new thread
		//tries to retrieve the user from the session
		User user = ControllerUtil.getSignedInUser();
		Locale locale = request.getLocale();
		String fullWiseContextPath = ControllerUtil.getPortalUrlString(request);  // e.g. http://localhost:8080/wise

		CreateRunEmailService emailService = 
				new CreateRunEmailService(runParameters, run, user, locale, fullWiseContextPath);
		Thread thread = new Thread(emailService);
		thread.start();
		
		status.setComplete();
		return modelAndView;
	}

	class CreateRunEmailService implements Runnable {

		private Object command;
		private Run run;
		private User user;
		private Locale locale;
		private String fullWiseContextPath;

		public CreateRunEmailService(
				Object command, Run run, User user, Locale locale, String fullWiseContextPath) {
			this.command = command;
			this.run = run;
			this.user = user;
			this.locale = locale;
			this.fullWiseContextPath = fullWiseContextPath;
		}

		public void run() {
			try {
				String sendEmailEnabledStr = wiseProperties.getProperty("send_email_enabled");
				Boolean sendEmailEnabled = Boolean.valueOf(sendEmailEnabledStr);
				if (!sendEmailEnabled) {
					return;
				}
				sendEmail();
			} catch (MessagingException e) {
				// what if there was an error sending email?
				// should uber_admin be notified?
				e.printStackTrace();
			}
		}

		/**
		 * sends an email to individuals to notify them of a new project run
		 * having been set up by a teacher
		 */
		private void sendEmail() throws MessagingException {
			RunParameters runParameters = (RunParameters) command;
			String teacherName = null;
			String teacherEmail = null;
			Serializable projectID = null;
			String schoolName = null;
			String schoolCity = null;
			String schoolState = null;
			String schoolPeriods = null;
			Date date = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMMMM d, yyyy");

			TeacherUserDetails teacherUserDetails = 
					(TeacherUserDetails) user.getUserDetails();

			teacherName = teacherUserDetails.getFirstname() + " " + 
					teacherUserDetails.getLastname();
			teacherEmail = teacherUserDetails.getEmailAddress();

			schoolName = teacherUserDetails.getSchoolname();
			schoolCity = teacherUserDetails.getCity();
			schoolState = teacherUserDetails.getState();


			schoolPeriods = runParameters.printAllPeriods();
			Set<String> projectcodes = new TreeSet<String>();
			String runcode = run.getRuncode();
			Set<Group> periods = run.getPeriods();
			for (Group period : periods) {
				projectcodes.add(runcode + "-" + period.getName());
			}

			projectID = runParameters.getProject().getId();
			Long runID = run.getId();

			String previewProjectUrl = fullWiseContextPath + "/previewproject.html?projectId="+run.getProject().getId();

			String[] recipients = wiseProperties.getProperty("project_setup").split(",");

			String defaultSubject = messageSource.getMessage("presentation.web.controllers.teacher.run.CreateRunController.setupRunConfirmationEmailSubject", 
					new Object[]{wiseProperties.getProperty("wise.name")}, Locale.US);

			String subject = messageSource.getMessage("presentation.web.controllers.teacher.run.CreateRunController.setupRunConfirmationEmailSubject", 
					new Object[]{wiseProperties.getProperty("wise.name")},defaultSubject, this.locale);


			String defaultMessage = messageSource.getMessage("presentation.web.controllers.teacher.run.CreateRunController.setupRunConfirmationEmailMessage", 
					new Object[]{
					wiseProperties.getProperty("wise.name"),
					teacherName,
					teacherUserDetails.getUsername(),
					teacherEmail,
					schoolName,
					schoolCity,
					schoolPeriods,
					projectcodes.toString(),
					run.getProject().getProjectInfo().getName(),
					projectID,
					runID,
					sdf.format(date),
					previewProjectUrl
			}, 
			Locale.US);

			String message = messageSource.getMessage("presentation.web.controllers.teacher.run.CreateRunController.setupRunConfirmationEmailMessage", 
					new Object[]{
					wiseProperties.getProperty("wise.name"),
					teacherName,
					teacherUserDetails.getUsername(),
					teacherEmail,
					schoolName,
					schoolCity,
					schoolPeriods,
					projectcodes.toString(),
					run.getProject().getProjectInfo().getName(),
					projectID,
					runID,
					sdf.format(date),
					previewProjectUrl
			}, 
			defaultMessage,
			this.locale);

			String fromEmail = wiseProperties.getProperty("portalemailaddress");

			//for testing out the email functionality without spamming the groups
			if(DEBUG) {
				recipients[0] = DEBUG_EMAIL;
			}

			//sends the email to the admin
			mailService.postMail(recipients, subject, message, fromEmail);

			//also send email to teacher	
			String[] teacherRecipient = new String[]{teacherEmail};

			String defaultTeacherSubject = messageSource.getMessage("presentation.web.controllers.teacher.run.CreateRunController.setupRunConfirmationTeacherEmailSubject", 
					new Object[]{run.getProject().getProjectInfo().getName()}, Locale.US);

			String teacherSubject = messageSource.getMessage("presentation.web.controllers.teacher.run.CreateRunController.setupRunConfirmationTeacherEmailSubject", 
					new Object[]{run.getProject().getProjectInfo().getName()},defaultTeacherSubject, this.locale);

			String defaultRunCodeDescription = messageSource.getMessage("teacher.run.create.createrunfinish.everyRunHasUniqueAccessCode", null, Locale.US);

			String runCodeDescription = messageSource.getMessage("teacher.run.create.createrunfinish.everyRunHasUniqueAccessCode", null, defaultRunCodeDescription, this.locale);

			String defaultTeacherMessage = messageSource.getMessage("presentation.web.controllers.teacher.run.CreateRunController.setupRunConfirmationTeacherEmailMessage", 
					new Object[]{
					teacherUserDetails.getUsername(),
					run.getProject().getProjectInfo().getName(),
					sdf.format(date),
					runcode,
					defaultRunCodeDescription
			}, 
			Locale.US);

			String teacherMessage = messageSource.getMessage("presentation.web.controllers.teacher.run.CreateRunController.setupRunConfirmationTeacherEmailMessage", 
					new Object[]{
					teacherUserDetails.getUsername(),
					run.getProject().getProjectInfo().getName(),
					sdf.format(date),
					runcode,
					runCodeDescription
			}, 
			defaultTeacherMessage,
			this.locale);

			if (wiseProperties.containsKey("discourse_url")) {
				String discourseURL = wiseProperties.getProperty("discourse_url");
				if (discourseURL != null && !discourseURL.isEmpty()) {
					// if this WISE instance uses discourse for teacher community, append link to it in the P.S. section of the email
					String defaultPS = messageSource.getMessage("teacherEmailPSCommunity", new Object[] {discourseURL}, Locale.US);
					String pS = messageSource.getMessage("teacherEmailPSCommunity", new Object[] {discourseURL}, defaultPS, this.locale);
					teacherMessage += "\n\n"+pS;
				}
			}

			//sends the email to the teacher
			mailService.postMail(teacherRecipient, teacherSubject, teacherMessage, fromEmail);
		}
	}
}