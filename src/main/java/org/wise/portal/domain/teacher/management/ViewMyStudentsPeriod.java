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
package org.wise.portal.domain.teacher.management;

import java.util.Set;

import org.wise.portal.domain.group.Group;
import org.wise.portal.domain.run.Run;
import org.wise.portal.domain.user.User;
import org.wise.portal.domain.workgroup.Workgroup;

/**
 * This domain object will contain information needed for the
 * teacher's viewmystudents page
 * 
 * @author Hiroki Terashima
 */
public class ViewMyStudentsPeriod implements Comparable<ViewMyStudentsPeriod> {
	
	private Run run;
	
	private Group period;
	
	private Set<User> grouplessStudents;
	
	private Set<Workgroup> workgroups;

	/**
	 * @return the run
	 */
	public Run getRun() {
		return run;
	}

	/**
	 * @param run the run to set
	 */
	public void setRun(Run run) {
		this.run = run;
	}

	/**
	 * @return the period
	 */
	public Group getPeriod() {
		return period;
	}

	/**
	 * @param period the period to set
	 */
	public void setPeriod(Group period) {
		this.period = period;
	}

	/**
	 * @return the grouplessStudents
	 */
	public Set<User> getGrouplessStudents() {
		return grouplessStudents;
	}

	/**
	 * @param grouplessStudents the grouplessStudents to set
	 */
	public void setGrouplessStudents(Set<User> grouplessStudents) {
		this.grouplessStudents = grouplessStudents;
	}

	/**
	 * @return the workgroups
	 */
	public Set<Workgroup> getWorkgroups() {
		return workgroups;
	}

	/**
	 * @param workgroups the workgroups to set
	 */
	public void setWorkgroups(Set<Workgroup> workgroups) {
		this.workgroups = workgroups;
	}

	/**
	 * Natural order is based on periodname
	 */
	public int compareTo(ViewMyStudentsPeriod o) {
		return this.period.compareTo(o.period);
	}
}
