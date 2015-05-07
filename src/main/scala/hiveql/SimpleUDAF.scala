package hiveql

import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.{SparkConf, SparkContext}

//
// Demonstrate a Hive user-defined aggregation function (UDAF). There are two ways to
// define these: simple and generic -- this is the simple kind.
// THe UDAF is defined in Java -- see src/main/java/hiveql/SumLargeSalesUDAF.java
//

object SimpleUDAF {

  def main (args: Array[String]) {
    val conf = new SparkConf().setAppName("HiveQL").setMaster("local[4]")
    val sc = new SparkContext(conf)
    val hiveContext = new HiveContext(sc)

    import hiveContext.implicits._

    // create an RDD of tuples with some data
    val custs = Seq(
      (1, "Widget Co", 120000.00, 0.00, "AZ"),
      (2, "Acme Widgets", 410500.00, 500.00, "CA"),
      (3, "Widgetry", 410500.00, 200.00, "CA"),
      (4, "Widgets R Us", 410500.00, 0.0, "CA"),
      (5, "Ye Olde Widgete", 500.00, 0.0, "MA")
    )
    val customerRows = sc.parallelize(custs, 4)
    val customerDF = customerRows.toDF("id", "name", "sales", "discount", "state")

    // register as a temporary table
    customerDF.printSchema()
    customerDF.registerTempTable("customers")

    // register the UDAF with the HiveContext, by referring to the class we defined above --
    // skip the 'TEMPORARY' if you want it to be persisted in the Hive metastore
    hiveContext.sql("CREATE TEMPORARY FUNCTION mysum AS 'hiveql.SumLargeSalesUDAF'")

    // now use it in a query
    val data1 = hiveContext.sql("SELECT state, mysum(sales) AS sales FROM customers GROUP BY state")
    data1.printSchema()
    data1.foreach(println)

  }
}
