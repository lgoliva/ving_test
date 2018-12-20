package ving.clan

class Alianca {
      boolean ativo

      static hasMany = [
            clans:Clan,
      ]

      static constraints = {
            ativo nullable:false
      }

}
