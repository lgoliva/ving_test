package ving.core

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
import org.apache.http.protocol.HttpContext
import org.apache.http.protocol.BasicHttpContext
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.client.methods.HttpHead
import org.apache.http.client.methods.HttpUriRequest


@Transactional
class CoreService {
      // static public String USER = 'Ousawa'
      // static public String PASS = '@c6q5yimc#'
    //   static public String USER = 'mamba odn'
    //   static public String PASS = 'jesuscristol'
    static public String USER = 'Lukta'
    static public String PASS = '@ds2vg6u9'
      static public def HTTP_CLIENT = null


      def getClient() {
          if (!HTTP_CLIENT) {
                login(USER,PASS)
          }
          HTTP_CLIENT
      }

      /**
      * Faz login da acc no vingadores.
      * @param  login é o login do usuário.
      * @param  senha é a senha do usuário.
      */
      private void login(String login, String senha){
            println "logou"
            def cookieStore = new BasicCookieStore()
            def http_client = HttpClients.custom().setDefaultCookieStore(cookieStore).build()
            HttpPost httpPost = new HttpPost("http://ving.mobi/login")
            List <NameValuePair> nvps = new ArrayList <NameValuePair>()
            nvps.add(new BasicNameValuePair("name", login))
            nvps.add(new BasicNameValuePair("password", senha))

            httpPost.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8))
            CloseableHttpResponse resposta = http_client.execute(httpPost)

            HttpEntity entity = resposta.getEntity()
            String responseBody = EntityUtils.toString(entity)
            if (search("${resposta}","\\W(welcome\\?error)")) {
                  throw new RuntimeException("Login ou senha inválido.")
            }
            resposta.close()
            HTTP_CLIENT = http_client
      }

      /**
      * Faz login da acc no vingadores.
      * @param  login é o login do usuário.
      * @param  senha é a senha do usuário.
      */
      def bruteForce(String login, String senha){
            def cookieStore = new BasicCookieStore()
            def http_client = HttpClients.custom().setDefaultCookieStore(cookieStore).build()
            HttpPost httpPost = new HttpPost("http://ving.mobi/login")
            List <NameValuePair> nvps = new ArrayList <NameValuePair>()
            nvps.add(new BasicNameValuePair("name", login))
            nvps.add(new BasicNameValuePair("password", senha))

            httpPost.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8))
            CloseableHttpResponse resposta = http_client.execute(httpPost)

            HttpEntity entity = resposta.getEntity()
            String responseBody = EntityUtils.toString(entity)
            if (search("${resposta}","\\W(welcome\\?error)")) {
                 return false
            }
            resposta.close()
            HTTP_CLIENT = http_client
            return true
      }

      /**
      * Faz uma requisição qualquer na url indicada
      * @param  url é a url indicada para a requisição
      * @return     retorna a resposta da requisição.
      */
      private String request(String url, Map settings = null) {
            CloseableHttpResponse resposta
            if (settings?.requestType == "POST") {
                  def httpRequest = new HttpPost(url)
                  if (settings.params) {
                        List <NameValuePair> nvps = new ArrayList <NameValuePair>()
                        (settings.params.keySet()).each { key -> //Params = [k:v, k:v, k:v]
                              nvps.add(new BasicNameValuePair("$key", "${settings.params["$key"]}"))
                        }
                        httpRequest.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8))
                        resposta = getClient().execute(httpRequest)
                  }
            } else {
                  resposta = getClient().execute(new HttpGet(url))
            }
            def entity = resposta.getEntity()
            String responseBody = EntityUtils.toString(entity)
            resposta.close()
            return responseBody.replaceAll('\t','').replaceAll('\r','').replaceAll('\n','')
      }

      /**
       * Pesquisa por um padrão na String indicada
       * @param   stringToSearch é a string para pesquisar
       * @param   pattern é o padrão a ser localizado na String /regex/
       * @return  retorna o primeiro grupo do padrão localizado.
       */
      def search(String stringToSearch, String pattern) {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(stringToSearch);
            String retorno
            if (m.find()) {
                  retorno=m.group(1)
            }
            retorno
      }
}
