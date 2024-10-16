package org.sagebionetworks.javadoc.velocity.controller;

import static org.sagebionetworks.repo.web.PathConstants.PATH_REGEX;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.xml.stream.events.EndElement;

import org.jsoup.Jsoup;
import org.sagebionetworks.javadoc.velocity.schema.SchemaUtils;
import org.sagebionetworks.javadoc.web.services.FilterUtils;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.web.RequiredScope;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Lists;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.StartElementTree;
import com.sun.source.doctree.EndElementTree;
import com.sun.source.doctree.AttributeTree;

import jdk.javadoc.doclet.DocletEnvironment;

public class ControllerUtils {
	
	public static String REQUEST_MAPPING_VALUE = RequestMapping.class.getName()+".value";
	public static String REQUEST_MAPPING_METHOD = RequestMapping.class.getName()+".method";
	public static String REQUEST_PARAMETER_VALUE = RequestParam.class.getName()+".value";
	public static String REQUEST_HEADER_VALUE = RequestHeader.class.getName()+".value";
	public static String REQUEST_PARAMETER_REQUIRED = RequestParam.class.getName()+".required";
	
	public static String CONTROLLER_INFO_DISPLAY_NAME = ControllerInfo.class.getName()+".displayName";
	public static String CONTROLLER_INFO_PATH = ControllerInfo.class.getName()+".path";
	
	public static int MAX_SHORT_DESCRIPTION_LENGTH = 150;
	public static String ELLIPSES = "&#8230";
	
	/**
	 * Translate from a a controller class to a Controller model.
	 * @param docletEnvironment 
	 * 
	 * @param typeElement
	 * @return
	 */
	public static ControllerModel translateToModel(DocletEnvironment docletEnvironment, TypeElement typeElement){
		ControllerModel model = new ControllerModel();
		// Setup the basic data
		model.setName(typeElement.getSimpleName().toString());
		model.setClassDescription(docletEnvironment.getElementUtils().getDocComment(typeElement));
		model.setFullClassName(typeElement.getQualifiedName().toString());
		// Map the annotations of the class
		Map<String, Object> annotationMap = mapAnnotation(typeElement.getAnnotationMirrors());
		// Get the display name and path if they exist
		model.setDisplayName((String) annotationMap.get(CONTROLLER_INFO_DISPLAY_NAME));
		model.setPath((String)annotationMap.get(CONTROLLER_INFO_PATH));
    	Iterator<ExecutableElement> methodIt = FilterUtils.requestMappingIterator(typeElement);
    	List<MethodModel> methods = new LinkedList<MethodModel>();
    	model.setMethods(methods);
    	while(methodIt.hasNext()){
    		ExecutableElement ExecutableElement = methodIt.next();
    		MethodModel methodModel = translateMethod(docletEnvironment, ExecutableElement);
    		methods.add(methodModel);
    	}
		return model;
	}

	public static MethodModel translateMethod(DocletEnvironment docletEnvironment, ExecutableElement executableElement) {
		MethodModel methodModel = new MethodModel();
		// Process the method annotations.
		processMethodAnnotations(docletEnvironment, executableElement, methodModel);
		// Now process the parameters
		processParameterAnnotations(docletEnvironment, executableElement, methodModel);
		DocCommentTree commentTree = docletEnvironment.getDocTrees().getDocCommentTree(executableElement);
		if(commentTree != null) {
			methodModel.setDescription(extractText(commentTree.getFullBody()));
		}else {
			methodModel.setDescription("");
		}
		String truncated = createTruncatedText(MAX_SHORT_DESCRIPTION_LENGTH, methodModel.getDescription());
		methodModel.setShortDescription(truncated);
		// remove regular expressions
		String urlDisplay = methodModel.getUrl().replaceAll(PATH_REGEX, "").replace("*", "");
		methodModel.setUrl(urlDisplay);
		String fullNameSuffix = urlDisplay.replaceAll("[\\{\\}]", "").replaceAll("/", ".");
		String fullName = methodModel.getHttpType() + fullNameSuffix;
		methodModel.setFullMethodName(fullName);
		Link methodLink = new Link("${" + fullName + "}", methodModel.getHttpType() + " " + urlDisplay);
		methodModel.setMethodLink(methodLink);
		return methodModel;
	}

	/**
	 * Create a text string that is shorter than the max number of characters.
	 * @param maxChars
	 * @param text
	 * @return
	 */
	public static String createTruncatedText(int maxChars, String text){
		if(text == null) return null;
		// Strip out all HTML text from the description.
		// If we do not remove the HTML, we could truncate in the middle of a tag
		// which would break the final HTML.
		text = Jsoup.parse(text).text();
		if(text.length() < maxChars) return text;
		// We need to ensure that we do not cut on any HTML tags
		StringBuilder builder = new StringBuilder();
		for(int i=0; i<maxChars; i++){
			char ch = text.charAt(i);
			builder.append(ch);
		}
		builder.append(ELLIPSES);
		return builder.toString();
	}
	
