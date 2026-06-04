package tandu.activities

import tandu.i18n.Lang

/** Starter scenarios for the Memory train chain game. Each theme is an
  * opening frame ("I packed my suitcase and put in…") that gives the
  * cumulative list a shared subject, so players add things that fit
  * rather than random words. Kept short — it's read aloud and chanted.
  * A roomy bank keeps the [[Roller]] from repeating a theme too soon.
  */
object MemoryChainBank:

  val en: Vector[String] = Vector(
    "I packed my suitcase",
    "I went to the shop and bought",
    "At the zoo I saw",
    "In my magic backpack there is",
    "On the picnic blanket there was",
    "In the spaceship I brought",
    "Grandma's attic is full of",
    "In my lunchbox there is",
    "At the party there was",
    "On the pirate ship I found",
    "In the garden grows",
    "For the trip I packed",
    "Under the sea I found",
    "In the forest I met",
    "At the farm there was",
    "In my dream there was",
    "For the camping trip I brought",
    "In the toy box there is",
    "At the bakery I bought",
    "In the wizard's pocket there was",
    "On the desert island I found",
    "In the fridge there is",
    "At the circus I saw",
    "In the treasure chest there was",
    "At the market I bought",
    "In the jungle I spotted",
    "In the castle I found",
    "On the rocket to the moon I took"
  )

  val pl: Vector[String] = Vector(
    "Pakuję walizkę",
    "Poszedłem do sklepu i kupiłem",
    "W zoo zobaczyłem",
    "W moim magicznym plecaku jest",
    "Na kocu na pikniku było",
    "Do rakiety zabrałem",
    "Strych babci jest pełen",
    "W moim pudełku na drugie śniadanie jest",
    "Na przyjęciu było",
    "Na statku pirackim znalazłem",
    "W ogrodzie rośnie",
    "Na wycieczkę spakowałem",
    "Pod wodą znalazłem",
    "W lesie spotkałem",
    "Na farmie było",
    "W moim śnie było",
    "Na biwak zabrałem",
    "W pudle z zabawkami jest",
    "W piekarni kupiłem",
    "W kieszeni czarodzieja było",
    "Na bezludnej wyspie znalazłem",
    "W lodówce jest",
    "W cyrku zobaczyłem",
    "W skrzyni ze skarbem było",
    "Na targu kupiłem",
    "W dżungli wypatrzyłem",
    "W zamku znalazłem",
    "W rakiecie na Księżyc zabrałem"
  )

  val es: Vector[String] = Vector(
    "Hago la maleta y meto",
    "Fui a la tienda y compré",
    "En el zoo vi",
    "En mi mochila mágica hay",
    "En la manta del picnic había",
    "A la nave espacial llevé",
    "El desván de la abuela está lleno de",
    "En mi fiambrera hay",
    "En la fiesta había",
    "En el barco pirata encontré",
    "En el jardín crece",
    "Para el viaje preparé",
    "Bajo el mar encontré",
    "En el bosque me encontré con",
    "En la granja había",
    "En mi sueño había",
    "Para acampar llevé",
    "En la caja de juguetes hay",
    "En la panadería compré",
    "En el bolsillo del mago había",
    "En la isla desierta encontré",
    "En la nevera hay",
    "En el circo vi",
    "En el cofre del tesoro había",
    "En el mercado compré",
    "En la jungla divisé",
    "En el castillo encontré",
    "En el cohete a la Luna llevé"
  )

  val fr: Vector[String] = Vector(
    "Je fais ma valise et j'y mets",
    "Je suis allé au magasin et j'ai acheté",
    "Au zoo j'ai vu",
    "Dans mon sac à dos magique il y a",
    "Sur la couverture du pique-nique il y avait",
    "Dans la fusée j'ai emporté",
    "Le grenier de mamie est plein de",
    "Dans ma boîte à goûter il y a",
    "À la fête il y avait",
    "Sur le bateau pirate j'ai trouvé",
    "Dans le jardin pousse",
    "Pour le voyage j'ai préparé",
    "Sous la mer j'ai trouvé",
    "Dans la forêt j'ai rencontré",
    "À la ferme il y avait",
    "Dans mon rêve il y avait",
    "Pour le camping j'ai emporté",
    "Dans la boîte à jouets il y a",
    "À la boulangerie j'ai acheté",
    "Dans la poche du magicien il y avait",
    "Sur l'île déserte j'ai trouvé",
    "Dans le frigo il y a",
    "Au cirque j'ai vu",
    "Dans le coffre au trésor il y avait",
    "Au marché j'ai acheté",
    "Dans la jungle j'ai repéré",
    "Dans le château j'ai trouvé",
    "Dans la fusée vers la Lune j'ai emporté"
  )

  val de: Vector[String] = Vector(
    "Ich packe meinen Koffer",
    "Ich war im Laden und habe gekauft",
    "Im Zoo habe ich gesehen",
    "In meinem Zauberrucksack ist",
    "Auf der Picknickdecke lag",
    "In die Rakete habe ich mitgenommen",
    "Omas Dachboden ist voll mit",
    "In meiner Brotdose ist",
    "Auf dem Fest gab es",
    "Auf dem Piratenschiff habe ich gefunden",
    "Im Garten wächst",
    "Für die Reise habe ich eingepackt",
    "Unter dem Meer habe ich gefunden",
    "Im Wald habe ich getroffen",
    "Auf dem Bauernhof gab es",
    "In meinem Traum gab es",
    "Zum Zelten habe ich mitgenommen",
    "In der Spielzeugkiste ist",
    "In der Bäckerei habe ich gekauft",
    "In der Tasche des Zauberers war",
    "Auf der einsamen Insel habe ich gefunden",
    "Im Kühlschrank ist",
    "Im Zirkus habe ich gesehen",
    "In der Schatztruhe war",
    "Auf dem Markt habe ich gekauft",
    "Im Dschungel habe ich entdeckt",
    "Im Schloss habe ich gefunden",
    "In der Rakete zum Mond habe ich mitgenommen"
  )

  def forLang(lang: Lang): Vector[String] = lang match
    case Lang.Pl => pl
    case Lang.En => en
    case Lang.Es => es
    case Lang.Fr => fr
    case Lang.De => de
