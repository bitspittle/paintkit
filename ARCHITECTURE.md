<!-- Last updated: 2021/04/05 --> 

In order to facilitate multiple users from the ground up, this application is split into a client and a server. The
server owns the source of truth - the current state of all image buffers, history, connected users, etc. - and the
client is mostly a UI frontend on top of that data. This is true even when you are using the app by itself - the client
spawns a server for you and connects to it behind the scenes.

This extra legwork ensures that core data is only modified through message passing, which requires a bit of extra effort
than just calling a function in your code directly, but it ensures that actions easily scale to multi-user, since even
single-user has to essentially behave as a remote connection. As a benefit, it also means we have a core which can be
much more resistant to UI crashes.

The client and server communicate through a very trivial IPC mechanism, with commands and responses defined inside a
protobuf schema used to shuttle byte array payloads across a socket. See the `ipc` module for more information. (This
module is generally useful and could potentially be extracted into its own library someday)

For those raw bytes, we could send anything, but since we're already using protobufs at the IPC level, we also use it
to define a protocol for the actual, specific events needed to communicate between the paintkit client and server, for
example messages for fetching and editing image buffers. See the `api` module for more information.

That's a brief, very high level look. Individual modules may contain their own README files with more information. 