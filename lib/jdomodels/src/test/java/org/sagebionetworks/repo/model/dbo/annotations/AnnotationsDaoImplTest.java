package org.sagebionetworks.repo.model.dbo.annotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:jdomodels-test-context.xml")
public class AnnotationsDaoImplTest {

	@Autowired
	private AnnotationsDao annotationsDao;
	
	private ObjectType objectType;
	private IdAndVersion idAndVersion;
	private Random rd;
	
	@BeforeEach
	public void before() {
		objectType = ObjectType.ENTITY;
		idAndVersion = IdAndVersion.newBuilder()
				.setId(123L)
				.setVersion(456L)
				.build();
		rd = new Random();
		annotationsDao.truncateAll();
	}
	
	@AfterEach
	public void after() {
		annotationsDao.truncateAll();
	}
	
	@Test
	public void testSetAnnotations() {
		
		Annotations annotations = createAnnotations();
		
		// Call under test
		annotationsDao.setAnnotations(objectType, idAndVersion, annotations);
		
		Optional<Annotations> result = annotationsDao.getAnnotations(objectType, idAndVersion);
		
		assertTrue(result.isPresent());
		assertEquals(annotations, result.get());
	}
	
	@Test
	public void testSetAnnotationsWithUpdate() {
		
		Annotations annotations = createAnnotations();
		
		annotationsDao.setAnnotations(objectType, idAndVersion, annotations);
		
		Optional<Long> id = annotationsDao.getAnnotationsId(objectType, idAndVersion);
		
		assertTrue(id.isPresent());
		
		AnnotationsValueType valueType = AnnotationsValueType.STRING;
		
		AnnotationsV2TestUtils.putAnnotations(annotations, keyForType(valueType), "new_value", valueType);
		
		// Call under test
		annotationsDao.setAnnotations(objectType, idAndVersion, annotations);
		
		Optional<Long> resultId = annotationsDao.getAnnotationsId(objectType, idAndVersion);
		
		assertTrue(resultId.isPresent());
		assertEquals(id, resultId);
		
		Optional<Annotations> result = annotationsDao.getAnnotations(objectType, idAndVersion);
		
		assertTrue(result.isPresent());
		assertEquals(annotations, result.get());
	}
	
	@Test
	public void testSetAnnotationsWithDifferentVersion() {
		
		Annotations annotations = createAnnotations();
		
		annotationsDao.setAnnotations(objectType, idAndVersion, annotations);
		
		Optional<Long> firstId = annotationsDao.getAnnotationsId(objectType, idAndVersion);
		
		idAndVersion = IdAndVersion.newBuilder()
				.setId(idAndVersion.getId())
				.setVersion(idAndVersion.getVersion().get() + 1)
				.build();
		
		// Call under test
		annotationsDao.setAnnotations(objectType, idAndVersion, annotations);
		
		Optional<Long> secondId = annotationsDao.getAnnotationsId(objectType, idAndVersion);
		
		assertNotEquals(firstId, secondId);
		
	}
	
	@Test
	public void testGetAnnotations() {
		
		Optional<Annotations> emptyAnnotations = annotationsDao.getAnnotations(objectType, idAndVersion);
		
		assertFalse(emptyAnnotations.isPresent());
		
		Annotations annotations = createAnnotations();
		
		annotationsDao.setAnnotations(objectType, idAndVersion, annotations);
		
		Optional<Annotations> result = annotationsDao.getAnnotations(objectType, idAndVersion);
		
		assertTrue(result.isPresent());
		assertEquals(annotations, result.get());
	}	
	
	private Annotations createAnnotations() {
		Annotations annotations = AnnotationsV2TestUtils.newEmptyAnnotationsV2(KeyFactory.keyToString(idAndVersion.getId()));
		for (AnnotationsValueType type : AnnotationsValueType.values()) {
			String key = keyForType(type);
			String value = String.valueOf(rd.nextLong());
			AnnotationsV2TestUtils.putAnnotations(annotations, key, value, type);
		}
		return annotations;
	}
	
	private String keyForType(AnnotationsValueType valueType) {
		return valueType.name() + "_key";
	}
	
}
