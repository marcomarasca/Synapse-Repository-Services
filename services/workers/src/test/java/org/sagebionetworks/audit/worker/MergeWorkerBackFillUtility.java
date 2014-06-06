package org.sagebionetworks.audit.worker;

import org.sagebionetworks.audit.dao.AccessRecordDAOImpl;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
/**
 * This is utility that can be run to merge access records on old stack.
 * 
 * @author John
 *
 */
public class MergeWorkerBackFillUtility {

	/**
	 * 
	 * @param args 0 = aws-accessKey, 1= aws-secretKey, 2=stack number
	 */
	public static void main(String[] args) {
		if(args == null || args.length < 3){
			throw new IllegalArgumentException("args 0 = aws-accessKey, 1= aws-secretKey, 2=stack number");
		}
		AmazonS3Client client = new AmazonS3Client(new BasicAWSCredentials(args[0],args[1]));
		AccessRecordDAOImpl dao = new AccessRecordDAOImpl();
		ReflectionTestUtils.setField(dao, "s3Client", client);
		dao.setStackInstanceNumber(Integer.parseInt(args[2]));
		dao.setAuditRecordBucketName("prod.access.record.sagebase.org");
		dao.initialize();
		MergeWorker worker = new MergeWorker(dao);
		worker.mergeOneBatch();
	}

}
