package org.sagebionetworks.translator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.javadoc.velocity.schema.SchemaUtils;
import org.sagebionetworks.javadoc.velocity.schema.TypeReference;
import org.sagebionetworks.openapi.model.Discriminator;
import org.sagebionetworks.openapi.model.OpenApiJsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.schema.EnumValue;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.TYPE;
import org.sagebionetworks.util.ValidateArgument;

public class ObjectSchemaUtils {
	/**
	 * Generates a mapping of class id to an ObjectSchema that represents that
	 * class. Starts out with all of the concrete classes found in `autoGen`
	 * 
	 * @param concreteClassNames - the iterator whose values represent ids of all
	 *                           concrete classes.
	 * @return a mapping of concrete classes between class id to an ObjectSchema
	 *         that represents it.
	 */
	public Map<String, ObjectSchema> getConcreteClasses(Iterator<String> concreteClassNames) {
		Map<String, ObjectSchema> classNameToObjectSchema = new HashMap<>();
		while (concreteClassNames.hasNext()) {
			String className = concreteClassNames.next();
			ObjectSchema schema = SchemaUtils.getSchema(className);
			SchemaUtils.recursiveAddTypes(classNameToObjectSchema, className, schema);
		}
		return classNameToObjectSchema;
	}

	/**
	 * Translates a mapping from class id to ObjectSchema to a mapping from class id
	 * to OpenApiJsonSchema;
	 * 
	 * @param classNameToObjectSchema - the mapping being translated
	 * @return a translated mapping consisting of class id to OpenApiJsonSchema
	 */
	public Map<String, OpenApiJsonSchema> getClassNameToJsonSchema(Map<String, ObjectSchema> classNameToObjectSchema) {
		Map<String, OpenApiJsonSchema> classNameToJsonSchema = new HashMap<>();
		
		for (String className : classNameToObjectSchema.keySet()) {
			ObjectSchema schema = classNameToObjectSchema.get(className);
			classNameToJsonSchema.put(className, translateObjectSchemaToJsonSchema(schema));
		}
		
		Map<String, List<TypeReference>> interfaces = SchemaUtils.mapImplementationsToIntefaces(classNameToObjectSchema);
		
		insertOneOfPropertyForInterfaces(classNameToJsonSchema, interfaces);
		
				
		return classNameToJsonSchema;
	}

