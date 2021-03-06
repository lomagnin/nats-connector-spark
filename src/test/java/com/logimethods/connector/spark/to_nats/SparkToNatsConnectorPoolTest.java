/*******************************************************************************
 * Copyright (c) 2016 Logimethods
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License (MIT)
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *******************************************************************************/
package com.logimethods.connector.spark.to_nats;

import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.logimethods.connector.nats_spark.IncompleteException;
import com.logimethods.connector.spark.to_nats.SparkToNatsConnector;
import com.logimethods.connector.spark.to_nats.SparkToNatsConnectorPool;

import static com.logimethods.connector.nats_spark.Constants.*;
import static io.nats.client.Options.*;

public class SparkToNatsConnectorPoolTest {

	private static final String SUBJECTS = "SUB, SUB";
	private static final String URL = "nats://localhost:4333";
	static final String clusterID = "test-cluster"; //"my_test_cluster";
	protected static final String DEFAULT_SUBJECT = "spark2natsStreamingSubject";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test(expected=IncompleteException.class)
	public void testEmptySetProperties() throws Exception {
		final Properties properties = new Properties();
		SparkToNatsConnectorPool.newPool().withProperties(properties).getConnector();
	}

	@Test
	public void testSetProperties() throws Exception {
		final Properties properties = new Properties();
		properties.setProperty(PROP_SUBJECTS, SUBJECTS);
		properties.setProperty(PROP_URL, URL);
		final SparkToNatsConnector<?> connector = SparkToNatsConnectorPool.newPool().withProperties(properties).getConnector();

		assertEquals(2, connector.getSubjects().size());
		assertEquals("SUB", connector.getSubjects().toArray()[0]);

		assertEquals(URL, connector.getNatsURL());
	}
    
    @Test()
    public void testStreamingSparkToNatsWithFilledPropertiesPublish() throws Exception {
		final Properties properties = new Properties();
		properties.setProperty(PROP_SUBJECTS, "sub1,"+DEFAULT_SUBJECT+" , sub2");
		final SparkToNatsConnectorPool<?> connectorPool = SparkToNatsConnectorPool.newStreamingPool(clusterID).withProperties(properties);
		final SparkToNatsConnector<?> connector = connectorPool.getConnector();
		assertEquals(3, connector.getSubjects().size());
    }

}
