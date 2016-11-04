package com.logimethods.connector.nats.to_spark;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.function.Function;

import org.apache.spark.storage.StorageLevel;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class NatsToSparkConnectorTest implements Serializable {
    @Rule
    public ExpectedException thrown= ExpectedException.none();

	@Test
	public void testExtractDataByteArray_String() {
		StandardNatsToSparkConnectorImpl<String> connector = 
				NatsToSparkConnector
					.receiveFromNats(String.class, StorageLevel.MEMORY_ONLY());
		
		String str = "A small piece of Text!";
		byte[] bytes = str.getBytes();
		assertEquals(str, connector.extractData(bytes));
	}

	@Test
	public void testExtractDataByteArray_Float() {
		StandardNatsToSparkConnectorImpl<Float> connector = 
				NatsToSparkConnector
					.receiveFromNats(Float.class, StorageLevel.MEMORY_ONLY());
		
		Float f = 1234324234.34f;
		byte[] bytes = ByteBuffer.allocate(Float.BYTES).putFloat(f).array();
		assertEquals(f, connector.extractData(bytes));
	}

	@Test
	public void testPublicExtractDataByteArray_Float() {
		Float f = 1234324234.34f;
		byte[] bytes = ByteBuffer.allocate(Float.BYTES).putFloat(f).array();
		assertEquals(f, NatsToSparkConnector.extractData(Float.class, bytes));
	}

	@Test
	public void testExtractDataByteArray_Exception() {		
		thrown.expect(UnsupportedOperationException.class);
		
		StandardNatsToSparkConnectorImpl<NatsToSparkConnectorTest> connector = 
				NatsToSparkConnector
					.receiveFromNats(NatsToSparkConnectorTest.class, StorageLevel.MEMORY_ONLY());
		
		byte[] bytes = "xxxx".getBytes();
		connector.extractData(bytes);
	}

	@Test
	public void testExtractDataByteArray_DataExtractor() throws IOException {
		final Function<byte[], Dummy> dataExtractor = bytes -> {
			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
			ObjectInput in = null;
			Object o = null;
			try {
				try {
					in = new ObjectInputStream(bis);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				o = in.readObject();
			} catch (ClassNotFoundException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					if (in != null) {
						in.close();
					}
				} catch (IOException ex) {
					// ignore close exception
				}
			}
			return (Dummy) o;

		};
		StandardNatsToSparkConnectorImpl<Dummy> connector = 
				NatsToSparkConnector
					.receiveFromNats(Dummy.class, StorageLevel.MEMORY_ONLY())
					.withDataExtractor(dataExtractor);

		Dummy dummy = new Dummy("Name");
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = new ObjectOutputStream(bos);   
		out.writeObject(dummy);
		out.flush();
		byte[] bytes = bos.toByteArray();
		bos.close();
		
		assertEquals(dummy, connector.extractData(bytes));
	}
}
