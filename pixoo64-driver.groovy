/**
 *
 *  Pixoo64 Hubitat Driver
 *
 *  Driver Author TIM DODD
 *
 *
 *    timd v1.0.0    added ability to turn on and off the pixoo64
 */



metadata {
    definition (name: "Pixoo64", namespace: "robododd", author: "Tim Dodd",cateogry:"General",description:"Control a Pixoo64") {
        capability "Refresh"
        capability "Switch"
    }
    
    // Preferences
    preferences {
        input "uri", "text", title: "URI", description: "(eg. http://[pixoo_ip])", required: true, displayDuringSetup: true
         input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

def initialize() {
     installed()
}

def installed() {
    setSchedule()
    refresh()
}

def updated() {
    setSchedule()
    refresh()
}

def setSchedule() {
  logDebug "Setting refresh interval to ${settings.refreshInterval}s"
  unschedule()
  switch(settings.refreshInterval){
    case "0":
      unschedule()
      break
    case "30":
      schedule("0/30 * * ? * * *", refresh)
      break
    case "60":
      schedule("0 * * ? * * *", refresh)
      break
    case "300":
      schedule("0 0/5 * ? * * *", refresh)
      break
    case "600":
      schedule("0 0/10 * ? * * *", refresh)
      break
    case "1800":
      schedule("0 0/30 * ? * * *", refresh)
      break
    case "3600":
      schedule("0 * 0/1 ? * * *", refresh)
      break
    default:
      unschedule()
  }
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def parseResp(resp) {
  
    // Update State
    logDebug resp
    state = resp
   
    synchronize(resp)
}

// Handle async callback
def parseResp(resp, data) {
    if(resp.getStatus() == 200)
        parseResp(resp.getJson())
    else if(resp.getStatus() == 408)
        log.error "HTTP Request Timeout"
    else
        log.error "Unhandled HTTP Error"
}


def parsePostResp(resp, data) {
    if(resp.getStatus() == 200)
        parsePostResp(resp.getJson())
    else if(resp.getStatus() == 408)
        log.error "HTTP Request Timeout"
    else
        log.error "Unhandled HTTP Error"
}

def synchronize(data){

    seg = data.LightSwitch?.toInteger();
    
    // Power
    if(seg > 0){
        if(device.currentValue("switch") != "on")
            sendEvent(name: "switch", value: "on")
    }
    else {
        if(device.currentValue("switch") == "on")
            sendEvent(name: "switch", value: "off")
    }
    
    //TODO: Synchronize everything else
}

// Switch Capabilities
def on() {
    def json = ['Command': "Channel/OnOffScreen", 'OnOff':1]
    sendEthernetPost(json)  
    sendEvent(name: "switch", value: "on")
}

def off() {
    def json = ['Command': "Channel/OnOffScreen", 'OnOff':0]
    sendEthernetPost(json)  
    sendEvent(name: "switch", value: "off")
}



def eventProcess(name,value,unit) {
    def descriptionText = ""
    if (device.currentValue(name).toString() != value.toString()) {
     
            if (unit != null) {
                descriptionText = "${device.displayName} ${name} is ${value}${unit}"
            } else {
                descriptionText = "${device.displayName} ${name} is ${value}"
            }
        
        sendEvent(name: name, value: value, descriptionText: descriptionText,unit: unit)
        if (txtEnable) log.info descriptionText
    }
}

// Device Functions
def refresh() {
    getSettings()
}


def sendEthernetPost(data) {
    if(settings.uri != null){
        
        def params = [
            uri: "${settings.uri}",
            path: "/post",
            requestContentType: 'application/json',
            contentType: 'application/json',
            body:data,
            timeout: 5
        ]

        try {            
            asynchttpPost(null, params)
        } catch (e) {
            log.error "something went wrong: $e"
        }
    }
}

// Helper Functions
def logDebug(message){
    if(logEnable) log.debug(message)
}

def getSettings(){
    logDebug "getSettings"

    def params = [
        uri: "${settings.uri}",
        path: "/post",
        requestContentType: "application/json",
        contentType: 'application/json',
        body: ["Command": "Channel/GetAllConf"],
        timeout: 8
    ]

    try {
        asynchttpPost('parseResp', params)
    } catch (e) {
        log.error "something went wrong: $e"
    }
}

