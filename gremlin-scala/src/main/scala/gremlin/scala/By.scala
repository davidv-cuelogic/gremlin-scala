package gremlin.scala

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.structure.T
import java.util.function.{Function ⇒ JFunction}

/**
  * by step can be used in combination with all sorts of other steps, e.g. group, order, dedup, sack, ...
  * http://tinkerpop.apache.org/docs/current/reference/#by-step
  */
trait By[Modulated] {
  def apply[End](traversal: GraphTraversal[_, End]): GraphTraversal[_, End]
}

object by {

  /* modulate by property */
  def apply[Modulated](key: Key[Modulated]) = new By[Modulated] {
    override def apply[End](traversal: GraphTraversal[_, End]) = traversal.by(key.name)
  }

  /* modulate by label - alias for `apply[String](T.label)` */
  def label[Modulated]() = new By[String] {
    override def apply[End](traversal: GraphTraversal[_, End]) = traversal.by(T.label)
  }

  /* modulate by T(oken) */
  def apply[Modulated](token: T) = new By[Modulated] {
    override def apply[End](traversal: GraphTraversal[_, End]) = traversal.by(token)
  }

  /* modulate by traversal 
   * n.b. cast here is a dirty hack, but we can't connect then End type of the start of byTraversal and `End`, 
   * so the calling site would have to specify type params which looks ugly and superfluous. only risk here is 
   * that caller is not restricted from doing non-element steps (e.g. call `.has()` on an integer). */
  def apply[Modulated](byTraversal: GremlinScala[Vertex, _] => GremlinScala[Modulated, _]) = new By[Modulated] {
    override def apply[End](traversal: GraphTraversal[_, End]) =
      traversal.by(byTraversal(__[End].asInstanceOf[GremlinScala[Vertex, _]]).traversal)
  }

  /* modulate by function
   * this is not called `apply` to discourage it's use (see http://tinkerpop.apache.org/docs/current/reference/#a-note-on-lambdas)
   * and because it conflicts with `apply(byTraversal)`
*/
  def function[From, Modulated](fun: From => Modulated) = new By[Modulated] {
    override def apply[End](traversal: GraphTraversal[_, End]) =
      traversal.by[From](new JFunction[From, AnyRef] {
        override def apply(from: From): AnyRef = fun(from).asInstanceOf[AnyRef]
      })
  }

  /* identity modulator */
  def apply[Modulated]() = new By[Modulated] {
    override def apply[End](traversal: GraphTraversal[_, End]) = traversal.by()
  }

}
