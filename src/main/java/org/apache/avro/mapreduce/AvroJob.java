// (c) Copyright 2010 Odiago, Inc.

package org.apache.avro.mapreduce;

import java.util.Collection;

import org.apache.avro.Schema;
import org.apache.avro.mapred.AvroKey;
import org.apache.avro.mapred.AvroValue;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.conf.Configuration;

/**
 * Utility methods for configuring jobs that work with Avro.
 */
public final class AvroJob {
  private AvroJob() {}

  private static String INPUT_KEY_SCHEMA_CONFIG_FIELD = "avro.schema.input.key";
  private static String INPUT_VALUE_SCHEMA_CONFIG_FIELD = "avro.schema.input.value";
  private static String KEY_MAP_OUTPUT_SCHEMA_CONFIG_FIELD = "avro.schema.mapoutput.key";
  private static String VALUE_MAP_OUTPUT_SCHEMA_CONFIG_FIELD = "avro.schema.mapoutput.value";
  private static String OUTPUT_KEY_SCHEMA_CONFIG_FIELD = "avro.schema.output.key";
  private static String OUTPUT_VALUE_SCHEMA_CONFIG_FIELD = "avro.schema.output.value";

  public static void setInputKeySchema(Job job, Schema schema) {
    job.getConfiguration().set(INPUT_KEY_SCHEMA_CONFIG_FIELD, schema.toString());
  }

  public static void setInputValueSchema(Job job, Schema schema) {
    job.getConfiguration().set(INPUT_VALUE_SCHEMA_CONFIG_FIELD, schema.toString());
  }

  public static void setMapOutputKeySchema(Job job, Schema schema) {
    job.setMapOutputKeyClass(AvroKey.class);
    job.getConfiguration().set(KEY_MAP_OUTPUT_SCHEMA_CONFIG_FIELD, schema.toString());
    job.setGroupingComparatorClass(AvroKeyComparator.class);
    job.setSortComparatorClass(AvroKeyComparator.class);
    addAvroSerialization(job.getConfiguration());
  }

  public static void setMapOutputValueSchema(Job job, Schema schema) {
    job.setMapOutputValueClass(AvroValue.class);
    job.getConfiguration().set(VALUE_MAP_OUTPUT_SCHEMA_CONFIG_FIELD, schema.toString());
    addAvroSerialization(job.getConfiguration());
  }

  public static void setOutputKeySchema(Job job, Schema schema) {
    job.setOutputKeyClass(AvroKey.class);
    job.getConfiguration().set(OUTPUT_KEY_SCHEMA_CONFIG_FIELD, schema.toString());
    addAvroSerialization(job.getConfiguration());
  }

  public static void setOutputValueSchema(Job job, Schema schema) {
    job.setOutputValueClass(AvroValue.class);
    job.getConfiguration().set(OUTPUT_VALUE_SCHEMA_CONFIG_FIELD, schema.toString());
    addAvroSerialization(job.getConfiguration());
  }

  private static void addAvroSerialization(Configuration conf) {
    Collection<String> serializations =
        conf.getStringCollection("io.serializations");
    if (!serializations.contains(AvroSerialization.class.getName())) {
      serializations.add(AvroSerialization.class.getName());
      conf.setStrings("io.serializations",
          serializations.toArray(new String[0]));
    }
  }

  public static Schema getInputKeySchema(Configuration conf) {
    String schemaString = conf.get(INPUT_KEY_SCHEMA_CONFIG_FIELD);
    return schemaString != null ? Schema.parse(schemaString) : null;
  }

  public static Schema getInputValueSchema(Configuration conf) {
    String schemaString = conf.get(INPUT_VALUE_SCHEMA_CONFIG_FIELD);
    return schemaString != null ? Schema.parse(schemaString) : null;
  }

  public static Schema getMapOutputKeySchema(Configuration conf) {
    String schemaString = conf.get(KEY_MAP_OUTPUT_SCHEMA_CONFIG_FIELD);
    return schemaString != null ? Schema.parse(schemaString) : null;
  }

  public static Schema getMapOutputValueSchema(Configuration conf) {
    String schemaString = conf.get(VALUE_MAP_OUTPUT_SCHEMA_CONFIG_FIELD);
    return schemaString != null ? Schema.parse(schemaString) : null;
  }

  public static Schema getOutputKeySchema(Configuration conf) {
    String schemaString = conf.get(OUTPUT_KEY_SCHEMA_CONFIG_FIELD);
    return schemaString != null ? Schema.parse(schemaString) : null;
  }

  public static Schema getOutputValueSchema(Configuration conf) {
    String schemaString = conf.get(OUTPUT_VALUE_SCHEMA_CONFIG_FIELD);
    return schemaString != null ? Schema.parse(schemaString) : null;
  }
}
