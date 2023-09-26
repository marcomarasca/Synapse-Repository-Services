package org.sagebionetworks.repo.manager.file;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.KeyPairUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.PrivateKey;
import java.util.concurrent.TimeUnit;


public class CloudFrontCache {
	@Autowired
	private StackConfiguration config;

	public static final String RSA = "RSA";
	public static final String CLOUDFRONT_PRIVATE_KEY = "CloudFrontPrivateKey";
	public static final String CLOUDFRONT_DOMAIN_NAME = "CloudFrontDomainName";
	public static final String CLOUDFRONT_KEY_PAIR_ID = "CloudFrontKeyPairId";
	public static final int CACHE_EXPIRATION_MINUTES = 5;

	LoadingCache<String, PrivateKey> privateKeyCache = CacheBuilder.newBuilder()
			.expireAfterWrite(CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES)
			.build(	new CacheLoader<>() {
						@Override
						public PrivateKey load(String key) {
							String cloudFrontPrivateKeyString = config.getCloudFrontPrivateKey();
							return KeyPairUtil.getPrivateKeyFromPEM(cloudFrontPrivateKeyString, RSA);
						}
					});

	LoadingCache<String, String> propertyCache = CacheBuilder.newBuilder()
			.expireAfterWrite(CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES)
			.build( new CacheLoader<>() {
						@Override
						public String load(String key) {
							switch (key) {
								case (CLOUDFRONT_DOMAIN_NAME):
									return config.getCloudFrontDomainName();
								case (CLOUDFRONT_KEY_PAIR_ID):
									return config.getCloudFrontKeyPairId();
								default:
									throw new UnsupportedOperationException("Cache key not supported: " + key);
							}

						}
					});

	public PrivateKey getPrivateKey() {
		return privateKeyCache.getUnchecked(CLOUDFRONT_PRIVATE_KEY);
	}

	public String getDomainName() {
		return propertyCache.getUnchecked(CLOUDFRONT_DOMAIN_NAME);
	}

	public String getKeyPairId(){
		return propertyCache.getUnchecked(CLOUDFRONT_KEY_PAIR_ID);
	}
}
