package ving.clan



import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class AliancaController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond Alianca.list(params), model:[aliancaInstanceCount: Alianca.count()]
    }

    def show(Alianca aliancaInstance) {
        respond aliancaInstance
    }

    def create() {
        respond new Alianca(params)
    }

    @Transactional
    def save(Alianca aliancaInstance) {
        if (aliancaInstance == null) {
            notFound()
            return
        }

        if (aliancaInstance.hasErrors()) {
            respond aliancaInstance.errors, view:'create'
            return
        }

        aliancaInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'alianca.label', default: 'Alianca'), aliancaInstance.id])
                redirect aliancaInstance
            }
            '*' { respond aliancaInstance, [status: CREATED] }
        }
    }

    def edit(Alianca aliancaInstance) {
        respond aliancaInstance
    }

    @Transactional
    def update(Alianca aliancaInstance) {
        if (aliancaInstance == null) {
            notFound()
            return
        }

        if (aliancaInstance.hasErrors()) {
            respond aliancaInstance.errors, view:'edit'
            return
        }

        aliancaInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'Alianca.label', default: 'Alianca'), aliancaInstance.id])
                redirect aliancaInstance
            }
            '*'{ respond aliancaInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(Alianca aliancaInstance) {

        if (aliancaInstance == null) {
            notFound()
            return
        }

        aliancaInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'Alianca.label', default: 'Alianca'), aliancaInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'alianca.label', default: 'Alianca'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
