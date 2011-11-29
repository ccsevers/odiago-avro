// (c) Copyright 2011 Odiago, Inc.

package org.apache.avro.io;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.mapred.AvroWrapper;
import org.junit.Test;

public class TestAvroKeyDeserializer {
  @Test
  public void testDeserialize() throws IOException {
    // Create a deserializer.
    Schema writerSchema = Schema.create(Schema.Type.STRING);
    Schema readerSchema = Schema.create(Schema.Type.STRING);
    AvroKeyDeserializer<CharSequence> deserializer
        = new AvroKeyDeserializer<CharSequence>(writerSchema, readerSchema);

    // Check the schemas.
    assertEquals(writerSchema, deserializer.getWriterSchema());
    assertEquals(readerSchema, deserializer.getReaderSchema());

    // Write some records to deserialize.
    DatumWriter<CharSequence> datumWriter = new GenericDatumWriter<CharSequence>(writerSchema);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Encoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
    datumWriter.write("record1", encoder);
    datumWriter.write("record2", encoder);
    encoder.flush();

    // Deserialize the records.
    ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
    deserializer.open(inputStream);
    AvroWrapper<CharSequence> record = null;

    record = deserializer.deserialize(record);
    assertEquals("record1", record.datum().toString());

    record = deserializer.deserialize(record);
    assertEquals("record2", record.datum().toString());

    deserializer.close();
  }
}
