package ving.player

import ving.player.*
import ving.core.*

class PlayerJob {
    static triggers = {
        simple name: 'fluxoTrigger', repeatInterval: 7020000l // a cada 119 minutos
    }

	// INJEÇÃO DE DEPENDÊNCIA --------------------------------------------------
	def playerService
	// -------------------------------------------------------------------------

	def debug = true //Utilizado para exibir no console informações do job

	def debug(String string) {
		if (debug) {
			println string
		}
	}

    def execute() {
        // playerService.wakeup()
        // println "Ataque realizado ${new Date().format("HH:mm:ss")}"
    }
}
