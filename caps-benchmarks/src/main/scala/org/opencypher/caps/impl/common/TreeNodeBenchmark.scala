/*
 * Copyright (c) 2016-2017 "Neo4j, Inc." [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opencypher.caps.impl.common

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._

@Threads(1)
@Fork(
  value = 1,
  jvmArgs = Array("-Xms4G") //,
//"-XX:+UnlockCommercialFeatures",
//"-XX:+FlightRecorder",
//"-XX:StartFlightRecording=duration=0s,delay=0s,dumponexit=true,filename=bm.jfr"
)
@Warmup(iterations = 10)
@Measurement(iterations = 10)
@State(Scope.Thread)
//@BenchmarkMode(Array(Mode.AverageTime))
//@OutputTimeUnit(TimeUnit.MICROSECONDS)
//@State(Scope.Benchmark)
class TreeNodeBenchmark {

  val calculation =
    Add(
      Number(0),
      Add(
        Number(1),
        Add(
          Add(
            Number(2),
            Number(3)
          ),
          Add(
            Number(4),
            Add(
              Number(5),
              Number(6)
            )
          )
        )
      )
    )

  @Setup
  def prepare: Unit = {}

  @TearDown
  def check: Unit = {}

  @Benchmark
  def transformUpNoOp: CalcExpr = calculation.transformUp {
    case Add(n1: Number, n2: Number) => Add(NoOp(n1), NoOp(n2))
    case Add(n1: Number, n2)         => Add(NoOp(n1), n2)
    case Add(n1, n2: Number)         => Add(n1, NoOp(n2))
  }

  @Benchmark
  def transformDownNoOp: CalcExpr = calculation.transformDown {
    case Add(n1: Number, n2: Number) => Add(NoOp(n1), NoOp(n2))
    case Add(n1: Number, n2)         => Add(NoOp(n1), n2)
    case Add(n1, n2: Number)         => Add(n1, NoOp(n2))
  }

  @Benchmark
  def transformUpSimplify: CalcExpr = calculation.transformUp {
    case Add(Number(n1), Number(n2)) => Number(n1 + n2)
  }

}

abstract class CalcExpr extends AbstractTreeNode[CalcExpr] {
  def eval: Int
}

case class Add(left: CalcExpr, right: CalcExpr) extends CalcExpr {
  def eval = left.eval + right.eval
}

case class Number(v: Int) extends CalcExpr {
  def eval = v
}

case class NoOp(in: CalcExpr) extends CalcExpr {
  def eval = in.eval
}
