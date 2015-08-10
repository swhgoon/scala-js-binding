package org.denigma.semantic.binders.binded

import org.denigma.binding.extensions._
import org.denigma.semantic.extensions.GraphUpdate
import org.scalajs.dom.raw.{HTMLElement, KeyboardEvent}
import org.w3.banana._
import rx.Rx
import rx.core.Var
import org.denigma.binding.binders.extractors.PropertyBinder
import org.denigma.binding.extensions._
import org.denigma.binding.views.BindableView
import org.denigma.semantic.binders.binded.{BindedTextProperty, Binded}
import org.denigma.semantic.extensions.GraphUpdate
import org.scalajs.dom
import org.scalajs.dom.ext._
import org.scalajs.dom.raw.{Event, HTMLElement, KeyboardEvent}
import org.w3.banana._
import rx.Rx
import rx.core.Var

import scala.collection.immutable.Map
import scala.collection.mutable
import scala.scalajs.js

class BindedTextProperty[Rdf<:RDF](
                           el:HTMLElement,
                           graph:Var[PointedGraph[Rdf]],
                           updates:Rx[GraphUpdate[Rdf]],
                           val predicate:Rdf#URI,
                           propertyName:String,
                           createIfNotExist:Boolean = true
                                    )
                         (implicit val ops:RDFOps[Rdf]) extends Binded[Rdf](graph,updates,createIfNotExist)(ops)
{
  import ops._


  protected def label(uri:Rdf#URI) = ops.lastSegment(uri)


  def nodes2labels(values: Set[Rdf#Node]): String = {
    values.size match {
      case 0 => ""
      case 1 => node2label(values.head)
      case many => values.foldLeft("") { case (acc, prop) => acc + node2label(prop) + "; " }
    }
  }


  def addKeyChangeHandler(html:HTMLElement,prop:String)(fun:(HTMLElement,KeyboardEvent,String,String)=>Unit) = {
    val basic = propertyString(html,prop)
    var (oldVal,newVal) = (basic,basic)
    def onKeyUp(event:KeyboardEvent):Unit = if(event.target==event.currentTarget){
      oldVal = newVal
      newVal = propertyString(html,prop)
      if(oldVal!=newVal) {
        fun(html,event,oldVal,newVal)
      }
    }

    html.onkeyup = onKeyUp _
  }


  protected def onObjectsChange(values: Set[Rdf#Node]): Unit = {
    el.dyn.updateDynamic(propertyName)(this.nodes2labels(values))
  }

  override def init() = {
    super.init()
    this.addKeyChangeHandler(el,this.propertyName){
      case (elem,ev,oldValue,newValue)=>
        updateFromHTML()
      //dom.console.log(s"change is from $oldValue to $newValue")
    }
  }


  override def objectsFromHTML(): Set[Rdf#Node] = propertyString(el,this.propertyName) match {
    case str if str == "true" || str == "false" => Set(ops.makeLiteral(str,ops.xsd.boolean))

    case str => Set(ops.makeLiteral(str,ops.xsd.string))
  }
}
