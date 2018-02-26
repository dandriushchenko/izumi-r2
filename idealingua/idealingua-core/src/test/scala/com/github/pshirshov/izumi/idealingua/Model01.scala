package com.github.pshirshov.izumi.idealingua

import com.github.pshirshov.izumi.idealingua.model.common.{Field, Generic, Primitive, UserType}
import com.github.pshirshov.izumi.idealingua.model.il._

object Model01 {
  val userIdTypeId = model.common.UserType.parse("izumi.test.domain01.UserId").toAlias
  val enumId = model.common.UserType.parse("izumi.test.domain01.AnEnum").toEnum
  val testInterfaceId = model.common.UserType.parse("izumi.test.domain01.TestInterface").toInterface

  val testValIdentifier = model.common.UserType.parse("izumi.test.domain01.TestValIdentifer").toIdentifier
  val testIdentifier = model.common.UserType.parse("izumi.test.domain01.TestIdentifer").toIdentifier
  val serviceIdentifier = model.common.UserType.parse("izumi.test.domain01.UserService").toService

  val testInterfaceFields = List(
    Field(userIdTypeId, "userId")
    , Field(Primitive.TInt32, "accountBalance")
    , Field(Primitive.TInt64, "latestLogin")
    , Field(Generic.TMap(Primitive.TString, Primitive.TString), "keys")
    , Field(Generic.TList(Primitive.TString), "nicknames")
  )


  val testValStructure = List(Field(userIdTypeId, "userId"))
  val testIdStructure = List(Field(userIdTypeId, "userId"), Field(Primitive.TString, "context"))
  val testIdObject = UserType.parse("izumi.test.domain01.TestObject").toDTO

  val domain: DomainDefinition = DomainDefinition(DomainId(Seq("izumi", "test"), "domain01"), Seq(
    FinalDefinition.Alias(userIdTypeId, Primitive.TString)
    , FinalDefinition.Enumeration(enumId, List("VALUE1", "VALUE2"))
    , FinalDefinition.Identifier(testValIdentifier, testValStructure)
    , FinalDefinition.Identifier(testIdentifier, testIdStructure)
    , FinalDefinition.Interface(
      testInterfaceId
      , testInterfaceFields
      , List.empty
    )
    , FinalDefinition.DTO(
      testIdObject
      , List(testInterfaceId))
  ), List(
    Service(serviceIdentifier, List(
      DefMethod.RPCMethod("createUser", DefMethod.Signature(List(testInterfaceId), List(testInterfaceId)))
    ))
  ), Map.empty)
}
