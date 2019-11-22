/**
 *  HA Door Sensor (v.0.0.2)
 *
 *  Authors
 *   - fison67@nate.com
 *  Copyright 2018
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
 
import groovy.json.JsonSlurper

metadata {
	definition (name: "HA Door Sensor", namespace: "fison67", author: "fison67") {
		capability "Contact Sensor"
      	capability "Sensor"
        capability "Refresh"		
        
        attribute "lastCheckin", "Date"
         
        command "setStatus"
	}


	simulator {
	}
    
    preferences {
        input name: "contactValue", title:"HA Open Value" , type: "string", required: true, defaultValue: "on"
	}

	tiles {
		multiAttributeTile(name:"contact", type: "generic", width: 6, height: 4){
			tileAttribute ("device.contact", key: "PRIMARY_CONTROL") {
               	attributeState "open", label:'${name}', icon:"http://postfiles12.naver.net/MjAxODA0MDNfNTMg/MDAxNTIyNzI0MjgzMjk1.H97Au-OWeJ5aUpwpYbhu3P_H_cA0tMHz-EWpgDPmmTcg.Jcbknv16shFQ86cBtY-Z1n4Jx9P1WGWGpj4voOLxzV8g.PNG.shin4299/door_on1.png?type=w3", backgroundColor:"#e86d13"
            	attributeState "closed", label:'${name}', icon:"https://postfiles.pstatic.net/MjAxODA0MDJfMTI3/MDAxNTIyNjcwOTc2NDgy.WVcwn0G7-BnyFTkk4pUxZ44j-810YDbVb81-A-52D1gg.X_0ijEFzbyu8IeYXU_fr0mVtS4v_4JbZncfmoFCPH5cg.PNG.shin4299/door_off.png?type=w3", backgroundColor:"#00a0dc"
			}
            
            tileAttribute("device.lastCheckin", key: "SECONDARY_CONTROL") {
    			attributeState("default", label:'\nLast Update: ${currentValue}')
            }
		}
        
        valueTile("lastOpen_label", "", decoration: "flat") {
            state "default", label:'Last\nOpen'
        }
        valueTile("lastOpen", "device.lastOpen", decoration: "flat", width: 3, height: 1) {
            state "default", label:'${currentValue}'
        }
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh", icon:"st.secondary.refresh"
        }
        valueTile("lastClosed_label", "", decoration: "flat") {
            state "default", label:'Last\nClosed'
        }
        valueTile("lastClosed", "device.lastClosed", decoration: "flat", width: 3, height: 1) {
            state "default", label:'${currentValue}'
        }
        
        valueTile("ha_url", "device.ha_url", width: 3, height: 1) {
            state "val", label:'${currentValue}', defaultState: true
        }
        
        valueTile("entity_id", "device.entity_id", width: 3, height: 1) {
            state "val", label:'${currentValue}', defaultState: true
        }
        
	}

}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

def setStatus(String value){
    if(state.entity_id == null){
    	return
    }
	log.debug "Status[${state.entity_id}] >> ${value}"
    
    def openBaseValue = "on"
    if(contactValue){
    	openBaseValue = contactValue
    }
    
    def now = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
    def _value = (openBaseValue == value ? "open" : "closed")
    	
    if(device.currentValue("contact") != _value){
        sendEvent(name: (_value == "open" ? "lastOpen" : "lastClosed"), value: now, displayed: false )
    }
    sendEvent(name: "contact", value:_value)
    sendEvent(name: "lastCheckin", value: now, displayed: false)
    sendEvent(name: "entity_id", value: state.entity_id, displayed: false)
}

def setHASetting(url, password, deviceId){
	state.app_url = url
    state.app_pwd = password
    state.entity_id = deviceId
    
    sendEvent(name: "ha_url", value: state.app_url, displayed: false)
}

def refresh(){
	log.debug "Refresh"
    def options = [
     	"method": "GET",
        "path": "/api/states/${state.entity_id}",
        "headers": [
        	"HOST": parent._getServerURL(),
            "Authorization": "Bearer " + parent._getPassword(),
            "Content-Type": "application/json"
        ]
    ]
    sendCommand(options, callback)
}

def callback(physicalgraph.device.HubResponse hubResponse){
	def msg
    try {
        msg = parseLanMessage(hubResponse.description)
		def jsonObj = new JsonSlurper().parseText(msg.body)
        setStatus(jsonObj.state)
    } catch (e) {
        log.error "Exception caught while parsing data: "+e;
    }
}

def updated() {
}

def sendCommand(options, _callback){
	def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: _callback])
    sendHubCommand(myhubAction)
}
