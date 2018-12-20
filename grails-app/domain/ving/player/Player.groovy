package ving.player

class Player {
    Long id
    String nome
    Date ultimoLogin
    int forca
    int saude
    int armadura
    int nivel

    static hasMany = [
        trofeus:Trofeu,
        nomes:Nick
    ]

    static constraints = {
        nome nullable:false, unique:true
        trofeus nullable:true //Mudar pra false dps
        nomes nullable:true  //Mudar pra false dps
        ultimoLogin nullable:false
        forca nullable:false
        saude nullable:false
        armadura nullable:false
        nivel nullable:false
    }

    static mapping = {
        id column: 'id', type: 'long', generator: 'assigned'
    }
    static namedQueries = {
      ultimaTroca {
          nomes{
              projections{
                  max 'vistoPelaPrimeiraVez'
              }
          }
      }
    }
}
