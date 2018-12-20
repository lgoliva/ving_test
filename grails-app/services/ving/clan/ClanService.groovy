package ving.clan

import groovy.json.*
import org.apache.http.Consts
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.apache.http.HttpEntity
import org.apache.http.NameValuePair
import org.apache.http.util.EntityUtils
import grails.transaction.Transactional
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicNameValuePair
import org.apache.http.impl.client.BasicCookieStore
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.CloseableHttpResponse
import java.lang.RuntimeException
import ving.core.CoreService

import jxl.Workbook
import jxl.format.Alignment
import jxl.format.Colour
import jxl.format.Border
import jxl.format.BorderLineStyle
import jxl.write.Label
import jxl.write.Number
import jxl.write.WritableFont
import jxl.write.WritableSheet
import jxl.write.WritableCellFormat
import jxl.write.WritableWorkbook

@Transactional
class ClanService {
        //============= Injeção =============//
        def coreService
        def playerService
        //===================================//
        //============= Variáveis globais ===//
        private static String VING_URL_PREFIX = 'http://ving.mobi'
        private static String CLAN_URL_PREFIX = VING_URL_PREFIX+'/clan?id='
        private static BigDecimal VALOR_DIAMANTES = 10
        private static BigDecimal VALOR_XP = (1/500)
        private static BigDecimal VALOR_GOLD = 25
        private static BigDecimal VALOR_SILVER = (1/5000)
        //===================================//

        /**
         * Faz pontuação de acordo com numero de xp e prata feito por cada membro do clã
         * de acordo com usuário logado e cria um tópico no fórum do clã
         * @method avaliacaoSemanal
         * @return Link do tópico criado.
         */
        def avaliacaoSemanal() {
            //sufixo do link pro histórico das doações do clã
            String historySufix = "_history?id="
            //Faz uma requisição, que devolve o HTML do clã do usuário logado
            String responseBody = coreService.request("http://ving.mobi/clans")
            //Pega o link da tesouraria e o id do clã
            def tesourariaAndId = coreService.search(responseBody,"(\\/clan_budget\\?id=\\d+)").split('\\?id=')
            //Faz a requisição da primeira página da tesouraria
            responseBody = coreService.request("${VING_URL_PREFIX}${tesourariaAndId[0]}${historySufix}${tesourariaAndId[1]}")
            //Opções a serem contabilizadas
            def opt = [gold:1,silver:2,expirience:3,diamond:4]
            //Mapa com as doações
            def map = [:]
            for (it in opt) {
                map["${it.key}"] = []
                String url = "${VING_URL_PREFIX}${tesourariaAndId[0]}${historySufix}${tesourariaAndId[1]}&sort=${opt[it.key]}&page="
                def sHtml = "#"
                int num = 1
                while (sHtml) {
                    sHtml = coreService.search(coreService.request("${url}${num}"),'<div class="mt5 mlr10 mb5 sh lblue">([\\w\\W]+?)<div class="hr_g mb2">')
                    sHtml = sHtml.replaceAll('\t','').replaceAll('\r','').replaceAll('\n','').replaceAll("</div>",'')
                    if (sHtml) {
                        num++
                        map."${it.key}" += sHtml
                    }
                }
            }

            def avaliacao = [:]
            for (key in map.keySet()) {
                map[key].each { value ->
                    def players = value.split("""<div class=\"c-block\">""").findAll {
                        it != ""
                    }
                    def playerTemp = []
                    players.each { player ->
                        def playerAtual = [:]
                        def id = coreService.search(player,'href="\\/view_profile\\?player_id=(\\d+)')
                        int kv = Integer.parseInt(coreService.search(player,"""view/image/icons/${key}.png\\" />(\\d+)"""))
                        if (!avaliacao[(id)]) {
                            playerAtual['nome'] = coreService.search(player,'player_name\\">(.+?[^<])<\\u002fa>')
                            playerAtual[(key)] = (kv)
                            avaliacao[(id)] = playerAtual
                        } else {
                            avaliacao[(id)] << [(key):kv]
                        }
                    }
                }
            }

            Date date = new Date();

            def mes;
            if ("${date.month+1}".length()<2) {
                mes = "0${date.month+1}"
            } else {
                mes = "${date.month+1}"
            }

            def dia;
            if ("${date.date}".length()<2) {
                dia = "0${date.date}"
            } else {
                dia = "${date.date}"
            }

            def title = "Avaliação - ${dia}/${mes}/${date.year-100}"
            def avaliacaoFormatada = formatarAvaliacao(calculoAvaliacao(avaliacao),title)
            novoTopico(title, avaliacaoFormatada, tesourariaAndId[1])
        }


        def calculoAvaliacao(def avaliacao) {
            //Pontuação
            BigDecimal points = 0.0

            avaliacao.eachWithIndex { entry->
                points =  (entry.value.expirience ? entry.value.expirience : 0) * VALOR_XP
                points += (entry.value.diamond ? entry.value.diamond : 0) * VALOR_DIAMANTES
                points += (entry.value.gold ? entry.value.gold : 0) * VALOR_GOLD
                points += (entry.value.silver ? entry.value.silver : 0) * VALOR_SILVER
                points = points.setScale(0, BigDecimal.ROUND_CEILING)
                entry.value << ["pontuacao" : points]
            }
            avaliacao
        }

