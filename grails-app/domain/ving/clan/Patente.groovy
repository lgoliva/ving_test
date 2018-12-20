package ving.clan

enum Patente {
      LIDER(1,"Líder"),
      MARECHAL(2,"Marechal"),
      GENERAL(3,"General"),
      OFICIAL(4,"Oficial"),
      VETERANO(5,"Veterano"),
      NOVATO(6,"Novato")

      private final int id
      private final String nome

      public String nome() {
            return nome
      }

      public int id() {
            return id
      }



}
