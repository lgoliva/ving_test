package ving.player

class Trofeu {
      String nome
      int atributos
      int porcentagemExperiencia
      int porcentagemPrata
      int diamantes
      int ordem

      static constraints = {
            nome nullable:false
            atributos nullable:false
            porcentagemExperiencia nullable:false
            porcentagemPrata nullable:false
            diamantes nullable:false
            ordem nullable:true
      }

}
