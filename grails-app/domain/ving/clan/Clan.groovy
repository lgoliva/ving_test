package ving.clan

class Clan {
      Long id
      String nome
      Alianca alianca
      int experiencia
      Date dataFundacao

      static hasMany = [
            membros:Membro,
            edificios:EdificioClan
      ]

      static constraints = {
            nome nullable:false
            membros nullable:false
            alianca nullable:true
            edificios nullable:true //Mudar pra false dps
            experiencia nullable:true  //Mudar pra false dps
            dataFundacao nullable:true  //Mudar pra false dps
      }

      static mapping = {
            id column: 'id', type: 'long', generator: 'assigned'
      }

}
