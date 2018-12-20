package ving.clan

class EdificioClan {
      Clan clan
      Edificio edificio
      int nivel
      Boolean ativo //null = 30%, false = 0%, true = 100% [tri-state]

      static constraints = {
            clan nullable:false
            edificio nullable:false
            nivel nullable:false
            ativo nullable:true
      }
}
