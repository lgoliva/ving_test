package ving.clan

enum Edificio {
      ACADEMIA(1,"Academia"),
      CONHECIMENTO(2,"Arquivos do Conhecimento"),
      MAGIA(3,"Loja de Magia"),
      TROFEUS(4,"Sala dos Troféus"),
      ARMEIRO(5,"Armeiro")

      private final int id
      private final String nome

      public String nome() {
            return nome
      }

      public int id() {
            return id
      }



}
