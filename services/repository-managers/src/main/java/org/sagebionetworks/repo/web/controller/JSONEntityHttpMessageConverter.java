package org.sagebionetworks.repo.web.controller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.ErrorResponse;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.util.JSONEntityUtil;
import org.sagebionetworks.schema.adapter.JSONArrayAdapter;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONArrayAdapterImpl;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;


public class JSONEntityHttpMessageConverter implements	HttpMessageConverter<JSONEntity> {

	
	private static final String UTF_8 = "UTF-8";
	private static final String CONCRETE_TYPE = "concreteType";
	private static final String ENTITY_TYPE = "entityType";
	private List<MediaType> supportedMedia;
	private Set<Class <? extends JSONEntity>> classesToValidateConversion;
	private static final String VALIDATION_ERROR = "JSON Element in Entity is Unsupported: %s";
	private static final String MISSING_ELEMENT_ERROR = "Missing element in child array of %s element on conversion";
	
	/**
	 * When set to true, this message converter will attempt to convert any object to JSON.
	 */
	boolean convertAnyRequestToJson = false;
	
	/**
	 *  When set to true, this message converter will attempt to convert any object to JSON
	 *  regardless of the requested type.
	 *  
	 * @param convertAnyRequestToJson
	 */
	public void setConvertAnyRequestToJson(boolean convertAnyRequestToJson) {
		this.convertAnyRequestToJson = convertAnyRequestToJson;
	}

