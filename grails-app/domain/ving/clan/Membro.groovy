package ving.clan

import ving.player.Player

class Membro {
      boolean ativo
      Clan clan
      Player player
      Long experiencia

      static constraints = {
            ativo nullable:false
            clan nullable:false
            player nullable:false
            experiencia nullable:false
      }

}
