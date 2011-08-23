// (c) Copyright 2011 Odiago, Inc.

package org.apache.avro.mapreduce;

import java.io.IOException;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.SeekableInput;
import org.apache.avro.io.DatumReader;
import org.apache.avro.mapred.AvroKey;
import org.apache.avro.mapred.FsInput;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads records from an input split representing a chunk of an Avro container file.
 *
 * @param <T> The (java) type of data in Avro container file.
 */
public class AvroKeyRecordReader<T> extends RecordReader<AvroKey<T>, NullWritable> {
  private static final Logger LOG = LoggerFactory.getLogger(AvroKeyRecordReader.class);

  /** The reader schema to use for the records in the Avro container file. */
  private final Schema mReaderSchema;

  /** A reusable object to hold records of the Avro container file. */
  private final AvroKey<T> mCurrentRecord;

  /** A reader for the Avro container file containing the current input split. */
  private DataFileReader<T> mAvroFileReader;

  /**
   * The byte offset into the Avro container file where the first block that fits
   * completely within the current input split begins.
   */
  private long mStartPosition;

  /** The byte offset into the Avro container file where the current input split ends. */
  private long mEndPosition;

  /**
   * Constructor.
   *
   * @param readerSchema The reader schema to use for the records in the Avro container file.
   */
  public AvroKeyRecordReader(Schema readerSchema) {
    mReaderSchema = readerSchema;
    mCurrentRecord = new AvroKey<T>(null);
  }

  /** {@inheritDoc} */
  @Override
  public void initialize(InputSplit inputSplit, TaskAttemptContext context)
      throws IOException, InterruptedException {
    if (!(inputSplit instanceof FileSplit)) {
      throw new IllegalArgumentException("Only compatible with FileSplits.");
    }
    FileSplit fileSplit = (FileSplit) inputSplit;

    // Open a seekable input stream to the Avro container file.
    SeekableInput seekableFileInput
        = createSeekableInput(context.getConfiguration(), fileSplit.getPath());

    // Wrap the seekable input stream in an Avro DataFileReader.
    mAvroFileReader = createAvroFileReader(seekableFileInput,
        new SpecificDatumReader<T>(mReaderSchema));

    // Initialize the start and end offsets into the file based on the boundaries of the
    // input split we're responsible for.  We will read the first block that begins
    // after the input split start boundary.  We will read up to but not including the
    // first block that starts after input split end boundary.

    // Sync to the closest block/record boundary just after beginning of our input split.
    mAvroFileReader.sync(fileSplit.getStart());

    // Initialize the start position to the beginning of the first block of the input split.
    mStartPosition = mAvroFileReader.previousSync();

    // Initialize the end position to the end of the input split (this isn't necessarily
    // on a block boundary so using this for reporting progress will be approximate.
    mEndPosition = fileSplit.getStart() + fileSplit.getLength();
  }

  /** {@inheritDoc} */
  @Override
  public boolean nextKeyValue() throws IOException, InterruptedException {
    assert null != mAvroFileReader;

    if (mAvroFileReader.hasNext() && !mAvroFileReader.pastSync(mEndPosition)) {
      mCurrentRecord.datum(mAvroFileReader.next(mCurrentRecord.datum()));
      return true;
    }
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public AvroKey<T> getCurrentKey() throws IOException, InterruptedException {
    return mCurrentRecord;
  }

  /** {@inheritDoc} */
  @Override
  public NullWritable getCurrentValue() throws IOException, InterruptedException {
    return NullWritable.get();
  }

  /** {@inheritDoc} */
  @Override
  public float getProgress() throws IOException, InterruptedException {
    assert null != mAvroFileReader;

    if (mEndPosition == mStartPosition) {
      // Trivial empty input split.
      return 0.0f;
    }
    long bytesRead = mAvroFileReader.previousSync() - mStartPosition;
    long bytesTotal = mEndPosition - mStartPosition;
    LOG.debug("Progress: bytesRead=" + bytesRead + ", bytesTotal=" + bytesTotal);
    return Math.min(1.0f, (float) bytesRead / (float) bytesTotal);
  }

  /** {@inheritDoc} */
  @Override
  public void close() throws IOException {
    if (null != mAvroFileReader) {
      try {
        mAvroFileReader.close();
      } finally {
        mAvroFileReader = null;
      }
    }
  }

  /**
   * Creates a seekable input stream to an Avro container file.
   *
   * @param conf The hadoop configuration.
   * @param path The path to the avro container file.
   * @throws IOException If there is an error reading from the path.
   */
  protected SeekableInput createSeekableInput(Configuration conf, Path path)
      throws IOException {
    return new FsInput(path, conf);
  }

  /**
   * Creates an Avro container file reader from a seekable input stream.
   *
   * @param input The input containing the Avro container file.
   * @param datumReader The reader to use for the individual records in the Avro container file.
   * @throws IOException If there is an error reading from the input stream.
   */
  protected DataFileReader<T> createAvroFileReader(
      SeekableInput input, DatumReader<T> datumReader) throws IOException {
    return new DataFileReader<T>(input, datumReader);
  }
}
