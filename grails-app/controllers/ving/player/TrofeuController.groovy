package ving.player



import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class TrofeuController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond Trofeu.list(params), model:[trofeuInstanceCount: Trofeu.count()]
    }

    def show(Trofeu trofeuInstance) {
        respond trofeuInstance
    }

    def create() {
        respond new Trofeu(params)
    }

    @Transactional
    def save(Trofeu trofeuInstance) {
        if (trofeuInstance == null) {
            notFound()
            return
        }

        if (trofeuInstance.hasErrors()) {
            respond trofeuInstance.errors, view:'create'
            return
        }

        trofeuInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'trofeu.label', default: 'Trofeu'), trofeuInstance.id])
                redirect trofeuInstance
            }
            '*' { respond trofeuInstance, [status: CREATED] }
        }
    }

    def edit(Trofeu trofeuInstance) {
        respond trofeuInstance
    }

    @Transactional
    def update(Trofeu trofeuInstance) {
        if (trofeuInstance == null) {
            notFound()
            return
        }

        if (trofeuInstance.hasErrors()) {
            respond trofeuInstance.errors, view:'edit'
            return
        }

        trofeuInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'Trofeu.label', default: 'Trofeu'), trofeuInstance.id])
                redirect trofeuInstance
            }
            '*'{ respond trofeuInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(Trofeu trofeuInstance) {

        if (trofeuInstance == null) {
            notFound()
            return
        }

        trofeuInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'Trofeu.label', default: 'Trofeu'), trofeuInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'trofeu.label', default: 'Trofeu'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
