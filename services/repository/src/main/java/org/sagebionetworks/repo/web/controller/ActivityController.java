package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.service.ServiceProvider;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.RequiredScope;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * <p>
 * The <a
 * href="${org.sagebionetworks.repo.model.provenance.Activity}">Activity</a>
 * model represents the main record of Provenance in Synapse. It is analygous to
 * the Activity defined in the <a href="http://www.w3.org/TR/prov-primer/">W3C
 * Specification on Provenance</a>.
 * </p>
 * <h6>Used & Generated By</h6>
 * <p>
 * Used links are stored directly in the Activity model object as an array of <a
 * href="${org.sagebionetworks.repo.model.provenance.Used}">Used</a> objects.
 * There is a flag in <a
 * href="${org.sagebionetworks.repo.model.provenance.Used}">Used</a> that marks
 * if it was "executed". <a
 * href="${org.sagebionetworks.repo.model.provenance.Used}">Used</a> is an
 * interface that is implemented by two objects:
 * </p>
 * <ul>
 * <li><a
 * href="${org.sagebionetworks.repo.model.provenance.UsedEntity}">UsedEntity</a>
 * - For referencing <a
 * href="${org.sagebionetworks.repo.model.Entity}">Entities</a> already stored
 * in Synapse</li>
 * <li><a
 * href="${org.sagebionetworks.repo.model.provenance.UsedURL}">UsedURL</a> - For
 * referencing URL-accessed resources stored outside of Synapse. In Provenance
 * visualizations, some URLs are given a special icon, such as links to <a
 * href="https://github.com/">GitHub</a>. Note: it is also possible to wrap a
 * URL with a <a
 * href="${org.sagebionetworks.repo.model.FileEntity}">FileEntity</a> if you
 * want all the resources that come with Synapse entities.</li>
 * </ul>
 * <p>
 * wasGeneratedBy links are stored for each version of each Entity. Thus you
 * update the entity with the activity id that generated it. You can ask the
 * entity service which activity generated it, and conversely you can ask the
 * activity service what entity versions were generatedBy a given activity.
 * </p>
 * <h6>Access Control for Activities</h6>
 * <p>
 * Access to <a
 * href="${org.sagebionetworks.repo.model.provenance.Activity}">Activity</a>
 * objects is dictated by the following rules:
 * <ul>
 * <li><b>READ</b> - Granted to those users who can see a single Entity that was
 * generated by this Activity.</li>
 * <li><b>UPDATE/DELETE</b> - You must be the creator of the Activity to modify
 * or delete it.</li>
 * <li><b>Setting generatedBy for an Entity</b> (see <a href="${POST.entity}">POST
 * /entity</a>) - You must be the creator of the activity to connect it to
 * an Entity. (The Entity services allow you to specify an activityId that
 * creates a generatedBy relationship between an Activity and an Entity.)</li>
 * </ul>
 * 
 * </p>
 */
