package tandu.activities

import com.raquo.laminar.api.L.*
import tandu.Kind
import tandu.i18n.Strings

trait Activity:
  def id: String
  def name(s: Strings): String
  def description(s: Strings): String
  def minPlayers: Int
  def maxPlayers: Int
  def handsFree: Boolean
  def kind: Kind = Kind.Games

  /** The page body shown after Suggest. Rendered fresh on each navigation. */
  def render(): HtmlElement
