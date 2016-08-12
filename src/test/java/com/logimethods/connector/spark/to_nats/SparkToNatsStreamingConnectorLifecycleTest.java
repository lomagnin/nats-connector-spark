/*******************************************************************************
 * Copyright (c) 2016 Logimethods
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License (MIT)
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *******************************************************************************/
package com.logimethods.connector.spark.to_nats;

import static com.logimethods.connector.nats.spark.UnitTestUtilities.NATS_SERVER_URL;
import static io.nats.client.Constants.PROP_URL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Level;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.logimethods.connector.nats.spark.NatsStreamingSubscriber;
import com.logimethods.connector.nats.spark.STANServer;
import com.logimethods.connector.nats.spark.TestClient;
import com.logimethods.connector.nats.spark.UnitTestUtilities;
import com.logimethods.connector.nats_spark.Utilities;

//@Ignore
@SuppressWarnings("serial")
public class SparkToNatsStreamingConnectorLifecycleTest extends AbstractSparkToNatsConnectorTest {

	static final String clusterID = "test-cluster"; //"my_test_cluster";

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// Enable tracing for debugging as necessary.
		Level level = Level.TRACE;
		UnitTestUtilities.setLogLevel(SparkToNatsConnectorPool.class, level);
		UnitTestUtilities.setLogLevel(SparkToNatsConnector.class, level);
		UnitTestUtilities.setLogLevel(SparkToNatsStreamingConnectorImpl.class, level);
		UnitTestUtilities.setLogLevel(SparkToNatsStreamingConnectorLifecycleTest.class, level);
		UnitTestUtilities.setLogLevel(TestClient.class, level);
		UnitTestUtilities.setLogLevel("org.apache.spark", Level.WARN);
		UnitTestUtilities.setLogLevel("org.spark-project", Level.WARN);
	
		logger = LoggerFactory.getLogger(SparkToNatsStreamingConnectorLifecycleTest.class);       
	}

	@Test(timeout=20000)
	public void testStaticSparkToNatsWithConnectionLifecycle() throws Exception {  
    	runServer(clusterID, false);

    	long poolSize = SparkToNatsStreamingConnectorPool.poolSize();
		
		final List<String> data = getData();

		final String subject1 = "subject1";

		final String subject2 = "subject2";

		final int partitionsNb = 2;
		final JavaDStream<String> lines = ssc.textFileStream(tempDir.getAbsolutePath()).repartition(partitionsNb);		
		
		final Properties properties = new Properties();
		properties.setProperty(PROP_URL, STAN_URL);
		SparkToNatsConnectorPool
			.newStreamingPool(clusterID)
			.withProperties(properties)
			.withConnectionTimeout(Duration.ofSeconds(2))
			.withSubjects(DEFAULT_SUBJECT, subject1, subject2)
			.withConnectionTimeout(Duration.ofSeconds(partitionsNb))
			.publishToNats(lines);
		
		ssc.start();

		TimeUnit.SECONDS.sleep(1);

		final NatsStreamingSubscriber ns1 = getNatsStreamingSubscriber(data, subject1, clusterID, getUniqueClientName() + "_SUB1");
		final NatsStreamingSubscriber ns2 = getNatsStreamingSubscriber(data, subject2, clusterID, getUniqueClientName() + "_SUB1");
		writeTmpFile(data);
		// wait for the subscribers to complete.
		ns1.waitForCompletion();
		ns2.waitForCompletion();
		
		TimeUnit.MILLISECONDS.sleep(100);
System.out.println(SparkToNatsStreamingConnectorPool.connectionsPoolMap);
		assertEquals("The connections Pool size should be the same a the number of Spark partitions", 
				poolSize + partitionsNb, SparkToNatsStreamingConnectorPool.poolSize());
				
		final NatsStreamingSubscriber ns1p = getNatsStreamingSubscriber(data, subject1, clusterID, getUniqueClientName() + "_SUB1");
		final NatsStreamingSubscriber ns2p = getNatsStreamingSubscriber(data, subject2, clusterID, getUniqueClientName() + "_SUB1");
		writeTmpFile(data);
		// wait for the subscribers to complete.
		ns1p.waitForCompletion();
		ns2p.waitForCompletion();
		TimeUnit.MILLISECONDS.sleep(100);
		assertEquals("The connections Pool size should be the same a the number of Spark partitions", 
				poolSize + partitionsNb, SparkToNatsStreamingConnectorPool.poolSize());

		ssc.stop();
		ssc = null;
		
		logger.debug("Spark Context Stopped");
		
		TimeUnit.SECONDS.sleep(5);
		logger.debug("After 5 sec delay");
		
		System.out.println(SparkToNatsStreamingConnectorPool.connectionsPoolMap);
		assertTrue("The pool size should have been reduced to its original value", SparkToNatsStreamingConnectorPool.poolSize() == poolSize);
	}

    static STANServer runServer(String clusterID, boolean debug) {
        STANServer srv = new STANServer(clusterID, STANServerPORT, debug);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return srv;
    }
    
    static String getUniqueClientName() {
    	return "clientName_" + Utilities.generateUniqueID();
    }
}