@ControllerInfo(displayName="Activity Services", path="repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class ActivityController{
	
	@Autowired
	ServiceProvider serviceProvider;
			
	/**
	 * Create a new <a
	 * href="${org.sagebionetworks.repo.model.provenance.Activity}"
	 * >Activity</a>. If the passed <a
	 * href="${org.sagebionetworks.repo.model.provenance.Activity}">Activity</a>
	 * object contains a <a
	 * href="${org.sagebionetworks.repo.model.provenance.Used}" >Used</a> array,
	 * you must set the concreteType field of each Used subclass.
	 * <p>
	 * Access Control: READ access is granted to those users who can see a single Entity that
	 * was generated by this Activity.
	 * </p>
	 * 
	 * @param userId
	 *            The user that is doing the create.
	 * @param header
	 *            Used to get content type information.
	 * @param request
	 *            The body is extracted from the request.
	 * @return The new <a
	 *         href="${org.sagebionetworks.repo.model.provenance.Activity}"
	 *         >Activity</a>
	 * @throws DatastoreException
	 *             Thrown when an there is a server failure.
	 * @throws InvalidModelException
	 *             Thrown if the passed object does not match the expected
	 *             entity schema.
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 *             Thrown only for the case where the requesting user is not
	 *             found.
	 * @throws IOException
	 *             Thrown if there is a failure to read the header.
	 * @throws JSONObjectAdapterException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { 
			UrlHelpers.ACTIVITY
			}, method = RequestMethod.POST)
	public @ResponseBody
	Activity createActivity(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody Activity activity,
			HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException, JSONObjectAdapterException {
		return serviceProvider.getActivityService().createActivity(userId, activity);
	}
	
	/**
	 * Get an existing <a
	 * href="${org.sagebionetworks.repo.model.provenance.Activity}">Activity</a>
	 * <p>
	 * <b>Access Control</b>: Granted to those users who can see a single Entity
	 * that was generated by this Activity.
	 * </p>
	 * 
	 * @param id
	 *            The ID of the activity to fetch.
	 * @param userId
	 *            The user that is doing the get.
	 * @param request
	 * @return The requested Activity if it exists.
	 * @throws NotFoundException
	 *             Thrown if the requested activity does not exist.
	 * @throws DatastoreException
	 *             Thrown when an there is a server failure.
	 * @throws UnauthorizedException
	 *             Thrown if specified user is unauthorized to access this
	 *             activity.
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.ACTIVITY_ID
			}, method = RequestMethod.GET)
	public @ResponseBody
	Activity getActivity(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		return serviceProvider.getActivityService().getActivity(userId, id);
	}
	
	/**
	 * Update an <a
	 * href="${org.sagebionetworks.repo.model.provenance.Activity}">Activity</a>
	 * <p><b>Access Control</b>: You must be the creator of the Activity to modify it.</p>
	 * 
	 * @param id
	 *            The id of the activity to update.
	 * @param userId
	 *            The user that is doing the update.
	 * @param header
	 *            Used to get content type information.
	 * @param etag
	 *            A valid etag must be provided for every update call.
	 * @param request
	 *            Used to read the contents.
	 * @return the updated activity
	 * @throws NotFoundException
	 *             Thrown if the given activity does not exist.
	 * @throws ConflictingUpdateException
	 *             Thrown when the passed etag does not match the current etag
	 *             of the activity. This will occur when an activity gets
	 *             updated after getting the current etag.
	 * @throws DatastoreException
	 *             Thrown when there is a server side problem.
	 * @throws InvalidModelException
	 *             Thrown if the passed activity contents do not match the
	 *             expected schema.
	 * @throws UnauthorizedException
	 * @throws IOException
	 *             There is a problem reading the contents.
	 * @throws JSONObjectAdapterException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.ACTIVITY_ID
	}, method = RequestMethod.PUT)
	public @ResponseBody
	Activity updateActivity(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody Activity activity,
			HttpServletRequest request)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException, IOException, JSONObjectAdapterException {
		return serviceProvider.getActivityService().updateActivity(userId, activity);
	}
	
	/**
	 * Delete an <a
	 * href="${org.sagebionetworks.repo.model.provenance.Activity}">Activity</a>
	 * <p><b>Access Control</b>: You must be the creator of the Activity to delete it.</p>
	 * @param id
	 *            The id of activity to delete.
	 * @param userId
	 *            The user that is deleting the activity.
	 * @param request
	 * @throws NotFoundException
	 *             Thrown when the activity to delete does not exist.
	 * @throws DatastoreException
	 *             Thrown when there is a server side problem.
	 * @throws UnauthorizedException
	 *             Thrown when the user is not allowed to access or delete the
	 *             specified activity.
	 */
	@RequiredScope({modify})
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = { 			
			UrlHelpers.ACTIVITY_ID
			}, method = RequestMethod.DELETE)
	public void deleteActivity(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		serviceProvider.getActivityService().deleteActivity(userId, id);
	}
	
	/**
	 * <p>
	 * Get the <a href="${org.sagebionetworks.repo.model.Entity}">Entities</a>
	 * that were generatedBy an <a
	 * href="${org.sagebionetworks.repo.model.provenance.Activity}"
	 * >Activity</a>. Returns a <a
	 * href="${org.sagebionetworks.reflection.model.PaginatedResults}">
	 * PaginatedResults</a> of <a
	 * href="${org.sagebionetworks.repo.model.Reference}">Reference</a> objects.
	 * </p>
	 * <p>
	 * <b>Access Control</b>: This service will return References to all generatedBy
	 * Entities, regardless of whether you have access to resolve them into full
	 * Entity objects.
	 * </p>
	 * 
	 * @param id
	 * @param userId
	 * @param offset
	 * @param limit
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = {
			UrlHelpers.ACTIVITY_GENERATED
	}, method = RequestMethod.GET) 
	public @ResponseBody
	PaginatedResults<Reference> getEntitiesGeneratedBy(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit) 
			throws NotFoundException, DatastoreException, UnauthorizedException {
		return serviceProvider.getActivityService().getEntitiesGeneratedBy(userId, id, limit, offset);
	}

}
