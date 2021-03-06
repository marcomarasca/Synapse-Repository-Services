package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.web.NotFoundException;

public interface MembershipRequestDAO {
	/**
	 * @param dto object to be created
	 * @return the newly created object
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public MembershipRequest create(MembershipRequest dto) throws DatastoreException, InvalidModelException;

	/**
	 * Retrieves the object given its id
	 * 
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public MembershipRequest get(String id) throws DatastoreException, NotFoundException;
	
	/**
	 * 
	 * @param requesterId
	 * @param now
	 * @param limit
	 * @param offset
	 * @return
	 */
	public List<MembershipRequest> getOpenByRequesterInRange(
			long requesterId, long now, long limit, long offset);

	/**
	 * Get the open (unexpired and unfulfilled) MembershipRequests received by the given team
	 * 
	 * @param teamId
	 * @param now current time, expressed as a long
	 * @param offset
	 * @param limit
	 * 
	 */
	public List<MembershipRequest> getOpenByTeamInRange(long teamId, long now, long limit, long offset) throws DatastoreException, NotFoundException;

	/**
	 * Get the open (unexpired and unfulfilled) MembershipRequests received by the given team from a given requester
	 * @param teamId
	 * @param requesterId
	 * @param now
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<MembershipRequest> getOpenByTeamAndRequesterInRange(long teamId, long requesterId, long now, long limit, long offset) throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @param requesterId
	 * @param now
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getOpenByRequesterCount(long requesterId, long now) throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @param teamId
	 * @param now
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getOpenByTeamCount(long teamId, long now) throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @param teamId
	 * @param requesterId
	 * @param now
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getOpenByTeamAndRequesterCount(long teamId, long requesterId, long now) throws DatastoreException, NotFoundException;

	/**
	 * delete the object given by the given ID
	 * 
	 * @param id
	 *            the id of the object to be deleted
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void delete(String id) throws DatastoreException, NotFoundException;
	
	/**
	 * 
	 * @param teamId
	 * @param requesterId
	 * @throws DatastoreException
	 */
	public void deleteByTeamAndRequester(long teamId, long requesterId) throws DatastoreException;

	/**
	 * Retrieve the number of open requests for all teamIds
	 * 
	 * @param teamIds
	 * @param expireAfter
	 * @return
	 */
	public long getOpenByTeamsCount(List<String> teamIds, long expireAfter);
}
