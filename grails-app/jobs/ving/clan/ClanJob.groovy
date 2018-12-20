package ving.clan

import ving.clan.*
import ving.core.*

class ClanJob {
    static triggers = {
        cron name:'cronTrigger', startDelay: 1, cronExpression: "0 57 14 ? * 1"
    }

	// INJEÇÃO DE DEPENDÊNCIA --------------------------------------------------
	def clanService
	// -------------------------------------------------------------------------

	def debug = true //Utilizado para exibir no console informações do job

	def debug(String string) {
		if (debug) {
			println string
		}
	}

    def execute() {
        println clanService.avaliacaoSemanal()
    }
}
