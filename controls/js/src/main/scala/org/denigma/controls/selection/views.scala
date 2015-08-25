package org.denigma.controls.selection

import org.denigma.binding.binders.GeneralBinder
import org.denigma.binding.views.{BindableView, ItemsSetView}
import org.scalajs.dom.raw.HTMLElement
import rx.Rx
import rx.core.Var
import rx.ops._
import org.denigma.binding._


class OptionView(val elem:HTMLElement,item:Var[TextSelection],val params:Map[String,Any]) extends BindableView
{
  import rx.ops._
  val label = item.map(_.label)
  val value = item.map(_.value)
  val position = item.map(_.position)
}


class SelectOptionsView(val elem:HTMLElement, val items: Var[scala.collection.immutable.SortedSet[Var[TextSelection]]],val params:Map[String,Any]) extends ItemsSetView
{

  type Item = Var[TextSelection]

  type ItemView = OptionView

  val hasOptions: Rx[Boolean] = items.map{case ops=>  ops.nonEmpty}

  override protected def warnIfNoBinders(asError:Boolean) = if(asError) super.warnIfNoBinders(asError)

  override def newItem(item: Item): OptionView = constructItemView(item){
    case (el,mp)=>new OptionView(el,item, mp).withBinder{new GeneralBinder(_)}
  }

}
