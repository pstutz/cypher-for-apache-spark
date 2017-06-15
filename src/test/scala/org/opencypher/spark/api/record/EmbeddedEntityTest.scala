package org.opencypher.spark.api.record

import org.opencypher.spark.TestSuiteImpl
import org.opencypher.spark.api.exception.SparkCypherException
import org.opencypher.spark.api.expr.Expr

class EmbeddedEntityTest extends TestSuiteImpl {

  test("Construct embedded node") {
    val given =
      EmbeddedNode("n" -> "id").build
        .withImpliedLabel("Person")
        .withOptionalLabel("Employee" -> "is_emp")
        .withProperty("name")
        .withProperty("age" -> "YEARS")
        .withProperty("age" -> "AGE")

    val actual = EmbeddedNode(
      "n",
      "id",
      Map("Person" -> None, "Employee" -> Some("is_emp")),
      Map("name" -> Set("name"), "age" -> Set("YEARS", "AGE"))
    )

    given should equal(actual)
    show(given.slots) should equal("Seq((AGE,n.age :: ?), (YEARS,n.age :: ?), (id,n :: :Person:-Employee NODE), (is_emp,n:Employee :: BOOLEAN), (name,n.name :: ?))")
  }

  test("Construct embedded relationship with static type") {
    val given =
      EmbeddedRelationship("r").from("src").relType("KNOWS").to("dst").build
        .withProperty("name")
        .withProperty("age" -> "YEARS")
        .withProperty("age" -> "AGE")

    val actual = EmbeddedRelationship(
      "r",
      "r",
      "src",
      Right("KNOWS"),
      "dst",
      Map("name" -> Set("name"), "age" -> Set("YEARS", "AGE"))
    )

    given should equal(actual)
    show(given.slots) should equal("Seq((AGE,r.age :: ?), (YEARS,r.age :: ?), (dst,target(r :: :KNOWS RELATIONSHIP)), (name,r.name :: ?), (r,r :: :KNOWS RELATIONSHIP), (src,source(r :: :KNOWS RELATIONSHIP)))")
  }

  test("Construct embedded relationship with dynamic type") {
    val given =
      EmbeddedRelationship("r").from("src").relTypes("typ", "ADMIRES", "IGNORES").to("dst").build
        .withProperty("name")
        .withProperty("age" -> "YEARS")
        .withProperty("age" -> "AGE")

    val actual = EmbeddedRelationship(
      "r",
      "r",
      "src",
      Left("typ" -> Set("ADMIRES", "IGNORES")),
      "dst",
      Map("name" -> Set("name"), "age" -> Set("YEARS", "AGE"))
    )

    given should equal(actual)
    show(given.slots) should equal("Seq((AGE,r.age :: ?), (YEARS,r.age :: ?), (dst,target(r :: :ADMIRES|IGNORES RELATIONSHIP)), (name,r.name :: ?), (r,r :: :ADMIRES|IGNORES RELATIONSHIP), (src,source(r :: :ADMIRES|IGNORES RELATIONSHIP)), (typ,type(r) :: STRING))")
  }

    test("Refuses to use the same slot multiple times when constructing nodes") {
      raisesSlotReUse(EmbeddedNode("n" -> "the_slot").build.withOptionalLabel("Person" -> "the_slot").slots)
      raisesSlotReUse(EmbeddedNode("n" -> "the_slot").build.withProperty("a" -> "the_slot").slots)
    }

    test("Refuses to use the same slot multiple times when constructing relationships") {
      raisesSlotReUse(EmbeddedRelationship("r").from("r").to("b").relType("KNOWS").build.slots)
      raisesSlotReUse(EmbeddedRelationship("r").from("a").to("r").relType("KNOWS").build.slots)
      raisesSlotReUse(EmbeddedRelationship("r").from("a").to("b").relTypes("r", "KNOWS").build.slots)
      raisesSlotReUse(EmbeddedRelationship("r" -> "the_slot").from("a").to("b").relType("KNOWS").build.withProperty("a" -> "the_slot").slots)
    }

    private def show(slots: Map[String, Expr]) = {
      val result = slots.keys.toSeq.sorted.map(k => k -> slots(k)).mkString("Seq(", ", ", ")")
      // println(result)
      result
    }

    private def raisesSlotReUse[T](f: => T): Unit = {
      an[SparkCypherException] should be thrownBy(f)
    }
}