        def formatarAvaliacao(def avaliacao, def titulo) {
            int pos = 1
            int sumExp = avaliacao.values().sum { it.expirience?it.expirience:0 }
            int sumGold = avaliacao.values().sum { it.gold?it.gold:0 }
            int sumSilver = avaliacao.values().sum { it.silver?it.silver:0 }
            def infosClan = obterNomeBrasaoClan()
            def avaliacaoFormatada = ["[center][img=50]${infosClan.brasao}[/img][color=#EFF0A1][br][b] ${infosClan.nome} [/b][/color][br]${titulo}[/center][ul][li]Experiência Semanal Clã: ${sumExp}[/li][li]Ouro Semanal Clã: ${sumGold}[/li][li]Prata Semanal Clã: ${sumSilver}[/li][/ul]"]
            avaliacaoFormatada.add("[br]")
            (avaliacao.sort{-it.value.pontuacao}).eachWithIndex { entry, i ->
                int position = i+1;
                String colorBB = ""
                String icon = ""
                int posicaoPercentual = new BigDecimal("${(100/avaliacao.size())*(avaliacao.size()-(position-1))}").setScale(0,BigDecimal.ROUND_FLOOR)
                switch(posicaoPercentual) {
                    case 0..24:
                        colorBB = "[color=#FF3434]"
                        icon = "[img=18]view/image/smiles/39.gif[/img]"
                    break
                    case 25..50:
                        colorBB = "[color=#D4FF00]"
                        icon = "[img=18]view/image/smiles/26.gif[/img]"
                    break
                    case 51..70:
                        colorBB = "[color=#7AFE4E]"
                        icon = "[img=18]view/image/smiles/41.gif[/img]"
                    break
                    case 71..97:
                        colorBB = "[color=#7AFE4E]"
                        icon = "[img=18]view/image/smiles/16.gif[/img]"
                    break
                    case 98..100:
                        colorBB = "[color=#fff]"
                        icon = "[img=18]view/image/smiles/48.gif[/img]"
                    default:
                    break
                }

                String bb = "${colorBB}[b]${position}º - [url=view_profile?player_id=${entry.key}]${entry.value.nome}[/url][/b] ${icon}[/color][br][br]"
                bb += "[color=#DCF8C6][b][center]Pontuação: ${entry.value.pontuacao}[/center][/b]"
                bb += "([img=18]/view/image/icons/diamond.png[/img]) Diamantes:[br][right][i]Produção Semanal: ${entry.value.diamond?entry.value.diamond:0} ■■"
                bb += "[br]Média Diária: ${def d = (entry.value.diamond?entry.value.diamond:0); (d==0?0:(d/7).setScale(0, BigDecimal.ROUND_HALF_UP))} ■■[/i][/right][br]"
                bb += "([img=18]/view/image/icons/expirience.png[/img]) Experiência:[br][right][i]Produção Semanal:  ${entry.value.expirience?entry.value.expirience:0} ■■"
                bb += "[br]Média Diária: ${def xp = (entry.value.expirience?entry.value.expirience:0); (xp==0?0:(xp/7).setScale(0, BigDecimal.ROUND_HALF_UP))} ■■[/i][/right][br]"
                bb += "([img=18]/view/image/icons/gold.png[/img]) Ouro:[br][right][i]Produção Semanal: ${entry.value.gold?entry.value.gold:0} ■■"
                bb += "[br]Média Diária: ${def g = (entry.value.gold?entry.value.gold:0); (g==0?0:(g/7).setScale(0, BigDecimal.ROUND_HALF_UP))} ■■[/i][/right][br]"
                bb += "([img=18]/view/image/icons/silver.png[/img]) Prata:[br][right][i]Produção Semanal: ${entry.value.silver?entry.value.silver:0} ■■"
                bb += "[br]Média Diária: ${def s = (entry.value.silver?entry.value.silver:0); (s==0?0:(s/7).setScale(0, BigDecimal.ROUND_HALF_UP))} ■■[/i][/right]"
                bb += "[br][/color]"

                if (avaliacaoFormatada[pos].length()+bb.length() <= 2499) {
                    avaliacaoFormatada[pos] += bb
                } else {
                    pos++;
                    avaliacaoFormatada.add(bb)
                }
            }

            String formula = "[color=wheat][b][center]Fórmula da pontuação:[/center][/b][br](Diamantes * ${VALOR_DIAMANTES}) + (Experiência * ${VALOR_XP}) + (Ouro * ${VALOR_GOLD}) + (Prata * ${VALOR_SILVER})[/color]"
            avaliacaoFormatada.add(formula)

            avaliacaoFormatada
        }

