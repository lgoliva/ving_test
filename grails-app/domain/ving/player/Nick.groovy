package ving.player

class Nick {
    Player player
    String nome
    Date vistoPelaPrimeiraVez
    Date vistoPorUltimo

    static constraints = {
        player nullable:false
        nome nullable:false
        vistoPelaPrimeiraVez nullable:false
        vistoPorUltimo nullable:false
    }
}
