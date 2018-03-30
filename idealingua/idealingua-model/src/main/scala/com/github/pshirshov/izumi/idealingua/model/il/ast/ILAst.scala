package com.github.pshirshov.izumi.idealingua.model.il.ast

import com.github.pshirshov.izumi.idealingua.model.common.TypeId._
import com.github.pshirshov.izumi.idealingua.model.common._
import com.github.pshirshov.izumi.idealingua.model.il.ast.ILAst.{Super, Tuple}

sealed trait ILAst {
  def id: TypeId
}


sealed trait ILStructure extends ILAst {
  def id: StructureId
  def fields: Tuple
  def superclasses: Super
}

object ILAst {

  case class Field(typeId: TypeId, name: String)
  case class PrimitiveField(typeId: Primitive, name: String)

  type Composite = List[InterfaceId]
  type Tuple = List[Field]
  type PrimitiveTuple = List[PrimitiveField]

  case class Super(interfaces: Composite, concepts: Composite) {
    val all: List[InterfaceId] = interfaces ++ concepts
  }

  object Super {
    def empty: Super = Super(List.empty, List.empty)

    def interfaces(ids: List[InterfaceId]): Super = Super(ids, List.empty)
  }

  case class Identifier(id: IdentifierId, fields: PrimitiveTuple) extends ILAst

  case class Interface(id: InterfaceId, fields: Tuple, superclasses: Super) extends ILStructure

  case class DTO(id: DTOId, fields: Tuple, superclasses: Super) extends ILStructure

  case class Enumeration(id: EnumId, members: List[String]) extends ILAst

  case class Alias(id: AliasId, target: TypeId) extends ILAst

  case class Adt(id: AdtId, alternatives: List[TypeId]) extends ILAst

  case class Service(id: ServiceId, methods: List[Service.DefMethod])

  object Service {

    trait DefMethod

    object DefMethod {

      case class Signature(input: Composite, output: Composite) {
        def asList: List[InterfaceId] = input ++ output
      }

      case class RPCMethod(name: String, signature: Signature) extends DefMethod

    }

  }

}









