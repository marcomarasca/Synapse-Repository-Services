package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class AccessApprovalController extends BaseController {
	
	@Autowired
	ServiceProvider serviceProvider;
	
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ACCESS_APPROVAL, method = RequestMethod.POST)
	public @ResponseBody
	AccessApproval createAccessApproval(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException, ForbiddenException, InvalidModelException, IOException {
		return serviceProvider.accessApprovalService.createAccessApproval(userId, header, request);
	}
	
	public static AccessApproval instanceForType(String typeString) throws DatastoreException {
		try {
			return (AccessApproval)Class.forName(typeString).newInstance();
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
	}

	public AccessApproval deserialize(HttpServletRequest request, HttpHeaders header) throws DatastoreException, IOException {
		return serviceProvider.accessApprovalService.deserialize(request, header);
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_APPROVAL_WITH_ENTITY_ID, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<AccessApproval> getAccessApprovals(
				@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String entityId,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException, ForbiddenException {
		return serviceProvider.accessApprovalService.getAccessApprovals(userId, entityId, request);
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_APPROVAL_WITH_APPROVAL_ID, method = RequestMethod.DELETE)
	public void deleteAccessApprovals(
				@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String approvalId) throws DatastoreException, UnauthorizedException, NotFoundException, ForbiddenException {
		serviceProvider.accessApprovalService.deleteAccessApprovals(userId, approvalId);
	}
	
}
