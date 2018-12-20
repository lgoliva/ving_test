package ving.clan



import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class EdificioClanController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond EdificioClan.list(params), model:[edificioClanInstanceCount: EdificioClan.count()]
    }

    def show(EdificioClan edificioClanInstance) {
        respond edificioClanInstance
    }

    def create() {
        respond new EdificioClan(params)
    }

    @Transactional
    def save(EdificioClan edificioClanInstance) {
        if (edificioClanInstance == null) {
            notFound()
            return
        }

        if (edificioClanInstance.hasErrors()) {
            respond edificioClanInstance.errors, view:'create'
            return
        }

        edificioClanInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'edificioClan.label', default: 'EdificioClan'), edificioClanInstance.id])
                redirect edificioClanInstance
            }
            '*' { respond edificioClanInstance, [status: CREATED] }
        }
    }

    def edit(EdificioClan edificioClanInstance) {
        respond edificioClanInstance
    }

    @Transactional
    def update(EdificioClan edificioClanInstance) {
        if (edificioClanInstance == null) {
            notFound()
            return
        }

        if (edificioClanInstance.hasErrors()) {
            respond edificioClanInstance.errors, view:'edit'
            return
        }

        edificioClanInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'EdificioClan.label', default: 'EdificioClan'), edificioClanInstance.id])
                redirect edificioClanInstance
            }
            '*'{ respond edificioClanInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(EdificioClan edificioClanInstance) {

        if (edificioClanInstance == null) {
            notFound()
            return
        }

        edificioClanInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'EdificioClan.label', default: 'EdificioClan'), edificioClanInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'edificioClan.label', default: 'EdificioClan'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
