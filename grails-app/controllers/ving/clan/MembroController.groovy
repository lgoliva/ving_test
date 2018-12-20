package ving.clan



import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class MembroController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond Membro.list(params), model:[membroInstanceCount: Membro.count()]
    }

    def show(Membro membroInstance) {
        respond membroInstance
    }

    def create() {
        respond new Membro(params)
    }

    @Transactional
    def save(Membro membroInstance) {
        if (membroInstance == null) {
            notFound()
            return
        }

        if (membroInstance.hasErrors()) {
            respond membroInstance.errors, view:'create'
            return
        }

        membroInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'membro.label', default: 'Membro'), membroInstance.id])
                redirect membroInstance
            }
            '*' { respond membroInstance, [status: CREATED] }
        }
    }

    def edit(Membro membroInstance) {
        respond membroInstance
    }

    @Transactional
    def update(Membro membroInstance) {
        if (membroInstance == null) {
            notFound()
            return
        }

        if (membroInstance.hasErrors()) {
            respond membroInstance.errors, view:'edit'
            return
        }

        membroInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'Membro.label', default: 'Membro'), membroInstance.id])
                redirect membroInstance
            }
            '*'{ respond membroInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(Membro membroInstance) {

        if (membroInstance == null) {
            notFound()
            return
        }

        membroInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'Membro.label', default: 'Membro'), membroInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'membro.label', default: 'Membro'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
