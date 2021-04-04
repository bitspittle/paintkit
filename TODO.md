Current goals, in no particular order:

# Client 
* Welcome Window
    * Pressing "New" will prompt a dialog asking for w / h
        * Continuing will start a server and put you into a canvas editor
        * Client will create a messenger and attach to the server
    * Pressing "Open" will show a dialog letting you choose a file (.png for now)
* Canvas Window
    * Create a canvas view that you can draw pixels on
    * Press ctrl-z to undo

# Server

* On start, server should create a receiver, return a user id with
  full permissions
* Server should remove user if no response for ~1 minute
    * Server should auto-shut down if no users

# Protocol

* Flatbuffers?
* Define messages for...
    * uid
    * permission
        * admin
        * editor
        * reader
    * user
    * history
    * action
    * pixel
    * layer
    * buffer
    * commands / responses
        * update pixels
        * add layer
        * remove layer
        * move layer
        * undo
        * redo
        * shutdown
        * update permissions
        * fetch buffer
    * events
        * pixels updated
        * layer removed

* Create messenger for client side, receiver for server side
    * Messenger can send commands, suspends for response
    * Messenger has an event flow
    * Receiver can handle commands, must respond with a response
    * Receiver can send events anytime
    
# Settings

* Stored in ~/.paintkit/
* config.toml - Customizable settings that someone might copy across machines
* state.toml - misc state worth restoring across runs, e.g. recents
* Need a toml parsing library    