	public JSONEntityHttpMessageConverter(Set<Class <? extends JSONEntity>> classesToValidateConversion) {
		ValidateArgument.required(classesToValidateConversion, "classesToValidateConversion");
		supportedMedia = new ArrayList<MediaType>();
		supportedMedia.add(MediaType.APPLICATION_JSON);
		supportedMedia.add(MediaType.TEXT_PLAIN);
		this.classesToValidateConversion = classesToValidateConversion;
	}

	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) {
		// Does the class implement JSONEntity a JSONEntity?
		if(!JSONEntityUtil.isJSONEntity(clazz)) return false;
		// Are we converting any request to json?
		if(convertAnyRequestToJson) return true;
		// Is the requested type a json type?
		return isJSONType(mediaType);
	}

	
	public static boolean isJSONType(MediaType type){
		if(type == null) return false;
		if(type.getType() == null) return false;
		if(type.getSubtype() == null) return false;
		if(!"application".equals(type.getType().toLowerCase())) return false;
		if(!"json".equals(type.getSubtype().toLowerCase())) return false;
		return true;
	}

	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		return MediaType.TEXT_PLAIN.includes(mediaType) || 
				(isJSONType(mediaType) && JSONEntityUtil.isJSONEntity(clazz));
	}

	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return supportedMedia;
	}
	
	
	public static void validateJSONEntity(JSONEntity parsedEntity, String originalJsonString) 
			throws JSONObjectAdapterException {
		// Validating: throws an IllegalArgumentException if the parsedEntity is missing
		// an element from the originalJsonString
		JSONObject parsedEntityJsonObject = EntityFactory.createJSONObjectForEntity(parsedEntity);
		JSONObjectAdapter parsedObject = new JSONObjectAdapterImpl(parsedEntityJsonObject);
		JSONObjectAdapter originalObject = new JSONObjectAdapterImpl(originalJsonString);
		validateJSONEntityRecursive(parsedObject, originalObject);
	}
	
	public static void validateJSONEntityRecursive(JSONObjectAdapter parsedObject, 
			JSONObjectAdapter originalObject) throws JSONObjectAdapterException {
		// throws an IllegalArgumentException if the parsedObject is missing a key that
		// the original object has.
		for (String key : originalObject.keySet()) {
			Object object = originalObject.get(key);
			if (!parsedObject.has(key)) {
				// element is missing, therefore unsupported
				throw new IllegalArgumentException(String.format(VALIDATION_ERROR, key));
			} else if (object instanceof JSONObjectAdapterImpl) {
				// JSON object, so we recurse
				JSONObjectAdapter objectAdapter = (JSONObjectAdapterImpl) object;
				JSONObjectAdapter parsedObjectAdapter = parsedObject.getJSONObject(key);
				validateJSONEntityRecursive(parsedObjectAdapter, objectAdapter);
			} else if (object instanceof JSONArrayAdapterImpl) {
				// if array object, recursively handle it
				JSONArrayAdapter originalArray = (JSONArrayAdapterImpl) object;
				JSONArrayAdapter parsedArray = parsedObject.getJSONArray(key);
				validateJSONArrayRecursive(parsedArray, originalArray, key);
			}
		}
	}
	
	public static void validateJSONArrayRecursive(JSONArrayAdapter parsedArray, 
			JSONArrayAdapter originalArray, String key) throws JSONObjectAdapterException {
		/*
		 * NOTE: we pass in the key as well to indicate the closest parent key of an invalid array
		 * conversion (throws exception on unequal array sizes). this is because we can have a key 
		 * mapping to an array of arrays of arrays, in which the embedded arrays do not have an 
		 * immediate key mapping that we can report. So we should report the closest parent key.
		 */
		if (originalArray.length() != parsedArray.length()) {
			throw new IllegalArgumentException(String.format(MISSING_ELEMENT_ERROR, key));
		}
		for (int i = 0; i < originalArray.length(); i++) {
			// get each element, and recurse accordingly if they are JSONObject or JSONArray
			Object parsedElement = parsedArray.get(i);
			Object originalElement = originalArray.get(i);
			if (parsedElement instanceof JSONObject && originalElement instanceof JSONObject) {
				JSONObjectAdapter originalObjectElement = new JSONObjectAdapterImpl((JSONObject) originalElement);
				JSONObjectAdapter parsedObjectElement = new JSONObjectAdapterImpl((JSONObject) parsedElement);
				validateJSONEntityRecursive(parsedObjectElement, originalObjectElement);
			} else if (parsedElement instanceof JSONArray && originalElement instanceof JSONArray) {
				JSONArrayAdapter nextParsedArray = new JSONArrayAdapterImpl((JSONArray) parsedElement);
				JSONArrayAdapter nextOriginalArray = new JSONArrayAdapterImpl((JSONArray) originalElement);
				validateJSONArrayRecursive(nextParsedArray, nextOriginalArray, key);
			}
		}
	}
	
	// This is specified by HTTP 1.1
	private static final Charset HTTP_1_1_DEFAULT_CHARSET = Charset.forName("ISO-8859-1");
	
	// This is the character set used by Synapse if the client does not specify one
	private static final Charset SYNAPSE_DEFAULT_CHARSET = Charset.forName(UTF_8);

	@Override
	public JSONEntity read(Class<? extends JSONEntity> clazz, HttpInputMessage inputMessage) throws IOException,
			HttpMessageNotReadableException, IllegalArgumentException {
		// First read the string
		Charset charsetForDeSerializingBody = inputMessage.getHeaders().getContentType().getCharset();
		if (charsetForDeSerializingBody==null) {
			// HTTP 1.1 says that the default is ISO-8859-1
			charsetForDeSerializingBody = HTTP_1_1_DEFAULT_CHARSET;
		}
		String jsonString = JSONEntityHttpMessageConverter.readToString(inputMessage.getBody(), charsetForDeSerializingBody);
		try {
			JSONEntity entity = EntityFactory.createEntityFromJSONString(jsonString, clazz);
			// validate the entity if its class is one which we should validate
			if (classesToValidateConversion.contains(clazz)) {
				validateJSONEntity(entity, jsonString);
			}
			return entity;
		} catch (JSONObjectAdapterException e) {
			// Try to convert entity type to a concrete type and try again. See PLFM-2079.
			try {
				JSONObject jsonObject = new JSONObject(jsonString);
				if(jsonObject.has(ENTITY_TYPE)){
					// get the entity type so we can replace it with concrete type
					String type = jsonObject.getString(ENTITY_TYPE);
				jsonObject.remove(ENTITY_TYPE);
					jsonObject.put(CONCRETE_TYPE, type);
					jsonString = jsonObject.toString();
					// try again
					return EntityFactory.createEntityFromJSONString(jsonString, clazz);
				}else{
					// Something else went wrong
					throw new HttpMessageNotReadableException(e.getMessage(), e);
				}
			} catch (JSONException e1) {
				throw new HttpMessageNotReadableException(e1.getMessage(), e);
			} catch (JSONObjectAdapterException e2) {
				throw new HttpMessageNotReadableException(e2.getMessage(), e);
			}
		}
	}

	/**
	 * Read a string from an input stream
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static String readToString(InputStream in, Charset charSet)
			throws IOException {
		if(in == null) throw new IllegalArgumentException("No content to map to Object due to end of input");
		try {
			if(charSet == null){
				charSet = Charset.forName(UTF_8);
			}
			BufferedInputStream bufferd = new BufferedInputStream(in);
			byte[] buffer = new byte[1024];
			StringBuilder builder = new StringBuilder();
			int index = -1;
			while ((index = bufferd.read(buffer, 0, buffer.length)) > 0) {
				builder.append(new String(buffer, 0, index, charSet));
			}
			return builder.toString();
		} finally {
			in.close();
		}
	}
	
	/**
	 * Read a string from an input stream
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static String readToString(Reader reader) throws IOException {
		if(reader == null) throw new IllegalArgumentException("Reader cannot be null");
		try {
			char[] buffer = new char[1024];
			StringBuilder builder = new StringBuilder();
			int index = -1;
			while ((index = reader.read(buffer, 0, buffer.length)) > 0) {
				builder.append(buffer, 0, index);
			}
			return builder.toString();
		} finally {
			reader.close();
		}
	}

	/**
	 * Write a string to an oupt stream
	 * @param toWrite
	 * @param out
	 * @param charSet
	 * @throws IOException
	 */
	public static long writeToStream(String toWrite, OutputStream out,	Charset charSet) throws IOException {
		try {
			if(charSet == null){
				charSet = Charset.forName(UTF_8);
			}
			BufferedOutputStream bufferd = new BufferedOutputStream(out);
			byte[] bytes = toWrite.getBytes(charSet);
			bufferd.write(bytes);
			bufferd.flush();
			return bytes.length;
		} finally {
			out.close();
		}
	}

	@Override
	public void write(JSONEntity entity, final MediaType contentType,
			HttpOutputMessage outputMessage) throws IOException,
			HttpMessageNotWritableException {
		// First write the entity to a JSON string
		try {
			MediaType contentTypeForResponseHeader = contentType;
			if (contentTypeForResponseHeader.isWildcardType() || contentTypeForResponseHeader.isWildcardSubtype()) {
				// this will leave the character set unspecified, but we fill that in below
				contentTypeForResponseHeader = MediaType.APPLICATION_JSON;
			}
			Charset charsetForSerializingBody = contentTypeForResponseHeader.getCharset();
			if (charsetForSerializingBody==null) {
				charsetForSerializingBody = SYNAPSE_DEFAULT_CHARSET;
				// Let's make it explicit in the response header
				contentTypeForResponseHeader = new MediaType(
						contentTypeForResponseHeader.getType(),
						contentTypeForResponseHeader.getSubtype(),
						charsetForSerializingBody
				);
			}
			HttpHeaders headers = outputMessage.getHeaders();
			headers.setContentType(contentTypeForResponseHeader);
			String jsonString;
			if (contentTypeForResponseHeader.includes(MediaType.TEXT_PLAIN)) {
				jsonString = convertEntityToPlainText(entity);
			} else {
				jsonString = EntityFactory.createJSONStringForEntity(entity);
			}
			long length = JSONEntityHttpMessageConverter.writeToStream(jsonString, outputMessage.getBody(), charsetForSerializingBody);
			if (headers.getContentLength() == -1) {
				headers.setContentLength(length);
			}
		} catch (JSONObjectAdapterException e) {
			throw new HttpMessageNotWritableException(e.getMessage(), e);
		}

	}
	
	public static String convertEntityToPlainText(JSONEntity entity) throws JSONObjectAdapterException {
		if (entity instanceof ErrorResponse) {
			return ((ErrorResponse)entity).getReason();
		} else {
			return EntityFactory.createJSONStringForEntity(entity);
		}
	}

	/**
	 * Read an entity from the reader.
	 * @param reader
	 * @return
	 * @throws IOException 
	 * @throws JSONObjectAdapterException 
	 */
	public static Entity readEntity(Reader reader) throws IOException, JSONObjectAdapterException {
		// First read in the string
		String jsonString = readToString(reader);
		// Read it into an adapter
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonString);
		return createEntityFromeAdapter(adapter);
	}

	/**
	 * There are many things that can go wrong with this and we want to make sure the error messages
	 * are always meaningful.
	 * @param adapter
	 * @return
	 * @throws JSONObjectAdapterException
	 */
	public static Entity createEntityFromeAdapter(JSONObjectAdapter adapter)
			throws JSONObjectAdapterException {
		// Get the entity type
		String typeClassName = adapter.getString("concreteType");
		if(typeClassName==null){
			throw new IllegalArgumentException("Cannot determine the entity type.  The entityType property is null");
		}
		// Create a new instance using the full class name
		Entity newInstance = null;
		try {
			// 
			Class<? extends Entity> entityClass = (Class<? extends Entity>) Class.forName(typeClassName);
			newInstance = entityClass.newInstance();
		} catch (Exception e) {
			throw new IllegalArgumentException("Unknown entity type: "+typeClassName+". Message: "+e.getMessage());
		}
		// Populate the new instance with the JSON.
		newInstance.initializeFromJSONObject(adapter);
		return newInstance;
	}

}
