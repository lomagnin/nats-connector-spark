/*******************************************************************************
 * Copyright (c) 2016 Logimethods
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License (MIT)
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *******************************************************************************/
package com.logimethods.connector.spark.to_nats;

import static com.logimethods.connector.nats.spark.UnitTestUtilities.NATS_SERVER_URL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Level;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.logimethods.connector.nats.spark.StandardNatsSubscriber;
import com.logimethods.connector.nats.spark.TestClient;
import com.logimethods.connector.nats.spark.UnitTestUtilities;

//@Ignore
@SuppressWarnings("serial")
public class SparkToStandardNatsConnectorTimeoutTest extends AbstractSparkToNatsConnectorTest {

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// Enable tracing for debugging as necessary.
		Level level = Level.TRACE;
		UnitTestUtilities.setLogLevel(SparkToNatsConnectorPool.class, level);
		UnitTestUtilities.setLogLevel(SparkToNatsConnector.class, level);
		UnitTestUtilities.setLogLevel(SparkToStandardNatsConnectorImpl.class, level);
		UnitTestUtilities.setLogLevel(SparkToStandardNatsConnectorTimeoutTest.class, level);
		UnitTestUtilities.setLogLevel(TestClient.class, level);
		UnitTestUtilities.setLogLevel("org.apache.spark", Level.WARN);
		UnitTestUtilities.setLogLevel("org.spark-project", Level.WARN);
	
		logger = LoggerFactory.getLogger(SparkToStandardNatsConnectorTimeoutTest.class);       
		
		UnitTestUtilities.startDefaultServer();
	}

	@Test(timeout=20000)
	public void testStaticSparkToNatsWithConnectionTimeout() throws Exception {  
		long poolSize = SparkToStandardNatsConnectorPool.poolSize();
		
		final List<String> data = getData();

		final String subject1 = "subject1";

		final String subject2 = "subject2";

		final int partitionsNb = 2;
		final JavaDStream<String> lines = ssc.textFileStream(tempDir.getAbsolutePath()).repartition(partitionsNb);		
		
		SparkToNatsConnectorPool.newPool()
			.withSubjects(DEFAULT_SUBJECT, subject1, subject2)
			.withNatsURL(NATS_SERVER_URL)
			.withConnectionTimeout(Duration.ofSeconds(partitionsNb))
			.publishToNats(lines);
		
		ssc.start();

		TimeUnit.SECONDS.sleep(1);

		final StandardNatsSubscriber ns1 = getStandardNatsSubscriber(data, subject1);
		final StandardNatsSubscriber ns2 = getStandardNatsSubscriber(data, subject2);
		writeTmpFile(data);
		// wait for the subscribers to complete.
		ns1.waitForCompletion();
		ns2.waitForCompletion();
		assertEquals("The connections Pool size should be the same a the number of Spark partitions", 
					partitionsNb, SparkToStandardNatsConnectorPool.poolSize());
				
		final StandardNatsSubscriber ns1p = getStandardNatsSubscriber(data, subject1);
		final StandardNatsSubscriber ns2p = getStandardNatsSubscriber(data, subject2);
		writeTmpFile(data);
		// wait for the subscribers to complete.
		ns1p.waitForCompletion();
		ns2p.waitForCompletion();
		assertEquals("The connections Pool size should be the same a the number of Spark partitions", 
					partitionsNb, SparkToStandardNatsConnectorPool.poolSize());

		ssc.stop();
		ssc = null;
		
		logger.debug("Spark Context Stopped");
		
		assertTrue("The pool size should have been increased from " + poolSize, SparkToStandardNatsConnectorPool.poolSize() > poolSize);
		
System.out.println("DATE:  " + new Date());
		TimeUnit.SECONDS.sleep(5);
System.out.println("DATE:  " + new Date());
		logger.debug("After 5 sec delay");
		
		assertTrue("The pool size should have been reduced to its original value", SparkToStandardNatsConnectorPool.poolSize() == poolSize);
	}
}
