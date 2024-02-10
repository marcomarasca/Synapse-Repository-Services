package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class BeanTest implements ApplicationContextAware {

	ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	private static final List<String> EXCEPTIONS = Lists.newArrayList(
			"org.springframework.transaction.annotation.AnnotationTransactionAttributeSource",
			"org.springframework.transaction.interceptor.TransactionInterceptor",
			"org.springframework.aop.support.DefaultBeanFactoryPointcutAdvisor");
	private static final Pattern UNNAMED_BEAN_PATTERN = Pattern.compile("^(.*)#[0-9]+$");
	
	// The semaphore uses the standard transactional annotation
	private static final Set<String> TRANSACTIONAL_EXCEPTIONS = Set.of(
		"refreshLockTimeout", "releaseLock", "attemptToAcquireLock", "runGarbageCollection"
	);

	@Test
	public void testNoUnnamedBeans() {
		List<String> foundBeans = Lists.newLinkedList();
		for (String beanName : applicationContext.getBeanDefinitionNames()) {
			Matcher matcher = UNNAMED_BEAN_PATTERN.matcher(beanName);
			if (matcher.matches() && !EXCEPTIONS.contains(matcher.group(1))) {
				foundBeans.add(beanName);
			}
		}
		assertEquals("", StringUtils.join(foundBeans, ","), "Found beans without name/id. Either give the bean a name/id or add to exceptions in the test, otherwise Spring will not guarantee that the bean is a singleton");
	}

	@Test
	public void testTransactionalNotUsed() {
		// Transactional is not used anymore, use @WriteTransaction, @NewWriteTransaction or @MandatoryWriteTransaction
		Reflections reflections = new Reflections("org.sagebionetworks", Scanners.MethodsAnnotated, Scanners.TypesAnnotated);
		assertEquals(0, reflections.getTypesAnnotatedWith(Transactional.class).size());
		assertEquals(0, reflections.getMethodsAnnotatedWith(Transactional.class).stream().filter(m -> !TRANSACTIONAL_EXCEPTIONS.contains(m.getName())).count());
	}
}
