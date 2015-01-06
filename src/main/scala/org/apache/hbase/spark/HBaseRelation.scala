package org.apache.hbase.spark

import org.apache.avro.file.DataFileReader
import org.apache.avro.generic.{GenericRecord, GenericDatumReader}
import org.apache.avro.mapred.FsInput
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.HBaseConfiguration

import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.hbase.mapreduce.TableInputFormat
import org.apache.spark.rdd.RDD

import org.apache.spark.sql._
import org.apache.spark.sql.sources.{TableScan, BaseRelation}

import scala.collection.JavaConversions._

case class HBaseRelation(hbaseSite: String, tableName: String, schemaDefine: String)(@transient val sqlContext: SQLContext) extends TableScan {

  /*val schema = {
    val fileReader = newReader()
    val convertedSchema = toSqlType(fileReader.getSchema).dataType match {
      case s: StructType => s
      case other =>
        sys.error(s"Avro files must contain Records to be read, type $other not supported")
    }
    fileReader.close()
    convertedSchema
  }

  // By making this a lazy val we keep the RDD around, amortizing the cost of locating splits.
  lazy val buildScan = {
    val baseRdd = sqlContext.sparkContext.hadoopFile(
      hbaseSite,
      classOf[org.apache.avro.mapred.AvroInputFormat[GenericRecord]],
      classOf[org.apache.avro.mapred.AvroWrapper[GenericRecord]],
      classOf[org.apache.hadoop.io.NullWritable],
      sqlContext.sparkContext.defaultMinPartitions)

    baseRdd.map { record =>
      val values = (0 until schema.fields.size).map { i =>
        record._1.datum().get(i) match {
          case u: org.apache.avro.util.Utf8 => u.toString
          case other => other
        }
      }

      Row.fromSeq(values)
    }
  }

  private def newReader() = {
    val path = new Path(hbaseSite)
    val fs = FileSystem.get(path.toUri, sqlContext.sparkContext.hadoopConfiguration)

    val status = fs.getFileStatus(path)
    val singleFile = if (status.isDir) {
      fs.listStatus(path)
        .find(_.getPath.toString endsWith "avro")
        .map(_.getPath)
        .getOrElse(sys.error(s"Could not find .avro file with schema at $path"))
    } else {
      path
    }
    val input = new FsInput(singleFile, sqlContext.sparkContext.hadoopConfiguration)
    val reader = new GenericDatumReader[GenericRecord]()
    DataFileReader.openReader(input, reader)
  }

  private case class SchemaType(dataType: DataType, nullable: Boolean)

  private def toSqlType(avroSchema: Schema): SchemaType = {
    import Schema.Type._

    avroSchema.getType match {
      case INT => SchemaType(IntegerType, nullable = false)
      case STRING => SchemaType(StringType, nullable = false)
      case BOOLEAN => SchemaType(BooleanType, nullable = false)
      case BYTES => SchemaType(BinaryType, nullable = false)
      case DOUBLE => SchemaType(DoubleType, nullable = false)
      case FLOAT => SchemaType(FloatType, nullable = false)

      case RECORD =>
        val fields = avroSchema.getFields.map { f =>
          val schemaType = toSqlType(f.schema())
          StructField(f.name, schemaType.dataType, schemaType.nullable)
        }

        SchemaType(StructType(fields), nullable = false)

      case ARRAY =>
        val schemaType = toSqlType(avroSchema.getElementType)
        SchemaType(
          ArrayType(schemaType.dataType, containsNull = schemaType.nullable),
          nullable = false)

      case UNION => avroSchema.getTypes.toSeq match {
        case Seq(t1, t2) if t1.getType == NULL => toSqlType(t2).copy(nullable = true)
        case Seq(t1, t2) if t2.getType == NULL => toSqlType(t1).copy(nullable = true)
        case other =>
          sys.error(s"Union types with anything other than null not supported: $other")
      }

      case other => sys.error(s"Unsupported type $other")
    }
  }*/

  override def schema: StructType = ???

  lazy val buildScan = {
    val conf = HBaseConfiguration.create()
    conf.set(TableInputFormat.INPUT_TABLE, tableName)
    var hbaseRDD = sqlContext.sparkContext.newAPIHadoopRDD(conf, classOf[TableInputFormat],
      classOf[org.apache.hadoop.hbase.io.ImmutableBytesWritable],
      classOf[org.apache.hadoop.hbase.client.Result])
    hbaseRDD.map { record =>
      /*val values = (0 until schema.fields.size).map { i =>
         match {
          case u: org.apache.avro.util.Utf8 => u.toString
          case other => other
        }
      }*/
      val values = HBaseRecord(record._2).toValues()

      Row.fromSeq(values)
    }
  }
}
