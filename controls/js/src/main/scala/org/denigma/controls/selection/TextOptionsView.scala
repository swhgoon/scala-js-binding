package org.denigma.controls.selection

import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.extensions._
import org.denigma.binding.views._
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.ext.KeyCode
import org.scalajs.dom.raw.HTMLElement
import rx.Rx
import rx.core.Var
import rx.ops._

class TextOptionsView(val elem:HTMLElement,
                        val items: Rx[scala.collection.immutable.Seq[Var[TextSelection]]],
                        onkeydown: Var[KeyboardEvent]
                         )
  extends CollectionView
{

  type Item = Var[TextSelection]

  type ItemView = OptionView

  def my(str:String) = str+"_of_"+this.id //to name Vars for debugging purposes

  val keyDownHandler = onkeydown.onChange(my("keydown_handler"))(event=>{
    event.keyCode match  {
      case KeyCode.Down => focus() = focus.now +1
      case KeyCode.Up => focus() = focus.now - 1
      case KeyCode.Enter =>
        val f = focus.now
        if(f > -1) if(items.now.length>f) fire(SelectTextOptionEvent(items.now(f),this,this))
        else dom.console.error(s"some bug with options focus for focus ${f} and items ${items.now}")

      //val opts = this.subviews.values.collectFirst{   case v:SelectOptionsView=> v   } //KOSTYL TODO:fix it!
      //opts
      case other=>
    }
  })

  val removedInserted = items.removedInserted

  val indexedItems = items.map(its=>its.zipWithIndex)

  val hasOptions: Rx[Boolean] = items.map{case ops=> ops.nonEmpty}

  val focus:Var[Int] = Var(-1)
  focus.onChange("focus_change"){
    f=>
      val its = indexedItems.now
      for{
        (it,i)<-its
        item = it.now
      } if(item.preselected && f!=i){
        it() = item.copy()(item.position,false)
        } else if(f==i) it() = item.copy()(item.position,true)
  }


  override protected def warnIfNoBinders(asError:Boolean) = if(asError) super.warnIfNoBinders(asError)

  override def newItem(item: Item): OptionView = constructItemView(item){
    case (el,mp)=>new OptionView(el,item).withBinder{new GeneralBinder(_)}
  }

  /**
   * Adds subscription
   */
  override protected def subscribeUpdates(): Unit = {
    template.style.display = "none"
    this.items.now.foreach(i=>this.addItemView(i,this.newItem(i))) //initialization of views
    items.onChange("items_change"){its=>
      val (removed,inserted) = removedInserted.now
      for(r <- removed) onRemove(r)
      for(i <- inserted) onInsert(i)
      for{
        (it,i)<-indexedItems.now
        item = it.now
        if item.position!=i
      } it() = item.copy()(i,item.preselected)
      focus() = -1
    }
  }
}