	/**
	 * Translates an ObjectSchema to a OpenApiJsonSchema
	 * 
	 * @param objectSchema - the schema being translated.
	 * @return the resulting OpenApiJsonSchema
	 */
	OpenApiJsonSchema translateObjectSchemaToJsonSchema(ObjectSchema objectSchema) {
		ValidateArgument.required(objectSchema, "objectSchema");

		TYPE schemaType = objectSchema.getType();

		EnumValue[] enumValues = objectSchema.getEnum();
		if (enumValues != null) {
			OpenApiJsonSchema jsonSchema = new OpenApiJsonSchema();
			jsonSchema.setType(translateObjectSchemaTypeToJsonSchemaType(schemaType));

			List<Object> values = new ArrayList<>();
			for (EnumValue enumValue: enumValues) {
				values.add(enumValue.getName());
			}

			jsonSchema.set_enum(values);

			return jsonSchema;
		}

		if (isPrimitive(schemaType)) {
			return getSchemaForPrimitiveType(schemaType);
		}
		
		OpenApiJsonSchema jsonSchema = new OpenApiJsonSchema();
		
		jsonSchema.setType(translateObjectSchemaTypeToJsonSchemaType(schemaType));

		Map<String, ObjectSchema> properties = objectSchema.getProperties();
		
		if (properties != null) {
			jsonSchema.setProperties(translatePropertiesFromObjectSchema(properties, objectSchema.getId()));
			
			List<String> requiredProperties = properties.entrySet().stream()
				.filter(property -> property.getValue().isRequired())
				.map(Entry<String, ObjectSchema>::getKey)
				.collect(Collectors.toList());
			
			if (!requiredProperties.isEmpty()) {
				jsonSchema.setRequired(requiredProperties);
			}
		}

		ObjectSchema items = objectSchema.getItems();
		if (items != null) {
			populateSchemaForArrayType(jsonSchema, items, objectSchema.getId());
		}
		if (objectSchema.getUniqueItems()) {
			jsonSchema.setUniqueItems(objectSchema.getUniqueItems());
		}
		if (objectSchema.getDescription() != null) {
			jsonSchema.setDescription(objectSchema.getDescription());
		}
		
		// If the object is an interface we need to add the discriminator on the concrete type
		if (TYPE.INTERFACE == schemaType) {
			jsonSchema.setDiscriminator(new Discriminator().setPropertyName(ObjectSchema.CONCRETE_TYPE));
		}
		
		// If the concreteType is included then make sure to set it as a required property
		if (jsonSchema.getProperties() != null && jsonSchema.getProperties().containsKey(ObjectSchema.CONCRETE_TYPE)) {
			List<String> requiredProperties = jsonSchema.getRequired() == null ? new ArrayList<>() : new ArrayList<>(jsonSchema.getRequired());
									
			if (!requiredProperties.contains(ObjectSchema.CONCRETE_TYPE)) {
				requiredProperties.add(ObjectSchema.CONCRETE_TYPE);
			}
						
			jsonSchema.setRequired(requiredProperties);
			
			// Include the single value of the class in the enum of the concreteType property See https://sagebionetworks.jira.com/browse/PLFM-8257
			if (TYPE.OBJECT == schemaType) {
				jsonSchema.getProperties().get(ObjectSchema.CONCRETE_TYPE)
					.set_enum(List.of(objectSchema.getId()));
			}
		}

		return jsonSchema;
	}

	/**
	 * Translate the "properties" attribute of an ObjectSchema, which is a mapping
	 * between propertyName to ObjectSchema
	 * 
	 * @param properties - the properties we are translating
	 * @param schemaId   - the id of the schema whose properties we are translating
	 * @return an equivalent mapping between class id to OpenApiJsonSchema
	 */
	Map<String, JsonSchema> translatePropertiesFromObjectSchema(Map<String, ObjectSchema> properties, String schemaId) {
		ValidateArgument.required(properties, "properties");
		Map<String, JsonSchema> result = new LinkedHashMap<>();
		for (String propertyName : properties.keySet()) {
			OpenApiJsonSchema property = getPropertyAsJsonSchema(properties.get(propertyName), schemaId);
			result.put(propertyName, property);
		}
		return result;
	}

	/**
	 * Translates a property of an ObjectSchema to a OpenApiJsonSchema.
	 * 
	 * @param property     the ObjectSchema property
	 * @param schemaId	   the id of the schema whose property we are translating
	 * @return the OpenApiJsonSchema that is equivalent to the ObjectSchema property
	 */
	public OpenApiJsonSchema translateObjectSchemaPropertyToJsonSchema(ObjectSchema property, String schemaId) {
		ValidateArgument.required(property, "property");
		ValidateArgument.required(schemaId, "schemaId");
		ValidateArgument.required(property.getType(), "property.type");
		TYPE propertyType = property.getType();
		if (isPrimitive(propertyType)) {
			return getSchemaForPrimitiveType(propertyType);
		} else {
			OpenApiJsonSchema schema = new OpenApiJsonSchema();
			if (property.getDescription() != null) {
				schema.setDescription(property.getDescription());
			}
			if (property.getUniqueItems()) {
				schema.setUniqueItems(property.getUniqueItems());
			}

			switch (propertyType) {
			case ARRAY:
				populateSchemaForArrayType(schema, property.getItems(), schemaId);
				break;
			case TUPLE_ARRAY_MAP:
			case MAP:
				populateSchemaForMapType(schema, property, schemaId);			
				break;
			case OBJECT:
			case INTERFACE:
				populateSchemaForObjectType(schema, property, schemaId);
				break;
			default:
				throw new IllegalArgumentException("Unsupported propertyType " + propertyType);
			}
			return schema;
		}
	}
	
