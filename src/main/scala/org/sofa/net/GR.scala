package org.sofa.net

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.net.URLConnection

object GR {
}

/** Google Reader access provider. */
class GR(val user:String, val pass:String) {
    val _loginURL = "https://www.google.com/accounts/ClientLogin"
    val _tokenURL = "http://www.google.com/reader/api/0/token"

    private var _sid:String = null
    private var _lsid:String = null
    private var _auth:String = null
    private var _token:String = null

    init
    
    def init() {
        val loginURL    = new URL("%s?service=reader&Email=%s&Passwd=%s".format(_loginURL, user, pass))
        val loginBuffer = new BufferedReader(new InputStreamReader(loginURL.openStream))
        
        _sid  = loginBuffer.readLine.split("=")(1)
        _lsid = loginBuffer.readLine.split("=")(1)
        _auth = loginBuffer.readLine.split("=")(1)

        val tokenURL = new URL(_tokenURL)
        val tokenURLConnection = tokenURL.openConnection
        tokenURLConnection.setRequestProperty("Authorization", "GoogleLogin auth=%s".format(_auth))
        tokenURLConnection.connect
        /// XXX TODO XXX
//        _token = Tools.inputstream2string(tokenURLConnection.getInputStream) TODO XXX
        /// XXX TODO XXX

    }

    def request(query:String):InputStream = {
        val queryURL = new URL(query)
        val queryURLConnection = queryURL.openConnection
        queryURLConnection.setRequestProperty("Authorization", "GoogleLogin auth=%s".format(_auth))
        queryURLConnection.connect
        
        queryURLConnection.getInputStream
    }

    def getSid():String = _sid

    def getLsid():String = _lsid

    def getAuth():String = _auth

    def getToken():String = _token
}