        def novoTopico(String titulo, List texto, String clanId, boolean forumClan = true){
            String F_GENERAIS = 'Fórum dos Generais'
            String F_CLAN = 'Fórum do Clã'
            String urlClan = "http://ving.mobi/forum?id="+clanId
            String responseBody = coreService.request(urlClan)
            String forumEscolhido = '0'
            if (forumClan) {
                forumEscolhido = coreService.search(responseBody,"""href=\\"threads\\?id=(\\d+)\\"><img src=\\"http:\\/\\/144.76.127.94\\/view\\/image\\/icons\\/(\\w+|\\W+)?.png\\" class=\\"icon\\" \\/> ${F_CLAN}""")
            } else {
                forumEscolhido = coreService.search(responseBody,"""href=\\"threads\\?id=(\\d+)\\"><img src=\\"http:\\/\\/144.76.127.94\\/view\\/image\\/icons\\/(\\w+|\\W+)?.png\\" class=\\"icon\\" \\/> ${F_GENERAIS}""")
            }

            List <NameValuePair> nvps = new ArrayList <NameValuePair>()
            HttpPost httpPost = new HttpPost("http://ving.mobi/create_thread")
            nvps.add(new BasicNameValuePair("clan_only", 'on'))
            nvps.add(new BasicNameValuePair("forum_id", forumEscolhido))
            nvps.add(new BasicNameValuePair("thread_name", titulo))
            nvps.add(new BasicNameValuePair("thread_text", texto[0]))
            httpPost.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8))
            CloseableHttpResponse resposta = coreService.getClient().execute(httpPost)
            resposta.close()
            responseBody = coreService.request("http://ving.mobi/threads?id="+forumEscolhido)
            def substringTitulo = ""
            if (titulo.length()< 15) {
                substringTitulo = titulo
            } else {
                substringTitulo = titulo.substring(0, 15)
            }

            int idx = 1
            def topicoCriado = coreService.search(responseBody,"""thread\\?id=(\\d+)&thread_page=1\\" class=\\"mbtn mb2  \\"><img src=\\"http:\\/\\/144.76.127.94\\/view\\/image\\/icons\\/chat_read.png\\" class=\\"icon\\" \\/> ${substringTitulo}""")
            if (topicoCriado == null) {
                topicoCriado = coreService.search(responseBody,"""thread\\?id=(\\d+)&thread_page=2\\" class=\\"mbtn mb2  \\"><img src=\\"http:\\/\\/144.76.127.94\\/view\\/image\\/icons\\/chat_read.png\\" class=\\"icon\\" \\/> ${substringTitulo}""")
            }
            texto.eachWithIndex { it, i ->
                if (i!=0) {
                    httpPost = new HttpPost("http://ving.mobi/thread_message")
                    nvps.add(new BasicNameValuePair("message_text", it))
                    nvps.add(new BasicNameValuePair("answer_id", '0'))
                    nvps.add(new BasicNameValuePair("thread_id", topicoCriado))
                    httpPost.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8))
                    resposta = coreService.getClient().execute(httpPost)
                    resposta.close()
                }
            }
            coreService.request("http://ving.mobi/close_thread?id=${topicoCriado}")
            "http://ving.mobi/thread?id=${topicoCriado}&thread_page=1"
        }

        /**
        * Pesquisa informações sobre um clã
        * @param        clanId é o id do clã
        * @TO-DO        Pesquisar informações do histórico do clã
        * @return       retorna um mapa com informações do clã
        */
        def getClan(int clanId = 42) {
            String urlClan = CLAN_URL_PREFIX+clanId
            String responseBody = coreService.request(urlClan)
            [membros:obterMembros(clanId, responseBody),on:logMembros(clanId)]
        }

        def getClans(List clans) {
            def logClans = [:]
            clans.each { clanId ->
                def nome = obterNomeClanById(clanId);
                logClans << ["${(nome)}":logMembros(clanId)]
            }
            logClans
        }

        def obterNomeClanById(def clanId) {

            def responseBody = coreService.request("${CLAN_URL_PREFIX}${clanId}")

            String strBrasao ="\\w+...\\w+...\\d+.\\d+.\\d+.\\d+.(\\w+.\\w+.\\w+.herb\\d+.\\w+)"
            String strNomeClan = "mb.ml5.>(.{2,}?)<"

            String nomeClan = coreService.search(responseBody,strNomeClan)

            nomeClan
        }

        /**
        * Verifica informações dos jogadores online para facilitar decisões
        * @param  clanId id do clã alvo
        * @return        retorna um mapa com as informações dos players
        */
        def logMembros(int clanId = 42) {
            def list = obterMembros(clanId)
            def map = [:]
            map.qtdOnline = 0
            map.patente = [:]
            map.forcaTotalOnline = 0
            map.saudeTotalOnline = 0
            map.armaduraTotalOnline = 0
            list.each { it ->
                String responseBody = coreService.request('http://ving.mobi/view_profile?player_id='+it.id)
                if (it.online) {
                    map.qtdOnline++
                    if (map.patente["${it.patente}"]) {
                        map.patente["${it.patente}"]++
                    } else {
                        map.patente["${it.patente}"] = 1
                    }
                    map.forcaTotalOnline += playerService.obterForca(responseBody)
                    map.saudeTotalOnline += playerService.obterSaude(responseBody)
                    map.armaduraTotalOnline += playerService.obterArmadura(responseBody)
                }
            }
            map
        }

        /**
        * Obtém todos os membros de um clã
        * @param   clanId é o id do clã
        * @param   responseBody é a resposta de uma requisição para a página inicial do clã
        * @return  retorna uma lista onde cada membro é um mapa.
        */
        def obterMembros(def clanId, String responseBody = null) {
            if (!responseBody) {
                String urlClan = CLAN_URL_PREFIX+clanId
                responseBody = coreService.request(urlClan)
            }
            int membros = Integer.parseInt(coreService.search(responseBody,'Funcionários do Clã \\((\\d+)'))
            int paginas = ((BigDecimal) (membros/10)).setScale(0,BigDecimal.ROUND_CEILING)
            String htmlContext = '<div class="mt5 mb10 mlr10 sh">([\\w\\W]+?)</div>'
            def result = []
            (1..paginas).each { number ->
                String url = CLAN_URL_PREFIX+clanId+"&page="+number
                responseBody = coreService.request(url)
                def string = coreService.search(responseBody,htmlContext)
                (string.split("<br />")).each { stringMembro ->
                    def var = obterInformacoes(stringMembro);
                    if (var) {
                        result << var
                    }
                }
            }

            result.sort{x,y->
                y.xp <=> x.xp
            }

        }

        /**
        * Obtém todos os membros de um clã com seus respectivos status
        * @param   clanId é o id do clã
        * @param   responseBody é a resposta de uma requisição para a página inicial do clã
        * @return  retorna uma lista ordenada de forma decrescente pelo valor da forca de cada mapa que representa um membro.
        */
        def obterMembrosComStatus(def clanId, String responseBody = null) {
            if (!responseBody) {
                String urlClan = CLAN_URL_PREFIX+clanId
                responseBody = coreService.request(urlClan)
            }
            int membros = Integer.parseInt(coreService.search(responseBody,'Funcionários do Clã \\((\\d+)'))
            int paginas = ((BigDecimal) (membros/10)).setScale(0,BigDecimal.ROUND_CEILING)
            String htmlContext = '<div class="mt5 mb10 mlr10 sh">([\\w\\W]+?)</div>'
            def result = []
            (1..paginas).each { number ->
                String url = CLAN_URL_PREFIX+clanId+"&page="+number
                responseBody = coreService.request(url)
                def string = coreService.search(responseBody,htmlContext)
                (string.split("<br />")).each { stringMembro ->
                    def var = obterStatus(stringMembro);
                    if (var) {
                        result << var
                    }
                }
            }
            result.sort{x,y->
                y.forca <=> x.forca
            }
        }

        /**
        * Obtém o valor de bravura conquistado por cada membro do clã.
        * @param   clanId é o id do clã
        * @param   responseBody é a resposta de uma requisição para a página inicial do clã
        * @return  retorna um mapa com o total de bravura do clã e a lista de seus membros ordenada pela bravura.
        */
        def obterBravuraClan(def clanId, String responseBody = null){
            if (!responseBody) {
                String urlClan = CLAN_URL_PREFIX+clanId
                responseBody = coreService.request(urlClan)
            }
            int qtdMembros = Integer.parseInt(coreService.search(responseBody,'Funcionários do Clã \\((\\d+)'))
            int paginas = ((BigDecimal) (qtdMembros/10)).setScale(0,BigDecimal.ROUND_CEILING)
            String htmlContext = '<div class="mt5 mb10 mlr10 sh">([\\w\\W]+?)</div>'
            def membros = []
            def map = [:]
            map.bravuraTotal = 0
            map.membros = [];

            (1..paginas).each { number ->
                String url = CLAN_URL_PREFIX+clanId+"&page="+number
                responseBody = coreService.request(url)
                def string = coreService.search(responseBody,htmlContext)
                (string.split("<br />")).each { stringMembro ->
                    stringMembro = stringMembro.replaceAll('\t','')
                    stringMembro = stringMembro.replaceAll('\r','')
                    stringMembro = stringMembro.replaceAll('\n','')
                    if (stringMembro) {
                        def nome = coreService.search(stringMembro,'>(\\w.*?)<')
                        def id = coreService.search(stringMembro,'href="\\w+.\\w+.(\\w+)')

                        String profilePlayer = coreService.request('http://ving.mobi/view_profile?player_id='+id)
                        def bravura = playerService.obterBravura(profilePlayer)

                        map.bravuraTotal += bravura;
                        membros << [id:id, nome:nome, bravura: bravura]
                    }
                }
            }
            membros.sort{x,y->
                y.bravura <=> x.bravura
            }

            map.membros = membros;

            map;
        }

        def obterBravuraTotalClan(){
          String urlClan = VING_URL_PREFIX+'/clan_tournament'
          String responseBody = coreService.request(urlClan);
          String htmlContext = '(>\\d+)';

          def responseBodyList = responseBody.split("Bravura");

          def string = coreService.search(responseBodyList[2],htmlContext);
          def totalBravura = Integer.parseInt(string.substring(1));

          totalBravura;
        }

        /**
        * Obtém a informação de um membro baseado num padrão de string removido do HTML da resposta do vingadores
        * @param  sHtml é a String citada como resposta do vingadores
        * @return      caso exista um sHtml ele retorna um mapa com as informações do jogador, se não, retorna sHtml (string vazia)
        */
        def obterInformacoes(String sHtml){
            sHtml = sHtml.replaceAll('\t','')
            sHtml = sHtml.replaceAll('\r','')
            sHtml = sHtml.replaceAll('\n','')
            if (sHtml) {
                def online = sHtml.contains(/_on_/)
                def nome = coreService.search(sHtml,'>(\\w.*?)<')
                def patente = coreService.search(sHtml,'<span\\W[^>]+>(\\w.*?)</span>')
                def xp = coreService.search(sHtml,'(\\d+((\\d{0})|(\\.(\\d{2}|\\d{1})))(m|k|g)|\\((\\d+)\\))')
                def id = coreService.search(sHtml,'href="\\w+.\\w+.(\\w+)')

                String responseBody = coreService.request('http://ving.mobi/view_profile?player_id='+id)
                def diasClan = playerService.obterDiasClan(responseBody)

                return [online:online, nome:nome, patente:patente, xp:converterExperiencia(xp), id:id, dias:diasClan]
            }
            return sHtml
        }

        /**
        * Obtém informações sobre o status de um membro baseado num padrão de string extraído do HTML da resposta do vingadores
        * @param  sHtml é a String citada como resposta do vingadores
        * @return caso exista um sHtml ele retorna um mapa com as informações do jogador, se não, retorna sHtml (string vazia)
        */
        def obterStatus(String sHtml){
            sHtml = sHtml.replaceAll('\t','')
            sHtml = sHtml.replaceAll('\r','')
            sHtml = sHtml.replaceAll('\n','')
            if (sHtml) {
                def nome = coreService.search(sHtml,'>(\\w.*?)<')
                def id = coreService.search(sHtml,'href="\\w+.\\w+.(\\w+)')

                String responseBody = coreService.request('http://ving.mobi/view_profile?player_id='+id)
                def forca = playerService.obterForca(responseBody)
                def saude = playerService.obterSaude(responseBody)
                def arm = playerService.obterArmadura(responseBody)

                return [id:id, nome:nome, forca: forca, arm: arm, saude:saude]
            }
            return sHtml
        }

        /**
        * Compara a força dos membros de dois clãs.
        * @param   clanId1 é o id do primeiro clã da comparação
        * @param   clanId2 é o id do segundo clã da comparação
        * @return  retorna uma lista onde cada item é um mapa ['membro clan1', 'membro clan1', 'diff forca'].
        */
        def compararForcaClans(int clanId1 = 4, int clanId2 = 11){
          def result = [];
          def clan1 = obterMembrosComStatus(clanId1);
          def clan2 = obterMembrosComStatus(clanId2);
          def forcaTotalClan1 = 0;
          def forcaTotalClan2 = 0;

          int index = (clan1.size() <= clan2.size()) ? clan1.size() : clan2.size();

          for (def i=0; i<index; i++) {
              forcaTotalClan1 += clan1[i].forca;
              forcaTotalClan2 += clan2[i].forca;

              result << [clan1: clan1[i].nome, clan2:clan2[i].nome, diff:(clan1[i].forca - clan2[i].forca)]
          }

          if(clan1.size() == clan2.size()){
              result << [totalClan1: forcaTotalClan1, totalClan2:forcaTotalClan2, diff:(forcaTotalClan1 - forcaTotalClan2)]
          }

          result
        }

        /**
        * Compara o total de bravura de dois clãs.
        * @param   clanId1 é o id do primeiro clã da comparação
        * @param   clanId2 é o id do segundo clã da comparação
        * @return  retorna o mapa ['lista bravura clan1', 'lista bravura clan2', 'diff bravura'].
        */
        def compararBravuraClans(int clanId1 = 4, int clanId2 = 11){
          def clan1 = obterBravuraClan(clanId1);
          def clan2 = obterBravuraClan(clanId2);

          ["${(obterNomeClanById(clanId1))}":clan1, "${(obterNomeClanById(clanId2))}":clan2, diff:(clan1.bravuraTotal - clan2.bravuraTotal)]

        }

        /**
        * Obtém todos os membros de um clã que participaram da BPT.
        * @param   clanId é o id do clã.
        * @param   responseBody é a resposta de uma requisição para a página de histórico da BPT.
        * @return  retorna uma lista com os ids dos membros que compareceram à BPT.
        */
        def obterMembrosBPT(def clanId, String responseBody = null) {
            if (!responseBody) {
                String urlBPTHistory = VING_URL_PREFIX+"/throne_history"
                responseBody = coreService.request(urlBPTHistory)
            }

            String s = """(<tr class="lblue"><td class="w33 plr10 nwr">.+\\d+<\\/td><\\/tr>)"""
            String string = coreService.search(responseBody,s)
            List a = string.split("nwr\"><a")
            a.remove(0)

            String htmlContext = 'id\\W(\\d+)\\W+'
            List result = []

            a.each {
                 string = coreService.search(it, htmlContext)
                 result.add(string)
            }

            result
        }

        /**
        * Obtém todos os membros de um clã que participaram do CDT.
        * @param   clanId é o id do clã.
        * @param   responseBody é a resposta de uma requisição para a página de histórico do CDT.
        * @return  retorna uma lista com os ids dos membros que compareceram ao CDT.
        */
        def obterMembrosCDT(def clanId, String responseBody = null) {
            if (!responseBody) {
                String urlBPTHistory = VING_URL_PREFIX+"/tower_history"
                responseBody = coreService.request(urlBPTHistory)
            }

            String s = """(<tr class="lblue"><td class="w33 plr10 nwr lft"><img src="http://144.76.127.94/view/image/icons/our.png" class="icon" alt=""/>.+\\d+<\\/td><\\/tr>)"""
            String string = coreService.search(responseBody,s)
            List a = string.split("><a")

            a.remove(0)

            String htmlContext = 'id\\W(\\d+)\\W+'
            def result = []

            a.each {
                 string = coreService.search(it, htmlContext)
                 result.add(string)
            }

            //Adiciona o comandante do CDT na lista de participantes
            s = "commander.\\w+.{2,}?id.(\\d+)"
            string = coreService.search(responseBody,s)

            if(string){
                result.add(string)
            }

            result
        }

        /**
        * Obtém todos os membros de um clã que não compareceram a um evento (CDT ou BPT)
        * @param   clanId é o id do clã.
        * @param   membrosEvento é a lista com todos os membros do clã que compareceram ao evento (CDT ou BPT)
        * @return  retorna uma lista com o nome dos membros que não compareceram ao evento.
        */
        def obterMembrosAusentes(List idsEvento, def clanId){
          List membrosCla = obterMembros(clanId)
          List ausentes = []

          membrosCla.each {
              if (!idsEvento.contains(it.id)) {
                    ausentes.add(it.nome)
              }
          }
          ausentes
        }

        def obterDiaMes() {
            Date date = new Date()

            def mes;
            if ("${date.month+1}".length()<2) {
                mes = "0${date.month+1}"
            } else {
                mes = "${date.month+1}"
            }

            def dia;
            if ("${date.date}".length()<2) {
                dia = "0${date.date}"
            } else {
                dia = "${date.date}"
            }

            [dia:dia, mes:mes]
        }

        /**
        * Obtém todos os membros de um clã que faltaram a BPT
        * @return  retorna uma lista com o nome dos membros que não compareceram à BPT.
        */
        def obterFaltasBPT(){
            def clanId = obterIdClan()
            List membrosBPT = obterMembrosBPT(clanId)
            def postagens = []
            def diaMes = obterDiaMes()
            // String faltosos = "[center][color=wheat]Membros [b]faltosos[/b] da [/color][color=gold]Batalha pelo Trono[/color][color=wheat] do dia: [b]${diaMes.dia}/${diaMes.mes}[/b][/center][br][br]"
            def listFaltas = obterMembrosAusentes(membrosBPT, clanId)
            String mensagem = "FALTAS BPT: "

            listFaltas.eachWithIndex { membro, i ->
                if (i<listFaltas.size()-1) {
                    mensagem += "${membro}, "
                } else {
                    mensagem += "${membro}."
                }
            }
            // postagens.add(faltosos)
            return mensagem
            // ["Link:",novoTopico("FALTAS BPT ${diaMes.dia}/${diaMes.mes}", postagens, clanId)]
        }

        /**
        * Obtém todos os membros de um clã que faltaram o CDT)
        * @return  retorna uma lista com o nome dos membros que não compareceram ao CDT.
        */
        def obterFaltasCDT(){
            def clanId = obterIdClan()
            obterMembrosCDT(clanId)
            List membrosCDT = obterMembrosCDT(clanId)
            // def postagens = []
            def diaMes = obterDiaMes()
            // String faltosos = "[center][color=wheat]Membros [b]faltosos[/b] do [/color][color=gold]Cerco das Torres[/color][color=wheat] do dia: [b]${diaMes.dia}/${diaMes.mes}[/b][/center][br][br]"
            def listFaltas = obterMembrosAusentes(membrosCDT, clanId)
            String mensagem = "FALTAS CDT: "

            listFaltas.eachWithIndex { membro, i ->
              if (i<listFaltas.size()-1) {
                mensagem += "${membro}, "
              } else {
                mensagem += "${membro}."
              }
            }
            // postagens.add(faltosos)

            return mensagem
        //    ["Link:",novoTopico("FALTAS CDT ${diaMes.dia}/${diaMes.mes}", postagens, clanId)]
        }

        def obterFaltas() {
            String mensagem = ""

            mensagem += obterFaltasCDT()

            if (mensagem != "") {
                mensagem = "${mensagem}<br/>"
            }

            mensagem += obterFaltasBPT()

            mensagem
        }

        def obterFaltasTopico() {

        }

        def lerTopico(String titulo, boolean generais = false) {
            //coreService.search(responseBody,"""thread\\?id=(\\d+)&thread_page=1\\" class=\\"mbtn mb2  \\"><img src=\\"http:\\/\\/144.76.127.94\\/view\\/image\\/icons\\/\\w+[\\s\\S]\\w+.png\\" class=\\"icon\\" \\/> ${titulo}""")
        }

        /**
         * Obtém o id do clã do jogador logado.
         * @method obterIdClan
         * @return id do clã.
         */
        String obterIdClan() {
            def responseBody = coreService.request("${VING_URL_PREFIX}/profile")
            coreService.search(responseBody,"\\w+..\\s\\w+...clan\\?\\w+.(\\d+)")
        }

        /**
        * Obtém o nome e o brasão do clã do jogador logado.
        * @method obterNomeBrasaoClan
        * @return mapa com as informações do clã.
        */
        def obterNomeBrasaoClan() {
            def clanId = obterIdClan()
            def responseBody = coreService.request("${CLAN_URL_PREFIX}${clanId}")

            String strBrasao ="\\w+...\\w+...\\d+.\\d+.\\d+.\\d+.(\\w+.\\w+.\\w+.herb\\d+.\\w+)"
            String strNomeClan = "mb.ml5.>(.{2,}?)<"

            String brasao = coreService.search(responseBody,strBrasao)
            String nomeClan = coreService.search(responseBody,strNomeClan)

            [nome:nomeClan, brasao:brasao]
        }

        /**
        * Recebe um valor (ex: 300m) e multiplica o número pela letra para obter o valor real de experiência
        * @param  xp É o valor de experiência que a pessoa tem no jogo (ex: 300m)
        * @return    um inteiro correspondente a experiência
        */
        int converterExperiencia(String xp) {
            int K = 1000
            int M = 1000000
            int G = 100000000
            int xpMembro = 0
            String var
            String experienciaAbaixoDeMil = coreService.search(xp,'\\((\\d+)\\)')
            if (experienciaAbaixoDeMil) {
                xp = experienciaAbaixoDeMil
            } else {
                var = xp[xp.length()-1]
            }
            switch(var) {
                case 'k':
                    xpMembro = ((BigDecimal)Float.parseFloat(xp.replace(xp[xp.length()-1],''))).setScale(2,BigDecimal.ROUND_CEILING) * K
                break
                case 'm':
                    xpMembro = ((BigDecimal)Float.parseFloat(xp.replace(xp[xp.length()-1],''))).setScale(2,BigDecimal.ROUND_CEILING) * M
                break
                case 'g':
                    xpMembro = ((BigDecimal)Float.parseFloat(xp.replace(xp[xp.length()-1],''))).setScale(2,BigDecimal.ROUND_CEILING) * G
                break
                default:
                    xpMembro = Integer.parseInt(xp)
                break
            }
            xpMembro
        }

        /**
        * Obtém as informações sobre o status de um clã: total de força, saúde e armadura, bem como a lista de membros por força.
        * @param clanId Id do clã.
        * @return mapa com as informações sobre o status do clã.
        */
        def obterEstatisticaFullById(int clanId = 4) {
            def list = obterMembrosComStatus(clanId)
            def map = [:]
            map.forcaTotal = 0
            map.saudeTotal = 0
            map.armaduraTotal = 0
            map.s2 = [] //>3k de força
            map.s3 = []
            map.s4 = []
            map.s5 = []
            map.s6 = []
            map.s7 = []
            map.s8 = []
            map.s9 = []
            map.s10 = [] //<=10k de força

            list.each { it ->
                String responseBody = coreService.request('http://ving.mobi/view_profile?player_id='+it.id)

                def forca = playerService.obterForca(responseBody)
                def saude = playerService.obterSaude(responseBody)
                def arm = playerService.obterArmadura(responseBody)

                map.forcaTotal += forca
                map.saudeTotal += saude
                map.armaduraTotal += arm

                def numStatus = (forca - (forca % 1000))/1000

                if(numStatus <= 2){
                    map.s2 << [id:it.id, nome:it.nome, forca: forca, arm: arm, saude:saude]
                }else if(numStatus >= 10){
                    map.s10 << [id:it.id, nome:it.nome, forca: forca, arm: arm, saude:saude]
                }else{
                    map["s"+numStatus] << [id:it.id, nome:it.nome, forca: forca, arm: arm, saude:saude]
                }
            }
            map
        }

        /**
        * Obtém as informações sobre o status de um clã: total de força, saúde e armadura, bem como a lista de membros por força.
        * @param clanId Id do clã.
        * @return mapa com as informações sobre o status do clã.
        */
        def obterEstatisticaById(int clanId = 4) {
            def list = obterMembrosComStatus(clanId)
            def map = [:]
            map.forcaTotal = 0
            map.saudeTotal = 0
            map.armaduraTotal = 0
            map.membros = [];

            list.each { it ->
                String responseBody = coreService.request('http://ving.mobi/view_profile?player_id='+it.id)

                def forca = playerService.obterForca(responseBody)
                def saude = playerService.obterSaude(responseBody)
                def arm = playerService.obterArmadura(responseBody)
                def classificacao = forca + arm + saude;

                map.forcaTotal += forca
                map.saudeTotal += saude
                map.armaduraTotal += arm

                map.membros << [id:it.id, nome:it.nome, classificacao: classificacao, forca: forca]
            }
            map
        }

        def obterEstatisticasClans(List clans, boolean full = true) {
            def estatisticas = [:]
            clans.each { clanId ->
                def nome = ''
                switch(clanId) {
                    case 4:
                        nome = "LS"
                    break
                    case 11:
                        nome = "FC"
                    break
                    case 16:
                        nome = "FOT"
                    break
                    case 33:
                        nome = "DMC"
                    break
                    case 75:
                        nome = "Tinf"
                    break
                    default:
                        nome = "$clanId"
                    break
                }
                if(full){
                    estatisticas << ["${(nome)}":obterEstatisticaFullById(clanId)]
                }else{
                    estatisticas << ["${(nome)}":obterEstatisticaById(clanId)]
                }
            }
            estatisticas
        }


        def preencherPlanilhaEstatistica(List clans, WritableWorkbook workbook, def sheetId = 0){
          def estatisticas = obterEstatisticasClans(clans, false);
          def labels = estatisticas.keySet() as String[];
          def values = estatisticas.values();
          def linha = 5;
          def coluna = 0;
          def OFFSET_COLUNA = 5
          def numForca;

          //Adiciona nova folha no arquivo xls
          WritableSheet sheet = workbook.createSheet("Clans", sheetId);

          WritableCellFormat titleCellFormat = obterFormatacaoTituloCelula();
          WritableCellFormat valueCellFormat = obterFormatacaoValorCelula();

          WritableCellFormat infoCellFormat;

          for (def i = 0; i < values.size() ; i++) {
             def mapClan = values[i];

             //Adiciona cabecalho Forca Total, Saude Total, Armadura Total
             sheet.addCell(new Label(coluna+1,1, "Forca Total", titleCellFormat))
             sheet.addCell(new Label(coluna+2,1, "Saude Total", titleCellFormat))
             sheet.addCell(new Label(coluna+3,1, "Armadura Total", titleCellFormat))

             //Preenche a planilha com os valores de forca, saude e armadura total do clan
             sheet.addCell(new Number(coluna+1,2, mapClan.forcaTotal.doubleValue(), valueCellFormat))
             sheet.addCell(new Number(coluna+2,2, mapClan.saudeTotal.doubleValue(), valueCellFormat))
             sheet.addCell(new Number(coluna+3,2, mapClan.armaduraTotal.doubleValue(), valueCellFormat))

             //Adiciona cabecalho da tabela de membros
             sheet.addCell(new Label(coluna,4, "ID"))
             sheet.addCell(new Label(coluna+1,4, "Nome", titleCellFormat))
             sheet.addCell(new Label(coluna+2,4, "Forca", titleCellFormat))
             sheet.addCell(new Label(coluna+3,4, "Classificacao", titleCellFormat))

             def membros = mapClan.membros;

             for(def j = 0; j < membros.size(); j++){
                def membro = membros[j];
                def forca = membro.forca;

                numForca = (forca - (forca % 1000))/1000
                infoCellFormat = obterFormatacaoCelulaForca(numForca.intValue());

                //Preenche a planilha com os dados dos membros do clan
                sheet.addCell(new Number(coluna,linha, Double.parseDouble(membro.id)))
                sheet.addCell(new Label(coluna+1,linha, membro.nome, infoCellFormat))
                sheet.addCell(new Number(coluna+2,linha, forca.doubleValue(), infoCellFormat))
                sheet.addCell(new Number(coluna+3,linha, membro.classificacao.doubleValue(), infoCellFormat))
                linha++;
             }

             linha = 5;
             coluna += OFFSET_COLUNA;
          }
        }

        def obterFormatacaoTituloCelula(){
            //Cria objetos para formatar as celulas com os titulos das colunas
            WritableFont cellFont = new WritableFont(WritableFont.ARIAL, 10);
            cellFont.setBoldStyle(WritableFont.BOLD);

            WritableCellFormat titleCell = new WritableCellFormat(cellFont);
            titleCell.setBorder(Border.BOTTOM, BorderLineStyle.MEDIUM);
            titleCell.setBorder(Border.RIGHT, BorderLineStyle.MEDIUM);
            titleCell.setAlignment(Alignment.CENTRE);

            return titleCell;
        }

        def obterFormatacaoValorCelula(Boolean centraliza = true){
            WritableCellFormat valueCell = new WritableCellFormat();
            valueCell.setBorder(Border.RIGHT, BorderLineStyle.MEDIUM);
            if(centraliza){
                valueCell.setAlignment(Alignment.CENTRE);
            }

            return valueCell;
        }

        def obterFormatacaoCelulaForca(int forca){
          WritableCellFormat cellFormat = new WritableCellFormat();
          cellFormat.setBorder(Border.BOTTOM, BorderLineStyle.THIN);
          cellFormat.setBorder(Border.RIGHT, BorderLineStyle.MEDIUM);

           switch(forca){
               case 13:
                   cellFormat.setBackground(Colour.INDIGO);
                   break
               case 12:
                   cellFormat.setBackground(Colour.TEAL);
                   break
               case 11:
                  cellFormat.setBackground(Colour.BLUE2);
                   break
               case 10:
                   cellFormat.setBackground(Colour.LIGHT_ORANGE);
                   break
               case 9:
                   cellFormat.setBackground(Colour.LIME);
                   break
               case 8:
                   cellFormat.setBackground(Colour.CORAL);
                   break
               case 7:
                   cellFormat.setBackground(Colour.LAVENDER);
                   break
               case 6:
                   cellFormat.setBackground(Colour.GOLD);
                   break
               case 5:
                   cellFormat.setBackground(Colour.ROSE);
                   break
               case 4:
                   cellFormat.setBackground(Colour.PALE_BLUE);
                   break
               case 3:
                   cellFormat.setBackground(Colour.TAN);
                   break
               case 2:
                   cellFormat.setBackground(Colour.YELLOW2);
                   break
               default:
                   cellFormat.setBackground(Colour.GRAY_25);
                   break
            }
            return cellFormat;
        }

        def criarPlanilhaBravura(WritableWorkbook workbook, def sheetId = 0, List clans){
            def listaBravuraClans = []

            for (idClan in clans) {
                listaBravuraClans.add(obterBravuraClan(idClan))
            }

            def bravuraTotalClan1 = obterBravuraTotalClan();

            WritableSheet sheet = workbook.createSheet("Bravura", sheetId);

            sheet.addCell(new Label(1,1, "Bravura Clan: "));
            sheet.addCell(new Number(2,1, bravuraTotalClan1));

            int coluna = 0

            for (bravura in listaBravuraClans) {
                preencherPlanilhaBravura(sheet, bravura, coluna);
                coluna += 4
            }
        }

        def preencherPlanilhaBravura(WritableSheet sheet, def mapClan, int coluna = 0){
            def linha = 5;

            //Adiciona cabecalho e valor da soma das bravuras dos memebros do clan.
            sheet.addCell(new Label(coluna+1,2, "Bravura Total"))
            sheet.addCell(new Number(coluna+2,2, mapClan.bravuraTotal.doubleValue()))

            WritableCellFormat titleCellFormat = obterFormatacaoTituloCelula();
            WritableCellFormat valueCellFormat = obterFormatacaoValorCelula(false);

            //Adiciona cabecalho da tabela de membros
            sheet.addCell(new Label(coluna,4, "ID"))
            sheet.addCell(new Label(coluna+1,4, "Nome", titleCellFormat))
            sheet.addCell(new Label(coluna+2,4, "Bravura", titleCellFormat));

             def membros = mapClan.membros;

             for(def j = 0; j < membros.size(); j++){
                def membro = membros[j];

                //Preenche a planilha com os dados dos membros do clan
                sheet.addCell(new Number(coluna,linha, Double.parseDouble(membro.id)))
                sheet.addCell(new Label(coluna+1,linha, membro.nome, valueCellFormat))
                sheet.addCell(new Number(coluna+2,linha, membro.bravura.doubleValue(), valueCellFormat))
                linha++;
            }
        }

        def preencherPlanilhaXP(WritableWorkbook workbook, def sheetId = 0){
            def membros = obterMembros(4);
            def linha = 2;

            WritableSheet sheet = workbook.createSheet("XP", sheetId);

            //Adiciona cabecalho da tabela de membros
            sheet.addCell(new Label(0, 1, "ID"))
            sheet.addCell(new Label(1,1, "Nome"))
            sheet.addCell(new Label(2,1, "XP"));
            sheet.addCell(new Label(3,1, "Média XP"));

            for(def j = 0; j < membros.size(); j++){
               def membro = membros[j];

               //Preenche a planilha com os dados dos membros do clan
               sheet.addCell(new Number(0,linha, Double.parseDouble(membro.id)))
               sheet.addCell(new Label(1,linha, membro.nome))
               sheet.addCell(new Number(2,linha, membro.xp.doubleValue()))
               sheet.addCell(new Number(3,linha, (membro.xp/membro.dias)))
               linha++;
           }
        }


    def calcularForcas(def lista) {

        String resposta = ""

        lista.each { clanId ->
            def map = [:]
            map.clan = obterNomeClanById(clanId)
            def membros = obterMembros(clanId)
            map.quantidadeMembros = membros.size()
            map.forcaTotal = 0
            map.saudeTotal = 0
            map.armaduraTotal = 0

            membros.each { it ->
                String responseBody = coreService.request('http://ving.mobi/view_profile?player_id='+it.id)
                map.forcaTotal += playerService.obterForca(responseBody)
                map.saudeTotal += playerService.obterSaude(responseBody)
                map.armaduraTotal += playerService.obterArmadura(responseBody)
            }

            resposta += """
                Clã: ${map.clan}<br>
                Força Total: ${map.forcaTotal}<br>
                Saúde Total: ${map.saudeTotal}<br>
                Armadura Total: ${map.armaduraTotal}<br>
                <br><br>
            """
        }

        resposta
    }

}
