// (c) Copyright 2011 Odiago, Inc.

package org.apache.avro.mapreduce;

import java.io.File;
import java.io.IOException;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.DatumWriter;

/**
 * A utility class for working with Avro container files within tests.
 */
public final class AvroFiles {
  private AvroFiles() {}

  /**
   * Creates an avro container file.
   *
   * @param file The file to create.
   * @param schema The schema for the records the file should contain.
   * @param records The records to put in the file.
   * @param <T> The (java) type of the avro records.
   * @return The created file.
   */
  public static <T> File createFile(File file, Schema schema, T... records)
      throws IOException {
    DatumWriter<T> datumWriter = new GenericDatumWriter<T>(schema);
    DataFileWriter<T> fileWriter = new DataFileWriter<T>(datumWriter);
    fileWriter.create(schema, file);
    for (T record : records) {
      fileWriter.append(record);
    }
    fileWriter.close();

    return file;
  }
}
