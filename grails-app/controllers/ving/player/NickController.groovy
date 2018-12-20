package ving.player



import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class NickController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond Nick.list(params), model:[nickInstanceCount: Nick.count()]
    }

    def show(Nick nickInstance) {
        respond nickInstance
    }

    def create() {
        respond new Nick(params)
    }

    @Transactional
    def save(Nick nickInstance) {
        if (nickInstance == null) {
            notFound()
            return
        }

        if (nickInstance.hasErrors()) {
            respond nickInstance.errors, view:'create'
            return
        }

        nickInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'nick.label', default: 'Nick'), nickInstance.id])
                redirect nickInstance
            }
            '*' { respond nickInstance, [status: CREATED] }
        }
    }

    def edit(Nick nickInstance) {
        respond nickInstance
    }

    @Transactional
    def update(Nick nickInstance) {
        if (nickInstance == null) {
            notFound()
            return
        }

        if (nickInstance.hasErrors()) {
            respond nickInstance.errors, view:'edit'
            return
        }

        nickInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'Nick.label', default: 'Nick'), nickInstance.id])
                redirect nickInstance
            }
            '*'{ respond nickInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(Nick nickInstance) {

        if (nickInstance == null) {
            notFound()
            return
        }

        nickInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'Nick.label', default: 'Nick'), nickInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'nick.label', default: 'Nick'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
