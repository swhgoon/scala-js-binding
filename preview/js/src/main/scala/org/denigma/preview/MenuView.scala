package org.denigma.preview

import org.denigma.binding.views.{MapCollectionView, BindableView}
import org.scalajs.dom.raw.HTMLElement
import rx.Rx
import rx.core.Var

import scala.Predef
import scala.collection.immutable._


/**
 * Menu view, this view is devoted to displaying menus
 * @param elem html element
 * @param params view params (if any)
 */
class MenuView(elem:HTMLElement, params:Map[String,Any] = Map.empty) extends MapCollectionView(elem,params) {
self =>



    override val items: Rx[Seq[Map[String, Any]]] = Var(
        Seq(
            Map("uri"->"pages/bind","label"->"Basic binding example"),
            Map("uri"->"pages/collection","label"->"Collection binding"),
            Map("uri"->"pages/controls","label"->"Various controls"),
            Map("uri"->"pages/rdf","label"->"RDF support")
          /*
                      Map("uri"->"pages/editing","label"->"Page editing"),
                      Map("uri"->"pages/data","label"->"Data editing")
          */
        )
    )
}