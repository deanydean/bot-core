/*
 * Copyright 2014 Matt Dean
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
package gart.io

import gart.comm.*
import gart.Gart


import java.nio.file.*

/**
 * A Communicator that will watch the filesystem for changes.
 * @author deanydean
 */
class FilesystemWatch {
    
    public static final String FS_COMM = "fs"
    public static final String FS_CHANGED = "fs.ENTRY_MODIFY"
    public static final String FS_CREATED = "fs.ENTRY_CREATE"
    public static final String FS_DELETED = "fs.ENTRY_DELETE"
    
    public static final String FS_PATH = "fs.path"
    public static final String FS_CHANGETYPE = "fs.changetype"
    
    // Create a single static instance of each
    private static final FS = FileSystems.getDefault()

    def watcher = FS.newWatchService()
    def watchThread
    def watching = [:]
    def location
    
    public FilesystemWatch(location){
        // Init the watch for this location
        def path = FS.getPath(location)
        this.register(path)

        // Init the sub name
        this.location = location

        // Create a daemon thread than handles the nio2 events
        this.watchThread = Thread.startDaemon("fs-watcher for $location"){
            while(true){
                WatchKey watchKey = watcher.take()
                def dir = this.watching.get(watchKey)
                try{
                    for (def event : watchKey.pollEvents()) {
                        def file = dir.resolve(event.context())
                        def type = "${FS_COMM}.${event.kind().name()}"
                        Gart.LOG.debug "fs-change: $type $path"

                        def comm = new Comm(getCommName(type, location))
                        comm.set(FS_CHANGETYPE, type)
                        comm.set(FS_PATH, file.toString())
                        comm.publish()
                    }

                    if(!watchKey.reset()){
                        Gart.LOG.error "Failed to reset fs-watch key"
                        return
                    }
                }catch(e){
                    Gart.LOG.error "Something failed in fs-watcher: $e"
                }
            }
        }
    }

    private WatchKey register(Path location){
        def key = location.register(this.watcher, 
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE, 
            StandardWatchEventKinds.ENTRY_MODIFY)
        this.watching.put(key,location)
    }

    public void addSubscriber(type, communicator){
        // Generate the event name for this
        communicator.subscribeTo(getCommName(type, this.location))
    }

    public static String getCommName(type, location){
        return "${type}.${location}"
    }
}

