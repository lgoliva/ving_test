package ving.player

import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional
import grails.converters.*

class PlayerController {
    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]
    def playerService

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond Player.list(params), model:[playerInstanceCount: Player.count()]
    }

    def superW() {
        def a = ["super":playerService.superW()]
        render a as JSON
    }

    def show(Player playerInstance) {
        respond playerInstance
    }

    def brutus(String login) {
        render playerService.bruteForce(login) as JSON
    }

    def create() {
        respond new Player(params)
    }

    def obterPorId(Long id){
        render playerService.obterPorId(id) as JSON
    }

    def mapear() {
        render playerService.mapear() as JSON
    }

    @Transactional
    def save(Player playerInstance) {
        if (playerInstance == null) {
            notFound()
            return
        }

        if (playerInstance.hasErrors()) {
            respond playerInstance.errors, view:'create'
            return
        }

        playerInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'player.label', default: 'Player'), playerInstance.id])
                redirect playerInstance
            }
            '*' { respond playerInstance, [status: CREATED] }
        }
    }

    def edit(Player playerInstance) {
        respond playerInstance
    }

    @Transactional
    def update(Player playerInstance) {
        if (playerInstance == null) {
            notFound()
            return
        }

        if (playerInstance.hasErrors()) {
            respond playerInstance.errors, view:'edit'
            return
        }

        playerInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'Player.label', default: 'Player'), playerInstance.id])
                redirect playerInstance
            }
            '*'{ respond playerInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(Player playerInstance) {

        if (playerInstance == null) {
            notFound()
            return
        }

        playerInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'Player.label', default: 'Player'), playerInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'player.label', default: 'Player'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
