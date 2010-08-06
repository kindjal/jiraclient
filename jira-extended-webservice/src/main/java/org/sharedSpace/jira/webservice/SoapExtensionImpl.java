/*
 * Copyright (C)  2010 Mario Ivankovits
 *
 * This file is part of jira-webservice-extensions.
 *
 * Ebean is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * Ebean is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with jira-webservice-extensions; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package org.sharedSpace.jira.webservice;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.EntityNotFoundException;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.bc.project.component.MutableProjectComponent;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.bc.project.component.ProjectComponentManager;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.exception.RemoveException;
import com.atlassian.jira.issue.AttachmentManager;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.worklog.Worklog;
import com.atlassian.jira.issue.worklog.WorklogManager;
import com.atlassian.jira.ofbiz.OfBizDelegator;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.rpc.auth.TokenManager;
import com.atlassian.jira.rpc.exception.RemoteAuthenticationException;
import com.atlassian.jira.rpc.exception.RemoteValidationException;
import com.atlassian.jira.rpc.exception.RemoteException;
import com.atlassian.jira.rpc.soap.JiraSoapServiceImpl;
import com.atlassian.jira.rpc.soap.beans.RemoteFieldValue;
import com.atlassian.jira.rpc.soap.beans.RemoteIssue;
import com.atlassian.jira.rpc.soap.service.AdminService;
import com.atlassian.jira.rpc.soap.service.IssueConstantsService;
import com.atlassian.jira.rpc.soap.service.IssueService;
import com.atlassian.jira.rpc.soap.service.ProjectRoleService;
import com.atlassian.jira.rpc.soap.service.ProjectService;
import com.atlassian.jira.rpc.soap.service.SchemeService;
import com.atlassian.jira.rpc.soap.service.SearchService;
import com.atlassian.jira.rpc.soap.service.UserService;
import com.atlassian.jira.rpc.soap.util.SoapUtilsBean;
import com.atlassian.jira.util.JiraDurationUtils;
import com.atlassian.jira.web.bean.FieldVisibilityBean;
import com.opensymphony.user.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SoapExtensionImpl extends JiraSoapServiceImpl implements SoapExtension
{
	private final ProjectManager projectManager;
	private final TokenManager tokenManager;
	private final SearchProvider searchProvider;
	private final CustomFieldManager customFieldManager;
	private final SoapUtilsBean soapUtilsBean;
	private final FieldVisibilityBean fieldVisibilityBean;
	private final AttachmentManager attachmentManager;
	private final OfBizDelegator ofBizDelegator;
	private final WorklogManager worklogManager;
	private final JiraDurationUtils jiraDurationUtils;
	private final IssueManager issueManager;
	private final SearchRequestService searchRequestService;
	private final OptionsManager optionsManager;
	private final ProjectComponentManager projectComponentManager;
	private final SubTaskManager subTaskManager;

	public SoapExtensionImpl(OptionsManager optionsManager, SearchRequestService searchRequestService, IssueManager issueManager, JiraDurationUtils jiraDurationUtils, WorklogManager worklogManager, OfBizDelegator ofBizDelegator, FieldVisibilityBean fieldVisibilityBean, ProjectManager projectManager, AttachmentManager attachmentManager, CustomFieldManager customFieldManager, SoapUtilsBean soapUtilsBean, SearchProvider searchProvider, TokenManager tokenManager, ProjectService projectService, IssueService issueService, UserService userService, SchemeService schemeService, AdminService adminService, SearchService searchService, ProjectRoleService projectRoleService, IssueConstantsService issueConstantsService, ProjectComponentManager projectComponentManager, SubTaskManager subTaskManager)
	{
		super(tokenManager, projectService, issueService, userService, schemeService, adminService, searchService, projectRoleService, issueConstantsService);

		this.ofBizDelegator = ofBizDelegator;
		this.projectManager = projectManager;
		this.attachmentManager = attachmentManager;
		this.customFieldManager = customFieldManager;
		this.soapUtilsBean = soapUtilsBean;
		this.searchProvider = searchProvider;
		this.tokenManager = tokenManager;
		this.fieldVisibilityBean = fieldVisibilityBean;
		this.worklogManager = worklogManager;
		this.jiraDurationUtils = jiraDurationUtils;
		this.issueManager = issueManager;
		this.searchRequestService = searchRequestService;
		this.optionsManager = optionsManager;
		this.projectComponentManager = projectComponentManager;
		this.subTaskManager = subTaskManager;
	}

	public String ping()
	{
		return "pong";
	}

	/**
	 * retrieve all issues of type <code>linkType</code> the issue <code>issueIdFrom</code> points to.
	 */
	public RemoteIssue[] getLinkedIssues(String token, Long issueIdFrom, String linkType) throws RemoteException
	{
		User user = tokenManager.retrieveUserNoPermissionCheck(token);

		IssueService is = ComponentManager.getComponentInstanceOfType(IssueService.class);
		RemoteIssue issueFrom = is.getIssueById(user, Long.toString(issueIdFrom));
		if (issueFrom == null)
		{
			throw new RemoteException("issue 'from' not found. id=" + issueIdFrom);
		}

		IssueLinkManager ilm = ComponentManager.getInstance().getIssueLinkManager();
		IssueLinkType contributeLinkType = null;
		if (!linkType.equals(""))
		{
			contributeLinkType = getLinkType(linkType);
		}

		// Collection<IssueLink> issueLinks = ilm.getIssueLinks(issueIdFrom);
		List<IssueLink> issueInLinks = ilm.getInwardLinks(issueIdFrom);

		List<RemoteIssue> ret = new ArrayList<RemoteIssue>();
		if (issueInLinks != null)
		{
			for (IssueLink issueLink : issueInLinks)
			{
				if (contributeLinkType == null || issueLink.getIssueLinkType().equals(contributeLinkType))
				{
					Long issueIdTo = issueLink.getSourceId();

					if (issueIdTo != issueIdFrom)
					{
						RemoteIssue issueTo = is.getIssueById(user, Long.toString(issueIdTo));

						ret.add(issueTo);
					}
				}
			}
		}

		List<IssueLink> issueOutLinks = ilm.getOutwardLinks(issueIdFrom);

		if (issueOutLinks != null)
		{
			for (IssueLink issueLink : issueOutLinks)
			{
				if (contributeLinkType == null || issueLink.getIssueLinkType().equals(contributeLinkType))
				{
					Long issueIdTo = issueLink.getDestinationId();

					if (issueIdTo != issueIdFrom)
					{
						RemoteIssue issueTo = is.getIssueById(user, Long.toString(issueIdTo));

						ret.add(issueTo);
					}
				}
			}
		}
		return ret.toArray(new RemoteIssue[ret.size()]);
	}

	/**
	 * remove the link with given info
	 */
	public void unlinkIssue(String token, Long issueIdFrom, Long issueIdTo, String linkType) throws RemoteException
	{
		User user = tokenManager.retrieveUserNoPermissionCheck(token);

		IssueService is = ComponentManager.getComponentInstanceOfType(IssueService.class);
		RemoteIssue issueFrom = is.getIssueById(user, Long.toString(issueIdFrom));
		if (issueFrom == null)
		{
			throw new RemoteException("issue 'from' not found. id=" + issueIdFrom);
		}
		RemoteIssue issueTo = is.getIssueById(user, Long.toString(issueIdTo));
		if (issueTo == null)
		{
			throw new RemoteException("issue 'to' not found. id=" + issueIdTo);
		}

		IssueLinkManager ilm = ComponentManager.getInstance().getIssueLinkManager();
		IssueLinkType contributeLinkType;
		if (linkType.equals(""))
		{
			contributeLinkType = null;
		}
		else
		{
		contributeLinkType = getLinkType(linkType);
		}

		IssueLink issueLink = ilm.getIssueLink(issueIdFrom, issueIdTo, contributeLinkType.getId());

		if (issueLink == null)
		{
			// link does not exist
			return;
		}

		try
		{
			ilm.removeIssueLink(issueLink, user);
		}
		catch (RemoveException e)
		{
			throw new RemoteException(e.getLocalizedMessage());
		}
	}

	/**
	 * create a outward link from <code>issueIdFrom</code> to <code>issueIdTo</code> of type <code>linkType</code>.
	 *
	 * @param token	   user token from login
	 * @param issueIdFrom issue on which the outward link should be created
	 * @param issueIdTo   issue which should receive the inward link
	 * @param linkType	name of the link type to create
	 * @param unique	  failure if an outward link of type <code>linkType</code> already exists
	 * @param replace	 replace all links of type <code>linkType</code> from the "from issue".
	 * @throws RemoteException
	 */
	public void linkIssue(String token, Long issueIdFrom, Long issueIdTo, String linkType, boolean unique, boolean replace) throws RemoteException
	{
		User user = tokenManager.retrieveUserNoPermissionCheck(token);

		IssueService is = ComponentManager.getComponentInstanceOfType(IssueService.class);
		RemoteIssue issueFrom = is.getIssueById(user, Long.toString(issueIdFrom));
		if (issueFrom == null)
		{
			throw new RemoteException("issue 'from' not found. id=" + issueIdFrom);
		}
		RemoteIssue issueTo = is.getIssueById(user, Long.toString(issueIdTo));
		if (issueTo == null)
		{
			throw new RemoteException("issue 'to' not found. id=" + issueIdTo);
		}

		IssueLinkManager ilm = ComponentManager.getInstance().getIssueLinkManager();
		IssueLinkType contributeLinkType = getLinkType(linkType);

		if (unique)
		{
			Collection<IssueLink> issueFromLinks = ilm.getIssueLinks(issueIdFrom);
			if (issueFromLinks != null)
			{
				for (IssueLink issueLink : issueFromLinks)
				{
					if (issueLink.getIssueLinkType().equals(contributeLinkType))
					{
						throw new RemoteException("link of type '" + contributeLinkType.getName() + "' already exists");
					}
				}
			}
		}
		if (replace)
		{
			Collection<IssueLink> issueFromLinks = ilm.getIssueLinks(issueIdFrom);
			if (issueFromLinks != null)
			{
				for (IssueLink issueLink : issueFromLinks)
				{
					if (issueLink.getIssueLinkType().equals(contributeLinkType))
					{
						try
						{
							ilm.removeIssueLink(issueLink, user);
						}
						catch (RemoveException e)
						{
							throw new RemoteException(e.getLocalizedMessage());
						}
					}
				}
			}
		}

		try
		{
			ilm.createIssueLink(issueIdFrom, issueIdTo, contributeLinkType.getId(), null, user);
		}
		catch (CreateException e)
		{
			throw new RemoteException(e.getLocalizedMessage());
		}
	}

	private IssueLinkType getLinkType(String linkType)
		throws RemoteException
	{
		IssueLinkTypeManager iltm = ComponentManager.getComponentInstanceOfType(IssueLinkTypeManager.class);

		Collection<IssueLinkType> contributeLinkTypes = iltm.getIssueLinkTypesByName(linkType);
		if (contributeLinkTypes == null || contributeLinkTypes.size() < 1)
		{
			throw new RemoteException("no link type with name '" + linkType + "' found.");
		}

		IssueLinkType contributeLinkType = contributeLinkTypes.iterator().next();
		return contributeLinkType;
	}

	public RemoteIssue getIssueForWorklog(String token, Long worklogId) throws RemoteException, SearchException
	{
		Worklog worklog = worklogManager.getById(worklogId);
		if (worklog == null)
		{
			return null;
		}

		return convertIssueObjectToRemoteIssue(worklog.getIssue());
	}

	private RemoteIssue convertIssueObjectToRemoteIssue(Issue issue) throws RemoteException
	{
		return new RemoteIssue(issue, customFieldManager, attachmentManager, soapUtilsBean);
	}

	private String formatTimeDuration(long timeSpentInSeconds)
	{
		return jiraDurationUtils.getFormattedDuration(new Long(timeSpentInSeconds));
	}

	public String[] getCustomFieldValues(String token, Long customFieldId, Long issueId) throws RemoteException, SearchException
	{
		User user = tokenManager.retrieveUserNoPermissionCheck(token);

		Issue issue = issueManager.getIssueObject(issueId);
		CustomField customField = customFieldManager.getCustomFieldObject(customFieldId);

		Options options = optionsManager.getOptions(customField.getRelevantConfig(issue));

		String[] ret = new String[options.size()];
		for (int i = 0; i<options.size(); i++)
		{
			ret[i] = ((Option) options.get(i)).getValue();
		}

		return ret;
	}

	public long addComponent(String token, String projectKey, String name, String description, String lead, long assigneeType) throws RemoteException, RemoteAuthenticationException
	{
		User user = tokenManager.retrieveUserNoPermissionCheck(token);

		Project project = projectManager.getProjectObjByKey(projectKey);
		if (project == null)
		{
			throw new RemoteException("project with key '" + projectKey + "' not found.");
		}

		ProjectComponent component = projectComponentManager.create(name, description, lead, assigneeType, project.getId());
		if (component == null)
		{
			throw new RemoteException("problems creating component with name '" + name + "' not found.");
		}

		return component.getId();
	}

	public void removeComponent(String token, String projectKey, long componentId) throws RemoteException, RemoteAuthenticationException
	{
		User user = tokenManager.retrieveUserNoPermissionCheck(token);

		Project project = projectManager.getProjectObjByKey(projectKey);
		if (project == null)
		{
			throw new RemoteException("project with key '" + projectKey + "' not found.");
		}

		try
		{
			projectComponentManager.delete(componentId);
		}
		catch (EntityNotFoundException e)
		{
			throw new RemoteException(e.getLocalizedMessage());
		}
	}

	public void updateComponent(String token, String projectKey, long componentId, String name, String description, String lead, long assigneeType) throws RemoteException, RemoteAuthenticationException
	{
		User user = tokenManager.retrieveUserNoPermissionCheck(token);

		Project project = projectManager.getProjectObjByKey(projectKey);
		if (project == null)
		{
			throw new RemoteException("project with key '" + projectKey + "' not found.");
		}

		MutableProjectComponent mutableCmp;
		try
		{
			ProjectComponent cmp = projectComponentManager.find(componentId);
			if (!(cmp instanceof MutableProjectComponent))
			{
				throw new RemoteException("ProjectComponentManager did not return an updateable component object");
			}

			mutableCmp = (MutableProjectComponent) cmp;
		}
		catch (EntityNotFoundException e)
		{
			throw new RemoteException(e.getLocalizedMessage());
		}

		mutableCmp.setName(name);
		mutableCmp.setDescription(description);
		mutableCmp.setLead(lead);
		mutableCmp.setAssigneeType(assigneeType);

		try
		{
			projectComponentManager.update(mutableCmp);
		}
		catch (EntityNotFoundException e)
		{
			throw new RemoteException(e.getLocalizedMessage());
		}
	}

	public void createSubtaskLink(String token, long parentIssueId, long subtaskIssueId, long issueTypeId) throws RemoteException
	{
		User user = tokenManager.retrieveUserNoPermissionCheck(token);

		MutableIssue parentIssue = issueManager.getIssueObject(parentIssueId);
		if (parentIssue == null)
		{
			throw new RemoteException("parent issue not found. id=" + parentIssueId);
		}

		MutableIssue subIssue = issueManager.getIssueObject(subtaskIssueId);
		if (subIssue == null)
		{
			throw new RemoteException("sub issue not found. id=" + subtaskIssueId);
		}
		if (subIssue.getParentId() != null)
		{
			throw new RemoteException("sub issue already linked. id=" + subtaskIssueId + " parent=" + subIssue.getParentId());
		}

		subIssue.setParentId(parentIssue.getId());
		try
		{
			subTaskManager.createSubTaskIssueLink(parentIssue, subIssue, user);
		}
		catch (CreateException e)
		{
			throw new RemoteException(e.getLocalizedMessage());
		}

    // MBC: This throws an exception, do it with the client instead
		// Update the IssueType field to the new SubtaskIssueType
		//RemoteFieldValue[] actionParams = new RemoteFieldValue[1];
		//actionParams[0] = new RemoteFieldValue("issuetype", new String[] {"" + issueTypeId });
		//this.updateIssue(token, subIssue.getKey(), actionParams);
	}
}
