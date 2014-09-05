package org.denigma.binding.binders

import org.denigma.binding.commons.ILogged
import org.denigma.binding.extensions._
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.extensions._
import rx._

import scala.collection.immutable.Map
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.util.{Failure, Success}


/**
 *  A class that contains basic functions for all bindings
 */
abstract class BasicBinding extends ILogged
{


  def bindAttributes(el:HTMLElement,ats:Map[String, String] ):Unit

  protected def dataAttributesOnly(ats:Map[String,String]): Map[String, String] = ats.collect{
    case (key,value) if key.contains("data-") && !key.contains("data-view")=> (key.replace("data-",""),value)
  }


  def id:String

//  = {
//    val params = js.Dynamic.literal( "html" -> newInnerHTML)
//
//    if(uri!="")  dom.window.history.pushState(params,dom.document.title,uri)
//    el.innerHTML = newInnerHTML
//  }

  protected def processUrl(url:String, relativeURI:Boolean = true):String =
    if(url.contains("://")) {
      val st = url.indexOf("://")+3
      sq.withHost(url.substring(url.indexOf("/",st)))
    }
    else
      sq.withHost(url)

  /**
   * Makes id for the binding element
   * @param el html element
   * @param title title of id
   * @return
   */
  def makeId(el:HTMLElement,title:String): String = el.id match {
    case s if s=="" ||  s==null || s.isInstanceOf[js.prim.Undefined]=>
      el.id = title + "#" +  math.round(10000*math.random) //to make length shorter
      el.id

    case id=>
      id

  }


  /**
   * Binds value to reactive property
   * @param key key to witch to bind to
   * @param el html element
   * @param rx reactive variable
   * @param assign assign function that assigns value to html element
   * @tparam T type param
   * @return
   */
  def bindRx[T](key:String,el:HTMLElement ,rx:Rx[T])(assign:(HTMLElement,T)=>Unit): Unit = {

    val eid = this.makeId(el, key)
    lazy val obs: Obs = Obs(rx, eid, skipInitial = false) {
      /*
      dom.document.getElementById(eid) match {
        case null =>
          dom.console.info(s"$eid was not find, killing observable...")
          obs.kill()

        case element: HTMLElement =>
          val value = rx.now
          //el.dyn.obs = obs.asInstanceOf[js.Dynamic]
          assign(element, value)
      }
      */
      val value = rx.now
      assign(el,value)
    }
    val o = obs //TO MAKE LAZY STUFF WORK

  }


  /**
   * Creates and even handler that can be attached to different listeners
   * @param el element
   * @param par rx parameter
   * @param assign function that assigns var values to some element properties
   * @tparam TEV type of event
   * @tparam TV type of rx
   * @return
   */
  def makeEventHandler[TEV<:Event,TV](el:HTMLElement,par:Rx[TV])(assign:(TEV,Var[TV],HTMLElement)=>Unit):(TEV)=>Unit = ev=> par match {
    case v:Var[TV] => assign(ev,v,el)
    case _=> dom.console.error(s"rx is not Var")
  }



  protected def otherPartial:PartialFunction[String,Unit] = {case _=>}

}