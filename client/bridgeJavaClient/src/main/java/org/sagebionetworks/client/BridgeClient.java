package org.sagebionetworks.client;

import java.util.List;

import org.sagebionetworks.bridge.model.Community;
import org.sagebionetworks.bridge.model.data.ParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
import org.sagebionetworks.bridge.model.versionInfo.BridgeVersionInfo;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.*;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RowSet;

/**
 * Abstraction for Synapse.
 * 
 * @author jmhill
 * 
 */
public interface BridgeClient extends BaseClient {

	/**
	 * Get the endpoint of the bridge service
	 */
	public String getBridgeEndpoint();

	/**
	 * The repository endpoint includes the host and version. For example: "https://repo-prod.prod.sagebase.org/repo/v1"
	 */
	public void setBridgeEndpoint(String repoEndpoint);

	/****** general bridge info ******/

	/**
	 * get version info for bridge stack
	 * 
	 * @return
	 * @throws SynapseException
	 */
	public BridgeVersionInfo getBridgeVersionInfo() throws SynapseException;

	/****** communities ******/

	/**
	 * create a new community
	 * 
	 * @param community the template to create a new community from
	 * @return the newly created community
	 * @throws SynapseException
	 */
	public Community createCommunity(Community community) throws SynapseException;

	/**
	 * Get the communities for the current user
	 * 
	 * @throws SynapseException
	 */
	public PaginatedResults<Community> getCommunities(long limit, long offset) throws SynapseException;

	/**
	 * Get all the available communities
	 * 
	 * @throws SynapseException
	 */
	public PaginatedResults<Community> getAllCommunities(long limit, long offset) throws SynapseException;

	/**
	 * Get community by id
	 * 
	 * @throws SynapseException
	 */
	public Community getCommunity(String communityId) throws SynapseException;

	/**
	 * Update community information
	 * 
	 * @param community
	 * @return
	 * @throws SynapseException
	 */
	public Community updateCommunity(Community community) throws SynapseException;

	/**
	 * Delete a community
	 * 
	 * @param communityId
	 * @return
	 * @throws SynapseException
	 */
	public void deleteCommunity(String communityId) throws SynapseException;

	/**
	 * Get all the members of a community (admins only)
	 * 
	 * @return
	 * @throws SynapseException
	 */
	public PaginatedResults<UserGroupHeader> getCommunityMembers(String communityId, long limit, long offset) throws SynapseException;

	/**
	 * Join a community
	 * 
	 * @param community
	 * @throws SynapseException
	 */
	public void joinCommunity(String communityId) throws SynapseException;

	/**
	 * Leave a community
	 * 
	 * @param community
	 * @throws SynapseException
	 */
	public void leaveCommunity(String communityId) throws SynapseException;

	/**
	 * Add a current member as an admin for the community
	 * 
	 * @param communityId
	 * @param principalId
	 * @throws SynapseException
	 */
	public void addCommunityAdmin(String communityId, String principalId) throws SynapseException;

	/**
	 * Remove a current member as an admin for the community
	 * 
	 * @param communityId
	 * @param principalId
	 * @throws SynapseException
	 */
	public void removeCommunityAdmin(String communityId, String principalId) throws SynapseException;

	/**
	 * Upload new participant provided data for this participant for a specific data set
	 * 
	 * @param data
	 * @param participantDataId the id of the participantData to append to
	 * @throws SynapseException
	 */
	public RowSet appendParticipantData(String participantDataId, RowSet data) throws SynapseException;

	/**
	 * Upload new participant provided data for a participant
	 * 
	 * @param participantIdentifier the de-identified identifier for the participant
	 * @param participantDataId the id of the participantData to append to
	 * @param data
	 * @throws SynapseException
	 */
	public RowSet appendParticipantData(String participantIdentifier, String participantDataId, RowSet data) throws SynapseException;

	/**
	 * Upload changed participant provided data
	 * 
	 * @param participantIdentifier the de-identified identifier for the participant
	 * @param participantDataId the id of the participantData to append to
	 * @param data
	 * @throws SynapseException
	 */
	public RowSet updateParticipantData(String participantDataId, RowSet data) throws SynapseException;

	/**
	 * retrieve participant data
	 * 
	 * @param participantDataId the id of the participantData to retrieve
	 * @return
	 * @throws SynapseException
	 */
	public RowSet getParticipantData(String participantDataId) throws SynapseException;

	public ParticipantDataDescriptor createParticipantData(ParticipantDataDescriptor participantDataDescriptor) throws SynapseException;

	public PaginatedResults<ParticipantDataDescriptor> getAllParticipantDatas(long limit, long offset) throws SynapseException;

	public PaginatedResults<ParticipantDataDescriptor> getParticipantDatas(long limit, long offset) throws SynapseException;

	public ParticipantDataColumnDescriptor createParticipantDataColumn(ParticipantDataColumnDescriptor participantDataColumnDescriptor1) throws SynapseException;

	public PaginatedResults<ParticipantDataColumnDescriptor> getParticipantDataColumns(String participantDataId, long limit, long offset) throws SynapseException;
}
