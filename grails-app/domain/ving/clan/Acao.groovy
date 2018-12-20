package ving.clan

enum Acao {
      EXPULSOU(1,"expulsou"),
      RECEBEU(2,"recebeu"),
      SAIU(3,"saiu"),
      PROMOVEU(4,"promoveu"),
      MUDOU(5,"mudou"),
      DESPROMOVEU(6,"despromoveu"),
      FUNDOU(7,"fundou")

      private final int id
      private final String nome

      public String nome() {
            return nome
      }

      public int id() {
            return id
      }



}
