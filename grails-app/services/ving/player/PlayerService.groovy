package ving.player

import groovy.json.*
import grails.transaction.Transactional
import ving.core.CoreService
import java.lang.RuntimeException

@Transactional
class PlayerService {
    //============= Injeção =============//
    def coreService
    //===================================//
    //============= Variáveis globais ===//
    private static String VING_URL_PREFIX = 'http://ving.mobi'
    private static String PLAYER_URL_PREFIX = VING_URL_PREFIX+'/view_profile?player_id='
    //===================================//

    def wakeup() {
        superW()
        coreService.request(VING_URL_PREFIX+"/auto_wakeup")
    }
    //TO-DO
    def getTop50Players(){}
        //regex navegação ==> pvp=&page=(\d+)
        //
    def superW() {
        def superW = '1';
        while (superW == '1') {
            superW = coreService.search(coreService.request(VING_URL_PREFIX+"/arena?super_wakeup=1"), "x(\\d+)")
        }
        superW

    }

    def mapear() {
        (0..300).each {
            println obterPorId(Long.parseLong("${it}"))
        }
        [status:'ok']
    }

    def bruteForce(String login) {
        println "?"
        def str = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890!$#@-:&%'
        def a = false
        def arr = []
        def senha = ''
        while (a==false) {
            println "??"
            (5..15).each { i ->
                (11978571669969891796072783721689098736458938142546425857555362864628009582789845319680000000000000000).times{
                    senha = generateString(new Random(),str,i)
                    while (arr.contains(senha)) {
                        senha = generateString(new Random(),str,i)
                    }
                    println senha
                    arr.add(senha)
                    try {
                        a = coreService.bruteForce(login)
                    } catch (e) {

                    }
                }

            }
        }
        println senha
        [(login):senha]
    }

    public static String generateString(Random rng, String characters, int length) {
        char[] text = new char[length]
        for (int i = 0; i < length; i++){
            text[i] = characters.charAt(rng.nextInt(characters.length()))
        }
        return new String(text)
    }

    def obterPorId(Long id) {
        Player player = Player.findById(id)
        String playerRequest
        if (player) {
            return player
        } else {
            player = new Player()
            playerRequest = coreService.search(coreService.request(PLAYER_URL_PREFIX+id),"""<div class=\\"yell mlr10 mt5 mb5\\">([\\w\\W]+?)<\\u002fdiv>""")
        }
        player.id = id
        player.nome = obterNome(playerRequest)
        if (player.nome == "Algoz" && id != 143865) {
            return [error:"Player inexistente"]
        }
        println player.nome
        println player.id
        player.nivel = Integer.parseInt(obterNivel(playerRequest))
        player.ultimoLogin = obterUltimoLogin(playerRequest)
        player.forca = obterForca(playerRequest)
        player.saude = obterSaude(playerRequest)
        player.armadura = obterArmadura(playerRequest)
        player.save(flush:true)
    }

    def getClanByPlayer(def playerId) {
          String playerRequest = coreService.request('http://ving.mobi/view_profile?player_id='+playerId)
          String nome = coreService.search(playerRequest,"\\w+\\W+: .+[^>]\\W+\\d+.>(.+)<\\/a>")
          String idClan = coreService.search(playerRequest,"\\w+\\W+: .+[^>]\\W+(\\d+).>.+<\\/a>")

          return [nome:nome, id:idClan]
    }

    def obterNome(String playerRequest) {
        String patternNome = "\\w+.\\/\\/\\d+\\.\\d+\\.\\d+\\.\\d+\\W\\w+\\W\\w+\\W\\w+\\W\\w+.\\w+\\W+(\\w.*?),\\s\\d+\\snível"
        coreService.search(playerRequest, patternNome)
    }

    def obterNivel(String playerRequest) {
        String patternNivel = "\\w+.\\/\\/\\d+\\.\\d+\\.\\d+\\.\\d+\\W\\w+\\W\\w+\\W\\w+\\W\\w+.\\w+\\W+\\w.*?,\\s(\\d+)\\snível"
        coreService.search(playerRequest, patternNivel)
    }

    def obterForca(String playerRequest) {
        String patternForca = 'Força:\\W+(\\d+)'
        Integer.parseInt(coreService.search(playerRequest, patternForca))
    }
    def obterSaude(String playerRequest) {
        String patternSaude = 'Saúde:\\W+(\\d+)'
        Integer.parseInt(coreService.search(playerRequest, patternSaude))
    }
    def obterArmadura(String playerRequest) {
        String patternArmadura = 'Armadura:\\W+(\\d+)'
        Integer.parseInt(coreService.search(playerRequest, patternArmadura))
    }
    def obterBravura(String playerRequest) {
        String patternBravura = 'Bravura<.a>:\\W+(\\d+)'
        Integer.parseInt(coreService.search(playerRequest, patternBravura))
    }
    def obterDiasClan(String playerRequest) {
        String patternDiasClan = 'Dias no Clã.\\W+(\\d+)'
        Integer.parseInt(coreService.search(playerRequest, patternDiasClan))
    }

    def obterUltimoLogin(String playerRequest) {
        Date dateLogin
        def leitorInicioSessao = coreService.search(playerRequest,'Último início de sessão:\\s+\\W\\w+\\s\\w+\\W+\\b(.+(\\d|\\w))<\\/span>').replaceAll("""lose\">""","")
        if (leitorInicioSessao) {
            switch(leitorInicioSessao) {
                case "hoje":
                    dateLogin = new Date()
                break
                case "ontem":
                    dateLogin = new Date() -1
                break
                default:
                    def listDate = leitorInicioSessao.split(" ")
                    def dia = Integer.parseInt(listDate[0])
                    def mes = listDate[1].replace(" ","")
                    def ano = Integer.parseInt(listDate[2].replace(" ",""))-1900
                    switch(mes) {
                        case 'jan':
                            mes = 0
                        break
                        case 'fev':
                            mes = 1
                        break
                        case 'mar':
                            mes = 2
                        break
                        case 'abr':
                            mes = 3
                        break
                        case 'mai':
                            mes = 4
                        break
                        case 'jun':
                            mes = 5
                        break
                        case 'jul':
                            mes = 6
                        break
                        case 'ago':
                            mes = 7
                        break
                        case 'set':
                            mes = 8
                        break
                        case 'out':
                            mes = 9
                        break
                        case 'nov':
                            mes = 10
                        break
                        case 'dez':
                            mes = 11
                        break
                        default:
                            throw RuntimeException("Não foi possível obter o mês")
                        break
                    }
                    dateLogin = new Date(ano,mes,dia)
                break
            }
        } else {
            dateLogin = new Date()
        }
        dateLogin
    }

}
