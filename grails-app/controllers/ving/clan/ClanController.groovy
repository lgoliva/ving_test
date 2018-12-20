package ving.clan

import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional
import grails.converters.*

import jxl.Workbook
import jxl.write.Label
import jxl.write.WritableSheet
import jxl.write.WritableWorkbook

@Transactional(readOnly = true)
class ClanController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]
    def clanService

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond Clan.list(params), model:[clanInstanceCount: Clan.count()]
    }

    def clan(int id) {
          if (!id) {
                id = 4
          }

          def clan
          try {
                clan = ['clan':(clanService.getClan(id))]
          } catch (e) {
                clan = ['error':e]
          }
          render clan as JSON
    }

    def avaliacao() {
        redirect(url: clanService.avaliacaoSemanal())
    }

    def clans() {
         def inicio = new Date().time
         def list = [4,42,11,16,33]
         def resp = clanService.getClans(list)
         def fim = new Date().time
         def respp = [clans:resp, tempo:(fim-inicio)/1000]
         render respp as JSON
    }

    def faltasBPT(){
        String resp = clanService.obterFaltasBPT()
        render resp
    }

    def faltasCDT(){
      String resp = clanService.obterFaltasCDT()
      render resp
    }

    def faltas() {
        String resp = clanService.obterFaltas()
        render resp
    }

    def show(Clan clanInstance) {
        clanService.login()
        respond clanInstance
    }

    def compararForcaClans(){
      def resp = clanService.compararForcaClans();
      render resp as JSON
    }

    def compararBravuraClans(){
      def resp = clanService.compararBravuraClans();
      render resp as JSON
    }

    def estatisticasClans(){
      def resp = clanService.obterEstatisticasClans([4,11]);
      render resp as JSON
    }

    def obterPlanilhaClan(){
      response.setContentType('application/vnd.ms-excel')
      response.setHeader('Content-Disposition', 'Attachment;Filename="vings.xls"')

      WritableWorkbook workbook = Workbook.createWorkbook(response.outputStream)

      clanService.preencherPlanilhaEstatistica([4, 16, 33, 74], workbook);
      clanService.criarPlanilhaBravura(workbook, 1, [4, 16, 33, 74]);

      workbook.write();
      workbook.close();
    }

    def calcularForcas() {
        String resp = clanService.calcularForcas([4, 16, 33, 74])
        render resp
    }

    def obterResultados() {
        println "Vamos testar"
    }

    def create() {
        respond new Clan(params)
    }

    @Transactional
    def save(Clan clanInstance) {
        if (clanInstance == null) {
            notFound()
            return
        }

        if (clanInstance.hasErrors()) {
            respond clanInstance.errors, view:'create'
            return
        }

        clanInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'clan.label', default: 'Clan'), clanInstance.id])
                redirect clanInstance
            }
            '*' { respond clanInstance, [status: CREATED] }
        }
    }

    def edit(Clan clanInstance) {
        respond clanInstance
    }

    @Transactional
    def update(Clan clanInstance) {
        if (clanInstance == null) {
            notFound()
            return
        }

        if (clanInstance.hasErrors()) {
            respond clanInstance.errors, view:'edit'
            return
        }

        clanInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'Clan.label', default: 'Clan'), clanInstance.id])
                redirect clanInstance
            }
            '*'{ respond clanInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(Clan clanInstance) {

        if (clanInstance == null) {
            notFound()
            return
        }

        clanInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'Clan.label', default: 'Clan'), clanInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'clan.label', default: 'Clan'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