	private static void processParameterAnnotations(DocletEnvironment env, ExecutableElement executableElement, MethodModel methodModel) {
		var params = executableElement.getParameters();
		// Start with false here.  If we find the userId parameter this will be changed to true.
		methodModel.setIsAuthenticationRequired(false);
		Map<String, ParameterModel> paramMap = new HashMap<String, ParameterModel>();
        if(params != null){
        	for(VariableElement param: params){
        		var paramAnnos = param.getAnnotationMirrors();
        		if(paramAnnos != null){
        			for(AnnotationMirror ad: paramAnnos){
        				String qualifiedName = ad.getAnnotationType().toString();
        				Map<String, Object> annotationMap = mapAnnotation(ad);
        				if(RequestBody.class.getName().equals(qualifiedName)){
        					// Request body
        					String schema = SchemaUtils.getEffectiveSchema(param.asType().toString());
        					if(schema != null){
								TypeMirror paramType = param.asType();
								TypeElement paramElelmentType = env.getElementUtils().getTypeElement(paramType.toString());
								if (!paramElelmentType.getTypeParameters().isEmpty()) {
									Link paramLink = new Link("${" + paramType.toString() + "}", paramElelmentType.getSimpleName().toString());
									methodModel.setRequestBody(paramLink);

									List<Link> genericParameters = Lists.newArrayList();
									for (TypeParameterElement type : paramElelmentType.getTypeParameters()) {
										Link link = new Link();
										link.setHref("${" + type.toString() + "}");
										link.setDisplay(type.getSimpleName().toString());
										genericParameters.add(link);
									}
									methodModel.setRequestBodyGenericParams(genericParameters.toArray(new Link[] {}));
								} else {
									Link paramLink = new Link("${" + paramType.toString() + "}", paramElelmentType.getSimpleName().toString());
									methodModel.setRequestBody(paramLink);
								}
							}
        				}else if(PathVariable.class.getName().equals(qualifiedName)){
        					// Path parameter
        					ParameterModel paramModel = new ParameterModel();
        					paramModel.setName(param.toString());
        					methodModel.addPathVariable(paramModel);
        					paramMap.put(param.toString(), paramModel);
        				}else if(RequestParam.class.getName().equals(qualifiedName)){
        					// if this is the userId parameter then we do not show it,
        					// rather it means this method requires authentication.
        					if(AuthorizationConstants.USER_ID_PARAM.equals(annotationMap.get(REQUEST_PARAMETER_VALUE))){
        						methodModel.setIsAuthenticationRequired(true);
        					}else{
            					ParameterModel paramModel = new ParameterModel();
            					// If the annotation has a value then it should be used as the name.
            					String requestParameterValue = (String)annotationMap.get(REQUEST_PARAMETER_VALUE);
            					if(requestParameterValue != null){
            						paramModel.setName(requestParameterValue);
            					}else{
            						paramModel.setName(param.getSimpleName().toString());
            					}
            					paramModel.setIsOptional(!(isRequired(annotationMap)));
            					methodModel.addParameter(paramModel);
            					paramMap.put(param.getSimpleName().toString(), paramModel);
        					}
        				} else if (RequestHeader.class.getName().equals(qualifiedName)) {
        					// if this is the authorization header we do not show it,
        					// rather it means that this method requires authentication
        					if (AuthorizationConstants.SYNAPSE_AUTHORIZATION_HEADER_NAME.equals(annotationMap.get(REQUEST_HEADER_VALUE))) {
        						methodModel.setIsAuthenticationRequired(true);
        					}
        				}
        			}
        		}
        	}
        }
        
        // Find the text associated with each method parameter
        DocCommentTree docTree = env.getDocTrees().getDocCommentTree(executableElement);
        if(docTree != null) {
        	docTree.getBlockTags().stream().filter(t-> t instanceof ParamTree ).map(t->(ParamTree)t).forEach(t->{
        		ParameterModel paramModel = paramMap.get(t.getName().toString());
        		if(paramModel != null) {
        			paramModel.setDescription(extractText(t.getDescription()));
        		}
        	});
        }
	}

	public static String extractText(List<? extends DocTree> tree) {
		StringBuilder sb = new StringBuilder();
		tree.forEach(t -> {
			if (t instanceof TextTree) {
				sb.append(((TextTree) t).getBody());
			} else if (t instanceof StartElementTree) {
				sb.append((StartElementTree) t);
			} else if (t instanceof EndElementTree) {
				sb.append((EndElementTree) t);
			} else {
				System.out.println("Unknown type: " + t.getClass().toString());
			}
		});
		return sb.toString();
	}


	private static void processMethodAnnotations(DocletEnvironment env, ExecutableElement executableElement,
			MethodModel methodModel) {
		var annos = executableElement.getAnnotationMirrors();
        if(annos != null){
        	for (AnnotationMirror ad: annos) {
        		String qualifiedName = ad.getAnnotationType().toString();
        	if (RequestMapping.class.getName().equals(qualifiedName)){
        			extractRequestMapping(methodModel, ad);
        		} else if (ResponseBody.class.getName().equals(qualifiedName)) {
					extractResponseLink(env, executableElement, methodModel);
        		} else if (RequiredScope.class.getName().equals(qualifiedName)) {
        			extractRequiredScope(methodModel, ad);
        		}
        	}
        }
	}
	
