package org.sagebionetworks.repo.model.dbo.annotations;

import java.util.Optional;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.entity.IdAndVersion;

public interface AnnotationsDao {

	/**
	 * Fetches the annotations for the object with the given id
	 * 
	 * @param objectType   The object type
	 * @param idAndVersion The object id, note that the version must be included
	 * @return An optional containing the annotations for the given object, note that the etag of the Annotations is not set
	 */
	Optional<Annotations> getAnnotations(ObjectType objectType, IdAndVersion idAndVersion);

	/**
	 * Sets the annotation for the object with the given id
	 * 
	 * @param objectType   The object type
	 * @param idAndVersion The object id, note that the version must be included
	 * @param annotations  The annotations to be set on the given object
	 * @return The same object that was passed as input
	 */
	Annotations setAnnotations(ObjectType objectType, IdAndVersion idAndVersion, Annotations annotations);
	
	// For testing
	
	Optional<Long> getAnnotationsId(ObjectType objectType, IdAndVersion idAndVersion);
	
	void truncateAll();

}