	/**
	 * Populate the schema for the Array type
	 * 
	 * @param schema the schema being populated
	 * @param items the schema which represents each element in the array
	 * @param schemaId the id of the schema which contains the property
	 */
	void populateSchemaForArrayType(OpenApiJsonSchema schema, ObjectSchema items, String schemaId) {
		ValidateArgument.required(schema, "schema");
		ValidateArgument.required(items, "items");
		ValidateArgument.required(schemaId, "schemaId");
		schema.setType(Type.array);
		OpenApiJsonSchema itemsSchema = getPropertyAsJsonSchema(items, schemaId);
		schema.setItems(itemsSchema);
	}
	
	/**
	 * Populate the schema for the Map and Tuple_Array_Map types
	 * 
	 * @param schema the schema being populated
	 * @param property the property being looked at
	 * @param schemaId the id of the schema which contains the property
	 */
	void populateSchemaForMapType(OpenApiJsonSchema schema, ObjectSchema property, String schemaId) {
		ValidateArgument.required(schema, "schema");
		ValidateArgument.required(property, "property");
		ValidateArgument.required(schemaId, "schemaId");
		schema.setType(Type.object);
		OpenApiJsonSchema additionalProperty = getPropertyAsJsonSchema(property.getValue(), schemaId);
		schema.setAdditionalProperties(additionalProperty);
	}
	
	/**
	 * Get the OpenApiJsonSchema representation of a property.
	 * 
	 * @param property the property to be translated
	 * @param schemaId the id of the original schema which contains this property
	 * @return the OpenApiJsonSchema representation of the property
	 */
	OpenApiJsonSchema getPropertyAsJsonSchema(ObjectSchema property, String schemaId) {
		ValidateArgument.required(property, "property");
		ValidateArgument.required(schemaId, "schemaId");
		if (isSelfReferencing(property)) {
			return generateReferenceSchema(schemaId);
		}
		return translateObjectSchemaPropertyToJsonSchema(property, schemaId);
	}
	
	/**
	 * Populate the schema for the Object and Interface types.
	 * 
	 * @param schema the schema being populated
	 * @param property the property being looked at
	 * @param schemaId the id of the schema which contains the property
	 */
	void populateSchemaForObjectType(OpenApiJsonSchema schema, ObjectSchema property, String schemaId) {
		ValidateArgument.required(schema, "schema");
		ValidateArgument.required(property, "property");
		ValidateArgument.required(schemaId, "schemaId");
		String referenceId = isSelfReferencing(property) ? schemaId : property.getId();
		if (referenceId != null) {
			schema.set$ref(getPathInComponents(referenceId));
		}
	}
	
	/**
	 * Generates a OpenApiJsonSchema that is a reference to schema with schemaId
	 * 
	 * @param schemaId the id of the schema
	 * @return a OpenApiJsonSchema that is a reference to schemaId
	 */
	OpenApiJsonSchema generateReferenceSchema(String schemaId) {
		ValidateArgument.required(schemaId, "schemaId");
		OpenApiJsonSchema schema = new OpenApiJsonSchema();
		schema.set$ref(getPathInComponents(schemaId));
		return schema;
	}

	/**
	 * Returns whether a property in an ObjectSchema is referencing the ObjectSchema itself.
	 * 
	 * @param property the property in question
	 * @return true if the property is referencing the original ObjectSchema, false otherwise.
	 */
	boolean isSelfReferencing(ObjectSchema property) {
		if (property.get$recursiveRef() == null) {
			return false;
		}
		return property.get$recursiveRef().equals("#");
	}

	/**
	 * Returns if the given TYPE is primitive.
	 * 
	 * @param type - the TYPE in question
	 * @return true if 'type' is primitive, false otherwise.
	 */
	boolean isPrimitive(TYPE type) {
		return type.equals(TYPE.BOOLEAN) || type.equals(TYPE.NUMBER) || type.equals(TYPE.STRING)
				|| type.equals(TYPE.INTEGER);
	}

