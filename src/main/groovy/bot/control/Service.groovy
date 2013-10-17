/*
 * Copyright 2013 Matt Dean
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package bot.control

import bot.comm.*

/**
 * The basic skeleton structure of a Service instance.
 * @author deanydean
 */
public abstract class Service extends Communicator {

    public static final SERVICE = "service"

    public static final START = "start"
    public static final STOP = "stop"

    def name
    def enabled
    def level
    def onComm

    static Closure onServiceEvent = { data ->
        def service = data[0]
        def comm = data[1]
        switch(comm.id){
            case START: service.onStart(); break;
            case STOP: service.onStop(); break;
            default: 
                if(service.onComm)
                    service.onComm(comm)
                else
                    service.LOG.info "Unknown $service op for $comm"
        }
    }

    public Service(name, enabled=false, level=9, onComm=null){
        super(onServiceEvent)
        this.name = name
        this.enabled = enabled
        this.level = level
        this.onComm = onComm
    }

    void init(){
        // Subscribe to comms for me
        this.subscribeTo("${SERVICE}.${this.name}")
    }

    // Service impls must override these methods
    protected abstract void onStart()
    protected abstract void onStop()

    public final void startService(){
        new Comm("${SERVICE}.${this.name}.${START}").publish() 
    }

    public final void stopService(){
        new Comm("${SERVICE}.${this.name}.${STOP}").publish()
    }

}