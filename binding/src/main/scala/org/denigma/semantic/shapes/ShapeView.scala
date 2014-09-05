package org.denigma.semantic.shapes

import org.denigma.binding.extensions.sq
import org.denigma.binding.binders.extractors.EventBinding
import org.denigma.binding.views.{Injector, CollectionView, IView, BindableView}
import org.denigma.semantic.binding.{RDFBinder, ChangeSlot}
import org.scalajs.dom
import org.scalajs.dom.{MouseEvent, HTMLElement}
import org.scalax.semweb.shex.{ArcRule, Shape}
import rx.Rx
import rx.core.Var
import rx.ops._
import org.denigma.binding.extensions._
import org.denigma.binding
import org.scalajs.dom.extensions._

import scala.Predef
import scala.collection.immutable.Map
import scalatags.Text.Tag


object ShapeInside {

  def apply(initial:Shape):ShapeInside = ShapeInside(initial,initial)

}

case class ShapeInside(initial:Shape,current:Shape,wantsToDie:Boolean = false) extends ChangeSlot
{
  override type Value = Shape

  def updateArc(arc:ArcRule) = {
    current.arcRules()
  }
}


object ArcView {

  def apply(el:HTMLElement,params:Map[String,Any]) = {
    new JustArcView(el,params)
  }


  class JustArcView(val elem:HTMLElement, val params:Map[String,Any]) extends ArcView {

    override def activateMacro(): Unit = {extractors.foreach(_.extractEverything(this))}

    override protected def attachBinders(): Unit = BindableView.defaultBinders(this)

  }

}

trait ArcView extends BindableView
{

  val arc = Var(params("item").asInstanceOf[ArcRule])

 require(params.contains("item"), "ArcView should contain arc item inside")



}

trait ShapeView extends BindableView with CollectionView
{


  protected def getShape:Shape = {
    require(params.contains("shape"),"ShapeView must contain shape in params")
    this.params("shape").asInstanceOf[Shape]
  }

  lazy val shapeInside: Var[ShapeInside] = Var(ShapeInside(this.getShape))


  override type Item = ArcRule
  override type ItemView = ArcView



  override def newItem(item:Item):ItemView =
  {
    //dom.console.log(template.outerHTML.toString)
    val el = template.cloneNode(true).asInstanceOf[HTMLElement]

    el.removeAttribute("data-template")
    val mp: Map[String, Any] = Map[String,Any]("item"->item)

    val view: ItemView = el.attributes.get("data-item-view") match {
      case None=>
        ArcView.apply(el, mp)
      case Some(v)=> this.inject(v.value,el,mp) match {
        case iv:ItemView=> iv
        case _=>
          dom.console.error(s"view ${v.value} exists but does not inherit ItemView")
          ArcView.apply(el,mp)
      }
    }
    //item.handler(onItemChange(item))
    view

  }

  val removeClick = EventBinding.createMouseEvent()


  val items: Rx[List[ArcRule]] = this.shapeInside.map(shi=>shi.current.arcSorted())


}
