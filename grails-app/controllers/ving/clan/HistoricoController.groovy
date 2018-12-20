package ving.clan



import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class HistoricoController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond Historico.list(params), model:[historicoInstanceCount: Historico.count()]
    }

    def show(Historico historicoInstance) {
        respond historicoInstance
    }

    def create() {
        respond new Historico(params)
    }

    @Transactional
    def save(Historico historicoInstance) {
        if (historicoInstance == null) {
            notFound()
            return
        }

        if (historicoInstance.hasErrors()) {
            respond historicoInstance.errors, view:'create'
            return
        }

        historicoInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'historico.label', default: 'Historico'), historicoInstance.id])
                redirect historicoInstance
            }
            '*' { respond historicoInstance, [status: CREATED] }
        }
    }

    def edit(Historico historicoInstance) {
        respond historicoInstance
    }

    @Transactional
    def update(Historico historicoInstance) {
        if (historicoInstance == null) {
            notFound()
            return
        }

        if (historicoInstance.hasErrors()) {
            respond historicoInstance.errors, view:'edit'
            return
        }

        historicoInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'Historico.label', default: 'Historico'), historicoInstance.id])
                redirect historicoInstance
            }
            '*'{ respond historicoInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(Historico historicoInstance) {

        if (historicoInstance == null) {
            notFound()
            return
        }

        historicoInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'Historico.label', default: 'Historico'), historicoInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'historico.label', default: 'Historico'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
