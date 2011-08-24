// (c) Copyright 2011 Odiago, Inc.

package org.apache.avro.mapreduce;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.avro.Schema;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.mapred.AvroKey;
import org.apache.avro.mapred.AvroOutputFormat;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 * FileOutputFormat for writing Avro container files.
 *
 * <p>Since Avro container files only contain records (not key/value pairs), this output
 * format ignores the value.</p>
 *
 * @param <T> The (java) type of the Avro data to write.
 */
public class AvroKeyOutputFormat<T> extends FileOutputFormat<AvroKey<T>, NullWritable> {
  /** A factory for creating record writers. */
  private final RecordWriterFactory mRecordWriterFactory;

  /**
   * Constructor.
   */
  public AvroKeyOutputFormat() {
    this(new RecordWriterFactory());
  }

  /**
   * Constructor.
   *
   * @param recordwriterfactory A factory for creating record writers.
   */
  protected AvroKeyOutputFormat(RecordWriterFactory recordWriterFactory) {
    mRecordWriterFactory = recordWriterFactory;
  }

  /**
   * A factory for creating record writers.
   *
   * @param <T> The java type of the avro record to write.
   */
  protected static class RecordWriterFactory<T> {
    /**
     * Creates a new record writer instance.
     *
     * @param writerSchema The writer schema for the records to write.
     * @param compressionCodec The compression type for the writer file.
     * @param outputStream The target output stream for the records.
     */
    protected RecordWriter<AvroKey<T>, NullWritable> create(
        Schema writerSchema, CodecFactory compressionCodec, OutputStream outputStream)
        throws IOException {
      return new AvroKeyRecordWriter<T>(writerSchema, compressionCodec, outputStream);
    }
  }

  /** {@inheritDoc} */
  @Override
  public RecordWriter<AvroKey<T>, NullWritable> getRecordWriter(TaskAttemptContext context)
      throws IOException {
    Configuration conf = context.getConfiguration();

    // Get the writer schema.
    Schema writerSchema = AvroJob.getOutputSchema(conf);
    if (null == writerSchema) {
      throw new IOException(
          "AvroOutputFormat requires an output schema. Use AvroJob.setOutputSchema().");
    }

    // Get the compression codec.
    CodecFactory compressionCodec;
    if (FileOutputFormat.getCompressOutput(context)) {
      // Deflate compression.
      int compressionLevel = conf.getInt(
          org.apache.avro.mapred.AvroOutputFormat.DEFLATE_LEVEL_KEY,
          org.apache.avro.mapred.AvroOutputFormat.DEFAULT_DEFLATE_LEVEL);
      compressionCodec = CodecFactory.deflateCodec(compressionLevel);
    } else {
      // No compression.
      compressionCodec = CodecFactory.nullCodec();
    }

    // Create the Avro container file.
    Path path = getDefaultWorkFile(context, org.apache.avro.mapred.AvroOutputFormat.EXT);
    OutputStream outputStream = path.getFileSystem(context.getConfiguration()).create(path);

    return mRecordWriterFactory.create(writerSchema, compressionCodec, outputStream);
  }
}
