package com.github.pshirshov.izumi.distage.provisioning.strategies

import com.github.pshirshov.izumi.distage.model.exceptions.UnexpectedProvisionResultException
import com.github.pshirshov.izumi.distage.model.monadic.DIEffect
import com.github.pshirshov.izumi.distage.model.monadic.DIEffect.syntax._
import com.github.pshirshov.izumi.distage.model.plan.ExecutableOp.MonadicOp
import com.github.pshirshov.izumi.distage.model.plan.ExecutableOp.MonadicOp.ExecuteEffect
import com.github.pshirshov.izumi.distage.model.provisioning.strategies.EffectStrategy
import com.github.pshirshov.izumi.distage.model.provisioning.{NewObjectOp, OperationExecutor, ProvisioningKeyProvider}
import com.github.pshirshov.izumi.distage.model.reflection.universe.RuntimeDIUniverse
import com.github.pshirshov.izumi.distage.model.reflection.universe.RuntimeDIUniverse.{SafeType, TagK}
import com.github.pshirshov.izumi.fundamentals.platform.functional.Identity

class EffectStrategyDefaultImpl
  extends EffectStrategy {

  protected[this] val identityEffectType: RuntimeDIUniverse.SafeType = SafeType.getK[Identity]

  override def executeEffect[F[_]: TagK](
                                          context: ProvisioningKeyProvider
                                        , executor: OperationExecutor
                                        , op: MonadicOp.ExecuteEffect
                                        )(implicit F: DIEffect[F]): F[Seq[NewObjectOp.NewInstance]] = {
    val provisionerEffectType = SafeType.getK[F]
    val ExecuteEffect(target, actionOp, _, _) = op
    val actionEffectType = op.wiring.effectHKTypeCtor

    val isEffect = actionEffectType != identityEffectType
    if (isEffect && !(actionEffectType <:< provisionerEffectType)) {
      // FIXME: should be thrown earlier [imports (or missing non-imports???) too; add more sanity checks wrt imports after GC, etc.]
      throw new ThisException_ShouldBePartOfPrepSanityCheckReally_SameAsImports(
        s"Incompatible effect types $actionEffectType <!:< $provisionerEffectType"
      )
    }

    executor.execute(context, actionOp)
      .flatMap(_.toList match {
        case NewObjectOp.NewInstance(_, action0) :: Nil if isEffect =>
          val action = action0.asInstanceOf[F[Any]]
          action.map(newInstance => Seq(NewObjectOp.NewInstance(target, newInstance)))
        case NewObjectOp.NewInstance(_, newInstance) :: Nil =>
          F.pure(Seq(NewObjectOp.NewInstance(target, newInstance)))
        case r =>
          throw new UnexpectedProvisionResultException(s"Unexpected operation result for ${actionOp.target}: $r, expected a single NewInstance!", r)
      })
  }

}
