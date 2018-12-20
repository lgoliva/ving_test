package ving.clan

class Historico {
      Clan clan
      Acao acao
      Membro origem
      Membro destino
      Date data
      boolean sistema
      String descricao
      Patente patente

      static constraints = {
            clan nullable:false
            acao nullable:false
            origem nullable:false
            destino nullable:true
            data nullable:false
            sistema nullable:false
            descricao nullable:true
            patente nullable:true
      }
}
