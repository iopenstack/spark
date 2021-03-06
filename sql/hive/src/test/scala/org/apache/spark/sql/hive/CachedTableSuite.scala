/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.hive

import org.apache.spark.sql.execution.SparkLogicalPlan
import org.apache.spark.sql.columnar.InMemoryColumnarTableScan
import org.apache.spark.sql.hive.execution.HiveComparisonTest

class CachedTableSuite extends HiveComparisonTest {
  TestHive.loadTestTable("src")

  test("cache table") {
    TestHive.cacheTable("src")
  }

  createQueryTest("read from cached table",
    "SELECT * FROM src LIMIT 1", reset = false)

  test("check that table is cached and uncache") {
    TestHive.table("src").queryExecution.analyzed match {
      case SparkLogicalPlan(_ : InMemoryColumnarTableScan) => // Found evidence of caching
      case noCache => fail(s"No cache node found in plan $noCache")
    }
    TestHive.uncacheTable("src")
  }

  createQueryTest("read from uncached table",
    "SELECT * FROM src LIMIT 1", reset = false)

  test("make sure table is uncached") {
    TestHive.table("src").queryExecution.analyzed match {
      case cachePlan @ SparkLogicalPlan(_ : InMemoryColumnarTableScan) =>
        fail(s"Table still cached after uncache: $cachePlan")
      case noCache => // Table uncached successfully
    }
  }

  test("correct error on uncache of non-cached table") {
    intercept[IllegalArgumentException] {
      TestHive.uncacheTable("src")
    }
  }
}