	private static void extractRequiredScope(MethodModel methodModel, AnnotationMirror ad) {
		List<String> requiredScopes = new ArrayList<String>();
		ad.getElementValues().forEach((k,v)->{
			if(v.getValue() != null) {
				Collection<?> coll = (Collection<?>) v.getValue();
				coll.forEach(s->{
					String rawValue = s.toString();
					if(rawValue.startsWith(OAuthScope.class.getName())) {
						int i = OAuthScope.class.getName().toString().length();
						requiredScopes.add(s.toString().substring(i+1));
					}else {
						requiredScopes.add(rawValue);
					}
				});
			}
		});
		if (requiredScopes.isEmpty()) {
			methodModel.setRequiredScopes(null);
		} else {
			methodModel.setRequiredScopes(requiredScopes.toArray(new String[] {}));
		}
	}	

	private static void extractResponseLink(DocletEnvironment env, ExecutableElement executableElement, MethodModel methodModel) {
		// If the return type is a generic, erasure() will return the outside class.
		TypeElement returnType = env.getElementUtils()
				.getTypeElement(env.getTypeUtils().erasure(executableElement.getReturnType()).toString());
		if(returnType == null) {
			return;
		}
		String schema = SchemaUtils.getEffectiveSchema(returnType.getQualifiedName().toString());
		if(schema == null) {
			return;
		}
		
		Link responseLink = new Link();
		responseLink.setHref("${" + returnType.getQualifiedName().toString() + "}");
		responseLink.setDisplay(returnType.getSimpleName().toString());
		methodModel.setResponseBody(responseLink);
		
		if(executableElement.getReturnType() instanceof DeclaredType) {
			DeclaredType returnDeclared = (DeclaredType) executableElement.getReturnType();
			List<Link> genericParameters = Lists.newArrayList();
			returnDeclared.getTypeArguments().forEach(a->{
				TypeElement paramType = env.getElementUtils().getTypeElement(a.toString());
				Link link = new Link();
				link.setHref("${" + paramType.getQualifiedName().toString() + "}");
				link.setDisplay(paramType.getSimpleName().toString());
				genericParameters.add(link);
			});
			if(!genericParameters.isEmpty()) {
				methodModel.setResponseBodyGenericParams(genericParameters.toArray(new Link[] {}));
			}
		}
	}

	/**
	 * Extract the request mapping data from the annotation.
	 * @param methodModel
	 * @param ad
	 */
	private static void extractRequestMapping(MethodModel methodModel, AnnotationMirror ad) {
		ad.getElementValues().forEach((k,v)->{
			if("value".equals(k.getSimpleName().toString())) {
				String rawValue = v.getValue().toString();
				if(rawValue!= null){
		    		methodModel.setUrl(rawValue.substring(1, rawValue.length()-1));
				}
			}else if("method".equals(k.getSimpleName().toString())){
				String rawValue = v.getValue().toString();
				if(rawValue.startsWith(RequestMethod.class.getName())) {
					int inxed = RequestMethod.class.getName().length();
					methodModel.setHttpType(v.getValue().toString().substring(inxed+1));
				}else {
					methodModel.setHttpType(rawValue);
				}

			}
		});
	}
	
	/**
	 * Put all annotation value key pairs into a map for easier lookup.
	 * @param list
	 * @return
	 */
	public static Map<String, Object> mapAnnotation(List<? extends AnnotationMirror> list) {
		Map<String, Object> map = new HashMap<String, Object>();
		if(list != null){
			for(AnnotationMirror anno: list){
				mapAnnotation(anno, map);
			}
		}
		return map;
	}

	/**
	 * Put all annotation value key pairs into a map for easier lookup.
	 * @param ad
	 * @return
	 */
	public static Map<String, Object> mapAnnotation(AnnotationMirror ad){
		 Map<String, Object> map = new HashMap<String, Object>();
		 mapAnnotation(ad, map);
		 return map;
	}
	
	/**
	 * Put all annotation value key pairs into a map for easier lookup.
	 * @param ad
	 * @param map
	 */
	public static void mapAnnotation(AnnotationMirror ad, Map<String, Object> map){
		 var pairs = ad.getElementValues();
		 pairs.forEach((k,v)->{
			 map.put(ad.getAnnotationType().toString()+"."+ k.getSimpleName().toString(), v.getValue());
		 });
	}
	/**
	 * Check for the required annotation.
	 * @param map
	 * @return
	 */
	public static boolean isRequired(Map<String, Object> map){
		Boolean value = (Boolean) map.get(REQUEST_PARAMETER_REQUIRED);
		if(value != null){
			return value;
		}else{
			return false;
		}
	}
}