	/**
	 * Inserts the oneOf properties into all of the JsonSchemas which represent
	 * interfaces.
	 * 
	 * @param classNameToJsonSchema mapping from class ids to a OpenApiJsonSchema that
	 *                              represents that class.
	 * @param interfaces            the interfaces present in the application mapped
	 *                              to the list of classes that implement them.
	 */
	void insertOneOfPropertyForInterfaces(Map<String, OpenApiJsonSchema> classNameToJsonSchema,
			Map<String, List<TypeReference>> interfaces) {
		for (String className : classNameToJsonSchema.keySet()) {
			if (interfaces.containsKey(className)) {
				Set<TypeReference> implementers = getImplementers(className, interfaces);
				List<JsonSchema> oneOf = new ArrayList<>();
				for (TypeReference implementer : implementers) {
					String id = implementer.getId();
					if (!classNameToJsonSchema.containsKey(id)) {
						throw new IllegalArgumentException(
								"Implementation of " + className + " interface with name " + id + " was not found.");
					}
					OpenApiJsonSchema newSchema = new OpenApiJsonSchema();
					newSchema.set$ref(getPathInComponents(id));
					oneOf.add(newSchema);
				}
				classNameToJsonSchema.get(className).setOneOf(oneOf);
			}
		}
	}

	/**
	 * Generated the path to a class name that exists in the "components" section of
	 * the OpenAPI specification.
	 * 
	 * @param className the className in question
	 * @return the path in the componenets section where the className exists.
	 */
	String getPathInComponents(String className) {
		ValidateArgument.required(className, "className");
		return "#/components/schemas/" + className;
	}

	/**
	 * Recursively gets all of the concrete implementers of the given interface.
	 * 
	 * @param interfaceId the id of the interface
	 * @param interfaces  a mapping between all interfaces and their implementers
	 * @return a set of all of the concrete implementers of the interface
	 */
	Set<TypeReference> getImplementers(String interfaceId, Map<String, List<TypeReference>> interfaces) {
		Set<TypeReference> allImplementers = new HashSet<>();

		List<TypeReference> implementers = interfaces.get(interfaceId);
		for (TypeReference implementer : implementers) {
			String implementerId = implementer.getId();
			boolean isInterface = interfaces.containsKey(implementerId);
			if (isInterface) {
				// add all of the classes that implement this interface
				Set<TypeReference> currentImplementers = getImplementers(implementerId, interfaces);
				allImplementers.addAll(currentImplementers);
			} else {
				allImplementers.add(implementer);
			}
		}

		return allImplementers;
	}

	/**
	 * Translates the TYPE enum used for ObjectSchema to type used for OpenApiJsonSchema
	 * 
	 * @param type the ObjectSchema type being translated
	 * @return the equivalent OpenApiJsonSchema type.
	 */
	Type translateObjectSchemaTypeToJsonSchemaType(TYPE type) {
		ValidateArgument.required(type, "type");
		switch (type) {
		case NULL:
			return Type._null;
		case ARRAY:
			return Type.array;
		case OBJECT:
		case INTERFACE:
			return Type.object;
		case STRING:
			return Type.string;
		default:
			throw new IllegalArgumentException("Unable to convert non-primitive type " + type);
		}
	}

	/**
	 * Constructs a OpenApiJsonSchema for a primitive class.
	 * 
	 * @param type - the primitive type
	 * @return the OpenApiJsonSchema used to represent this type.
	 */
	OpenApiJsonSchema getSchemaForPrimitiveType(TYPE type) {
		ValidateArgument.required(type, "type");
		OpenApiJsonSchema schema = new OpenApiJsonSchema();
		switch (type) {
		case STRING:
			schema.setType(Type.string);
			break;
		case INTEGER:
			schema.setType(Type.integer);
			schema.setFormat("int32");
			break;
		case NUMBER:
			schema.setType(Type.number);
			break;
		case BOOLEAN:
			schema.setType(Type._boolean);
			break;
		default:
			throw new IllegalArgumentException("Unable to translate primitive type " + type);
		}
		return schema;
	}
}
